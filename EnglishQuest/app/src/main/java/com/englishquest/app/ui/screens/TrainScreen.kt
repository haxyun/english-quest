package com.englishquest.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.englishquest.app.game.Chests
import com.englishquest.app.ui.AppViewModel
import com.englishquest.app.ui.Screen
import com.englishquest.app.ui.components.BigButton
import com.englishquest.app.ui.components.EmojiBadge
import com.englishquest.app.ui.components.GameCard
import com.englishquest.app.ui.components.SectionTitle
import com.englishquest.app.ui.theme.GameColors

@Composable
fun TrainScreen(vm: AppViewModel) {
    val c = vm.content ?: return
    val p by vm.progress.collectAsState()
    val due = vm.dueCount(p)
    val unlocked = vm.unlockedUnitCount(p)

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        SectionTitle("Entraînement libre")
        ModeCard(
            emoji = "🧠", color = MaterialTheme.colorScheme.primary,
            title = "Révisions intelligentes",
            text = if (due > 0) "$due carte(s) à revoir aujourd'hui. L'algorithme fait revenir ce que tu oublies."
            else "Rien d'urgent ! Une séance renforce quand même tes points faibles.",
            button = "Réviser",
        ) { vm.startReview() }
        ModeCard(
            emoji = "⏱️", color = GameColors.Fire,
            title = "Contre-la-montre",
            text = "60 secondes, un maximum de bonnes réponses. Record : ${p.bestTimeAttack}",
            button = "Lancer le chrono",
        ) { vm.startTimeAttack() }
        ModeCard(
            emoji = "❤️", color = GameColors.Heart,
            title = "Mode survie",
            text = "3 vies, des questions sans fin. Jusqu'où iras-tu ? Record : ${p.bestSurvival}",
            button = "Survivre",
        ) { vm.startSurvival() }

        SectionTitle("Boutique")
        GameCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                EmojiBadge("🧊", GameColors.Ice, 56)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Gel de série", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Protège ta série si tu oublies un jour. Tu en as ${p.freezes}/${Chests.MAX_FREEZES}.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                    val canBuy = p.gems >= Chests.FREEZE_PRICE && p.freezes < Chests.MAX_FREEZES
                    BigButton(
                        text = when {
                            p.freezes >= Chests.MAX_FREEZES -> "Stock plein ❄️"
                            else -> "Acheter · ${Chests.FREEZE_PRICE} 💎"
                        },
                        onClick = { vm.buyFreeze() },
                        enabled = canBuy,
                        container = GameColors.Ice,
                        content = Color.White,
                    )
                }
            }
        }

        SectionTitle("Cours de grammaire")
        c.units.take(unlocked).forEach { u ->
            val g = c.grammarById[u.grammar] ?: return@forEach
            GameCard(onClick = { vm.go(Screen.Course(g.id)) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(g.emoji, fontSize = 28.sp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(g.title, style = MaterialTheme.typography.titleMedium)
                        Text(g.summary, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
                    }
                }
            }
        }
        val locked = c.units.size - unlocked
        if (locked > 0) {
            Text(
                "🔒 $locked autre(s) cours à débloquer sur la carte.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(4.dp),
            )
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun ModeCard(emoji: String, color: Color, title: String, text: String, button: String, onClick: () -> Unit) {
    GameCard {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                EmojiBadge(emoji, color, 56)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleMedium)
                    Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(12.dp))
            BigButton(button, onClick, container = color, content = Color.White)
        }
    }
}

/** Écran « cours » : la règle de grammaire expliquée simplement. */
@Composable
fun CourseScreen(vm: AppViewModel, lessonId: String) {
    val c = vm.content ?: return
    val g = c.grammarById[lessonId] ?: return
    BackHandler { vm.openTab(1) }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { vm.openTab(1) }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
            }
            Text("Le cours", style = MaterialTheme.typography.titleLarge)
        }
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text("${g.emoji}  ${g.title}", style = MaterialTheme.typography.headlineSmall)
            Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = MaterialTheme.shapes.medium) {
                Text(
                    "💡 ${g.summary}",
                    modifier = Modifier.padding(14.dp),
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            g.rules.forEachIndexed { i, r ->
                GameCard {
                    Column {
                        Text("${i + 1}. ${r.title}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(6.dp))
                        Text(r.text, style = MaterialTheme.typography.bodyLarge)
                        r.examples.forEach { ex ->
                            Row(Modifier.padding(top = 6.dp)) {
                                Text("🇬🇧 ", fontSize = 14.sp)
                                Text(ex, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                                Text(
                                    "🔊",
                                    fontSize = 18.sp,
                                    modifier = Modifier.padding(start = 6.dp).clickable { vm.speaker.speak(ex.substringBefore("(").trim()) },
                                )
                            }
                        }
                    }
                }
            }
            Text(
                "${g.items.size} exercices t'attendent : QCM, phrases à trous, traductions, chasse à l'erreur…",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
        }
        Column(Modifier.padding(16.dp)) {
            BigButton("🎯 M'entraîner sur cette règle", onClick = { vm.startPractice(g.id) })
        }
    }
}
