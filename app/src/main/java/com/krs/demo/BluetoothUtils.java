package com.krs.demo;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;

import java.util.HashMap;

public class BluetoothUtils {

    private static HashMap<String, String> attributes = new HashMap();
    public static final String UUID1 = "0000ffe0-0000-1000-8000-00805f9b34fb";
    public static final String UUID2 = "000000ff-0000-1000-8000-00805f9b34fb";
    public static final String UUID3 = "0000ffe1-0000-1000-8000-00805f9b34fb";

    public static final String DUUID = "00002902-0000-1000-8000-00805f9b34fb";

    public static long SCAN_PERIOD = 10000;
    public static final int REQUEST_BLUETOOTH_ENABLE_CODE = 101;
    public static final int REQUEST_LOCATION_ENABLE_CODE = 101;

    static {
        attributes.put(UUID1, "HM05");
        attributes.put(UUID2, "ESP32");
        attributes.put(UUID3, "HM05_1");
    }

    public static String lookup(String uuid) {
        String name = attributes.get(uuid);
        return name;
    }

    public static BluetoothAdapter getBluetoothAdapter(Context context) {
        BluetoothManager mBluetoothManager = (BluetoothManager) context.getSystemService(context.BLUETOOTH_SERVICE);
        return mBluetoothManager.getAdapter();
    }
}
