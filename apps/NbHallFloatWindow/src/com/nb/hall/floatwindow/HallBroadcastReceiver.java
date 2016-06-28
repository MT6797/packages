package com.nb.hall.floatwindow;

import android.app.ActivityManager;
import android.app.KeyguardManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.PowerManager;
import android.os.SystemClock;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.animation.Animation;
import android.os.SystemProperties;
import android.app.StatusBarManager;



public class HallBroadcastReceiver extends BroadcastReceiver {

	 @Override
	    public void onReceive(Context context, Intent intent) {
		    TelephonyManager tm = (TelephonyManager)context.getSystemService(Service.TELEPHONY_SERVICE); 
		    int callStatus = tm.getCallState();
		  //  Log.d("lqh", "FloatWindow  call status:"+callStatus);
		    if(callStatus != TelephonyManager.CALL_STATE_IDLE||"1".equals(SystemProperties.get("sys.config.mmitest", "0")))
		    {
			HallFloatWindow.removeView();	
		    	return;
		    }
		    
	        String action = intent.getAction();
	        Log.d("lqh", "FloatWindow action:"+action);
	        if (action.equals("android.intent.action.ORBIT_FLEX")) {
	           int state = intent.getExtras().getInt("state");
	          // Log.d("lqh", "FloatWindow hall state:"+state);
	           if("1".equals(util.readHallState()))
	            {
				HallFloatWindow.removeView();	
	            }else
	            {
			   HallFloatWindow.createFloatWindow(context);
	            }
	        }else if(action.equals(Intent.ACTION_BOOT_COMPLETED)||action.equals(TelephonyManager.ACTION_PHONE_STATE_CHANGED))
	        {
	        	if("1".equals(util.readHallState()))
	        	{
				HallFloatWindow.removeView();	
	        	}
	        	else
	        	{
				HallFloatWindow.createFloatWindow(context);
	        	}
	        }
	 }	 
}
