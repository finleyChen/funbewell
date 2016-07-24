
#ifndef SENSORS_H
#define SENSORS_H


//======================================================
// Global variables to share GUI state with rest of code
//======================================================
extern bool flagAudio;
extern bool flagAccel;
extern bool flagGps;

//======================================================
// System callback when program is terminated
//======================================================

void expireGracefully(void *data);

//======================================================
// Function prototypes for Sensor Control API
//======================================================

/////////////////
// Initialization
/////////////////

bool SensorInit();

////////////////
// Accelerometer
////////////////
bool setAccelSamplerate(unsigned int sampleRate);
bool setAccelSamplesRequired(unsigned int numSamples);
bool startAccel();
bool stopAccel();

////////
// Audio
////////
bool setAudioSamplerate(unsigned int sampleRate);
bool setAudioSamplesRequired(unsigned int numSamples);
bool startAudio();
bool stopAudio();

//////
// GPS
//////
bool setGpsSamplerate(unsigned int sampleRate);
bool startGps();
bool stopGps();

#endif
