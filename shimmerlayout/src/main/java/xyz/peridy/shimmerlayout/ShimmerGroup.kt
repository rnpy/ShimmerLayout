package xyz.peridy.shimmerlayout

import android.animation.TimeInterpolator
import android.animation.ValueAnimator
import android.os.Handler
import android.view.View
import java.lang.ref.WeakReference
import java.util.*

/**
 * A group of [ShimmerLayout] to synchronize. All layouts using the same group will share the same
 * [ValueAnimator] and [TimeInterpolator]. If those layouts do not use the same animation duration
 * interpolation, these will be set by the first view to animate (don't do that).
 */
class ShimmerGroup {
    private var valueAnimator: ValueAnimator? = null
    private val animatedViews = ArrayList<WeakReference<ShimmerLayout>>()
    internal var animatedValue = 0f

    internal fun addView(shimmerLayout: ShimmerLayout, animationDuration: Long, timeInterpolator: TimeInterpolator) {
        animatedViews.removeAll { it.get() == shimmerLayout || it.get() == null }
        animatedViews.add(WeakReference(shimmerLayout))
        startAnimator(animationDuration, timeInterpolator)
    }

    internal fun removeView(shimmerLayout: ShimmerLayout) {
        animatedViews.removeAll { it.get() == shimmerLayout || it.get() == null }
        if (valueAnimator?.isStarted == true && animatedViews.isEmpty()) {
            Handler().postDelayed({
                // Wait a short time after the last animated view is removed to stop animator, this
                // makes adding another view right after much smoother.
                if (animatedViews.isEmpty()) {
                    valueAnimator?.removeAllUpdateListeners()
                    valueAnimator?.end()
                    valueAnimator = null
                }
            }, 500)
        }
    }

    private fun startAnimator(animationDuration: Long, timeInterpolator: TimeInterpolator) {
        if (valueAnimator != null) {
            return
        }
        valueAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            interpolator = timeInterpolator
            duration = animationDuration
            repeatCount = ValueAnimator.INFINITE
            addUpdateListener { animation -> invalidateViews(animation.animatedValue as Float) }
            start()
        }
    }

    private fun invalidateViews(animatedValue: Float) {
        this.animatedValue = animatedValue

        with(animatedViews.iterator()) {
            forEach {
                it.get()?.let { view ->
                    if (view.visibility == View.VISIBLE) {
                        view.invalidate()
                    } else {
                        view.stopShimmerAnimation()
                    }
                } ?: remove()
            }
        }
    }
}
