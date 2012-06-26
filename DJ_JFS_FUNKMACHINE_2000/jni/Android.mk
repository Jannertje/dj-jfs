LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

OPENCV_CAMERA_MODULES:=off

OPENCV_LIB_TYPE:=STATIC

include /home/floris/Android/android-opencv2/OpenCV-2.4.1/share/opencv/OpenCV.mk

LOCAL_MODULE    := chessboard
LOCAL_SRC_FILES := chessboard.cpp orb.cpp
LOCAL_LDLIBS +=  -llog -ldl

include $(BUILD_SHARED_LIBRARY)	