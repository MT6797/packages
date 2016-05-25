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
//import com.mediatek.common.featureoption.FeatureOption;

import android.media.AudioSystem;
import com.nb.mmitest.R;
import android.media.MediaRecorder;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import android.os.storage.StorageManager;
class MicMainTest extends Test {

	private TestLayout1 tl = null;

	private boolean mLoopOn = false;

	private MediaPlayer mMediaPlayer = null;

	private AudioManager am;
	//add by liliang.bao for main mic test
	private final int AUDIO_USER_TEST = 0x90;
	private final int LOOPBACK_OUTPUT_RECEIVER_ON = 0x18;
	private final int LOOPBACK_OUTPUT_RECEIVER_OFF = 0x19;
	private final int LOOPBACK_OUTPUT_EARPHONE_ON = 0x20;
	private final int LOOPBACK_OUTPUT_SPEAKER_ON = 0x22;
	//add by liliang.bao end
	
	MediaRecorder mr;
	File mFile;
	boolean gRecordFlag = false;
	MicMainTest(ID pid, String s) {
		this(pid, s, 0);
	}

	MicMainTest(ID pid, String s, int timein) {
		super(pid, s, timein, 0);
		/*
		hKey = new KeyHandler() {
			public boolean handleKey(KeyEvent event) {
				return true;
			}

		};
		*/
	}	
    private Button btnPcba;
	private void setPCBAButton(){
		btnPcba = new Button(mContext);
		btnPcba.setText(getResource(R.string.start_record));
		btnPcba.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				if(!gRecordFlag)
					startRecord();
				else
					playRecord();
			}
		});
		this.tl.ll.addView(btnPcba, 1);
	}

	private void startRecord()
	{
		
		      mFile = new File("/sdcard/test.3gpp");
				try {
					Log.d(TAG, " file path:"+mFile.getAbsolutePath());
					boolean result = mFile.createNewFile();
					
					mr = new MediaRecorder();
					mr.setAudioSource(MediaRecorder.AudioSource.MIC);
					mr.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
					mr.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
					mr.setOutputFile(mFile.getAbsolutePath());
					mr.prepare();
					mr.start();
					Log.d(TAG, "create file: "+result+" file path:"+mFile.getAbsolutePath());
					btnPcba.setText(getResource(R.string.stop_record));
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					Log.d(TAG, "startRecord: "+e.toString());
				}
				
				gRecordFlag = true;
	}
	private void playRecord()
	{
		Log.d(TAG, "playRecord : "+" mr: "+mr);
		gRecordFlag = false;
		try {
			mr.stop();
        } catch (RuntimeException e) {
        	
        	Log.d(TAG, "stopRecord: "+e.toString());
        } finally {
        	mr.reset();
    		mr.release();
    		mr = null;
    		
        }
		

		
		am = (AudioManager) mContext
				.getSystemService(Context.AUDIO_SERVICE);

		mMediaPlayer = new MediaPlayer();

		am.setMode(AudioSystem.MODE_NORMAL);
		//am.setStreamVolume(AudioManager.STREAM_MUSIC,
		//am.getStreamMaxVolume(AudioManager.STREAM_MUSIC), 0);
		
		am.setSpeakerphoneOn(true);
		am.setWiredHeadsetOn(false);
		btnPcba.setText(getResource(R.string.start_record));
		try {
			
			mMediaPlayer.setDataSource(mFile.getPath());
			mMediaPlayer.prepare();
			mMediaPlayer.start();

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			Log.d(TAG, "playRecord: "+e.toString());
		}
	}
	@Override
	protected void Run() {
		// this function executes the test
		switch (mState) {
		case INIT:
			Log.d(TAG, "@@@@@@@@@@@@@@@@@@@@@@@Main mic INIT");

			am = (AudioManager) mContext
					.getSystemService(Context.AUDIO_SERVICE);
			tl = new TestLayout1(mContext, mName, getResource(R.string.loop_mic));
			mContext.setContentView(tl.ll);
			setPCBAButton();
			mState++;

			break;

		case END:
		default:
			Log.d(TAG, "@@@@@@@@@@@@@@@@@@@@@@@Main mic END");
			if (tl != null)
				tl.setEnabledButtons(false);
			if(mMediaPlayer!=null)
					mMediaPlayer.stop();
			if(mr!=null)
				mr.stop();
			gRecordFlag = false;
			am.setSpeakerphoneOn(false);
			if(mFile!=null&&mFile.exists())
				mFile.delete();
				
/*
			if (mLoopOn) {
				WXKJClient mOffClient = null;
				mOffClient = new WXKJClient();
				if (!mOffClient.connect()) {
					Log.e(TAG, "Connecting MMI proxy fail!");
				} else {
				    //SystemClock.sleep(100);
				    if (!mOffClient.sendCommand(WXKJClient.MMICMD_AUDIOLOOP_OFF)) {
					    Log.e(TAG, "Send command to MMI proxy fail!");
					    mOffClient.disconnect();
				    } else {
				    
				        //SystemClock.sleep(500);
				        boolean success2 = mOffClient.readReply();
					    Log.d(TAG, "Main Mic readReply off " + success2);
				        mOffClient.disconnect();
					    mOffClient = null;
					    mLoopOn = false;
				    }
			    }
			}
			Log.d(TAG, "@@@@@@@@@@@@@@@@@@@@@@@Main mic END1");
*/
			
			
			break;
		}
	}

	@Override
	protected void onTimeInFinished() {
	    if (tl != null)
			tl.showButtons();
	}

}
