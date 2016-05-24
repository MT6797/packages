package com.android.soundrecorder.tests;

import com.android.soundrecorder.Recorder;
import com.android.soundrecorder.SoundRecorder;

import android.app.Instrumentation;
import android.test.ActivityInstrumentationTestCase2;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import com.android.soundrecorder.R;
import com.jayway.android.robotium.solo.Solo;
public class SoundRecorderPerformanceTestCase
    extends ActivityInstrumentationTestCase2<SoundRecorder> {

    private SoundRecorder mActivity;
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

    private static final int WAIT_FOR_RESPOND_TIME = 2000;
    private static final int RECORDING_TIME = 2000;

    private static final String TAG = "SR/SoundRecorderPerformanceTest";

    public SoundRecorderPerformanceTestCase(Class<SoundRecorder> activityClass) {
        super(activityClass);
    }

    public SoundRecorderPerformanceTestCase() {
        super(SoundRecorder.class);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        Log.i(TAG, "super.setUp");
        setActivityInitialTouchMode(false);
        mSoundRecorder = getActivity();
        mInstrumentation = getInstrumentation();
        mSolo = new Solo(mInstrumentation, mSoundRecorder);

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
    public void tearDown() throws Exception {
        try {
            mSolo.finalize();
        } catch (Throwable t) {
            t.printStackTrace();
        }
        mSolo.finishOpenedActivities();
        mInstrumentation.waitForIdleSync();
        super.tearDown();
    }
    /**
     * Test start recording response time.
     */
    public void testCase01_StartAndStopAndSaveButtonPerformance() throws Exception {
        long mStartRecordingTime = System.currentTimeMillis();
        Log.i(TAG,
            "[Performance test][SoundRecorder] recording start [" + mStartRecordingTime + "]");
        mSolo.clickOnView(mRecordButton);
        mSolo.sleep(RECORDING_TIME);
        long mStartStopTime = System.currentTimeMillis();
        Log.i(TAG,
            "[Performance test][SoundRecorder] recording stop start [" + mStartStopTime + "]");
        mSolo.clickOnView(mStopButton);
        mSolo.sleep(WAIT_FOR_RESPOND_TIME);
        long mStartSaveTime = System.currentTimeMillis();
        Log.i(TAG,
            "[Performance test][SoundRecorder] recording save start [" + mStartSaveTime + "]");
        mSolo.clickOnView(mAcceptButton);
    }
}
