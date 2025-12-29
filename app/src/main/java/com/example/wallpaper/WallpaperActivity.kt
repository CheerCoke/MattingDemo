package com.example.wallpaper

import android.os.Bundle
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


    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWallpaperBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initListener()
    }

    fun initListener() {
        binding.btnSetLiveTest.setOnClickListener {
            WallpaperHelper.setTestLiveWallpaper(this)
        }
        binding.btnSet3d.setOnClickListener {
            pickImage {
                WallpaperHelper.set3DWallpaper(this, it?:return@pickImage)
            }
        }

        binding.btnSetVideo.setOnClickListener {

            PictureSelector.create(this).openGallery(SelectMimeType.ofVideo())
                .isDirectReturnSingle(true).setMaxSelectNum(1)
                .setSelectionMode(SelectModeConfig.SINGLE).setImageEngine(CoilEngine())
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
    }

    fun pickImage(onResult: (String?) -> Unit) {
        PictureSelector.create(this).openGallery(SelectMimeType.ofImage())
            .isDirectReturnSingle(true).setSelectionMode(SelectModeConfig.SINGLE).setMaxSelectNum(1)
            .setImageEngine(CoilEngine()).forResult(object : OnResultCallbackListener<LocalMedia> {
                override fun onResult(result: ArrayList<LocalMedia>?) {
                    if (result != null) {
                        onResult(result.get(0).availablePath)
                    }
                }

                override fun onCancel() {

                }

            })
    }
}