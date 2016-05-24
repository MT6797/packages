/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein is
 * confidential and proprietary to MediaTek Inc. and/or its licensors. Without
 * the prior written permission of MediaTek inc. and/or its licensors, any
 * reproduction, modification, use or disclosure of MediaTek Software, and
 * information contained herein, in whole or in part, shall be strictly
 * prohibited.
 *
 * MediaTek Inc. (C) 2015. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER
 * ON AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL
 * WARRANTIES, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR
 * NONINFRINGEMENT. NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH
 * RESPECT TO THE SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY,
 * INCORPORATED IN, OR SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES
 * TO LOOK ONLY TO SUCH THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO.
 * RECEIVER EXPRESSLY ACKNOWLEDGES THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO
 * OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES CONTAINED IN MEDIATEK
 * SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK SOFTWARE
 * RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S
 * ENTIRE AND CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE
 * RELEASED HEREUNDER WILL BE, AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE
 * MEDIATEK SOFTWARE AT ISSUE, OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE
 * CHARGE PAID BY RECEIVER TO MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek
 * Software") have been modified by MediaTek Inc. All revisions are subject to
 * any receiver's applicable license agreements with MediaTek Inc.
 */

#define LOG_TAG "Gallery2_jni_fv3d"
#include <utils/Log.h>
#include <android/bitmap.h>

#include "jni.h"
#include "fv3d_object.h"

#define LOG_TAG "jni_fv3d"

static jlong create(JNIEnv *env, jobject thiz) {
    ALOGI("<create>");
    FV3DObject* myFV3D = new FV3DObject();
    return reinterpret_cast<jlong>((void*) myFV3D);
}

static jboolean initFreeView(JNIEnv *env, jobject thiz, jlong caller, jbyteArray bitmap, jint inputWidth,
        jint inputHeight, jbyteArray depthBuffer, jint depthWidth, jint depthHeight, jint outputWidth,
        jint outputHeight, jint orientation) {
    FV3DObject* myFV3D = reinterpret_cast<FV3DObject*>((long) caller);
    unsigned char* bitmapData = (unsigned char*) env->GetByteArrayElements(bitmap, 0);
    unsigned char* depthData = (unsigned char*) env->GetByteArrayElements(depthBuffer, 0);

    bool res = myFV3D->initFreeView(bitmapData, inputWidth, inputHeight, depthData, depthWidth, depthHeight,
            outputWidth, outputHeight, orientation);

    env->ReleaseByteArrayElements(bitmap, (jbyte *) bitmapData, 0);
    env->ReleaseByteArrayElements(depthBuffer, (jbyte *) depthData, 0);
    return res;
}

static jboolean step(JNIEnv *env, jobject thiz, jlong caller, jint x, jint y, jint outputTexId) {
    FV3DObject* myFV3D = reinterpret_cast<FV3DObject*>((long) caller);
    bool res = myFV3D->step(x, y, outputTexId);
    return (res ? JNI_TRUE : JNI_FALSE);
}

static void release(JNIEnv *env, jobject thiz, jlong caller) {
    FV3DObject* myFV3D = reinterpret_cast<FV3DObject*>((long) caller);
    myFV3D->release();
    delete myFV3D;
}

static const char *classPathName = "com/mediatek/galleryfeature/stereo/freeview3d/FreeViewJni";

static JNINativeMethod methods[] = {
  {"nativeCreate", "()J", (void*)create },
  {"nativeInitFreeView", "(J[BII[BIIIII)Z", (void*)initFreeView},
  {"nativeStep", "(JIII)Z", (void*)step },
  {"nativeRelease", "(J)V", (void*)release },
};

/*
 * Register several native methods for one class.
 */
static int registerNativeMethods(JNIEnv* env, const char* className, JNINativeMethod* gMethods, int numMethods) {
    jclass clazz;

    clazz = env->FindClass(className);
    if (clazz == NULL) {
        // ALOGE("Native registration unable to find class '%s'", className);
        return JNI_FALSE;
    }
    if (env->RegisterNatives(clazz, gMethods, numMethods) < 0) {
        // ALOGE("RegisterNatives failed for '%s'", className);
        return JNI_FALSE;
    }

    return JNI_TRUE;
}

/*
 * Register native methods for all classes we know about.
 *
 * returns JNI_TRUE on success.
 */
static int registerNatives(JNIEnv* env) {
    if (!registerNativeMethods(env, classPathName, methods, sizeof(methods) / sizeof(methods[0]))) {
        return JNI_FALSE;
    }

    return JNI_TRUE;
}

// ----------------------------------------------------------------------------

/*
 * This is called by the VM when the shared library is first loaded.
 */

typedef union {
    JNIEnv* env;
    void* venv;
} UnionJNIEnvToVoid;

jint JNI_OnLoad(JavaVM* vm, void* reserved) {
    UnionJNIEnvToVoid uenv;
    uenv.venv = NULL;
    jint result = -1;
    JNIEnv* env = NULL;

    ALOGI("JNI_OnLoad");

    if (vm->GetEnv(&uenv.venv, JNI_VERSION_1_4) != JNI_OK) {
        ALOGE("ERROR: GetEnv failed");
        goto bail;
    }
    env = uenv.env;

    if (registerNatives(env) != JNI_TRUE) {
        ALOGE("ERROR: registerNatives failed");
        goto bail;
    }

    result = JNI_VERSION_1_4;
    ALOGI("JNI_OnLoad successfully");

    bail: return result;
}
