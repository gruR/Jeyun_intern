package com.example.jeyun.main_map;
/*
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

public class StepCount extends AppCompatActivity implements SensorEventListener {

    private SensorManager sensorManager;
    private Sensor stepCountSensor;
    public int step_cnt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
        stepCountSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        if(stepCountSensor == null) {
            //Toast.makeText(this, "No Step Detect Sensor", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(this, stepCountSensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if(event.sensor.getType() == Sensor.TYPE_STEP_COUNTER) {
            //tvStepCount.setText("Step Count : " + String.valueOf(event.values[0]));
            step_cnt = (int)event.values[0];
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

}*/

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import com.kircherelectronics.fsensor.filter.BaseFilter;
import com.kircherelectronics.fsensor.filter.averaging.LowPassFilter;

import java.util.ArrayList;
import java.util.Date;

public class StepCount extends AppCompatActivity implements SensorEventListener{

    private SensorManager sensorManager;
    public int count;
    boolean activityRunning;
    int num;
    int count_num;
    TextView ex;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ex = (TextView)findViewById(R.id.textView_temp);

        num = 0;
        count_num = 0;
        ex.setText(""+count);
        //number = (TextView) findViewById(R.id.number);
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        init();
    }


    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            updateThread();
        }
    };

    private void updateThread() {
        num++;
    }

    @Override
    protected void onStart() {

        super.onStart();
        Thread myThread = new Thread(new Runnable() {
            public void run() {
                while (true) {
                    try {
                        handler.sendMessage(handler.obtainMessage());
                        Thread.sleep(1000);
                    } catch (Throwable t) {
                    }
                }
            }
        });

        myThread.start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        activityRunning = true;

        /* STEP DETECTOR
        Sensor countSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR);
        if (countSensor != null) {
            sensorManager.registerListener(this, countSensor, SensorManager.SENSOR_DELAY_UI);
        } else {
            Toast.makeText(this, "Count sensor not available!", Toast.LENGTH_LONG).show();
        }
        */

        Sensor countSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if (countSensor != null) {
            sensorManager.registerListener(this, countSensor, SensorManager.SENSOR_DELAY_UI);
        } else {
            Toast.makeText(this, "Count sensor not available!", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        activityRunning = false;
        // if you unregister the last listener, the hardware will stop detecting step events
//        sensorManager.unregisterListener(this);
    }

    double previousTime = 0;
    double previousRunningTime = 0;

    double RunningThreshold;
    private float currentThreshold;

    ArrayList<Double> Acclist;
    ArrayList<Double> RunningAcc;

    private boolean isStateRunning;

    @Override
    public void onSensorChanged(SensorEvent event) {
        double accelerometerValue = getSensorValue(event);
        double timestamp = getTimestamp(event);

        if (isStateRunning == false) {
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
                    isStateRunning = true;
                }
            }
        } else {
            if (accelerometerValue < 1.3f) {
                Acclist.add(accelerometerValue);
                if (activityRunning && (Acclist.get(1) > currentThreshold) && (timestamp - previousTime) > 500
                        && (Acclist.get(1) > max(Acclist.get(0), Acclist.get(2)))) {
                    count=++count_num;
                    previousTime = timestamp;
                }

                Acclist.remove(0);
            }
        }
    }

    public double max(double v1, double v2) {
        if (v1 < v2) return v2;
        else return v1;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
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
    private BaseFilter filter;

    private Double getSensorValue(SensorEvent event) {
        acceleration = new float[3];
        filteredAcceleration = new float[3];

        System.arraycopy(event.values, 0, acceleration, 0, event.values.length);
        filteredAcceleration = filter.filter(acceleration);

        return Math.sqrt(filteredAcceleration[0] * filteredAcceleration[0] +
                filteredAcceleration[1] * filteredAcceleration[1] +
                filteredAcceleration[2] * filteredAcceleration[2]) - 9.8f;
    }

    private void init() {

        Acclist = new ArrayList<Double>();
        Acclist.add(0.4);
        Acclist.add(0.4);
        currentThreshold = 0.45f;

        RunningThreshold = 0.01;
        RunningAcc = new ArrayList<Double>();
        isStateRunning = false;

        //LPF
        filter = new LowPassFilter(); // LowPassFilter(), MeanFilter(), MedianFilter();
        filter.setTimeConstant(0.18f);
        //LPF
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
}
