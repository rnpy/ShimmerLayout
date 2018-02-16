package xyz.peridy.shimmerlayout

import android.content.Context
import android.graphics.*
import android.os.Build
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout

class ShimmerLayout @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyle: Int = 0)
    : FrameLayout(context, attrs, defStyle) {

    /**
     * Optional group to synchronize multiple [ShimmerLayout] animations
     */
    var shimmerGroup: ShimmerGroup? = null

    /**
     * Default shader angle, only used if [shaderInterpolator] is null
     */
    var shimmerAngle: Int
    /**
     * Default shader width, only used if [shaderInterpolator] is null
     */
    var shimmerWidth: Int
    /**
     * Default shader center width, only used if [shaderInterpolator] is null
     */
    var shimmerCenterWidth: Int

    /**
     * Animation duration
     */
    var shimmerDuration: Long
    /**
     * Shimmer color, used as filter on shader
     */
    var shimmerColor = 0
        set(value) {
            maskPaint.colorFilter = PorterDuffColorFilter(value, PorterDuff.Mode.SRC_IN)
            field = value
        }

    /**
     * Gets the [Shader] to use for current animation value. Default uses a [LinearGradient]
     */
    var shaderInterpolator: Interpolator<Shader>? = null
    /**
     * Gets the color to use for current animation value. Default is null, color remains the same.
     */
    var colorInterpolator: Interpolator<Int>? = null
    /**
     * [Interpolator] used to get the [Matrix] to use for each animation value. This matrix will
     * be used to transform the shader on each [dispatchDraw] operation.
     */
    var matrixInterpolator: Interpolator<Matrix>? = object : Interpolator<Matrix> {
        // Default translation from left to right
        override fun getInterpolation(input: Float) = Matrix().apply {
            // animate from -1 to +1, max dimension will define the shimmer Paint size.
            val translateX = (input * 2 - 1) * translateRange
            setTranslate(translateX, 0f)
        }
    }

    private var translateRange = 0
    private var animating = false
    private val maskPaint = Paint().apply {
        isDither = true
        isFilterBitmap = true
        xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP)
    }

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
        customizePaint(shimmerGroup.animatedValue)
        canvas.drawPaint(maskPaint)
        canvas.restore()
    }

    private fun customizePaint(animatedValue: Float) {
        shaderInterpolator?.let { maskPaint.shader = it.getInterpolation(animatedValue) }
        colorInterpolator?.let { shimmerColor = it.getInterpolation(animatedValue) }
        matrixInterpolator?.let { maskPaint.shader.setLocalMatrix(it.getInterpolation(animatedValue)) }
    }

    interface Interpolator<out T> {
        /**
         * Maps a value representing the elapsed fraction of an animation to a value that represents
         * the interpolated [T].
         *
         * @param input value between 0 and 1 indicating animation progress
         * @return [T] value for [input]
         */
        fun getInterpolation(input: Float): T
    }

    // convenience methods to simplify interpolator declaration from kotlin
    inline fun setMatrixInterpolator(crossinline value: (Float) -> Matrix) {
        matrixInterpolator = object : Interpolator<Matrix> {
            override fun getInterpolation(input: Float) = value.invoke(input)
        }
    }

    inline fun setShaderInterpolator(crossinline value: (Float) -> Shader) {
        shaderInterpolator = object : Interpolator<Shader> {
            override fun getInterpolation(input: Float) = value.invoke(input)
        }
    }

    inline fun setColorInterpolator(crossinline value: (Float) -> Int) {
        colorInterpolator = object : Interpolator<Int> {
            override fun getInterpolation(input: Float) = value.invoke(input)
        }
    }

    companion object {
        private const val DEFAULT_DURATION = 1200
        private const val DEFAULT_ANGLE = 20
        private val DEFAULT_COLOR = R.color.default_foreground_color
        private val DEFAULT_CENTER_WIDTH = R.dimen.shimmer_width_center_default
        private val DEFAULT_SHADOW_WIDTH = R.dimen.shimmer_width_default
    }
}
