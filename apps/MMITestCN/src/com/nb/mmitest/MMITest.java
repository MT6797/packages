package com.nb.mmitest;

import static android.provider.Settings.System.SCREEN_OFF_TIMEOUT;
import android.app.Activity;
import android.content.Context;

import android.content.pm.ActivityInfo;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.SystemProperties;
import android.provider.Settings;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.util.Log;
import android.util.DisplayMetrics;

import android.content.res.AssetFileDescriptor;

//import com.android.versionapi.VersionAPI;
import java.io.FileOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
//Begin Add by jiqian.shi for turn on bluetooth 20130228
/*add by stephen*/
import android.bluetooth.BluetoothAdapter;
//End
import com.nb.mmitest.BuildConfig;
import com.nb.mmitest.Test.ID;

import android.net.Uri;

import android.app.KeyguardManager;
import android.os.Debug;
import java.io.InputStream;
import java.io.FileNotFoundException;

//for wifi Add by jiqian.shi 20130304
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiManager;
import android.content.Intent;
import android.content.IntentFilter;
/*end add*/
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.widget.Toast;
public class MMITest extends Activity {
	/** Called when the activity is first created. */

	private String TAG = "MMITest";
	
     private BluetoothAdapter mBtAdapter;
	
	private WifiManager mWifiManager;
	
	private String mShortCode ="";
	private Button ManuButton;
	private Button AutoButton;

	static final int AUTO_MODE = 1;
	static final int MANU_MODE = 2;

	public static int mode = 0;

	// global variables used by Class Test
	static public int mgMode = MANU_MODE;

	public CompassDummy mCompassDummy;
	// modify by lei.guo for pr107477 begin 001
	// private String ENABLED_INPUT_METHODS;
	// private String DEFAULT_INPUT_METHOD;
	private String ENABLED_INPUT_METHODS = null;
	private String DEFAULT_INPUT_METHOD = null;
	// modify by lei.guo for pr107477 end 001
	private OprtModePolling pollMode;
	// public static Context mContext;

	// VersionAPI vapi = new VersionAPI();

	public DisplayMetrics metrics = new DisplayMetrics();

	@Override
	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

		String  mmitest_version = SystemProperties.get("ro.mmitest");
		if(mmitest_version!=null&&mmitest_version.equals("true")){
		    Settings.Secure.putInt(getContentResolver(), Settings.Secure.DEVICE_PROVISIONED, 1);
		    
		     KeyguardManager localKeyguardManager =
		            (KeyguardManager)getSystemService(Context.KEYGUARD_SERVICE);
		                KeyguardManager.KeyguardLock localKeyguardLock = 
		                localKeyguardManager
		                                .newKeyguardLock(TAG);
		                    Log.e(TAG, "disable keguard");
		                        localKeyguardLock.disableKeyguard();
		                     // localKeyguardLock.reenableKeyguard();
		}
		// set screen appearance
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		int mask = WindowManager.LayoutParams.FLAG_FULLSCREEN;
		mask |= WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
		getWindow().setFlags(mask, mask);

		getWindowManager().getDefaultDisplay().getMetrics(metrics);

		Lcd.setSize(getWindowManager().getDefaultDisplay().getWidth(),
				getWindowManager().getDefaultDisplay().getHeight());

		// Log.i(TAG,"Lcd metrics : w="+Lcd.width()+" h="+Lcd.height()+" "+metrics.toString());
		Log.i(TAG,
				"saved instance state : "
						+ (savedInstanceState == null ? "NULL"
								: savedInstanceState.toString()));

		// getVersion();

		if (false && !BuildConfig.isSW) {
			// check if pointercal exists if it does , start TSCal.
			File f = new File("/data/data/touchscreen.test/files/pointercal");
			// => now TSCalibration starts each time MMITEST reboots
			if ( /* f.exists() && */savedInstanceState == null) {
				Intent calIntent = new Intent(Intent.ACTION_MAIN, null);
				/*
				 * the activity will not be launched if it is already running at
				 * the top of the history stack.
				 */
				/* to avoid returning to TSCalibration after pressing back key */
				calIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
				calIntent.setClassName("touchscreen.test",
						"touchscreen.test.CalibrationTest");
				startActivity(calIntent);
			}

			pollMode = new OprtModePolling();

			Log.d(TAG, "BuildConfig.isSW=" + BuildConfig.isSW);
		}

		if (BuildConfig.getMmiTest()) {
			Settings.System.putString(getContentResolver(), "enableAutoAnswerLQ", "true");
			String autoanswer = Settings.System.getString(getContentResolver(), "enableAutoAnswerLQ");
			Log.d(TAG, " autoanswer " + autoanswer);
		}
		
		// Get the local Bluetooth adapter
	       mBtAdapter = BluetoothAdapter.getDefaultAdapter();
	       if (!mBtAdapter.isEnabled()) {
		   mBtAdapter.enable();
		}

		mWifiManager = (WifiManager)getSystemService(Context.WIFI_SERVICE);
		if(null != mWifiManager){ 
		    if(mWifiManager.isWifiEnabled() == false){
		    mWifiManager.setWifiEnabled(true);
		   }
		}
			
		// execute MTK touch panel Calibration
		// Intent Execution = new Intent(Intent.ACTION_MAIN, null);
		// Execution.setClassName("com.mediatek.app.touchpanel",
		// "com.mediatek.app.touchpanel.Calibrator");
		// startActivity(Execution);

		// mContext = (Context)this;

		/* keep compass alive */
		// mCompassDummy = new CompassDummy();

		showIdleScreen();
	}

	public void onResume() {
		super.onResume();
		if (pollMode != null)
			pollMode.enable();
		
		mShortCode = BuildConfig.GetASCStringFromTrace(TracabilityStruct.ID.SHORT_CODE_I);
		if(mShortCode!=null&&mShortCode.length()>3){
		    mShortCode =mShortCode.substring(0,3);
		}
		
		if (BuildConfig.getMmiTest()) {
			Settings.System.putString(getContentResolver(), "enableAutoAnswerLQ", "true");
			String autoanswer = Settings.System.getString(getContentResolver(), "enableAutoAnswerLQ");
			Log.d(TAG, " autoanswer " + autoanswer);
		}

		Log.d(TAG, "back to idle screen");
	}

	public void onPause() {
		super.onPause();
		if (pollMode != null)
			pollMode.disable();

		Log.d(TAG, "pollMode suspended");
	}

	@Override
	public boolean dispatchKeyEvent(KeyEvent event) {
		boolean result = false;

		switch (event.getKeyCode()) {
		// MMITest doesn't exit on any key press unless it is launched from app
		// SW
		case KeyEvent.KEYCODE_BACK:

			super.dispatchKeyEvent(event);
			
			break;

		case KeyEvent.KEYCODE_ENDCALL:
			// power off is done in PhoneWindowManager.java
			// pm.goToSleep(android.os.SystemClock.uptimeMillis() + 1);
			break;
		default:
			result = true;
			break;

		}
		Log.i(TAG, "dispatchKeyEvent() res=" + result);
		return result;
	}

	private void StartManualActivity() {
		Intent List = new Intent(Intent.ACTION_MAIN, null);
		List.setClassName(this, "com.nb.mmitest.ManuList");
		List.putExtra("ShortCode", mShortCode);
		// List.addCategory(Intent.CATEGORY_SAMPLE_CODE);
		mode = MANU_MODE;

	//	configSettings();
		startActivity(List);
	}

	private void StartAutoActivity() {
		Intent Auto = new Intent(Intent.ACTION_MAIN, null);
		Auto.setClassName(this, "com.nb.mmitest.AutoTest");
		// List.addCategory(Intent.CATEGORY_SAMPLE_CODE); mode = AUTO_MODE;
		Auto.putExtra("ShortCode", mShortCode);
	//	configSettings();
		startActivity(Auto);

	}

	class CompassDummy implements SensorEventListener {

		TestLayout1 tl;
		String mDisplayString;

		private SensorManager mSensorManager;
		private Sensor mCompass;

		CompassDummy() {
			mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

			mCompass = mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);

			if (!mSensorManager.registerListener(this, mCompass,
					SensorManager.SENSOR_DELAY_NORMAL)) {
				Log.e(TAG, "register listener for sensor " + mCompass.getName()
						+ " failed");
			}

		}

		public void onSensorChanged(SensorEvent event) {

		}

		public void onAccuracyChanged(Sensor s, int accuracy) {

		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		/* restore settings */
		// Settings.Secure.putString(getContentResolver(),
		// Settings.Secure.ENABLED_INPUT_METHODS, ENABLED_INPUT_METHODS);

		// Settings.Secure.putString(getContentResolver(),
		// Settings.Secure.DEFAULT_INPUT_METHOD, DEFAULT_INPUT_METHOD);

		// Log.i(TAG,"restore settings ENABLED_INPUT_METHODS");
		mBtAdapter.disable();
		mWifiManager.setWifiEnabled(false);
	}

	public void configSettings() {

		/* no screen orientation change */
		Settings.System.putInt(getContentResolver(),
				Settings.System.ACCELEROMETER_ROTATION, 0);

		/* no fancy animation */
		Settings.System.putInt(getContentResolver(),
				Settings.System.TRANSITION_ANIMATION_SCALE, 0);

		try {

			Settings.Secure.putInt(getContentResolver(),
					Settings.Secure.BLUETOOTH_ON, 0);

			
			// remove all input methods
			// String DEFAULT_INPUT_METHOD =
			// Settings.Secure.getString(getContentResolver(),
			// Settings.Secure.DEFAULT_INPUT_METHOD);
			// Log.i(TAG,"default input :"+DEFAULT_INPUT_METHOD);

			// ENABLED_INPUT_METHODS =
			// Settings.Secure.getString(getContentResolver(),
			// Settings.Secure.ENABLED_INPUT_METHODS);

			// Log.i(TAG,"enabled inputs :"+ ENABLED_INPUT_METHODS);

			// stop input methods
			// Settings.Secure.putString(getContentResolver(),
			// Settings.Secure.ENABLED_INPUT_METHODS, null);

			// Settings.Secure.putString(getContentResolver(),
			// Settings.Secure.DEFAULT_INPUT_METHOD, null);
			
		} catch (Exception e) {
			Log.e(TAG, "can't write secure settings");
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {

		Log.i(TAG, "onSaveInstanceState called");

	}

	private String ModemVersion = SystemProperties.get("ro.custom.build.version");
	private String TuningVersion = "default";
	private String ProjName = SystemProperties.get("ro.product.model")+" MMITEST";

	private String getMemoryInfo(){
		ProcessBuilder cmd;
		String result = new String();

		try{
			String[] args = {"/system/bin/cat", "/proc/meminfo"};
			cmd = new ProcessBuilder(args);

			Process process = cmd.start();
			InputStream in = process.getInputStream();
			byte[] re = new byte[1024];
			while(in.read(re) != -1){
				System.out.println(new String(re));
				result = result + new String(re);
			}
			in.close();
		} catch(IOException ex){
			ex.printStackTrace();
		}
		return result;
	}



	private void getVersion() {

		BufferedReader mVersionFifoReader = null;
		String versionfifo = "/data/local/tmp/mverproxy.fifo";
		char[] version = new char[256];
		try {
			mVersionFifoReader = new BufferedReader(
					new FileReader(versionfifo), 256);
			mVersionFifoReader.read(version, 0, version.length);
			Log.i(TAG, " versions : \n" + new String(version));
		} catch (IOException e) {
			Log.e(TAG, " plug status file can't be accessed " + e);
		} finally {
			try {
				if (mVersionFifoReader != null)
					mVersionFifoReader.close();
			} catch (IOException excep) {
				Log.e(TAG, "can't close file" + excep);
			}
		}

		String[] VersionNames = new String(version).split("\n");

		for (int verN = 0; verN < VersionNames.length; verN++) {
			if (VersionNames[verN] == null)
				break;
			if (VersionNames[verN].matches("K.{7}")) {
				ModemVersion = VersionNames[verN];
			}
			if (VersionNames[verN].matches("T.{7}"))
				TuningVersion = VersionNames[verN];
		}

	}

	private void showIdleScreen() {

		/* create MMITest first screen dynamically */
		LinearLayout ll = new LinearLayout(this);
		// ll.setLayoutParams(new
		// LinearLayout.LayoutParams(LayoutParams.FILL_PARENT,LayoutParams.FILL_PARENT));
		ll.setOrientation(LinearLayout.VERTICAL);

		LinearLayout llsk = new LinearLayout(this);
		llsk.setOrientation(LinearLayout.HORIZONTAL);

		LinearLayout slsk = new LinearLayout(this);
		slsk.setOrientation(LinearLayout.HORIZONTAL);

		TextView tvtitle = new TextView(this);
		tvtitle.setGravity(Gravity.CENTER);
		//tvtitle.setTypeface(Typeface.MONOSPACE, 1);
		// tvtitle.setTextAppearance(mActivity,
		// android.R.style.TextAppearance_Large);
		//tvtitle.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
		tvtitle.setTextSize(30);
		tvtitle.setText("MOBILE PHONES");
		//tvtitle.setShadowLayer(5, 5, 2.0f, Color.GRAY);
		tvtitle.setPadding(3, 3, 2, 2);

		TextView tvbody = new TextView(this);
		tvbody.setGravity(Gravity.CENTER);
		// tvbody.setTypeface(Typeface.MONOSPACE, 1);
		tvbody.setTextAppearance(this, android.R.style.TextAppearance_Large);
		tvtitle.setTextSize(20);

		/*
		 * menglian removed tvbody.setText("OPAL MMITEST \n" +
		 * "SW="+ModemVersion+"\n"+ "TUN="+TuningVersion);
		 */
		//setProjName();
		tvbody.setText(ProjName + "\n" + "\n" + "SWN:" + ModemVersion + "\n");

		TextView tvmode = new TextView(this);
		tvmode.setGravity(Gravity.CENTER);
		// tvbody.setTypeface(Typeface.MONOSPACE, 1);
		tvmode.setTextAppearance(this, android.R.style.TextAppearance_Medium);

		tvmode.setText("operating mode = UNKNOWN ");
		tvmode.setBackgroundColor(Color.DKGRAY);
		tvmode.setPadding(5, 5, 5, 5);
		if (pollMode != null)
			pollMode.setTextView(tvmode);
		/*
		 * String oprt_mode = pollMode.getCurrent(); if(
		 * oprt_mode.matches("FTM") ){ tvmode.setBackgroundColor(Color.GREEN);
		 * }else { tvmode.setBackgroundColor(Color.RED); }
		 * tvmode.setText("operating mode = "+oprt_mode);
		 */

		Button blsk = new Button(this);
		Button brsk = new Button(this);
		blsk.setText(this.getResources().getString(R.string.auto));
		brsk.setText(this.getResources().getString(R.string.manu));

		// create sub linear layout for the buttons and add the buttons to it
		LinearLayout.LayoutParams llsklp = new LinearLayout.LayoutParams(
				LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT, 1);
		llsk.addView(blsk, llsklp);
		llsk.addView(brsk, llsklp);
		llsk.setGravity(Gravity.CENTER);

		// add everything to layout with accurate weights
		ll.addView(tvtitle, new LinearLayout.LayoutParams(
				LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT, 0));
		ll.addView(tvbody, new LinearLayout.LayoutParams(
				LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT, 1));
		if (pollMode != null) {
			ll.addView(tvmode, new LinearLayout.LayoutParams(
					LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT, 0));
		}

		ll.addView(slsk, new LinearLayout.LayoutParams(
				LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT, 2));
		ll.addView(llsk, new LinearLayout.LayoutParams(
				LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT, 2));

		// TODO press OK or FAILED should have different behavior
		blsk.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				StartAutoActivity();
				mgMode = AUTO_MODE;

			}
		});

		brsk.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				StartManualActivity();
				mgMode = MANU_MODE;
			}
		});

		Button agingTestBtn = new Button(this);
		Button resetBtn = new Button(this);
		agingTestBtn.setText(this.getResources().getString(R.string.agingtest));
		resetBtn.setText(this.getResources().getString(R.string.reset));

		slsk.addView(agingTestBtn, llsklp);
		slsk.addView(resetBtn, llsklp);
		slsk.setGravity(Gravity.CENTER);

		
		// TODO press OK or FAILED should have different behavior
		agingTestBtn.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				Intent intent= new Intent();
				intent.setAction("nb.action.agintTest");
				startActivity(intent);

			}
		});

		resetBtn.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				resetPhone();
			}
		});

		setContentView(ll);

	}
	public void resetPhone()
	{            LayoutInflater factory = LayoutInflater.from(MMITest.this);
                     final View textEntryView = factory.inflate(R.layout.alert_dialog_text_entry, null);
			final EditText pswEdt = (EditText)textEntryView.findViewById(R.id.password_edit);
			new AlertDialog.Builder(MMITest.this)
				.setMessage(R.string.resetTip)
				.setView(textEntryView)
				.setPositiveButton(android.R.string.ok,
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog,int whichButton) {
			Log.d("bll", " pswEdt.getText()  "+pswEdt.getText());
							if("007".equals(pswEdt.getText().toString()))
								sendBroadcast(new Intent("android.intent.action.MASTER_CLEAR"));
							else
								Toast.makeText(MMITest.this,MMITest.this.getResources().getString(R.string.password_error),Toast.LENGTH_SHORT).show();

							}
								})
				.setNegativeButton(android.R.string.cancel,
								new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog,
											int whichButton) {
									}
								}).create().show();
		
	}

	class OprtModePolling extends Thread {
		WXKJRapi rpcClient;
		private String mOprtMode = WXKJRapi.OprtMode
				.valToString(WXKJRapi.OprtMode.SYS_OPRT_MODE_NONE.getVal());
		private String mLastOprtMode = mOprtMode;
		private TextView mOMTextView;
		private volatile boolean mRun = false;

		OprtModePolling() {

			try {
				rpcClient = new WXKJRapi(MMITest.this);
				setPriority(1);
				start();
				enable();
			} catch (IOException e) {
				Log.e(TAG, "error while polling phone operating mode" + e);
			}

		}

		public String getCurrent() {
			return mOprtMode;
		}

		public void setTextView(TextView tv) {
			mOMTextView = tv;

		}

		public synchronized void disable() {
			mRun = false;
		}

		public synchronized void enable() {
			mRun = true;
			notify();
		}

		public void run() {

			Log.d(TAG, "started : " + toString());
			boolean TEST = false;

			while (true) {
				if (TEST)
					mOprtMode = mOprtMode.equals("FTM") ? "TOTO" : "FTM";
				else
					mOprtMode = WXKJRapi.OprtMode.valToString(rpcClient
							.getOprtMode());

				Log.d(TAG, "operating mode =" + mOprtMode);

				if (mOprtMode == null) {
					Log.e(TAG, "getOprtMode() returned null");
				} else if (!mOprtMode.equals(mLastOprtMode)) {
					/* redraw the linked Text view */
					mLastOprtMode = mOprtMode;
					if (mOMTextView != null) {
						mOMTextView.post(new Runnable() {
							public void run() {
								if (mOprtMode.equals("FTM")) {
									mOMTextView.setBackgroundColor(Color.RED);
									mOMTextView
											.setText("WARNING operating mode = "
													+ mLastOprtMode);
								} else {
									mOMTextView
											.setBackgroundColor(Color.DKGRAY);
									mOMTextView.setText("operating mode = "
											+ mLastOprtMode);
								}
								mOMTextView.invalidate();
								Log.d(TAG, "textView changed to "
										+ mLastOprtMode);
							}
						});
					}
					Log.d(TAG, "operating mode changed to : " + mOprtMode);

				}

				try {
					synchronized (this) {
						if (!mRun)
							wait(); // wait has to be in a sync block
						else
							wait(1000);
					}
				} catch (InterruptedException e) {
					Log.d(TAG, "OprtModePolling interrupted!");
				}

			}
		}

	}

}
