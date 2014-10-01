
package edu.uci.fallsensor.service;

import android.bluetooth.BluetoothDevice;

import java.util.UUID;

/**
 * Wrapper to manage BLE transactions with BLE devices
 * 
 * @author Sonny
 */
public class BLETask {
    final BLEState state;
    final BluetoothDevice device;
    final UUID serviceUuid;
    final UUID charaUuid;
    final byte[] msgOrCmd;

    public static BLETask newInstance(
            BLEState state, BluetoothDevice device, UUID serviceUuid, UUID charaUuid, byte[] msgOrCmd) {
        return new BLETask(state, device, serviceUuid, charaUuid, msgOrCmd);
    }

    private BLETask(BLEState state, BluetoothDevice device, UUID serviceUuid, UUID charaUuid, byte[] msgOrCmd) {
        this.state = state;
        this.device = device;
        this.serviceUuid = serviceUuid;
        this.charaUuid = charaUuid;
        this.msgOrCmd = msgOrCmd;
    }
    
    public String toString() {
        return new StringBuilder().append(state).append(",")
                .append(device.getName()).append(",")
                .append(serviceUuid).append(",")
                .append(charaUuid).append(",")
                .append(msgOrCmd).toString();
    }
}
