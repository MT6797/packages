package com.android.soundrecorder.tests;

import android.app.Activity;
import android.os.Bundle;
import android.test.LaunchPerformanceBase;

public class RecordingFileListLaunchPerformance extends LaunchPerformanceBase {
    public void onCreate(Bundle arguments) {
        super.onCreate(arguments);
        mIntent.setClassName(getTargetContext(),
                "com.android.soundrecorder.RecordingFileList");
        start();
    }

    public void onStart() {
        super.onStart();
        LaunchApp();
        finish(Activity.RESULT_OK, mResults);
    }
}