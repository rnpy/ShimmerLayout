package xyz.peridy.shimmerlayout

import android.content.Context
import android.graphics.*
import android.os.Build

internal object ShimmerUtil {
    internal fun getShimmerShader(width: Int, angle: Double, shadowWidth: Int, shadowCenter: Int): Shader {
        val shadowWidthPercent = shadowWidth / 2f / width
        val shadowCenterPercent = shadowCenter / 2f / width
        val angleInRadians = Math.toRadians(angle)
        return LinearGradient(
                0f, 0f,
                Math.cos(angleInRadians).toFloat() * width, Math.sin(angleInRadians).toFloat() * width,
                intArrayOf(Color.TRANSPARENT, Color.BLACK, Color.BLACK, Color.TRANSPARENT),
                floatArrayOf(0.5f - shadowWidthPercent, 0.5f - shadowCenterPercent, 0.5f + shadowCenterPercent, 0.5f + shadowWidthPercent),
                Shader.TileMode.CLAMP)
    }

    internal fun getColor(context: Context, id: Int): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            context.getColor(id)
        } else {
            @Suppress("DEPRECATION")
            context.resources.getColor(id)
        }
    }
}
