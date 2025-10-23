package com.ddmh.wallpaper.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.opengl.GLException
import android.opengl.GLUtils
import android.util.Log
import android.util.Size
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.IntBuffer
import javax.microedition.khronos.opengles.GL10

internal object OpenGlUtils {
    private const val TAG = "OpenGLUtils"
    const val NO_TEXTURE: Int = -1
    const val FLOAT_SIZE_BYTES: Int = 4
    val VERTICES_DATA: FloatArray = floatArrayOf( // X, Y, Z, U, V
        -1.0f, 1.0f, 0.0f, 0.0f, 1.0f,
        1.0f, 1.0f, 0.0f, 1.0f, 1.0f,
        -1.0f, -1.0f, 0.0f, 0.0f, 0.0f,
        1.0f, -1.0f, 0.0f, 1.0f, 0.0f
    )
    val CUBE = floatArrayOf(
        -1.0f, -1.0f,
        1.0f, -1.0f,
        -1.0f, 1.0f,
        1.0f, 1.0f
    )
    fun loadTextureFromRes(context: Context, resId: Int): Int {
        //创建纹理对象
        val textureId = IntArray(1)
        //生成纹理：纹理数量、保存纹理的数组，数组偏移量
        GLES20.glGenTextures(1, textureId, 0)
        if (textureId[0] == 0) {
            Log.e(TAG, "创建纹理对象失败")
        }
        //原尺寸加载位图资源（禁止缩放）
        val options = BitmapFactory.Options()
        options.inScaled = false
        val bitmap = BitmapFactory.decodeResource(context.resources, resId, options)
        if (bitmap == null) {
            //删除纹理对象
            GLES20.glDeleteTextures(1, textureId, 0)
            Log.e(TAG, "加载位图失败")
        }
        //绑定纹理到opengl
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId[0])
        //设置放大、缩小时的纹理过滤方式，必须设定，否则纹理全黑
        GLES20.glTexParameterf(
            GLES20.GL_TEXTURE_2D,
            GLES20.GL_TEXTURE_MIN_FILTER,
            GLES20.GL_NEAREST.toFloat()
        )
        GLES20.glTexParameterf(
            GLES20.GL_TEXTURE_2D,
            GLES20.GL_TEXTURE_MAG_FILTER,
            GLES20.GL_NEAREST.toFloat()
        )
        //将位图加载到opengl中，并复制到当前绑定的纹理对象上
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)
        //释放bitmap资源（上面已经把bitmap的数据复制到纹理上了）
        checkNotNull(bitmap)
        bitmap.recycle()
        //解绑当前纹理，防止其他地方以外改变该纹理
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
        //返回纹理对象
        return textureId[0]
    }

    /**
     * 根据bitmap创建2D纹理
     * @param bitmap
     * @param minFilter     缩小过滤类型 (1.GL_NEAREST ; 2.GL_LINEAR)
     * @param magFilter     放大过滤类型
     * @param wrapS         纹理S方向边缘环绕;也称作X方向
     * @param wrapT         纹理T方向边缘环绕;也称作Y方向
     * @return 返回创建的 Texture ID
     */
    fun createTexture(
        bitmap: Bitmap?,
        minFilter: Int,
        magFilter: Int,
        wrapS: Int,
        wrapT: Int
    ): Int {
        val textureHandle =
            createTextures(GLES20.GL_TEXTURE_2D, 1, minFilter, magFilter, wrapS, wrapT)
        if (bitmap != null) {
            // 4.把bitmap加载到纹理中
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)
        }
        return textureHandle[0]
    }

    /**
     * 创建纹理
     * @param textureTarget Texture类型。
     * 1. 相机用 GLES11Ext.GL_TEXTURE_EXTERNAL_OES
     * 2. 图片用 GLES20.GL_TEXTURE_2D
     * @param count         创建纹理数量
     * @param minFilter     缩小过滤类型 (1.GL_NEAREST ; 2.GL_LINEAR)
     * @param magFilter     放大过滤类型
     * @param wrapS         纹理S方向边缘环绕;也称作X方向
     * @param wrapT         纹理T方向边缘环绕;也称作Y方向
     * @return 返回创建的 Texture ID
     */
    fun createTextures(
        textureTarget: Int, count: Int, minFilter: Int, magFilter: Int, wrapS: Int,
        wrapT: Int
    ): IntArray {
        val textureHandles = IntArray(count)
        for (i in 0 until count) {
            // 1.生成纹理
            GLES20.glGenTextures(1, textureHandles, i)
            // 2.绑定纹理
            GLES20.glBindTexture(textureTarget, textureHandles[i])
            // 3.设置纹理属性
            // 设置纹理的缩小过滤类型（1.GL_NEAREST ; 2.GL_LINEAR）
            GLES20.glTexParameterf(textureTarget, GLES20.GL_TEXTURE_MIN_FILTER, minFilter.toFloat())
            // 设置纹理的放大过滤类型（1.GL_NEAREST ; 2.GL_LINEAR）
            GLES20.glTexParameterf(textureTarget, GLES20.GL_TEXTURE_MAG_FILTER, magFilter.toFloat())
            // 设置纹理的X方向边缘环绕
            GLES20.glTexParameteri(textureTarget, GLES20.GL_TEXTURE_WRAP_S, wrapS)
            // 设置纹理的Y方向边缘环绕
            GLES20.glTexParameteri(textureTarget, GLES20.GL_TEXTURE_WRAP_T, wrapT)
        }
        return textureHandles
    }

    fun createTexture(width: Int, height: Int, config: Int): Int {
        val texture = IntArray(1)
        GLES20.glGenTextures(1, texture, 0)
        // Bind the texture handle to the 2D texture target.
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture[0])
        // Configure min/mag filtering, i.e. what scaling method do we use if what we're rendering
        // is smaller or larger than the source image.
        GLES20.glTexImage2D(
            GLES20.GL_TEXTURE_2D,
            0,
            config,
            width,
            height,
            0,
            GLES20.GL_RGBA,
            GLES20.GL_UNSIGNED_BYTE,
            null
        )

        GLES20.glTexParameterf(
            GLES20.GL_TEXTURE_2D,
            GL10.GL_TEXTURE_MIN_FILTER,
            GL10.GL_LINEAR.toFloat()
        )
        GLES20.glTexParameterf(
            GLES20.GL_TEXTURE_2D,
            GL10.GL_TEXTURE_MAG_FILTER,
            GL10.GL_LINEAR.toFloat()
        )
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_S, GL10.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_T, GL10.GL_CLAMP_TO_EDGE)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)

        return texture[0]
    }

    fun createOESTexture(textureTarget: Int): Int {
        var textureTarget = textureTarget
        if (textureTarget != 0) {
            val tex = IntArray(1)
            tex[0] = textureTarget
            GLES20.glDeleteTextures(1, tex, 0)
        }
        val textures = IntArray(1)
        GLES20.glGenTextures(1, textures, 0)
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textures[0])
        GLES20.glTexParameterf(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_LINEAR.toFloat()
        )
        GLES20.glTexParameterf(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR.toFloat()
        )
        GLES20.glTexParameteri(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GL10.GL_TEXTURE_WRAP_S, GL10.GL_CLAMP_TO_EDGE
        )
        GLES20.glTexParameteri(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GL10.GL_TEXTURE_WRAP_T, GL10.GL_CLAMP_TO_EDGE
        )
        textureTarget = textures[0]

        return textureTarget
    }

    /**
     * Checks to see if a GLES error has been raised.
     */
    fun checkGlError(op: String) {
        val error = GLES20.glGetError()
        if (error != GLES20.GL_NO_ERROR) {
            val msg = op + ": glError 0x" + Integer.toHexString(error)
            Log.e("render", msg)
        }
    }


    fun loadTexture(img: Bitmap?, usedTexId: Int): Int {
        return loadTexture(img, usedTexId, true)
    }



    @JvmStatic
    fun loadTexture(img: Bitmap?, usedTexId: Int, recycle: Boolean): Int {
        val textures = IntArray(1)
        if (usedTexId == NO_TEXTURE) {
            GLES20.glGenTextures(1, textures, 0)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[0])
            GLES20.glTexParameterf(
                GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR.toFloat()
            )
            GLES20.glTexParameterf(
                GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR.toFloat()
            )
            GLES20.glTexParameterf(
                GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE.toFloat()
            )
            GLES20.glTexParameterf(
                GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE.toFloat()
            )

            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, img, 0)
        } else {
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, usedTexId)
            GLUtils.texSubImage2D(GLES20.GL_TEXTURE_2D, 0, 0, 0, img)
            textures[0] = usedTexId
        }
        if (recycle) {
            img?.recycle()
        }
        return textures[0]
    }

    @JvmStatic
    fun loadTexture(data: IntBuffer?, width: Int, height: Int, usedTexId: Int): Int {
        val textures = IntArray(1)
        if (usedTexId == NO_TEXTURE) {
            GLES20.glGenTextures(1, textures, 0)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[0])
            GLES20.glTexParameterf(
                GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR.toFloat()
            )
            GLES20.glTexParameterf(
                GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR.toFloat()
            )
            GLES20.glTexParameterf(
                GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE.toFloat()
            )
            GLES20.glTexParameterf(
                GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE.toFloat()
            )
            GLES20.glTexImage2D(
                GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, width, height,
                0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, data
            )
        } else {
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, usedTexId)
            GLES20.glTexSubImage2D(
                GLES20.GL_TEXTURE_2D, 0, 0, 0, width,
                height, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, data
            )
            textures[0] = usedTexId
        }
        return textures[0]
    }

    fun loadTextureAsBitmap(data: IntBuffer, size: Size, usedTexId: Int): Int {
        val bitmap = Bitmap
            .createBitmap(data.array(), size.width, size.height, Bitmap.Config.ARGB_8888)
        return loadTexture(bitmap, usedTexId)
    }

    @JvmStatic
    fun loadShader(strSource: String?, iType: Int): Int {
        val compiled = IntArray(1)
        val iShader = GLES20.glCreateShader(iType)
        GLES20.glShaderSource(iShader, strSource)
        GLES20.glCompileShader(iShader)
        GLES20.glGetShaderiv(iShader, GLES20.GL_COMPILE_STATUS, compiled, 0)
        if (compiled[0] == 0) {
            Log.d(
                "Load Shader Failed", """
     Compilation
     ${GLES20.glGetShaderInfoLog(iShader)}
     """.trimIndent()
            )
            return 0
        }
        return iShader
    }

    @Throws(GLException::class)
    @JvmStatic
    fun createProgram(vertexShader: Int, pixelShader: Int): Int {
        val program = GLES20.glCreateProgram()
        if (program == 0) {
            throw RuntimeException("Could not create program")
        }

        GLES20.glAttachShader(program, vertexShader)
        GLES20.glAttachShader(program, pixelShader)

        GLES20.glLinkProgram(program)
        val linkStatus = IntArray(1)
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0)
        if (linkStatus[0] != GLES20.GL_TRUE) {
            GLES20.glDeleteProgram(program)
            throw RuntimeException("Could not link program")
        }
        return program
    }


    @JvmStatic
    fun loadProgram(strVSource: String?, strFSource: String?): Int {
        val iVShader: Int
        val iFShader: Int
        val link = IntArray(1)
        iVShader = loadShader(strVSource, GLES20.GL_VERTEX_SHADER)
        if (iVShader == 0) {
            Log.d("Load Program", "Vertex Shader Failed")
            return 0
        }
        iFShader = loadShader(strFSource, GLES20.GL_FRAGMENT_SHADER)
        if (iFShader == 0) {
            Log.d("Load Program", "Fragment Shader Failed")
            return 0
        }

        val iProgId = GLES20.glCreateProgram()

        GLES20.glAttachShader(iProgId, iVShader)
        GLES20.glAttachShader(iProgId, iFShader)

        GLES20.glLinkProgram(iProgId)

        GLES20.glGetProgramiv(iProgId, GLES20.GL_LINK_STATUS, link, 0)
        if (link[0] <= 0) {
            Log.d("Load Program", "Linking Failed")
            return 0
        }
        GLES20.glDeleteShader(iVShader)
        GLES20.glDeleteShader(iFShader)
        return iProgId
    }

    fun rnd(min: Float, max: Float): Float {
        val fRandNum = Math.random().toFloat()
        return min + (max - min) * fRandNum
    }

    @JvmStatic
    fun setupSampler(target: Int, mag: Int, min: Int) {
        GLES20.glTexParameterf(target, GLES20.GL_TEXTURE_MAG_FILTER, mag.toFloat())
        GLES20.glTexParameterf(target, GLES20.GL_TEXTURE_MIN_FILTER, min.toFloat())
        GLES20.glTexParameteri(target, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(target, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
    }

    @JvmStatic
    fun createBuffer(data: FloatArray): Int {
        return createBuffer(toFloatBuffer(data))
    }

    @JvmStatic
    fun createBuffer(data: FloatBuffer): Int {
        val buffers = IntArray(1)
        GLES20.glGenBuffers(buffers.size, buffers, 0)
        updateBufferData(buffers[0], data)
        return buffers[0]
    }

    @JvmStatic
    fun toFloatBuffer(data: FloatArray): FloatBuffer {
        val buffer = ByteBuffer
            .allocateDirect(data.size * FLOAT_SIZE_BYTES)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
        buffer.put(data).position(0)
        return buffer
    }

    @JvmStatic
    fun updateBufferData(bufferName: Int, data: FloatBuffer) {
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, bufferName)
        GLES20.glBufferData(
            GLES20.GL_ARRAY_BUFFER,
            data.capacity() * FLOAT_SIZE_BYTES,
            data,
            GLES20.GL_STATIC_DRAW
        )
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)
    }

    @JvmStatic
    fun readPixelsBitmap(outputWidth:Int , outputHeight:Int):Bitmap {
        val byteBuffer = ByteBuffer.allocateDirect(outputWidth * outputHeight * 4)
        byteBuffer.order(ByteOrder.nativeOrder())
        GLES20.glReadPixels(
            0,
            0,
            outputWidth,
            outputHeight,
            GLES20.GL_RGBA,
            GLES20.GL_UNSIGNED_BYTE,
            byteBuffer
        )
        return Bitmap.createBitmap(outputWidth, outputHeight, Bitmap.Config.ARGB_4444)
    }



    fun createFrameBufferTexture(width: Int, height: Int): Int {
        val texture = IntArray(1)
        GLES20.glGenTextures(1, texture, 0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture[0])

        GLES20.glTexParameterf(
            GLES20.GL_TEXTURE_2D,
            GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR.toFloat()
        )
        GLES20.glTexParameterf(
            GLES20.GL_TEXTURE_2D,
            GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR.toFloat()
        )
        GLES20.glTexParameterf(
            GLES20.GL_TEXTURE_2D,
            GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE.toFloat()
        )
        GLES20.glTexParameterf(
            GLES20.GL_TEXTURE_2D,
            GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE.toFloat()
        )
        GLES20.glTexImage2D(
            GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, width, height, 0,
            GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null
        )
        return texture[0]
    }

    fun createLuminanceTexture(width: Int, height: Int, textureId: IntArray, type: Int): Int {
        GLES20.glGenTextures(1, textureId, 0)
        GLES20.glBindTexture(type, textureId[0])
        GLES20.glTexParameterf(
            type,
            GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR.toFloat()
        )
        GLES20.glTexParameterf(
            type,
            GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR.toFloat()
        )
        GLES20.glTexParameterf(
            type,
            GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE.toFloat()
        )
        GLES20.glTexParameterf(
            type,
            GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE.toFloat()
        )
        GLES20.glTexImage2D(
            type, 0, GLES20.GL_LUMINANCE, width, height, 0,
            GLES20.GL_LUMINANCE, GLES20.GL_UNSIGNED_BYTE, null
        )
        return textureId[0]
    }
}
