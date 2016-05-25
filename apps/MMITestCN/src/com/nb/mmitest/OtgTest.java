package com.nb.mmitest;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Timer;
import java.util.TimerTask;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import com.nb.mmitest.MemorycardTest.SdPlugReceiver;
import com.nb.mmitest.Test.ID;
import com.nb.mmitest.USBTest.PlugStateReceiever;

import android.os.Environment;
import android.os.SystemProperties;

import android.os.BatteryManager;
import android.os.storage.StorageManager;
import android.util.Log;
public class OtgTest extends Test {
	String mDisplayString;
	TestLayout1 tl;
	SdPlugReceiver mMediaActionReceiver = new SdPlugReceiver();
	private Timer mTimer = null;
	private String TAG = "OtgTest";
	private boolean regFlag = false;
	StorageManager storagemanager = null;
	String state;
	OtgTest(ID pid, String s) {
		super(pid, s);
		TAG = Test.TAG + TAG;

	}

	OtgTest(ID pid, String s, int timein) {
		super(pid, s, timein, 0);
		TAG = Test.TAG + TAG;

	}

	@Override
	protected void Run() {
		// this function executes the test
		switch (mState) {
		case INIT:

			IntentFilter intentFilter = new IntentFilter(
					Intent.ACTION_MEDIA_MOUNTED);
			intentFilter.addAction(Intent.ACTION_MEDIA_BAD_REMOVAL);
			intentFilter.addAction(Intent.ACTION_MEDIA_REMOVED);
			intentFilter.addAction(Intent.ACTION_MEDIA_EJECT);
			intentFilter.addDataScheme("file");
			mContext.registerReceiver(mMediaActionReceiver, intentFilter);
			 Log.d(TAG, "==registerReceiver=>OtgTest is start");
			tl = new TestLayout1(mContext, mName,
					getResource(R.string.otg_display));
			mContext.setContentView(tl.ll);		
			
		   String mountPoint = "/mnt/media_rw/usbotg";
		   String mountPoint1 = "/storage/usbotg";
		   String mountPoint2 = "/mnt/usbotg";
			
			if (storagemanager == null)
			 storagemanager=(StorageManager)mContext.getSystemService(Context.STORAGE_SERVICE);
							
			state = storagemanager.getVolumeState(mountPoint);
			 Log.d(TAG, "bll====>mountPoint  state: "+state +"  "+storagemanager.getVolumeState(mountPoint1)+" "+storagemanager.getVolumeState(mountPoint2));
			if (storagemanager.getVolumeState(mountPoint).equals(
					Environment.MEDIA_MOUNTED)||storagemanager.getVolumeState(mountPoint1).equals(
							Environment.MEDIA_MOUNTED)||storagemanager.getVolumeState(mountPoint2).equals(
									Environment.MEDIA_MOUNTED)) {
				tl = new TestLayout1(mContext, mName,
						getResource(R.string.otg_pass));
				mContext.setContentView(tl.ll);	
				 Log.d(TAG, "bll====>otg state: "+state);
			}
			break;

		case END://
			  mContext.unregisterReceiver(mMediaActionReceiver);
			   Log.d(TAG, "unregisterReceiver===>");
			break;

		default:
			break;
		}
	}

	@Override
	protected void onTimeInFinished() {

	}
   
	@Override
	public void Exit() {
		super.Exit();
	}
	
	class SdPlugReceiver extends BroadcastReceiver {

		private String mVolume;

		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			Log.d(TAG, "received :" + action);
			mVolume = (intent.getDataString());
			if (action.equals(Intent.ACTION_MEDIA_MOUNTED)) {
				
				if (mVolume.matches(".*usbotg")) {
					Log.d(TAG, "usbotg inserted");
					tl = new TestLayout1(mContext, mName,
							getResource(R.string.otg_pass));
					mContext.setContentView(tl.ll);	
				} else {
					Log.d(TAG, "unknown media inserted :" + mVolume);
				}
			} else if (action.equals(Intent.ACTION_MEDIA_EJECT)
					|| action.equals(Intent.ACTION_MEDIA_BAD_REMOVAL)
					|| action.equals(Intent.ACTION_MEDIA_REMOVED)) {
				   Log.d(TAG, intent.getDataString() + " removed");
				   if (mVolume.matches(".*usbotg")) {
						Log.d(TAG, "usbotg removed");
						tl = new TestLayout1(mContext, mName,
								getResource(R.string.otg_out));
						mContext.setContentView(tl.ll);	
			}

		}
	}
	}
}
