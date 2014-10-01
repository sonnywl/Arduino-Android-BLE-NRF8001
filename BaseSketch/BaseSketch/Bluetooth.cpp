
/*
* Bluetooth.cpp
*
* Created: 8/20/2014 1:35:23 PM
*  Author: Sonny
*/
#include "Bluetooth.h"

aci_evt_opcode_t laststatus = ACI_EVT_DISCONNECTED;

void Bluetooth::setBLESerial(Adafruit_BLE_UART *BTLEserial) {
    serial = BTLEserial;
}

void Bluetooth::enableBluetooth() {
    serial->begin();
}

void Bluetooth::sendSystemStatus(int systemStatus) {
    char test[20];
    sprintf(test, "%d", systemStatus);
    String res = test;
    sendData(res);
}

void Bluetooth::sendData(String msg) {
    uint8_t sendbuffer[20];
    msg.getBytes(sendbuffer, 20);
    char sendbuffersize = min(20, msg.length());
    serial->write(sendbuffer, sendbuffersize);
}

void Bluetooth::checkBluetooth(void (*bleCallback)(BLUETOOTH_STATE)) {
    serial->pollACI();
    // Ask what is our current status
    aci_evt_opcode_t status = serial->getState();
    // If the status changed....
    if (status != laststatus) {
        // print it out!
        if (status == ACI_EVT_DEVICE_STARTED) {
            bleCallback(ADVERTISING);
            Serial.println(F("* Advertising started"));
        }
        if (status == ACI_EVT_CONNECTED) {
            Serial.println(F("* Connected! HERE"));
            bleCallback(CONNECTED);
        }
        if (status == ACI_EVT_DISCONNECTED) {
            Serial.println(F("* Disconnected or advertising timed out"));
            bleCallback(DISCONNECTED);
        }
        // OK set the last status change to this one
        laststatus = status;
    }

    if (status == ACI_EVT_CONNECTED) {
        // Lets see if there's any data for us!
        // OK while we still have something to read, get a character and print it out
        char returnValue = 0;
        while (serial->available()) {
            char c = serial->read();
            Serial.print(c);
            returnValue = 1;
            if(returnValue) {
                bleCallback(CONFIRMED);
            }
        }
    }
}
