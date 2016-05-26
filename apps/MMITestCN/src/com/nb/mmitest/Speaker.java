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

import com.nb.mmitest.R;
import com.android.fmradio.FmNative;

class SpeakerTest extends Test {

	private TestLayout1 tl = null;

	private Process p;

	private WXKJRapi remote;

	private boolean mLoopOn = false;

	private MediaPlayer mMediaPlayer = null;

	private AudioManager am;
	private int defaultVol;
	SpeakerTest(ID pid, String s) {
		this(pid, s, 0);
	}

	SpeakerTest(ID pid, String s, int timein) {
		super(pid, s, timein, 0);
		/*
		hKey = new KeyHandler() {
			public boolean handleKey(KeyEvent event) {
				return true;
			}

		};
		*/

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

	private void startMelody() throws java.io.IOException,
			IllegalArgumentException, IllegalStateException {
		// player.setAudioStreamType(AudioManager.STREAM_MUSIC);
		mMediaPlayer.setLooping(true);
		mMediaPlayer.prepare();
		mMediaPlayer.start();

	}

	View.OnClickListener LeftButton = new View.OnClickListener() {
		public void onClick(View v) {
			if (mMediaPlayer != null)
				mMediaPlayer.stop();
			mState = END;
			Run();
			ExecuteTest.currentTest.Result = Test.FAILED;
			ExecuteTest.currentTest.Exit();
		}
	};

	View.OnClickListener RightButton = new View.OnClickListener() {
		public void onClick(View v) {
			mState++;
			Run();
		}
	};

	@Override
	protected void Run() {
		// this function executes the test
		switch (mState) {
		case INIT:
			Log.d(TAG, "@@@@@@@@@@@@@@@@@@@@@@@Speaker INIT");

			mTimeIn.start();

			am = (AudioManager) mContext
					.getSystemService(Context.AUDIO_SERVICE);

			if(mMediaPlayer == null)
			{
				mMediaPlayer = new MediaPlayer();

			//mMediaPlayer.reset();
			//mMediaPlayer.setVolume(1.0f, 1.0f);

			am.setMode(AudioSystem.MODE_NORMAL);
			defaultVol = am.getStreamVolume(AudioManager.STREAM_MUSIC);
			am.setStreamVolume(AudioManager.STREAM_MUSIC,
					am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)-2, 0);
			
			am.setSpeakerphoneOn(true);
			am.setWiredHeadsetOn(false);

			try {

				setDataSourceFromResource(mContext.getResources(),
						mMediaPlayer, R.raw.mojito_112_signal_30s_9db);

				startMelody();

			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
			
			tl = new TestLayout1(mContext, mName, getResource(R.string.speaker_test),
					getResource(R.string.fail), getResource(R.string.pass));
			
			mContext.setContentView(tl.ll);
			mTimeIn.start();
			if (!mTimeIn.isFinished()) 
				tl.hideButtons();

			break;

		case END:
			if (tl != null)
				tl.setEnabledButtons(false);
			
			if (mMediaPlayer != null) {
				mMediaPlayer.stop();
				mMediaPlayer.release();
				mMediaPlayer = null;
			}
			Log.d(TAG, "@@@@@@@@@@@@@@@@@@@@@@@Speaker END1");

			//if(MMITest.mgMode == MMITest.MANU_MODE){
			am.setSpeakerphoneOn(false);
			//am.setMode(AudioSystem.MODE_NORMAL);
			am.setStreamMute(AudioManager.STREAM_MUSIC, false);
			am.setStreamMute(AudioManager.STREAM_NOTIFICATION, false);
			am.setStreamMute(AudioManager.STREAM_RING, false);
			am.setStreamVolume(AudioManager.STREAM_MUSIC, defaultVol, 0);
			//}
			Log.d(TAG, "@@@@@@@@@@@@@@@@@@@@@@@Speaker END");
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
	@Override
	public void Exit() {
			if (mMediaPlayer != null) {
				mMediaPlayer.stop();
				mMediaPlayer.release();
				mMediaPlayer = null;
			}
		super.Exit();
	}
}
