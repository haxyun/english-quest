package com.englishquest.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.englishquest.app.data.Progress
import com.englishquest.app.game.Levels
import com.englishquest.app.ui.theme.GameColors
import kotlin.random.Random

/** Gros bouton arrondi, facile à toucher. */
@Composable
fun BigButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    container: Color = MaterialTheme.colorScheme.primary,
    content: Color = MaterialTheme.colorScheme.onPrimary,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth().height(58.dp),
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(containerColor = container, contentColor = content),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp, pressedElevation = 0.dp),
    ) {
        Text(text, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
    }
}

@Composable
fun BigOutlinedButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(54.dp),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(2.dp, MaterialTheme.colorScheme.outline),
    ) {
        Text(text, style = MaterialTheme.typography.titleSmall)
    }
}

/** Petite pastille « emoji + valeur ». */
@Composable
fun StatPill(emoji: String, value: String, color: Color, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    Surface(
        shape = CircleShape,
        color = color.copy(alpha = 0.15f),
        modifier = modifier.then(if (onClick != null) Modifier.clip(CircleShape).clickable { onClick() } else Modifier),
    ) {
        Row(Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(emoji, fontSize = 16.sp)
            Spacer(Modifier.width(4.dp))
            Text(value, fontWeight = FontWeight.ExtraBold, color = color, fontSize = 15.sp)
        }
    }
}

/** Barre du haut : série, gels, gemmes, niveau. */
@Composable
fun TopStatusBar(p: Progress, onClick: () -> Unit) {
    val level = Levels.level(p.totalXp)
    Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            StatPill("🔥", p.streak.toString(), GameColors.Fire, onClick = onClick)
            StatPill("❄️", p.freezes.toString(), GameColors.Ice, onClick = onClick)
            StatPill("💎", p.gems.toString(), GameColors.Gem, onClick = onClick)
            StatPill("⭐", "Niv. $level", MaterialTheme.colorScheme.primary, onClick = onClick)
        }
        Spacer(Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { Levels.progress(p.totalXp) },
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            strokeCap = StrokeCap.Round,
        )
    }
}

/** Carte arrondie générique. */
@Composable
fun GameCard(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.surface,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth().then(
            if (onClick != null) Modifier.clip(RoundedCornerShape(20.dp)).clickable { onClick() } else Modifier
        ),
        shape = RoundedCornerShape(20.dp),
        color = color,
        tonalElevation = 1.dp,
        shadowElevation = 2.dp,
    ) {
        Box(Modifier.padding(16.dp)) { content() }
    }
}

/** Sélecteur en forme de pilule (objectif quotidien, thème…). */
@Composable
fun RowScope.ChoicePill(label: String, selected: Boolean, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.weight(1f).heightIn(min = 48.dp).clip(RoundedCornerShape(14.dp)).clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        color = if (selected) cs.primary else cs.surfaceVariant,
        border = if (selected) null else BorderStroke(1.dp, cs.outline),
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp)) {
            Text(
                label,
                color = if (selected) cs.onPrimary else cs.onSurfaceVariant,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                fontSize = 14.sp,
            )
        }
    }
}

private data class Particle(
    val x0: Float, val vx: Float, val vy: Float, val g: Float,
    val rot: Float, val spin: Float, val color: Color, val w: Float, val h: Float,
)

private val confettiColors = listOf(
    Color(0xFFFF4D6D), Color(0xFFFFC233), Color(0xFF22B573), Color(0xFF4FC3F7), Color(0xFF7C5CFF), Color(0xFFFF8A00),
)

/** Pluie de confettis rejouée à chaque changement de [trigger] (> 0). */
@Composable
fun ConfettiOverlay(trigger: Int, modifier: Modifier = Modifier) {
    val anim = remember { Animatable(1f) }
    val particles = remember(trigger) {
        val r = Random(trigger * 7919 + 13)
        List(80) {
            Particle(
                x0 = r.nextFloat(),
                vx = (r.nextFloat() - 0.5f) * 0.5f,
                vy = 0.2f + r.nextFloat() * 0.5f,
                g = 0.6f + r.nextFloat() * 0.8f,
                rot = r.nextFloat() * 360f,
                spin = (r.nextFloat() - 0.5f) * 4f,
                color = confettiColors[r.nextInt(confettiColors.size)],
                w = 10f + r.nextFloat() * 10f,
                h = 6f + r.nextFloat() * 6f,
            )
        }
    }
    LaunchedEffect(trigger) {
        if (trigger > 0) {
            anim.snapTo(0f)
            anim.animateTo(1f, tween(2200, easing = LinearEasing))
        }
    }
    val t = anim.value
    if (trigger > 0 && t < 1f) {
        Canvas(modifier.fillMaxSize()) {
            particles.forEach { pa ->
                val x = size.width * (pa.x0 + pa.vx * t)
                val y = size.height * (-0.08f + pa.vy * t + pa.g * t * t)
                rotate(pa.rot + 360f * pa.spin * t, Offset(x, y)) {
                    drawRect(
                        color = pa.color,
                        topLeft = Offset(x - pa.w / 2, y - pa.h / 2),
                        size = Size(pa.w, pa.h),
                        alpha = (1f - t * 0.7f).coerceIn(0f, 1f),
                    )
                }
            }
        }
    }
}

/** Petite ligne « libellé : valeur ». */
@Composable
fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.titleLarge,
        modifier = modifier.padding(top = 8.dp, bottom = 4.dp),
    )
}

/** Fond coloré circulaire pour un emoji. */
@Composable
fun EmojiBadge(emoji: String, color: Color, size: Int = 48) {
    Box(
        Modifier.width(size.dp).height(size.dp).clip(CircleShape).background(color.copy(alpha = 0.18f)),
        contentAlignment = Alignment.Center,
    ) {
        Text(emoji, fontSize = (size * 0.5f).sp)
    }
}
