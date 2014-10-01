
package com.ble.sensors;

import java.util.UUID;

/**
 * Bluetooth Gatt Attributed provided by Adafruit's NRF8001 Breakout board
 * https://github.com/adafruit/Adafruit_nRF8001
 * <p>
 * You can send and receive up to 20 bytes at a time between your BLE-enabled
 * phone or tablet and the Arduino.
 * </p>
 * 
 * @author Sonny
 */
public final class NRF8001 {
    private NRF8001() {
    }

    public static final UUID
            SERVICE = UUID.fromString("6E400001-B5A3-F393-E0A9-E50E24DCCA9E"),
            RX_CHARA = UUID.fromString("6E400002-B5A3-F393-E0A9-E50E24DCCA9E"),
            TX_CHARA = UUID.fromString("6E400003-B5A3-F393-E0A9-E50E24DCCA9E");
}
