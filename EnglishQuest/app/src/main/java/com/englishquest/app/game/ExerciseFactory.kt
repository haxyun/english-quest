package com.englishquest.app.game

import com.englishquest.app.data.Content
import com.englishquest.app.data.GrammarItem
import com.englishquest.app.data.GrammarLesson
import com.englishquest.app.data.Word
import com.englishquest.app.srs.CardState
import kotlin.random.Random

enum class WordEx { MCQ_EN_FR, MCQ_FR_EN, FILL, TYPE_FR_EN, TYPE_EN_FR, LISTEN_WORD, LISTEN_SENTENCE }

/** Fabrique les exercices à partir du contenu. */
class ExerciseFactory(
    private val c: Content,
    private val rnd: Random,
    private val ttsReady: Boolean,
) {
    private val punct = Regex("[.,!?;:]+$")

    // ------------------------------------------------------------ mots

    fun wordExercise(w: Word, card: CardState?, allowed: List<WordEx>? = null): Exercise {
        val seen = card?.seen ?: 0
        val reps = card?.reps ?: 0
        val candidates = mutableListOf<WordEx>()
        if (allowed != null) {
            candidates += allowed
        } else if (seen == 0) {
            candidates += WordEx.MCQ_EN_FR
        } else if (seen <= 2 || reps == 0) {
            candidates += listOf(WordEx.MCQ_FR_EN, WordEx.MCQ_FR_EN, WordEx.FILL, WordEx.MCQ_EN_FR, WordEx.LISTEN_WORD)
        } else {
            candidates += listOf(WordEx.TYPE_FR_EN, WordEx.TYPE_FR_EN, WordEx.TYPE_EN_FR, WordEx.FILL, WordEx.LISTEN_SENTENCE, WordEx.MCQ_FR_EN)
        }
        val usable = candidates.filter { isPossible(w, it) }.ifEmpty { listOf(WordEx.MCQ_FR_EN) }
        return buildWord(w, usable[rnd.nextInt(usable.size)], isNew = seen == 0)
    }

    private fun isPossible(w: Word, t: WordEx): Boolean = when (t) {
        WordEx.FILL -> blank(w) != null
        WordEx.LISTEN_WORD -> ttsReady
        WordEx.LISTEN_SENTENCE -> ttsReady && w.example.isNotBlank()
        else -> true
    }

    fun buildWord(w: Word, type: WordEx, isNew: Boolean = false): Exercise {
        val id = Content.wordCardId(w.id)
        val expl = wordExplanation(w)
        val sol = "${w.en} = ${w.fr}"
        return when (type) {
            WordEx.MCQ_EN_FR -> {
                val opts = optionsWith(w.fr, distractors(w, 3).map { it.fr })
                Exercise.Choice(
                    Uid.next(), id,
                    if (isNew) "Nouveau mot ! Que signifie…" else "Que signifie…",
                    w.en, opts.first, opts.second, speak = w.en, autoSpeak = true, hint = null,
                    explanation = expl, solution = sol, isNew = isNew,
                )
            }
            WordEx.MCQ_FR_EN -> {
                val opts = optionsWith(w.en, distractors(w, 3).map { it.en })
                Exercise.Choice(
                    Uid.next(), id, "Comment dit-on en anglais…", w.fr, opts.first, opts.second,
                    speak = null, autoSpeak = false, hint = null, explanation = expl, solution = sol,
                )
            }
            WordEx.FILL -> {
                val b = blank(w)
                if (b == null) buildWord(w, WordEx.MCQ_FR_EN) else {
                    val opts = optionsWith(b.second, distractors(w, 3).map { head(it.en) })
                    Exercise.Choice(
                        Uid.next(), id, "Complète la phrase", b.first, opts.first, opts.second,
                        speak = null, autoSpeak = false, hint = "(${w.fr})",
                        explanation = expl, solution = w.example,
                    )
                }
            }
            WordEx.TYPE_FR_EN -> {
                val h = head(w.en)
                Exercise.Typing(
                    Uid.next(), id, "Traduis en anglais", w.fr,
                    accepted = AnswerCheck.variants(w.en, english = true),
                    speak = null, autoSpeak = false, sentence = false, answerInEnglish = true,
                    hint = "Commence par « ${h.take(1)}… » (${h.length} lettres)",
                    explanation = expl, solution = w.en,
                )
            }
            WordEx.TYPE_EN_FR -> Exercise.Typing(
                Uid.next(), id, "Traduis en français", w.en,
                accepted = AnswerCheck.variants(w.fr, english = false),
                speak = w.en, autoSpeak = true, sentence = false, answerInEnglish = false, hint = null,
                explanation = expl, solution = w.fr,
            )
            WordEx.LISTEN_WORD -> Exercise.Typing(
                Uid.next(), id, "Écoute et écris le mot anglais 🎧", "",
                accepted = AnswerCheck.variants(w.en, english = true),
                speak = head(w.en), autoSpeak = true, sentence = false, answerInEnglish = true,
                hint = "Indice : ${w.fr}", explanation = expl, solution = w.en,
            )
            WordEx.LISTEN_SENTENCE -> Exercise.Typing(
                Uid.next(), id, "Dictée : écris la phrase entendue 🎧", "",
                accepted = listOf(w.example),
                speak = w.example, autoSpeak = true, sentence = true, answerInEnglish = true,
                hint = "Elle contient le mot « ${w.en} » (${w.fr})", explanation = expl, solution = w.example,
            )
        }
    }

    fun match(words: List<Word>): Exercise.Match = Exercise.Match(
        Uid.next(),
        words.distinctBy { it.fr }.map { MatchPair(Content.wordCardId(it.id), it.en, it.fr) },
    )

    private fun wordExplanation(w: Word): String {
        val sb = StringBuilder("« ${w.en} » signifie « ${w.fr} ».")
        if (w.note.isNotBlank()) sb.append("\n💡 ").append(w.note)
        if (w.example.isNotBlank()) sb.append("\nExemple : ").append(w.example)
        return sb.toString()
    }

    /** Mot principal sans « to / a / the » ni parenthèses. */
    fun head(en: String): String {
        val first = en.split("/").first().replace(Regex("\\(.*?\\)"), "").trim()
        return AnswerCheck.stripLeading(first, english = true)
    }

    /** Phrase d'exemple avec un trou à la place du mot, ou null si le mot n'apparaît pas tel quel. */
    fun blank(w: Word): Pair<String, String>? {
        val h = head(w.en)
        if (h.isBlank() || w.example.isBlank()) return null
        val regex = Regex("\\b" + Regex.escape(h) + "(s|es)?\\b", RegexOption.IGNORE_CASE)
        val m = regex.find(w.example) ?: return null
        return w.example.replaceRange(m.range, "_____") to m.value
    }

    private fun distractors(w: Word, n: Int): List<Word> {
        val sameTheme = c.words.filter { it.theme == w.theme && it.id != w.id && it.fr != w.fr && it.en != w.en }.shuffled(rnd)
        val result = sameTheme.take(n).toMutableList()
        if (result.size < n) {
            result += c.words.filter { it.id != w.id && it !in result && it.fr != w.fr }.shuffled(rnd).take(n - result.size)
        }
        return result
    }

    /** Mélange la bonne réponse avec des distracteurs ; renvoie (options, index de la bonne réponse). */
    private fun optionsWith(correct: String, others: List<String>): Pair<List<String>, Int> {
        val clean = others.filter { !it.equals(correct, ignoreCase = true) }.distinct().take(3)
        val all = (clean + correct).shuffled(rnd)
        return all to all.indexOf(correct)
    }

    // ------------------------------------------------------------ grammaire

    fun grammarExercise(g: GrammarLesson, index: Int): Exercise {
        val id = Content.grammarCardId(g.id, index)
        return when (val item = g.items[index]) {
            is GrammarItem.Mcq -> {
                val order = item.options.indices.shuffled(rnd)
                Exercise.Choice(
                    Uid.next(), id, "Choisis la bonne réponse", item.prompt,
                    order.map { item.options[it] }, order.indexOf(item.answer),
                    speak = null, autoSpeak = false, hint = null,
                    explanation = item.explain,
                    solution = item.prompt.replace("___", item.options[item.answer]),
                )
            }
            is GrammarItem.Fill -> Exercise.Typing(
                Uid.next(), id, "Écris le mot manquant", item.prompt,
                accepted = item.answers, speak = null, autoSpeak = false,
                sentence = false, answerInEnglish = true, hint = null,
                explanation = item.explain,
                solution = item.prompt.replace("___", item.answers.first()),
            )
            is GrammarItem.Order -> orderExercise(g, id, item)
            is GrammarItem.SpotError -> Exercise.SpotError(
                Uid.next(), id, "Touche le mot qui contient une erreur",
                item.sentence.split(" "), item.wrong,
                explanation = item.explain, solution = "Correction : ${item.correction}",
            )
        }
    }

    private fun tokens(s: String): List<String> =
        s.split(" ").map { it.replace(punct, "") }.filter { it.isNotBlank() }

    private fun orderExercise(g: GrammarLesson, id: String, item: GrammarItem.Order): Exercise {
        val toEnglish = rnd.nextInt(100) < 60 || item.fr.isBlank()
        return if (toEnglish) {
            val answer = tokens(item.en)
            val pool = c.grammar.flatMap { it.items }.filterIsInstance<GrammarItem.Order>()
                .flatMap { tokens(it.en) }.filter { t -> answer.none { it.equals(t, true) } }.distinct()
            val extra = pool.shuffled(rnd).take(2)
            Exercise.Order(
                Uid.next(), id,
                if (item.fr.isBlank()) "Remets les mots dans l'ordre" else "Traduis en anglais",
                item.fr, answer, (answer + extra).shuffled(rnd), speak = null,
                explanation = item.explain, solution = item.en,
            )
        } else {
            val answer = tokens(item.fr)
            val pool = c.grammar.flatMap { it.items }.filterIsInstance<GrammarItem.Order>()
                .flatMap { tokens(it.fr) }.filter { t -> answer.none { it.equals(t, true) } }.distinct()
            val extra = pool.shuffled(rnd).take(2)
            Exercise.Order(
                Uid.next(), id, "Traduis en français", item.en, answer, (answer + extra).shuffled(rnd),
                speak = item.en, explanation = item.explain, solution = item.fr,
            )
        }
    }

    /** Variante rapide (contre-la-montre) : seulement des QCM et des « repère l'erreur ». */
    fun quickGrammarExercise(g: GrammarLesson, index: Int): Exercise? = when (g.items[index]) {
        is GrammarItem.Mcq, is GrammarItem.SpotError -> grammarExercise(g, index)
        else -> null
    }
}
