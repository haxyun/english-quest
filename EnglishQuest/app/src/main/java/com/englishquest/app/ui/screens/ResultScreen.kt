package com.englishquest.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.englishquest.app.audio.SoundFx
import com.englishquest.app.game.ChestReward
import com.englishquest.app.game.Rarity
import com.englishquest.app.ui.AppViewModel
import com.englishquest.app.ui.SessionResult
import com.englishquest.app.ui.components.BigButton
import com.englishquest.app.ui.components.ConfettiOverlay
import com.englishquest.app.ui.theme.GameColors

@Composable
fun ResultScreen(vm: AppViewModel, r: SessionResult) {
    var confetti by remember { mutableIntStateOf(0) }
    BackHandler { vm.closeResult(r) }

    LaunchedEffect(Unit) {
        if (r.won || r.mode == com.englishquest.app.game.Mode.SURVIVAL) {
            SoundFx.victory()
            confetti = 1
        } else {
            SoundFx.defeat()
        }
    }

    val pop = remember { Animatable(0.3f) }
    LaunchedEffect(Unit) { pop.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)) }

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Spacer(Modifier.height(8.dp))
            Text(r.emoji, fontSize = 84.sp, modifier = Modifier.scale(pop.value))
            Text(r.title, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
            Text(r.subtitle, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ResultTile("⭐", "+${r.xp}", "XP", GameColors.Gold, Modifier.weight(1f))
                val acc = if (r.answered > 0) r.correct * 100 / r.answered else 0
                ResultTile("🎯", "$acc %", "précision", GameColors.Correct, Modifier.weight(1f))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ResultTile("🔥", "x${r.maxCombo}", "meilleur combo", GameColors.Fire, Modifier.weight(1f))
                ResultTile("⏱️", "${r.seconds / 60}:${(r.seconds % 60).toString().padStart(2, '0')}", "durée", GameColors.Ice, Modifier.weight(1f))
            }

            if (r.levelUp) {
                Banner("🆙", "Niveau ${r.newLevel} !", "Rang : ${r.rank}", MaterialTheme.colorScheme.primary)
            }
            if (r.record) {
                Banner("🏆", "Nouveau record !", "Tu as battu ton meilleur score.", GameColors.Gold)
            }
            if (r.streakExtended) {
                Banner(
                    "🔥", "Série : ${r.streak} jour${if (r.streak > 1) "s" else ""}",
                    if (r.freezeEarned) "Bonus : +1 gel de série ❄️ (7 jours d'affilée !)" else "Reviens demain pour la prolonger !",
                    GameColors.Fire,
                )
            }
            if (r.goalReached) {
                Banner("🎯", "Objectif du jour atteint !", "+10 💎 de récompense", GameColors.Correct)
            }
            r.newBadges.forEach { b ->
                Banner(b.emoji, "Succès débloqué : ${b.name}", b.desc, GameColors.Gem)
            }

            r.chest?.let { ChestView(it) { confetti++ } }

            Spacer(Modifier.height(4.dp))
            BigButton("Continuer", onClick = { vm.closeResult(r) })
            Spacer(Modifier.height(12.dp))
        }
        ConfettiOverlay(confetti)
    }
}

@Composable
private fun ResultTile(emoji: String, value: String, label: String, color: Color, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = color.copy(alpha = 0.14f),
        border = BorderStroke(2.dp, color.copy(alpha = 0.6f)),
        modifier = modifier,
    ) {
        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(emoji, fontSize = 24.sp)
            Text(value, style = MaterialTheme.typography.titleLarge)
            Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun Banner(emoji: String, title: String, text: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = color.copy(alpha = 0.13f),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(emoji, fontSize = 30.sp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = color)
                Text(text, color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

@Composable
private fun ChestView(chest: ChestReward, onOpened: () -> Unit) {
    var opened by remember { mutableStateOf(false) }
    val color = when (chest.rarity) {
        Rarity.COMMON -> GameColors.Ice
        Rarity.RARE -> GameColors.Gem
        Rarity.EPIC -> GameColors.Gold
    }
    val wobbleTransition = rememberInfiniteTransition(label = "wobble")
    val wobble by wobbleTransition.animateFloat(
        initialValue = -8f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(tween(260), RepeatMode.Reverse),
        label = "wobbleAngle",
    )
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = color.copy(alpha = 0.15f),
        border = BorderStroke(2.dp, color),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .clickable(enabled = !opened) {
                opened = true
                SoundFx.chest()
                onOpened()
            },
    ) {
        Column(Modifier.padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            if (!opened) {
                Text(chest.rarity.emoji, fontSize = 64.sp, modifier = Modifier.rotate(wobble))
                Text(chest.rarity.label, style = MaterialTheme.typography.titleMedium, color = color)
                Text("Touche pour l'ouvrir !", fontWeight = FontWeight.Bold)
            } else {
                val appear = remember { MutableTransitionState(false).apply { targetState = true } }
                AnimatedVisibility(
                    visibleState = appear,
                    enter = scaleIn(spring(dampingRatio = Spring.DampingRatioHighBouncy)) + fadeIn(),
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("✨", fontSize = 48.sp)
                        Text("${chest.rarity.label} ouvert !", style = MaterialTheme.typography.titleMedium, color = color)
                        if (chest.xp > 0) Text("+${chest.xp} XP ⭐", fontSize = 22.sp, fontWeight = FontWeight.Black)
                        if (chest.gems > 0) Text("+${chest.gems} 💎", fontSize = 22.sp, fontWeight = FontWeight.Black)
                        if (chest.freeze) Text("+1 gel de série ❄️", fontSize = 22.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }
}
