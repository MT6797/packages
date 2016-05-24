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

#include <utils/Log.h>
#include <stdio.h>
#include "jni.h"
#include "image_refocus.h"

using namespace android;

#define LOG_TAG "Gallery2_jni_image_refocus"

ImageRefocus *mImageRefocus = NULL;

static void imageRefocus(JNIEnv *env, jobject thiz, jint jpsWidth, jint jpsHeight, jint maskWidth,
        jint maskHeight, jint posX, jint posY, jint viewWidth, jint viewHeight, jint orientation,
        jint mainCamPos, jint touchCoordX1st, jint touchCoordY1st, jint refocusMode,
        jint depthRotation) {
    mImageRefocus = new ImageRefocus(jpsWidth, jpsHeight, maskWidth, maskHeight, posX, posY,
            viewWidth, viewHeight, orientation, mainCamPos, touchCoordX1st, touchCoordY1st,
            refocusMode, depthRotation);
     ALOGI("imageRefocus, instance ID %p", mImageRefocus);
}

static jboolean initRefocusNoDepthMap(JNIEnv *env, jobject thiz, jstring targetFilePath,
        jint outImgWidth, jint outImgHeight, jint imgOrientation, jbyteArray jpsBuffer,
        jint jpsBufferSize, jint inStereoImgWidth, jint inStereoImgHeight,
        jbyteArray maskBuffer, jint maskBufferSize, jint maskWidth, jint maskHeight,
        jbyteArray ldcBuffer, jint ldcBufferSize, jint ldcWidth, jint ldcHeight) {
    ALOGI("initRefocusNoDepthMap");
    const char *pTargetFilePath = env->GetStringUTFChars(targetFilePath, false);
    unsigned char* pJpsBuffer = (unsigned char*)env->GetByteArrayElements(jpsBuffer, 0);
    unsigned char* pMaskBuffer = (unsigned char*)env->GetByteArrayElements(maskBuffer, 0);
    unsigned char* pLdcBuffer = (unsigned char*)env->GetByteArrayElements(ldcBuffer, 0);
    jboolean initResult;
    initResult = mImageRefocus->initRefocusNoDepthMap(pTargetFilePath, outImgWidth, outImgHeight,
            imgOrientation, pJpsBuffer, jpsBufferSize, inStereoImgWidth, inStereoImgHeight,
            pMaskBuffer, maskBufferSize, maskWidth, maskHeight, pLdcBuffer, ldcBufferSize,
            ldcWidth, ldcHeight);
    env->ReleaseStringUTFChars(targetFilePath, pTargetFilePath);
    env->ReleaseByteArrayElements(jpsBuffer, (jbyte *)pJpsBuffer, 0);
    env->ReleaseByteArrayElements(maskBuffer, (jbyte *)pMaskBuffer, 0);
    env->ReleaseByteArrayElements(ldcBuffer, (jbyte *)pLdcBuffer, 0);
    return initResult;
}

static jboolean initRefocusNoDepthMapUseJpgBuf(JNIEnv *env, jobject thiz, jbyteArray jpgBuffer,
        jint jpgBufferSize, jint outImgWidth, jint outImgHeight, jint imgOrientation,
        jbyteArray jpsBuffer, jint jpsBufferSize, jint inStereoImgWidth, jint inStereoImgHeight,
        jbyteArray maskBuffer, jint maskBufferSize, jint maskWidth, jint maskHeight,
        jbyteArray ldcBuffer, jint ldcBufferSize, jint ldcWidth, jint ldcHeight) {
    ALOGI("initRefocusNoDepthMapUseJpgBuf");
    unsigned char* pJpgBuffer = (unsigned char*)env->GetByteArrayElements(jpgBuffer, 0);
    unsigned char* pJpsBuffer = (unsigned char*)env->GetByteArrayElements(jpsBuffer, 0);
    unsigned char* pMaskBuffer = (unsigned char*)env->GetByteArrayElements(maskBuffer, 0);
    unsigned char* pLdcBuffer = (unsigned char*)env->GetByteArrayElements(ldcBuffer, 0);
    jboolean initResult;
    initResult = mImageRefocus->initRefocusNoDepthMap(pJpgBuffer, jpgBufferSize, outImgWidth,
            outImgHeight, imgOrientation, pJpsBuffer, jpsBufferSize, inStereoImgWidth,
            inStereoImgHeight, pMaskBuffer, maskBufferSize, maskWidth, maskHeight,
            pLdcBuffer, ldcBufferSize, ldcWidth, ldcHeight);
    env->ReleaseByteArrayElements(jpgBuffer, (jbyte*)pJpgBuffer, 0);
    env->ReleaseByteArrayElements(jpsBuffer, (jbyte *)pJpsBuffer, 0);
    env->ReleaseByteArrayElements(maskBuffer, (jbyte *)pMaskBuffer, 0);
    env->ReleaseByteArrayElements(ldcBuffer, (jbyte *)pLdcBuffer, 0);
    return initResult;
}

static jboolean initRefocusWithDepthMap(JNIEnv *env, jobject thiz, jstring targetFilePath,
        jint outImgWidth, jint outImgHeight, jint imgOrientation,
                jbyteArray depthMapBuffer, jint depthMapBufferSize, jint inStereoImgWidth,
                jint inStereoImgHeight) {
    ALOGI("initRefocusWithDepthMap");
    const char *cTargetFilePath = env->GetStringUTFChars(targetFilePath, false);
    unsigned char* cDepthMapBuffer = (unsigned char*)env->GetByteArrayElements(depthMapBuffer, 0);
    jboolean initResult;
    initResult = mImageRefocus->initRefocusWithDepthMap(cTargetFilePath, outImgWidth, outImgHeight,
            imgOrientation, cDepthMapBuffer, depthMapBufferSize, inStereoImgWidth, inStereoImgHeight);
    env->ReleaseStringUTFChars(targetFilePath, cTargetFilePath);
    env->ReleaseByteArrayElements(depthMapBuffer, (jbyte *)cDepthMapBuffer, 0);
    return initResult;
}

static jboolean initRefocusWithDepthMapUseJpgBuf(JNIEnv *env, jobject thiz, jbyteArray jpgBuffer,
        jint jpgBufferSize, jint outImgWidth, jint outImgHeight, jint imgOrientation,
        jbyteArray depthMapBuffer, jint depthMapBufferSize, jint inStereoImgWidth,
        jint inStereoImgHeight) {
    ALOGI("initRefocusWithDepthMapUseJpgBuf");
    unsigned char* pJpgBuffer = (unsigned char*)env->GetByteArrayElements(jpgBuffer, 0);
    unsigned char* pDepthMapBuffer = (unsigned char*)env->GetByteArrayElements(depthMapBuffer, 0);
    jboolean initResult;
    initResult = mImageRefocus->initRefocusWithDepthMap(pJpgBuffer, jpgBufferSize, outImgWidth, outImgHeight,
            imgOrientation, pDepthMapBuffer, depthMapBufferSize, inStereoImgWidth, inStereoImgHeight);
    env->ReleaseByteArrayElements(jpgBuffer, (jbyte*)pJpgBuffer, 0);
    env->ReleaseByteArrayElements(depthMapBuffer, (jbyte *)pDepthMapBuffer, 0);
    return initResult;
}

static jobject generateRefocusImage(JNIEnv *env, jobject thiz, jint touchX, jint touchY, jint depthOfField) {
    ALOGI("generateImage %d, %d, %d) ", touchX, touchY, depthOfField);

    ImageBuffer image = mImageRefocus->generateRefocusImage(touchX, touchY, depthOfField);

    jclass cls = env->FindClass("com/mediatek/galleryfeature/stereo/ImageBuffer");
    jmethodID mid = env->GetMethodID(cls, "<init>", "()V");
    jobject obj = env->NewObject(cls, mid);

    jfieldID width = env->GetFieldID(cls, "width", "I");
    jfieldID height = env->GetFieldID(cls, "height", "I");
    jfieldID buffer = env->GetFieldID(cls, "buffer", "[B");

    env->SetIntField(obj, width, image.width);
    env->SetIntField(obj, height, image.height);

    int len = image.width * image.height * 4;
    if(len > 0) {
        jbyteArray jarray = env->NewByteArray(len);
        unsigned char* tempData = (unsigned char*) env->GetByteArrayElements(jarray, 0);
        memcpy(tempData, image.buffer, len);
        env->ReleaseByteArrayElements(jarray, (jbyte *) tempData, 0);
        env->SetObjectField(obj, buffer, jarray);
    }
    return obj;
}

static jboolean generateDepth(JNIEnv *env, jobject thiz) {
    jboolean initResult = mImageRefocus->generate();
    return initResult;
}

static jint getDepthBufferSize(JNIEnv *env, jobject thiz) {
    ALOGI("getDepthBufferSize");
    return mImageRefocus->getDepthBufferSize();
}

static jint getDepthBufferWidth(JNIEnv *env, jobject thiz) {
    ALOGI("getDepthBufferWidth");
    return mImageRefocus->getDepthBufferWidth();
}

static jint getDepthBufferHeight(JNIEnv *env, jobject thiz) {
    ALOGI("getDepthBufferHeight");
    return mImageRefocus->getDepthBufferHeight();
}

static jint getXMPDepthBufferSize(JNIEnv *env, jobject thiz) {
    ALOGI("getDepthBufferSize");
    return mImageRefocus->getXMPDepthBufferSize();
}

static jint getXMPDepthBufferWidth(JNIEnv *env, jobject thiz) {
    ALOGI("getDepthBufferWidth");
    return mImageRefocus->getXMPDepthBufferWidth();
}

static jint getXMPDepthBufferHeight(JNIEnv *env, jobject thiz) {
    ALOGI("getDepthBufferHeight");
    return mImageRefocus->getXMPDepthBufferHeight();
}

static jint getMetaBufferWidth(JNIEnv *env, jobject thiz) {
    ALOGI("getMetaBufferWidth");
    return mImageRefocus->getMetaBufferWidth();
}

static jint getMetaBufferHeight(JNIEnv *env, jobject thiz) {
    ALOGI("getMetaBufferHeight");
    return mImageRefocus->getMetaBufferHeight();
}

static void saveDepthMapInfo(JNIEnv *env, jobject thiz, jbyteArray depthBuffer,
        jbyteArray xmpDepthBuffer) {
    ALOGI("saveDepthMapInfo");
    unsigned char* depthData = (unsigned char*)env->GetByteArrayElements(depthBuffer, 0);
    unsigned char* xmpDepthData = (unsigned char*)env->GetByteArrayElements(xmpDepthBuffer, 0);
    mImageRefocus->saveDepthMapInfo((unsigned char*)depthData, (unsigned char*)xmpDepthData);
    env->ReleaseByteArrayElements(depthBuffer, (jbyte *)depthData, 0);
    env->ReleaseByteArrayElements(xmpDepthBuffer, (jbyte *)xmpDepthData, 0);
}

static void saveRefocusImage(JNIEnv *env, jobject thiz, jstring saveFileName, jint inSampleSize) {
    ALOGI("<saveRefocusImage> %d", inSampleSize);
    const char *cSaveFileName = env->GetStringUTFChars(saveFileName, false);
    mImageRefocus->saveRefocusImage(cSaveFileName, inSampleSize);
    env->ReleaseStringUTFChars(saveFileName, cSaveFileName);
}

static void release(JNIEnv *env, jobject thiz) {
    ALOGI("release()");
    if (NULL != mImageRefocus) {
        mImageRefocus->deinit();
    }
}

static const char *classPathName = "com/mediatek/galleryfeature/stereo/RefocusImageJni";

static JNINativeMethod methods[] = {
  {"nativeImageRefocus", "(IIIIIIIIIIIIII)V", (void*)imageRefocus },
  {"nativeInitRefocusNoDepthMap", "(Ljava/lang/String;III[BIII[BIII[BIII)Z", (void*)initRefocusNoDepthMap},
  {"nativeInitRefocusNoDepthMapUseJpgBuf", "([BIIII[BIII[BIII[BIII)Z", (void*)initRefocusNoDepthMapUseJpgBuf},
  {"nativeInitRefocusWithDepthMap", "(Ljava/lang/String;III[BIII)Z", (void*)initRefocusWithDepthMap },
  {"nativeInitRefocusWithDepthMapUseJpgBuf", "([BIIII[BIII)Z", (void*)initRefocusWithDepthMapUseJpgBuf },
  {"nativeGenerateRefocusImage", "(III)Ljava/lang/Object;", (void*)generateRefocusImage},
  {"nativeGenerateDepth", "()Z", (void*)generateDepth},
  {"nativeGetDepthBufferSize", "()I", (void*)getDepthBufferSize },
  {"nativeGetDepthBufferWidth", "()I", (void*)getDepthBufferWidth },
  {"nativeGetDepthBufferHeight", "()I", (void*)getDepthBufferHeight },
  {"nativeGetXMPDepthBufferSize", "()I", (void*)getXMPDepthBufferSize },
  {"nativeGetXMPDepthBufferWidth", "()I", (void*)getXMPDepthBufferWidth },
  {"nativeGetMetaBufferWidth", "()I", (void*)getMetaBufferWidth},
  {"nativeGetMetaBufferHeight", "()I", (void*)getMetaBufferHeight},
  {"nativeGetXMPDepthBufferHeight", "()I", (void*)getXMPDepthBufferHeight },
  {"nativeSaveDepthMapInfo", "([B[B)V", (void*)saveDepthMapInfo },
  {"nativeSaveRefocusImage", "(Ljava/lang/String;I)V", (void*)saveRefocusImage },
  {"nativeRelease", "()V", (void*)release },
};

/*
 * Register several native methods for one class.
 */
static int registerNativeMethods(JNIEnv* env, const char* className,
    JNINativeMethod* gMethods, int numMethods) {
    jclass clazz;

    clazz = env->FindClass(className);
    if (clazz == NULL) {
        ALOGE("Native registration unable to find class '%s'", className);
        return JNI_FALSE;
    }
    if (env->RegisterNatives(clazz, gMethods, numMethods) < 0) {
        ALOGE("RegisterNatives failed for '%s'", className);
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
  if (!registerNativeMethods(env, classPathName,
                 methods, sizeof(methods) / sizeof(methods[0]))) {
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

bail:
    return result;
}
