package xyz.peridy.shimmerlayout

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.os.AsyncTask
import android.view.View
import java.lang.ref.WeakReference
import java.util.ArrayList

/**
 * A group of layouts sharing the exact same shimmer effect. The layout displayed in all views using
 * the same groups must:
 * - Use layouts with the same dimensions, content can be different.
 * - Use the same shimmer angle and duration.
 *
 * Not following this may yield unexpected results.
 *
 * @param bitmapLoader an optional parameter that can be used to integrate with bitmap memory
 *                     management libraries. By default this will use {@link DefaultBitmapLoader}
 *                     which uses Android's standard Bitmap creation and recycling methods.
 */
class ShimmerGroup @JvmOverloads constructor(private val bitmapLoader: BitmapLoader = DefaultBitmapLoader()) {

    private var maskAnimator: ValueAnimator? = null
    private var maskBitmap: Bitmap? = null
    private val animatedViews = ArrayList<WeakReference<ShimmerLayout>>()
    private var initializeSourceMaskBitmapTask: AsyncTask<Void, Void, Void>? = null
    private lateinit var shimmerConfig: ShimmerConfig
    private var maskRect: Rect? = null

    internal var maskOffsetX: Int = 0

    internal fun initialize(width: Int,
                            height: Int,
                            shimmerAngle: Int,
                            shimmerAnimationDuration: Long) {
        shimmerConfig = ShimmerConfig(width, height, shimmerAngle, shimmerAnimationDuration)
    }

    internal fun getMaskBitmap(context: Context): Bitmap? {
        val maskRect = maskRect
        if (maskBitmap == null && maskRect != null && initializeSourceMaskBitmapTask == null) {
            val width = maskRect.width()
            val height = maskRect.height()

            initializeSourceMaskBitmapTask = object : AsyncTask<Void, Void, Void>() {
                override fun doInBackground(vararg p0: Void?): Void? {
                    if (maskBitmap == null) {
                        maskBitmap = createBitmap(context, width, height, Bitmap.Config.ALPHA_8)
                        if (maskBitmap != null) {
                            val paint = Paint()
                            paint.shader = LinearGradient(
                                    (-maskRect.left).toFloat(), 0f,
                                    (width + maskRect.left).toFloat(), 0f,
                                    intArrayOf(Color.TRANSPARENT, Color.BLACK, Color.BLACK, Color.TRANSPARENT),
                                    floatArrayOf(0.5f - SHADOW_WIDTH, 0.5f - CENTER_WIDTH, 0.5f + CENTER_WIDTH, 0.5f + SHADOW_WIDTH),
                                    Shader.TileMode.CLAMP)
                            val canvas = Canvas(maskBitmap)
                            canvas.rotate(shimmerConfig.shimmerAngle.toFloat(), (width / 2).toFloat(), (height / 2).toFloat())
                            canvas.drawRect((-maskRect.left).toFloat(), maskRect.top.toFloat(), (width + maskRect.left).toFloat(), maskRect.bottom.toFloat(), paint)
                        } else {
                            onLowMemory(context)
                        }
                    }
                    initializeSourceMaskBitmapTask = null
                    return null
                }
            }.execute()
        }

        return maskBitmap
    }

    internal fun removeView(context: Context, shimmerLayout: ShimmerLayout) {
        animatedViews.removeAll { it.get() == shimmerLayout || it.get() == null }
        if (maskAnimator?.isStarted == true && animatedViews.isEmpty()) {
            maskAnimator?.removeAllUpdateListeners()
            maskAnimator?.end()
            maskAnimator = null
            maskBitmap?.let { bitmapLoader.release(context, it) }
            maskBitmap = null
        }
    }

    internal fun addView(shimmerLayout: ShimmerLayout) {
        animatedViews.removeAll { it.get() == shimmerLayout || it.get() == null }
        animatedViews.add(WeakReference(shimmerLayout))
        startAnimator()
    }

    private fun startAnimator() {
        maskRect = maskRect ?: ShimmerUtil.calculateMaskRect(shimmerConfig.width, shimmerConfig.height, shimmerConfig.shimmerAngle, SHADOW_WIDTH)

        if (maskAnimator != null) {
            return
        }
        maskAnimator = ValueAnimator.ofInt(-shimmerConfig.width, shimmerConfig.width).apply {
            duration = shimmerConfig.shimmerAnimationDuration
            repeatCount = ValueAnimator.INFINITE
            addUpdateListener { animation -> invalidateViews(animation.animatedValue as Int) }
            start()
        }
    }

    private fun invalidateViews(maskOffsetX: Int) {
        this.maskOffsetX = maskOffsetX
        with(maskRect) {
            if ((this != null) && (maskOffsetX + width() >= 0)) {
                animatedViews.toTypedArray().forEach {
                    val view = it.get()
                    if (view != null) {
                        if (view.visibility == View.VISIBLE) {
                            view.invalidate()
                        } else {
                            view.stopShimmerAnimation()
                        }
                    } else {
                        animatedViews.remove(it)
                    }
                }
            }
        }
    }

    private fun onLowMemory(context: Context) {
        animatedViews.clear()
        maskBitmap?.let { bitmapLoader.release(context, it) }
    }

    private fun createBitmap(context: Context, width: Int, height: Int, config: Bitmap.Config = Bitmap.Config.ARGB_8888) =
            try {
                bitmapLoader.get(context, width, height, config)
            } catch (_: Exception) {
                // Don't crash if creation fails for any reason, just disable animation.
                null
            }

    private class ShimmerConfig(val width: Int,
                                val height: Int,
                                val shimmerAngle: Int,
                                val shimmerAnimationDuration: Long)

    companion object {
        private val CENTER_WIDTH = 0.03f
        private val SHADOW_WIDTH = 0.15f
    }
}
