package com.ddmh.wallpaper

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import com.ddmh.wallpaper.util.SpUtils

object WallpaperHelper {
    const val ARG_BG_PATH = "arg_bg_path"
    const val ARG_DEPTH_PATH = "arg_depth_path"
    const val ARG_FRONT_PATH = "arg_front_path"
    const val ARG_VIDEO_PATH = "arg_video_path"

    /**
     * 设置3d壁纸
     */
    fun set3DWallpaper(context: Context, bgPath: String, depthPath: String, frontPath: String) {
        try {
            SpUtils.putString(context, ARG_BG_PATH, bgPath)
            SpUtils.putString(context, ARG_DEPTH_PATH, depthPath)
            SpUtils.putString(context, ARG_FRONT_PATH, frontPath)
            val intent = Intent()
            intent.setAction(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER)
            intent.putExtra(
                WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                ComponentName(
                    context.packageName,
                    GLWallpaperService::class.java.canonicalName!!
                )
            )
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 设置图片壁纸
     */
    fun setBitmapWallpaper(context: Context, bitmap: Bitmap) {
        WallpaperManager.getInstance(context).setBitmap(bitmap)
    }

    /**
     * 设置视频壁纸
     * @param videoPath 视频路径
     */
    fun setVideoWallpaper(context: Context, videoPath: String) {
        SpUtils.putString(context, ARG_VIDEO_PATH, videoPath)
        val intent = Intent()
        intent.setAction(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER)
        intent.putExtra(
            WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
            ComponentName(
                context.packageName,
                VideoWallpaperService::class.java.canonicalName!!
            )
        )
        context.startActivity(intent)

    }
}