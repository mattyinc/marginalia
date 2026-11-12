package com.mattyinc.marginalia.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
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
        columns = GridCells.Adaptive(168.dp),
        contentPadding = PaddingValues(
            start = 20.dp,
            end = 20.dp,
            top = padding.calculateTopPadding() + 20.dp,
            bottom = padding.calculateBottomPadding() + 28.dp,
        ),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
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
    val art = artFor(story)
    Column(modifier.fillMaxWidth()) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(0.8f)
                .clip(FolioShapeSmall)
                .background(art.color.copy(alpha = 0.35f))
        ) {
            if (art.cover != null) {
                Image(
                    painter = painterResource(art.cover),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().graphicsLayer { alpha = if (seen) 0.62f else 1f },
                )
            }
            if (seen) {
                Text(
                    "Watched",
                    style = Type.UiSmall,
                    color = Ink.Vellum,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp)
                        .clip(CircleShape)
                        .background(Ink.Deep.copy(alpha = 0.78f))
                        .padding(horizontal = 11.dp, vertical = 6.dp),
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(story.title, style = Type.TitleSmall, color = Ink.Vellum, modifier = Modifier.padding(horizontal = 4.dp))
        Spacer(Modifier.height(3.dp))
        Text(
            text = if (story.minutes == 1) "About 1 minute" else "About ${story.minutes} minutes",
            style = Type.UiSmall,
            color = Ink.VellumDim,
            modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 6.dp),
        )
    }
}
