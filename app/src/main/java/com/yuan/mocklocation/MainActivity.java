package com.yuan.mocklocation;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    AlertDialog mPermissionRequestDialog;
    TextView mLocationTipTextView;
    EditText mLongitudeEditText,mLatitudeEditText,mAltitudeEditText,mLocationInterval;
    private LocationManager locationManager;
    private List<String> allProviders;
    Button mStartButton;
    String [] permissions = new String[]{
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
    };
    private static final String TAG = MainActivity.class.getSimpleName();
    private ExecutorService mExecutorService = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable);
        thread.setName("locationThread");
        return thread;
    });
    private boolean mIsStoppedMock = true;


    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
    }

    private void initView() {
        mLocationTipTextView = findViewById(R.id.tv_location_permission_tip);
        mLocationTipTextView.setVisibility(View.GONE);
        mStartButton = findViewById(R.id.bt_start);
        mStartButton.setOnClickListener(this);
        mLongitudeEditText = findViewById(R.id.et_longitude);
        mLatitudeEditText = findViewById(R.id.et_latitude);
        mAltitudeEditText = findViewById(R.id.et_altitude);
        mLocationInterval = findViewById(R.id.et_location_interval);
        mLongitudeEditText.setText("30.543068");
        mLatitudeEditText.setText("104.067131");
        mAltitudeEditText.setText("30");
        mLocationInterval.setText("2000");
    }

    /**
     * 检查定位权限
     */
    @RequiresApi(api = Build.VERSION_CODES.M)
    private void checkMustPermission() {
        int checkSelfPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
        if (checkSelfPermission == PackageManager.PERMISSION_GRANTED){
            startMockLocation();
        }else if(shouldShowRequestPermissionRationale(Manifest.permission_group.LOCATION)){
            showPermissionDialog();
        }else {
           requestPermissions(permissions,0);
        }
    }

    /**
     * 展示定位权限说明
     */
    @RequiresApi(api = Build.VERSION_CODES.M)
    private void showPermissionDialog() {
        if (mPermissionRequestDialog == null){
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.tips);
            builder.setMessage(R.string.location_permission_request)
                    .setPositiveButton(R.string.confirm, (dialogInterface, i) -> {
                        requestPermissions(permissions,0);
                    })
                    .setNegativeButton(R.string.cancel, (dialogInterface, i) -> {
                        mLocationTipTextView.setVisibility(View.VISIBLE);
                    });
            mPermissionRequestDialog = builder.create();
        }
        if (!mPermissionRequestDialog.isShowing()){
            mPermissionRequestDialog.show();
        }
    }


    /**
     * 开始模拟位置；
     */
    private void startMockLocation(){
        registerTestLocationProvider();
        setIsStoppedMock(false);
        mExecutorService.execute(new SetLoactionTask());
    }

    /**
     * 注册模拟器；
     */
    private void registerTestLocationProvider() {
        if (locationManager== null){
            locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        }
        allProviders = locationManager.getAllProviders();
        for (String providerName: allProviders){
            LocationProvider provider = locationManager.getProvider(providerName);
            Log.d(TAG,providerName+ ": provider is null "  + (provider == null) );
            if ("passive".equals(providerName)) {
                continue;
            }
            try {
                locationManager.removeTestProvider(providerName);
            }catch (Exception e){
                e.printStackTrace();
            }

            if (provider != null ){
                locationManager.addTestProvider(provider.getName(),
                        provider.requiresNetwork(),
                        provider.requiresSatellite(),
                        provider.requiresCell(),
                        provider.hasMonetaryCost(),
                        provider.supportsAltitude(),
                        provider.supportsSpeed(),
                        provider.supportsBearing(),
                        provider.getPowerRequirement(),
                        provider.getAccuracy()
                );
                locationManager.setTestProviderEnabled(provider.getName(),true);
            }
        }
    }


    private void setLocation() {
        Location location;
        for ( String providerName : allProviders){
            if (providerName.equals("passive")){
                continue;
            }
            location = new Location(providerName);

            location.setLatitude(convertTextToDouble(mLongitudeEditText,30.543068));  // 维度（度）
            location.setLongitude(convertTextToDouble(mLatitudeEditText,104.067131)); // 经度（度）
            location.setAltitude(convertTextToDouble(mAltitudeEditText,30));  // 高程（米）
            location.setBearing(180);  // 方向（度）
            location.setSpeed(10);  //速度（米/秒）
            location.setAccuracy(0.1f);  // 精度（米）
            location.setTime(new Date().getTime());  // 本地时间
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN) {
                location.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());
            }
            locationManager.setTestProviderLocation(providerName,location);
        }
    }

    private double convertTextToDouble(EditText editText, double defaultValue) {
        if (editText == null || TextUtils.isEmpty(editText.getText())) {
            Log.d(TAG, "convertTextToDouble text is null");
            return defaultValue;
        }
        double temp = defaultValue;
        String str = editText.getText().toString().trim();
        Log.d(TAG, "str:" + str);
        try {
            temp = Double.parseDouble(str);
        } catch (NumberFormatException exception) {
            Log.d(TAG, "convertTextToDouble,id:" + editText.getId());
            temp = defaultValue;
        }
        Log.d(TAG, "finaly value is :" + temp);
        return temp;
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.bt_start:
                if (mIsStoppedMock){
                    checkMustPermission();
                }else {
                    setIsStoppedMock(true);
                }
                refreshStartButton();
                break;
        }
    }

    class SetLoactionTask implements Runnable{
        @Override
        public void run() {
            while (!mIsStoppedMock){
                setLocation();
                try {
                    Thread.sleep((long)convertTextToDouble(mLocationInterval,2000));
                } catch (InterruptedException e) {
                   Log.d(TAG,e.getMessage());
                }
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshStartButton();
    }

    private void refreshStartButton(){
        if (mIsStoppedMock){
            mStartButton.setText(R.string.start_mock);
        }else {
            mStartButton.setText(R.string.stop_mock);
        }
    }

    private void setIsStoppedMock(boolean isStoppedMock){
        mIsStoppedMock = isStoppedMock;
        refreshStartButton();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED){
            mLocationTipTextView.setVisibility(View.GONE);
            startMockLocation();
        }else{
            mLocationTipTextView.setVisibility(View.VISIBLE);
        }
    }
}