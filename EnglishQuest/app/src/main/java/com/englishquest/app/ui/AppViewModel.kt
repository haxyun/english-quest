package com.englishquest.app.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.englishquest.app.audio.SoundFx
import com.englishquest.app.audio.Speaker
import com.englishquest.app.data.Content
import com.englishquest.app.data.ContentLoader
import com.englishquest.app.data.DayStats
import com.englishquest.app.data.Progress
import com.englishquest.app.data.ProgressStore
import com.englishquest.app.data.Settings
import com.englishquest.app.game.Badge
import com.englishquest.app.game.Badges
import com.englishquest.app.game.ChestReward
import com.englishquest.app.game.Chests
import com.englishquest.app.game.Exercise
import com.englishquest.app.game.Levels
import com.englishquest.app.game.Mode
import com.englishquest.app.game.Rarity
import com.englishquest.app.game.SessionBuilder
import com.englishquest.app.game.SessionController
import com.englishquest.app.notif.ReminderScheduler
import com.englishquest.app.srs.CardState
import com.englishquest.app.srs.Sm2
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalTime
import kotlin.math.max
import kotlin.random.Random

sealed interface Screen {
    data object Loading : Screen
    data object Onboarding : Screen
    data class Main(val tab: Int) : Screen
    data class Course(val lessonId: String) : Screen
    data object Session : Screen
    data class Result(val result: SessionResult) : Screen
}

data class SessionResult(
    val mode: Mode,
    val won: Boolean,
    val emoji: String,
    val title: String,
    val subtitle: String,
    val xp: Int,
    val answered: Int,
    val correct: Int,
    val maxCombo: Int,
    val seconds: Int,
    val levelUp: Boolean,
    val newLevel: Int,
    val rank: String,
    val streak: Int,
    val streakExtended: Boolean,
    val freezeEarned: Boolean,
    val goalReached: Boolean,
    val record: Boolean,
    val newBadges: List<Badge>,
    val chest: ChestReward?,
)

class AppViewModel(app: Application) : AndroidViewModel(app) {

    private val store = ProgressStore(app)
    private val saveMutex = Mutex()
    private val rnd = Random(System.currentTimeMillis())

    val speaker = Speaker(app)

    var content: Content? by mutableStateOf(null)
        private set

    private val _progress = MutableStateFlow(Progress())
    val progress: StateFlow<Progress> = _progress

    var screen: Screen by mutableStateOf(Screen.Loading)
        private set

    var session: SessionController? by mutableStateOf(null)
        private set

    /** Message affiché en surimpression (série perdue, gel utilisé…). */
    var notice: String? by mutableStateOf(null)

    private var loaded = false

    init {
        viewModelScope.launch {
            val c = withContext(Dispatchers.IO) { ContentLoader.load(app) }
            val p = withContext(Dispatchers.IO) { store.load() }
            content = c
            _progress.value = p
            applySettingsSideEffects(p.settings)
            loaded = true
            refreshStreak()
            screen = if (p.onboarded) Screen.Main(0) else Screen.Onboarding
            ReminderScheduler.schedule(app, p.settings)
        }
    }

    override fun onCleared() {
        speaker.shutdown()
        super.onCleared()
    }

    fun onResume() {
        if (loaded) refreshStreak()
    }

    fun today(): Long = LocalDate.now().toEpochDay()

    private fun update(f: (Progress) -> Progress) {
        _progress.value = f(_progress.value)
        persist()
    }

    private fun persist() {
        viewModelScope.launch(Dispatchers.IO) {
            saveMutex.withLock { store.save(_progress.value) }
        }
    }

    // ---------------------------------------------------------------- navigation

    fun go(s: Screen) {
        screen = s
    }

    fun openTab(tab: Int) = go(Screen.Main(tab))

    // ---------------------------------------------------------------- carte & unités

    fun unitUnlocked(index: Int, p: Progress): Boolean {
        val c = content ?: return false
        if (index == 0) return true
        val prev = c.units.getOrNull(index - 1) ?: return false
        return prev.id in p.bosses
    }

    /** Première unité dont le boss n'est pas encore vaincu. */
    fun currentUnitIndex(p: Progress): Int {
        val c = content ?: return 0
        val i = c.units.indexOfFirst { it.id !in p.bosses }
        return if (i < 0) c.units.lastIndex.coerceAtLeast(0) else i
    }

    fun unlockedUnitCount(p: Progress): Int = currentUnitIndex(p) + 1

    fun dueCount(p: Progress): Int = p.cards.count { it.value.seen > 0 && it.value.due <= today() }

    // ---------------------------------------------------------------- séries (streak)

    fun refreshStreak() {
        val p = _progress.value
        val t = today()
        if (p.lastActiveDay < 0 || p.streak == 0) return
        val gap = t - p.lastActiveDay
        if (gap <= 1) return
        val missed = (gap - 1).toInt()
        if (p.freezes >= missed) {
            update {
                it.copy(freezes = it.freezes - missed, lastActiveDay = t - 1, freezesUsed = it.freezesUsed + missed)
            }
            val (np, _) = Badges.evaluate(_progress.value, null, content?.units?.size ?: 0, t, System.currentTimeMillis())
            if (np != _progress.value) update { np }
            notice = if (missed == 1) "❄️ Ouf ! Un gel de série a protégé ta série de ${p.streak} jours hier."
            else "❄️ $missed gels de série ont protégé ta série de ${p.streak} jours."
        } else {
            update { it.copy(streak = 0, lostStreak = it.lostStreak || p.streak >= 3) }
            notice = "💔 Ta série de ${p.streak} jour(s) s'est éteinte… Une leçon aujourd'hui pour la rallumer !"
        }
    }

    // ---------------------------------------------------------------- démarrage des sessions

    private fun builder(): SessionBuilder? {
        val c = content ?: return null
        return SessionBuilder(c, _progress.value, rnd, today(), speaker.ready)
    }

    fun startLesson(unitIndex: Int, lessonIndex: Int) {
        val c = content ?: return
        val u = c.units.getOrNull(unitIndex) ?: return
        val b = builder() ?: return
        val list = b.lesson(u, lessonIndex)
        if (list.isEmpty()) return
        startSession(
            Mode.LESSON, "${u.emoji} ${u.title} · leçon ${lessonIndex + 1}", unitIndex, lessonIndex,
            list, null, b,
        )
    }

    fun startBoss(unitIndex: Int) {
        val c = content ?: return
        val u = c.units.getOrNull(unitIndex) ?: return
        val b = builder() ?: return
        val gen = b.generator(b.bossCards(u), quick = false, hard = true)
        startSession(
            Mode.BOSS, "Boss : ${u.bossName}", unitIndex, -1, emptyList(), gen, b,
            bossName = u.bossName, bossEmoji = u.bossEmoji,
        )
    }

    fun startReview() {
        val b = builder() ?: return
        val list = b.review()
        if (list.isEmpty()) {
            notice = "🧠 Rien à réviser pour l'instant : termine d'abord une leçon sur la carte !"
            return
        }
        startSession(Mode.REVIEW, "🧠 Révisions intelligentes", -1, -1, list, null, b)
    }

    fun startPractice(lessonId: String) {
        val c = content ?: return
        val g = c.grammarById[lessonId] ?: return
        val b = builder() ?: return
        val list = b.grammarPractice(lessonId)
        if (list.isEmpty()) return
        startSession(Mode.PRACTICE, "${g.emoji} ${g.title}", -1, -1, list, null, b)
    }

    fun startTimeAttack() {
        val b = builder() ?: return
        val gen = b.generator(b.pool(unlockedUnitCount(_progress.value)), quick = true, hard = false)
        startSession(Mode.TIME_ATTACK, "⏱️ Contre-la-montre", -1, -1, emptyList(), gen, b)
    }

    fun startSurvival() {
        val b = builder() ?: return
        val gen = b.generator(b.pool(unlockedUnitCount(_progress.value)), quick = false, hard = false)
        startSession(Mode.SURVIVAL, "❤️ Mode survie", -1, -1, emptyList(), gen, b)
    }

    private fun startSession(
        mode: Mode,
        title: String,
        unitIndex: Int,
        lessonIndex: Int,
        list: List<Exercise>,
        gen: (() -> Exercise?)?,
        b: SessionBuilder,
        bossName: String = "",
        bossEmoji: String = "",
    ) {
        val s = SessionController(
            mode = mode,
            title = title,
            unitIndex = unitIndex,
            lessonIndex = lessonIndex,
            initial = list,
            generator = gen,
            retry = { b.retry(it) },
            onAnswer = { results, typo -> recordAnswer(results, typo) },
            onFinish = { finishSession(it) },
            bossName = bossName,
            bossEmoji = bossEmoji,
        )
        if (s.finished) return // aucun exercice disponible
        session = s
        screen = Screen.Session
    }

    // ---------------------------------------------------------------- réponses

    private fun recordAnswer(results: Map<String, Boolean>, typo: Boolean) {
        val t = today()
        update { p ->
            val cards = HashMap(p.cards)
            for ((id, ok) in results) {
                val q = if (!ok) Sm2.FAIL else if (typo) Sm2.HARD else Sm2.GOOD
                cards[id] = Sm2.review(cards[id] ?: CardState(), q, t)
            }
            val good = results.values.all { it }
            val day = p.days[t] ?: DayStats()
            p.copy(
                cards = cards,
                totalAnswers = p.totalAnswers + 1,
                totalCorrect = p.totalCorrect + if (good) 1 else 0,
                days = p.days + (t to day.copy(answers = day.answers + 1, correct = day.correct + if (good) 1 else 0)),
            )
        }
    }

    fun quitSession() {
        val s = session ?: return
        val seconds = s.elapsedSeconds()
        val t = today()
        update { p ->
            val day = p.days[t] ?: DayStats()
            p.copy(totalSeconds = p.totalSeconds + seconds, days = p.days + (t to day.copy(seconds = day.seconds + seconds)))
        }
        session = null
        screen = Screen.Main(if (s.mode == Mode.LESSON || s.mode == Mode.BOSS) 0 else 1)
    }

    private fun finishSession(s: SessionController) {
        val c = content ?: return
        if (s.answered == 0) {
            // Rien n'a été joué (ex. chrono écoulé sans réponse) : retour simple.
            session = null
            screen = Screen.Main(if (s.mode == Mode.LESSON || s.mode == Mode.BOSS) 0 else 1)
            return
        }
        refreshStreak()
        val p0 = _progress.value
        val t = today()
        var p = p0
        val seconds = s.elapsedSeconds()
        val levelBefore = Levels.level(p0.totalXp)

        val trainingMode = s.mode == Mode.LESSON || s.mode == Mode.REVIEW || s.mode == Mode.PRACTICE
        val perfect = trainingMode && s.wrongCount == 0 && s.answered >= 8
        val bonus = when (s.mode) {
            Mode.LESSON -> 20 + if (perfect) 15 else 0
            Mode.REVIEW, Mode.PRACTICE -> 15 + if (perfect) 10 else 0
            Mode.BOSS -> if (s.won) 60 else 0
            Mode.TIME_ATTACK, Mode.SURVIVAL -> 0
        }
        val sessionXp = s.xp + bonus

        // Série de jours
        var streakExtended = false
        var freezeEarned = false
        if (s.answered >= 3 && p.lastActiveDay != t) {
            val newStreak = if (p.lastActiveDay == t - 1) p.streak + 1 else 1
            p = p.copy(streak = newStreak, bestStreak = max(p.bestStreak, newStreak), lastActiveDay = t)
            streakExtended = true
            if (newStreak % 7 == 0 && p.freezes < Chests.MAX_FREEZES) {
                p = p.copy(freezes = p.freezes + 1)
                freezeEarned = true
            }
        }

        // Objectif quotidien et statistiques du jour
        val goalSec = p.settings.dailyGoalMin * 60
        val day = p.days[t] ?: DayStats()
        val goalBefore = day.seconds >= goalSec
        val newDay = day.copy(xp = day.xp + sessionXp, seconds = day.seconds + seconds, sessions = day.sessions + 1)
        val goalReached = !goalBefore && newDay.seconds >= goalSec
        p = p.copy(
            totalXp = p.totalXp + sessionXp,
            totalSeconds = p.totalSeconds + seconds,
            sessions = p.sessions + 1,
            days = p.days + (t to newDay),
            bestCombo = max(p.bestCombo, s.maxCombo),
            perfectSessions = p.perfectSessions + if (perfect) 1 else 0,
            gems = p.gems + if (goalReached) 10 else 0,
        )

        // Progression sur la carte
        val unit = c.units.getOrNull(s.unitIndex)
        if (unit != null && s.mode == Mode.LESSON) {
            val done = p.lessonsDone[unit.id] ?: 0
            if (s.lessonIndex == done) p = p.copy(lessonsDone = p.lessonsDone + (unit.id to done + 1))
        }
        if (unit != null && s.mode == Mode.BOSS && s.won) p = p.copy(bosses = p.bosses + unit.id)

        // Records
        var record = false
        if (s.mode == Mode.TIME_ATTACK && s.correctCount > p.bestTimeAttack) {
            record = p.bestTimeAttack > 0
            p = p.copy(bestTimeAttack = s.correctCount)
        }
        if (s.mode == Mode.SURVIVAL && s.correctCount > p.bestSurvival) {
            record = p.bestSurvival > 0
            p = p.copy(bestSurvival = s.correctCount)
        }

        // Coffre aléatoire
        var chest: ChestReward? = null
        if (s.answered >= 5 && (s.won || s.mode == Mode.SURVIVAL)) {
            val r = Chests.roll(rnd, atLeastRare = s.mode == Mode.BOSS, freezes = p.freezes)
            chest = r
            p = p.copy(
                totalXp = p.totalXp + r.xp,
                gems = p.gems + r.gems,
                freezes = if (r.freeze) (p.freezes + 1).coerceAtMost(Chests.MAX_FREEZES) else p.freezes,
                chests = p.chests + 1,
                epicChests = p.epicChests + if (r.rarity == Rarity.EPIC) 1 else 0,
            )
        }

        // Badges
        val (pb, newBadges) = Badges.evaluate(p, LocalTime.now().hour, c.units.size, t, System.currentTimeMillis())
        p = pb

        _progress.value = p
        persist()

        val levelAfter = Levels.level(p.totalXp)
        val accuracy = if (s.answered > 0) s.correctCount * 100 / s.answered else 0
        val (emoji, title, subtitle) = when (s.mode) {
            Mode.BOSS -> if (s.won) Triple(s.bossEmoji, "Boss vaincu !", "${s.bossName} est K.O. L'unité suivante est débloquée !")
            else Triple("💀", "Défaite…", "${s.bossName} t'a eu cette fois. Révise et retente ta chance !")
            Mode.TIME_ATTACK -> Triple("⏱️", "Temps écoulé !", "${s.correctCount} bonnes réponses en 60 secondes.")
            Mode.SURVIVAL -> Triple("❤️", "Fin de la survie", "Tu as tenu ${s.correctCount} bonnes réponses.")
            Mode.LESSON -> Triple(if (perfect) "💯" else "🎉", "Leçon terminée !", "Précision : $accuracy %")
            Mode.REVIEW -> Triple("🧠", "Révisions terminées !", "Précision : $accuracy %")
            Mode.PRACTICE -> Triple("📚", "Entraînement terminé !", "Précision : $accuracy %")
        }

        if (levelAfter > levelBefore) SoundFx.levelUp()

        session = null
        screen = Screen.Result(
            SessionResult(
                mode = s.mode, won = s.won, emoji = emoji, title = title, subtitle = subtitle,
                xp = sessionXp, answered = s.answered, correct = s.correctCount, maxCombo = s.maxCombo,
                seconds = seconds, levelUp = levelAfter > levelBefore, newLevel = levelAfter,
                rank = Levels.rank(levelAfter), streak = p.streak, streakExtended = streakExtended,
                freezeEarned = freezeEarned, goalReached = goalReached, record = record,
                newBadges = newBadges, chest = chest,
            )
        )
    }

    fun closeResult(r: SessionResult) {
        openTab(
            when (r.mode) {
                Mode.LESSON, Mode.BOSS -> 0
                else -> 1
            }
        )
    }

    // ---------------------------------------------------------------- boutique, réglages

    fun buyFreeze(): Boolean {
        val p = _progress.value
        if (p.gems < Chests.FREEZE_PRICE || p.freezes >= Chests.MAX_FREEZES) return false
        update { it.copy(gems = it.gems - Chests.FREEZE_PRICE, freezes = it.freezes + 1) }
        SoundFx.chest()
        return true
    }

    fun updateSettings(f: (Settings) -> Settings) {
        update { it.copy(settings = f(it.settings)) }
        val s = _progress.value.settings
        applySettingsSideEffects(s)
        ReminderScheduler.schedule(getApplication<Application>(), s)
    }

    private fun applySettingsSideEffects(s: Settings) {
        SoundFx.enabled = s.sound
        speaker.rate = s.ttsRate
    }

    fun completeOnboarding(goalMin: Int, reminderOn: Boolean) {
        update { it.copy(onboarded = true, settings = it.settings.copy(dailyGoalMin = goalMin, reminderOn = reminderOn)) }
        val s = _progress.value.settings
        applySettingsSideEffects(s)
        ReminderScheduler.schedule(getApplication<Application>(), s)
        screen = Screen.Main(0)
    }

    fun resetProgress() {
        val keep = _progress.value.settings
        _progress.value = Progress(onboarded = true, settings = keep)
        persist()
        notice = "Progression remise à zéro. Nouveau départ ! 🚀"
        screen = Screen.Main(0)
    }
}
