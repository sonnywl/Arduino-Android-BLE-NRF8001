
package edu.uci.fallsensor.service;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;

import com.ble.sensors.NRF8001;

import edu.uci.fallsensor.service.BLEManager.BLEManagerCallback;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

public class BLEController implements BLEManagerCallback {

    public static final String TAG = BLEController.class.getSimpleName();
    private static final int SCAN_INTERVAL = 800;
    private static BLEController service;

    private final BluetoothManager manager;
    private final PowerManager mPowerManager;

    private Context mContext;
    private boolean executingTask = false;
    private ArrayList<BLEServiceListener> mListeners;
    private ArrayList<BLETask> tasksQueue;
    private BLEManager mBLEManager;
    private BluetoothAdapter mAdapter;

    private WakeLock mWakeLock = null;

    public static BLEController getInstance(Context context) {
        if (service == null) {
            service = new BLEController(context);
        }
        return service;
    }

    private BLEController(Context context) {
        mContext = context;
        manager = (BluetoothManager) context
                .getSystemService(Context.BLUETOOTH_SERVICE);
        mPowerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        mWakeLock = mPowerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);

        tasksQueue = new ArrayList<BLETask>();
        mListeners = new ArrayList<BLEServiceListener>();

        if (checkBleHardwareAvailable()) {
            if (!isBtEnabled()) {
                mAdapter.enable(); // Or do the dialog request method
            }
            mBLEManager = BLEManager.getInstance(context, mAdapter, this);
        }
        Log.i(TAG, "On Create " + mBLEManager.hashCode());
    }

    /* run test and check if this device has BT and BLE hardware available */
    public boolean checkBleHardwareAvailable() {
        if (manager == null)
            return false;
        // .. and then get adapter from manager
        mAdapter = manager.getAdapter();

        if (mAdapter == null)
            return false;
        // and then check if BT LE is also available
        boolean hasBle = mContext.getPackageManager().hasSystemFeature(
                PackageManager.FEATURE_BLUETOOTH_LE);
        return hasBle;
    }

    /*
     * before any action check if BT is turned ON and enabled for us call this
     * in onResume to be always sure that BT is ON when Your application is put
     * into the foreground
     */
    public boolean isBtEnabled() {
        if (manager == null)
            return false;

        final BluetoothAdapter adapter = manager.getAdapter();
        if (adapter == null)
            return false;

        return adapter.isEnabled();
    }

    public boolean connect(final BluetoothDevice bluetoothDevice) {
        mWakeLock.acquire();
        if (mBLEManager.getMapping().size() < BLEManager.NUM_CONNECTION) {
            // Enable Bluetooth Device Data Link
            addTask(BLETask.newInstance(
                    BLEState.DEVICE_CONNECT, bluetoothDevice, null, null, null));
            addTask(BLETask.newInstance(
                    BLEState.CHARACTERISTIC_NOTIFICATION_WRITE, bluetoothDevice,
                    NRF8001.SERVICE, NRF8001.TX_CHARA,
                    BLEManager.ENABLE));
            return true;
        } else {
            return false;
        }
        
    }

    public void disconnect(final BluetoothDevice bluetoothDevice) {
        if (bluetoothDevice != null) {
            addTask(BLETask.newInstance(BLEState.CHARACTERISTIC_NOTIFICATION_WRITE,
                    bluetoothDevice,
                    NRF8001.SERVICE, NRF8001.TX_CHARA, BLEManager.DISABLE));
        } else {
            Log.e(TAG, "Null Bluetooth Device");
        }
        mWakeLock.release();
    }

    public void scanDevices() {
        mBLEManager.scanDevices(SCAN_INTERVAL);
    }

    public void shutdown() {
        mBLEManager.disconnectAll();
    }

    public void registerUiListener(BLEServiceListener listener) {
        mListeners.add(listener);
        Log.i(TAG, "Registed " + mListeners.size());
    }

    public void removeUiListener(BLEServiceListener listener) {
        mListeners.remove(listener);
        Log.i(TAG, "Removed " + mListeners.size());
    }

    // BLE Queue Commands
    private void addTask(BLETask task) {
        synchronized (tasksQueue) {
            if (tasksQueue.size() > 0 || executingTask) {
                Log.i(TAG, "Added " + task);
                tasksQueue.add(task);
            } else {
                Log.i(TAG, "Executing " + task);
                mBLEManager.runTask(task);
                executingTask = true;
            }
        }
    }

    private void removeTasks(BluetoothDevice device) {
        synchronized (tasksQueue) {
            if (tasksQueue.size() > 0) {
                Iterator<BLETask> it = tasksQueue.iterator();
                while (it.hasNext()) {
                    BLETask task = it.next();
                    if (task.device.getAddress().equals(device.getAddress())) {
                        it.remove();
                    }
                }
                Log.i(TAG, "Queue " + tasksQueue.size());
            }
            if (tasksQueue.size() == 0) {
                executingTask = false;
            }
        }
    }

    // UI Handles
    public void sendDeviceMsg(BluetoothDevice device, String msg) {
        addTask(BLETask.newInstance(BLEState.CHARACTERISTIC_WRITE, device, NRF8001.SERVICE,
                NRF8001.RX_CHARA, msg.getBytes()));
    }

    public void notifyUserDecision(BluetoothDevice device, int choice) {
        //@formatter:off
        addTask(BLETask.newInstance(BLEState.CHARACTERISTIC_WRITE, device, NRF8001.SERVICE,
                NRF8001.RX_CHARA, new byte[] {(byte) choice}));
        //@formatter:on
    }

    // BLE Manager Callback Interface functions
    @Override
    public void updateRssi(BluetoothGatt gatt, int rssi, int status) {
        Log.i(TAG, gatt.getDevice().getAddress() + " " + rssi);
        for (BLEServiceListener listener : mListeners) {
            listener.notifyBLEListener(gatt, rssi);
        }
    }

    @Override
    public void notifyBytesReceived(byte[] values, BluetoothGatt gatt) {
        String msg;
        try {
            msg = new String(values, "UTF-8");
            Log.i(TAG, "Received:" + msg);
            try {
                int systemStatus = Integer.parseInt(msg);
                switch (systemStatus) {
                    case 1:
                    case 2:
                }
            } catch (NumberFormatException e) {
                Log.e(TAG, "Recieved non-number msg:" + msg);
            }
            if (msg.equals("Fall")) {

            } else if (msg.equals("Status")) {

            }
            for (BLEServiceListener listener : mListeners) {
                listener.notifyBLEListener(BLEState.CHARACTERISTIC_CHANGED, msg, 0);
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void devicesDiscovered(BluetoothDevice[] devices) {
        Log.i(TAG, "Devices:" + devices.length + ":listeners " + mListeners.size() + " "
                + hashCode());
        for (BLEServiceListener listener : mListeners) {
            listener.notifyBLEListener(BLEState.DEVICE_CONNECT, devices);
        }
    }

    @Override
    public void notifyCompleteTransaction(BLEState transaction, BLETask currentRunningTask,
            BluetoothGatt gatt) {
        BLETask task = null;
        switch (transaction) {
            case DESCRIPTOR_WRITE:
                if (currentRunningTask.state == BLEState.CHARACTERISTIC_NOTIFICATION_WRITE) {
                    if (Arrays.equals(BLEManager.DISABLE, currentRunningTask.msgOrCmd)) {
                        mBLEManager.disconnect(currentRunningTask.device);
                    }
                    if (gatt.equals(mBLEManager.getCurrentBluetoothGatt())) {
                        Log.i(TAG, "Gatt is the same as current");
                    }
                }
                break;
            default:
        }
        executingTask = false;
        if (tasksQueue.size() > 0) {
            synchronized (tasksQueue) {
                if (tasksQueue.size() > 0) {
                    task = tasksQueue.remove(0);
                }
            }
        }
        if (task != null) {
            mBLEManager.runTask(task);
            Log.i(TAG, "Executing " + task);
        }
        Log.i(TAG, "Notified Complete " + tasksQueue.size() + " " + transaction);
    }

    public interface BLEServiceListener {
        void notifyBLEListener(BluetoothGatt gatt, int rssi);

        void notifyBLEListener(BLEState state, BluetoothDevice[] device);

        void notifyBLEListener(BLEState state, String sensorMsg, int status);
    }

    class BLERssiHandler extends Handler {
        public static final int START = 0;
        public static final int STOP = 1;

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case START:
                    sendEmptyMessageDelayed(START, 2000);
                    mBLEManager.getCurrentBluetoothGatt().readRemoteRssi();
                    break;
                case STOP:
                    removeMessages(START);
                    break;
            }
            super.handleMessage(msg);
        }
    }
}
