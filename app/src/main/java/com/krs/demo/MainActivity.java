package com.krs.demo;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.StrictMode;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextClock;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;

import org.json.JSONObject;

import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    final static int REQUEST_LOCATION = 199;
    private static final String TAG = "MainActivity";
    public TextView txt_title4 = null;
    private LocationRequest mLocationRequest;
    private GoogleApiClient mGoogleApiClient;
    private PendingResult<LocationSettingsResult> result;
    private BluetoothDevice bluetoothDevice;
    private Button btnScan, btnTare,btnMode,btnInc,btnShift;
    public static Button btnScan1,btnTare1,btnMode1,btnInc1,btnShift1;
    private BluetoothAdapter mBluetoothAdapter;
    private TextView txt_gross_wt = null;
    public static TextView txt_gross_wt1 = null;
    private TextClock textClock = null;
    private BluetoothLEService mBluetoothLEService;
    private BluetoothGattCharacteristic mNotifyCharacteristic;
    private BluetoothLeScanner bluetoothLeScanner;
    private SecondBle mSecondBle=null;
    public static boolean isScanClick=false;
    public static boolean isScanClick1=false;
    public final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                if (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1) == BluetoothAdapter.STATE_OFF) {
                    alert();
                }
            }
            if (LocationManager.PROVIDERS_CHANGED_ACTION.equals(action)) {
                mGoogleApiClient = new GoogleApiClient.Builder(MainActivity.this).addApi(LocationServices.API).addConnectionCallbacks(MainActivity.this).addOnConnectionFailedListener(MainActivity.this).build();
                mGoogleApiClient.connect();
            }
        }
    };

    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLEService = ((BluetoothLEService.LocalBinder) service).getService();
            if (!mBluetoothLEService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            btnScan.setText("Connecting device...");
            btnTare.setEnabled(true);
            btnMode.setEnabled(true);
            btnInc.setEnabled(true);
            btnShift.setEnabled(true);
            Log.e(TAG, "Mac "+bluetoothDevice.getAddress());
            mBluetoothLEService.connect(bluetoothDevice.getAddress());
            startScanning(false);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLEService = null;
            btnTare.setEnabled(false);
            btnMode.setEnabled(false);
            btnInc.setEnabled(false);
            btnShift.setEnabled(false);
        }
    };

    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(isScanClick==true && isScanClick1==false){
                final String action = intent.getAction();
                if (BluetoothLEService.ACTION_GATT_CONNECTED.equals(action)) {
                    updateConnectionState(getString(R.string.connected));
                    invalidateOptionsMenu();
                } else if (BluetoothLEService.ACTION_GATT_DISCONNECTED.equals(action)) {
                    updateConnectionState(getString(R.string.scan));
                } else if (BluetoothLEService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                    displayGattServices(mBluetoothLEService.getSupportedGattServices());
                } else if (BluetoothLEService.ACTION_DATA_AVAILABLE.equals(action)) {
                    byte[] dataInput = mNotifyCharacteristic.getValue();
                    displayData(dataInput);
                }
            }

        }
    };
//5029922563

    private final ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);

                bluetoothDevice = result.getDevice();

                Intent gattServiceIntent = new Intent(MainActivity.this, BluetoothLEService.class);
                bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

                Log.d(TAG,"address: "+bluetoothDevice.getAddress());
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
            Log.d(TAG, "Scanning Failed " + errorCode);
        }
    };

    private static IntentFilter GattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLEService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLEService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLEService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLEService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }

    public static void setBluetooth(boolean enable) {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        boolean isEnabled = bluetoothAdapter.isEnabled();
        if (enable && !isEnabled) {
            bluetoothAdapter.enable();
        } else if (!enable && isEnabled) {
            bluetoothAdapter.disable();

        }
        // No need to change bluetooth state
    }

    private void updateConnectionState(final String status) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                btnScan.setText(status);
            }
        });
    }

    private void displayData(byte[] data) {
        try {
            if (data != null) {
                String output = "";
                for(int i = 0; i < data.length; i++) {
                    output = output + (char) data[i];
                }
                txt_gross_wt.setText(output);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void displayGattServices(List<BluetoothGattService> gattServices) {
        if (gattServices == null) return;
        String uuid = null;
        String serviceString = "unknown service";
        String charaString = "unknown characteristic";

        for (BluetoothGattService gattService : gattServices) {

            uuid = gattService.getUuid().toString();
            serviceString = BluetoothUtils.lookup(uuid);

            if (serviceString != null) {
                List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();

                for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                    HashMap<String, String> currentCharaData = new HashMap<String, String>();
                    uuid = gattCharacteristic.getUuid().toString();
                    charaString = BluetoothUtils.lookup(uuid);
                    if (charaString != null) {
                        //  serviceName.setText(charaString);
                    }
                    mNotifyCharacteristic = gattCharacteristic;
                    if (mNotifyCharacteristic != null) {
                        final int charaProp = mNotifyCharacteristic.getProperties();
                        if ((charaProp | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
                            mBluetoothLEService.setCharacteristicNotification(mNotifyCharacteristic, true);
                        }
                    }
                    return;
                }
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        MemoryAllocation();
        btnScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(btnScan.getText().toString().equalsIgnoreCase(getString(R.string.scan))){
                    alert("Are you want to Scan?", getString(R.string.scan));
                }else if(btnScan.getText().toString().equalsIgnoreCase(getString(R.string.connected))){
                    alert("Are you want to DISCONNECT?", getString(R.string.connected));
                }
            }
        });
        btnTare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mNotifyCharacteristic != null) {
                    mBluetoothLEService.writeCharacteristic(mNotifyCharacteristic, "T");
                } else {
                    Toast.makeText(MainActivity.this, "Please connect again!", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnMode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mNotifyCharacteristic != null) {
                    mBluetoothLEService.writeCharacteristic(mNotifyCharacteristic, "M");
                } else {
                    Toast.makeText(MainActivity.this, "Please connect again!", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnInc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mNotifyCharacteristic != null) {
                    mBluetoothLEService.writeCharacteristic(mNotifyCharacteristic, "I");
                } else {
                    Toast.makeText(MainActivity.this, "Please connect again!", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnShift.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mNotifyCharacteristic != null) {
                    mBluetoothLEService.writeCharacteristic(mNotifyCharacteristic, "S");
                } else {
                    Toast.makeText(MainActivity.this, "Please connect again!", Toast.LENGTH_SHORT).show();
                }
            }
        });


        btnScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                isScanClick1=false;
                isScanClick=true;
                if(btnScan.getText().toString().equalsIgnoreCase(getString(R.string.scan))){
                    alert("Are you want to Scan?", getString(R.string.scan));
                }else if(btnScan.getText().toString().equalsIgnoreCase(getString(R.string.connected))){
                    alert("Are you want to DISCONNECT?", getString(R.string.connected));
                }
            }
        });

        btnScan1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                isScanClick=false;
                isScanClick1=true;
                mSecondBle=new SecondBle(MainActivity.this);
                if(btnScan1.getText().toString().equalsIgnoreCase(getString(R.string.scan))){
                    mSecondBle.alert("Are you want to Scan?", getString(R.string.scan));
                }else if(btnScan1.getText().toString().equalsIgnoreCase(getString(R.string.connected))){
                    mSecondBle.alert("Are you want to DISCONNECT?", getString(R.string.connected));
                }
            }
        });



        btnTare1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mNotifyCharacteristic != null) {
                    mBluetoothLEService.writeCharacteristic(mNotifyCharacteristic, "T");
                } else {
                    Toast.makeText(MainActivity.this, "Please connect again!", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnMode1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mNotifyCharacteristic != null) {
                    mBluetoothLEService.writeCharacteristic(mNotifyCharacteristic, "M");
                } else {
                    Toast.makeText(MainActivity.this, "Please connect again!", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnInc1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mNotifyCharacteristic != null) {
                    mBluetoothLEService.writeCharacteristic(mNotifyCharacteristic, "I");
                } else {
                    Toast.makeText(MainActivity.this, "Please connect again!", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnShift1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mNotifyCharacteristic != null) {
                    mBluetoothLEService.writeCharacteristic(mNotifyCharacteristic, "S");
                } else {
                    Toast.makeText(MainActivity.this, "Please connect again!", Toast.LENGTH_SHORT).show();
                }
            }
        });

        textClock.setFormat12Hour("dd/MM/yyyy hh:mm:ss a EEE");
        textClock.setFormat24Hour(null);

        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());



    }

    private void MemoryAllocation() {

        textClock = (TextClock) findViewById(R.id.textClock);

        txt_title4 = (TextView) findViewById(R.id.txt_title4);
        txt_gross_wt = (TextView) findViewById(R.id.txt_gross_wt);
        txt_gross_wt1= (TextView) findViewById(R.id.txt_gross_wt1);
        btnScan = (Button) findViewById(R.id.btnScan);
        btnTare = (Button) findViewById(R.id.btnTare);
        btnMode = (Button) findViewById(R.id.btnMode);
        btnInc = (Button) findViewById(R.id.btnInc);
        btnShift = (Button) findViewById(R.id.btnShift);

        btnScan1= (Button) findViewById(R.id.btnScan1);
        btnTare1 = (Button) findViewById(R.id.btnTare1);
        btnMode1 = (Button) findViewById(R.id.btnMode1);
        btnInc1 = (Button) findViewById(R.id.btnInc1);
        btnShift1 = (Button) findViewById(R.id.btnShift1);

        mBluetoothAdapter = BluetoothUtils.getBluetoothAdapter(MainActivity.this);
        if(mBluetoothAdapter==null){
            Toast.makeText(MainActivity.this, "Bluetooth not supported!", Toast.LENGTH_SHORT).show();
            return;
        }
        bluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();
    }

    public void alert(String message, final String state) {
         new android.app.AlertDialog.Builder(this)
                .setIcon(R.mipmap.ic_launcher)
                .setTitle(getResources().getString(R.string.app_name))
                .setMessage(message)
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                       if (state.equalsIgnoreCase(getString(R.string.scan))) {
                            try {
                                if(bluetoothDevice!=null){
                                    mBluetoothLEService.connect(bluetoothDevice.getAddress());
                                }else{
                                    startScanning(true);
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }else if(state.equalsIgnoreCase(getString(R.string.connected))){
                            mBluetoothLEService.disconnect();
                            mBluetoothLEService.close();
                            btnScan.setText(getString(R.string.scan));
                            txt_gross_wt.setText("0.0");
                            Toast.makeText(MainActivity.this, "disconnected", Toast.LENGTH_SHORT).show();
                        }

                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                })
                .show();
    }

    private void alert() {
        android.app.AlertDialog alertDialog = new android.app.AlertDialog.Builder(this)
                //set icon
                .setIcon(android.R.drawable.ic_dialog_alert)
                //set title
                .setTitle(getResources().getString(R.string.app_name))
                //set message
                .setMessage("Please turn on Bluetooth")
                //set positive button
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        setBluetooth(true);
                    }
                }).show();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if(mBluetoothAdapter==null){
            Toast.makeText(MainActivity.this, "Bluetooth not supported!", Toast.LENGTH_LONG).show();
            return;
        }

        if (ContextCompat.checkSelfPermission(this.getApplicationContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
        } else {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, Constants.REQUEST_LOCATION_ENABLE_CODE);
        }
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(), android.Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this.getApplicationContext(), android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {

        } else {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        }

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "Your devices that don't support BLE", Toast.LENGTH_LONG).show();
            finish();
        }
        if ( !mBluetoothAdapter.enable()) {
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(intent, Constants.REQUEST_BLUETOOTH_ENABLE_CODE);
        }

        if (mBluetoothLEService != null) {
            final boolean result = mBluetoothLEService.connect(bluetoothDevice.getAddress());
            Log.d(TAG, "Connect request result=" + result);
        }

        registerReceiver(mGattUpdateReceiver, GattUpdateIntentFilter());
        registerReceiver(mReceiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
        registerReceiver(mReceiver, new IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION));

        mGoogleApiClient = new GoogleApiClient.Builder(this).addApi(LocationServices.API).addConnectionCallbacks(this).addOnConnectionFailedListener(this).build();
        mGoogleApiClient.connect();
        txt_gross_wt.setText("0.0");
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 1: {

                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Toast.makeText(MainActivity.this, "Permission denied to read your External storage", Toast.LENGTH_SHORT).show();
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mReceiver);
        unregisterReceiver(mGattUpdateReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mSecondBle.onUnregister();
    }

    private void startScanning(final boolean enable) {

        Handler mHandler = new Handler();
        if (enable) {
            List<ScanFilter> scanFilters = new ArrayList<>();
            final ScanSettings settings = new ScanSettings.Builder().build();
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {

                    if (!btnScan.getText().toString().equalsIgnoreCase(getString(R.string.connected))) {
                        btnScan.setText(getString(R.string.scan));
                    }
                    if (bluetoothLeScanner != null) {
                        bluetoothLeScanner.stopScan(scanCallback);
                    }
                }
            }, Constants.SCAN_PERIOD);


            btnScan.setText("Scanning...");
            if (bluetoothLeScanner != null) {
                bluetoothLeScanner.startScan(scanFilters, settings, scanCallback);
            }

        } else {

            mHandler.post(new Runnable() {
                @Override
                public void run() {

                    btnScan.setText(getString(R.string.scan));
                    if (bluetoothLeScanner != null) {
                        bluetoothLeScanner.stopScan(scanCallback);
                    }
                }
            });

        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        mLocationRequest = LocationRequest.create();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(30 * 1000);
        mLocationRequest.setFastestInterval(5 * 1000);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder().addLocationRequest(mLocationRequest);
        builder.setAlwaysShow(true);

        result = LocationServices.SettingsApi.checkLocationSettings(mGoogleApiClient, builder.build());

        result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
            @Override
            public void onResult(LocationSettingsResult result) {
                final Status status = result.getStatus();
                //final LocationSettingsStates state = result.getLocationSettingsStates();
                switch (status.getStatusCode()) {
                    case LocationSettingsStatusCodes.SUCCESS:
                        // All location settings are satisfied. The client can initialize location
                        // requests here.
                        //...
                        break;
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                        // Location settings are not satisfied. But could be fixed by showing the user
                        // a dialog.
                        try {
                            // Show the dialog by calling startResolutionForResult(),
                            // and check the result in onActivityResult().
                            status.startResolutionForResult(MainActivity.this, REQUEST_LOCATION);
                        } catch (Exception e) {
                            // Ignore the error.
                        }
                        break;
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        // Location settings are not satisfied. However, we have no way to fix the
                        // settings so we won't show the dialog.
                        //...
                        break;
                }
            }
        });


    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        Log.d("onActivityResult()", Integer.toString(resultCode));

        //final LocationSettingsStates states = LocationSettingsStates.fromIntent(data);
        switch (requestCode) {
            case REQUEST_LOCATION:
                switch (resultCode) {
                    case Activity.RESULT_OK: {
                        // All required changes were successfully made
                        Toast.makeText(MainActivity.this, "Location enabled by user!", Toast.LENGTH_LONG).show();
                        break;
                    }
                    case Activity.RESULT_CANCELED: {
                        // The user was asked to change settings, but chose not to
                        Toast.makeText(MainActivity.this, "Location not enabled, user cancelled.", Toast.LENGTH_LONG).show();
                        break;
                    }
                    default: {
                        break;
                    }
                }
                break;
        }
    }

}
