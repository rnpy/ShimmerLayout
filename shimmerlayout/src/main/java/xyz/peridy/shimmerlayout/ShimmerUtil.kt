package xyz.peridy.shimmerlayout

import android.content.Context
import android.graphics.Matrix
import android.graphics.Point
import android.graphics.Rect
import android.os.Build

internal object ShimmerUtil {

    internal fun getColor(context: Context, id: Int): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            context.getColor(id)
        } else {
            @Suppress("DEPRECATION")
            context.resources.getColor(id)
        }
    }

    internal fun calculateMaskRect(width: Int, height: Int, shimmerAngle: Int, shadowWidth: Float): Rect {
        val shimmerWidth = width / 2
        if (shimmerAngle == 0) {
            return Rect((shimmerWidth * (0.5 - shadowWidth)).toInt(), 0, (shimmerWidth * (0.5 + shadowWidth)).toInt(), height)
        }

        val top = 0
        val center = (height * 0.5).toInt()
        val right = (shimmerWidth * (0.5 + shadowWidth)).toInt()
        val originalTopRight = Point(right, top)
        val originalCenterRight = Point(right, center)

        val rotatedTopRight = rotatePoint(originalTopRight, shimmerAngle.toFloat(), (shimmerWidth / 2).toFloat(), (height / 2).toFloat())
        val rotatedCenterRight = rotatePoint(originalCenterRight, shimmerAngle.toFloat(), (shimmerWidth / 2).toFloat(), (height / 2).toFloat())
        val rotatedIntersection = getTopIntersection(rotatedTopRight, rotatedCenterRight)
        val halfMaskHeight = distanceBetween(rotatedCenterRight, rotatedIntersection)

        val paddingVertical = height / 2 - halfMaskHeight
        val paddingHorizontal = shimmerWidth - rotatedIntersection.x

        return Rect(paddingHorizontal, paddingVertical, shimmerWidth - paddingHorizontal, height - paddingVertical)
    }

    /**
     * Finds the intersection of the line and the top of the canvas
     *
     * @param p1 First point of the line of which the intersection with the canvas should be determined
     * @param p2 Second point of the line of which the intersection with the canvas should be determined
     * @return The point of intersection
     */
    private fun getTopIntersection(p1: Point, p2: Point): Point {
        val x1 = p1.x.toDouble()
        val x2 = p2.x.toDouble()
        val y1 = (-p1.y).toDouble()
        val y2 = (-p2.y).toDouble()
        // slope-intercept form of the line represented by the two points
        val m = (y2 - y1) / (x2 - x1)
        val b = y1 - m * x1
        // The intersection with the line represented by the top of the canvas
        val x = ((0 - b) / m).toInt()
        val y = 0
        return Point(x, y)
    }

    private fun rotatePoint(point: Point, degrees: Float, cx: Float, cy: Float): Point {
        val pts = FloatArray(2)
        pts[0] = point.x.toFloat()
        pts[1] = point.y.toFloat()

        val transform = Matrix()
        transform.setRotate(degrees, cx, cy)
        transform.mapPoints(pts)

        return Point(pts[0].toInt(), pts[1].toInt())
    }

    private fun distanceBetween(p1: Point, p2: Point) =
            Math.ceil(Math.sqrt(Math.pow((p1.x - p2.x).toDouble(), 2.0) + Math.pow((p1.y - p2.y).toDouble(), 2.0))).toInt()
}
