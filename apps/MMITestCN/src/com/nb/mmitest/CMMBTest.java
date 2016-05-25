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

/*
 * Empty Test use as a default when no test was defined
 */
class CMMBTest extends Test {
	TelephonyManager tm = null;
	private WXKJRapi remote;
	private int mOprtMode;
	private boolean mIsFTMmode = true;
	private boolean isCMMBAppOpened = false;
	private Thread mThread;
	String mDisplayCMMBTestString = "";

	CMMBTest(ID pid, String s) {
		super(pid, s);
		hKey = new KeyHandler() {
			public boolean handleKey(KeyEvent event) {
				return true;
			}

		};
	}

	@Override
	protected void Run() {
		//tm = (TelephonyManager) mContext
		//		.getSystemService(Context.TELEPHONY_SERVICE);

		switch (mState) {
		case INIT:
			//Intent intent = new Intent("com.ben.MBBMSService");
			//mContext.startService(intent);
			if (!isCMMBAppOpened) {
				mState++;
				isCMMBAppOpened = true;
				TestLayout1 tl = null;
				
				mDisplayCMMBTestString  = "Start CMMB app...";
				tl = new TestLayout1(mContext, mName, mDisplayCMMBTestString);
				tl.setEnabledButtons(false, tl.brsk);
				mContext.setContentView(tl.ll);
				
				SetTimer(500, new CallBack() {
					public void c() {
						Intent Execution = new Intent(Intent.ACTION_MAIN, null);
						Execution.setClassName("com.mediatek.cmmb.app",
								"com.mediatek.cmmb.app.FtmActivity");
						mContext.startActivity(Execution);
					}
				});
				break;
			}
		case INIT + 1:
			TestLayout1 tl = null;
			tl = new TestLayout1(mContext, mName, "CMMB test");
			mContext.setContentView(tl.ll);
			mState++;
			isCMMBAppOpened = false;
			if (false && MMITest.mgMode == MMITest.AUTO_MODE) {
				SetTimer(1000, new CallBack() {
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
			break;
		default:
			break;
		}
	}
}
