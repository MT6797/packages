package com.nb.mmitest;

import android.hardware.Camera;

import android.content.Context;
import android.util.Log;
import com.nb.mmitest.R;
import com.nb.mmitest.LightTest.CToggleBacklight;

import java.util.Timer;
import java.util.TimerTask;
/*
 * Empty Test use as a default when no test was defined
 */
class FlashLEDTest extends Test{

	TestLayout1 tl;
	String mDisplayString;
	private String TAG = "FlashLED";

	private int mCount=0;
	private int mLastValue=0;

	private android.hardware.Camera mCamera;
	private Camera.Parameters mParameters;

	FlashLEDTest(ID pid, String s) {
		super(pid, s);
		TAG = Test.TAG + TAG;

	}

	FlashLEDTest(ID pid, String s,int timein) {
		super(pid, s, timein, 0);
		TAG = Test.TAG + TAG;
	}


	@Override
	protected void Run() {
		// this function executes the test
		switch (mState) {
		case INIT: 
			Log.e("bll", "flashled INIT");
			try{
				mCamera = Camera.open();
			}catch (Exception e){
				Log.e("MMI Test", "can't open camera ");
				tl = new TestLayout1(mContext,mName,getResource(R.string.flash_led_fail));
				// tl.setAutoTestButtons(true,true);
				mContext.setContentView(tl.ll);
				return;
			}

            mCamera.startPreview();
			mParameters = mCamera.getParameters();

			mTimer = new Timer("BacklightTimer", true);
			mTimerTask = new CToggleBacklight(mId);
			mTimer.schedule(mTimerTask, 0, 300);
			
			// tl = new TestLayout1(mContext, mName, "turning on", 1000, true);
			tl = new TestLayout1(mContext, mName, getResource(R.string.flash_led));
			mContext.setContentView(tl.ll);
            // Add by changmei.chen@tcl.com 2013-01-19 for MMITest spec 2.0.
			mTimeIn.start();
			if (!mTimeIn.isFinished()) 
				tl.hideButtons();
			break;

		case INIT+1: 
			Log.e("bll", "flashled INIT+!");
			tl = new TestLayout1(mContext, mName, getResource(R.string.flash_check));
			mContext.setContentView(tl.ll);
			
			break;

		case END://
		    // Add by changmei.chen@tcl.com 2013-01-19 for stability.
		    if (tl != null)
				tl.setEnabledButtons(false);
			if(null != mTimer)
				mTimer.cancel();
			if(mTimerTask!=null)
			mTimerTask.cancel();
			if(mCamera != null){
				mCamera.release();
			}
			break;
			
		default:
			break;
		}
	}
    
	@Override
	protected void onTimeInFinished() {    
        // Add by changmei.chen@tcl.com 2013-01-19 for MMITest spec 2.0.
		if(tl!=null)
			tl.showButtons();    
	}
	private Timer mTimer = null;
	private TimerTask mTimerTask = null;
	class CToggleBacklight extends TimerTask{
		private int mId;
		private boolean mFlash = true;
		CToggleBacklight(int id){
			mId = id;
		}
				
		@Override
		public void run(){
				String fl="";
				try{
				if(!mFlash)
				{
					fl = "off";
					mParameters.set("flash-mode", fl);
					mCamera.setParameters(mParameters);
					Log.d("bll","off");
					mFlash = true;
				}else
				{
					Log.d("bll","torch");
					fl = "torch";
					mParameters.set("flash-mode", fl);
					
					mCamera.setParameters(mParameters);
					mFlash = false;
				}
				}catch(Exception e)
				{
					Log.d("bll", "flashLed: exception:"+e.toString());
				}
	
		}
	}
}

