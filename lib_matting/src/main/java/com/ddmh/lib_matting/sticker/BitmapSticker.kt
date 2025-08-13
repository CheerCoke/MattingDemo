package com.ddmh.lib_matting.sticker

import android.graphics.Bitmap
import android.graphics.Canvas

class BitmapSticker(val bitmap: Bitmap) : Sticker() {
    override fun draw(canvas: Canvas) {
        canvas.save()
        canvas.concat(matrix)
        canvas.drawBitmap(bitmap, 0f, 0f, null)
        canvas.restore()
    }

    override fun getWidth(): Int = bitmap.width

    override fun getHeight(): Int = bitmap.height

    override fun setAlpha(alpha: Int): Sticker {
        return this
    }
}