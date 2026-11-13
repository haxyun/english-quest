package com.englishquest.app.data

import com.englishquest.app.srs.CardState
import org.json.JSONArray
import org.json.JSONObject

data class DayStats(
    val xp: Int = 0,
    val seconds: Int = 0,
    val answers: Int = 0,
    val correct: Int = 0,
    val sessions: Int = 0,
)

data class Settings(
    val dailyGoalMin: Int = 10,
    val reminderOn: Boolean = true,
    val reminderHour: Int = 18,
    val reminderMinute: Int = 0,
    val themeMode: Int = 0, // 0 = système, 1 = clair, 2 = sombre
    val sound: Boolean = true,
    val haptics: Boolean = true,
    val ttsRate: Float = 0.9f,
)

data class Progress(
    val onboarded: Boolean = false,
    val totalXp: Int = 0,
    val gems: Int = 20,
    val streak: Int = 0,
    val bestStreak: Int = 0,
    val lastActiveDay: Long = -1,
    val freezes: Int = 1,
    val cards: Map<String, CardState> = emptyMap(),
    val lessonsDone: Map<String, Int> = emptyMap(), // id d'unité -> nb de leçons terminées
    val bosses: Set<String> = emptySet(),           // unités dont le boss est vaincu
    val badges: Map<String, Long> = emptyMap(),     // id de badge -> date de déblocage
    val days: Map<Long, DayStats> = emptyMap(),     // epoch day -> statistiques
    val totalAnswers: Int = 0,
    val totalCorrect: Int = 0,
    val totalSeconds: Int = 0,
    val sessions: Int = 0,
    val bestCombo: Int = 0,
    val chests: Int = 0,
    val epicChests: Int = 0,
    val bestSurvival: Int = 0,
    val bestTimeAttack: Int = 0,
    val perfectSessions: Int = 0,
    val lostStreak: Boolean = false,
    val freezesUsed: Int = 0,
    val settings: Settings = Settings(),
)

/** Conversion Progress <-> JSON (avec org.json, inclus dans Android). */
object ProgressJson {

    fun toJson(p: Progress): String {
        val o = JSONObject()
        o.put("v", 1)
        o.put("onboarded", p.onboarded)
        o.put("totalXp", p.totalXp)
        o.put("gems", p.gems)
        o.put("streak", p.streak)
        o.put("bestStreak", p.bestStreak)
        o.put("lastActiveDay", p.lastActiveDay)
        o.put("freezes", p.freezes)
        o.put("totalAnswers", p.totalAnswers)
        o.put("totalCorrect", p.totalCorrect)
        o.put("totalSeconds", p.totalSeconds)
        o.put("sessions", p.sessions)
        o.put("bestCombo", p.bestCombo)
        o.put("chests", p.chests)
        o.put("epicChests", p.epicChests)
        o.put("bestSurvival", p.bestSurvival)
        o.put("bestTimeAttack", p.bestTimeAttack)
        o.put("perfectSessions", p.perfectSessions)
        o.put("lostStreak", p.lostStreak)
        o.put("freezesUsed", p.freezesUsed)

        val cards = JSONObject()
        for ((id, c) in p.cards) {
            val a = JSONArray()
            a.put(c.ef); a.put(c.reps); a.put(c.interval); a.put(c.due)
            a.put(c.last); a.put(c.seen); a.put(c.correct); a.put(c.lapses)
            cards.put(id, a)
        }
        o.put("cards", cards)

        val lessons = JSONObject()
        for ((k, v) in p.lessonsDone) lessons.put(k, v)
        o.put("lessonsDone", lessons)

        o.put("bosses", JSONArray(p.bosses.toList()))

        val badges = JSONObject()
        for ((k, v) in p.badges) badges.put(k, v)
        o.put("badges", badges)

        val days = JSONObject()
        for ((d, s) in p.days) {
            val a = JSONArray()
            a.put(s.xp); a.put(s.seconds); a.put(s.answers); a.put(s.correct); a.put(s.sessions)
            days.put(d.toString(), a)
        }
        o.put("days", days)

        val s = p.settings
        val so = JSONObject()
        so.put("dailyGoalMin", s.dailyGoalMin)
        so.put("reminderOn", s.reminderOn)
        so.put("reminderHour", s.reminderHour)
        so.put("reminderMinute", s.reminderMinute)
        so.put("themeMode", s.themeMode)
        so.put("sound", s.sound)
        so.put("haptics", s.haptics)
        so.put("ttsRate", s.ttsRate.toDouble())
        o.put("settings", so)
        return o.toString()
    }

    fun fromJson(text: String): Progress {
        val o = JSONObject(text)
        val d = Progress()

        val cards = HashMap<String, CardState>()
        o.optJSONObject("cards")?.let { co ->
            val keys = co.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                val a = co.optJSONArray(k) ?: continue
                cards[k] = CardState(
                    ef = a.optDouble(0, 2.5),
                    reps = a.optInt(1),
                    interval = a.optInt(2),
                    due = a.optLong(3),
                    last = a.optLong(4, -1),
                    seen = a.optInt(5),
                    correct = a.optInt(6),
                    lapses = a.optInt(7),
                )
            }
        }

        val lessons = HashMap<String, Int>()
        o.optJSONObject("lessonsDone")?.let { lo ->
            val keys = lo.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                lessons[k] = lo.optInt(k)
            }
        }

        val bosses = HashSet<String>()
        o.optJSONArray("bosses")?.let { ba -> for (i in 0 until ba.length()) bosses.add(ba.optString(i)) }

        val badges = HashMap<String, Long>()
        o.optJSONObject("badges")?.let { bo ->
            val keys = bo.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                badges[k] = bo.optLong(k)
            }
        }

        val days = HashMap<Long, DayStats>()
        o.optJSONObject("days")?.let { dobj ->
            val keys = dobj.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                val day = k.toLongOrNull() ?: continue
                val a = dobj.optJSONArray(k) ?: continue
                days[day] = DayStats(a.optInt(0), a.optInt(1), a.optInt(2), a.optInt(3), a.optInt(4))
            }
        }

        val so = o.optJSONObject("settings")
        val ds = Settings()
        val settings = if (so == null) ds else Settings(
            dailyGoalMin = so.optInt("dailyGoalMin", ds.dailyGoalMin),
            reminderOn = so.optBoolean("reminderOn", ds.reminderOn),
            reminderHour = so.optInt("reminderHour", ds.reminderHour),
            reminderMinute = so.optInt("reminderMinute", ds.reminderMinute),
            themeMode = so.optInt("themeMode", ds.themeMode),
            sound = so.optBoolean("sound", ds.sound),
            haptics = so.optBoolean("haptics", ds.haptics),
            ttsRate = so.optDouble("ttsRate", ds.ttsRate.toDouble()).toFloat(),
        )

        return Progress(
            onboarded = o.optBoolean("onboarded", d.onboarded),
            totalXp = o.optInt("totalXp", d.totalXp),
            gems = o.optInt("gems", d.gems),
            streak = o.optInt("streak", d.streak),
            bestStreak = o.optInt("bestStreak", d.bestStreak),
            lastActiveDay = o.optLong("lastActiveDay", d.lastActiveDay),
            freezes = o.optInt("freezes", d.freezes),
            cards = cards,
            lessonsDone = lessons,
            bosses = bosses,
            badges = badges,
            days = days,
            totalAnswers = o.optInt("totalAnswers"),
            totalCorrect = o.optInt("totalCorrect"),
            totalSeconds = o.optInt("totalSeconds"),
            sessions = o.optInt("sessions"),
            bestCombo = o.optInt("bestCombo"),
            chests = o.optInt("chests"),
            epicChests = o.optInt("epicChests"),
            bestSurvival = o.optInt("bestSurvival"),
            bestTimeAttack = o.optInt("bestTimeAttack"),
            perfectSessions = o.optInt("perfectSessions"),
            lostStreak = o.optBoolean("lostStreak"),
            freezesUsed = o.optInt("freezesUsed"),
            settings = settings,
        )
    }
}
