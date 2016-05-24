LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

# We only want this apk build for tests.
LOCAL_MODULE_TAGS := tests

LOCAL_STATIC_JAVA_LIBRARIES := mockito-target

LOCAL_JAVA_LIBRARIES := android.test.runner

# Only compile source java files in this apk.
LOCAL_SRC_FILES := $(call all-java-files-under, src)

LOCAL_PACKAGE_NAME := ContactsProviderTests

LOCAL_INSTRUMENTATION_FOR := ContactsProvider
LOCAL_CERTIFICATE := shared

LOCAL_PROGUARD_ENABLED := disabled

LOCAL_STATIC_JAVA_LIBRARIES := libatannotations_contactsprovider

include $(BUILD_PACKAGE)

include $(CLEAR_VARS)
LOCAL_MODULE_TAGS := optional

LOCAL_PREBUILT_STATIC_JAVA_LIBRARIES := \
        libatannotations_contactsprovider:mtkatannotations_contactsprovider.jar

include $(BUILD_MULTI_PREBUILT)