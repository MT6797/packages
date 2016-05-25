
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

/*
 * BT Test use as a default when no test was defined
 * add by stephen.huang
 */
class BluetoothTest extends Test {   
 
    // Member fields
    private BluetoothAdapter mBtAdapter;

    ArrayList<String> mDevicesList = new ArrayList<String>();
    
    private int mBluetoothState = 0, mBluetoothOldState = 0;
    
    private final String TAG = super.TAG + "BTTEst";

    private IntentFilter filter;
    
    private TestLayout1 tl;
    
    private final int STATE_SEARCHING = 100;

    BluetoothTest(ID pid, String s) {
        super(pid, s);

        hKey = new KeyHandler() {
			public boolean handleKey(KeyEvent event) {
				if(event.getAction() == KeyEvent.ACTION_UP )
				if (event.getKeyCode() == KeyEvent.KEYCODE_BACK ) {
		    		if(mState != BluetoothAdapter.STATE_OFF) {
		    			Log.i(TAG, "waiting BT ends");
		    			mBtAdapter.disable();
		    		}

					return true;
				}
				return false;
			}
		};
        
        
    }

    protected void onExit() {

    	try{ 
		if(mState != BluetoothAdapter.STATE_OFF) {
		       Log.i(TAG, "waiting BT ends");
		    	mBtAdapter.disable();
	       }
		if(mBTReceiver != null){
    			mContext.unregisterReceiver(mBTReceiver);
			//if(mName.equals("Bluetooth")){
    			if(true){
				Log.d(TAG, "onExit got the mName = "+ mName+ " --jiqian !!!");
				ExecuteTest.currentTest.Exit();
			}
		}
    	}catch ( Exception e) {
    		Log.e(TAG, "already unregistered "+mName);
    	}
    }

    /**
     * Start device discover with the BluetoothAdapter
     */
    private void doDiscovery() {
        Log.d(TAG, "doDiscovery()");

        // If we're already discovering, stop it
        if (mBtAdapter.isDiscovering()) {
            mBtAdapter.cancelDiscovery();
        }
        // Request discover from BluetoothAdapter
        mBtAdapter.startDiscovery();
    }

 

    @Override
    protected void Run() {
        // this function executes the test
        switch (mState) {
      case  INIT:
    	  
    	  mDevicesList.clear();    
          // Register for broadcasts when a device is discovered
     filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
     filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
     filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
     filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
     mContext.registerReceiver(mBTReceiver, filter);


     // Get the local Bluetooth adapter
     mBtAdapter = BluetoothAdapter.getDefaultAdapter();
     
     mBluetoothState = mBtAdapter.getState();
     
     if (!mBtAdapter.isEnabled()) {
    	 mBtAdapter.enable();
         tl = new TestLayout1(mContext,mName,getResource(R.string.turn_on_bt));
         mContext.setContentView(tl.ll);
         
     }else {
    	 Log.d(TAG, "BT already ON!");
		  tl = new TestLayout1(mContext, mName, 
				  getResource(R.string.bt_discovery),
				  getResource(R.string.fail),getResource(R.string.pass),
				  mLeftButton, mRightButton);
		  mBluetoothOldState = BluetoothAdapter.STATE_ON;
		  doDiscovery();
     }
/*     
     SetTimer(15000, new CallBack() {
    	public void c() {
    		mState = TIMEOUT;
    		Run();
    	}
     });
*/
     mContext.setContentView(tl.ll);

    break;

      case BluetoothAdapter.STATE_OFF :
      if(mBluetoothOldState == BluetoothAdapter.STATE_TURNING_OFF ) {
    	  onExit();
      }
    	     break;
      case BluetoothAdapter.STATE_TURNING_ON :
    	  tl = new TestLayout1(mContext,mName,getResource(R.string.bt_enable));
    	  tl.hideButtons();
    	  mContext.setContentView(tl.ll);
    	     
    	     break;
      case BluetoothAdapter.STATE_ON :
    	  if(mDevicesList.isEmpty()) {
    		  tl = new TestLayout1(mContext,mName,getResource(R.string.bt_start));
        	  tl.hideButtons();
    		  mContext.setContentView(tl.ll);
        	  doDiscovery();

    	  }else{
         	  tl = new TestLayout1(mContext, mName, 
         			 getResource(R.string.bt_ok)+getResource(R.string.bt)+":"+mDevicesList.get(0),
    				 getResource(R.string.fail),getResource(R.string.pass),
    				  mLeftButton, mRightButton);
        	  mContext.setContentView(tl.ll);

    		  Log.d(TAG,"Discovery finished");
    	  }
 	     break;

      case BluetoothAdapter.STATE_TURNING_OFF :
    	  tl = new TestLayout1(mContext,mName,getResource(R.string.bt_disable));
    	  tl.hideButtons();
    	  mContext.setContentView(tl.ll);
    	     
    	     break;
    	     
      case STATE_SEARCHING :
    	  StopTimer();
     	  tl = new TestLayout1(mContext, mName, 
				  mDevicesList.isEmpty() ? getResource(R.string.bt_search) : getResource(R.string.bt_ok)+getResource(R.string.bt)+":"+
				  mDevicesList.get(0),
				  getResource(R.string.fail),getResource(R.string.pass),
				  mLeftButton, mRightButton);
	 if (mDevicesList.isEmpty()){
	   tl.setEnabledButtons(false);
	 }
 
	 if(MMITest.mgMode == MMITest.AUTO_MODE){
	    tl.setEnabledButtons(false);
	 }
    	  mContext.setContentView(tl.ll);
    	  break;


     case TIMEOUT:
     
     String error;
     if(mBluetoothOldState == BluetoothAdapter.STATE_TURNING_ON ) {
       error = "\nBT Enable failed";
     }else {
       error = "\nno device found";
     }
     		  tl = new TestLayout1(mContext, mName, 
				  error,
				  getResource(R.string.fail),getResource(R.string.pass),
				  mLeftButton, mRightButton);
	  mContext.setContentView(tl.ll);

      // If we're discovering, stop it
      if (mBtAdapter.isDiscovering()) {
          mBtAdapter.cancelDiscovery();
      }

     break;
     
     case END:
    	 StopTimer();
     break;
     }
        /* only in auto test mode, do autorun */
		if(false == mDevicesList.isEmpty()||mState == STATE_SEARCHING){
			if(MMITest.mgMode == MMITest.AUTO_MODE){
				AutoRun();
			}
		}
    }
    
    @Override
	protected void AutoRunCallback(){
		/* impl logic in desired page */
		switch(mState){
		/* at battery temperature display state */
		case STATE_SEARCHING:
			if (false == mDevicesList.isEmpty()){
				Result = Test.PASSED;
				onExit();
			}
			break;
		default:
			break;
		}
    }
   
    // The BroadcastReceiver that listens for discovered devices and
    // changes the title when discovery is finished
    private final BroadcastReceiver mBTReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            // When discovery finds a device
            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
            	/* STATE_OFF, STATE_TURNING_ON, STATE_ON, STATE_TURNING_OFF */
                mBluetoothState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, 0);
                mBluetoothOldState = intent.getIntExtra(BluetoothAdapter.EXTRA_PREVIOUS_STATE,0);
                
                mState = mBluetoothState;
                
                Hashtable<Integer,String> h = new Hashtable<Integer,String>();
                h.put(BluetoothAdapter.STATE_OFF,"STATE_OFF");
                h.put(BluetoothAdapter.STATE_ON,"STATE_ON");
                h.put(BluetoothAdapter.STATE_TURNING_OFF,"STATE_TURNING_OFF");
                h.put(BluetoothAdapter.STATE_TURNING_ON,"STATE_TURNING_ON");
                Log.d(TAG, "state changed "+h.get(mBluetoothOldState)+"=>" + h.get(mBluetoothState) );
                	
            }else if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (!mDevicesList.contains(device.getAddress()) ) {
                    mDevicesList.add(device.getAddress());
                    Log.d(TAG, Arrays.toString(mDevicesList.toArray()));
                }else{
                	return;
                }
            // When discovery is finished, change the Activity title
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                if (mDevicesList.size() == 0) {
                    mDevicesList.add("No device found");
                    Log.d(TAG, Arrays.toString(mDevicesList.toArray()));
                }
                mState = BluetoothAdapter.STATE_ON;
            } else if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
            	mState = STATE_SEARCHING;
            }else {
            	return; /* don't change test state */
            }
            
            Run();
        }
    };
   
    private View.OnClickListener mRightButton = new View.OnClickListener() {
    	public void onClick(View v) {
    		
    		ExecuteTest.currentTest.Result=Test.PASSED;

    		if(mState != BluetoothAdapter.STATE_OFF) {
    			Log.i(TAG, "waiting BT ends");
    			mBtAdapter.disable();
    		}

    	}
    };

    private View.OnClickListener mLeftButton = new View.OnClickListener() {
    	public void onClick(View v) {
  
    		ExecuteTest.currentTest.Result=Test.FAILED;

    		if(mState != BluetoothAdapter.STATE_OFF) {
    			Log.i(TAG, "waiting BT ends");
    			mBtAdapter.disable();
    		}

    	}
    };
    
}
