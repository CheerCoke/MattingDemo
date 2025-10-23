/***
 * Excerpted from "OpenGL ES for Android",
 * published by The Pragmatic Bookshelf.
 * Copyrights apply to this code. It may not be used to create training material, 
 * courses, books, articles, and the like. Contact us if you are in doubt.
 * We make no guarantees that this code is fit for any purpose. 
 * Visit http://www.pragmaticprogrammer.com/titles/kbogla for more book information.
 ***/
package com.ddmh.wallpaper;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.opengl.GLSurfaceView;
import android.service.wallpaper.WallpaperService;
import android.util.Log;
import android.view.SurfaceHolder;

import com.ddmh.wallpaper.render.Object3DCombinedRenderer;
import com.ddmh.wallpaper.util.LoggerConfig;
import com.ddmh.wallpaper.util.SensorHelper;

/**
 * 壁纸服务
 *
 * @author Edison
 */
public class GLWallpaperService extends WallpaperService {
    @Override
    public Engine onCreateEngine() {
        return new GLEngine();
    }

    public class GLEngine extends Engine {
        private static final String TAG = "GLEngine";

        private static final float MAX_TRANS_DEGREE_X = 60f;   // X轴最大旋转角度
        private static final float MIN_TRANS_DEGREE_X = 0f;   // X轴最小旋转角度
        private static final float MAX_TRANS_DEGREE_Y = 15f;   // Y轴最大旋转角度
        private static final float MIN_TRANS_DEGREE_Y = -15f;   // Y轴最大旋转角度
        static final float ALPHA = 0.25f;


        private WallpaperGLSurfaceView glSurfaceView;
        private Object3DCombinedRenderer renderer;
        private boolean rendererSet;

        private SensorHelper sensorHelper;


        protected float[] lowPass(float[] input, float[] output) {
            if (output == null) return input;

            for (int i = 0; i < input.length; i++) {
                output[i] = output[i] + ALPHA * (input[i] - output[i]);
            }
            return output;
        }


        private float[] mAcceleValues = new float[16];
        private float[] mMageneticValues = new float[16];


        @Override
        public void onCreate(SurfaceHolder surfaceHolder) {
            super.onCreate(surfaceHolder);
            if (LoggerConfig.ON) {
                Log.d(TAG, "onCreate(" + surfaceHolder + ")");
            }
            renderer = new Object3DCombinedRenderer(GLWallpaperService.this);

            sensorHelper = new SensorHelper(GLWallpaperService.this);
            glSurfaceView = new WallpaperGLSurfaceView(GLWallpaperService.this);
            glSurfaceView.setEGLContextClientVersion(2);
            glSurfaceView.setPreserveEGLContextOnPause(true);
            glSurfaceView.setRenderer(renderer);
            rendererSet = true;
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);
            if (LoggerConfig.ON) {
                Log.d(TAG, "onVisibilityChanged(" + visible + ")");
            }
            if (rendererSet) {
                if (visible) {
                    glSurfaceView.onResume();
                    sensorHelper.listenTypeOf(Sensor.TYPE_ACCELEROMETER, event -> {
                        mAcceleValues = lowPass(event.values.clone(), mAcceleValues);
                        handleSensorChange();

                    });
                    sensorHelper.listenTypeOf(Sensor.TYPE_MAGNETIC_FIELD, event -> {
                        mMageneticValues = lowPass(event.values.clone(), mMageneticValues);
                        handleSensorChange();
                    });

                } else {
                    sensorHelper.release();
                    glSurfaceView.onPause();
                }
            }
        }

        @Override
        public void onOffsetsChanged(final float xOffset, final float yOffset,
                                     float xOffsetStep, float yOffsetStep, int xPixelOffset, int yPixelOffset) {
        }


        @Override
        public void onDestroy() {
            super.onDestroy();
            if (LoggerConfig.ON) {
                Log.d(TAG, "onDestroy()");
            }
            glSurfaceView.onWallpaperDestroy();
        }


        private void handleSensorChange() {

            float[] values = new float[3];
            float[] R = new float[9];
            SensorManager.getRotationMatrix(R, null, mAcceleValues, mMageneticValues);
            SensorManager.getOrientation(R, values);
            // x轴的偏转角度
            float degreeX = -(float) Math.toDegrees(values[1]);
            // y轴的偏转角度
            float degreeY = (float) Math.toDegrees(values[2]);


            if (degreeX > MAX_TRANS_DEGREE_X) {
                degreeX = MAX_TRANS_DEGREE_X;
            }
            if (degreeX < MIN_TRANS_DEGREE_X) {
                degreeX = MIN_TRANS_DEGREE_X;
            }
            if (degreeY > MAX_TRANS_DEGREE_Y) {
                degreeY = MAX_TRANS_DEGREE_Y;
            }
            if (degreeY < MIN_TRANS_DEGREE_Y) {
                degreeY = MIN_TRANS_DEGREE_Y;
            }

            Log.d(TAG, "degreeX:" + degreeX + ",degreeY:" + degreeY);

            renderer.handleSensorChange(((degreeX / (MAX_TRANS_DEGREE_X - MIN_TRANS_DEGREE_X)) - 0.5f) * 2f, degreeY / (MAX_TRANS_DEGREE_Y - MIN_TRANS_DEGREE_Y) * 2f);
        }

        class WallpaperGLSurfaceView extends GLSurfaceView {
            private static final String TAG = "WallpaperGLSurfaceView";

            WallpaperGLSurfaceView(Context context) {
                super(context);

                if (LoggerConfig.ON) {
                    Log.d(TAG, "WallpaperGLSurfaceView(" + context + ")");
                }
            }

            @Override
            public SurfaceHolder getHolder() {
                if (LoggerConfig.ON) {
                    Log.d(TAG, "getHolder(): returning " + getSurfaceHolder());
                }
                return getSurfaceHolder();
            }

            public void onWallpaperDestroy() {
                if (LoggerConfig.ON) {
                    Log.d(TAG, "onWallpaperDestroy()");
                }
                super.onDetachedFromWindow();
            }
        }
    }
}

