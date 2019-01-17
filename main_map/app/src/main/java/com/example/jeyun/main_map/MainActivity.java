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
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import java.util.List;

public class MainActivity extends AppCompatActivity {

        WifiManager wifiManager;
        List<ScanResult> mScanResult;
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

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_main);
            if (Build.VERSION.SDK_INT >= 23 && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 1001);
            }

            in_mag_arr = new double[110];
            pos_arr = new int[3];

            mapViewFragment = (MapView) getSupportFragmentManager().findFragmentById(R.id.fragMapView);

            q_mag_x = new float[QSIZE];
            q_mag_z = new float[QSIZE];
            wifiManager = (WifiManager)getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            if(wifiManager!=null)
            {
                if(!wifiManager.isWifiEnabled())
                {
                    wifiManager.setWifiEnabled(true);
                }
                final IntentFilter filter = new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
                filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
                registerReceiver(WifiScanReceiver,filter);
                wifiManager.startScan();
            }
        }
        private BroadcastReceiver WifiScanReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                final String action = intent.getAction();
                if(action!=null)
                {
                    if(action.equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION))
                    {
                        mScanResult = wifiManager.getScanResults();
                        wifiManager.startScan();
                    }
                    else if(action.equals(WifiManager.NETWORK_STATE_CHANGED_ACTION))
                    {
                        context.sendBroadcast(new Intent("wifi.ON_NETWORK_STATE_CHANGED"));
                    }
                }
            }
        };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(WifiScanReceiver);

    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(WifiScanReceiver);
    }

    @Override
    protected void onResume() {
        super.onResume();
        final IntentFilter filter = new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        registerReceiver(WifiScanReceiver,filter);
        wifiManager.startScan();
    }

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

        public int getCurrentDirection() {
            return direction_frombt;
        }

    /*입력한 각도로 마커의 방향을 변경한다
    public void changeDirection(int degree) {
        GShandler.clearTotalOmega();
        mapViewFragment.changeDirecton((4 - direction_frombt) * 90 + degree);
        direction_frombt = degree/90;
    }*/
}
