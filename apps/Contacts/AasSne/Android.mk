LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional

LOCAL_SRC_FILES := $(call all-java-files-under, src)

LOCAL_PACKAGE_NAME := AasSne

LOCAL_CERTIFICATE := shared
LOCAL_APK_LIBRARIES := Contacts

LOCAL_JAVA_LIBRARIES += telephony-common

LOCAL_PROGUARD_ENABLED := disabled
LOCAL_JACK_ENABLED := disabled
# Put plugin apk together to specific folder
LOCAL_MODULE_PATH := $(PRODUCT_OUT)/system/plugin

include $(BUILD_PACKAGE)

# Include plug-in's makefile to automated generate .mpinfo
include vendor/mediatek/proprietary/frameworks/opt/plugin/mplugin.mk

