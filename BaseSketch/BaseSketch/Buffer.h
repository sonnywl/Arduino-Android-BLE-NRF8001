/*
 * Buffer.h
 *
 *  Created on: Sep 7, 2014
 *      Author: Sonny
 */

#ifndef BUFFER_H_
#define BUFFER_H_
#include <math.h>

class Buffer
{
public:
	Buffer(double*dataSet, int size);
    double* dataSet;
	bool capacityReached;
	void addValue(double value);
	double getAverage(void);
    double getStdDev(double avg);
	void clear(void);
private:
	int pointer;
	int capacity;

	void increment(bool adding);
	int size();
};

#endif /* BUFFER_H_ */
