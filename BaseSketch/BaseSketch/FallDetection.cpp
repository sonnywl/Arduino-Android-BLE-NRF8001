/*
* FallDetection.c
*
* Created: 9/8/2014 12:55:36 AM
*  Author: Sonny
*/

#include "FallDetection.h"
#include <math.h>
#include <Time.h>
#include <stdint.h>            // has to be added to use uint8_t
static int LOW_THRESHOLD = 2;
static int HIGH_THRESHOLD = 20;
static float FREEFALL_DURATION = 0.2;
static float FREEFALL_SLIDER = 0.2;
static float FREEFALL_DURATION_SLIDER = 4;

static float innerTimer = 0;
static float outerTimer = 0;

static double accelAvg = 0.0;
static double accelBufferAvg = 0.0;
static double fallDuration = 0.0;

static char fallPhaseOne = 0;
static char fallPhaseTwo = 0;
static char fallEvent = 0;
static char timeFreeFallEvent = 0;
static char timeGroundContactEvent = 0;


DETECTION_STATE fall_state_t = INITIAL;

static int checkAccelEvent(double accelEvent) {
    if(accelEvent < LOW_THRESHOLD *3) {
        return 0;
    }
    else if ((fallPhaseOne) && accelEvent > HIGH_THRESHOLD * 30) {
        return 1;
    }
    else {
        return -1;
    }
}


// Port of FallService.java
// https://github.com/kevinmalby/SCALE_Fall_Monitoring
void updateFallDetection(double instAccelVect, double bufferAccelAverage, unsigned long timeDiff) {
    fallEvent = checkAccelEvent(instAccelVect);
    switch(fallEvent) {
        case 0:
        timeFreeFallEvent = (!timeFreeFallEvent) ? 1 : 0;
        innerTimer = 0;
        outerTimer = 0;
        break;
        case 1:
        timeGroundContactEvent = (!timeGroundContactEvent) ? 1 : 0;
        break;
    }

    if(timeFreeFallEvent) {
        if(innerTimer > FREEFALL_DURATION * 4 && accelBufferAvg < LOW_THRESHOLD * 3) {
            fall_state_t = PHASE_ONE;
            fallPhaseOne = 1;
            timeFreeFallEvent = 0;
        }
        else if (innerTimer < 0.15 && accelBufferAvg > LOW_THRESHOLD * 3) {
            timeFreeFallEvent = 0;
        }
    }

    if(timeGroundContactEvent) {
        fall_state_t = PHASE_TWO;
        fallPhaseTwo = 1;
        timeGroundContactEvent = 0;
    }

    innerTimer += (timeDiff) / 1000000000.0;
    outerTimer += (timeDiff) / 1000000000.0;

    fallDuration = FREEFALL_DURATION_SLIDER *8;
    if(outerTimer <= fallDuration && fallPhaseOne && fallPhaseTwo) {
        fall_state_t = FALL_CRITICAL;
    }
    else if(outerTimer > fallDuration) {
        fall_state_t = INITIAL;
        fallPhaseOne = 0;
        timeFreeFallEvent = 0;
        timeGroundContactEvent = 0;
        outerTimer = 0;
    }

    if(fall_state_t == FALL_CRITICAL) {
        fall_state_t = INITIAL;
        timeFreeFallEvent = 0;
        timeGroundContactEvent = 0;
    }
}


DETECTION_STATE getUpdateDetectionState() {
    return fall_state_t;
}

void setUpdateDetectionState(DETECTION_STATE state) {
    fall_state_t = state;
}
