package xyz.peridy.shimmerlayout

import android.animation.TimeInterpolator
import android.animation.ValueAnimator
import android.os.Handler
import java.lang.ref.WeakReference
import java.util.concurrent.CopyOnWriteArrayList

/**
 * A group of [ShimmerLayout] to synchronize. All layouts using the same group will share the same
 * [ValueAnimator] and [TimeInterpolator]. If those layouts do not use the same animation duration
 * interpolation, these will be set by the first view to animate (don't do that).
 */
class ShimmerGroup {
    // CopyOnWriteArrayList is slower than ArrayList, but allows concurrent access. We're mostly
    // iterating and doing very few add/remove operations, so performance impact is negligible.
    // This mostly prevent concurrent access issues when multiple ShimmerLayout are removed at the
    // same time (removeView being called multiple times, possibly concurrently)
    private val animatedViews = CopyOnWriteArrayList<WeakReference<ShimmerLayout>>()
    private var valueAnimator: ValueAnimator? = null
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
                    if (view.isShown) {
                        view.invalidate()
                    } else {
                        view.stopShimmerAnimation()
                    }
                } ?: remove()
            }
        }
    }
}
