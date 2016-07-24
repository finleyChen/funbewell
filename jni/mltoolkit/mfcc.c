#include <stdio.h>
#include <stdlib.h>
#include <math.h>
#include <assert.h>
#include "mfcc.h"
//#include <vector>
//using namespace std;


#define SAFE_DELETE(a)  if (a)  {     \
									free(a); \
									a = NULL; \
									}
// increase these numbers if assert failure occurs
#define mel_Length_MAX  41
#define FILTER_SIZE 500
#ifndef FLT_MIN
	#define FLT_MIN (1.0e-37)
#endif
#ifndef M_PI
    #define M_PI 3.141592653589793//2384626433832795
#endif

int mel_Length;
int* centers = NULL;
double* rNormalize = NULL;
double* iNormalize = NULL;
double* inputCopy = NULL;
double* outputCopy = NULL;
double* output = NULL;
double* Xr = NULL; double* Xi = NULL;
double* window = NULL;
double* input = NULL;
double filters[mel_Length_MAX][FILTER_SIZE];  // hope it is enough
int filterStart[FILTER_SIZE];
int filterSize[mel_Length_MAX];
int inputLength, psLength;
void initMfcc(int sample_rate, int mell, int high_freq, int low_freq, int inputlen) {
	//std::cout<<"aaaaa"<<endl;
	SAFE_DELETE(centers); 
	SAFE_DELETE(rNormalize);
	SAFE_DELETE(iNormalize);
	SAFE_DELETE(inputCopy); 
	SAFE_DELETE(outputCopy);
	SAFE_DELETE(output);
	SAFE_DELETE(Xr); SAFE_DELETE(Xi);
	SAFE_DELETE(window);
	SAFE_DELETE(input);
	
	inputLength = inputlen; window = (double*)malloc(sizeof(double)*inputLength);
	psLength = inputlen / 2;
	int i;
	double niquist = sample_rate / 2.0f;
    double highMel = 1000*log(1+high_freq/700.0f)/log(1+1000.0f/700);
    double lowMel = 1000*log(1+low_freq/700.0f)/log(1+1000.0f/700);
	mel_Length = mell;
	centers = (int*)malloc(sizeof(int) * (mel_Length + 2));
	for (i=0; i < mel_Length + 2; i++) {
		double melCenter = lowMel + i*(highMel-lowMel)/(mel_Length+1);
		centers[i] = (int) (floor(.5 + psLength*700*(exp(melCenter*log(1+1000.0/700.0)/1000)-1)/niquist));
	}
	//initialize windows
	for (i = 0; i < inputLength; i++)
		 window[i]=.54f-.46f*cos((2*M_PI*i)/inputLength);

	// initialize filters
	assert(mel_Length < mel_Length_MAX);
	for (i=0;i<mel_Length;i++) {
		filterStart[i] = centers[i]+1;
		filterSize[i] = (centers[i+2]-centers[i]-1);
		int freq, j;
		for (freq=centers[i]+1, j=0 ; freq<=centers[i+1]; freq++, j++)
		{
			assert(j < FILTER_SIZE);
			filters[i][j] = (freq-centers[i])/ (double)(centers[i+1]-centers[i]);
		}
		for (freq=centers[i+1]+1 ; freq < centers[i+2] ; freq++, j++)
		{
			assert(j < FILTER_SIZE);
			filters[i][j] = (centers[i+2]-freq)/ (double)(centers[i+2]-centers[i+1]);
		}
	}
	/*
	// test ////////////////
	for (i=0;i<mel_Length;i++) {
		 printf("%d,",filterStart[i]);
		 printf("%d,\n",filterSize[i]);
		//for (int j = 0;j<70;j++) printf("%f,",filters[i][j]);
		//printf("\n");
	}
	printf("\n");
	*/
	//////////////////////////////////
	inputCopy  = (double*)malloc(sizeof(double)*mel_Length);
	outputCopy = (double*)malloc(sizeof(double)*mel_Length);
	output = (double*)malloc(sizeof(double)*mel_Length);
	rNormalize = (double*)malloc(sizeof(double)*mel_Length);
	iNormalize = (double*)malloc(sizeof(double)*mel_Length);
	Xr = (double*)malloc(sizeof(double)*mel_Length);
	Xi = (double*)malloc(sizeof(double)*mel_Length);
	input = (double*)malloc(sizeof(double)*inputLength);
	double sqrt2n=sqrt(2.0f/inputLength);
	for (int i=0;i<mel_Length;i++)
	{
		rNormalize[i]=cos(M_PI*i/(2*mel_Length))*sqrt2n;
		iNormalize[i]=-sin(M_PI*i/(2*mel_Length))*sqrt2n;
	}
	rNormalize[0] /= sqrt(2.0);
	/*// test only /////////////////////////
	for (int i=0;i<mel_Length;i++)
	{
		printf("%f,",rNormalize[i]);
	}
	printf("\n");
	for (int i=0;i<mel_Length;i++)
	{
		printf("%f,",iNormalize[i]);
	}
	printf("\n");
	////////////////////////////////
	*/
}


void delMfcc() {
	SAFE_DELETE(centers); 
	SAFE_DELETE(rNormalize);
	SAFE_DELETE(iNormalize);
	SAFE_DELETE(inputCopy); 
	SAFE_DELETE(outputCopy);
	SAFE_DELETE(output);
	SAFE_DELETE(Xr); SAFE_DELETE(Xi);
	SAFE_DELETE(window);
	SAFE_DELETE(input);

}


// data the real fft results from the outsource
// count size of data
double* calculateMfcc(double* data)
{
	int i,j;
	for (i = 0; i < inputLength; i ++)
		input[i] = data[i]; // * window[i];
	double* tmpBuffer1 = (double*)malloc(sizeof(double) * mel_Length);      
	int nbFilters = mel_Length;
	for (i = 0 ; i < nbFilters ; i++)
	{
		int j;
		tmpBuffer1[i]=0;
		int filterSizee = filterSize[i];
		int filtStart = filterStart[i];
		for (j=0;j<filterSizee;j++)
		{
			tmpBuffer1[i] += filters[i][j]*input[j+filtStart];
		}
	}
	/*///////////test
	for (int i=0;i<mel_Length;i++)
	{
		printf("%f,",tmpBuffer1[i]);
	}
	printf("\n");
	*//////////////////////////

	for (i=0, j=0 ;i<mel_Length ; i+=2, j++){
		inputCopy[j]=log(tmpBuffer1[i]+FLT_MIN);
		//printf("%d,%d\n",i,j);
	}
	for (i = mel_Length-1; i>=0 ; i-=2, j++){
		inputCopy[j]=log(tmpBuffer1[i]+FLT_MIN);
	//printf("%d,%d\n",i,j);
	}
	double wr; double wi; double w; int n, m;
	for (n=0; n<mel_Length; n++) {
	    Xr[n]=Xi[n]=0;
	    for (m=0; m<mel_Length; m++) {
	      w=2*M_PI*m*n/mel_Length;
	      if (1/*forward*/) w=-w;
	      wr=cos(w); wi=sin(w);
	      Xr[n]=Xr[n]+wr*inputCopy[m];
	      Xi[n]=Xi[n]+wi*inputCopy[m];
	    }
	    Xr[n]=Xr[n]/sqrt((double)mel_Length);
	    Xi[n]=Xi[n]/sqrt((double)mel_Length);
	 }



	for (i = 0; i < mel_Length; i++)
		if (i <= mel_Length / 2)
			outputCopy[i] = Xr[i];
		else
			outputCopy[i] = Xi[i];	
	
	for (i=1;i<mel_Length/2;i++)	{
		output[i]=rNormalize[i]*outputCopy[i] - iNormalize[i]*outputCopy[mel_Length-i];
		output[mel_Length-i]=rNormalize[mel_Length-i]*outputCopy[i] + iNormalize[mel_Length-i]*outputCopy[mel_Length-i];
	}

	output[0]=outputCopy[0]*rNormalize[0];
	output[mel_Length/2] = outputCopy[mel_Length/2]*rNormalize[mel_Length/2];
	free(tmpBuffer1);
	return output;
}

