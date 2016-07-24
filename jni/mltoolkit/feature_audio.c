/*
 *  MLToolKit
 *
 *  Created by Hong Lu on 07/07/2011.
 *
 */


#include "kiss_fftr.h"
#include <math.h>
#include "mfcc.h"
#include "classifier.h"
#include "feature_audio.h"
#include "features.h"
#include <android/log.h>
#define LOG_TAG "mltoolkit"
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR  , LOG_TAG, __VA_ARGS__) 
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG  , LOG_TAG, __VA_ARGS__)
#define DEBUG 0

#define MAX_FFT 2048

void getFrameFeature( short * data, double * hanning,int frameIndex, int frame_length, int fft_length, int mfcc_length, double * feature) {
	///////////////////////////////////////////////////////	
	double sample[frame_length];
	kiss_fft_cpx freq[fft_length];
	double fft[fft_length]; 
    double energySpec[fft_length];
	double sumfft = 0;
	double sumEnerySpec = 0;
	double rms = 0;
	double specEntropy = 0;
	double specFlux = 0;
	double specCentroid = 0;
	double rolloff = 0;
	double bandwidth = 0;
	//use a huge number here.
	static double lastfft[MAX_FFT];
	//benchmark timer
	//time_t timeStamp[10];
	//struct timeval tv[10];	
		//timeStamp[0] = clock();
		//gettimeofday(&tv[0], NULL);
		//printf("audio processing %d,%d\n",frame_length,data[511]);     
		for(int i=0;i<frame_length;i++){
			//if(frameIndex < 5)printf("%d,",*(s+i));
			//printf("%d,",*(s+i));
			sample[i] = (*(data+i))*hanning[i];
			rms += (*(data+i))*(*(data+i));
		}
		feature[0] = sqrt(rms/frame_length);
                //printf("rms %f,",rms_history[frameIndex % window_length]);
		kiss_fftr_cfg cfg = kiss_fftr_alloc(frame_length,0, NULL, NULL);
		kiss_fftr(cfg, sample, freq);
		free(cfg);
		//********************** discard dc, i start from 1 ********************************
		for(int i = 0;i<fft_length;i++){
			energySpec[i] = (double)(freq[i].i*freq[i].i+freq[i].r*freq[i].r);
			fft[i] = sqrt(energySpec[i]);
			if(fft[i] == 0){
				fft[i] = 0.00000000000000000001;
			}
			if (i>=1){
				sumfft += fft[i];
				sumEnerySpec += energySpec[i];
			}
		}
		/*
		 for(int i=0;i<fft_length;i++){
		 if(DEBUG) printf("%f,",energySpec[i]);
		 }
		 if(DEBUG) printf("\n");		
		 */
		//////////////////////////// mfcc calculation ////////////////////////////////////		
		//gettimeofday(&tv[2], NULL);
		double* mf = calculateMfcc(energySpec);
		memcpy(feature+6, mf, sizeof(double)*mfcc_length);
		//gettimeofday(&tv[3], NULL);
		 /*
		 for(int i = 0;i<mfcc_length;i++){
		 printf("%f,",mf[i]);
		 }
		 printf("\n");
		*/
		//fft[],and spectralenenry[] are normalized
		///////////////////////////////////////////////////////////////////////////////////
		
		for(int i=1;i<fft_length;i++){
			fft[i] = fft[i]/sumfft;
			energySpec[i] = energySpec[i]/sumEnerySpec;
		}
		//reuse sumfft for roll off
		sumfft = 0;
		for(int i=1;i<fft_length;i++){
			sumfft += fft[i];
			if( sumfft >= 0.93 ){
				rolloff = i;
				feature[1] = rolloff;
				break;
			}
		}
		
		if(frameIndex>0){
			for(int i=1;i<fft_length;i++){
				specEntropy += fft[i]*log(fft[i]/lastfft[i]);
				specFlux += pow(fft[i]-lastfft[i],2);
				specCentroid += i*energySpec[i];
			}
			for(int i=1;i<fft_length;i++){
				bandwidth += (i - specCentroid)*(i- specCentroid)*energySpec[i];
			}
			feature[2] = specEntropy;
			feature[3] = specFlux;
			feature[4] = specCentroid;
			bandwidth = sqrt(bandwidth);
			feature[5] = bandwidth;
			//if(DEBUG) printf("%f,%f\n",specFlux,specCentroid);
		}
		
		
		//keep history
		if( frameIndex==0 ){
			for(int i = 1;i<fft_length;i++){
				lastfft[i] = fft[i];
			}
		}else{
			for(int i=1;i<fft_length;i++){
				lastfft[i] = fft[i]*0.05 +lastfft[i]*0.95;
			}
		}
                //printf("rms %f\n",rms_history[frameIndex % window_length]);

		//timeStamp[4] = clock();
		//gettimeofday(&tv[4], NULL);
		//right now step can only be 1/2 window or window.
}


void getWindowFeature(int window_length, int frameIndex, int mfcc_length, double * se_history, double * sf_history, double * sc_history, double * ro_history, double * bw_history, double * mfcc_history, double * rms_history, double * feature){
			double semean = 0;
			double sev = 0;
			double sfmean = 0;
			double sfv = 0;
			double scmean = 0;
			double scv = 0;
			double romean = 0;
			double rov = 0;
			double bwmean = 0;
			double bwv = 0;
			double rmsmean = 0;
			double rmsv = 0;
			double lowenergy = 0;
			double pr[9];
			//double entmean = 0;
			//double entv = 0;
			int result = 0;
			//maxTuple m;
			//printf("enter-------------\n", rmsmean,rmsv);			
			variance(rms_history,window_length, &rmsmean,&rmsv);
			//printf("%f,%f,slience\n", rmsmean,rmsv);
			variance(se_history, window_length, &semean, &sev);
			variance(sf_history, window_length, &sfmean, &sfv);
			variance(sc_history, window_length, &scmean, &scv);
			variance(ro_history, window_length, &romean, &rov);
			variance(bw_history, window_length, &bwmean, &bwv);
			
			/*
			for(int i=0; i<window_length; i++){
				if(DEBUG) LOGD("se: %f", se_history[i]*10e17);				
			}
  			if(DEBUG) LOGD("se: %f,%f", semean*10e17,sev*10e17);
			*/
			double tmp =  rmsmean*0.5;
			for(int i=0;i<window_length;i++){
				if(rms_history[i]<=tmp)lowenergy++;
			}
			
			feature[0] = semean;feature[1] = sev; feature[2] = sfmean;feature[3] = sfv; 
			feature[4] = romean;feature[5] = rov; feature[6] = scmean; feature[7] = scv; 
			feature[8] = bwmean; feature[9] = bwv; feature[10] = lowenergy;
			//timeStamp[5] = clock();
			//gettimeofday(&tv[5], NULL);
			memcpy(feature+11, mfcc_history + (frameIndex % window_length)*mfcc_length + 1, sizeof(double)*(mfcc_length-1));
			feature[24] = rmsmean;
}
