/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.nb.mmitest;

import java.io.File;
import java.util.Timer;
import java.util.TimerTask;

import com.nb.mmitest.HallTest.HallBroadcastReceiver;
import com.nb.mmitest.Test.ID;

import android.app.Activity;
import android.content.Context;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;

import com.silead.fp.utils.*;
import egistec.ets.api.FingerprintReceiver;
import egistec.ets.api.FpExtraLib;
import egistec.ets.api.FpResDef;
/**
 * This example shows how to use choice mode on a list. This list is in
 * CHOICE_MODE_MULTIPLE mode, which means the items behave like checkboxes.
 */
public class FpTest extends Test implements FingerprintEnrollSidecar.Listener{
	private final static String TAG = "FpTest";
	private FpControllerNative mControllerNative;
	private FpExtraLib mFpEtsManager;
	TestLayout1 tl;
	String mDisplayString;
	String mFpCheckString="";
	Timer mTimer=null;
	private int count = 3;
	private boolean checkFlag = true;
	private FingerprintEnrollSidecar mSidecar;
	Handler myHandler = new Handler() {
		public void handleMessage(Message msg) {
			count--;
			mDisplayString = getResource(R.string.Fp_display);
			if (count < 0)
				count = 0;
			if (count == 0 && checkFlag) {
				tl.brsk.setEnabled(true);
				mDisplayString += getResource(R.string.Fp_ok);
				mSidecar.onStop();
				mSidecar.setListener(null);
			} else {
				mDisplayString += getResource(R.string.Fp_count);
				mDisplayString += Integer.toString(count);
			}
			File file =new File("/dev/esfp0");
			if(file.exists()){
				mDisplayString +="\n\n\n";
				mDisplayString += mFpCheckString;
			}
			tl.getBody().setText(mDisplayString);
			super.handleMessage(msg);
		}
	};

	FpTest(ID pid, String s) {
		super(pid, s);	
		count = 3;

	}

	FpTest(ID pid, String s, int timein) {
		super(pid, s, timein, 0);
	}

	@Override
	protected void Run() {
		// this function executes the test
		switch (mState) {
		case INIT:
					
			mDisplayString = getResource(R.string.Fp_display);
			if (count == 0) {
				tl.brsk.setEnabled(true);
				mDisplayString += getResource(R.string.Fp_ok);
			} else {

				mDisplayString += getResource(R.string.Fp_count);
				mDisplayString += Integer.toString(count);
				
			}
			File file =new File("/dev/esfp0");
			if(file.exists())
			{
				mDisplayString +="\n\n\n";
				if(-1 == testEtsFp())
				{
					mFpCheckString = getResource(R.string.fp_check_fail);
					checkFlag=false;
				}else
				{
					mFpCheckString = getResource(R.string.fp_check_ok);
					checkFlag=true;
				}
				mDisplayString += mFpCheckString;
			}
			mSidecar = new FingerprintEnrollSidecar(mContext);
			mSidecar.setListener(this);
			mSidecar.onStart();
			tl = new TestLayout1(mContext, mName, mDisplayString);
			if(count > 0)
				tl.brsk.setEnabled(false);
			mContext.setContentView(tl.ll);
			
			break;
		case INIT + 1:
		case INIT + 2:
		case INIT + 3:
			myHandler.sendEmptyMessage(0);
			break;

		case END://
			if (mTimer != null)
			{
				mTimer.cancel();
				mTimer = null;
			}
			count =3;

			break;
		default:
			break;
		}
	}

	@Override
	protected void onTimeInFinished() {
		// mTimer.cancel();
		// cancelIdentify();
	}

	private void vibrate() {
		android.os.Vibrator vib = (android.os.Vibrator) mContext
				.getSystemService(Context.VIBRATOR_SERVICE);
		vib.vibrate(250);

	}

	@Override
	public void Exit() {
		//setBacklightNormalState();
		//setBacklight(PowerManager.BRIGHTNESS_OFF);
		if(null != mTimer)
		{
			mTimer.cancel();
			mTimer = null;
		}
		count = 3;
		mSidecar.onStop();
		mSidecar.setListener(null);
		super.Exit();
	}
	
    @Override
    public void onEnrollmentHelp(CharSequence helpString) {
    	Log.d(TAG, "onEnrollmentHelp fp steps: "+mSidecar.getEnrollmentSteps()+"  remaining: "+mSidecar.getEnrollmentRemaining());
    }

    @Override
    public void onEnrollmentError(CharSequence errString) {
    	Log.d(TAG, "onEnrollmentError fp steps: "+mSidecar.getEnrollmentSteps()+"  remaining: "+mSidecar.getEnrollmentRemaining());   		
    }

    @Override
    public void onEnrollmentProgressChange(int steps, int remaining) {
    	Log.d(TAG, "onEnrollmentProgressChange fp steps: "+mSidecar.getEnrollmentSteps()+"  remaining: "+mSidecar.getEnrollmentRemaining());
    	vibrate();
    	myHandler.sendEmptyMessage(0);
    }
    
    
	public int testEtsFp()
	{
		mFpEtsManager = FpExtraLib.getInstance();

		if(mFpEtsManager == null){
			Log.d(TAG,"mFpEtsManager is null\n");
			return -1;
		}
		
		int ret = mFpEtsManager.sensorProb();
		Log.i(TAG, "Test1 sensor_prob(); ret="+ret);
//		mDisplayString+="Test1:\n sensor_prob(); ret="+ret+"\n";
		if (ret != FpResDef.RESULT_OK) {
			return -1;
		}
		
		ret = mFpEtsManager.sensorTest(1000);
		Log.i(TAG, "Test2 scriptID="+1000+" ; ret="+ret);
//		mDisplayString+="Test2:\n scriptID="+1000+" ; ret="+ret+"\n";
		if (ret != FpResDef.RESULT_OK) {
			return -1;
		}

		ret = mFpEtsManager.sensorTest(1001);
		Log.i(TAG, "Test3 scriptID="+1001+" ; ret="+ret);
	//	mDisplayString+="Test3:\n scriptID="+1001+" ; ret="+ret+"\n";
		if (ret != FpResDef.RESULT_OK) {
			return -1;
		}

		ret = mFpEtsManager.sensorTest(1002);
		Log.i(TAG, "Test4: scriptID="+1002+" ; ret="+ret);
	//	mDisplayString+="Test4:\n scriptID="+1002+" ; ret="+ret+"\n";
		
		if (ret != FpResDef.RESULT_OK) {

			return -1;
		}
		return 0;
	}
}
