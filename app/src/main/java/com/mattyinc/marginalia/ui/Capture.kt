package com.mattyinc.marginalia.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mattyinc.marginalia.data.StashWithCount

/** Write down an idea from whatever you are reading. */
@Composable
fun CaptureSheet(
    stashes: List<StashWithCount>,
    onDismiss: () -> Unit,
    onSave: (title: String, body: String, source: String, stash: String) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var title by rememberSaveable { mutableStateOf("") }
    var body by rememberSaveable { mutableStateOf("") }
    var source by rememberSaveable { mutableStateOf("") }
    var stash by rememberSaveable { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Ink.Folio,
        contentColor = Ink.Vellum,
        scrimColor = Ink.Deep.copy(alpha = 0.7f),
        shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .imePadding()
                .padding(start = 26.dp, end = 26.dp, bottom = 24.dp)
        ) {
            Text("New idea", style = Type.Title, color = Ink.Vellum)
            Spacer(Modifier.height(20.dp))
            Field("Title", "The idea in a few words", title, { title = it }, Type.TitleSmall)
            Field("Idea", "Two to four sentences, in your own words", body, { body = it }, Type.Reading, singleLine = false)
            Field("Source", "Book, chapter or person", source, { source = it }, Type.Margin.copy(fontSize = 17.sp))
            Field("Stash", "Where to keep it", stash, { stash = it }, Type.Ui.copy(fontSize = 17.sp))
            if (stashes.isNotEmpty()) {
                Row(
                    Modifier.horizontalScroll(rememberScrollState()).padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    stashes.forEach { item ->
                        val picked = stash.trim().equals(item.stash.name, ignoreCase = true)
                        Box(
                            Modifier
                                .pressable { stash = item.stash.name }
                                .clip(RoundedCornerShape(50))
                                .background(
                                    pigmentFor(item.stash.name).copy(alpha = if (picked) 0.38f else 0.14f)
                                )
                                .padding(horizontal = 14.dp, vertical = 9.dp)
                        ) {
                            Text(item.stash.name, style = Type.UiSmall, color = Ink.Vellum)
                        }
                    }
                }
            }
            Spacer(Modifier.height(18.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                PillButton("Cancel", onDismiss)
                Spacer(Modifier.padding(6.dp))
                PillButton(
                    label = "Save idea",
                    onClick = { onSave(title, body, source, stash) },
                    filled = true,
                    enabled = title.isNotBlank() && body.isNotBlank(),
                )
            }
        }
    }
}

@Composable
private fun Field(
    label: String,
    hint: String,
    value: String,
    onChange: (String) -> Unit,
    style: TextStyle,
    singleLine: Boolean = true,
) {
    Column(Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
        Text(label, style = Type.UiSmall, color = Ink.VellumDim)
        Spacer(Modifier.height(6.dp))
        BasicTextField(
            value = value,
            onValueChange = onChange,
            singleLine = singleLine,
            minLines = if (singleLine) 1 else 3,
            textStyle = style.copy(color = Ink.Vellum),
            cursorBrush = SolidColor(Ink.Gilt),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Ink.Vellum.copy(alpha = 0.06f))
                .padding(horizontal = 16.dp, vertical = 14.dp),
            decorationBox = { inner ->
                Box {
                    if (value.isEmpty()) {
                        Text(hint, style = style, color = Ink.VellumDim.copy(alpha = 0.6f))
                    }
                    inner()
                }
            },
        )
    }
}
