LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := tests

LOCAL_JAVA_LIBRARIES := android.test.runner

LOCAL_SRC_FILES := $(call all-java-files-under, src)

LOCAL_PACKAGE_NAME := CarrierConfigTests
LOCAL_CERTIFICATE := platform

LOCAL_INSTRUMENTATION_FOR := CarrierConfig

include $(BUILD_PACKAGE)
