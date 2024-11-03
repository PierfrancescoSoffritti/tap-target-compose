package com.psoffritti.taptargetcompose

import androidx.annotation.FloatRange
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.TextUnit

/**
 * A composable that can shows tap targets in succession.
 * @param showTapTargets Whether to show the tap targets or not.
 * @param modifier The modifier to apply to this layout.
 * @param onComplete Called when all tap targets have been shown.
 * @param state The state of [TapTargetCoordinator],
 * can be used to know which tap target is being shown.
 * @param content The main content. Each composable in the [content] can be marked as a tap target.
 */
@Composable
fun TapTargetCoordinator(
  showTapTargets: Boolean,
  modifier: Modifier = Modifier,
  onComplete: () -> Unit = { },
  // TODO(issue#1) use rememberSavable
  state: TapTargetCoordinatorState = remember { TapTargetCoordinatorState() },
  contentAlignment: Alignment = Alignment.Center,
  content: @Composable TapTargetScope.() -> Unit,
) {
  val scope = remember(state) { TapTargetScope(state) }

  Box(
    contentAlignment = contentAlignment,
    modifier = modifier
  ) {
    // Always render the content.
    scope.content()
    if (showTapTargets) {
      val currentTapTarget = state.currentTarget
      if (currentTapTarget != null) {
        // Render the TapTarget in an overlay.
        // The overlay is removed when the composable leaves the composition.
        Overlay(key = Unit) {
          TapTarget(
            tapTarget = currentTapTarget,
            onComplete = {
              state.currentTargetIndex++
              if (state.currentTargetIndex >= state.tapTargets.size) {
                // There are no TapTargets left to render.
                onComplete()
              }
            }
          )
        }
      }
    }
  }
}

/** The scope from which a tap target can be created, through [Modifier.tapTarget]. */
class TapTargetScope internal constructor(private val state: TapTargetCoordinatorState) {

  /**
   * Modifier used to mark a [Composable] as a tap target.
   * @param precedence The precedence of the target. Targets are shown in order of precedence.
   * @param tapTargetStyle The style of the tap target.
   * @param onTargetClick Called when the target is clicked.
   * @param onTargetCancel Called when the target is cancelled, by clicking outside the target.
   */
  fun Modifier.tapTarget(
    title: TextDefinition,
    description: TextDefinition,
    precedence: Int,
    tapTargetStyle: TapTargetStyle = TapTargetStyle.Default,
    // TODO(issue#2) can we avoid intercepting the click instead of defining a callback?
    onTargetClick: () -> Unit = { },
    onTargetCancel: () -> Unit = { },
  ): Modifier {
    return onGloballyPositioned { layoutCoordinates ->
      state.tapTargets[precedence] = TapTarget(
        precedence = precedence,
        coordinates = layoutCoordinates,
        title = title,
        description = description,
        style = tapTargetStyle,
        onTargetClick = onTargetClick,
        onTargetCancel = onTargetCancel,
      )
    }
  }

  /**
   * The same as [Modifier.tapTarget], but takes a [TapTargetDefinition].
   * @see Modifier.tapTarget
   */
  fun Modifier.tapTarget(tapTargetDefinition: TapTargetDefinition): Modifier {
    return tapTarget(
      tapTargetDefinition.title,
      tapTargetDefinition.description,
      tapTargetDefinition.precedence,
      tapTargetDefinition.tapTargetStyle,
      tapTargetDefinition.onTargetClick,
      tapTargetDefinition.onTargetCancel
    )
  }
}

/** The content of a tap target. */
data class TapTargetDefinition(
  val title: TextDefinition,
  val description: TextDefinition,
  val precedence: Int,
  val tapTargetStyle: TapTargetStyle = TapTargetStyle.Default,
  val onTargetClick: () -> Unit = { },
  val onTargetCancel: () -> Unit = { },
)

/** Keeps track of which tap target is currently being shown. */
class TapTargetCoordinatorState internal constructor() {
  internal val tapTargets = mutableStateMapOf<Int, TapTarget>()
  internal var currentTargetIndex by mutableIntStateOf(0)

  val currentTarget get() = tapTargets[currentTargetIndex]
}

/**
 * Defines a tap target.
 * @param precedence The precedence of the target. Tap Targets are shown in order of precedence.
 * @param title The title of the tap target.
 * @param description The description of the tap target.
 * @param coordinates The [LayoutCoordinates] of the target.
 * @param style The style of the tap target.
 * @param onTargetClick Called when the target is clicked.
 * @param onTargetCancel Called when the target is cancelled, by clicking outside the target.
 */
class TapTarget internal constructor(
  val precedence: Int,
  val title: TextDefinition,
  val description: TextDefinition,
  val coordinates: LayoutCoordinates,
  val style: TapTargetStyle = TapTargetStyle.Default,
  val onTargetClick: () -> Unit,
  val onTargetCancel: () -> Unit,
)

/** Defines a text block shown in the tap target. */
data class TextDefinition(
  val text: String,
  internal val textStyle: TextStyle = TextStyle.Default,
  internal val color: Color = Color.Unspecified,
  internal val fontSize: TextUnit = TextUnit.Unspecified,
  internal val fontStyle: FontStyle? = null,
  internal val fontWeight: FontWeight? = null,
  internal val fontFamily: FontFamily? = null,
  internal val letterSpacing: TextUnit = TextUnit.Unspecified,
  internal val textDecoration: TextDecoration? = null,
  internal val textAlign: TextAlign? = null,
  internal val lineHeight: TextUnit = TextUnit.Unspecified,
) {
  val style = textStyle.merge(
    TextStyle(
      color = color,
      fontSize = fontSize,
      fontWeight = fontWeight,
      textAlign = textAlign ?: TextAlign.Unspecified,
      lineHeight = lineHeight,
      fontFamily = fontFamily,
      textDecoration = textDecoration,
      fontStyle = fontStyle,
      letterSpacing = letterSpacing
    )
  )
}

/**
 * Defines the look and feel of a tap target.
 * @param backgroundColor The background color of the main circle.
 * @param backgroundAlpha The alpha of the main circle.
 * @param tapTargetHighlightColor The color of the highlight circle.
 */
data class TapTargetStyle(
  val backgroundColor: Color = Color.Blue,
  @FloatRange(from = 0.0, to = 1.0)
  val backgroundAlpha: Float = 1f,
  val tapTargetHighlightColor: Color = Color.White,
) {
  companion object {
    val Default = TapTargetStyle()
  }
}