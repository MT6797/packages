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
import com.nb.mmitest.R;
interface CallBack {
	public void c();
}

interface TestHandler {
	public boolean h(Test t);
}

interface KeyHandler {
	public boolean handleKey(KeyEvent msg);
}

interface TouchHandler {
       public boolean handleTouch(MotionEvent msg);
 }

interface ConfigChangedHandler {
	public void h(Configuration c);
}

class Lcd {
	// methods and attributes for Singleton pattern
	private Lcd() {
		//20121123 ying.pang for bug 359585
		Width = 720;
		Height = 1184;
	}

	private int Width, Height;

	static private Lcd _instance = new Lcd();

	static public void setSize(int w, int h) {
		_instance.Width = w;
		_instance.Height = h;
	}

	static public int width() {
		return _instance.Width;
	}

	static public int height() {
		return _instance.Height;
	}

	// methods and attributes for global data
}

public abstract class Test {

	Test(ID pid, String s, int tin, int tout) {
		this(pid, s);

		mTimeOut = new TimeCheck(tout);
		mTimeIn = new TimeCheck(tin);

	}

	Test(ID pid, String s) {
		mId = pid.ordinal();
		mName = s;
		Result = NOT_TESTED;

		/* increment the total count */
		mTestCount++;
		/* default values */
		mTimeOut = new TimeCheck(0);
		mTimeIn = new TimeCheck(0);

	}

	// globalVariables
	// public static int LcdWidth = 240;

	// public static int LcdHeight = 320;

	// constants
	public enum ID {
		EMPTY, 
		LCD_MIRERGB, 
		LCD_BLACK, 
		LCD_CHECKER, 
		LCD_GREYCHART,
		LCD_LEVEL,//add by xianfeng.xu for CR364979
		LCD_WHITE, 
		LCD_MENU, 
		LCD_MACBETH, 
		KEYPAD, 
		KEYPAD_INIT, 
		CAMERA_INIT, 
		CAMERA_IMG,
		CAMERA_IMG_FRONT,
		CAMERA_ZOOM, 
		CAMERA_LED,
		CHARGER_LED,//add by xianfeng.xu for CR364979
		BACKLIGHT, 
		KBD_BACKLIGHT, 
		SKBD_BACKLIGHT, 
		VIB, 
		HALL,
		FP,
		SLIDE,
		CMMB,
		TP1,
		TP2,
		SIM, 
		CHARGER_PRES, 
		CHARGER_MISS, 
		HEADSET_IN, 
		HEADSET_LEFT, 
		HEADSET_RIGHT, 
		HEADSET_OUT, 
		MELODY, 
		RECEIVER,
		SPEAKER,
		MICMAIN,
		MICSUB,
		MIC,
		BT, 
		WIFI,
		MEMORYCARD, 
		MEMORYCARD_RW, 
		OTG,
		USB,
		MHL,
		MISC,
		TEMPBAT,
		TRACABILITY,
		COMPASS,
		GYROSCOPE,
		GSENSOR,
		LIGHTSENSOR,
		GPS,
		TS_CALIBRATION,
		EMERGENCY_CALL,
		HEADSET,
		NWSETTING,
		FMRADIO,
		ALSPS,
		NFC_ACTIVE,
    		NFC_PASSIVE,
		MAX_ITEMS,
		
	};

	public final int INIT = 0;
	public final int TIMEOUT = 0xFFFE;

	public final int END = 0xFFFF;

	// test name
	protected String mName;

	// test id
	protected int mId;

	// position in the Autotest list
	// this is valid only if we are in AutoTest mode
	public int mPosition;
	public boolean passFlag=false;

	// the total number of test
	// incremented every time a new test is created
	static protected int mTestCount = 0;

	// time check on the test
	protected TimeCheck mTimeOut, mTimeIn;

	private CountDownTimer mTimer;

	// Activity context (should be ExecuteTest) : activity host of the Test
	protected Activity mContext;

	// current state of the test execution
	public int mState;
	public int mNextState;

	// test status
	public final static int NOT_TESTED = 0;

	public final static int PASSED = 1;

	public final static int FAILED = 2;

	protected static final String TAG = "MMITEST";

	protected int Result; /* NOT_TESTED, PASSED , FAILED */

	public int getResult() {
		return Result;
	}

	public String output;

	/* test execution steps */
	public synchronized void Create(Activity a) {
		mContext = a;
		Init();
	}

	public synchronized void Start() {

		mState = INIT;
		Run();

	}

	// show the last step of the test
	public synchronized void Stop() {
		if (mState != END) {
			mState = END;
			Run();
		} else {
			Log.w(TAG, "call Test.Stop while State == END");
		}
	}

	public synchronized void Exit() {
		Stop();
		mContext.setResult(Activity.RESULT_OK);
		mContext.finish();

	}
       
	protected abstract void Run();
	protected void Init(){};
        
        TouchHandler hTouch;
        
	public void onResume() {
	};

	/*
	 * default Key Handler for the test
	 */

	KeyHandler hKey = null;

	/*
	 * default Config Change Handler for the test
	 */

	protected ConfigChangedHandler hConfigChanged = null;

	void onConfigurationChanged(Configuration c) {
		if (hConfigChanged != null) {
			hConfigChanged.h(c);
		}
	}

	/*
	 * timeout in milliseconds
	 */
	public void SetTimer(long t, final CallBack cb) {
		mTimer = new CountDownTimer(t, 1000) {
			public void onTick(long l) {
			}

			public void onFinish() {
				cb.c();
			}
		}.start();
	}

	public void StopTimer() {
		if (mTimer != null) {
			mTimer.cancel();
		}
	}

	public String toString() {
		return mName;
	}

	public int getId() {
		return mId;
	}

	// protected abstract void onTimeInFinished();
	protected void onTimeInFinished() {
		Log.d(TAG, "Test:onTimeInFinished()");
	}
	
	/* for auto run test feature  */
	private Handler hdlAutoRunDelay = new Handler();
	protected void AutoRunCallback(){}
	protected final void AutoRun(){
		hdlAutoRunDelay.postDelayed(new Runnable(){
			public void run(){
				AutoRunCallback();
			}
		}, 1000);
	}
	
	class TimeCheck {

		private CountDownTimer mTimer;

		public final int INIT = 0;
		public final int RUNNING = 1;
		public final int CANCELED = 2;
		public final int FINISHED = 3;

		private int mState = INIT;

		private int mTimeElapsed = 0;

		private int mTimeSec;

		private CallBack mCallback;

		TimeCheck(int timesec, CallBack cb) {
			this(timesec);
			mCallback = cb;
		}

		TimeCheck(int timesec) {

			mTimeSec = timesec;

			mTimer = new CountDownTimer(timesec, 1000) {
				public void onFinish() {
					if (mTimeSec != 0) {
						/*
						 * in case timeout = 0 we don't want to trigger any
						 * callbacks
						 */
						if (mCallback != null)
							mCallback.c();

						onTimeInFinished();
					}

					mState = FINISHED;
				}

				public void onTick(long l) {
					mTimeElapsed += 1000;
				}
			};
		}

		public void start() {
			mState = RUNNING;
			mTimeElapsed = 0;
			mTimer.start();
		}

		private void onFinish() {
			mState = FINISHED;
		}

		public void cancel() {
			mTimer.cancel();
			mState = CANCELED;
		}

		public int getState() {
			return mState;
		}

		public boolean isFinished() {
			return (mState == FINISHED);
		}

		public int getTime() {
			return mTimeElapsed;
		}
	}
	public String getResource(int id)
	{
		return mContext.getResources().getString(id);
	}
	
	public String getResource(int id,String format)
	{
		return mContext.getResources().getString(id,format);
	}
}

class TestLayout1 {

	private Activity mActivity;

	public LinearLayout ll;// the base frame

	private TextView tvtitle;
	private TextView tvbody;
	private LinearLayout llsk;// the softkey frame

	Button blsk = null;
	Button bmsk = null;
	Button brsk = null;

	private boolean middleSkExist = false;
	public TextView getBody()
	{
		return tvbody;
	}

	private void setTitleView(String title) {

		tvtitle = new TextView(mActivity);
		tvtitle.setGravity(Gravity.CENTER);
		tvtitle.setTypeface(Typeface.MONOSPACE, 1);
		// tvtitle.setTextAppearance(mActivity,
		// android.R.style.TextAppearance_Large);
		tvtitle.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
		tvtitle.setTextSize(20);
		tvtitle.setText(title);

	}

	private void setDefaultSoftkeys(String lsk, String msk, String rsk) {

		if (msk != null) {
			bmsk = new Button(mActivity);
			bmsk.setText(msk);
			bmsk.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					// MSK default behavior is to restart the current test
					ExecuteTest.currentTest.Start();
				}
			});
		}

		if (lsk != null) {
			blsk = new Button(mActivity);
			blsk.setText(lsk);
			// LSK default behavior is to FAIL the current test
			blsk.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					ExecuteTest.currentTest.Result = Test.FAILED;
					ExecuteTest.currentTest.Exit();

				}
			});
		}

		if (rsk != null) {
			brsk = new Button(mActivity);
			brsk.setText(rsk);

			// RSK default behavior is to PASS the current test
			brsk.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					ExecuteTest.currentTest.Result = Test.PASSED;
					ExecuteTest.currentTest.Exit();
				}
			});
		}

	}

	TestLayout1(Activity a, String title, String body, String lsk, String msk,
			String rsk, View.OnClickListener lskcb, View.OnClickListener mskcb,
			View.OnClickListener rskcb) {
		this(a, title, body, lsk, msk, rsk);

		blsk.setOnClickListener(lskcb);
		brsk.setOnClickListener(rskcb);
		bmsk.setOnClickListener(mskcb);

	}

	TestLayout1(Activity a, String title, String body, String lsk, String rsk,
			View.OnClickListener lskcb, View.OnClickListener rskcb) {
		this(a, title, body, lsk, null, rsk);

		blsk.setOnClickListener(lskcb);
		brsk.setOnClickListener(rskcb);
	}

	TestLayout1(Activity a, String title, String body, String lsk, String rsk) {
		this(a, title, body, lsk, null, rsk);
	}

	TestLayout1(Activity a, String title, String body) {
		this(a, title, body, a.getResources().getString(R.string.fail), null, a.getResources().getString(R.string.pass));
	}

	TestLayout1(Activity a, String title, String body, String lsk, String msk,
			String rsk) {

		mActivity = a;

		ll = new LinearLayout(mActivity);
		// ll.setLayoutParams(new
		// LinearLayout.LayoutParams(LayoutParams.FILL_PARENT,LayoutParams.FILL_PARENT));
		ll.setOrientation(LinearLayout.VERTICAL);

		llsk = new LinearLayout(mActivity);
		llsk.setOrientation(LinearLayout.HORIZONTAL);
		if (false) {
			tvtitle = new TextView(mActivity);
			tvtitle.setGravity(Gravity.CENTER);
			tvtitle.setTypeface(Typeface.MONOSPACE, 1);
			// tvtitle.setTextAppearance(mActivity,
			// android.R.style.TextAppearance_Large);
			tvtitle.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
			tvtitle.setTextSize(20);
			tvtitle.setText(title);
		} else {
			setTitleView(title);
		}
		 tvbody = new TextView(mActivity);
		//tvbody.setGravity(Gravity.CENTER);//jiqian.shi modify for display
		// tvbody.setTypeface(Typeface.MONOSPACE, 1);
		tvbody.setTextAppearance(mActivity,
				android.R.style.TextAppearance_Large);
		tvbody.setText(body);

		if (false) {
			blsk = new Button(mActivity);
			brsk = new Button(mActivity);
			blsk.setText(getResource(R.string.fail));
			brsk.setText(getResource(R.string.pass));

			// TODO press OK or FAILED should have different behavior
			blsk.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					ExecuteTest.currentTest.Result = Test.FAILED;
					ExecuteTest.currentTest.Exit();

				}
			});

			brsk.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					ExecuteTest.currentTest.Result = Test.PASSED;
					ExecuteTest.currentTest.Exit();
				}
			});
		} else {
			setDefaultSoftkeys(lsk, msk, rsk);
		}

		// create sub linear layout for the buttons and add the buttons to it
		LinearLayout.LayoutParams llsklp = new LinearLayout.LayoutParams(
				LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT, 1);

		llsk.addView(blsk, llsklp);
		/* add the extra center button this one only can be null */
		if (bmsk != null) {
			llsk.addView(bmsk, llsklp);
		}
		llsk.addView(brsk, llsklp);

		llsk.setGravity(Gravity.CENTER);

		// add everything to layout with accurate weights
		ll.addView(tvtitle, new LinearLayout.LayoutParams(
				LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT, 3));
		ll.addView(tvbody, new LinearLayout.LayoutParams(
				LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT, 1));
		ll.addView(llsk, new LinearLayout.LayoutParams(
				LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT, 3));

	}

	// add customer view
	TestLayout1(Activity a, String title, View body) {

		mActivity = a;

		ll = new LinearLayout(mActivity);
		// ll.setLayoutParams(new
		// LinearLayout.LayoutParams(LayoutParams.FILL_PARENT,LayoutParams.FILL_PARENT));
		ll.setOrientation(LinearLayout.VERTICAL);

		llsk = new LinearLayout(mActivity);
		llsk.setOrientation(LinearLayout.HORIZONTAL);

		if (false) {
			tvtitle = new TextView(mActivity);
			tvtitle.setGravity(Gravity.CENTER);
			tvtitle.setTypeface(Typeface.MONOSPACE, 1);
			// tvtitle.setTextAppearance(mActivity,
			// android.R.style.TextAppearance_Large);
			tvtitle.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
			tvtitle.setTextSize(20);
			tvtitle.setText(title);
		} else {
			setTitleView(title);
		}

		View vbody = body;

		blsk = new Button(mActivity);
		brsk = new Button(mActivity);
		blsk.setText(getResource(R.string.fail));
		brsk.setText(getResource(R.string.pass));

		// create sub linear layout for the buttons and add the buttons to it
		LinearLayout.LayoutParams llsklp = new LinearLayout.LayoutParams(
				LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT, 1);
		llsk.addView(blsk, llsklp);
		llsk.addView(brsk, llsklp);
		llsk.setGravity(Gravity.CENTER);

		// add everything to layout with accurate weights
		ll.addView(tvtitle, new LinearLayout.LayoutParams(
				LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT, 3));
		ll.addView(vbody, new LinearLayout.LayoutParams(
				LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT, 1));
		ll.addView(llsk, new LinearLayout.LayoutParams(
				LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT, 3));

		// TODO press OK or FAILED should have different behavior
		blsk.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				ExecuteTest.currentTest.Result = Test.FAILED;
				ExecuteTest.currentTest.Exit();
			}
		});

		brsk.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				ExecuteTest.currentTest.Result = Test.PASSED;
				ExecuteTest.currentTest.Exit();
			}
		});

	}

	public void hideButtons() {
		if (brsk != null)
			brsk.setVisibility(View.GONE);
		if (blsk != null)
			blsk.setVisibility(View.GONE);
		if (bmsk != null)
			bmsk.setVisibility(View.GONE);
	}

	public void showButtons() {
		if (brsk != null)
			brsk.setVisibility(View.VISIBLE);
		if (blsk != null)
			blsk.setVisibility(View.VISIBLE);
		if (bmsk != null)
			bmsk.setVisibility(View.VISIBLE);
	}

	public void setEnabledButtons(boolean en) {
		if (brsk != null)
			brsk.setEnabled(en);
		if (blsk != null)
			blsk.setEnabled(en);
		if (bmsk != null)
			bmsk.setEnabled(en);
	}

	public void setEnabledButtons(boolean en, Button b) {
		b.setEnabled(en);
	}
	
	public String getResource(int id)
	{
		return mActivity.getResources().getString(id);
	}
}

class TestLayoutWhiteTheme {

	private Activity mActivity;

	public LinearLayout ll;// the base frame

	private TextView tvtitle;
	private TextView tvbody;
	private LinearLayout llsk;// the softkey frame

	Button blsk = null;
	Button bmsk = null;
	Button brsk = null;

	private boolean middleSkExist = false;

	private void setTitleView(String title) {

		tvtitle = new TextView(mActivity);
		tvtitle.setGravity(Gravity.CENTER);
		tvtitle.setTypeface(Typeface.MONOSPACE, 1);
		// tvtitle.setTextAppearance(mActivity,
		// android.R.style.TextAppearance_Large);
		tvtitle.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
		tvtitle.setTextSize(20);
		tvtitle.setText(title);
		tvtitle.setTextColor(Color.BLACK);

	}

	private void setDefaultSoftkeys(String lsk, String msk, String rsk) {

		if (msk != null) {
			bmsk = new Button(mActivity);
			bmsk.setText(msk);
			bmsk.setTextColor(Color.BLACK);
			bmsk.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					// MSK default behavior is to restart the current test
					ExecuteTest.currentTest.Start();
				}
			});
		}

		if (lsk != null) {
			blsk = new Button(mActivity);
			blsk.setText(lsk);
			blsk.setTextColor(Color.BLACK);
			// LSK default behavior is to FAIL the current test
			blsk.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					ExecuteTest.currentTest.Result = Test.FAILED;
					ExecuteTest.currentTest.Exit();

				}
			});
		}

		if (rsk != null) {
			brsk = new Button(mActivity);
			brsk.setText(rsk);
			brsk.setTextColor(Color.BLACK);
			// RSK default behavior is to PASS the current test
			brsk.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					ExecuteTest.currentTest.Result = Test.PASSED;
					ExecuteTest.currentTest.Exit();
				}
			});
		}

	}

	TestLayoutWhiteTheme(Activity a, String title, String body, String lsk, String msk,
			String rsk, View.OnClickListener lskcb, View.OnClickListener mskcb,
			View.OnClickListener rskcb) {
		this(a, title, body, lsk, msk, rsk);

		blsk.setOnClickListener(lskcb);
		brsk.setOnClickListener(rskcb);
		bmsk.setOnClickListener(mskcb);

	}

	TestLayoutWhiteTheme(Activity a, String title, String body, String lsk, String rsk,
			View.OnClickListener lskcb, View.OnClickListener rskcb) {
		this(a, title, body, lsk, null, rsk);

		blsk.setOnClickListener(lskcb);
		brsk.setOnClickListener(rskcb);
	}

	TestLayoutWhiteTheme(Activity a, String title, String body, String lsk, String rsk) {
		this(a, title, body, lsk, null, rsk);
	}

	TestLayoutWhiteTheme(Activity a, String title, String body) {
		this(a, title, body, a.getResources().getString(R.string.fail), null, a.getResources().getString(R.string.pass));
	}

	TestLayoutWhiteTheme(Activity a, String title, String body, String lsk, String msk,
			String rsk) {

		mActivity = a;

		ll = new LinearLayout(mActivity);
		// ll.setLayoutParams(new
		// LinearLayout.LayoutParams(LayoutParams.FILL_PARENT,LayoutParams.FILL_PARENT));
		ll.setOrientation(LinearLayout.VERTICAL);
		ll.setBackgroundColor(Color.WHITE);

		llsk = new LinearLayout(mActivity);
		llsk.setOrientation(LinearLayout.HORIZONTAL);
		if (false) {
			tvtitle = new TextView(mActivity);
			tvtitle.setGravity(Gravity.CENTER);
			tvtitle.setTypeface(Typeface.MONOSPACE, 1);
			// tvtitle.setTextAppearance(mActivity,
			// android.R.style.TextAppearance_Large);
			tvtitle.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
			tvtitle.setTextSize(20);
			tvtitle.setText(title);
		} else {
			setTitleView(title);
		}
		 tvbody = new TextView(mActivity);
		//tvbody.setGravity(Gravity.CENTER);//jiqian.shi modify for display
		// tvbody.setTypeface(Typeface.MONOSPACE, 1);
		tvbody.setTextAppearance(mActivity,
				android.R.style.TextAppearance_Large);
		tvbody.setText(body);
		tvbody.setTextColor(Color.BLACK);

		if (false) {
			blsk = new Button(mActivity);
			brsk = new Button(mActivity);
			blsk.setText(getResource(R.string.fail));
			brsk.setText(getResource(R.string.pass));

			// TODO press OK or FAILED should have different behavior
			blsk.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					ExecuteTest.currentTest.Result = Test.FAILED;
					ExecuteTest.currentTest.Exit();

				}
			});

			brsk.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					ExecuteTest.currentTest.Result = Test.PASSED;
					ExecuteTest.currentTest.Exit();
				}
			});
		} else {
			setDefaultSoftkeys(lsk, msk, rsk);
		}

		// create sub linear layout for the buttons and add the buttons to it
		LinearLayout.LayoutParams llsklp = new LinearLayout.LayoutParams(
				LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT, 1);

		llsk.addView(blsk, llsklp);
		/* add the extra center button this one only can be null */
		if (bmsk != null) {
			llsk.addView(bmsk, llsklp);
		}
		llsk.addView(brsk, llsklp);

		llsk.setGravity(Gravity.CENTER);

		// add everything to layout with accurate weights
		ll.addView(tvtitle, new LinearLayout.LayoutParams(
				LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT, 3));
		ll.addView(tvbody, new LinearLayout.LayoutParams(
				LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT, 1));
		ll.addView(llsk, new LinearLayout.LayoutParams(
				LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT, 3));

	}

	// add customer view
	TestLayoutWhiteTheme(Activity a, String title, View body) {

		mActivity = a;

		ll = new LinearLayout(mActivity);
		// ll.setLayoutParams(new
		// LinearLayout.LayoutParams(LayoutParams.FILL_PARENT,LayoutParams.FILL_PARENT));
		ll.setOrientation(LinearLayout.VERTICAL);

		llsk = new LinearLayout(mActivity);
		llsk.setOrientation(LinearLayout.HORIZONTAL);

		if (false) {
			tvtitle = new TextView(mActivity);
			tvtitle.setGravity(Gravity.CENTER);
			tvtitle.setTypeface(Typeface.MONOSPACE, 1);
			// tvtitle.setTextAppearance(mActivity,
			// android.R.style.TextAppearance_Large);
			tvtitle.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
			tvtitle.setTextSize(20);
			tvtitle.setText(title);
		} else {
			setTitleView(title);
		}

		View vbody = body;

		blsk = new Button(mActivity);
		brsk = new Button(mActivity);
		blsk.setText(getResource(R.string.fail));
		brsk.setText(getResource(R.string.pass));

		// create sub linear layout for the buttons and add the buttons to it
		LinearLayout.LayoutParams llsklp = new LinearLayout.LayoutParams(
				LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT, 1);
		llsk.addView(blsk, llsklp);
		llsk.addView(brsk, llsklp);
		llsk.setGravity(Gravity.CENTER);

		// add everything to layout with accurate weights
		ll.addView(tvtitle, new LinearLayout.LayoutParams(
				LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT, 3));
		ll.addView(vbody, new LinearLayout.LayoutParams(
				LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT, 1));
		ll.addView(llsk, new LinearLayout.LayoutParams(
				LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT, 3));

		// TODO press OK or FAILED should have different behavior
		blsk.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				ExecuteTest.currentTest.Result = Test.FAILED;
				ExecuteTest.currentTest.Exit();
			}
		});

		brsk.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				ExecuteTest.currentTest.Result = Test.PASSED;
				ExecuteTest.currentTest.Exit();
			}
		});

	}

	public void hideButtons() {
		if (brsk != null)
			brsk.setVisibility(View.GONE);
		if (blsk != null)
			blsk.setVisibility(View.GONE);
		if (bmsk != null)
			bmsk.setVisibility(View.GONE);
	}

	public void showButtons() {
		if (brsk != null)
			brsk.setVisibility(View.VISIBLE);
		if (blsk != null)
			blsk.setVisibility(View.VISIBLE);
		if (bmsk != null)
			bmsk.setVisibility(View.VISIBLE);
	}

	public void setEnabledButtons(boolean en) {
		if (brsk != null)
			brsk.setEnabled(en);
		if (blsk != null)
			blsk.setEnabled(en);
		if (bmsk != null)
			bmsk.setEnabled(en);
	}

	public void setEnabledButtons(boolean en, Button b) {
		b.setEnabled(en);
	}
	
	public String getResource(int id)
	{
		return mActivity.getResources().getString(id);
	}
}
/*
 * Empty Test use as a default when no test was defined
 */
class EmptyTest extends Test {

	EmptyTest(ID pid, String s) {
		super(pid, s);
	}

	@Override
	protected void Run() {
		// this function executes the test
		switch (mState) {
		case INIT: // init the test, shows the first screen

			TestLayout1 tl = new TestLayout1(mContext, mName, "Call 测试",
					getResource(R.string.fail), getResource(R.string.pass));
			mContext.setContentView(tl.ll);

			break;
		case INIT + 1:// step n of the test, update the screen, set key
			// handlers

			break;
		case END://

			break;
		}

	}
}

/*
 * Empty Test use as a default when no test was defined
 */
class MiscTest extends Test {

	TestLayout1 tl;

	private int tolerance = 65; //16;// 8;

	private int mGoodLinesCount;

	/*
	 * private Rect rTop = new Rect(-1, 50-tolerance, Lcd.width(),
	 * 50+tolerance); private Rect rBottom = new Rect(-1,
	 * Lcd.height()-50-tolerance, Lcd.width(), Lcd.height()-50+tolerance);
	 * private Rect rLeft = new Rect(50-tolerance, -1, 50+tolerance,
	 * Lcd.height()); private Rect rRight = new Rect(Lcd.width()-50-tolerance,
	 * -1, Lcd.width()-50+tolerance, Lcd.height());
	 */
	int margin = 10;
	private Rect rLeftBtn = new Rect(margin, Lcd.height() - 50, Lcd.width() / 2
			- margin, Lcd.height());
	private Rect rRightBtn = new Rect(Lcd.width() / 2 + margin,
			Lcd.height() - 50, Lcd.width() - margin, Lcd.height());

	int tolerance_x = (int) (tolerance / Math.sin(Math.atan(Lcd.height()
			/ Lcd.width())));
	int tolerance_y = (int) (tolerance / Math.cos(Math.atan(Lcd.height()
			/ Lcd.width())));

	private Point[] rBotLeftTopRight = {
			new Point(0, Lcd.height() + tolerance_y),
			new Point(Lcd.width(), tolerance_y),
			new Point(Lcd.width(), -tolerance_y),
			new Point(0, Lcd.height() - tolerance_y) };

	private Point[] rTopLeftBotRight = {
			new Point(0, tolerance_y),
			new Point(Lcd.width(), Lcd.height() + tolerance_y),
			new Point(Lcd.width(), Lcd.height() - tolerance_y),
			new Point(0, -tolerance_y) };

	private int dist = tolerance;
	private int start_x1 = 80;
	private int start_x2 = start_x1 + tolerance + dist;
	private int start_x3 = start_x2 + tolerance + dist;
	private int mLinesCount = 3;

	private Point[] p1 = {
			new Point(start_x1, 0),
			new Point(start_x1, Lcd.height() ),
			new Point(start_x1 + tolerance, Lcd.height() ),
			new Point(start_x1 + tolerance, 0) };

	private Point[] p2 = {
			new Point(start_x2, 0),
			new Point(start_x2, Lcd.height() ),
			new Point(start_x2 + tolerance, Lcd.height() ),
			new Point(start_x2 + tolerance, 0) };

	private Point[] p3 = {
			new Point(start_x3, 0),
			new Point(start_x3, Lcd.height() ),
			new Point(start_x3 + tolerance, Lcd.height() ),
			new Point(start_x3 + tolerance, 0) };

	final Parallelepipede pl1 = new Parallelepipede(p1);
	final Parallelepipede pl2 = new Parallelepipede(p2);
	final Parallelepipede pl3 = new Parallelepipede(p3);

	MiscTest(ID pid, String s) {
		super(pid, s);

	}

	MiscTest(ID pid, String s, int timein) {
		super(pid, s, timein, 0);

	}

	float mAverageX = 0;
	float mAverageY = 0;

	int mEcartType = 0;

	@Override
	protected void Run() {
		// this function executes the test
		switch (mState) {
		case INIT: //

			mTimeIn.start();

			// result will be set to false if the pen goes out of the shapes
			Result = NOT_TESTED;
			if (MMITest.mode == MMITest.AUTO_MODE && false) {
				tl = new TestLayout1(mContext, "Please draw on the canvas",
						new MyView(mContext));
				mContext.setContentView(tl.ll);
			} else {
				mContext.setContentView(new MyView(mContext));
			}

			mState++;

			mGoodLinesCount = 0;

			break;

		case END:

			if (MMITest.mode == MMITest.AUTO_MODE) {
				tl = new TestLayout1(mContext, mName, "test finished");
				mContext.setContentView(tl.ll);
			}
			// Exit();

			break;
		default:
		}
	}

	public class MyView extends View {

		private Bitmap mBitmap;
		private Canvas mCanvas;
		private Path mPath;
		private Paint mBitmapPaint;
		private Paint mPaint;
		private AlertDialog mAlertDialog, mAlertDialogMsg, mAlertDialogEnd, mAlertDialogOK;

		public MyView(Context c) {
			super(c);
			mPaint = new Paint();
			mPaint.setAntiAlias(true);
			// mPaint.setDither(true);
			mPaint.setColor(0xFFFF0000);
			mPaint.setStyle(Paint.Style.STROKE);
			mPaint.setStrokeJoin(Paint.Join.BEVEL);
			mPaint.setStrokeCap(Paint.Cap.ROUND);
			mPaint.setStrokeWidth(1);
			mBitmap = Bitmap.createBitmap(Lcd.width(), Lcd.height(),
					Bitmap.Config.ARGB_8888);
			mCanvas = new Canvas(mBitmap);
			mPath = new Path();
			mBitmapPaint = new Paint(Paint.DITHER_FLAG);

			mAlertDialogEnd = new AlertDialog.Builder(mContext)
					.setTitle("TEST RESULT")
					.setMessage("Pen out of bounds!")
/*
					.setNegativeButton("PASS",
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int whichButton) {
									ExecuteTest.currentTest.Result = Test.PASSED;
									ExecuteTest.currentTest.Exit();
								}
							})
*/
					.setPositiveButton(getResource(R.string.fail),
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int whichButton) {
									ExecuteTest.currentTest.Result = Test.FAILED;
									ExecuteTest.currentTest.Exit();
								}
							}).create();

			mAlertDialogOK = new AlertDialog.Builder(mContext)
					.setTitle("TEST RESULT")
					.setMessage("OK!")

					.setNegativeButton(getResource(R.string.pass),
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int whichButton) {
									ExecuteTest.currentTest.Result = Test.PASSED;
									ExecuteTest.currentTest.Exit();
								}
							}).create();

			mAlertDialogMsg = new AlertDialog.Builder(mContext)
					.setTitle("TEST RESULT")
					.setMessage("line too short!")
					.setNeutralButton("OK",
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int whichButton) {
								}
							})/*
							 * .setOnKeyListener(new
							 * DialogInterface.OnKeyListener() { public boolean
							 * onKey(DialogInterface dialog, int keyCode,
							 * KeyEvent event) { if ( keyCode ==
							 * KeyEvent.KEYCODE_BACK && event.getAction() ==
							 * KeyEvent.ACTION_UP ) return false; else return
							 * true; } })
							 */
					.create();

		}

		@Override
		protected void onDraw(Canvas canvas) {
			canvas.drawColor(Color.WHITE/* 0xFFAAAAAA */);

			canvas.drawBitmap(mBitmap, 0, 0, mBitmapPaint);

			/* draw 2 parallelepipede on the screen */
			pl1.draw(canvas);
			pl2.draw(canvas);
			pl3.draw(canvas);

			/* draw references lines on the screen */
			Paint p = new Paint();
			p.setColor(Color.BLACK);
			p.setStyle(Paint.Style.STROKE);
			p.setTextSize(20);

			/* draw lines start / end point on the screen */
/*			float Xborder = 24;
			float Yborder = 32;
			float width = 20;// 10;
			canvas.drawLine(Xborder - width, Yborder, Xborder + width, Yborder,
					p);
			canvas.drawLine(Xborder, Yborder - width, Xborder, Yborder + width,
					p);

			canvas.drawLine(Lcd.width() - Xborder - width, Yborder, Lcd.width()
					- Xborder + width, Yborder, p);
			canvas.drawLine(Lcd.width() - Xborder, Yborder - width, Lcd.width()
					- Xborder, Yborder + width, p);

			canvas.drawLine(Lcd.width() - Xborder - width, Lcd.height()
					- Yborder, Lcd.width() - Xborder + width, Lcd.height()
					- Yborder, p);
			canvas.drawLine(Lcd.width() - Xborder, Lcd.height() - Yborder
					- width, Lcd.width() - Xborder, Lcd.height() - Yborder
					+ width, p);

			canvas.drawLine(Xborder - width, Lcd.height() - Yborder, Xborder
					+ width, Lcd.height() - Yborder, p);
			canvas.drawLine(Xborder, Lcd.height() - Yborder - width, Xborder,
					Lcd.height() - Yborder + width, p);
*/
			// header text
			canvas.drawText("Please draw on ", Lcd.width() / 2 - 35, 25, p);
			canvas.drawText("the yellow area", Lcd.width() / 2 - 35, 50, p);
			// footer text
			canvas.drawText((Result == FAILED ? getResource(R.string.fail) : ""),
					Lcd.width() / 2 - 20, Lcd.height() - 25, p);

			/* draw the current pen position */
			canvas.drawPath(mPath, mPaint);

		}

		private float mX, mY;
		private static final float TOUCH_TOLERANCE = 1;

		private void touch_start(float x, float y) {
			mPath.reset();
			mPath.moveTo(x, y);
			mX = x;
			mY = y;
		}

		private void touch_move(float x, float y) {
			float dx = Math.abs(x - mX);
			float dy = Math.abs(y - mY);
			if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
				mPath.quadTo(mX, mY, (x + mX) / 2, (y + mY) / 2);
				mX = x;
				mY = y;
			}
		}

		private void touch_up() {
			mPath.lineTo(mX, mY);
			// commit the path to our offscreen
			mCanvas.drawPath(mPath, mPaint);
			// kill this so we don't double draw
			// mPath.reset();
		}

		@Override
		public boolean onTouchEvent(MotionEvent event) {
			float x = event.getX();
			float y = event.getY();

			// check if the point is inside the bounds drawn on the screen
			if (!pl1.includePoint(x, y) && !pl2.includePoint(x, y) && !pl3.includePoint(x, y)) {
				Result = FAILED;
			}

			Log.d(TAG, "x = " + x + " y = " + y);

			switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				touch_start(x, y);
				invalidate();
				mAverageX = x;
				mAverageY = y;
				break;
			case MotionEvent.ACTION_MOVE:
				touch_move(x, y);
				invalidate();
				mAverageX = (x + mAverageX) / 2;
				mAverageY = (y + mAverageY) / 2;
				break;
			case MotionEvent.ACTION_UP:
				touch_up();
				invalidate();

				Log.d(TAG, "AVERAGES : x = " + mAverageX + " y = " + mAverageY);

				/* check the length of the path */
				RectF rect = new RectF(0, 0, 0, 0);
				mPath.computeBounds(rect, true);
				float mPathLength = (float) Math.sqrt(rect.height()
						* rect.height() + rect.width() * rect.width());

				Log.i(TAG, "path length is " + mPathLength);

				mAlertDialog = mAlertDialogEnd;

				if (Result == FAILED) {
					mAlertDialog.setMessage("Pen out of bounds!");
				} else if (mPathLength < 680) {
					mAlertDialog = mAlertDialogMsg;
				} else if (mGoodLinesCount == (mLinesCount - 1)) {
					// mAlertDialog.setMessage("OK!");
					mAlertDialog = mAlertDialogOK;
				} else {
					mGoodLinesCount++;
					mAlertDialog = null;
				}

				Result = NOT_TESTED;

				if (mAlertDialog == null) {

				} else if (!mAlertDialog.isShowing()) {
					if (mTimeIn.isFinished())
						mAlertDialog.show();
				}

				break;
			}
			return true;
		}

	}

	class Parallelepipede {
		private Path mPath;
		private Paint mPaint;
		private Point[] points;

		Parallelepipede(Point[] p) {
			points = p.clone();
			mPath = new Path();

			mPaint = new Paint();
			mPaint.setAntiAlias(true);
			mPaint.setColor(Color.YELLOW);
			mPaint.setStyle(Paint.Style.FILL);
			mPaint.setStrokeJoin(Paint.Join.BEVEL);
			mPaint.setStrokeCap(Paint.Cap.ROUND);
			mPaint.setStrokeWidth(1);
		}

		void draw(Canvas c) {
			mPath.reset();
			mPath.moveTo(points[0].x, points[0].y);
			for (int i = 1; i < points.length; i++) {
				mPath.lineTo(points[i].x, points[i].y);
			}
			mPath.close();
			c.drawPath(mPath, mPaint);
		}

		/*
		 * checks if the point (x,y) is included in the Parallelepipede
		 */

		public boolean includePoint(float x, float y) {
			Point p = new Point((int) x, (int) y);
			double d1 = distLineToPoint(points[0], points[1], p);
			double d2 = distLineToPoint(points[2], points[3], p);
			double range = distLineToPoint(points[0], points[1], points[2]);
			Log.d(TAG, "includePoint: " + d1 + " " + d2 + " " + range);
			/*
			 * to be included in the shape, the distance from (x,y) to the
			 * bottom or top line should not exceed the distance between the
			 * bottom to top line
			 */
			if (Math.max(d1, d2) < range) {
				return true;
			}
			return false;
		}

		/* computes the shortest distance form a point to a line */
		/*                                                       
		 * 
		 */
		private double distLineToPoint(Point A, Point B, Point p) {

			/*
			 * let [AB] be the segment and C the projection of C on (AB) AC * AB
			 * (Cx-Ax)(Bx-Ax) + (Cy-Ay)(By-Ay) u = ------- =
			 * ------------------------------- ||AB||^2 ||AB||^2
			 */
			double det = Math.pow(B.x - A.x, 2) + Math.pow(B.y - A.y, 2);
			if (det == 0) {
				return 0;
			}

			double u = ((p.x - A.x) * (B.x - A.x) + (p.y - A.y) * (B.y - A.y))
					/ det;

			/*
			 * The projection point P can then be found:
			 * 
			 * Px = Ax + r(Bx-Ax) Py = Ay + r(By-Ay)
			 */
			double Px = A.x + u * (B.x - A.x);
			double Py = A.y + u * (B.y - A.y);

			// Log.d(TAG,"distLineToPoint : u="+u+" Px=" +Px +" Py=" +Py);

			/* the distance to (AB) is the the [Pp] segment length */

			double distance = Math.sqrt(Math.pow(Px - p.x, 2)
					+ Math.pow(Py - p.y, 2));

			return distance;
		}

	}/* Parallelepipede */

}

/*
 * Slide Test
 */
class SlideTest extends Test {

	SlideTest(ID pid, String s) {
		super(pid, s);

	}

	SlideTest(ID pid, String s, int timein) {
		super(pid, s, timein, 0);
	}

	boolean mSlideOpen;

	TestLayout1 tl;

	@Override
	protected void Run() {
		// this function executes the test
		switch (mState) {
		case INIT: // init the test, shows the first screen

			mTimeIn.start();

			Settings.Secure.putString(mContext.getContentResolver(),
					Settings.Secure.ENABLED_INPUT_METHODS, null);

			hConfigChanged = new ConfigChangedHandler() {
				public void h(Configuration c) {
					mSlideOpen = (c.hardKeyboardHidden == Configuration.KEYBOARDHIDDEN_NO);
					Run();
				}
			};

			mSlideOpen = (mContext.getResources().getConfiguration().hardKeyboardHidden == Configuration.KEYBOARDHIDDEN_NO);

			if (mSlideOpen) {
				tl = new TestLayout1(mContext, mName,
						"Slide is Open  \nplease Close Slide ", getResource(R.string.fail), getResource(R.string.pass));
			} else {
				tl = new TestLayout1(mContext, mName,
						"Slide is Closed \nplease Open Slide ", getResource(R.string.fail), getResource(R.string.pass));
			}
			if (!mTimeIn.isFinished()) {
				tl.setEnabledButtons(false);
			}

			mContext.setContentView(tl.ll);
			mState++;

			break;
		default://
			if (mSlideOpen) {
				tl = new TestLayout1(mContext, mName, "Slide is Open  ",
						getResource(R.string.fail), getResource(R.string.pass));
			} else {
				tl = new TestLayout1(mContext, mName, "Slide is Closed ",
						getResource(R.string.fail), getResource(R.string.pass));
			}
			mState++;

			if (!mTimeIn.isFinished()) {
				tl.setEnabledButtons(false);
			}

			mContext.setContentView(tl.ll);

			break;
		case END:
			hConfigChanged = null;

			break;
		}

	}

	@Override
	protected void onTimeInFinished() {
		if (tl != null)
			tl.setEnabledButtons(true);
	}

}

/*
 * Image display test
 */
class ImageTest extends Test {

	private int mImageId;

	ImageTest(ID pid, String s, int r) {
		super(pid, s);
		mImageId = r;

	}

	ImageTest(ID pid, String s, int r, int timein) {
		super(pid, s, timein, 0);
		mImageId = r;

		hKey = new KeyHandler() {
			public boolean handleKey(KeyEvent event) {
				if (event.getKeyCode() == KeyEvent.KEYCODE_MENU)
					return !mTimeIn.isFinished();
				return false;
			}
		};

	}

	@Override
	protected void Run() {
		// this function executes the test
		switch (mState) {
		case INIT: // init the test, shows the first screen
			/*
			 * if we don't care about scaling // Create a LinearLayout in which
			 * to add the ImageView LinearLayout mLinearLayout = new
			 * LinearLayout(mContext);
			 * 
			 * // Instantiate an ImageView and define its properties ImageView i
			 * = new ImageView(mContext); i.setImageResource(mImageId);
			 * i.setAdjustViewBounds(true); // set the ImageView bounds to match
			 * the Drawable's dimensions
			 * i.setScaleType(ImageView.ScaleType.FIT_END);
			 * Log.d(TAG,"scale :"+i.getScaleType());
			 * 
			 * // Add the ImageView to the layout and set the layout as the
			 * content view mLinearLayout.addView(i);
			 */
			mTimeIn.start();

			mContext.setContentView(new UnscaledView(mContext));
			Result = PASSED;

			break;
		case INIT + 1:// step n of the test, update the screen, set key
			// handlers

			break;
		case END://

			break;
		}

	}

	private class UnscaledView extends View {
		private Paint mPaint = new Paint();
		private Path mPath = new Path();
		private boolean mAnimate;
		private long mNextTime;
		Bitmap mBitmap;

		android.graphics.BitmapFactory.Options bfo;

		public UnscaledView(Context context) {
			super(context);
			// if we don't turn of the the scaling property, image will be
			// resized according
			// to screen density . as we set density = 120 and the reference in
			// android is 160
			// image will be down-sized by 0.75 ratio */
			bfo = new android.graphics.BitmapFactory.Options();
			bfo.inScaled = false;

			mBitmap = android.graphics.BitmapFactory.decodeResource(
					getResources(), mImageId, bfo);
		}

		@Override
		protected void onDraw(Canvas canvas) {

			mBitmap.setDensity(Bitmap.DENSITY_NONE);
			canvas.setDensity(Bitmap.DENSITY_NONE);
			canvas.drawBitmap(mBitmap, 0, 0, null);

		}

		@Override
		protected void onAttachedToWindow() {
			mAnimate = true;
			super.onAttachedToWindow();
		}

		@Override
		protected void onDetachedFromWindow() {
			mAnimate = false;
			super.onDetachedFromWindow();
		}
	}

}
