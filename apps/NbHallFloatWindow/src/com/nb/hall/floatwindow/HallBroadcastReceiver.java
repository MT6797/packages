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
		    Log.d("lqh", "FloatWindow  call status:"+callStatus);
		    if(callStatus != TelephonyManager.CALL_STATE_IDLE||"1".equals(SystemProperties.get("sys.config.mmitest", "0")))
		    {
		    	finishActivity(context);
		    	return;
		    }
		    
	        String action = intent.getAction();
	        Log.d("lqh", "FloatWindow action:"+action);
	        if (action.equals("android.intent.action.ORBIT_FLEX")) {
	           int state = intent.getExtras().getInt("state");
	           Log.d("lqh", "FloatWindow hall state:"+state);
	           if("1".equals(util.readHallState()))
	            {
	        	   finishActivity(context);
	           }else
	            {
	        	   startActivity(context);
	            }
	        }else if(action.equals(Intent.ACTION_BOOT_COMPLETED)||action.equals(TelephonyManager.ACTION_PHONE_STATE_CHANGED))
	        {
	        	if("1".equals(util.readHallState()))
	        	{
	        		finishActivity(context);
	        	}
	        	else
	        	{
	        		startActivity(context);
	        	}
	        }
	 }
	 
	 private void finishActivity(Context context)
	 {
		 if(HallFloatActivity.getInstance()!=null)
		 {
			 Log.d("lqh","finishActivity 1.");
			KeyguardManager mKeyguardManager = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
			if( mKeyguardManager.inKeyguardRestrictedInputMode())
			{			 
		  		 HallFloatActivity.getInstance().finish();
		  		 HallFloatActivity.getInstance().overridePendingTransition(0, 0);
		  		 HallFloatWindow.removeView();
			}else
			{
			     HallFloatWindow.removeView();
  		        HallFloatActivity.getInstance().finish();
  		        HallFloatActivity.getInstance().overridePendingTransition(0, 0);
			}
  		 Log.d("lqh","finishActivity 2.");
		 }
		 
	 }
	 private void startActivity(Context context)
	 {
		 if(isActivityStarted(context))
			 return;
		StatusBarManager statusBarManager = (StatusBarManager) context.getSystemService(
                android.app.Service.STATUS_BAR_SERVICE);
		if(statusBarManager!=null)
			statusBarManager.collapsePanels();
  	   Intent itent = new Intent();
  	   itent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
  	   itent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
  	   itent.setClass(context, HallFloatActivity.class);
  	   context.startActivity(itent);  	   
	 }
	 
	 private  boolean isActivityStarted(Context context)
	 {
         ActivityManager am = (ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE);
         if(am.getRunningTasks(1).size() > 0)
           {
         ComponentName cn = am.getRunningTasks(1).get(0).topActivity;
         if (cn != null &&
                 cn.getClassName() != null &&(
                 cn.getClassName().equals("com.nb.hall.floatwindow.HallFloatActivity"))){
        	 	Log.d("lqh","HallFloatActivity has started.");
             return true;
         	}
          }else
        	  Log.d("lqh","getRunningTasks size is 0");
         return false;
	   }
}
