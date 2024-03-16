package com.psoffritti.taptargetcompose

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import android.view.ViewGroup
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

private const val DEBUG = false

/** Padding between the tap target and the highlight circle. */
private val TARGET_PADDING = 20.dp

/** The horizontal margin for the text block */
private val TEXT_HORIZONTAL_MARGIN = 40.dp
/** The vertical margin for the text block */
private val TEXT_VERTICAL_MARGIN = 40.dp

/** The margin between the outer circle and the text. */
private val OUTER_CIRCLE_INTERNAL_MARGIN = 40.dp

/** The maximum width of the text. */
private val MAX_TEXT_WIDTH = 360f.dp

/** The margin between the title and description text. */
private val TEXT_SPACING = 2.dp

private fun Dp.toPx(density: Density) = with(density) { toPx() }

private const val LOG_TAG = "TapTarget"

/** Composable responsible for drawing the tap target. */
@Composable
internal fun TapTargetContent(tapTarget: TapTarget, onComplete: () -> Unit) {
  val density = LocalDensity.current
  val screenSizePx = Size(
    LocalConfiguration.current.screenWidthDp * density.density,
    LocalConfiguration.current.screenHeightDp * density.density
  )

  var lastTargetCenter by remember { mutableStateOf(Offset.Zero) }
  // For moving targets, the coordinates can change, use a function
  // to always get the latest.
  val getTargetCenterPx = {
    if (tapTarget.coordinates.isAttached) {
      val center = tapTarget.coordinates.boundsInWindow().center
      lastTargetCenter = center
      center
    }
    else {
      lastTargetCenter
    }
  }

  val targetMaxDimensionPx = max(
    tapTarget.coordinates.size.width,
    tapTarget.coordinates.size.height
  )
  val targetRadiusPx = targetMaxDimensionPx / 2 + TARGET_PADDING.toPx(density)

  // The radius of the outer circle of the tap target.
  var outerCircleRadiusPx by remember { mutableFloatStateOf(0f) }
  // The radius of the highlight circle shown around the target.
  var highlightCircleRadiusPx by remember { mutableFloatStateOf(0f) }
  // Whether we are animating in or out.
  var animateIn by remember { mutableStateOf(true) }
  // Whether the user clicked the target.
  var targetClicked by remember { mutableStateOf(false) }
  // Whether the user clicked outside the target.
  var targetCancelled by remember { mutableStateOf(false) }

  val outerCircleAnimatable = remember { Animatable(0f) }
  val highlightCircleAnimatable = remember { Animatable(0f) }
  val tapTargetCircleAnimatable = remember { Animatable(0f) }
  val textAlphaAnimatable = remember { Animatable(0f) }

  if (targetClicked) {
    // The user tapped the target, notify the target.
    tapTarget.onTargetClick()
    targetClicked = false
  }

  if (
    // We have completed the current target, and are now animating out.
    !animateIn &&
    // The outer circle has finished collapsing.
    outerCircleAnimatable.value == 0f &&
    // The highlight circle has finished collapsing.
    highlightCircleAnimatable.value == 0f
  ) {
    // Animate out is complete. We are now switching to the next target and should animate in.
    animateIn = true

    if (targetCancelled) {
      // The user tapped outside the target, notify the target.
      tapTarget.onTargetCancel()
      targetCancelled = false
    }

    // The animation is complete, notify that we are ready for the next target.
    onComplete()
  }

  if (animateIn) {
    AnimateIn(
      tapTarget,
      outerCircleAnimatable,
      highlightCircleAnimatable,
      tapTargetCircleAnimatable,
      textAlphaAnimatable
    )
  }
  else {
    AnimateOut(
      tapTarget,
      outerCircleAnimatable,
      highlightCircleAnimatable,
      textAlphaAnimatable
    )
  }

  val maxTextWidthPx = MAX_TEXT_WIDTH.toPx(density)
  val textHorizontalMarginPx = TEXT_HORIZONTAL_MARGIN.toPx(density)
  val textVerticalMarginPx = TEXT_VERTICAL_MARGIN.toPx(density)

  val textWidthPx = min(screenSizePx.width, maxTextWidthPx) - textHorizontalMarginPx * 2

  val constraints = Constraints.fixedWidth(textWidthPx.toInt())
  val textMeasurer = rememberTextMeasurer()
  val titleMeasure = tapTarget.title.rememberMeasure(textMeasurer, constraints)
  val descriptionMeasure = tapTarget.description.rememberMeasure(textMeasurer, constraints)

  val textBlockHeightPx = titleMeasure.size.height +
          descriptionMeasure.size.height +
          TEXT_SPACING.toPx(density)

  val textBlockTopLeft = getTextBlockOffset(
    Size(textWidthPx, textBlockHeightPx),
    screenSizePx,
    getTargetCenterPx(),
    targetRadiusPx,
    textHorizontalMarginPx,
    textVerticalMarginPx
  )
  val textBlockRect = Rect(
    textBlockTopLeft.x,
    textBlockTopLeft.y,
    textBlockTopLeft.x + textWidthPx,
    textBlockTopLeft.y + textBlockHeightPx
  )

  val topLeftRadius = textBlockRect.topLeft.distanceTo(getTargetCenterPx())
  val topRightRadius = textBlockRect.topRight.distanceTo(getTargetCenterPx())
  val bottomLeftRadius = textBlockRect.bottomLeft.distanceTo(getTargetCenterPx())
  val bottomRightRadius = textBlockRect.bottomRight.distanceTo(getTargetCenterPx())
  val maxRadius = max(
    topLeftRadius,
    topRightRadius,
    bottomLeftRadius,
    bottomRightRadius
  )

  outerCircleRadiusPx = maxRadius + OUTER_CIRCLE_INTERNAL_MARGIN.toPx(density)
  highlightCircleRadiusPx = min(targetRadiusPx*2, outerCircleRadiusPx)

  Render(
    tapTarget,
    onTargetCancel = {
      targetCancelled = true
      animateIn = false
    },
    onTargetClick = {
      targetClicked = true
      animateIn = false
    },
    outerCircleScale = outerCircleAnimatable.value,
    highlightCircleScale = highlightCircleAnimatable.value,
    tapTargetCircleScale = tapTargetCircleAnimatable.value,
    getTargetCenter = getTargetCenterPx,
    targetRadius = targetRadiusPx,
    outerCircleRadius = outerCircleRadiusPx,
    highlightCircleRadius = highlightCircleRadiusPx,
    textBlockTopLeft = textBlockTopLeft,
    titleMeasure = titleMeasure,
    descriptionMeasure = descriptionMeasure,
    textBlockRect = textBlockRect,
    textAlpha = textAlphaAnimatable.value
  )
}

@Composable
private fun Render(
  tapTarget: TapTarget,
  onTargetClick: () -> Unit,
  onTargetCancel: () -> Unit,
  textAlpha: Float,
  outerCircleScale: Float,
  highlightCircleScale: Float,
  tapTargetCircleScale: Float,
  getTargetCenter: () -> Offset,
  targetRadius: Float,
  outerCircleRadius: Float,
  highlightCircleRadius: Float,
  textBlockTopLeft: Offset,
  titleMeasure: TextLayoutResult,
  descriptionMeasure: TextLayoutResult,
  textBlockRect: Rect
) {
  Overlay(key = tapTarget) {
    Canvas(
      modifier = Modifier
        .fillMaxSize()
        .pointerInput(tapTarget) {
          detectTapGestures { tapOffset ->
            when {
              tapOffset.isOutsideCircle(getTargetCenter(), outerCircleRadius) -> {
                // The user clicked outside the target
                onTargetCancel()
              }

              tapOffset.isInsideCircle(getTargetCenter(), targetRadius) -> {
                // The user clicked the target
                onTargetClick()
              }
            }
          }
        } // Add transparency to the entire canvas, so we can show what's below the tap target area.
        .graphicsLayer(alpha = 0.99f)
    ) {
      // Don't draw the circles if they are smaller than the targetRadius.
      // Otherwise we would draw above the tap target during animation.
      if (outerCircleRadius * outerCircleScale < targetRadius) {
        return@Canvas
      }

      // Draw outer circle
      drawCircle(
        center = getTargetCenter(),
        radius = outerCircleRadius * outerCircleScale,
        color = tapTarget.style.backgroundColor,
        alpha = tapTarget.style.backgroundAlpha
      )

      // Draw highlight circle
      drawCircle(
        center = getTargetCenter(),
        radius = highlightCircleRadius * highlightCircleScale,
        color = tapTarget.style.tapTargetHighlightColor,
        alpha = 1 - highlightCircleScale.pow(4)
      )

      // XOR circle used to reveal the tap target, since we are drawing the other circles above it.
      drawCircle(
        center = getTargetCenter(),
        radius = targetRadius + ((targetRadius / 10) * tapTargetCircleScale),
        color = tapTarget.style.tapTargetHighlightColor,
        blendMode = BlendMode.Xor
      )

      drawText(
        textLayoutResult = titleMeasure,
        topLeft = textBlockTopLeft,
        alpha = textAlpha.pow(2)
      )
      drawText(
        textLayoutResult = descriptionMeasure,
        topLeft = textBlockTopLeft.plus(
          Offset(x = 0f, y = titleMeasure.size.height + TEXT_SPACING.toPx())
        ),
        alpha = textAlpha.pow(2)
      )

      if (DEBUG) {
        // Draw the text block rect to see text bounds.
        drawRect(
          color = Color.Black.copy(alpha = 0.4f),
          topLeft = textBlockRect.topLeft,
          size = Size(textBlockRect.width, textBlockRect.height)
        )
      }
    }
  }
}

/**
 * Calculates and returns the top left coordinates of the text block.
 * @param textBlockSize The size of the text block.
 * @param targetCenter The center of the target.
 * @param targetRadius The radius of the target.
 * @param horizontalMargin The horizontal margin between the text block and the screen edge.
 * @param verticalMargin The vertical margin between the text block and the screen edge.
 */
private fun getTextBlockOffset(
  textBlockSize: Size,
  screenSize: Size,
  targetCenter: Offset,
  targetRadius: Float,
  horizontalMargin: Float,
  verticalMargin: Float
): Offset {
  // The X coordinate of the text block, if positioned to the left of the target.
  val xLeft = max(targetCenter.x - textBlockSize.width, horizontalMargin)
  // The X coordinate of the text block, if positioned to the right of the target.
  val xRightTemp = targetCenter.x
  val xRight = if (xRightTemp + textBlockSize.width > screenSize.width - horizontalMargin) {
    // The text block would end outside of the margin. Make sure it doesn't.
    screenSize.width - horizontalMargin - textBlockSize.width
  }
  else {
    xRightTemp
  }

  val xOffset = if (xLeft > 0) {
    xLeft
  }
  else {
    xRight
  }

  // The Y coordinate of the text block, if positioned above the target.
  val yTop = targetCenter.y - targetRadius - textBlockSize.height - verticalMargin
  // The Y coordinate of the text block, if positioned below the target.
  val yBottom = targetCenter.y + targetRadius + verticalMargin

  val yOffset = if (yTop > 0) {
    yTop
  } else {
    yBottom
  }

  return Offset(xOffset, yOffset)
}

@Composable
private fun AnimateIn(
  tapTarget: TapTarget,
  outerCircleAnimatable: Animatable<Float, AnimationVector1D>,
  highlightCircleAnimatable: Animatable<Float, AnimationVector1D>,
  tapTargetCircleAnimatable: Animatable<Float, AnimationVector1D>,
  textAlphaAnimatable: Animatable<Float, AnimationVector1D>
) {
  // Outer circle
  LaunchedEffect(tapTarget) {
    outerCircleAnimatable.animateTo(
      targetValue = 1f,
      animationSpec = tween(
        durationMillis = 300,
        easing = FastOutSlowInEasing,
      ),
    )
  }

  // Highlight circle
  LaunchedEffect(tapTarget) {
    highlightCircleAnimatable.animateTo(
      targetValue = 1f,
      animationSpec = infiniteRepeatable(
        animation = tween(1000, easing = FastOutSlowInEasing),
        repeatMode = RepeatMode.Restart,
      )
    )
  }

  // Tap target circle
  LaunchedEffect(tapTarget) {
    tapTargetCircleAnimatable.animateTo(
      targetValue = 1f,
      animationSpec = infiniteRepeatable(
        animation = tween(500, easing = FastOutLinearInEasing),
        repeatMode = RepeatMode.Reverse,
      )
    )
  }

  // Text alpha
  LaunchedEffect(tapTarget) {
    delay(200)
    textAlphaAnimatable.animateTo(
      targetValue = 1f,
      animationSpec = tween(
        durationMillis = 300,
        easing = FastOutSlowInEasing,
      ),
    )
  }
}

@Composable
private fun AnimateOut(
  tapTarget: TapTarget,
  outerCircleAnimatable: Animatable<Float, AnimationVector1D>,
  highlightCircleAnimatable: Animatable<Float, AnimationVector1D>,
  textAlphaAnimatable: Animatable<Float, AnimationVector1D>
) {
  // Outer circle
  LaunchedEffect(tapTarget) {
    outerCircleAnimatable.animateTo(
      targetValue = 0f,
      animationSpec = tween(
        durationMillis = 300,
        easing = FastOutSlowInEasing,
      ),
    )
  }

  // Highlight circle
  LaunchedEffect(tapTarget) {
    highlightCircleAnimatable.animateTo(
      targetValue = 0f,
      animationSpec = tween(
        durationMillis = 300,
        easing = FastOutSlowInEasing,
      )
    )
  }

  // Text alpha
  LaunchedEffect(tapTarget) {
    textAlphaAnimatable.animateTo(
      targetValue = 0f,
      animationSpec = tween(
        durationMillis = 300,
        easing = FastOutSlowInEasing,
      ),
    )
  }
}

/**
 * A composable that is drawn as an overlay over the entire screen.
 * @param key The key used to know when to remove the overlay.
 * @param content The content of the overlay.
 */
@Composable
private fun Overlay(
  key: Any?,
  content: @Composable () -> Unit
) {
  val context = LocalContext.current
  val activity = context.getActivity()
  if (activity == null) {
    Log.d(LOG_TAG, "Can't show overlay, activity is null")
    return
  }

  val decor = activity.window.decorView as? ViewGroup
  if (decor == null) {
    Log.d(LOG_TAG, "Can't show overlay, decor is null")
    return
  }

  val layoutParams = ViewGroup.LayoutParams(
    ViewGroup.LayoutParams.MATCH_PARENT,
    ViewGroup.LayoutParams.MATCH_PARENT
  )

  // We want the overlay to be always rendered above the entire content of the app.
  // To do this we need to access the decor view of the activity and add our overlay to it.
  // Otherwise the tap target rendering would be limited to the size of the [TapTargetCoordinator].
  DisposableEffect(key) {
    val composeView = ComposeView(context).apply {
      setContent { content() }
    }
    decor.addView(composeView, layoutParams)

    onDispose {
      decor.removeView(composeView)
    }
  }
}

private fun Context.getActivity(): Activity? = when (this) {
  is Activity -> this
  is ContextWrapper -> baseContext.getActivity()
  else -> null
}

@Composable
private fun TextDefinition.rememberMeasure(
  textMeasurer: TextMeasurer,
  constraints: Constraints
): TextLayoutResult {
  return remember(
    text,
    constraints,
    style.color,
    style.fontSize,
    style.fontWeight,
    style.textAlign,
    style.lineHeight,
    style.fontFamily,
    style.textDecoration,
    style.fontStyle,
    style.letterSpacing
  ) {
    textMeasurer.measure(
      AnnotatedString(text),
      constraints = constraints,
      style = TextStyle(
        color = style.color,
        fontSize = style.fontSize,
        fontWeight = style.fontWeight,
        textAlign = style.textAlign,
        lineHeight = style.lineHeight,
        fontFamily = style.fontFamily,
        textDecoration = style.textDecoration,
        fontStyle = style.fontStyle,
        letterSpacing = style.letterSpacing
      )
    )
  }
}