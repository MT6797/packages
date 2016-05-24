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


#include <stdio.h>
#include <android/bitmap.h>
#include <utils/Log.h>
#include <string.h>

#include "jni.h"
#include "FancyColor.h"
#include "ImageRender.h"

using namespace android;

#define LOG_TAG "MtkGallery2_FancyColor_jni_fancycolor"
#define EFFECT_COUNT 7

ImageRender* m_ImageRender = new ImageRender();
FancyColor* m_fancyColor_stroke = new FancyColor();
FancyColor* m_fancyColor_radial_blur = new FancyColor();

char* m_effectName[] = { "imageFilterNormal", "imageFilterSihouette", "imageFilterWhiteBoard", "imageFilterBlackBoard",
        "imageFilterNegative", "imageFilterRadialBlur", "imageFilterStroke" };

typedef jobject (*EFFECT_FUNC)(JNIEnv *env, jobject thiz, jobject bitmap, jbyteArray mask, jint width, jint height,
        jobjectArray rect, jint center_x, jint center_y);

jboolean initFancyColor(JNIEnv *env, jobject thiz, jobject bitmap, jbyteArray mask, jobject rect, jint imgWidth,
        jint imgHeight, jint center_x, jint center_y, char* data, FancyColor* fancyColor) {
    ALOGI("<initFancyColor>");

    jclass rect_class = env->FindClass("android/graphics/Rect");
    if (rect_class == NULL) {
        ALOGI("<initFancyColor> fail!!!");
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

    char* destination = 0;
    AndroidBitmap_lockPixels(env, bitmap, (void**) &destination);
    int len = imgWidth * imgHeight * 4;
    memcpy(data, destination, len);
    unsigned char* imageData = (unsigned char*) data;
    unsigned char* maskData = (unsigned char*) env->GetByteArrayElements(mask, 0);

    fancyColor->init(imageData, maskData, maskRect, imgWidth, imgHeight, center_x, center_y);

    env->ReleaseByteArrayElements(mask, (jbyte*) maskData, 0);

    AndroidBitmap_unlockPixels(env, bitmap);
    return true;
}

jboolean doStrokeEffect(JNIEnv *env, jobject thiz) {
    ALOGI("<doStrokeEffect>");

    jboolean result = false;
    if (NULL != m_fancyColor_stroke) {
        result = m_fancyColor_stroke->doStrokeEffect();
    }
    return result;
}

jboolean doRadialBlurEffect(JNIEnv *env, jobject thiz) {
    ALOGI("<doRadialBlurEffect>");

    jboolean result = false;
    if (NULL != m_fancyColor_radial_blur) {
        result = m_fancyColor_radial_blur->doRadialBlurEffect();
    }
    return result;
}

jobject getStrokeImg(JNIEnv *env, jobject thiz, jobject bitmap, jint imgWidth, jint imgHeight) {
    ALOGI("<getStrokeImg>");

    char* destination = 0;
    AndroidBitmap_lockPixels(env, bitmap, (void**) &destination);

    if (NULL != m_fancyColor_stroke) {
        m_fancyColor_stroke->getStrokeImg(destination, imgWidth, imgHeight);
    }
    AndroidBitmap_unlockPixels(env, bitmap);

    return bitmap;
}

jobject getRadialBlurImg(JNIEnv *env, jobject thiz, jobject bitmap, jint imgWidth, jint imgHeight) {
    ALOGI("<getRadialBlurImg>");

    char* destination = 0;
    AndroidBitmap_lockPixels(env, bitmap, (void**) &destination);

    if (NULL != m_fancyColor_radial_blur) {
        m_fancyColor_radial_blur->getRadialBlurImg(destination, imgWidth, imgHeight);
    }
    AndroidBitmap_unlockPixels(env, bitmap);

    return bitmap;
}

void release(JNIEnv *env, jobject thiz, FancyColor* fancyColor) {
    ALOGI("<release>");

    if (NULL != fancyColor) {
        fancyColor->release();
    }
}

// exported functions
jobject imageFilterNormal(JNIEnv *env, jobject thiz, jobject bitmap, jbyteArray mask, jint width, jint height,
        jobjectArray rect, jint center_x, jint center_y) {
    ALOGI("<imageFilterNormal>");
    return bitmap;
}

jobject imageFilterStroke(JNIEnv *env, jobject thiz, jobject bitmap, jbyteArray mask, jint width, jint height,
        jobjectArray rect, jint center_x, jint center_y) {
    ALOGI("<imageFilterStroke>");

    int len = width * height * 4;
    char* data = new char[len];
    if (data == NULL) {
        ALOGI("<imageFilterStroke>allocate memory fail!!!");
        return NULL;
    }
    jboolean result = initFancyColor(env, thiz, bitmap, mask, rect, width, height, center_x, center_y,
                                     data, m_fancyColor_stroke);
    if (!result) {
        ALOGI("<imageFilterStroke> init fail");
        delete [] data;
        return NULL;
    }
    result = doStrokeEffect(env, thiz);
    if (!result) {
        ALOGI("<imageFilterStroke> doStroke fail");
        delete [] data;
        return NULL;
    }
    jobject image = getStrokeImg(env, thiz, bitmap, width, height);
    release(env, thiz, m_fancyColor_stroke);
    delete [] data;
    return image;
}

jobject imageFilterRadialBlur(JNIEnv *env, jobject thiz, jobject bitmap, jbyteArray mask, jint width, jint height,
        jobjectArray rect, jint center_x, jint center_y) {
    ALOGI("<imageFilterRadialBlur>");

    int len = width * height * 4;
    char* data = new char[len];
    if (NULL == data) {
        ALOGI("<imageFilterRadialBlur>allocate memory fail!!!");
        return NULL;
    }
    jboolean result = initFancyColor(env, thiz, bitmap, mask, rect, width, height, center_x, center_y,
                                     data, m_fancyColor_radial_blur);
    if (!result) {
        delete [] data;
        ALOGI("<imageFilterRadialBlur> init fail");
        return NULL;
    }
    result = doRadialBlurEffect(env, thiz);
    if (!result) {
        delete [] data;
        ALOGI("<imageFilterRadialBlur> doRadialBlur fail");
        return NULL;
    }
    jobject image = getRadialBlurImg(env, thiz, bitmap, width, height);
    release(env, thiz, m_fancyColor_radial_blur);
    delete [] data;
    return image;
}

jobject imageFilterNegative(JNIEnv *env, jobject thiz, jobject bitmap, jbyteArray mask, jint width, jint height,
        jobjectArray rect, jint center_x, jint center_y) {
    ALOGI("<imageFilterNegative>");
    char* destination = 0;
    AndroidBitmap_lockPixels(env, bitmap, (void**) &destination);
    unsigned char* maskData = (unsigned char*) env->GetByteArrayElements(mask, 0);

    int len = width * height * 4;
    char*filter = new char[len];
    memcpy(filter, destination, len);
    m_ImageRender->imageFilterNegative(filter, width, height);
    m_ImageRender->merge2Bitmap(destination, filter, maskData, width, height);

    env->ReleaseByteArrayElements(mask, (jbyte *) maskData, 0);
    AndroidBitmap_unlockPixels(env, bitmap);
    if (filter != NULL) {
        delete filter;
        filter = NULL;
    }
    return bitmap;
}

jobject imageFilterBlackBoard(JNIEnv *env, jobject thiz, jobject bitmap, jbyteArray mask, jint width, jint height,
        jobjectArray rect, jint center_x, jint center_y) {
    ALOGI("<imageFilterBlackBoard>");
    char* destination = 0;
    AndroidBitmap_lockPixels(env, bitmap, (void**) &destination);
    unsigned char* maskData = (unsigned char*) env->GetByteArrayElements(mask, 0);

    int len = width * height * 4;
    char*filter = new char[len];
    float p = 1.0;
    memcpy(filter, destination, len);
    m_ImageRender->ImageFilterBlackBoard(filter, width, height, p);
    m_ImageRender->merge2Bitmap(destination, filter, maskData, width, height);

    env->ReleaseByteArrayElements(mask, (jbyte *) maskData, 0);
    AndroidBitmap_unlockPixels(env, bitmap);
    if (filter != NULL) {
        delete filter;
        filter = NULL;
    }
    return bitmap;
}

jobject imageFilterWhiteBoard(JNIEnv *env, jobject thiz, jobject bitmap, jbyteArray mask, jint width, jint height,
        jobjectArray rect, jint center_x, jint center_y) {
    ALOGI("<imageFilterWhiteBoard>");
    char* destination = 0;
    AndroidBitmap_lockPixels(env, bitmap, (void**) &destination);
    unsigned char* maskData = (unsigned char*) env->GetByteArrayElements(mask, 0);

    int len = width * height * 4;
    char*filter = new char[len];
    float p = 1.0;
    memcpy(filter, destination, len);
    m_ImageRender->ImageFilterWhiteBoard(filter, width, height, p);
    m_ImageRender->merge2Bitmap(destination, filter, maskData, width, height);

    env->ReleaseByteArrayElements(mask, (jbyte *) maskData, 0);
    AndroidBitmap_unlockPixels(env, bitmap);
    if (filter != NULL) {
        delete filter;
        filter = NULL;
    }
    return bitmap;
}

jobject imageFilterSihouette(JNIEnv *env, jobject thiz, jobject bitmap, jbyteArray mask, jint width, jint height,
        jobjectArray rect, jint center_x, jint center_y) {
    ALOGI("<imageFilterSihouette>");
    char* destination = 0;
    AndroidBitmap_lockPixels(env, bitmap, (void**) &destination);
    unsigned char* maskData = (unsigned char*) env->GetByteArrayElements(mask, 0);

    m_ImageRender->ImageFilterSihouette(destination, maskData, width, height);

    env->ReleaseByteArrayElements(mask, (jbyte *) maskData, 0);
    AndroidBitmap_unlockPixels(env, bitmap);

    return bitmap;
}

static jobject imageFilterKMeans(JNIEnv *env, jobject thiz, jobject bitmap, jbyteArray mask, jint width, jint height,
        jobject large_ds_bitmap, jint lwidth, jint lheight, jobject small_ds_bitmap, jint swidth, jint sheight, jint p,
        jint seed) {
    ALOGI("<imageFilterKMeans>");
    char* destination = 0;
    char* larger_ds_dst = 0;
    char* smaller_ds_dst = 0;
    AndroidBitmap_lockPixels(env, bitmap, (void**) &destination);
    AndroidBitmap_lockPixels(env, large_ds_bitmap, (void**) &larger_ds_dst);
    AndroidBitmap_lockPixels(env, small_ds_bitmap, (void**) &smaller_ds_dst);
    unsigned char* maskData = (unsigned char*) env->GetByteArrayElements(mask, 0);

    int len = width * height * 4;
    char*filter = new char[len];
    memcpy(filter, destination, len);
    m_ImageRender->ImageFilterKMeans(filter, width, height, larger_ds_dst, lwidth, lheight, smaller_ds_dst, swidth,
            sheight, p, seed);
    m_ImageRender->merge2Bitmap(destination, filter, maskData, width, height);

    env->ReleaseByteArrayElements(mask, (jbyte *) maskData, 0);
    AndroidBitmap_unlockPixels(env, small_ds_bitmap);
    AndroidBitmap_unlockPixels(env, large_ds_bitmap);
    AndroidBitmap_unlockPixels(env, bitmap);
    if (filter != NULL) {
        delete filter;
        filter = NULL;
    }
    return bitmap;
}

static jobject imageFilterBwFilter(JNIEnv *env, jobject thiz, jobject bitmap, jbyteArray mask, jint width, jint height,
        jint rw, jint gw, jint bw) {
    ALOGI("<imageFilterBwFilter>");
    char* destination = 0;
    AndroidBitmap_lockPixels(env, bitmap, (void**) &destination);
    unsigned char* maskData = (unsigned char*) env->GetByteArrayElements(mask, 0);

    int len = width * height * 4;
    char*filter = new char[len];
    memcpy(filter, destination, len);
    m_ImageRender->ImageFilterBwFilter(filter, width, height, rw, gw, bw);
    m_ImageRender->merge2Bitmap(destination, filter, maskData, width, height);

    env->ReleaseByteArrayElements(mask, (jbyte *) maskData, 0);
    AndroidBitmap_unlockPixels(env, bitmap);
    if (filter != NULL) {
        delete filter;
        filter = NULL;
    }
    return bitmap;
}

// ImageFilterKMeans & ImageFilterBwFilter are not included in this array
EFFECT_FUNC m_func[EFFECT_COUNT] = { imageFilterNormal, imageFilterSihouette, imageFilterWhiteBoard,
        imageFilterBlackBoard, imageFilterNegative, imageFilterRadialBlur, imageFilterStroke };

jobjectArray getFancyColorEffects(JNIEnv *env, jobject thiz) {
    ALOGI("<getFancyColorEffects>");
    jstring str;
    jobjectArray effectArray = 0;
    effectArray = env->NewObjectArray(EFFECT_COUNT, env->FindClass("java/lang/String"), 0);
    for (int i = 0; i < EFFECT_COUNT; i++) {
        str = env->NewStringUTF(m_effectName[i]);
        env->SetObjectArrayElement(effectArray, i, str);
    }
    return effectArray;
}

jint getFancyColorEffectsCount(JNIEnv *env, jobject thiz) {
    return (jint) EFFECT_COUNT;
}

jobject getFancyColorEffectImage(JNIEnv *env, jobject thiz, jint effect_index, jobject bitmap, jbyteArray mask,
        jint width, jint height, jobjectArray rect, jint center_x, jint center_y) {
    return (m_func[effect_index])(env, thiz, bitmap, mask, width, height, rect, center_x, center_y);
}

static jbyteArray resizeMask(JNIEnv *env, jobject thiz, jbyteArray mask, jint width, jint height, jint dstWidth,
        jint dstHeight) {
    jbyteArray jarray = env->NewByteArray(dstWidth * dstHeight);
    unsigned char* oriMask = (unsigned char*) env->GetByteArrayElements(mask, 0);
    unsigned char* newMask = (unsigned char*) env->GetByteArrayElements(jarray, 0);

    int sample_x = width / dstWidth;
    int sample_y = height / dstHeight;

    for (int i = 0; i < dstHeight; i++) {
        for (int j = 0; j < dstWidth; j++) {
            newMask[i * dstWidth + j] = oriMask[i * sample_y * width + j * sample_x];
        }
    }

    env->ReleaseByteArrayElements(jarray, (jbyte *) newMask, 0);
    env->ReleaseByteArrayElements(mask, (jbyte *) oriMask, 0);
    return jarray;
}

static const char *classPathName = "com/mediatek/galleryfeature/stereo/fancycolor/FancyColorJni";

static JNINativeMethod methods[] = {
  {"imageFilterBwFilter", "(Ljava/lang/Object;[BIIIII)Ljava/lang/Object;", (void*)imageFilterBwFilter},
  {"imageFilterKMeans", "(Ljava/lang/Object;[BIILjava/lang/Object;IILjava/lang/Object;IIII)Ljava/lang/Object;",
          (void*)imageFilterKMeans},
  {"getFancyColorEffects", "()[Ljava/lang/Object;", (void*)getFancyColorEffects},
  {"getFancyColorEffectsCount", "()I", (void*)getFancyColorEffectsCount},
  {"getFancyColorEffectImage", "(ILjava/lang/Object;[BIILjava/lang/Object;II)Ljava/lang/Object;",
          (void*)getFancyColorEffectImage},
  {"nativeResizeMask", "([BIIII)[B", (void*)resizeMask},
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
