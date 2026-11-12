package com.mattyinc.marginalia.ui

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.mattyinc.marginalia.R
import kotlin.math.abs

/**
 * The palette comes from the manuscript page seen at night: a lapis-dark ground,
 * vellum for text, gold leaf for the one accent, and the scribe's pigments to
 * tell stashes apart.
 */
object Ink {
    val Night = Color(0xFF0C1024)
    val Deep = Color(0xFF070918)
    val Folio = Color(0xFF151B3C)
    val FolioHigh = Color(0xFF1D2550)
    val Vellum = Color(0xFFF0E8D6)
    val VellumDim = Color(0xFFB3B0BF)
    val Gilt = Color(0xFFE0B252)
    val Hairline = Color(0x26F0E8D6)
}

private val Lapis = Color(0xFF6F8FF2)
private val Verdigris = Color(0xFF58B5A2)
private val Madder = Color(0xFFE0708A)
private val Orpiment = Color(0xFFE0B252)
private val Tyrian = Color(0xFFAE86E6)
private val Minium = Color(0xFFEC8A62)

private val Pigments = listOf(Lapis, Verdigris, Madder, Orpiment, Tyrian, Minium)

private val KnownTopics = mapOf(
    "Abiding" to Orpiment,
    "Discipline" to Minium,
    "Stillness" to Lapis,
    "Craft" to Verdigris,
    "Wisdom" to Tyrian,
    "Courage" to Madder,
)

/** A stash or topic always gets the same pigment, wherever it shows up. */
fun pigmentFor(name: String): Color =
    KnownTopics[name] ?: Pigments[abs(name.lowercase().hashCode() % Pigments.size)]

private fun newsreader(weight: Int, italic: Boolean = false) = Font(
    resId = if (italic) R.font.newsreader_italic else R.font.newsreader,
    weight = FontWeight(weight),
    style = if (italic) FontStyle.Italic else FontStyle.Normal,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

private fun hanken(weight: Int) = Font(
    resId = R.font.hanken_grotesk,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

val Newsreader = FontFamily(
    newsreader(400), newsreader(500), newsreader(600),
    newsreader(400, italic = true), newsreader(500, italic = true),
)
val Hanken = FontFamily(hanken(400), hanken(500), hanken(600))

/** Newsreader carries everything you read; Hanken Grotesk carries everything you press. */
object Type {
    val Display = TextStyle(
        fontFamily = Newsreader, fontWeight = FontWeight.Medium,
        fontSize = 40.sp, lineHeight = 43.sp, letterSpacing = (-0.02).em,
    )
    val Title = TextStyle(
        fontFamily = Newsreader, fontWeight = FontWeight.Medium,
        fontSize = 27.sp, lineHeight = 31.sp, letterSpacing = (-0.015).em,
    )
    val TitleSmall = TextStyle(
        fontFamily = Newsreader, fontWeight = FontWeight.Medium,
        fontSize = 21.sp, lineHeight = 25.sp, letterSpacing = (-0.01).em,
    )
    val Reading = TextStyle(
        fontFamily = Newsreader, fontWeight = FontWeight.Normal,
        fontSize = 19.sp, lineHeight = 29.sp,
    )
    val ReadingSmall = TextStyle(
        fontFamily = Newsreader, fontWeight = FontWeight.Normal,
        fontSize = 16.sp, lineHeight = 24.sp,
    )
    val Margin = TextStyle(
        fontFamily = Newsreader, fontWeight = FontWeight.Normal, fontStyle = FontStyle.Italic,
        fontSize = 15.sp, lineHeight = 20.sp, letterSpacing = 0.01.em,
    )
    val Ui = TextStyle(
        fontFamily = Hanken, fontWeight = FontWeight.Medium,
        fontSize = 15.sp, lineHeight = 20.sp,
    )
    val UiSmall = TextStyle(
        fontFamily = Hanken, fontWeight = FontWeight.Medium,
        fontSize = 13.sp, lineHeight = 17.sp, letterSpacing = 0.01.em,
    )
}

/**
 * Springs, described the way a designer would: how much it overshoots and how fast it
 * gets there. `settle` never overshoots; `flick` does, a little, and is only used
 * when the motion follows a throw of the finger.
 */
object Motion {
    fun <T> settle(): FiniteAnimationSpec<T> = spring(dampingRatio = 1f, stiffness = 380f)
    fun <T> flick(): FiniteAnimationSpec<T> = spring(dampingRatio = 0.8f, stiffness = 300f)
    fun <T> quick(): FiniteAnimationSpec<T> = spring(dampingRatio = 1f, stiffness = 1100f)

    /** Where a release at this velocity (px/s) would coast to, as scrolling does. */
    fun project(velocity: Float, rate: Float = 0.998f): Float =
        (velocity / 1000f) * rate / (1f - rate)
}

@Composable
fun MarginaliaTheme(content: @Composable () -> Unit) {
    val colors = darkColorScheme(
        primary = Ink.Gilt,
        onPrimary = Ink.Night,
        secondary = Lapis,
        background = Ink.Night,
        onBackground = Ink.Vellum,
        surface = Ink.Folio,
        onSurface = Ink.Vellum,
        surfaceVariant = Ink.FolioHigh,
        onSurfaceVariant = Ink.VellumDim,
        surfaceContainer = Ink.Folio,
        surfaceContainerHigh = Ink.FolioHigh,
        surfaceContainerLow = Ink.Folio,
        outline = Ink.Hairline,
    )
    MaterialTheme(
        colorScheme = colors,
        typography = Typography(bodyLarge = Type.Ui, bodyMedium = Type.Ui, labelLarge = Type.Ui),
        content = content,
    )
}

/** Responds the instant a finger lands, not when it lifts. */
fun Modifier.pressable(
    enabled: Boolean = true,
    role: Role = Role.Button,
    onClick: () -> Unit,
): Modifier = composed {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = Motion.quick(),
        label = "press",
    )
    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(
            interactionSource = interaction,
            indication = null,
            enabled = enabled,
            role = role,
            onClick = onClick,
        )
}

/** Lays text out along the margin, reading bottom to top. */
fun Modifier.alongMargin(): Modifier = this
    .layout { measurable, _ ->
        val placeable = measurable.measure(Constraints())
        layout(placeable.height, placeable.width) {
            placeable.place(
                x = -(placeable.width / 2 - placeable.height / 2),
                y = -(placeable.height / 2 - placeable.width / 2),
            )
        }
    }
    .rotate(-90f)
