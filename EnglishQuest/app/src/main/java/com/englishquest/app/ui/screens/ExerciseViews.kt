package com.englishquest.app.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.englishquest.app.audio.SoundFx
import com.englishquest.app.game.Answer
import com.englishquest.app.game.Exercise
import com.englishquest.app.game.SessionController
import com.englishquest.app.ui.AppViewModel
import com.englishquest.app.ui.theme.GameColors
import kotlinx.coroutines.delay

/** Affiche l'exercice courant selon son type. */
@Composable
fun ExerciseView(vm: AppViewModel, s: SessionController, ex: Exercise) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (ex.isNew) {
                Surface(shape = CircleShape, color = GameColors.Gem) {
                    Text(
                        "✨ NOUVEAU",
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
                Spacer(Modifier.width(8.dp))
            }
            Text(ex.instruction, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        }
        when (ex) {
            is Exercise.Choice -> ChoiceView(vm, s, ex)
            is Exercise.Typing -> TypingView(vm, s, ex)
            is Exercise.Order -> OrderView(vm, s, ex)
            is Exercise.SpotError -> SpotErrorView(s, ex)
            is Exercise.Match -> MatchView(vm, s, ex)
        }
    }
}

// -------------------------------------------------------------------- éléments communs

@Composable
private fun SpeakButtons(vm: AppViewModel, text: String, big: Boolean = false) {
    if (!vm.speaker.ready) return
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.clip(RoundedCornerShape(16.dp)).clickable { vm.speaker.speak(text) },
        ) {
            Box(Modifier.size(if (big) 76.dp else 48.dp), contentAlignment = Alignment.Center) {
                Text("🔊", fontSize = if (big) 34.sp else 22.sp)
            }
        }
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.secondaryContainer,
            modifier = Modifier.clip(RoundedCornerShape(16.dp)).clickable { vm.speaker.speak(text, slow = true) },
        ) {
            Box(Modifier.size(if (big) 76.dp else 48.dp), contentAlignment = Alignment.Center) {
                Text("🐢", fontSize = if (big) 30.sp else 20.sp)
            }
        }
    }
}

private enum class TileState { NORMAL, SELECTED, CORRECT, WRONG, DISABLED }

@Composable
private fun AnswerTile(
    text: String,
    state: TileState,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val target = when (state) {
        TileState.NORMAL, TileState.DISABLED -> cs.surface
        TileState.SELECTED -> cs.primaryContainer
        TileState.CORRECT -> GameColors.Correct.copy(alpha = 0.22f)
        TileState.WRONG -> GameColors.Wrong.copy(alpha = 0.2f)
    }
    val border = when (state) {
        TileState.NORMAL, TileState.DISABLED -> cs.outline
        TileState.SELECTED -> cs.primary
        TileState.CORRECT -> GameColors.Correct
        TileState.WRONG -> GameColors.Wrong
    }
    val bg by animateColorAsState(target, tween(200), label = "tileBg")
    val bc by animateColorAsState(border, tween(200), label = "tileBorder")
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = bg,
        border = BorderStroke(2.dp, bc),
        shadowElevation = if (state == TileState.NORMAL) 2.dp else 0.dp,
        modifier = modifier
            .alpha(if (state == TileState.DISABLED) 0.35f else 1f)
            .clip(RoundedCornerShape(16.dp))
            .clickable(enabled = enabled && state != TileState.DISABLED) { onClick() },
    ) {
        Box(Modifier.heightIn(min = 52.dp).padding(horizontal = 14.dp, vertical = 12.dp), contentAlignment = Alignment.CenterStart) {
            Text(text, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun Prompt(text: String) {
    if (text.isNotBlank()) {
        Text(text, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
    }
}

// -------------------------------------------------------------------- QCM

@Composable
private fun ChoiceView(vm: AppViewModel, s: SessionController, ex: Exercise.Choice) {
    var selected by remember(ex.uid) { mutableIntStateOf(-1) }
    val fb = s.feedback
    val locked = fb != null || s.current?.uid != ex.uid

    LaunchedEffect(ex.uid) {
        if (ex.autoSpeak && ex.speak != null) {
            delay(250)
            vm.speaker.speak(ex.speak)
        }
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Prompt(ex.prompt)
            ex.hint?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 15.sp) }
        }
        if (ex.speak != null && vm.speaker.ready) {
            Spacer(Modifier.width(8.dp))
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.clip(CircleShape).clickable { vm.speaker.speak(ex.speak) },
            ) {
                Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) { Text("🔊", fontSize = 22.sp) }
            }
        }
    }
    Spacer(Modifier.height(4.dp))
    ex.options.forEachIndexed { i, opt ->
        val state = when {
            fb == null -> if (i == selected) TileState.SELECTED else TileState.NORMAL
            i == ex.correct -> TileState.CORRECT
            i == selected -> TileState.WRONG
            else -> TileState.NORMAL
        }
        AnswerTile(opt, state, Modifier.fillMaxWidth(), enabled = !locked) {
            selected = i
            s.draft = Answer.Index(i)
            SoundFx.tap()
        }
    }
}

// -------------------------------------------------------------------- réponse tapée

@Composable
private fun TypingView(vm: AppViewModel, s: SessionController, ex: Exercise.Typing) {
    var text by remember(ex.uid) { mutableStateOf("") }
    val focus = remember { FocusRequester() }
    val locked = s.feedback != null || s.current?.uid != ex.uid

    LaunchedEffect(ex.uid) {
        if (ex.autoSpeak && ex.speak != null) {
            delay(300)
            vm.speaker.speak(ex.speak)
        }
        delay(350)
        try {
            focus.requestFocus()
        } catch (e: Exception) {
        }
    }

    if (ex.prompt.isNotBlank()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f)) { Prompt(ex.prompt) }
            if (ex.speak != null && vm.speaker.ready) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.clip(CircleShape).clickable { vm.speaker.speak(ex.speak) },
                ) {
                    Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) { Text("🔊", fontSize = 22.sp) }
                }
            }
        }
    } else if (ex.speak != null) {
        SpeakButtons(vm, ex.speak, big = true)
    }

    OutlinedTextField(
        value = text,
        onValueChange = {
            text = it
            s.draft = if (it.isBlank()) null else Answer.Text(it)
        },
        enabled = !locked,
        modifier = Modifier.fillMaxWidth().focusRequester(focus),
        placeholder = { Text(if (ex.answerInEnglish) "Écris en anglais…" else "Écris en français…") },
        textStyle = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface),
        shape = RoundedCornerShape(16.dp),
        minLines = if (ex.sentence) 2 else 1,
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.None,
            autoCorrectEnabled = false,
            imeAction = ImeAction.Done,
        ),
        keyboardActions = KeyboardActions(onDone = {
            if (!locked && text.isNotBlank()) s.check(Answer.Text(text))
        }),
    )
    ex.hint?.let {
        Text("💡 $it", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// -------------------------------------------------------------------- remettre dans l'ordre

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OrderView(vm: AppViewModel, s: SessionController, ex: Exercise.Order) {
    val picked = remember(ex.uid) { mutableStateListOf<Int>() }
    val locked = s.feedback != null || s.current?.uid != ex.uid

    fun publish() {
        s.draft = if (picked.isEmpty()) null else Answer.Tokens(picked.map { ex.bank[it] })
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.weight(1f)) { Prompt(ex.prompt) }
        if (ex.speak != null && vm.speaker.ready) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.clip(CircleShape).clickable { vm.speaker.speak(ex.speak) },
            ) {
                Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) { Text("🔊", fontSize = 22.sp) }
            }
        }
    }

    // Zone de réponse
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(2.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp),
    ) {
        FlowRow(
            Modifier.padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (picked.isEmpty()) {
                Text(
                    "Touche les mots ci-dessous dans le bon ordre",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(6.dp),
                )
            }
            picked.forEachIndexed { pos, bankIndex ->
                WordChip(ex.bank[bankIndex], used = false, enabled = !locked) {
                    picked.removeAt(pos)
                    publish()
                    SoundFx.tap()
                }
            }
        }
    }

    HorizontalDivider(Modifier.padding(vertical = 4.dp))

    // Banque de mots
    FlowRow(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        ex.bank.forEachIndexed { i, word ->
            val used = i in picked
            WordChip(word, used = used, enabled = !locked && !used) {
                picked.add(i)
                publish()
                SoundFx.tap()
            }
        }
    }
}

@Composable
private fun WordChip(word: String, used: Boolean, enabled: Boolean, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (used) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface,
        border = BorderStroke(2.dp, MaterialTheme.colorScheme.outline),
        shadowElevation = if (used) 0.dp else 2.dp,
        modifier = Modifier.clip(RoundedCornerShape(12.dp)).clickable(enabled = enabled) { onClick() },
    ) {
        Text(
            word,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp).alpha(if (used) 0f else 1f),
            fontWeight = FontWeight.SemiBold,
            fontSize = 17.sp,
        )
    }
}

// -------------------------------------------------------------------- repérer l'erreur

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SpotErrorView(s: SessionController, ex: Exercise.SpotError) {
    var selected by remember(ex.uid) { mutableIntStateOf(-1) }
    val fb = s.feedback
    val locked = fb != null || s.current?.uid != ex.uid
    Text(
        "Une seule erreur s'est glissée dans cette phrase. Trouve-la !",
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    FlowRow(
        Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        ex.tokens.forEachIndexed { i, tok ->
            val state = when {
                fb == null -> if (i == selected) TileState.SELECTED else TileState.NORMAL
                i == ex.wrong -> if (fb.correct) TileState.CORRECT else TileState.WRONG
                i == selected -> TileState.SELECTED
                else -> TileState.NORMAL
            }
            AnswerTile(tok, state, enabled = !locked) {
                selected = i
                s.draft = Answer.Index(i)
                SoundFx.tap()
            }
        }
    }
}

// -------------------------------------------------------------------- associer les paires

@Composable
private fun MatchView(vm: AppViewModel, s: SessionController, ex: Exercise.Match) {
    val left = remember(ex.uid) { ex.pairs.indices.shuffled() }
    val right = remember(ex.uid) { ex.pairs.indices.shuffled() }
    var selL by remember(ex.uid) { mutableStateOf<Int?>(null) }
    var selR by remember(ex.uid) { mutableStateOf<Int?>(null) }
    var matched by remember(ex.uid) { mutableStateOf(setOf<Int>()) }
    var wrong by remember(ex.uid) { mutableStateOf<Pair<Int, Int>?>(null) }
    val failed = remember(ex.uid) { mutableSetOf<String>() }
    val locked = s.feedback != null || s.current?.uid != ex.uid

    LaunchedEffect(wrong) {
        if (wrong != null) {
            delay(450)
            wrong = null
        }
    }

    fun attempt(l: Int?, r: Int?) {
        if (l == null || r == null) return
        if (l == r) {
            matched = matched + l
            SoundFx.tap()
            if (matched.size == ex.pairs.size) s.check(Answer.MatchDone(failed.toSet()))
        } else {
            failed += ex.pairs[l].cardId
            wrong = l to r
            SoundFx.softWrong()
        }
        selL = null
        selR = null
    }

    Text("Touche un mot anglais puis sa traduction.", color = MaterialTheme.colorScheme.onSurfaceVariant)
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            left.forEach { idx ->
                val state = when {
                    idx in matched -> TileState.DISABLED
                    wrong?.first == idx -> TileState.WRONG
                    selL == idx -> TileState.SELECTED
                    else -> TileState.NORMAL
                }
                AnswerTile(ex.pairs[idx].left, state, Modifier.fillMaxWidth(), enabled = !locked) {
                    selL = idx
                    vm.speaker.speak(ex.pairs[idx].left)
                    attempt(idx, selR)
                }
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            right.forEach { idx ->
                val state = when {
                    idx in matched -> TileState.DISABLED
                    wrong?.second == idx -> TileState.WRONG
                    selR == idx -> TileState.SELECTED
                    else -> TileState.NORMAL
                }
                AnswerTile(ex.pairs[idx].right, state, Modifier.fillMaxWidth(), enabled = !locked) {
                    selR = idx
                    attempt(selL, idx)
                }
            }
        }
    }
    if (matched.size == ex.pairs.size) {
        Text("🎉 Toutes les paires sont trouvées !", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(), fontWeight = FontWeight.Bold)
    }
}
