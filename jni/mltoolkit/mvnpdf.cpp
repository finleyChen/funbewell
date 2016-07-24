#include <stdio.h>
#include <stdlib.h>
#include <math.h>
#include <memory.h>
#include "mvnpdf.h"

double mvnpdf(double* s, const double* mean, const double* inv_sigma, const double
log_deter_sigma, const int dim)
{
double* temp = (double*)malloc(sizeof(double)*dim);
double* x = (double*)malloc(sizeof(double)*dim);
double result = 0;
int i,j;

for (i = 0; i < dim; i++)
{x[i] = s[i] - mean[i];}

for (i=0; i < dim; i++) {
temp[i] = 0;
for ( j = 0; j < dim; j++)
temp[i] += x[j]*inv_sigma[j*dim + i];
}

for (i = 0; i < dim; i++)
result += temp[i]*x[i];
//printf("result:%f\n",result);
result = (-0.5*result);
result -= log(pow((2*M_PI), dim/2.0))+log_deter_sigma/2;
free(temp);
free(x);
//printf("result:%f\n",result);
return result;


}

double mvnpdf_diagonal(double* x, const double* mean, const double* sigma, const int dim) {
		double* inv_sigma = (double*)malloc(dim*dim*sizeof(double));
		memset(inv_sigma, 0 , dim*dim*sizeof(double));
		double log_det_sigma = 0;
		for (int i = 0; i < dim; i++) {
			inv_sigma[i*dim + i] = 1/sigma[i];
			//printf("%f , %f\n",log_det_sigma,sigma[i]);
			log_det_sigma += log(sigma[i]);
					}
	/*
	    for(int i = 0; i < dim; i++ ){
			for (int j = 0; j < dim; j++) printf("%f",inv_sigma[i*dim+j]);
			printf("\n");
		}
	 */
		double result = mvnpdf(x, mean, inv_sigma, log_det_sigma, dim);
		free(inv_sigma); return result;
}


double mvnpdf_diagonal_simple(double* s, const double* mean, const double* sigma, const int dim) {
	double* inv_sigma = (double*)malloc(dim*sizeof(double));
	//memset(inv_sigma, 0 , dim*dim*sizeof(double));
	double* temp = (double*)malloc(sizeof(double)*dim);
	double* x = (double*)malloc(sizeof(double)*dim);
	double result = 0;
	int i;
	double log_det_sigma = 0;
	for (int i = 0; i < dim; i++) {
		inv_sigma[i] = 1/sigma[i];
		//printf("%f , %f\n",log_det_sigma,sigma[i]);
		log_det_sigma += log(sigma[i]);
	}
	/*
	 for(int i = 0; i < dim; i++ ){
	 for (int j = 0; j < dim; j++) printf("%f",inv_sigma[i*dim+j]);
	 printf("\n");
	 }
	 */
	//double result = mvnpdf(x, mean, inv_sigma, log_det_sigma, dim);
	
	for (i = 0; i < dim; i++)
	{x[i] = s[i] - mean[i];}
	
	for (i=0; i < dim; i++) {
		temp[i] = 0;
		temp[i] = x[i]*inv_sigma[i];
	}
	
	for (i = 0; i < dim; i++)
		result += temp[i]*x[i];
	//printf("result:%f\n",result);
	result = (-0.5*result);
	result -= log(pow((2*M_PI), dim/2.0))+log_det_sigma/2;
	free(temp);
	free(x);
	//printf("result:%f\n",result);
	free(inv_sigma);
	return result;
}


