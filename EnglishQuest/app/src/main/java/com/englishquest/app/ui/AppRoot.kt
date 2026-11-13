package com.englishquest.app.ui

import android.app.Activity
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.englishquest.app.ui.components.TopStatusBar
import com.englishquest.app.ui.screens.BadgesScreen
import com.englishquest.app.ui.screens.CourseScreen
import com.englishquest.app.ui.screens.MapScreen
import com.englishquest.app.ui.screens.OnboardingScreen
import com.englishquest.app.ui.screens.ResultScreen
import com.englishquest.app.ui.screens.SessionScreen
import com.englishquest.app.ui.screens.SettingsScreen
import com.englishquest.app.ui.screens.StatsScreen
import com.englishquest.app.ui.screens.TrainScreen
import com.englishquest.app.ui.theme.EnglishQuestTheme

@Composable
fun AppRoot(vm: AppViewModel) {
    val p by vm.progress.collectAsState()
    val dark = when (p.settings.themeMode) {
        1 -> false
        2 -> true
        else -> isSystemInDarkTheme()
    }

    // Icônes de la barre d'état lisibles quel que soit le thème choisi
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            val ctrl = WindowCompat.getInsetsController(window, view)
            ctrl.isAppearanceLightStatusBars = !dark
            ctrl.isAppearanceLightNavigationBars = !dark
        }
    }

    EnglishQuestTheme(darkTheme = dark) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
                AnimatedContent(
                    targetState = vm.screen,
                    contentKey = { it::class },
                    transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(160)) },
                    label = "screen",
                ) { s ->
                    when (s) {
                        Screen.Loading -> LoadingScreen()
                        Screen.Onboarding -> OnboardingScreen(vm)
                        is Screen.Main -> MainScreen(vm, (vm.screen as? Screen.Main)?.tab ?: s.tab)
                        is Screen.Course -> CourseScreen(vm, s.lessonId)
                        Screen.Session -> {
                            val session = vm.session
                            if (session != null) SessionScreen(vm, session) else Box(Modifier.fillMaxSize())
                        }
                        is Screen.Result -> ResultScreen(vm, s.result)
                    }
                }
            }
        }

        val notice = vm.notice
        if (notice != null) {
            AlertDialog(
                onDismissRequest = { vm.notice = null },
                confirmButton = { TextButton(onClick = { vm.notice = null }) { Text("OK") } },
                text = { Text(notice, style = MaterialTheme.typography.bodyLarge) },
            )
        }
    }
}

@Composable
private fun LoadingScreen() {
    Column(
        Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("⚡", fontSize = 64.sp)
        Spacer(Modifier.height(16.dp))
        CircularProgressIndicator()
    }
}

private val tabs = listOf(
    "🗺️" to "Carte",
    "⚡" to "Jouer",
    "🏆" to "Succès",
    "📊" to "Stats",
    "⚙️" to "Réglages",
)

@Composable
private fun MainScreen(vm: AppViewModel, tab: Int) {
    val p by vm.progress.collectAsState()
    Column(Modifier.fillMaxSize()) {
        TopStatusBar(p, onClick = { vm.openTab(3) })
        Box(Modifier.weight(1f)) {
            when (tab) {
                0 -> MapScreen(vm)
                1 -> TrainScreen(vm)
                2 -> BadgesScreen(vm)
                3 -> StatsScreen(vm)
                else -> SettingsScreen(vm)
            }
        }
        NavigationBar(windowInsets = WindowInsets(0, 0, 0, 0), tonalElevation = 4.dp) {
            tabs.forEachIndexed { i, (emoji, label) ->
                NavigationBarItem(
                    selected = tab == i,
                    onClick = { vm.openTab(i) },
                    icon = { Text(emoji, fontSize = 22.sp) },
                    label = { Text(label, fontSize = 11.sp, maxLines = 1) },
                )
            }
        }
    }
}
