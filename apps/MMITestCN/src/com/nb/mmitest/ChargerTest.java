
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

class ChargerInTest extends ChargerTest {

	ChargerInTest(ID pid,String s){
		super(pid, s,true);
	}
}

class ChargerOutTest extends ChargerTest {

	ChargerOutTest(ID pid,String s){
		super(pid, s,false);
	}


}


class ChargerTest extends Test {
	int timeout=0;
	private final ChargerInBroadcastReceiver mReceiver = new ChargerInBroadcastReceiver();
	int status;
	boolean mLogicPluggedIn;
	String mExpectedState;
	String mInitialState;
	String TAG = "MMITEST:ChargerTest";
	
	int mPlugType = 0;
	
	private String AC_ONLINE_PATH = "/sys/class/power_supply/ac/online";
	private String USB_ONLINE_PATH = "/sys/class/power_supply/usb/online";
	private BufferedReader mAcPlugStatusbr;
	private String mAcPlugStatusStr="";
	private String ONLINE="1";
	private String OFFLINE = "0";
	private static long time = 0;
	
	private boolean mRegistered=false;
	
	private TestLayout1 tl;


	class ChargerInBroadcastReceiver extends BroadcastReceiver {


		public /*synchronized*/ void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (Intent.ACTION_BATTERY_CHANGED.equals(action)) {
				
/*				
				status = intent.getIntExtra("status", BatteryManager.BATTERY_STATUS_UNKNOWN);
				int BatteryLevel = intent.getIntExtra("level",-1);
				int BatteryStatus = intent.getIntExtra("status",-1);
		        int BatteryHealth = intent.getIntExtra("health", -1 );
		        boolean BatteryPresent = intent.getBooleanExtra("present",false);
		        int PlugType =  intent.getIntExtra("plugged", 0);
		        int BatteryVoltage = intent.getIntExtra("voltage", 0);
		        int BatteryTemperature = intent.getIntExtra("temperature", 0);

	            Log.d(TAG, "updateBattery level:" + BatteryLevel +
	                    " status:" + BatteryStatus + 
	                    " health:" + BatteryHealth +  
	                    " present:" + BatteryPresent + 
	                    " voltage: " + BatteryVoltage +
	                    " temperature: " + BatteryTemperature +
	                    " plug type (AC=1,USB=2)" + mPlugType +
	                    "count " + count);
*/
				Log.d(TAG, action +"received");
				time=SystemClock.elapsedRealtime();

				
				// currently we can't check the type of cable from the intent parameters, 
				// the value is wrong in the beginning. we have to wait till the USB check has been done
				// then we can get the real status of the cable
				//SystemClock.sleep(2000);
				getCableStatus();
	            
	            if(mAcPlugStatusStr.equals(ONLINE)){
	            	// charger inserted
	            	if(mLogicPluggedIn){
	            		mState=END;
	            		Run();
	            	}
	            }else if(!mLogicPluggedIn){
	            	mState=END;
	            	Run();
	            }
		    /* ajayet : currently there is a bug in SW : USB is detected as BATTERY_PLUGGED_AC */
		    /* a workaround is to detect is USB is connected, in this case we assume it is not charger */
			}else if(Intent.ACTION_UMS_CONNECTED.equals(action)){
				Log.d(TAG, "USB connected");

			}else if(Intent.ACTION_UMS_DISCONNECTED.equals(action)){
				Log.d(TAG, "USB disconnected");
			}

		}
	}

	ChargerTest(ID pid,String s, boolean logic){
		super(pid,s);

		hKey = new KeyHandler() {
			public boolean handleKey(KeyEvent event) {
				return true;
			}

		};

		mLogicPluggedIn = logic;
		
		getCableStatus();

	}

	@Override
	protected void Run() {
		// this function executes the test


		switch (mState) {
		case INIT: 
			tl = new TestLayout1(mContext,mName,"get cable status");

			SetTimer(100 , new CallBack() { 
				public void c() {
					mState++;
					Run();
				}
			});
		break;

		case INIT+1:

			time=SystemClock.elapsedRealtime();
			getCableStatus();

			if(mAcPlugStatusStr.equals(ONLINE) == mLogicPluggedIn ){
				if(mLogicPluggedIn)
					tl = new TestLayout1(mContext,mName,getResource(R.string.charger_in));
				else
					tl = new TestLayout1(mContext,mName,getResource(R.string.charger_out));
				
			}else{
				if(mLogicPluggedIn)
					tl = new TestLayout1(mContext,mName,getResource(R.string.pls_charger_insert));
				else
					tl = new TestLayout1(mContext,mName,getResource(R.string.charger_remove));

				// register plug events and wait 
				IntentFilter iflt = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
							iflt.addAction(Intent.ACTION_UMS_DISCONNECTED);
							iflt.addAction(Intent.ACTION_UMS_CONNECTED);
				mContext.registerReceiver(mReceiver, iflt);
				mRegistered=true;
                // set timeout
				SetTimer(10000 , new CallBack() { 
					public void c() {
						mState = TIMEOUT;
						Run();
					}
				});

			}


			break;



		case TIMEOUT:
//			mState =END;
			tl = new TestLayout1(mContext,mName,getResource(R.string.charger_timeout));
//			mContext.unregisterReceiver(mReceiver);

			

			break;

		case END:
		{
			StopTimer();// cancel timeout
			
			if(mLogicPluggedIn)
				tl = new TestLayout1(mContext,mName,getResource(R.string.charger_insert)
//						+"\nmAcPlugStatusStr "+mAcPlugStatusStr+
//						"time : \n"+(SystemClock.elapsedRealtime()-time)/1000.0
						);
			else
				tl = new TestLayout1(mContext,mName,getResource(R.string.charger_remove));
			
			if(mRegistered){
				mContext.unregisterReceiver(mReceiver);
				mRegistered=false;
			}
			
//			getCableStatus();
//			if(mAcPlugStatusStr.equals(ONLINE))
//			SetTimer(500 , new CallBack() { 
//				public void c() {
//					Run();
//				}
//			});

		}
		break;

		default:
			break;

		}
		mContext.setContentView(tl.ll);



	}

	private synchronized void getCableStatus(){
		try{
			mAcPlugStatusbr = new BufferedReader(new FileReader(AC_ONLINE_PATH),8);
			mAcPlugStatusStr = mAcPlugStatusbr.readLine();
			Log.i(TAG, "AC online : "+ mAcPlugStatusStr );
		}catch(IOException e){
			Log.d(TAG," plug status file can't be accessed "+e);
		}finally{
			try{
				if(mAcPlugStatusbr != null)
					mAcPlugStatusbr.close();
			}catch(IOException e){

			}
		}

	}

}
