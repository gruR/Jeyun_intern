package com.example.jw.magnetic;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
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
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DecimalFormat;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    DBHelper dbHelper;
    SQLiteDatabase db;
    TextView magX, magY, magZ, angle, result,wifi;
    Button measure, save;

    static boolean flag = false, done = true;
    WifiManager wifiManager;
    WifiReciver reciver;
    List<ScanResult> ScanResult;
    MagClass magClass[] = new MagClass[361];
    SensorManager sensorManager;

    private double prevTime = 0, prevAngle = 0;
    private static final float NS2S = 1.0f / 1000000000.0f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if(Build.VERSION.SDK_INT>=23 &&ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_COARSE_LOCATION)!=PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(this,new String[] {Manifest.permission.ACCESS_COARSE_LOCATION},1001);
        }
        dbHelper = new DBHelper(this);
        db = dbHelper.getWritableDatabase();

        magX = (TextView) findViewById(R.id.magX);
        magY = (TextView) findViewById(R.id.magY);
        magZ = (TextView) findViewById(R.id.magZ);
        angle = (TextView) findViewById(R.id.angle);
        result = (TextView) findViewById(R.id.result);
        wifi = (TextView)findViewById(R.id.wifi);
        measure = (Button) findViewById(R.id.btnMeasure);
        save = (Button) findViewById(R.id.btnSave);
        wifiManager = (WifiManager)getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        reciver = new WifiReciver();
        registerReceiver(reciver,new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(reciver);
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        sensorManager.unregisterListener(mSensorListener);
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(reciver,new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        int delay = SensorManager.SENSOR_DELAY_UI;
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        sensorManager.registerListener(mSensorListener, sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), delay);
        sensorManager.registerListener(mSensorListener, sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE), delay);
    }


    SensorEventListener mSensorListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            if (event.accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE)
                return;

            float[] v = event.values;

            switch (event.sensor.getType()) {
                case Sensor.TYPE_MAGNETIC_FIELD:
                    magX.setText("" + v[0]);
                    magY.setText("" + v[1]);
                    magZ.setText("" + v[2]);

                    if (flag == true) {
                        Log.i("magnetic class", (Integer.parseInt(angle.getText().toString()) + 180) + " " + magClass[Integer.parseInt(angle.getText().toString()) + 180].getSize());
                        if (magClass[Integer.parseInt(angle.getText().toString()) + 180].getSize() < 10)
                            magClass[Integer.parseInt(angle.getText().toString()) + 180].addValue(v[0], v[1], v[2]);
                    }
                    break;
                case Sensor.TYPE_GYROSCOPE:

                    if (prevTime != 0) {
                        float dt = (float) (event.timestamp - prevTime) * NS2S;
                        double temp = v[2] * dt;

                        if (Math.abs(Math.toDegrees(temp)) < 7d && Math.abs(Math.toDegrees(temp)) > 1d)
                            prevAngle += temp;

                    }
                    angle.setText(""+(int)Math.toDegrees(prevAngle));
                    prevTime = event.timestamp;
                    break;

            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    };

    class CustomTask extends AsyncTask<String, Void, Void> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(String... strings) {
            if (strings[0].equals("measure")) {

                for (int i = 0; i < 361; i++)
                    magClass[i] = new MagClass();
                while (!done) {
                    int i, count = 0;
                    for (i = 0; i < 360; i++) {
                        if (magClass[i].getSize() == 10 && !magClass[i].isPrinted()) {
                            magClass[i].setPrinted(true);
                            count++;
                            String temp = String.valueOf((int) Math.ceil(count / 360));
                            result.setText(temp);
                        } else if (magClass[i].getSize() < 10)
                            continue;
                    }
                    if (count == 360)
                        done = true;
                }
                flag = false;
            } else if (strings[0].equals("save")) {
//                for (int i = 0; i < 360; i++) {
//                    String sql = "INSERT INTO mValue(magX, magY, magZ) VALUES (" + magClass[i].getValue()[0] + ", " + magClass[i].getValue()[1] + ", " + magClass[i].getValue()[2] + ");";
//                    db.execSQL(sql);
//                }
//                done = false;

                String sql = "INSERT INTO mValue(magX, magY, magZ) VALUES (" + magX.getText().toString() + ", " + magY.getText().toString() + ", " + magZ.getText().toString() + ");";
                db.execSQL(sql);

                Log.i("sqlQuery", sql);
            }
            return null;
        }
    }

    public void onClickButton(View v) {
        switch (v.getId()) {
            case R.id.btnMeasure:
                CustomTask measureTask = new CustomTask();
                flag = true;
                done = false;
                result.setText("측정 중");
                wifiManager.startScan();
                wifi.setText("Start");
                measureTask.execute("measure");
                break;
            case R.id.btnSave:
                unregisterReceiver(reciver);
               // if (done) {
                    CustomTask sqlTask = new CustomTask();
                    sqlTask.execute("save");
//                } else {
//                    Toast.makeText(this, "측정을 해야합니다", Toast.LENGTH_SHORT).show();
//                }

                break;
        }
    }
    class WifiReciver extends BroadcastReceiver
    {
        public void onReceive(Context c, Intent intnet)
        {
            ScanResult=wifiManager.getScanResults();
            wifi.setText("Current Wifi\n");
            for(int i = 0;i<8;i++)
            {
                double exp = (27.55 - (20 * Math.log10(ScanResult.get(i).frequency)) + Math.abs(ScanResult.get(i).level)) / 20.0;
                double dis = (Math.pow(10.0, exp) * 100.0) / 100.0;
                DecimalFormat df = new DecimalFormat("###.##");
                wifi.append((i+1)+" .SSID : "+(ScanResult.get(i)).SSID+ " RRSI : "+ (ScanResult.get(i)).level+" distance : "+ df.format(dis)+"\n");
            }
        }

    }
}
