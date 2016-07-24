/*
 *  accfeatures.c
 *  SensingEngine
 *
 *  Created by hong on 11/8/09.
 *  Copyright 2009 __MyCompanyName__. All rights reserved.
 *
 */

#include "procAcc.h"
#include <string.h>
#include "features.h"
#include "config.h"
#include "kiss_fftr.h"
#include "classifier.h"


//#define PROCacc
#ifdef PROCacc
  #define PROCACC( COMMAND ) COMMAND; 
#else
  #define PROCACC( COMMAND )
#endif

void normalizedAcc(double x, double y, double z, double * x_ptr, double * y_ptr, double * z_ptr){
	*x_ptr = (x - X_OFFSET)/X_SCALE;
	*y_ptr = (y - Y_OFFSET)/Y_SCALE;
	*z_ptr = (z - Z_OFFSET)/Z_SCALE;
}



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

int processAcc(double * buf,int len,int sr,void (*callBack)(int))
{
    double *accX;
    double *accY;
    double *accZ;
    static double oldMeanX = 0;
    static double oldMeanY = 0;
    static double oldMeanZ = 0;
    static int counter = 0;
    double meanX,meanY,meanZ,meanNorm,diffNorm;
    double gX,gY,gZ,gNorm;
    double v[len], h[len];
    int unknown = 0;                 // transitional movement detection
    int motion_state = -2;
    //struct timeval tv[6];
    //gettimeofday(&tv[0], NULL);
    counter++;
    accX = buf;
    accY = buf+len;
    accZ = buf+len*2;
    //gettimeofday(&tv[1], NULL);
    // apply admission control to current frame
    meanX = mean(accX,len);
    meanY = mean(accY,len);
    meanZ = mean(accZ,len);
    //meanNorm = sqrt(meanX*meanX+meanY*meanY+meanZ*meanZ);
    diffNorm = sqrt(pow(meanX - oldMeanX,2)+pow(meanY - oldMeanY,2)+pow(meanZ - oldMeanZ,2));
    
    //debug    
    PROCACC(
    for(int i = 0; i < len*3;i++){
	printf("%.12f,",*(buf+i));
    }
    printf("\nmean:%.12f,%.12f,%.12f,%.12f,%.12f,%.12f,%.12f\n\n",meanX,meanY,meanZ,oldMeanX,oldMeanY,oldMeanZ,diffNorm);	    
    )

    if (diffNorm > 0.2) { // sensor oritention changed too much
        unknown = 1; 
        counter = 0;    // reset the counter (i.e. # of admitted state so far)
	motion_state = -1;
        //cout<<" unknown"<<endl;
        // mark current state as unknown
        // fprintf(sFile,"%d ",-1);
    }
	//gettimeofday(&tv[2], NULL);
    
    if (counter > 0 && unknown != 1) {  // current state is NOT rejected
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
		//gettimeofday(&tv[3], NULL);
	

	//debug
	PROCACC(printf("%.12f,%.12f,%.12f\n",gX,gY,gZ);
	for(int i = 0;i<len;i++) {
                //cout<<fVector[i]<<",";
                printf("%.12f,",v[i]);
        }
		printf("\n\n");	 

	for(int i = 0;i<len;i++) {
                //cout<<fVector[i]<<",";
                printf("%.12f,",h[i]);
        }
		printf("\n\n");
	)

        double fVector[25];
        featExtract(v,h,fVector,len,sr);
        //cout<< "feature done "<<endl;
        
	//debug
	PROCACC(
	for(int i = 0;i<25;i++) {
        //cout<<fVector[i]<<",";
                printf("%f,",fVector[i]);
        }
	printf("\n");	 
	)

        //cout<<endl;
        //fprintf(sFile,"\n\n");
		//gettimeofday(&tv[4], NULL);
        //motion_state = accTree(fVector);   // classification on current frame
	motion_state = gaussian(fVector);	
	//printf("===========================gaussian says %d \n",gaussian(fVector));
		//gettimeofday(&tv[5], NULL);
    }
	//printf("\n============%d===============\n",motion_state);
	//for(int i = 0;i<6;i++){printf("%d,",(tv[i].tv_sec-tv[0].tv_sec)*1000000 + tv[i].tv_usec - tv[0].tv_usec);}printf(";\n");
    oldMeanX = meanX;
    oldMeanY = meanY;
    oldMeanZ = meanZ;
    callBack(motion_state);
    return motion_state;

}

