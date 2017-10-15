package xyz.peridy.shimmerdemo

import android.os.Handler
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast

class DebugInfo(view: View) {
    private val text = view.findViewById<TextView>(R.id.bitmap_count_text_view)
    private val button = view.findViewById<Button>(R.id.memory_button)

    private val handler = Handler()
    private val runnable = object : Runnable {
        override fun run() {
            val getString = formatString("get", get)
            val putString = formatString("put", release)
            val failString = formatString("fail", fail)
            text.text = "$getString\n$putString\n$failString"
            handler.postDelayed(this, 100)
        }
    }

    private fun formatString(label: String, value: Int): String = "bitmap.$label : $value"

    init {
        handler.post(runnable)
        button.setOnClickListener {
            simulateLowMemory = !simulateLowMemory
            button.setText(if (simulateLowMemory) R.string.low_memory else R.string.high_memory)
            if (simulateLowMemory) {
                Toast.makeText(it.context, R.string.simulating_low_memory, Toast.LENGTH_SHORT).show()
            }
        }
    }

    companion object {
        var release = 0
        var get = 0
        var fail = 0
        var simulateLowMemory = false
    }
}
