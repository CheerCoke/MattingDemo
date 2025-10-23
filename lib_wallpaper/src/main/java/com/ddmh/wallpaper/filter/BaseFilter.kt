package com.ddmh.wallpaper.filter

import android.graphics.PointF
import android.opengl.GLES20
import com.ddmh.wallpaper.util.OpenGlUtils
import com.ddmh.wallpaper.util.OpenGlUtils.loadProgram
import java.nio.FloatBuffer
import java.util.LinkedList

open class BaseFilter @JvmOverloads constructor(
    private val vertexShader: String? = NO_FILTER_VERTEX_SHADER,
    private val fragmentShader: String? = NO_FILTER_FRAGMENT_SHADER
) {
    var TAG: String = javaClass.simpleName
    private val runOnDraw = LinkedList<Runnable>()

    private val runAfterOnDraw = LinkedList<Runnable>()
    var program: Int = 0
        private set
    var attribPosition: Int = 0
        private set
    var uniformTexture: Int = 0
        private set
    var attribTextureCoordinate: Int = 0
        private set
    var outputWidth: Int = 0
        private set
    var outputHeight: Int = 0
        private set
    var isInitialized: Boolean = false
        private set
    private val handleMap = HashMap<String, Int>()
    protected var intensity: Float = 1f

    private fun init() {
        onInit()
        onInitialized()
    }

    open fun onInit() {
        program = loadProgram(vertexShader, fragmentShader)
        attribPosition = GLES20.glGetAttribLocation(program, "position")
        uniformTexture = GLES20.glGetUniformLocation(program, "inputImageTexture")
        attribTextureCoordinate = GLES20.glGetAttribLocation(program, "inputTextureCoordinate")
        isInitialized = true
    }

    open fun onInitialized() {
    }

    fun ifNeedInit() {
        if (!isInitialized) init()
    }

    fun destroy() {
        isInitialized = false
        GLES20.glDeleteProgram(program)
        handleMap.clear()
        runOnDraw.clear()
        runAfterOnDraw.clear()
        onDestroy()
    }

    open fun onDestroy() {

    }

    open fun onOutputSizeChanged(width: Int, height: Int) {
        outputWidth = width
        outputHeight = height
    }

    open fun onDraw(
        textureId: Int, cubeBuffer: FloatBuffer,
        textureBuffer: FloatBuffer
    ) {
        GLES20.glUseProgram(program)
        runPendingOnDrawTasks()
        if (!isInitialized) {
            return
        }

//        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

        cubeBuffer.position(0)
        GLES20.glVertexAttribPointer(attribPosition, 2, GLES20.GL_FLOAT, false, 0, cubeBuffer)
        GLES20.glEnableVertexAttribArray(attribPosition)
        textureBuffer.position(0)
        GLES20.glVertexAttribPointer(
            attribTextureCoordinate, 2, GLES20.GL_FLOAT, false, 0,
            textureBuffer
        )
        GLES20.glEnableVertexAttribArray(attribTextureCoordinate)
        if (textureId != OpenGlUtils.NO_TEXTURE) {
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
            GLES20.glUniform1i(uniformTexture, 0)
        }
        onDrawArraysPre()
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
        onDrawArraysAfter(cubeBuffer, textureBuffer)
        GLES20.glDisableVertexAttribArray(attribPosition)
        GLES20.glDisableVertexAttribArray(attribTextureCoordinate)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)

        runPendingAfterDrawTasks()
    }

    protected fun runPendingAfterDrawTasks() {
        synchronized(runAfterOnDraw) {
            while (!runAfterOnDraw.isEmpty()) {
                runAfterOnDraw.removeFirst().run()
            }
        }
    }

    protected open fun onDrawArraysPre() {
    }

    protected open fun onDrawArraysAfter(
        cubeBuffer: FloatBuffer,
        textureBuffer: FloatBuffer
    ) {
    }

    protected fun getHandle(program: Int, name: String): Int {
        val value = handleMap[name]
        if (value != null) {
            return value
        }

        var location = GLES20.glGetAttribLocation(program, name)
        if (location == -1) {
            location = GLES20.glGetUniformLocation(program, name)
        }
        check(location != -1) { "Could not get attrib or uniform location for $name" }
        handleMap[name] = location
        return location
    }

    protected fun runPendingOnDrawTasks() {
        synchronized(runOnDraw) {
            while (!runOnDraw.isEmpty()) {
                runOnDraw.removeFirst().run()
            }
        }
    }


    fun setInteger(location: Int, intValue: Int) {
        runOnDraw {
            ifNeedInit()
            GLES20.glUniform1i(location, intValue)
        }
    }

    fun setFloat(location: Int, floatValue: Float) {
        runOnDraw {
            ifNeedInit()
            GLES20.glUniform1f(location, floatValue)
        }
    }

    fun setFloatVec2(location: Int, arrayValue: FloatArray?) {
        runOnDraw {
            ifNeedInit()
            GLES20.glUniform2fv(location, 1, FloatBuffer.wrap(arrayValue))
        }
    }

    fun setFloatVec3(location: Int, arrayValue: FloatArray?) {
        runOnDraw {
            ifNeedInit()
            GLES20.glUniform3fv(location, 1, FloatBuffer.wrap(arrayValue))
        }
    }

    fun setFloatVec4(location: Int, arrayValue: FloatArray?) {
        runOnDraw {
            ifNeedInit()
            GLES20.glUniform4fv(location, 1, FloatBuffer.wrap(arrayValue))
        }
    }

    protected fun setFloatArray(location: Int, arrayValue: FloatArray) {
        runOnDraw {
            ifNeedInit()
            GLES20.glUniform1fv(location, arrayValue.size, FloatBuffer.wrap(arrayValue))
        }
    }

    fun setPoint(location: Int, point: PointF) {
        runOnDraw {
            ifNeedInit()
            val vec2 = FloatArray(2)
            vec2[0] = point.x
            vec2[1] = point.y
            GLES20.glUniform2fv(location, 1, vec2, 0)
        }
    }

    protected fun setUniformMatrix3f(location: Int, matrix: FloatArray?) {
        runOnDraw {
            ifNeedInit()
            GLES20.glUniformMatrix3fv(location, 1, false, matrix, 0)
        }
    }

    protected fun setUniformMatrix4f(location: Int, matrix: FloatArray?) {
        runOnDraw {
            ifNeedInit()
            GLES20.glUniformMatrix4fv(location, 1, false, matrix, 0)
        }
    }

    protected fun runOnDraw(runnable: Runnable) {
        synchronized(runOnDraw) {
            runOnDraw.addLast(runnable)
        }
    }

    protected fun runAfterOnDraw(runnable: Runnable) {
        synchronized(runAfterOnDraw) {
            runAfterOnDraw.addLast(runnable)
        }
    }

    open fun adjust(intensity: Float) {
        this.intensity = intensity
    }


    open fun getOutputTextureId(): Int = OpenGlUtils.NO_TEXTURE


    /**
     * 预览时是否可见
     */
    open fun enablePreview(): Boolean {
        return true
    }

    /**
     * 是否需要保证方向为竖直向上
     */
    open fun ensureStraightUp(): Boolean {
        return false
    }

    override fun toString(): String {
        return this.javaClass.simpleName
    }

    companion object {
        const val NO_FILTER_VERTEX_SHADER: String = "" +
                "attribute vec4 position;\n" +
                "attribute vec4 inputTextureCoordinate;\n" +
                " \n" +
                "varying vec2 textureCoordinate;\n" +
                " \n" +
                "void main()\n" +
                "{\n" +
                "    gl_Position = position;\n" +
                "    textureCoordinate = inputTextureCoordinate.xy;\n" +
                "}"
        const val NO_FILTER_FRAGMENT_SHADER: String = "" +
                "varying highp vec2 textureCoordinate;\n" +
                " \n" +
                "uniform sampler2D inputImageTexture;\n" +
                " \n" +
                "void main()\n" +
                "{\n" +
                "     gl_FragColor = texture2D(inputImageTexture, textureCoordinate);\n" +
                "}"
    }
}

