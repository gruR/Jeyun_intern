package com.example.jeyun_worker.indooratlas.regions;


import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.Switch;
import android.widget.TextView;

import com.example.jeyun_worker.indooratlas.R;
import com.indooratlas.android.sdk.IALocation;
import com.indooratlas.android.sdk.IALocationListener;
import com.indooratlas.android.sdk.IALocationManager;
import com.indooratlas.android.sdk.IALocationRequest;
import com.indooratlas.android.sdk.IARegion;

public class RegionsActivity extends FragmentActivity implements IALocationListener,
        IARegion.Listener {

    Switch wifiSwitch, blueSwitch, locaSwitch;
    IALocationManager mManager;
    IARegion mCurrentVenue = null;
    IARegion mCurrentFloorPlan = null;
    Integer mCurrentFloorLevel = null;
    Float mCurrentCertainty = null;

    TextView mUiVenue;
    TextView mUiVenueId;
    TextView mUiFloorPlan;
    TextView mUiFloorPlanId;
    TextView mUiFloorLevel;
    TextView mUiFloorCertainty;

    WifiManager wifiManager;
    BluetoothManager bluetoothManager;
    BluetoothAdapter bluetoothAdapter;
    LocationManager locationManager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_regions);

        wifiSwitch = (Switch) findViewById(R.id.wifiSwitch);
        blueSwitch = (Switch) findViewById(R.id.blueSwitch);
        locaSwitch = (Switch) findViewById(R.id.locaSwitch);

        wifiManager = (WifiManager) this.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (!wifiManager.isWifiEnabled()) {
            wifiSwitch.setChecked(false);
        } else if (wifiManager.isWifiEnabled()) {
            wifiSwitch.setChecked(true);
        }

        bluetoothManager = (BluetoothManager)getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();
        if(bluetoothAdapter == null || !bluetoothAdapter.isEnabled())
            blueSwitch.setChecked(false);
        else if (bluetoothAdapter.getState() == BluetoothAdapter.STATE_TURNING_ON || bluetoothAdapter.getState() == BluetoothAdapter.STATE_ON)
            blueSwitch.setChecked(true);

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER))
            locaSwitch.setChecked(true);
        else
            locaSwitch.setChecked(false);

        mManager = IALocationManager.create(this);
        mManager.registerRegionListener(this);
        mManager.requestLocationUpdates(IALocationRequest.create(), this);

        mUiVenue = (TextView) findViewById(R.id.text_view_venue);
        mUiVenueId = (TextView) findViewById(R.id.text_view_venue_id);
        mUiFloorPlan = (TextView) findViewById(R.id.text_view_floor_plan);
        mUiFloorPlanId = (TextView) findViewById(R.id.text_view_floor_plan_id);
        mUiFloorLevel = (TextView) findViewById(R.id.text_view_floor_level);
        mUiFloorCertainty = (TextView) findViewById(R.id.text_view_floor_certainty);

        updateUi();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!wifiManager.isWifiEnabled()) {
            wifiSwitch.setChecked(false);
        } else if (wifiManager.isWifiEnabled()) {
            wifiSwitch.setChecked(true);
        }

        if(bluetoothAdapter == null || !bluetoothAdapter.isEnabled())
            blueSwitch.setChecked(false);
        else if (bluetoothAdapter.getState() == BluetoothAdapter.STATE_TURNING_ON || bluetoothAdapter.getState() == BluetoothAdapter.STATE_ON)
            blueSwitch.setChecked(true);

        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER))
            locaSwitch.setChecked(true);
        else
            locaSwitch.setChecked(false);
    }

    @Override
    protected void onDestroy() {
        mManager.destroy();
        super.onDestroy();
    }

    @Override
    public void onLocationChanged(IALocation iaLocation) {
        mCurrentFloorLevel = iaLocation.hasFloorLevel() ? iaLocation.getFloorLevel() : null;
        mCurrentCertainty = iaLocation.hasFloorCertainty() ? iaLocation.getFloorCertainty() : null;
        updateUi();
    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {
    }

    @Override
    public void onEnterRegion(IARegion iaRegion) {
        if (iaRegion.getType() == IARegion.TYPE_VENUE) {
            mCurrentVenue = iaRegion;
        } else if (iaRegion.getType() == IARegion.TYPE_FLOOR_PLAN) {
            mCurrentFloorPlan = iaRegion;
        }
        updateUi();
    }

    @Override
    public void onExitRegion(IARegion iaRegion) {
        if (iaRegion.getType() == IARegion.TYPE_VENUE) {
            mCurrentVenue = iaRegion;
        } else if (iaRegion.getType() == IARegion.TYPE_FLOOR_PLAN) {
            mCurrentFloorPlan = iaRegion;
        }
        updateUi();
    }

    void updateUi() {
        String venue = getString(R.string.venue_outside);
        String venueId = "";
        String floorPlan = "";
        String floorPlanId = "";
        String level = "";
        String certainty = "";
        if (mCurrentVenue != null) {
            venue = getString(R.string.venue_inside);
            venueId = mCurrentVenue.getId();
            if (mCurrentFloorPlan != null) {
                floorPlan = mCurrentFloorPlan.getName();
                floorPlanId = mCurrentFloorPlan.getId();
            } else {
                floorPlan = getString(R.string.floor_plan_outside);
            }
        }
        if (mCurrentFloorLevel != null) {
            level = mCurrentFloorLevel.toString();
        }
        if (mCurrentCertainty != null) {
            certainty = getString(R.string.floor_certainty_percentage, mCurrentCertainty * 100.0f);
        }
        setText(mUiVenue, venue, true);
        setText(mUiVenueId, venueId, true);
        setText(mUiFloorPlan, floorPlan, true);
        setText(mUiFloorPlanId, floorPlanId, true);
        setText(mUiFloorLevel, level, true);
        setText(mUiFloorCertainty, certainty, false); // do not animate as changes can be frequent
    }

    /**
     * Set the text of a TextView and make a animation to notify when the value has changed
     */
    void setText(@NonNull TextView view, @NonNull String text, boolean animateWhenChanged) {
        if (!view.getText().toString().equals(text)) {
            view.setText(text);
            if (animateWhenChanged) {
                view.startAnimation(AnimationUtils.loadAnimation(this, R.anim.notify_change));
            }
        }
    }

    public void onToggleSwitch(View v) {
        switch (v.getId()) {
            case R.id.wifiSwitch:
                if (!wifiManager.isWifiEnabled()) {
                    wifiManager.setWifiEnabled(true);
                } else if (wifiManager.isWifiEnabled()) {
                    wifiManager.setWifiEnabled(false);
                }
                break;
            case R.id.blueSwitch:
                if(bluetoothAdapter == null || !bluetoothAdapter.isEnabled())
                {
                    Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivity(intent);
                }
                else
                    bluetoothAdapter.disable();
                break;
            case R.id.locaSwitch:
               Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
               intent.addCategory(Intent.CATEGORY_DEFAULT);
               startActivity(intent);
                break;
        }
    }
}
