package com.android.soundrecorder.tests;

import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.test.ActivityInstrumentationTestCase2;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import com.android.soundrecorder.R;
import com.android.soundrecorder.Recorder;
import com.android.soundrecorder.SoundRecorder;
import com.jayway.android.robotium.solo.Solo;

public class SoundRecorderFromMMSTest extends
        ActivityInstrumentationTestCase2<SoundRecorder> {
    private static final int RECORDING_TIME = 1000;
    private static final int WAIT_FOR_RESPOND_TIME = 2000;
    private static final long FILE_SIZE_BYTES = 300000;
    private static final String FILE_TYPE = "audio/amr";
    private static final String INTENT_ACTION = "android.intent.action.GET_CONTENT";

    private Solo mSolo;
    private Instrumentation mInstrumentation = null;
    private SoundRecorder mSoundRecorder = null;

    private ImageButton mRecordButton = null;
    private ImageButton mPlayButton = null;
    private ImageButton mStopButton = null;
    private ImageButton mPauseRecordingButton = null;
    private ImageButton mFileListButton = null;
    private Button mAcceptButton = null;
    private Button mDiscardButton = null;
    private TextView mStateMessage = null;
    private TextView mFileNameView = null;

    public SoundRecorderFromMMSTest() {
        super("com.android.soundrecorder", SoundRecorder.class);
    }

    public SoundRecorderFromMMSTest(String pkg,
            Class<SoundRecorder> activityClass) {
        super("com.android.soundrecorder", SoundRecorder.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        Intent intent = new Intent();
        intent.setType(FILE_TYPE);
        intent.putExtra(
                android.provider.MediaStore.Audio.Media.EXTRA_MAX_BYTES,
                FILE_SIZE_BYTES);
        intent.setAction(INTENT_ACTION);
        setActivityIntent(intent);

        setActivityInitialTouchMode(false);
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
        mPauseRecordingButton = (ImageButton) mSolo
                .getView(R.id.pauseRecordingButton);
        mStateMessage = (TextView) mSolo.getView(R.id.stateMessage2);
        mFileNameView = (TextView) mSolo.getView(R.id.recordingFileName);
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

    public void testCase01_recordAndStopAndAccept() {
        checkIdleUI();

        // record
        mSolo.clickOnView(mRecordButton);
        mSolo.sleep(WAIT_FOR_RESPOND_TIME);
        checkRecordingUI();
        String filePathWithTmp = SoundRecorderTestUtils
                .getCurrentFilePath(mSoundRecorder);
        SoundRecorderTestUtils.checkFileExist(filePathWithTmp);

        // stop
        mSolo.clickOnView(mStopButton);
        mSolo.sleep(WAIT_FOR_RESPOND_TIME);
        checkSavingUI();

        // accept
        mSolo.clickOnView(mAcceptButton);
        mSolo.sleep(WAIT_FOR_RESPOND_TIME);

        // check file and db
        String filePath = filePathWithTmp.substring(0,
                filePathWithTmp.lastIndexOf(Recorder.SAMPLE_SUFFIX));
        Context context = mInstrumentation.getContext();
        SoundRecorderTestUtils.checkFileNotExist(filePathWithTmp);
        SoundRecorderTestUtils.checkFileExist(filePath);
        SoundRecorderTestUtils.checkFileInMediaDB(context, filePathWithTmp,
                false);
        SoundRecorderTestUtils.checkFileInMediaDB(context, filePath, true);

        // recorver envioroment
        SoundRecorderTestUtils.deleteFile(filePath);
        SoundRecorderTestUtils.deleteFromMediaDB(context, filePath);
    }

    public void testCase02_recordAndStopAndDiscard() {
        checkIdleUI();

        // record
        mSolo.clickOnView(mRecordButton);
        mSolo.sleep(WAIT_FOR_RESPOND_TIME);
        checkRecordingUI();
        String filePathWithTmp = SoundRecorderTestUtils
                .getCurrentFilePath(mSoundRecorder);
        SoundRecorderTestUtils.checkFileExist(filePathWithTmp);

        // stop
        mSolo.clickOnView(mStopButton);
        mSolo.sleep(WAIT_FOR_RESPOND_TIME);
        checkSavingUI();

        // discard
        mSolo.clickOnView(mDiscardButton);
        mSolo.sleep(WAIT_FOR_RESPOND_TIME);

        // check file and db
        String filePath = filePathWithTmp.substring(0,
                filePathWithTmp.lastIndexOf(Recorder.SAMPLE_SUFFIX));
        Context context = mInstrumentation.getContext();
        SoundRecorderTestUtils.checkFileNotExist(filePathWithTmp);
        SoundRecorderTestUtils.checkFileNotExist(filePath);
        SoundRecorderTestUtils.checkFileInMediaDB(context, filePathWithTmp,
                false);
        SoundRecorderTestUtils.checkFileInMediaDB(context, filePath, false);
    }

    public void testCase03_recordAndStopAndRerecord() {
        checkIdleUI();

        // record
        mSolo.clickOnView(mRecordButton);
        mSolo.sleep(WAIT_FOR_RESPOND_TIME);
        // check
        checkRecordingUI();
        String filePathWithTmp1 = SoundRecorderTestUtils
                .getCurrentFilePath(mSoundRecorder);
        SoundRecorderTestUtils.checkFileExist(filePathWithTmp1);

        // stop
        mSolo.clickOnView(mStopButton);
        mSolo.sleep(WAIT_FOR_RESPOND_TIME);
        // check
        checkSavingUI();

        // re-record
        mSolo.clickOnView(mRecordButton);
        mSolo.sleep(WAIT_FOR_RESPOND_TIME);
        // check
        checkRecordingUI();
        String filePathWithTmp2 = SoundRecorderTestUtils
                .getCurrentFilePath(mSoundRecorder);
        SoundRecorderTestUtils.checkFileExist(filePathWithTmp2);
        SoundRecorderTestUtils.checkFileNotExist(filePathWithTmp1);

        // stop
        mSolo.clickOnView(mStopButton);
        mSolo.sleep(WAIT_FOR_RESPOND_TIME);
        //check
        checkSavingUI();

        //recover envioroment
        SoundRecorderTestUtils.deleteFile(filePathWithTmp2);
    }

    protected void checkIdleUI() {
        assertTrue(mRecordButton.isEnabled());
        assertFalse(mStopButton.isEnabled());
        assertFalse(mAcceptButton.isShown());
        assertFalse(mDiscardButton.isShown());
        assertFalse(mStateMessage.isShown());
        assertFalse(mFileNameView.isShown());

        assertFalse(mPlayButton.isShown());
        assertFalse(mFileListButton.isShown());
        assertFalse(mPauseRecordingButton.isShown());
    }

    protected void checkRecordingUI() {
        assertFalse(mRecordButton.isEnabled());
        assertTrue(mStopButton.isEnabled());
        assertFalse(mAcceptButton.isShown());
        assertFalse(mDiscardButton.isShown());
        assertTrue(mStateMessage.isShown());
        assertTrue(mFileNameView.isShown());

        assertFalse(mPlayButton.isShown());
        assertFalse(mFileListButton.isShown());
        assertFalse(mPauseRecordingButton.isShown());
    }

    protected void checkSavingUI() {
        assertTrue(mRecordButton.isEnabled());
        assertFalse(mStopButton.isEnabled());
        assertTrue(mAcceptButton.isShown());
        assertTrue(mDiscardButton.isShown());
        assertFalse(mStateMessage.isShown());
        assertTrue(mFileNameView.isShown());

        assertFalse(mPlayButton.isShown());
        assertFalse(mFileListButton.isShown());
        assertFalse(mPauseRecordingButton.isShown());
    }
}
