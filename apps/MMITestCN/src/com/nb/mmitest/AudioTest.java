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
//Add new feature
import com.nb.mmitest.BuildConfig;
import com.nb.mmitest.R;

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

class MelodyTest extends Test {

	private TestLayout1 tl = null;

	private Process p;

	private WXKJRapi remote;

	private boolean mLoopOn = false;

	private MediaPlayer mMediaPlayer = null;

	private AudioManager am;

	MelodyTest(ID pid, String s) {
		this(pid, s, 0);
	}

	MelodyTest(ID pid, String s, int timein) {
		super(pid, s, timein, 0);

		//hKey = new KeyHandler() {
		//	public boolean handleKey(KeyEvent event) {
		//		return true;
		//	}

		//};

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

			mTimeIn.start();

			am = (AudioManager) mContext
					.getSystemService(Context.AUDIO_SERVICE);

			mMediaPlayer = new MediaPlayer();
			if (false) {
				mState++;
			} else {
				mMediaPlayer.setVolume(11.0f, 11.0f);
				/* add by stephen.huang */
				//am.setMode(AudioManager.MODE_IN_CALL);
				/* end add */
				am.setSpeakerphoneOn(false);
				am.setWiredHeadsetOn(false);
				// am.setMode(AudioSystem.ROUTE_SPEAKER);
				mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);	
				am.setMode(AudioManager.MODE_IN_COMMUNICATION);				
				am.setStreamVolume(AudioManager.STREAM_MUSIC,am.getStreamMaxVolume(AudioManager.STREAM_MUSIC), 0);
				am.setStreamVolume(AudioManager.STREAM_VOICE_CALL, am.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL), 0);
				try {
					setDataSourceFromResource(mContext.getResources(),
							mMediaPlayer, R.raw.mojito_112_signal_30s_9db);
					startMelody();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				tl = new TestLayout1(mContext, mName, "Receiver discrete test",
						"Fail", "Pass", LeftButton, RightButton);

				if (!mTimeIn.isFinished())
					tl.setEnabledButtons(false);

				mContext.setContentView(tl.ll);

				break;
			}
		case INIT + 1:
			// mMediaPlayer = new MediaPlayer();
			// mMediaPlayer.setVolume(11, 11);
			mTimeIn.start();

			mMediaPlayer.reset();
			mMediaPlayer.setVolume(1.0f, 1.0f);

			am.setStreamVolume(AudioManager.STREAM_MUSIC,
					am.getStreamMaxVolume(AudioManager.STREAM_MUSIC), 0);
			am.setMode(AudioSystem.MODE_NORMAL);

			am.setSpeakerphoneOn(true);
			am.setWiredHeadsetOn(false);

			try {
				// ajayet : replaced the resource because quick gets
				// PVMFErrNotSupported from driver */

				setDataSourceFromResource(mContext.getResources(),
						mMediaPlayer, R.raw.mojito_112_signal_30s_9db/*
																		 * R.raw.
																		 * quick
																		 */);

				// mMediaPlayer.setDataSource(mContext.getFileStreamPath("samplefile.amr").getAbsolutePath());

				startMelody();

			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			if (BuildConfig.getMmiTest()) {
				tl = new TestLayout1(mContext, mName, "Speaker Melody test",
						"Fail", "Pass", LeftButton, RightButton);
			} else {
				tl = new TestLayout1(mContext, mName, "Speaker Melody test",
						"Fail", "Pass");
			}
			am.setSpeakerphoneOn(false);
			if (!mTimeIn.isFinished())
				tl.setEnabledButtons(false);

			mContext.setContentView(tl.ll);

			break;

		case INIT + 2:
			if (mMediaPlayer != null) {
				mMediaPlayer.stop();
				mMediaPlayer.release();
				mMediaPlayer = null;
			}
			/* restore normal audio state to avoid affecting other applications */
			/*
			am.setStreamMute(AudioManager.STREAM_MUSIC, false);
			am.setStreamMute(AudioManager.STREAM_NOTIFICATION, false);
			am.setStreamMute(AudioManager.STREAM_RING, false);
			*/
			//20121009 ying.pang for mic test
			am.setRouting(AudioManager.MODE_IN_CALL, AudioManager.ROUTE_EARPIECE, AudioManager.ROUTE_ALL);    
			mContext.setVolumeControlStream(AudioManager.STREAM_VOICE_CALL);
			am.setMode(AudioSystem.MODE_IN_CALL);  
			//20121026 ying.pang for change volume larger
			int maxVol = am.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL);			
			am.setStreamVolume(AudioManager.STREAM_VOICE_CALL, maxVol, 0);
			//
			// mMediaPlayer.stop();
			// mMediaPlayer.release();
			// /* restore normal audio state to avoid affecting other
			// applications
			// */
			// //am.setMode(AudioSystem.MODE_IN_CALL);
			SystemClock.sleep(2000);
			// /*
			// try {
			// remote = new WXKJRapi(mContext);
			// remote.setAudioHandsetLoop(false);
			// SystemClock.sleep(1500);// required to let the first command
			// finish
			//
			// remote.setAudioHandsetLoop(true);
			// mLoopOn = true;
			// tl = new
			// TestLayout1(mContext,mName,"loop from MIC test","Fail","Pass");
			// mContext.setContentView(tl.ll);
			//
			// }catch (FileNotFoundException e ) {
			// tl = new
			// TestLayout1(mContext,mName,"loop from MIC test init failed","Fail","Pass");
			// mContext.setContentView(tl.ll);
			// }
			// */
			// WXKJRapi.doAudioHandsetLoop(true);
			{
				WXKJClient mClient = new WXKJClient();
				if (!mClient.connect()) {
					Log.e(TAG, "Connecting MMI proxy fail!");
					break;
				}
				if (!mClient.sendCommand(WXKJClient.MMICMD_AUDIOLOOP_ON)) {
					Log.e(TAG, "Send command to MMI proxy fail!");
					mClient.disconnect();
				}
				// SystemClock.sleep(500);
				boolean success = mClient.readReply();
				mClient.disconnect();
			}
			mLoopOn = true;
			if(util.isMtkDualMicSupport(mContext))
			{
				tl = new TestLayout1(mContext, mName, "loop from MIC test", "Fail",
					"Pass", LeftButton, RightButton);
			}
			else
			{
				tl = new TestLayout1(mContext, mName, "loop from MIC test",
					"Fail", "Pass");
			}
			mContext.setContentView(tl.ll);

			break;

		case INIT + 3:
			if(util.isMtkDualMicSupport(mContext))
			{
				{
					WXKJClient mClient = new WXKJClient();
					if (!mClient.connect()) {
						Log.e(TAG, "Connecting MMI proxy fail!");
						break;
					}
					if (!mClient.sendCommand(WXKJClient.MMICMD_AUDIOLOOP_OFF)) {
						Log.e(TAG, "Send command to MMI proxy fail!");
						mClient.disconnect();
					}
					// SystemClock.sleep(500);
					boolean success = mClient.readReply();
					mClient.disconnect();
				}

				SystemClock.sleep(1000);

				{
					WXKJClient mClient = new WXKJClient();
					if (!mClient.connect()) {
						Log.e(TAG, "Connecting MMI proxy fail!");
						break;
					}
					if (!mClient.sendCommand(WXKJClient.MMICMD_SUB_AUDIOLOOP_ON)) {
						Log.e(TAG, "Send command to MMI proxy fail!");
						mClient.disconnect();
					}
					// SystemClock.sleep(500);
					boolean success = mClient.readReply();
					mClient.disconnect();
				}
				tl = new TestLayout1(mContext, mName, "loop from sub MIC test",
						"Fail", "Pass");
				mContext.setContentView(tl.ll);
			}
			break;

		case END:
			if (p != null) {
				p.destroy();
			}

			if (mLoopOn) {
				WXKJClient mClient = new WXKJClient();
				if (!mClient.connect()) {
					Log.e(TAG, "Connecting MMI proxy fail!");
					am.setStreamMute(AudioManager.STREAM_MUSIC, false);
					am.setStreamMute(AudioManager.STREAM_NOTIFICATION, false);
					am.setStreamMute(AudioManager.STREAM_RING, false);
					am.setMode(AudioSystem.MODE_NORMAL);
					break;
				}
				if (!mClient.sendCommand(WXKJClient.MMICMD_SUB_AUDIOLOOP_OFF)) {
					Log.e(TAG, "Send command to MMI proxy fail!");
					mClient.disconnect();
				}
				// SystemClock.sleep(500);
				boolean success = mClient.readReply();
				mClient.disconnect();
				am.setStreamMute(AudioManager.STREAM_MUSIC, false);
				am.setStreamMute(AudioManager.STREAM_NOTIFICATION, false);
				am.setStreamMute(AudioManager.STREAM_RING, false);
				am.setMode(AudioSystem.MODE_NORMAL);
				// remote.setAudioHandsetLoop(false);
				// WXKJRapi.doAudioHandsetLoop(false);
				mLoopOn = false;

				// SystemClock.sleep(2000);
			} else {
				if (mMediaPlayer != null) {
					mMediaPlayer.stop();
					mMediaPlayer.release();
					mMediaPlayer = null;
				}

				/*
				 * restore normal audio state to avoid affecting other
				 * applications
				 */
				am.setStreamMute(AudioManager.STREAM_MUSIC, false);
				am.setStreamMute(AudioManager.STREAM_NOTIFICATION, false);
				am.setStreamMute(AudioManager.STREAM_RING, false);
				am.setMode(AudioSystem.MODE_NORMAL);
			}
			break;
		default:
		}
	}

	@Override
	protected void onTimeInFinished() {
		if (tl != null)
			tl.setEnabledButtons(true);
	}

}

/* for TEST only */
class AudioLoopTest extends Test {

	private TestLayout1 tl = null;

	private Process p;

	private WXKJRapi remote;

	private boolean mLoopOn = false;

	AudioLoopTest(ID pid, String s) {
		this(pid, s, 0);
	}

	AudioLoopTest(ID pid, String s, int timein) {
		super(pid, s, timein, 0);

		//hKey = new KeyHandler() {
		//	public boolean handleKey(KeyEvent event) {
		//		return true;
		//	}

		//};

	}

	@Override
	protected void Run() {
		// this function executes the test
		switch (mState) {
		case INIT:

			AudioManager am = (AudioManager) mContext
					.getSystemService(Context.AUDIO_SERVICE);

			MediaPlayer mMediaPlayer = new MediaPlayer();

			mMediaPlayer.setVolume(11.0f, 11.0f);

			am.setMode(AudioManager.MODE_IN_CALL);
			am.setSpeakerphoneOn(false);
			am.setWiredHeadsetOn(false);

			try {
				AssetFileDescriptor afd = mContext.getResources()
						.openRawResourceFd(R.raw.in_call_alarm);
				if (afd != null) {
					mMediaPlayer.setDataSource(afd.getFileDescriptor(),
							afd.getStartOffset(), afd.getLength());
					afd.close();
				}
				mMediaPlayer.setLooping(false);
				mMediaPlayer.prepare();
				mMediaPlayer.start();

				while (mMediaPlayer.isPlaying()) {
					;
				}

			} catch (IOException e) {
				// TODO Auto-generated catch block
				Log.d(TAG, "ERROR" + e);
			}
			mMediaPlayer.stop();

			// mMediaPlayer=null;

			// System.gc();

			SystemClock.sleep(2000);

			/*
			 * try { remote = new WXKJRapi(mContext);
			 * remote.setAudioHandsetLoop(false); SystemClock.sleep(1000);//
			 * required to let the first command finish
			 * remote.setAudioHandsetLoop(true); mLoopOn = true; tl = new
			 * TestLayout1(mContext,mName,"loop from MIC test","Fail","Pass");
			 * mContext.setContentView(tl.ll);
			 * 
			 * }catch (FileNotFoundException e ) { tl = new
			 * TestLayout1(mContext,
			 * mName,"loop from MIC test init failed","Fail","Pass");
			 * mContext.setContentView(tl.ll); }
			 */
			WXKJRapi.doAudioHandsetLoop(true);
			mLoopOn = true;
			tl = new TestLayout1(mContext, mName, "loop from MIC test", "Fail",
					"Pass");
			mContext.setContentView(tl.ll);

			break;

		case END:
			if (p != null) {
				p.destroy();
			}

			if (mLoopOn) {
				// remote.setAudioHandsetLoop(false);
				WXKJRapi.doAudioHandsetLoop(false);
				mLoopOn = false;

				SystemClock.sleep(2000);
			}

			break;
		default:
		}
	}

	@Override
	protected void onTimeInFinished() {
		if (tl != null)
			tl.setEnabledButtons(true);
	}

}

class MicTest extends Test {
	static final String SAMPLE_PREFIX = "recording";
	public static final int SDCARD_ACCESS_ERROR = 1;
	File mSampleFile = null;
	private MediaPlayer mMediaPlayer;
	private String TAG = super.TAG + "MicTest";

	MediaRecorder recorder;

	MicTest(ID pid, String s) {
		super(pid, s);

		//hKey = new KeyHandler() {
		//	public boolean handleKey(KeyEvent event) {
		//		return true;
		//	}

		//};
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

	private void startRecord() throws IllegalStateException, IOException {
		if (mSampleFile == null) {
			File sampleDir = Environment.getDataDirectory();
			if (!sampleDir.canWrite()) // Workaround for broken sdcard support
										// on the device.
				sampleDir = new File("/sdcard/sdcard");

			mSampleFile = mContext.getFileStreamPath("mictest.amr");
			mSampleFile.createNewFile();
			mSampleFile.canRead();

			// the source file created by MMITest will be --rw------ by default
			// as MMITest has root rights
			// to let the player application work properly, file must have
			// -rw-rw-rw- rights.
			// so we change the file rights here using chmod call

			try {
				Process p = Runtime.getRuntime().exec(
						"chmod 777 " + mSampleFile.getPath());
				p.waitFor();
				p = Runtime.getRuntime().exec("ls -l " + mSampleFile.getPath());
				p.waitFor();

				BufferedReader reader = new BufferedReader(
						new java.io.InputStreamReader(p.getInputStream()));
				String line = reader.readLine();
				Log.i(TAG, "output file rights changed to: " + line);

			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				Log.e(TAG, "can't change file rights");
			}

		}

		recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
		recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
		// recorder.setOutputFormat(MediaRecorder.OutputFormat.RAW_AMR);
		recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
		recorder.setOutputFile(mSampleFile.getAbsolutePath());
		recorder.prepare();
		recorder.start(); // Recording is now started
	}

	public void delete() {

		if (mSampleFile != null) {
			mSampleFile.delete();
		}
		mSampleFile = null;

	}

	@Override
	protected void Run() {
		// this function executes the test
		switch (mState) {

		case INIT:

			mMediaPlayer = new MediaPlayer();

			recorder = new MediaRecorder();
			try {
				startRecord();
			} catch (IllegalStateException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			View.OnClickListener LButton = new View.OnClickListener() {
				public void onClick(View v) {
					Result = Test.FAILED;
					recorder.stop();
					recorder.reset();

					recorder.release();
					recorder = null;

					Exit();
				}
			};

			View.OnClickListener RButton = new View.OnClickListener() {
				public void onClick(View v) {
					mState++;
					recorder.stop();
					recorder.reset();

					recorder.release();
					recorder = null;
					Run();

				}
			};
			TestLayout1 t2 = new TestLayout1(mContext, mName,
					"Start to record sound.pls speak loudly", "Fail",
					"Stop and play", LButton, RButton);
			mContext.setContentView(t2.ll);

			break;
		case INIT + 1:

			mMediaPlayer.setVolume(11, 11);
			AudioManager am = (AudioManager) mContext
					.getSystemService(Context.AUDIO_SERVICE);

			am.setSpeakerphoneOn(true);
			am.setWiredHeadsetOn(false);

			// SystemClock.sleep(1000);

			try {
				mMediaPlayer.setDataSource(mSampleFile.getAbsolutePath());
				startMelody(mMediaPlayer);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			TestLayout1 tl = new TestLayout1(mContext, mName,
					"Can you hear what you said?", "Fail", "Pass");
			mContext.setContentView(tl.ll);
			mState++;

			break;

		case END:
			// Now the object cannot be reused
			mMediaPlayer.stop();
			mMediaPlayer.reset();
			mMediaPlayer.release();
			// delete();

			break;
		default:
		}
	}

}
