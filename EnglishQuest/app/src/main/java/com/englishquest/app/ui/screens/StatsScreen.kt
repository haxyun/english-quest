package com.englishquest.app.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.englishquest.app.data.Content
import com.englishquest.app.game.Badges
import com.englishquest.app.game.Levels
import com.englishquest.app.srs.Sm2
import com.englishquest.app.ui.AppViewModel
import com.englishquest.app.ui.components.GameCard
import com.englishquest.app.ui.components.SectionTitle
import com.englishquest.app.ui.theme.GameColors
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun StatsScreen(vm: AppViewModel) {
    val c = vm.content ?: return
    val p by vm.progress.collectAsState()
    val today = vm.today()

    val seenWords = p.cards.count { it.key.startsWith("w:") && it.value.seen > 0 }
    val learned = Badges.learnedWords(p)
    val mastered = Badges.masteredWords(p)
    val accuracy = if (p.totalAnswers > 0) p.totalCorrect * 100 / p.totalAnswers else 0
    val hours = p.totalSeconds / 3600
    val minutes = (p.totalSeconds % 3600) / 60

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SectionTitle("Tes statistiques")
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile("📖", "$seenWords", "mots vus", Modifier.weight(1f))
            StatTile("✅", "$learned", "mots appris", Modifier.weight(1f))
            StatTile("💎", "$mastered", "maîtrisés", Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile("🎯", "$accuracy %", "précision", Modifier.weight(1f))
            StatTile("⏱️", if (hours > 0) "${hours}h${minutes.toString().padStart(2, '0')}" else "$minutes min", "temps total", Modifier.weight(1f))
            StatTile("🎮", "${p.sessions}", "sessions", Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile("🔥", "${p.bestStreak}", "meilleure série", Modifier.weight(1f))
            StatTile("⚡", "${p.bestCombo}", "meilleur combo", Modifier.weight(1f))
            StatTile("⭐", "${Levels.level(p.totalXp)}", "niveau", Modifier.weight(1f))
        }

        // 7 derniers jours
        val last7 = (6 downTo 0).map { today - it }
        GameCard {
            Column {
                Text("XP des 7 derniers jours", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                BarChart(
                    values = last7.map { (p.days[it]?.xp ?: 0).toFloat() },
                    labels = last7.map {
                        LocalDate.ofEpochDay(it).dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.FRANCE).take(3)
                    },
                    highlight = last7.size - 1,
                    color = MaterialTheme.colorScheme.primary,
                    format = { it.toInt().toString() },
                )
            }
        }

        // 8 dernières semaines
        val todayDate = LocalDate.ofEpochDay(today)
        val monday = todayDate.minusDays((todayDate.dayOfWeek.value - 1).toLong()).toEpochDay()
        val weeks = (7 downTo 0).map { monday - it * 7L }
        GameCard {
            Column {
                Text("Minutes par semaine", style = MaterialTheme.typography.titleMedium)
                Text("Les 8 dernières semaines (la dernière barre = cette semaine)", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                Spacer(Modifier.height(8.dp))
                BarChart(
                    values = weeks.map { start -> (0 until 7).sumOf { p.days[start + it]?.seconds ?: 0 } / 60f },
                    labels = weeks.map { start -> LocalDate.ofEpochDay(start).let { "${it.dayOfMonth}/${it.monthValue}" } },
                    highlight = weeks.size - 1,
                    color = GameColors.Fire,
                    format = { it.toInt().toString() },
                )
                val thisWeek = weeks.last()
                val prevWeek = weeks[weeks.size - 2]
                val wNow = (0 until 7).sumOf { p.days[thisWeek + it]?.xp ?: 0 }
                val wPrev = (0 until 7).sumOf { p.days[prevWeek + it]?.xp ?: 0 }
                Spacer(Modifier.height(8.dp))
                Text(
                    when {
                        wPrev == 0 && wNow == 0 -> "Lance ta première semaine d'entraînement ! 💪"
                        wNow >= wPrev -> "📈 Cette semaine : $wNow XP (contre $wPrev la semaine dernière). Bravo !"
                        else -> "📉 Cette semaine : $wNow XP (contre $wPrev). Encore un effort !"
                    },
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }

        // Progression par thème
        GameCard {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Vocabulaire par thème", style = MaterialTheme.typography.titleMedium)
                c.themes.forEach { t ->
                    val ids = c.words.filter { it.theme == t.id }.map { Content.wordCardId(it.id) }
                    val ok = ids.count { Sm2.isLearned(p.cards[it]) }
                    val ratio = if (ids.isEmpty()) 0f else ok.toFloat() / ids.size
                    Column {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("${t.emoji} ${t.name}", modifier = Modifier.weight(1f))
                            Text("$ok / ${ids.size}", fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { ratio },
                            modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                            color = GameColors.Correct,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            strokeCap = StrokeCap.Round,
                        )
                    }
                }
            }
        }

        // Grammaire
        GameCard {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Grammaire", style = MaterialTheme.typography.titleMedium)
                val gIds = c.grammar.flatMap { g -> g.items.indices.map { Content.grammarCardId(g.id, it) } }
                val gOk = gIds.count { Sm2.isLearned(p.cards[it]) }
                Text("$gOk / ${gIds.size} exercices de grammaire réussis à la dernière tentative.")
                Text("🏰 Boss vaincus : ${p.bosses.size} / ${c.units.size}")
                Text("🎁 Coffres ouverts : ${p.chests}")
                Text("⏱️ Record contre-la-montre : ${p.bestTimeAttack} · ❤️ Record survie : ${p.bestSurvival}")
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun StatTile(emoji: String, value: String, label: String, modifier: Modifier = Modifier) {
    Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surface, tonalElevation = 1.dp, shadowElevation = 1.dp, modifier = modifier) {
        Column(Modifier.padding(vertical = 12.dp, horizontal = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(emoji, fontSize = 22.sp)
            Text(value, style = MaterialTheme.typography.titleLarge, maxLines = 1)
            Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, maxLines = 1)
        }
    }
}

/** Histogramme simple, animé. */
@Composable
private fun BarChart(
    values: List<Float>,
    labels: List<String>,
    highlight: Int,
    color: Color,
    format: (Float) -> String,
) {
    val max = (values.maxOrNull() ?: 0f).coerceAtLeast(1f)
    Row(
        Modifier.fillMaxWidth().height(170.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        values.forEachIndexed { i, v ->
            val target = (v / max).coerceIn(0f, 1f)
            val anim by animateFloatAsState(targetValue = target, animationSpec = tween(700), label = "bar$i")
            Column(
                Modifier.weight(1f).fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
            ) {
                Text(if (v > 0f) format(v) else "", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height((110f * anim).coerceAtLeast(4f).dp)
                        .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                        .background(if (i == highlight) color else color.copy(alpha = 0.45f))
                )
                Spacer(Modifier.height(4.dp))
                Text(labels.getOrElse(i) { "" }, fontSize = 10.sp, maxLines = 1, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
