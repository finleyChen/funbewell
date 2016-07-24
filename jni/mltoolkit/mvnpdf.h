/*
 *  MLToolKit
 *
 *  Created by Hong Lu on 07/07/2011.
 *
 */

#ifndef _MVNPDF_
#define _MVNPDF_
#ifndef M_PI
#define M_PI 3.14159265358979323846
#endif
#if __cplusplus
extern "C" 	{
#endif 
	// x:   of size dim*1
	// mean: the mean, of size dim*1
	// inv_sigma: inverse of SIGMA, a dim*dim matrix
	// deter_sigma: determinant of SIGMA
	// dim: # of dimensions of feature vector x
	double mvnpdf(double* s, const double* mean, const double* inv_sigma, const double log_deter_sigma, const int dim);	
	double mvnpdf_diagonal_simple(double* s, const double* mean, const double* sigma, const int dim) ;
	double mvnpdf_diagonal(double* x, const double* mean, const double* sigma, const int dim); 
#if __cplusplus
}
#endif



#endif
