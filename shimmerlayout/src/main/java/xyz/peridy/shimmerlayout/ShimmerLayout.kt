package xyz.peridy.shimmerlayout

import android.content.Context
import android.graphics.*
import android.os.Build
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout

class ShimmerLayout @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyle: Int = 0)
    : FrameLayout(context, attrs, defStyle) {

    private var translateRange = 0
    private var animating = false // TODO: in group?
    private val maskPaint = Paint().apply {
        isAntiAlias = true
        isDither = true
        isFilterBitmap = true
        xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP)
    }

    // Interpolators, can be used to customize animation
    var shaderInterpolator: ShaderInterpolator? = null
    var colorInterpolator: ColorInterpolator? = null
    var matrixInterpolator: MatrixInterpolator? = object : MatrixInterpolator {
        // Default translation from left to right
        override fun getMatrixForOffset(offsetPercent: Float) = Matrix().apply {
            // animate from -1 to +1, max dimension will define the shimmer Paint size.
            val translateX = (offsetPercent * 2 - 1) * translateRange
            setTranslate(translateX, 0f)
        }
    }

    // Optional group to synchronize multiple shimmer layouts
    var shimmerGroup: ShimmerGroup? = null

    var shimmerAngle: Int
    var shimmerDuration: Long
    var shimmerWidth: Int
    var shimmerCenterWidth: Int

    private var _shimmerColor = 0
    var shimmerColor
        set(value) {
            maskPaint.colorFilter = PorterDuffColorFilter(value, PorterDuff.Mode.SRC_IN)
            _shimmerColor = value
        }
        get() = _shimmerColor

    init {
        setWillNotDraw(false)
        val attributes = context.theme.obtainStyledAttributes(attrs, R.styleable.ShimmerLayout, 0, 0)
        try {
            shimmerAngle = attributes.getInteger(R.styleable.ShimmerLayout_angle, DEFAULT_ANGLE)
            shimmerDuration = attributes.getInteger(R.styleable.ShimmerLayout_animation_duration, DEFAULT_DURATION).toLong()
            shimmerColor = attributes.getColor(R.styleable.ShimmerLayout_foreground_color, ShimmerUtil.getColor(getContext(), DEFAULT_COLOR))
            shimmerWidth = attributes.getDimensionPixelSize(R.styleable.ShimmerLayout_shimmer_width, resources.getDimensionPixelSize(DEFAULT_SHADOW_WIDTH))
            shimmerCenterWidth = attributes.getDimensionPixelSize(R.styleable.ShimmerLayout_shimmer_width, resources.getDimensionPixelSize(DEFAULT_CENTER_WIDTH))
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

    internal fun stopShimmerAnimation() {
        shimmerGroup?.removeView(this)
        animating = false
    }

    private fun ensureAnimationStarted() {
        if (!animating && width > 0 && visibility == View.VISIBLE) {
            if (shimmerGroup == null) {
                shimmerGroup = ShimmerGroup()
            }
            if (shaderInterpolator == null) {
                maskPaint.shader = ShimmerUtil.getShimmerShader(width, shimmerAngle.toDouble(), shimmerWidth, shimmerCenterWidth)
            }
            translateRange = Math.max(width, height)
            shimmerGroup?.addView(this, shimmerDuration)
            animating = true
        }
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
        customizePaint(shimmerGroup.offsetPercent)
        canvas.drawPaint(maskPaint)
        canvas.restore()
    }

    private fun customizePaint(offsetPercent: Float) {
        shaderInterpolator?.let { maskPaint.shader = it.getShaderForOffset(offsetPercent) }
        colorInterpolator?.let { shimmerColor = it.getColorForOffset(offsetPercent) }
        matrixInterpolator?.let { maskPaint.shader.setLocalMatrix(it.getMatrixForOffset(offsetPercent)) }
    }

    interface ShaderInterpolator {
        /**
         * Gets the {@link Shader} to use for current animation offset. Default uses a {@link LinearGradient}
         *
         * @param offsetPercent value between 0 and 1 indicating animation progress
         * @return Shader to use for current offset
         */
        fun getShaderForOffset(offsetPercent: Float): Shader
    }

    interface ColorInterpolator {
        /**
         * Gets the color to use for current animation offset. Default is null, color remains the same.
         *
         * @param offsetPercent value between 0 and 1 indicating animation progress
         * @return Color to use for current offset, as a 32-bit int value
         */
        fun getColorForOffset(offsetPercent: Float): Int
    }

    interface MatrixInterpolator {
        /**
         * Gets the {@link Matrix} to use for current animation offset. This matrix will be used to
         * transform the shader.
         *
         * @param offsetPercent value between 0 and 1 indicating animation progress
         * @return Matrix to use to transform shader
         */
        fun getMatrixForOffset(offsetPercent: Float): Matrix
    }

    companion object {
        private val DEFAULT_DURATION = 1200
        private val DEFAULT_ANGLE = 20
        private val DEFAULT_COLOR = R.color.default_foreground_color
        private val DEFAULT_CENTER_WIDTH = R.dimen.shimmer_width_center_default
        private val DEFAULT_SHADOW_WIDTH = R.dimen.shimmer_width_default
    }
}
