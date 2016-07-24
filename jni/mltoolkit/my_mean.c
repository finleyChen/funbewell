#include "my_mean.h"
#include "features.h"

double mymean(){
  int i;
  double a[100];
  
  for(i=0;i<100;i++){
    a[i]=i;
  }
  double val = mean(a, 100);
  
  return val;
}
