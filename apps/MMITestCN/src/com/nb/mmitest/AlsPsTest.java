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

class AlsPsTest extends Test implements SensorEventListener {
	private TestLayout1 tl;
	private SensorManager mSensorManager;
	private Sensor mProximitySensor;
	private int far2neer = 0;
	private int near2far = 1;
	String mDisplayPSString = "";
	private int temp = 0;

	AlsPsTest(ID pid, String s) {
		super(pid, s);
	}

	protected void Run() {
		switch (mState) {
		case INIT: // init the test, shows the first screen
			tl = new TestLayout1(mContext, mName, "\nOpening.....", getResource(R.string.fail), getResource(R.string.ok));
			//tl.setEnabledButtons(false);
			mContext.setContentView(tl.ll);
			SetTimer(500, new CallBack() {
				public void c() {
					mState++;
					Run();
				}
			});
			tl.setEnabledButtons(false, tl.brsk);
			break;
		case INIT + 1:
			if (mSensorManager == null)
				mSensorManager = (SensorManager) mContext
						.getSystemService(Context.SENSOR_SERVICE);
			mProximitySensor = mSensorManager
					.getDefaultSensor(Sensor.TYPE_PROXIMITY);
			if (mProximitySensor != null) {
				StringBuilder ps_info = new StringBuilder();
				StringBuilder ps1_info = new StringBuilder();
				StringBuilder ps2_info = new StringBuilder();
				ps_info.append(getResource(R.string.proximity));
				ps1_info.append(getResource(R.string.distance) + Integer.toString(near2far));
				ps1_info.append("cm");
				mDisplayPSString = ps_info + "\n" + ps1_info + "\n" + ps2_info;
				temp = 1;
				tl = new TestLayout1(mContext, mName, mDisplayPSString, getResource(R.string.fail),
						getResource(R.string.ok));
				Log.i(TAG,
						"ProximitySensor opened : "
								+ mProximitySensor.getName());
				if (!mSensorManager.registerListener(this, mProximitySensor,
						SensorManager.SENSOR_DELAY_NORMAL)) {
					Log.e(TAG, "register listener for ProximitySensor "
							+ mProximitySensor.getName() + " failed");
				}
				Log.d(TAG, "INIT+1:near2far " + near2far);
				Log.d(TAG, "INIT+1:far2near " + far2neer);

			}

			else {
				tl = new TestLayout1(mContext, mName,
						"ProximitySensor not detected");
			}
			//if ((near2far == 0) && (far2neer == 0))
			//	tl.setEnabledButtons(false, tl.brsk);
			mContext.setContentView(tl.ll);
			mState++;
			tl.setEnabledButtons(false, tl.brsk);
			break;
		case END:
			mSensorManager.unregisterListener(this, mProximitySensor);
			far2neer = 0;
			near2far = 1;
			temp = 0;
			Log.d(TAG, "ProximitySensor listener unregistered");
			break;

		/* enable layout buttons only after TimeIn elapsed */
		// if(!mTimeIn.isFinished())
		// tl.setEnabledButtons(false);

		default:
			break;
		}

	}

	public void onSensorChanged(SensorEvent event) {
		float distance;
		Log.d(TAG, "onSensorChanged: (" + event.values[0] + ")");
	//	if (temp == 1) {
	//		temp++;
	//	} else {
			distance = event.values[0];
			if (distance == 0)
				far2neer++;
			if (distance == 1)
				near2far++;
	//	}
			near2far = (int)distance;
			far2neer=0;
		StringBuilder ps_info = new StringBuilder();
		StringBuilder ps1_info = new StringBuilder();
		StringBuilder ps2_info = new StringBuilder();
		ps_info.append(getResource(R.string.proximity));
		ps1_info.append(getResource(R.string.distance) + Integer.toString(near2far));
		ps1_info.append("cm");
		//ps2_info.append(getResource(R.string.proximity_far_near) + Integer.toString(far2neer));
		mDisplayPSString = ps_info + "\n" + ps1_info + "\n" + ps2_info;
		//tl = new TestLayout1(mContext, mName, mDisplayPSString, getResource(R.string.fail), getResource(R.string.ok));
		tl.getBody().setText(mDisplayPSString);
		Log.d(TAG, "onSensorChanged: mDisplayPSString = " + mDisplayPSString);
		//if ((near2far == 0) && (far2neer == 0))
		//	tl.setEnabledButtons(false, tl.brsk);
		//mContext.setContentView(tl.ll);
		// Run();
		if(0 == near2far)
		{
			Log.d(TAG, "set buttons enable true");
			tl.setEnabledButtons(true, tl.brsk);
		}
	}

	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub
		// Log.i(TAG,"sensor"+sensor.getName()+" accuracy changed :"+accuracy);
	}

}
