/*
 *  MLToolKit
 *
 *  Created by Hong Lu on 07/07/2011.
 *
 */

#include "features.h"
#include <math.h>
#include "kiss_fftr.h"

double mean(const double * data,int length){ 
	double sum = 0;
	int count = length;
	while( count-- )
	{
		sum += *data++;
	}
	//printf("%d,%d\n",count,c);
	return sum/length;
}

double meanInt(const int * data,int length){ 
	double sum = 0;
	int count = length;
	while( count-- )
	{
		sum += *data++;
	}
	return sum/length;
}


double var(double const * data, int length){
	double sum = 0;
	int count = length;
	double m;
	m = mean(data,length);
	while( count-- )
	{
		sum += pow(*data++ - m,2);
	}
	return sum/length;
}

//variance() normalizes the VAR by N-1 if N>1, where N is the sample size. Y is normalized by N, if N = 1 
void variance(double * data,int length,double * mean,double * variance){
	double tmp_mean = 0;
	double v = 0;
	double tmp = 0;
	for(int i =0;i<length;i++){
		tmp_mean += *(data+i);
	}
	tmp_mean = tmp_mean/length;
	*mean = tmp_mean;
	for(int i =0;i<length;i++){
		//if(DEBUG) printf("%f,",sf_history[i]);
		tmp =  *(data+i) - tmp_mean;
		v += tmp * tmp;
	}
	if (length>1){
	*variance = v/(length-1);
	} else{
	*variance = v/(length);
	}
}

double corr(double * data_1, double *data_2, int length){
	double mean_1,var_1,mean_2,var_2;
	double tmp = 0;
	variance(data_1,length,&mean_1,&var_1);
	variance(data_2,length,&mean_2,&var_2);
	for(int i = 0;i < length;i++){
		tmp+= (data_1[i]-mean_1)*(data_2[i]-mean_2);
	}
	if (length>1){
	tmp /= (length-1); //cov(X,Y)
	}else{
	tmp /= length;
	}	
	return tmp/sqrt(var_1*var_2);

}


double norm(double x,double y,double z){
    return sqrt(x*x+y*y+z*z);
}

int zcr(double * data,int length){
    int counter = 0;
    for(int i = 1; i < length; i++){
        if ( (*(data+i-1))*(*(data+i)) < 0 ) counter++; 
    }
    return counter;
}

int mcr(double * in,double mean,int len){
    int counter = 0;
    for(int i = 1; i < len; i++){
        if ( (*(in+i-1) - mean)*(*(in+i) - mean) < 0 ) counter++; 
    }
    return counter;
}

int spectrum(double * data,int length,double * spec){
	kiss_fft_cpx * freq;      //                   freq[WINDOW_SIZE/2+1];
	freq = (kiss_fft_cpx *)malloc(sizeof(kiss_fft_cpx)*(length/2+1));	
	kiss_fftr_cfg cfg=kiss_fftr_alloc(length,0,NULL,NULL);
	if (!freq || !cfg){
	return 1;
	}
	kiss_fftr(cfg,data,freq);
	for(int i = 0;i < length/2+1;i++){
		spec[i] = sqrt(pow(freq[i].r,2) + pow(freq[i].i,2));
	}
	free(cfg);
	free(freq);
	return 0;
}

