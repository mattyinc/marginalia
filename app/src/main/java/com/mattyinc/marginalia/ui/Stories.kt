package com.mattyinc.marginalia.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mattyinc.marginalia.Progress
import com.mattyinc.marginalia.data.Story

/** The shelf of stories: short histories and ideas that play like a film strip. */
@Composable
fun StoriesScreen(
    stories: List<Story>,
    watched: Set<String>,
    progress: Progress,
    padding: PaddingValues,
    onOpen: (Story) -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(250.dp),
        contentPadding = PaddingValues(
            start = 20.dp,
            end = 20.dp,
            top = padding.calculateTopPadding() + 20.dp,
            bottom = padding.calculateBottomPadding() + 28.dp,
        ),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Row(
                Modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Stories", style = Type.Display, color = Ink.Vellum)
                    Text(
                        "Short histories and ideas, read aloud",
                        style = Type.Margin.copy(fontSize = 16.sp),
                        color = Ink.VellumDim,
                    )
                }
                GoalRing(progress, diameter = 52.dp)
            }
        }
        items(stories, key = { it.id }) { story ->
            SharedFolio(key = "story-${story.id}") { shared ->
                StoryCover(
                    story = story,
                    seen = story.id in watched,
                    modifier = shared.pressable { onOpen(story) },
                )
            }
        }
    }
}

@Composable
private fun StoryCover(story: Story, seen: Boolean, modifier: Modifier = Modifier) {
    val pigment = pigmentFor(story.topic)
    Row(
        modifier
            .fillMaxWidth()
            .height(212.dp)
            .folioSurface()
            .background(
                Brush.linearGradient(
                    colors = listOf(pigment.copy(alpha = if (seen) 0.14f else 0.36f), Color.Transparent),
                    start = Offset.Zero,
                    end = Offset(700f, 700f),
                )
            )
            .padding(18.dp)
    ) {
        Box(Modifier.width(28.dp), contentAlignment = Alignment.TopStart) {
            Text(
                story.topic,
                style = Type.Margin.copy(fontSize = 14.sp),
                color = pigment,
                maxLines = 1,
                modifier = Modifier.alongMargin(),
            )
        }
        Column(
            Modifier
                .weight(1f)
                .fillMaxSize()
                .drawBehind {
                    drawLine(pigment, Offset(0f, 0f), Offset(0f, size.height), 1.5.dp.toPx())
                }
                .padding(start = 16.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                story.title,
                style = Type.Title,
                color = if (seen) Ink.Vellum.copy(alpha = 0.75f) else Ink.Vellum,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = when {
                    seen -> "Watched"
                    story.minutes == 1 -> "About 1 minute"
                    else -> "About ${story.minutes} minutes"
                },
                style = Type.UiSmall,
                color = if (seen) pigment else Ink.VellumDim,
            )
        }
    }
}
