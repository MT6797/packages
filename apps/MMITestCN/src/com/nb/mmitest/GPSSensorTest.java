
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

import java.util.NoSuchElementException;
import com.nb.mmitest.R;

/*
 * Empty Test use as a default when no test was defined
 */
class GPSSensorTest extends Test implements LocationListener {

	TestLayout1 tl;
    String mDisplayString;

	private LocationManager mLocationManager;
    private float[] mValues;
    private String TAG = "GPS";
    private int mStatusCount=0, mLocationCount=0;
    
    private double mLatitude=0;
    private double mLongitude=0;
    //private Location mLocation=null;
    private GpsStatus mGpsStatus=null;
    
    private boolean mPRN1Found = false;

    
    GPSStatusListener mGpsStatusListener = new GPSStatusListener();
 
    GPSSensorTest(ID pid, String s) {
		super(pid, s);
		TAG = Test.TAG + TAG;
	}
	

	@Override
	protected void Run() {
		// this function executes the test
		switch (mState) {
		case INIT: // init the test, shows the first screen
			mStatusCount=0; mLocationCount=0;
	        mLocationManager = (LocationManager)mContext.getSystemService(Context.LOCATION_SERVICE);
	        
	        tl = new TestLayout1(mContext,mName,getResource(R.string.gps_init),getResource(R.string.fail),getResource(R.string.pass));
	        tl.hideButtons();
			mContext.setContentView(tl.ll);

	        
	        if (!Settings.Secure.isLocationProviderEnabled(mContext.getContentResolver(), LocationManager.GPS_PROVIDER)) {
	        	Settings.Secure.setLocationProviderEnabled(mContext.getContentResolver(),
	                    LocationManager.GPS_PROVIDER, true);
	        }
	        Log.d(TAG, "providers : "+ mLocationManager.getAllProviders());
	        
	        //mLocation = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
	        mGpsStatus= mLocationManager.getGpsStatus(null);

            /* now register to GPS updates */ 	        
	        try{
	        mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, this);
	        }catch (Exception e){
	        	Log.d(TAG, "can't init Location Listener "+e);
	        }
	        
	        mLocationManager.addGpsStatusListener(mGpsStatusListener);

	        SetTimer(500, new CallBack() {
	         public void c(){
	        	 mState++;
	        	 Run();
	         }
	        });
			

			break;
			
		case TIMEOUT://
			
			tl = new TestLayout1(mContext,mName,getResource(R.string.gps_init_fail),getResource(R.string.fail),getResource(R.string.pass));
			mContext.setContentView(tl.ll);
			
			mState = END;

		case END://
			StopTimer();

			if (mLocationManager != null) {
				mLocationManager.removeUpdates(this);
				mLocationManager.removeGpsStatusListener(mGpsStatusListener);
				mLocationManager=null;
			}
			
			//Settings.Secure.setLocationProviderEnabled(mContext.getContentResolver(),
            //        LocationManager.GPS_PROVIDER, false);
			break;

		default:
			String sDisplay;
			int numSatellites=0;
			String SatellitesPRNs=" ";
			String SatellitesSNRs="";
			
			/* first pass : set TIMEOUT  */
			if( MMITest.mode == MMITest.AUTO_MODE && mState == INIT+1) {
				StopTimer();
/*				SetTimer(60000, new CallBack() {
					public void c(){
						mState = TIMEOUT;
						Run();
					}
				});
*/				
			}

			
			if (mGpsStatus != null) {
				Iterator<GpsSatellite> mSatArray = mGpsStatus.getSatellites().iterator(); 
				while (mSatArray.hasNext()){
					numSatellites++;
					int PRN = 0;
					float SNR = 0.0f;
					try {
						GpsSatellite mSatellite = mSatArray.next();
						PRN = mSatellite.getPrn();
						SNR = mSatellite.getSnr();
					}catch (NoSuchElementException e){
						Log.e(TAG, "BUG: Here should be a Satellite!");
					}
					if( PRN == 1 ) {
					    mPRN1Found = true;
					}
                    SatellitesPRNs += Integer.toString(PRN)+ (mSatArray.hasNext() ? "/":" ");
                    SatellitesSNRs += Float.toString(SNR)+ (mSatArray.hasNext() ? "/":" ");
				}
			
			}

			if(numSatellites>0)
			sDisplay =  getResource(R.string.gps_ok)+numSatellites+getResource(R.string.gps_satellite)+"\n"+
   						(numSatellites > 0 ?"PRN="+ SatellitesPRNs
   						+"tracked\n"+"SNR="+SatellitesSNRs+"tracked\n":"\n");
			          //  +"updates = "+mStatusCount;
			else
			sDisplay =  numSatellites+getResource(R.string.gps_satellite)+"\n"+
   						(numSatellites > 0 ?"PRN="+ SatellitesPRNs+
   						"tracked\n"+"SNR="+SatellitesSNRs+"tracked\n":"\n");
			            //+"updates = "+mStatusCount;
			
            Log.i(TAG,sDisplay);
		    tl = new TestLayout1(mContext,mName, sDisplay ,getResource(R.string.fail),getResource(R.string.pass));
		    if (numSatellites < 1)
		        tl.setEnabledButtons(false, tl.brsk);
			mContext.setContentView(tl.ll);
			mState++;
			
			if (numSatellites > 0 && MMITest.mode == MMITest.AUTO_MODE ) {
				//mState = END;
			}

			break;
		}
		
		/* only in auto test mode, do autorun */
		if(MMITest.mgMode == MMITest.AUTO_MODE){
			AutoRun();
		}
	}
	
    @Override
	protected void AutoRunCallback(){
		/* impl logic in desired page */
		switch(mState){
		/* at battery temperature display state */
		case INIT:
		case TIMEOUT:
		case END:
			break;
		default:
			if(mGpsStatus.getSatellites().iterator().hasNext()){
				Result = Test.PASSED;
				Exit();
			}
			break;
		}
	}
	
        public void onStatusChanged(String provider, int status, Bundle extras) {

            Log.d(TAG, "Provider "+provider+"Status changed to ["+status+"]");
        }
        public void  onLocationChanged(Location location){
            Log.d(TAG, "Location changed ["+location.toString());

            mLatitude  = location.getLatitude();
            mLongitude = location.getLongitude();  
            mLocationCount++;
            Run();

        }
        public void 	onProviderDisabled(String provider){
        
        }
        public void 	onProviderEnabled(String provider){
        	
        }
 

        class GPSStatusListener implements GpsStatus.Listener {

        	public void onGpsStatusChanged(int event) {
			if (mLocationManager != null) {
        		mLocationManager.getGpsStatus(mGpsStatus);
        		mStatusCount++;
        		Run();
			}
        	}
        }
}

