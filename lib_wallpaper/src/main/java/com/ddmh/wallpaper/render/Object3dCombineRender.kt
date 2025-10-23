package com.ddmh.wallpaper.render


import android.R.attr.x
import android.R.attr.y
import android.content.Context
import android.graphics.Bitmap
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.os.SystemClock
import android.util.Log
import androidx.core.graphics.createBitmap
import com.ddmh.wallpaper.WallpaperHelper.ARG_BG_PATH
import com.ddmh.wallpaper.WallpaperHelper.ARG_DEPTH_PATH
import com.ddmh.wallpaper.WallpaperHelper.ARG_FRONT_PATH
import com.ddmh.wallpaper.filter.BaseFilter
import com.ddmh.wallpaper.util.BitmapUtils
import com.ddmh.wallpaper.util.LogUtils
import com.ddmh.wallpaper.util.OpenGlUtils
import com.ddmh.wallpaper.util.Rotation
import com.ddmh.wallpaper.util.SpUtils
import com.ddmh.wallpaper.util.TextResourceReader.readShaderTextFromAsset
import com.ddmh.wallpaper.util.TextureRotationUtil
import com.ddmh.wallpaper.util.TextureRotationUtil.TEXTURE_NO_ROTATION
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.max

/**
 * 将背景图、前景抠图与深度图合成立体视差效果的渲染器
 */
class Object3DCombinedRenderer(
    private val context: Context
) : GLSurfaceView.Renderer {

    private val bgPath: String by lazy { SpUtils.getString(context, ARG_BG_PATH) }
    private val depthPath: String by lazy { SpUtils.getString(context, ARG_DEPTH_PATH) }
    private val frontPath: String by lazy { SpUtils.getString(context, ARG_FRONT_PATH) }

    private var frameStartTimeMs: Long = 0
    private var startTimeMs: Long = 0
    private var frameCount = 0

    private var outputWidth = 0
    private var outputHeight = 0
    private var imageWidth = 0
    private var imageHeight = 0

    // 动态偏移控制
    private var destinationXFactor = 0f
    private var destinationYFactor = 0f
    private var startXFactor = 0f
    private var startYFactor = 0f
    private var currentXFactor = 0f
    private var currentYFactor = 0f
    private var isAnimatingToDestination = false
    private var animationStartTime = 0L
    private val ANIMATION_DURATION_MS = 100L

    private var lastXValue = 0f
    private var lastYValue = 0f

    private var parallaxFilter: DepthParallaxFilter? = null

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        parallaxFilter = DepthParallaxFilter(readShaderTextFromAsset(context,"frag.glsl"))
        parallaxFilter?.onInit()

        // 加载三张纹理
        (BitmapUtils.uriToBitmap(context, bgPath)?:emptyBitmap()).let { bg ->
            imageWidth = bg.width
            imageHeight = bg.height
            val bgTex = OpenGlUtils.loadTexture(bg, OpenGlUtils.NO_TEXTURE)
            parallaxFilter?.setTexture(0, bgTex)
        }

        (BitmapUtils.uriToBitmap(context, depthPath)?:emptyBitmap()).let { depth ->
            val depthTex = OpenGlUtils.loadTexture(depth, OpenGlUtils.NO_TEXTURE)
            parallaxFilter?.setTexture(1, depthTex)
        }

        (BitmapUtils.uriToBitmap(context, frontPath)?:emptyBitmap()).let { front ->
            val frontTex = OpenGlUtils.loadTexture(front, OpenGlUtils.NO_TEXTURE)
            parallaxFilter?.setTexture(2, frontTex)
        }
    }

    private fun emptyBitmap() = createBitmap(1,1, Bitmap.Config.ARGB_8888)

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        outputWidth = width
        outputHeight = height
        adjustImageScaling()
        parallaxFilter?.onOutputSizeChanged(width, height)
    }

    override fun onDrawFrame(gl: GL10?) {
        logFrameRate()
        updateLinearInterpolation()
        parallaxFilter?.setFactor(currentXFactor, currentYFactor)
        parallaxFilter?.onDraw(0, glCubeBuffer, glTextureBuffer)
    }

    private fun updateLinearInterpolation() {
        if (!isAnimatingToDestination) return
        val elapsed = System.currentTimeMillis() - animationStartTime
        if (elapsed >= ANIMATION_DURATION_MS) {
            isAnimatingToDestination = false
            currentXFactor = destinationXFactor
            currentYFactor = destinationYFactor
        } else {
            val t = elapsed.toFloat() / ANIMATION_DURATION_MS
            val progress = 1f - (1 - t) * (1 - t)
            currentXFactor = startXFactor + (destinationXFactor - startXFactor) * progress
            currentYFactor = startYFactor + (destinationYFactor - startYFactor) * progress
        }
    }

    fun handleSensorChange(xValue: Float, yValue: Float) {
        destinationXFactor = xValue
        destinationYFactor = yValue
        startXFactor = currentXFactor
        startYFactor = currentYFactor
        animationStartTime = System.currentTimeMillis()
        isAnimatingToDestination = true
        lastXValue = xValue
        lastYValue = yValue
    }

    private fun logFrameRate() {
        if (frameStartTimeMs == 0L) frameStartTimeMs = SystemClock.elapsedRealtime()
        if (startTimeMs == 0L) startTimeMs = SystemClock.elapsedRealtime()
        val elapsedRealtimeMs = SystemClock.elapsedRealtime()
        val elapsedSeconds = (elapsedRealtimeMs - startTimeMs) / 1000.0
        if (elapsedSeconds >= 1.0) {
            Log.v(TAG, (frameCount / elapsedSeconds).toString() + "fps")
            startTimeMs = SystemClock.elapsedRealtime()
            frameCount = 0
        }
        frameCount++
    }

    private fun adjustImageScaling() {
        var outW = outputWidth.toFloat()
        var outH = outputHeight.toFloat()
        if (imageWidth == 0 || imageHeight == 0 || outW == 0f || outH == 0f) return

        val ratio1: Float = outW / imageWidth
        val ratio2: Float = outH / imageHeight
        val ratioMax = max(ratio1, ratio2)
        val imageWidthNew: Int = Math.round(imageWidth * ratioMax)
        val imageHeightNew: Int = Math.round(imageHeight * ratioMax)

        val ratioWidth = imageWidthNew / outW
        val ratioHeight = imageHeightNew / outH

        var cube = OpenGlUtils.CUBE
        var textureCords = TextureRotationUtil.getRotation(Rotation.NORMAL, false, false)
        val distHorizontal = (1 - 1 / ratioWidth) / 2
        val distVertical = (1 - 1 / ratioHeight) / 2
        textureCords = floatArrayOf(
            addDistance(textureCords[0], distHorizontal),
            addDistance(textureCords[1], distVertical),
            addDistance(textureCords[2], distHorizontal),
            addDistance(textureCords[3], distVertical),
            addDistance(textureCords[4], distHorizontal),
            addDistance(textureCords[5], distVertical),
            addDistance(textureCords[6], distHorizontal),
            addDistance(textureCords[7], distVertical),
        )

        glCubeBuffer.clear()
        glCubeBuffer.put(cube).position(0)
        glTextureBuffer.clear()
        glTextureBuffer.put(textureCords).position(0)
    }

    private fun addDistance(coordinate: Float, distance: Float): Float {
        return if (coordinate == 0.0f) distance else 1 - distance
    }

    companion object {
        private const val TAG = "Object3DRenderer"

    }

    private val glCubeBuffer: FloatBuffer by lazy {
        ByteBuffer.allocateDirect(OpenGlUtils.CUBE.size * 4).order(ByteOrder.nativeOrder())
            .asFloatBuffer()
    }
    private val glTextureBuffer: FloatBuffer by lazy {
        ByteBuffer.allocateDirect(TEXTURE_NO_ROTATION.size * 4).order(ByteOrder.nativeOrder())
            .asFloatBuffer()
    }
}

/**
 * 深度视差合成滤镜
 */
class DepthParallaxFilter(fragShader: String) : BaseFilter(
    NO_FILTER_VERTEX_SHADER,fragShader
) {
    private val textureIds = IntArray(3)
    private var factorHandle = 0

    private var resolutionHandle = 0
//    private var timeHandle = 0

    override fun onInit() {
        super.onInit()
        factorHandle = getHandle(program, "u_factor")
        resolutionHandle = getHandle(program, "u_resolution")
//        timeHandle = getHandle(program, "u_time")
    }

    fun setTexture(index: Int, texId: Int) {
        textureIds[index] = texId
    }

    fun setFactor(x: Float, y: Float) {
        LogUtils.e("x:$x  y:$y")
        setFloatVec2(factorHandle, floatArrayOf(y, x))
    }

    override fun onOutputSizeChanged(width: Int, height: Int) {
        super.onOutputSizeChanged(width, height)
        setFloatVec2(resolutionHandle, floatArrayOf(width.toFloat(), height.toFloat()))
    }


    override fun onDraw(textureId: Int, cubeBuffer: FloatBuffer, textureBuffer: FloatBuffer) {
        GLES20.glUseProgram(program)
        runPendingOnDrawTasks()
//        setFloat(timeHandle, SystemClock.uptimeMillis() / 1000f)
        cubeBuffer.position(0)
        GLES20.glVertexAttribPointer(attribPosition, 2, GLES20.GL_FLOAT, false, 0, cubeBuffer)
        GLES20.glEnableVertexAttribArray(attribPosition)
        textureBuffer.position(0)
        GLES20.glVertexAttribPointer(
            attribTextureCoordinate,
            2,
            GLES20.GL_FLOAT,
            false,
            0,
            textureBuffer
        )
        GLES20.glEnableVertexAttribArray(attribTextureCoordinate)

        // 绑定三张纹理
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureIds[0])
        GLES20.glUniform1i(getHandle(program, "u_bg"), 0)

        GLES20.glActiveTexture(GLES20.GL_TEXTURE1)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureIds[1])
        GLES20.glUniform1i(getHandle(program, "u_depth"), 1)

        GLES20.glActiveTexture(GLES20.GL_TEXTURE2)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureIds[2])
        GLES20.glUniform1i(getHandle(program, "u_front"), 2)

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)

        GLES20.glDisableVertexAttribArray(attribPosition)
        GLES20.glDisableVertexAttribArray(attribTextureCoordinate)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
    }
}
