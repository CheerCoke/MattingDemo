package com.ddmh.wallpaper.util


import android.util.Log
import com.example.feature_wallpaper.BuildConfig


internal object LogUtils {

    private val isDebug = LoggerConfig.ON
    const val TAG: String = "wallpaper"

    @JvmStatic
    fun v(msg: String?) {
        if (isDebug) {
            Log.v(TAG, msg!!)
        }
    }

    @JvmStatic
    fun i(msg: String?) {
        if (isDebug) {
            Log.i(TAG, msg!!)
        }
    }

    @JvmStatic
    fun d(tag:String,msg: String?) {
        if (isDebug) {
            Log.d(tag, msg!!)
        }
    }

    @JvmStatic
    fun e(msg: String?) {
        Log.e(TAG, msg!!)
    }

    @JvmStatic
    fun e(tag:String,msg: String?) {
        Log.e(TAG+tag, msg!!)
    }
}
