package com.mattyinc.marginalia.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import com.mattyinc.marginalia.Progress
import com.mattyinc.marginalia.data.Idea
import com.mattyinc.marginalia.data.StashWithCount
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

/** Lets the keyboard throw the top card the same way a finger does. */
class StackCommands {
    var fling: ((direction: Int) -> Unit)? = null
    var open: (() -> Unit)? = null
}

@Composable
fun TodayScreen(
    feed: List<Idea>?,
    progress: Progress,
    stashes: List<StashWithCount>,
    expanded: Boolean,
    padding: PaddingValues,
    commands: StackCommands,
    onOpen: (Idea) -> Unit,
    onKeep: (Idea) -> Unit,
    onPass: (Idea) -> Unit,
    onReturnPassed: () -> Unit,
    onCapture: () -> Unit,
    onStash: (Long) -> Unit,
) {
    Row(Modifier.fillMaxSize().padding(padding)) {
        Column(Modifier.weight(1.25f).fillMaxHeight()) {
            TodayHeader(progress = progress, showRing = !expanded)
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                when {
                    feed == null -> Unit
                    feed.isEmpty() -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        EmptyNote(
                            title = "You have read every idea",
                            body = "Bring back the ones you passed, or write one of your own.",
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            PillButton("Bring back passed", onReturnPassed)
                            PillButton("Write an idea", onCapture, filled = true)
                        }
                    }
                    else -> IdeaStack(
                        ideas = feed,
                        commands = commands,
                        onOpen = onOpen,
                        onKeep = onKeep,
                        onPass = onPass,
                    )
                }
            }
        }
        if (expanded) {
            TodaySidePanel(
                progress = progress,
                stashes = stashes,
                onStash = onStash,
                modifier = Modifier.weight(0.75f).fillMaxHeight(),
            )
        }
    }
}

@Composable
private fun TodayHeader(progress: Progress, showRing: Boolean) {
    val date = remember {
        LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.getDefault()))
    }
    Row(
        Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text("Today", style = Type.Display, color = Ink.Vellum)
            Text(date, style = Type.Margin.copy(fontSize = 16.sp), color = Ink.VellumDim)
        }
        if (showRing) {
            Column(horizontalAlignment = Alignment.End) {
                GoalRing(progress, diameter = 52.dp)
                Spacer(Modifier.height(4.dp))
                Text(streakLine(progress), style = Type.UiSmall, color = Ink.VellumDim)
            }
        }
    }
}

private fun streakLine(progress: Progress): String = when {
    progress.streak > 1 -> "${progress.streak} days running"
    progress.streak == 1 -> "1 day so far"
    else -> "of ${progress.goal} today"
}

@Composable
private fun TodaySidePanel(
    progress: Progress,
    stashes: List<StashWithCount>,
    onStash: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .padding(top = 28.dp, end = 28.dp, bottom = 28.dp)
            .folioSurface()
            .verticalScroll(rememberScrollState())
            .padding(28.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            GoalRing(progress, diameter = 132.dp)
            Spacer(Modifier.width(22.dp))
            Column {
                Text("${progress.readToday} of ${progress.goal}", style = Type.Title, color = Ink.Vellum)
                Text("ideas read today", style = Type.Ui, color = Ink.VellumDim)
                Spacer(Modifier.height(10.dp))
                Text(
                    text = when {
                        progress.streak > 1 -> "${progress.streak} days running"
                        progress.streak == 1 -> "First day of a streak"
                        else -> "Read ${progress.goal} to start a streak"
                    },
                    style = Type.Margin,
                    color = Ink.Gilt,
                )
            }
        }
        Spacer(Modifier.height(30.dp))
        Text("Stashes", style = Type.TitleSmall, color = Ink.Vellum)
        Spacer(Modifier.height(6.dp))
        if (stashes.isEmpty()) {
            Text(
                "Swipe an idea to the right and it is kept in a stash named after its topic.",
                style = Type.Margin.copy(fontSize = 16.sp, lineHeight = 22.sp),
                color = Ink.VellumDim,
            )
        }
        stashes.forEach { item ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .pressable { onStash(item.stash.id) }
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(10.dp).clip(CircleShape).background(pigmentFor(item.stash.name)))
                Spacer(Modifier.width(14.dp))
                Text(item.stash.name, style = Type.Reading, color = Ink.Vellum, modifier = Modifier.weight(1f))
                Text(item.ideaCount.toString(), style = Type.Ui, color = Ink.VellumDim)
            }
        }
    }
}

/**
 * The stack of folios. The top one follows the finger exactly; letting go hands the
 * finger's speed to a spring, and where the throw would coast to decides whether the
 * idea is kept (right), passed (left) or comes back to rest.
 */
@Composable
fun IdeaStack(
    ideas: List<Idea>,
    commands: StackCommands,
    onOpen: (Idea) -> Unit,
    onKeep: (Idea) -> Unit,
    onPass: (Idea) -> Unit,
    modifier: Modifier = Modifier,
) {
    val top = ideas.first()
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val keep by rememberUpdatedState(onKeep)
    val pass by rememberUpdatedState(onPass)
    val open by rememberUpdatedState(onOpen)

    val dragX = remember(top.id) { Animatable(0f) }
    val dragY = remember(top.id) { Animatable(0f) }
    var committed by remember(top.id) { mutableStateOf(false) }
    var cardWidthPx by remember { mutableFloatStateOf(1f) }
    var areaWidthPx by remember { mutableFloatStateOf(1f) }

    fun commit(direction: Int, velocity: Float) {
        if (committed) return
        committed = true
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        val idea = top
        scope.launch {
            var handedOff = false
            dragX.animateTo(
                targetValue = direction * areaWidthPx * 1.5f,
                animationSpec = Motion.settle(),
                initialVelocity = velocity,
            ) {
                // Hand over as soon as the folio has left the page, not when the spring rests.
                if (!handedOff && abs(value) > areaWidthPx) {
                    handedOff = true
                    if (direction > 0) keep(idea) else pass(idea)
                }
            }
            if (!handedOff) {
                if (direction > 0) keep(idea) else pass(idea)
            }
        }
    }

    fun comeBack(velocityX: Float, velocityY: Float) {
        scope.launch { dragX.animateTo(0f, Motion.flick(), velocityX) }
        scope.launch { dragY.animateTo(0f, Motion.flick(), velocityY) }
    }

    SideEffect {
        commands.fling = { direction -> commit(direction, direction * 2400f) }
        commands.open = { open(top) }
    }

    Column(modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        BoxWithConstraints(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .onSizeChanged { areaWidthPx = it.width.toFloat() },
            contentAlignment = Alignment.Center,
        ) {
            val cardWidth: Dp = min(maxWidth - 48.dp, 540.dp)
            val cardHeight: Dp = min(maxHeight - 44.dp, cardWidth * 1.38f)
            val visible = ideas.take(3)

            visible.asReversed().forEach { idea ->
                key(idea.id) {
                    val depth = visible.indexOf(idea)
                    if (depth == 0) {
                        Box(
                            Modifier
                                .size(cardWidth, cardHeight)
                                .onSizeChanged { cardWidthPx = it.width.toFloat() }
                                .pointerInput(idea.id) {
                                    val tracker = VelocityTracker()
                                    detectDragGestures(
                                        onDragStart = {
                                            tracker.resetTracking()
                                            // Catch the folio mid-flight if it was coming back.
                                            scope.launch { dragX.stop() }
                                            scope.launch { dragY.stop() }
                                        },
                                        onDragCancel = { comeBack(0f, 0f) },
                                        onDragEnd = {
                                            val v = tracker.calculateVelocity()
                                            val landing = dragX.value + Motion.project(v.x)
                                            val threshold = cardWidthPx * 0.5f
                                            when {
                                                landing > threshold -> commit(1, v.x)
                                                landing < -threshold -> commit(-1, v.x)
                                                else -> comeBack(v.x, v.y)
                                            }
                                        },
                                    ) { change, amount ->
                                        change.consume()
                                        tracker.addPosition(change.uptimeMillis, change.position)
                                        if (!committed) {
                                            scope.launch {
                                                dragX.snapTo(dragX.value + amount.x)
                                                // Sideways is the gesture; up and down only gives a little.
                                                dragY.snapTo(dragY.value + amount.y * 0.3f)
                                            }
                                        }
                                    }
                                }
                                .graphicsLayer {
                                    translationX = dragX.value
                                    translationY = dragY.value
                                    rotationZ = dragX.value / areaWidthPx * 13f
                                    transformOrigin = TransformOrigin(0.5f, 1.25f)
                                }
                        ) {
                            SharedFolio(key = "today-${idea.id}", modifier = Modifier.fillMaxSize()) { shared ->
                                FolioPage(
                                    idea = idea,
                                    fill = true,
                                    modifier = shared
                                        .fillMaxSize()
                                        .pressable { open(idea) }
                                        .folioSurface(),
                                )
                            }
                            SwipeStamp(
                                label = "Keep",
                                color = Ink.Gilt,
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(18.dp)
                                    .graphicsLayer {
                                        alpha = (dragX.value / (cardWidthPx * 0.4f)).coerceIn(0f, 1f)
                                    },
                            )
                            SwipeStamp(
                                label = "Pass",
                                color = Ink.Vellum,
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(18.dp)
                                    .graphicsLayer {
                                        alpha = (-dragX.value / (cardWidthPx * 0.4f)).coerceIn(0f, 1f)
                                    },
                            )
                        }
                    } else {
                        // Folios underneath rise to meet you as the top one leaves.
                        FolioPage(
                            idea = idea,
                            fill = true,
                            modifier = Modifier
                                .size(cardWidth, cardHeight)
                                .graphicsLayer {
                                    val lift = (abs(dragX.value) / (cardWidthPx * 0.5f)).coerceIn(0f, 1f)
                                    val d = depth - lift
                                    val s = 1f - 0.06f * d
                                    scaleX = s
                                    scaleY = s
                                    translationY = 20.dp.toPx() * d
                                    alpha = (1f - 0.42f * d).coerceIn(0f, 1f)
                                }
                                .folioSurface(),
                        )
                    }
                }
            }
        }
        Row(
            Modifier.padding(top = 4.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PillButton("Pass", { commit(-1, -2400f) })
            PillButton("Keep", { commit(1, 2400f) }, filled = true)
        }
    }
}

@Composable
private fun SwipeStamp(label: String, color: androidx.compose.ui.graphics.Color, modifier: Modifier = Modifier) {
    Box(
        modifier
            .clip(CircleShape)
            .background(Ink.Deep.copy(alpha = 0.78f))
            .padding(horizontal = 14.dp, vertical = 7.dp)
    ) {
        Text(label, style = Type.Ui, color = color)
    }
}
