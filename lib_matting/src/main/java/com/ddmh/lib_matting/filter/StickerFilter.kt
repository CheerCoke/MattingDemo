package com.ddmh.lib_matting.filter

import android.opengl.GLES20
import android.opengl.Matrix
import androidx.camera.core.impl.utils.futures.Futures.transform
import com.ddmh.lib_camera_effect.effect.filter.BaseFilter
import com.ddmh.lib_camera_effect.opengl.GlUtil
import com.ddmh.lib_camera_effect.util.OpenGlUtils
import com.ddmh.lib_matting.sticker.Sticker
import com.ddmh.lib_matting.sticker.StickerView
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

/**
 * 贴纸绘制
 */
class StickerFilter() : BaseFilter() {
    private var stickerProgram = 0

    // 贴纸模型
    var mStickerView: StickerView? = null

    // 组件绘制器列表
    var mComponentRenders: MutableList<ComponentFilter> =
        ArrayList()

    var mTextureHandle = 0
    var mPositionHandle = 0
    var mTextureCoordHandle = 0
    var mMVPMatrixHandle = 0
    var orthographicMatrixUniform = 0
    var mTextureVertices: FloatBuffer? = null
    var orthographicMatrix: FloatArray = FloatArray(16)
    var transform3D = FloatArray(16)
    override fun onInit() {
        orthographicMatrix = FloatArray(16)
        Matrix.orthoM(orthographicMatrix, 0, -1.0f, 1.0f, -1.0f, 1.0f, -1.0f, 1.0f)
        transform3D = FloatArray(16)
        Matrix.setIdentityM(transform3D, 0)
        // 顺时针旋转180°的纹理坐标
        val texData2 = floatArrayOf(
            1.0f, 1.0f, 0.0f, 1.0f, 1.0f, 0.0f,
            0.0f, 0.0f,
        )
        mTextureVertices = ByteBuffer.allocateDirect(texData2.size * 4)
            .order(ByteOrder.nativeOrder()).asFloatBuffer()
        mTextureVertices?.put(texData2)?.position(0)
        stickerProgram = OpenGlUtils.loadProgram(
            "precision highp float;\n" +
                    "attribute vec4 position;\n" +
                    " attribute vec4 inputTextureCoordinate;\n" +
                    " \n" +
                    " uniform mat4 transformMatrix;\n" +
                    " uniform mat4 orthographicMatrix;\n" +
                    " \n" +
                    " varying vec2 textureCoordinate;\n" +
                    " \n" +
                    " void main()\n" +
                    " {\n" +
                    "    gl_Position = transformMatrix * vec4(position.xy, 0.0, 1.0)*orthographicMatrix;\n" +
//                    "    gl_Position =  vec4(position.xy, 0.0, 1.0);\n" +
                    "     textureCoordinate = inputTextureCoordinate.xy;\n" +
                    " }", NO_FILTER_FRAGMENT_SHADER
        )
        super.onInit()
        for (componentRender in mComponentRenders) {
            componentRender.onInit()
        }
    }

    override fun onInitialized() {
        super.onInitialized()
        mPositionHandle = GLES20.glGetAttribLocation(stickerProgram, "position")
        mTextureHandle = GLES20.glGetUniformLocation(stickerProgram, "inputImageTexture")
        mTextureCoordHandle = GLES20.glGetAttribLocation(stickerProgram, "inputTextureCoordinate")
        mMVPMatrixHandle = GLES20.glGetUniformLocation(stickerProgram, "transformMatrix")
        orthographicMatrixUniform = GLES20.glGetUniformLocation(stickerProgram, "orthographicMatrix")


        setUniformMatrix4f(mMVPMatrixHandle, transform3D)
        setUniformMatrix4f(orthographicMatrixUniform, orthographicMatrix)

    }

    override fun onDrawArraysPre() {
        super.onDrawArraysPre()

        for (componentRender in mComponentRenders) {
            componentRender.onDrawPre()
        }

    }

    override fun onDrawArraysAfter(cubeBuffer: FloatBuffer, textureBuffer: FloatBuffer) {
        super.onDrawArraysAfter(cubeBuffer, textureBuffer)
        textureBuffer.position(0)
        // 开启混合
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_ONE, GLES20.GL_ONE_MINUS_SRC_ALPHA)
        GLES20.glUseProgram(stickerProgram)


        for (componentRender in mComponentRenders) {
//            Matrix.setIdentityM(transform3D, 0)
//            Matrix.setRotateM(transform3D, 0, componentRender.sticker.currentAngle, 0f, 0f, 1.0f)
//            setUniformMatrix4f(
//                mMVPMatrixHandle,
//                transform3D
//            )

            componentRender.onDraw(
                mTextureHandle,
                mPositionHandle,
                mTextureCoordHandle,
                mTextureVertices ?: textureBuffer
            )
        }
        GLES20.glDisable(GLES20.GL_BLEND)
    }

    override fun onOutputSizeChanged(width: Int, height: Int) {
        super.onOutputSizeChanged(width, height)
        Matrix.orthoM(orthographicMatrix, 0, -1.0f, 1.0f, -1.0f * height /  width, 1.0f *  height / width, -1.0f, 1.0f);
        setUniformMatrix4f(orthographicMatrixUniform, orthographicMatrix)

    }


    override fun onDestroy() {
        super.onDestroy()
        mComponentRenders.forEach { it.onDestroy() }
    }


    /**
     * 设置贴纸模型
     *
     */
    fun bindStickerView(stickerView: StickerView) {
        mStickerView = stickerView
        runOnDraw {
            mComponentRenders.clear()
            mStickerView?.allStickers?.forEach {
                mComponentRenders.add(
                    ComponentFilter(
                        it,
                        stickerView.width,
                        stickerView.height,
                        outputWidth,
                        outputHeight
                    )
                )
            }
        }
    }

    /**
     * 添加贴纸
     */
    fun addSticker(sticker: Sticker, width: Int, height: Int) {
        runOnDraw {
            mComponentRenders.add(
                ComponentFilter(
                    sticker,
                    width,
                    height,
                    outputWidth,
                    outputHeight
                )
            )
        }
    }

    fun removeSticker(sticker: Sticker) {
        runOnDraw {
            mComponentRenders.removeAll { it.sticker.serialVersionUID == sticker.serialVersionUID }
        }
    }

    fun touchSticker(sticker: Sticker) {
        runOnDraw {
            mComponentRenders.firstOrNull() { it.sticker.serialVersionUID == sticker.serialVersionUID }
                ?.let { target ->
                    mComponentRenders.remove(target)
                    mComponentRenders.add(target)
                }
        }
    }

}