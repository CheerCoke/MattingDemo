package com.ddmh.lib_matting.filter

import android.graphics.PointF
import android.graphics.RectF
import android.opengl.GLES20
import android.util.Log
import com.ddmh.lib_camera.preview.GLRenderer.Companion.TAG
import com.ddmh.lib_camera.util.GlUtil
import com.ddmh.lib_camera_effect.util.OpenGlUtils
import com.ddmh.lib_matting.sticker.BitmapSticker
import com.ddmh.lib_matting.sticker.Sticker
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

class ComponentFilter(
    val sticker: Sticker,
    val stickerLayoutWidth: Int,
    val stickerLayoutHeight: Int,
    val outputWidth: Int,
    val outputHeight: Int
) {

    private var mRenderVertices: FloatBuffer? = null

    private var mTexture = OpenGlUtils.NO_TEXTURE


    fun onInit() {
        // 4个顶点，每个顶点由x，y两个float变量组成，每个float占4字节，总共32字节
        mRenderVertices = ByteBuffer.allocateDirect(32)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
    }

    fun onDrawPre() {
        if (mTexture == OpenGlUtils.NO_TEXTURE) {
            if (sticker is BitmapSticker) {
                val bitmap = sticker.bitmap
                if (bitmap == null || bitmap.isRecycled) {
                    return
                }
                GLES20.glActiveTexture(GLES20.GL_TEXTURE4)
                mTexture =
                    OpenGlUtils.loadTexture(bitmap, OpenGlUtils.NO_TEXTURE, false)
                GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
            }

        }

    }

    fun onDestroy() {
        if (mTexture != OpenGlUtils.NO_TEXTURE) {
            GlUtil.deleteTextureId(mTexture)
            mTexture = 0
        }
    }


    /**
     * 绘制组件
     *
     *
     * 在GL线程调用
     *
     * @param textureHandle      纹理指针
     * @param positionHandle     渲染顶点坐标指针
     * @param textureCoordHandle 纹理顶点坐标指针
     * @param textureVertices    纹理顶点坐标
     */
    fun onDraw(
        textureHandle: Int,
        positionHandle: Int,
        textureCoordHandle: Int,
        textureVertices: FloatBuffer
    ) {
        updateRenderVertices()
        mRenderVertices!!.position(0)
        textureVertices.position(0)


        GLES20.glActiveTexture(GLES20.GL_TEXTURE4)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTexture)
        GLES20.glUniform1i(textureHandle, 4)

        GLES20.glVertexAttribPointer(positionHandle, 2, GLES20.GL_FLOAT, false, 0, mRenderVertices)
        GLES20.glEnableVertexAttribArray(positionHandle)
        GLES20.glVertexAttribPointer(
            textureCoordHandle,
            2,
            GLES20.GL_FLOAT,
            false,
            0,
            textureVertices
        )

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)

    }


    /**
     * 更新渲染顶点坐标
     * 在GL线程调用
     */
    fun updateRenderVertices() {
        if (mRenderVertices == null) {
            onInit()
        }
        val correctedBounds = sticker.mappedBound
        val angle = sticker.currentAngle
        Log.i(TAG, " correctedBounds : ${correctedBounds.width()}  ${correctedBounds.height()}")
        val glLeftTop =
            transVerticesToOpenGL(
                correctAspect(
                    PointF(correctedBounds.left, correctedBounds.top),
                    stickerLayoutWidth,
                    stickerLayoutHeight,
                    outputWidth,
                    outputHeight
                ),
                stickerLayoutWidth,
                stickerLayoutHeight
            )
        val glRightBottom = transVerticesToOpenGL(
            correctAspect(
                PointF(correctedBounds.right, correctedBounds.bottom),
                stickerLayoutWidth,
                stickerLayoutHeight,
                outputWidth,
                outputHeight
            ),
            stickerLayoutWidth,
            stickerLayoutHeight
        )

        val bounds = RectF(glLeftTop.x, glLeftTop.y, glRightBottom.x, glRightBottom.y)
        // 确定贴纸四个顶点坐标（View 坐标）
        val leftTop = PointF(bounds.left, bounds.top)
        val rightTop = PointF(bounds.right, bounds.top)
        val leftBottom = PointF(bounds.left, bounds.bottom)
        val rightBottom = PointF(bounds.right, bounds.bottom)



        Log.i(TAG, "OpenGL坐标系位置: $leftTop,  $rightTop, $leftBottom,$rightBottom")

        val vertices = FloatArray(8)
        vertices[0] = rightBottom.x
        vertices[1] = rightBottom.y
        vertices[2] = leftBottom.x
        vertices[3] = leftBottom.y
        vertices[4] = rightTop.x
        vertices[5] = rightTop.y
        vertices[6] = leftTop.x
        vertices[7] = leftTop.y

//        val normalizedHeight = bounds.width()/bounds.height()
//        vertices[1] *= normalizedHeight;
//        vertices[3] *= normalizedHeight;
//        vertices[5] *= normalizedHeight;
//        vertices[7] *= normalizedHeight;
        mRenderVertices = ByteBuffer.allocateDirect(vertices.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer();
//        mRenderVertices?.clear()
        mRenderVertices?.put(vertices)?.position(0)
    }


    fun transVerticesToOpenGL(point: PointF, viewWidth: Int, viewHeight: Int): PointF {
        val glX = (point.x / viewWidth) * 2f - 1f
        val glY = 1f - (point.y / viewHeight) * 2f
        return PointF(glX, glY)
    }

    fun correctAspect(
        point: PointF,
        inputWidth: Int,
        inputHeight: Int,
        outputWidth: Int,
        outputHeight: Int
    ): PointF {
        val inputRatio = inputWidth.toFloat() / inputHeight
        val outputRatio = outputWidth.toFloat() / outputHeight
        return if (inputRatio > outputRatio) {
            // 横向挤压
            PointF(point.x * outputRatio / inputRatio, point.y)
        } else {
            // 纵向挤压
            PointF(point.x, point.y * inputRatio / outputRatio)
        }
    }


}