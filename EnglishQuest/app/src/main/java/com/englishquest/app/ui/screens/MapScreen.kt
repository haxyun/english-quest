package com.englishquest.app.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.englishquest.app.data.Content
import com.englishquest.app.data.Progress
import com.englishquest.app.data.UnitInfo
import com.englishquest.app.ui.AppViewModel
import com.englishquest.app.ui.Screen
import com.englishquest.app.ui.theme.GameColors
import kotlin.math.max

private enum class NodeState { DONE, CURRENT, LOCKED }

@Composable
fun MapScreen(vm: AppViewModel) {
    val c = vm.content ?: return
    val p by vm.progress.collectAsState()
    val current = vm.currentUnitIndex(p)
    val listState = rememberLazyListState()
    LaunchedEffect(Unit) {
        // L'élément 0 est la carte « objectif du jour » ; les unités commencent à l'index 1.
        if (current > 0) listState.scrollToItem(max(0, current + 1))
    }
    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { DailyGoalCard(vm, p) }
        itemsIndexed(c.units, key = { _, u -> u.id }) { i, u ->
            UnitSection(vm, c, p, i, u)
        }
        item {
            Text(
                "🏁 Fin du parcours… pour l'instant ! Continue les révisions pour tout maîtriser.",
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun DailyGoalCard(vm: AppViewModel, p: Progress) {
    val today = vm.today()
    val day = p.days[today]
    val goal = p.settings.dailyGoalMin * 60
    val done = day?.seconds ?: 0
    val ratio = (done.toFloat() / goal).coerceIn(0f, 1f)
    val due = vm.dueCount(p)
    Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { ratio },
                    modifier = Modifier.size(64.dp),
                    strokeWidth = 7.dp,
                    color = if (ratio >= 1f) GameColors.Correct else MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surface,
                )
                Text(if (ratio >= 1f) "✅" else "🎯", fontSize = 22.sp)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text("Objectif du jour", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                Text(
                    "${done / 60} / ${p.settings.dailyGoalMin} min · ${day?.xp ?: 0} XP aujourd'hui",
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                if (due > 0) {
                    Text(
                        "🧠 $due carte(s) à réviser",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 4.dp).clip(RoundedCornerShape(8.dp)).clickable { vm.startReview() },
                    )
                }
            }
        }
    }
}

@Composable
private fun UnitSection(vm: AppViewModel, c: Content, p: Progress, index: Int, u: UnitInfo) {
    val unlocked = vm.unitUnlocked(index, p)
    val done = p.lessonsDone[u.id] ?: 0
    val beaten = u.id in p.bosses
    val color = GameColors.unitPalette[index % GameColors.unitPalette.size]
    val theme = c.themeById[u.theme]
    val grammar = c.grammarById[u.grammar]

    Column(Modifier.fillMaxWidth().alpha(if (unlocked) 1f else 0.55f)) {
        // En-tête de l'unité
        Surface(shape = RoundedCornerShape(22.dp), color = Color.Transparent, modifier = Modifier.fillMaxWidth()) {
            Column(
                Modifier
                    .background(Brush.horizontalGradient(listOf(color, color.copy(alpha = 0.75f))))
                    .padding(16.dp)
            ) {
                Text("UNITÉ ${index + 1}", color = Color.White.copy(alpha = 0.85f), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Text("${u.emoji} ${u.title}", color = Color.White, style = MaterialTheme.typography.titleLarge)
                Text(
                    "${theme?.emoji ?: ""} ${theme?.name ?: ""} · ${grammar?.emoji ?: ""} ${grammar?.title ?: ""}",
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 13.sp,
                )
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (unlocked && grammar != null) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White.copy(alpha = 0.22f),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.5f)),
                            modifier = Modifier.clip(RoundedCornerShape(12.dp)).clickable { vm.go(Screen.Course(grammar.id)) },
                        ) {
                            Text("📖 Le cours", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp))
                        }
                        Spacer(Modifier.width(10.dp))
                    }
                    Text(
                        when {
                            !unlocked -> "🔒 Bats le boss précédent"
                            beaten -> "👑 Unité terminée"
                            else -> "$done / ${Content.LESSONS_PER_UNIT} leçons"
                        },
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))

        // Chemin des leçons
        val offsets = listOf((-70).dp, 0.dp, 70.dp, 0.dp)
        for (k in 0 until Content.LESSONS_PER_UNIT) {
            val state = when {
                !unlocked -> NodeState.LOCKED
                k < done -> NodeState.DONE
                k == done -> NodeState.CURRENT
                else -> NodeState.LOCKED
            }
            MapNode(
                emoji = when (state) {
                    NodeState.DONE -> "⭐"
                    NodeState.CURRENT -> listOf("📘", "🎧", "✍️", "🧩")[k % 4]
                    NodeState.LOCKED -> "🔒"
                },
                label = "Leçon ${k + 1}",
                state = state,
                color = color,
                size = 70.dp,
                xOffset = offsets[k % offsets.size],
                onClick = { vm.startLesson(index, k) },
            )
            Spacer(Modifier.height(8.dp))
        }
        val bossState = when {
            beaten -> NodeState.DONE
            unlocked && done >= Content.LESSONS_PER_UNIT -> NodeState.CURRENT
            else -> NodeState.LOCKED
        }
        MapNode(
            emoji = if (bossState == NodeState.LOCKED) "🔒" else u.bossEmoji,
            label = if (beaten) "${u.bossName} 👑" else "BOSS : ${u.bossName}",
            state = bossState,
            color = GameColors.Heart,
            size = 92.dp,
            xOffset = (-40).dp,
            onClick = { vm.startBoss(index) },
        )
    }
}

@Composable
private fun MapNode(
    emoji: String,
    label: String,
    state: NodeState,
    color: Color,
    size: Dp,
    xOffset: Dp,
    onClick: () -> Unit,
) {
    val scale = if (state == NodeState.CURRENT) {
        val inf = rememberInfiniteTransition(label = "pulse")
        inf.animateFloat(
            initialValue = 1f,
            targetValue = 1.09f,
            animationSpec = infiniteRepeatable(tween(750), RepeatMode.Reverse),
            label = "pulseScale",
        ).value
    } else 1f
    val bg = when (state) {
        NodeState.DONE -> GameColors.Gold
        NodeState.CURRENT -> color
        NodeState.LOCKED -> GameColors.Locked.copy(alpha = 0.45f)
    }
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .offset(x = xOffset)
                .scale(scale)
                .size(size)
                .shadow(if (state == NodeState.LOCKED) 0.dp else 8.dp, CircleShape)
                .clip(CircleShape)
                .background(bg)
                .border(4.dp, Color.White.copy(alpha = if (state == NodeState.CURRENT) 0.9f else 0.35f), CircleShape)
                .clickable(enabled = state != NodeState.LOCKED) { onClick() },
            contentAlignment = Alignment.Center,
        ) {
            Text(emoji, fontSize = (size.value * 0.42f).sp)
        }
        Text(
            label,
            modifier = Modifier.offset(x = xOffset).padding(top = 4.dp),
            style = MaterialTheme.typography.labelLarge,
            color = if (state == NodeState.LOCKED) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onBackground,
        )
    }
}
