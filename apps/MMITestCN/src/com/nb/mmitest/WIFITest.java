
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
import android.os.PatternMatcher;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
 * WIFI Test use as a default when no test was defined
 * 
 */

class WIFITest extends Test {
    
    private WifiManager mWifiManager;
    
    private IntentFilter mIntentFilter;
    
    private String networkList;
    
    /** String present in capabilities if the scan result is ad-hoc */
    private static final String ADHOC_CAPABILITY = "[IBSS]";
    
    private TestLayout1 tl;
    
    WIFITest(ID pid, String s) {
        super(pid, s);
    }
    
    WIFITest(ID pid, String s, int timein ) {
        super(pid, s, timein, 0);
    }
    private boolean WIFIinit(){
    	
        mWifiManager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
        if(null == mWifiManager){
            return false;
        }
        if(mWifiManager.isWifiEnabled() == false){
            mWifiManager.setWifiEnabled(true);
            SystemClock.sleep(5000);
        }
        return true;
    }
    @Override
    protected void Run() {
        // this function executes the test
        switch (mState) {
        case INIT:
            tl = new TestLayout1(mContext,mName,getResource(R.string.wifi_init),getResource(R.string.fail),getResource(R.string.pass));
            tl.setEnabledButtons(false);
            
        	mContext.setContentView(tl.ll);
        	SetTimer(500, new CallBack(){
        		public void c() {
        			mState++;
        			Run();
        		}
        	});

        	break;
            case INIT+1: // init wifi and show status.
                if(!WIFIinit()){
                    TestLayout1 tl_failed = new TestLayout1(mContext,mName,getResource(R.string.wifi_init_fail),getResource(R.string.fail),getResource(R.string.pass));
		    tl_failed.setEnabledButtons(false);
                    mContext.setContentView(tl_failed.ll);
                    Result = Test.FAILED;
                }else{
                    TestLayout1 tl_succeed = new TestLayout1(mContext,mName,getResource(R.string.wifi_search),getResource(R.string.fail),getResource(R.string.pass));
			if(MMITest.mgMode == MMITest.AUTO_MODE){
			   tl_succeed.setEnabledButtons(false);
			}
                    mContext.setContentView(tl_succeed.ll);
                    mIntentFilter = new IntentFilter();
                    mIntentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
                    mContext.registerReceiver(mWIFIReceiver, mIntentFilter);
                    mWifiManager.startScan();
//                    StartTimeout();
                }
                    break;
            case END://
            	  try{
	    		  if(mWIFIReceiver != null){
		                mContext.unregisterReceiver(mWIFIReceiver);
		                mWifiManager.setWifiEnabled(false);
		                //StopTimer();
				   ExecuteTest.currentTest.Exit();
		  	  }
    	  	  }catch( Exception e) {
    			Log.e(TAG, "already unregistered"+mName);
  	  	  }
                break;
        }

    }

    private void StartTimeout() {
        SetTimer(15000/* miliseconds */, TimeoutCb);
    }

    CallBack TimeoutCb = new CallBack() {
        public void c() {
            TestLayout1 tl_succeed = new TestLayout1(mContext,mName,getResource(R.string.wifi_no),getResource(R.string.fail),getResource(R.string.pass));
            mContext.setContentView(tl_succeed.ll);
            Stop();
        }
    };
    
    private final BroadcastReceiver mWIFIReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (action.equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {
                handleScanResultsAvailable();
            } 
        }
    };
    
    private void handleScanResultsAvailable(){
        List<ScanResult> list = mWifiManager.getScanResults();
        if (list != null){
            for (int i = list.size() - 1; i >= 0; i--){
                final ScanResult scanResult = list.get(i);
                
                if (scanResult == null) {
                    continue;
                }
                /*
                 * Ignore adhoc, enterprise-secured, or hidden networks.
                 * Hidden networks show up with empty SSID.
                 */
                if (WIFITest.isAdhoc(scanResult)
                        || TextUtils.isEmpty(scanResult.SSID)) {
                    continue;
                }
                
                final String ssid = WIFITest.convertToQuotedString(scanResult.SSID);
                
                String rssi = Integer.toString(scanResult.level);
                
                if (ssid.startsWith("\"NVRAM WARNING: Err")) {
                	continue;
                }
                
                /*add SSID into network list and display on screen*/
                if(null == networkList){
                    StopTimer();/*succeed to find a network*/
                    networkList = ssid + "," + rssi ;
                    networkList += "\n";
                }else{
                    if(networkList.indexOf(ssid) < 0){
                        networkList += ssid + "," + rssi ;
                        networkList += "\n";
                    }
                }
            }
            if(null != networkList){
	            Result = Test.PASSED;
	            TestLayout1 tl_networkFound = new TestLayout1(mContext,mName,getResource(R.string.wifi_ok)+networkList,getResource(R.string.fail),getResource(R.string.pass));
				
		     if(MMITest.mgMode == MMITest.AUTO_MODE){
  	              tl_networkFound.setEnabledButtons(false);
		     }
				
	            mContext.setContentView(tl_networkFound.ll);
				if (MMITest.mgMode == MMITest.AUTO_MODE) {
					SetTimer(1000, new CallBack() {
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
            }
        }
    }
    
    public static String convertToQuotedString(String string) {
        if (TextUtils.isEmpty(string)) {
            return "";
        }
        
        final int lastPos = string.length() - 1;
        if (lastPos < 0 || (string.charAt(0) == '"' && string.charAt(lastPos) == '"')) {
            return string;
        }
        
        return "\"" + string + "\"";
    }
    
    public static boolean isAdhoc(ScanResult scanResult) {
        return scanResult.capabilities.contains(ADHOC_CAPABILITY);
    }
}
/*End add*/
