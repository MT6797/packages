
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
class CompassTest extends Test implements SensorEventListener {

	TestLayout1 tl;
    String mDisplayString;

	private static SensorManager mSensorManager;
	private Sensor mCompass;
    private SampleView mView;
    private float[] mValues;
    private static final String TAG = "Compass";

 
 
	CompassTest(ID pid, String s) {
		super(pid, s);
	}
	
	CompassTest(ID pid, String s, int timein) {
		super(pid, s,timein, 0);
	}
	//add by xianfeng.xu for CR354436 begin
	View.OnClickListener LeftButton = new View.OnClickListener() {
		public void onClick(View v) {

			mState = END;
			Run();
			ExecuteTest.currentTest.Result = Test.FAILED;
			ExecuteTest.currentTest.Exit();
		}
	};

	View.OnClickListener RightButton = new View.OnClickListener() {
		public void onClick(View v) {
			mState++;
			Run();
		}
	};
	//add by xianfeng.xu for CR354436 end

	@Override
	protected void Run() {
		// this function executes the test
		switch (mState) {
		case INIT: // init the test, shows the first screen
		
		    mTimeIn.start();
		   	
			if(mSensorManager == null)
	            mSensorManager = (SensorManager)mContext.getSystemService(Context.SENSOR_SERVICE);
          
			mCompass = mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);

			if (!mSensorManager.registerListener(this, mCompass, SensorManager.SENSOR_DELAY_NORMAL)){
				Log.e(TAG, "register listener for sensor "+mCompass.getName()+" failed");
			}
			

			//tl = new TestLayout1(mContext,mName,"compass test start : \n move the phone in an 8 shape","Fail","Pass",LeftButton, RightButton);
			//mContext.setContentView(tl.ll);

	        mView = new SampleView(this.mContext);
	        tl = new TestLayout1(mContext,mName, (View) mView);
	        if(!mTimeIn.isFinished()) {
	            tl.setEnabledButtons(false);
	        }
	        mContext.setContentView(tl.ll/*mView*/);
			
			break;
		case END://
			
			
			mSensorManager.unregisterListener(this,mCompass);
	        	break;
			
		default:
				

	        mView = new SampleView(this.mContext);
	        tl = new TestLayout1(mContext,mName, (View) mView);
	        if(!mTimeIn.isFinished()) {
	            tl.setEnabledButtons(false);
	        }
	        mContext.setContentView(tl.ll/*mView*/);

			break;
		}

	}
	
        
    public void onSensorChanged(SensorEvent event) {
    	
        mValues = event.values;

        Log.d(TAG, "sensorChanged (" + mValues[0] + ", " + mValues[1] + ", " + mValues[2] + ")");
		

            if (mView != null) {
		
            	//tl.ll.invalidate();
               mView.invalidate();
            }
        }

    public void onAccuracyChanged(Sensor s, int accuracy) {
            // TODO Auto-generated method stub
        //Log.d(TAG, "sensor accuracy changed " + accuracy);
            
        }

 
    private class SampleView extends View {
        private Paint   mPaint = new Paint();
        private Path    mPath = new Path();
        private boolean mAnimate;
        private long    mNextTime;

        public SampleView(Context context) {
            super(context);
		
		
            // Construct a wedge-shaped path
            mPath.moveTo(0, -50);
            mPath.lineTo(-20, 60);
            mPath.lineTo(0, 50);
            mPath.lineTo(20, 60);
            mPath.close();
        }
    
        @Override protected void onDraw(Canvas canvas) {
            Paint paint = mPaint;

            canvas.drawColor(Color.WHITE);
            
		


            paint.setAntiAlias(true);
            paint.setColor(Color.BLACK);
            paint.setStyle(Paint.Style.FILL);

            int w = canvas.getWidth();
            int h = canvas.getHeight();
            int cx = w / 2;
            int cy = h / 2;

            canvas.translate(cx, cy);
            if (mValues != null) {            
                canvas.rotate(-mValues[0]);
            }
            canvas.drawPath(mPath, mPaint);
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
    
    @Override
    protected void onTimeInFinished() {
        if(tl!=null)
            tl.setEnabledButtons(true);
    }


}


