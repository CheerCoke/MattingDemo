package com.example.wallpaper

import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import coil.load
import com.ddmh.wallpaper.WallpaperHelper
import com.example.CoilEngine
import com.example.mattingdemo.databinding.ActivityWallpaperBinding
import com.luck.picture.lib.basic.PictureSelector
import com.luck.picture.lib.config.SelectMimeType
import com.luck.picture.lib.config.SelectModeConfig
import com.luck.picture.lib.entity.LocalMedia
import com.luck.picture.lib.interfaces.OnResultCallbackListener
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream


class WallpaperActivity : AppCompatActivity() {
    private lateinit var binding: ActivityWallpaperBinding

    private var bgPath: String? = null
    private var frontPath: String? = null
    private var depthPath: String? = null


    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWallpaperBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initListener()
    }

    fun initListener() {
        binding.btnSet.setOnClickListener {
            val bg = bgPath ?: return@setOnClickListener
            val depth = depthPath ?: return@setOnClickListener
            WallpaperHelper.set3DWallpaper(this, bg, depth, frontPath ?: "")
        }

        binding.btnSetVideo.setOnClickListener {

            PictureSelector.create(this).openGallery(SelectMimeType.ofVideo())
                .isDirectReturnSingle(true)
                .setMaxSelectNum(1)
                .setSelectionMode(SelectModeConfig.SINGLE)
                .setImageEngine(CoilEngine())
                .forResult(object : OnResultCallbackListener<LocalMedia> {
                    override fun onResult(result: ArrayList<LocalMedia>?) {
                        if (result != null) {
                            val videoPath = result.get(0).availablePath ?: return
                            WallpaperHelper.setVideoWallpaper(
                                this@WallpaperActivity, videoPath
                            )
                        }
                    }

                    override fun onCancel() {

                    }

                })
        }

        binding.btnSetImage.setOnClickListener {
            pickImage {
                WallpaperHelper.setBitmapWallpaper(
                    this@WallpaperActivity,
                    MediaStore.Images.Media.getBitmap(contentResolver, it?.toUri())
                )
            }
        }

        binding.ivBg.setOnClickListener {
            pickImage {
                binding.ivBg.load(it)
                bgPath = it
            }
        }
        binding.ivBgDepth.setOnClickListener {
            pickImage {
                binding.ivBgDepth.load(it)
                depthPath = it
            }
        }
        binding.ivFront.setOnClickListener {
            pickImage {
                binding.ivFront.load(it)
                frontPath = it
            }
        }
    }


    fun pickImage(onResult: (String?) -> Unit) {
        PictureSelector.create(this).openGallery(SelectMimeType.ofImage())
            .isDirectReturnSingle(true)
            .setSelectionMode(SelectModeConfig.SINGLE)
            .setMaxSelectNum(1)
            .setImageEngine(CoilEngine())
            .forResult(object : OnResultCallbackListener<LocalMedia> {
                override fun onResult(result: ArrayList<LocalMedia>?) {
                    if (result != null) {
                        onResult(result.get(0).availablePath)
                    }
                }

                override fun onCancel() {

                }

            })
    }

    private fun writeMp4ToNative(file: File, `is`: InputStream) {
        try {
            val os = FileOutputStream(file)
            var len = -1
            val buffer = ByteArray(1024)
            while ((`is`.read(buffer).also { len = it }) != -1) {
                os.write(buffer, 0, buffer.size)
            }
            os.flush()
            os.close()
            `is`.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}