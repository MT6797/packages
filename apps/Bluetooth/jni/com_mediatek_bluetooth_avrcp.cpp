#define LOG_TAG "BluetoothAvrcpServiceJni"

#define LOG_NDEBUG 0

#include "com_android_bluetooth.h"
#include "mtk_bt_rc.h"

#include "utils/Log.h"
#include "android_runtime/AndroidRuntime.h"

#include <string.h>

/**
 * Support Mediatek AVRCP Enhanced functions.
 * 1. Support Media Player selection feature. (2015.9)
 */
namespace android {
/** M: Add for media player selection*/
static jmethodID method_handleSetAddressedPlayer;
static jmethodID method_handleGetFolderItemsCmd;
static jmethodID method_handleGetTotalItemsNum;

static jobject mMtkCallbacksObj = NULL;

static JNIEnv *sMtkCallbackEnv = NULL;

// refer to function in com_android_bluetooth_avrcp.cpp
extern btrc_interface_t * getBluetoothAvrcpInterface();

static bool checkCallbackThread() {
    sMtkCallbackEnv = getCallbackEnv();

    JNIEnv* env = AndroidRuntime::getJNIEnv();
    if (sMtkCallbackEnv != env || sMtkCallbackEnv == NULL) return false;
    return true;
}

/** M: MediaPlayer selection feature*/
void btavrcp_set_addressed_player_callback(int player_id) {
    ALOGI("%s", __FUNCTION__);

    if (!checkCallbackThread()) {
        ALOGE("Callback: '%s' is not called on the correct thread", __FUNCTION__);
        return;
    }

    if (mMtkCallbacksObj) {
          sMtkCallbackEnv->CallVoidMethod(mMtkCallbacksObj, method_handleSetAddressedPlayer,
                      (jint) player_id);
    } else {
        ALOGE("%s: mMtkCallbacksObj is null", __FUNCTION__);
    }
   checkAndClearExceptionFromCallback(sMtkCallbackEnv, __FUNCTION__);
}

/** M: MediaPlayer selection feature*/
void btavrcp_get_folder_items_callback(
            btrc_get_folder_item_cmd_t *p_getfolder) {
    jintArray attrs;

    ALOGI("%s", __FUNCTION__);

    if (!checkCallbackThread()) {
        ALOGE("Callback: '%s' is not called on the correct thread", __FUNCTION__);
        return;
    }

    attrs = (jintArray) sMtkCallbackEnv->NewIntArray(BTRC_MAX_ELEM_ATTR_SIZE);
    if (!attrs) {
        ALOGE("Fail to new jintArray for attrs");
        checkAndClearExceptionFromCallback(sMtkCallbackEnv, __FUNCTION__);
        return;
    }
    sMtkCallbackEnv->SetIntArrayRegion(attrs, 0, BTRC_MAX_ELEM_ATTR_SIZE,
            (jint *) p_getfolder->attribute);

    if (mMtkCallbacksObj) {
        sMtkCallbackEnv->CallVoidMethod(mMtkCallbacksObj, method_handleGetFolderItemsCmd,
                (jint) p_getfolder->scope, (jint) p_getfolder->start_item,
                (jint) p_getfolder->end_item, (jint) p_getfolder->attr_count,
                attrs);
    } else {
        ALOGE("%s: mMtkCallbacksObj is null", __FUNCTION__);
    }
    checkAndClearExceptionFromCallback(sMtkCallbackEnv, __FUNCTION__);
    sMtkCallbackEnv->DeleteLocalRef(attrs);
}

/** M: MediaPlayer selection feature*/
void btavrcp_get_total_items_num_callback(btrc_browsable_scope_t scope) {
    ALOGI("%s", __FUNCTION__);

    if (!checkCallbackThread()) {
        ALOGE(
                "Callback: '%s' is not called on the correct thread", __FUNCTION__);
        return;
    }
    if (mMtkCallbacksObj) {
        sMtkCallbackEnv->CallVoidMethod(mMtkCallbacksObj, method_handleGetTotalItemsNum,
                (jint) scope);
    } else {
        ALOGE("%s: mMtkCallbacksObj is null", __FUNCTION__);
    }

    checkAndClearExceptionFromCallback(sMtkCallbackEnv, __FUNCTION__);
}

static void classInitNative(JNIEnv* env, jclass clazz) {
    /** M: Add for media player selection */
    method_handleSetAddressedPlayer = env->GetMethodID(clazz,
            "handleSetAddressedPlayer", "(I)V");
    method_handleGetFolderItemsCmd = env->GetMethodID(clazz,
            "handleGetFolderItemsCmd", "(IIII[I)V");
    method_handleGetTotalItemsNum = env->GetMethodID(clazz,
            "handleGetTotalItemsNum", "(I)V");
    ALOGI("%s: succeeds", __FUNCTION__);
}

static void initNative(JNIEnv *env, jobject object) {
    const bt_interface_t* btInf;
    bt_status_t status;

    if ((btInf = getBluetoothInterface()) == NULL) {
        ALOGE("Bluetooth module is not loaded");
        return;
    }

    if (mMtkCallbacksObj != NULL) {
         ALOGW("Cleaning up Enhanced Avrcp callback object");
         env->DeleteGlobalRef(mMtkCallbacksObj);
         mMtkCallbacksObj = NULL;
    }

    mMtkCallbacksObj = env->NewGlobalRef(object);
}

static void cleanupNative(JNIEnv *env, jobject object) {
    const bt_interface_t* btInf;

    if ((btInf = getBluetoothInterface()) == NULL) {
        ALOGE("Bluetooth module is not loaded");
        return;
    }

    if (mMtkCallbacksObj != NULL) {
        env->DeleteGlobalRef(mMtkCallbacksObj);
        mMtkCallbacksObj = NULL;
    }
}

/** M: MediaPlayer selection feature*/
static jboolean registerNotificationRspAddrPlayerNative(JNIEnv *env,
        jobject object, jint type, jshort playerId, jshort uidCounter) {
    bt_status_t status;
    btrc_register_notification_t param;
    const btrc_interface_t * avrcpInterface = getBluetoothAvrcpInterface();
    ALOGI("%s: avrcpInterface: %p, type:%d, playerId:%d", __FUNCTION__,
          avrcpInterface, type, playerId);
    if (!avrcpInterface)
        return JNI_FALSE;

    param.addr_player.player_id = (short)playerId;
    param.addr_player.uid_counter = (short)uidCounter;

    if ((status = avrcpInterface->register_notification_rsp(
            BTRC_EVT_ADDR_PLAYER_CHANGE, (btrc_notification_type_t) type, &param))
            != BT_STATUS_SUCCESS) {
        ALOGE("Failed register_notification_rsp play status, status: %d", status);
    }

    return (status == BT_STATUS_SUCCESS) ? JNI_TRUE : JNI_FALSE;
}

/** M: MediaPlayer selection feature*/
static jboolean registerNotificationRspAvalPlayerNative(JNIEnv *env,
        jobject object, jint type) {
    bt_status_t status;
    const btrc_interface_t * avrcpInterface = getBluetoothAvrcpInterface();
    ALOGI("%s: avrcpInterface: %p, type: %d", __FUNCTION__, avrcpInterface, type);
    if (!avrcpInterface)
        return JNI_FALSE;

    if ((status = avrcpInterface->register_notification_rsp(
            BTRC_EVT_AVAL_PLAYERS_CHANGE, (btrc_notification_type_t) type, NULL))
            != BT_STATUS_SUCCESS) {
        ALOGE("Failed register_notification_rsp play status, status: %d", status);
    }

    return (status == BT_STATUS_SUCCESS) ? JNI_TRUE : JNI_FALSE;
}

/** M: MediaPlayer selection feature*/
static jboolean setAddressedPlayerRspNative(JNIEnv *env, jobject object,
        jint rspStatus) {
    bt_status_t status;
    const btrc_interface_t * avrcpInterface = getBluetoothAvrcpInterface();
    ALOGI("%s: jint: %d, uint8_t: %2X", __FUNCTION__, rspStatus, (btrc_status_t) rspStatus);

    ALOGI("%s: avrcpInterface: %p", __FUNCTION__, avrcpInterface);
    if (!avrcpInterface)
        return JNI_FALSE;

    if ((status = avrcpInterface->set_addressed_player_rsp(
            (btrc_status_t) rspStatus)) != BT_STATUS_SUCCESS) {
        ALOGE("Failed set_addressed_player, status: %d", status);
    }

    return (status == BT_STATUS_SUCCESS) ? JNI_TRUE : JNI_FALSE;
}

/** M: MediaPlayer selection feature*/
static jboolean getFolderItemRspNative(JNIEnv *env, jobject object,
        jint rspStatus, jshort uidCounter, jshort itemNum,
        jobjectArray itemList) {
    bt_status_t status;
    jobject localObj;
    btrc_get_folder_item_rsp_t param;
    btrc_browsable_item_t *browsableItems = NULL;
    const btrc_interface_t * avrcpInterface = getBluetoothAvrcpInterface();
    ALOGI("%s: avrcpInterface: %p", __FUNCTION__, avrcpInterface);
    if (!avrcpInterface)
        return JNI_FALSE;

    param.rsp_status = (btrc_status_t) rspStatus;
    param.uid_counter = (uint16_t) uidCounter;
    param.item_num = (uint16_t) itemNum;

    if (itemNum > 0) {
        browsableItems = new btrc_browsable_item_t[itemNum];
        if (!browsableItems) {
            ALOGE("get_element_attr_rsp: not have enough memeory");
            return JNI_FALSE;
        }
        jclass classMediaPlayerFullName =
                env->FindClass(
                        "com/mediatek/bluetooth/avrcp/AvrcpEnhance$MediaPlayerFullName");
        jfieldID charsetField = env->GetFieldID(classMediaPlayerFullName,
                "mCharSetId", "S");
        jfieldID lengthField = env->GetFieldID(classMediaPlayerFullName,
                "mLength", "S");
        jfieldID nameStrField = env->GetFieldID(classMediaPlayerFullName,
                "mFullName", "[B");
        ALOGI("Complete find MediaPlayerFullName Class");
        jclass classMediaPlayerData =
                env->FindClass(
                        "com/mediatek/bluetooth/avrcp/AvrcpEnhance$MediaPlayerData");
        jfieldID playidField = env->GetFieldID(classMediaPlayerData,
                "mPlayerId", "S");
        jfieldID majortypeField = env->GetFieldID(classMediaPlayerData,
                "mMajorType", "B");
        jfieldID subtypeField = env->GetFieldID(classMediaPlayerData,
                "mSubType", "I");
        jfieldID playstatusField = env->GetFieldID(classMediaPlayerData,
                "mPlayStatus", "I");
        jfieldID featuresField = env->GetFieldID(classMediaPlayerData,
                "mFeatures", "[B");
        jfieldID fullnameField =
                env->GetFieldID(classMediaPlayerData, "mFullName",
                        "Lcom/mediatek/bluetooth/avrcp/AvrcpEnhance$MediaPlayerFullName;");
        ALOGI("Complete find MediaPlayerData class");
        jclass classMediaPlayerBrowsableData =
                        env->FindClass(
                                "com/mediatek/bluetooth/avrcp/AvrcpEnhance$MediaPlayerBrowsableData");
        jfieldID browabletypeField = env->GetFieldID(classMediaPlayerBrowsableData,
                        "mBrowsableType", "I");
        jfieldID browabledataField = env->GetFieldID(classMediaPlayerBrowsableData,
                        "mData", "Lcom/mediatek/bluetooth/avrcp/AvrcpEnhance$MediaPlayerData;");

        ALOGI("Complete find kinds of data classes and types.");

        for (uint16_t i = 0; i < param.item_num; i++) {
            localObj = (jobject) env->GetObjectArrayElement(itemList, i);
            btrc_browsable_item_type_t item_type = (btrc_browsable_item_type_t) env->GetIntField(
                    localObj, browabletypeField);
            browsableItems[i].item_type = item_type;
            ALOGI("Get browableItem: %d", item_type);
            jobject browsableObj = env->GetObjectField(localObj, browabledataField);
            ALOGI("Get browable Object");
            browsableItems[i].u.player.player_id = (uint16_t) env->GetShortField(
                    browsableObj, playidField);
            browsableItems[i].u.player.major_type = (uint8_t) env->GetByteField(
                    browsableObj, majortypeField);
            browsableItems[i].u.player.sub_type = (uint32_t) env->GetIntField(
                    browsableObj, subtypeField);
            browsableItems[i].u.player.play_status =
                    (btrc_play_status_t) env->GetIntField(browsableObj,
                            playstatusField);

            // Get Futures's bit set
            jbyteArray featureArray = (jbyteArray) env->GetObjectField(browsableObj,
                    featuresField);
            jbyte * featurebits = env->GetByteArrayElements(featureArray, NULL);
            if (!featurebits) {
                status = BT_STATUS_FAIL;
                jniThrowIOException(env, EINVAL);
                goto RETURN_CLEAN;
            }
            for (int j = 0; j < BTRC_FEATURE_MASK_SIZE; ++j) {
                browsableItems[i].u.player.features[j] = featurebits[j];
            }

            // Get full name item
            jobject fullNameObj = env->GetObjectField(browsableObj, fullnameField);
            browsableItems[i].u.player.full_name.charset_id = env->GetShortField(
                    fullNameObj, charsetField);
            jshort fullNameLength = env->GetShortField(fullNameObj,
                    lengthField);
            browsableItems[i].u.player.full_name.str_len = (short) fullNameLength;
            browsableItems[i].u.player.full_name.p_str = new uint8_t[fullNameLength];
            jbyteArray nameStrByteArray = (jbyteArray) env->GetObjectField(
                    fullNameObj, nameStrField);
            jbyte * nameStrBytes = env->GetByteArrayElements(nameStrByteArray,
                    NULL);
            if (!nameStrBytes) {
                status = BT_STATUS_FAIL;
                jniThrowIOException(env, EINVAL);
                goto RETURN_CLEAN;
            }
            for (short k = 0; k < fullNameLength; ++k) {
              browsableItems[i].u.player.full_name.p_str[k] = (uint8_t)nameStrBytes[k];
            }
            env->ReleaseByteArrayElements(nameStrByteArray, nameStrBytes, 0);
            env->ReleaseByteArrayElements(featureArray, featurebits, 0);
            env->DeleteLocalRef(browsableObj);
        }
    }
    ALOGI("Complete parse param.");
    param.item_list = browsableItems;

    if ((status = avrcpInterface->get_folder_item_rsp(&param))
            != BT_STATUS_SUCCESS) {
        ALOGE("Failed get_folder_item_rsp, status: %d", status);
    }

 RETURN_CLEAN:
    if (browsableItems != NULL) {
        ALOGI("delete browsableItems memory, itemNUM:%d", itemNum);
        for (int i = 0; i < itemNum; i++) {
            if (browsableItems[i].u.player.full_name.p_str != NULL) {
                delete[] browsableItems[i].u.player.full_name.p_str;
                ALOGI("delete browsable Item Names[%d]", i);
            }
        }
        delete[] browsableItems;
        ALOGI("delete browsableItems");
    } else {
        ALOGI("delete browsableItemNames is NULL");
    }
    return (status == BT_STATUS_SUCCESS) ? JNI_TRUE : JNI_FALSE;
}

/** M: MediaPlayer selection feature*/
static jboolean getTotalItemsNumRspNative(JNIEnv *env, jobject object,
        jint rspStatus, jshort uidCounter, jint num) {
    bt_status_t status;
    const btrc_interface_t * avrcpInterface = getBluetoothAvrcpInterface();
    ALOGI("%s: rspStatus: %d, uidCounter: %hd, num: %d", __FUNCTION__,
          rspStatus, uidCounter, num);

    ALOGI("%s: avrcpInterface: %p", __FUNCTION__, avrcpInterface);
    if (!avrcpInterface)
        return JNI_FALSE;

    if ((status = avrcpInterface->get_total_items_num_rsp(
            (btrc_status_t) rspStatus, (uint16_t) uidCounter, (uint32_t) num))
            != BT_STATUS_SUCCESS) {
        ALOGE("Failed get_total_items_num_rsp, status: %d", status);
    }
    return (status == BT_STATUS_SUCCESS) ? JNI_TRUE : JNI_FALSE;
}

static JNINativeMethod sMethods[] = {
    {"classInitNative", "()V", (void *) classInitNative},
    {"initNative", "()V", (void *) initNative},
    {"cleanupNative", "()V", (void *) cleanupNative},
    {"registerNotificationRspAddrPlayerNative", "(ISS)Z",
     (void *) registerNotificationRspAddrPlayerNative},
    {"registerNotificationRspAvalPlayerNative", "(I)Z",
     (void *) registerNotificationRspAvalPlayerNative},
    {"setAddressedPlayerRspNative", "(I)Z",
     (void*) setAddressedPlayerRspNative },
    {"getTotalItemsNumRspNative", "(ISI)Z",
     (void*) getTotalItemsNumRspNative },
    {"getFolderItemRspNative", "(ISS[Ljava/lang/Object;)Z",
     (void*) getFolderItemRspNative },
};

int register_com_mediatek_bluetooth_avrcp(JNIEnv* env) {
    return jniRegisterNativeMethods(env, "com/mediatek/bluetooth/avrcp/AvrcpEnhance",
                                    sMethods, NELEM(sMethods));
}

}  // namespace android
