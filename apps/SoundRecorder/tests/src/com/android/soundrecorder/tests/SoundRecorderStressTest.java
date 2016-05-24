package com.android.soundrecorder.tests;

import java.util.ArrayList;
import java.util.List;

import com.android.soundrecorder.Recorder;
import com.android.soundrecorder.SoundRecorder;
import com.android.soundrecorder.RecordingFileList;

import android.app.Instrumentation;
import android.app.Instrumentation.ActivityMonitor;
import android.os.SystemClock;
import android.test.ActivityInstrumentationTestCase2;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import com.android.soundrecorder.R;
import com.jayway.android.robotium.solo.Solo;

public class SoundRecorderStressTest extends
        ActivityInstrumentationTestCase2<SoundRecorder> {
    private static String TAG = "SR/SoundRecorderStressTest";
    private static int WAIT_FOR_RESPOND_TIME = 2000;
    private static int WAIT_LONG_PRESS = 2000;
    private static int WAIT_FOR_ACTIVITY_CHANGE = 2000;
    private static int WAIT_FOR_RECORD_TIME = 5000;
    private static int WAIT_FOR_DELETE = 3000;
    private static int WAIT_FOR_SAVE = 2000;
    private static long TOTAL_TIME_ZERO = 1;
    private static long TOTAL_TIME_62H = 216000000; // 62 hours
    private static long TOTAL_TIME_1M = 60000; // 1 minites
    // private static int RANDROM_CLICK_TIMES = 100000;
    private int mTotalTimes = 0;
    private long mTimeHasGone = 0;
    private long mTimeBegin = 0;

    private Solo mSolo = null;
    private Instrumentation mInstrumentation = null;
    private List<OperateAndCheck> mOperateList = null;
    private List<OperateAndCheck> mOperateList_backup = null;
    private SoundRecorder mSoundRecorder = null;
    private RecordingFileList mRecordingFileList = null;

    // Recourse references in SoundRecorder
    private ImageButton mRecordButton = null;
    private ImageButton mPauseRecordingButton = null;
    private ImageButton mStopButton = null;
    private ImageButton mPlayButton = null;
    private ImageButton mFileListButton = null;
    private Button mDiscardButton = null;
    private Button mAcceptButton = null;
    private TextView mStateMessage = null;
    private TextView mFileNameTextView = null;

    // Recourse references in RecordingFileList
    private ImageButton mRecordButtonInList = null;
    private ImageButton mDeleteButton = null;
    private ListView mRecordingFileListView = null;

    abstract class OperateAndCheck {
        private int mTimes = 0;

        abstract public void operate();

        abstract public void check();

        abstract public void setNextAvailableOperate();

        public void statesticsRunTimes() {
            mTimes++;
        }
    }

    OperateAndCheck mFunction01_record = new OperateAndCheck() {
        public void operate() {
            Log.i(TAG, "<mFunction01_record>");
            mSolo.clickOnView(mRecordButton);
        }

        public void check() {
            mSolo.sleep(WAIT_FOR_RECORD_TIME);
            assertFalse(mRecordButton.isShown());
            assertFalse(mPlayButton.isShown());
            assertTrue(mStopButton.isEnabled());
            assertFalse(mAcceptButton.isShown());
            assertFalse(mDiscardButton.isShown());
            assertTrue(mStateMessage.isShown());
            assertTrue(mStateMessage.getText().equals(
                    mSolo.getString(R.string.recording)));
        }

        public void setNextAvailableOperate() {
            mOperateList = new ArrayList<OperateAndCheck>(2);
            mOperateList.add(mFunction02_pauseRecord);
            mOperateList.add(mFunction03_stopRecord);
        }
    };

    OperateAndCheck mFunction02_pauseRecord = new OperateAndCheck() {
        public void operate() {
            Log.i(TAG, "<mFunction02_pauseRecord>");
            mSolo.clickOnView(mPauseRecordingButton);
        }

        public void check() {
            mSolo.sleep(WAIT_FOR_RESPOND_TIME);
            assertTrue(mStateMessage.isShown());
            assertTrue(mStateMessage.getText().equals(
                    mSolo.getString(R.string.recording_paused)));

            String recordingFilePath = SoundRecorderTestUtils
                    .getCurrentFilePath(mSoundRecorder);
            String recordingFileName = recordingFilePath.substring(
                    (recordingFilePath.lastIndexOf('/') + 1), recordingFilePath
                            .indexOf(Recorder.SAMPLE_SUFFIX));
            assertTrue(mFileNameTextView.getText().equals(recordingFileName));

            assertTrue(mRecordButton.isEnabled());
            assertFalse(mPauseRecordingButton.isShown());
            assertTrue(mStopButton.isEnabled());
        }

        public void setNextAvailableOperate() {
            mOperateList = new ArrayList<OperateAndCheck>(2);
            mOperateList.add(mFunction01_record);
            mOperateList.add(mFunction03_stopRecord);
        }
    };

    OperateAndCheck mFunction03_stopRecord = new OperateAndCheck() {
        public void operate() {
            Log.i(TAG, "<mFunction03_stopRecord>");
            mSolo.clickOnView(mStopButton);
        }

        public void check() {
            mSolo.sleep(WAIT_FOR_RESPOND_TIME);
            assertTrue(mRecordButton.isEnabled());
            assertTrue(mPlayButton.isEnabled());
            assertFalse(mStopButton.isEnabled());
            assertTrue(mAcceptButton.isEnabled());
            assertTrue(mDiscardButton.isEnabled());
            assertFalse(mStateMessage.isShown());
        }

        public void setNextAvailableOperate() {
            mOperateList = new ArrayList<OperateAndCheck>(6);
            mOperateList.add(mFunction04_stopRecord_play);
            mOperateList.add(mFunction01_record);
            mOperateList.add(mFunction07_discard);
            mOperateList.add(mFunction08_accept);
            mOperateList.add(mFunction16_menuItem_voiceQuality);
            mOperateList.add(mFunction17_menuItem_mode);
        }
    };

    OperateAndCheck mFunction04_stopRecord_play = new OperateAndCheck() {
        public void operate() {
            Log.i(TAG, "<mFunction04_stopRecord_play>");
            mSolo.clickOnView(mPlayButton);
        }

        public void check() {
            mSolo.sleep(WAIT_FOR_RESPOND_TIME);
            assertTrue(mRecordButton.isEnabled());
            assertTrue(mPlayButton.isEnabled());
            assertTrue(mStopButton.isEnabled());
            assertTrue(mAcceptButton.isEnabled());
            assertTrue(mDiscardButton.isEnabled());
            assertFalse(mStateMessage.isShown());
        }

        public void setNextAvailableOperate() {
            mOperateList = new ArrayList<OperateAndCheck>(5);
            mOperateList.add(mFunction01_record);
            mOperateList.add(mFunction05_stopRecord_pausePlay);
            mOperateList.add(mFunction06_stopRecord_stopPlay);
            mOperateList.add(mFunction07_discard);
            mOperateList.add(mFunction08_accept);
        }
    };

    OperateAndCheck mFunction05_stopRecord_pausePlay = new OperateAndCheck() {
        public void operate() {
            Log.i(TAG, "<mFunction05_stopRecord_pausePlay>");
            mSolo.clickOnView(mPlayButton);
        }

        public void check() {
            mSolo.sleep(WAIT_FOR_RESPOND_TIME);
            assertTrue(mRecordButton.isEnabled());
            assertTrue(mPlayButton.isEnabled());
            assertTrue(mStopButton.isEnabled());
            assertTrue(mAcceptButton.isEnabled());
            assertTrue(mDiscardButton.isEnabled());
            assertFalse(mStateMessage.isShown());
        }

        public void setNextAvailableOperate() {
            mOperateList = new ArrayList<OperateAndCheck>(5);
            mOperateList.add(mFunction01_record);
            mOperateList.add(mFunction04_stopRecord_play);
            mOperateList.add(mFunction06_stopRecord_stopPlay);
            mOperateList.add(mFunction07_discard);
            mOperateList.add(mFunction08_accept);
        }
    };

    OperateAndCheck mFunction06_stopRecord_stopPlay = new OperateAndCheck() {
        public void operate() {
            Log.i(TAG, "<mFunction06_stopRecord_stopPlay>");
            mSolo.clickOnView(mStopButton);
        }

        public void check() {
            mSolo.sleep(WAIT_FOR_RESPOND_TIME);
            assertTrue(mRecordButton.isEnabled());
            assertTrue(mPlayButton.isEnabled());
            assertFalse(mStopButton.isEnabled());
            assertTrue(mAcceptButton.isEnabled());
            assertTrue(mDiscardButton.isEnabled());
            assertFalse(mStateMessage.isShown());
        }

        public void setNextAvailableOperate() {
            mOperateList = new ArrayList<OperateAndCheck>(6);
            mOperateList.add(mFunction01_record);
            mOperateList.add(mFunction04_stopRecord_play);
            mOperateList.add(mFunction07_discard);
            mOperateList.add(mFunction08_accept);
            mOperateList.add(mFunction16_menuItem_voiceQuality);
            mOperateList.add(mFunction17_menuItem_mode);
        }
    };

    OperateAndCheck mFunction07_discard = new OperateAndCheck() {
        public void operate() {
            Log.i(TAG, "<mFunction07_discard>");
            mSolo.clickOnView(mDiscardButton);
        }

        public void check() {
            mSolo.sleep(WAIT_FOR_RESPOND_TIME);
            assertTrue(mRecordButton.isEnabled());
            assertFalse(mPlayButton.isShown());
            assertFalse(mStopButton.isShown());
            assertFalse(mAcceptButton.isShown());
            assertFalse(mDiscardButton.isShown());
        }

        public void setNextAvailableOperate() {
            mOperateList = new ArrayList<OperateAndCheck>(4);
            mOperateList.add(mFunction01_record);
            mOperateList.add(mFunction09_list);
            mOperateList.add(mFunction16_menuItem_voiceQuality);
            mOperateList.add(mFunction17_menuItem_mode);
        }
    };

    OperateAndCheck mFunction08_accept = new OperateAndCheck() {
        public void operate() {
            Log.i(TAG, "<mFunction08_accept>");
            mSolo.clickOnView(mAcceptButton);
        }

        public void check() {
            mSolo.sleep(WAIT_FOR_SAVE);
            assertTrue(mRecordButton.isEnabled());
            assertFalse(mPlayButton.isShown());
            assertFalse(mStopButton.isShown());
            assertFalse(mAcceptButton.isShown());
            assertFalse(mDiscardButton.isShown());
        }

        public void setNextAvailableOperate() {
            mOperateList = new ArrayList<OperateAndCheck>(4);
            mOperateList.add(mFunction01_record);
            mOperateList.add(mFunction09_list);
            mOperateList.add(mFunction16_menuItem_voiceQuality);
            mOperateList.add(mFunction17_menuItem_mode);
        }
    };

    OperateAndCheck mFunction09_list = new OperateAndCheck() {
        private ActivityMonitor mRecordingFileListAM = null;

        public void operate() {
            Log.i(TAG, "<mFunction09_list>");
            mSolo.clickOnView(mFileListButton);
        }

        public void check() {
            mSolo.sleep(WAIT_FOR_ACTIVITY_CHANGE);
            mRecordingFileList = (RecordingFileList) mSolo.getCurrentActivity();
            assertNotNull("activity should be launched successfully",
                    mRecordingFileList);
            mRecordingFileListView = (ListView) mRecordingFileList
                    .findViewById(R.id.recording_file_list_view);
            mRecordButtonInList = (ImageButton) mRecordingFileList
                    .findViewById(R.id.recordButton);
            mDeleteButton = (ImageButton) mRecordingFileList
                    .findViewById(R.id.deleteButton);
        }

        public void setNextAvailableOperate() {
            mOperateList = new ArrayList<OperateAndCheck>(3);
            mOperateList.add(mFunction10_list_play);
            mOperateList.add(mFunction11_list_longPress);
            mOperateList.add(mFunction22_list_record);
        }
    };

    OperateAndCheck mFunction10_list_play = new OperateAndCheck() {
        private int mCount = 0;

        public void operate() {
            Log.i(TAG, "<mFunction10_list_play>");
            mCount = mRecordingFileListView.getCount();
            if (0 == mCount) {
                Log.i(TAG, "mRecordingFileListView.getCount() == 0");
                return;
            }

            int index = SoundRecorderTestUtils.getRandom(mCount);
            Log.i(TAG, "mRecordingFileListView.getCount() = " + mCount);
            Log.i(TAG, "list index = " + index);
            mSolo.clickOnText(getTextInList(index));
        }

        public void check() {
            if (0 == mCount) {
                return;
            }

            mSolo.sleep(WAIT_FOR_RESPOND_TIME);
            assertTrue(mRecordButton.isEnabled());
            assertTrue(mPlayButton.isEnabled());
            assertTrue(mStopButton.isEnabled());
            assertFalse(mAcceptButton.isShown());
            assertFalse(mDiscardButton.isShown());
            assertFalse(mStateMessage.isShown());
//            assertTrue(SoundRecorderTestUtils
//                    .isMediaPlayerPlaying(mSoundRecorder));
            mRecordingFileList = null;
            mRecordingFileListView = null;
            mRecordButtonInList = null;
            mDeleteButton = null;
        }

        public void setNextAvailableOperate() {
            if (0 == mCount) {
                mOperateList = new ArrayList<OperateAndCheck>(1);
                mOperateList.add(mFunction01_record);
                return;
            }

            mOperateList = new ArrayList<OperateAndCheck>(3);
            mOperateList.add(mFunction01_record);
            mOperateList.add(mFunction23_pausePlay);
            mOperateList.add(mFunction24_stopPlay);
        }
    };

    OperateAndCheck mFunction11_list_longPress = new OperateAndCheck() {
        private boolean mInEditMode = true;
        private int mCount = 0;

        public void operate() {
            Log.i(TAG, "<mFunction11_list_longPress>");
            mCount = mRecordingFileListView.getCount();
            if (0 == mCount) {
                Log.i(TAG, "mRecordingFileListView.getCount() == 0");
                return;
            }
            int index = SoundRecorderTestUtils.getRandom(mCount);
            Log.i(TAG, "list index = " + index);
            mSolo.clickLongOnText(getTextInList(index));
            scrollListToTop();
        }

        public void check() {
            if (0 == mCount) {
                return;
            }
            mSolo.sleep(WAIT_FOR_RESPOND_TIME);
            mInEditMode = SoundRecorderTestUtils
                    .isInEditMode(mRecordingFileList);
            if (mInEditMode) {
                assertTrue(mDeleteButton.isShown());
                assertTrue(mDeleteButton.isEnabled());
            } else {
                assertTrue(mRecordButtonInList.isShown());
                assertTrue(mRecordButtonInList.isEnabled());
            }
        }

        public void setNextAvailableOperate() {
            if (0 == mCount) {
                mOperateList = new ArrayList<OperateAndCheck>(1);
                mOperateList.add(mFunction22_list_record);
                return;
            }

            if (mInEditMode) {
                mOperateList = new ArrayList<OperateAndCheck>(3);
                mOperateList.add(mFunction11_list_longPress);
                mOperateList.add(mFunction12_list_clickInEditMode);
                mOperateList.add(mFunction13_list_delete);
            } else {
                mOperateList = new ArrayList<OperateAndCheck>(3);
                mOperateList.add(mFunction10_list_play);
                mOperateList.add(mFunction11_list_longPress);
                mOperateList.add(mFunction22_list_record);
            }
        }
    };

    OperateAndCheck mFunction12_list_clickInEditMode = new OperateAndCheck() {
        private boolean mInEditMode = true;
        private int mCount = 0;

        public void operate() {
            Log.i(TAG, "<mFunction12_list_clickInEditMode>");
            mCount = mRecordingFileListView.getCount();
            if (0 == mCount) {
                Log.i(TAG, "mRecordingFileListView.getCount() == 0");
                return;
            }

            int index = SoundRecorderTestUtils.getRandom(mCount);
            Log.i(TAG, "list index = " + index);
            mSolo.clickOnText(getTextInList(index));
            scrollListToTop();
        }

        public void check() {
            if (0 == mCount) {
                return;
            }

            mSolo.sleep(WAIT_FOR_RESPOND_TIME);
            mInEditMode = SoundRecorderTestUtils
                    .isInEditMode(mRecordingFileList);
            if (mInEditMode) {
                assertTrue(mDeleteButton.isShown());
                assertTrue(mDeleteButton.isEnabled());
            } else {
                assertTrue(mRecordButtonInList.isShown());
                assertTrue(mRecordButtonInList.isEnabled());
            }
        }

        public void setNextAvailableOperate() {
            if (0 == mCount) {
                mOperateList = new ArrayList<OperateAndCheck>(1);
                mOperateList.add(mFunction22_list_record);
                return;
            }

            if (mInEditMode) {
                mOperateList = new ArrayList<OperateAndCheck>(3);
                mOperateList.add(mFunction11_list_longPress);
                mOperateList.add(mFunction12_list_clickInEditMode);
                mOperateList.add(mFunction13_list_delete);
            } else {
                mOperateList = new ArrayList<OperateAndCheck>(3);
                mOperateList.add(mFunction10_list_play);
                mOperateList.add(mFunction11_list_longPress);
                mOperateList.add(mFunction22_list_record);
            }
        }
    };

    OperateAndCheck mFunction13_list_delete = new OperateAndCheck() {
        public void operate() {
            Log.i(TAG, "<mFunction13_list_delete>");
            mSolo.clickOnView(mDeleteButton);
        }

        public void check() {
            mSolo.sleep(WAIT_FOR_RESPOND_TIME);
            String multipleDelete = mSolo
                    .getString(R.string.alert_delete_multiple);
            String singleDelete = mSolo.getString(R.string.alert_delete_single);
            boolean result = false;
            if (null != multipleDelete) {
                result = mSolo.searchText(multipleDelete);

            }
            if (null != singleDelete) {
                if (!result) {
                    result = mSolo.searchText(singleDelete);
                }
            }
            assertTrue(result);
        }

        public void setNextAvailableOperate() {
            mOperateList = new ArrayList<OperateAndCheck>(2);
            mOperateList.add(mFunction15_list_deleteOk);
            mOperateList.add(mFunction14_list_deleteCancel);
        }
    };

    OperateAndCheck mFunction14_list_deleteCancel = new OperateAndCheck() {
        public void operate() {
            Log.i(TAG, "<mFunction14_list_deleteCancel>");
            mSolo.clickOnButton(mSolo.getString(R.string.cancel));
        }

        public void check() {
            mSolo.sleep(WAIT_FOR_RESPOND_TIME);
            assertTrue(mDeleteButton.isShown());
            assertTrue(mDeleteButton.isEnabled());
        }

        public void setNextAvailableOperate() {
            mOperateList = new ArrayList<OperateAndCheck>(3);
            mOperateList.add(mFunction11_list_longPress);
            mOperateList.add(mFunction12_list_clickInEditMode);
            mOperateList.add(mFunction13_list_delete);
        }
    };

    OperateAndCheck mFunction15_list_deleteOk = new OperateAndCheck() {
        public void operate() {
            Log.i(TAG, "<mFunction15_list_deleteOk>");
            mSolo.clickOnButton(mSolo.getString(R.string.ok));
        }

        public void check() {
            mSolo.sleep(WAIT_FOR_DELETE);
            assertTrue(mRecordButtonInList.isEnabled());
            assertFalse(mDeleteButton.isShown());
            assertFalse(SoundRecorderTestUtils.isInEditMode(mRecordingFileList));
        }

        public void setNextAvailableOperate() {
            mOperateList = new ArrayList<OperateAndCheck>(3);
            mOperateList.add(mFunction10_list_play);
            mOperateList.add(mFunction11_list_longPress);
            mOperateList.add(mFunction22_list_record);
        }
    };

    OperateAndCheck mFunction22_list_record = new OperateAndCheck() {
        private ActivityMonitor mSoundRecorderAM = null;

        public void operate() {
            Log.i(TAG, "<mFunction22_list_record>");
            mSoundRecorderAM = mInstrumentation.addMonitor(
                    "com.android.soundrecorder.SoundRecorder", null, false);
            mSolo.clickOnView(mRecordButtonInList);
        }

        public void check() {
            mSolo.sleep(WAIT_FOR_RECORD_TIME);
            assertNotNull(mSoundRecorderAM
                    .waitForActivityWithTimeout(WAIT_FOR_ACTIVITY_CHANGE));
            mRecordingFileList = null;
            mRecordingFileListView = null;
            mRecordButtonInList = null;
            mDeleteButton = null;
        }

        public void setNextAvailableOperate() {
            mOperateList = new ArrayList<OperateAndCheck>(2);
            mOperateList.add(mFunction02_pauseRecord);
            mOperateList.add(mFunction03_stopRecord);
        }
    };

    OperateAndCheck mFunction16_menuItem_voiceQuality = new OperateAndCheck() {
        public void operate() {
            Log.i(TAG, "<mFunction16_menuItem_voiceQuality>");
            mSolo.clickOnMenuItem(mSolo.getString(R.string.voice_quality));
        }

        public void check() {
            mSolo.sleep(WAIT_FOR_RESPOND_TIME);
            assertNotNull(mSolo.getCurrentViews(ListView.class));
        }

        public void setNextAvailableOperate() {
            backupOperateList();
            mOperateList = new ArrayList<OperateAndCheck>(2);
            mOperateList.add(mFunction18_selectFormat);
            mOperateList.add(mFunction20_selectFormat_cancel);
        }
    };

    OperateAndCheck mFunction17_menuItem_mode = new OperateAndCheck() {
        private boolean mModeMenuItemExist = false;

        public void operate() {
            Log.i(TAG, "<mFunction17_menuItem_mode>");
            mSolo.sendKey(Solo.MENU);
            mSolo.sleep(WAIT_FOR_RESPOND_TIME);
            mModeMenuItemExist = mSolo.searchText(mSolo
                    .getString(R.string.recording_mode));
            if (mModeMenuItemExist) {
                mSolo.clickOnText(mSolo.getString(R.string.recording_mode));
            } else {
                mSolo.goBack();
            }
        }

        public void check() {
            if (mModeMenuItemExist) {
                mSolo.sleep(WAIT_FOR_RESPOND_TIME);
                assertNotNull(mSolo.getCurrentViews(ListView.class));
            }
        }

        public void setNextAvailableOperate() {
            if (mModeMenuItemExist) {
                backupOperateList();
                mOperateList = new ArrayList<OperateAndCheck>(2);
                mOperateList.add(mFunction19_selectMode);
                mOperateList.add(mFunction21_selectMode_cancel);
            }
        }
    };

    OperateAndCheck mFunction18_selectFormat = new OperateAndCheck() {
        public void operate() {
            Log.i(TAG, "<mFunction18_selectFormat>");
            int random = SoundRecorderTestUtils.getRandom(2);
            Log.i(TAG, "random = " + random);
            mSolo.clickInList(random);
        }

        public void check() {
            mSolo.sleep(WAIT_FOR_RESPOND_TIME);
            assertFalse(mSolo.searchText(mSolo
                    .getString(R.string.select_voice_quality)));
        }

        public void setNextAvailableOperate() {
            restoreOperateList();
        }
    };

    OperateAndCheck mFunction19_selectMode = new OperateAndCheck() {
        public void operate() {
            Log.i(TAG, "<mFunction19_selectMode>");
            int random = SoundRecorderTestUtils.getRandom(3);
            Log.i(TAG, "random = " + random);
            mSolo.clickInList(random);
        }

        public void check() {
            mSolo.sleep(WAIT_FOR_RESPOND_TIME);
            assertFalse(mSolo.searchText(mSolo
                    .getString(R.string.select_recording_mode)));
        }

        public void setNextAvailableOperate() {
            restoreOperateList();
        }
    };

    OperateAndCheck mFunction20_selectFormat_cancel = new OperateAndCheck() {
        public void operate() {
            Log.i(TAG, "<mFunction20_selectFormat_cancel>");
            mSolo.clickOnButton(mSolo.getString(R.string.cancel));
        }

        public void check() {
            mSolo.sleep(WAIT_FOR_RESPOND_TIME);
            assertFalse(mSolo.searchText(mSolo
                    .getString(R.string.select_voice_quality)));
        }

        public void setNextAvailableOperate() {
            restoreOperateList();
        }
    };

    OperateAndCheck mFunction21_selectMode_cancel = new OperateAndCheck() {
        public void operate() {
            Log.i(TAG, "<mFunction21_selectMode_cancel>");
            mSolo.clickOnButton(mSolo.getString(R.string.cancel));
        }

        public void check() {
            mSolo.sleep(WAIT_FOR_RESPOND_TIME);
            assertFalse(mSolo.searchText(mSolo
                    .getString(R.string.select_recording_mode)));
        }

        public void setNextAvailableOperate() {
            restoreOperateList();
        }
    };

    OperateAndCheck mFunction23_pausePlay = new OperateAndCheck() {
        public void operate() {
            Log.i(TAG, "<mFunction23_pausePlay>");
            mSolo.clickOnView(mPlayButton);
        }

        public void check() {
            mSolo.sleep(WAIT_FOR_RESPOND_TIME);
            assertTrue(mRecordButton.isEnabled());
            assertTrue(mPlayButton.isEnabled());
            assertTrue(mStopButton.isEnabled());
            assertFalse(mAcceptButton.isShown());
            assertFalse(mDiscardButton.isShown());
            assertFalse(mStateMessage.isShown());
        }

        public void setNextAvailableOperate() {
            mOperateList = new ArrayList<OperateAndCheck>(3);
            mOperateList.add(mFunction01_record);
            mOperateList.add(mFunction25_play);
            mOperateList.add(mFunction24_stopPlay);
        }
    };

    OperateAndCheck mFunction24_stopPlay = new OperateAndCheck() {
        public void operate() {
            Log.i(TAG, "<mFunction24_stopPlay>");
            mSolo.clickOnView(mStopButton);
        }

        public void check() {
            mSolo.sleep(WAIT_FOR_RESPOND_TIME);
            assertTrue(mRecordButton.isEnabled());
            assertTrue(mPlayButton.isEnabled());
            assertFalse(mStopButton.isEnabled());
            assertFalse(mAcceptButton.isShown());
            assertFalse(mDiscardButton.isShown());
            assertFalse(mStateMessage.isShown());
        }

        public void setNextAvailableOperate() {
            mOperateList = new ArrayList<OperateAndCheck>(3);
            mOperateList.add(mFunction01_record);
            mOperateList.add(mFunction25_play);
            mOperateList.add(mFunction09_list);
            mOperateList.add(mFunction16_menuItem_voiceQuality);
            mOperateList.add(mFunction17_menuItem_mode);
        }
    };

    OperateAndCheck mFunction25_play = new OperateAndCheck() {
        public void operate() {
            Log.i(TAG, "<mFunction25_play>");
            mSolo.clickOnView(mPlayButton);
        }

        public void check() {
            mSolo.sleep(WAIT_FOR_RESPOND_TIME);
            assertTrue(mRecordButton.isEnabled());
            assertTrue(mPlayButton.isEnabled());
            assertTrue(mStopButton.isEnabled());
            assertFalse(mAcceptButton.isShown());
            assertFalse(mDiscardButton.isShown());
            assertFalse(mStateMessage.isShown());
        }

        public void setNextAvailableOperate() {
            mOperateList = new ArrayList<OperateAndCheck>(3);
            mOperateList.add(mFunction01_record);
            mOperateList.add(mFunction23_pausePlay);
            mOperateList.add(mFunction24_stopPlay);
        }
    };

    public SoundRecorderStressTest(Class<SoundRecorder> activityClass) {
        super(activityClass);
    }

    public SoundRecorderStressTest() {
        super(SoundRecorder.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        setActivityInitialTouchMode(false);
        initResourceRefs();
        mOperateList = new ArrayList<OperateAndCheck>(4);
        mOperateList.add(mFunction01_record);
        mOperateList.add(mFunction09_list);
        mOperateList.add(mFunction16_menuItem_voiceQuality);
        mOperateList.add(mFunction17_menuItem_mode);
        mTimeBegin = SystemClock.elapsedRealtime();
    }

    private void initResourceRefs() {
        mSoundRecorder = getActivity();
        assertNotNull(mSoundRecorder);
        mInstrumentation = getInstrumentation();
        assertNotNull(mInstrumentation);
        mSolo = new Solo(mInstrumentation, mSoundRecorder);
        assertNotNull(mSolo);

        mRecordButton = (ImageButton) mSolo.getView(R.id.recordButton);
        mPlayButton = (ImageButton) mSolo.getView(R.id.playButton);
        mStopButton = (ImageButton) mSolo.getView(R.id.stopButton);
        mFileListButton = (ImageButton) mSolo.getView(R.id.fileListButton);
        mPauseRecordingButton = (ImageButton) mSolo
                .getView(R.id.pauseRecordingButton);
        mAcceptButton = (Button) mSoundRecorder.findViewById(R.id.acceptButton);
        mDiscardButton = (Button) mSoundRecorder
                .findViewById(R.id.discardButton);
        mStateMessage = (TextView) mSolo.getView(R.id.stateMessage2);
        mFileNameTextView = (TextView) mSolo.getView(R.id.recordingFileName);
    }

    @Override
    protected void tearDown() throws Exception {
        Log.i(TAG, "mFunction01_record(runtime percent): "
                + String.valueOf(mFunction01_record.mTimes
                        / (float) mTotalTimes));
        Log.i(TAG, "mFunction02_pauseRecord(runtime percent): "
                + String.valueOf(mFunction02_pauseRecord.mTimes
                        / (float) mTotalTimes));
        Log.i(TAG, "mFunction03_stopRecord(runtime percent): "
                + String.valueOf(mFunction03_stopRecord.mTimes
                        / (float) mTotalTimes));
        Log.i(TAG, "mFunction04_stopRecord_play(runtime percent): "
                + String.valueOf(mFunction04_stopRecord_play.mTimes
                        / (float) mTotalTimes));
        Log.i(TAG, "mFunction05_stopRecord_pausePlay(runtime percent): "
                + String.valueOf(mFunction05_stopRecord_pausePlay.mTimes
                        / (float) mTotalTimes));
        Log.i(TAG, "mFunction06_stopRecord_stopPlay(runtime percent): "
                + String.valueOf(mFunction06_stopRecord_stopPlay.mTimes
                        / (float) mTotalTimes));
        Log.i(TAG, "mFunction07_discard(runtime percent): "
                + String.valueOf(mFunction07_discard.mTimes
                        / (float) mTotalTimes));
        Log.i(TAG, "mFunction08_accept(runtime percent): "
                + String.valueOf(mFunction08_accept.mTimes
                        / (float) mTotalTimes));
        Log
                .i(TAG, "mFunction09_list(runtime percent): "
                        + String.valueOf(mFunction09_list.mTimes
                                / (float) mTotalTimes));
        Log.i(TAG, "mFunction10_list_play(runtime percent): "
                + String.valueOf(mFunction10_list_play.mTimes
                        / (float) mTotalTimes));
        Log.i(TAG, "mFunction11_list_longPress(runtime percent): "
                + String.valueOf(mFunction11_list_longPress.mTimes
                        / (float) mTotalTimes));
        Log.i(TAG, "mFunction12_list_clickInEditMode(runtime percent): "
                + String.valueOf(mFunction12_list_clickInEditMode.mTimes
                        / (float) mTotalTimes));
        Log.i(TAG, "mFunction13_list_delete(runtime percent): "
                + String.valueOf(mFunction13_list_delete.mTimes
                        / (float) mTotalTimes));
        Log.i(TAG, "mFunction14_list_deleteCancel(runtime percent): "
                + String.valueOf(mFunction14_list_deleteCancel.mTimes
                        / (float) mTotalTimes));
        Log.i(TAG, "mFunction15_list_deleteOk(runtime percent): "
                + String.valueOf(mFunction15_list_deleteOk.mTimes
                        / (float) mTotalTimes));
        Log.i(TAG, "mFunction16_menuItem_voiceQuality(runtime percent): "
                + String.valueOf(mFunction16_menuItem_voiceQuality.mTimes
                        / (float) mTotalTimes));
        Log.i(TAG, "mFunction17_menuItem_mode(runtime percent): "
                + String.valueOf(mFunction17_menuItem_mode.mTimes
                        / (float) mTotalTimes));
        Log.i(TAG, "mFunction18_selectFormat(runtime percent): "
                + String.valueOf(mFunction18_selectFormat.mTimes
                        / (float) mTotalTimes));
        Log.i(TAG, "mFunction19_selectMode(runtime percent): "
                + String.valueOf(mFunction19_selectMode.mTimes
                        / (float) mTotalTimes));
        Log.i(TAG, "mFunction20_selectFormat_cancel(runtime percent): "
                + String.valueOf(mFunction20_selectFormat_cancel.mTimes
                        / (float) mTotalTimes));
        Log.i(TAG, "mFunction21_selectMode_cancel(runtime percent): "
                + String.valueOf(mFunction21_selectMode_cancel.mTimes
                        / (float) mTotalTimes));
        Log.i(TAG, "mFunction22_list_record(runtime percent): "
                + String.valueOf(mFunction22_list_record.mTimes
                        / (float) mTotalTimes));
        Log.i(TAG, "mFunction23_pausePlay(runtime percent): "
                + String.valueOf(mFunction23_pausePlay.mTimes
                        / (float) mTotalTimes));
        Log.i(TAG, "mFunction24_stopPlay(runtime percent): "
                + String.valueOf(mFunction24_stopPlay.mTimes
                        / (float) mTotalTimes));
        Log
                .i(TAG, "mFunction25_play(runtime percent): "
                        + String.valueOf(mFunction25_play.mTimes
                                / (float) mTotalTimes));
        Log.i(TAG, "Total Time = " + mTimeHasGone / 60000.0f + " minites");
        Log.i(TAG, "Total Times = " + mTotalTimes);
        Log.i(TAG, "Average runnning time of per function = " + mTimeHasGone
                / mTotalTimes / 1000.0f + " seconds");

        mSolo.finishOpenedActivities();
        try {
            mSolo.finalize();
        } catch (Throwable t) {
            t.printStackTrace();
        }
        mInstrumentation.waitForIdleSync();
        super.tearDown();
    }

    public void testCase01_RandomClick() {
        int random = 0;
        OperateAndCheck operate = null;
        while (mTimeHasGone < TOTAL_TIME_ZERO) {
            mTotalTimes++;
            Log.i(TAG, ">>>>>>>>>>>Times " + mTotalTimes + " begin");
            Log.i(TAG, "mTimeHasGone = " + mTimeHasGone);
            random = SoundRecorderTestUtils.getRandom(mOperateList.size());
            operate = mOperateList.get(random);
            operate.operate();
            operate.statesticsRunTimes();
            operate.check();
            operate.setNextAvailableOperate();
            mTimeHasGone = SystemClock.elapsedRealtime() - mTimeBegin;
        }
    }

    private void backupOperateList() {
        int size = mOperateList.size();
        mOperateList_backup = new ArrayList<OperateAndCheck>(size);
        for (int i = 0; i < size; i++) {
            mOperateList_backup.add(mOperateList.get(i));
        }
    }

    private void restoreOperateList() {
        int size = mOperateList_backup.size();
        mOperateList = new ArrayList<OperateAndCheck>(size);
        for (int i = 0; i < size; i++) {
            mOperateList.add(mOperateList_backup.get(i));
        }
    }

    private String getTextInList(int index) {
        assertTrue((index >= 0) && (index < mRecordingFileListView.getCount()));
        ListAdapter listAdapter = mRecordingFileListView.getAdapter();
        View view = null;
        boolean goonScroll = true;
        int j = 0;
        int i = 0;
        while (j <= index && goonScroll) {
            try {
                view = listAdapter.getView(i, null, mRecordingFileListView);
                i++;
                j++;
            } catch (IndexOutOfBoundsException e) {
                Log.e(TAG, "<getViewInList> " + e.getMessage());
                view = null;
                goonScroll = mSolo.scrollDownList(0);
                i = 0;
            }
        }
        if (null != view) {
            TextView textView = (TextView) view
                    .findViewById(R.id.record_file_name);
            String fileName = textView.getText().toString();
            Log.i(TAG, "<getViewInList> text on view is " + fileName);
            return fileName;
        } else {
            Log.i(TAG, "<getViewInList> view = null");
            return null;
        }
    }

    private void scrollListToTop() {
        Log.i(TAG, "<scrollListToTop>");
        boolean goonScroll = true;
        while (goonScroll) {
            goonScroll = mSolo.scrollUp();
        }
    }
}
