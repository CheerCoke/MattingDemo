package com.ddmh.wallpaper.util

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.core.util.Consumer

/**
 * 传感器辅助类
 *
 * @author frankie
 * @date 2024-05-11
 */
class SensorHelper(
    context: Context
) {
    // 官方文档: https://developer.android.com/develop/sensors-and-location/sensors/sensors_overview?hl=zh-cn
    // 传感器服务(注意要在隐私协议标注，影响上架)
    private val mSensorManager: SensorManager by lazy {
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }

    // 存储传感器监听器
    private val mListeners: MutableMap<Int, SensorEventListener> = HashMap()

    /**
     * 获取对应类型默认传感器
     *
     * @param type 传感器类型，例如: Sensor.TYPE_ACCELEROMETER
     */
    @Suppress("MemberVisibilityCanBePrivate")
    fun getSensor(type: Int, wakeUp: Boolean = false): Sensor? {
        return mSensorManager.getDefaultSensor(type, wakeUp)
    }

    /**
     * 获取所有类型的传感器
     *
     * @return 所有支持的sensor
     */
    fun getAllSensor(): List<Sensor> {
        return mSensorManager.getSensorList(Sensor.TYPE_ALL)
    }

    /**
     * 注册传感器监听，记得及时关闭监听
     *
     * @param type 监听传感器的类型
     * @param listener 监听回调
     * @param samplingPeriodUs 指定获取传感器频率
     *      SENSOR_DELAY_FASTEST 最快，延迟最小，同时也最消耗资源
     *      SENSOR_DELAY_GAME 适合游戏的频率
     *      SENSOR_DELAY_NORMAL 正常频率
     *      SENSOR_DELAY_UI 适合普通应用的频率,省电低耗
     */
    @Suppress("MemberVisibilityCanBePrivate")
    fun listenTypeOf(
        type: Int,
        listener: SensorEventListener,
        samplingPeriodUs: Int = SensorManager.SENSOR_DELAY_NORMAL
    ) {
        val sensor = getSensor(type)
        val oldListener = mListeners[type]

        // 先解除旧的监听，再监听
        if (oldListener != null) {
            mSensorManager.unregisterListener(oldListener)
        }

        // 注册监听，将listener保存起来
        mSensorManager.registerListener(listener, sensor, samplingPeriodUs)
        mListeners[type] = listener
    }

    /**
     * 取消传感器监听
     *
     * @param type 监听传感器的类型
     */
    fun stopListenTypeOf(type: Int) {
        val listener = mListeners[type]
        mSensorManager.unregisterListener(listener)
    }

    /**
     * 直接监听传感器，等release自动关闭监听
     *
     * @param type 类型
     * @param callback 加速度结果
     */
    fun listenTypeOf(
        type: Int,
        callback: Consumer<SensorEvent?>
    ) {
        // 直接监听，等release里面关闭
        listenTypeOf(type, object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                callback.accept(event)
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        })
    }

    /**
     * 释放资源
     */
    fun release() {
        // 取消所有监听
        mListeners.entries.forEach {
            val listener = it.value
            mSensorManager.unregisterListener(listener)
        }
        mListeners.clear()
    }
}
