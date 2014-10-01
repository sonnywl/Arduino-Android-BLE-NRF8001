
package edu.uci.fallsensor.service;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothAdapter.LeScanCallback;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.ble.sensors.NRF8001;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class BLEManager implements LeScanCallback {
    private static BLEManager manager = null;
    public static final String TAG = BLEManager.class.getSimpleName();

    //@formatter:off
    public static final int NUM_CONNECTION = 4;
    public static final byte[] ENABLE = new byte[]{1};
    public static final byte[] DISABLE = new byte[]{0};
    public static final UUID CCCD = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    //@formatter:on

    private final Context mContext;
    private int duration = 0;
    private BLETask currentRunningTask;
    private final BLEScanHandler mHandler;
    private BLEManagerCallback mCallback;
    private final BluetoothAdapter mAdapter;
    /** Actively using Bluetooth Gatt connection */
    private BluetoothGatt mCurrentBluetoothGatt = null;
    private BluetoothMetaData mBluetoothMetaData;
    private final HashSet<BluetoothDevice> mBluetoothDevices;
    private final HashMap<BluetoothDevice, BluetoothGatt> deviceConnectionMap;
    private final HashSet<BluetoothDevice> emptySet = new HashSet<BluetoothDevice>();

    public static BLEManager getInstance(Context context, BluetoothAdapter adapter,
            BLEManagerCallback callback) {
        if (manager == null) {
            manager = new BLEManager(context, adapter, callback);
            Log.i(TAG, "Creating new instance " + manager.hashCode());
        }
        Log.i(TAG, "Returning instance " + manager.hashCode());
        return manager;
    }

    private BLEManager(Context context, BluetoothAdapter adapter, BLEManagerCallback callback) {
        mAdapter = adapter;
        mContext = context;
        mCallback = callback;
        mHandler = new BLEScanHandler();
        mBluetoothDevices = new HashSet<BluetoothDevice>(NUM_CONNECTION);
        deviceConnectionMap = new HashMap<BluetoothDevice, BluetoothGatt>();
        mBluetoothMetaData = BluetoothMetaData.getInstance();
    }

    public void scanDevices(int duration) {
        this.duration = duration;
        if (mAdapter.isDiscovering()) {
            mAdapter.cancelDiscovery();
        }
        mHandler.sendEmptyMessage(BLEScanHandler.SCAN);
        mAdapter.startLeScan(this);
        mBluetoothDevices.clear();
    }

    /**
     * Returns all devices discovered and that are bonded;
     */
    public void stopScanDevices() {
        Log.i(TAG, "Stopping Scan");
        mAdapter.stopLeScan(this);
        Set<BluetoothDevice> devices = new HashSet<BluetoothDevice>();
        // Adding Active Connected Devices
        for (BluetoothDevice dev : deviceConnectionMap.keySet()) {
            devices.add(dev);
        }
        // Adding Discovered Devices
        for (BluetoothDevice dev : mBluetoothDevices) {
            devices.add(dev);
        }
        mCallback.devicesDiscovered(
                devices.toArray(new BluetoothDevice[] {}));
    }

    public boolean connect(final BluetoothDevice bluetoothDevice) {
        if (mAdapter == null || bluetoothDevice == null) {
            return false;
        }
        if (deviceConnectionMap.size() <= NUM_CONNECTION) {
            BluetoothGatt gatt = bluetoothDevice.connectGatt(mContext, true, mBleCallback);
            mCurrentBluetoothGatt = gatt;
            deviceConnectionMap.put(bluetoothDevice, gatt);
            return true;
        }
        return false;
    }

    public void disconnect(final BluetoothDevice bluetoothDevice) {
        if (deviceConnectionMap.containsKey(bluetoothDevice)) {
            BluetoothGatt gatt = deviceConnectionMap.get(bluetoothDevice);
            if (gatt.equals(mCurrentBluetoothGatt)) {
                mCurrentBluetoothGatt = null;
            }
            gatt.disconnect();
            gatt.close();

            deviceConnectionMap.remove(bluetoothDevice);
        }
    }

    public Set<BluetoothDevice> getBondedDevices() {
        if (mAdapter != null) {
            return mAdapter.getBondedDevices();
        }
        return emptySet;
    }

    public void disconnectAll() {
        deviceConnectionMap.clear();
    }

    public HashMap<BluetoothDevice, BluetoothGatt> getMapping() {
        return deviceConnectionMap;
    }

    public BluetoothGatt getCurrentBluetoothGatt() {
        return mCurrentBluetoothGatt;
    }

    @Override
    public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
        if (!mBluetoothDevices.contains(device)) {
            mBluetoothDevices.add(device);
            mBluetoothMetaData.putDevice(device, rssi);
        }
    }

    public void runTask(final BLETask task) {
        currentRunningTask = task;
        new Thread(new Runnable() {
            public void run() {
                switch (task.state) {
                    case CHARACTERISTIC_NOTIFICATION_WRITE:
                        setCharacteristicNotification(deviceConnectionMap.get(task.device),
                                task.serviceUuid, task.charaUuid, task.msgOrCmd);
                        break;
                    case CHARACTERISTIC_WRITE:
                        setCharacteristic(deviceConnectionMap.get(task.device), task.serviceUuid,
                                task.charaUuid, task.msgOrCmd);
                        break;
                    case DESCRIPTOR_WRITE:
                        break;
                    case DEVICE_CONNECT:
                        connect(task.device);
                        break;
                    case DEVICE_DISCONNECT:
                        disconnect(task.device);
                        break;
                    default:
                }
            }
        }) {
        }.start();
    }

    public boolean setCharacteristic(BluetoothGatt gatt, UUID serviceUuid, UUID charaUuid,
            byte[] values) {
        BluetoothGattService service = gatt.getService(serviceUuid);
        BluetoothGattCharacteristic chara = service.getCharacteristic(charaUuid);
        chara.setValue(values);
        return gatt.writeCharacteristic(chara);
    }

    public void setCharacteristicNotification(BluetoothGatt gatt, UUID serviceUuid,
            UUID charaUuid, byte[] notificationEnable) {
        BluetoothGattService service = gatt.getService(serviceUuid);
        Log.i(TAG, (service != null) + ":" + (charaUuid != null));
        BluetoothGattCharacteristic chara = service.getCharacteristic(charaUuid);
        boolean enable = Arrays.equals(notificationEnable, ENABLE);
        if (enable) {
            chara.setValue(ENABLE);
        } else {
            chara.setValue(DISABLE);
        }
        gatt.writeCharacteristic(chara);

        // Local Notification
        BluetoothGattDescriptor descriptor = chara.getDescriptor(CCCD);
        mCurrentBluetoothGatt.setCharacteristicNotification(chara, enable);
        if (enable) {
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        } else {
            descriptor.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
        }
        mCurrentBluetoothGatt.writeDescriptor(descriptor);
    }

    public boolean sendCharacteristicData(byte[] msg, UUID serviceUuid, UUID destinationUuid) {
        if (msg.length < 20) {
            if (mCurrentBluetoothGatt != null) {
                BluetoothGattCharacteristic chara = mCurrentBluetoothGatt.getService(serviceUuid)
                        .getCharacteristic(destinationUuid);
                chara.setValue(msg);
                return mCurrentBluetoothGatt.writeCharacteristic(chara);
            }
        }
        return false;
    }

    private final BluetoothGattCallback mBleCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                gatt.discoverServices();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                gatt.close();
                gatt = null;
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            Log.i(TAG, "onServicesDiscovered");
            if (status == BluetoothGatt.GATT_SUCCESS) {
                mCallback.notifyCompleteTransaction(BLEState.DEVICE_SERVICE, currentRunningTask, gatt);
            } else {
                Log.i(TAG, "onServicesDiscovered Failed:" + status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                BluetoothGattCharacteristic characteristic,
                int status) {
            Log.i(TAG, "onCharacteristicRead");
            if (status == BluetoothGatt.GATT_SUCCESS) {
                mCallback.notifyCompleteTransaction(BLEState.CHARACTERISTIC_READ, currentRunningTask, gatt);
            } else {
                Log.i(TAG, "onCharacteristicRead Failed:" + status);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                BluetoothGattCharacteristic characteristic) {
            Log.i(TAG, "onCharacteristicChanged");
            if (characteristic.getUuid().equals(NRF8001.TX_CHARA)) {
                mCallback.notifyBytesReceived(characteristic.getValue(), gatt);
            } else {
                Log.i(TAG, "onCharacteristicChanged Failed:");
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt,
                BluetoothGattCharacteristic characteristic, int status) {
            Log.i(TAG, "onCharacteristicWrite");
            if (status == BluetoothGatt.GATT_SUCCESS) {
                mCallback.notifyCompleteTransaction(BLEState.CHARACTERISTIC_WRITE, currentRunningTask, gatt);
            } else {
                Log.i(TAG, "onCharacteristicWrite Failed:" + status);
            }
        };

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor,
                int status) {
            Log.i(TAG, "onDescriptorWrite");
            if (status == BluetoothGatt.GATT_SUCCESS) {
                mCallback.notifyCompleteTransaction(BLEState.DESCRIPTOR_WRITE, currentRunningTask, gatt);
            } else {
                Log.i(TAG, "onDescriptorWrite Failed:" + status);
            }
        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            Log.i(TAG, "onReadRemoteRssi");
            if (status == BluetoothGatt.GATT_SUCCESS) {
                mCallback.updateRssi(gatt, rssi, status);
            } else {
                Log.i(TAG, "onReadRemoteRssi Failed:" + status);
            }
        };
    };

    public interface BLEManagerCallback {
        /**
         * Notifying caller of characteristic/descriptor completed BLEState is
         * used for debugging purpose to understand transaction progress
         * @param currentRunningTask 
         */
        void notifyCompleteTransaction(BLEState transaction, BLETask currentRunningTask, BluetoothGatt gatt);

        /** Rssi Remote Query */
        void updateRssi(BluetoothGatt gatt, int rssi, int status);

        /** Message/Value received via BLE */
        void notifyBytesReceived(byte[] values, BluetoothGatt gatt);

        /** Discovered Bluetooth Devices */
        void devicesDiscovered(BluetoothDevice[] device);
    }

    class BLEScanHandler extends Handler {
        public static final int SCAN = 0;
        public static final int STOP = 1;

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case SCAN:
                    this.sendEmptyMessageDelayed(STOP, duration);
                    break;
                case STOP:
                    stopScanDevices();
                    break;
            }
            super.handleMessage(msg);
        }
    }
}
