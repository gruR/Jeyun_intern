package com.example.jeyun.garam_main;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import com.kircherelectronics.fsensor.filter.BaseFilter;
import com.kircherelectronics.fsensor.filter.averaging.LowPassFilter;

import java.util.ArrayList;
import java.util.Date;

import static com.example.jeyun.garam_main.MainActivity.CURRENT_STATE;

public class accSensor extends AppCompatActivity implements SensorEventListener {

    SensorManager sm;
    Sensor sensor;
    private final int sensorType = Sensor.TYPE_ACCELEROMETER;

    private double accelerometerValue;
    private double timestamp;

    //for Acceleration sensor
    private double previousTime;
    private double previousRunningTime;

    private double RunningThreshold;
    private float currentThreshold;

    private ArrayList<Double> Acclist;
    private ArrayList<Double> RunningAcc;

    private double stepThreshold;
    private double stepInterval;
    private MapView fragment;

    public accSensor(SensorManager sm, MapView fragment) {
        super();
        this.sm = sm;
        this.fragment = fragment;
        sensor = sm.getDefaultSensor(sensorType);

        //LPF
        filter_acceleration = new LowPassFilter(); // LowPassFilter(), MeanFilter(), MedianFilter();
        filter_acceleration.setTimeConstant(0.18f);
        //LPF

        //Accelerometer initialize
        Acclist = new ArrayList<Double>();
        Acclist.add(0.4);
        Acclist.add(0.4);
        currentThreshold = 0.45f;

        RunningThreshold = 0.01;
        RunningAcc = new ArrayList<Double>();

        stepThreshold = 1.3;
        stepInterval = 500;
        previousRunningTime = 0;
        previousTime = 0;
        //***************************************//
    }

    public void start() {
        if (sensor != null) {
            sm.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI);
        } else {
            Toast.makeText(this, "Acceleration sensor not available!", Toast.LENGTH_LONG).show();
        }
    }

    public void stop() {
        sm.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        setAccelerometerValue(getSensorValue(sensorEvent));
        setTimestamp(timestamp = getTimestamp(sensorEvent));
        AccelerationSensing();
    }

    public void AccelerationSensing() {
        double accelerometerValue = getAccelerometerValue();
        double accelerometerTimestamp = getTimestamp();

        if (CURRENT_STATE == MainActivity.MODE_STATE.WaitingMove) {
            detect_change_move(accelerometerValue, accelerometerTimestamp);
        } else if (CURRENT_STATE == MainActivity.MODE_STATE.DR) {
            if (accelerometerValue < stepThreshold) {
                Acclist.add(accelerometerValue);
                if ((Acclist.get(1) > currentThreshold) && (accelerometerTimestamp - previousTime) > stepInterval
                        && (Acclist.get(1) > max(Acclist.get(0), Acclist.get(2)))) {
                    //걸음이 카운트 되는 자리
                    switch (fragment.getCurrentDirection()) {
                        case 0:    //UP
                            fragment.stepDectect(0, 1);
                            break;
                        case 1:    //RIGHT
                            fragment.stepDectect(1, 0);
                            break;
                        case 2:    //DOWN
                            fragment.stepDectect(0, -1);
                            break;
                        case 3:    //LEFT
                            fragment.stepDectect(-1, 0);
                            break;
                    }
                    ((MainActivity) fragment.getActivity()).stepCountIncrease(1);
                    previousTime = accelerometerTimestamp;
                }
                Acclist.remove(0);
            }
            detect_change_move(accelerometerValue, accelerometerTimestamp);
        }
    }

    public void detect_change_move(double accelerometerValue, double timestamp) {
        if (previousRunningTime == 0) {
            previousRunningTime = timestamp;
        }
        if (timestamp - previousRunningTime < 1000) // obtain acc during 1 second
        {
            RunningAcc.add(accelerometerValue);
        } else {
            double variance = Math.pow(calculateSD(RunningAcc), 2); // calculate variance

            previousRunningTime = 0;
            RunningAcc.clear();

            if (variance >= RunningThreshold) {
                CURRENT_STATE = MainActivity.MODE_STATE.DR;
            } else
                CURRENT_STATE = MainActivity.MODE_STATE.WaitingMove;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    /**
     * Gets event time. http://stackoverflow.com/questions/5500765/accelerometer-sensorevent-timestamp
     */
    private long getTimestamp(SensorEvent event) {
        return (new Date()).getTime() + (event.timestamp - System.nanoTime()) / 1000000L;
    }

    //LPF
    private float[] acceleration;
    private float[] filteredAcceleration;
    private BaseFilter filter_acceleration; //low pass filter

    private Double getSensorValue(SensorEvent event) {

        acceleration = new float[3];
        filteredAcceleration = new float[3];

        System.arraycopy(event.values, 0, acceleration, 0, event.values.length);
        filteredAcceleration = filter_acceleration.filter(acceleration);

        return Math.sqrt(filteredAcceleration[0] * filteredAcceleration[0] +
                filteredAcceleration[1] * filteredAcceleration[1] +
                filteredAcceleration[2] * filteredAcceleration[2]) - 9.8f;
    }

    public double calculateSD(ArrayList<Double> numArray) {
        double sum = 0.0, standardDeviation = 0.0;

        for (double num : numArray) {
            sum += num;
        }

        double mean = sum / numArray.size();

        for (double num : numArray) {
            standardDeviation += Math.pow(num - mean, 2);
        }

        return Math.sqrt(standardDeviation / numArray.size());
    }

    public double max(double v1, double v2) {
        if (v1 < v2) return v2;
        else return v1;
    }


    // setter & getter *********************************** //
    public double getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(double timestamp) {
        this.timestamp = timestamp;
    }

    public double getAccelerometerValue() {
        return accelerometerValue;
    }

    public void setAccelerometerValue(double accelerometerValue) {
        this.accelerometerValue = accelerometerValue;
    }

    public void clearPreviousRunningTime() {
        previousRunningTime = 0;
    }

    public void clearRunningAcc() {
        RunningAcc.clear();
    }
}

