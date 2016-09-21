
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

import android.content.DialogInterface;
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
import com.android.fmradio.FmNative;

/*
 * Empty Test use as a default when no test was defined
 */
class GSensorTest extends Test implements SensorEventListener {

	TestLayout1 tl;
    String mDisplayString;
    DDDVect mDir;
    private enum position { UNDEF,UP, DOWN, LEFT, RIGHT, FACE_UP, FACE_DOWN };
    
    private int POS_BIT_UP        = 0x1;
    private int POS_BIT_DOWN      = 0x2;
    private int POS_BIT_LEFT      = 0x4;
    private int POS_BIT_RIGHT     = 0x8;
    private int POS_BIT_FACE_DOWN = 0x10;
    private int POS_BIT_FACE_UP   = 0x20;
    private int POS_BIT_ALL       = POS_BIT_UP | POS_BIT_DOWN | POS_BIT_LEFT | POS_BIT_RIGHT | POS_BIT_FACE_DOWN | POS_BIT_FACE_UP;

	private SensorManager mSensorManager;
    //private float[] mValues;
    private String TAG = "GSensor";
    private int mCount=0;
    private position mPosition=position.UNDEF;
    int mPositionChecked = 0;
    
    private Sensor gsensor;
    
   	Bitmap mBitmap;
 
    GSensorTest(ID pid, String s) {
		super(pid, s);
		TAG = Test.TAG + TAG;
		
	}
	
    GSensorTest(ID pid, String s,int timein) {
		super(pid, s, timein, 0);
		TAG = Test.TAG + TAG;
		
	}
	

	@Override
	protected void Run() {
		// this function executes the test
		switch (mState) {
		case INIT: // init the test, shows the first screen

//            mTimeIn.start();

			if(mSensorManager == null) {
				mSensorManager = (SensorManager)mContext.getSystemService(Context.SENSOR_SERVICE);
			}
			// first screen is to check UP position only (for AUTO mode )
			mPositionChecked=POS_BIT_ALL & ~POS_BIT_UP;


			gsensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

	        if (gsensor != null ) {
	        	Log.i(TAG, "GSensor opened : "+gsensor.getName());
	        if (!mSensorManager.registerListener(this, gsensor, SensorManager.SENSOR_DELAY_NORMAL)){
	        	Log.e(TAG, "register listener for sensor "+gsensor.getName()+" failed");
	        }
			}else{
				tl = new TestLayout1(mContext,mName,getResource(R.string.sensor_no));
				mContext.setContentView(tl.ll);
			}

			tl = new TestLayout1(mContext,mName,(View)new PositionView(mContext));
			mContext.setContentView(tl.ll);
			
//			if(!mTimeIn.isFinished())
			    tl.setEnabledButtons(false);
/*
	        if (MMITest.mode == MMITest.AUTO_MODE){
	        	SetTimer(5000, new CallBack (){ 
	        		public void c(){
	        			mState = TIMEOUT;
	        			Run();
	        		}
	        	});
	        }
*/			
			setPCBAButton();
			mState++;
			break;
			
		case INIT+1: 
			// check UP is ok : reset all other positions status
			// TODO : in case of AUTO test, stop here
			if(mPositionChecked == POS_BIT_ALL){
				mPositionChecked = POS_BIT_UP;
				
				//if (MMITest.mode == MMITest.AUTO_MODE){
					StopTimer();
				//}
				
				mState++;
				/* trigger a new screen refresh as the display is only updated when a position change has been detected */ 
				SetTimer(10, new CallBack (){ 
	        		public void c(){
	        			Run();
	        			}
	        	});

			}else
			{
				tl = new TestLayout1(mContext,mName,(View)new PositionView(mContext));
				tl.setEnabledButtons(false);
				mContext.setContentView(tl.ll);
			}
			

			break;
			
			
		case TIMEOUT:
			tl = new TestLayout1(mContext,mName,getResource(R.string.sensor_timeout),getResource(R.string.fail),getResource(R.string.pass));
			mContext.setContentView(tl.ll);

			break;

		case END://
				mSensorManager.unregisterListener(this,gsensor);
				Log.d(TAG, "gsensor listener unregistered");

			break;
			
		default:
            if (mPositionChecked != POS_BIT_ALL){
				tl = new TestLayout1(mContext,mName,(View)new PositionView(mContext));
				tl.setEnabledButtons(false);
				mContext.setContentView(tl.ll);
			}else{
				/* all positions checked */
				StopTimer();
				tl = new TestLayout1(mContext,mName,getResource(R.string.sensor_check_all));
				mContext.setContentView(tl.ll);
				mState=END;
				if(MMITest.mgMode == MMITest.AUTO_MODE) {
					ExecuteTest.currentTest.Result=Test.PASSED;
					ExecuteTest.currentTest.Exit();
				}
			}
			/* enable layout buttons only after TimeIn elapsed */
//			if(!mTimeIn.isFinished())
//			    tl.setEnabledButtons(false);
		break;
		}

	}
	
    class DDDVect {
        float Vx;
        float Vy;
        float Vz;
    
        DDDVect(float x, float y, float z) {
            Vx=x;Vy=y;Vz=z;
        }
        public String toString()
        {
        	return "X:"+Vx+"  Y:"+Vy+" Z:"+Vz;
        }
        
        public float getYAngle() {
            return getAngle(Vy);
        }
        
        public float getXAngle() {
            return getAngle(Vx);
        }
        
        public float getZAngle() {
            return getAngle(Vz);
        }
        
        private float getAngle(float ref) {
            return (float) Math.toDegrees(Math.acos(ref/Math.sqrt(Vx*Vx+Vy*Vy+Vz*Vz)));
        }

    }
    
    float mAngleToYaxis;
    float mAngleToXaxis;
    float mAngleToZaxis;
    
    public void onSensorChanged(SensorEvent event) {
        	
        	float ThresholdHigh = (float) 8.0;
        	float ThresholdLow = (float) 2.0;
        	position mOldPosition = mPosition;
        	
        	
            Log.d(TAG, "onSensorChanged: (" + event.values[0] + ", " + event.values[1] + ", " + event.values[2] + ")");
            //mValues = event.values;
            
            mDir = new DDDVect(event.values[0],event.values[1],event.values[2]);
            
            mAngleToYaxis = mDir.getYAngle();
            mAngleToXaxis = mDir.getXAngle();
            mAngleToZaxis = mDir.getZAngle();

            mCount++;
            if ( mAngleToYaxis < 15 ){
            	mPosition = position.UP;
            	mPositionChecked |= POS_BIT_UP;
            }else if ( mAngleToYaxis > 165 ){
            	mPosition = position.DOWN;
            	mPositionChecked |= POS_BIT_DOWN;
            }else if ( mAngleToXaxis < 15 ){
            	mPosition = position.LEFT;
            	mPositionChecked |= POS_BIT_LEFT;
            }else if ( mAngleToXaxis > 165 ){
            	mPosition = position.RIGHT;
            	mPositionChecked |= POS_BIT_RIGHT;
            }else if ( mAngleToZaxis > 165 ){
            	mPosition = position.FACE_DOWN;
            	mPositionChecked |= POS_BIT_FACE_DOWN;
            }else if ( mAngleToZaxis < 15 ){
            	mPosition = position.FACE_UP; 
            	mPositionChecked |= POS_BIT_FACE_UP;
            }else{
            	mPosition = position.UNDEF;
            }
 
            if ( mOldPosition != mPosition ) {
                Log.d(TAG,"handest position changed :"+mPosition+" (checked = "+ mPositionChecked+")");
                Log.d(TAG,"V : "+mAngleToYaxis+" deg , H : "+mAngleToXaxis+" deg");
               // Run();
            }
            Run();
                Log.d(TAG,"V : "+mAngleToYaxis+" deg , H : "+mAngleToXaxis+" deg");

    }

    public void onAccuracyChanged(Sensor sensor, int accuracy) {
            // TODO Auto-generated method stub
            //Log.i(TAG,"sensor"+sensor.getName()+" accuracy changed :"+accuracy);
    }
        
 
    private class PositionView extends View {
        private Paint   mPaint = new Paint();
        

        public PositionView(Context context) {
            super(context);
            
    		mBitmap = android.graphics.BitmapFactory.decodeResource(getResources(), R.drawable.phone );
        }
    
        @Override protected void onDraw(Canvas canvas) {
            Paint paint = mPaint;

            canvas.drawColor(Color.WHITE);
            int length = 140, offset = 60, border = 20,textSize=20;
            
            if(Lcd.width() == 1080)
            {
            	length = 380;
            	offset = 90;
            	border = 30;
            	textSize = 40;
            }else if(Lcd.width() == 1440)
	    {
            	length = 500;
            	offset = 120;
            	border = 45;
            	textSize = 60;
	    }
            paint.setAntiAlias(true);
            paint.setColor(Color.BLACK);
            paint.setStyle(Paint.Style.FILL);
            paint.setStrokeWidth(4);

            int w = canvas.getWidth();
            int h = canvas.getHeight();
            int cx = w / 2;
            int cy =  h / 2 - 40 ;

            canvas.translate(cx, cy);
            if( mDir!=null)
            {
            paint.setTextSize(textSize);
            canvas.drawText(mDir.toString(), -150, length+200, paint);
            }
            
            /* UP LINE */
            if( (mPositionChecked & POS_BIT_UP) == 0){
            	canvas.drawLine(0, -offset, 0, -(length+offset), paint);
            	canvas.drawLine(-border, -(offset+length-border), 0, -(length+offset), paint);
            	canvas.drawLine( border, -(offset+length-border), 0, -(length+offset), paint);
            	Matrix m = new Matrix();
            	m.setTranslate(10, -length );
            	paint.setColor(Color.BLUE);
				paint.setTextSize(textSize);
              	canvas.drawBitmap(mBitmap, m, paint);

//            	canvas.drawBitmap(mBitmap, border , -length + border, null);
            	canvas.drawText(getResource(R.string.sensor_handset_up), -Lcd.width()/2 + border , 0, paint);
            }
            
            /*wtchen: Face up testing*/
            if( (mPositionChecked & POS_BIT_FACE_UP) == 0){
               canvas.drawLine(-50, -offset*2, -50, -(length/2+offset*2), paint);
               canvas.drawLine(-50-border, -(offset*2+length/2-border), -50, -(offset*2+length/2), paint);
               canvas.drawLine(-50+border, -(offset*2+length/2-border), -50, -(offset*2+length/2), paint);
			   paint.setTextSize(textSize);
               //canvas.drawText("face up", -50+border , -offset*2-border, paint);
			   canvas.drawText(getResource(R.string.sensor_face_up), -length , -offset*2-border, paint);		
             }
            if( (mPositionChecked & POS_BIT_FACE_DOWN) == 0){
            	canvas.drawLine(50, -offset*2, 50, -(length/2+offset*2), paint);
               canvas.drawLine(50-border, -(offset*2+border), 50, -(offset*2), paint);
               canvas.drawLine(50+border, -(offset*2+border), 50, -(offset*2), paint);
			   paint.setTextSize(textSize);
               canvas.drawText(getResource(R.string.sensor_face_down), 50+border , -offset*2-border, paint);

            }

            /* DOWN LINE */
            if((mPositionChecked & POS_BIT_DOWN) == 0){
            	Matrix m = new Matrix();
            	m.setTranslate(10, -length);
            	m.postRotate(180);
            	canvas.drawLine(0, offset, 0, length+offset, paint);
            	canvas.drawLine(-border, (offset+length-border), 0, length+offset, paint);
            	canvas.drawLine( border, (offset+length-border), 0, length+offset, paint);
				paint.setTextSize(textSize);
            	canvas.drawText(getResource(R.string.sensor_down), 10, length, paint);
            	canvas.drawBitmap(mBitmap, m, null);

            }

            /* LEFT LINE */
            if((mPositionChecked & POS_BIT_LEFT) == 0){
            	Matrix m = new Matrix();
            	m.setTranslate(border , -length  );
            	m.postRotate(270);

              	canvas.drawBitmap(mBitmap, m, paint);

            	canvas.drawLine( -offset,0 , -(length+offset),0, paint);
            	canvas.drawLine(-(offset+length-border), -border, -(length+offset), 0, paint);
            	canvas.drawLine(-(offset+length-border),  border, -(length+offset), 0, paint);
				paint.setTextSize(textSize);
            	canvas.drawText(getResource(R.string.sensor_left), -length, 50, paint);
            	canvas.drawBitmap(mBitmap, m, null);

            }

            /* RIGHT LINE */
            if((mPositionChecked & POS_BIT_RIGHT) == 0){

            	Matrix m = new Matrix();
            	m.setTranslate(- mBitmap.getWidth() - border, -length );
            	m.postRotate(90);

              	canvas.drawBitmap(mBitmap, m, paint);

            	canvas.drawLine( offset,0 , (length+offset),0, paint);
            	canvas.drawLine((offset+length-border), -border, (length+offset), 0, paint);
            	canvas.drawLine((offset+length-border),  border, (length+offset), 0, paint);
				paint.setTextSize(textSize);
            	canvas.drawText(getResource(R.string.sensor_right), length-50, 50, paint);
            }

 
       }
    
        @Override
        protected void onAttachedToWindow() {
            super.onAttachedToWindow();
        }
        
        @Override
        protected void onDetachedFromWindow() {
            super.onDetachedFromWindow();
        }
    }
    @Override
    protected void onTimeInFinished() {
        if(tl!=null)
            tl.setEnabledButtons(true);
    }
    
    private Button btnPcba;
	private void setPCBAButton(){
		btnPcba = new Button(mContext);
		btnPcba.setText("PCBA");
		btnPcba.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				new AlertDialog.Builder(mContext)
				.setTitle("PCBA").setMessage(
	            		"x = " + Float.toString(mAngleToXaxis) + '\n' + 
	            		"y = " + Float.toString(mAngleToYaxis) + '\n' + 
	            		"z = " + Float.toString(mAngleToZaxis) + '\n' + 
	            		"G-Sensor test OK!"
	            ).setPositiveButton(getResource(R.string.pass), new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						ExecuteTest.currentTest.Result = Test.PASSED;
						ExecuteTest.currentTest.Exit();
					}
				}).setNegativeButton(getResource(R.string.fail), new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						ExecuteTest.currentTest.Result = Test.FAILED;
						ExecuteTest.currentTest.Exit();
					}
				}).show();
			}
		});
		this.tl.ll.addView(btnPcba, 1);
	}
}
