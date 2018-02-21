package xyz.peridy.shimmerdemo

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.RadialGradient
import android.graphics.Shader
import android.os.Bundle
import android.view.animation.CycleInterpolator
import com.trello.rxlifecycle2.components.RxActivity
import xyz.peridy.shimmerlayout.ShimmerLayout

class ShadersDemoActivity : RxActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_shaders)
    }

    override fun onStart() {
        super.onStart()
        customizeShimmer()
    }

    private fun customizeShimmer() = with(findViewById<ShimmerLayout>(R.id.shimmer_layout)) {
        // Disable default translation animation.
        matrixEvaluator = null

        // Replace shadow shader with a RadialGradient changing radius depending on animation.
        setShaderEvaluator { fraction ->
            val radius = Math.abs(fraction) * width + 1
            RadialGradient(width / 2f, height / 2f, radius, intArrayOf(Color.BLACK, Color.TRANSPARENT), null, Shader.TileMode.CLAMP)
        }

        // Use a CycleInterpolator to have the gradient grow and shrink.
        timeInterpolator = CycleInterpolator(1f)
    }

    companion object {
        fun start(activity: Activity) {
            val intent = Intent(activity, ShadersDemoActivity::class.java)
            activity.startActivity(intent)
        }
    }
}