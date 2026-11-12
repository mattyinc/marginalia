package com.mattyinc.marginalia.ui

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import com.mattyinc.marginalia.R
import com.mattyinc.marginalia.data.Story

/** A story's cover illustration and the colour that goes with it. */
data class StoryArt(@DrawableRes val cover: Int?, val color: Color)

private val Art = mapOf(
    "aksum" to StoryArt(R.drawable.cover_aksum, Color(0xFFE5874F)),
    "rome" to StoryArt(R.drawable.cover_rome, Color(0xFFA57BE0)),
    "nicaea" to StoryArt(R.drawable.cover_nicaea, Color(0xFF7C97F0)),
    "press" to StoryArt(R.drawable.cover_press, Color(0xFF4DB8A4)),
    "compound" to StoryArt(R.drawable.cover_compound, Color(0xFFE3B24C)),
    "tenochtitlan" to StoryArt(R.drawable.cover_tenochtitlan, Color(0xFF45B5CB)),
    "english-bible" to StoryArt(R.drawable.cover_english_bible, Color(0xFFE0708A)),
    "pareto" to StoryArt(R.drawable.cover_pareto, Color(0xFF8F95F2)),
)

/** Stories added without artwork still get a colour from their topic. */
fun artFor(story: Story): StoryArt = Art[story.id] ?: StoryArt(null, pigmentFor(story.topic))
