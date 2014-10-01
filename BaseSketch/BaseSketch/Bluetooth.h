/*
* BluetoothShield.h
*
* Created: 8/15/2014 1:44:54 PM
*  Author: Sonny
*/


#ifndef BLUETOOTH_H_
#define BLUETOOTH_H_

#include <SPI.h>
#include "Adafruit_BLE_UART.h"

// Connect CLK/MISO/MOSI to hardware SPI
// e.g. On UNO & compatible: CLK = 13, MISO = 12, MOSI = 11
#define ADAFRUITBLE_REQ 10
#define ADAFRUITBLE_RDY 2     // This should be an interrupt pin, on Uno thats #2 or #3
#define ADAFRUITBLE_RST 9

typedef enum {
    ADVERTISING,    // 0
    CONNECTED,      // 1
    CONFIRMED,       // 3
    DISCONNECTED,    // 2
}BLUETOOTH_STATE;

class Bluetooth {
    Adafruit_BLE_UART *serial;
    public:
    void setBLESerial(Adafruit_BLE_UART *BTLEserial);
    void enableBluetooth();
    void checkBluetooth(void (*BluetoothCallback)(BLUETOOTH_STATE));
    void sendSystemStatus(int);
    void sendData(String msg);
    private:
};

#endif /* BLUETOOTH_H_ */

