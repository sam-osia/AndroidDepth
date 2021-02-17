package com.example.androiddepth;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

public class MotionSensorListener implements SensorEventListener {
    private final String TAG = MotionSensorListener.class.getSimpleName();
    private IMotionSensorListener motionSensorListener;

    private float[] gravity;
    private double[] accel = new double[3];
    private double[] gyro = new double[3];
    private double[] linAcc = new double[3];
    private double[] ori = new double[3];

    public MotionSensorListener(IMotionSensorListener motionSensorListener) {
        this.motionSensorListener = motionSensorListener;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        int sensor = event.sensor.getType();
        if (sensor == Sensor.TYPE_ACCELEROMETER)
        {
            gravity = event.values; // used for getting orientation
            accel[0] = event.values[0];
            accel[1] = event.values[1];
            accel[2] = event.values[2];
        }
        else if (sensor == Sensor.TYPE_GYROSCOPE)
        {
            gyro[0] = event.values[0];
            gyro[1] = event.values[1];
            gyro[2] = event.values[2];
        }
        else if (sensor == Sensor.TYPE_LINEAR_ACCELERATION)
        {
            linAcc[0] = event.values[0];
            linAcc[1] = event.values[1];
            linAcc[2] = event.values[2];
        }
        else if (sensor == Sensor.TYPE_MAGNETIC_FIELD)
        {
            float R[] = new float[9];
            float I[] = new float[9];
            float[] geomagnetic = event.values;
            boolean success = SensorManager.getRotationMatrix(R, I, gravity, geomagnetic);
            if (success) {
                float orientation[] = new float[3];
                SensorManager.getOrientation(R, orientation);
                ori[0] = orientation[0] * 180 / Math.PI;
                ori[1] = orientation[1] * 180 / Math.PI;
                ori[2] = orientation[2] * 180 / Math.PI;
            }
            this.motionSensorListener.onRawMotionDataAvailable(accel, gyro, linAcc, ori);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}
