package com.nb.hall.floatwindow;

import java.util.ArrayList;
import android.app.KeyguardManager;
import android.app.WallpaperManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;

public class HallFloatWindow {
	private static WindowManager sWindowManager;
	private WindowManager.LayoutParams mLayoutParams = null;
	Context mContext;
	private static View sMainView;
	private ViewPager mViewPager;
	private ArrayList<View> mViews;
	private static HallFloatWindow sInstance;
	private final String TAG = "HallFloatWindow";
	private SharedPreferences mSharePre;
	private final String SHARE_PRE = "com.nb.hall";
	private final int AWAKE_INTERVAL_DEFAULT_MS = 10000;
	private static boolean sAddFlag = false;
	public HallFloatWindow(final Context context) {
		this.mContext = context;
		sWindowManager = (WindowManager) context
				.getSystemService(Context.WINDOW_SERVICE);
		mLayoutParams = new WindowManager.LayoutParams();
		mLayoutParams.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
		mLayoutParams.gravity = Gravity.CENTER_HORIZONTAL
				| Gravity.CENTER_VERTICAL;

		mLayoutParams.x = 0;
		mLayoutParams.y = 0;
		mLayoutParams.screenOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
		Display display = sWindowManager.getDefaultDisplay();
		Point point = new Point();
		display.getRealSize(point);
		mLayoutParams.width = Math.min(point.x, point.y);
		mLayoutParams.height = Math.max(point.x, point.y);
		mLayoutParams.flags = WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
				| WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED;
		KeyguardManager mKeyguardManager = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
		if( mKeyguardManager.inKeyguardRestrictedInputMode())
			mLayoutParams.userActivityTimeout = AWAKE_INTERVAL_DEFAULT_MS;
		sMainView = LayoutInflater.from(context).inflate(R.layout.hall_float,
				null);
		mViews = new ArrayList<View>();
		View view = LayoutInflater.from(context).inflate(R.layout.hall_analog_clock,
				null);
		mViews.add(view);
		mViewPager = (ViewPager) sMainView.findViewById(R.id.viewpager);
		view = LayoutInflater.from(context).inflate(
				R.layout.hall_clock_view, null);
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
		if("0".equals(util.readHallState()))
		{
			sWindowManager.addView(sMainView, mLayoutParams);
			sAddFlag = true;
		}

		mSharePre = context.getSharedPreferences(SHARE_PRE, 0);
		int item = mSharePre.getInt("item", 0);
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
	public static HallFloatWindow createFloatWindow(Context context) {

		sInstance = new HallFloatWindow(context);	
	//	sInstance.notifyAll();
		Log.d("lqh", "sInstance is created");
		return sInstance;
	}
	
	public static void removeView()
	{
		Log.d("lqh", "removeView----");
		if(sMainView != null && sWindowManager !=null&&sAddFlag)
		{   
			sWindowManager.removeView(sMainView);
			Log.d("lqh", "removeView--222--");
		}
		sWindowManager = null;
		sMainView = null;
		sAddFlag = false;
	}
}
