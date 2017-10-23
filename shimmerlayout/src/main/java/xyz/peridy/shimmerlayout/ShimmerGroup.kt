package xyz.peridy.shimmerlayout

import android.animation.ValueAnimator
import android.view.View
import java.lang.ref.WeakReference
import java.util.ArrayList

/**
 * A group of shimmer layouts to synchronize. All layouts using the same group will share the same
 * {@link ValueAnimator}. If those layouts do not use the same animation duration, the duration will
 * be set by the first view to animate (don't do that).
 */
class ShimmerGroup {
    private var valueAnimator: ValueAnimator? = null
    private val animatedViews = ArrayList<WeakReference<ShimmerLayout>>()
    internal var offsetPercent = 0f

    internal fun addView(shimmerLayout: ShimmerLayout, animationDuration:Long) {
        animatedViews.removeAll { it.get() == shimmerLayout || it.get() == null }
        animatedViews.add(WeakReference(shimmerLayout))
        startAnimator(animationDuration)
    }

    internal fun removeView(shimmerLayout: ShimmerLayout) {
        animatedViews.removeAll { it.get() == shimmerLayout || it.get() == null }
        if (valueAnimator?.isStarted == true && animatedViews.isEmpty()) {
            valueAnimator?.removeAllUpdateListeners()
            valueAnimator?.end()
            valueAnimator = null
        }
    }

    private fun startAnimator(animationDuration:Long) {
        if (valueAnimator != null) {
            return
        }
        valueAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = animationDuration
            repeatCount = ValueAnimator.INFINITE
            addUpdateListener { animation -> invalidateViews(animation.animatedValue as Float) }
            start()
        }
    }

    private fun invalidateViews(offsetXPercent: Float) {
        this.offsetPercent = offsetXPercent

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
