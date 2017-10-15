package xyz.peridy.shimmerdemo

import android.content.Context
import android.graphics.Bitmap
import com.bumptech.glide.Glide

import xyz.peridy.shimmerlayout.BitmapLoader

class ShimmerDemoGlideBitmapLoader : BitmapLoader {
    override fun release(context: Context, bitmap: Bitmap) {
        DebugInfo.release++
        Glide.get(context).bitmapPool.put(bitmap)
    }

    override fun get(context: Context, width: Int, height: Int, config: Bitmap.Config): Bitmap? {
        val result = if (DebugInfo.simulateLowMemory) null else Glide.get(context).bitmapPool.get(width, height, config)
        if (result == null) DebugInfo.fail++ else DebugInfo.get++
        return result
    }
}