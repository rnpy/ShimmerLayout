package xyz.peridy.shimmerlayout

import android.content.Context
import android.graphics.Bitmap

/**
 * Default bitmap loader, this uses default Android Bitmap management methods.
 */
class DefaultBitmapLoader : BitmapLoader {
    override fun release(context: Context, bitmap: Bitmap) =
        bitmap.recycle()

    override fun get(context: Context, width: Int, height: Int, config: Bitmap.Config): Bitmap? =
            Bitmap.createBitmap(width, height, config)
}
