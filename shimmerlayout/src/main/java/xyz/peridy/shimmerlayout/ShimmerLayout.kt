package xyz.peridy.shimmerlayout

import android.animation.TimeInterpolator
import android.content.Context
import android.graphics.*
import android.os.Build
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout

class ShimmerLayout @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyle: Int = 0)
    : FrameLayout(context, attrs, defStyle) {

    /**
     * Optional group to synchronize multiple [ShimmerLayout] animations
     */
    var shimmerGroup: ShimmerGroup? = null

    /**
     * Default shader angle, only used if [shaderEvaluator] is null
     */
    var shimmerAngle: Int
    /**
     * Default shader width, only used if [shaderEvaluator] is null
     */
    var shimmerWidth: Int
    /**
     * Default shader center width, only used if [shaderEvaluator] is null
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
     * [TimeInterpolator] to use for animation.
     *
     * Defaults to [LinearInterpolator].
     */
    var timeInterpolator: TimeInterpolator = LinearInterpolator()

    /**
     * Optional [Evaluator] providing the [Shader] to use for each animation value.
     *
     * Defaults to a [LinearGradient] defined by [shimmerAngle], [shimmerWidth] and
     * [shimmerCenterWidth] parameters.
     *
     * If a custom shader evaluator is provided, those parameters are ignored.
     */
    var shaderEvaluator: Evaluator<Shader>? = null

    /**
     * Optional [Evaluator] providing the Color to use for current animation value.
     *
     * Default is null, in that case the color remains the same ([shimmerColor]) for the entire
     * animation duration.
     *
     * If an evaluator is set, [shimmerColor] is ignored.
     */
    var colorEvaluator: Evaluator<Int>? = null

    /**
     * Optional [Evaluator] used to get the [Matrix] to use for each animation value. This matrix
     * will be used to transform the shader on each [dispatchDraw] operation.
     *
     * Defaults to a [Evaluator] providing a translation from left to right.
     */
    var matrixEvaluator: Evaluator<Matrix>? = object : Evaluator<Matrix> {
        // Default translation from left to right
        override fun evaluate(fraction: Float) = Matrix().apply {
            // animate from -1 to +1, max dimension will define the shimmer Paint size.
            val translateX = (fraction * 2 - 1) * translateRange
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
            if (shaderEvaluator == null) {
                maskPaint.shader = ShimmerUtil.getShimmerShader(width, shimmerAngle.toDouble(), shimmerWidth, shimmerCenterWidth)
            }
            translateRange = Math.max(width, height)
            shimmerGroup?.addView(this, shimmerDuration, timeInterpolator)
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
        shaderEvaluator?.let { maskPaint.shader = it.evaluate(animatedValue) }
        colorEvaluator?.let { shimmerColor = it.evaluate(animatedValue) }
        matrixEvaluator?.let { maskPaint.shader.setLocalMatrix(it.evaluate(animatedValue)) }
    }

    interface Evaluator<out T> {
        /**
         * Maps a value representing the elapsed fraction of an animation to a value that represents
         * the interpolated [T].
         *
         * @param fraction value between 0 and 1 indicating animation progress
         * @return [T] value for [fraction]
         */
        fun evaluate(fraction: Float): T
    }

    // convenience methods to simplify evaluators declaration from kotlin
    inline fun setMatrixEvaluator(crossinline value: (Float) -> Matrix) {
        matrixEvaluator = object : Evaluator<Matrix> {
            override fun evaluate(fraction: Float) = value.invoke(fraction)
        }
    }

    inline fun setShaderEvaluator(crossinline value: (Float) -> Shader) {
        shaderEvaluator = object : Evaluator<Shader> {
            override fun evaluate(fraction: Float) = value.invoke(fraction)
        }
    }

    inline fun setColorEvaluator(crossinline value: (Float) -> Int) {
        colorEvaluator = object : Evaluator<Int> {
            override fun evaluate(fraction: Float) = value.invoke(fraction)
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
