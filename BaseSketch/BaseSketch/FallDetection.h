/*
 * FallDetection.h
 *
 * Created: 9/5/2014 2:41:55 PM
 *  Author: Sonny
 */ 


#ifndef FALLDETECTION_H_
#define FALLDETECTION_H_

// Definitions used for Fall Detection Algorithm
#define STRIKE_THRESHOLD 0x20 //62.5mg/LSB, 0x20=2g
#define STRIKE_WINDOW 0x0A //20ms/LSB, 0x0A=10=200ms
#define STABLE_THRESHOLD 0x08 //62.5mg/LSB, 0x10=0.5g
#define STABLE_TIME 0x02 //1s/LSB, 0x02=2s
#define STABLE_WINDOW 0xAF //20ms/LSB, 0xAF=175=3.5s
#define NOMOVEMENT_THRESHOLD 0x03 //62.5mg/LSB, 0x03=0.1875g
#define NOMOVEMENT_TIME 0x0A //1s/LSB, 0x0A=10s
#define FREE_FALL_THRESHOLD 0x0C //62.5mg/LSB, 0x0C=0.75g
#define FREE_FALL_TIME 0x06 //5ms/LSB, 0x06=30ms
#define FREE_FALL_OVERTIME 0x0F //20ms/LSB, 0x0F=15=300ms
#define FREE_FALL_INTERVAL 0x05 //20ms/LSB, 0x05=100ms
#define DELTA_VECTOR_SUM_THRESHOLD 0x7D70 //1g=0xFF, 0x7D70=0.7g^2
#define GRAVITY 9.81

typedef enum {
    INITIAL,		// 0xF0 Initial
    PHASE_ONE,	    // 0xF1 Weightless
    PHASE_TWO,		// 0xF2 Strike
    FALL_CRITICAL   // 0xF3 Fall Confirm
}
DETECTION_STATE;

void updateFallDetection(double instAccelVect, double bufferAccelAverage, unsigned long timeDiff);
    
#endif /* FALLDETECTION_H_ */