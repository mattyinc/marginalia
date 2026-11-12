package com.mattyinc.marginalia.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mattyinc.marginalia.Progress
import com.mattyinc.marginalia.data.Idea

val FolioShape = RoundedCornerShape(26.dp)
val FolioShapeSmall = RoundedCornerShape(20.dp)

/** Which folio is currently open in the reader, and where it was opened from. */
data class OpenIdea(val key: String, val id: Long)

val LocalSharedScope = staticCompositionLocalOf<SharedTransitionScope> {
    error("No SharedTransitionScope provided")
}
val LocalOpenKey = compositionLocalOf<String?> { null }

val FolioBounds = BoundsTransform { _, _ -> spring(dampingRatio = 0.88f, stiffness = 300f) }

/**
 * A folio that can grow into the reader. While it is open, the folio leaves this
 * spot (the reader is the same object, moved), and the gap it leaves keeps its size
 * so nothing around it shifts.
 */
@Composable
fun SharedFolio(
    key: String,
    modifier: Modifier = Modifier,
    content: @Composable (Modifier) -> Unit,
) {
    val shared = LocalSharedScope.current
    val isOpen = LocalOpenKey.current == key
    val density = LocalDensity.current
    var lastHeight by remember { mutableIntStateOf(0) }
    val hold = if (isOpen && lastHeight > 0) {
        Modifier.heightIn(min = with(density) { lastHeight.toDp() })
    } else {
        Modifier
    }
    Box(
        modifier
            .onSizeChanged { if (it.height > 0) lastHeight = it.height }
            .then(hold)
    ) {
        with(shared) {
            AnimatedVisibility(visible = !isOpen, enter = fadeIn(), exit = fadeOut()) {
                content(
                    Modifier.sharedBounds(
                        sharedContentState = rememberSharedContentState(key = key),
                        animatedVisibilityScope = this@AnimatedVisibility,
                        boundsTransform = FolioBounds,
                        clipInOverlayDuringTransition = OverlayClip(FolioShape),
                    )
                )
            }
        }
    }
}

/** The page surface every idea sits on. */
fun Modifier.folioSurface(shape: RoundedCornerShape = FolioShape): Modifier = this
    .clip(shape)
    .background(Brush.verticalGradient(listOf(Ink.FolioHigh, Ink.Folio)))
    .border(1.dp, Ink.Hairline, shape)

/**
 * The idea as a page: the topic runs up the margin beside a pigment rule, and the
 * text sits to the right of it, the way a note sits beside a ruled column.
 */
@Composable
fun FolioPage(
    idea: Idea,
    modifier: Modifier = Modifier,
    large: Boolean = true,
    bodyLines: Int = Int.MAX_VALUE,
    fill: Boolean = false,
) {
    val pigment = pigmentFor(idea.topic)
    val gutter = if (large) 34.dp else 26.dp
    Row(modifier.padding(if (large) 22.dp else 16.dp)) {
        Box(Modifier.width(gutter), contentAlignment = Alignment.TopStart) {
            Text(
                text = idea.topic,
                style = Type.Margin.copy(fontSize = if (large) 15.sp else 13.sp),
                color = pigment,
                maxLines = 1,
                modifier = Modifier.alongMargin(),
            )
        }
        Column(
            Modifier
                .weight(1f)
                .then(if (fill) Modifier.fillMaxHeight() else Modifier)
                .drawBehind {
                    drawLine(
                        color = pigment,
                        start = Offset(0f, 0f),
                        end = Offset(0f, size.height),
                        strokeWidth = 1.5.dp.toPx(),
                    )
                }
                .padding(start = if (large) 18.dp else 14.dp)
        ) {
            Text(
                text = idea.title,
                style = if (large) Type.Title else Type.TitleSmall,
                color = Ink.Vellum,
            )
            Spacer(Modifier.height(if (large) 14.dp else 8.dp))
            Text(
                text = idea.body,
                style = if (large) Type.Reading else Type.ReadingSmall,
                color = Ink.Vellum.copy(alpha = 0.88f),
                maxLines = bodyLines,
                overflow = TextOverflow.Ellipsis,
                modifier = if (fill) Modifier.weight(1f) else Modifier,
            )
            Spacer(Modifier.height(if (large) 14.dp else 10.dp))
            Text(
                text = idea.source,
                style = Type.Margin.copy(fontSize = if (large) 15.sp else 13.sp),
                color = Ink.VellumDim,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Night ground with a wash of the current pigment, like lamplight on the desk. */
@Composable
fun Ambient(tint: Color, modifier: Modifier = Modifier) {
    val wash by animateColorAsState(tint, tween(900), label = "wash")
    Canvas(modifier.fillMaxSize()) {
        drawRect(Ink.Night)
        drawRect(
            Brush.radialGradient(
                colors = listOf(wash.copy(alpha = 0.26f), Color.Transparent),
                center = Offset(size.width * 0.88f, size.height * 0.06f),
                radius = size.maxDimension * 0.72f,
            )
        )
        drawRect(
            Brush.radialGradient(
                colors = listOf(Ink.Gilt.copy(alpha = 0.07f), Color.Transparent),
                center = Offset(size.width * 0.05f, size.height),
                radius = size.maxDimension * 0.55f,
            )
        )
    }
}

/** Gold fills the ring as today's ideas are read. */
@Composable
fun GoalRing(progress: Progress, diameter: Dp, modifier: Modifier = Modifier) {
    val fraction by animateFloatAsState(
        targetValue = (progress.readToday.toFloat() / progress.goal).coerceIn(0f, 1f),
        animationSpec = Motion.flick(),
        label = "goal",
    )
    Box(modifier.size(diameter), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = size.minDimension * 0.085f
            val arcSize = Size(size.width - stroke, size.height - stroke)
            val topLeft = Offset(stroke / 2, stroke / 2)
            drawArc(
                color = Ink.Hairline, startAngle = -90f, sweepAngle = 360f, useCenter = false,
                topLeft = topLeft, size = arcSize, style = Stroke(stroke),
            )
            drawArc(
                color = Ink.Gilt, startAngle = -90f, sweepAngle = 360f * fraction, useCenter = false,
                topLeft = topLeft, size = arcSize, style = Stroke(stroke, cap = StrokeCap.Round),
            )
        }
        Text(
            text = progress.readToday.toString(),
            style = Type.Title.copy(fontSize = (diameter.value * 0.38f).sp, lineHeight = (diameter.value * 0.42f).sp),
            color = Ink.Vellum,
        )
    }
}

enum class Glyph { Plus, Close, Back }

/** The three marks the app needs, drawn as pen strokes. */
@Composable
fun GlyphIcon(glyph: Glyph, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier.size(20.dp)) {
        val w = size.width
        val stroke = 1.8.dp.toPx()
        fun line(x1: Float, y1: Float, x2: Float, y2: Float) = drawLine(
            color, Offset(x1 * w, y1 * w), Offset(x2 * w, y2 * w), stroke, StrokeCap.Round,
        )
        when (glyph) {
            Glyph.Plus -> {
                line(0.5f, 0.12f, 0.5f, 0.88f)
                line(0.12f, 0.5f, 0.88f, 0.5f)
            }
            Glyph.Close -> {
                line(0.2f, 0.2f, 0.8f, 0.8f)
                line(0.8f, 0.2f, 0.2f, 0.8f)
            }
            Glyph.Back -> {
                line(0.62f, 0.15f, 0.28f, 0.5f)
                line(0.28f, 0.5f, 0.62f, 0.85f)
            }
        }
    }
}

/** A quiet pill button. `filled` is the one gold button a screen is allowed. */
@Composable
fun PillButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    filled: Boolean = false,
    enabled: Boolean = true,
    tint: Color = Ink.Vellum,
) {
    val shape = RoundedCornerShape(50)
    Box(
        modifier
            .pressable(enabled = enabled, onClick = onClick)
            .clip(shape)
            .then(
                if (filled) Modifier.background(if (enabled) Ink.Gilt else Ink.Gilt.copy(alpha = 0.3f))
                else Modifier
                    .background(Ink.Vellum.copy(alpha = 0.06f))
                    .border(1.dp, Ink.Hairline, shape)
            )
            .heightIn(min = 48.dp)
            .padding(horizontal = 22.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = Type.Ui, color = if (filled) Ink.Night else tint)
    }
}

@Composable
fun EmptyNote(title: String, body: String, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, style = Type.Title, color = Ink.Vellum)
        Spacer(Modifier.height(8.dp))
        Text(body, style = Type.Margin.copy(fontSize = 16.sp, lineHeight = 22.sp), color = Ink.VellumDim)
    }
}
