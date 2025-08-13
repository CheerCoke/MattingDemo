package com.example.mattingdemo
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
import android.view.animation.OvershootInterpolator

class RecordButtonView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private var isRecording = false
    private var progress = 0f // 按钮形态进度 0: 圆形, 1: 方形

    private val buttonPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.RED
        style = Paint.Style.FILL
    }

    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.RED
        style = Paint.Style.STROKE
        strokeWidth = 10f
    }

    private val breathPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.RED
        style = Paint.Style.FILL
        alpha = 60
    }

    private var breathRadius = 0f
    private var breathIncreasing = true

    private var animator = ValueAnimator()
    private var breathAnimator: ValueAnimator? = null

    private var recordProgress = 0f // 0-1 之间表示录制时间进度
    private var showProgressRing = true

    private var listener: ((Boolean) -> Unit)? = null



    fun setOnRecordToggleListener(l: (Boolean) -> Unit) {
        listener = l
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val size = MeasureSpec.getSize(widthMeasureSpec).coerceAtMost(MeasureSpec.getSize(heightMeasureSpec))
        setMeasuredDimension(size, size)
    }

    override fun onDraw(canvas: Canvas) {
        val cx = width / 2f
        val cy = height / 2f
        val baseRadius = width * 0.35f
        val maxBreath = width * 0.45f

        // 🌫️ 呼吸圈（待机时）
        if (isRecording) {
            canvas.drawCircle(cx, cy, breathRadius, breathPaint)
        }

        // ⬛ 录制按钮主体：从圆形变方形
        if (isRecording) {
            val size = baseRadius * 0.5f * progress
            val radius = 20f
            canvas.drawRoundRect(
                cx - size, cy - size, cx + size, cy + size,
                radius, radius, buttonPaint
            )
        } else {
            canvas.drawCircle(cx, cy, baseRadius * (1 - progress), buttonPaint)
        }

        // 🔘 外圈进度环
        if (isRecording) {
            val sweepAngle = recordProgress * 360
            canvas.drawArc(
                cx - maxBreath, cy - maxBreath, cx + maxBreath, cy + maxBreath,
                -90f, sweepAngle, false, ringPaint
            )
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            toggleState()
            listener?.invoke(isRecording)
            return true
        }
        return super.onTouchEvent(event)
    }

    private fun toggleState() {
        isRecording = !isRecording
        animateShapeTransition()
        if (isRecording) {
            startBreathAnim()
            showProgressRing = true
        } else {
            stopBreathAnim()
            resetToInitialState()
        }
    }

    private fun animateShapeTransition() {
        animator.cancel()
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 300
            interpolator = OvershootInterpolator()
            addUpdateListener {
                progress = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    private fun startBreathAnim() {
        breathAnimator = ValueAnimator.ofFloat(0.35f, 0.45f).apply {
            duration = 1000
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener {
                breathRadius = width * (it.animatedValue as Float)
                invalidate()
            }
            start()
        }
    }

    private fun stopBreathAnim() {
        breathAnimator?.cancel()
        breathRadius = 0f
    }

    fun updateProgress(fraction: Float) {
        recordProgress = fraction.coerceIn(0f, 1f)
        invalidate()
    }

    fun resetToInitialState() {
        isRecording = false
        showProgressRing = false
        recordProgress = 0f
        animator.cancel()
        ValueAnimator.ofFloat(progress, 0f).apply {
            duration = 200
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                progress = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    fun isRecording(): Boolean = isRecording
}
