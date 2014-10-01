
/*
* Accelerometer.cpp
*
* Created: 8/20/2014 12:48:16 PM
*  Author: Sonny
*/

#include "Accelerometer.h"


void setupADXL(ADXL345 *adxl, int range) {
	adxl->powerOn(range);
    adxl->setLowPower(false);
    
	adxl->setAxisOffset(0,0,10);
	//set activity/ inactivity thresholds (0-255)
	adxl->setActivityThreshold(25); //62.5mg per increment
	adxl->setInactivityThreshold(50); //62.5mg per increment
	adxl->setTimeInactivity(5); // how many seconds of no activity is inactive?
	
	//look of activity movement on this axes - 1 == on; 0 == off
	adxl->setActivityX(1);
	adxl->setActivityY(1);
	adxl->setActivityZ(1);
	
	//look of inactivity movement on this axes - 1 == on; 0 == off
	adxl->setInactivityX(1);
	adxl->setInactivityY(1);
	adxl->setInactivityZ(1);
	
	//look of tap movement on this axes - 1 == on; 0 == off
	adxl->setTapDetectionOnX(1);
	adxl->setTapDetectionOnY(1);
	adxl->setTapDetectionOnZ(1);
	
	//set values for what is a tap, and what is a double tap (0-255)
	adxl->setTapThreshold(50); //62.5mg per increment
	adxl->setTapDuration(15); //625?s per increment
	adxl->setDoubleTapLatency(80); //1.25ms per increment
	adxl->setDoubleTapWindow(200); //1.25ms per increment
	
	//set values for what is considered freefall (0-255)
	adxl->setFreeFallThreshold(7); //(5 - 9) recommended - 62.5mg per increment
	adxl->setFreeFallDuration(45); //(20 - 70) recommended - 5ms per increment
	
	//setting all interupts to take place on int pin 1
	//I had issues with int pin 2, was unable to reset it
	adxl->setInterruptMapping( ADXL345_INT_SINGLE_TAP_BIT,   ADXL345_INT1_PIN );
	adxl->setInterruptMapping( ADXL345_INT_DOUBLE_TAP_BIT,   ADXL345_INT1_PIN );
	adxl->setInterruptMapping( ADXL345_INT_FREE_FALL_BIT,    ADXL345_INT1_PIN );
	adxl->setInterruptMapping( ADXL345_INT_ACTIVITY_BIT,     ADXL345_INT1_PIN );
	adxl->setInterruptMapping( ADXL345_INT_INACTIVITY_BIT,   ADXL345_INT1_PIN );
	
	//register interupt actions - 1 == on; 0 == off
	adxl->setInterrupt( ADXL345_INT_SINGLE_TAP_BIT, 1);
	adxl->setInterrupt( ADXL345_INT_DOUBLE_TAP_BIT, 1);
	adxl->setInterrupt( ADXL345_INT_FREE_FALL_BIT,  1);
	adxl->setInterrupt( ADXL345_INT_ACTIVITY_BIT,   1);
	adxl->setInterrupt( ADXL345_INT_INACTIVITY_BIT, 1);
}
