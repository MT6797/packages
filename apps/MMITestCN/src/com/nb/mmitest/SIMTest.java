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

import android.os.SystemProperties;
import com.nb.mmitest.R;
/*
 * Empty Test use as a default when no test was defined
 */
class SIMTest extends Test {
	TelephonyManager tm = null;
	private WXKJRapi remote;

	private int mOprtMode;

	private boolean mIsFTMmode = true;

	private Thread mThread;

	private boolean FEATURE_MODE_CHANGE = false;
	String mDisplaySIMTestString = "";
	String mDisplaySIM1TestString = "";
	String mDisplaySIM2TestString = "";
        private static final int GEMINI_SIM_1 = 0;
        private static final int GEMINI_SIM_2 = 1;

	private void setOnlineModeThread() {
		mThread = new Thread() {
			public void run() {

				remote.setOnlineMode();

				try {
					sleep(500);
				} catch (InterruptedException e) {
					Log.e(TAG, "exception :" + e);
				}

				mOprtMode = remote.getOprtMode();
			}
		};
		mThread.start();
	}

	SIMTest(ID pid, String s) {
		super(pid, s);
		//hKey = new KeyHandler() {
		//	public boolean handleKey(KeyEvent event) {
		//		return true;
		//	}

		//};
	}

	@Override
	protected void Run() {
		tm = (TelephonyManager) mContext
				.getSystemService(Context.TELEPHONY_SERVICE);

		// this function executes the test
		switch (mState) {
		case INIT: // start the vibrator and display the screen
			// Run every seconds

			if (FEATURE_MODE_CHANGE) {
				try {
					remote = new WXKJRapi(mContext);
				} catch (IOException e) {
					Log.e(TAG, "WXKJRapi() error" + e);
				}

				mOprtMode = remote.getOprtMode();
				if (WXKJRapi.OprtMode.SYS_OPRT_MODE_FTM.getVal() == mOprtMode) {
					TestLayout1 tl = new TestLayout1(mContext, mName,
							"changing to ONLINE mode ");
					tl.hideButtons();
					mContext.setContentView(tl.ll);

					setOnlineModeThread();

				} else {
					TestLayout1 tl = new TestLayout1(mContext, mName,
							"phone is ONLINE");
					tl.hideButtons();
					mContext.setContentView(tl.ll);
				}

				SetTimer(500, new CallBack() {
					public void c() {
						int count = 0;
						if (mThread != null)
							while (mThread.isAlive() && count < 50) {
								count++;
								SystemClock.sleep(100);
							}

						if (WXKJRapi.OprtMode.SYS_OPRT_MODE_ONLINE.getVal() != mOprtMode) {
							setOnlineModeThread();

							count = 0;
							if (mThread != null)
								while (mThread.isAlive() && count < 50) {
									count++;
									SystemClock.sleep(100);
								}
						}
						mState++;
						Run();

					}
				});

				break;
			}
		case INIT + 1:
			Log.d(TAG,
					"current oprt_mode = "
							+ WXKJRapi.OprtMode.valToString(mOprtMode));

			TestLayout1 tl = null;
			boolean bSIMCardOk = false;
			if (SystemProperties.get("ro.mtk_gemini_support").equals("1") == true) {
				boolean sim1State = tm.hasIccCard(GEMINI_SIM_1);
				boolean sim2State = tm.hasIccCard(GEMINI_SIM_2);
				if (sim1State == true)
					mDisplaySIM1TestString = getResource(R.string.sim1_ok);
				else
					mDisplaySIM1TestString = getResource(R.string.sim1_miss);

				if (sim2State == true)
					mDisplaySIM2TestString = getResource(R.string.sim2_ok);
				else
					mDisplaySIM2TestString = getResource(R.string.sim2_miss);

				mDisplaySIMTestString = mDisplaySIM1TestString + "\n"
						+ mDisplaySIM2TestString;
				if (sim1State && sim2State)
					bSIMCardOk = true;
			} else {
				boolean simState = tm.hasIccCard();
				if (simState == true)
					mDisplaySIMTestString = getResource(R.string.sim_ok);
				else
					mDisplaySIMTestString = getResource(R.string.sim_miss);
				if (simState == true)
					bSIMCardOk = true;
			}

			tl = new TestLayout1(mContext, mName, mDisplaySIMTestString);
			
			if (bSIMCardOk && MMITest.mgMode == MMITest.AUTO_MODE) {
				tl.setEnabledButtons(false);
			}
			else if(bSIMCardOk == false)
				tl.setEnabledButtons(false, tl.brsk);
			
			mContext.setContentView(tl.ll);
			mState++;
			if (bSIMCardOk && MMITest.mgMode == MMITest.AUTO_MODE) {
				SetTimer(1500, new CallBack() {
					public void c() {
						mState = END;
						//ExecuteTest.currentTest.Result = Test.PASSED;
						Result = Test.PASSED;
						mContext.setResult(Activity.RESULT_OK);
						mContext.finish();
					}
				});
			}
			break;

		case END:
			if (FEATURE_MODE_CHANGE) {
				tl = new TestLayout1(mContext, mName, "test finished");
				mContext.setContentView(tl.ll);

				if (remote != null)
					if (WXKJRapi.OprtMode.SYS_OPRT_MODE_FTM.getVal() != mOprtMode) {
						remote.setFTMMode();
						SystemClock.sleep(500);
						mOprtMode = remote.getOprtMode();
						Log.d(TAG,
								"current oprt_mode = "
										+ WXKJRapi.OprtMode
												.valToString(mOprtMode));
					}
			}
			break;
		default:
			break;
		}
	}
}
