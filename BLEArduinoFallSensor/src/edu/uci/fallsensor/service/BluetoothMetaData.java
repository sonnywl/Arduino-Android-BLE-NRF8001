
package edu.uci.fallsensor.service;

import android.bluetooth.BluetoothDevice;

import java.util.HashMap;

public class BluetoothMetaData {
    private static BluetoothMetaData instance;
    private HashMap<String, Integer> deviceRssi;

    public static BluetoothMetaData getInstance() {
        if (instance == null) {
            instance = new BluetoothMetaData();
        }
        return instance;
    }

    private BluetoothMetaData() {
        deviceRssi = new HashMap<String, Integer>();
    }

    public int getRssi(BluetoothDevice device) {
        return deviceRssi.get(device.getAddress()).intValue();
    }

    public void putDevice(BluetoothDevice device, int rssi) {
        deviceRssi.put(device.getAddress(), rssi);
    }
}
