package com.android.soundrecorder.tests;

import java.io.File;

import android.app.Instrumentation;
import android.app.Instrumentation.ActivityMonitor;
import android.content.Context;
import android.content.res.Resources;
import android.os.Environment;
import android.test.ActivityInstrumentationTestCase2;

import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import com.android.soundrecorder.R;
import com.android.soundrecorder.Recorder;
import com.android.soundrecorder.LogUtils;
import com.android.soundrecorder.SoundRecorder;
import com.android.soundrecorder.SoundRecorderService;
import com.android.soundrecorder.RecordingFileList;

import com.jayway.android.robotium.solo.Solo;

public class RecordingFileListTest extends
        ActivityInstrumentationTestCase2<SoundRecorder> {
    private Instrumentation mInstrumentation = null;
    private Solo mSolo = null;
    private RecordingFileList mRecordingFileList = null;
    private SoundRecorder mSoundRecorder = null;
    private ListView mRecordingFileListView = null;
    private ImageButton mRecordButtonInFileList = null;
    private ImageButton mDeleteButtonInFileList = null;
    private View mEmptyView = null;
    private Resources mRes = null;
    private Context mContext = null;
    private String mRecordingFilePathInSetup = null;

    private ImageButton mRecordButtonInSoundRecorder = null;
    private ImageButton mPlayButtonInSoundRecorder = null;
    private ImageButton mStopButtonInSoundRecorder = null;
    private ImageButton mFileListButtonInSoundRecorder = null;
    private Button mDiscardButtonInSoundRecorder = null;
    private Button mAcceptButtonInSoundRecorder = null;
    private TextView mStateMessageInSoundRecorder = null;

    private static final String TAG = "SR/RecordingFileListTest";
    private static final int RECORDING_TIME = 6000;
    private static final int WAIT_FOR_RESPOND_TIME = 2000;
    private static final int WAIT_FOR_ACTIVITY = 2000;
    private static final int WAIT_MOUNT_UNMOUNT = 10000;
    private static final int WAIT_LONG_PRESS = 5000;
    private static final int WAIT_FOR_TOAST_DISMISS = 5000;
    private static final int FIRST_ITEM = 1;

    public RecordingFileListTest() {
        super("com.android.soundrecorder", SoundRecorder.class);
    }

    public RecordingFileListTest(String pkg, Class<SoundRecorder> activityClass) {
        super("com.android.soundrecorder", SoundRecorder.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        setActivityInitialTouchMode(false);
        mInstrumentation = this.getInstrumentation();
        assertNotNull(mInstrumentation);
        mSoundRecorder = getActivity();
        assertNotNull(mSoundRecorder);
        mSolo = new Solo(mInstrumentation, mSoundRecorder);

        // get view references in SoundRecorder
        mRecordButtonInSoundRecorder = (ImageButton) mSoundRecorder
                .findViewById(R.id.recordButton);
        mPlayButtonInSoundRecorder = (ImageButton) mSoundRecorder
                .findViewById(R.id.playButton);
        mStopButtonInSoundRecorder = (ImageButton) mSoundRecorder
                .findViewById(R.id.stopButton);
        mFileListButtonInSoundRecorder = (ImageButton) mSoundRecorder
                .findViewById(R.id.fileListButton);
        mDiscardButtonInSoundRecorder = (Button) mSoundRecorder
                .findViewById(R.id.discardButton);
        mAcceptButtonInSoundRecorder = (Button) mSoundRecorder
                .findViewById(R.id.acceptButton);
        mStateMessageInSoundRecorder = (TextView) mSoundRecorder
                .findViewById(R.id.stateMessage2);

        // record a file first
        mSolo.clickOnView(mRecordButtonInSoundRecorder);
        mSolo.sleep(RECORDING_TIME);
        mRecordingFilePathInSetup = SoundRecorderTestUtils
                .getCurrentFilePath(mSoundRecorder);
        mSolo.clickOnView(mStopButtonInSoundRecorder);
        mSolo.sleep(WAIT_FOR_RESPOND_TIME);
        mSolo.clickOnView(mAcceptButtonInSoundRecorder);

        // go to file list
        ActivityMonitor recordingFileListAM = mInstrumentation.addMonitor(
                "com.android.soundrecorder.RecordingFileList", null, false);
        mSolo.sleep(WAIT_FOR_TOAST_DISMISS);
        mSolo.clickOnView(mFileListButtonInSoundRecorder);
        waitActivityAndInitResourceRefs(recordingFileListAM);
    }

    @Override
    protected void tearDown() throws Exception {
        // delete recording file which is created in setUp()
        SoundRecorderTestUtils.deleteFile(mRecordingFilePathInSetup);
        SoundRecorderTestUtils.deleteFromMediaDB(mSoundRecorder,
                mRecordingFilePathInSetup);
        mSolo.finishOpenedActivities();
        try {
            mSolo.finalize();
        } catch (Throwable t) {
            t.printStackTrace();
        }
        super.tearDown();
    }

    public void testCase01_ClickRecord() {
        ActivityMonitor soundRecorderAM = mInstrumentation.addMonitor(
                "com.android.soundrecorder.SoundRecorder", null, false);

        // 1-record in recording file list
        mSolo.clickOnView(mRecordButtonInFileList);
        assertNotNull(soundRecorderAM
                .waitForActivityWithTimeout(WAIT_FOR_ACTIVITY));
        mSolo.sleep(RECORDING_TIME);
        // Check recording state and UI
        assertTrue(SoundRecorderTestUtils.getCurrentState(mSoundRecorder) ==
                   SoundRecorderService.STATE_RECORDING);
        checkRecordingUI();
        // Check recording file
        String filePathWithTmp = SoundRecorderTestUtils
                .getCurrentFilePath(mSoundRecorder);
        LogUtils.i(TAG, "filePathWithTmp = " + filePathWithTmp);
        SoundRecorderTestUtils.checkFileExist(filePathWithTmp);

        // 2-stop record
        mSolo.clickOnView(mStopButtonInSoundRecorder);
        mSolo.sleep(WAIT_FOR_RESPOND_TIME);
        // Check
        checkSavingUI();

        // 3-accept
        Button mAcceptButtonInSoundRecorder = (Button) mSoundRecorder
                .findViewById(R.id.acceptButton);
        mSolo.clickOnView(mAcceptButtonInSoundRecorder);
        mSolo.sleep(WAIT_FOR_RESPOND_TIME);
        // Check if recording file has been added to MediaDB
        String filePath = filePathWithTmp.substring(0,
                filePathWithTmp.indexOf(Recorder.SAMPLE_SUFFIX));
        SoundRecorderTestUtils.checkFileExist(filePath);
        SoundRecorderTestUtils.checkFileNotExist(filePathWithTmp);
        SoundRecorderTestUtils.checkFileInMediaDB(mSoundRecorder, filePath,
                true);
        checkIdleUI();

        //recovery enviroment
        SoundRecorderTestUtils.deleteFile(filePath);
        SoundRecorderTestUtils.deleteFromMediaDB(mSoundRecorder, filePath);
    }

    public void testCase02_LongpressAndDelete() {
        assertTrue((mRecordingFileListView != null)
                && (mRecordingFileListView.getCount() > 0));

        // select a recording file to be deleted
        ListAdapter listAdapter = mRecordingFileListView.getAdapter();
        View view = listAdapter.getView(0, null, mRecordingFileListView);
        TextView textView = (TextView) view.findViewById(R.id.record_file_name);
        String fileName = textView.getText().toString();
        mSolo.clickLongOnText(fileName, 1, WAIT_LONG_PRESS);

        // click delete button
        mSolo.clickOnView(mDeleteButtonInFileList);
        mSolo.sleep(WAIT_FOR_RESPOND_TIME);
        assertTrue(mSolo.getText(mSolo.getString(R.string.alert_delete_single))
                .isShown());
        mInstrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_DOWN);
        mInstrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_RIGHT);
        mInstrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_CENTER);
        mInstrumentation.waitForIdleSync();

        try {
            Thread.sleep(WAIT_FOR_ACTIVITY);
        } catch (InterruptedException e) {
            LogUtils.e(TAG, e.getMessage());
        }

        // check
        assertFalse(isExistInRecordingFileList(fileName));
        StringBuilder sb = new StringBuilder();
        sb.append(Environment.getExternalStorageDirectory());
        sb.append("/");
        sb.append(Recorder.RECORD_FOLDER);
        sb.append("/");
        sb.append(fileName);
        SoundRecorderTestUtils.checkFileNotExist(sb.toString());
        SoundRecorderTestUtils.checkFileInMediaDB(mRecordingFileList,
                sb.toString(), false);
    }

//    public void testCase03_ClickPlay() {
//        ActivityMonitor soundRecorderAM = mInstrumentation.addMonitor(
//                "com.android.soundrecorder.SoundRecorder", null, false);
//
//        mSolo.clickInList(FIRST_ITEM);
//        assertNotNull(soundRecorderAM
//                .waitForActivityWithTimeout(WAIT_FOR_ACTIVITY));
//        // check
//        checkPlayingUI();
//        assertTrue(SoundRecorderTestUtils.isMediaPlayerPlaying(mSoundRecorder));
//    }

    public void testCase04_PressBackKey() {
        ActivityMonitor soundRecorderAM = mInstrumentation.addMonitor(
                "com.android.soundrecorder.SoundRecorder", null, false);
        ActivityMonitor recordingFileListAM = mInstrumentation.addMonitor(
                "com.android.soundrecorder.RecordingFileList", null, false);

        // press back key
        mSolo.goBack();
        mSolo.sleep(WAIT_FOR_RESPOND_TIME);
        assertNotNull(soundRecorderAM
                .waitForActivityWithTimeout(WAIT_FOR_ACTIVITY));
        checkIdleUI();

        // go to file list
        mSolo.clickOnView(mFileListButtonInSoundRecorder);
        mSolo.sleep(WAIT_FOR_RESPOND_TIME);
        waitActivityAndInitResourceRefs(recordingFileListAM);

        // long press one item
        mSolo.clickLongInList(FIRST_ITEM);
        mSolo.sleep(WAIT_FOR_RESPOND_TIME);
        assertTrue(mDeleteButtonInFileList.isEnabled());
        assertFalse(mRecordButtonInFileList.isShown());

        // press back key
        mSolo.goBack();
        mSolo.goBack();
        mSolo.sleep(WAIT_FOR_RESPOND_TIME);
        assertNotNull(soundRecorderAM
                .waitForActivityWithTimeout(WAIT_FOR_ACTIVITY));
        checkIdleUI();
    }

    public void testCase05_SwitchEditNormalMode() {
        mSolo.clickLongInList(FIRST_ITEM);
        mSolo.sleep(WAIT_FOR_RESPOND_TIME);
        assertTrue(mDeleteButtonInFileList.isEnabled());
        assertFalse(mRecordButtonInFileList.isShown());

        mSolo.clickInList(FIRST_ITEM);
        mSolo.sleep(WAIT_FOR_RESPOND_TIME);
        assertTrue(mRecordButtonInFileList.isEnabled());
        assertFalse(mDeleteButtonInFileList.isShown());
    }

    /**
     * unmount SD card when RecordingFileList is active, RecordingFileList
     * should finish and return to SoundRecorder
     */
    public void testCase06_UnmountSDCard() {
        /*
        ActivityMonitor soundRecorderAM = mInstrumentation.addMonitor(
                "com.android.soundrecorder.SoundRecorder", null, false);

        // unmount SD card
        SoundRecorderTestUtils.unmountSDCard(mSoundRecorder);
        mSolo.sleep(WAIT_MOUNT_UNMOUNT);
        // check whether return to SoundRecorder
        assertNotNull(soundRecorderAM
                .waitForActivityWithTimeout(WAIT_FOR_ACTIVITY));
        checkIdleUI();

        // press record
        mSolo.clickOnView(mRecordButtonInSoundRecorder);
        mSolo.sleep(WAIT_FOR_RESPOND_TIME);
        // check whether can begin record
        checkIdleUI();
        assertTrue(SoundRecorderTestUtils.getCurrentState(mSoundRecorder) ==
                   SoundRecorderService.STATE_IDLE);

        // mount SD card
        SoundRecorderTestUtils.mountSDCard(mSoundRecorder);
        mSolo.sleep(WAIT_MOUNT_UNMOUNT);
        */
    }

    public void testCase07_StartPlayErrorProcess() {
        assertTrue((mRecordingFileListView != null)
                && (mRecordingFileListView.getCount() > 0));
        ListAdapter listAdapter = mRecordingFileListView.getAdapter();
        View view = listAdapter.getView(0, null, mRecordingFileListView);
        TextView textView = (TextView) view.findViewById(R.id.record_file_name);
        String fileName = textView.getText().toString();

        ActivityMonitor soundRecorderAM = mInstrumentation.addMonitor(
                "com.android.soundrecorder.SoundRecorder", null, false);

        // delete the first recording file
        StringBuilder sb = new StringBuilder();
        sb.append(Environment.getExternalStorageDirectory());
        sb.append("/");
        sb.append(Recorder.RECORD_FOLDER);
        sb.append("/");
        sb.append(fileName);
        File tempFile = new File(sb.toString());
        assertNotNull(tempFile);
        if (!tempFile.delete()) {
            LogUtils.e(TAG, "<testCase07_StartPlayErrorProcess> delete file failed");
            return;
        }

        // press the first recording file to playback
        mSolo.clickOnText(fileName);
        assertNotNull(soundRecorderAM
                .waitForActivityWithTimeout(WAIT_FOR_ACTIVITY));

        // check error dialog
        mSolo.sleep(WAIT_FOR_ACTIVITY);
        TextView errorTextView = mSolo.getText(mSolo
                .getString(R.string.recording_doc_deleted));
        assertNotNull(errorTextView);
        assertTrue(errorTextView.isShown());

        SoundRecorderTestUtils.deleteFromMediaDB(mRecordingFileList, tempFile.getAbsolutePath());
    }

    public void testCase08_DeleteAllAndShowNoRecordingFile() {
        assertTrue((mRecordingFileListView != null)
                && (mRecordingFileListView.getCount() > 0));

        // long press and inter EDIT mode
        ListAdapter listAdapter = mRecordingFileListView.getAdapter();
        View view = listAdapter.getView(0, null, mRecordingFileListView);
        TextView textView = (TextView) view.findViewById(R.id.record_file_name);
        String fileName = textView.getText().toString();
        mSolo.clickLongOnText(fileName, 1, WAIT_LONG_PRESS);

        // select all recording file
        boolean goonScroll;
        int totalItemCount = mRecordingFileListView.getCount();
        for (int j = 0; j < totalItemCount; ) {
            listAdapter = mRecordingFileListView.getAdapter();
            int i = 1;
            while (true) {
                try {
                    view = listAdapter.getView(i, null, mRecordingFileListView);
                } catch (IndexOutOfBoundsException e) {
                    LogUtils.e(TAG, "<testCase08_DeleteAllAndShowNoRecordingFile> "
                                    + e.getMessage());
                    view = null;
                    break;
                }
                textView = (TextView) view.findViewById(R.id.record_file_name);
                fileName = textView.getText().toString();
                mSolo.clickOnText(fileName);
                mSolo.sleep(WAIT_FOR_RESPOND_TIME);
                i++;
                j++;
            }
            goonScroll = mSolo.scrollDownList(0);
            mSolo.sleep(WAIT_FOR_RESPOND_TIME);
            if (!goonScroll) {
                break;
            }
        }

        // click delete button
        mSolo.clickOnView(mDeleteButtonInFileList);
        mSolo.sleep(WAIT_FOR_RESPOND_TIME);
        assertTrue(mSolo.getText(
                mSolo.getString(R.string.alert_delete_multiple)).isShown());
        mSolo.clickOnButton(mSolo.getString(R.string.ok));
        mSolo.sleep(WAIT_FOR_RESPOND_TIME);

        // check UI
        TextView noRecordingFileTextView = mSolo.getText(mSolo
                .getString(R.string.no_recording_file));
        assertTrue(noRecordingFileTextView.isShown());
    }

    public void testCase09_Relaunch() {
        //relaunch in NORMAL state
        relaunchRecordingFileList();
        ActivityMonitor recordingFileListAM = mInstrumentation.addMonitor(
                "com.android.soundrecorder.RecordingFileList", null, false);
        //check
        waitActivityAndInitResourceRefs(recordingFileListAM);
        assertFalse(mDeleteButtonInFileList.isShown());
        assertTrue(mRecordButtonInFileList.isEnabled());

        //turn into EDIT state
        mSolo.clickLongInList(FIRST_ITEM);
        mSolo.sleep(WAIT_FOR_RESPOND_TIME);
        assertTrue(mDeleteButtonInFileList.isEnabled());
        assertFalse(mRecordButtonInFileList.isShown());

        //relaunch in EDIT state
        relaunchRecordingFileList();

        //check
        waitActivityAndInitResourceRefs(recordingFileListAM);
        assertTrue(mDeleteButtonInFileList.isEnabled());
        assertFalse(mRecordButtonInFileList.isShown());

        // click delete button
        // mSolo = new Solo(mInstrumentation, mRecordingFileList);
        // initResourceRefs();
        // mSolo.clickOnButton(R.id.deleteButton);
        // mSolo.sleep(WAIT_FOR_RESPOND_TIME);
        // assertTrue(mSolo.getText(
        // mSolo.getString(R.string.alert_delete_single)).isShown());

        // relaunch when show delete dialog
        // relaunchRecordingFileList();

        // check
        // waitActivityAndInitResourceRefs(recordingFileListAM);
        // mSolo.sleep(WAIT_FOR_RESPOND_TIME);
        // assertTrue(mSolo.getText(
        // mSolo.getString(R.string.alert_delete_single)).isShown());

        // click ok button
        // mSolo = new Solo(mInstrumentation, mRecordingFileList);
        // initResourceRefs();
        // mSolo.clickOnButton(mSolo.getString(R.string.ok));
        // mSolo.sleep(WAIT_FOR_RESPOND_TIME);

        // check
        // assertFalse(mDeleteButtonInFileList.isShown());
        // assertTrue(mRecordButtonInFileList.isEnabled());
    }

    private void relaunchRecordingFileList() {
        mSolo.getCurrentActivity().runOnUiThread(new Runnable() {

            @Override
            public void run() {
                mSolo.getCurrentActivity().recreate();

            }
        });
    }

    private boolean isExistInRecordingFileList(String recordFileName) {
        mRecordingFileListView = (ListView) mRecordingFileList
                .findViewById(R.id.recording_file_list_view);
        assertTrue((mRecordingFileListView != null)
                && (mRecordingFileListView.getCount() > 0));
        ListAdapter listAdapter = mRecordingFileListView.getAdapter();
        String recordName;
        View view;
        TextView textView;
        int totalItemCount = mRecordingFileListView.getCount();
        for (int i = 0; i < totalItemCount; i++) {
            view = listAdapter.getView(i, null, mRecordingFileListView);
            textView = (TextView) view.findViewById(R.id.record_file_name);
            recordName = textView.getText().toString();
            if (recordName.equals(recordFileName)) {
                return true;
            }
        }
        return false;
    }

    private void checkIdleUI() {
        assertTrue(mRecordButtonInSoundRecorder.isEnabled());
        assertFalse(mPlayButtonInSoundRecorder.isShown());
        assertFalse(mStopButtonInSoundRecorder.isEnabled());
        assertFalse(mAcceptButtonInSoundRecorder.isShown());
        assertFalse(mDiscardButtonInSoundRecorder.isShown());
    }

    private void checkRecordingUI() {
        assertFalse(mRecordButtonInSoundRecorder.isShown());
        assertFalse(mPlayButtonInSoundRecorder.isShown());
        assertTrue(mStopButtonInSoundRecorder.isEnabled());
        assertFalse(mAcceptButtonInSoundRecorder.isShown());
        assertFalse(mDiscardButtonInSoundRecorder.isShown());

        assertTrue(mStateMessageInSoundRecorder.isShown());
        assertTrue(mStateMessageInSoundRecorder.getText().equals(
                mSolo.getString(R.string.recording)));
    }

    private void checkSavingUI() {
        assertTrue(mRecordButtonInSoundRecorder.isEnabled());
        assertTrue(mPlayButtonInSoundRecorder.isEnabled());
        assertFalse(mStopButtonInSoundRecorder.isEnabled());
        assertTrue(mAcceptButtonInSoundRecorder.isEnabled());
        assertTrue(mDiscardButtonInSoundRecorder.isEnabled());

        assertFalse(mStateMessageInSoundRecorder.isShown());
    }

    private void checkPlayingUI() {
        assertTrue(mRecordButtonInSoundRecorder.isEnabled());
        assertTrue(mPlayButtonInSoundRecorder.isEnabled());
        assertTrue(mStopButtonInSoundRecorder.isEnabled());
        assertTrue(mAcceptButtonInSoundRecorder.isEnabled());
        assertTrue(mDiscardButtonInSoundRecorder.isEnabled());
        assertFalse(mStateMessageInSoundRecorder.isShown());
    }

    private void initResourceRefs() {
        mRecordingFileList = (RecordingFileList) mSolo.getCurrentActivity();
        assertNotNull("activity should be launched successfully",
                mRecordingFileList);

        mRes = mRecordingFileList.getResources();
        assertNotNull(mRes);

        // get res by id
        mRecordingFileListView = (ListView) mRecordingFileList
                .findViewById(R.id.recording_file_list_view);
        mRecordButtonInFileList = (ImageButton) mRecordingFileList
                .findViewById(R.id.recordButton);
        mDeleteButtonInFileList = (ImageButton) mRecordingFileList
                .findViewById(R.id.deleteButton);
        mEmptyView = mRecordingFileList.findViewById(R.id.empty_view);
        mContext = mRecordingFileList.getApplicationContext();
    }
    private void waitActivityAndInitResourceRefs(ActivityMonitor am) {
        mRecordingFileList = (RecordingFileList) am.waitForActivityWithTimeout(WAIT_FOR_ACTIVITY);
        assertNotNull("activity should be launched successfully",
                mRecordingFileList);

        mSolo.sleep(WAIT_FOR_ACTIVITY);
        mRes = mRecordingFileList.getResources();
        assertNotNull(mRes);

        // get res by id
        mRecordingFileListView = (ListView) mRecordingFileList
                .findViewById(R.id.recording_file_list_view);
        mRecordButtonInFileList = (ImageButton) mRecordingFileList
                .findViewById(R.id.recordButton);
        mDeleteButtonInFileList = (ImageButton) mRecordingFileList
                .findViewById(R.id.deleteButton);
        mEmptyView = mRecordingFileList.findViewById(R.id.empty_view);
        mContext = mRecordingFileList.getApplicationContext();
    }
}
