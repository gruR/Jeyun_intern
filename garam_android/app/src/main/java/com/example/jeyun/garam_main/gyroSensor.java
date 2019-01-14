package com.example.jeyun.garam_main;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import com.kircherelectronics.fsensor.filter.BaseFilter;
import com.kircherelectronics.fsensor.filter.averaging.LowPassFilter;

import static com.example.jeyun.garam_main.MainActivity.CURRENT_STATE;

public class gyroSensor extends AppCompatActivity implements SensorEventListener {

    SensorManager sm;
    Sensor sensor;
    private final int sensorType = Sensor.TYPE_GYROSCOPE;

    private double gyroValue;
    private double timestamp;

    //for Gyroscope
    private double previousGyroTime;

    // Create a constant to convert nanoseconds to seconds.
    private static final float NS2S = 1.0f / 1000000000.0f;

    //for gyro sensor
    private float totalOmega;

    double DETECT_GYRO_MAX = 7d;
    double DETECT_GYRO_MIN = 1d;

    public gyroSensor(SensorManager sm) {
        super();
        this.sm = sm;
        sensor = sm.getDefaultSensor(sensorType);

        //LPF
        filter_gyro = new LowPassFilter();
        filter_gyro.setTimeConstant(0.18f);
        //LPF

        // Gyroscope initialize
        previousGyroTime = 0;
        totalOmega = 0;
    }

    public void start() {
        if (sensor != null) {
            sm.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI);
        } else {
            Toast.makeText(this, "Gyroscope sensor not available!", Toast.LENGTH_LONG).show();
        }
    }

    public void stop() {
        sm.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        gyroValue = getSensorValue(sensorEvent);
        timestamp = sensorEvent.timestamp;

        GyroscopeSensing();
    }

    public void GyroscopeSensing() {
        if (CURRENT_STATE == MainActivity.MODE_STATE.DR) // DR 모드/Wait에서만 작동
        {
            if (previousGyroTime != 0) {
                float dT = (float) (timestamp - previousGyroTime) * NS2S;

                double temp = gyroValue * dT;

                //Log.d("gyro", String.valueOf(Math.toDegrees(temp)));

                if (Math.toDegrees(temp) < DETECT_GYRO_MAX && Math.toDegrees(temp) > DETECT_GYRO_MIN)
                    totalOmega += temp;

            }
            previousGyroTime = timestamp;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    //LPF
    private BaseFilter filter_gyro;//low pass filter
    private float[] gyro;
    private float[] filteredGyro;


    private Double getSensorValue(SensorEvent event) {

        gyro = new float[3];
        filteredGyro = new float[3];

        System.arraycopy(event.values, 0, gyro, 0, event.values.length);
        filteredGyro = filter_gyro.filter(gyro);

        return Math.sqrt(filteredGyro[0] * filteredGyro[0] +
                filteredGyro[1] * filteredGyro[1] +
                filteredGyro[2] * filteredGyro[2]);
    }

    public void clearTotalOmega() {
        totalOmega = 0;
    }

    public double getGyroValue() {
        return gyroValue;
    }

    public void setGyroValue(double gyroValue) {
        this.gyroValue = gyroValue;
    }

    public double getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(double timestamp) {
        this.timestamp = timestamp;
    }

    public float getTotalOmega() {
        return totalOmega;
    }

   public void setTotalOmega(float totalOmega) {
        this.totalOmega = totalOmega;
    }
}

