package xyz.peridy.shimmerlayout

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.os.AsyncTask
import android.view.View
import java.lang.ref.WeakReference
import java.util.ArrayList
import java.util.concurrent.locks.ReentrantLock

class ShimmerGroup @JvmOverloads constructor(private val bitmapLoader: BitmapLoader = DefaultBitmapLoader()) {

    private var maskAnimator: ValueAnimator? = null
    private var destinationBitmap: Bitmap? = null
    private var sourceMaskBitmap: Bitmap? = null
    private val animatedViews = ArrayList<WeakReference<ShimmerLayout>>()
    private val bitmapCreationLock: ReentrantLock by lazy { ReentrantLock() }

    private lateinit var shimmerConfig: ShimmerConfig

    internal var maskOffsetX: Int = 0
    internal var maskRect: Rect? = null

    fun initialize(width: Int,
                   height: Int,
                   shimmerColor: Int,
                   shimmerAngle: Int,
                   shimmerAnimationDuration: Long) {
        shimmerConfig = ShimmerConfig(width, height, shimmerColor, shimmerAngle, shimmerAnimationDuration)
    }

    private fun invalidateViews(maskOffsetX: Int) {
        this.maskOffsetX = maskOffsetX
        val maskRect = maskRect

        if (maskRect != null) {
            val shimmerBitmapWidth = maskRect.width()
            if (maskOffsetX + shimmerBitmapWidth >= 0)
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

    private fun onLowMemory(context: Context) {
        animatedViews.clear()
        sourceMaskBitmap?.let { bitmapLoader.release(context, it) }
        destinationBitmap?.let { bitmapLoader.release(context, it) }
    }

    internal fun initializeDestinationBitmap(context: Context): Bitmap? {
        if (destinationBitmap == null) {
            object : AsyncTask<Void, Void, Void>() {
                override fun doInBackground(vararg p0: Void?): Void? {
                    try {
                        bitmapCreationLock.lock()
                        if (destinationBitmap == null) {
                            destinationBitmap = createBitmap(context, shimmerConfig.width, shimmerConfig.height)
                            if (destinationBitmap == null) {
                                onLowMemory(context)
                            }
                        }
                    } finally {
                        bitmapCreationLock.unlock()
                    }
                    return null
                }

            }.execute()
        }

        return destinationBitmap
    }

    internal fun initializeSourceMaskBitmap(context: Context): Bitmap? {
        val maskRect = maskRect
        if (sourceMaskBitmap == null && maskRect != null) {
            val width = maskRect.width()
            val height = maskRect.height()

            object : AsyncTask<Void, Void, Void>() {
                override fun doInBackground(vararg p0: Void?): Void? {
                    try {
                        bitmapCreationLock.lock()
                        if (sourceMaskBitmap == null) {
                            sourceMaskBitmap = createBitmap(context, width, height)
                            if (sourceMaskBitmap != null) {
                                val paint = Paint().apply {
                                    val edgeColor = ShimmerUtil.reduceColorAlphaValueToZero(shimmerConfig.shimmerColor)
                                    shader = LinearGradient(
                                            (-maskRect.left).toFloat(), 0f,
                                            (width + maskRect.left).toFloat(), 0f,
                                            intArrayOf(edgeColor, shimmerConfig.shimmerColor, shimmerConfig.shimmerColor, edgeColor),
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
                    } finally {
                        bitmapCreationLock.unlock()
                    }
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

    private fun createBitmap(context: Context, width: Int, height: Int) =
            try {
                bitmapLoader.get(context, width, height, Bitmap.Config.ARGB_8888)
            } catch (_: OutOfMemoryError) {
                null
            }

    private class ShimmerConfig(val width: Int,
                                val height: Int,
                                val shimmerColor: Int,
                                val shimmerAngle: Int,
                                val shimmerAnimationDuration: Long)
}
