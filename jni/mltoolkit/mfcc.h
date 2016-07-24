#ifndef _MFCC_H_
#define _MFCC_H_

#ifdef __cplusplus
extern "C" {
#endif
void initMfcc(int sample_rate, int mell, int high_freq, int low_freq, int inputlen);

double* calculateMfcc(double* data);

void delMfcc();

#ifdef __cplusplus
}
#endif
	
	
#endif 
