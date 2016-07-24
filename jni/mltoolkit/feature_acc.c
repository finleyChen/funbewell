/*
 *  MLToolKit
 *
 *  Created by Hong Lu on 07/07/2011.
 *
 */

#include "feature_acc.h"
#include "features.h"
#include "kiss_fftr.h"
#include <android/log.h>
#define LOG_TAG "mltoolkit"
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR  , LOG_TAG, __VA_ARGS__) 
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG  , LOG_TAG, __VA_ARGS__)
#define DEBUG 0

void freqFeat(double * data,int len, int sr,double * spectrum,double * feat){
	kiss_fft_cpx freq[len/2+1];
	double fft[len/2]; 
	double fftsum = 0,fftsum0_1 = 0,fftsum1_3 = 0,fftsum3_5 = 0,fftsum5_end = 0,ensum = 0;
	double fftr=0,fftr1=0,fftr2=0,entropy=0;
	double fftPeak;
	double bin = len/sr;
	kiss_fftr_cfg cfg=kiss_fftr_alloc(len,0,NULL,NULL);
	kiss_fftr(cfg,data,freq);
	free(cfg);
	//cout<< "fft done" <<endl;
	double max = 0;
	int index = 0;
	for(int i = 1;i < len/2+1;i++){
		fft[i-1] = sqrt(pow(freq[i].r,2) + pow(freq[i].i,2));
		if(fft[i-1] == 0){
			fft[i-1] = 0.0000000000001;
		}
		fftsum += fft[i-1];
		if (i <= bin) fftsum0_1 += fft[i-1];
		if (i <= 3*bin && i >= bin) fftsum1_3 += fft[i-1];
		if (i <= 5*bin && i >= 3*bin) fftsum3_5 += fft[i-1];
		if (i >= 5*bin) fftsum5_end += fft[i-1];
		if (i == 1){
			max = fft[i-1];
			index = 1;
		}
		else if(fft[i-1]>max){
			max = fft[i-1];
			index = i;
		}
	}
	//cout<< "peak done" <<endl;
	fftPeak = index;
	fftr  = (fftsum0_1+fftsum1_3-fft[3])/(fftsum-fftsum0_1-fftsum1_3+fft[3]+fft[11]);
	fftr1 = fftsum0_1/fftsum1_3;
	fftr2 = fftsum3_5/fftsum5_end;
	//cout<< "ratio done" <<endl;
	for(int i = 0;i < len/2;i++){
		spectrum[i] = fft[i];
		fft[i] = fft[i]*fft[i];     //energy spectrum
		ensum += fft[i];
	}
	//cout<< ensum <<endl;
	for(int i = 0;i < len/2;i++){
		fft[i] = fft[i]/ensum;
		if(fft[i] == 0){
			fft[i] = 0.0000000000001;
		}
		//cout<<fft[i]<<" ";           
		entropy -= fft[i]*log(fft[i]);  //entropy
		//cout<<entropy<<" ";
	}
	feat[0] = fftr;   
	feat[1] = fftr1;
	feat[2] = fftr2;
	feat[3] = fftPeak;
	feat[4] = entropy;
	feat[5] = fftsum0_1; 
	feat[6] = fftsum1_3;
	feat[7] = fftsum3_5;
	feat[8] = fftsum5_end;   
}

void featExtract(double* v,double * h,double * features,int len,int sr){
	double meanH=0,meanV=0,varH=0,varV=0,mcrV=0,mcrH=0,corrf = 0;
	double spectrumV[len/2];
	double spectrumH[len/2];
	double ffeatV[9],ffeatH[9];
	variance(v,len,&meanV,&varV);
	variance(h,len,&meanH,&varH);
	//mean crossing
	mcrV = mcr(v,meanV,len);
	mcrH = mcr(h,meanH,len);
	//freq feautures
	freqFeat(v,len,sr,spectrumV,ffeatV);
	freqFeat(h,len,sr,spectrumH,ffeatH);
	//corr(X,Y)	
	corrf = corr(spectrumV,spectrumH,len/2);
	double fVector[25] = {meanV,meanH,varV,varH,mcrV,mcrH,ffeatV[0],ffeatH[0],
		ffeatV[1],ffeatH[1],ffeatV[2],ffeatH[2],ffeatV[3],ffeatH[3],
		ffeatV[4],ffeatH[4],ffeatV[5],ffeatH[5],ffeatV[6],ffeatH[6],
	ffeatV[7],ffeatH[7],ffeatV[8],ffeatH[8],corrf};
	memcpy(features,fVector,25*sizeof(double));        
}

int getAccFeatures(double *accX,double *accY,double *accZ,int len,int sr,double* fVector)
{
    static double oldMeanX = 0;
    static double oldMeanY = 0;
    static double oldMeanZ = 0;
    static int counter = 0;
    double meanX,meanY,meanZ,meanNorm,diffNorm;
    double gX,gY,gZ,gNorm;
    double v[len], h[len];

	if(DEBUG) LOGD("acc feature extraction called  + %d", counter);
    counter++;
	if(DEBUG) LOGD("acc feature extraction called  + %d", counter);
    //gettimeofday(&tv[1], NULL);
    // apply admission control to current frame
    meanX = mean(accX,len);
    meanY = mean(accY,len);
    meanZ = mean(accZ,len);
    //meanNorm = sqrt(meanX*meanX+meanY*meanY+meanZ*meanZ);
    diffNorm = sqrt(pow(meanX - oldMeanX,2)+pow(meanY - oldMeanY,2)+pow(meanZ - oldMeanZ,2));
    
    //debug    
    if (diffNorm > 0.2) { // sensor oritention changed too much
        counter = 0;    // reset the counter (i.e. # of admitted state so far)
	    oldMeanX = meanX;
	    oldMeanY = meanY;
	    oldMeanZ = meanZ;
        return 1; 
    }
	
	
	//[gX,gY,gZ] is the estimated G direction
	gX = (meanX + oldMeanX)/2;
	gY = (meanY + oldMeanY)/2;
	gZ = (meanZ + oldMeanZ)/2;
	//printf("%.12f,%.12f,%.12f\n",gX,gY,gZ);
	gNorm = sqrt(gX*gX+gY*gY+gZ*gZ);
	//printf("%.12f\n",gNorm);
	gX = gX/gNorm;
	gY = gY/gNorm;
	gZ = gZ/gNorm;
	//projection
	for (int i = 0; i < len; i++) {
		v[i] = accX[i]*gX + accY[i]*gY + accZ[i]*gZ;
		h[i] = norm(accX[i] - v[i]*gX,accY[i] - v[i]*gY,accZ[i] - v[i]*gZ);
	}
	
	//double fVector[25];
	featExtract(v,h,fVector,len,sr);
	//cout<< "feature done "<<endl;
	//printf("\n============%d===============\n",motion_state);
	//for(int i = 0;i<6;i++){printf("%d,",(tv[i].tv_sec-tv[0].tv_sec)*1000000 + tv[i].tv_usec - tv[0].tv_usec);}printf(";\n");
    oldMeanX = meanX;
    oldMeanY = meanY;
    oldMeanZ = meanZ;
	if(DEBUG) LOGD("acc feature extraction called  + %d", counter);
    return 0;
}

