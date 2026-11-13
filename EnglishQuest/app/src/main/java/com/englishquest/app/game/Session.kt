package com.englishquest.app.game

import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.math.max
import kotlin.math.min

enum class Mode { LESSON, REVIEW, PRACTICE, TIME_ATTACK, SURVIVAL, BOSS }

data class Feedback(
    val correct: Boolean,
    val typo: Boolean,
    val solution: String,
    val explanation: String,
    val xp: Int,
    val canOverride: Boolean,
    val userAnswer: String?,
)

/**
 * Moteur d'une session de jeu : file d'exercices, vérification, combos, vies, chrono, boss.
 * Les champs « by mutableStateOf » sont observés directement par l'interface Compose.
 */
class SessionController(
    val mode: Mode,
    val title: String,
    val unitIndex: Int,
    val lessonIndex: Int,
    initial: List<Exercise>,
    private val generator: (() -> Exercise?)?,
    private val retry: (Exercise) -> Exercise,
    private val onAnswer: (results: Map<String, Boolean>, typo: Boolean) -> Unit,
    private val onFinish: (SessionController) -> Unit,
    val bossName: String = "",
    val bossEmoji: String = "",
) {
    private val queue = ArrayDeque(initial)

    var current by mutableStateOf<Exercise?>(null)
        private set
    var feedback by mutableStateOf<Feedback?>(null)
        private set
    /** Réponse en cours de saisie (avant d'appuyer sur « Vérifier »). */
    var draft by mutableStateOf<Answer?>(null)

    var combo by mutableIntStateOf(0)
        private set
    var maxCombo by mutableIntStateOf(0)
        private set
    var xp by mutableIntStateOf(0)
        private set
    var answered by mutableIntStateOf(0)
        private set
    var correctCount by mutableIntStateOf(0)
        private set
    var wrongCount by mutableIntStateOf(0)
        private set
    var planned by mutableIntStateOf(max(1, initial.size))
        private set

    var lives by mutableIntStateOf(if (mode == Mode.SURVIVAL || mode == Mode.BOSS) 3 else -1)
        private set
    val bossMaxHp = 12
    var bossHp by mutableIntStateOf(bossMaxHp)
        private set
    var timeLeftMs by mutableLongStateOf(if (mode == Mode.TIME_ATTACK) TIME_ATTACK_MS else 0L)
        private set

    var finished by mutableStateOf(false)
        private set
    var won by mutableStateOf(false)
        private set

    // Compteurs utilisés comme déclencheurs d'animations
    var comboFlash by mutableIntStateOf(0)
        private set
    var hitTrigger by mutableIntStateOf(0)
        private set
    var wrongTrigger by mutableIntStateOf(0)
        private set

    private val startedAt = SystemClock.elapsedRealtime()
    private var pendingResults: Map<String, Boolean> = emptyMap()
    private var pendingTypo = false

    init {
        advance()
    }

    val progress: Float
        get() = if (mode == Mode.LESSON || mode == Mode.REVIEW || mode == Mode.PRACTICE)
            (answered.toFloat() / planned).coerceIn(0f, 1f) else 0f

    fun elapsedSeconds(): Int =
        ((SystemClock.elapsedRealtime() - startedAt) / 1000L).toInt().coerceIn(0, 3600)

    private fun xpFor(comboAfter: Int): Int {
        val base = when (mode) {
            Mode.BOSS -> 15
            Mode.TIME_ATTACK -> 6
            Mode.SURVIVAL -> 8
            else -> 10
        }
        return base + min(max(0, comboAfter - 1) * 2, 10)
    }

    fun check(answer: Answer) {
        val ex = current ?: return
        if (feedback != null || finished) return
        var typo = false
        var userText: String? = null
        var explanation = ex.explanation
        var solution = ex.solution
        val outcome: Pair<Boolean, Map<String, Boolean>> = when (ex) {
            is Exercise.Choice -> {
                val i = (answer as? Answer.Index)?.i ?: -1
                val good = i == ex.correct
                if (i in ex.options.indices && !good) userText = ex.options[i]
                good to mapOf(ex.cardId to good)
            }
            is Exercise.Typing -> {
                val s = (answer as? Answer.Text)?.s ?: ""
                val r = AnswerCheck.check(s, ex.accepted, ex.sentence)
                val good = r != AnswerCheck.Result.WRONG
                typo = r == AnswerCheck.Result.TYPO
                userText = s.trim().ifEmpty { null }
                good to mapOf(ex.cardId to good)
            }
            is Exercise.Order -> {
                val t = (answer as? Answer.Tokens)?.t ?: emptyList()
                val good = AnswerCheck.sameSentence(t.joinToString(" "), ex.answer.joinToString(" "))
                userText = t.joinToString(" ").ifEmpty { null }
                good to mapOf(ex.cardId to good)
            }
            is Exercise.SpotError -> {
                val i = (answer as? Answer.Index)?.i ?: -1
                val good = i == ex.wrong
                if (i in ex.tokens.indices && !good) userText = "Tu as touché « ${ex.tokens[i]} »"
                good to mapOf(ex.cardId to good)
            }
            is Exercise.Match -> {
                val failed = (answer as? Answer.MatchDone)?.failed ?: emptySet()
                val missed = ex.pairs.filter { it.cardId in failed }
                explanation = if (missed.isEmpty()) "Toutes les paires du premier coup !"
                else "À revoir :\n" + missed.joinToString("\n") { "• ${it.left} = ${it.right}" }
                solution = ""
                (failed.size <= 1) to ex.pairs.associate { it.cardId to (it.cardId !in failed) }
            }
        }
        val ok = outcome.first
        val results = outcome.second
        pendingResults = results
        pendingTypo = typo
        if (!ok) wrongTrigger++
        feedback = Feedback(
            correct = ok,
            typo = typo,
            solution = solution,
            explanation = explanation,
            xp = if (ok) xpFor(combo + 1) else 0,
            canOverride = !ok && ex is Exercise.Typing,
            userAnswer = userText,
        )
    }

    /** Passe à la question suivante. [override] = « J'avais bon » (réponse tapée jugée correcte). */
    fun proceed(override: Boolean = false) {
        val fb = feedback ?: return
        val ex = current ?: return
        var ok = fb.correct
        var results = pendingResults
        if (override && !ok) {
            ok = true
            results = results.mapValues { true }
        }
        onAnswer(results, pendingTypo)
        answered++

        if (ok) {
            combo++
            maxCombo = max(maxCombo, combo)
            correctCount++
            xp += xpFor(combo)
            if (combo % 5 == 0) comboFlash++
            if (mode == Mode.BOSS) {
                val dmg = if (combo >= 3) 2 else 1
                bossHp = max(0, bossHp - dmg)
                hitTrigger++
                if (bossHp == 0) {
                    feedback = null
                    finish(true)
                    return
                }
            }
        } else {
            combo = 0
            wrongCount++
            if (lives > 0) {
                lives--
                if (lives == 0) {
                    feedback = null
                    finish(mode == Mode.SURVIVAL)
                    return
                }
            }
            if ((mode == Mode.LESSON || mode == Mode.REVIEW || mode == Mode.PRACTICE) && ex !is Exercise.Match) {
                queue.addLast(retry(ex))
                planned++
            }
        }
        feedback = null
        advance()
    }

    private fun advance() {
        draft = null
        if (mode == Mode.TIME_ATTACK && timeLeftMs <= 0L) {
            finish(true)
            return
        }
        var next = queue.removeFirstOrNull()
        if (next == null) next = generator?.invoke()
        if (next == null) {
            finish(mode != Mode.BOSS)
            return
        }
        current = next
    }

    /** Appelé régulièrement par l'écran en mode contre-la-montre. */
    fun tick(deltaMs: Long) {
        if (finished || mode != Mode.TIME_ATTACK) return
        val fb = feedback
        if (fb != null && !fb.correct) return // chrono en pause pendant la lecture de l'explication
        timeLeftMs = max(0L, timeLeftMs - deltaMs)
        if (timeLeftMs == 0L && fb == null) finish(true)
    }

    private fun finish(victory: Boolean) {
        if (finished) return
        finished = true
        won = victory
        onFinish(this)
    }

    companion object {
        const val TIME_ATTACK_MS = 60_000L
    }
}
