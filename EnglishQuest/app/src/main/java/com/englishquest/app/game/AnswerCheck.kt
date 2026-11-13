package com.englishquest.app.game

import java.text.Normalizer
import kotlin.math.max
import kotlin.math.min

/** Vérification tolérante des réponses tapées au clavier. */
object AnswerCheck {

    enum class Result { EXACT, TYPO, WRONG }

    private val marks = Regex("\\p{Mn}+")
    private val nonWord = Regex("[^a-z0-9' ]")
    private val spaces = Regex("\\s+")
    private val parens = Regex("\\(.*?\\)")

    fun normalize(input: String): String {
        var t = input.lowercase()
            .replace('’', '\'').replace('‘', '\'').replace('`', '\'')
            .replace("œ", "oe").replace("æ", "ae")
        t = Normalizer.normalize(t, Normalizer.Form.NFD).replace(marks, "")
        t = t.replace("won't", "will not")
            .replace("can't", "can not")
            .replace("cannot", "can not")
            .replace("n't", " not")
            .replace("'m", " am")
            .replace("'re", " are")
            .replace("'ve", " have")
            .replace("'ll", " will")
        t = t.replace(nonWord, " ").replace(spaces, " ").trim()
        return t
    }

    private val enPrefixes = listOf("to ", "a ", "an ", "the ")
    private val frPrefixes = listOf("un ", "une ", "le ", "la ", "les ", "l'", "des ", "du ", "se ", "s'")

    /** Retire « to », « a », « the »… en tête (pour accepter « belong » comme « to belong »). */
    fun stripLeading(s: String, english: Boolean): String {
        var t = s.trim()
        val list = if (english) enPrefixes else frPrefixes
        var changed = true
        while (changed) {
            changed = false
            for (p in list) {
                if (t.lowercase().startsWith(p) && t.length > p.length + 1) {
                    t = t.substring(p.length).trim()
                    changed = true
                }
            }
        }
        return t
    }

    /** « origines, milieu » -> [origines, milieu, origines, milieu, ...] avec variantes sans article. */
    fun variants(raw: String, english: Boolean): List<String> {
        val out = LinkedHashSet<String>()
        out.add(raw)
        val noParens = raw.replace(parens, " ").replace(spaces, " ").trim()
        out.add(noParens)
        val parts = raw.split('/', ',', ';').map { it.trim() }.filter { it.isNotEmpty() }
        for (p in parts) {
            val np = p.replace(parens, " ").replace(spaces, " ").trim()
            if (np.isNotEmpty()) {
                out.add(np)
                out.add(stripLeading(np, english))
            }
        }
        out.add(stripLeading(noParens, english))
        return out.filter { it.isNotBlank() }
    }

    fun check(input: String, accepted: List<String>, sentence: Boolean): Result {
        val n = normalize(input)
        if (n.isEmpty()) return Result.WRONG
        val targets = accepted.map { normalize(it) }.filter { it.isNotEmpty() }
        if (targets.any { it == n }) return Result.EXACT
        // Petite faute de frappe tolérée
        var best = Int.MAX_VALUE
        var bestLen = 0
        for (t in targets) {
            val d = levenshtein(n, t)
            if (d < best) {
                best = d
                bestLen = t.length
            }
        }
        val tolerance = if (sentence) max(1, bestLen / 12) else if (bestLen >= 5) 1 else 0
        return if (best <= tolerance) Result.TYPO else Result.WRONG
    }

    fun sameSentence(a: String, b: String): Boolean = normalize(a) == normalize(b)

    fun levenshtein(a: String, b: String): Int {
        if (a == b) return 0
        if (a.isEmpty()) return b.length
        if (b.isEmpty()) return a.length
        var prev = IntArray(b.length + 1) { it }
        var cur = IntArray(b.length + 1)
        for (i in 1..a.length) {
            cur[0] = i
            for (j in 1..b.length) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                cur[j] = min(min(cur[j - 1] + 1, prev[j] + 1), prev[j - 1] + cost)
            }
            val tmp = prev
            prev = cur
            cur = tmp
        }
        return prev[b.length]
    }
}
