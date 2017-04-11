package com.pixplicity.huethings.sensors;

import android.hardware.Sensor;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

// Listen for registration events from the sensor driver
public class SensorCallback extends SensorManager.DynamicSensorCallback {

    private static final String TAG = SensorCallback.class.getSimpleName();

    private SensorEventListener mSensorEventListener;
    private SensorManager mSensorManager;

    public SensorCallback(SensorManager sensorManager,
                          SensorEventListener sensorEventListener) {
        mSensorManager = sensorManager;
        mSensorEventListener = sensorEventListener;
    }

    @Override
    public void onDynamicSensorConnected(Sensor sensor) {
        Log.i(TAG, sensor.getName() + " has been connected");

        // Begin listening for sensor readings
        mSensorManager.registerListener(mSensorEventListener, sensor,
                SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    public void onDynamicSensorDisconnected(Sensor sensor) {
        Log.i(TAG, sensor.getName() + " has been disconnected");

        // Stop receiving sensor readings
        mSensorManager.unregisterListener(mSensorEventListener);
    }

}
