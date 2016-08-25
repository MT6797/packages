/**
 * 
 */
package com.nb.hall.floatwindow;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

public class PhoneStatusService extends Service {
	private static final String TAG = "PhoneStatusService";	
	public static String sName,sPhoneNumber;
	public static boolean sIsCalling,sIsOutgoingCall;
	public static boolean sIsIncomingCall, sIsActive;
	public static long sCallElapseTime = 0;
	public int mCallState ;

	
	public  final int INVALID = 0;
    public  final int NEW = 1;            /* The call is new. */
    public  final int IDLE = 2;           /* The call is idle.  Nothing active */
    public  final int ACTIVE = 3;         /* There is an active call */
    public  final int INCOMING = 4;       /* A normal incoming phone call */
    public  final int CALL_WAITING = 5;   /* Incoming call while another is active */
    public  final int DIALING = 6;        /* An outgoing call during dial phase */
    public  final int REDIALING = 7;      /* Subsequent dialing attempt after a failure */
    public  final int ONHOLD = 8;         /* An active phone call placed on hold */
    public  final int DISCONNECTING = 9;  /* A call is being ended. */
    public  final int DISCONNECTED = 10;  /* State after a call disconnects */
    public  final int CONFERENCED = 11;   /* Call part of a conference call */
    public  final int SELECT_PHONE_ACCOUNT = 12; /* Waiting for account selection */
    public  final int CONNECTING = 13;    /* Waiting for Telecomm broadcast to finish */
	/* (non-Javadoc)
	 * @see android.app.Service#onBind(android.content.Intent)
	 */
	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public void onStart(Intent intent, int startId) {
		// TODO Auto-generated method stub
		super.onStart(intent, startId);
		Log.d(TAG, "start service PhoneStatusService");
		registerReceiver();		
	}

	@Override
	public void onDestroy() {
		// TODO Auto-generated method stub
		Log.d(TAG, "onDestroy");
		unregisterReceiver();
		super.onDestroy();
	}

	private void registerReceiver() {
		IntentFilter intent = new IntentFilter();
		intent.addAction("android.intent.action.NEW_OUTGOING_CALL");
		intent.addAction("android.intent.action.PHONE_STATE");
		intent.addAction("nb.intent.action.PHONE_ACTIVE");
		this.registerReceiver(mPhoneBroadcastReceiver, intent);
	}

	private void unregisterReceiver() {
		unregisterReceiver(mPhoneBroadcastReceiver);
	}

	private BroadcastReceiver mPhoneBroadcastReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {				
			Log.d(TAG, "receive: "+intent.getAction());
			if (intent.getAction().equals(Intent.ACTION_NEW_OUTGOING_CALL)) {
				sPhoneNumber = intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER);
				sName = util.getContactNameFromPhoneBook(context, sPhoneNumber);
				sIsOutgoingCall = true;
				sCallElapseTime =0;
				//sIsCalling = true;
				Log.d(TAG, "=outgoing phone==>call name:"+sName+"   phonenumber:"+sPhoneNumber);
			} else if(intent.getAction().equals("nb.intent.action.PHONE_ACTIVE")){
				//mHander.sendEmptyMessageDelayed(UPDATE_TIME_MSG, 1000);			
				if(!sIsIncomingCall) 
				{
					if(intent.getStringExtra("number")!=null)
					{
						sPhoneNumber = intent.getStringExtra("number");
					}	
					mCallState = intent.getIntExtra("callState",0);
	
					long activeTime = intent.getLongExtra("activeTime",0)/1000;
					if(mCallState == ACTIVE && activeTime!=0)  //防止多通电话同时在线，其中一通挂断
						sCallElapseTime = activeTime;
				}
				if(intent.getStringExtra("name")!=null)//防止名字和号码一致
				{
					String tempName = intent.getStringExtra("name");
					String tempPhoneNumber="";
					if(tempName !=null)
						tempName = tempName.replaceAll(" ","");
					if(sPhoneNumber!=null)
					 	tempPhoneNumber = sPhoneNumber.replaceAll(" ","");
					if(!tempName.equals(tempPhoneNumber))
						sName = intent.getStringExtra("name");
					
				}
				Log.d(TAG, "=activite==>call name:"+sName+"   phonenumber:"+sPhoneNumber+"  mCallState:"+mCallState);
			}else{
				TelephonyManager tManager = (TelephonyManager) getSystemService(Service.TELEPHONY_SERVICE);
				switch (tManager.getCallState()) {
				case TelephonyManager.CALL_STATE_RINGING:
					sIsCalling = true;
					sIsIncomingCall = true;
					sPhoneNumber = intent.getStringExtra("incoming_number");
					sName = util.getContactNameFromPhoneBook(context, sPhoneNumber);
					sCallElapseTime = 0;
					Log.d(TAG, "=ring==>call name:"+sName+"   phonenumber:"+sPhoneNumber);
					break;
				case TelephonyManager.CALL_STATE_OFFHOOK:
			//		if(!sIsActive)						 
					sIsActive = true;
					sIsIncomingCall = false;
					sIsOutgoingCall = false;
					sIsCalling = true;
					break;
				case TelephonyManager.CALL_STATE_IDLE:
					sIsIncomingCall = false;
					sIsCalling = false;
					sIsActive = false;
					sIsOutgoingCall = false;
					sCallElapseTime = 0;
					//mHander.removeMessages(UPDATE_TIME_MSG);
					break;
				}
				Log.d(TAG, "===>call state: "+tManager.getCallState());
			}
			Log.d(TAG, "===>sIsCalling: "+PhoneStatusService.sIsCalling+"   sIsOutgoingCall: "+PhoneStatusService.sIsOutgoingCall+ "  sIsActive: "+PhoneStatusService.sIsActive+" isComing:"+PhoneStatusService.sIsIncomingCall);
			HallFloatWindow instance = HallFloatWindow.getInstance();
			if (instance != null)
				instance.updateViewState();
		}				
	};
	private final int UPDATE_TIME_MSG = 1;
	private Handler mHander = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			// TODO Auto-generated method stub
			super.handleMessage(msg);
			if(msg.what == UPDATE_TIME_MSG)
			{
				sCallElapseTime++;
				mHander.removeMessages(UPDATE_TIME_MSG);
				mHander.sendEmptyMessageDelayed(UPDATE_TIME_MSG, 1000);
				HallFloatWindow instance = HallFloatWindow.getInstance();
				if (instance != null)
					instance.updateViewState();
			}
		}
	};
}
