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

class FMRadioTest extends Test {
	public static final String TAG = "FMRadioTest";

	private MediaPlayer mMP = null;
	private AudioManager mAM = null;
	private int FIXED_STATION_FREQ = 971; // 971 * 100k Hz
	private String FIXED_STATION_FREQ_STR = "97.1";

	// Audio Manager parameters
	private String AUDIO_PATH_LOUDSPEAKER = "AudioSetForceToSpeaker=1";
	private String AUDIO_PATH_EARPHONE = "AudioSetForceToSpeaker=0";
	private String FM_AUDIO_ENABLE = "AudioSetFmEnable=1";
	private String FM_AUDIO_DISABLE = "AudioSetFmEnable=0";
	private TestLayout1 tl;

	FMRadioTest(ID pid, String s) {
		super(pid, s);
	}

	FMRadioTest(ID pid, String s, int timein) {
		super(pid, s, timein, 0);
	}

	@Override
	protected void Run() {
		boolean bRet = false;
		if (null == mContext) {
			Log.v(TAG, "mContext is null");
		} else {

			// this function executes the test
			switch (mState) {
			case INIT:
				mTimeIn.start();

				mContext.setVolumeControlStream(AudioManager.STREAM_MUSIC);
				mAM = (AudioManager) mContext
						.getSystemService(Context.AUDIO_SERVICE);

				mMP = new MediaPlayer();
				try {
					mMP.setDataSource("MEDIATEK://MEDIAPLAYER_PLAYERTYPE_FM");
				} catch (IOException ex) {
					// TODO: notify the user why the file couldn't be opened
					Log.e(TAG, "setDataSource: " + ex);
					// return;
				} catch (IllegalArgumentException ex) {
					// TODO: notify the user why the file couldn't be opened
					Log.e(TAG, "setDataSource: " + ex);
					// return;
				} catch (IllegalStateException ex) {
					Log.e(TAG, "setDataSource: " + ex);
					// return;
				}
				mMP.setAudioStreamType(AudioManager.STREAM_MUSIC);

				bRet = FmNative.openDev();
				Log.i(TAG, "--- FmNative.opendev: " + bRet);

				FmNative.setMute(true);
				FmNative.powerUp((float) FIXED_STATION_FREQ / 10);

				try {
					mMP.prepare();
				} catch (Exception e) {
					Log.e(TAG, "Exception: Cannot call MediaPlayer prepare.");
				}
				mMP.start();

				FmNative.setMute(false);
				// mAM.setParameters(AUDIO_PATH_LOUDSPEAKER);
				// mAM.setParameters(AUDIO_PATH_EARPHONE);

				tl = new TestLayout1(mContext, mName, "FM Freq is "
						+ FIXED_STATION_FREQ_STR);
				if (!mTimeIn.isFinished())
					tl.setEnabledButtons(false);

				mContext.setContentView(tl.ll);

				mState++;
				break;
			case END:
				FmNative.setMute(true);
				// mAM.setParameters(FM_AUDIO_DISABLE);
				mMP.stop();
				mMP.release();
				mMP = null;

				FmNative.powerDown(0);
				FmNative.closeDev();
				// Exit();
				break;
			default:
				break;
			}
		}

	}

	@Override
	protected void onTimeInFinished() {
		Log.d(TAG, "update buttons");
		if (tl != null)
			tl.setEnabledButtons(true);
	}

}
