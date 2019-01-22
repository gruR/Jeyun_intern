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

import java.util.List;

public class MainActivity extends AppCompatActivity {

    WifiManager wifiManager;
    List<ScanResult> mScanResult;
    MapView map;
    StepCount step;

    //MODE
    public enum MODE_STATE {
        Calculate, Calibration, Initialization, WaitingMove, DR
    }

    MapView mapViewFragment;

    int[] pos_arr;
    double[] in_mag_arr;

    //Variable
    int direction_frombt = 0; // 0 : UP, 1 : RIGHT, 2 : DOWN, 3 : LEFT
    Activity thisActivity = this;

    public CalClass calClass;
    SensorManager sensorManager;
    float[] rota = new float[9];
    public float[] result_data = new float[3];
    public float[] mag_data = new float[3];
    public float[] acc_data = new float[3];
    public static Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (Build.VERSION.SDK_INT >= 23 && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 1001);
        }

        in_mag_arr = new double[110];
        pos_arr = new int[3];

        step = new StepCount(sensorManager, (MapView) getSupportFragmentManager().findFragmentById(R.id.fragMapView));
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
        step.onPause();
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        sensorManager.unregisterListener(mSensorListener);
    }

    @Override
    protected void onResume() {
        super.onResume();
        final IntentFilter filter = new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        registerReceiver(WifiScanReceiver, filter);
        wifiManager.startScan();
        Sensor sensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        int delay = SensorManager.SENSOR_DELAY_UI;
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        sensorManager.registerListener(mSensorListener, sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), delay);
        sensorManager.registerListener(mSensorListener, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), delay);

        if (sensor != null) {
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI);
        } else {
            Toast.makeText(this, "Magnetic sensor not available!", Toast.LENGTH_LONG).show();
        }

        step.onResume();
    }

    SensorEventListener mSensorListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            if (event.accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE)
                return;

            float[] v = event.values;

            switch (event.sensor.getType()) {
                case Sensor.TYPE_MAGNETIC_FIELD:
                    mag_data = event.values.clone();
                    break;
                case Sensor.TYPE_ACCELEROMETER:
                    acc_data = event.values.clone();
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


    class CustomTask extends AsyncTask<Context, Void, CalClass> {
        @Override
        protected CalClass doInBackground(Context... contexts) {
            return new CalClass(contexts[0]);
        }
    }
}