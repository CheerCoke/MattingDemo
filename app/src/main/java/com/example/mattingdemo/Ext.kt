package com.example.mattingdemo

import android.content.Context
import android.content.Intent
import android.os.Environment
import androidx.core.content.FileProvider
import java.io.File


fun generateVideoFilePath(name: String): String {
    val dir =
        File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)?.absolutePath + File.separator + "lib_camera")
    if (!dir.exists()) dir.mkdirs()
    return dir.absolutePath + File.separator + name
}

fun openFile(context:Context,file: File) {
    if (file.exists()) {
        val intent = Intent(Intent.ACTION_VIEW)
        val mimeType = when {
            file.name.endsWith(".jpg") || file.name.endsWith(".jpeg") || file.name.endsWith(
                ".png"
            ) -> "image/*"

            file.name.endsWith(".mp4") || file.name.endsWith(".3gp") -> "video/*"
            else -> {
                return
            }
        }
        val uri =
            // Android 7.0 及以上需要使用 FileProvider
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

        intent.setDataAndType(uri, mimeType)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        if (context.packageManager.queryIntentActivities(intent, 0).isNotEmpty()) {
            context.startActivity(intent)
        }
    }
}
