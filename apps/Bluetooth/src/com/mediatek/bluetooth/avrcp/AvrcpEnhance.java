package com.mediatek.bluetooth.avrcp;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

/**
 * Enhanced AVRCP class.
 *
 * Main implementation of AVRCP1.5 media player selection feature.
 * It defines methods to get media player information data, control
 * media player start and get media player running status callback.
 *
 *
 * @hide
 */
public class AvrcpEnhance {
    public static final String TAG = AvrcpEnhance.class.getSimpleName();
    private static final boolean DEBUG = true;

    public static final int BTRC_FEATURE_MASK_SIZE = 16;

    public static final short BTRC_CHARSET_ID_ASCII = 0x0003;
    public static final short BTRC_CHARSET_ID_UTF8 = 0x006a;
    public static final short BTRC_CHARSET_ID_UTF16 = 0x03f7;
    public static final short BTRC_CHARSET_ID_UTF32 = 0x03f9;

    public static final int BTRC_STS_INTERNAL_ERR = 0x03;
    public static final int BTRC_STS_NO_ERROR = 0x04;
    public static final int BTRC_STS_BAD_PLAYER_ID = 0x11;
    public static final int BTRC_STS_PLAYER_N_ADDR = 0x13;
    public static final int BTRC_STS_NO_AVAL_PLAYER = 0x15;

    // Media player major player type bit map
    public static final int BTRC_MJ_TYPE_AUDIO = 0x01; /* Audio */
    public static final int BTRC_MJ_TYPE_VIDEO = 0x02; /* Video */
    public static final int BTRC_MJ_TYPE_BC_AUDIO = 0x04; /* Broadcasting Audio */
    public static final int BTRC_MJ_TYPE_BC_VIDEO = 0x08; /* Broadcasting Video */
    public static final int BTRC_MJ_TYPE_BC_NONE = 0x00;

    // Media player play status
    public static final int BTRC_PLAYSTATE_STOPPED = 0x00; /* Stopped */
    public static final int BTRC_PLAYSTATE_PLAYING = 0x01; /* Playing */
    public static final int BTRC_PLAYSTATE_PAUSED = 0x02; /* Paused */
    public static final int BTRC_PLAYSTATE_FWD_SEEK = 0x03; /* Fwd Seek */
    public static final int BTRC_PLAYSTATE_REV_SEEK = 0x04; /* Rev Seek */
    public static final int BTRC_PLAYSTATE_ERROR = 0xFF; /* Error */

    // Player feature bit mask
    public static final int BTRC_SELECT_MASK = 0;
    public static final int BTRC_UP_MASK = 1;
    public static final int BTRC_DOWN_MASK = 2;
    public static final int BTRC_LEFT_MASK = 3;
    public static final int BTRC_RIGHT_MASK = 4;
    public static final int BTRC_RIGHTUP_MASK = 5;
    public static final int BTRC_RIGHTDOWN_MASK = 6;
    public static final int BTRC_LEFTUP_MASK = 7;

    public static final int BTRC_LEFTDOWN_MASK = 8;
    public static final int BTRC_ROOT_MENU_MASK = 9;
    public static final int BTRC_SETUP_MENU_MASK = 10;
    public static final int BTRC_CONTENTS_MENU_MASK = 11;
    public static final int BTRC_FAVORITE_MENU_MASK = 12;
    public static final int BTRC_EXIT_MASK = 13;
    public static final int BTRC_0_MASK = 14;
    public static final int BTRC_1_MASK = 15;

    public static final int BTRC_2_MASK = 16;
    public static final int BTRC_3_MASK = 17;
    public static final int BTRC_4_MASK = 18;
    public static final int BTRC_5_MASK = 19;
    public static final int BTRC_6_MASK = 20;
    public static final int BTRC_7_MASK = 21;
    public static final int BTRC_8_MASK = 22;
    public static final int BTRC_9_MASK = 23;

    public static final int BTRC_DOT_MASK = 24;
    public static final int BTRC_ENTER_MASK = 25;
    public static final int BTRC_CLEAR_MASK = 26;
    public static final int BTRC_CHNL_UP_MASK = 27;
    public static final int BTRC_CHNL_DOWN_MASK = 28;
    public static final int BTRC_PREV_CHNL_MASK = 29;
    public static final int BTRC_SOUND_SEL_MASK = 30;
    public static final int BTRC_INPUT_SEL_MASK = 31;

    public static final int BTRC_DISP_INFO_MASK = 32;
    public static final int BTRC_HELP_MASK = 33;
    public static final int BTRC_PAGE_UP_MASK = 34;
    public static final int BTRC_PAGE_DOWN_MASK = 35;
    public static final int BTRC_POWER_MASK = 36;
    public static final int BTRC_VOL_UP_MASK = 37;
    public static final int BTRC_VOL_DOWN_MASK = 38;
    public static final int BTRC_MUTE_MASK = 39;

    public static final int BTRC_PLAY_MASK = 40;
    public static final int BTRC_STOP_MASK = 41;
    public static final int BTRC_PAUSE_MASK = 42;
    public static final int BTRC_RECORD_MASK = 43;
    public static final int BTRC_REWIND_MASK = 44;
    public static final int BTRC_FAST_FWD_MASK = 45;
    public static final int BTRC_EJECT_MASK = 46;
    public static final int BTRC_FORWARD_MASK = 47;

    public static final int BTRC_BACKWARD_MASK = 48;
    public static final int BTRC_ANGLE_MASK = 49;
    public static final int BTRC_SUBPICTURE_MASK = 50;
    public static final int BTRC_F1_MASK = 51;
    public static final int BTRC_F2_MASK = 52;
    public static final int BTRC_F3_MASK = 53;
    public static final int BTRC_F4_MASK = 54;
    public static final int BTRC_F5_MASK = 55;

    public static final int BTRC_VENDOR_MASK = 56;
    public static final int BTRC_GROUP_NAVI_MASK = 57;
    public static final int BTRC_ADV_CTRL_MASK = 58;
    public static final int BTRC_BROWSE_MASK = 59;
    public static final int BTRC_SEARCH_MASK = 60;
    public static final int BTRC_ADD2NOWPLAY_MASK = 61;
    public static final int BTRC_UID_UNIQUE_MASK = 62;
    public static final int BTRC_BR_WH_ADDR_MASK = 63;

    public static final int BTRC_SEARCH_WH_ADDR_MASK = 64;
    public static final int BTRC_NOW_PLAY_MASK = 65;
    public static final int BTRC_UID_PERSIST_MASK = 66;
    public static final int BTRC_NUMBER_OF_ITEM_MASK = 67;
    public static final int BTRC_COVER_ART_MASK = 68;

    public static final int BTRC_SCOPE_PLAYER_LIST = 0x00;
    public static final int BTRC_SCOPE_FILE_SYSTEM = 0x01;
    public static final int BTRC_SCOPE_SEARCH = 0x02;
    public static final int BTRC_SCOPE_NOW_PLAYING = 0x03;

    private static final int BTRC_BROWSABLE_ITEM_TYPE_PLAYER = 1;
    private static final int BTRC_BROWSABLE_ITEM_TYPE_FOLDER = 2;
    private static final int BTRC_BROWSABLE_ITEM_TYPE_MEDIA = 3;

    private static final int MEDIA_PLAYER_ADDED = 1;
    private static final int MEDIA_PLAYER_REMOVED = 2;
    private static final int MEDIA_PLAYER_REPLACED = 3;

    private final static int NOTIFICATION_TYPE_INTERIM = 0;
    private final static int NOTIFICATION_TYPE_CHANGED = 1;
    private final static int NOTIFICATION_TYPE_REJECTED = 2;

    private static final int MESSAGE_SET_ADDR_PLAYER = 200;
    private static final int MESSAGE_GET_FOLDER_ITEMS = 201;
    private static final int MESSAGE_GET_FOLDER_ITEMS_NUM = 202;

    final static int EVT_AVAL_PLAYERS_CHANGE = 0x0a;
    final static int EVT_ADDR_PLAYER_CHANGE = 0x0b;

    private static final String DEFAULT_MEDIAPLAYER_PACKAGE_NAME = "com.android.music";
    private static final Short sDefaultPlayerId = 0;
    private static Short sPlayerIdCounter = 1;

    private Context mContext;
    private AppInstallReceiver mReceiver;
    private Timer mTimer = new Timer();
    private boolean mTimerIsRunning = false;
    private HashMap<Short, MediaPlayerItem> mPlayerCache = new HashMap<Short, MediaPlayerItem>();
    private HashMap<String, Short> mPlayerPkgMap = new HashMap<String, Short>();

    private short mCurrentPlayerId = -1;
    private boolean mIsCurrentPlayerInterim = false;
    private boolean mPlayerInStage = false;
    private boolean mPlayerInStageIsChanged = false;

    private int mAvalPlayerChangedNT;
    private int mAddrPlayerChangedNT;
    private AvalPlayerChangeListener mAvalListener = null;
    private AddrPlayerChangeListener mAddrListener = null;

    private MediaPlayerStatusChangeCallback mStatusChangeCallback = null;
    private Handler mHandler = null;

    /**
     * interface to notify available media player changes.
     * @hide
     */
    public static interface MediaPlayerStatusChangeCallback {
        /**
         * Notify media player play/track/playpos rejected.
         */
        public void onPlayerStatusReject();
    }

    /**
     * Media player full name class.
     * @hide
     */
    static class MediaPlayerFullName {
        short mCharSetId;
        short mLength;
        byte mFullName[];
        @Override
        public String toString() {
            return "(" + new String(mFullName, StandardCharsets.UTF_8) + ")";
        }
    }

    /**
     * media player data structure.
     * data structure definition matches media player data defined in bt_rc.h
     * @hide
     */
    static class MediaPlayerData {
        short mPlayerId;
        byte mMajorType;
        int mSubType;
        int mPlayStatus;
        byte mFeatures[];
        MediaPlayerFullName mFullName;
        @Override
        public String toString() {
            return "[PlayerID: " + mPlayerId
                    + ", MajorType: " + Byte.toString(mMajorType)
                    + ", SubType: " + mSubType
                    + ", PlayStatus: " + mPlayStatus
                    + ", Features: " + Arrays.toString(mFeatures)
                    + ", FullName: " + mFullName.toString()
                    + "]";
        }
    }

    /**
     * media player browsable data structure.
     * data structure definition matches media player browsable data
     * defined in bt_rc.h
     * @hide
     */
    static class MediaPlayerBrowsableData {
        int mBrowsableType;
        MediaPlayerData mData;
        @Override
        public String toString() {
            return "[BrowsableType: " + mBrowsableType
                    + ", Data: " + mData.toString()
                    + "]";
        }
    }

    /**
     * Media Player Item definition.
     * @hide
     */
    static class MediaPlayerItem {
        short mPlayerId;
        String mPackageName;
        String mProcessName;
        int mIsForeground;
        boolean mIsBackground;
        MediaPlayerBrowsableData mData;
        @Override
        public String toString() {
            return "PlayerID: " + mPlayerId
                    + ", PackageName: " + mPackageName
                    + ", ProcessName: " + mProcessName
                    + ", MediaPlayerData: " + mData.toString();
        }
    }

    /**
     * to get addressed player change callback.
     * @hide
     */
    private class AddrPlayerChangeListener {
        /**
         * Notify addressed media player changed.
         * @param playerId the media player id which it changes to
         * @param uidCounter the uidcounter.
         *                   At present, do not support this field.
         * @hide
         */
        public void onAddrPlayerChanged(short playerId, short uidCounter) {
            Log.i(TAG, "Receive on Addr Player Changed:"
                    + playerId + ", mAddrPlayerChangedNT: " + mAddrPlayerChangedNT);
            if (mAddrPlayerChangedNT == NOTIFICATION_TYPE_INTERIM) {
                mAddrPlayerChangedNT = NOTIFICATION_TYPE_CHANGED;
                Log.i(TAG, "registerNotificationRspAddrPlayerNative(CHANGED) playerId:"
                                + playerId);
                registerNotificationRspAddrPlayerNative(mAddrPlayerChangedNT,
                        playerId, uidCounter);
                if (mStatusChangeCallback != null) {
                    mStatusChangeCallback.onPlayerStatusReject();
                }
            }
        }
    }

    /**
     * to get Available players change callback.
     * @hide
     */
    private class AvalPlayerChangeListener {
        /**
         * Notify available media player changed.
         * @param playerId the id of the changed media player.
         *                 e.g. added or deleted
         * @param uidCounter the uidcounter.
         *                   At present, do not support this field.
         * @hide
         */
        public void onAvalPlayerChanged(short playerId, short uidCounter) {
            if (mAvalPlayerChangedNT == NOTIFICATION_TYPE_INTERIM) {
                mAvalPlayerChangedNT = NOTIFICATION_TYPE_CHANGED;
                Log.i(TAG, "registerNotificationRspAvalPlayerNative(CHANGED)");
                registerNotificationRspAvalPlayerNative(mAvalPlayerChangedNT);
            }
        }
    }

    /**
     * A internal receiver to receive package install/removed intent.
     *
     * @hide
     */
    private class AppInstallReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i(TAG, "onReceive");
            String action = intent.getAction();
            if (action != null && action.equals(Intent.ACTION_PACKAGE_ADDED)) {
                String packageName = intent.getData().getSchemeSpecificPart();
                Log.i(TAG, "New App Install, PackageName:" + packageName);
                processAppUpdate(packageName, MEDIA_PLAYER_ADDED);
            } else if (action != null
                    && action.equals(Intent.ACTION_PACKAGE_REMOVED)) {
                String packageName = intent.getData().getSchemeSpecificPart();
                Log.i(TAG, "An App Removed, PackageName:" + packageName);
                processAppUpdate(packageName, MEDIA_PLAYER_REMOVED);
            } else if (action != null
                    && action.equals(Intent.ACTION_PACKAGE_REPLACED)) {
                String packageName = intent.getData().getSchemeSpecificPart();
                Log.i(TAG, "An App Replaced, PackageName:" + packageName);
                processAppUpdate(packageName, MEDIA_PLAYER_REPLACED);
            }
        }
    }

    private void processAppUpdate(String pkgName, int type) {
        //if non-initialize, initial firstly.
        if (mCurrentPlayerId == -1) {
            Log.i(TAG, "update MediaPlayer. non-initialzed before, init firstly");
            getAvailableMediaPlayerItems(false);
        }
        Log.i(TAG, "update MediaPlayer. Package:" + pkgName + ", Type:" + type);
        switch(type) {
        case MEDIA_PLAYER_ADDED: {
            PackageManager pm = mContext.getPackageManager();
            Intent intent = new Intent(Intent.ACTION_MEDIA_BUTTON);
            List<ResolveInfo> pkgMediaReceiversList = pm.queryBroadcastReceivers(
                    intent, 0);
            for (ResolveInfo info: pkgMediaReceiversList) {
                if (info != null && info.activityInfo != null
                        && pkgName.equals(info.activityInfo.packageName)) {
                    short playerId = addPlayerId(info, pm);
                    Log.i(TAG, "Install MediaPlayer Item: "
                                    + mPlayerCache.get(playerId).toString());
                    if (mAvalListener != null) {
                        mAvalListener.onAvalPlayerChanged(playerId, (short) 0);
                    }
                    return;
                }
            }
            break;
        }
        case MEDIA_PLAYER_REMOVED: {
            if (!mPlayerPkgMap.containsKey(pkgName)) {
                return;
            }
            Short playerId = mPlayerPkgMap.get(pkgName);
            MediaPlayerItem playerItem = mPlayerCache.remove(playerId);
            mPlayerPkgMap.remove(pkgName);
            Log.i(TAG, "Remove PlayerItem: " + playerId + ", PackageName:"
                    + pkgName);
            if (mCurrentPlayerId == playerId) {
                mCurrentPlayerId = sDefaultPlayerId;
            }
            if (mAvalListener != null) {
                mAvalListener.onAvalPlayerChanged(playerId, (short) 0);
            }
            break;
        }
        case MEDIA_PLAYER_REPLACED: {
            //TODO: do nothing now.
            break;
        }
        default:
            break;
        }
    }

    /**
     * Android provides 3 intents to distinguish a music application.
     * Those are,
     *     1. MediaStore.INTENT_ACTION_MUSIC_PLAYER
     *     2. Intent.ACTION_MAIN works with Intent.CATEGORY_APP_MUSIC
     *     3. Intent.ACTION_MEDIA_BUTTON
     * The default music application implement the above 3 intents.
     * However, many 3rd party APPs may not follow android design to implement
     * a media player.
     * Here uses the "Intent.ACTION_MEDIA_BUTTON" to decide whether it is a
     * media player application, because if an APP supports this intent,
     * it may support start/pause/fast-forward/fast-backward sent from CT to TG.
     */
    private ArrayList<Object> getAvailableMediaPlayerItems(boolean needResult) {
        short maxPlayerId = -1;
        PackageManager pm = mContext.getPackageManager();
        ActivityManager am = (ActivityManager) mContext
                .getSystemService(Context.ACTIVITY_SERVICE);
        Intent intent = new Intent(Intent.ACTION_MEDIA_BUTTON);
        List<ResolveInfo> pkgMediaReceiversList = pm.queryBroadcastReceivers(
                intent, 0);
        Log.i(TAG, "Print ResolveInfo: " + pkgMediaReceiversList.size());

        ArrayList<String> prevKeyArray = new ArrayList<String>(mPlayerPkgMap.keySet());
        for (ResolveInfo info : pkgMediaReceiversList) {
            if (info.activityInfo != null) {
                short playerId;
                if (!mPlayerPkgMap.containsKey(info.activityInfo.packageName)) {
                    playerId = addPlayerId(info, pm);
                    // If current player is running background before starting
                    // AVRCP service
                    if (am.getPackageImportance(info.activityInfo.packageName)
                            < RunningAppProcessInfo.IMPORTANCE_SERVICE) {
                        if (mCurrentPlayerId == -1
                                || !mPlayerCache.containsKey(mCurrentPlayerId)) {
                            mCurrentPlayerId = playerId;
                        }
                    }
                } else {
                    playerId = mPlayerPkgMap.get(info.activityInfo.packageName);
                }
                if (prevKeyArray.contains(info.activityInfo.packageName)) {
                    prevKeyArray.remove(info.activityInfo.packageName);
                }
                if (playerId > maxPlayerId) {
                    maxPlayerId = playerId;
                }
            }
        }
        // M: In some case user may not register Available Media Player change
        // listener, so we cannot monitor APP install/uninstall status.
        // But if TG wants to get player item, we should return the existing
        // media players and remove the missed media player
        if (prevKeyArray.size() > 0) {
            for (String pkgName : prevKeyArray) {
                Short playerId = mPlayerPkgMap.get(pkgName);
                MediaPlayerItem playerItem = mPlayerCache.remove(playerId);
                mPlayerPkgMap.remove(pkgName);
                Log.i(TAG, "Remove unknown PlayerItem: " + playerId + ", PackageName:"
                        + pkgName);
            }
        }
        // update current media player ID if it is invalid.
        if (mCurrentPlayerId == -1
                || !mPlayerCache.containsKey(mCurrentPlayerId)) {
            mCurrentPlayerId = sDefaultPlayerId;
        }
        if (DEBUG) {
            Log.i(TAG, "=============Print Player Cache Start=============");
            Short[] playerIdArray = mPlayerCache.keySet().toArray(new Short[0]);
            for (short playerID : playerIdArray) {
                Log.i(TAG, mPlayerCache.get(playerID).toString());
                Log.i(TAG, "------------------------------------");
            }
            Log.i(TAG, "=============Print Player Cache End=============");
        }
        if (needResult) {
            ArrayList<Object> result = new ArrayList<Object>();
            for (short i = 0; i <= maxPlayerId; i++) {
                if (mPlayerCache.containsKey(i)) {
                    result.add(mPlayerCache.get(i).mData);
                }
            }
            return result;
        }
        return null;
    }

    private void startAddressedPlayer(short playerId) {
        MediaPlayerItem player = mPlayerCache.get(playerId);
        if (player == null || player.mPackageName == null
                || player.mPackageName.length() < 1) {
            Log.e(TAG, "startAddressedPlayer Invalid Player Item. PlayerId:"
                    + playerId
                    + ", PackageName:"
                    + ((player == null) ? "Player is NULL"
                            : player.mPackageName));
            return;
        }
        Intent intent = mContext.getPackageManager().getLaunchIntentForPackage(
                player.mPackageName);
        mContext.startActivity(intent);
        Log.i(TAG, "startAddressedPlayer PlayerId:" + playerId
                + ", PackageName:"
                + ((player == null) ? "Player is NULL"
                        : player.mPackageName));
    }

    private void stopAddressedPlayer(short playerId) {
        MediaPlayerItem player = mPlayerCache.get(playerId);
        if (player == null || player.mPackageName == null
                || player.mPackageName.length() < 1) {
            Log.e(TAG, "stopAddressedPlayer Invalid Player Item. PlayerId:"
                    + playerId
                    + ", PackageName:"
                    + ((player == null) ? "Player is NULL"
                            : player.mPackageName));
            return;
        }
        ActivityManager am = (ActivityManager) mContext
                .getSystemService(Context.ACTIVITY_SERVICE);
        List<RunningAppProcessInfo> runningAppProcessInfo = am
                .getRunningAppProcesses();

        int pid = -1;
        for (RunningAppProcessInfo appProcess : runningAppProcessInfo) {
            Log.d(appProcess.processName.toString(), "is running");
            if (appProcess.processName.equals(player.mPackageName)) {
                pid = appProcess.pid;
                break;
            }
        }
        if (pid > 0) {
            Log.i(TAG, "Kill MediaPlayer(ID:" + playerId + ","
                    + player.mPackageName + ") pid:" + pid);
            android.os.Process.killProcess(pid);
        } else {
            Log.e(TAG, "Can not find MediaPlayer(ID:" + playerId + ","
                    + player.mPackageName + ") process id.");
        }
    }

    private int setAddressedPlayerItem(short playerId) {
        //NOTE: player id is 16bit unsigned short,
        //so the valid range is [0 ~ 2^16-1]
        if (playerId < 0 || playerId >= 65535) {
            return BTRC_STS_BAD_PLAYER_ID;
        }
        if (mPlayerCache.size() == 0) {
            return BTRC_STS_NO_AVAL_PLAYER;
        }
        if (!mPlayerCache.containsKey(playerId)) {
            return BTRC_STS_BAD_PLAYER_ID;
        }
        // Stop Previous Media Player
        stopAddressedPlayer(mCurrentPlayerId);
        // Start the desired Media Player
        startAddressedPlayer(playerId);
        mCurrentPlayerId = playerId;
        mIsCurrentPlayerInterim = true;
        return BTRC_STS_NO_ERROR;
    }

    private void startAppStatusMonitor() {
        if (mTimerIsRunning) {
            Log.i(TAG, "Timer is already running...");
            return;
        }
        mTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                ActivityManager am = (ActivityManager) mContext
                        .getSystemService(Context.ACTIVITY_SERVICE);
                String[] playerPkgNameArray = mPlayerPkgMap.keySet().toArray(
                        new String[0]);
                for (String playerPackageName : playerPkgNameArray) {
                    Log.i(TAG, "[Monitor]PlayerPackageName: "
                            + playerPackageName);
                    Short playerId = mPlayerPkgMap.get(playerPackageName);
                    MediaPlayerItem playItem = mPlayerCache.get(playerId);
                    if (am.getPackageImportance(playerPackageName)
                            == RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                        if (playItem.mIsForeground == 0) {
                            playItem.mIsForeground = 1;
                            Log.d(TAG, "[Monitor]Media Player "
                                    + playItem.mPackageName
                                    + " has been launched");
                        } else if (playItem.mIsBackground) {
                            playItem.mIsForeground++;
                            playItem.mIsBackground = false;
                            Log.d(TAG, "[Monitor]Media Player "
                                    + playItem.mPackageName + " is relaunched");
                        }
                        Log.d(TAG, "[Monitor]Curr Player Id: "
                                + mCurrentPlayerId + ",TargetPlayerId: "
                                + playerId + ", Curr Interim: "
                                + mIsCurrentPlayerInterim
                                + ",curr player in Stage: " + mPlayerInStage
                                + ", player Stage State is Changed: "
                                + mPlayerInStageIsChanged);
                        if (mCurrentPlayerId == playerId) {
                            // Addressed Media Player is changed by CT
                            if (mIsCurrentPlayerInterim) {
                                if (mAddrListener != null) {
                                    mAddrListener.onAddrPlayerChanged(playerId,
                                            (short) 0);
                                }
                                mIsCurrentPlayerInterim = false;
                            }
                            // reset mPlayerInStageIsChanged status, because it
                            // is already notified
                            if (mPlayerInStageIsChanged) {
                                mPlayerInStageIsChanged = false;
                            }
                        } else {
                            // Addressed Media Player is locally changed in TG
                            // by user
                            if (mPlayerInStageIsChanged) {
                                if (mAddrListener != null) {
                                    mAddrListener.onAddrPlayerChanged(playerId,
                                            (short) 0);
                                }
                                mCurrentPlayerId = playerId;
                                mPlayerInStageIsChanged = false;
                            } else {
                                // check while current media player's process is
                                // still running
                                MediaPlayerItem item = mPlayerCache
                                        .get(mCurrentPlayerId);
                                if (item == null
                                        || am.getPackageImportance(item.mPackageName)
                                        >= RunningAppProcessInfo.IMPORTANCE_SERVICE) {
                                    if (mAddrListener != null) {
                                        mAddrListener.onAddrPlayerChanged(
                                                playerId, (short) 0);
                                    }
                                    mCurrentPlayerId = playerId;
                                }
                            }
                        }
                    } else {
                        if (!playItem.mIsBackground) {
                            playItem.mIsBackground = true;
                            Log.d(TAG, "Media Player " + playItem.mPackageName
                                    + " goes into background");
                        }
                    }
                }
            }
        }, 100, 5000);
        mTimerIsRunning = true;
    }

    private void stopAppStatusMonitor() {
        mTimer.cancel();
        mTimerIsRunning = false;
    }

    private Short genPlayerId(String pkgName) {
        if (DEFAULT_MEDIAPLAYER_PACKAGE_NAME.equals(pkgName)) {
            return sDefaultPlayerId;
        }
        int loopCount = 0;
        do {
            if (!mPlayerCache.containsKey(sPlayerIdCounter)) {
                return sPlayerIdCounter++;
            }
            sPlayerIdCounter++;
            if (sPlayerIdCounter > 1000) {
                sPlayerIdCounter = 1;
            }
        } while(loopCount <= 1000);
        Log.e(TAG, "All PlayerId is used.");
        return -1;
    }

    private short addPlayerId(ResolveInfo info, PackageManager pm) {
        MediaPlayerItem playerItem = new MediaPlayerItem();
        playerItem.mPlayerId = genPlayerId(info.activityInfo.packageName);
        playerItem.mPackageName = info.activityInfo.packageName;
        playerItem.mProcessName = info.activityInfo.processName;
        playerItem.mIsForeground = 0;
        playerItem.mIsBackground = false;

        MediaPlayerFullName fullName = new MediaPlayerFullName();
        CharSequence label = info.loadLabel(pm);
        fullName.mLength = (short) label.length();
        fullName.mCharSetId = BTRC_CHARSET_ID_UTF8;
        fullName.mFullName = label.toString().getBytes(
                StandardCharsets.UTF_8);

        MediaPlayerData data = new MediaPlayerData();
        data.mPlayerId = playerItem.mPlayerId;
        data.mMajorType = BTRC_MJ_TYPE_AUDIO;
        data.mSubType = BTRC_MJ_TYPE_BC_NONE;
        data.mPlayStatus = BTRC_PLAYSTATE_STOPPED;
        BitSet bs = new BitSet(BTRC_FEATURE_MASK_SIZE * 8);
        bs.set(BTRC_PLAY_MASK);
        bs.set(BTRC_STOP_MASK);
        bs.set(BTRC_PAUSE_MASK);
        bs.set(BTRC_REWIND_MASK);
        bs.set(BTRC_FAST_FWD_MASK);
        bs.set(BTRC_FORWARD_MASK);
        bs.set(BTRC_BACKWARD_MASK);
        data.mFeatures = new byte[BTRC_FEATURE_MASK_SIZE];
        System.arraycopy(bs.toByteArray(), 0, data.mFeatures, 0,
                bs.toByteArray().length);
        data.mFullName = fullName;

        MediaPlayerBrowsableData browsableData = new MediaPlayerBrowsableData();
        browsableData.mBrowsableType = BTRC_BROWSABLE_ITEM_TYPE_PLAYER;
        browsableData.mData = data;
        playerItem.mData = browsableData;

        mPlayerCache.put(playerItem.mPlayerId, playerItem);
        mPlayerPkgMap.put(playerItem.mPackageName, playerItem.mPlayerId);

        Log.i(TAG, "Add PlayerItem: " + playerItem.toString());
        return playerItem.mPlayerId;
    }

    static {
        classInitNative();
    }

    /**
     * Construct function.
     * @param context AVRCP profile context
     * @param callback callback interface implementation object
     *
     * @hide
     */
    public AvrcpEnhance(Context context,
                        MediaPlayerStatusChangeCallback callback) {
        mContext = context;
        mAvalPlayerChangedNT = NOTIFICATION_TYPE_CHANGED;
        mAddrPlayerChangedNT = NOTIFICATION_TYPE_CHANGED;
        mStatusChangeCallback = callback;
        initNative();
    }

    /**
     * called when AVRCP profile starts.
     * Do some initialization here if needs.
     *
     * @param handler AVRCP message handler
     *                We use this handler to send mesage
     *                to AVRCP.java's handler
     *
     * @hide
     */
    public void start(Handler handler) {
        mHandler = handler;
    }

    /**
     * called when AVRCP profile stops.
     * Do clean here if needs.
     *
     * @hide
     */
    public void stop() {
        cleanAddrPlayerChangeListener();
        cleanAvalPlayerChangeListener();
        Log.d(TAG, "AvrcpEnhance is cleaned.");
    }

    /**
     * @hide
     */
    public void cleanup() {
        cleanupNative();
    }

    /**
     * set whether media player is clearing.
     *
     * @param clear clear flag.
     *              When media player starts, it sets flag to 0;
     *              When media player quits, it sets flag to 1;
     *
     * @hide
     */
    public void setPlayerChangeFlag(int clear) {
        Log.i(TAG, "Set Player Changed Flag: " + clear);
        boolean newState = (clear == 0) ? true : false;
        if (!mPlayerInStage && newState) {
            mPlayerInStageIsChanged = true;
        }
        mPlayerInStage = newState;
    }

    /**
     * Get current media player id.
     * Current media player should be the last media player user used.
     * @return cached current media player id
     *
     * @hide
     */
    public short getCurrentAddressedPlayer() {
        if (mCurrentPlayerId == -1) {
            getAvailableMediaPlayerItems(false);
        }
        return mCurrentPlayerId;
    }

    /**
     * Set available media player changes listener.
     * @param listener listener object
     *
     * @hide
     */
    public synchronized void setAvalPlayerChangeListener(
            AvalPlayerChangeListener listener) {
        if (mReceiver == null) {
            mReceiver = new AppInstallReceiver();
            IntentFilter filter = new IntentFilter();
            filter.addAction(Intent.ACTION_PACKAGE_ADDED);
            filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
            filter.addAction(Intent.ACTION_PACKAGE_CHANGED);
            filter.addDataScheme("package");
            mContext.registerReceiver(mReceiver, filter);
            Log.i(TAG, "Register Receiver");
        }
        mAvalListener = listener;
        Log.i(TAG, "Set Aval Player Change Listener: " + listener);
    }

    /**
     * Clear available media player changes listener.
     *
     * @hide
     */
    public synchronized void cleanAvalPlayerChangeListener() {
        if (mReceiver != null) {
            mContext.unregisterReceiver(mReceiver);
            Log.i(TAG, "unregister Receiver");
            mReceiver = null;
        }
        if (mAvalListener != null) {
            mAvalListener = null;
        }
        Log.i(TAG, "clean Aval Player Change Listener: " + mAvalListener);
    }

    /**
     * Set addressed media player changes listener.
     * @param listener listener object
     *
     * @hide
     */
    public void setAddrPlayerChangeListener(AddrPlayerChangeListener listener) {
        startAppStatusMonitor();
        mAddrListener = listener;
        Log.i(TAG, "Set Addr Player Change Listener: " + listener);
    }

    /**
     * Clear addressed media player changes listener.
     *
     * @hide
     */
    public void cleanAddrPlayerChangeListener() {
        stopAppStatusMonitor();
        if (mAddrListener != null) {
            mAddrListener = null;
        }
        Log.i(TAG, "clean Addr Player Change Listener: " + mAddrListener);
    }

    /**
     * Implement set addressed player function called by Bluetooth stack.
     *
     * @param playId the addressed player id which will be changed to
     * @return execute result. if success, return BTRC_STS_NO_ERROR;
     *         else return fail reason
     * @hide
     */
    public int processSetAddressedPlayer(int playId) {
        //if non-initialize, initial firstly.
        if (mCurrentPlayerId == -1) {
            getAvailableMediaPlayerItems(false);
        }
        int ret = setAddressedPlayerItem((short) playId);
        Log.i(TAG, "processSetAddressedPlayer return status: " + ret);
        return ret;
    }

    /**
     * Implement get media player items command called by Bluetooth stack.
     *
     * @param startItem the first media player item index
     * @param endItem the last media player item index
     * @param attrCount attribute number. At present we do not support this
     *                  field, so it should be already 0.
     * @param attr attribute value array. At present, we also do not support
     *             this field.
     * @return return media player item array
     *
     * @hide
     */
    public Object[] processMediaPlayerItemsCmd(int startItem, int endItem,
            int attrCount, int[] attr) {
        Log.i(TAG, "processMediaPlayerItemsCmd startItem: " + startItem
                + ",endItem: " + endItem);
        ArrayList<Object> result = getAvailableMediaPlayerItems(true);
        if (startItem >= result.size()) {
            Log.i(TAG, "processMediaPlayerItemsCmd startItem is large than total items.");
            return new Object[0];
        }
        if (endItem > result.size()) {
            endItem = result.size();
        }
        Log.i(TAG, "processMediaPlayerItemsCmd adjusted startItem: "
                + startItem + ",endItem: " + endItem);
        return result.subList(startItem, endItem).toArray();
    }

    /**
     * Implement get media player number function called by Bluetooth stack.
     *
     * @return return media player item number
     *
     * @hide
     */
    public int processGetTotoalMediaPlayerItemsNum() {
        getAvailableMediaPlayerItems(false);
        Log.i(TAG,
                "processGetTotoalMediaPlayerItemsNum : " + mPlayerCache.size());
        return mPlayerCache.size();
    }

    /**
     * Process extra register notification.
     * This function should be call in processRegisterNotification method
     * in AVRCP.java
     *
     * @param eventId event id callback from stack
     * @param param  event parameter
     *
     * @hide
     */
    public void processRegisterNotification(int eventId, int param) {
        switch (eventId) {
            case EVT_ADDR_PLAYER_CHANGE:
                mAddrPlayerChangedNT = NOTIFICATION_TYPE_INTERIM;
                short playerId = -1;
                playerId = getCurrentAddressedPlayer();
                setAddrPlayerChangeListener(new AddrPlayerChangeListener());
                if (playerId >= 0) {
                    Log.i(TAG, "registerNotificationRspAddrPlayerNative(INTERIM) playerId:"
                                    + playerId);
                    registerNotificationRspAddrPlayerNative(mAddrPlayerChangedNT,
                            playerId, (short) 0);
                } else {
                    //value 2 refer to Avrcp.NOTIFICATION_TYPE_REJECTED
                    mAddrPlayerChangedNT = 2;
                    Log.i(TAG, "registerNotificationRspAddrPlayerNative(REJECT) playerId:"
                                    + playerId);
                    registerNotificationRspAddrPlayerNative(mAddrPlayerChangedNT,
                            playerId, (short) 0);
                }
                break;
            case EVT_AVAL_PLAYERS_CHANGE:
                mAvalPlayerChangedNT = NOTIFICATION_TYPE_INTERIM;
                setAvalPlayerChangeListener(new AvalPlayerChangeListener());
                Log.i(TAG, "registerNotificationRspAvalPlayerNative(INTERIM)");
                registerNotificationRspAvalPlayerNative(mAvalPlayerChangedNT);
                break;
            default:
                break;
        }
    }

    /**
     * handle extra message.
     * This method will be called by handleMessage method in AVRCP.java
     *
     * @param msg message object
     *
     * @hide
     */
    public void handleMessage(Message msg) {
        switch (msg.what) {
        case MESSAGE_SET_ADDR_PLAYER:
            if (DEBUG) { Log.d(TAG, "MESSAGE_SET_ADDR_PLAYER:" + msg.arg1); }
            //set rejected firstly
            int rspStatus = processSetAddressedPlayer(msg.arg1);
            setAddressedPlayerRspNative(rspStatus);
            if (mStatusChangeCallback != null) {
                mStatusChangeCallback.onPlayerStatusReject();
            }
            break;

        case MESSAGE_GET_FOLDER_ITEMS:
            if (DEBUG) { Log.d(TAG, "MESSAGE_GET_FOLDER_ITEMS:" + msg.arg1); }
            Bundle data = msg.getData();
            int scope = data.getInt("scope");
            int startItem = data.getInt("startItem");
            int endItem = data.getInt("endItem");
            //At present, we only support player list
            if (scope != BTRC_SCOPE_PLAYER_LIST) {
                Log.e(TAG, "Get Folder items Fail. Unsupported browsable scope: " + scope);
                break;
            }
            Object[] result = null;
            result = processMediaPlayerItemsCmd(startItem, endItem, 0, null);
            if (result == null) {
                getFolderItemRspNative(BTRC_STS_INTERNAL_ERR,
                        (short) 0, (short) 0, null);
            } else {
                getFolderItemRspNative(BTRC_STS_NO_ERROR,
                        (short) 0, (short) result.length, result);
            }
            break;

        case MESSAGE_GET_FOLDER_ITEMS_NUM:
            if (DEBUG) { Log.d(TAG, "MESSAGE_GET_FOLDER_ITEMS_NUM:" + msg.arg1); }
            int itemScope = msg.arg1;
            // At present, we only support player list
            if (itemScope != BTRC_SCOPE_PLAYER_LIST) {
                Log.e(TAG, "Get Folder Item num Fail. Unsupported browsable scope: "
                      + itemScope);
                break;
            }
            int num = processGetTotoalMediaPlayerItemsNum();
            if (num < 0) {
                getTotalItemsNumRspNative(BTRC_STS_INTERNAL_ERR,
                                          (short) 0, 0);
            } else {
                getTotalItemsNumRspNative(BTRC_STS_NO_ERROR,
                                          (short) 0, num);
            }
            break;
        default:
            break;
        }
    }

    private void handleSetAddressedPlayer(int playerId) {
        Log.d(TAG, "handleSetAddressedPlayer, playerId: " + playerId);
        Message msg = mHandler.obtainMessage(MESSAGE_SET_ADDR_PLAYER,
                playerId, 0);
        mHandler.sendMessage(msg);
    }

    private void handleGetFolderItemsCmd(int scope, int startItem, int endItem,
            int attrCount, int[] attr) {
        Log.d(TAG, "handleGeFolderItemsCmd, scope: " + scope
                + ", startItem: " + startItem + ", endItem: " + endItem
                + ", attrCount:" + attrCount + ", attr:"
                + (attr != null ? attr.toString() : "NULL"));
        Message msg = mHandler.obtainMessage(MESSAGE_GET_FOLDER_ITEMS);
        Bundle data = new Bundle();
        data.putInt("scope", scope);
        data.putInt("startItem", startItem);
        data.putInt("endItem", endItem);
        //Do not process 'attrCount' and 'attr' now, because these fields are
        //not supported at present.
        //data.putInt("attrCount", attrCount);
        //data.putIntArray("attr", attr);
        msg.setData(data);
        mHandler.sendMessage(msg);
    }

    private void handleGetTotalItemsNum(int scope) {
        Log.d(TAG, "handleGetTotalItemsNum, scope: " + scope);
        Message msg = mHandler.obtainMessage(MESSAGE_GET_FOLDER_ITEMS_NUM, scope, 0);
        mHandler.sendMessage(msg);
    }

    private native static void classInitNative();
    private native void initNative();
    private native void cleanupNative();
    private native boolean registerNotificationRspAddrPlayerNative(int type,
            short playerId, short uidCounter);
    private native boolean registerNotificationRspAvalPlayerNative(int type);
    private native boolean setAddressedPlayerRspNative(int rspStatus);
    private native boolean getFolderItemRspNative(int rspStatus,
            short uidCounter, short itemNum, Object[] itemList);
    private native boolean getTotalItemsNumRspNative(int rspStatus,
            short uidCounter, int num);

}
