package com.englishquest.app.ui.screens

import android.Manifest
import android.app.TimePickerDialog
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.englishquest.app.ui.AppViewModel
import com.englishquest.app.ui.components.BigOutlinedButton
import com.englishquest.app.ui.components.ChoicePill
import com.englishquest.app.ui.components.GameCard
import com.englishquest.app.ui.components.SectionTitle

/** Demande l'autorisation d'envoyer des notifications (Android 13+). */
@Composable
fun rememberNotificationPermissionRequester(onDenied: () -> Unit = {}): () -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted) onDenied()
    }
    return {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}

fun formatTime(h: Int, m: Int): String = "%02d:%02d".format(h, m)

fun showTimePicker(context: Context, h: Int, m: Int, onPicked: (Int, Int) -> Unit) {
    TimePickerDialog(context, { _, hh, mm -> onPicked(hh, mm) }, h, m, true).show()
}

@Composable
fun SettingsScreen(vm: AppViewModel) {
    val p by vm.progress.collectAsState()
    val s = p.settings
    val context = LocalContext.current
    val requestPermission = rememberNotificationPermissionRequester {
        Toast.makeText(context, "Sans autorisation, le rappel ne pourra pas s'afficher.", Toast.LENGTH_LONG).show()
    }
    var confirmReset by remember { mutableStateOf(false) }
    var rate by remember(s.ttsRate) { mutableFloatStateOf(s.ttsRate) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        SectionTitle("Réglages")

        GameCard {
            Column {
                Text("🎯 Objectif quotidien", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(5, 10, 15, 20).forEach { m ->
                        ChoicePill("$m min", s.dailyGoalMin == m) { vm.updateSettings { it.copy(dailyGoalMin = m) } }
                    }
                }
            }
        }

        GameCard {
            Column {
                SettingSwitch("🔔 Rappel quotidien", "Une notification si tu ne t'es pas entraîné(e).", s.reminderOn) { on ->
                    if (on) requestPermission()
                    vm.updateSettings { it.copy(reminderOn = on) }
                }
                if (s.reminderOn) {
                    Spacer(Modifier.height(8.dp))
                    BigOutlinedButton("⏰ Heure du rappel : ${formatTime(s.reminderHour, s.reminderMinute)}", onClick = {
                        showTimePicker(context, s.reminderHour, s.reminderMinute) { h, m ->
                            vm.updateSettings { it.copy(reminderHour = h, reminderMinute = m) }
                        }
                    })
                }
            }
        }

        GameCard {
            Column {
                Text("🌗 Thème", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Auto", "☀️ Clair", "🌙 Sombre").forEachIndexed { i, label ->
                        ChoicePill(label, s.themeMode == i) { vm.updateSettings { it.copy(themeMode = i) } }
                    }
                }
            }
        }

        GameCard {
            Column {
                SettingSwitch("🔊 Effets sonores", "Sons de bonne réponse, combo, coffre…", s.sound) { on ->
                    vm.updateSettings { it.copy(sound = on) }
                }
                SettingSwitch("📳 Vibrations", "Petit retour haptique à chaque réponse.", s.haptics) { on ->
                    vm.updateSettings { it.copy(haptics = on) }
                }
            }
        }

        GameCard {
            Column {
                Text("🗣️ Vitesse de la voix anglaise", style = MaterialTheme.typography.titleMedium)
                Slider(
                    value = rate,
                    onValueChange = { rate = it },
                    valueRange = 0.5f..1.3f,
                    onValueChangeFinished = { vm.updateSettings { it.copy(ttsRate = rate) } },
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("x${"%.1f".format(rate)}", modifier = Modifier.weight(1f))
                    TextButton(onClick = {
                        vm.speaker.rate = rate
                        vm.speaker.speak("Hello! Ready for your English quest?")
                    }) { Text("▶ Tester") }
                }
                if (!vm.speaker.ready) {
                    Text(
                        "⚠️ Aucune voix anglaise détectée : les exercices d'écoute sont désactivés. " +
                            "Installe « Synthèse vocale Google » et la voix anglaise dans les paramètres Android " +
                            "(Accessibilité › Synthèse vocale).",
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }

        GameCard {
            Column {
                Text("ℹ️ À propos", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(6.dp))
                Text(
                    "English Quest fonctionne 100 % hors ligne : pas de compte, pas de pub, pas de collecte de données. " +
                        "Ta progression est enregistrée sur ce téléphone uniquement.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(10.dp))
                BigOutlinedButton("🗑️ Réinitialiser ma progression", onClick = { confirmReset = true })
            }
        }
        Spacer(Modifier.height(16.dp))
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("Tout recommencer ?") },
            text = { Text("XP, série, badges et mots appris seront effacés. Cette action est définitive.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmReset = false
                    vm.resetProgress()
                }) { Text("Effacer", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("Annuler") } },
        )
    }
}

@Composable
private fun SettingSwitch(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
