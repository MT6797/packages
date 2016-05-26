package com.android.incallui;

import com.mediatek.incallui.InCallUtils;
import com.mediatek.incallui.wrapper.FeatureOptionWrapper;


import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.PowerManager;
import android.os.SystemClock;
import android.telephony.TelephonyManager;
import android.view.View;


public class HallBroadcastReceiver extends BroadcastReceiver {
	public static HallBroadcastReceiver sHallBroadcastReceiver;
	private HallCallFragment mHallCallFragment;

    // Only one this receiver can be registered/unregisted
    public synchronized static HallBroadcastReceiver getInstance(Context context) {
        if (sHallBroadcastReceiver == null) {
        	sHallBroadcastReceiver = new HallBroadcastReceiver();
        }

        return sHallBroadcastReceiver;
    }

	 @Override
	    public void onReceive(Context context, Intent intent) {
	        String action = intent.getAction();
	        Log.d(this, "action: " + action);
			TelephonyManager tm = (TelephonyManager) context
					.getSystemService(Service.TELEPHONY_SERVICE);
			int callStatus = tm.getCallState();
			if (callStatus == TelephonyManager.CALL_STATE_IDLE) {
				return;
			}
	       /* if (action.equals("android.intent.action.ORBIT_FLEX")) {
	           int state = intent.getExtras().getInt("state");
	           Log.d(this, "====>hall state: "+state);
	           if(state == 0)
	            {
	        	   InCallPresenter.getInstance().setHallFragmentVisible(true);
	        	   TelecomAdapter.getInstance().turnOffProximitySensor(true);
	        	   InCallPresenter.getInstance().startUi(InCallPresenter.getInstance().getInCallState());
	        	  // InCallPresenter.getInstance().setWindowFullScreen(true);
	           }else
	            {
	        	   InCallPresenter.getInstance().setHallFragmentVisible(false);
	        	   TelecomAdapter.getInstance().turnOnProximitySensor();
	        	   InCallPresenter.getInstance().onUiShowing(false);
	        	  // InCallPresenter.getInstance().setWindowFullScreen(false);
	            }
	        }else */
			if(action.equals(TelephonyManager.ACTION_PHONE_STATE_CHANGED)||action.equals("android.intent.action.ORBIT_FLEX"))
	        {
		        final ProximitySensor sensor = InCallPresenter.getInstance().getProximitySensor();
		      
	        	if("0".equals(ProximitySensor.readHallState()))
	        	{
		        	   InCallPresenter.getInstance().setHallFragmentVisible(true);
		        	   if (sensor != null) 
		        		   sensor.turnOffProximitySensor(true);

	        	}else
	        	{
		        	   InCallPresenter.getInstance().setHallFragmentVisible(false);
		        	   if (sensor != null) 
		        		   sensor.turnOnProximitySensor();
	        	}
	        }
	 }
	 
	    public void register(Context context) {
	        IntentFilter hallFilter = new IntentFilter("android.intent.action.ORBIT_FLEX");	  
	        hallFilter.addAction(TelephonyManager.ACTION_PHONE_STATE_CHANGED);
	        if(context != null)
	        context.registerReceiver(this, hallFilter);
	    }

	    public void unregister(Context context) {
	    	if(context != null)
	    		context.unregisterReceiver(this);
	    }
	    
	    private void wakeupSystem(Context context) {
	        PowerManager mPowerManager = (PowerManager)context.getSystemService(Context.POWER_SERVICE);
	        mPowerManager.wakeUp(SystemClock.uptimeMillis());
	    }
}
