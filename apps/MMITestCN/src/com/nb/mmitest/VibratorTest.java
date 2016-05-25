
package com.nb.mmitest;
import static android.provider.Settings.System.SCREEN_OFF_TIMEOUT;
import com.nb.mmitest.R;
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
import java.util.Timer;
import java.util.TimerTask;

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
class VibratorTest extends Test {

	private Vibrator vibrator=null;
	
	private TestLayout1 tl;
	
	VibratorTest(ID pid, String s) {
		super(pid, s);     
	}
	
	VibratorTest(ID pid, String s,int timein) {
		super(pid, s,timein,0);     
	}

	private void toggleVibrator(){
		long[] pattern =new long[]{100,1000};
		if(null==vibrator){               
			vibrator = (Vibrator)mContext.getSystemService(Context.VIBRATOR_SERVICE);
		}
		
		StopTimer();
		if(mState < 7){
			SetTimer(500, new CallBack(){
				public void c(){
					toggleVibrator();
				}
			});
		}
		
		switch(mState){
		case 1:
		case 3:
		case 5:
			vibrator.vibrate(pattern,0);
			break;
		default:
			vibrator.cancel();
			break;
		}
		
		++mState;
	}
	
	/* use simple timer for schedule task to enable vibrator */
	private Timer mTimer = null;
	private TimerTask mTimerTask = null;
	class CToggleVibrator extends TimerTask{
		private int mFlag = 1;
		
		@Override
		public void run(){
			long[] pattern =new long[]{100,1000};
			if(null==vibrator){               
				vibrator = (Vibrator)mContext.getSystemService(Context.VIBRATOR_SERVICE);
			}
			
			if(mFlag >= 7){
			//	cancel();
			}
			
			switch(mFlag){
			case 1:
			case 3:
			case 5:
			//	vibrator.vibrate(pattern,0);
			//	break;
			default:
				//vibrator.cancel();
				vibrator.vibrate(pattern,1);
				break;
			}
			
			++mFlag;
		}
	}
	
	@Override
	protected void Run() {              
		long[] pattern =new long[]{100,1000};
		if(null==vibrator){               
			vibrator = (Vibrator)mContext.getSystemService(Context.VIBRATOR_SERVICE);
		}

		switch (mState) {
		case END:
			vibrator.cancel();
			break;
			
		default:
			/* go shake */
			/* toggleVibrator(); */
			
			/* other way to use vibrator */
			if(mTimer == null)
			{
				mTimer = new Timer();
				mTimerTask = new CToggleVibrator();
				mTimer.schedule(mTimerTask, 0, 1500);
			}
			
			tl = new TestLayout1(mContext, mName, getResource(R.string.vibrator_test));
			mContext.setContentView(tl.ll);
			// Add by changmei.chen@tcl.com 2013-01-19 for MMITest spec 2.0.
			mTimeIn.start();
			if (!mTimeIn.isFinished()) 
				tl.hideButtons();
			// Add by changmei.chen@tcl.com 2013-01-19 for MMITest spec 2.0.
			
			break;
		}
	}
	
	@Override
	protected void onTimeInFinished() {
	    Log.d(TAG,"update buttons");
	    //if(tl != null)
	        //tl.setEnabledButtons(true);
	    if (tl != null)
			tl.showButtons(); // Add by changmei.chen@tcl.com 2013-01-19 for MMITest spec 2.0.
	}

	@Override
	public void Exit() {
		if(null != vibrator)
			vibrator.cancel();
		if(null != mTimer)
		{
			mTimer.cancel();
			mTimer = null;
		}
		super.Exit();
	}
}
