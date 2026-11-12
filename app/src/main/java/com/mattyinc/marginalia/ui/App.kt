package com.mattyinc.marginalia.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mattyinc.marginalia.MarginaliaViewModel

private val TabNames = listOf("Stories", "Ideas", "Library")

@Composable
fun MarginaliaApp(vm: MarginaliaViewModel = viewModel()) {
    MarginaliaTheme {
        val feed by vm.feed.collectAsStateWithLifecycle()
        val all by vm.all.collectAsStateWithLifecycle()
        val stashes by vm.stashes.collectAsStateWithLifecycle()
        val progress by vm.progress.collectAsStateWithLifecycle()
        val stories by vm.stories.collectAsStateWithLifecycle()
        val watched by vm.watched.collectAsStateWithLifecycle()

        var tab by rememberSaveable { mutableIntStateOf(0) }
        var open by remember { mutableStateOf<OpenIdea?>(null) }
        var openStory by remember { mutableStateOf<String?>(null) }
        var capturing by rememberSaveable { mutableStateOf(false) }
        var query by rememberSaveable { mutableStateOf("") }
        var selectedStash by rememberSaveable { mutableStateOf<Long?>(null) }
        val commands = remember { StackCommands() }
        val focus = remember { FocusRequester() }

        // The lamplight takes the colour of whatever is in front of you.
        val tint = when {
            tab == 0 -> Ink.Gilt
            tab == 1 -> feed?.firstOrNull()?.let { pigmentFor(it.topic) } ?: Ink.Gilt
            else -> stashes.firstOrNull { it.stash.id == selectedStash }
                ?.let { pigmentFor(it.stash.name) } ?: Ink.Gilt
        }

        BackHandler(enabled = open != null) { open = null }
        BackHandler(enabled = openStory != null) { openStory = null }
        LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }

        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .focusRequester(focus)
                .focusable()
                .onKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                    when {
                        event.key == Key.Escape && openStory != null -> { openStory = null; true }
                        event.key == Key.Escape && open != null -> { open = null; true }
                        open != null || openStory != null || capturing -> false
                        event.key == Key.N -> { capturing = true; true }
                        event.key == Key.One -> { tab = 0; true }
                        event.key == Key.Two -> { tab = 1; true }
                        event.key == Key.Three -> { tab = 2; true }
                        tab != 1 -> false
                        event.key == Key.DirectionRight -> { commands.fling?.invoke(1); true }
                        event.key == Key.DirectionLeft -> { commands.fling?.invoke(-1); true }
                        event.key == Key.Enter || event.key == Key.Spacebar -> { commands.open?.invoke(); true }
                        else -> false
                    }
                }
        ) {
            val compact = maxWidth < 600.dp
            val expanded = maxWidth >= 840.dp
            val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

            Ambient(tint)

            SharedTransitionLayout(Modifier.fillMaxSize()) {
                CompositionLocalProvider(
                    LocalSharedScope provides this,
                    LocalOpenKey provides (open?.key ?: openStory?.let { "story-$it" }),
                ) {
                    Box(Modifier.fillMaxSize()) {
                        Row(Modifier.fillMaxSize()) {
                            if (!compact) {
                                NavRail(tab, { tab = it }, { capturing = true })
                            }
                            val padding = PaddingValues(
                                top = statusTop,
                                bottom = if (compact) 92.dp else 0.dp,
                            )
                            AnimatedContent(
                                targetState = tab,
                                transitionSpec = {
                                    (fadeIn(tween(220)) + scaleIn(Motion.settle(), initialScale = 0.985f)) togetherWith
                                        fadeOut(tween(120))
                                },
                                label = "tab",
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                            ) { current ->
                                if (current == 0) {
                                    StoriesScreen(
                                        stories = stories,
                                        watched = watched,
                                        progress = progress,
                                        padding = padding,
                                        onOpen = { openStory = it.id },
                                    )
                                } else if (current == 1) {
                                    TodayScreen(
                                        feed = feed,
                                        progress = progress,
                                        stashes = stashes,
                                        expanded = expanded,
                                        padding = padding,
                                        commands = commands,
                                        onOpen = { open = OpenIdea("today-${it.id}", it.id) },
                                        onKeep = { vm.keep(it) },
                                        onPass = { vm.pass(it) },
                                        onReturnPassed = { vm.returnPassedToFeed() },
                                        onCapture = { capturing = true },
                                        onStash = {
                                            selectedStash = it
                                            query = ""
                                            tab = 2
                                        },
                                    )
                                } else {
                                    LibraryScreen(
                                        all = all,
                                        stashes = stashes,
                                        expanded = expanded,
                                        padding = padding,
                                        query = query,
                                        onQuery = { query = it },
                                        selectedStash = selectedStash,
                                        onSelectStash = { selectedStash = it },
                                        onOpen = { open = OpenIdea("library-${it.id}", it.id) },
                                    )
                                }
                            }
                        }

                        if (compact) {
                            FloatingNav(
                                tab = tab,
                                onTab = { tab = it },
                                onCapture = { capturing = true },
                                modifier = Modifier.align(Alignment.BottomCenter),
                            )
                        }

                        // The reader: the page dims and the folio you touched grows into place.
                        AnimatedContent(
                            targetState = open,
                            transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(200)) },
                            label = "reader",
                            modifier = Modifier.fillMaxSize(),
                        ) { target ->
                            val idea = target?.let { t -> all.firstOrNull { it.id == t.id } }
                            if (target != null && idea != null) {
                                Box(Modifier.fillMaxSize()) {
                                    Box(
                                        Modifier
                                            .fillMaxSize()
                                            .background(Ink.Deep.copy(alpha = 0.78f))
                                            .clickable(
                                                interactionSource = remember { MutableInteractionSource() },
                                                indication = null,
                                            ) { open = null }
                                    )
                                    Reader(
                                        idea = idea,
                                        stashes = stashes,
                                        folio = Modifier.sharedBounds(
                                            sharedContentState = rememberSharedContentState(key = target.key),
                                            animatedVisibilityScope = this@AnimatedContent,
                                            boundsTransform = FolioBounds,
                                            clipInOverlayDuringTransition = OverlayClip(FolioShape),
                                        ),
                                        onClose = { open = null },
                                        onMove = { vm.moveToStash(idea.id, it) },
                                        onNote = { vm.setNote(idea.id, it) },
                                        onDelete = {
                                            open = null
                                            vm.delete(idea.id)
                                        },
                                    )
                                }
                            } else {
                                Box(Modifier.fillMaxSize())
                            }
                        }

                        AnimatedContent(
                            targetState = openStory,
                            transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(220)) },
                            label = "story",
                            modifier = Modifier.fillMaxSize(),
                        ) { storyId ->
                            val story = stories.firstOrNull { it.id == storyId }
                            if (story != null) {
                                StoryPlayer(
                                    story = story,
                                    modifier = Modifier.sharedBounds(
                                        sharedContentState = rememberSharedContentState(key = "story-${story.id}"),
                                        animatedVisibilityScope = this@AnimatedContent,
                                        boundsTransform = FolioBounds,
                                        clipInOverlayDuringTransition = OverlayClip(FolioShape),
                                    ),
                                    onClose = { openStory = null },
                                    onFinished = { vm.finishStory(story.id) },
                                    onKeepTakeaway = { vm.keepTakeaway(story) },
                                )
                            } else {
                                Box(Modifier.fillMaxSize())
                            }
                        }
                    }
                }
            }

            if (capturing) {
                CaptureSheet(
                    stashes = stashes,
                    onDismiss = { capturing = false },
                    onSave = { title, body, source, stash ->
                        vm.addOwn(title, body, source, stash)
                        capturing = false
                    },
                )
            }
        }
    }
}

/** Phone navigation: a pill that floats over the page, with a marker that slides between tabs. */
@Composable
private fun FloatingNav(tab: Int, onTab: (Int) -> Unit, onCapture: () -> Unit, modifier: Modifier = Modifier) {
    val itemWidth: Dp = 86.dp
    Row(
        modifier
            .navigationBarsPadding()
            .padding(bottom = 14.dp)
            .clip(CircleShape)
            .background(Ink.FolioHigh.copy(alpha = 0.96f))
            .border(1.dp, Ink.Hairline, CircleShape)
            .padding(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box {
            val x by animateDpAsState(itemWidth * tab, Motion.flick(), label = "marker")
            Box(
                Modifier
                    .offset(x = x)
                    .size(itemWidth, 52.dp)
                    .clip(CircleShape)
                    .background(Ink.Vellum.copy(alpha = 0.11f))
            )
            Row {
                TabNames.forEachIndexed { index, name ->
                    Box(
                        Modifier.size(itemWidth, 52.dp).pressable { onTab(index) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(name, style = Type.Ui, color = if (tab == index) Ink.Vellum else Ink.VellumDim)
                    }
                }
            }
        }
        Spacer(Modifier.width(6.dp))
        CaptureButton(onCapture, 52.dp)
    }
}

/** Tablet navigation: a rail down the leading edge. */
@Composable
private fun NavRail(tab: Int, onTab: (Int) -> Unit, onCapture: () -> Unit) {
    val itemHeight: Dp = 60.dp
    Column(
        Modifier
            .width(112.dp)
            .fillMaxHeight()
            .systemBarsPadding()
            .padding(vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("M", style = Type.Display.copy(fontSize = 34.sp), color = Ink.Gilt)
        Spacer(Modifier.height(28.dp))
        Box {
            val y by animateDpAsState(itemHeight * tab, Motion.flick(), label = "marker")
            Box(
                Modifier
                    .offset(y = y)
                    .size(92.dp, itemHeight - 8.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Ink.Vellum.copy(alpha = 0.10f))
            )
            Column {
                TabNames.forEachIndexed { index, name ->
                    Box(
                        Modifier.size(92.dp, itemHeight).pressable { onTab(index) }.padding(bottom = 8.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(name, style = Type.Ui, color = if (tab == index) Ink.Vellum else Ink.VellumDim)
                    }
                }
            }
        }
        Spacer(Modifier.weight(1f))
        CaptureButton(onCapture, 60.dp)
    }
}

@Composable
private fun CaptureButton(onClick: () -> Unit, diameter: Dp) {
    Box(
        Modifier
            .pressable(onClick = onClick)
            .size(diameter)
            .clip(CircleShape)
            .background(Ink.Gilt),
        contentAlignment = Alignment.Center,
    ) {
        GlyphIcon(Glyph.Plus, Ink.Night)
    }
}
