package com.example.androiddepth;

public interface IMotionSensorListener {
    public void onRawMotionDataAvailable(double[] accel, double[] gyro, double[] linAcc, double[] ori);
}
