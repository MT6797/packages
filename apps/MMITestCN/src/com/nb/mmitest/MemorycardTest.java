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
import android.os.storage.StorageManager;
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

import java.io.OutputStream;
import android.content.Context;
import com.nb.mmitest.R;
class MemorycardTest extends Test {

	private String TAG = "MMITEST:SDcard Test";
	private boolean mSdInserted = false;
	String state;
	StorageManager storagemanager = null;

	private final int SD_INSERT = INIT + 1;
	private final int SD_CHECK = INIT + 2;
	private final int SD_TIMEOUT = INIT + 3;

	SdPlugReceiver mMediaActionReceiver = new SdPlugReceiver();

	MemorycardTest(ID pid, String s) {
		super(pid, s);
	}

	File SdRootDir;

	TestLayout1 tl;

	@Override
	protected void Run() {
		// this function executes the test
		StopTimer();
		switch (mState) {
		case INIT: // check if sd card is present
			IntentFilter intentFilter = new IntentFilter(
					Intent.ACTION_MEDIA_MOUNTED);
			intentFilter.addAction(Intent.ACTION_MEDIA_BAD_REMOVAL);
			intentFilter.addAction(Intent.ACTION_MEDIA_REMOVED);
			intentFilter.addAction(Intent.ACTION_MEDIA_EJECT);
			intentFilter.addDataScheme("file");

			mContext.registerReceiver(mMediaActionReceiver, intentFilter);
			// mReceiver.onReceive(mContext,mContext.getIntent());

			SdRootDir = new File("/sdcard");					
			
			String mountPoint = "/mnt/m_external_sd";
			
			if (storagemanager == null)
			 storagemanager=(StorageManager)mContext.getSystemService(Context.STORAGE_SERVICE);
							
			state = storagemanager.getVolumeState(mountPoint);
			
			if (storagemanager.getVolumeState(mountPoint).equals(
					Environment.MEDIA_MOUNTED)) {
			
				mSdInserted = true;
				mState = SD_CHECK;
			} else {
				mSdInserted = false;
				Log.d(TAG,
						"storage state :"
								+ storagemanager.getVolumeState(mountPoint));
			}

			if (mSdInserted) {
				tl = new TestLayout1(mContext, mName, getResource(R.string.sdcard_ok));
				// TODO: in case of a bad removal, the directory is still
				// mounted
				// while the SD is removed, how to test this?

				// goto next step
				//SetTimer(500, new CallBack() {
				//	public void c() {
				//		Run();
				//	}
				//});
				if (MMITest.mgMode == MMITest.AUTO_MODE) {
				tl.setEnabledButtons(false);
				}
				if (MMITest.mgMode == MMITest.AUTO_MODE) {
					SetTimer(1500, new CallBack() {
						public void c() {
							Result = Test.PASSED;
							if (mState != END) {
								mState = END;
								Run();
							}
							mContext.setResult(Activity.RESULT_OK);
							mContext.finish();
						}
					});
				}
			} else {
				tl = new TestLayout1(mContext, mName, getResource(R.string.sdcard_miss));
				tl.setEnabledButtons(false, tl.brsk);
				/*
				 * SetTimer(15000, new CallBack() { public void c() { mState =
				 * SD_TIMEOUT; Run(); } });
				 */
			}

			break;
		case SD_CHECK:// write and read file to MMC
			try {
				// File f = new File("/sdcard/sdtest.txt");
				File f = new File(mContext.getExternalFilesDir(null), "sdtest.txt");
				FileWriter fw = new FileWriter(f);
				String test = "SD est tu la?";
				char[] readbuf = new char[20];

				fw.write(test);
				fw.close();

				if (!f.exists()) {
					Log.d(TAG, f + " does not exist");
				}
				if (!f.canWrite())
					Log.d(TAG, f + " can't be written");

				FileReader fr = new FileReader(f);
				int nbread = fr.read(readbuf);
				if (nbread == 0) {
					Log.d(TAG, "nothing to read in sdtest");
				}
				fr.close();

				// Attempt to delete it
				boolean success = f.delete();
				if (!success)
					Log.d(TAG, f + " deletion failed");

				String read = new String(readbuf, 0, nbread);
				if (read.equals(test)) {
					tl = new TestLayout1(mContext, mName, getResource(R.string.sdcard_ok));
				} else {
					tl = new TestLayout1(mContext, mName, getResource(R.string.sdcard_no));
				}
			} catch (IOException e) {
				Log.d(TAG, "SD access error :" + e);
				// tl = new
				// TestLayout1(mContext,mName,"R/W SD failed\n check SD is correctly inserted");
			}
			break;
		case SD_TIMEOUT://
			tl = new TestLayout1(mContext, mName, getResource(R.string.sdcard_timeout));
			// mContext.unregisterReceiver(mMediaActionReceiver);

			break;

		case END:
			mContext.unregisterReceiver(mMediaActionReceiver);

			break;
		}
		mContext.setContentView(tl.ll);

	}

	class SdPlugReceiver extends BroadcastReceiver {

		private String mVolume;

		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			Log.d(TAG, "received :" + action);

			if (action.equals(Intent.ACTION_MEDIA_MOUNTED)) {
				mVolume = (intent.getDataString());
				if (mVolume.matches(".*sdcard")) {
					Log.d(TAG, "SD inserted");
					mState = SD_CHECK;
					Run();
				} else {
					Log.d(TAG, "unknown media inserted :" + mVolume);
				}
			} else if (action.equals(Intent.ACTION_MEDIA_EJECT)
					|| action.equals(Intent.ACTION_MEDIA_BAD_REMOVAL)
					|| action.equals(Intent.ACTION_MEDIA_REMOVED)) {
				Log.d(TAG, intent.getDataString() + " removed");
			}

		}
	}

}
