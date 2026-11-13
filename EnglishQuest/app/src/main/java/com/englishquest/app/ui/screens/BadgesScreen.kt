package com.englishquest.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.englishquest.app.game.Badge
import com.englishquest.app.game.Badges
import com.englishquest.app.game.Levels
import com.englishquest.app.ui.AppViewModel
import com.englishquest.app.ui.theme.GameColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun BadgesScreen(vm: AppViewModel) {
    val p by vm.progress.collectAsState()
    var selected by remember { mutableStateOf<Badge?>(null) }
    val level = Levels.level(p.totalXp)
    val next = Levels.nextRank(level)

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                Column(Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Ton rang", color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text(Levels.rank(level), style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onPrimaryContainer, textAlign = TextAlign.Center)
                    Text("Niveau $level · ${p.totalXp} XP", color = MaterialTheme.colorScheme.onPrimaryContainer)
                    if (next != null) {
                        Text(
                            "Prochain rang au niveau ${next.first} : ${next.second}",
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            Text(
                "🏆 Succès : ${p.badges.size} / ${Badges.all.size}",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        items(Badges.all, key = { it.id }) { b ->
            val unlocked = b.id in p.badges
            val secret = b.hidden && !unlocked
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = if (unlocked) GameColors.Gold.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant,
                border = if (unlocked) BorderStroke(2.dp, GameColors.Gold) else null,
                modifier = Modifier.heightIn(min = 120.dp).clip(RoundedCornerShape(18.dp)).clickable { selected = b },
            ) {
                Column(
                    Modifier.padding(8.dp).alpha(if (unlocked) 1f else 0.55f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(if (secret) "❓" else b.emoji, fontSize = 36.sp)
                    Text(
                        if (secret) "Succès caché" else b.name,
                        style = MaterialTheme.typography.labelLarge,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                    )
                }
            }
        }
    }

    selected?.let { b ->
        val unlocked = b.id in p.badges
        val secret = b.hidden && !unlocked
        AlertDialog(
            onDismissRequest = { selected = null },
            confirmButton = { TextButton(onClick = { selected = null }) { Text("OK") } },
            title = { Text(if (secret) "❓ Succès caché" else "${b.emoji} ${b.name}") },
            text = {
                Text(
                    when {
                        secret -> "Mystère… Continue à jouer pour le découvrir !"
                        unlocked -> b.desc + "\n\n✅ Débloqué le " +
                            SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE).format(Date(p.badges[b.id] ?: 0L))
                        else -> b.desc + "\n\n🔒 Pas encore débloqué."
                    }
                )
            },
        )
    }
}
