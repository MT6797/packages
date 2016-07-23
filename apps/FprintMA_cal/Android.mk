LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)


LOCAL_STATIC_JAVA_LIBRARIES := android-support-v4 \
                               android-support-v13 \

LOCAL_MODULE_TAGS := optional

LOCAL_SRC_FILES := \
        $(call all-java-files-under, src) 

LOCAL_RESOURCE_DIR := $(LOCAL_PATH)/res

LOCAL_PACKAGE_NAME := FprintMACal
LOCAL_CERTIFICATE := platform
LOCAL_PRIVILEGED_MODULE := true

LOCAL_PROGUARD_FLAG_FILES := proguard.flags
LOCAL_PROGUARD_ENABLED := disabled

LOCAL_DEX_PREOPT := false 

include $(BUILD_PACKAGE)
