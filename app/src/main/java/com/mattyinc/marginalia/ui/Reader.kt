package com.mattyinc.marginalia.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mattyinc.marginalia.data.Idea
import com.mattyinc.marginalia.data.StashWithCount

/**
 * The folio, opened. Same page, more room, and a place to write your own note
 * in the margin. `folio` carries the shared bounds that tie it to the card it grew from.
 */
@Composable
fun Reader(
    idea: Idea,
    stashes: List<StashWithCount>,
    folio: Modifier,
    onClose: () -> Unit,
    onMove: (Long?) -> Unit,
    onNote: (String) -> Unit,
    onDelete: () -> Unit,
) {
    val pigment = pigmentFor(idea.topic)
    val stashName = stashes.firstOrNull { it.stash.id == idea.stashId }?.stash?.name
    var note by remember(idea.id) { mutableStateOf(idea.note) }
    var menuOpen by remember { mutableStateOf(false) }

    Box(
        Modifier.fillMaxSize().systemBarsPadding().imePadding().padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            folio
                .widthIn(max = 700.dp)
                .fillMaxWidth()
                .folioSurface()
                .verticalScroll(rememberScrollState())
                .padding(start = 26.dp, end = 26.dp, top = 18.dp, bottom = 28.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box {
                    Row(
                        Modifier
                            .pressable { menuOpen = true }
                            .clip(CircleShape)
                            .background(Ink.Vellum.copy(alpha = 0.07f))
                            .padding(horizontal = 14.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            Modifier.size(9.dp).clip(CircleShape)
                                .background(if (stashName != null) pigmentFor(stashName) else Ink.VellumDim)
                        )
                        Spacer(Modifier.width(9.dp))
                        Text(stashName ?: "Not kept", style = Type.Ui, color = Ink.Vellum)
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        stashes.forEach { item ->
                            DropdownMenuItem(
                                text = { Text("Keep in ${item.stash.name}", style = Type.Ui) },
                                onClick = {
                                    menuOpen = false
                                    onMove(item.stash.id)
                                },
                            )
                        }
                        if (idea.stashId != null) {
                            DropdownMenuItem(
                                text = { Text("Take out of stash", style = Type.Ui) },
                                onClick = {
                                    menuOpen = false
                                    onMove(null)
                                },
                            )
                        }
                    }
                }
                Spacer(Modifier.weight(1f))
                Box(
                    Modifier
                        .pressable(onClick = onClose)
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Ink.Vellum.copy(alpha = 0.07f)),
                    contentAlignment = Alignment.Center,
                ) {
                    GlyphIcon(Glyph.Close, Ink.Vellum)
                }
            }

            Spacer(Modifier.height(26.dp))
            Text(idea.title, style = Type.Display, color = Ink.Vellum)
            Spacer(Modifier.height(20.dp))

            Column(
                Modifier
                    .fillMaxWidth()
                    .drawBehind {
                        drawLine(pigment, Offset(0f, 0f), Offset(0f, size.height), 1.5.dp.toPx())
                    }
                    .padding(start = 20.dp)
            ) {
                Text(
                    idea.body,
                    style = Type.Reading.copy(fontSize = 21.sp, lineHeight = 33.sp),
                    color = Ink.Vellum.copy(alpha = 0.92f),
                )
                Spacer(Modifier.height(16.dp))
                Text(idea.source, style = Type.Margin.copy(fontSize = 16.sp), color = Ink.VellumDim)
            }

            Spacer(Modifier.height(30.dp))
            Text("Your note", style = Type.UiSmall, color = Ink.VellumDim)
            Spacer(Modifier.height(8.dp))
            BasicTextField(
                value = note,
                onValueChange = {
                    note = it
                    onNote(it)
                },
                textStyle = Type.Margin.copy(fontSize = 19.sp, lineHeight = 27.sp, color = Ink.Gilt),
                cursorBrush = SolidColor(Ink.Gilt),
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { inner ->
                    Box {
                        if (note.isEmpty()) {
                            Text(
                                "Write in the margin",
                                style = Type.Margin.copy(fontSize = 19.sp, lineHeight = 27.sp),
                                color = Ink.Gilt.copy(alpha = 0.45f),
                            )
                        }
                        inner()
                    }
                },
            )

            if (idea.own) {
                Spacer(Modifier.height(28.dp))
                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    PillButton("Delete idea", onDelete, tint = Ink.VellumDim)
                }
            }
        }
    }
}
