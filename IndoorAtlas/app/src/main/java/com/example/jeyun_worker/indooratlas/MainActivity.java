package com.example.jeyun_worker.indooratlas;

import android.Manifest;
import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;

import com.example.jeyun_worker.indooratlas.Simple.SimpleActivity;
import com.example.jeyun_worker.indooratlas.Wayfind.WayActivity;
import com.example.jeyun_worker.indooratlas.regions.RegionsActivity;
import com.switcher.AutoSwitchView;
import com.switcher.builder.CarouselStrategyBuilder;
import com.switcher.builder.DirectionMode;

public class MainActivity extends AppCompatActivity {
    private ImageView simple, way, setting;
    private AutoSwitchView as1, as2;

    private final int CODE_PERMISSIONS = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        String[] neededPermissions = {
                Manifest.permission.CHANGE_WIFI_STATE,
                Manifest.permission.ACCESS_WIFI_STATE,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN
        };
        ActivityCompat.requestPermissions( this, neededPermissions, CODE_PERMISSIONS );
        simple = (ImageView)findViewById(R.id.simple);
        way = (ImageView)findViewById(R.id.way);
        setting = (ImageView)findViewById(R.id.setting);

//        as1 = (AutoSwitchView)findViewById(R.id.as1);
//        as2 = (AutoSwitchView)findViewById(R.id.as2);
//
//        as1.setSwitchStrategy(new CarouselStrategyBuilder().
//                setAnimDuration(900).
//                setInterpolator(new AccelerateDecelerateInterpolator()).
//                setMode(DirectionMode.left2Right).
//                build());
//        as2.setSwitchStrategy(new CarouselStrategyBuilder().
//                setAnimDuration(900).
//                setInterpolator(new AccelerateDecelerateInterpolator()).
//                setMode(DirectionMode.left2Right).
//                build());

//        as1.startSwitcher();
//        as2.startSwitcher();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

    }

    @Override
    protected void onPause() {
        super.onPause();
//        as1.stopSwitcher();
//        as2.stopSwitcher();
    }

    @Override
    protected void onResume() {
        super.onResume();
//        as1.startSwitcher();
//        as2.startSwitcher();
    }

    public void onClickActivity(View v){
        Intent intent;
        switch (v.getId())
        {
            case R.id.simple:
                intent = new Intent(MainActivity.this,SimpleActivity.class);
                startActivity(intent);
                break;
            case R.id.way:
                intent = new Intent(MainActivity.this,WayActivity.class);
                startActivity(intent);
                break;
            case R.id.setting:
                intent = new Intent(MainActivity.this,RegionsActivity.class);
                startActivity(intent);
                break;
        }
    }
}
