package com.nb.mmitest;

import com.nb.mmitest.Test.ID;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

public class GyroscopeSensorTest extends Test implements SensorEventListener  {
	private final String TAG="GyroscopeSensorTest";
	private TestLayout1 tl;
	private SensorManager sensorManager;
	private Sensor magneticSensor;
	private Sensor accelerometerSensor;
	private Sensor gyroscopeSensor;
	// 将纳秒转化为秒
	private static final float NS2S = 1.0f / 1000000000.0f;
	private float timestamp;
	private float angle[] = new float[3];
	
	GyroscopeSensorTest(ID pid, String s) {
		super(pid, s);
	}
	
	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		// TODO Auto-generated method stub
		if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
			//从 x、y、z 轴的正向位置观看处于原始方位的设备，如果设备逆时针旋转，将会收到正值；否则，为负值
			 if(timestamp != 0){ 
			// 得到两次检测到手机旋转的时间差（纳秒），并将其转化为秒
			 final float dT = (event.timestamp - timestamp) * NS2S;
			// 将手机在各个轴上的旋转角度相加，即可得到当前位置相对于初始位置的旋转弧度
			 angle[0] += event.values[0] * dT;
			 angle[1] += event.values[1] * dT;
			 angle[2] += event.values[2] * dT;
			// 将弧度转化为角度
			 float anglex = (float) Math.toDegrees(angle[0]);
			 float angley = (float) Math.toDegrees(angle[1]);
			 float anglez = (float) Math.toDegrees(angle[2]);

			 Log.d(TAG, "anglex------------>" + anglex);
			 Log.d(TAG, "angley------------>" + angley);
			 Log.d(TAG, "anglez------------>" + anglez);
			
			 Log.d(TAG, "gyroscopeSensor.getMinDelay()----------->" +
			 gyroscopeSensor.getMinDelay());
			// tl = new TestLayout1(mContext, mName, "\n\n\n"+"x:"+event.values[0]+"\ny:"+event.values[1]+"\nz:"+event.values[2], getResource(R.string.fail), getResource(R.string.ok));
			// mContext.setContentView(tl.ll);
			 tl.getBody().setText("\n\n\n"+"x:"+event.values[0]+"\ny:"+event.values[1]+"\nz:"+event.values[2]);
			 }
			 //将当前时间赋值给timestamp
			 timestamp = event.timestamp;

		}
	}

	@Override
	protected void Run() {
		switch (mState) {
		case INIT: 
			tl = new TestLayout1(mContext, mName, "\nOpening.....", getResource(R.string.fail), getResource(R.string.ok));
			mContext.setContentView(tl.ll);
			sensorManager = (SensorManager) mContext.getSystemService(Context.SENSOR_SERVICE);
			gyroscopeSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
			sensorManager.registerListener(this, gyroscopeSensor,
					SensorManager.SENSOR_DELAY_NORMAL);

			break;
		case INIT + 1:
		
			mState++;
			break;
		case END:
			sensorManager.unregisterListener(this,gyroscopeSensor);

		default:
			break;
		}

	}
}
