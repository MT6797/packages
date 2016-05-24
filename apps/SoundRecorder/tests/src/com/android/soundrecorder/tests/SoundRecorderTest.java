package com.android.soundrecorder.tests;

import android.app.Instrumentation;
import android.app.Instrumentation.ActivityMonitor;
import android.test.ActivityInstrumentationTestCase2;
import com.jayway.android.robotium.solo.Solo;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import com.android.soundrecorder.R;
import com.android.soundrecorder.Recorder;
import com.android.soundrecorder.SoundRecorder;
import com.android.soundrecorder.SoundRecorderService;
import com.android.soundrecorder.tests.annotation.UiAnnotation;

import java.io.File;
import java.util.ArrayList;


public class SoundRecorderTest extends ActivityInstrumentationTestCase2<SoundRecorder> {
    private Solo mSolo;
    private Instrumentation mInstrumentation = null;
    private SoundRecorder mSoundRecorder = null;
    private Recorder mRecorder;
    private String mRecordingFileBak = null;
    private ImageButton mRecordButton = null;
    private ImageButton mPlayButton = null;
    private ImageButton mStopButton = null;
    private ImageButton mPauseRecordingButton = null;
    private ImageButton mFileListButton = null;
    private Button mAcceptButton = null;
    private Button mDiscardButton = null;
    private TextView mStateMessage = null;
    private TextView mFileNameView = null;

    private static final String TAG = "SR/SoundRecorderTest";
    private static final String THREE_3GPP = ".3gpp";
    private static final int RECORDING_TIME_MIN = 100;
    private static final int RECORDING_TIME = 1000;
    private static final int RECORD_LONG_TIME = 5000;
    private static final int PLAYING_WAIT_STOP = 10000;
    private static final int WAIT_ACTIVITY_SETUP = 2000;
    private static final int WAIT_MOUNT_UNMOUNT = 10000;
    private static final int WAIT_FOR_RESPOND_TIME = 2000;
    private static final int WAIT_FOR_SAVE = 2000;
    private static final int FORMAT_ITEM_COUNT = 2;

    public SoundRecorderTest() {
        super("com.android.soundrecorder", SoundRecorder.class);
    }

    public SoundRecorderTest(String pkg, Class<SoundRecorder> activityClass) {
        super("com.android.soundrecorder", SoundRecorder.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        setActivityInitialTouchMode(false);
        initResourceRefs();
    }

    @Override
    protected void tearDown() throws Exception {
        mSolo.finishOpenedActivities();
        try {
            mSolo.finalize();
        } catch (Throwable t) {
            t.printStackTrace();
        }
        mInstrumentation.waitForIdleSync();
        super.tearDown();
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
        mAcceptButton = (Button) mSolo.getView(R.id.acceptButton);
        mDiscardButton = (Button) mSolo.getView(R.id.discardButton);
        mPauseRecordingButton = (ImageButton) mSolo.getView(R.id.pauseRecordingButton);
        mStateMessage = (TextView) mSolo.getView(R.id.stateMessage2);
        mFileNameView = (TextView) mSolo.getView(R.id.recordingFileName);
    }

    @UiAnnotation
    public void testCase01_RecordAndStop() {
        // Record Test
        checkIdleUI();
        mSolo.clickOnView(mRecordButton);
        mSolo.sleep(WAIT_FOR_RESPOND_TIME);

        // Check recording file
        String filePathWithTmp = SoundRecorderTestUtils.getCurrentFilePath(mSoundRecorder);
        SoundRecorderTestUtils.checkFileExist(filePathWithTmp);
        // Check recording state and UI
        assertTrue(SoundRecorderTestUtils.getCurrentState(mSoundRecorder) ==
                   SoundRecorderService.STATE_RECORDING);
        checkRecordingUI();

        // Recording 1 min
        mSolo.sleep(RECORDING_TIME);
        mSolo.clickOnView(mStopButton);
        mSolo.sleep(WAIT_FOR_RESPOND_TIME);

        // Check Saving state and UI
        assertTrue(SoundRecorderTestUtils.getCurrentState(mSoundRecorder) ==
                   SoundRecorderService.STATE_IDLE);
        checkSavingUI();
        SoundRecorderTestUtils.deleteFile(filePathWithTmp);
    }

    @UiAnnotation
    public void testCase02_AceeptDiscardAndRerecordAfterRecording() {
        // Aceept Test
        startRecordThenStop(RECORDING_TIME);
        String filePathWithTmp = SoundRecorderTestUtils.getCurrentFilePath(mSoundRecorder);
        SoundRecorderTestUtils.checkFileExist(filePathWithTmp);

        mSolo.clickOnView(mAcceptButton);
        mSolo.sleep(WAIT_FOR_SAVE);
        assertTrue(SoundRecorderTestUtils.getCurrentState(mSoundRecorder) ==
                   SoundRecorderService.STATE_IDLE);
        checkIdleUI();
        // Check if recording file has been added to MediaDB
        String filePath = filePathWithTmp.substring(0, filePathWithTmp
                .indexOf(Recorder.SAMPLE_SUFFIX));
        SoundRecorderTestUtils.checkFileExist(filePath);
        SoundRecorderTestUtils.checkFileNotExist(filePathWithTmp);
        SoundRecorderTestUtils.checkFileInMediaDB(mSoundRecorder, filePath, true);
        SoundRecorderTestUtils.deleteFile(filePath);
        SoundRecorderTestUtils.deleteFromMediaDB(mSoundRecorder, filePath);

        // Discard Test
        startRecordThenStop(RECORDING_TIME);
        filePathWithTmp = SoundRecorderTestUtils.getCurrentFilePath(mSoundRecorder);
        SoundRecorderTestUtils.checkFileExist(filePathWithTmp);

        mSolo.clickOnView(mDiscardButton);
        mSolo.sleep(WAIT_FOR_RESPOND_TIME);
        assertTrue(SoundRecorderTestUtils.getCurrentState(mSoundRecorder) ==
                   SoundRecorderService.STATE_IDLE);
        checkIdleUI();
        // Check if recording file has been deleted and not in MediaDB
        filePath = filePathWithTmp.substring(0, filePathWithTmp.indexOf(Recorder.SAMPLE_SUFFIX));
        SoundRecorderTestUtils.checkFileNotExist(filePath);
        SoundRecorderTestUtils.checkFileNotExist(filePathWithTmp);
        SoundRecorderTestUtils.checkFileInMediaDB(mSoundRecorder, filePath, false);

        // Rerecord Test
        startRecordThenStop(RECORDING_TIME);
        mSolo.clickOnView(mRecordButton);
        mSolo.sleep(WAIT_FOR_RESPOND_TIME);
        assertTrue(SoundRecorderTestUtils.getCurrentState(mSoundRecorder) ==
                   SoundRecorderService.STATE_RECORDING);
        checkRecordingUI();
        filePathWithTmp = SoundRecorderTestUtils.getCurrentFilePath(mSoundRecorder);
        SoundRecorderTestUtils.checkFileExist(filePathWithTmp);
        SoundRecorderTestUtils.deleteFile(filePathWithTmp);
    }

    @UiAnnotation
    public void testCase03_PlayAndPauseAndGoonAndStop() {
        startRecordThenStop(RECORD_LONG_TIME);
        String filePathTmp = SoundRecorderTestUtils.getCurrentFilePath(mSoundRecorder);
        SoundRecorderTestUtils.checkFileExist(filePathTmp);

        // Play
        mSolo.clickOnView(mPlayButton);
        mSolo.sleep(WAIT_FOR_RESPOND_TIME);
        assertTrue(SoundRecorderTestUtils.getCurrentState(mSoundRecorder) ==
                   SoundRecorderService.STATE_PLAYING);
        checkPlayingUI();

        // pause play
        mSolo.clickOnView(mPlayButton);
        mSolo.sleep(WAIT_FOR_RESPOND_TIME);
        assertTrue(SoundRecorderTestUtils.getCurrentState(mSoundRecorder) ==
                   SoundRecorderService.STATE_PAUSE_PLAYING);
        checkPlayingUI();

        // goon play
        mSolo.clickOnView(mPlayButton);
        mSolo.sleep(WAIT_FOR_RESPOND_TIME);
        assertTrue(SoundRecorderTestUtils.getCurrentState(mSoundRecorder) ==
                   SoundRecorderService.STATE_PLAYING);
        checkPlayingUI();

        // Stop (After Finishing the play)
        mSolo.sleep(PLAYING_WAIT_STOP);
        assertTrue(SoundRecorderTestUtils.getCurrentState(mSoundRecorder) ==
                   SoundRecorderService.STATE_IDLE);
        checkSavingUI();

        // Stop Test
        mSolo.clickOnView(mPlayButton);
        mSolo.sleep(WAIT_FOR_RESPOND_TIME);
        mSolo.clickOnView(mStopButton);
        mSolo.sleep(WAIT_FOR_RESPOND_TIME);
        assertTrue(SoundRecorderTestUtils.getCurrentState(mSoundRecorder) ==
                   SoundRecorderService.STATE_IDLE);
        checkSavingUI();
        SoundRecorderTestUtils.deleteFile(filePathTmp);
    }

    // public void testCase04_RecordAndBack() {
    // // Record Test
    // checkIdleUI();
    // mSolo.clickOnView(mRecordButton);
    // mSolo.sleep(WAIT_FOR_RESPOND_TIME);
    // assertTrue(SoundRecorderTestUtils.getCurrentState(mSoundRecorder) ==
    // SoundRecorderService.STATE_RECORDING);
    // checkRecordingUI();
    // String filePathTmp = SoundRecorderTestUtils
    // .getCurrentFilePath(mSoundRecorder);
    // SoundRecorderTestUtils.checkFileExist(filePathTmp);
    //
    // mSolo.sleep(RECORDING_TIME);
    //
    // // Back key Test
    // mSolo.goBack();
    // mSolo.sleep(WAIT_FOR_RESPOND_TIME);
    // assertTrue(SoundRecorderTestUtils.getCurrentState(mSoundRecorder) ==
    // SoundRecorderService.STATE_IDLE);
    // checkSavingUI();
    // SoundRecorderTestUtils.deleteFile(filePathTmp);
    // }
    /**
     * record one audio,then pause,and then go on recording and press back key
     */
    public void testCase05_RecordAndPauseAndGoonAndStop() {
        // Record Test
        checkIdleUI();
        mSolo.clickOnView(mRecordButton);
        mSolo.sleep(WAIT_FOR_RESPOND_TIME);
        assertTrue(SoundRecorderTestUtils.getCurrentState(mSoundRecorder) ==
                   SoundRecorderService.STATE_RECORDING);
        checkRecordingUI();
        String filePathTmp = SoundRecorderTestUtils.getCurrentFilePath(mSoundRecorder);
        SoundRecorderTestUtils.checkFileExist(filePathTmp);
        mSolo.sleep(RECORD_LONG_TIME);

        // Pause Test
        mSolo.clickOnView(mPauseRecordingButton);
        mSolo.sleep(WAIT_FOR_RESPOND_TIME);
        checkPauseRecordingUI();

        // goon test
        mSolo.clickOnView(mRecordButton);
        mSolo.sleep(WAIT_FOR_RESPOND_TIME);
        checkRecordingUI();

        // stop Test
        mSolo.clickOnView(mStopButton);
        mSolo.sleep(WAIT_FOR_RESPOND_TIME);
        assertTrue(SoundRecorderTestUtils.getCurrentState(mSoundRecorder) ==
                   SoundRecorderService.STATE_IDLE);
        checkSavingUI();
        SoundRecorderTestUtils.deleteFile(filePathTmp);
    }

    public void testCase06_SelectFormatAndRecord() {
        checkIdleUI();

        ArrayList<TextView> formatList = null;
        String formatSuffix = null;
        int formatListSize = 0;
        File recordingFile = null;
        for (int formatItemIndex = 0; formatItemIndex < 2; ) {
            // click menu item
            mSolo.clickOnMenuItem(mSolo.getString(R.string.voice_quality));
            mSolo.sleep(WAIT_FOR_RESPOND_TIME);

            // select a format
            formatItemIndex++;
            formatList = mSolo.clickInList(formatItemIndex);
            if ((formatItemIndex == FORMAT_ITEM_COUNT) && (formatList == null)) {
                break;
            }
            if (formatList.size() != 0) {
                formatSuffix = THREE_3GPP;
                mSolo.sleep(WAIT_FOR_RESPOND_TIME);

                // record and stop
                startRecordThenStop(RECORDING_TIME);
                String recordingFilePath = SoundRecorderTestUtils
                        .getCurrentFilePath(mSoundRecorder);
                SoundRecorderTestUtils.checkFileExist(recordingFilePath);
                recordingFile = new File(recordingFilePath);
                if (recordingFilePath.endsWith(Recorder.SAMPLE_SUFFIX)) {
                    recordingFilePath = recordingFilePath.substring(0, recordingFilePath
                            .indexOf(Recorder.SAMPLE_SUFFIX));
                }
                assertTrue(recordingFilePath.endsWith(formatSuffix));

                mSolo.clickOnView(mDiscardButton);
                mSolo.sleep(WAIT_FOR_RESPOND_TIME);
                SoundRecorderTestUtils.checkFileNotExist(recordingFilePath);
            }
        }
    }

    public void testCase07_SelectModeAndRecord() {
        checkIdleUI();

        // click menu item
        mSolo.sendKey(Solo.MENU);
        mSolo.sleep(WAIT_FOR_RESPOND_TIME);
        boolean isModeMenuItemExist = mSolo.searchText(mSolo.getString(R.string.recording_mode));
        if (isModeMenuItemExist) {
            for (int modeItemIndex = 0; modeItemIndex < 3; ) {
                // click menu item
                if (modeItemIndex == 0) {
                    mSolo.clickOnText(mSolo.getString(R.string.recording_mode));
                } else {
                    mSolo.clickOnMenuItem(mSolo.getString(R.string.recording_mode));
                }
                mSolo.sleep(WAIT_FOR_RESPOND_TIME);

                // select a mode
                modeItemIndex++;
                mSolo.clickInList(modeItemIndex);
                mSolo.sleep(WAIT_FOR_RESPOND_TIME);

                // record and stop
                startRecordThenStop(RECORDING_TIME);
                String filePathTmp = SoundRecorderTestUtils.getCurrentFilePath(mSoundRecorder);
                SoundRecorderTestUtils.checkFileExist(filePathTmp);
                checkSavingUI();

                //discard
                mSolo.clickOnView(mDiscardButton);
                mSolo.sleep(WAIT_FOR_RESPOND_TIME);
                SoundRecorderTestUtils.checkFileNotExist(filePathTmp);
            }
        }
    }

    public void testCase08_ChangeOrientation() {
        checkIdleUI();

        // idle
        mSolo.setActivityOrientation(Solo.LANDSCAPE);
        mSolo.sleep(WAIT_FOR_RESPOND_TIME);
        initResourceRefs();
        checkIdleUI();
        mSolo.setActivityOrientation(Solo.PORTRAIT);
        mSolo.sleep(WAIT_FOR_RESPOND_TIME);
        initResourceRefs();
        checkIdleUI();

        // record
        mSolo.clickOnView(mRecordButton);
        mSolo.sleep(WAIT_FOR_RESPOND_TIME);
        checkRecordingUI();
        String filePathTmp = SoundRecorderTestUtils.getCurrentFilePath(mSoundRecorder);
        SoundRecorderTestUtils.checkFileExist(filePathTmp);
        mSolo.setActivityOrientation(Solo.LANDSCAPE);
        mSolo.sleep(WAIT_FOR_RESPOND_TIME);
        initResourceRefs();
        checkRecordingUI();
        mSolo.setActivityOrientation(Solo.PORTRAIT);
        mSolo.sleep(WAIT_FOR_RESPOND_TIME);
        initResourceRefs();
        checkRecordingUI();
        mSolo.sleep(RECORDING_TIME);

        // pause-recording
        mSolo.clickOnView(mPauseRecordingButton);
        mSolo.sleep(WAIT_FOR_RESPOND_TIME);
        checkPauseRecordingUI();
        mSolo.setActivityOrientation(Solo.LANDSCAPE);
        mSolo.sleep(WAIT_FOR_RESPOND_TIME);
        initResourceRefs();
        checkPauseRecordingUI();
        mSolo.setActivityOrientation(Solo.PORTRAIT);
        mSolo.sleep(WAIT_FOR_RESPOND_TIME);
        initResourceRefs();
        checkPauseRecordingUI();

        // stop-recording
        mSolo.clickOnView(mStopButton);
        mSolo.sleep(WAIT_FOR_RESPOND_TIME);
        checkSavingUI();
        mSolo.setActivityOrientation(Solo.LANDSCAPE);
        mSolo.sleep(WAIT_FOR_RESPOND_TIME);
        initResourceRefs();
        checkSavingUI();
        mSolo.setActivityOrientation(Solo.PORTRAIT);
        mSolo.sleep(WAIT_FOR_RESPOND_TIME);
        initResourceRefs();
        checkSavingUI();

        // play
        mSolo.clickOnView(mPlayButton);
        mSolo.sleep(WAIT_FOR_RESPOND_TIME);
        checkPlayingUI();
        mSolo.setActivityOrientation(Solo.LANDSCAPE);
        mSolo.sleep(WAIT_FOR_RESPOND_TIME);
        initResourceRefs();
        checkPlayingUI();
        mSolo.setActivityOrientation(Solo.PORTRAIT);
        mSolo.sleep(WAIT_FOR_RESPOND_TIME);
        initResourceRefs();
        checkPlayingUI();

        // stop-play
        mSolo.clickOnView(mStopButton);
        mSolo.sleep(WAIT_FOR_RESPOND_TIME);
        checkSavingUI();
        mSolo.setActivityOrientation(Solo.LANDSCAPE);
        mSolo.sleep(WAIT_FOR_RESPOND_TIME);
        initResourceRefs();
        checkSavingUI();
        mSolo.setActivityOrientation(Solo.PORTRAIT);
        mSolo.sleep(WAIT_FOR_RESPOND_TIME);
        initResourceRefs();
        checkSavingUI();

        // accept
        mSolo.clickOnView(mAcceptButton);
        mSolo.sleep(WAIT_FOR_SAVE);
        checkIdleUI();
        mSolo.setActivityOrientation(Solo.LANDSCAPE);
        mSolo.sleep(WAIT_FOR_RESPOND_TIME);
        initResourceRefs();
        checkIdleUI();
        mSolo.setActivityOrientation(Solo.PORTRAIT);
        mSolo.sleep(WAIT_FOR_RESPOND_TIME);
        initResourceRefs();
        checkIdleUI();
        String filePath = filePathTmp.substring(0, filePathTmp.indexOf(Recorder.SAMPLE_SUFFIX));
        SoundRecorderTestUtils.checkFileExist(filePath);
        SoundRecorderTestUtils.checkFileNotExist(filePathTmp);
        SoundRecorderTestUtils.checkFileInMediaDB(mSoundRecorder, filePath, true);
        SoundRecorderTestUtils.deleteFile(filePath);
        SoundRecorderTestUtils.deleteFromMediaDB(mSoundRecorder, filePath);
    }

//    public void testCase09_UnmountSDCard() {
//        checkIdleUI();
//
//        // record
//        mSolo.clickOnView(mRecordButton);
//        mSolo.sleep(WAIT_FOR_RESPOND_TIME);
//        checkRecordingUI();
//        String filePathTmp = SoundRecorderTestUtils.getCurrentFilePath(mSoundRecorder);
//        SoundRecorderTestUtils.checkFileExist(filePathTmp);
//
//        // unmount SD card
//        SoundRecorderTestUtils.unmountSDCard(mSoundRecorder);
//        mSolo.sleep(WAIT_MOUNT_UNMOUNT);
//        TextView errorTextView = mSolo.getText(mSolo.getString(R.string.recording_fail));
//        assertNotNull(errorTextView);
//        assertTrue(errorTextView.isShown());
//
//        // mount SD card
//        SoundRecorderTestUtils.mountSDCard(mSoundRecorder);
//        mSolo.sleep(WAIT_MOUNT_UNMOUNT);
//        checkIdleUI();
//        SoundRecorderTestUtils.checkFileNotExist(filePathTmp);
//
//        // record
//        mSolo.clickOnView(mRecordButton);
//        mSolo.sleep(WAIT_FOR_RESPOND_TIME);
//        checkRecordingUI();
//        filePathTmp = SoundRecorderTestUtils.getCurrentFilePath(mSoundRecorder);
//        SoundRecorderTestUtils.checkFileExist(filePathTmp);
//        SoundRecorderTestUtils.deleteFile(filePathTmp);
//    }

//    public void testCase10_StartRecordErrorProcess() {
//        checkIdleUI();
//
//        // start a MediaRecorder
//        File tempFile = new File(Environment.getExternalStorageDirectory(),
//                "SoundRecorder_testCase10.amr");
//        try {
//            if (!tempFile.exists()) {
//                boolean isCreatSuccess = tempFile.createNewFile();
//                if (isCreatSuccess) {
//                    Log.i(TAG, "creat temp file successfully");
//                }
//            }
//        } catch (IOException e) {
//            Log.i(TAG, "<testCase10_CatchException> " + e.getMessage());
//        }
//
//        MediaRecorder tempRecorder = new MediaRecorder();
//        tempRecorder.setOutputFile(tempFile.getAbsolutePath());
//        tempRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
//        tempRecorder.setOutputFormat(MediaRecorder.OutputFormat.AMR_NB);
//        tempRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
//        try {
//            tempRecorder.prepare();
//            tempRecorder.start();
//        } catch (IllegalStateException e) {
//            Log.i(TAG, e.getMessage());
//        } catch (IOException e) {
//            Log.i(TAG, e.getMessage());
//        }
//
//        mSolo.sleep(RECORDING_TIME);
//
//        // start recording in SoundRecorder
//        mSolo.clickOnView(mRecordButton);
//        mSolo.sleep(WAIT_FOR_RESPOND_TIME);
//
//        if (!mRecordButton.isEnabled() && !mPlayButton.isEnabled() && mStopButton.isEnabled()
//            && !mAcceptButton.isShown() && !mDiscardButton.isShown() && mStateMessage.isShown()
//            && mStateMessage.getText().equals(mSolo.getString(R.string.recording))) {
//            TextView errorTextView = mSolo.getText(mSolo.getString(R.string.recorder_occupied));
//            assertNotNull(errorTextView);
//            assertTrue(errorTextView.isShown());
//        }
//
//        try {
//            tempRecorder.stop();
//        } catch (RuntimeException e) {
//            Log.i(TAG, e.getMessage());
//        }
//
//        if (tempFile.exists()) {
//            boolean isDeleted = tempFile.delete();
//            if (isDeleted) {
//                Log.i(TAG, "temp file is deleted successfully");
//            }
//        }
//    }

    public void testCase11_Relaunch() {
        startRecordThenStop(RECORDING_TIME);
        String filePathWithTmp = SoundRecorderTestUtils.getCurrentFilePath(mSoundRecorder);
        SoundRecorderTestUtils.checkFileExist(filePathWithTmp);

        // relaunch after stop recording
        relaunchSoundRecorder();

        // check
        ActivityMonitor soundRecorderAM = mInstrumentation.addMonitor(
                "com.android.soundrecorder.SoundRecorder", null, false);
        assertNotNull(soundRecorderAM.waitForActivityWithTimeout(WAIT_ACTIVITY_SETUP));
        initResourceRefs();
        checkSavingUI();
        SoundRecorderTestUtils.checkFileExist(filePathWithTmp);

        // delete recording file
        SoundRecorderTestUtils.deleteFile(filePathWithTmp);
    }

    private void relaunchSoundRecorder() {
        mSolo.getCurrentActivity().runOnUiThread(new Runnable() {

            @Override
            public void run() {
                mSolo.getCurrentActivity().recreate();

            }
        });
    }

    private void startRecordThenStop(int recordingTime) {
        int finalTime = recordingTime;
        mSolo.clickOnView(mRecordButton);
        mSolo.sleep(WAIT_FOR_RESPOND_TIME);

        // asure the recording time is enough (>0.1s)
        if (finalTime < RECORDING_TIME_MIN) {
            finalTime = RECORDING_TIME_MIN;
        }
        mSolo.sleep(finalTime);
        mRecordingFileBak = SoundRecorderTestUtils.getCurrentFilePath(mSoundRecorder);
        mSolo.clickOnView(mStopButton);
        mSolo.sleep(WAIT_FOR_RESPOND_TIME);
        checkSavingUI();
    }

    protected void checkIdleUI() {
        assertTrue(mRecordButton.isEnabled());
        assertFalse(mPlayButton.isShown());
        assertFalse(mStopButton.isEnabled());
        assertFalse(mAcceptButton.isShown());
        assertFalse(mDiscardButton.isShown());
    }

    protected void checkRecordingUI() {
        assertFalse(mRecordButton.isShown());
        assertFalse(mPlayButton.isShown());
        assertTrue(mStopButton.isEnabled());
        assertFalse(mAcceptButton.isShown());
        assertFalse(mDiscardButton.isShown());

        assertTrue(mStateMessage.isShown());
        assertTrue(mStateMessage.getText().equals(mSolo.getString(R.string.recording)));
    }

    protected void checkSavingUI() {
        assertTrue(mRecordButton.isEnabled());
        assertTrue(mPlayButton.isEnabled());
        assertFalse(mStopButton.isEnabled());
        assertTrue(mAcceptButton.isEnabled());
        assertTrue(mDiscardButton.isEnabled());
        assertFalse(mStateMessage.isShown());
    }

    protected void checkPlayingUI() {
        assertTrue(mRecordButton.isEnabled());
        assertTrue(mPlayButton.isEnabled());
        assertTrue(mStopButton.isEnabled());
        assertTrue(mAcceptButton.isEnabled());
        assertTrue(mDiscardButton.isEnabled());
        assertFalse(mStateMessage.isShown());
    }

    protected void checkPauseRecordingUI() {
        assertTrue(mStateMessage.isShown());
        assertTrue(mStateMessage.getText().equals(mSolo.getString(R.string.recording_paused)));

        String recordingFilePath = SoundRecorderTestUtils.getCurrentFilePath(mSoundRecorder);
        String recordingFileName = recordingFilePath.substring(
                (recordingFilePath.lastIndexOf('/') + 1), recordingFilePath
                        .indexOf(Recorder.SAMPLE_SUFFIX));
        assertTrue(mFileNameView.getText().equals(recordingFileName));

        assertTrue(mRecordButton.isEnabled());
        assertFalse(mPauseRecordingButton.isShown());
        assertTrue(mStopButton.isEnabled());
    }
}
