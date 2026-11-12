package com.mattyinc.marginalia.ui

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mattyinc.marginalia.data.Scene
import com.mattyinc.marginalia.data.Story
import java.util.Locale
import kotlin.math.roundToInt

/** Reads captions aloud with the voice already on the device. */
class Narrator(context: Context) {
    var ready by mutableStateOf(false)
        private set
    var speaking by mutableStateOf(false)
        private set

    private lateinit var tts: TextToSpeech

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                runCatching {
                    tts.setLanguage(Locale.US)
                    tts.setSpeechRate(0.96f)
                    tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                        override fun onStart(utteranceId: String?) {
                            speaking = true
                        }

                        override fun onDone(utteranceId: String?) {
                            speaking = false
                        }

                        @Deprecated("Deprecated in Java")
                        override fun onError(utteranceId: String?) {
                            speaking = false
                        }
                    })
                }
                ready = true
            }
        }
    }

    fun speak(text: String) {
        if (!ready) return
        speaking = true
        val result = runCatching { tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "scene") }
        if (result.getOrNull() != TextToSpeech.SUCCESS) speaking = false
    }

    fun stop() {
        speaking = false
        if (ready) runCatching { tts.stop() }
    }

    fun shutdown() {
        runCatching {
            tts.stop()
            tts.shutdown()
        }
    }
}

private fun stage(p: Float, start: Float, end: Float): Float =
    ((p - start) / (end - start)).coerceIn(0f, 1f)

private fun ease(x: Float): Float = FastOutSlowInEasing.transform(x.coerceIn(0f, 1f))

/**
 * Plays a story the way a phone plays stories: scenes advance on their own, tap the
 * right side to skip ahead, the left to go back, and hold anywhere to stop the clock.
 */
@Composable
fun StoryPlayer(
    story: Story,
    modifier: Modifier,
    onClose: () -> Unit,
    onFinished: () -> Unit,
    onKeepTakeaway: () -> Unit,
) {
    val context = LocalContext.current
    val art = artFor(story)
    val pigment = art.color
    val scenes = story.played
    val lastIndex = scenes.lastIndex

    var index by remember(story.id) { mutableIntStateOf(0) }
    var progress by remember { mutableFloatStateOf(0f) }
    var paused by remember { mutableStateOf(false) }
    var muted by remember { mutableStateOf(false) }
    var kept by remember(story.id) { mutableStateOf(false) }

    val narrator = remember { Narrator(context) }
    DisposableEffect(Unit) { onDispose { narrator.shutdown() } }

    LaunchedEffect(index, narrator.ready, muted) {
        if (muted) narrator.stop() else narrator.speak(scenes[index].say)
    }

    LaunchedEffect(index) {
        if (index == lastIndex) onFinished()
        val duration = scenes[index].durationMs.toFloat()
        var elapsed = 0f
        var last = withFrameNanos { it }
        progress = 0f
        while (true) {
            val now = withFrameNanos { it }
            if (!paused) elapsed += (now - last) / 1_000_000f
            last = now
            progress = (elapsed / duration).coerceIn(0f, 1f)
            // Wait for the voice to finish its sentence, within reason.
            val voiceDone = muted || !narrator.speaking || elapsed > duration + 9000f
            if (elapsed >= duration && voiceDone && index < lastIndex) {
                index += 1
                break
            }
            if (elapsed >= duration && index == lastIndex) break
        }
    }

    Box(
        modifier
            .fillMaxSize()
            .background(Ink.Deep)
            .drawBehind {
                drawRect(
                    Brush.radialGradient(
                        colors = listOf(pigment.copy(alpha = 0.30f), Color.Transparent),
                        center = Offset(size.width * 0.15f, size.height * 0.2f),
                        radius = size.maxDimension * 0.8f,
                    )
                )
            }
            .pointerInput(story.id) {
                detectTapGestures(
                    onPress = {
                        paused = true
                        tryAwaitRelease()
                        paused = false
                    },
                    onLongPress = { },
                    onTap = { at ->
                        if (at.x < size.width * 0.3f) {
                            if (index > 0) index -= 1
                        } else if (index < lastIndex) {
                            index += 1
                        }
                    },
                )
            }
    ) {
        Column(Modifier.fillMaxSize().systemBarsPadding().padding(horizontal = 20.dp, vertical = 14.dp)) {
            // One segment per scene, filling as it plays.
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                scenes.forEachIndexed { i, _ ->
                    Box(
                        Modifier
                            .weight(1f)
                            .height(3.dp)
                            .clip(CircleShape)
                            .background(Ink.Hairline)
                            .drawBehind {
                                val filled = when {
                                    i < index -> 1f
                                    i == index -> progress
                                    else -> 0f
                                }
                                drawRect(Ink.Vellum, size = Size(size.width * filled, size.height))
                            }
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(story.title, style = Type.Ui, color = Ink.VellumDim, modifier = Modifier.weight(1f), maxLines = 1)
                PillButton(if (muted) "Sound on" else "Sound off", { muted = !muted })
                Spacer(Modifier.width(8.dp))
                Box(
                    Modifier
                        .pressable(onClick = onClose)
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Ink.Vellum.copy(alpha = 0.07f)),
                    contentAlignment = Alignment.Center,
                ) {
                    GlyphIcon(Glyph.Close, Ink.Vellum)
                }
            }

            AnimatedContent(
                targetState = index,
                transitionSpec = {
                    (fadeIn(tween(320)) + scaleIn(tween(420), initialScale = 0.97f)) togetherWith fadeOut(tween(180))
                },
                label = "scene",
                modifier = Modifier.weight(1f).fillMaxWidth(),
            ) { shown ->
                val scene = scenes[shown]
                // A scene that is on its way out holds its final frame.
                val p: () -> Float = { if (shown == index) progress else 1f }
                SceneLayout(
                    visual = { SceneVisual(story, scene, pigment, p) },
                    caption = {
                        if (scene.kind == "takeaway") {
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                PillButton(
                                    label = if (kept) "Kept in ${story.topic}" else "Keep this idea",
                                    onClick = {
                                        if (!kept) {
                                            kept = true
                                            onKeepTakeaway()
                                        }
                                    },
                                    filled = !kept,
                                )
                                PillButton("Done", onClose)
                            }
                        } else {
                            Text(
                                scene.say,
                                style = Type.Reading.copy(fontSize = 21.sp, lineHeight = 31.sp),
                                color = Ink.Vellum.copy(alpha = 0.9f),
                                modifier = Modifier.graphicsLayer { alpha = stage(p(), 0f, 0.08f) },
                            )
                        }
                    },
                )
            }
        }
    }
}

/** Drawing above the caption on a tall screen, beside it on a wide one. */
@Composable
private fun SceneLayout(visual: @Composable () -> Unit, caption: @Composable () -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wide = maxWidth > 760.dp && maxWidth > maxHeight
        if (wide) {
            Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1.15f).fillMaxHeight().padding(28.dp), contentAlignment = Alignment.CenterStart) {
                    visual()
                }
                Box(Modifier.weight(0.85f).padding(28.dp), contentAlignment = Alignment.CenterStart) {
                    caption()
                }
            }
        } else {
            Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier.weight(1f).widthIn(max = 640.dp).fillMaxWidth().padding(vertical = 18.dp, horizontal = 6.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    visual()
                }
                Box(Modifier.widthIn(max = 640.dp).fillMaxWidth().padding(start = 6.dp, end = 6.dp, bottom = 22.dp)) {
                    caption()
                }
            }
        }
    }
}

@Composable
private fun SceneVisual(story: Story, scene: Scene, pigment: Color, p: () -> Float) {
    when (scene.kind) {
        "title" -> Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
            val cover = artFor(story).cover
            if (cover != null) {
                // The cover settles into place as the story opens.
                Image(
                    painter = painterResource(cover),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .aspectRatio(0.8f, matchHeightConstraintsFirst = true)
                        .clip(FolioShape)
                        .graphicsLayer {
                            val s = 1.07f - 0.07f * ease(stage(p(), 0f, 0.9f))
                            scaleX = s
                            scaleY = s
                        },
                )
                Spacer(Modifier.height(22.dp))
            }
            Text(story.topic, style = Type.Margin.copy(fontSize = 18.sp), color = pigment)
            Spacer(Modifier.height(6.dp))
            Text(
                story.title,
                style = Type.Display.copy(fontSize = 44.sp, lineHeight = 47.sp),
                color = Ink.Vellum,
                modifier = Modifier.graphicsLayer {
                    val a = ease(stage(p(), 0f, 0.18f))
                    alpha = a
                    translationY = (1f - a) * 24.dp.toPx()
                },
            )
            Spacer(Modifier.height(16.dp))
            Box(
                Modifier.fillMaxWidth().height(2.dp).drawBehind {
                    drawLine(
                        pigment, Offset(0f, 0f), Offset(size.width * ease(stage(p(), 0.1f, 0.6f)), 0f),
                        2.dp.toPx(), StrokeCap.Round,
                    )
                }
            )
        }

        "words" -> {
            val parts = remember(scene.words) { scene.words.split(' ').filter { it.isNotBlank() } }
            val step = 0.5f / parts.size.coerceAtLeast(1)
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(11.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                parts.forEachIndexed { i, word ->
                    Text(
                        word,
                        style = Type.Display.copy(fontSize = 44.sp, lineHeight = 50.sp),
                        color = if (word.endsWith('.') || word.endsWith('?')) Ink.Vellum else Ink.Vellum.copy(alpha = 0.92f),
                        modifier = Modifier.graphicsLayer {
                            val a = ease(stage(p(), i * step, i * step + 0.14f))
                            alpha = a
                            translationY = (1f - a) * 20.dp.toPx()
                        },
                    )
                }
            }
        }

        "number" -> {
            val shown = scene.from + ((scene.to - scene.from) * ease(stage(p(), 0f, 0.5f))).roundToInt()
            val digits = if (scene.plain) shown.toString() else String.format(Locale.US, "%,d", shown)
            Column(Modifier.fillMaxWidth()) {
                Text(
                    scene.prefix + digits + scene.suffix,
                    style = Type.Display.copy(fontSize = 84.sp, lineHeight = 88.sp),
                    color = pigment,
                )
                Spacer(Modifier.height(6.dp))
                Text(scene.label, style = Type.Margin.copy(fontSize = 20.sp, lineHeight = 26.sp), color = Ink.Vellum)
            }
        }

        "timeline" -> Row(
            Modifier.fillMaxWidth().drawBehind {
                val y = 8.dp.toPx()
                drawLine(Ink.Hairline, Offset(0f, y), Offset(size.width, y), 2.dp.toPx())
                drawLine(
                    pigment, Offset(0f, y), Offset(size.width * ease(stage(p(), 0f, 0.75f)), y),
                    2.dp.toPx(), StrokeCap.Round,
                )
            },
        ) {
            val n = scene.marks.size.coerceAtLeast(1)
            scene.marks.forEachIndexed { i, mark ->
                Column(
                    Modifier.weight(1f).padding(end = 10.dp).graphicsLayer {
                        val at = 0.75f * (i + 0.2f) / n
                        val a = ease(stage(p(), at, at + 0.12f))
                        alpha = a
                        translationY = (1f - a) * 14.dp.toPx()
                    },
                ) {
                    Box(Modifier.size(16.dp).clip(CircleShape).background(pigment))
                    Spacer(Modifier.height(14.dp))
                    Text(mark.label, style = Type.Title, color = Ink.Vellum)
                    Spacer(Modifier.height(4.dp))
                    Text(mark.note, style = Type.Ui, color = Ink.VellumDim)
                }
            }
        }

        "bars" -> {
            val max = scene.bars.maxOfOrNull { it.value }?.takeIf { it > 0f } ?: 1f
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(28.dp)) {
                scene.bars.forEachIndexed { i, bar ->
                    Column {
                        Text(bar.display, style = Type.Display.copy(fontSize = 40.sp, lineHeight = 44.sp), color = Ink.Vellum)
                        Text(bar.label, style = Type.Ui, color = Ink.VellumDim)
                        Spacer(Modifier.height(10.dp))
                        Box(
                            Modifier.fillMaxWidth().height(14.dp).drawBehind {
                                val r = CornerRadius(size.height / 2)
                                drawRoundRect(Ink.Hairline, cornerRadius = r)
                                val grow = ease(stage(p(), 0.05f + i * 0.2f, 0.5f + i * 0.2f))
                                drawRoundRect(
                                    pigment,
                                    size = Size((size.width * bar.value / max * grow).coerceAtLeast(size.height * grow), size.height),
                                    cornerRadius = r,
                                )
                            }
                        )
                    }
                }
            }
        }

        "curve" -> Column(Modifier.fillMaxWidth()) {
            Canvas(Modifier.fillMaxWidth().aspectRatio(1.6f)) {
                val values = scene.values
                if (values.size >= 2) {
                    val top = values.max()
                    val stepX = size.width / (values.size - 1)
                    fun point(i: Int) = Offset(i * stepX, size.height - (values[i] / top) * size.height * 0.94f)
                    val line = Path().apply {
                        moveTo(point(0).x, point(0).y)
                        for (i in 1 until values.size) {
                            val a = point(i - 1)
                            val b = point(i)
                            val mid = (a.x + b.x) / 2
                            cubicTo(mid, a.y, mid, b.y, b.x, b.y)
                        }
                    }
                    val fill = Path().apply {
                        addPath(line)
                        lineTo(size.width, size.height)
                        lineTo(0f, size.height)
                        close()
                    }
                    val reveal = ease(stage(p(), 0.05f, 0.7f))
                    drawLine(Ink.Hairline, Offset(0f, size.height), Offset(size.width, size.height), 1.dp.toPx())
                    clipRect(right = size.width * reveal + 6.dp.toPx()) {
                        drawPath(
                            fill,
                            Brush.verticalGradient(listOf(pigment.copy(alpha = 0.32f), Color.Transparent)),
                        )
                        drawPath(
                            line, pigment,
                            style = Stroke(3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
                        )
                    }
                    for (i in values.indices) {
                        if (i * stepX <= size.width * reveal + 1f) {
                            drawCircle(Ink.Vellum, 5.dp.toPx(), point(i))
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth()) {
                Text(scene.startLabel, style = Type.Ui, color = Ink.VellumDim, modifier = Modifier.weight(1f))
                Text(
                    scene.endLabel,
                    style = Type.TitleSmall,
                    color = Ink.Vellum,
                    textAlign = TextAlign.End,
                    modifier = Modifier.graphicsLayer { alpha = stage(p(), 0.6f, 0.75f) },
                )
            }
        }

        "list" -> Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            val n = scene.items.size.coerceAtLeast(1)
            scene.items.forEachIndexed { i, item ->
                Row(
                    Modifier.graphicsLayer {
                        val at = 0.6f * i / n
                        val a = ease(stage(p(), at, at + 0.14f))
                        alpha = a
                        translationX = (1f - a) * -18.dp.toPx()
                    },
                ) {
                    Box(Modifier.padding(top = 12.dp).size(9.dp).clip(CircleShape).background(pigment))
                    Spacer(Modifier.width(16.dp))
                    Text(item, style = Type.Title.copy(fontSize = 29.sp, lineHeight = 35.sp), color = Ink.Vellum)
                }
            }
        }

        else -> Column(
            Modifier
                .fillMaxWidth()
                .drawBehind {
                    drawLine(
                        pigment, Offset(0f, 0f), Offset(0f, size.height * ease(stage(p(), 0f, 0.3f))),
                        2.dp.toPx(), StrokeCap.Round,
                    )
                }
                .padding(start = 22.dp)
        ) {
            // The takeaway: the one idea the story leaves you with.
            Text(
                scene.say,
                style = Type.Display.copy(fontSize = 33.sp, lineHeight = 41.sp),
                color = Ink.Vellum,
                modifier = Modifier.graphicsLayer { alpha = ease(stage(p(), 0.05f, 0.25f)) },
            )
        }
    }
}
