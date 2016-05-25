
package com.nb.mmitest;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import android.os.BatteryManager;
import android.util.Log;

import android.view.View;
import com.nb.mmitest.R;
/*
 * Usb cable test
 * We use native Android API here, also we can read the standard Linux sysfs node i.e. 
 * /sys/devices/platform/msm-battery/power_supply/usb/online
 */
class USBTest extends Test {  
	
	private String USB_ONLINE_PATH = "/sys/class/power_supply/usb/online";
	private BufferedReader mUsbPlugStatusbr;
	private String mUsbPlugStatus;
	private String ONLINE	= "1";
	private String OFFLINE 	= "0";

	private boolean mRegistered = false;
	private String TAG = "USBTest";
	
	IntentFilter inf;
	private final PlugStateReceiever USBRcv = new PlugStateReceiever();
	TestLayout1 tl;
	
	private boolean mAC = false; 
	private boolean mUSB = false; 
	private boolean mPopFlag = false;//in user version, there is a pop-up.

	private final int TEST_IN_HINT 	= INIT;
	private final int TEST_IN_OK 	= INIT + 1;
	private final int TEST_OUT_HINT 	= INIT + 2;
	private final int TEST_OUT_OK 	= INIT + 3;

	private synchronized void getCableStatus(){
		try{
			mUsbPlugStatusbr = new BufferedReader(new FileReader(USB_ONLINE_PATH),8);
			mUsbPlugStatus = mUsbPlugStatusbr.readLine();
			Log.i(TAG, "USB online : "+ mUsbPlugStatus );
		}catch(IOException e){
			Log.e(TAG, USB_ONLINE_PATH +" can't be read "+e);
		}finally{
			try{
				if(mUsbPlugStatusbr != null)
					mUsbPlugStatusbr.close();
			}catch(IOException e){
				Log.e(TAG,USB_ONLINE_PATH +" can't be closed "+e);
			}
		}
	}

	USBTest(ID pid, String s) {
		super(pid, s);
		TAG = super.TAG+TAG;
	}

	@Override
	protected void Run() {
		switch (mState) {
		case INIT:
			inf = new IntentFilter();
			inf.addAction(Intent.ACTION_UMS_DISCONNECTED);
			inf.addAction(Intent.ACTION_UMS_CONNECTED);
			inf.addAction(Intent.ACTION_BATTERY_CHANGED);

			if(!mRegistered){
				mContext.registerReceiver(USBRcv, inf);
				mRegistered=true;
			}

			//getCableStatus();
			if(mPopFlag){
				mPopFlag = false;

				if(mAC || mUSB ){ //already ok.
				//if( mUSB ){ //already ok.
					mState = TEST_IN_OK;
					Run();
				}
			}else{
				mAC = false;
				mUSB = false;
			}
			ShowStatus(mAC, mUSB);
		//	ShowStatus(true, mUSB);
			
			SetTimer(60000, new CallBack() {
				public void c() {
					mState = TIMEOUT;
					Run();
				}
			});
			break;

		case TEST_IN_OK:
			//StopTimer();
			/*String in_ok = "Please insert USB cable.\n\n" + 
				"Charger : OK\n" +
				"USB : OK\n";
				*/
			String in_ok = getResource(R.string.usb_tip)+getResource(R.string.usb_ok);//"Please insert USB cable.\n\n" + "USB : OK\n"+ "Charge : OK\n";
			
			tl = new TestLayout1(mContext, mName, in_ok, getResource(R.string.fail),getResource(R.string.pass));
			mContext.setContentView(tl.ll);
			SetTimer(1000, new CallBack() {
				public void c() {
					mState = TEST_OUT_HINT;
					Run();
				}
			});
			break;

		case TEST_OUT_HINT:
			tl = new TestLayout1(mContext, mName, getResource(R.string.usb_remove));
			tl.setEnabledButtons(false, tl.brsk);
			mContext.setContentView(tl.ll);
			
			SetTimer(60000, new CallBack() {
				public void c() {
					mState = TIMEOUT;
					Run();
				}
			});
			break;
		
		case TEST_OUT_OK:
			StopTimer();
			tl = new TestLayout1(mContext, mName, getResource(R.string.ok), getResource(R.string.fail),getResource(R.string.pass));
			mContext.setContentView(tl.ll);
			
			//mLastPageOfMultiPage = true;
			ExecuteTest.currentTest.Result=Test.PASSED;
			ExecuteTest.currentTest.Exit();
			
			
			SetTimer(1000, new CallBack() {
				public void c() {
					mState = TIMEOUT;
					Run();
				}
			});

			
			if(mRegistered)
				mContext.unregisterReceiver(USBRcv);
			mRegistered = false;
			break;

		case TIMEOUT:
			tl = new TestLayout1(mContext, mName, getResource(R.string.usb_timeout)); 
			tl.setEnabledButtons(false, tl.brsk);
			mContext.setContentView(tl.ll);
			break;

		case END:
		default:
			StopTimer();
			try{
				if(mRegistered)
					mContext.unregisterReceiver(USBRcv);
				mRegistered=false;
			}catch (Exception e) {
				Log.e(TAG, "unregister failed " + e);
			}
			break;
		}
	}

	private int ShowStatus(boolean ac, boolean usb){
		String ins = getResource(R.string.usb_tip);

		if(ac){
			ins = ins + getResource(R.string.ac_ok);
		}else{
			ins = ins + getResource(R.string.ac_no);
		}

		if(usb){
			ins = ins + getResource(R.string.usb_ok);
		}else{
			ins = ins + getResource(R.string.usb_no);
		}

		tl = new TestLayout1(mContext, mName, ins);
		tl.setEnabledButtons(false, tl.brsk);
		mContext.setContentView(tl.ll);

		return 0;
	}

	public class PlugStateReceiever extends BroadcastReceiver{
		
		private final int PLUGGED 	= 1;
		private final int UNPLUGGED 	= 2;
		private final int AC_PLUGGED 	= 4;
		private final int USB_PLUGGED 	= 8;
		private final int UNKNOWN 	= 16;

		@Override
		public void onReceive(Context context, Intent intent) {
			int mPlugged = UNKNOWN;

			String a = intent.getAction();
			Log.w(TAG, "intent received : " + a);

		/* 	
			//FIXME: sometimes, this will not work.
			if(a.equals(Intent.ACTION_UMS_CONNECTED)){
				mPlugged = PLUGGED;
			}else if(a.equals(Intent.ACTION_UMS_DISCONNECTED)){
				mPlugged = UNPLUGGED;
			}
		*/

			if (a.equals(Intent.ACTION_BATTERY_CHANGED)){
				int mBatteryPlug = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
				Log.w(TAG, "intent received : " + mBatteryPlug);
				if (mBatteryPlug == -1)
					return;
				
				if(mBatteryPlug == BatteryManager.BATTERY_PLUGGED_USB){
					mPlugged = PLUGGED;
					mUSB = true;
				}else if(mBatteryPlug == BatteryManager.BATTERY_PLUGGED_AC){
					mPlugged = PLUGGED;
					mAC = true;
				}else{
					mPlugged = UNPLUGGED;
				}
			}
			
			if(mPlugged == PLUGGED){
				if(mState == TEST_IN_HINT){
					StopTimer();
					ShowStatus(mAC, mUSB);
				//	ShowStatus(true, mUSB);

					if(mPopFlag){
						return;//don't goto next step during onPause()
					}
					
					if(mUSB || mAC){
					//if(mUSB){
						mState = TEST_IN_OK;
						Run();
					}	
				}//in hint

			}else if(mPlugged == UNPLUGGED){
				if (mState == TEST_OUT_HINT){
					mState = TEST_OUT_OK;
					Run();
				}else if(mState == TEST_IN_HINT){
					//jump = false;
				}
			}
		}//onR
	}

	/*@Override
	public void Pause() {
		Log.w(TAG, "++++++Pause: " + mState);
		if(mState != END){
			StopTimer();
			mPopFlag = true; //in user version, there is a pop-up.
		}else{
			mPopFlag = false;
		}
	}*/
	
}
