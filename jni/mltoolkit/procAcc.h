/*
 *  procAcc.h
 *  SensingEngine
 *
 *  Created by hong on 11/8/09.
 *  Copyright 2009 __MyCompanyName__. All rights reserved.
 *
 */
#ifndef _procACC_H_
#define _procACC_H_


#ifdef __cplusplus
extern "C" {
#endif 


// apply the calibration

int processAcc(double * buf,int len,int sr,void (*callBack)(int));

void normalizedAcc(double x, double y, double z, double * x_ptr, double * y_ptr, double * z_ptr);

void freqFeat(double * data,int len,int sr,double * spectrum,double * feat);

void featExtract(double* v,double * h,double * features,int len,int sr);

 #ifdef __cplusplus
 }
 #endif 

#endif
