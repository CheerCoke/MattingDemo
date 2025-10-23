package com.ddmh.wallpaper.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.InputStream
import androidx.core.net.toUri

internal object BitmapUtils {
    fun uriToBitmap(context: Context, uriString: String): Bitmap? {
        return try {
            val uri = uriString.toUri()
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()
            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}