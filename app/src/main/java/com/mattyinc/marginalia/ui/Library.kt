package com.mattyinc.marginalia.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mattyinc.marginalia.data.Idea
import com.mattyinc.marginalia.data.StashWithCount

@Composable
fun LibraryScreen(
    all: List<Idea>,
    stashes: List<StashWithCount>,
    expanded: Boolean,
    padding: PaddingValues,
    query: String,
    onQuery: (String) -> Unit,
    selectedStash: Long?,
    onSelectStash: (Long?) -> Unit,
    onOpen: (Idea) -> Unit,
) {
    val searching = query.isNotBlank()
    val results = remember(all, query) {
        if (query.isBlank()) emptyList()
        else all.filter { idea ->
            listOf(idea.title, idea.body, idea.source, idea.topic, idea.note)
                .any { it.contains(query.trim(), ignoreCase = true) }
        }
    }

    if (expanded) {
        // Two panes: stashes stay put on the left, their ideas fill the right.
        val current = selectedStash ?: stashes.firstOrNull()?.stash?.id
        val ideas = remember(all, current) { all.filter { it.stashId != null && it.stashId == current } }
        Row(Modifier.fillMaxSize().padding(padding)) {
            Column(Modifier.width(340.dp).fillMaxHeight().padding(start = 24.dp, top = 20.dp, end = 8.dp)) {
                Text("Library", style = Type.Display, color = Ink.Vellum)
                Spacer(Modifier.height(16.dp))
                SearchField(query, onQuery)
                Spacer(Modifier.height(12.dp))
                LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
                    items(stashes, key = { it.stash.id }) { item ->
                        StashRow(
                            item = item,
                            selected = !searching && item.stash.id == current,
                            onClick = {
                                onQuery("")
                                onSelectStash(item.stash.id)
                            },
                        )
                    }
                }
            }
            Box(Modifier.weight(1f).fillMaxHeight()) {
                when {
                    searching -> IdeaGrid(results, onOpen, "Nothing matches", "Try a different word.", top = 28.dp)
                    stashes.isEmpty() -> EmptyLibrary(Modifier.align(Alignment.Center))
                    else -> IdeaGrid(
                        ideas, onOpen,
                        "This stash is empty", "Keep an idea with this topic and it lands here.",
                        top = 28.dp,
                    )
                }
            }
        }
        return
    }

    BackHandler(enabled = selectedStash != null || searching) {
        if (searching) onQuery("") else onSelectStash(null)
    }

    Column(Modifier.fillMaxSize().padding(padding)) {
        Column(Modifier.padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 12.dp)) {
            Text("Library", style = Type.Display, color = Ink.Vellum)
            Spacer(Modifier.height(14.dp))
            SearchField(query, onQuery)
        }
        if (searching) {
            IdeaGrid(results, onOpen, "Nothing matches", "Try a different word.")
        } else {
            AnimatedContent(
                targetState = selectedStash,
                transitionSpec = {
                    // Going in enters from the right; coming back leaves the same way.
                    val forward = targetState != null
                    val enter = slideInHorizontally(Motion.settle()) { if (forward) it / 4 else -it / 4 } + fadeIn()
                    val exit = slideOutHorizontally(Motion.settle()) { if (forward) -it / 4 else it / 4 } + fadeOut()
                    enter togetherWith exit
                },
                label = "stash",
                modifier = Modifier.fillMaxSize(),
            ) { stashId ->
                if (stashId == null) {
                    if (stashes.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { EmptyLibrary() }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(190.dp),
                            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 120.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            items(stashes, key = { it.stash.id }) { item ->
                                StashTile(item) { onSelectStash(item.stash.id) }
                            }
                        }
                    }
                } else {
                    val stash = stashes.firstOrNull { it.stash.id == stashId }
                    val ideas = remember(all, stashId) { all.filter { it.stashId == stashId } }
                    Column(Modifier.fillMaxSize()) {
                        Row(
                            Modifier
                                .padding(horizontal = 14.dp)
                                .pressable { onSelectStash(null) }
                                .padding(horizontal = 10.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            GlyphIcon(Glyph.Back, Ink.VellumDim)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                stash?.stash?.name ?: "",
                                style = Type.Title,
                                color = pigmentFor(stash?.stash?.name ?: ""),
                            )
                        }
                        IdeaGrid(
                            ideas, onOpen,
                            "This stash is empty", "Keep an idea with this topic and it lands here.",
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyLibrary(modifier: Modifier = Modifier) {
    EmptyNote(
        title = "Nothing kept yet",
        body = "Swipe an idea to the right on Today and it is kept here.",
        modifier = modifier,
    )
}

@Composable
private fun SearchField(query: String, onQuery: (String) -> Unit) {
    val shape = RoundedCornerShape(50)
    BasicTextField(
        value = query,
        onValueChange = onQuery,
        singleLine = true,
        textStyle = Type.Ui.copy(color = Ink.Vellum, fontSize = 16.sp),
        cursorBrush = SolidColor(Ink.Gilt),
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Ink.Vellum.copy(alpha = 0.06f))
            .border(1.dp, Ink.Hairline, shape)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        decorationBox = { inner ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f)) {
                    if (query.isEmpty()) {
                        Text("Search every idea", style = Type.Ui.copy(fontSize = 16.sp), color = Ink.VellumDim)
                    }
                    inner()
                }
                if (query.isNotEmpty()) {
                    GlyphIcon(Glyph.Close, Ink.VellumDim, Modifier.pressable { onQuery("") })
                }
            }
        },
    )
}

@Composable
private fun StashTile(item: StashWithCount, onClick: () -> Unit) {
    val pigment = pigmentFor(item.stash.name)
    Column(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 148.dp)
            .pressable(onClick = onClick)
            .folioSurface(FolioShapeSmall)
            .background(
                Brush.linearGradient(
                    colors = listOf(pigment.copy(alpha = 0.30f), Color.Transparent),
                    start = Offset.Zero,
                    end = Offset(520f, 520f),
                )
            )
            .drawBehind {
                drawLine(
                    pigment,
                    Offset(22.dp.toPx(), 0f),
                    Offset(22.dp.toPx(), size.height),
                    1.5.dp.toPx(),
                )
            }
            .padding(start = 36.dp, top = 20.dp, end = 16.dp, bottom = 18.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(item.stash.name, style = Type.Title, color = Ink.Vellum)
        Spacer(Modifier.height(28.dp))
        Text(
            if (item.ideaCount == 1) "1 idea" else "${item.ideaCount} ideas",
            style = Type.UiSmall,
            color = Ink.VellumDim,
        )
    }
}

@Composable
private fun StashRow(item: StashWithCount, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .pressable(onClick = onClick)
            .clip(shape)
            .background(if (selected) Ink.Vellum.copy(alpha = 0.09f) else Color.Transparent)
            .padding(horizontal = 16.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(pigmentFor(item.stash.name)))
        Spacer(Modifier.width(14.dp))
        Text(item.stash.name, style = Type.Reading, color = Ink.Vellum, modifier = Modifier.weight(1f))
        Text(item.ideaCount.toString(), style = Type.Ui, color = Ink.VellumDim)
    }
}

@Composable
private fun IdeaGrid(
    ideas: List<Idea>,
    onOpen: (Idea) -> Unit,
    emptyTitle: String,
    emptyBody: String,
    top: androidx.compose.ui.unit.Dp = 4.dp,
) {
    if (ideas.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            EmptyNote(emptyTitle, emptyBody)
        }
        return
    }
    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Adaptive(300.dp),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = top, bottom = 120.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalItemSpacing = 14.dp,
        modifier = Modifier.fillMaxSize(),
    ) {
        items(ideas, key = { it.id }) { idea ->
            SharedFolio(key = "library-${idea.id}", modifier = Modifier.animateItem()) { shared ->
                FolioPage(
                    idea = idea,
                    large = false,
                    bodyLines = 5,
                    modifier = shared
                        .fillMaxWidth()
                        .pressable { onOpen(idea) }
                        .folioSurface(FolioShapeSmall),
                )
            }
        }
    }
}
