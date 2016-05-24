/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 *
 * MediaTek Inc. (C) 2014. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER ON
 * AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL WARRANTIES,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NONINFRINGEMENT.
 * NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH RESPECT TO THE
 * SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY, INCORPORATED IN, OR
 * SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES TO LOOK ONLY TO SUCH
 * THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. RECEIVER EXPRESSLY ACKNOWLEDGES
 * THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES
 * CONTAINED IN MEDIATEK SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK
 * SOFTWARE RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND
 * CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE RELEASED HEREUNDER WILL BE,
 * AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE,
 * OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE CHARGE PAID BY RECEIVER TO
 * MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek Software")
 * have been modified by MediaTek Inc. All revisions are subject to any receiver's
 * applicable license agreements with MediaTek Inc.
 */

package com.mediatek.audioprofile;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.UserHandle;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.util.Log;
import android.widget.Toast;

import com.android.settings.InstrumentedFragment;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.search.Indexable.SearchIndexProvider;
import com.android.settings.search.SearchIndexableRaw;

import com.mediatek.settings.FeatureOption;

import java.util.ArrayList;
import java.util.List;

public class SoundEnhancement extends SettingsPreferenceFragment implements Indexable {

    private static final String TAG = "SoundEnhancement";

    private Context mContext;
    private AudioProfileManager mProfileManager;
    private AudioManager mAudioManager = null;

    // Sound enhancement
    private static final String KEY_MUSIC_PLUS = "music_plus";
    private static final String KEY_SOUND_ENAHCNE = "sound_enhance";
    private static final String KEY_BESLOUDNESS = "bes_loudness";
    private static final String KEY_BESSURROUND = "bes_surround";
    private static final String KEY_LOSSLESSBT = "bes_lossless";

    // Audio enhance preference
    private SwitchPreference mMusicPlusPrf;
    //BesLoudness checkbox preference
    private SwitchPreference mBesLoudnessPref;
    private Preference mBesSurroundPref;
    //LosslessBT checkbox preference
    private SwitchPreference mLosslessBTPref;

    // the keys about set/get the status of driver
    private static final String GET_MUSIC_PLUS_STATUS = "GetMusicPlusStatus";
    private static final String GET_MUSIC_PLUS_STATUS_ENABLED = "GetMusicPlusStatus=1";
    private static final String SET_MUSIC_PLUS_ENABLED = "SetMusicPlusStatus=1";
    private static final String SET_MUSIC_PLUS_DISABLED = "SetMusicPlusStatus=0";

    // the params when set/get the status of besloudness
    private static final String GET_BESLOUDNESS_STATUS = "GetBesLoudnessStatus";
    private static final String GET_BESLOUDNESS_STATUS_ENABLED = "GetBesLoudnessStatus=1";
    private static final String SET_BESLOUDNESS_ENABLED = "SetBesLoudnessStatus=1";
    private static final String SET_BESLOUDNESS_DISABLED = "SetBesLoudnessStatus=0";

    // Sound enhance category has no preference
    private static final int SOUND_PREFERENCE_NULL_COUNT = 0;

    private static final String MTK_AUDENH_SUPPORT_State = "MTK_AUDENH_SUPPORT";
    private static final String MTK_AUDENH_SUPPORT_on = "MTK_AUDENH_SUPPORT=true";
    private static final String MTK_AUDENH_SUPPORT_off = "MTK_AUDENH_SUPPORT=false";

    // the params when set/get the status of losslessBT
    public static final String GET_LOSSLESSBT_STATUS = "LosslessBT_Status";
    public static final String GET_LOSSLESSBT_STATUS_ENABLED = "LosslessBT_Status=1";
    public static final String SET_LOSSLESSBT_ENABLED = "LosslessBT_Status=1";
    public static final String SET_LOSSLESSBT_DISABLED = "LosslessBT_Status=0";
    public static final String SET_LOSSLESSBT_USERID = "LosslessBT_UserId=";

    private String mAudenhState  = null;

    public static final int LOSSLESS_ICON_ID = R.drawable.bt_audio;
    public static final String CLOSE_LOSSLESS_NOTIFICATION =
            "android.intent.action.LOSSLESS_NOTIFICATION_CLOSE";
    public static final String LOSSLESS_ADD = "android.intent.action.LOSSLESS_ADD";
    public static final String LOSSLESS_CLOSE = "android.intent.action.LOSSLESS_CLOSE";
    public static final String LOSSLESS_PLAYING = "android.intent.action.LOSSLESS_PLAYING";
    public static final String LOSSLESS_STOP = "android.intent.action.LOSSLESS_STOP";
    public static final String LOSSLESS_NOT_SUPPORT = "android.intent.action.LOSSLESS_NOT_SUPPORT";

    private static final String NOTIFICATION_TAG = "Lossless_notification";

    private static final boolean LOSSLESS_SUPPORT = FeatureOption.MTK_LOSSLESS_SUPPORT;

    //Notification Manager
    private NotificationManager mNotificationManager;

    /**
     * called to do the initial creation of a fragment.
     *
     * @param icicle
     */
    public void onCreate(Bundle icicle) {
        Log.d("@M_" + TAG, "onCreate");
        super.onCreate(icicle);
        mContext = getActivity();

        mProfileManager = (AudioProfileManager) getSystemService(Context.AUDIO_PROFILE_SERVICE);
        mNotificationManager = ((NotificationManager) getSystemService(
                Context.NOTIFICATION_SERVICE));
        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        //Query the audenh state
        mAudenhState = mAudioManager.getParameters(MTK_AUDENH_SUPPORT_State);
        Log.d("@M_" + TAG, "AudENH state: " + mAudenhState);
        PreferenceScreen root = getPreferenceScreen();
        if (root != null) {
            root.removeAll();
        }
        addPreferencesFromResource(R.xml.audioprofile_sound_enhancement);

        // get the music plus preference
        mMusicPlusPrf = (SwitchPreference) findPreference(KEY_MUSIC_PLUS);
        mBesLoudnessPref = (SwitchPreference) findPreference(KEY_BESLOUDNESS);
        mBesSurroundPref = (Preference) findPreference(KEY_BESSURROUND);
        mLosslessBTPref = (SwitchPreference) findPreference(KEY_LOSSLESSBT);

        if (!mAudenhState.equalsIgnoreCase(MTK_AUDENH_SUPPORT_on)) {
            Log.d("@M_" + TAG, "remove audio enhance preference " + mMusicPlusPrf);
            getPreferenceScreen().removePreference(mMusicPlusPrf);
        }
        if (!FeatureOption.MTK_BESLOUDNESS_SUPPORT) {
            Log.d("@M_" + TAG, "feature option is off, remove BesLoudness preference");
            getPreferenceScreen().removePreference(mBesLoudnessPref);
        }
        if (!FeatureOption.MTK_BESSURROUND_SUPPORT) {
            Log.d("@M_" + TAG, "remove BesSurround preference " + mBesSurroundPref);
            getPreferenceScreen().removePreference(mBesSurroundPref);
        }
        if (!LOSSLESS_SUPPORT) {
            Log.d("@M_" + TAG, "feature option is off, remove BesLosslessPref preference");
            getPreferenceScreen().removePreference(mLosslessBTPref);
        }

        getActivity().registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.d("@M_" + TAG, "get close notification reciver.");
                mLosslessBTPref.setChecked(false);
            }
        }, new IntentFilter(CLOSE_LOSSLESS_NOTIFICATION));

        setHasOptionsMenu(false);
    }

    private void updatePreferenceHierarchy() {
        // update music plus state
        if (mAudenhState.equalsIgnoreCase(MTK_AUDENH_SUPPORT_on)) {
            String state = mAudioManager.getParameters(GET_MUSIC_PLUS_STATUS);
            Log.d("@M_" + TAG, "get the state: " + state);
            boolean isChecked = false;
            if (state != null) {
                isChecked = state.equals(GET_MUSIC_PLUS_STATUS_ENABLED) ? true
                        : false;
            }
            mMusicPlusPrf.setChecked(isChecked);
        }

        //update Besloudness preference state
        if (FeatureOption.MTK_BESLOUDNESS_SUPPORT) {
            String state = mAudioManager.getParameters(GET_BESLOUDNESS_STATUS);
            Log.d("@M_" + TAG, "get besloudness state: " + state);
            mBesLoudnessPref.setChecked(GET_BESLOUDNESS_STATUS_ENABLED.equals(state));
        }

        //update losslessBT preference state
        if (LOSSLESS_SUPPORT) {
            String state = mAudioManager.getParameters(GET_LOSSLESSBT_STATUS);
            Log.d("@M_" + TAG, "get losslessBT state: " + state);
            boolean checkedStatus = GET_LOSSLESSBT_STATUS_ENABLED.equals(state);
            Log.d("@M_" + TAG, "update the losslessBT state: " + checkedStatus);
            mLosslessBTPref.setChecked(checkedStatus);
        }
    }

    /**
     * called when the fragment is visible to the user Need to update summary
     * and active profile, register for the profile change.
     */
    public void onResume() {
        Log.d("@M_" + TAG, "onResume");
        super.onResume();
        updatePreferenceHierarchy();
    }

    /**
     * called when the fragment is unvisible to the user unregister the profile
     * change listener.
     */
    public void onPause() {
        super.onPause();
    }

    /**
     * Click the preference and enter into the EditProfile.
     *
     * @param preferenceScreen
     * @param preference
     *            the clicked preference
     * @return set success or fail
     */
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
            Preference preference) {

        // click the music plus checkbox
        if (mAudenhState.equalsIgnoreCase(MTK_AUDENH_SUPPORT_on)) {
            if (mMusicPlusPrf == preference) {
                boolean enabled = ((SwitchPreference) preference).isChecked();
                String cmdStr = enabled ? SET_MUSIC_PLUS_ENABLED : SET_MUSIC_PLUS_DISABLED;
                Log.d("@M_" + TAG, " set command about music plus: " + cmdStr);
                mAudioManager.setParameters(cmdStr);
            }
        }

        if (FeatureOption.MTK_BESLOUDNESS_SUPPORT) {
            if (mBesLoudnessPref == preference) {
                boolean enabled = ((SwitchPreference) preference).isChecked();
                String cmdStr = enabled ? SET_BESLOUDNESS_ENABLED : SET_BESLOUDNESS_DISABLED;
                Log.d("@M_" + TAG, " set command about besloudness: " + cmdStr);
                mAudioManager.setParameters(cmdStr);
            }
        }

        if (mBesSurroundPref == null) {
            Log.d("@M_" + TAG, " mBesSurroundPref = null");
        } else if (mBesSurroundPref.getKey() == null) {
            Log.d("@M_" + TAG, " mBesSurroundPref.getKey() == null)");
        }

        if (mBesSurroundPref == preference) {
            Log.d("@M_" + TAG, " mBesSurroundPref onPreferenceTreeClick");
            ((SettingsActivity) getActivity())
                    .startPreferencePanel(BesSurroundSettings.class.getName(),
                            null, -1, mContext.getText(R.string.audio_profile_bes_surround_title),
                            null, 0);
        }

        if (LOSSLESS_SUPPORT) {
            if (mLosslessBTPref == preference) {
                boolean enabled = ((SwitchPreference) preference).isChecked();
                if (enabled) {
                    Toast.makeText(getActivity(), R.string.lossless_toastmessage,
                            Toast.LENGTH_SHORT).show();
                    setLosslessStatus(SET_LOSSLESSBT_ENABLED);
                    mLosslessBTPref.setChecked(true);
                } else {
                    //Intent stopMusicIntent = new Intent(LOSSLESS_STOP_MUSIC);
                    //getActivity().sendBroadcastAsUser(stopMusicIntent, UserHandle.CURRENT);
                    mLosslessBTPref.setChecked(false);
                    cancelNotification(LOSSLESS_ICON_ID);
                    setLosslessStatus(SET_LOSSLESSBT_DISABLED);
                }
            }

        }

        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }


    private void setLosslessStatus(String keys) {
        Log.d("@M_" + TAG, " set command about losslessBT: " + keys);
        mAudioManager.setParameters(keys);
        if (keys.equals(SET_LOSSLESSBT_ENABLED)) {
            String losslessUserId = SET_LOSSLESSBT_USERID;
            losslessUserId = losslessUserId + UserHandle.myUserId();
            Log.d("@M_" + TAG, " LosslessBT userid cmd: " + losslessUserId);
            mAudioManager.setParameters(SET_LOSSLESSBT_USERID);
        }
    }

    private void addToNotification() {
        Log.d("@M_" + TAG, "Enable the lossless BT.");
        Intent addNotification = new Intent(LOSSLESS_ADD);
        getActivity().sendBroadcastAsUser(addNotification, UserHandle.CURRENT);
    }

    private void cancelNotification(int id) {
        mNotificationManager.cancelAsUser(NOTIFICATION_TAG, id, UserHandle.CURRENT);
    }

    public static final SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
    new BaseSearchIndexProvider() {
        @Override
        public List<SearchIndexableRaw> getRawDataToIndex(Context context, boolean enabled) {
            final List<SearchIndexableRaw> result = new ArrayList<SearchIndexableRaw>();
            final Resources res = context.getResources();

            SearchIndexableRaw data = new SearchIndexableRaw(context);
            data.title = res.getString(R.string.sound_enhancement_title);
            data.screenTitle = res.getString(R.string.sound_enhancement_title);
            data.keywords = res.getString(R.string.sound_enhancement_title);
            result.add(data);

            return result;
        }
    };

    @Override
    protected int getMetricsCategory() {
        return InstrumentedFragment.METRICS_AUDIOPROFILE;
    }
}

