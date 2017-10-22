package xyz.peridy.shimmerlayout

import android.content.Context
import android.graphics.*
import android.os.Build
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout

class ShimmerLayout @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyle: Int = 0)
    : FrameLayout(context, attrs, defStyle) {

    private var animating = false // TODO: in group?
    private val maskPaint = Paint().apply {
        isAntiAlias = true
        isDither = true
        isFilterBitmap = true
        xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP)
    }

    private val shimmerAngle: Int
    private val shimmerDuration: Long
    private val shimmerWidth = DEFAULT_SHADOW_WIDTH
    private val shimmerCenterWidth = DEFAULT_CENTER_WIDTH

    private var _shimmerColor: Int = 0
    var shimmerColor
        set(value) {
            maskPaint.colorFilter = PorterDuffColorFilter(value, PorterDuff.Mode.SRC_IN)
            _shimmerColor = value
        }
        get() = _shimmerColor

    var shimmerGroup: ShimmerGroup? = null

    init {
        setWillNotDraw(false)
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
            ensureAnimationStarted()
            dispatchDrawUsingPaint(canvas)
        }
    }

    override fun setVisibility(visibility: Int) {
        super.setVisibility(visibility)
        if (visibility == View.VISIBLE) {
            ensureAnimationStarted()
        } else {
            stopShimmerAnimation()
        }
    }

    private fun ensureAnimationStarted() {
        if (!animating && width > 0 && visibility == View.VISIBLE) {
            if (shimmerGroup == null) {
                shimmerGroup = ShimmerGroup()
            }
            maskPaint.shader = ShimmerUtil.getShimmerShader(width, shimmerAngle.toDouble(), shimmerWidth, shimmerCenterWidth)
            shimmerGroup?.addView(this, shimmerDuration)
            animating = true
        }
    }

    internal fun stopShimmerAnimation() {
        shimmerGroup?.removeView(this)
        animating = false
    }

    private fun dispatchDrawUsingPaint(canvas: Canvas) {
        val shimmerGroup = shimmerGroup ?: return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            canvas.saveLayer(null, null)
        } else {
            @Suppress("DEPRECATION") // deprecated in 26, but added in 21
            canvas.saveLayer(null, null, Canvas.ALL_SAVE_FLAG)
        }
        super.dispatchDraw(canvas)
        val translateX = shimmerGroup.offsetX * width * 1.5f - width * 0.75f // TODO: should depend on angle
        maskPaint.shader.setLocalMatrix(Matrix().apply { setTranslate(translateX, 0f) })
        canvas.drawPaint(maskPaint)
        canvas.restore()
    }

    companion object {
        private val DEFAULT_DURATION = 1200
        private val DEFAULT_ANGLE = 20
        private val DEFAULT_COLOR = R.color.default_foreground_color
        private val DEFAULT_CENTER_WIDTH = 0.01f // TODO: move this to attrs
        private val DEFAULT_SHADOW_WIDTH = 0.07f
    }
}
