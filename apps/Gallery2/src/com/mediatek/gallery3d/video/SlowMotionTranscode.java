/*
* Copyright (C) 2014 MediaTek Inc.
* Modification based on code covered by the mentioned copyright
* and/or permission notice(s).
*/
/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mediatek.gallery3d.video;

import java.io.FileDescriptor;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.ref.WeakReference;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.mediatek.galleryframework.util.MtkLog;


public class SlowMotionTranscode {

    public interface OnInfoListener {
        boolean onInfo(int msg, int ext1, int ext2);
    }

    static {
        // The runtime will add "lib" on the front and ".o" on the end of
        // the name supplied to loadLibrary.
        System.loadLibrary("jni_slow_motion");
    }

    private static final String TAG = "SlowMotionTranscode";
    private FileDescriptor srcFd;
    private FileDescriptor dstFd;
    private Context mContext;
    private long mNativeContext;
    private EventHandler mEventHandler;

    private OnInfoListener mOnInfoListener;

    private class EventHandler extends Handler {

        public EventHandler(Looper looper) {
            super(looper);
        }
        @Override
        public void handleMessage(Message msg) {
            // TODO Auto-generated method stub
            MtkLog.v(TAG, "handleMessage what " + msg.what);
            mOnInfoListener.onInfo(msg.what, msg.arg1, msg.arg2);
        }
    }

    public SlowMotionTranscode(Context context) {
        MtkLog.i(TAG, "SlowMotionTranscode");
        mContext = context;

        Looper looper;
        if ((looper = Looper.myLooper()) != null) {
            mEventHandler = new EventHandler(looper);
        } else if ((looper = Looper.getMainLooper()) != null) {
            mEventHandler = new EventHandler(looper);
        } else {
            mEventHandler = null;
        }

        native_setup(new WeakReference<SlowMotionTranscode>(this));
    }

    public void setOnInfoListener(OnInfoListener l) {
        mOnInfoListener = l;
    }
    private void onInfo(int msg, int ext1, int ext2) {
        mOnInfoListener.onInfo(msg, ext1, ext2);
    }

    public int stopSaveSpeedEffect() {
        return native_stopSaveSpeedEffect();
    }

    public int setSpeedEffectParams(long startPos, long endPos, String params) {
        return native_setSpeedEffectParams(startPos, endPos, params);
    }

    public int startSaveSpeedEffect(String srcPath, String dstPath) throws IOException {
        RandomAccessFile src;
        RandomAccessFile dst;
        MtkLog.v(TAG, "startSaveSpeedEffect srcPath " + srcPath + " dstPath " + dstPath);

        src = new RandomAccessFile(srcPath, "r");
        dst = new RandomAccessFile(dstPath, "rw");
        MtkLog.v(TAG, "startSaveSpeedEffect srcfd " +  src.getFD() + " dstfd " + dst.getFD()
                                                + "srcLength " + src.length());
        native_startSaveSpeedEffect(src.getFD(), dst.getFD(), src.length());
        src.close();
        dst.close();
        return 0;
    }

    /**
     * Set speed effect parameters, such as slow motion interval, slow motion speed
     *               caller need call this interface before start speed effect handling
     * @param startPos   start position of speed effect (such as slow motion) interval
     * @param endPos     end position of speed effect (such as slow motion) interval
     * @param params     the speed effect parameters,such as "slow-motion-speed = 4;video-framerate = 30;mute-autio = 0"
     * @return      0 indicate successful, otherwise error type will return
     */
    private native final int native_setSpeedEffectParams(long startPos, long endPos, String params);


    /**
     * Start save the speed effect
     * @param srcFd  File Description of the src file
     * @param dstFd  File Description of the dst file
     * @param srcLength Length of the src file.
     * @return    0 indicate successful, otherwise error type will return
     */
    private native final int native_startSaveSpeedEffect(
            FileDescriptor srcFd, FileDescriptor dstFd, long srcLength);


    /**
     * Stop the speed effect opertion
     *                 Caller can call this interface if user has cancelled the speed effect.
     *                 Part of the video will be transfered, caller can delete the output video if user cancel the operation
     * @return     0 indicate successful, otherwise error type will return
     */
    private native final int native_stopSaveSpeedEffect();


    /**
     *  Post message from Native.
     */
    private static void postEventFromNative(Object ref, int what, int arg1, int arg2, Object obj) {
        SlowMotionTranscode sm = (SlowMotionTranscode) ((WeakReference) ref).get();
        if (sm == null) {
            MtkLog.e(TAG, "postEventFromNative: Null sm! what=" + what + ", arg1=" + arg1 + ", arg2="
                    + arg2);
            return;
        }
        if (sm.mEventHandler != null) {
            Message m = sm.mEventHandler.obtainMessage(what, arg1, arg2, obj);
            sm.mEventHandler.sendMessage(m);
        }
    }
    /**
     * Setup slow motion Native runtime.
     *           Should call this firstly before any other operation.
     * @param slowmotion_this
     */
    private native final void native_setup(Object slowmotion_this);


}
