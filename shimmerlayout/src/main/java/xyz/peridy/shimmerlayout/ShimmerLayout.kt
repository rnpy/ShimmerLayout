package xyz.peridy.shimmerlayout

import android.content.Context
import android.graphics.*
import android.os.Build
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout

class ShimmerLayout @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyle: Int = 0)
    : FrameLayout(context, attrs, defStyle) {

    private var renderingCanvas: Canvas? = null
    private var animating = false
    private var maskPaint: Paint
    private val shimmerAngle: Int
    private val shimmerDuration: Long

    var shimmerGroup: ShimmerGroup? = null
    var shimmerColor: Int = 0
        set(value) {
            maskPaint.colorFilter = PorterDuffColorFilter(value, PorterDuff.Mode.SRC_IN)
        }

    init {
        setWillNotDraw(false)

        maskPaint = Paint().apply {
            isAntiAlias = true
            isDither = true
            isFilterBitmap = true
            xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP)
        }

        val attributes = context.theme.obtainStyledAttributes(attrs, R.styleable.ShimmerLayout, 0, 0)
        try {
            shimmerAngle = attributes.getInteger(R.styleable.ShimmerLayout_angle, DEFAULT_ANGLE)
            shimmerDuration = attributes.getInteger(R.styleable.ShimmerLayout_animation_duration, DEFAULT_DURATION).toLong()
            shimmerColor = attributes.getColor(R.styleable.ShimmerLayout_foreground_color, ShimmerUtil.getColor(getContext(), DEFAULT_COLOR))
        } finally {
            attributes.recycle()
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
            startShimmerAnimationIfNecessary()
            dispatchDrawUsingBitmap(canvas)
        }
    }

    override fun setVisibility(visibility: Int) {
        super.setVisibility(visibility)
        if (visibility == View.VISIBLE) {
            startShimmerAnimationIfNecessary()
        } else {
            stopShimmerAnimation()
        }
    }

    private fun startShimmerAnimationIfNecessary() {
        if (!animating && width > 0 && visibility == View.VISIBLE) {
            if (shimmerGroup == null) {
                shimmerGroup = ShimmerGroup(DefaultBitmapLoader())
            }
            shimmerGroup?.initialize(width, height, shimmerAngle, shimmerDuration)
            shimmerGroup?.addView(this)
            animating = true
        }
    }

    internal fun stopShimmerAnimation() {
        shimmerGroup?.removeView(context, this)
        renderingCanvas = null
        animating = false
    }

    private fun dispatchDrawUsingBitmap(canvas: Canvas) {
        val shimmerGroup = shimmerGroup ?: return
        val localMaskBitmap = shimmerGroup.getMaskBitmap(context)
        if (localMaskBitmap == null) {
            // mask is not ready yet, draw normal view
            super.dispatchDraw(canvas)
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            canvas.saveLayer(null, null)
        } else {
            @Suppress("DEPRECATION") // deprecated in 26, but added in 21
            canvas.saveLayer(null, null, Canvas.ALL_SAVE_FLAG)
        }
        super.dispatchDraw(canvas)
        canvas.drawBitmap(localMaskBitmap, shimmerGroup.maskOffsetX.toFloat(), 0f, maskPaint)
        canvas.restore()
    }

    companion object {
        private val DEFAULT_DURATION = 1500
        private val DEFAULT_ANGLE = 20
        private val DEFAULT_COLOR = R.color.default_foreground_color
    }
}
