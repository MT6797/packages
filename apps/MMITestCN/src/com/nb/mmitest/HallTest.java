package com.nb.mmitest;



import android.app.ActivityManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

public class HallTest extends Test {

	TestLayout1 tl;
	String mDisplayString;
	private String TAG = "HallTest";
	private HallBroadcastReceiver mHallBroadReceiver;

	HallTest(ID pid, String s) {
		super(pid, s);
		TAG = Test.TAG + TAG;

	}

	HallTest(ID pid, String s, int timein) {
		super(pid, s, timein, 0);
		TAG = Test.TAG + TAG;
	}

	@Override
	protected void Run() {
		// this function executes the test
		switch (mState) {
		case INIT:
			tl = new TestLayout1(mContext, mName,
					getResource(R.string.hall_display));
			mContext.setContentView(tl.ll);
			mHallBroadReceiver = new HallBroadcastReceiver();
		    IntentFilter hallFilter = new IntentFilter("android.intent.action.ORBIT_FLEX");	  
	        if(mContext != null)
	        	mContext.registerReceiver(mHallBroadReceiver, hallFilter);
	        tl.getBody()
			.setText(getResource(R.string.hall_display) + "1");
	        tl.setEnabledButtons(false, tl.brsk);
			break;

		case END://

			break;

		default:
			break;
		}
	}

	@Override
	protected void onTimeInFinished() {
    	if(mContext != null)
    		mContext.unregisterReceiver(mHallBroadReceiver);
	}

	public class HallBroadcastReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {

			String action = intent.getAction();

			if (action.equals("android.intent.action.ORBIT_FLEX")) {
				int state = intent.getExtras().getInt("state");
				tl.getBody()
						.setText(getResource(R.string.hall_display) + state);
				if(state == 0)
				 tl.setEnabledButtons(true, tl.brsk);
			}
		}
	}

}
