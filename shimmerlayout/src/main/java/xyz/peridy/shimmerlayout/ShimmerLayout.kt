package xyz.peridy.shimmerlayout

import android.content.Context
import android.graphics.*
import android.os.Build
import android.util.AttributeSet
import android.view.View
import android.view.ViewTreeObserver
import android.widget.FrameLayout

class ShimmerLayout @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyle: Int = 0)
    : FrameLayout(context, attrs, defStyle) {

    private var renderingCanvas: Canvas? = null

    private val shimmerAngle: Int
    private val shimmerColor: Int
    private val shimmerDuration: Long

    var shimmerGroup: ShimmerGroup? = null

    init {
        setWillNotDraw(false)

        val a = context.theme.obtainStyledAttributes(attrs, R.styleable.ShimmerLayout, 0, 0)
        try {
            shimmerAngle = a.getInteger(R.styleable.ShimmerLayout_angle, DEFAULT_ANGLE)
            shimmerDuration = a.getInteger(R.styleable.ShimmerLayout_animation_duration, DEFAULT_DURATION).toLong()
            shimmerColor = a.getColor(R.styleable.ShimmerLayout_foreground_color, ShimmerUtil.getColor(getContext(), DEFAULT_COLOR))
        } finally {
            a.recycle()
        }

        if (visibility == View.VISIBLE) {
            startShimmerAnimation()
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (visibility == View.VISIBLE) {
            startShimmerAnimation()
        }
    }

    override fun onDetachedFromWindow() {
        stopShimmerAnimation()
        super.onDetachedFromWindow()
    }

    override fun dispatchDraw(canvas: Canvas) {
        if (width <= 0 || height <= 0) {
            super.dispatchDraw(canvas)
        } else {
            dispatchDrawUsingBitmap(canvas)
        }
    }

    override fun setVisibility(visibility: Int) {
        super.setVisibility(visibility)
        if (visibility == View.VISIBLE) {
            startShimmerAnimation()
        } else {
            stopShimmerAnimation()
        }
    }

    internal fun startShimmerAnimation() {
        if (width == 0) {
            viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                        viewTreeObserver.removeOnGlobalLayoutListener(this)
                    } else {
                        @Suppress("DEPRECATION")
                        viewTreeObserver.removeGlobalOnLayoutListener(this)
                    }
                    startShimmerAnimation()
                }
            })
            return
        }

        shimmerGroup?.initialize(width, height, shimmerAngle, shimmerColor, shimmerDuration)
        shimmerGroup?.startAnimator()
        shimmerGroup?.addView(this)
    }

    internal fun stopShimmerAnimation() {
        shimmerGroup?.removeView(context, this)
        renderingCanvas = null
    }

    private fun dispatchDrawUsingBitmap(canvas: Canvas) {
        super.dispatchDraw(canvas)

        val shimmerGroup = shimmerGroup ?: return
        val localAvailableBitmap = shimmerGroup.initializeDestinationBitmap(context) ?: return
        val localMaskRect = shimmerGroup.maskRect ?: return

        var localRenderingCanvas = renderingCanvas
        if (localRenderingCanvas == null) {
            localRenderingCanvas = Canvas(localAvailableBitmap)
            renderingCanvas = localRenderingCanvas
        }

        drawMasked(shimmerGroup, localRenderingCanvas)
        canvas.save()
        canvas.clipRect(shimmerGroup.maskOffsetX, 0, shimmerGroup.maskOffsetX + localMaskRect.width(), height)
        canvas.drawBitmap(localAvailableBitmap, 0f, 0f, null)
        canvas.restore()
    }

    private fun drawMasked(shimmerGroup: ShimmerGroup, renderCanvas: Canvas) {
        if (shimmerGroup.maskRenderedForOffsetX == shimmerGroup.maskOffsetX) {
            // This is called on UI thread, there's no concurrency issue here, as only a single
            // layout will draw the mask at once.
            return
        }
        val localMaskBitmap = shimmerGroup.initializeSourceMaskBitmap(context) ?: return
        renderCanvas.save()
        renderCanvas.clipRect(shimmerGroup.maskOffsetX, 0, shimmerGroup.maskOffsetX + localMaskBitmap.width, height)

        super.dispatchDraw(renderCanvas)
        renderCanvas.drawBitmap(localMaskBitmap, shimmerGroup.maskOffsetX.toFloat(), 0f, shimmerGroup.maskPaint)
        renderCanvas.restore()
        shimmerGroup.maskRenderedForOffsetX = shimmerGroup.maskOffsetX
    }

    companion object {
        private val DEFAULT_DURATION = 1500
        private val DEFAULT_ANGLE = 20
        private val DEFAULT_COLOR = R.color.default_foreground_color
    }
}
