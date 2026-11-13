package com.englishquest.app.ui.screens

import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.englishquest.app.audio.SoundFx
import com.englishquest.app.game.Answer
import com.englishquest.app.game.Exercise
import com.englishquest.app.game.Feedback
import com.englishquest.app.game.Mode
import com.englishquest.app.game.SessionController
import com.englishquest.app.ui.AppViewModel
import com.englishquest.app.ui.components.BigButton
import com.englishquest.app.ui.components.ConfettiOverlay
import com.englishquest.app.ui.theme.GameColors
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

private val praises = listOf("Excellent !", "Bravo !", "Parfait !", "Brilliant!", "Well done!", "Spot on!", "Génial !", "Nickel !")

@Composable
fun SessionScreen(vm: AppViewModel, s: SessionController) {
    val p by vm.progress.collectAsState()
    val haptic = LocalHapticFeedback.current
    var askQuit by remember { mutableStateOf(false) }
    var confetti by remember { mutableStateOf(0) }

    BackHandler { askQuit = true }

    // Chrono du contre-la-montre
    if (s.mode == Mode.TIME_ATTACK) {
        LaunchedEffect(s) {
            var last = SystemClock.elapsedRealtime()
            while (!s.finished) {
                delay(100)
                val now = SystemClock.elapsedRealtime()
                s.tick(now - last)
                last = now
            }
        }
    }

    // Sons, vibrations et passage automatique
    val fb = s.feedback
    LaunchedEffect(fb) {
        if (fb == null) return@LaunchedEffect
        if (fb.correct) {
            SoundFx.correct(s.combo + 1)
            if (p.settings.haptics) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            if (s.mode == Mode.TIME_ATTACK) {
                delay(450)
                s.proceed()
            }
        } else {
            SoundFx.wrong()
            if (p.settings.haptics) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }
    LaunchedEffect(s.comboFlash) {
        if (s.comboFlash > 0) {
            SoundFx.combo()
            if (s.combo >= 10) confetti++
        }
    }
    LaunchedEffect(s.hitTrigger) {
        if (s.hitTrigger > 0) SoundFx.hit()
    }

    // Petite secousse de l'écran à chaque erreur
    val shake = remember { Animatable(0f) }
    LaunchedEffect(s.wrongTrigger) {
        if (s.wrongTrigger > 0) {
            for (x in listOf(14f, -12f, 9f, -6f, 3f, 0f)) shake.animateTo(x, tween(45))
        }
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().offset { IntOffset(shake.value.roundToInt(), 0) }) {
            SessionHeader(s, onClose = { askQuit = true })
            if (s.mode == Mode.BOSS) BossBar(s)

            Box(Modifier.weight(1f).fillMaxWidth()) {
                AnimatedContent(
                    targetState = s.current,
                    contentKey = { it?.uid ?: -1 },
                    transitionSpec = {
                        (slideInHorizontally(tween(260)) { it / 2 } + fadeIn(tween(260))) togetherWith
                            (slideOutHorizontally(tween(200)) { -it / 2 } + fadeOut(tween(200)))
                    },
                    label = "exercise",
                ) { ex ->
                    if (ex != null) {
                        Column(
                            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp),
                        ) {
                            ExerciseView(vm, s, ex)
                            Spacer(Modifier.height(24.dp))
                        }
                    }
                }
            }

            BottomPanel(s)
        }

        ComboBanner(s.comboFlash, s.combo)
        ConfettiOverlay(confetti)
    }

    if (askQuit) {
        AlertDialog(
            onDismissRequest = { askQuit = false },
            title = { Text("Quitter la session ?") },
            text = { Text("Tu perdras l'XP de cette session (les mots déjà révisés restent comptés).") },
            confirmButton = {
                TextButton(onClick = {
                    askQuit = false
                    vm.quitSession()
                }) { Text("Quitter", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { askQuit = false }) { Text("Continuer") } },
        )
    }
}

@Composable
private fun SessionHeader(s: SessionController, onClose: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onClose) { Icon(Icons.Filled.Close, contentDescription = "Quitter") }
        Box(Modifier.weight(1f)) {
            when (s.mode) {
                Mode.LESSON, Mode.REVIEW, Mode.PRACTICE -> {
                    val prog by animateFloatAsState(s.progress, tween(400), label = "progress")
                    LinearProgressIndicator(
                        progress = { prog },
                        modifier = Modifier.fillMaxWidth().height(14.dp).clip(CircleShape),
                        color = GameColors.Correct,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        strokeCap = StrokeCap.Round,
                    )
                }
                Mode.TIME_ATTACK -> {
                    val ratio = s.timeLeftMs.toFloat() / SessionController.TIME_ATTACK_MS
                    Column {
                        LinearProgressIndicator(
                            progress = { ratio },
                            modifier = Modifier.fillMaxWidth().height(14.dp).clip(CircleShape),
                            color = if (ratio < 0.2f) GameColors.Wrong else GameColors.Fire,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            strokeCap = StrokeCap.Round,
                        )
                        Text(
                            "⏱️ ${(s.timeLeftMs + 999) / 1000} s   ·   ✅ ${s.correctCount}",
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                }
                Mode.SURVIVAL, Mode.BOSS -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Hearts(s.lives)
                        if (s.mode == Mode.SURVIVAL) {
                            Spacer(Modifier.width(12.dp))
                            Text("✅ ${s.correctCount}", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
        Spacer(Modifier.width(8.dp))
        ComboPill(s.combo)
    }
}

@Composable
private fun Hearts(lives: Int) {
    Row {
        repeat(3) { i ->
            val alive = i < lives
            val sc by animateFloatAsState(if (alive) 1f else 0.7f, spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "heart$i")
            Text(if (alive) "❤️" else "🖤", fontSize = 22.sp, modifier = Modifier.scale(sc).padding(end = 2.dp))
        }
    }
}

@Composable
private fun ComboPill(combo: Int) {
    val visible = combo >= 2
    val target = if (visible) 1f else 0f
    val sc by animateFloatAsState(
        targetValue = target,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "combo",
    )
    // Petit « pop » à chaque nouvelle bonne réponse
    val pop = remember { Animatable(1f) }
    LaunchedEffect(combo) {
        if (combo >= 2) {
            pop.snapTo(1.35f)
            pop.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
        }
    }
    if (sc > 0.01f) {
        Surface(
            shape = CircleShape,
            color = GameColors.Fire,
            modifier = Modifier.scale(sc * pop.value),
        ) {
            Text(
                "🔥 x$combo",
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            )
        }
    } else {
        Spacer(Modifier.width(1.dp))
    }
}

@Composable
private fun BossBar(s: SessionController) {
    val shakeBoss = remember { Animatable(0f) }
    LaunchedEffect(s.hitTrigger) {
        if (s.hitTrigger > 0) {
            for (x in listOf(-16f, 14f, -10f, 6f, 0f)) shakeBoss.animateTo(x, tween(50))
        }
    }
    val hp by animateFloatAsState(s.bossHp.toFloat() / s.bossMaxHp, tween(350), label = "hp")
    Surface(
        color = GameColors.Heart.copy(alpha = 0.12f),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                s.bossEmoji,
                fontSize = 44.sp,
                modifier = Modifier.offset { IntOffset(shakeBoss.value.roundToInt(), 0) },
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(s.bossName, fontWeight = FontWeight.ExtraBold)
                LinearProgressIndicator(
                    progress = { hp },
                    modifier = Modifier.fillMaxWidth().height(12.dp).clip(CircleShape),
                    color = GameColors.Heart,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    strokeCap = StrokeCap.Round,
                )
                Text(
                    "PV ${s.bossHp} / ${s.bossMaxHp}" + if (s.combo >= 3) "  ·  💥 coups critiques !" else "",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ComboBanner(trigger: Int, combo: Int) {
    var visible by remember { mutableStateOf(false) }
    var shown by remember { mutableStateOf(0) }
    LaunchedEffect(trigger) {
        if (trigger > 0) {
            shown = combo
            visible = true
            delay(1100)
            visible = false
        }
    }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        AnimatedVisibility(
            visible = visible,
            enter = scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn(),
            exit = scaleOut() + fadeOut(),
        ) {
            Surface(shape = RoundedCornerShape(24.dp), color = GameColors.Fire, shadowElevation = 12.dp) {
                Column(Modifier.padding(horizontal = 28.dp, vertical = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("COMBO x$shown", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Black)
                    Text(
                        when {
                            shown >= 20 -> "Légendaire ! 👑"
                            shown >= 10 -> "En feu ! 🔥🔥"
                            else -> "Bonus d'XP ! 🔥"
                        },
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

@Composable
private fun BottomPanel(s: SessionController) {
    val fb = s.feedback
    val ex = s.current
    Box(Modifier.fillMaxWidth()) {
        if (fb == null) {
            if (ex != null && ex !is Exercise.Match) {
                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    BigButton(
                        text = "Vérifier",
                        onClick = { s.draft?.let { s.check(it) } },
                        enabled = s.draft != null,
                    )
                    if (ex is Exercise.Typing || ex is Exercise.Order) {
                        TextButton(
                            onClick = { s.check(if (ex is Exercise.Typing) Answer.Text("") else Answer.Tokens(emptyList())) },
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                        ) { Text("Je ne sais pas 🤷") }
                    }
                }
            }
        }
        AnimatedVisibility(
            visible = fb != null,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
        ) {
            // On garde la dernière valeur pour l'animation de sortie
            val last = remember { arrayOfNulls<Feedback>(1) }
            if (fb != null) last[0] = fb
            last[0]?.let { FeedbackPanel(s, it) }
        }
    }
}

@Composable
private fun FeedbackPanel(s: SessionController, fb: Feedback) {
    val good = fb.correct
    val bg = if (good) GameColors.Correct.copy(alpha = 0.16f) else GameColors.Wrong.copy(alpha = 0.14f)
    val accent = if (good) GameColors.Correct else GameColors.Wrong
    val praise = remember(fb) { praises.random() }
    Surface(color = bg, shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(if (good) "✅" else "❌", fontSize = 26.sp)
                Spacer(Modifier.width(8.dp))
                Text(
                    if (good) praise else "Pas tout à fait…",
                    color = accent,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f),
                )
                if (good && fb.xp > 0) {
                    Text("+${fb.xp} XP", color = accent, fontWeight = FontWeight.Black, fontSize = 18.sp)
                }
            }
            if (good && fb.typo) {
                Text("Attention à l'orthographe : ${fb.solution}", fontWeight = FontWeight.SemiBold)
            }
            if (!good) {
                if (fb.solution.isNotBlank()) {
                    Text("Bonne réponse : ${fb.solution}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                }
                fb.userAnswer?.let {
                    Text("Ta réponse : $it", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                }
            }
            if (!good || s.mode != Mode.TIME_ATTACK) {
                val showExplain = !good || fb.explanation.startsWith("Toutes") || fb.explanation.startsWith("À revoir")
                if (showExplain && fb.explanation.isNotBlank()) {
                    Text(fb.explanation, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                }
                Spacer(Modifier.height(4.dp))
                BigButton(
                    text = "Continuer",
                    onClick = { s.proceed() },
                    container = accent,
                    content = Color.White,
                )
                if (fb.canOverride) {
                    TextButton(onClick = { s.proceed(override = true) }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                        Text("J'avais bon (synonyme accepté)", textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }
}
