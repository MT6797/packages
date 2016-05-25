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

import com.nb.mmitest.BuildConfig;
import com.nb.mmitest.R;
import com.android.fmradio.FmNative;

class HeadInTest extends Test {
	int timeout = 0;
	private final HeadInsertBroadcastReceiver mHeadsetStatus = new HeadInsertBroadcastReceiver();
	private final int UNKNOWN = -1, OUT = 0, IN = 1;
	int mIsHeadsetPlugged = UNKNOWN; /* -1 = unknown , 0 = out, 1 = in */
	public Preview mPreview;

	HeadInTest(ID pid, String s) {
		super(pid, s);

		hKey = new KeyHandler() {
			public boolean handleKey(KeyEvent event) {
				return true;
			}

		};
	}

	@Override
	protected void Run() {
		// this function executes the test
		switch (mState) {
		case INIT:
			// Run every seconds

			IntentFilter intentFilter = new IntentFilter(
					Intent.ACTION_HEADSET_PLUG);
			mContext.registerReceiver(mHeadsetStatus, intentFilter);

			TestLayout1 tl = new TestLayout1(mContext, mName,
					"Headset out,pls insert headset");
			mContext.setContentView(tl.ll);
			SetTimer(10000 /* ms */, new CallBack() {
				public void c() {
					mState = TIMEOUT;
					Run();
				}
			});

			break;
		case TIMEOUT:
			tl = new TestLayout1(mContext, mName, "Headset detection fail");
			mContext.setContentView(tl.ll);
			try {
				mContext.unregisterReceiver(mHeadsetStatus);
			} catch (Exception e) {
				Log.e(TAG, "unregister failed " + e);
			}
			break;

		default:
			if (mIsHeadsetPlugged == IN) {
				StopTimer();
				tl = new TestLayout1(mContext, mName, "Headset detected");
				mContext.setContentView(tl.ll);
				try {
					mContext.unregisterReceiver(mHeadsetStatus);
				} catch (Exception e) {
					Log.e(TAG, "unregister failed " + e);
				}
				mState = END;
			} else if (mIsHeadsetPlugged == OUT) {
			}

			break;

		}
	}

	class HeadInsertBroadcastReceiver extends BroadcastReceiver {

		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			Log.w("Test", action);
			if (action.equals(Intent.ACTION_HEADSET_PLUG)) {

				mIsHeadsetPlugged = (intent.getIntExtra("state", UNKNOWN));
				Log.w(TAG,
						"Headset insert status received : "
								+ Integer.toString(mIsHeadsetPlugged));
				mState++;
				Run();
			}

		}

	}

}

class HeadLeftTest extends HeadSoundTest {
	HeadLeftTest(ID pid, String s) {
		super(pid, s, 0.0f, 0.125f);
	}
}

class HeadRightTest extends HeadSoundTest {
	HeadRightTest(ID pid, String s) {
		super(pid, s, 0.125f, 0.0f);
	}
}

class HeadSoundTest extends Test {

	private MediaPlayer mMediaPlayer;
	private float mLeftVol, mRightVol;

	HeadSoundTest(ID pid, String s, float leftvol, float rightvol) {
		super(pid, s);
		mLeftVol = leftvol;
		mRightVol = rightvol;

		hKey = new KeyHandler() {
			public boolean handleKey(KeyEvent event) {
				return true;
			}

		};
	}

	void setDataSourceFromResource(Resources resources, MediaPlayer player,
			int res) throws java.io.IOException {
		AssetFileDescriptor afd = resources.openRawResourceFd(res);
		if (afd != null) {
			player.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(),
					afd.getLength());
			afd.close();
		}
	}

	private void startMelody(MediaPlayer player) throws java.io.IOException,
			IllegalArgumentException, IllegalStateException {
		// player.setAudioStreamType(AudioManager.STREAM_MUSIC);
		player.setLooping(true);
		player.prepare();
		player.start();
	}

	@Override
	protected void Run() {
		// this function executes the test
		switch (mState) {
		case INIT:
			// Run every seconds
			mMediaPlayer = new MediaPlayer();
			mMediaPlayer.setVolume(mLeftVol, mRightVol);
			try {
				setDataSourceFromResource(mContext.getResources(),
						mMediaPlayer, R.raw.in_call_alarm);
				startMelody(mMediaPlayer);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			TestLayout1 tl = new TestLayout1(mContext, mName,
					"Headset right discrete test", "Fail", "Pass");
			mContext.setContentView(tl.ll);
			mState++;

			break;

		case END:

			mMediaPlayer.release();

			// Exit();

			break;
		default:
		}
	}

}

class HeadOutTest extends Test {
	int timeout = 0;
	private String TAG = Test.TAG + " HeadOutTest";
	private final HeadInsertBroadcastReceiver mHeadsetStatus = new HeadInsertBroadcastReceiver();
	private final int UNKNOWN = -1, OUT = 0, IN = 1;
	int mIsHeadsetPlugged = UNKNOWN; /* -1 = unknown , 0 = out, 1 = in */
	public Preview mPreview;

	HeadOutTest(ID pid, String s) {
		super(pid, s);

		hKey = new KeyHandler() {
			public boolean handleKey(KeyEvent event) {
				return true;
			}

		};
	}

	@Override
	protected void Run() {
		// this function executes the test
		switch (mState) {
		case INIT:
			// Run every seconds

			IntentFilter intentFilter = new IntentFilter(
					Intent.ACTION_HEADSET_PLUG);
			mContext.registerReceiver(mHeadsetStatus, intentFilter);

			TestLayout1 tl = new TestLayout1(mContext, mName,
					"Headset in,pls remove headset");
			mContext.setContentView(tl.ll);
			SetTimer(10000 /* ms */, new CallBack() {
				public void c() {
					mState = TIMEOUT;
					Run();
				}
			});

			break;
		case TIMEOUT:
			tl = new TestLayout1(mContext, mName,
					"TIMEOUT:\nHeadset not removed");
			mContext.setContentView(tl.ll);
			try {
				mContext.unregisterReceiver(mHeadsetStatus);
			} catch (Exception e) {
				Log.e(TAG, "unregister failed " + e);
			}
			break;

		default:
			if (mIsHeadsetPlugged == OUT) {
				StopTimer();
				tl = new TestLayout1(mContext, mName, "Headset removed");
				mContext.setContentView(tl.ll);
				try {
					mContext.unregisterReceiver(mHeadsetStatus);
				} catch (Exception e) {
					Log.e(TAG, "unregister failed " + e);
				}
				mState = END;
			} else if (mIsHeadsetPlugged == IN) {
			}
			break;

		}
	}

	class HeadInsertBroadcastReceiver extends BroadcastReceiver {

		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			Log.w("Test", action);
			if (action.equals(Intent.ACTION_HEADSET_PLUG)) {

				mIsHeadsetPlugged = (intent.getIntExtra("state", UNKNOWN));
				Log.w(TAG,
						"Headset insert status received : "
								+ Integer.toString(mIsHeadsetPlugged));
				mState++;
				Run();
			}

		}

	}

}

class HeadsetTest extends Test {
	int timeout = 0;
	private String TAG = Test.TAG + " HeadsetTest";
	private final HeadInsertBroadcastReceiver mHeadsetStatus = new HeadInsertBroadcastReceiver();
	private final int UNKNOWN = -1, OUT = 0, IN = 1;

	private float mVol = 1.0f;

	private int mSavedPlugStatus = UNKNOWN;

	private final int HEADSET_OUT = INIT + 1;
	private final int HEADSET_IN = INIT + 2;
	private final int HEADSET_LEFT = INIT + 3;
	private final int HEADSET_RIGHT = INIT + 4;
	private final int HEADSET_LOOP = INIT + 5;
	private final int HEADSET_FM = INIT + 6;
	private final int HEADSET_IN_END = INIT + 7;
	private final int HEADSET_TEST_OK = INIT + 8;
	// private final int HEADSET_IN_END = INIT+6;
	private int oldState = 0;
	
	private MediaPlayer mMP = null;
	private MediaPlayer mMediaPlayer;
	private int defaultRingVol = 8;
	private int defaultMusicVol = 8;
	private TestLayout1 tl;

	private WXKJRapi remote;
	private int mFMon = 0;
	private int mLoop = 0;
	private int rssi;
	private String mPreAudioParm="";

	HeadsetTest(ID pid, String s) {
		super(pid, s);

		hKey = new KeyHandler() {
			public boolean handleKey(KeyEvent event) {
				if (event.getAction() == KeyEvent.ACTION_UP) {
					if (event.getKeyCode() == KeyEvent.KEYCODE_VOLUME_UP) {
						mVol += 0.125;
					} else if (event.getKeyCode() == KeyEvent.KEYCODE_VOLUME_DOWN) {
						mVol -= 0.125;
					} else {
						return false;
					}
					if (mMediaPlayer != null) {
						mMediaPlayer.setVolume(0.0f, mVol);
						Log.d(TAG, "set volume to L=" + 0.0 + " R=" + mVol);
					}
					return true;
				} else {
					return false;
				}
			}
		};

	}

	private void playMelody(Resources resources, int res) {
		// player.setAudioStreamType(AudioManager.STREAM_MUSIC);
		AssetFileDescriptor afd = resources.openRawResourceFd(res);

		try {

			if (afd != null) {
				mMediaPlayer.setDataSource(afd.getFileDescriptor(),
						afd.getStartOffset(), afd.getLength());
				afd.close();
			}

			mMediaPlayer.setLooping(true);
			mMediaPlayer.prepare();
			mMediaPlayer.start();
		} catch (IOException e) {
			Log.e(TAG, "can't play melody cause:" + e);
		}
	}

	View.OnClickListener mLeftButton = new View.OnClickListener() {
		public void onClick(View v) {
			if(HEADSET_LOOP == mState)
			{
				AudioManager am = (AudioManager) mContext
					.getSystemService(Context.AUDIO_SERVICE);
				am.setParameters("SET_LOOPBACK_TYPE=0");
			}
			ExecuteTest.currentTest.Result = Test.FAILED;
			ExecuteTest.currentTest.Exit();
		}
	};

	View.OnClickListener mRightButton = new View.OnClickListener() {
		public void onClick(View v) {
			mState++;
		//	if(mState == HEADSET_LOOP)
		//		mState++;
			Run();
		}
	};

	private AudioManager mAM = null;
	private int FIXED_STATION_FREQ = 975; // 1036 * 100k Hz
	private String FM_AUDIO_ENABLE = "AudioSetFmEnable=1";
	private String FM_AUDIO_DISABLE = "AudioSetFmEnable=0";
	private String FM_RADIO_FREQ = "FMRadio freq 97.1 MHz";

	void PlayFM() {

        	Intent intent = new Intent();
        	intent.setAction("com.android.mmitest.command");
        	 mContext.sendBroadcast(intent);
        	 Log.e(TAG, "onStart, start FM service");
        
	}

	void StopFM() {
    	Intent intent = new Intent();
    	intent.setAction("com.android.music.musicservicecommand");
    	intent.putExtra("command","pause");
    	 mContext.sendBroadcast(intent);
    	 Log.e(TAG, "onStart, exit FM service");

	}

	@Override
	protected void Run() {
		// this function executes the test
		AudioManager am;
		switch (mState) {
		case INIT:

			mSavedPlugStatus = UNKNOWN;

			tl = new TestLayout1(mContext, mName, getResource(R.string.headset_insert));
			tl.setEnabledButtons(false);
			mContext.setContentView(tl.ll);

			IntentFilter intentFilter = new IntentFilter(
					Intent.ACTION_HEADSET_PLUG);
			mContext.registerReceiver(mHeadsetStatus, intentFilter);
			/*
			 * SetTimer(15000, new CallBack() { public void c() { mState =
			 * TIMEOUT; Run(); } });
			 */
			//mMediaPlayer = new MediaPlayer();
			if (mMediaPlayer != null) {
				//mMediaPlayer.stop();
				mMediaPlayer.release();
				mMediaPlayer = null;
			}
			
			Intent intent = new Intent();
			intent.setAction("com.android.fmradio.IFmRadioService");
			intent.setPackage("com.android.fmradio");
	       if (null == mContext.startService(intent)) {
	    	   Log.e(TAG, "onStart, cannot start FM service");
	        }
	       else
	    	   Log.e(TAG, "onStart,  start FM service");
	       
			break;
		case HEADSET_OUT:
			tl = new TestLayout1(mContext, mName, getResource(R.string.headset_insert));
			tl.setEnabledButtons(false);
			//we should avoid the crush here if head set is out
			
			if (mFMon == 1) {
				oldState = HEADSET_FM;
				StopFM();
				mFMon = 0;
			}
			
			if(mLoop ==1){
				oldState = HEADSET_LOOP;
			}
			
			if (mMediaPlayer != null) {
				mMediaPlayer.stop();
				mMediaPlayer.release();
				mMediaPlayer = null;
			}
			mContext.setContentView(tl.ll);
			break;

		case HEADSET_IN:
			// StopTimer();
			tl = new TestLayout1(mContext, mName, getResource(R.string.headset_detect), getResource(R.string.fail),
					getResource(R.string.pass), mLeftButton, mRightButton);
			if(oldState == HEADSET_LOOP){
				oldState = 0;
				mState = HEADSET_LOOP;
			}
			else if(oldState == HEADSET_FM){
				oldState = 0;	
				mState = HEADSET_FM;
			}
			else
				mState++;
			if(mMediaPlayer== null || mState != HEADSET_FM ||mState != HEADSET_LOOP){
				mMediaPlayer = new MediaPlayer();
			}
			mContext.setContentView(tl.ll);
			Run();
			break;

		case HEADSET_LEFT:
		    if (false) {
			mState++;
		    } else {
			tl = new TestLayout1(mContext, mName, "\n\n"+getResource(R.string.headset_left_detect),
					getResource(R.string.fail), getResource(R.string.pass), mLeftButton, mRightButton);
			mContext.setContentView(tl.ll);

			if (mMediaPlayer != null) {
				am = (AudioManager) mContext
						.getSystemService(Context.AUDIO_SERVICE);
				defaultMusicVol = am.getStreamVolume(AudioManager.STREAM_MUSIC);
				defaultRingVol = am.getStreamVolume(AudioManager.STREAM_RING);
				am.setStreamVolume(AudioManager.STREAM_MUSIC, 13, 0);
				am.setStreamVolume(AudioManager.STREAM_RING, 13, 0);

				am.setSpeakerphoneOn(false);
				am.setWiredHeadsetOn(true);
				mMediaPlayer.setVolume(mVol, 0.000f);
				playMelody(mContext.getResources(), R.raw.mojito_112_signal_30s_9db);
			}
			//mState ++;
			break;
		    }
		case HEADSET_RIGHT:
		    if (false) {
			mState++;
		    } else {
			tl = new TestLayout1(mContext, mName,
					"\n\n"+getResource(R.string.headset_right_detect), getResource(R.string.fail), getResource(R.string.pass), mLeftButton,
					mRightButton);
			mContext.setContentView(tl.ll);
			if (mMediaPlayer != null)
				mMediaPlayer.setVolume(0.000f, mVol);
			break;
		    }
			//end PR357467
		case HEADSET_LOOP:
		    if (!BuildConfig.getMmiTest()) {
			mState++;
		    } else {
			if (true) {
				if(mMediaPlayer != null){
					mMediaPlayer.stop();
					mMediaPlayer.release();
					mMediaPlayer = null;
				}
				// SystemClock.sleep(2000);

				am = (AudioManager) mContext
						.getSystemService(Context.AUDIO_SERVICE);
				am.setStreamMute(AudioManager.STREAM_MUSIC, false);
				am.setStreamMute(AudioManager.STREAM_NOTIFICATION, false);
				am.setStreamMute(AudioManager.STREAM_RING, false);
				am.setStreamMute(AudioManager.STREAM_VOICE_CALL, false);
				am.setSpeakerphoneOn(false);
				am.setWiredHeadsetOn(true);
				am.setMode(AudioSystem.MODE_NORMAL);
			}

//			SystemClock.sleep(1000);
//			{
//				WXKJClient mClient = new WXKJClient();
//				if (!mClient.connect()) {
//					Log.e(TAG, "Connecting MMI proxy fail!");
//					break;
//				}
//				if (!mClient.sendCommand(WXKJClient.MMICMD_HEADSET_AUDIOLOOP_ON)) {
//					Log.e(TAG, "Send command to MMI proxy fail!");
//					mClient.disconnect();
//				}
//				// SystemClock.sleep(500);
//				boolean success = mClient.readReply();
//				mClient.disconnect();
//			}
		//	 AudioSystem.setAudioCommand(
		//			 0x90, 0x20);
			am.setParameters("SET_LOOPBACK_TYPE=2,2");
			mLoop = 1;
			tl = new TestLayout1(mContext, mName, getResource(R.string.mic_loop_tip), getResource(R.string.fail), getResource(R.string.pass), mLeftButton, mRightButton);
			mContext.setContentView(tl.ll);
			break;
		    }
		case HEADSET_FM:

			
           //   if (!BuildConfig.getMmiTest()) {
			  if(mMediaPlayer != null){
				mMediaPlayer.stop();
				mMediaPlayer.release();
				mMediaPlayer = null;
			}

			// SystemClock.sleep(2000);

			am = (AudioManager) mContext
					.getSystemService(Context.AUDIO_SERVICE);

			am.setParameters("SET_LOOPBACK_TYPE=0");
			am.setMode(AudioSystem.MODE_NORMAL);
		//	}
			if (mLoop == 1) {
				mLoop = 0;

//				WXKJClient mClient = new WXKJClient();
//				if (!mClient.connect()) {
//					Log.e(TAG, "Connecting MMI proxy fail!");
//					break;
//				}
//				if (!mClient
//						.sendCommand(WXKJClient.MMICMD_HEADSET_AUDIOLOOP_OFF)) {
//					Log.e(TAG, "Send command to MMI proxy fail!");
//					mClient.disconnect();
//				}
//				// SystemClock.sleep(500);
//				boolean success = mClient.readReply();
//				mClient.disconnect();
		//		  AudioSystem.setAudioCommand(
		//					 0x90, 0x21);
			}

			// WXKJRapi.doAudioHeadsetLoop(false);
			PlayFM();
			mFMon = 1;
			rssi = FmNative.readRssi();
			if (true) {
				tl = new TestLayout1(mContext, mName, "\n"+getResource(R.string.fm_tip)+"\n\nRSSI: "+rssi, getResource(R.string.fail),
						getResource(R.string.pass), mLeftButton, mRightButton);
			} else {
				tl = new TestLayout1(mContext, mName, FM_RADIO_FREQ);
			}
			mContext.setContentView(tl.ll);
			break;

		case HEADSET_IN_END:
			// remote.setAudioStereoHeadsetLoop(false);
			// WXKJRapi.doAudioHeadsetLoop(false);
			// StopFM();
			if (mFMon == 1) {
				StopFM();
				mFMon = 0;
			}

			if (false) {
				mMediaPlayer.stop();
				mMediaPlayer.release();
				mMediaPlayer = null;

				SystemClock.sleep(500);

				am = (AudioManager) mContext
						.getSystemService(Context.AUDIO_SERVICE);

				am.setMode(AudioSystem.MODE_NORMAL);
			}
			
			tl = new TestLayout1(mContext, mName, "\n\n"+getResource(R.string.headset_ok));
    	    tl.hideButtons();
			mContext.setContentView(tl.ll);

			break;

		case TIMEOUT:
			tl = new TestLayout1(mContext, mName, "\n\n"+getResource(R.string.heaset_fail),
					getResource(R.string.fail), getResource(R.string.pass));
			mContext.setContentView(tl.ll);

			prepareExit();
			break;
		case HEADSET_TEST_OK:
			if (mFMon == 1) {
				StopFM();
				mFMon = 0;
			}

			prepareExit();
			ExecuteTest.currentTest.Result = Test.PASSED;
			ExecuteTest.currentTest.Exit();
			break;
		case END:
			if (mFMon == 1) {
				StopFM();
				mFMon = 0;
			}

			prepareExit();

			break;

		}
	}

	private void prepareExit() {
		AudioManager am = (AudioManager) mContext
				.getSystemService(Context.AUDIO_SERVICE);
		am.setParameters("SET_LOOPBACK_TYPE=0");
		if (mMediaPlayer != null) {
			mMediaPlayer.release();
		}
		am.setStreamVolume(AudioManager.STREAM_MUSIC, defaultMusicVol, 0);
		am.setStreamVolume(AudioManager.STREAM_RING, defaultRingVol, 0);
		try {
			mContext.unregisterReceiver(mHeadsetStatus);
		} catch (Exception e) {
			Log.e(TAG, "unregister failed " + e);
		}
	}

	class HeadInsertBroadcastReceiver extends BroadcastReceiver {

		public synchronized void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			Log.w("Test", action);
			/*if (mLoop == 1)
				return;
			if (mFMon == 1) {
				StopFM();
				mFMon = 0;
				return;
			}*/
			
			if (action.equals(Intent.ACTION_HEADSET_PLUG)) {

				int mPlugStatus = (intent.getIntExtra("state", UNKNOWN));

				Log.w(TAG,
						"Headset insert status received : "
								+ Integer.toString(mPlugStatus));
			
				if (mSavedPlugStatus != mPlugStatus) {
					if (mPlugStatus == IN) {
						mState = HEADSET_IN;
					} 
					else if (mState == HEADSET_IN_END && mPlugStatus == OUT) {
						mState = HEADSET_TEST_OK;
					} else {
						mState = HEADSET_OUT;
					}
					//modify by xianfeng.xu for CR364991 end
					mSavedPlugStatus = mPlugStatus;
					Run();
				}
			}

		}

	}

}
