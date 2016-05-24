package com.android.soundrecorder.tests;

import java.io.File;
import java.io.IOException;

import android.content.Context;
import android.content.Intent;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.storage.StorageManager;
import android.test.InstrumentationTestCase;
import android.util.Log;

import com.mediatek.media.MediaRecorderEx;
import com.mediatek.storage.StorageManagerEx;

import com.android.soundrecorder.RecordParamsSetting.RecordParams;
import com.android.soundrecorder.Recorder;
import com.android.soundrecorder.tests.annotation.*;

public class SoundRecorderTestByAnnotation extends InstrumentationTestCase {

    private static final String TAG = "SoundRecorderTestByAnnotation";
    private static final String RECORD_FOLDER = "RecordingTest";
    private static final int HIGH_ENCODER = 1;
    private static final int HIGH_AUDIO_CHANNELS = 1;
    private static final int HIGH_ENCODE_BITRATE = 12200;
    private static final int HIGH_SAMPLE_RATE = 8000;
    private static final int HIGH_OUTPUT_FORMAT = 3;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    @FwkAnnotation
    public void testcase001_testFwkRecording() {

        HandlerThread thread = new HandlerThread("testRecording");
        thread.start();
        Handler threadHandler = new Handler(thread.getLooper()) {
            public MediaRecorder recorder = null;
            @Override
            public void handleMessage(Message msg) {
                if (msg.what == 0) {
                    recorder = new MediaRecorder();
                    assert (recorder != null);

                    File dstFile = createRecordingFile("test01.3gpp");
                    assert (dstFile != null && dstFile.exists());
                    recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
                    recorder.setOutputFormat(HIGH_OUTPUT_FORMAT);
                    recorder.setOutputFile(dstFile.getAbsolutePath());
                    recorder.setAudioEncoder(HIGH_ENCODER);
                    recorder.setAudioChannels(HIGH_AUDIO_CHANNELS);
                    recorder.setAudioEncodingBitRate(HIGH_ENCODE_BITRATE);
                    recorder.setAudioSamplingRate(HIGH_SAMPLE_RATE);
                    assert (dstFile.length() == 0);
                    try {
                        recorder.prepare();
                    } catch (IOException e) {
                        Log.d(TAG, "testRecording " + e.getMessage());
                    }
                    recorder.start();
                } else if (msg.what == 1) {
                    assert (recorder != null);
                    recorder.stop();
                    recorder.release();
                    recorder = null;
                }
            }
        };

        threadHandler.sendEmptyMessage(0);
        waitFor(5000);
        threadHandler.sendEmptyMessage(1);
        waitFor(1000);
        thread = null;
        threadHandler = null;
    }

    private Recorder.RecorderListener mRecorderListener = new
        Recorder.RecorderListener() {
            @Override
            public void onStateChanged(Recorder recorder, int stateCode) {
            }
            @Override
            public void onError(Recorder recorder, int errorCode) {
                // TODO Auto-generated method stub
            }
    };

    @InternalApiAnnotation
    public void testcase002_testInternalRecording() {
        StorageManager mStorageManager = (StorageManager) getInstrumentation().
                getContext().getSystemService(Context.STORAGE_SERVICE);
        Recorder internalRecorder = new Recorder(mStorageManager, mRecorderListener);
        RecordParams param = new RecordParams();
        param.mAudioChannels = this.HIGH_AUDIO_CHANNELS;
        param.mAudioEffect = null;
        param.mAudioEncoder = this.HIGH_ENCODER;
        param.mAudioEncodingBitRate = this.HIGH_ENCODE_BITRATE;
        param.mAudioSamplingRate = this.HIGH_SAMPLE_RATE;
        param.mOutputFormat = this.HIGH_OUTPUT_FORMAT;
        param.mHDRecordMode = MediaRecorderEx.HDRecordMode.NORMAL;
        param.mAudioEffect = new boolean[]{false, false, false};
        Context context = getInstrumentation().getContext();
        boolean res = internalRecorder.startRecording(context, param, 0);
        assertTrue(res);
        waitFor(6000);
        internalRecorder.stopRecording();
    }

    @ExternalApiAnnotation
    public void testcase003_testRecordingFromExternalApp() {
        Context ctx = getInstrumentation().getContext();
        Intent intent = new Intent();
        //intent.addFlags(Activity.)
        //intent.setClass(ctx, SoundRecorder.class);
        //ctx.startActivity(intent);
    }

    private void waitFor(int msec) {
        try {
            Thread.sleep(msec);
        } catch (InterruptedException e) {
            Log.d(TAG, "testRecording intterrupted exception " + e.getMessage());
        }
    }
    private File createRecordingFile(String fileName) {
        File sampleDir = null;
        sampleDir = new File(StorageManagerEx.getDefaultPath());
        String sampleDirPath = sampleDir.getAbsolutePath() + File.separator
                + RECORD_FOLDER;
        sampleDir = new File(sampleDirPath);
        if (!sampleDir.exists()) {
            sampleDir.mkdirs();
        }
        File dstFile = null;
        try {
            dstFile = new File(sampleDir.getAbsolutePath()
                    + File.separator + fileName);
            if (!dstFile.exists()) {
                dstFile.createNewFile();
            } else {
                dstFile.delete();
                dstFile.createNewFile();
            }
        } catch (IOException e) {
            Log.d(TAG, "createRecordingFile " + e.getMessage());
            return dstFile;
        }
        return dstFile;
    }
}
