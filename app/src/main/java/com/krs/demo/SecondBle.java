package com.krs.demo;

import static android.content.Context.BIND_AUTO_CREATE;

import static com.krs.demo.MainActivity.isScanClick;
import static com.krs.demo.MainActivity.isScanClick1;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
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
import android.content.pm.PackageManager;
import android.location.LocationManager;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class SecondBle {

    Activity mActivity;
    private static final String TAG = "SecondActivity";
    private BluetoothDevice bluetoothDevice;
    private BluetoothLEService1 mBluetoothLEService1;
    private BluetoothGattCharacteristic mNotifyCharacteristic;
    private BluetoothLeScanner bluetoothLeScanner;

    SecondBle(Activity activity){
        mActivity=activity;
        BluetoothAdapter mBluetoothAdapter = BluetoothUtils.getBluetoothAdapter(mActivity);
        if(mBluetoothAdapter==null){
            Toast.makeText(mActivity, "Bluetooth not supported!", Toast.LENGTH_SHORT).show();
            return;
        }
        bluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();

        mActivity.registerReceiver(mGattUpdateReceiver1, GattUpdateIntentFilter());
        mActivity.registerReceiver(mReceiver1, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
        mActivity.registerReceiver(mReceiver1, new IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION));

        MainActivity.txt_gross_wt1.setText("0.0");
    }
// 336305000062

    private final BroadcastReceiver mReceiver1 = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                if (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1) == BluetoothAdapter.STATE_OFF) {
                    alert();
                }
            }
        }
    };

    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLEService1 = ((BluetoothLEService1.LocalBinder) service).getService();
            if (!mBluetoothLEService1.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                mActivity.finish();
            }
            MainActivity.btnScan1.setText("Connecting device...");
            MainActivity.btnTare1.setEnabled(true);
            MainActivity.btnMode1.setEnabled(true);
            MainActivity.btnInc1.setEnabled(true);
            MainActivity.btnShift1.setEnabled(true);
            Log.e(TAG, "Mac1 "+bluetoothDevice.getAddress());
            mBluetoothLEService1.connect(bluetoothDevice.getAddress());
            startScanning(false);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLEService1 = null;
            MainActivity.btnTare1.setEnabled(false);
            MainActivity.btnMode1.setEnabled(false);
            MainActivity.btnInc1.setEnabled(false);
            MainActivity.btnShift1.setEnabled(false);
        }
    };

    private final BroadcastReceiver mGattUpdateReceiver1 = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(isScanClick==false && isScanClick1==true){
                final String action = intent.getAction();
                if (BluetoothLEService1.ACTION_GATT_CONNECTED.equals(action)) {
                    updateConnectionState(mActivity.getString(R.string.connected));
                    mActivity.invalidateOptionsMenu();
                } else if (BluetoothLEService1.ACTION_GATT_DISCONNECTED.equals(action)) {
                    updateConnectionState(mActivity.getString(R.string.scan));
                } else if (BluetoothLEService1.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                    displayGattServices(mBluetoothLEService1.getSupportedGattServices());
                } else if (BluetoothLEService1.ACTION_DATA_AVAILABLE.equals(action)) {
                    byte[] dataInput = mNotifyCharacteristic.getValue();
                    displayData(dataInput);

                }
            }
        }
    };


    private final ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);

                bluetoothDevice = result.getDevice();

                Intent gattServiceIntent = new Intent(mActivity, BluetoothLEService1.class);
                mActivity.bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);


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

        intentFilter.addAction(BluetoothLEService1.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLEService1.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLEService1.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLEService1.ACTION_DATA_AVAILABLE);
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
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                MainActivity.btnScan1.setText(status);
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
                MainActivity.txt_gross_wt1.setText(output);
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
                            mBluetoothLEService1.setCharacteristicNotification(mNotifyCharacteristic, true);
                        }
                    }
                    return;
                }
            }
        }
    }

    void alert(String message, final String state) {
        new AlertDialog.Builder(mActivity)
                //set icon
                .setIcon(R.mipmap.ic_launcher)
                //set title
                .setTitle(mActivity.getResources().getString(R.string.app_name))
                //set message
                .setMessage(message)
                //set positive button
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                       if (state.equalsIgnoreCase(mActivity.getString(R.string.scan))) {
                            try {
                                if(bluetoothDevice!=null){
                                    mBluetoothLEService1.connect(bluetoothDevice.getAddress());
                                }else{
                                    startScanning(true);
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }else if(state.equalsIgnoreCase(mActivity.getString(R.string.connected))){
                            mBluetoothLEService1.disconnect();
                            mBluetoothLEService1.close();
                            MainActivity.btnScan1.setText(mActivity.getString(R.string.scan));
                            MainActivity.txt_gross_wt1.setText("0.0");
                            Toast.makeText(mActivity, "disconnected", Toast.LENGTH_SHORT).show();
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

    public void alert() {
        new AlertDialog.Builder(mActivity)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle(mActivity.getResources().getString(R.string.app_name))
                .setMessage("Please turn on Bluetooth")
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        setBluetooth(true);
                    }
                }).show();
    }

    public void onUnregister() {
        mActivity.unregisterReceiver(mReceiver1);
        mActivity.unregisterReceiver(mGattUpdateReceiver1);
    }

    private void startScanning(final boolean enable) {

        Handler mHandler = new Handler();
        if (enable) {
            List<ScanFilter> scanFilters = new ArrayList<>();
            final ScanSettings settings = new ScanSettings.Builder().build();
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {

                    if (!MainActivity.btnScan1.getText().toString().equalsIgnoreCase(mActivity.getString(R.string.connected))) {
                        MainActivity.btnScan1.setText(mActivity.getString(R.string.scan));
                    }
                    if (bluetoothLeScanner != null) {
                        bluetoothLeScanner.stopScan(scanCallback);
                    }
                }
            }, Constants.SCAN_PERIOD);


            MainActivity.btnScan1.setText("Scanning...");
            if (bluetoothLeScanner != null) {
                bluetoothLeScanner.startScan(scanFilters, settings, scanCallback);
            }

        } else {

            mHandler.post(new Runnable() {
                @Override
                public void run() {

                    MainActivity.btnScan1.setText(mActivity.getString(R.string.scan));
                    if (bluetoothLeScanner != null) {
                        bluetoothLeScanner.stopScan(scanCallback);
                    }
                }
            });

        }
    }
}
