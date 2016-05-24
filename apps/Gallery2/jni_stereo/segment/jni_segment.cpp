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
#include <android/bitmap.h>
#include <string.h>
#include <assert.h>

#include "jni.h"
#include "imagesegment.h"

using namespace android;

#define LOG_TAG "Gallery2_jni_segment"

Rect* gfaceRect = NULL;
ImageSegment* m_segment = NULL;

jboolean init(JNIEnv *env, jobject thiz, jbyteArray bitmap, jint imgWidth, jint imgHeight, jbyteArray depthBuf,
        jint depthWidth, jint depthHeight, jbyteArray occImgBuf, jint occImgWidth, jint occImgHeight,
        jint scribbleWidth, jint scribbleHeight, jint faceNum, jobjectArray faceRect, jintArray faceRip,
        jint orientation) {
    ALOGI("<imageRefocus>");

    jclass rect_class = env->FindClass("android/graphics/Rect");
    if (rect_class == NULL) {
        ALOGI("<imageRefocus>, fail!!!");
        return false;
    }
    unsigned char* imageData = (unsigned char*) env->GetByteArrayElements(bitmap, 0);
    unsigned char* depthBufPtr = (unsigned char*) env->GetByteArrayElements(depthBuf, 0);
    unsigned char* occImageBufPtr = (unsigned char*) env->GetByteArrayElements(occImgBuf, 0);
    jint *pFaceRip = env->GetIntArrayElements(faceRip, NULL);

    jfieldID left_field = env->GetFieldID(rect_class, "left", "I");
    jfieldID right_filed = env->GetFieldID(rect_class, "right", "I");
    jfieldID top_filed = env->GetFieldID(rect_class, "top", "I");
    jfieldID bottom_filed = env->GetFieldID(rect_class, "bottom", "I");

    gfaceRect = (Rect*) malloc(sizeof(Rect) * faceNum);
    for (int i = 0; i < faceNum; i++) {
        jobject rect = env->GetObjectArrayElement(faceRect, i);
        if (rect == NULL) {
            continue;
        }
        gfaceRect[i].left = env->GetIntField(rect, left_field);
        gfaceRect[i].top = env->GetIntField(rect, top_filed);
        gfaceRect[i].right = env->GetIntField(rect, right_filed);
        gfaceRect[i].bottom = env->GetIntField(rect, bottom_filed);
    }

    m_segment = new ImageSegment();
    m_segment->init(imageData, imgWidth, imgHeight, depthBufPtr, depthWidth, depthHeight, occImageBufPtr, occImgWidth,
            occImgHeight, scribbleWidth, scribbleHeight, faceNum, gfaceRect, pFaceRip, orientation);

    env->ReleaseByteArrayElements(depthBuf, (jbyte*) depthBufPtr, 0);
    env->ReleaseByteArrayElements(occImgBuf, (jbyte*) occImageBufPtr, 0);
    env->ReleaseByteArrayElements(bitmap, (jbyte*) imageData, 0);
    env->ReleaseIntArrayElements(faceRip, pFaceRip, 0);
    return true;
}

jboolean doSegment(JNIEnv *env, jobject thiz, jint scenario, jint mode, jbyteArray scribbleBuf, jobject roiRect) {
    ALOGI("<initRefocusNoDepthMap>");

    jclass rect_class = env->FindClass("android/graphics/Rect");
    if (rect_class == NULL) {
        ALOGI("<initRefocusNoDepthMap>, fail!!!!");
        return false;
    }
    jfieldID left_field = env->GetFieldID(rect_class, "left", "I");
    jfieldID right_filed = env->GetFieldID(rect_class, "right", "I");
    jfieldID top_filed = env->GetFieldID(rect_class, "top", "I");
    jfieldID bottom_filed = env->GetFieldID(rect_class, "bottom", "I");

    Rect rect;
    rect.left = env->GetIntField(roiRect, left_field);
    rect.top = env->GetIntField(roiRect, top_filed);
    rect.right = env->GetIntField(roiRect, right_filed);
    rect.bottom = env->GetIntField(roiRect, bottom_filed);

    jboolean initResult = false;
    if (scenario != SCENARIO_SCRIBBLE_FG && scenario != SCENARIO_SCRIBBLE_BG) {
        initResult = m_segment->doSegment(scenario, mode, NULL, rect);
    } else {
        unsigned char* scribbleBufPtr = (unsigned char*) env->GetByteArrayElements(scribbleBuf, 0);
        initResult = m_segment->doSegment(scenario, mode, scribbleBufPtr, rect);
        env->ReleaseByteArrayElements(scribbleBuf, (jbyte*) scribbleBufPtr, 0);
    }

    return initResult;
}

jboolean undoSegment(JNIEnv *env, jobject thiz) {
    ALOGI("<undoSegment>");

    jboolean res = m_segment->undoSegment();
    return res;
}

jboolean initSegmentMask(JNIEnv *env, jobject thiz, jbyteArray mask, jobject rect, jobject point) {
    ALOGI("<initSegmentMask>");

    jclass rect_class = env->FindClass("android/graphics/Rect");
    if (rect_class == NULL) {
        ALOGI("<initSegmentMask> fail!!!");
        return false;
    }
    jfieldID left_field = env->GetFieldID(rect_class, "left", "I");
    jfieldID right_filed = env->GetFieldID(rect_class, "right", "I");
    jfieldID top_filed = env->GetFieldID(rect_class, "top", "I");
    jfieldID bottom_filed = env->GetFieldID(rect_class, "bottom", "I");

    Rect maskRect;
    maskRect.left = env->GetIntField(rect, left_field);
    maskRect.top = env->GetIntField(rect, top_filed);
    maskRect.right = env->GetIntField(rect, right_filed);
    maskRect.bottom = env->GetIntField(rect, bottom_filed);

    jclass point_class = env->FindClass("android/graphics/Point");
    jfieldID x_field = env->GetFieldID(point_class, "x", "I");
    jfieldID y_field = env->GetFieldID(point_class, "y", "I");

    Point maskPoint;
    maskPoint.x = env->GetIntField(point, x_field);
    maskPoint.y = env->GetIntField(point, y_field);

    unsigned char* maskPtr = (unsigned char*) env->GetByteArrayElements(mask, 0);
    jboolean initResult = m_segment->initSegmentMask(maskPtr, maskRect, maskPoint);
    env->ReleaseByteArrayElements(mask, (jbyte*) maskPtr, 0);

    return initResult;
}

jbyteArray getSegmentMask(JNIEnv *env, jobject thiz, jint width, jint height, jboolean isNew) {
    ALOGI("<getSegmentMask>");

    jbyteArray jarray = env->NewByteArray(width * height);
    unsigned char* tempData = (unsigned char*) env->GetByteArrayElements(jarray, 0);
    unsigned char* ptr = m_segment->getSegmentMask(isNew);
    if (ptr != NULL && tempData != NULL) {
        memcpy(tempData, ptr, width * height);
    }
    env->ReleaseByteArrayElements(jarray, (jbyte *) tempData, 0);
    return jarray;
}

jobject getSegmentPoint(JNIEnv *env, jobject thiz, jboolean isNew) {
    ALOGI("<getSegmentPoint>");

    jclass rect_class = env->FindClass("android/graphics/Point");
    jmethodID mid = env->GetMethodID(rect_class, "<init>", "()V");
    jobject obj = env->NewObject(rect_class, mid);

    jfieldID x_field = env->GetFieldID(rect_class, "x", "I");
    jfieldID y_field = env->GetFieldID(rect_class, "y", "I");

    Point point = m_segment->getSegmentPoint(isNew);

    env->SetIntField(obj, x_field, point.x);
    env->SetIntField(obj, y_field, point.y);
    return obj;
}

jobject getSegmentRect(JNIEnv *env, jobject thiz, jboolean isNew) {
    ALOGI("<getSegmentRect>");
    jclass rect_class = env->FindClass("android/graphics/Rect");
    jmethodID mid = env->GetMethodID(rect_class, "<init>", "()V");
    jobject obj = env->NewObject(rect_class, mid);

    jfieldID left_field = env->GetFieldID(rect_class, "left", "I");
    jfieldID right_filed = env->GetFieldID(rect_class, "right", "I");
    jfieldID top_filed = env->GetFieldID(rect_class, "top", "I");
    jfieldID bottom_filed = env->GetFieldID(rect_class, "bottom", "I");

    Rect rect = m_segment->getSegmentRect(isNew);

    env->SetIntField(obj, left_field, rect.left);
    env->SetIntField(obj, right_filed, rect.right);
    env->SetIntField(obj, top_filed, rect.top);
    env->SetIntField(obj, bottom_filed, rect.bottom);
    return obj;
}

jobject getSegmentImg(JNIEnv *env, jobject thiz, jobject oriImg, jint oriWidth, jint oriHeight,
        jobject newImg, jint newWidth, jint newHeight, jboolean isNew) {
    ALOGI("<getSegmentImg>");

    char* oriImgPtr = 0;
    char* newImgPtr = 0;
    AndroidBitmap_lockPixels(env, oriImg, (void**) &oriImgPtr);
    AndroidBitmap_lockPixels(env, newImg, (void**) &newImgPtr);
    bool res = m_segment->getSegmentImg(oriImgPtr, oriWidth, oriHeight, newImgPtr,
            newWidth, newHeight, isNew);
    AndroidBitmap_unlockPixels(env, oriImg);
    AndroidBitmap_unlockPixels(env, newImg);
    return newImg;
}

jobject fillMaskToImg(JNIEnv *env, jobject thiz, jobject bitmap, jint width, jint height) {
    ALOGI("<fillMaskToImg>");

    char* destination = 0;
    AndroidBitmap_lockPixels(env, bitmap, (void**) &destination);
    m_segment->fillMaskToImg(destination, width, height);
    AndroidBitmap_unlockPixels(env, bitmap);
    return bitmap;
}

jboolean setNewBitmap(JNIEnv *env, jobject thiz, jobject bitmap, jint bitmapWidth,
        jint bitmapHeight, jbyteArray mask, jint maskWidth, jint maskHeight) {
    if (NULL == mask && NULL == m_segment) {
        ALOGI("<setNewBitmap> null mask and m_segment object,fail!!!");
        assert(false);
        return false;
    }
    char* destination = 0;
    AndroidBitmap_lockPixels(env, bitmap, (void**) &destination);
    if (m_segment != NULL) {
        m_segment->setNewBitmap(destination, bitmapWidth, bitmapHeight);
    } else {
        unsigned char* maskPtr = (unsigned char*) env->GetByteArrayElements(mask, 0);
        m_segment = new ImageSegment();
        m_segment->setNewBitmap(destination, bitmapWidth, bitmapHeight, maskPtr,
                maskWidth, maskHeight);
        env->ReleaseByteArrayElements(mask, (jbyte*) maskPtr, 0);
    }
    AndroidBitmap_unlockPixels(env, bitmap);
    return true;
}

void release(JNIEnv *env, jobject thiz) {
    ALOGI("release()");

    if (NULL != m_segment) {
        m_segment->release();
        delete m_segment;
        m_segment = NULL;
    }
    if (NULL != gfaceRect) {
        free(gfaceRect);
        gfaceRect = NULL;
    }
}

static const char *classPathName = "com/mediatek/galleryfeature/stereo/SegmentJni";

static JNINativeMethod methods[] = {
  {"nativeInit", "([BII[BII[BIIIII[Ljava/lang/Object;[II)Z", (void*)init },
  {"nativeDoSegment", "(II[BLjava/lang/Object;)Z", (void*)doSegment },
  {"nativeUndoSegment", "()Z", (void*)undoSegment},
  {"nativeInitSegmentMask", "([BLjava/lang/Object;Ljava/lang/Object;)Z", (void*)initSegmentMask},
  {"nativeGetSegmentMask", "(IIZ)[B", (void*)getSegmentMask},
  {"nativeGetSegmentPoint", "(Z)Ljava/lang/Object;", (void*)getSegmentPoint},
  {"nativeGetSegmentRect", "(Z)Ljava/lang/Object;", (void*)getSegmentRect},
  {"nativeGetSegmentImg", "(Ljava/lang/Object;IILjava/lang/Object;IIZ)Ljava/lang/Object;", (void*)getSegmentImg},
  {"nativeFillMaskToImg", "(Ljava/lang/Object;II)Ljava/lang/Object;", (void*)fillMaskToImg},

  {"nativeSetNewBitmap", "(Ljava/lang/Object;II[BII)Z", (void*)setNewBitmap},
  {"nativeRelease", "()V", (void*)release },
};

/*
 * Register several native methods for one class.
 */
static int registerNativeMethods(JNIEnv* env, const char* className, JNINativeMethod* gMethods, int numMethods) {
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

    bail: return result;
}
