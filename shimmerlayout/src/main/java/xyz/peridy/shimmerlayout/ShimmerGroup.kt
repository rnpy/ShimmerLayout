package xyz.peridy.shimmerlayout

import android.animation.ValueAnimator
import android.view.View
import java.lang.ref.WeakReference
import java.util.ArrayList

/**
 * A group of shimmer layouts to synchronize. All layouts using the same group will be synchronized.
 * The layout displayed in all views using the same groups must share the same animation duration.
 */
class ShimmerGroup {
    private var valueAnimator: ValueAnimator? = null
    private val animatedViews = ArrayList<WeakReference<ShimmerLayout>>()
    internal var offsetX = 0f

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

    private fun invalidateViews(maskOffsetX: Float) {
        this.offsetX = maskOffsetX

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
