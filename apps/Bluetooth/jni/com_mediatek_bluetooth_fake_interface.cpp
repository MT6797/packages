/*
 * This file is used to implement fake JNI function that declared
 * in java layer.
 * This is because we can use compile option to remove some code in C,
 * however java native function needs a default implementation.
 */

#include "com_android_bluetooth.h"
#ifdef MTK_BT_AVRCP_TG_1_5
#include "mtk_bt_rc.h"
#else
#include "hardware/bt_rc.h"
#endif
#include "android_runtime/AndroidRuntime.h"

namespace android {
// AVRCP1.5 media player selection feature default empty functions.
// These function's declaration is in AvrcpEnhance.java
#ifndef MTK_BT_AVRCP_TG_1_5
static void fakeAvrcpClassInitNative(JNIEnv* env, jclass clazz) {}
static void fakeAvrcpInitNative(JNIEnv *env, jobject object) {}
static void fakeAvrcpCleanupNative(JNIEnv *env, jobject object) {}
static jboolean fakeAvrcpRegisterNotificationRspAddrPlayerNative(JNIEnv *env,
                                                        jobject object,
                                                        jint type,
                                                        jshort playerId,
                                                        jshort uidCounter) {
    return JNI_FALSE;
}
static jboolean fakeAvrcpRegisterNotificationRspAvalPlayerNative(JNIEnv *env,
                                                        jobject object,
                                                        jint type) {
    return JNI_FALSE;
}
static jboolean fakeAvrcpSetAddressedPlayerRspNative(JNIEnv *env, jobject object,
                                            jint rspStatus) {
    return JNI_FALSE;
}
static jboolean fakeAvrcpGetFolderItemRspNative(JNIEnv *env, jobject object,
        jint rspStatus, jshort uidCounter, jshort itemNum,
                                       jobjectArray itemList) {
    return JNI_FALSE;
}
static jboolean fakeAvrcpGetTotalItemsNumRspNative(JNIEnv *env, jobject object,
                                          jint rspStatus, jshort uidCounter,
                                          jint num) {
    return JNI_FALSE;
}
static JNINativeMethod sFakeMethods[] = {
    {"classInitNative", "()V", (void *) fakeAvrcpClassInitNative},
    {"initNative", "()V", (void *) fakeAvrcpInitNative},
    {"cleanupNative", "()V", (void *) fakeAvrcpCleanupNative},
    {"registerNotificationRspAddrPlayerNative", "(ISS)Z",
     (void *) fakeAvrcpRegisterNotificationRspAddrPlayerNative},
    {"registerNotificationRspAvalPlayerNative", "(I)Z",
     (void *) fakeAvrcpRegisterNotificationRspAvalPlayerNative},
    {"setAddressedPlayerRspNative", "(I)Z",
     (void*) fakeAvrcpSetAddressedPlayerRspNative },
    {"getTotalItemsNumRspNative", "(ISI)Z",
     (void*) fakeAvrcpGetTotalItemsNumRspNative },
    {"getFolderItemRspNative", "(ISS[Ljava/lang/Object;)Z",
     (void*) fakeAvrcpGetFolderItemRspNative },
};

int register_com_mediatek_bluetooth_avrcp(JNIEnv* env) {
    return jniRegisterNativeMethods(env, "com/mediatek/bluetooth/avrcp/AvrcpEnhance",
                                    sFakeMethods, NELEM(sFakeMethods));
}
#endif
}  // namespace android
