#include <jni.h>
//#include <JNIHelp.h>
//#include <android_runtime/AndroidRuntime.h>

#include <stdlib.h>
#include <stdio.h>
#include <pthread.h>
#include <fcntl.h>
#include <semaphore.h>
#include <time.h>
#include <string.h>
#include <sys/stat.h>
#include "features.h"
#include "mfcc.h"
#include "procAudio.h"
#include <android/log.h>
#include "MLToolKit.h"
#include <unistd.h>
#include <dirent.h>
#include <assert.h>

//#include "wholething.c"

#define LOG_TAG "XY_JNI"

#define print(...) __android_log_print(ANDROID_LOG_DEBUG  , LOG_TAG, __VA_ARGS__) 
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR  , LOG_TAG, __VA_ARGS__) 
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG  , LOG_TAG, __VA_ARGS__) 

static JavaVM *gJavaVM;
static jobject gInterfaceObject;
const char *kInterfacePath = "edu/dartmouthcs/sensorlab/MLToolkitInterface";

jint
JNI_OnLoad(JavaVM *vm, void* reserved) {
  LOGD("JNI_OnLoad called.\n");

  gJavaVM = vm;
  JNIEnv *env = NULL;
  if((*vm)->GetEnv(vm, (void**)&env, JNI_VERSION_1_6)!=JNI_OK){
    LOGE("failed to get the environment using GetEnv()");
    return -1;
  }

  //class instance caching
  jclass cls = (*env)->FindClass(env, kInterfacePath);
  if(!cls){
    LOGE("initClass: failed to get %s class reference", kInterfacePath);
    return -1;
  }
  jmethodID constr = (*env)->GetMethodID(env, cls, "<init>", "()V");
  if(!constr){
    LOGE("initClass: failed to get %s constructor", kInterfacePath);
    return -1;
  }
  jobject localObj  = (*env)->NewObject(env, cls, constr);
  if(!localObj){
    LOGE("initClass: failed to create  %s object", kInterfacePath);
    return -1;
  }
  gInterfaceObject = (*env)->NewGlobalRef(env, localObj);

  //native fucntion registeration 

/*  JNINativeMethod methods[]={
    {
      "stringFromJNI",
      "()V",
      (void *) java_edu_dartmouthcs_sensorlab_mywrapper_stringfromjni
    }
  };
  
*/
  LOGD("Leaving JNI_OnLoad");  
  return JNI_VERSION_1_6;
}

void
Java_edu_dartmouthcs_sensorlab_MLToolkitInterface_destroy(JNIEnv* env, jobject this) {
  destroy();
}


void
Java_edu_dartmouthcs_sensorlab_MLToolkitInterface_getAccSample(JNIEnv* env, jobject this, jdouble x, jdouble y, jdouble z){
  //  char str[100];
  /*  double a = x;
    double b = y;
    double c = z;
    sprintf(str, "%f %f %f\n", a,b,c);
    print(str);
  */
  //LOGD("getAccSample called");
  getAccSample(x,y,z);
}

void
Java_edu_dartmouthcs_sensorlab_MLToolkitInterface_getAudioSample(JNIEnv* env, jobject this, jshortArray data){
  //LOGD("getAudioSample called");
  int len = (*env)->GetArrayLength(env, data);
  jboolean iscopy;
  jshort* pdata = (*env)->GetShortArrayElements(env, data, &iscopy); //is copy?
  getAudioSample(pdata, len);
  (*env)->ReleaseShortArrayElements(env, data, pdata, JNI_ABORT);
}

void accelActCallBack(int act){
  //LOGD("Entering callback_handler");
  jint jval = act;

  int status;
  JNIEnv *env;
  int isAttached = 0;

  status = (*gJavaVM)->GetEnv(gJavaVM, (void**)&env, JNI_VERSION_1_6);
  if (status < 0){
    //LOGE("callback_handler: failed to get JNI env, assuming native thread");
    status = (*gJavaVM)->AttachCurrentThread(gJavaVM, &env, NULL);
    if(status<0){
      LOGE("callback_handler: failed to attach current thread");
      return;
    }
    isAttached = 1;
  }

  jclass interfaceClass = (*env)->GetObjectClass(env, gInterfaceObject);
  if(!interfaceClass){
    LOGE("callback_handler: failed to get class reference");
    if(isAttached) (*gJavaVM)->DetachCurrentThread(gJavaVM);
    return;
  }

  jmethodID method = (*env)->GetStaticMethodID(env, interfaceClass, "accelActCallBack","(I)V");
  if(!method){
    LOGE("callback_handler: failed to get method ID");
    if(isAttached) (*gJavaVM)->DetachCurrentThread(gJavaVM);
  }
  (*env)->CallStaticVoidMethod(env, interfaceClass, method,jval);
  if(isAttached) (*gJavaVM)->DetachCurrentThread(gJavaVM);

  //LOGD("Leaving callback_handler");
}

void audioActCallBack(int act){
  //LOGD("Entering callback_handler");
  jint jval = act;

  int status;
  JNIEnv *env;
  int isAttached = 0;

  status = (*gJavaVM)->GetEnv(gJavaVM, (void**)&env, JNI_VERSION_1_6);
  if (status < 0){
    //LOGE("callback_handler: failed to get JNI env, assuming native thread");
    status = (*gJavaVM)->AttachCurrentThread(gJavaVM, &env, NULL);
    if(status<0){
      LOGE("callback_handler: failed to attach current thread");
      return;
    }
    isAttached = 1;
  }

  jclass interfaceClass = (*env)->GetObjectClass(env, gInterfaceObject);
  if(!interfaceClass){
    LOGE("callback_handler: failed to get class reference");
    if(isAttached) (*gJavaVM)->DetachCurrentThread(gJavaVM);
    return;
  }

  jmethodID method = (*env)->GetStaticMethodID(env, interfaceClass, "audioActCallBack","(I)V");
  if(!method){
    LOGE("callback_handler: failed to get method ID");
    if(isAttached) (*gJavaVM)->DetachCurrentThread(gJavaVM);
  }
  (*env)->CallStaticVoidMethod(env, interfaceClass, method,jval);
  if(isAttached) (*gJavaVM)->DetachCurrentThread(gJavaVM);

  //LOGD("Leaving callback_handler");
}

jint
Java_edu_dartmouthcs_sensorlab_MLToolkitInterface_init(JNIEnv* env, jobject this, jobject jacc_Cfg, jobject jaudio_Cfg, jobject jloc_Cfg) {

  jclass cls;
  jfieldID fid;

  acc_config* ac_Cfg, ac;
  audio_config* au_Cfg, au;
  loc_config* lo_Cfg, lo;

  

  if (jacc_Cfg == NULL){
    LOGD("acc is NULL \n");
    ac_Cfg = NULL;
  }
  else{
    LOGD("acc is not NULL \n");
    ac_Cfg = &ac;
    cls = (*env)->GetObjectClass(env, jacc_Cfg);
    fid = (*env)->GetFieldID(env, cls, "samplingRate", "I");
    if (fid==0){
      LOGE("can't find datafield\n");
      return 0;
    }
    jint acc_samplingRate = (*env)->GetIntField(env, jacc_Cfg, fid);
    fid = (*env)->GetFieldID(env, cls, "frameLength", "I");
    if (fid==0){
      LOGE("can't find datafield\n");
      return 0;
    }
    jint acc_frameLength = (*env)->GetIntField(env, jacc_Cfg, fid);
    ac_Cfg->samplingRate = acc_samplingRate;
    ac_Cfg->frameLength = acc_frameLength;
    ac_Cfg->callBack = accelActCallBack;
  }

  if(jaudio_Cfg == NULL){
    LOGD("audio is NULL \n");
    au_Cfg =NULL;
  }else{
    au_Cfg = &au;
    LOGD("audio is not NULL \n");

    cls = (*env)->GetObjectClass(env, jaudio_Cfg);
    fid = (*env)->GetFieldID(env, cls, "samplingRate", "I");
    if (fid==0){
      LOGE("can't find datafield\n");
      return 0;
    }
    jint audio_samplingRate = (*env)->GetIntField(env, jaudio_Cfg, fid);
    fid = (*env)->GetFieldID(env, cls, "frameLength", "I");
    if (fid==0){
      LOGE("can't find datafield\n");
      return 0;
    }
    jint audio_frameLength = (*env)->GetIntField(env, jaudio_Cfg, fid);

    fid = (*env)->GetFieldID(env, cls, "windowLength", "I");
    if (fid==0){
      LOGE("can't find datafield\n");
      return 0;
    }
    jint audio_windowLength = (*env)->GetIntField(env, jaudio_Cfg, fid);
    fid = (*env)->GetFieldID(env, cls, "mfccLength", "I");
    if (fid==0){
      LOGE("can't find datafield\n");
      return 0;
    }
    jint audio_mfccLength = (*env)->GetIntField(env, jaudio_Cfg, fid);
    au_Cfg->samplingRate = audio_samplingRate;
    au_Cfg->frameLength = audio_frameLength;
    au_Cfg->windowLength = 20;
    au_Cfg->mfccLength = 14;
    au_Cfg->callBack = audioActCallBack;
  }

  if(jloc_Cfg == NULL){
    LOGD("location is NULL\n");
    lo_Cfg = NULL;
  } else{
    LOGD("location is not NULL\n");
    lo_Cfg = &lo;
    cls = (*env)->GetObjectClass(env, jloc_Cfg);
    fid = (*env)->GetFieldID(env, cls, "samplingRate", "I");
    if (fid==0){
      LOGE("can't find datafield\n");
      return 0;
    }
    jint loc_samplingRate = (*env)->GetIntField(env, jloc_Cfg, fid);
    lo_Cfg->samplingRate = loc_samplingRate; 
    //lo_Cfg->callBack = ???;
  }



  init(ac_Cfg, au_Cfg, lo_Cfg);

  LOGD("Processing thread initiated");

}

