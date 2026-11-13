package com.englishquest.app.game

import com.englishquest.app.data.Progress
import com.englishquest.app.srs.Sm2
import kotlin.random.Random

// ------------------------------------------------------------------ niveaux et rangs

object Levels {
    /** XP totale nécessaire pour atteindre le niveau [level] (niv. 2 = 100, niv. 3 = 300, niv. 4 = 600…). */
    fun xpForLevel(level: Int): Int = 50 * level * (level - 1)

    fun level(xp: Int): Int {
        var l = 1
        while (xpForLevel(l + 1) <= xp) l++
        return l
    }

    /** Progression (0..1) vers le niveau suivant. */
    fun progress(xp: Int): Float {
        val l = level(xp)
        val start = xpForLevel(l)
        val end = xpForLevel(l + 1)
        return ((xp - start).toFloat() / (end - start)).coerceIn(0f, 1f)
    }

    fun xpToNext(xp: Int): Int = xpForLevel(level(xp) + 1) - xp

    private val ranks = listOf(
        1 to "Croissant débutant 🥐",
        3 to "Tea Time Rookie ☕",
        5 to "Phrasal Padawan 🧑‍🚀",
        7 to "Grammar Gremlin 👾",
        9 to "Idiom Hunter 🏹",
        11 to "Double-Decker Driver 🚌",
        13 to "Shakespeare's Intern 🎭",
        15 to "Present Perfect Pro ⏳",
        18 to "Word Wizard 🧙",
        21 to "Bac Slayer ⚔️",
        25 to "Legend of London 👑",
    )

    fun rank(level: Int): String = ranks.last { level >= it.first }.second

    fun nextRank(level: Int): Pair<Int, String>? = ranks.firstOrNull { it.first > level }
}

// ------------------------------------------------------------------ badges

class Badge(
    val id: String,
    val emoji: String,
    val name: String,
    val desc: String,
    val hidden: Boolean,
    val condition: (p: Progress, hour: Int?, units: Int, today: Long) -> Boolean,
)

object Badges {
    fun learnedWords(p: Progress) = p.cards.count { it.key.startsWith("w:") && Sm2.isLearned(it.value) }
    fun masteredWords(p: Progress) = p.cards.count { it.key.startsWith("w:") && Sm2.isMastered(it.value) }

    val all: List<Badge> = listOf(
        Badge("first", "🐣", "Premier pas", "Termine ta première session.", false) { p, _, _, _ -> p.sessions >= 1 },
        Badge("streak3", "🔥", "Ça chauffe", "Série de 3 jours.", false) { p, _, _, _ -> p.bestStreak >= 3 },
        Badge("streak7", "🌋", "Semaine de feu", "Série de 7 jours.", false) { p, _, _, _ -> p.bestStreak >= 7 },
        Badge("streak30", "☄️", "Inarrêtable", "Série de 30 jours.", false) { p, _, _, _ -> p.bestStreak >= 30 },
        Badge("combo10", "⚡", "Combo x10", "10 bonnes réponses d'affilée.", false) { p, _, _, _ -> p.bestCombo >= 10 },
        Badge("combo25", "🌩️", "Combo x25", "25 bonnes réponses d'affilée.", false) { p, _, _, _ -> p.bestCombo >= 25 },
        Badge("words50", "📗", "50 mots", "Apprends 50 mots.", false) { p, _, _, _ -> learnedWords(p) >= 50 },
        Badge("words200", "📘", "200 mots", "Apprends 200 mots.", false) { p, _, _, _ -> learnedWords(p) >= 200 },
        Badge("words500", "📚", "Dictionnaire vivant", "Apprends 500 mots.", false) { p, _, _, _ -> learnedWords(p) >= 500 },
        Badge("mastered50", "💎", "Mémoire d'éléphant", "Maîtrise 50 mots (intervalle de 3 semaines).", false) { p, _, _, _ -> masteredWords(p) >= 50 },
        Badge("boss1", "🗡️", "Tueur de boss", "Bats ton premier boss.", false) { p, _, _, _ -> p.bosses.isNotEmpty() },
        Badge("boss10", "🛡️", "Chevalier", "Bats 10 boss.", false) { p, _, _, _ -> p.bosses.size >= 10 },
        Badge("bossAll", "🏰", "Maître du donjon", "Bats tous les boss.", false) { p, _, u, _ -> u > 0 && p.bosses.size >= u },
        Badge("perfect", "💯", "Sans faute", "Termine une session sans aucune erreur.", false) { p, _, _, _ -> p.perfectSessions >= 1 },
        Badge("survival20", "❤️‍🔥", "Increvable", "20 bonnes réponses en mode survie.", false) { p, _, _, _ -> p.bestSurvival >= 20 },
        Badge("time25", "⏱️", "Éclair", "25 bonnes réponses au contre-la-montre.", false) { p, _, _, _ -> p.bestTimeAttack >= 25 },
        Badge("level10", "🎖️", "Niveau 10", "Atteins le niveau 10.", false) { p, _, _, _ -> Levels.level(p.totalXp) >= 10 },
        Badge("hours5", "⌛", "Bosseur", "Cumule 5 heures d'entraînement.", false) { p, _, _, _ -> p.totalSeconds >= 5 * 3600 },
        // --- succès cachés ---
        Badge("night", "🦉", "Oiseau de nuit", "Termine une session entre minuit et 5 h.", true) { _, h, _, _ -> h != null && h in 0..4 },
        Badge("early", "🐓", "Lève-tôt", "Termine une session entre 5 h et 7 h.", true) { _, h, _, _ -> h != null && h in 5..6 },
        Badge("phoenix", "🐦‍🔥", "Phénix", "Reconstruis une série de 3 jours après en avoir perdu une.", true) { p, _, _, _ -> p.lostStreak && p.streak >= 3 },
        Badge("freeze", "🧊", "Sauvé par la glace", "Un gel de série t'a sauvé.", true) { p, _, _, _ -> p.freezesUsed >= 1 },
        Badge("collector", "🗝️", "Collectionneur", "Ouvre 20 coffres.", true) { p, _, _, _ -> p.chests >= 20 },
        Badge("jackpot", "🎰", "Jackpot", "Ouvre un coffre épique.", true) { p, _, _, _ -> p.epicChests >= 1 },
        Badge("machine", "🤖", "Machine", "Réponds à 100 questions en une journée.", true) { p, _, _, t -> (p.days[t]?.answers ?: 0) >= 100 },
    )

    val byId: Map<String, Badge> = all.associateBy { it.id }

    /** Renvoie la progression mise à jour et la liste des badges débloqués à l'instant. */
    fun evaluate(p: Progress, hour: Int?, units: Int, today: Long, now: Long): Pair<Progress, List<Badge>> {
        val unlocked = all.filter { it.id !in p.badges && it.condition(p, hour, units, today) }
        if (unlocked.isEmpty()) return p to emptyList()
        return p.copy(badges = p.badges + unlocked.associate { it.id to now }) to unlocked
    }
}

// ------------------------------------------------------------------ coffres

enum class Rarity(val label: String, val emoji: String) {
    COMMON("Coffre commun", "🎁"),
    RARE("Coffre rare", "💼"),
    EPIC("Coffre épique", "👑"),
}

data class ChestReward(val rarity: Rarity, val xp: Int, val gems: Int, val freeze: Boolean)

object Chests {
    const val MAX_FREEZES = 2
    const val FREEZE_PRICE = 60

    fun roll(rnd: Random, atLeastRare: Boolean, freezes: Int): ChestReward {
        val r = rnd.nextInt(100)
        val rarity = when {
            r < 6 -> Rarity.EPIC
            r < 32 || atLeastRare -> Rarity.RARE
            else -> Rarity.COMMON
        }
        return when (rarity) {
            Rarity.COMMON ->
                if (rnd.nextBoolean()) ChestReward(rarity, xp = 10 + rnd.nextInt(21), gems = 0, freeze = false)
                else ChestReward(rarity, xp = 0, gems = 5 + rnd.nextInt(11), freeze = false)
            Rarity.RARE -> ChestReward(rarity, xp = 30 + rnd.nextInt(21), gems = 15 + rnd.nextInt(16), freeze = false)
            Rarity.EPIC -> {
                val canFreeze = freezes < MAX_FREEZES
                ChestReward(rarity, xp = 50, gems = if (canFreeze) 40 + rnd.nextInt(21) else 80, freeze = canFreeze)
            }
        }
    }
}
