
package com.nb.mmitest;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorListener;
import android.hardware.SensorManager;

import android.content.Context;
import android.util.Log;
import android.view.Gravity;
import android.widget.TextView;
import android.graphics.Color;


/*
 * Empty Test use as a default when no test was defined
 */
class LightSensorTest extends Test implements SensorEventListener {

	private int HIGH = 500;
	private int LOW = 100;
	private int fh = -1;
	private int fl = -1;
	
	TestLayout1 tl;
	private TextView tvbody;
	
	private SensorManager mSensorManager;
	private String TAG = "LightSensor";

	private Sensor lsensor;

	LightSensorTest(ID pid, String s) {
		super(pid, s);
		TAG = Test.TAG + TAG;

	}

	LightSensorTest(ID pid, String s,int timein) {
		super(pid, s, timein, 0);
		TAG = Test.TAG + TAG;

	}


	@Override
	protected void Run() {
		// this function executes the test
		switch (mState) {
		case INIT: // init the test, shows the first screen

			fl = -1;
			fh = -1;
			
			if(mSensorManager == null) {
				mSensorManager = (SensorManager)mContext.getSystemService(Context.SENSOR_SERVICE);
			}

			lsensor = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
			if (lsensor != null ) {
				Log.i(TAG, "LightSensor opened : "+lsensor.getName());
				if (!mSensorManager.registerListener(this, lsensor, SensorManager.SENSOR_DELAY_NORMAL)){
					Log.e(TAG, "register listener for sensor "+lsensor.getName()+" failed");
				}
			}else{
				tl = new TestLayout1(mContext,mName,getResource(R.string.sensor_not_found));
				mContext.setContentView(tl.ll);
			}
			
			tvbody = new TextView(mContext);
			tvbody.setGravity(Gravity.CENTER);
			// tvbody.setTypeface(Typeface.MONOSPACE, 1);
			tvbody.setTextAppearance(mContext, android.R.style.TextAppearance_Large);
			tvbody.setText("opening ...");

			//tl = new TestLayout1(mContext,mName, tvbody, "PASS", "FAIL");
			tl = new TestLayout1(mContext,mName, tvbody);
			tl.setEnabledButtons(false, tl.brsk);
			tl.setEnabledButtons(true, tl.blsk);
			mContext.setContentView(tl.ll);

			Log.e(TAG, "deng  for sensor "+ mState);
			//modify by xianfeng.xu for CR364998 begin
			//mState++;
			SetTimer(500, new CallBack() {
			    public void c() {
			        mState++;
			        Run();
			    }
			});
			//mTimeIn.start();
			break;

		case INIT+1:
		    String s = getResource(R.string.light_change)+"\n\n";
		    s = s + getResource(R.string.dark_fail,Integer.toString(LOW));;
		    s = s + getResource(R.string.bright_fail,Integer.toString(HIGH));
		    tvbody.setText(s);
		    mState++;
		    break;
		    //modify by xianfeng.xu for CR364998 end
		case END://
		default:
			mSensorManager.unregisterListener(this,lsensor);
			Log.d(TAG, "light sensor listener unregistered");
			StopTimer();
			break;
		}
	}
    
	public void onSensorChanged(SensorEvent event) {

		Log.d(TAG, "onSensorChanged: (" + event.values[0] + ", " 
				+ event.values[1] + ", " + event.values[2] + ")");
		
		int value = (int)event.values[0];	
		if(value < LOW){
			fl = 1;
		}else if(value > HIGH){
			fh = 1;
		}

		String s = getResource(R.string.light_tip) + value + "\n\n";
		if(fl>0){
			s = s +getResource(R.string.dark_ok,Integer.toString(LOW));
		}else{
			s = s + getResource(R.string.dark_fail,Integer.toString(LOW));
		}
		
		if(fh>0){
			s = s +getResource(R.string.bright_ok,Integer.toString(HIGH));
		}else{
			s = s + getResource(R.string.bright_fail,Integer.toString(HIGH));
		}

		tvbody.setText(s);

		if((fl > 0) && (fh > 0)){
			tl.setEnabledButtons(true, tl.brsk);;
		}
	}

	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub
		//Log.i(TAG,"sensor"+sensor.getName()+" accuracy changed :"+accuracy);
	}

	@Override
	protected void onTimeInFinished() {
		if(tl!=null)
			tl.setEnabledButtons(true, tl.blsk);;
	}

}

