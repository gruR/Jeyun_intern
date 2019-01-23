package com.example.jeyun.main_map;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import com.kircherelectronics.fsensor.filter.BaseFilter;
import com.kircherelectronics.fsensor.filter.averaging.LowPassFilter;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    WifiManager wifiManager;
    List<ScanResult> mScanResult;
    MapView map;
    StepCount step;
    TextView example;
    //MODE
    public enum MODE_STATE {
        Calculate, Calibration, Initialization, WaitingMove, DR
    }

    MapView mapViewFragment;

    int[] pos_arr;
    double[] in_mag_arr;

    //Variable
    //int direction_frombt = 0; // 0 : UP, 1 : RIGHT, 2 : DOWN, 3 : LEFT
    Activity thisActivity = this;

    public CalClass calClass;
    SensorManager sensorManager;
    //Sensor stepDetect;
    float[] rota = new float[9];
    public float[] result_data = new float[3];
    public float[] mag_data = new float[3];
    public float[] acc_data = new float[3];
    public int step_cnt=0;
    public static Context context;
    boolean activityRunning;
    public int count;
    int num;
    int count_num;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (Build.VERSION.SDK_INT >= 23 && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 1001);
        }
        example = (TextView)findViewById(R.id.textView_temp);
        in_mag_arr = new double[110];
        pos_arr = new int[3];

        num = 0;
        count_num = 0;

        mapViewFragment = (MapView) getSupportFragmentManager().findFragmentById(R.id.fragMapView);

        q_mag_x = new float[QSIZE];
        q_mag_z = new float[QSIZE];
        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wifiManager != null) {
            if (!wifiManager.isWifiEnabled()) {
                wifiManager.setWifiEnabled(true);
            }
            final IntentFilter filter = new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
            filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
            registerReceiver(WifiScanReceiver, filter);
            wifiManager.startScan();
        }


        CustomTask openDB = new CustomTask();
        try {
            calClass = openDB.execute(this).get();
        } catch (Exception e) {
            e.printStackTrace();
        }

        context = this;
        init();

    }

    private BroadcastReceiver WifiScanReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (action != null) {
                if (action.equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {
                    mScanResult = wifiManager.getScanResults();
                    wifiManager.startScan();
                } else if (action.equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
                    context.sendBroadcast(new Intent("wifi.ON_NETWORK_STATE_CHANGED"));
                }
            }
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(WifiScanReceiver);
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        sensorManager.unregisterListener(mSensorListener);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(WifiScanReceiver);
        //step.onPause();
        activityRunning = false;

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        sensorManager.unregisterListener(mSensorListener);
    }

    @Override
    protected void onResume() {
        super.onResume();
        activityRunning = true;

        final IntentFilter filter = new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        registerReceiver(WifiScanReceiver, filter);
        wifiManager.startScan();
        //Sensor sensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        int delay = SensorManager.SENSOR_DELAY_UI;
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        sensorManager.registerListener(mSensorListener, sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), delay);
        sensorManager.registerListener(mSensorListener, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), delay);
        sensorManager.registerListener(mSensorListener, sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR), delay);
//        step.onResume();
    }

    double previousTime = 0;
    double previousRunningTime = 0;

    double RunningThreshold;
    private float currentThreshold;

    ArrayList<Double> Acclist;
    ArrayList<Double> RunningAcc;

    private boolean isStateRunning;

    SensorEventListener mSensorListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            double accelerometerValue = getSensorValue(event);
            double timestamp = getTimestamp(event);

            if (event.accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE)
                return;

            float[] v = event.values;

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

            switch (event.sensor.getType()) {
                case Sensor.TYPE_MAGNETIC_FIELD:
                    mag_data = event.values.clone();
                    break;
                case Sensor.TYPE_ACCELEROMETER:
                    acc_data = event.values.clone();
                    example.setText(""+count);

                    break;

            }

            if (mag_data != null && acc_data != null) {
                SensorManager.getRotationMatrix(rota, null, acc_data, mag_data);
                SensorManager.getOrientation(rota, result_data);
                result_data[0] = (float) Math.toDegrees(result_data[0]);
                result_data[1] = (float) Math.toDegrees(result_data[1]);
                result_data[2] = (float) Math.toDegrees(result_data[2]);

                if (result_data[0] < 0) // 현재 폰이 보고있는 방향
                    result_data[0] += 360;
            }


        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    };

    @Override
    public void onBackPressed() {
        mapViewFragment.moveMap.clearExpectedCircle();
        AlertDialog.Builder alert_exit = new AlertDialog.Builder(this);
        alert_exit.setTitle("종료확인");
        alert_exit.setMessage("종료하시겠습니까?");
        alert_exit.setNegativeButton("취소", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
            }
        });
        alert_exit.setPositiveButton("종료", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                ActivityCompat.finishAffinity(thisActivity);
                System.runFinalizersOnExit(true);
                System.exit(0);
            }
        });
        AlertDialog alert = alert_exit.create();
        alert.show();
    }

    private final int QSIZE = 30;   // Qsize. calibration 시간에 영향 + cal 시 처음 x개는 흘린다 *
    float[] q_mag_x; //초기값 측정용 moving average 구하기
    float[] q_mag_z;

    public double max(double v1, double v2) {
        if (v1 < v2) return v2;
        else return v1;
    }


    private long getTimestamp(SensorEvent event) {
        return (new Date()).getTime() + (event.timestamp - System.nanoTime()) / 1000000L;
    }

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

    class CustomTask extends AsyncTask<Context, Void, CalClass> {
        @Override
        protected CalClass doInBackground(Context... contexts) {
            return new CalClass(contexts[0]);
        }
    }
}