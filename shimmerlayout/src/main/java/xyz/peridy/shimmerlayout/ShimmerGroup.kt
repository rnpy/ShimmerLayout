package xyz.peridy.shimmerlayout

import android.animation.ValueAnimator
import android.os.Handler
import android.view.View
import java.lang.ref.WeakReference
import java.util.ArrayList

/**
 * A group of [ShimmerLayout] to synchronize. All layouts using the same group will share the same
 * [ValueAnimator]. If those layouts do not use the same animation duration, the duration will
 * be set by the first view to animate (don't do that).
 */
class ShimmerGroup {
    private var valueAnimator: ValueAnimator? = null
    private val animatedViews = ArrayList<WeakReference<ShimmerLayout>>()
    internal var animatedValue = 0f

    internal fun addView(shimmerLayout: ShimmerLayout, animationDuration: Long) {
        animatedViews.removeAll { it.get() == shimmerLayout || it.get() == null }
        animatedViews.add(WeakReference(shimmerLayout))
        startAnimator(animationDuration)
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

    private fun startAnimator(animationDuration: Long) {
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
