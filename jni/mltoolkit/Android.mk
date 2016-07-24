LOCAL_PATH:=$(call my-dir)

include $(CLEAR_VARS)
LOCAL_CFLAGS:= -Wall -std=c99 -O2 -march=armv6 -marm -mfloat-abi=softfp -mfpu=vfp
LOCAL_CPPFLAGS:= -Wall -O2 -march=armv6 -marm -mfloat-abi=softfp -mfpu=vfp
LOCAL_MODULE:= mltoolkit
LOCAL_SRC_FILES:= MLToolKit.c features.c feature_acc.c kiss_fft.c kiss_fftr.c classifier.c mvnpdf.c mfcc.c feature_audio.c
LOCAL_LDLIBS := -lm -lc -llog 
include $(BUILD_SHARED_LIBRARY)
