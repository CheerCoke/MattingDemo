package com.ddmh.wallpaper.util;

import static androidx.core.math.MathUtils.clamp;

public class OrientationController {
    private float sensorX = 0f, sensorY = 0f;  // 来自传感器的角度（-1~1）
    private float targetX = 0f, targetY = 0f;  // 目标角度
    private float currentX = 0f, currentY = 0f; // 当前显示角度
    private float lastSensorX = 0f, lastSensorY = 0f; // 上一次传感器角度
    private float speed = 0.05f; // 移动速度
    private float threshold = 0.05f; // 角度变化阈值，小于该值的变化会被忽略

    // 更新目标角度（由传感器传入）
    public void setTarget(float x, float y) {
        // 计算传感器X轴和Y轴角度的变化量
        float deltaX = x - lastSensorX;
        float deltaY = y - lastSensorY;
        
        // 根据X轴角度变化趋势确定目标值
        if (deltaX > threshold) {
            // 向右倾斜动作，目标设为1
            targetX = 1f;
        } else if (deltaX < -threshold) {
            // 向左倾斜动作，目标设为-1
            targetX = -1f;
        } else {
            // 变化太小或者无变化，目标回到0
            targetX = 0f;
        }
        
        // 根据Y轴角度变化趋势确定目标值
        if (deltaY > threshold) {
            // 向上抬起动作，目标设为1
            targetY = 1f;
        } else if (deltaY < -threshold) {
            // 向下压动作，目标设为-1
            targetY = -1f;
        } else {
            // 变化太小或者无变化，目标回到0
            targetY = 0f;
        }
        
        // 更新传感器角度记录
        lastSensorX = x;
        lastSensorY = y;
    }

    // 每帧调用，用于计算新的角度（例如在渲染 onDrawFrame 中）
    public void update() {
        // 线性移动到目标位置
        if (currentX < targetX) {
            currentX = Math.min(currentX + speed, targetX);
        } else if (currentX > targetX) {
            currentX = Math.max(currentX - speed, targetX);
        }
        
        if (currentY < targetY) {
            currentY = Math.min(currentY + speed, targetY);
        } else if (currentY > targetY) {
            currentY = Math.max(currentY - speed, targetY);
        }
        
        // 限制范围在 [-1, 1] 内
        currentX = clamp(currentX, -1f, 1f);
        currentY = clamp(currentY, -1f, 1f);
    }

    public float getCurrentX() { return currentX; }
    public float getCurrentY() { return currentY; }
    
    // 设置移动速度
    public void setSpeed(float speed) {
        this.speed = speed;
    }
    
    // 设置阈值
    public void setThreshold(float threshold) {
        this.threshold = threshold;
    }
}