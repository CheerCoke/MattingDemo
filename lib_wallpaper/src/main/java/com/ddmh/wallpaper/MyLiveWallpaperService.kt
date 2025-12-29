package com.ddmh.wallpaper

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Looper
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import kotlin.random.Random

class MyLiveWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine {
        return MyEngine()
    }


    inner class MyEngine : Engine() {

        private val handler = android.os.Handler(Looper.getMainLooper())
        private val drawRunnable = Runnable { draw() }

        private var visible = false
        private var surfaceAvailable = false

        private val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG)

        private val circles = mutableListOf<MovingCircle>()

        override fun onSurfaceCreated(holder: SurfaceHolder) {
            super.onSurfaceCreated(holder)
            surfaceAvailable = true
            startDrawIfNeeded()
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            super.onSurfaceDestroyed(holder)
            surfaceAvailable = false
            handler.removeCallbacks(drawRunnable)
        }

        override fun onVisibilityChanged(visible: Boolean) {
            this.visible = visible
            if (visible) {
                startDrawIfNeeded()
            } else {
                handler.removeCallbacks(drawRunnable)
            }
        }

        override fun onDestroy() {
            super.onDestroy()
            handler.removeCallbacksAndMessages(null)
        }

        private fun startDrawIfNeeded() {
            if (visible && surfaceAvailable) {
                handler.removeCallbacks(drawRunnable)
                handler.post(drawRunnable)
            }
        }

        private fun initializeCircles(width: Int, height: Int) {
            circles.clear()
            repeat(10) {
                val radius = Random.nextFloat() * width * 0.05f + width * 0.02f
                circles.add(
                    MovingCircle(
                        x = Random.nextFloat() * (width - radius * 2) + radius,
                        y = Random.nextFloat() * (height - radius * 2) + radius,
                        radius = radius,
                        color = Color.rgb(
                            Random.nextInt(256),
                            Random.nextInt(256),
                            Random.nextInt(256)
                        ),
                        vx = (Random.nextFloat() - 0.5f) * 4f,
                        vy = (Random.nextFloat() - 0.5f) * 4f
                    )
                )
            }
        }

        private fun updateCircles(width: Int, height: Int) {
            for (circle in circles) {
                circle.x += circle.vx
                circle.y += circle.vy

                if (circle.x - circle.radius < 0 || circle.x + circle.radius > width) {
                    circle.vx = -circle.vx
                }
                if (circle.y - circle.radius < 0 || circle.y + circle.radius > height) {
                    circle.vy = -circle.vy
                }

                circle.x = circle.x.coerceIn(circle.radius, width - circle.radius)
                circle.y = circle.y.coerceIn(circle.radius, height - circle.radius)
            }
        }

        private fun draw() {
            val holder = surfaceHolder
            var canvas: Canvas? = null
            try {
                canvas = holder.lockCanvas()
                if (canvas != null) {
                    val width = canvas.width
                    val height = canvas.height

                    if (circles.isEmpty()) {
                        initializeCircles(width, height)
                    }

                    updateCircles(width, height)

                    canvas.drawColor(Color.BLACK)

                    for (circle in circles) {
                        circlePaint.color = circle.color
                        canvas.drawCircle(circle.x, circle.y, circle.radius, circlePaint)
                    }
                }
            } finally {
                canvas?.let { holder.unlockCanvasAndPost(it) }
            }

            if (visible && surfaceAvailable) {
                handler.postDelayed(drawRunnable, 16)
            }
        }
    }

}

data class MovingCircle(
    var x: Float,
    var y: Float,
    val radius: Float,
    val color: Int,
    var vx: Float, // x轴速度
    var vy: Float  // y轴速度
)

