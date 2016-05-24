/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.mediatek.audioprofile;

import android.content.ContentResolver;
import android.content.Context;
import android.media.AudioManager;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.preference.SeekBarPreference;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.SeekBar;

import com.android.settings.R;

/**
 * A slider preference that directly controls an audio stream volume (no dialog).
 */
public class VolumeSeekBarPreference extends SeekBarPreference
        implements PreferenceManager.OnActivityStopListener {
    private static final String TAG = "VolumeSeekBarPreference";

    private int mStream;
    private SeekBar mSeekBar;
    private SeekBarVolumizer mVolumizer;
    private Callback mCallback;
    private String mKey;

    /**
     * Constructor for class.
     *
     * @param context
     *            The application context
     * @param attrs
     *            More attribute set config
     * @param defStyleAttr
     *            Default styly attribute
     * @param defStyleRes
     *            Default style resource
     */
    public VolumeSeekBarPreference(Context context, AttributeSet attrs, int defStyleAttr,
            int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    /**
     * Constructor for class.
     *
     * @param context
     *            The application context
     * @param attrs
     *            More attribute set config
     * @param defStyleAttr
     *            Default styly attribute
     */
    public VolumeSeekBarPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    /**
     * Constructor for class.
     *
     * @param context
     *            The application context
     * @param attrs
     *            More attribute set config
     */
    public VolumeSeekBarPreference(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    /**
     * Constructor for class.
     *
     * @param context
     *            The application context
     */
    public VolumeSeekBarPreference(Context context) {
        this(context, null);
    }

    public void setStream(int stream) {
        mStream = stream;
    }

    public void setCallback(Callback callback) {
        mCallback = callback;
    }

    @Override
    public void onActivityStop() {
//        if (mVolumizer != null) {
//            mVolumizer.stop();
//        }
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);
        if (mStream == 0) {
            Log.w(TAG, "No stream found, not binding volumizer  ");
            return;
        }
        getPreferenceManager().registerOnActivityStopListener(this);
        final SeekBar seekBar = (SeekBar) view.findViewById(com.android.internal.R.id.seekbar);
        if (seekBar == mSeekBar) {
            return;
        }
        mSeekBar = seekBar;
        final SeekBarVolumizer.Callback sbvc = new SeekBarVolumizer.Callback() {
            @Override
            public void onSampleStarting(SeekBarVolumizer sbv) {
                if (mCallback != null) {
                    mCallback.onSampleStarting(sbv);
                }
            }
        };
        final Uri sampleUri = mStream == AudioManager.STREAM_MUSIC ? getMediaVolumeUri() : null;
        if (mVolumizer == null) {
            mVolumizer = new SeekBarVolumizer(getContext(), mStream, sampleUri, sbvc, mKey);
        }
        //mVolumizer.setProfile(mKey);
        mVolumizer.setSeekBar(mSeekBar);
    }

    private Uri getMediaVolumeUri() {
        return Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://"
                + getContext().getPackageName()
                + "/" + R.raw.media_volume);
    }

    /**
     * The call back interface.
     */
    public interface Callback {
        /**
         * Set call back.
         *
         * @param sbv
         *            Call back object
         */
        void onSampleStarting(SeekBarVolumizer sbv);
    }

    /**
     * bind the preference with the profile.
     *
     * @param key
     *            the profile key
     */
    public void setProfile(String key) {
        mKey = key;
    }

    public SeekBarVolumizer getSeekBar() {
        return mVolumizer;
    }
}
