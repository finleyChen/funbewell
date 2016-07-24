#include <pthread.h>
#include <stdio.h>

void thprint(void *ptr){
  for(int i=0; i<3; i++){
    printf("I'm a thread, # %d\n", i);
  }
}


void callth(){
  pthread_t t1;
  int iret1 = pthread_create(&t1, NULL, thprint, NULL);

}

