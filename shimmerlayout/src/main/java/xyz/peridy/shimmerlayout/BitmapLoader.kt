package xyz.peridy.shimmerlayout

import android.content.Context
import android.graphics.Bitmap

interface BitmapLoader {
    /**
     * Release bitmap, this should free memory used by bitmap, either using {@link Bitmap#recycle()}
     * or by using other memory management utilities (such as a BitmapPool)
     */
    fun release(context: Context, bitmap: Bitmap)

    /**
     * Creates a bitmap with expected dimensions. This can return null if creation fails for any
     * reason. The returned bitmap must only contain transparent pixels.
     */
    fun get(context: Context, width: Int, height: Int, config: Bitmap.Config): Bitmap?
}
