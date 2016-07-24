/*
 *  buffer.c
 *  MLToolKit
 *
 *  Created by hong on 2/16/10.
 *  Copyright 2010 __MyCompanyName__. All rights reserved.
 *
 */

#include "buffer.h"
#include <stdlib.h>
#include <stdio.h>


void initBuffer(dataBuffer * buf,int bufferSize, int bufferNumber){
	buf->bufferNum = bufferNumber;
	buf->buffer = (void*)malloc(bufferNumber*bufferSize);
	buf->buffers = (void**)malloc(bufferNumber*sizeof(void*)); 
	for(int i=0;i<bufferNumber;i++){
		buf->buffers[i] = buf->buffer + i * bufferSize;
		//printf("%u,\n",buf->buffers[i]);
		//buf->status[i] = 1;
	}
	buf->write = 0;
	pthread_mutex_init(&(buf->read_lock), NULL);
	buf->read = 0;
}

void delBuffer(dataBuffer * buf){
	free(buf->buffer);
	free(buf->buffers);
	pthread_mutex_destroy(&(buf->read_lock)); 
}

int nextWrite(dataBuffer * buf){
	pthread_mutex_lock(&(buf->read_lock));
	if ((buf->write+1)%buf->bufferNum == buf->read) {
		// buffer full
		// buf->write = (buf->read + 1)%buf->bufferNum;
		pthread_mutex_unlock(&(buf->read_lock));
		return 0;  // buffer full
	}
	else{
		buf->write = (buf->write + 1)%buf->bufferNum;
		pthread_mutex_unlock(&(buf->read_lock));
		//printf("next buffer %d\n",buf->write);
		return 1;  // normal
	}
}

//the caller should hold the lock
int nextRead(dataBuffer * buf){
	buf->read = (buf->read + 1)%buf->bufferNum;
	return 0;
}

/*
//databuffer.read can be the buffer index(0~bufferNum) which is read by somebody
//or -1 when nobody is reading at all.
void setRead(dataBuffer * buf,int index){
	buf->read = index;
}
*/
