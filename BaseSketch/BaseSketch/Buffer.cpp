/*
* Buffer.cpp
*
*  Created on: Sep 7, 2014
*      Author: Sonny
*/

#include "Buffer.h"


Buffer::Buffer(double *data, int size) {
    pointer = -1;
    capacity = size;
    capacityReached = false;
    dataSet = data;
}

void Buffer::addValue(double value) {
    increment(true);
    dataSet[pointer] = value;
}

double Buffer::getStdDev(double avg) {
    int totalSize;
    totalSize = size();
    if(totalSize == 0 || totalSize - 1 == 0) {
        return 0.0;
    }
    double total = 0.0;
    int loc, i;
    loc = pointer;
    for(i = 0; i < totalSize; i++) {
        if(loc < 0) {
            if(capacityReached) {
                loc = capacity;
            } else {
                break;
            }
        }
        total = pow(dataSet[i] - avg, 2);
    }
    
    return sqrt(total/ (totalSize - 1));
}

double Buffer::getAverage(void) {
    double total = 0.0;
    int totalSize = size();
    int i;
    if (capacityReached) {
        for (i = 0; i < capacity; i++) {
            total += dataSet[i];
        }
    } else { // When array is not populated
        if (pointer != -1) {
            for (i = 0; i <= pointer; i++) {
                total += dataSet[i];
            }
        }
    }
    if (totalSize == 0) {
        totalSize = 1;
    }
    return total / totalSize;
}

void Buffer::clear(void) {
    capacityReached = false;
    int i;
    for(i = 0; i < capacity; i++) {
        dataSet[i] = 9.81;
    }
}

void Buffer::increment(bool adding) {
    pointer++;
    if (adding) {
        if (pointer >= capacity) {
            pointer = 0;
            capacityReached = true;
        }
        } else {
        if (pointer >= capacity) {
            pointer = 0;
        }
    }
}

int Buffer::size() {
    if (capacityReached) {
        return capacity;
    }
    return (pointer + 1);
}

