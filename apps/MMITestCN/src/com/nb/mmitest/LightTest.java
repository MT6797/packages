
package com.nb.mmitest;
import static android.provider.Settings.System.SCREEN_OFF_TIMEOUT;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
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
import com.nb.mmitest.R;

/*
 * Empty Test use as a default when no test was defined
 */
class LightTest extends Test {
	// TODO : values are copied from Hardware Service, check they are in sync!
	static final int LIGHT_ID_BACKLIGHT = 0;
	static final int LIGHT_ID_KEYBOARD = 1;
	
	final int version = 2;
    final int count = 5; //on off on off on
    private BackLight bl = new BackLight(version);
    private String mBlName;
    
    private TestLayout1 tl;
	PowerManager pm;
	PowerManager.WakeLock wl_keyboad;
	KeyguardManager mkeyguardManager;
	KeyguardLock mkeyguardLock;
    NotificationManager nm;
    int Notification_ID_BASE = 110; 
	LightTest(ID pid, String s) {
		this(pid,s,0);
	}
	
	LightTest(ID pid, String s,int timein ) {
		super(pid, s, timein, 0);
		
		if(mId == Test.ID.BACKLIGHT.ordinal()){
			mBlName = "screen";				
		}else{
			mBlName = "keyboard";
		}

		
	}
	
/*	private void setBacklight(int brightness) {
        try {
            IPowerManager power = IPowerManager.Stub.asInterface(
                    ServiceManager.getService("power"));
            if (power != null) {
                power.setBacklightBrightness(brightness);
                Log.i(TAG, "set brightness to :"+ new Integer(brightness).toString());
            }
        } catch (RemoteException doe) {
			Log.e(mName, "can't set backlights");
        }        
	}
/*
	private void setBacklight2(int brightness) {
        try {
            IHardwareService hw = IHardwareService.Stub.asInterface(
                    ServiceManager.getService("hardware"));
            if (hw != null) {

        hw.setLightBrightness_UNCHECKED(HardwareService.LIGHT_ID_BACKLIGHT, brightness,
                HardwareService.BRIGHTNESS_MODE_USER);
        hw.setLightBrightness_UNCHECKED(HardwareService.LIGHT_ID_KEYBOARD, brightness, 
            HardwareService.BRIGHTNESS_MODE_USER);
        hw.setLightBrightness_UNCHECKED(HardwareService.LIGHT_ID_BUTTONS, brightness,
            HardwareService.BRIGHTNESS_MODE_USER);
                Log.i(TAG, "set brightness to :"+ new Integer(brightness).toString());

            hw.setFlashlightEnabled(true);
            }
        } catch (RemoteException doe) {
			Log.e(mName, "can't set backlights");
        }        
	}
*/
/*	private void setBacklightNormalState() {
			bl.setLcdBacklight(102);
			bl.setKbdBacklight(0);
	}

	private void ToggleBacklight() {
			setBacklight((mState & 1) == 0 ? PowerManager.BRIGHTNESS_OFF : PowerManager.BRIGHTNESS_ON);
	}

	private void ToggleBacklight2() {
		if(mId == Test.ID.BACKLIGHT.ordinal()){
			bl.setLcdBacklight((mState & 1) == 0 ? PowerManager.BRIGHTNESS_OFF: PowerManager.BRIGHTNESS_ON);
		}else{
			bl.setKbdBacklight((mState & 1) == 0 ? PowerManager.BRIGHTNESS_OFF: PowerManager.BRIGHTNESS_ON);
		}
	}

	private void ToggleBacklight3() {
		pm = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
		wl_keyboad = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, TAG);
		mkeyguardManager = (KeyguardManager)mContext.getSystemService(Context.KEYGUARD_SERVICE);  
		mkeyguardLock = mkeyguardManager.newKeyguardLock(""); 
		
		StopTimer();
		if(mState < 7){
			SetTimer(500, new CallBack(){
				public void c(){
					ToggleBacklight3();
				}
			});
		}
		
		if(mId == Test.ID.BACKLIGHT.ordinal()){ /* screen back light */
/*			switch (mState){
			case 1: /* flashing 3 times only */
/*			case 3:
			case 5:
				setBacklight(PowerManager.BRIGHTNESS_ON);
				Log.i(TAG, "ToggleBacklight3,mState = "+ mState);
				break;
			default:
				setBacklight(PowerManager.BRIGHTNESS_OFF);
				tl = new TestLayout1(mContext, mName,"Is " + mBlName + " Backlight flashing?");
				//if(!mTimeIn.isFinished())
				//        tl.setEnabledButtons(false);
				break;
			}
			mState++;
		}else{ /* keyboard back light */
/*			switch(mState){
			case 1:
			case 3:
			case 5:
				//wl_keyboad.acquire();
				//WXKJRapi.setKbdBacklight(1);
				bl.setKbdBacklight(PowerManager.BRIGHTNESS_ON);
				break;
			default:
				//wl_keyboad.release();
				//WXKJRapi.setKbdBacklight(0);
				bl.setKbdBacklight(PowerManager.BRIGHTNESS_OFF);
				break;
			}
			mState++;
		}
	}
		
	/* use simple timer for schedule task - blindy screen or kb */
	private Timer mTimer = null;
	private TimerTask mTimerTask = null;
	class CToggleBacklight extends TimerTask{
		private int mFlag = 0;
		private int mId;
		private boolean mFlash = false;
		
		CToggleBacklight(int id){
			mId = id;
		}
		
		
		@Override
		public void run(){
			pm = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
			wl_keyboad = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, TAG);

			
			if(mFlash)
			{
				if(mId == Test.ID.BACKLIGHT.ordinal()){
					setbacklight(true);
					mFlash = false;
				}
			}else
			{

				if(mId == Test.ID.BACKLIGHT.ordinal()){
					setbacklight(false);
					mFlash = true;
				}
			}
		}
	}
	
	@Override
	protected void Run() {
		switch (mState) {
		case END:
			//setBacklight(PowerManager.BRIGHTNESS_ON);
			break;
			
		default:
			/* flash this time, nok in auto test mode */
			/* ToggleBacklight3(); */
			
			/* new way for flash screen, in order to enable flash in auto test mode */			
			/* display content */
			if(mId == Test.ID.KBD_BACKLIGHT.ordinal()){
				tl = new TestLayout1(mContext, mName, getResource(R.string.keyboard_backligth));				
				writeLcdFile("/sys/class/leds/red/trigger", "timer");
				writeLcdFile("/sys/class/leds/red/brightness", "255");
				try{
					Thread.sleep(100);
				}catch(Exception e)
				{
					
				}
				writeLcdFile("/sys/class/leds/red/delay_on", "500");
				writeLcdFile("/sys/class/leds/red/delay_off", "500");
			}else
			{
				tl = new TestLayout1(mContext, mName, getResource(R.string.screen_backligth));	
				if(mTimer==null)
				{
					mTimer = new Timer();
					mTimerTask = new CToggleBacklight(mId);
					mTimer.schedule(mTimerTask, 0, 200);
				}

			}
			mContext.setContentView(tl.ll);
			break;
		}
	}
	
	@Override
	protected void onTimeInFinished() {
	    if(tl!=null)
	    tl.setEnabledButtons(true);
		Log.i(TAG, "onTimeInFinished");
		if(mId == Test.ID.KBD_BACKLIGHT.ordinal())
		{
			 writeLcdFile("/sys/class/leds/red/trigger", "none");
		}
	}

	@Override
	public void Exit() {
		//setBacklightNormalState();
		//setBacklight(PowerManager.BRIGHTNESS_OFF);
		if(null != mTimer)
		{
			mTimer.cancel();
			mTimer = null;
		}
		
		if(wl_keyboad != null && wl_keyboad.isHeld()){
			
			Log.i(TAG, "release");
			wl_keyboad.release();
			
		}
		if(mId == Test.ID.KBD_BACKLIGHT.ordinal())
		{
			// writeLcdFile("/sys/class/leds/button-backlight/brightness", "0");
			// nm.cancel(Notification_ID_BASE);
			 writeLcdFile("/sys/class/leds/red/trigger", "none");
		}else
			//setbacklight(true);
		{
			writeLcdFile("/sys/class/leds/lcd-backlight/brightness", "102");
			//Settings.System.putInt(mContext.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, 102);
		}
		Log.i(TAG, "Exit");
		super.Exit();
	}
       //PR401353-yancan-zhu-20130130 begin
      public  void writeLcdFile(String filename, String value) {
        try {
        	   // RandomAccessFile fa;
				//fa = new RandomAccessFile(filename, "rw");
				
            FileWriter writer = new FileWriter(filename);
            try {
                writer.write(value);
            	//fa.writeInt(value);
                Log.e(TAG, "filename:"+filename+" write value:"+value);
            } catch (IOException e) {
                Log.e(TAG, "IOException caught while writting "+e.toString());
            }
            writer.close();
            //fa.close();
        } catch (IOException e) {
            Log.e(TAG, "IOException caught while writting stream "+e.toString());
        }
    }

    public  void setbacklight(boolean on) {
        if (on) {
            writeLcdFile("/sys/class/leds/lcd-backlight/brightness", "255");
           // Settings.System.putInt(mContext.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, 255);
        } else {
            writeLcdFile("/sys/class/leds/lcd-backlight/brightness", /*102*/"10");//modify by baohua.peng for MMItest
           // Settings.System.putInt(mContext.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, 10);
        }
    }
   //PR401353-yancan-zhu-20130130 end
}
