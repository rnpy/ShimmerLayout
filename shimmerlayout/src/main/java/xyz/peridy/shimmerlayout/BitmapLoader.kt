package xyz.peridy.shimmerlayout

import android.content.Context
import android.graphics.Bitmap

interface BitmapLoader {
    fun release(context: Context, bitmap: Bitmap)

    fun get(context: Context, width: Int, height: Int, config: Bitmap.Config): Bitmap?
}
