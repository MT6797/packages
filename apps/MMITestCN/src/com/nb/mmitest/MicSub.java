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

class MicSubTest extends Test {

	private TestLayout1 tl = null;


	private WXKJRapi remote;

	private boolean mLoopOn = false;

	private MediaPlayer mMediaPlayer = null;

	private AudioManager am;
	

	MicSubTest(ID pid, String s) {
		this(pid, s, 0);
	}

	MicSubTest(ID pid, String s, int timein) {
		super(pid, s, timein, 0);
		/*

		hKey = new KeyHandler() {
			public boolean handleKey(KeyEvent event) {
				return true;
			}

		};
		*/
	}


	@Override
	protected void Run() {
		// this function executes the test
		switch (mState) {
		case INIT:
			Log.d(TAG, "@@@@@@@@@@@@@@@@@@@@@@@Sub mic INIT");

			mTimeIn.start();

			am = (AudioManager) mContext
					.getSystemService(Context.AUDIO_SERVICE);

			
			//20121009 ying.pang for mic test
			//am.setMode(AudioSystem.MODE_IN_CALL);
			//am.setRouting(AudioManager.MODE_IN_CALL, AudioManager.ROUTE_SPEAKER, AudioManager.ROUTE_ALL);
            //am.setWiredHeadsetOn(false);
			//am.setSpeakerphoneOn(true);
			//am.setStreamMute(AudioManager.STREAM_MUSIC, false);
			//am.setStreamMute(AudioManager.STREAM_NOTIFICATION, false);
			//am.setStreamMute(AudioManager.STREAM_RING, false);
			am.setMode(AudioSystem.MODE_NORMAL);
			//mContext.setVolumeControlStream(AudioManager.STREAM_VOICE_CALL);
			  
			//20121026 ying.pang for change volume larger
			int maxVol = am.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL);			
			am.setStreamVolume(AudioManager.STREAM_VOICE_CALL, maxVol, 0);
			am.setStreamVolume(AudioManager.STREAM_MUSIC,
					am.getStreamMaxVolume(AudioManager.STREAM_MUSIC), 0);
					
			SystemClock.sleep(1000);
			
			WXKJClient mClient = null;
			mClient = new WXKJClient();
			if (!mClient.connect()) {
				Log.e(TAG, "Connecting MMI proxy fail!");
				tl = new TestLayout1(mContext, mName, "Fail to connect to proxy");
			    mContext.setContentView(tl.ll);
				break;
			}
			//SystemClock.sleep(100);
			if (!mClient.sendCommand(WXKJClient.MMICMD_SUB_AUDIOLOOP_ON)) {
				Log.e(TAG, "Send command to MMI proxy fail!");
				mClient.disconnect();
			} else {
			
			    //SystemClock.sleep(100);
			    boolean success = mClient.readReply();
			    Log.d(TAG, "readReply() ON " + success);
			    mClient.disconnect();
			    mLoopOn = true;
			}
			
			tl = new TestLayout1(mContext, mName, "loop from sub MIC test",
					"Fail", "Pass");
			mContext.setContentView(tl.ll);
			mTimeIn.start();
			if (!mTimeIn.isFinished()) 
				tl.hideButtons();
			break;

		case END:
			if (tl != null)
				tl.setEnabledButtons(false);

			if (mLoopOn) {
				WXKJClient mOffClient = null;
			    mOffClient = new WXKJClient();
				if (!mOffClient.connect()) {
					Log.e(TAG, "Connecting MMI proxy fail!");
				} else {
				    //SystemClock.sleep(100);				
				    if (!mOffClient.sendCommand(WXKJClient.MMICMD_SUB_AUDIOLOOP_OFF)) {
					    Log.e(TAG, "Send command to MMI proxy fail!");
					    mOffClient.disconnect();
				    } else {
				
				        boolean success2 = mOffClient.readReply();
				        Log.d(TAG, "readReply() OFF " +success2);
				        mOffClient.disconnect();

					    mOffClient = null;			
				        mLoopOn = false;
				    }
				}
			} 
			
			//am.setSpeakerphoneOn(false);
			Log.d(TAG, "@@@@@@@@@@@@@@@@@@@@@@@Sub mic END1");
			//am.setSpeakerphoneOn(false);
			
			/*am.setMode(AudioSystem.MODE_NORMAL);			
			am.setStreamMute(AudioManager.STREAM_MUSIC, false);
			am.setStreamMute(AudioManager.STREAM_NOTIFICATION, false);
			am.setStreamMute(AudioManager.STREAM_RING, false);*/
			Log.d(TAG, "@@@@@@@@@@@@@@@@@@@@@@@Sub mic END");
			SystemClock.sleep(100);
			
			break;
		default:
		}
	}

	@Override
	protected void onTimeInFinished() {
	    if (tl != null)
			tl.showButtons();
	}

}
