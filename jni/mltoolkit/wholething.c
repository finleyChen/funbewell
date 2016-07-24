//#include "MsbTime.h"
#include "MLToolKit.h"
#include "Debug.h"
//#include <iostream>
//#include "comm.h" // For upload
//#include <stdlib.h>
#include <unistd.h>
#include <dirent.h> // For upload
#include <assert.h> // For upload
// N900 Specific includes

#include <android/log.h>
#define print(...) __android_log_print(ANDROID_LOG_DEBUG  , "testfft", __VA_ARGS__) 


// Set sensor sample rates in Hz
#define ACCEL_SAMPLERATE 32
#define AUDIO_SAMPLERATE 8192
// GPS sample rate is in seconds and can only be one of these values:
// 1,2,5,10,20,30,60,120 seconds
#define GPS_SAMPLERATE 1
// Set number of samples required for processing
#define ACCEL_SAMPLES_REQUIRED 128
#define AUDIO_SAMPLES_REQUIRED 512


//////////////////////////////////////////////////
//
// call backfuctions for accel and audio
//
///////////////////////////////////////////////

void accelActCallBack(int act){
  switch(act){
    case -1:
      print("acc: unknown\n");////fflush(stdout);
      break;
    case 0:
      print("acc: stationary\n");//fflush(stdout);
      break;
    case 1:
      print("acc: walking\n");//fflush(stdout);
      break;
    case 3:
      print("acc: running\n");//fflush(stdout);
      break;
    case 4:
      print("acc: vehicle\n");//fflush(stdout);
      break;
    default:
      print("acc: ?");
      break;
  }
}

void audioActCallBack(int act){
  switch(act){
    case -1:
      print("audio: silence\n");//fflush(stdout);

      break;
    case 0:
      print("audio: noise\n");//fflush(stdout);

      break;
    case 1:
      print("audio: voice\n");//fflush(stdout);

      break;
    default:
      print("audio: error\n");//fflush(stdout);

      break;
  }
}

////////////////////////////////////////////////////////////////////
//
//main
//
////////////////////////////////////////////////////////////////////
void wholething(){


  ////////////////////////////////////////////////
  // init the config of the tool kit
  ////////////////////////////////////////////////

  acc_config ac_Cfg;
  ac_Cfg.samplingRate =  ACCEL_SAMPLERATE;
  ac_Cfg.frameLength = ACCEL_SAMPLES_REQUIRED;
  ac_Cfg.callBack = accelActCallBack;

  audio_config au_Cfg;
  au_Cfg.samplingRate =  AUDIO_SAMPLERATE;
  au_Cfg.frameLength = AUDIO_SAMPLES_REQUIRED;
  au_Cfg.windowLength = 20;
  au_Cfg.mfccLength = 14;
  au_Cfg.callBack = audioActCallBack;
	
  ///////////////////////////////////////////////
  // init the toolkit
  //////////////////////////////////////////////

  init(&ac_Cfg,&au_Cfg,NULL);
  print("Processing thread inited");
	 
  int count = 0;

  short int tmp[512];
  for(int i = 0; i<512; i++) tmp[i] = i;	

  double x = 1;
  double y = 2;
  double z = 3;

  while(1){
  usleep(31250);
  if(count%2 == 0)getAudioSample(tmp,512);
  count++;		
  getAccSample(x,y,z);
  }


  ////////////////////////////////////////////////
  // Initialize sensing parameters
  ////////////////////////////////////////////////

  //setAccelSamplerate(ACCEL_SAMPLERATE);
  //setAccelSamplesRequired(ACCEL_SAMPLES_REQUIRED);
  //setAudioSamplerate(AUDIO_SAMPLERATE);
  //setAudioSamplesRequired(AUDIO_SAMPLES_REQUIRED);
  //setGpsSamplerate(GPS_SAMPLERATE);
  
  ////////////////////////////////////////////////
  // Initialize sensors and start sensing timers
  ////////////////////////////////////////////////

  //startAccel();
  //startAudio();
  //startGps();

  // Draw the the contents of the main window
  // gtk_main() will never return until the program exits.
}

