package com.nb.mmitest;

import static android.provider.Settings.System.SCREEN_OFF_TIMEOUT;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.AssetFileDescriptor;
import android.content.res.Resources;
import android.content.pm.ActivityInfo;
import android.graphics.*;
import android.graphics.Bitmap.Config;
//import android.hardware.Camera;
import android.media.AudioManager;
import android.media.AudioSystem;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.IPowerManager;
import android.os.PowerManager;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.Vibrator;
import android.os.BatteryManager;
import android.os.SystemClock;
import android.os.Looper;
import android.provider.Settings;


import com.android.internal.telephony.RILConstants;
import com.android.internal.telephony.TelephonyIntents;
import com.android.internal.telephony.TelephonyProperties;
import android.telephony.TelephonyManager;
import android.telephony.PhoneStateListener;

import android.text.TextUtils;
import android.text.method.MetaKeyKeyListener;
import android.util.Log;
import android.view.Gravity;
import android.util.SparseArray;

import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import java.io.File;
import java.io.FileWriter;
import java.io.FileReader;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.BufferedReader;
import java.util.Arrays;
import java.util.List;
import java.util.Iterator;

import android.view.Gravity;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.ImageView;
import android.widget.Button;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.LinearLayout; //import android.widget.LinearLayout.LayoutParams;
import android.widget.FrameLayout;
import android.view.ViewGroup.LayoutParams;
//import android.view.ViewGroup;
import java.util.ArrayList;
import java.util.Hashtable;
//import java.util.Map;
//import java.util.List;
//import java.io.IOException;
import android.widget.Toast;
import android.os.ServiceManager;

//import com.android.server.*;
/*add by stephen*/
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

//for wifi
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiManager;
import android.content.Intent;
import android.content.IntentFilter;
/*end add*/

import android.os.Vibrator;
import android.hardware.Camera;

import android.app.KeyguardManager;
import android.app.KeyguardManager.KeyguardLock;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorListener;
import android.hardware.SensorManager;

import android.location.*;

import android.content.res.Configuration;

import com.android.fmradio.FmNative;
import com.nb.mmitest.R;
/*
 * Battery temperature
 * reads and display the information in battery system file
 * /sys/devices/platform/msm-battery/power_supply/battery/temp
 */
class BatteryTempTest extends Test {

	BufferedReader mBattTempReader;
	String TAG = Test.TAG + "BatteryTempTest";
	private IntentFilter mIntentFilter;
	private String mBattTempValue;
	private String mBattContValue;
	private String mBattVoltageValue;
	private int mBattTemp;

	final private String SYS_BATTERY_PATH = "/sys/devices/platform/msm-battery/power_supply/battery/";

	BatteryTempTest(ID pid, String s) {
		super(pid, s);
	}

	/**
	 * Format a number of tenths-units as a decimal string without using a
	 * conversion to float. E.g. 347 -> "34.7"
	 */
	private final String tenthsToFixedString(int x) {
		int tens = x / 10;
		return Integer.toString(tens) + "." + (x - 10 * tens);
	}
	
	private final String milliToFixedString(int x) {
		int tens = x / 1000;
		return Integer.toString(tens) + "." + (x - 1000 * tens);
	}

	private final BroadcastReceiver mBatteryTmpIntentReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (action.equals(Intent.ACTION_BATTERY_CHANGED)) {
				mBattTemp = intent.getIntExtra("temperature", 0);
				mBattTempValue = tenthsToFixedString(intent.getIntExtra(
						"temperature", 0));
				mBattContValue = "" + intent.getIntExtra("level", 0);
				mBattVoltageValue =Integer.toString(intent.getIntExtra("voltage", 0));
				//smBattVoltageValue = milliToFixedString(intent.getIntExtra("voltage", 0));
				Run();
			}
		}
	};

	private synchronized String getBattTemp() {
		String result = null;
		try {
			mBattTempReader = new BufferedReader(new FileReader(
					SYS_BATTERY_PATH + "temp"), 8);
			result = mBattTempReader.readLine();
			Log.i(TAG, "AC online : " + result);
		} catch (IOException e) {
			Log.e(TAG, " plug status file can't be accessed " + e);
		} finally {
			try {
				if (mBattTempReader != null)
					mBattTempReader.close();
			} catch (IOException excep) {
				Log.e(TAG, "can't close file" + excep);
			}
		}
		return result;// can be null
	}

	@Override
	protected void Run() {
		// this function executes the test
		switch (mState) {
		case INIT: // init the test, shows the first screen
			mIntentFilter = new IntentFilter();
			mIntentFilter.addAction(Intent.ACTION_BATTERY_CHANGED);
			mContext.registerReceiver(mBatteryTmpIntentReceiver, mIntentFilter);
			mState++;
			break;

		case INIT + 1:
			String s;
			// mBattTempValue = getBattTemp();

			if (mBattTempValue == null) {
				s = "can't read battery temperature";
			} else {
				s = getResource(R.string.temperature_tip)+ mBattTempValue+"\n\n"
						//+ getResource(R.string.battery_content_tip) + mBattContValue + "%\n\n"
						+ getResource(R.string.battery_vol_tip) + mBattVoltageValue + "mV";
			}

			TestLayout1 tl = new TestLayout1(mContext, mName, s, getResource(R.string.fail), getResource(R.string.pass));
			if(MMITest.mgMode == MMITest.AUTO_MODE){
			 //  tl.setEnabledButtons(false);
			}
			//TestLayout1 	tl = new TestLayout1(mContext, mName, s, "Fail","Pass", mLeftButton, mRightButton);	
			if (200 > mBattTemp || mBattTemp > 500) {
			//	tl.setEnabledButtons(false, tl.brsk);
			}
			mContext.setContentView(tl.ll);
			mState = END;
			break;
		default:
			break;

		}

		/* only in auto test mode, do autorun */
		if(mState == END){
			if(MMITest.mgMode == MMITest.AUTO_MODE){
			//AutoRun();
			}
		}
	}
	
    private View.OnClickListener mRightButton = new View.OnClickListener() {
    	public void onClick(View v) {
    		ExecuteTest.currentTest.Result=Test.PASSED;
		onExit();
    	}
    };

    private View.OnClickListener mLeftButton = new View.OnClickListener() {
    	public void onClick(View v) {
    		ExecuteTest.currentTest.Result=Test.FAILED;
		onExit();
    	}
    };

	protected void onExit() {
       	try{ 
		     if (mBatteryTmpIntentReceiver != null){   
    	    	          mContext.unregisterReceiver(mBatteryTmpIntentReceiver);
			   if(mName.equals("Battery temp")||mName.equals("电池温度测试")){
			   	Log.d(TAG, "onExit got the mName = "+ mName+ " --kywang !!!");
			   	ExecuteTest.currentTest.Exit();
		   	   }
		     	}
    	       }catch ( Exception e) {
    		     Log.e(TAG, "already unregistered"+mName);
    	       }
       }
	
	@Override
	protected void AutoRunCallback(){
		/* impl logic in desired page */
		switch(mState){
		/* at battery temperature display state */
		case END:
			/* two cases: 1/can not read temp/; 2/temp is bad. */
			if(mBattTempValue == null || mBattTemp < 200 || mBattTemp > 500){
				Result = Test.FAILED;
			}else{
				Result = Test.PASSED;
			}
			onExit();						
			break;
		default:
			Log.i("BatteryTempTest", "default branch reached");
			break;
		}
	}
}
