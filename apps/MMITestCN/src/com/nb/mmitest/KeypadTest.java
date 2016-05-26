
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
import android.view.ViewConfiguration;
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
/*
 * Keyboard Test
 */
class KeypadTest extends Test {

	KeyPad kp = null;
	
	Button blsk;
	Button brsk;


	KeypadTest(ID pid, String s) {
		super(pid, s);
		
		

	}
	
	KeypadTest(ID pid, String s, int timein) {
		super(pid, s, timein, 0);
		
	}
	protected void Init(){
		kp = new KeyPad();
		hKey = new KeyHandler() {
		public boolean handleKey(KeyEvent event) {
			if(event.getKeyCode() == KeyEvent.KEYCODE_APP_SWITCH){
				return true;
			}
			return kp.updateKeyState(event.getAction(), event.getKeyCode());
		}
	};
	}
    @Override
	protected void onTimeInFinished() {
	    
	    if(blsk != null)
		blsk.setEnabled(true);
	    if(brsk != null)
		brsk.setEnabled(true);
	}

	private void StartTimeout() {
		SetTimer(10000/* miliseconds */, TimeoutCb);
	}

	CallBack TimeoutCb = new CallBack() {
		public void c() {
			Stop();
		}
	};

	class KeyPad {

		KeyPad() {

		}

		class Key extends Object {

			Key(String s) {
				mName = s;
				pressed = false;
			}

			String mName;

			Button b;

			boolean pressed;

			int keycode;

			public String toString() {
				return mName;
			}
		}

		// SparseArray <Key> keymaptoto = new SparseArray<Key>(2);

		// keymaptoto.append(10, new Key("test"));
		// keymaptoto.clear();

		// HashMap<Integer, Key> wordcount = new HashMap<Integer, Key>();
		// wordcount.put<10, new Key("test")>;
		

		
		Key[][] KeyStates_martell = {
				/*{
					new Key("Menu"), new Key("SEARCH"), new Key("Back") 
				},
				{
					new Key("Call"),new Key("Home"), /*new Key("End"), new Key("Camera")
				},*/
/*				{
					new Key("Q"), new Key("W"), new Key("E"), new Key("R"), new Key("T"),
					new Key("Y"), new Key("U"), new Key("I"), new Key("O"), new Key("P")
				},
				{
					new Key("A"), new Key("S"), new Key("D"), new Key("F"), new Key("G"),
					new Key("H"), new Key("J"), new Key("K"), new Key("L"), new Key("Del")
				},
				{
					new Key("Fn"), new Key("Z"), new Key("X"), new Key("C"), new Key("V"),
					new Key("B"), new Key("N"), new Key("M"), new Key("Sk"), new Key("Ret")
				},
				{
					new Key("Caps"), new Key("%"), new Key("  Space  "), new Key("SYM"),
					new Key("Ctrl")
				},
*/
				{
					new Key(getResource(R.string.vol_up)), new Key(getResource(R.string.vol_down)), /*new Key("POWER")*/					
				},
				//20121009 ying.pang for touch key test
				{ 
					new Key(getResource(R.string.key_set)), new Key(getResource(R.string.key_home)), new Key(getResource(R.string.key_back)) 
				},
		};

		final int[][] KeyCodes_martell = {
				/*{
					KeyEvent.KEYCODE_MENU, KeyEvent.KEYCODE_SEARCH, KeyEvent.KEYCODE_BACK 
				},
				{
					//KeyEvent.KEYCODE_CALL, 
					KeyEvent.KEYCODE_HOME, KeyEvent.KEYCODE_ENDCALL, KeyEvent.KEYCODE_CAMERA
				},*/
/*				{
					KeyEvent.KEYCODE_Q, KeyEvent.KEYCODE_W, KeyEvent.KEYCODE_E,
					KeyEvent.KEYCODE_R, KeyEvent.KEYCODE_T, KeyEvent.KEYCODE_Y,
					KeyEvent.KEYCODE_U, KeyEvent.KEYCODE_I, KeyEvent.KEYCODE_O,
					KeyEvent.KEYCODE_P
				},
				{
					KeyEvent.KEYCODE_A, KeyEvent.KEYCODE_S, KeyEvent.KEYCODE_D,
					KeyEvent.KEYCODE_F, KeyEvent.KEYCODE_G, KeyEvent.KEYCODE_H,
					KeyEvent.KEYCODE_J, KeyEvent.KEYCODE_K, KeyEvent.KEYCODE_L,
					KeyEvent.KEYCODE_DEL
				},
				{
					KeyEvent.KEYCODE_SHIFT_LEFT, KeyEvent.KEYCODE_Z, KeyEvent.KEYCODE_X,
					KeyEvent.KEYCODE_C, KeyEvent.KEYCODE_V, KeyEvent.KEYCODE_B,
					KeyEvent.KEYCODE_N, KeyEvent.KEYCODE_M, KeyEvent.KEYCODE_PERIOD, KeyEvent.KEYCODE_ENTER
				},
				{
					KeyEvent.KEYCODE_ALT_LEFT, KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_SPACE,
					KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_SEARCH
				},
*/
				{
					KeyEvent.KEYCODE_VOLUME_UP, KeyEvent.KEYCODE_VOLUME_DOWN, /*KeyEvent.KEYCODE_POWER*/					
				},
				//20121009 ying.pang for touch key test
				{
					KeyEvent.KEYCODE_MENU, KeyEvent.KEYCODE_HOME,KeyEvent.KEYCODE_BACK
				},

		};
		
		Key[][] KeyStates_martelllite = {
				{
					new Key(getResource(R.string.key_back)), new Key(getResource(R.string.key_home)), /*new Key("Multitask")*/
				},
				{
					/*new Key("Call"),new Key("Home"), /*new Key("End"), new Key("Camera")*/
				},
/*				{
					new Key("Q"), new Key("W"), new Key("E"), new Key("R"), new Key("T"),
					new Key("Y"), new Key("U"), new Key("I"), new Key("O"), new Key("P")
				},
				{
					new Key("A"), new Key("S"), new Key("D"), new Key("F"), new Key("G"),
					new Key("H"), new Key("J"), new Key("K"), new Key("L"), new Key("Del")
				},
				{
					new Key("Fn"), new Key("Z"), new Key("X"), new Key("C"), new Key("V"),
					new Key("B"), new Key("N"), new Key("M"), new Key("Sk"), new Key("Ret")
				},
				{
					new Key("Caps"), new Key("%"), new Key("  Space  "), new Key("SYM"),
					new Key("Ctrl")
				},
*/
				{
					new Key(getResource(R.string.vol_up)), new Key(getResource(R.string.vol_down)), /*new Key("POWER")*/
				}
		};

		 final int[][] KeyCodes_martelllite = {
				{
					KeyEvent.KEYCODE_BACK, KeyEvent.KEYCODE_HOME,/* KeyEvent.KEYCODE_RECENTAPP */
				},
				{
					//KeyEvent.KEYCODE_CALL, 
					/*KeyEvent.KEYCODE_HOME, KeyEvent.KEYCODE_ENDCALL, KeyEvent.KEYCODE_CAMERA*/
				},
/*				{
					KeyEvent.KEYCODE_Q, KeyEvent.KEYCODE_W, KeyEvent.KEYCODE_E,
					KeyEvent.KEYCODE_R, KeyEvent.KEYCODE_T, KeyEvent.KEYCODE_Y,
					KeyEvent.KEYCODE_U, KeyEvent.KEYCODE_I, KeyEvent.KEYCODE_O,
					KeyEvent.KEYCODE_P
				},
				{
					KeyEvent.KEYCODE_A, KeyEvent.KEYCODE_S, KeyEvent.KEYCODE_D,
					KeyEvent.KEYCODE_F, KeyEvent.KEYCODE_G, KeyEvent.KEYCODE_H,
					KeyEvent.KEYCODE_J, KeyEvent.KEYCODE_K, KeyEvent.KEYCODE_L,
					KeyEvent.KEYCODE_DEL
				},
				{
					KeyEvent.KEYCODE_SHIFT_LEFT, KeyEvent.KEYCODE_Z, KeyEvent.KEYCODE_X,
					KeyEvent.KEYCODE_C, KeyEvent.KEYCODE_V, KeyEvent.KEYCODE_B,
					KeyEvent.KEYCODE_N, KeyEvent.KEYCODE_M, KeyEvent.KEYCODE_PERIOD, KeyEvent.KEYCODE_ENTER
				},
				{
					KeyEvent.KEYCODE_ALT_LEFT, KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_SPACE,
					KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_SEARCH
				},
*/
				{
					KeyEvent.KEYCODE_VOLUME_UP, KeyEvent.KEYCODE_VOLUME_DOWN, /*KeyEvent.KEYCODE_POWER*/
				}

		};
		boolean updateKeyState(int action, int keycode) {

			
			boolean handled = false;
			int len = KeyCodes_martell.length;
			if(checkDeviceHasNavigationBar(mContext))
				len =1;
		for (int i = 0; i < len; i++) {
				for (int j = 0; j < KeyCodes_martell[i].length; j++) {
					if (KeyCodes_martell[i][j] == keycode) {
						if (action == KeyEvent.ACTION_UP) {// hide the button
							KeyStates_martell[i][j].pressed = true;
						KeyStates_martell[i][j].b.setVisibility(Button.INVISIBLE);
						}
						handled = true;
					}
				}
			}

				
				
			
			
			// update the current test status
			Result = kp.isAllKeyPressed() == true ? PASSED : FAILED;
			if (Result == PASSED) {
				ExecuteTest.currentTest.Result=Test.PASSED;
				brsk.setEnabled(true);
				//ExecuteTest.currentTest.Exit();
			}
			if(!handled) {
				Log.d(TAG,"key "+keycode+"not handled");
			}
			return handled;
		}

		boolean isAllKeyPressed() {
			
			int len = KeyCodes_martell.length;
			if(checkDeviceHasNavigationBar(mContext))
				len =1;
				
				for (int i = 0; i < len; i++) {
				for (int j = 0; j < KeyCodes_martell[i].length; j++) {
					if (KeyStates_martell[i][j].pressed == false) {
						return false;
					}
				}
							

					
				
				
				
			}
			return true;

		}

	}/* KeyPad */

	CallBack Cb = new CallBack() {
		public void c() {
			//mContext.getWindow().setType(WindowManager.LayoutParams.TYPE_KEYGUARD);
		}
	};

	public void onResume() {
		SetTimer(1000/* miliseconds */, Cb);
	}

	protected void Run() {
		
		String run;
		//KeyPad mkeypad=new KeyPad();// Add by changmei.chen@tcl.com 2013-01-19 for stability.
		/*
		run=BuildConfig.Switch_Project();
		if(run.equals("D") | run.equals("C"))
		{
			Run2();
			return;
			
		}
       */
		switch (mState) {

		case INIT:
			// display the keyboard on the screen
			output = this.toString();
			
/*			Log.d();
			Settings.Secure.putString(mContext.getContentResolver(),
            Settings.Secure.ENABLED_INPUT_METHODS, null);
     
            Settings.Secure.putString(mContext.getContentResolver(),
            Settings.Secure.DEFAULT_INPUT_METHOD, null); 
*/

			LinearLayout ll = new LinearLayout(mContext);
			ll.setOrientation(LinearLayout.VERTICAL);

			LinearLayout.LayoutParams lllp = new LinearLayout.LayoutParams(
					LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT, 0);
			TableLayout.LayoutParams tllp = new TableLayout.LayoutParams(
					LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT, 1);
			TableRow.LayoutParams trlp = new TableRow.LayoutParams(LayoutParams.FILL_PARENT,
					LayoutParams.WRAP_CONTENT, 1);
			
			/* add title to the view */
			TextView tvtitle = new TextView(mContext);
			tvtitle.setGravity(Gravity.CENTER);
			tvtitle.setTypeface(Typeface.MONOSPACE, 1);
			// tvtitle.setTextAppearance(mActivity,
			// android.R.style.TextAppearance_Large);
			tvtitle.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
			tvtitle.setTextSize(20);
			tvtitle.setText(mName);
			
			ll.addView(tvtitle, new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT,
					LayoutParams.FILL_PARENT, 3));
			int len = kp.KeyStates_martell.length;
			if(checkDeviceHasNavigationBar(mContext))
				len = 1;
			for (int i = 0; i <len; i++) {// create rows
				TableLayout tl = new TableLayout(mContext);
				TableRow tr = new TableRow(mContext);

				for (int j = 0; j < kp.KeyStates_martell[i].length; j++) {
					/*if( !BuildConfig.isSW || ( !kp.KeyStates[i][j].mName.matches("Home") && !kp.KeyStates[i][j].mName.matches("End")) )*/ {
                        Button b = new Button(mContext);
                        b.setClickable(false);
                        b.setText(kp.KeyStates_martell[i][j].mName);
                        // b.setMaxWidth(KeyNames[i][j].length()*48);
                        b.setPadding(0, 0, 0, 0);
                        kp.KeyStates_martell[i][j].b = b;
                        tr.addView(b, trlp);
                        kp.KeyStates_martell[i][j].pressed = false;
					}
				}
				// tr.setGravity(Gravity.FILL);
				// tl.setGravity(Gravity.FILL);
				tl.addView(tr, tllp);
				ll.addView(tl, lllp);
			}
			
			/* finally add the pass /failed buttons */
			blsk = new Button(mContext);
			brsk = new Button(mContext);
			blsk.setText(getResource(R.string.fail));
			brsk.setText(getResource(R.string.pass));
			
//		    mTimeIn.start();
//		    if(!mTimeIn.isFinished()) {
		        blsk.setEnabled(true);
		        brsk.setEnabled(false);
//		    }


			// TODO press OK or FAILED should have different behavior
			blsk.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					ExecuteTest.currentTest.Result=Test.FAILED;
					ExecuteTest.currentTest.Exit();

				}
			});
			
			brsk.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					ExecuteTest.currentTest.Result=Test.PASSED;
					ExecuteTest.currentTest.Exit();
				}
			});
			
			
			// create sub linear layout for the buttons and add the buttons to it
			LinearLayout llsk = new LinearLayout(mContext);
			llsk.setOrientation(LinearLayout.HORIZONTAL);

			LinearLayout.LayoutParams llsklp = new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT,
					LayoutParams.WRAP_CONTENT, 1);
			llsk.addView(blsk, llsklp);
			llsk.addView(brsk, llsklp);
			llsk.setGravity(Gravity.CENTER);
			
			ll.addView(llsk, new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT,
					LayoutParams.FILL_PARENT, 3));

			
			ll.setGravity(Gravity.BOTTOM);
			
			
			// has
			// no
			// effect
			
		//	WindowManager.LayoutParams wmlp = new WindowManager.LayoutParams(
			//		WindowManager.LayoutParams.TYPE_SYSTEM_ERROR);
			mContext.setContentView(ll);
			
			break;

		case END:
			break;
		default:
			break;
		}

	}
	
	protected void Run2() {

		switch (mState) {

		case INIT:
			// display the keyboard on the screen
			output = this.toString();
			
/*			Log.d();
			Settings.Secure.putString(mContext.getContentResolver(),
            Settings.Secure.ENABLED_INPUT_METHODS, null);
     
            Settings.Secure.putString(mContext.getContentResolver(),
            Settings.Secure.DEFAULT_INPUT_METHOD, null); 
*/

			LinearLayout ll = new LinearLayout(mContext);
			ll.setOrientation(LinearLayout.VERTICAL);

			LinearLayout.LayoutParams lllp = new LinearLayout.LayoutParams(
					LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT, 0);
			TableLayout.LayoutParams tllp = new TableLayout.LayoutParams(
					LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT, 1);
			TableRow.LayoutParams trlp = new TableRow.LayoutParams(LayoutParams.FILL_PARENT,
					LayoutParams.WRAP_CONTENT, 1);
			
			/* add title to the view */
			TextView tvtitle = new TextView(mContext);
			tvtitle.setGravity(Gravity.CENTER);
			tvtitle.setTypeface(Typeface.MONOSPACE, 1);
			// tvtitle.setTextAppearance(mActivity,
			// android.R.style.TextAppearance_Large);
			tvtitle.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
			tvtitle.setTextSize(20);
			tvtitle.setText(mName);
			
			ll.addView(tvtitle, new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT,
					LayoutParams.FILL_PARENT, 3));

			for (int i = 0; i < kp.KeyStates_martelllite.length; i++) {// create rows
				TableLayout tl = new TableLayout(mContext);
				TableRow tr = new TableRow(mContext);

				for (int j = 0; j < kp.KeyStates_martelllite[i].length; j++) {
					/*if( !BuildConfig.isSW || ( !kp.KeyStates[i][j].mName.matches("Home") && !kp.KeyStates[i][j].mName.matches("End")) )*/ {
                        Button b = new Button(mContext);
                        b.setClickable(false);
                        b.setText(kp.KeyStates_martelllite[i][j].mName);
                        // b.setMaxWidth(KeyNames[i][j].length()*48);
                        b.setPadding(0, 0, 0, 0);
                        kp.KeyStates_martelllite[i][j].b = b;
                        tr.addView(b, trlp);
                        kp.KeyStates_martelllite[i][j].pressed = false;
					}
				}
				// tr.setGravity(Gravity.FILL);
				// tl.setGravity(Gravity.FILL);
				tl.addView(tr, tllp);
				ll.addView(tl, lllp);
			}
			
			/* finally add the pass /failed buttons */
			blsk = new Button(mContext);
			brsk = new Button(mContext);
			blsk.setText(getResource(R.string.fail));
			brsk.setText(getResource(R.string.pass));
			
//		    mTimeIn.start();
//		    if(!mTimeIn.isFinished()) {
		        blsk.setEnabled(true);
		        brsk.setEnabled(false);
//		    }


			// TODO press OK or FAILED should have different behavior
			blsk.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					ExecuteTest.currentTest.Result=Test.FAILED;
					ExecuteTest.currentTest.Exit();

				}
			});
			
			brsk.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					ExecuteTest.currentTest.Result=Test.PASSED;
					ExecuteTest.currentTest.Exit();
				}
			});
			
			
			// create sub linear layout for the buttons and add the buttons to it
			LinearLayout llsk = new LinearLayout(mContext);
			llsk.setOrientation(LinearLayout.HORIZONTAL);

			LinearLayout.LayoutParams llsklp = new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT,
					LayoutParams.WRAP_CONTENT, 1);
			llsk.addView(blsk, llsklp);
			llsk.addView(brsk, llsklp);
			llsk.setGravity(Gravity.CENTER);
			
			ll.addView(llsk, new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT,
					LayoutParams.FILL_PARENT, 3));

			
			ll.setGravity(Gravity.BOTTOM);
			
//			mContext.getWindow().setType(WindowManager.LayoutParams.TYPE_KEYGUARD);// HOME
//			// has
//			// no
//			// effect
//			
//			
			//WindowManager.LayoutParams wmlp = new WindowManager.LayoutParams(
			//		WindowManager.LayoutParams.TYPE_SYSTEM_ERROR);

			mContext.setContentView(ll);
			break;

		case END:
			break;
		default:
			break;
		}

	}
    public static boolean checkDeviceHasNavigationBar(Context activity) {  
    	  
        //通过判断设备是否有返回键、菜单键(不是虚拟键,是手机屏幕外的按键)来确定是否有navigation bar  
        boolean hasMenuKey = ViewConfiguration.get(activity)  
                .hasPermanentMenuKey();  
        if (!hasMenuKey) {  
            // 做任何你需要做的,这个设备有一个导航栏  
            return true;  
        }  
        return false;  
    }  
}
