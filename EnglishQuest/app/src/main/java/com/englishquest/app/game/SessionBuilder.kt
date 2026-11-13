package com.englishquest.app.game

import com.englishquest.app.data.Content
import com.englishquest.app.data.Progress
import com.englishquest.app.data.UnitInfo
import com.englishquest.app.srs.CardState
import kotlin.math.max
import kotlin.random.Random

/** Compose le contenu de chaque type de session. */
class SessionBuilder(
    private val c: Content,
    private val p: Progress,
    private val rnd: Random,
    private val today: Long,
    ttsReady: Boolean,
) {
    val factory = ExerciseFactory(c, rnd, ttsReady)

    private fun card(id: String): CardState? = p.cards[id]

    /** Exercice pour une carte quelconque (mot ou grammaire). */
    fun exerciseForCard(id: String, quick: Boolean = false, hard: Boolean = false): Exercise? {
        if (id.startsWith("w:")) {
            val w = c.wordById[id.removePrefix("w:")] ?: return null
            val allowed = when {
                quick -> listOf(WordEx.MCQ_EN_FR, WordEx.MCQ_FR_EN, WordEx.FILL)
                hard -> listOf(WordEx.TYPE_FR_EN, WordEx.MCQ_FR_EN, WordEx.FILL, WordEx.LISTEN_WORD, WordEx.TYPE_EN_FR)
                else -> null
            }
            // Une carte jamais vue est traitée comme « vue une fois » hors leçon (pas d'écran « nouveau mot »)
            val cs = card(id) ?: if (allowed == null) CardState(seen = 1) else null
            return factory.wordExercise(w, cs, allowed)
        }
        if (id.startsWith("g:")) {
            val parts = id.split(":")
            if (parts.size != 3) return null
            val g = c.grammarById[parts[1]] ?: return null
            val i = parts[2].toIntOrNull() ?: return null
            if (i !in g.items.indices) return null
            return if (quick) factory.quickGrammarExercise(g, i) else factory.grammarExercise(g, i)
        }
        return null
    }

    /** Leçon k d'une unité : nouveaux mots + grammaire + paires + un peu de révision. */
    fun lesson(u: UnitInfo, k: Int): List<Exercise> {
        val words = c.lessonWords(u, k)
        val newWords = words.filter { (card(Content.wordCardId(it.id))?.seen ?: 0) == 0 }.take(7)
        val known = words.filter { it !in newWords }

        val intro = newWords.map { factory.wordExercise(it, null) }
        val reinforce = newWords.shuffled(rnd).take(4).map { factory.wordExercise(it, CardState(seen = 1)) }
        val knownEx = known.shuffled(rnd).take(max(0, 7 - newWords.size)).map {
            factory.wordExercise(it, card(Content.wordCardId(it.id)) ?: CardState(seen = 1))
        }

        val g = c.grammarById[u.grammar]
        val grammarEx = if (g == null || g.items.isEmpty()) emptyList() else {
            val main = g.items.indices.filter { it % Content.LESSONS_PER_UNIT == k }
            val extra = g.items.indices.filter { it !in main }.shuffled(rnd).take(1)
            (main + extra).map { factory.grammarExercise(g, it) }
        }

        val unitIds = words.map { Content.wordCardId(it.id) }.toSet()
        val reviews = dueCards().filter { it !in unitIds }.take(2).mapNotNull { exerciseForCard(it) }

        val first = (intro + grammarEx.take(1)).shuffled(rnd)
        val second = (reinforce + knownEx + grammarEx.drop(1) + reviews).shuffled(rnd)
        val result = ArrayList<Exercise>()
        result += first
        val matchWords = words.shuffled(rnd).take(5)
        if (matchWords.size >= 3) result += factory.match(matchWords)
        result += second
        return result
    }

    /** Cartes à réviser aujourd'hui, les plus en retard d'abord. */
    fun dueCards(): List<String> = p.cards.entries
        .filter { it.value.seen > 0 && it.value.due <= today }
        .sortedBy { it.value.due }
        .map { it.key }

    fun dueCount(): Int = dueCards().size

    /** Session de révision (SM-2) : cartes dues, complétées par les cartes les plus fragiles. */
    fun review(): List<Exercise> {
        var ids = dueCards().take(15)
        if (ids.size < 8) {
            val weakest = p.cards.entries
                .filter { it.value.seen > 0 && it.key !in ids }
                .sortedBy { it.value.ef + it.value.reps }
                .map { it.key }
            ids = ids + weakest.take(8 - ids.size)
        }
        val list = ids.shuffled(rnd).mapNotNull { exerciseForCard(it) }.toMutableList()
        val wordIds = ids.filter { it.startsWith("w:") }.mapNotNull { c.wordById[it.removePrefix("w:")] }
        if (wordIds.size >= 4) list.add(list.size / 2, factory.match(wordIds.shuffled(rnd).take(5)))
        return list
    }

    /** Entraînement sur une leçon de grammaire. */
    fun grammarPractice(lessonId: String): List<Exercise> {
        val g = c.grammarById[lessonId] ?: return emptyList()
        return g.items.indices.shuffled(rnd).take(10).map { factory.grammarExercise(g, it) }
    }

    /** Cartes utilisables dans les mini-jeux : celles des unités débloquées. */
    fun pool(unlockedUnits: Int): List<String> {
        val ids = c.units.take(max(1, unlockedUnits)).flatMap { c.unitCardIds(it) }
        val seen = ids.filter { (card(it)?.seen ?: 0) > 0 }
        return if (seen.size >= 12) seen else ids
    }

    /** Générateur infini pour contre-la-montre / survie / boss. */
    fun generator(ids: List<String>, quick: Boolean, hard: Boolean): () -> Exercise? {
        var last: String? = null
        return gen@{
            if (ids.isEmpty()) return@gen null
            repeat(12) {
                val id = ids[rnd.nextInt(ids.size)]
                if (id != last || ids.size == 1) {
                    val ex = exerciseForCard(id, quick = quick, hard = hard)
                    if (ex != null) {
                        last = id
                        return@gen ex
                    }
                }
            }
            null
        }
    }

    /** Cartes du boss d'une unité. */
    fun bossCards(u: UnitInfo): List<String> = c.unitCardIds(u)

    /** Nouvelle version d'un exercice raté (posée à nouveau en fin de session). */
    fun retry(ex: Exercise): Exercise {
        val id = ex.cardIds.firstOrNull()
        if (ex !is Exercise.Match && id != null && id.startsWith("w:")) {
            val w = c.wordById[id.removePrefix("w:")]
            if (w != null) return factory.wordExercise(w, CardState(seen = 1))
        }
        return ex.renewed()
    }
}
