package com.englishquest.app.game

/** Identifiant unique de chaque exercice affiché (sert aux animations). */
object Uid {
    private var n = 0
    @Synchronized
    fun next(): Int = ++n
}

data class MatchPair(val cardId: String, val left: String, val right: String)

/** Tous les types d'exercices de l'application. */
sealed class Exercise {
    abstract val uid: Int
    abstract val instruction: String
    abstract val explanation: String
    abstract val solution: String
    abstract val isNew: Boolean
    abstract val cardIds: List<String>

    /** QCM (vocabulaire, texte à trous, grammaire). */
    data class Choice(
        override val uid: Int,
        val cardId: String,
        override val instruction: String,
        val prompt: String,
        val options: List<String>,
        val correct: Int,
        val speak: String?,
        val autoSpeak: Boolean,
        val hint: String?,
        override val explanation: String,
        override val solution: String,
        override val isNew: Boolean = false,
    ) : Exercise() {
        override val cardIds: List<String> get() = listOf(cardId)
    }

    /** Réponse tapée : traduction FR→EN / EN→FR, dictée, texte à trous. */
    data class Typing(
        override val uid: Int,
        val cardId: String,
        override val instruction: String,
        val prompt: String,
        val accepted: List<String>,
        val speak: String?,
        val autoSpeak: Boolean,
        val sentence: Boolean,
        val answerInEnglish: Boolean,
        val hint: String?,
        override val explanation: String,
        override val solution: String,
        override val isNew: Boolean = false,
    ) : Exercise() {
        override val cardIds: List<String> get() = listOf(cardId)
    }

    /** Phrase à remettre dans l'ordre (ou traduction avec banque de mots). */
    data class Order(
        override val uid: Int,
        val cardId: String,
        override val instruction: String,
        val prompt: String,
        val answer: List<String>,
        val bank: List<String>,
        val speak: String?,
        override val explanation: String,
        override val solution: String,
        override val isNew: Boolean = false,
    ) : Exercise() {
        override val cardIds: List<String> get() = listOf(cardId)
    }

    /** Repérer le mot faux dans une phrase. */
    data class SpotError(
        override val uid: Int,
        val cardId: String,
        override val instruction: String,
        val tokens: List<String>,
        val wrong: Int,
        override val explanation: String,
        override val solution: String,
        override val isNew: Boolean = false,
    ) : Exercise() {
        override val cardIds: List<String> get() = listOf(cardId)
    }

    /** Associer des paires anglais / français. */
    data class Match(
        override val uid: Int,
        val pairs: List<MatchPair>,
        override val instruction: String = "Associe chaque mot à sa traduction",
        override val explanation: String = "",
        override val solution: String = "",
        override val isNew: Boolean = false,
    ) : Exercise() {
        override val cardIds: List<String> get() = pairs.map { it.cardId }
    }
}

/** Réponse donnée par l'utilisateur. */
sealed class Answer {
    data class Index(val i: Int) : Answer()
    data class Text(val s: String) : Answer()
    data class Tokens(val t: List<String>) : Answer()
    data class MatchDone(val failed: Set<String>) : Answer()
}

/** Copie d'un exercice avec un nouvel identifiant (pour le reposer plus tard). */
fun Exercise.renewed(): Exercise = when (this) {
    is Exercise.Choice -> copy(uid = Uid.next())
    is Exercise.Typing -> copy(uid = Uid.next())
    is Exercise.Order -> copy(uid = Uid.next(), bank = bank.shuffled())
    is Exercise.SpotError -> copy(uid = Uid.next())
    is Exercise.Match -> copy(uid = Uid.next())
}
