/*
 *  buffer.h
 *  MLToolKit
 *  
 *  A simple ring buffer,of buffers.
 *  single reader, single writer
 *
 *  Created by hong on 2/16/10.
 *  Copyright 2010 __MyCompanyName__. All rights reserved.
 *
 */
#ifndef _BUFFER_H_
#define _BUFFER_H_
#include <pthread.h>
//define bufferNum 3   //number of buffers
#ifdef __cplusplus
extern "C" {
#endif 

typedef struct{
	int bufferNum;
	int write;
	int read;
	//int status[bufferNum];
	void ** buffers;
	void * buffer;
	pthread_mutex_t read_lock;
} dataBuffer;

// bufferSize in byte
void initBuffer(dataBuffer * buf,int bufferSize, int bufferNumber);
void delBuffer(dataBuffer * buf);
int nextWrite(dataBuffer * buf);
//void setRead(dataBuffer * buf,int index);
int nextRead(dataBuffer * buf);

 #ifdef __cplusplus
 }
 #endif 


#endif // #ifndef _BUFFER_H_



