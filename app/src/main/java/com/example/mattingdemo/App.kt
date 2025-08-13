package com.example.mattingdemo

import android.app.Application
import com.ddmh.lib_camera.LibCamera

/**
 *
 * @date 2025/2/12
 * @author mac
 **/
class App : Application() {
    override fun onCreate() {
        super.onCreate()

        LibCamera.init(this, true)
//        LibCamera.init(this, object : IDecrypt {
//            override fun decryptBitmap(inputStream: InputStream): Bitmap? {
//                return FileDecryptUtils.decryptImageFile(this@App, inputStream)
//            }
//
//            override fun decryptFile(inputStream: InputStream): InputStream {
//                return FileDecryptUtils.decryptFile(this@App, inputStream)
//            }
//        },isDebug = true)

    }
}