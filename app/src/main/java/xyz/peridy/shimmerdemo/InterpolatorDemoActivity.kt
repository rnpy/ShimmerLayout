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
    private fun customizeShimmer() {
        val shimmerLayout = findViewById<ShimmerLayout>(R.id.shimmer_layout)
        val point = Point()
        windowManager.defaultDisplay.getSize(point)

        shimmerLayout.shimmerDuration = 4500
        shimmerLayout.shimmerWidth = point.x
        shimmerLayout.colorInterpolator = object : ShimmerLayout.ColorInterpolator {
            // Get current colour, rotate between 3 values using ArgbEvaluator
            val evaluator = ArgbEvaluator()
            val colours = arrayOf("#800000", "#008000", "#000080").map { Color.parseColor(it) }
            val count = colours.size
            override fun getColorForOffset(offsetPercent: Float): Int {
                val arrayPosition = (offsetPercent * count).toInt() % count
                val offset = offsetPercent * count % 1.0f
                return evaluator.evaluate(offset, colours[arrayPosition], colours[(arrayPosition + 1) % count]) as Int
            }
        }
        shimmerLayout.matrixInterpolator = object : ShimmerLayout.MatrixInterpolator {
            // Replace default behaviour (translation) with a rotation
            override fun getMatrixForOffset(offsetPercent: Float) = Matrix().apply {
                val angleOffset = if (offsetPercent < 0.5) {
                    offsetPercent * 2
                } else {
                    1f - (offsetPercent - 0.5f) * 2f
                }
                setRotate(-(angleOffset * 2 - 1) * 90)
            }
        }
    }

    /**
     * Animate TextView, shimmer can be applied to any view content, even animated
     */
    private fun animateTextView() {
        val textView = findViewById<TextView>(R.id.text_view)

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

        override fun onAnimationEnd(animation: Animator?) {
            onEnd()
        }

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
