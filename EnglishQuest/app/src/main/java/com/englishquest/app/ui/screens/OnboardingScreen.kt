package com.englishquest.app.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.englishquest.app.ui.AppViewModel
import com.englishquest.app.ui.components.BigButton
import com.englishquest.app.ui.components.BigOutlinedButton
import com.englishquest.app.ui.components.GameCard

@Composable
fun OnboardingScreen(vm: AppViewModel) {
    val p by vm.progress.collectAsState()
    var step by remember { mutableIntStateOf(0) }
    var goal by remember { mutableIntStateOf(10) }
    var hour by remember { mutableIntStateOf(p.settings.reminderHour) }
    var minute by remember { mutableIntStateOf(p.settings.reminderMinute) }
    val context = LocalContext.current
    val requestPermission = rememberNotificationPermissionRequester()

    AnimatedContent(
        targetState = step,
        transitionSpec = {
            (slideInHorizontally { it } + fadeIn()) togetherWith (slideOutHorizontally { -it } + fadeOut())
        },
        label = "onboarding",
    ) { st ->
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            when (st) {
                0 -> {
                    Spacer(Modifier.height(24.dp))
                    Text("⚡", fontSize = 80.sp)
                    Text("English Quest", style = MaterialTheme.typography.headlineLarge)
                    Text(
                        "Ton coach d'anglais pour viser B2 au bac… version jeu vidéo.",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Feature("🗺️", "20 unités, 500+ mots du bac et 20 leçons de grammaire")
                    Feature("🧠", "Les mots que tu rates reviennent pile quand il faut (répétition espacée)")
                    Feature("🔥", "Séries, combos, coffres, badges et boss à battre")
                    Feature("✈️", "100 % hors ligne, sans compte et sans pub")
                    Spacer(Modifier.height(8.dp))
                    BigButton("C'est parti !", onClick = { step = 1 })
                }
                1 -> {
                    Spacer(Modifier.height(24.dp))
                    Text("🎯", fontSize = 72.sp)
                    Text("Ton objectif quotidien", style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
                    Text(
                        "Mieux vaut 10 minutes tous les jours qu'une heure le dimanche. Tu pourras le changer plus tard.",
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    listOf(5 to "🙂 Cool", 10 to "💪 Régulier", 15 to "🔥 Sérieux", 20 to "🚀 Intense").forEach { (m, label) ->
                        GoalOption("$label · $m min / jour", goal == m) { goal = m }
                    }
                    BigButton("Continuer", onClick = { step = 2 })
                }
                else -> {
                    Spacer(Modifier.height(24.dp))
                    Text("🔔", fontSize = 72.sp)
                    Text("Un petit rappel ?", style = MaterialTheme.typography.headlineMedium)
                    Text(
                        "Une notification par jour, seulement si tu ne t'es pas encore entraîné(e). Désactivable à tout moment.",
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    BigOutlinedButton("⏰ Heure : ${formatTime(hour, minute)} (modifier)", onClick = {
                        showTimePicker(context, hour, minute) { h, m -> hour = h; minute = m }
                    })
                    BigButton("Activer le rappel", onClick = {
                        requestPermission()
                        vm.updateSettings { it.copy(reminderHour = hour, reminderMinute = minute) }
                        vm.completeOnboarding(goal, reminderOn = true)
                    })
                    TextButton(onClick = { vm.completeOnboarding(goal, reminderOn = false) }) {
                        Text("Plus tard")
                    }
                }
            }
        }
    }
}

@Composable
private fun Feature(emoji: String, text: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(emoji, fontSize = 28.sp)
        Spacer(Modifier.width(12.dp))
        Text(text, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun GoalOption(label: String, selected: Boolean, onClick: () -> Unit) {
    GameCard(
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        onClick = onClick,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Text(if (selected) "✅" else "⚪", fontSize = 20.sp)
        }
    }
}
