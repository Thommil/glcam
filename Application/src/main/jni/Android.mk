LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

OPENCV_CAMERA_MODULES:=off
OPENCV_INSTALL_MODULES:=on

include $(LOCAL_PATH)/../../../../lib/OpenCV-android-sdk/sdk/native/jni/OpenCV.mk

LOCAL_MODULE := animals-go
LOCAL_SRC_FILES := cv-opencv.cpp
LOCAL_LDLIBS    := -llog

include $(BUILD_SHARED_LIBRARY)