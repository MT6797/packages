#adupsfota
ifeq ($(strip $(ADUPS_FOTA_SUPPORT)), yes)
LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE := AdupsFotaReboot
LOCAL_MODULE_TAGS := optional
LOCAL_SRC_FILES := AdupsFotaReboot.apk
LOCAL_MODULE_CLASS := APPS
LOCAL_MODULE_SUFFIX := $(COMMON_ANDROID_PACKAGE_SUFFIX)
LOCAL_CERTIFICATE := platform
include $(BUILD_PREBUILT)
endif
