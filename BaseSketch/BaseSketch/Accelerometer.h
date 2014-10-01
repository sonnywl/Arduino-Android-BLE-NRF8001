/*
* IncFile1.h
*
* Created: 8/15/2014 1:24:42 PM
*  Author: Sonny
*/


#ifndef ACCELEROMETER_H_
#define ACCELEROMETER_H_

#include <ADXL345.h>

#define POWER_CTL  0x2D;	//Power Control Register
#define DATA_FORMAT  0x31;
#define DATAX0 0x32;	//X-Axis Data 0
#define DATAX1 0x33;	//X-Axis Data 1
#define DATAY0 0x34;	//Y-Axis Data 0
#define DATAY1 0x35;	//Y-Axis Data 1
#define DATAZ0 0x36;	//Z-Axis Data 0
#define DATAZ1 0x37;	//Z-Axis Data 1

typedef enum {
    INACTIVITY,
    ACTIVITY,
    FREE_FALL,
    MISC
} ADXL_STATE;

void setupADXL(ADXL345 *adxl, int range);

#endif /* ACCELEROMETER_H_ */