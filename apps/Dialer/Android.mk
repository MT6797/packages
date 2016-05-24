LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional

incallui_dir := ../InCallUI
contacts_common_dir := ../ContactsCommon
phone_common_dir := ../PhoneCommon

src_dirs := src \
    $(incallui_dir)/src \
    $(contacts_common_dir)/src \
    $(phone_common_dir)/src

# M: Add ContactsCommon ext
src_dirs += $(contacts_common_dir)/ext

res_dirs := res \
    $(incallui_dir)/res \
    $(contacts_common_dir)/res \
    $(phone_common_dir)/res

# M: Add ext resources
res_dirs += res_ext

# M: Add ContactsCommon ext resources
res_dirs += $(contacts_common_dir)/res_ext

# M: [InCallUI]additional res
res_dirs += $(incallui_dir)/res_ext
# M: [InCallUI]needed by AddMemberEditView who extends MTKRecipientEditTextView
# M: [InCallUI]FIXME: should replace this with google default RecipientEditTextView
res_dirs += ../../../frameworks/ex/chips/res

LOCAL_SRC_FILES := $(call all-java-files-under, $(src_dirs))
LOCAL_RESOURCE_DIR := $(addprefix $(LOCAL_PATH)/, $(res_dirs)) \
    frameworks/support/v7/cardview/res frameworks/support/v7/recyclerview/res

# M: [InCallUI]added com.android.mtkex.chips for MTKRecipientEditTextView
# M: [InCallUI]FIXME: should replace this with google default RecipientEditTextView
LOCAL_AAPT_FLAGS := \
    --auto-add-overlay \
    --extra-packages android.support.v7.cardview \
    --extra-packages android.support.v7.recyclerview \
    --extra-packages com.android.incallui \
    --extra-packages com.android.contacts.common \
    --extra-packages com.android.phone.common \
    --extra-packages com.android.mtkex.chips

LOCAL_JAVA_LIBRARIES := telephony-common ims-common

# M: [InCallUI]additional libraries
LOCAL_JAVA_LIBRARIES += mediatek-framework
# M: Add for ContactsCommon
LOCAL_JAVA_LIBRARIES += voip-common

LOCAL_STATIC_JAVA_LIBRARIES := \
    android-common \
    android-support-v13 \
    android-support-v4 \
    android-support-v7-cardview \
    android-support-v7-recyclerview \
    com.android.services.telephony.common \
    com.android.vcard \
    guava \
    libphonenumber

# M: add mtk-ex
LOCAL_STATIC_JAVA_LIBRARIES += com.mediatek.dialer.ext

# M: add for WFC support
LOCAL_STATIC_JAVA_LIBRARIES += wfo-common

# M: add for mtk-tatf case
LOCAL_STATIC_JAVA_LIBRARIES += com.mediatek.tatf.common

# M: [InCallUI]ext library
LOCAL_STATIC_JAVA_LIBRARIES += com.mediatek.incallui.ext
# M: [InCallUI]added for MTKRecipientEditTextView
# M: [InCallUI]FIXME: should replace this with google default RecipientEditTextView
LOCAL_STATIC_JAVA_LIBRARIES += android-common-chips

LOCAL_PACKAGE_NAME := Dialer
LOCAL_CERTIFICATE := shared
LOCAL_PRIVILEGED_MODULE := true

LOCAL_PROGUARD_FLAG_FILES := proguard.flags $(incallui_dir)/proguard.flags

# Uncomment the following line to build against the current SDK
# This is required for building an unbundled app.
# M: disable it for mediatek's internal function call.
#LOCAL_SDK_VERSION := current

include $(BUILD_PACKAGE)

# Use the following include to make our test apk.
include $(call all-makefiles-under,$(LOCAL_PATH))
