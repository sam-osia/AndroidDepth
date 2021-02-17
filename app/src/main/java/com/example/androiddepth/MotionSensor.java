package com.example.androiddepth;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorManager;

public class MotionSensor {

    public MotionSensor(Context context, IMotionSensorListener IMotionSensorListener)
    {
        SensorManager sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);

        Sensor accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        Sensor gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        Sensor linearAcceleration = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        Sensor magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        MotionSensorListener motionSensorListener = new MotionSensorListener(IMotionSensorListener);

        sensorManager.registerListener(motionSensorListener, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(motionSensorListener, gyroscope, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(motionSensorListener, linearAcceleration, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(motionSensorListener, magnetometer, SensorManager.SENSOR_DELAY_NORMAL);
    }
}
