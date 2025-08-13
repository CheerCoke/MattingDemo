package com.example.mattingdemo

import android.Manifest
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.SeekBar
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.core.graphics.applyCanvas
import androidx.core.graphics.createBitmap
import com.ddmh.lib_matting.sticker.BitmapSticker
import com.ddmh.lib_camera.LibCamera
import com.ddmh.lib_camera.core.BaseCameraActivity
import com.ddmh.lib_camera.core.BaseOnCameraInfoUpdate
import com.ddmh.lib_camera.core.CameraStateStore
import com.ddmh.lib_camera.core.DefaultUseCaseConfig
import com.ddmh.lib_camera.core.RecordUiState
import com.ddmh.lib_camera.core.UseCaseConfig
import com.ddmh.lib_camera.core.UseCaseType
import com.ddmh.lib_camera.model.CameraAspectRatio
import com.ddmh.lib_camera_effect.effect.filter.BaseFilter
import com.ddmh.lib_camera_effect.effect.filter.FilterGroup
import com.ddmh.lib_camera_effect.effect.filter_effect.FilterEffect
import com.ddmh.lib_matting.filter.MattingFilter
import com.ddmh.lib_matting.sticker.Sticker
import com.ddmh.lib_matting.filter.StickerFilter
import com.ddmh.lib_matting.sticker.OnStickerOperationListener
import com.example.mattingdemo.databinding.ActivityMattingVideoBinding
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions
import java.io.File
import kotlin.also
import kotlin.apply
import kotlin.let


class MattingVideoActivity :
    BaseCameraActivity<ActivityMattingVideoBinding>(ActivityMattingVideoBinding::inflate) {

    companion object {
        const val TAG = "MattingVideoActivity"
    }

    private var colorMode = ColorMode.GREEN
    private var bitmapBg: Bitmap? = null

    /**
     * 贴纸1
     */
    private var sticker1: Sticker? = null

    /**
     * 贴纸2
     */
    private var sticker2: Sticker? = null

    /**
     * 贴纸3
     */
    private var sticker3: Sticker? = null

    override fun getPreviewView(): PreviewView {
        return binding.layoutCamera.previewView
    }

    override fun getPreviewCover(): View? {
        return binding.layoutCamera.previewCover
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!XXPermissions.isGranted(this@MattingVideoActivity, Permission.CAMERA)) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
        getCameraController()?.setFilterEffects {
            object : FilterEffect() {
                override var isPreview: Boolean = true

                override fun filter(): () -> BaseFilter = {
                    FilterGroup(
                        listOf(
                            MattingFilter().apply {
                                setKeyColor(colorMode)
                                setBgBitmap(
                                    BitmapFactory.decodeResource(
                                        LibCamera.getApplication().resources,
                                        R.drawable.sample_bg
                                    )
                                )
                            },
                            StickerFilter().apply { bindStickerView(binding.stickerView) }
                        ))
                }

            }
        }

    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            Log.e(TAG, "bindToLifecycle")
            this@MattingVideoActivity.getCameraController()?.bindToLifecycle(this)
        }
    }

    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        // 获取到选中的图片 URI
        if (uri != null) {
            // 可以将 URI 转换为 Bitmap
            bitmapBg = MediaStore.Images.Media.getBitmap(contentResolver, uri).also {
                getCameraController()?.withFilter(MattingFilter::class.java) {
                    setBgBitmap(it)
                }
            }

        }
    }

    private val stickerPickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            MediaStore.Images.Media.getBitmap(contentResolver, uri).also { bitmap ->
                binding.stickerView.addSticker(
                    BitmapSticker(bitmap).also {
                        sticker3 = it
                    }
                )
            }

        }
    }

    // 启动图片选择器
    private fun openImagePicker() {
        imagePickerLauncher.launch("image/*")
    }

    private fun openStickerImagePicker() {
        stickerPickerLauncher.launch("image/*")
    }

    override fun initListener() {
        super.initListener()
        //贴纸事件监听
        binding.stickerView.setOnStickerOperationListener(object :
            OnStickerOperationListener() {
            override fun onStickerAdded(sticker: Sticker) {
                getCameraController()?.withFilter(StickerFilter::class.java) {
                    addSticker(sticker, binding.stickerView.width, binding.stickerView.height)
                }
            }

            override fun onStickerDeleted(sticker: Sticker) {
                getCameraController()?.withFilter(StickerFilter::class.java) {
                    removeSticker(sticker)
                }
            }

            override fun onStickerTouchedDown(sticker: Sticker) {
                getCameraController()?.withFilter(StickerFilter::class.java) {
                    touchSticker(sticker)
                }
            }
        })
        binding.btnSticker1.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                binding.stickerView.addSticker(
                    BitmapSticker(
                        createBitmap(300, 400).applyCanvas {
                            drawColor(Color.GREEN)
                        }
//                        BitmapFactory.decodeResource(
//                            resources,
//                            R.drawable.obama
//                        )
                    ).also {
                        sticker1 = it
                    }
                )
            } else {
                sticker1?.let {
                    binding.stickerView.remove(it)
                }
            }
        }
        binding.btnSticker2.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                binding.stickerView.addSticker(
                    BitmapSticker(
                        BitmapFactory.decodeResource(
                            resources,
                            R.drawable.icon_qc
                        )
                    ).also {
                        sticker2 = it
                    }
                )
            } else {
                sticker2?.let {
                    binding.stickerView.remove(it)
                }
            }
        }
        binding.btnSticker3.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                openStickerImagePicker()
            } else {
                sticker3?.let {
                    binding.stickerView.remove(it)
                }
            }
        }

        binding.rbMenu.setOnCheckedChangeListener { group, checkedId ->
            when (checkedId) {
                R.id.rb_red -> {
                    colorMode = ColorMode.RED
                }

                R.id.rb_green -> {
                    colorMode = ColorMode.GREEN
                }

                R.id.rb_blue -> {
                    colorMode = ColorMode.BLUE
                }
            }
            getCameraController()?.withFilter(MattingFilter::class.java) {
                setKeyColor(colorMode)
            }
        }

        binding.btnChangeImage.setOnClickListener {
            openImagePicker()
        }

        binding.btnChangeLens.setOnClickListener {
            getCameraController()?.let {
                it.setLensFacing(it.getLensFacing().toggle())
            }
        }

        binding.seekbarSimilarity.setOnSeekBarChangeListener(object :
            SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(
                seekBar: SeekBar?,
                progress: Int,
                fromUser: Boolean
            ) {
                if (fromUser) {
                    getCameraController()?.withFilter(MattingFilter::class.java) {
                        setSimilarity((seekBar?.progress ?: 0) / 100f)
                    }
                }

            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }
        })

        binding.recordButton.setOnRecordToggleListener {
            if (it) {
                getCameraController()?.startRecording(
                    generateVideoFilePath("${System.currentTimeMillis()}.mp4")
                )
                getCameraController()?.updateFilter()
            } else {
                getCameraController()?.stopRecording()
            }
        }

        //相机状态回调
        getCameraController()?.setCameraInfoCallBack(object : BaseOnCameraInfoUpdate() {

            override fun onRecordingUpdateUI(uiState: RecordUiState) {
                when (uiState) {
                    is RecordUiState.RECORDING -> {

                    }

                    is RecordUiState.FINALIZED -> {
                        openFile(this@MattingVideoActivity, File(uiState.videoPath))
                    }
                }

            }
        })

    }


    override fun getUseCaseConfig(): UseCaseConfig {
        return DefaultUseCaseConfig(UseCaseType(UseCaseType.PREVIEW or UseCaseType.VIDEO_CAPTURE))
    }


    override fun getCameraStateStore(): CameraStateStore {
        return super.getCameraStateStore().apply {
            zoomEnable = false
            aspectRatio = CameraAspectRatio.of(CameraAspectRatio.ASPECT_9_16)
        }
    }

}


object ColorMode {
    val RED = floatArrayOf(0.99f, 0.0f, 0.0f)
    val GREEN = floatArrayOf(0.0f, 0.9f, 0.0f)
    val BLUE = floatArrayOf(0.001f, 0.02f, 0.99f)
}

