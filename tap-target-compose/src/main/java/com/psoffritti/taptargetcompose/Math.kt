package com.psoffritti.taptargetcompose

import androidx.compose.ui.geometry.Offset
import kotlin.math.pow
import kotlin.math.sqrt

/** Returns true if the point is outside the circle defined by [center] and [radius]. */
internal fun Offset.isOutsideCircle(center: Offset, radius: Float): Boolean {
  return distanceTo(center) > radius
}

/** Returns true if the point is inside the circle defined by [center] and [radius]. */
internal fun Offset.isInsideCircle(center: Offset, radius: Float): Boolean {
  return !isOutsideCircle(center, radius)
}

/** Returns the distance between this [Offset] and [other]. */
internal fun Offset.distanceTo(other: Offset): Float {
  return sqrt((x - other.x).pow(2) + (y - other.y).pow(2))
}

internal fun max(vararg values: Float) = values.max()