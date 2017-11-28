package net.kwatts.powtools.drawables

import android.graphics.*
import android.graphics.drawable.Drawable
import kotlin.math.absoluteValue

/** Approximate scale to convert a GPS coordinate delta to a pixel size. */
const val GPS_COORDINATES_TO_PIXEL_SCALE = 5000f

/**
 * [Drawable] that renders a [track] (continuous series of geo points). The [track] [List] consists
 * of [Pair]s that make of the geo points along the [track], with each [Pair] holding the x and y
 * coordinates (whereas [Pair.first] holds x and [Pair.second] holds y). Note that this is swapped
 * compared to the standard [ISO 6709](https://en.wikipedia.org/wiki/ISO_6709) `latitude, longitude`
 * ordering.
 *
 * The [scale] parameter can be used to scale the geo coordinates pre-rendering, as geo coordinates
 * may have very small deltas between points (which don't nicely correlate to pixel distances).
 */
class TrackDrawable @JvmOverloads constructor(
        track: List<Pair<Double?, Double?>>,
        private val scale: Float = GPS_COORDINATES_TO_PIXEL_SCALE
) : Drawable() {

    private val paint: Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        strokeWidth = 1f
    }

    private val track: List<Pair<Double, Double>> = track
            .filterNotNullPairValues()
            .map { Pair(it.first * scale, it.second * scale) }
    private val trackBounds = this.track.bounds()

    fun setColor(color: Int): TrackDrawable {
        paint.color = color
        invalidateSelf()
        return this
    }

    fun setTrackWidth(widthPx: Float): TrackDrawable {
        paint.strokeWidth = widthPx
        invalidateSelf()
        return this
    }

    override fun getIntrinsicWidth(): Int = trackBounds.width().absoluteValue.toInt()
    override fun getIntrinsicHeight(): Int = trackBounds.height().absoluteValue.toInt()

    override fun draw(canvas: Canvas) {
        for (i in 0 until track.lastIndex) {
            canvas.drawTrackLine(track[i], track[i + 1])
        }
    }

    /** Y-axis drawing is inverted as track coordinate space is Y-up and [Canvas] is Y-down. */
    private fun Canvas.drawTrackLine(start: Pair<Double, Double>, end: Pair<Double, Double>) {
        val startX = start.first - trackBounds.left
        val startY = trackBounds.top - start.second
        val endX = end.first - trackBounds.left
        val endY = trackBounds.top - end.second

        drawLine(startX.toFloat(), startY.toFloat(), endX.toFloat(), endY.toFloat(), paint)
    }

    override fun setAlpha(alpha: Int) {
        paint.alpha = alpha
        invalidateSelf()
    }

    override fun getOpacity(): Int {
        return PixelFormat.TRANSLUCENT
    }

    override fun setColorFilter(colorFilter: ColorFilter) {
        paint.colorFilter = colorFilter
        invalidateSelf()
    }
}

private fun List<Pair<Double, Double>>.bounds(): RectF {
    val initial = RectF(Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY)
    return fold(initial) { rect, (x, y) ->
        rect.apply {
            left = minOf(left, x.toFloat())
            top = maxOf(top, y.toFloat())
            right = maxOf(right, x.toFloat())
            bottom = minOf(bottom, y.toFloat())
        }
    }
}

@Suppress("UNCHECKED_CAST")
private fun List<Pair<Double?, Double?>>.filterNotNullPairValues(): List<Pair<Double, Double>> =
        filter { (first, second) -> first != null && second != null } as List<Pair<Double, Double>>
