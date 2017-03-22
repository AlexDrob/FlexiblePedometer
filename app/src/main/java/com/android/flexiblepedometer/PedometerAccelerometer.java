package com.android.flexiblepedometer;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

/**
 * Created by AREG on 02.03.2017.
 */

public class PedometerAccelerometer implements SensorEventListener {

    private final static String TAG = "StepDetector";

    private static float mLimit = 15.0f;

    private float   mLastValues[] = new float[3*2];
    private float   mScale[] = new float[2];
    private float   mYOffset;

    private float   mLastDirections[] = new float[3*2];
    private float   mLastExtremes[][] = { new float[3*2], new float[3*2] };
    private float   mLastDiff[] = new float[3*2];
    private int     mLastMatch = -1;

    private SensorManager mSensorManager;
    private Sensor mSensor;

    private PedometerAccelerometerListener mPedometerAccelerometerListener;

    public interface PedometerAccelerometerListener {
        public void StepHasBeenDone();
    }

    public PedometerAccelerometer(Context context) {

        int h = 480; // TODO: remove this constant
        mYOffset = h * 0.5f;
        mScale[0] = - (h * 0.5f * (1.0f / (SensorManager.STANDARD_GRAVITY * 2)));
        mScale[1] = - (h * 0.5f * (1.0f / (SensorManager.MAGNETIC_FIELD_EARTH_MAX)));

        mSensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
    }

    public void PedometerAccelerometerListenerSet(PedometerAccelerometerListener listener) {
        mPedometerAccelerometerListener = listener;
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    public void StartListen() {
        mSensorManager.registerListener(PedometerAccelerometer.this, mSensor, 20000);//SensorManager.SENSOR_DELAY_FASTEST); // SensorManager.SENSOR_DELAY_NORMAL
    }

    public void StopListen() {
        mSensorManager.unregisterListener(PedometerAccelerometer.this);
    }

    public static void setSensitivity(float sensitivity) {
        mLimit = sensitivity;
        Log.d(TAG, "set new sensitivity: " + String.valueOf(mLimit));
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        Sensor sensor = event.sensor;
        //Log.d(TAG, "new accelerometer value! " + sensor.getType());
        //Log.d(TAG, "accuracy: " + event.accuracy);
        //Log.d(TAG, "values: " + String.valueOf(event.values[0]) + " " + String.valueOf(event.values[1]) + " "+ String.valueOf(event.values[2]));
        synchronized (this) {
            if (sensor.getType() == Sensor.TYPE_ORIENTATION) {
            }
            else {
                int j = (sensor.getType() == Sensor.TYPE_ACCELEROMETER) ? 1 : 0;
                if (j == 1) {
                    float vSum = 0;
                    for (int i=0 ; i<3 ; i++) {
                        final float v = mYOffset + event.values[i] * mScale[j];
                        vSum += v;
                    }
                    int k = 0;
                    float v = vSum / 3;

                    float direction = (v > mLastValues[k] ? 1 : (v < mLastValues[k] ? -1 : 0));
                    if (direction == - mLastDirections[k]) {
                        // Direction changed
                        int extType = (direction > 0 ? 0 : 1); // minumum or maximum?
                        mLastExtremes[extType][k] = mLastValues[k];
                        float diff = Math.abs(mLastExtremes[extType][k] - mLastExtremes[1 - extType][k]);

                        if (diff > mLimit) {

                            boolean isAlmostAsLargeAsPrevious = diff > (mLastDiff[k]*2/3);
                            boolean isPreviousLargeEnough = mLastDiff[k] > (diff/3);
                            boolean isNotContra = (mLastMatch != 1 - extType);

                            if (isAlmostAsLargeAsPrevious && isPreviousLargeEnough && isNotContra) {
                                Log.i(TAG, "step");
                                if (mPedometerAccelerometerListener != null) {
                                    mPedometerAccelerometerListener.StepHasBeenDone();
                                }
                                mLastMatch = extType;
                            }
                            else {
                                mLastMatch = -1;
                            }
                        }
                        mLastDiff[k] = diff;
                    }
                    mLastDirections[k] = direction;
                    mLastValues[k] = v;
                }
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        /*if (sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            if (accuracy != SensorManager.SENSOR_DELAY_FASTEST) {
                if (PedometerService.isListenAccelerometer() == true) {
                    mSensorManager.registerListener(PedometerAccelerometer.this, mSensor, SensorManager.SENSOR_DELAY_FASTEST);
                }
            }
        }*/
        Log.d(TAG, "onAccuracyChanged(), sensor:" + String.valueOf(sensor) + " accuracy: " + String.valueOf(accuracy));
    }
}
