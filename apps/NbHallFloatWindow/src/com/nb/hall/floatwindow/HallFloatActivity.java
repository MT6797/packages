package com.nb.hall.floatwindow;

import java.util.ArrayList;

import android.os.Bundle;
import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.util.Log;
import android.view.GestureDetector.OnGestureListener;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.widget.ViewFlipper;

public class HallFloatActivity extends Activity {

	private static HallFloatActivity instance;
	private final int AWAKE_INTERVAL_DEFAULT_MS = 10000;
	private static View sMainView;
	private ViewPager mViewPager;
	private ArrayList<View> mViews;
	private SharedPreferences mSharePre;

	private final String SHARE_PRE = "com.nb.hall";
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.hall_float);
		instance = this;
		KeyguardManager mKeyguardManager = (KeyguardManager) this.getSystemService(Context.KEYGUARD_SERVICE);
		getWindow().getAttributes().type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
		//getWindow().getAttributes().alpha=0.0f;
		//getWindow().getDecorView().setBackgroundResource(android.R.color.transparent);
		boolean flag = mKeyguardManager.inKeyguardRestrictedInputMode();
		if(flag)
			getWindow().getAttributes().userActivityTimeout = AWAKE_INTERVAL_DEFAULT_MS;
		getWindow().addFlags(
				WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED 
						| WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
		//HallFloatWindow.createFloatWindow(this);
		sMainView = LayoutInflater.from(this).inflate(R.layout.hall_float,
				null);
		mViews = new ArrayList<View>();
		View view = LayoutInflater.from(this).inflate(R.layout.hall_analog_clock,
				null);
		mViews.add(view);
		mViewPager = (ViewPager) findViewById(R.id.viewpager);
		view = LayoutInflater.from(this).inflate(
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

		mSharePre = this.getSharedPreferences(SHARE_PRE, 0);
		int item = mSharePre.getInt("item", 0);
		mViewPager.setCurrentItem(item);		
		Log.d("lqh", "oncreate ---");
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

	@Override
	protected void onResume() {
		super.onResume();
	}
	@Override
	protected void onStop() {
		super.onStop();
	}
	@Override
	protected void onDestroy() {
		super.onDestroy();
		//HallFloatWindow.removeView();
		Log.d("lqh", "onDestroy ---");
	}
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		return true;
	}

	public static HallFloatActivity getInstance() {
		return instance;
	}
}
