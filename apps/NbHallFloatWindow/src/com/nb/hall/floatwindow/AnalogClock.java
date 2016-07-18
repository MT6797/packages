/*
* Copyright (C) 2014 MediaTek Inc.
* Modification based on code covered by the mentioned copyright
* and/or permission notice(s).
*/
/*
 * Copyright (C) 2006 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.nb.hall.floatwindow;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.text.format.DateUtils;
import android.text.format.Time;
import android.util.AttributeSet;
import android.view.View;
import android.widget.RemoteViews.RemoteView;
import android.util.Log;
import java.util.TimeZone;

/**
 * This widget display an analogic clock with two hands for hours and minutes.
 */
public class AnalogClock extends View {
	private Time mCalendar;

	private final Drawable mHourHand;
	private final Drawable mMinuteHand;
	private final Drawable mSecondHand;
	private final Drawable mDial;
	private final Drawable mBorader;
	private final Drawable mTimeCircle;
	private final Drawable mMonthBg;
	private final Drawable mWeekBg;
	private final Drawable mHand;
	private final Drawable mHandCircle;
	private final Drawable mDayBorader;

	private final int mDialWidth;
	private final int mDialHeight;

	private boolean mAttached;

	private final Handler mHandler = new Handler();
	private float mSeconds;
	private float mMinutes;
	private float mHour;
	private boolean mChanged;
	private final Context mContext;
	private String mTimeZoneId;
	private boolean mNoSeconds = false;
	private float TIMECIRCLESCALE = (float) 0.4;
	private float WEEKSCALE = (float) 0.5;
	private int mWeek;
	private int mMonth;
	private int mDay;
	private final String TAG = "AnalogClock";
	// private final float mDotRadius;
	// private final float mDotOffset;
	private Paint mPaint;
	private boolean mFlag = true;

	public AnalogClock(Context context) {
		this(context, null);
	}

	public AnalogClock(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public AnalogClock(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		mContext = context;
		Resources r = mContext.getResources();

		mDial = r.getDrawable(R.drawable.clock_analog_dial_mipmap);
		mHourHand = r.getDrawable(R.drawable.clock_analog_hour_mipmap);
		mMinuteHand = r.getDrawable(R.drawable.clock_analog_minute_mipmap);
		mSecondHand = r.getDrawable(R.drawable.clock_analog_second_mipmap);
		mBorader = r.getDrawable(R.drawable.clock_analog_borader_mipmap);
		mTimeCircle = r.getDrawable(R.drawable.timecircle);
		mMonthBg = r.getDrawable(R.drawable.monthbg);
		mWeekBg = r.getDrawable(R.drawable.weekbg);
		mHand = r.getDrawable(R.drawable.handmonth);
		mHandCircle = r.getDrawable(R.drawable.monthcircle);
		mDayBorader = r.getDrawable(R.drawable.dayborder);

		// TypedArray a = context.obtainStyledAttributes(attrs,
		// R.styleable.AnalogClock);
		// mDotRadius = a.getDimension(R.styleable.AnalogClock_jewelRadius, 0);
		// mDotOffset = a.getDimension(R.styleable.AnalogClock_jewelOffset, 0);
		// final int dotColor = a.getColor(R.styleable.AnalogClock_jewelColor,
		// Color.WHITE);
		// if (dotColor != 0) {
		mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		mPaint.setColor(Color.BLACK);
		mPaint.setTextSize(34);

		mCalendar = new Time();

		mDialWidth = mDial.getIntrinsicWidth();
		mDialHeight = mDial.getIntrinsicHeight();
	}

	@Override
	protected void onAttachedToWindow() {
		super.onAttachedToWindow();

		if (!mAttached) {
			mAttached = true;
			IntentFilter filter = new IntentFilter();

			filter.addAction(Intent.ACTION_TIME_TICK);
			filter.addAction(Intent.ACTION_TIME_CHANGED);
			filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);

			getContext().registerReceiver(mIntentReceiver, filter, null, mHandler);
		}

		// NOTE: It's safe to do these after registering the receiver since the
		// receiver always runs
		// in the main thread, therefore the receiver can't run before this
		// method returns.

		// The time zone may have changed while the receiver wasn't registered,
		// so update the Time
		mCalendar = new Time();

		// Make sure we update to the current time
		onTimeChanged();

		// tick the seconds
		post(mClockTick);

	}

	@Override
	protected void onDetachedFromWindow() {
		super.onDetachedFromWindow();
		if (mAttached) {
			getContext().unregisterReceiver(mIntentReceiver);
			removeCallbacks(mClockTick);
			mAttached = false;
		}
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

		int widthMode = MeasureSpec.getMode(widthMeasureSpec);
		int widthSize = MeasureSpec.getSize(widthMeasureSpec);
		int heightMode = MeasureSpec.getMode(heightMeasureSpec);
		int heightSize = MeasureSpec.getSize(heightMeasureSpec);

		float hScale = 1.0f;
		float vScale = 1.0f;

		if (widthMode != MeasureSpec.UNSPECIFIED && widthSize < mDialWidth) {
			hScale = (float) widthSize / (float) mDialWidth;
		}

		if (heightMode != MeasureSpec.UNSPECIFIED && heightSize < mDialHeight) {
			vScale = (float) heightSize / (float) mDialHeight;
			/**
			 * M: The rest of the space is smaller than the view we are to draw,
			 * reduce the scale of ten percent(just an experience value) in
			 * order for other view to draw
			 */
			vScale -= 0.1;
		}

		float scale = Math.min(hScale, vScale);

		setMeasuredDimension(resolveSizeAndState((int) (mDialWidth * scale), widthMeasureSpec, 0),
				resolveSizeAndState((int) (mDialHeight * scale), heightMeasureSpec, 0));
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
		mChanged = true;
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);

		boolean changed = mChanged;
		if (changed) {
			mChanged = false;
		}

		int availableWidth = getWidth();
		int availableHeight = getHeight();
		Log.d(TAG, "1--------:width: " + availableWidth + "   hight: " + availableHeight);
		int x = availableWidth / 2;
		int y = availableHeight / 2;
		// Log.d(TAG, "2--------:x: "+x+" y: "+y);
		final Drawable dial = mDial;
		int w = dial.getIntrinsicWidth();
		int h = dial.getIntrinsicHeight();

		boolean scaled = false;

		if (availableWidth < w || availableHeight < h) {
			scaled = true;
			float scale = Math.min((float) availableWidth / (float) w, (float) availableHeight / (float) h);
			canvas.save();
			canvas.scale(scale, scale, x, y);
		}
		// canvas.scale((float)0.9, (float)0.9, x, y);
		if (changed) {
			dial.setBounds(x - (w / 2), y - (h / 2), x + (w / 2), y + (h / 2));
			mBorader.setBounds(x - (w / 2), y - (h / 2), x + (w / 2), y + (h / 2));

		}
		dial.draw(canvas);

		mBorader.draw(canvas);
		// draw month
		drawSubBg(canvas, mMonthBg, x, y - getHeight() / 4, changed, 0.5f);
		// Log.d(TAG, "3--------:hight: "+getHeight());
		drawHand(canvas, mHand, x, y - getHeight() / 4, mMonth / 12.0f * 360.0f, changed, 0.5f);
		drawPoint(canvas, mHandCircle, x, y - getHeight() / 4, changed);

		// draw week
		drawSubBg(canvas, mWeekBg, x, y + getHeight() / 4, changed, 0.5f);
		// Log.d(TAG, "3--------:hight: "+getHeight());
		drawHand(canvas, mHand, x, y + getHeight() / 4, mWeek / 7.0f * 360.0f, changed, 0.5f);
		drawPoint(canvas, mHandCircle, x, y + getHeight() / 4, changed);
		// if (mDotRadius > 0f && mDotPaint != null) {
		// canvas.drawCircle(x, y - (h / 2) + mDotOffset, mDotRadius,
		// mDotPaint);
		// }
		// draw month day
		drawSubBg(canvas, mDayBorader, 3 * x / 2, y, changed, 1.0f);
		canvas.drawText(Integer.toString(mDay), 3 * x / 2 - mDayBorader.getIntrinsicWidth() / 4,
				y + mDayBorader.getIntrinsicHeight() / 4, mPaint);

		drawHand(canvas, mHourHand, x, y, mHour / 12.0f * 360.0f, changed, 1.0f);
		drawHand(canvas, mMinuteHand, x, y, mMinutes / 60.0f * 360.0f, changed, 1.0f);
		if (mSeconds == 0.0 && mFlag) {
			mFlag = false;
			mSeconds = 59;
		}
		if (mSeconds == 0.0)
			mFlag = true;

		drawHand(canvas, mSecondHand, x, y, mSeconds / 60.0f * 360.0f, changed, 1.0f);
		Log.d(TAG, "mSeconds: " + mSeconds);

		drawPoint(canvas, mTimeCircle, x, y, changed);

		if (scaled) {
			canvas.restore();
		}
	}

	private void drawPoint(Canvas canvas, Drawable hand, int x, int y, boolean changed) {
		canvas.save();
		if (changed) {
			final int w = hand.getIntrinsicWidth();
			final int h = hand.getIntrinsicHeight();
			hand.setBounds(x - (w / 2) - 2, y - (h / 2), x + (w / 2) - 2, y + (h / 2));
		}
		canvas.scale(TIMECIRCLESCALE, TIMECIRCLESCALE, x, y);
		hand.draw(canvas);
		canvas.restore();
	}

	private void drawSubBg(Canvas canvas, Drawable hand, int x, int y, boolean changed, float scale) {
		canvas.save();
		// canvas.scale(WEEKSCALE, WEEKSCALE, x, y);
		if (changed) {
			int w = (int) (hand.getIntrinsicWidth() * scale);
			int h = (int) (hand.getIntrinsicHeight() * scale);
			hand.setBounds(x - (w / 2), y - (h / 2), x + (w / 2), y + (h / 2));
			// Log.d(TAG, "4--------:hight: "+getHeight()/4+" h: "+h);
		}

		hand.draw(canvas);
		canvas.restore();
	}

	private void drawHand(Canvas canvas, Drawable hand, int x, int y, float angle, boolean changed, float scale) {
		canvas.save();
		canvas.rotate(angle, x, y);
		canvas.scale(scale, scale, x, y);
		if (changed) {
			final int w = hand.getIntrinsicWidth();
			final int h = hand.getIntrinsicHeight();
			hand.setBounds(x - (w / 2), y - (h / 2), x + (w / 2), y + (h / 2));
		}
		hand.draw(canvas);
		canvas.restore();
	}

	private void onTimeChanged() {
		mCalendar.setToNow();

		if (mTimeZoneId != null) {
			mCalendar.switchTimezone(mTimeZoneId);
		}

		int hour = mCalendar.hour;
		int minute = mCalendar.minute;
		int second = mCalendar.second;
		// long millis = System.currentTimeMillis() % 1000;

		mSeconds = second;// (float) ((second * 1000 + millis) / 166.666);
		mMinutes = minute + second / 60.0f;
		mHour = hour + mMinutes / 60.0f;
		mChanged = true;
		mWeek = mCalendar.weekDay;
		mMonth = mCalendar.month + 1;
		mDay = mCalendar.monthDay;
		updateContentDescription(mCalendar);
	}

	private final BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getAction().equals(Intent.ACTION_TIMEZONE_CHANGED)) {
				String tz = intent.getStringExtra("time-zone");
				mCalendar = new Time(TimeZone.getTimeZone(tz).getID());
			}
			onTimeChanged();
			invalidate();
		}
	};

	private final Runnable mClockTick = new Runnable() {

		@Override
		public void run() {
			onTimeChanged();
			invalidate();
			AnalogClock.this.postDelayed(mClockTick, 1000);
		}
	};

	private void updateContentDescription(Time time) {
		final int flags = DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_24HOUR;
		String contentDescription = DateUtils.formatDateTime(mContext, time.toMillis(false), flags);
		setContentDescription(contentDescription);
	}

	public void setTimeZone(String id) {
		mTimeZoneId = id;
		onTimeChanged();
	}

	public void enableSeconds(boolean enable) {
		mNoSeconds = !enable;
	}

}
