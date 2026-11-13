package com.englishquest.app.srs

import kotlin.math.max
import kotlin.math.roundToInt

/**
 * État de répétition espacée d'une carte (un mot ou un exercice de grammaire).
 * Les dates sont des « epoch days » (nombre de jours depuis le 1er janvier 1970).
 */
data class CardState(
    val ef: Double = 2.5,      // facteur de facilité (SM-2)
    val reps: Int = 0,         // nombre de réussites consécutives
    val interval: Int = 0,     // intervalle actuel en jours
    val due: Long = 0,         // jour où la carte doit revenir
    val last: Long = -1,       // dernier jour de révision
    val seen: Int = 0,         // nombre total de passages
    val correct: Int = 0,      // nombre total de bonnes réponses
    val lapses: Int = 0,       // nombre d'oublis
)

/**
 * Algorithme SM-2 (SuperMemo 2), légèrement adapté :
 * - qualité 0..5 (on utilise 4 = bonne réponse, 3 = bonne réponse avec faute de frappe, 1 = erreur) ;
 * - une erreur remet la carte à zéro et la rend « due » aujourd'hui ;
 * - plusieurs bonnes réponses le même jour ne font pas exploser l'intervalle.
 */
object Sm2 {
    const val GOOD = 4
    const val HARD = 3
    const val FAIL = 1

    fun review(c: CardState, quality: Int, today: Long): CardState {
        val q = quality.coerceIn(0, 5)
        val seen = c.seen + 1
        if (q < 3) {
            return c.copy(
                ef = max(1.3, c.ef - 0.2),
                reps = 0,
                interval = 0,
                due = today,
                last = today,
                seen = seen,
                lapses = c.lapses + 1,
            )
        }
        val correct = c.correct + 1
        if (c.last == today && c.reps > 0) {
            // Déjà réussie aujourd'hui : on compte la réponse sans repousser la carte.
            return c.copy(seen = seen, correct = correct)
        }
        val reps = c.reps + 1
        val interval = when (reps) {
            1 -> 1
            2 -> 6
            else -> max(1, (c.interval * c.ef).roundToInt())
        }
        val ef = max(1.3, c.ef + (0.1 - (5 - q) * (0.08 + (5 - q) * 0.02)))
        return CardState(ef, reps, interval, today + interval, today, seen, correct, c.lapses)
    }

    fun isLearned(c: CardState?) = c != null && c.reps >= 1
    fun isMastered(c: CardState?) = c != null && c.interval >= 21
}
