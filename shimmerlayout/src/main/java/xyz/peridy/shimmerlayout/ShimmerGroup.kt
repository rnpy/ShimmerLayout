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
 * - Use the exact same layout, their dimensions and content must be the same.
 * - Use the same shimmer colour, angle and duration.
 *
 * Not following this will yield unexpected results.
 *
 * @param bitmapLoader an optional parameter that cna be used to integrate with bitmap memory
 *                     management libraries. By default this will use {@link DefaultBitmapLoader}
 *                     which uses Android's standard Bitmap creation and recycling methods.
 */
class ShimmerGroup @JvmOverloads constructor(private val bitmapLoader: BitmapLoader = DefaultBitmapLoader()) {

    private var maskAnimator: ValueAnimator? = null
    private var destinationBitmap: Bitmap? = null
    private var sourceMaskBitmap: Bitmap? = null
    private val animatedViews = ArrayList<WeakReference<ShimmerLayout>>()

    private var initializeDestinationBitmapTask: AsyncTask<Void, Void, Void>? = null
    private var initializeSourceMaskBitmapTask: AsyncTask<Void, Void, Void>? = null

    private lateinit var shimmerConfig: ShimmerConfig
    internal lateinit var maskPaint: Paint

    internal var maskOffsetX: Int = 0
    internal var maskRenderedForOffsetX: Int = 0
    internal var maskRect: Rect? = null

    fun initialize(width: Int,
                   height: Int,
                   shimmerAngle: Int,
                   shimmerColor: Int,
                   shimmerAnimationDuration: Long) {
        shimmerConfig = ShimmerConfig(width, height, shimmerAngle, shimmerAnimationDuration)
        maskPaint = Paint().apply {
            isAntiAlias = true
            isDither = true
            isFilterBitmap = true
            xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
            colorFilter = PorterDuffColorFilter(shimmerColor, PorterDuff.Mode.SRC_IN)
        }
    }

    private fun invalidateViews(maskOffsetX: Int) {
        this.maskOffsetX = maskOffsetX
        val maskRect = maskRect

        if (maskRect != null) {
            val shimmerBitmapWidth = maskRect.width()
            if (maskOffsetX + shimmerBitmapWidth >= 0) {
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
        sourceMaskBitmap?.let { bitmapLoader.release(context, it) }
        destinationBitmap?.let { bitmapLoader.release(context, it) }
    }

    internal fun initializeDestinationBitmap(context: Context): Bitmap? {
        if (destinationBitmap == null && initializeDestinationBitmapTask == null) {
            initializeDestinationBitmapTask = object : AsyncTask<Void, Void, Void>() {
                override fun doInBackground(vararg p0: Void?): Void? {
                    if (destinationBitmap == null) {
                        destinationBitmap = createBitmap(context, shimmerConfig.width, shimmerConfig.height)
                        if (destinationBitmap == null) {
                            onLowMemory(context)
                        }
                        initializeDestinationBitmapTask = null
                    }
                    return null
                }
            }.execute()
        }

        return destinationBitmap
    }

    internal fun initializeSourceMaskBitmap(context: Context): Bitmap? {
        val maskRect = maskRect
        if (sourceMaskBitmap == null && maskRect != null && initializeSourceMaskBitmapTask == null) {
            val width = maskRect.width()
            val height = maskRect.height()

            initializeSourceMaskBitmapTask = object : AsyncTask<Void, Void, Void>() {
                override fun doInBackground(vararg p0: Void?): Void? {
                    if (sourceMaskBitmap == null) {
                        sourceMaskBitmap = createBitmap(context, width, height, Bitmap.Config.ALPHA_8)
                        if (sourceMaskBitmap != null) {
                            val paint = Paint().apply {
                                shader = LinearGradient(
                                        (-maskRect.left).toFloat(), 0f,
                                        (width + maskRect.left).toFloat(), 0f,
                                        intArrayOf(Color.TRANSPARENT, Color.BLACK, Color.BLACK, Color.TRANSPARENT),
                                        floatArrayOf(0.30f, 0.47f, 0.53f, 0.70f),
                                        Shader.TileMode.CLAMP)
                            }
                            val canvas = Canvas(sourceMaskBitmap)
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

        return sourceMaskBitmap
    }

    internal fun removeView(context: Context, shimmerLayout: ShimmerLayout) {
        animatedViews.removeAll { it.get() == shimmerLayout || it.get() == null }
        if (maskAnimator?.isStarted == true && animatedViews.isEmpty()) {
            maskAnimator?.removeAllUpdateListeners()
            maskAnimator?.end()
            maskAnimator = null
            sourceMaskBitmap?.let { bitmapLoader.release(context, it) }
            destinationBitmap?.let { bitmapLoader.release(context, it) }
            sourceMaskBitmap = null
            destinationBitmap = null
        }
    }

    internal fun addView(shimmerLayout: ShimmerLayout) {
        animatedViews.removeAll { it.get() == shimmerLayout || it.get() == null }
        animatedViews.add(WeakReference(shimmerLayout))
    }

    internal fun startAnimator() {
        val localMaskRect = maskRect ?: ShimmerUtil.calculateMaskRect(shimmerConfig.width, shimmerConfig.height, shimmerConfig.shimmerAngle)
        maskRect = localMaskRect

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
}
