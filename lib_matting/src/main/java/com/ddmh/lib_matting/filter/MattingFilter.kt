package com.ddmh.lib_matting.filter

import android.graphics.Bitmap
import android.opengl.GLES20
import android.opengl.Matrix
import android.widget.ImageView.ScaleType
import com.ddmh.lib_camera.LibCamera
import com.ddmh.lib_camera_effect.effect.filter.ImageTwoInputFilter
import com.ddmh.lib_camera_effect.model.Rotation
import com.ddmh.lib_camera_effect.opengl.GlUtil
import com.ddmh.lib_camera_effect.util.ImageUtil
import com.ddmh.lib_camera_effect.util.OpenGlUtils
import com.ddmh.lib_matting.R
import com.ddmh.lib_matting.utils.TextResourceReader

/**
 * 抠图绘制
 */
class MattingFilter :
    ImageTwoInputFilter(
        "attribute vec4 position;\n" +
                "uniform mat4 uMVPMatrix;\n" +
                "attribute vec4 inputTextureCoordinate;\n" +
                " \n" +
                "varying vec2 textureCoordinate;\n" +
                "varying vec2 textureCoordinate2;\n" +
                " \n" +
                "void main()\n" +
                "{\n" +
                "    gl_Position = position;\n" +
                "    textureCoordinate = inputTextureCoordinate.xy;\n" +
                "    textureCoordinate2 = (uMVPMatrix*vec4(inputTextureCoordinate.x,inputTextureCoordinate.y,0.0, 1.0)).xy;\n" +
                "}",
        TextResourceReader.readTextFileFromResource(
            LibCamera.getApplication(),
            R.raw.matting
        )
    ) {


    fun setKeyColor(colorArray: FloatArray) {
        runOnDraw {
            GLES20.glUniform1f(getHandle(program, "keyColorR"), colorArray[0])
            GLES20.glUniform1f(getHandle(program, "keyColorG"), colorArray[1])
            GLES20.glUniform1f(getHandle(program, "keyColorB"), colorArray[2])
        }

    }

    fun setBgBitmap(bitmap: Bitmap) {
        runOnDraw {
            if (filterSourceTexture2 != OpenGlUtils.NO_TEXTURE) {
                GlUtil.deleteTextureId(filterSourceTexture2)
                filterSourceTexture2 = OpenGlUtils.NO_TEXTURE
            }
            recycleBitmap()
            this.bitmap = bitmap
            if (filterSourceTexture2 == OpenGlUtils.NO_TEXTURE) {
                if (bitmap.isRecycled) {
                    return@runOnDraw
                }
                GLES20.glActiveTexture(GLES20.GL_TEXTURE3)
                filterSourceTexture2 =
                    OpenGlUtils.loadTexture(bitmap, OpenGlUtils.NO_TEXTURE, false)
                GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
            }

            setUniformMatrix4f(
                getHandle(program, "uMVPMatrix"),
                createCenterCropTexMatrix(
                    bitmap.width,
                    bitmap.height,
                    outputWidth,
                    outputHeight
                )
            )
        }

    }

    override fun onDrawArraysPre() {
        super.onDrawArraysPre()
        setRotation(Rotation.ROTATION_90,false,true)
    }

    fun createCenterCropTexMatrix(
        texWidth: Int, texHeight: Int,
        outWidth: Int, outHeight: Int
    ): FloatArray {
        val matrix = FloatArray(16)
        Matrix.setIdentityM(matrix, 0)

        val texRatio = texWidth.toFloat() / texHeight
        val outRatio = outWidth.toFloat() / outHeight

        if (texRatio > outRatio) {
            // 纹理更宽 -> 横向要裁掉
            val scaleX = outRatio / texRatio
            // 保证中心对齐
            Matrix.translateM(matrix, 0, (1f - scaleX) / 2f, 0f, 0f)
            Matrix.scaleM(matrix, 0, scaleX, 1f, 1f)
        } else {
            // 纹理更高 -> 纵向要裁掉
            val scaleY = texRatio / outRatio
            Matrix.translateM(matrix, 0, 0f, (1f - scaleY) / 2f, 0f)
            Matrix.scaleM(matrix, 0, 1f, scaleY, 1f)
        }

        return matrix
    }

    fun setSimilarity(similarity: Float) {
        runOnDraw {
            setFloat(getHandle(program, "similarity"), similarity)
        }
    }
}