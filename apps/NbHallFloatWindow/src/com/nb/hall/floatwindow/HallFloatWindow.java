package com.nb.hall.floatwindow;

import java.util.ArrayList;

import android.app.KeyguardManager;
import android.app.Service;
import android.app.WallpaperManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

public class HallFloatWindow {
	private static WindowManager sWindowManager;
	private WindowManager.LayoutParams mLayoutParams = null;
	Context mContext;
	private static View sMainView;
	private ViewPager mViewPager;
	private ArrayList<View> mViews;
	private View mCallView;
	private static HallFloatWindow sInstance;
	private final String TAG = "HallFloatWindow";
	private SharedPreferences mSharePre;
	private final String SHARE_PRE = "com.nb.hall";
	private final int AWAKE_INTERVAL_DEFAULT_MS = 10000;
	private static boolean sAddFlag = false;
	private final static Object mLock = new Object();
	private ImageButton mEndCall;
	private NbSlidingTab mNbSlidingTab;
	private TextView mNameText, mNumberText, mCallElapseTImeText;
	private View mEndCallContainer;
	private boolean mCallHookFlag = false;

	public HallFloatWindow(final Context context) {
		this.mContext = context;
		sWindowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
		mLayoutParams = new WindowManager.LayoutParams();
		mLayoutParams.type = WindowManager.LayoutParams.TYPE_NAVIGATION_BAR_PANEL;
		mLayoutParams.gravity = Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL;

		mLayoutParams.screenOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
		Display display = sWindowManager.getDefaultDisplay();
		Point point = new Point();
		display.getRealSize(point);
		mLayoutParams.width = Math.min(point.x, point.y);
		mLayoutParams.height = Math.max(point.x, point.y);
		mLayoutParams.flags =  WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED | WindowManager.LayoutParams.FLAG_FULLSCREEN|WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;

		mLayoutParams.userActivityTimeout = AWAKE_INTERVAL_DEFAULT_MS;

		synchronized (mLock) {			
			if (sAddFlag)
				return;

			if ("0".equals(util.readHallState())) {
				initView(context);
				sWindowManager.addView(sMainView, mLayoutParams);
				Log.d(TAG, "sInstance is created");
				sAddFlag = true;
			}
		}

		mSharePre = context.getSharedPreferences(SHARE_PRE, 0);
		int item = mSharePre.getInt("item", 0);
		if(mViewPager!=null)
			mViewPager.setCurrentItem(item);
	}

	public class MyOnPageChangeListener implements OnPageChangeListener {
		@Override
		public void onPageSelected(int arg0) {
			SharedPreferences.Editor localEditor = mSharePre.edit();
			localEditor.putInt("item", arg0);
			localEditor.apply();
		}

		@Override
		public void onPageScrolled(int arg0, float arg1, int arg2) {
		}

		@Override
		public void onPageScrollStateChanged(int arg0) {
		}
	}

	private void initView(Context context) {
		sMainView = LayoutInflater.from(context).inflate(R.layout.hall_view, null);
		mViews = new ArrayList<View>();
		View view = LayoutInflater.from(context).inflate(R.layout.hall_analog_clock, null);
		mViews.add(view);
		mViewPager = (ViewPager) sMainView.findViewById(R.id.viewpager);
		view = LayoutInflater.from(context).inflate(R.layout.hall_clock_view, null);
		mViews.add(view);
		mViewPager.setOnPageChangeListener(new MyOnPageChangeListener());
		PagerAdapter mPagerAdapter = new PagerAdapter() {

			@Override
			public boolean isViewFromObject(View arg0, Object arg1) {
				return arg0 == arg1;
			}

			@Override
			public int getCount() {
				return mViews.size();
			}

			@Override
			public void destroyItem(View container, int position, Object object) {
				((ViewPager) container).removeView(mViews.get(position));
			}

			@Override
			public Object instantiateItem(View container, int position) {
				((ViewPager) container).addView(mViews.get(position));
				return mViews.get(position);
			}
		};
		mViewPager.setAdapter(mPagerAdapter);

		mCallView = (View) sMainView.findViewById(R.id.hall_call_screen);
		mEndCallContainer = sMainView.findViewById(R.id.floating_end_call_action_button_container);
		mEndCall = (ImageButton) sMainView.findViewById(R.id.end_call_action_button);
		mEndCall.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				Intent intent = new Intent();
				intent.setClassName("com.android.dialer", "com.android.incallui.NotificationBroadcastReceiver");
				intent.setAction("com.android.incallui.ACTION_HANG_UP_ONGOING_CALL");
				mContext.sendBroadcast(intent);
			}
		});
		mNbSlidingTab = (NbSlidingTab) sMainView.findViewById(R.id.nbSlidlingView);
		mNameText = (TextView) sMainView.findViewById(R.id.name);
		mNumberText = (TextView) sMainView.findViewById(R.id.number);
		mCallElapseTImeText = (TextView) sMainView.findViewById(R.id.callStatus);
		updateViewState();
	}

	public void updateViewState() {
		Log.d(TAG,
				"sIsCalling: " + PhoneStatusService.sIsCalling + "   sIsOutgoingCall: "
						+ PhoneStatusService.sIsOutgoingCall + "  sIsActive: " + PhoneStatusService.sIsActive
						+ " isComing: " + PhoneStatusService.sIsIncomingCall);
		synchronized (mLock) {
			if (isCalling()) {
				if (mViewPager == null || mCallView == null)
					return;
				mViewPager.setVisibility(View.GONE);
				mCallView.setVisibility(View.VISIBLE);
				if (PhoneStatusService.sIsIncomingCall) {
					mEndCallContainer.setVisibility(View.GONE);
					mNbSlidingTab.setVisibility(View.VISIBLE);
				} else if (PhoneStatusService.sIsOutgoingCall || PhoneStatusService.sIsActive||mCallHookFlag) {
					mEndCallContainer.setVisibility(View.VISIBLE);
					mNbSlidingTab.setVisibility(View.GONE);
				}
				mNameText.setText(PhoneStatusService.sName);
				mNumberText.setText(PhoneStatusService.sPhoneNumber);
				if (PhoneStatusService.sIsActive) {
					if (PhoneStatusService.sCallElapseTime != 0) {
						if (PhoneStatusService.sCallElapseTime >= 3600)
							mCallElapseTImeText
									.setText(String.format("%02d:%02d:%02d", PhoneStatusService.sCallElapseTime / 3600,
											(PhoneStatusService.sCallElapseTime / 60) % 60,
											PhoneStatusService.sCallElapseTime % 60));
						else
							mCallElapseTImeText
									.setText(String.format("%02d:%02d", (PhoneStatusService.sCallElapseTime / 60) % 60,
											PhoneStatusService.sCallElapseTime % 60));
					}else
						mCallElapseTImeText.setText("");
				}
			} else {
				mViewPager.setVisibility(View.VISIBLE);
				mCallView.setVisibility(View.GONE);
				mCallElapseTImeText.setText("");
			}
		}
	}

	public static HallFloatWindow getInstance() {
		return sInstance;
	}

	public static void createFloatWindow(Context context) {
		Log.d("HallFloatWindow", "createFloatWindow ******");
		sInstance = new HallFloatWindow(context);
	}

	public static void removeView() {
		// Log.d("lqh", "removeView----");
		synchronized (mLock) {
			if (sMainView != null && sWindowManager != null && sAddFlag) {
				sWindowManager.removeView(sMainView);
				Log.d("HallFloatWindow", "removeView--222--");
			}
			sWindowManager = null;
			sInstance = null;
			sMainView = null;
			sAddFlag = false;
		}
	}
    private boolean isCalling()  //当刚开机时，PhoneStatusService服务还为启动时，即打电话，所以通过此函数加强判断
    {
    	TelephonyManager tManager = (TelephonyManager) mContext.getSystemService(Service.TELEPHONY_SERVICE);
    	if(TelephonyManager.CALL_STATE_IDLE == tManager.getCallState())
    	{
    		mCallHookFlag = false;
    		return false;
    	}
    	else if(TelephonyManager.CALL_STATE_OFFHOOK == tManager.getCallState())
    	{
    		mCallHookFlag =true;
    	}
    	
    	if(!PhoneStatusService.sIsCalling) //防止服务不存在，重新启动
    	{
    		Intent service = new Intent(mContext, PhoneStatusService.class);
    		mContext.startService(service);
    		Log.d(TAG, "restart PhoneStatusService");
    	}
    	return true;
    }
}
