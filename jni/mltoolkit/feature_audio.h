/*
 *  MLToolKit
 *
 *  Created by Hong Lu on 07/07/2011.
 *
 */


#ifndef _FeatAudio_H_
#define _FeatAudio_H_


#ifdef __cplusplus
extern "C" {
#endif



void getFrameFeature( short * data, double * hanning, int frameIndex, int frame_length, int fft_length, int mfcc_length, double * feature);

void getWindowFeature(int window_length, int frameIndex, int mfcc_length, double * se_history, double * sf_history, double * sc_history, double * ro_history, double * bw_history, double * mfcc_history, double * rms_history, double * feature);
 #ifdef __cplusplus
 }
 #endif 

#endif