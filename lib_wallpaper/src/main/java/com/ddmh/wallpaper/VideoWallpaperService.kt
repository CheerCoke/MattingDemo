package com.ddmh.wallpaper

import android.service.wallpaper.WallpaperService


import android.media.MediaPlayer
import android.net.Uri
import android.view.SurfaceHolder
import com.ddmh.wallpaper.WallpaperHelper.ARG_VIDEO_PATH
import com.ddmh.wallpaper.util.SpUtils
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException

class VideoWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine {
        return VideoWallpaperEngine()
    }


    inner class VideoWallpaperEngine : Engine(), SurfaceHolder.Callback {
        private var mediaPlayer: MediaPlayer? = null
        private var videoPath: String = "" // 视频文件路径

        override fun onCreate(surfaceHolder: SurfaceHolder) {
            super.onCreate(surfaceHolder)
            surfaceHolder.addCallback(this)
            videoPath = SpUtils.getString(applicationContext, ARG_VIDEO_PATH)

        }

        override fun surfaceCreated(holder: SurfaceHolder) {
            initMediaPlayer()
        }

        override fun surfaceChanged(
            holder: SurfaceHolder,
            format: Int,
            width: Int,
            height: Int
        ) {
        }

        override fun surfaceDestroyed(holder: SurfaceHolder) {
        }

        override fun onDestroy() {
            super.onDestroy()
            releaseMediaPlayer()
        }

        override fun onVisibilityChanged(visible: Boolean) {
            super.onVisibilityChanged(visible)
            if (visible) {
                startPlayback()
            } else {
                pausePlayback()
            }
        }

        private fun initMediaPlayer() {
            mediaPlayer = MediaPlayer()
            try {
                when {
                    videoPath.startsWith("content://") -> {
                        // 处理 content URI
                        val uri = Uri.parse(videoPath)
                        val assetFileDescriptor = contentResolver.openAssetFileDescriptor(uri, "r")
                        assetFileDescriptor?.let {
                            mediaPlayer?.setDataSource(it.fileDescriptor, it.startOffset, it.length)
                            it.close()
                        }
                    }

                    else -> {
                        // 检查文件是否存在
                        val file = File(videoPath)
                        if (file.exists()) {
                            mediaPlayer?.setDataSource(videoPath)
                        } else {
                            throw FileNotFoundException("File not found: $videoPath")
                        }
                    }
                }
                mediaPlayer?.setSurface(surfaceHolder.surface)

                // 设置循环播放
                mediaPlayer?.isLooping = true

                // 设置准备监听器
                mediaPlayer?.setOnPreparedListener {
                    startPlayback()
                }

                // 异步准备
                mediaPlayer?.prepareAsync()

            } catch (e: IOException) {
                e.printStackTrace()
            }
        }

        private fun startPlayback() {
            if (mediaPlayer?.isPlaying == false) {
                mediaPlayer?.start()
            }
        }

        private fun pausePlayback() {
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.pause()
            }
        }

        private fun releaseMediaPlayer() {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.stop()
                }
                it.release()
            }
            mediaPlayer = null
        }


    }
}
