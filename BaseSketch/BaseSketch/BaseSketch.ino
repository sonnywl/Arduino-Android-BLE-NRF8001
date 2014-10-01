
/*
* BaseSketch.ino
*
* Created: 9/18/2014 6:26:52 PM
* Author: Sonny
*/
#define DEBUG 0
#define SHORT_INTERVAL_US 20000
#define LONG_INTERVAL_US 1000000
#define PHASE_ONE_THRESHOLD 6
#define PHASE_TWO_THRESHOLD 6

#include <avr/sleep.h>
#include <avr/power.h>
#include <math.h>

#include <Adafruit_BLE_UART.h>
#include <ADXL345.h>

#include <TimerOne/TimerOne.h>
#include <Wire/Wire.h>
#include <SPI/SPI.h>

#include "Accelerometer.h"
#include "Bluetooth.h"
#include "Buffer.h"
#include "FallDetection.h"

volatile int counter = 0;

typedef enum {
    MONITOR,    // Fall Detection
    BROADCAST,  // Fall Broadcast
    RESET,
    INFO   // System Info to User
}
SYSTEM_STATE;

ADXL345 adxl; //variable adxl is an instance of the ADXL345 library
Adafruit_BLE_UART BTLEserial = Adafruit_BLE_UART(ADAFRUITBLE_REQ, ADAFRUITBLE_RDY, ADAFRUITBLE_RST);
Bluetooth bluetooth;

SYSTEM_STATE sys_state_t;
DETECTION_STATE detc_state_t;
ADXL_STATE adxl_state_t;

double adxlVal[3];
double data[5];
double instAccel = 0.0;
Buffer buffer = Buffer(data, sizeof(data)/sizeof(double));

char isBroadcasted = 0;
long timeCounter = 0;
long lastMillis = 0;

unsigned long phaseOneCount;   // Weightless Phase Count
unsigned long phaseTwoCount;   // Strike Phase Count

void timerIsr() {
    counter++;
}

void enterSleep(void) {
    set_sleep_mode(SLEEP_MODE_IDLE);
    sleep_enable();
    power_adc_disable();
    // power_spi_disable();
    power_timer0_disable();
    power_timer2_disable();
    // power_twi_disable();

    /* Now enter sleep mode. */
    sleep_mode();
    
    /* The program will continue from here after the timer timeout*/
    sleep_disable(); /* First thing to do is disable sleep. */
    
    /* Re-enable the peripherals. */
    power_all_enable();
}

/* Callback for bluetooth detection */
void updateBluetoothStatus(BLUETOOTH_STATE state) {
    switch(state) {
        case CONNECTED:{
            // Notify Connected Device that there is something serious
            // Remove if for steady interval broadcast testing
            if(!isBroadcasted) {
                bluetooth.sendSystemStatus(1);
                isBroadcasted = 1;
            }
            break;
        }
        case ADVERTISING:{
            isBroadcasted = 1;
            break;
        }
        case CONFIRMED: {
            // Received Response from BLE Master
            isBroadcasted = 0;
            sys_state_t = RESET;
            break;
        }
    }
}

void resetCounts() {
    phaseOneCount = 0;
    phaseTwoCount = 0;
}

void setup()
{
    //Serial.begin(9600);
    
    sys_state_t = MONITOR;
    detc_state_t = INITIAL;
    
    bluetooth.setBLESerial(&BTLEserial);
    bluetooth.enableBluetooth();
    bluetooth.checkBluetooth(&updateBluetoothStatus);
    
    setupADXL(&adxl, 4);
    
    Timer1.initialize(LONG_INTERVAL_US);
    Timer1.attachInterrupt(timerIsr);
}

void loop()
{
    
    switch(sys_state_t) {
        case BROADCAST: {
            // For monitor loop
            BTLEserial.pollACI();
            bluetooth.checkBluetooth(&updateBluetoothStatus);
            break;
        }
        case RESET: {
            sys_state_t = MONITOR;
            Timer1.initialize(LONG_INTERVAL_US);
            Timer1.attachInterrupt(timerIsr);
            resetCounts();
            break;
        }
        case MONITOR: {
            adxl.get_Gxyz(adxlVal);
            instAccel = sqrt(pow(adxlVal[0], 2))
            + sqrt(pow(adxlVal[1], 2))
            + sqrt(pow(adxlVal[2], 2));

            buffer.addValue(instAccel);

            double buffAvg, buffStdDev;
            buffAvg = buffer.getAverage();
            buffStdDev = buffer.getStdDev(buffAvg);

            byte intrpt = adxl.getInterruptSource();
            
            // freefall
            if(adxl.triggered(intrpt, ADXL345_FREE_FALL)){
                adxl_state_t = FREE_FALL;
                // Serial.println(F("Freefall"));
            }
            
            //inactivity
            else if(adxl.triggered(intrpt, ADXL345_INACTIVITY)){
                adxl_state_t = INACTIVITY;
                // Serial.println(F("inactive"));
            }
            
            //activity
            else if(adxl.triggered(intrpt, ADXL345_ACTIVITY)){
                adxl_state_t = ACTIVITY;
                // Serial.println(F("activity"));
            }
            
            //***** Deactiviation Phase ****//
            if(detc_state_t == FALL_CRITICAL) {
                // Serial.println(F("Fall Detected"));
                sys_state_t = BROADCAST;
                detc_state_t = INITIAL;
                resetCounts();
            }

            if(detc_state_t == PHASE_TWO){
                phaseTwoCount++;
                if(phaseTwoCount >= PHASE_TWO_THRESHOLD) {
                    // Serial.println(F("Phase Two Deactivated"));
                    detc_state_t = INITIAL;
                    resetCounts();
                }
            }
            
            if(detc_state_t == PHASE_ONE){
                phaseOneCount++;
                if(phaseOneCount >= PHASE_ONE_THRESHOLD) {
                    // Serial.println(F("Phase One Deactivated"));
                    phaseOneCount = 0;
                    detc_state_t = INITIAL;
                }
            }

            //***** Phase change ****//
            // Fall Confirmation Phase
            if(detc_state_t == PHASE_TWO) {
                // Person does not move else system will
                // Else the Deactivation of Phase Two will happen
                if(instAccel > 0.5) {
                    // Serial.println(F("FALL TRANSITION"));
                    detc_state_t = FALL_CRITICAL;
                    resetCounts();
                }
            }

            // Strike Phase
            if(detc_state_t == PHASE_ONE && instAccel > 2 && buffStdDev > 0.1) {
                // Serial.println(F("Phase Two Transition"));
                detc_state_t = PHASE_TWO;
                phaseOneCount = 0;
            }

            // Weightless Phase
            if(detc_state_t == INITIAL && instAccel < 0.5 && buffAvg < 1) {
                // Serial.println(F("Phase One Transition"));
                if(DEBUG) {
                    sys_state_t = BROADCAST;
                }
                detc_state_t = PHASE_ONE;
            }
            
            // Sampling Interval Change
            if(adxl_state_t == ACTIVITY) {
                Timer1.initialize(SHORT_INTERVAL_US);
                Timer1.attachInterrupt(timerIsr);
                enterSleep();
            }
            // If Fall Detected, disable timer so broadcast is continuous and
            // reduce phone's communication energy consumption cost
            else if(detc_state_t == FALL_CRITICAL) {
                Timer1.stop();
                sleep_disable();
            }
            else {
                Timer1.initialize(LONG_INTERVAL_US);
                Timer1.attachInterrupt(timerIsr);
                enterSleep();
            }
            break;
        }
    }
}
