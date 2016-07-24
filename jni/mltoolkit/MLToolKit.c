/*
 *  MLToolKit
 *
 *  Created by Hong Lu on 07/07/2011.
 *
 */

#include <jni.h>
#include <android/log.h>
#define LOG_TAG "mltoolkit"
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR  , LOG_TAG, __VA_ARGS__) 
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG  , LOG_TAG, __VA_ARGS__)

#include "feature_acc.h"
#include "mfcc.h"
#include "classifier.h"
#include "feature_audio.h"

#define DEBUG 0

JNIEXPORT double JNICALL Java_edu_dartmouth_cs_mltoolkit_processing_AccFeatureExtraction_test(JNIEnv* env, jobject this, jdoubleArray data){
	jdouble tmp = 0;
	if(DEBUG) LOGD("JNI called 1");
	int len = (*env)->GetArrayLength(env, data);
	if(DEBUG) LOGD("JNI called 2 + %d", len);
	jdouble * pdata =  (*env)->GetDoubleArrayElements(env, data, 0);
	for(int i =0 ; i<len; i++){
		tmp += pdata[i];
	}
	if(DEBUG) LOGD("JNI called 3 + %f", tmp);
	(*env)->ReleaseDoubleArrayElements(env,data, pdata, JNI_ABORT);
	return tmp;
}

JNIEXPORT void JNICALL Java_edu_dartmouth_cs_mltoolkit_processing_AccFeatureExtraction_testArray(JNIEnv* env, jobject this, jdoubleArray data, jdoubleArray feature){
	if(DEBUG) LOGD("JNI called 1");
	int len = (*env)->GetArrayLength(env, data);
	if(DEBUG) LOGD("JNI called 2 + %d", len);
	jdouble * pdata =  (*env)->GetDoubleArrayElements(env, data, 0);
	jdouble * pfeature =  (*env)->GetDoubleArrayElements(env, feature, 0);
	for(int i =0 ; i<len; i++){
		pfeature[i] = pdata[i];
	}
	(*env)->ReleaseDoubleArrayElements(env,data, pdata, JNI_ABORT);
	(*env)->ReleaseDoubleArrayElements(env,feature, pfeature, 0);	
	//if(DEBUG) LOGD("JNI called 3 + %f", tmp);
	return;
}

JNIEXPORT int JNICALL Java_edu_dartmouth_cs_mltoolkit_processing_AccFeatureExtraction_getAccFeature(JNIEnv* env, jobject this, jdoubleArray data_x, jdoubleArray data_y, jdoubleArray data_z, jint sr, jint len,jdoubleArray feature){
	if(DEBUG) LOGD("JNI: acc feature");
	jdouble * x =  (*env)->GetDoubleArrayElements(env, data_x, 0);
	jdouble * y =  (*env)->GetDoubleArrayElements(env, data_y, 0);
	jdouble * z =  (*env)->GetDoubleArrayElements(env, data_z, 0);
	jdouble * fVector =  (*env)->GetDoubleArrayElements(env, feature, 0);
	//if(DEBUG) LOGD("JNI called 3 + %f", tmp);
	jint status = getAccFeatures(x,y,z, len, sr,fVector);
	(*env)->ReleaseDoubleArrayElements(env,data_x, x, JNI_ABORT);
	(*env)->ReleaseDoubleArrayElements(env,data_y, y, JNI_ABORT);
	(*env)->ReleaseDoubleArrayElements(env,data_z, z, JNI_ABORT);
	(*env)->ReleaseDoubleArrayElements(env,feature, fVector, 0);
	return status;
}

JNIEXPORT int JNICALL Java_edu_dartmouth_cs_mltoolkit_processing_AccInference_gaussianAccClassifier(JNIEnv* env, jobject this, jdoubleArray feature){
	if(DEBUG) LOGD("JNI: acc classification");
	//int len = (*env)->GetArrayLength(env, feature);
	//if(DEBUG) LOGD("JNI called, feature length: %d", len);
	jdouble * pfeature =  (*env)->GetDoubleArrayElements(env, feature, 0);
	int result = acc_classifier_gaussian(pfeature);
	(*env)->ReleaseDoubleArrayElements(env,feature, pfeature, JNI_ABORT);	
	//if(DEBUG) LOGD("JNI called 3 + %f", tmp);
	return result;
}

JNIEXPORT void JNICALL Java_edu_dartmouth_cs_mltoolkit_processing_AudioFeatureExtraction_initMfcc(JNIEnv* env, jobject this, jint sample_rate, jint mell, jint high_freq, jint low_freq, jint inputlen){
	if(DEBUG) LOGD("JNI audio init mfcc");
	initMfcc(sample_rate, mell, high_freq, low_freq, inputlen);
	//if(DEBUG) LOGD("JNI called 3 + %f", tmp);
	return;
}

JNIEXPORT void JNICALL Java_edu_dartmouth_cs_mltoolkit_processing_AudioFeatureExtraction_delMfcc(JNIEnv* env, jobject this){
	if(DEBUG) LOGD("JNI audio del mfcc");
	delMfcc();
	//if(DEBUG) LOGD("JNI called 3 + %f", tmp);
	return;
}

JNIEXPORT void JNICALL Java_edu_dartmouth_cs_mltoolkit_processing_AudioFeatureExtraction_getFrameFeature(JNIEnv* env, jobject this, jshortArray data, jdoubleArray hanning,jint frameIndex, jint frame_length, jint fft_length, jint mfcc_length, jdoubleArray feature){
	if(DEBUG) LOGD("JNI audio feature");
	jshort * pdata =  (*env)->GetShortArrayElements(env, data, 0);
	jdouble * phanning =  (*env)->GetDoubleArrayElements(env, hanning, 0);
	jdouble * pfeature =  (*env)->GetDoubleArrayElements(env, feature, 0);
	getFrameFeature( pdata, phanning, frameIndex, frame_length, fft_length, mfcc_length, pfeature);
	(*env)->ReleaseShortArrayElements(env,data, pdata, JNI_ABORT);
	(*env)->ReleaseDoubleArrayElements(env,hanning, phanning, JNI_ABORT);	
	(*env)->ReleaseDoubleArrayElements(env,feature, pfeature, 0);
	//if(DEBUG) LOGD("JNI called 3 + %f", tmp);
	return;
}

JNIEXPORT void JNICALL Java_edu_dartmouth_cs_mltoolkit_processing_AudioFeatureExtraction_getWindowFeature(JNIEnv* env, jobject this, jint window_length, jint frameIndex, jint mfcc_length, jdoubleArray se_history, jdoubleArray sf_history, jdoubleArray sc_history, jdoubleArray ro_history, jdoubleArray bw_history, jdoubleArray mfcc_history, jdoubleArray rms_history, jdoubleArray feature){
	if(DEBUG) LOGD("JNI audio window feature");
	jdouble * ptr_se_history =  (*env)->GetDoubleArrayElements(env, se_history, 0);
	jdouble * ptr_sf_history =  (*env)->GetDoubleArrayElements(env, sf_history, 0);
	jdouble * ptr_sc_history =  (*env)->GetDoubleArrayElements(env, sc_history, 0);
	jdouble * ptr_ro_history =  (*env)->GetDoubleArrayElements(env, ro_history, 0);
	jdouble * ptr_bw_history =  (*env)->GetDoubleArrayElements(env, bw_history, 0);
	jdouble * ptr_mfcc_history =  (*env)->GetDoubleArrayElements(env, mfcc_history, 0);
	jdouble * ptr_rms_history =  (*env)->GetDoubleArrayElements(env, rms_history, 0);
	jdouble * ptr_feature =  (*env)->GetDoubleArrayElements(env, feature, 0);
	
	getWindowFeature(window_length, frameIndex, mfcc_length,  ptr_se_history,  ptr_sf_history,  ptr_sc_history,  ptr_ro_history,  ptr_bw_history,  ptr_mfcc_history,  ptr_rms_history,  ptr_feature);
	//if(DEBUG) LOGD("returned: %f,%f,%f,%f", ptr_feature[0],ptr_feature[1],ptr_feature[2],ptr_feature[3]);
	(*env)->ReleaseDoubleArrayElements(env, se_history,ptr_se_history , JNI_ABORT);
	(*env)->ReleaseDoubleArrayElements(env, sf_history,ptr_sf_history , JNI_ABORT);
	(*env)->ReleaseDoubleArrayElements(env, sc_history,ptr_sc_history , JNI_ABORT);
	(*env)->ReleaseDoubleArrayElements(env, ro_history,ptr_ro_history , JNI_ABORT);
	(*env)->ReleaseDoubleArrayElements(env, bw_history,ptr_bw_history , JNI_ABORT);
	(*env)->ReleaseDoubleArrayElements(env, mfcc_history,ptr_mfcc_history , JNI_ABORT);
	(*env)->ReleaseDoubleArrayElements(env, rms_history,ptr_rms_history , JNI_ABORT);
	(*env)->ReleaseDoubleArrayElements(env,feature, ptr_feature, 0);
	//if(DEBUG) LOGD("JNI called 3 + %f", tmp);
	return;
}