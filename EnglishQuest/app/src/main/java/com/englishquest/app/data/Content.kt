package com.englishquest.app.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/*
 * Contenu pédagogique, lu depuis app/src/main/assets/content.json.
 * Pour ajouter des mots ou des exercices, il suffit de modifier ce fichier JSON.
 */

data class ThemeInfo(val id: String, val name: String, val emoji: String)

data class Word(
    val id: String,
    val en: String,
    val fr: String,
    val theme: String,
    val example: String,
    val note: String,
)

data class Rule(val title: String, val text: String, val examples: List<String>)

sealed class GrammarItem {
    abstract val explain: String

    data class Mcq(val prompt: String, val options: List<String>, val answer: Int, override val explain: String) : GrammarItem()
    data class Fill(val prompt: String, val answers: List<String>, override val explain: String) : GrammarItem()
    data class Order(val en: String, val fr: String, override val explain: String) : GrammarItem()
    data class SpotError(val sentence: String, val wrong: Int, val correction: String, override val explain: String) : GrammarItem()
}

data class GrammarLesson(
    val id: String,
    val title: String,
    val emoji: String,
    val summary: String,
    val rules: List<Rule>,
    val items: List<GrammarItem>,
)

data class UnitInfo(
    val id: String,
    val title: String,
    val emoji: String,
    val theme: String,
    val grammar: String,
    val bossName: String,
    val bossEmoji: String,
    val wordIds: List<String>,
)

class Content(
    val themes: List<ThemeInfo>,
    val units: List<UnitInfo>,
    val grammar: List<GrammarLesson>,
    val words: List<Word>,
) {
    val wordById: Map<String, Word> = words.associateBy { it.id }
    val grammarById: Map<String, GrammarLesson> = grammar.associateBy { it.id }
    val themeById: Map<String, ThemeInfo> = themes.associateBy { it.id }

    fun unitWords(u: UnitInfo): List<Word> = u.wordIds.mapNotNull { wordById[it] }

    /** Mots introduits par la leçon [lesson] (0..LESSONS_PER_UNIT-1) d'une unité. */
    fun lessonWords(u: UnitInfo, lesson: Int): List<Word> {
        val all = unitWords(u)
        if (all.isEmpty()) return all
        val from = all.size * lesson / LESSONS_PER_UNIT
        val to = all.size * (lesson + 1) / LESSONS_PER_UNIT
        return all.subList(from, to)
    }

    /** Toutes les cartes (mots + exercices de grammaire) d'une unité. */
    fun unitCardIds(u: UnitInfo): List<String> {
        val w = u.wordIds.map { wordCardId(it) }
        val g = grammarById[u.grammar]?.items?.indices?.map { grammarCardId(u.grammar, it) } ?: emptyList()
        return w + g
    }

    companion object {
        const val LESSONS_PER_UNIT = 4
        fun wordCardId(wordId: String) = "w:$wordId"
        fun grammarCardId(lessonId: String, index: Int) = "g:$lessonId:$index"
    }
}

object ContentLoader {

    fun load(context: Context): Content {
        val text = context.assets.open("content.json").bufferedReader(Charsets.UTF_8).use { it.readText() }
        return parse(text)
    }

    fun parse(text: String): Content {
        val root = JSONObject(text)

        val themes = root.optJSONArray("themes").objects().map {
            ThemeInfo(it.optString("id"), it.optString("name"), it.optString("emoji"))
        }

        val words = root.optJSONArray("words").objects().mapNotNull {
            val id = it.optString("id")
            val en = it.optString("en")
            val fr = it.optString("fr")
            if (id.isBlank() || en.isBlank() || fr.isBlank()) null
            else Word(id, en, fr, it.optString("theme"), it.optString("example"), it.optString("note"))
        }

        val grammar = root.optJSONArray("grammar").objects().map { g ->
            val rules = g.optJSONArray("rules").objects().map { r ->
                Rule(r.optString("title"), r.optString("text"), r.optJSONArray("examples").strings())
            }
            val items = g.optJSONArray("items").objects().mapNotNull { parseItem(it) }
            GrammarLesson(g.optString("id"), g.optString("title"), g.optString("emoji", "📘"), g.optString("summary"), rules, items)
        }

        // Les mots d'un thème sont répartis équitablement entre les unités qui partagent ce thème.
        val rawUnits = root.optJSONArray("units").objects()
        val byTheme = rawUnits.groupBy { it.optString("theme") }
        val assigned = HashMap<String, List<String>>()
        for ((theme, list) in byTheme) {
            val ids = words.filter { it.theme == theme }.map { it.id }
            val n = list.size
            list.forEachIndexed { i, u ->
                val from = ids.size * i / n
                val to = ids.size * (i + 1) / n
                assigned[u.optString("id")] = ids.subList(from, to)
            }
        }
        val units = rawUnits.map {
            val id = it.optString("id")
            UnitInfo(
                id = id,
                title = it.optString("title"),
                emoji = it.optString("emoji", "⭐"),
                theme = it.optString("theme"),
                grammar = it.optString("grammar"),
                bossName = it.optString("bossName", "Le Boss"),
                bossEmoji = it.optString("bossEmoji", "👹"),
                wordIds = assigned[id] ?: emptyList(),
            )
        }
        return Content(themes, units, grammar, words)
    }

    private fun parseItem(o: JSONObject): GrammarItem? = try {
        val explain = o.optString("explain")
        when (o.optString("type")) {
            "mcq" -> {
                val opts = o.optJSONArray("options").strings()
                val ans = o.optInt("answer", 0)
                if (opts.size >= 2 && ans in opts.indices) GrammarItem.Mcq(o.optString("prompt"), opts, ans, explain) else null
            }
            "fill" -> {
                val answers = o.optJSONArray("answers").strings()
                if (answers.isNotEmpty()) GrammarItem.Fill(o.optString("prompt"), answers, explain) else null
            }
            "order" -> GrammarItem.Order(o.optString("en"), o.optString("fr"), explain)
            "error" -> {
                val sentence = o.optString("sentence")
                val wrong = o.optInt("wrong", -1)
                if (wrong in sentence.split(" ").indices) GrammarItem.SpotError(sentence, wrong, o.optString("correction"), explain) else null
            }
            else -> null
        }
    } catch (e: Exception) {
        null
    }

    private fun JSONArray?.objects(): List<JSONObject> {
        if (this == null) return emptyList()
        val out = ArrayList<JSONObject>(length())
        for (i in 0 until length()) optJSONObject(i)?.let { out.add(it) }
        return out
    }

    private fun JSONArray?.strings(): List<String> {
        if (this == null) return emptyList()
        val out = ArrayList<String>(length())
        for (i in 0 until length()) out.add(optString(i))
        return out
    }
}
