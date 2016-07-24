/*
 *  MLToolKit
 *
 *  Created by Hong Lu on 07/07/2011.
 *
 */
#ifndef _procACC_H_
#define _procACC_H_


#ifdef __cplusplus
extern "C" {
#endif 


// apply the calibration

void freqFeat(double * data,int len,int sr,double * spectrum,double * feat);

void featExtract(double* v,double * h,double * features,int len,int sr);

int getAccFeatures(double *accX,double *accY,double *accZ,int len,int sr,double* fVector);

 #ifdef __cplusplus
 }
 #endif 

#endif
