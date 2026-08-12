LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE := fnainput
LOCAL_SRC_FILES := fnainput.c
LOCAL_LDLIBS := -llog
include $(BUILD_EXECUTABLE)
