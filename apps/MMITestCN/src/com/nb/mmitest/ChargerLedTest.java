package com.nb.mmitest;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

class ChargerLedTest extends Test
{
	TestLayout1 tl;
	private static final String CHARGERLEDPATHRED = "/sys/class/leds/red/brightness";
	private static final String CHARGERLEDPATHGREEN = "/sys/class/leds/green/brightness";
	private static final String CHARGERLEDPATHBLUE = "/sys/class/leds/blue/brightness";
	BufferedReader ChargerLedBuffer;	

	File ChargerLedRedPath = new File(CHARGERLEDPATHRED);
	File ChargerLedGreenPath = new File(CHARGERLEDPATHGREEN);
	File ChargerLedBluePath = new File(CHARGERLEDPATHBLUE);
	private final String TAG ="ChargerLedTest";
	
	private int count =0;
	
		
	Handler mHandler = new Handler(){

		@Override
		public void handleMessage(Message msg) {
			// TODO Auto-generated method stub
			super.handleMessage(msg);
			switch(count%3)
			{
			   case 0:
				   changeLedColor(255,0,0);
				   break;
			   case 1:
				   changeLedColor(0,255,0);
				   break;
			   case 2:
				   changeLedColor(0,0,255);
				   break;
			}
			mHandler.sendEmptyMessageDelayed(1, 500);
			count++;
		}
		
	};
	
	
	ChargerLedTest(ID pid, String s) {
		super(pid, s);
		// TODO Auto-generated constructor stub
	}

	protected void Run() {
		// this function executes the test
		switch (mState) {
		case INIT: 
			count =0;
			mHandler.removeMessages(1);
			mHandler.sendEmptyMessageDelayed(1, 10);
			tl = new TestLayout1(mContext, mName, getResource(R.string.led_rgb_test));
			mContext.setContentView(tl.ll);
			break;			
			
		case END:
			mHandler.removeMessages(1);
			changeLedColor(0,0,0);
			break;
			
		default:
			break;
		}
		
	}
	void changeLedColor(long red,long green,long blue)
	{
		
		try{
				FileOutputStream outStream = new FileOutputStream(ChargerLedRedPath);				
				outStream.write(((Long)red).toString().getBytes());
				Log.d(TAG,"TCThychu  write the value "+red+" successfully");
				outStream.close();
				
				outStream = new FileOutputStream(ChargerLedGreenPath);				
				outStream.write(((Long)green).toString().getBytes());
				Log.d(TAG,"TCThychu  write the value "+green+" successfully");
				outStream.close();
				
				outStream = new FileOutputStream(ChargerLedBluePath);				
				outStream.write(((Long)blue).toString().getBytes());
				Log.d(TAG,"TCThychu  write the value "+blue+" successfully");
				outStream.close();

			
		} catch (FileNotFoundException e) {   
             e.printStackTrace();   
         } catch (IOException e) {   
             e.printStackTrace();   
         }	catch (Exception e){
			Log.e("MMI Test", "can't open ChargerLED ");
			 tl = new TestLayout1(mContext,mName,"Charger LED open failed");
			 mContext.setContentView(tl.ll);
			return;
		}
	}
	
}