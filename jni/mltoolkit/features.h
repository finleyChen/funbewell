/*
 *  MLToolKit
 *
 *  Created by Hong Lu on 07/07/2011.
 *
 */

#ifndef _features_h_
#define _features_h_

#ifdef __cplusplus
extern "C" {
#endif 

//#define norm(x,y,z) (sqrt((x)*(x)+(y)*(y)+(z)*(z)))

double mean(const double * data,int len);

double var(double const * data, int len);

//calculate variance and mean in Matlab manner 
void variance(double * data,int length,double * mean,double * variance);

// correlation coefficient
double corr(double * data_1, double *data_2, int length);

double norm(double x,double y,double z);

// zero crossing
int zcr(double * data,int length);

int mcr(double * in,double mean,int len);

/* fft
 *data:the time series, should be 2^N
 *length: the size of the time series 
 *spec: the spectrum, spec[0] is the DC. spec[1 ~ len/2] is the fft bins.
 *
 */
int spectrum(double * data,int length,double * spec);

 #ifdef __cplusplus
 }
 #endif 


#endif
//



