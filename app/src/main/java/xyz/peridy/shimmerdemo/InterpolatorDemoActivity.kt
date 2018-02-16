package xyz.peridy.shimmerdemo

import android.animation.Animator
import android.animation.ArgbEvaluator
import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Point
import android.os.Bundle
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.TextView
import com.trello.rxlifecycle2.components.RxActivity

import xyz.peridy.shimmerlayout.ShimmerLayout

class InterpolatorDemoActivity : RxActivity() {

    private var currentTextIndex = 0
    private val textView: TextView by lazy { findViewById<TextView>(R.id.text_view) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_interpolators)
    }

    override fun onStart() {
        super.onStart()
        customizeShimmer()
        animateTextView()
    }

    /**
     * Customize shimmer effect using interpolators, change colour and default translation behaviour
     */
    private fun customizeShimmer() = with(findViewById<ShimmerLayout>(R.id.shimmer_layout)) {
        val point = Point()
        windowManager.defaultDisplay.getSize(point)

        shimmerDuration = 4500
        shimmerWidth = point.x
        shimmerAngle = 90

        // interpolator can be declared using a custom class
        colorInterpolator = object : ShimmerLayout.Interpolator<Int> {
            // Get current colour, rotate between 3 values using ArgbEvaluator
            val evaluator = ArgbEvaluator()
            val colours = arrayOf("#800000", "#008000", "#000080").map { Color.parseColor(it) }
            val count = colours.size

            override fun getInterpolation(input: Float): Int {
                val arrayPosition = (input * count).toInt() % count
                val offset = input * count % 1.0f
                return evaluator.evaluate(offset, colours[arrayPosition], colours[(arrayPosition + 1) % count]) as Int
            }
        }

        // or using kotlin convenience method
        setMatrixInterpolator { input ->
            Matrix().apply {
                setRotate(input * 360)
            }
        }
    }

    /**
     * Animate TextView, shimmer can be applied to any view content, even animated
     */
    private fun animateTextView() {
        // Animate TextView out (+ fade out)
        textView.animate()
                .alpha(0f)
                .translationY(-120f)
                .setStartDelay(2500)
                .setInterpolator(AccelerateInterpolator())
                .setListener(AnimationEndListener {
                    // Change text, animate back in
                    textView.text = getString(when (++currentTextIndex % 3) {
                        0 -> R.string.please_wait_1
                        1 -> R.string.please_wait_2
                        else -> R.string.please_wait_3
                    })
                    textView.animate()
                            .alpha(1f)
                            .translationY(0f)
                            .setStartDelay(350)
                            .setInterpolator(DecelerateInterpolator())
                            .setListener(AnimationEndListener {
                                // Repeat
                                animateTextView()
                            }).start()
                }).start()
    }

    private class AnimationEndListener(val onEnd: () -> Unit) : Animator.AnimatorListener {
        override fun onAnimationRepeat(animation: Animator?) {
        }

        override fun onAnimationEnd(animation: Animator?) = onEnd()

        override fun onAnimationCancel(animation: Animator?) {
        }

        override fun onAnimationStart(animation: Animator?) {
        }
    }

    companion object {
        fun start(activity: Activity) {
            val intent = Intent(activity, InterpolatorDemoActivity::class.java)
            activity.startActivity(intent)
        }
    }
}
