/*
 *  MLToolKit.h
 *  MLToolKit
 *
 *  By hong Lu on 2/19/10.
 *  MLtoolkit Project, 2010
 *
 */

#ifndef _MLToolKit_H_
#define _MLToolKit_H_
#include "buffer.h"
//#include "config.h"
#include <stdint.h>

#ifdef __cplusplus
extern "C" {
#endif 
/*
extern struct dataBuffer buffer;
//pthread_mutex_t mutex_accBuffer;
// the semaphore implementation is different in Linux and Mac
// the sensing thread use this to wake up the processing thread
extern sem_t * sem_acc;
// the buffer lock
extern spthread_mutex_t mutex_Proc_acc;
extern int processing;
*/
//double accSamples[3*WINDOW_SIZE];


//////////////////////////////////////////////////////
// config for sensors
//////////////////////////////////////////////////////

typedef struct {
   int samplingRate;
   int frameLength;               // # of samples
   void (*callBack)(int);
} acc_config;

typedef struct {
   int samplingRate;
   int frameLength;
   int windowLength;
   int mfccLength;
   void (*callBack)(int);
} audio_config;

typedef struct {
   int samplingRate;
   void (*callBack)(int);
} loc_config;


// call this before start sensing, 
// allocate mem, start processing thread and put it to sleep.
void init(acc_config * acc_Cfg, audio_config * audio_Cfg, loc_config * loc_Cfg);

// the processing thread
//void * processAccelerometerData(void * arg);

// accelerometer call back function, call to add new sample
void getAccSample(double x,double y,double z);

// audio sample, call to add new sample
void getAudioSample(short * data,int len);

// Call to get IMEI string
//extern char *getIMEIstring();

// call at exit
void destroy();

#ifdef __cplusplus
}
#endif 
#endif
