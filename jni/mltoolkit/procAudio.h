/*
*  By hong Lu on 4/19/10.
*  MLtoolkit Project, 2010
*
*/


#ifndef _PROCAUDIO_H_
#define _PROCAUDIO_H_

#ifdef __cplusplus
extern "C" {
#endif

void initAudioProcessor(int frameLength, int windowLength, int mfccLength,int sr);

void delAudioProcessor();

int processAudio( short * data, int length, void (*callBack)(int));

#ifdef __cplusplus
}
#endif
	
	
#endif 
