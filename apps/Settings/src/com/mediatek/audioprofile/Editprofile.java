/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 *
 * MediaTek Inc. (C) 2010. All rights reserved.
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

import android.app.Activity;
import android.content.ContentQueryMap;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.ContentObserver;
import android.database.Cursor;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
//import android.net.sip.SipManager;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceScreen;
import android.preference.TwoStatePreference;
import android.provider.Settings;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.android.internal.telephony.PhoneConstants;
import com.android.settings.InstrumentedFragment;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;
import com.mediatek.audioprofile.AudioProfileManager.Scenario;
//import com.mediatek.gemini.GeminiUtils;
import com.mediatek.settings.FeatureOption;

import java.util.Observable;
import java.util.Observer;

/**
 * Edit profile fragment, started when one profile setting button is clicked.
 */
public class Editprofile extends SettingsPreferenceFragment {
    public static final String KEY_VIBRATE = "phone_vibrate";
    //public static final String KEY_VOLUME = "ring_volume";
    public static final String KEY_MEDIA_VOLUME = "media_volume";
    public static final String KEY_ALARM_VOLUME = "alarm_volume";
    public static final String KEY_RING_VOLUME = "ring_volume";
    private static final String KEY_NOTIFICATION_VOLUME = "notification_volume";
    public static final String KEY_RINGTONE = "phone_ringtone";
    public static final String KEY_VIDEO_RINGTONE = "video_call_ringtone";
    public static final String KEY_SIP_RINGTONE = "sip_call_ringtone";
    public static final String KEY_NOTIFY = "notifications_ringtone";
    public static final String KEY_DTMF_TONE = "audible_touch_tones";
    public static final String KEY_SOUND_EFFECTS = "audible_selection";
    public static final String KEY_LOCK_SOUNDS = "screen_lock_sounds";
    public static final String KEY_HAPTIC_FEEDBACK = "haptic_feedback";
    private static final String ACTION_SIM_SETTINGS = "com.android.settings.sim.SELECT_SUB";

    private static final String TAG = "AudioProfile/EditProfile";

    private TwoStatePreference mVibrat;
    private TwoStatePreference mDtmfTone;
    private TwoStatePreference mSoundEffects;
    private TwoStatePreference mHapticFeedback;
    private TwoStatePreference mLockSounds;

    //private RingerVolumePreference mVolumePref;
    private DefaultRingtonePreference mVoiceRingtone;
    private DefaultRingtonePreference mVideoRingtone;
    private DefaultRingtonePreference mSipRingtone;
    private DefaultRingtonePreference mNotify;

    private static final int SAMPLE_CUTOFF = 2000;  // manually cap sample playback at 2 seconds
    private final VolumePreferenceCallback mVolumeCallback = new VolumePreferenceCallback();
    private final H mHandler = new H();

    private Context mContext;
    private boolean mVoiceCapable;

    private boolean mIsSilentMode;
    private AudioProfileManager mProfileManager;

    private boolean mIsMeetingMode;

    private ContentQueryMap mContentQueryMap;

    private Observer mSettingsObserver;
    private String mKey;

    private long mSimId = -1;
    private int mCurOrientation;
    private TelephonyManager mTeleManager;
    private Cursor mSettingsCursor;
    private static final int REQUEST_CODE = 0;

    private String mSIMSelectorTitle;

    private static final int SINGLE_SIMCARD = 1;
    private int mSelectRingtongType = -1;

    public static final int RINGTONE_INDEX = 1;
    public static final int VIDEO_RINGTONE_INDEX = 2;
    public static final int SIP_RINGTONE_INDEX = 3;
    public static final int CONFIRM_FOR_SIM_SLOT_ID_REQUEST = 124;

    public VolumeSeekBarPreference mVolume;

    private final ContentObserver mRingtoneObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            mHandler.sendEmptyMessage(H.RINGTONE_CHANGE);
        }
    };

    /**
     * If Silent Mode, remove all sound selections, include Volume, Ringtone,
     * Notifications, touch tones, sound effects, lock sounds. For Volume,
     * Ringtone and Notifications, need to set the profile's Scenario.
     *
     * @param icicle
     *            the bundle which passed if the fragment recreated
     */
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        mContext = getActivity();

        addPreferencesFromResource(R.xml.edit_profile_prefs);
        mTeleManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        mVoiceCapable = Utils.isVoiceCapable(mContext);

        final SettingsActivity parentActivity = (SettingsActivity) getActivity();
        Bundle bundle = this.getArguments();

        Log.d("@M_" + TAG, "onCreate activity = " + parentActivity + ",bundle = "
                + bundle + ",this = " + this);

        mKey = bundle.getString("profileKey");

        mProfileManager = (AudioProfileManager) getSystemService(Context.AUDIO_PROFILE_SERVICE);
        Scenario scenario = AudioProfileManager.getScenario(mKey);

        mIsSilentMode = scenario.equals(Scenario.SILENT);
        mIsMeetingMode = scenario.equals(Scenario.MEETING);
        mSIMSelectorTitle = getActivity().getString(R.string.settings_label);

        initPreference();
    }

    /**
     * return true if the current device support sms service.
     *
     * @return
     */
    private boolean isSmsCapable() {
        return mTeleManager != null && mTeleManager.isSmsCapable();
    }

    /**
     * Register a contentObserve for CMCC load to detect whether vibrate in
     * silent profile
     */
    @Override
    public void onStart() {
        super.onStart();

        // Observer ringtone and notification changed
        mContext.getContentResolver().registerContentObserver(
                Settings.System.getUriFor(Settings.System.RINGTONE), false,
                mRingtoneObserver);

        // listen for vibrate_in_silent settings changes
        mSettingsCursor = getContentResolver().query(
                Settings.System.CONTENT_URI, null,
                "(" + Settings.System.NAME + "=?)",
                new String[] { AudioProfileManager.getVibrationKey(mKey) }, null);
        mContentQueryMap = new ContentQueryMap(mSettingsCursor,
                Settings.System.NAME, true, null);
    }

    /**
     * stop sampling and revert the volume(no save)for RingerVolumePreference
     * when the fragment is paused.
     */
    @Override
    public void onPause() {
        super.onPause();
        Log.d("@M_" + TAG, "onPause");
        mVolumeCallback.stopSample();
        if (mSettingsObserver != null) {
            mContentQueryMap.deleteObserver(mSettingsObserver);
        }
        if (mSettingsCursor != null) {
            mSettingsCursor.close();
            mSettingsCursor = null;
        }

    }

    @Override
    public void onStop() {
        super.onStop();
        mContext.getContentResolver().unregisterContentObserver(mRingtoneObserver);
     }

    /**
     * Init the preference and remove some in the silent profile.
     */
    private void initPreference() {
        PreferenceScreen parent = getPreferenceScreen();

        initVolume(parent);
        initRingtoneAndNotification(parent);
        initSystemAudio();
        //mVolumePref = (RingerVolumePreference) findPreference(KEY_VOLUME);

        if (mIsSilentMode || mIsMeetingMode) {
            removePrefWhenSilentOrMeeting(parent);
            return;
        }

        //if (mVolumePref != null) {
        //    mVolumePref.setProfile(mKey);
        //}

        if (mVoiceCapable) {
            initVoiceCapablePref(parent);
        } else {
            initNoVoiceCapablePref(parent);
        }
    }

    private void initVolume(PreferenceScreen parent) {
        initVolumePreference(KEY_ALARM_VOLUME, AudioManager.STREAM_ALARM);
        if (mVoiceCapable) {
            mVolume = initVolumePreference(KEY_RING_VOLUME, AudioManager.STREAM_RING);
            parent.removePreference(parent.findPreference(KEY_NOTIFICATION_VOLUME));
        } else {
            mVolume = initVolumePreference(KEY_NOTIFICATION_VOLUME,
                    AudioManager.STREAM_NOTIFICATION);
            parent.removePreference(parent.findPreference(KEY_RING_VOLUME));
        }
    }

    private void initRingtoneAndNotification(PreferenceScreen parent) {
        initNotification(parent);
        initRingtone(parent);
    }

    private void initNotification(PreferenceScreen parent) {
        mNotify = (DefaultRingtonePreference) parent.findPreference(KEY_NOTIFY);
        if (mNotify != null) {
            mNotify.setStreamType(DefaultRingtonePreference.NOTIFICATION_TYPE);
            mNotify.setProfile(mKey);
            mNotify.setRingtoneType(AudioProfileManager.TYPE_NOTIFICATION);
            mNotify.setNoNeedSIMSelector(true);
        }
    }

    private void initRingtone(PreferenceScreen parent) {
        mVoiceRingtone = (DefaultRingtonePreference) parent.findPreference(KEY_RINGTONE);
        mVideoRingtone = (DefaultRingtonePreference) parent.findPreference(KEY_VIDEO_RINGTONE);
        mSipRingtone = (DefaultRingtonePreference) parent.findPreference(KEY_SIP_RINGTONE);
    }

    private void initSystemAudio() {
        mVibrat = (TwoStatePreference) findPreference(KEY_VIBRATE);
        mDtmfTone = (TwoStatePreference) findPreference(KEY_DTMF_TONE);
        mSoundEffects = (TwoStatePreference) findPreference(KEY_SOUND_EFFECTS);
        mLockSounds = (TwoStatePreference) findPreference(KEY_LOCK_SOUNDS);
        mHapticFeedback = (TwoStatePreference) findPreference(KEY_HAPTIC_FEEDBACK);
        setPreferenceListener(KEY_VIBRATE, mVibrat);
        setPreferenceListener(KEY_DTMF_TONE, mDtmfTone);
        setPreferenceListener(KEY_SOUND_EFFECTS, mSoundEffects);
        setPreferenceListener(KEY_LOCK_SOUNDS, mLockSounds);
        setPreferenceListener(KEY_HAPTIC_FEEDBACK, mHapticFeedback);
    }

    private void removePrefWhenSilentOrMeeting(PreferenceScreen parent) {
        parent.removePreference(mDtmfTone);
        parent.removePreference(mSoundEffects);
        parent.removePreference(mLockSounds);
        parent.removePreference(mVoiceRingtone);
        parent.removePreference(mVideoRingtone);
        parent.removePreference(mSipRingtone);
        parent.removePreference(mNotify);
        mVibrat.setEnabled(false);
    }

    private void initVoiceCapablePref(PreferenceScreen parent) {
        parent.removePreference(mVideoRingtone);
        mVoiceRingtone.setTitle(R.string.ringtone_title);

        parent.removePreference(mSipRingtone);

        if (mVoiceRingtone != null) {
            mVoiceRingtone
                    .setStreamType(DefaultRingtonePreference.RING_TYPE);
            mVoiceRingtone.setProfile(mKey);
            mVoiceRingtone
                    .setRingtoneType(AudioProfileManager.TYPE_RINGTONE);
            if (!FeatureOption.MTK_MULTISIM_RINGTONE_SUPPORT) {
                mVoiceRingtone.setNoNeedSIMSelector(true);
            }
        }

        if (mVideoRingtone != null) {
            mVideoRingtone
                    .setStreamType(DefaultRingtonePreference.RING_TYPE);
            mVideoRingtone.setProfile(mKey);
            mVideoRingtone
                    .setRingtoneType(AudioProfileManager.TYPE_VIDEO_CALL);
            if (!FeatureOption.MTK_MULTISIM_RINGTONE_SUPPORT) {
                mVideoRingtone.setNoNeedSIMSelector(true);
            }
        }
    }

    private void initNoVoiceCapablePref(PreferenceScreen parent) {
        if (FeatureOption.MTK_PRODUCT_IS_TABLET) {
            mVibrat.setSummary(R.string.sms_vibrate_summary);
        }
        if (!isSmsCapable()) {
            parent.removePreference(mVibrat);
        }
        parent.removePreference(mDtmfTone);
        parent.removePreference(mVoiceRingtone);
        parent.removePreference(mVideoRingtone);
        parent.removePreference(mSipRingtone);
    }

    /**
     * Update the preference checked status from framework in onResume().
     */
    private void updatePreference() {
        mVibrat.setChecked(mProfileManager.isVibrationEnabled(mKey));
        mDtmfTone.setChecked(mProfileManager.isDtmfToneEnabled(mKey));
        mSoundEffects.setChecked(mProfileManager.isSoundEffectEnabled(mKey));
        mLockSounds.setChecked(mProfileManager.isLockScreenEnabled(mKey));
        mHapticFeedback.setChecked(mProfileManager.isVibrateOnTouchEnabled(mKey));
    }

    /**
     * Update the preference checked status.
     */
    @Override
    public void onResume() {
        super.onResume();
        updatePreference();
        if (mIsSilentMode) {
            if (mSettingsObserver == null) {
                mSettingsObserver = new Observer() {
                    public void update(Observable o, Object arg) {
                        Log.d("@M_" + TAG, "update");
                        if (mVibrat != null) {
                            final String name = AudioProfileManager.getVibrationKey(mKey);
                            Log.d("@M_" + TAG, "name " + name);
                            String vibrateEnabled = Settings.System.getString(
                                    getContentResolver(), name);
                            if (vibrateEnabled != null) {
                                mVibrat.setChecked("true"
                                        .equals(vibrateEnabled));
                                Log.d("@M_" + TAG,
                                        "vibrate setting is "
                                                + "true".equals(vibrateEnabled));
                            }

                        }
                    }
                };
                mContentQueryMap.addObserver(mSettingsObserver);
            }
        }
    }

    // === Volumes ===

    private VolumeSeekBarPreference initVolumePreference(String key, int stream) {
        Log.d("@M_" + TAG, "Init volume preference, key = " + key + ",stream = " + stream);
        final VolumeSeekBarPreference volumePref = (VolumeSeekBarPreference) findPreference(key);
        volumePref.setStream(stream);
        volumePref.setCallback(mVolumeCallback);
        volumePref.setProfile(mKey);

        return volumePref;
    }

    /**
     * Volume preference callback class.
     */
    private final class VolumePreferenceCallback implements VolumeSeekBarPreference.Callback {
        private SeekBarVolumizer mCurrent;

        @Override
        public void onSampleStarting(SeekBarVolumizer sbv) {
            if (mCurrent != null && mCurrent != sbv) {
                mCurrent.stopSample();
            }
            mCurrent = sbv;
            if (mCurrent != null) {
                mHandler.removeMessages(H.STOP_SAMPLE);
                mHandler.sendEmptyMessageDelayed(H.STOP_SAMPLE, SAMPLE_CUTOFF);
            }
        }

        public void onStreamValueChanged(int stream, int progress) {
            if (stream == AudioManager.STREAM_RING) {
                mHandler.removeMessages(H.UPDATE_RINGER_ICON);
                mHandler.obtainMessage(H.UPDATE_RINGER_ICON, progress, 0).sendToTarget();
            }
        }

        public void stopSample() {
            if (mCurrent != null) {
                mCurrent.stopSample();
            }
        }

        public void ringtoneChanged() {
            if (mCurrent != null) {
                mCurrent.ringtoneChanged();
            } else {
                mVolume.getSeekBar().ringtoneChanged();
            }
        }
    };

    /**
     * called when the preference is clicked.
     *
     * @param preferenceScreen
     *            the clicked preference which will be attached to
     * @param preference
     *            the clicked preference
     * @return true
     */
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
            Preference preference) {
        Log.d("@M_" + TAG, "Key :" + preference.getKey());
        if ((preference.getKey()).equals(KEY_RINGTONE)) {
            setRingtongTypeAndStartSIMSelector(RINGTONE_INDEX);
        } else if ((preference.getKey()).equals(KEY_VIDEO_RINGTONE)) {
            setRingtongTypeAndStartSIMSelector(VIDEO_RINGTONE_INDEX);
        }

        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    private void setPreferenceListener(final String preferenceType, Preference p) {
        p.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                setPreferenceChangeToDatabase((Boolean) newValue, preferenceType);
                return true;
            }
        });
    }

    private void setPreferenceChangeToDatabase(boolean isChecked, String preferenceType) {
        Log.d("@M_" + TAG, "Preference type :" + preferenceType);
        if (preferenceType.equals(KEY_VIBRATE)) {
            mProfileManager.setVibrationEnabled(mKey, isChecked);
        } else if (preferenceType.equals(KEY_DTMF_TONE)) {
            mProfileManager.setDtmfToneEnabled(mKey, isChecked);
        } else if (preferenceType.equals(KEY_SOUND_EFFECTS)) {
            mProfileManager.setSoundEffectEnabled(mKey, isChecked);
        } else if (preferenceType.equals(KEY_LOCK_SOUNDS)) {
            mProfileManager.setLockScreenEnabled(mKey, isChecked);
        } else if (preferenceType.equals(KEY_HAPTIC_FEEDBACK)) {
            mProfileManager.setVibrateOnTouchEnabled(mKey, isChecked);
        }
    }

    private void setRingtongTypeAndStartSIMSelector(int keyIndex) {
        Log.d("@M_" + TAG, "Selected ringtone type index = " + keyIndex);
        if (FeatureOption.MTK_MULTISIM_RINGTONE_SUPPORT) {
            //final int numSlots = mTeleManager.getSimCount();
            final int numSlots = SubscriptionManager.from(mContext)
                    .getActiveSubscriptionInfoCount();
            int simNum = numSlots;
            Log.d("@M_" + TAG, "simList.size() == " + simNum);

            if (simNum > SINGLE_SIMCARD) {
                mSelectRingtongType = keyIndex;
                setRingtoneType(keyIndex);
                startSIMCardSelectorActivity();
            }
        }
    }

    private void setRingtoneType(int keyIndex) {
        switch(keyIndex) {
            case RINGTONE_INDEX:
                mVoiceRingtone.setRingtoneType(AudioProfileManager.TYPE_RINGTONE);
                break;
            case VIDEO_RINGTONE_INDEX:
                mVideoRingtone.setRingtoneType(AudioProfileManager.TYPE_VIDEO_CALL);
                break;
            default:
                break;
        }
    }

    private void startSIMCardSelectorActivity() {
        Intent intent = new Intent();
        intent.setAction(ACTION_SIM_SETTINGS);
        startActivityForResult(intent, REQUEST_CODE);
    }

    /**
     * called when rotate the screen.
     *
     * @param newConfig
     *            the current new config
     */
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        Log.d("@M_" + TAG, "onConfigurationChanged: newConfig = " + newConfig
                + ",mCurOrientation = " + mCurOrientation + ",this = " + this);
        super.onConfigurationChanged(newConfig);
        if (newConfig != null && newConfig.orientation != mCurOrientation) {
            mCurOrientation = newConfig.orientation;
        }
        this.getListView().clearScrapViewsIfNeeded();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d("@M_" + TAG, "onActivityResult " + "requestCode " + requestCode + " "
                + resultCode + "resultCode");
        if (requestCode == REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                mSimId = data.getLongExtra(PhoneConstants.SUBSCRIPTION_KEY,
                                    SubscriptionManager.INVALID_SUBSCRIPTION_ID);
                setRingtoneSIMId(mSimId);
            }
            Log.v("@M_" + TAG, "Select SIM id = " + mSimId);
        }
    }

    private void setRingtoneSIMId(long simId) {
        switch(mSelectRingtongType) {
            case RINGTONE_INDEX:
                mVoiceRingtone.setSimId(simId);
                mVoiceRingtone.simSelectorOnClick();
                break;
            case VIDEO_RINGTONE_INDEX:
                mVideoRingtone.setSimId(simId);
                mVideoRingtone.simSelectorOnClick();
                break;
            default:
                break;
        }
    }

    /**
     * Edit profile hanlder.
     */
    private final class H extends Handler {
        private static final int UPDATE_PHONE_RINGTONE = 1;
        private static final int UPDATE_NOTIFICATION_RINGTONE = 2;
        private static final int STOP_SAMPLE = 3;
        private static final int UPDATE_RINGER_ICON = 4;
        private static final int RINGTONE_CHANGE = 5;

        private H() {
            super(Looper.getMainLooper());
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case STOP_SAMPLE:
                    mVolumeCallback.stopSample();
                    break;
                case RINGTONE_CHANGE:
                    Log.d("@M_" + TAG, "Ringtone changed.");
                    mVolumeCallback.ringtoneChanged();
                    break;
                default:
                    break;
            }
        }
    }

    @Override
    protected int getMetricsCategory() {
        return InstrumentedFragment.METRICS_AUDIOPROFILE;
    }
}
