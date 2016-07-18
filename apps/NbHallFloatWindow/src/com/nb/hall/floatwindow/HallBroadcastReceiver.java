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

	private final String TAG = "HallBroadcastReceiver";
	@Override
	public void onReceive(Context context, Intent intent) {
		if ("1".equals(SystemProperties.get("sys.config.mmitest", "0"))) {
			HallFloatWindow.removeView();
			return;
		}

		String action = intent.getAction();
		Log.d(TAG, "FloatWindow action:" + action);
		if (action.equals("android.intent.action.ORBIT_FLEX")) {
			int state = intent.getExtras().getInt("state");
			Log.d(TAG, "FloatWindow hall state:" + state);
			if ("1".equals(util.readHallState())) {
				Log.d(TAG, "11111");
				HallFloatWindow.removeView();
			} else {
				HallFloatWindow instance = HallFloatWindow.getInstance();
				if(instance == null)
					HallFloatWindow.createFloatWindow(context);
				Log.d(TAG, "2222");
			}
		} else if (action.equals(Intent.ACTION_BOOT_COMPLETED)) {
			Intent service = new Intent(context, PhoneStatusService.class);
			context.startService(service);
			if ("1".equals(util.readHallState())) {
				HallFloatWindow.removeView();
			} else {
				HallFloatWindow instance = HallFloatWindow.getInstance();
				if(instance == null)
					HallFloatWindow.createFloatWindow(context);
			}
		} 
	}
}
