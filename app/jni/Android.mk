# Path of the sources
LOCAL_PATH := $(call my-dir)

# The only real JNI libraries
include $(CLEAR_VARS)
LOCAL_LDLIBS := -llog  -lz
LOCAL_CFLAGS =  -DTARGET_ARCH_ABI=\"${TARGET_ARCH_ABI}\"
LOCAL_SRC_FILES:= jniglue.c scan_ifs.c
LOCAL_MODULE = opvpnutil
include $(BUILD_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := main
LOCAL_LDLIBS   = -llog
LOCAL_CFLAGS    += -fPIE
LOCAL_LDFLAGS 	+= -fPIE -pie 
LOCAL_SRC_FILES := main.c



include $(BUILD_EXECUTABLE)
