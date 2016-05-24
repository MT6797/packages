LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

# We only want this apk build for tests.
LOCAL_MODULE_TAGS := tests
LOCAL_CERTIFICATE := shared

LOCAL_JAVA_LIBRARIES := android.test.runner

# M: add robotium lib for mediatek's case
LOCAL_STATIC_JAVA_LIBRARIES := librobotium4

# Include all test java files.
LOCAL_SRC_FILES := $(call all-java-files-under, src)

src_dirs := src \
    ../../ContactsCommon/TestCommon/src

# Include all test java files.
LOCAL_SRC_FILES := $(call all-java-files-under, $(src_dirs))

LOCAL_PACKAGE_NAME := DialerTests

LOCAL_INSTRUMENTATION_FOR := Dialer

include $(BUILD_PACKAGE)
