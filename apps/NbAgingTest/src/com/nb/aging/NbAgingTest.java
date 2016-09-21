package com.nb.aging;

import java.io.IOException;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ActivityInfo;
import android.content.res.AssetFileDescriptor;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.Typeface;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.VideoView;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

public class NbAgingTest extends Activity {

	private CameraPreView mPreview;

	private static WindowManager wm;
	private static WindowManager.LayoutParams params;
	private VideoView videoView;

	private ActivityManager mActivityManager;
	private int mCameraId = 0;
	private FrameLayout mMainLayout;
	private AudioManager mAm;
	private Vibrator mVibrate;
	private String TAG = "NbAgingTest";
	private final int CAMERA_CHANGE_MSG = 1;
	private final int UPDATE_TIME_MSG = 2;
	private final int AUDIO_MODE_MSG = 3;
	private long mTimeCount = 0;
	private SharedPreferences mSharePreference;
	private Editor mEditor;
	private boolean mAudioNormalMode = true;
	private int AUDIO_MODE_SWITCH_SECONDS = 15;
	private int CAMERA_SWITCH_SECONDS = 10;
	private long mElapseTime = 0;
	private long mFrontCameraTestTime = 0;
	private long mBackCameraTestTime = 0;
	private long mAudioNormalModeTestTime = 0;
	private long mAudioCallModeTestTime = 0;
	private Handler mHander = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			// TODO Auto-generated method stub
			super.handleMessage(msg);
			if (msg.what == CAMERA_CHANGE_MSG) {
				Log.d(TAG, "=====>CAMERA_CHANGE_MSG");
				if (mPreview != null)
					mMainLayout.removeView(mPreview);
				if (mCameraId == 0) {
					mPreview = new CameraPreView(NbAgingTest.this);
					mCameraId = 1;
				} else {
					mPreview = new CameraPreView(NbAgingTest.this, 0, 1);
					mCameraId = 0;
				}
				mHander.sendEmptyMessageDelayed(CAMERA_CHANGE_MSG, CAMERA_SWITCH_SECONDS * 1000);
				mMainLayout.addView(mPreview,
						new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT, 1));
			} else if (msg.what == UPDATE_TIME_MSG) {
				mHander.sendEmptyMessageDelayed(UPDATE_TIME_MSG, 1000);
				mElapseTimeText.setText(
						String.format("%02d:%02d:%02d", mElapseTime / 3600, (mElapseTime / 60) % 60, mElapseTime % 60));
				if (mElapseTime > mTimeCount) {
					Log.d(TAG, "test finish");
					mHander.removeMessages(UPDATE_TIME_MSG);
					mHander.removeMessages(AUDIO_MODE_MSG);
					mHander.removeMessages(CAMERA_CHANGE_MSG);
					NbAgingTest.this.finish();
				}

				mElapseTime++;
				if (mCameraId == 1)
					mFrontCameraTestTime++;
				else
					mBackCameraTestTime++;

				if (mAm.getMode() == AudioManager.MODE_NORMAL)
					mAudioNormalModeTestTime++;
				else
					mAudioCallModeTestTime++;

				saveTestTime();

			} else if (msg.what == AUDIO_MODE_MSG) {
				Log.d(TAG, "*****>AUDIO_MODE_MSG mAudioNormalMode:" + mAudioNormalMode);
				if (mAudioNormalMode) {
					mAm.setMode(AudioManager.MODE_NORMAL);
					mAudioNormalMode = false;
				} else {
					mAudioNormalMode = true;
					mAm.setMode(AudioManager.MODE_IN_CALL);
					mAm.setSpeakerphoneOn(false);
					mAm.setWiredHeadsetOn(false);
					int maxVol = mAm.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL);

					int defaultVol = mAm.getStreamVolume(AudioManager.STREAM_VOICE_CALL);
					mAm.setStreamVolume(AudioManager.STREAM_VOICE_CALL, maxVol, AudioManager.STREAM_VOICE_CALL);
				}
				mHander.sendEmptyMessageDelayed(AUDIO_MODE_MSG, AUDIO_MODE_SWITCH_SECONDS * 1000);
			}
			// NbAgingTest.this.mMainLayout.invalidate();
		}
	};

	private TextView mElapseTimeText;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mPreview = new CameraPreView(this);
		mMainLayout = new FrameLayout(this);
		// mMainLayout.addView(mPreview,
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		int mask = WindowManager.LayoutParams.FLAG_FULLSCREEN;
		mask |= WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
		getWindow().setFlags(mask, mask);
		// new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT,
		// LayoutParams.FILL_PARENT, 1));
		setContentView(mMainLayout);
		mAm = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);
		wm = (WindowManager) getApplicationContext().getSystemService(Context.WINDOW_SERVICE);
		mSharePreference = getSharedPreferences("AgingConfig", Context.MODE_PRIVATE);
		mEditor = mSharePreference.edit();
		//startTest();
	}

	@Override
	public void onResume() {
		// mHander.removeMessages(CAMERA_CHANGE_MSG);
		 startTest();
		super.onResume();
	}

	@Override
	public void onStop() {
		if (videoView != null)
		{
			videoView.stopPlayback();
			wm.removeView(videoView);
		}
		if (mElapseTimeText != null)
			wm.removeView(mElapseTimeText);
		if (mVibrate != null)
			mVibrate.cancel();
		videoView = null;
		mElapseTimeText = null;
		
		mHander.removeMessages(UPDATE_TIME_MSG);
		mHander.removeMessages(AUDIO_MODE_MSG);
		mHander.removeMessages(CAMERA_CHANGE_MSG);
		mAm.setMode(AudioManager.MODE_NORMAL);
		super.onStop();
		Log.d(TAG, "onStop");

	}

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		Log.d(TAG, "onDestroy");
	}

	private void createElapseTimeView() {

		mElapseTimeText = new TextView(this);
		mElapseTimeText.setTextSize(30);
		params = new WindowManager.LayoutParams();

		params.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;

		params.format = PixelFormat.RGBA_8888;
		params.gravity = Gravity.TOP;
		params.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
		params.width = 550;
		params.height = 300;
		wm.addView(mElapseTimeText, params);
	}

	private void createVedioView() {

		videoView = new VideoView(this);
		videoView.setVideoURI(Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.test_video));
		videoView.setMediaController(new MediaController(this));
		videoView.requestFocus();
		videoView.start();
		videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
			public void onPrepared(MediaPlayer mp) {
				mp.start();
				mp.setLooping(true);

			}
		});

		params = new WindowManager.LayoutParams();

		params.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;

		params.format = PixelFormat.RGBA_8888;

		params.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
		params.gravity = Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL;
		params.screenOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;

		Display display = wm.getDefaultDisplay();
		Point point = new Point();
		display.getRealSize(point);
		params.width = Math.min(point.x, point.y);
		params.height = Math.max(point.x, point.y);
		videoView.setOnTouchListener(new OnTouchListener() {
			int lastX, lastY;
			int paramX, paramY;

			public boolean onTouch(View v, MotionEvent event) {
				switch (event.getAction()) {
				case MotionEvent.ACTION_DOWN:
					lastX = (int) event.getRawX();
					lastY = (int) event.getRawY();
					paramX = params.x;
					paramY = params.y;
					break;
				case MotionEvent.ACTION_MOVE:
					int dx = (int) event.getRawX() - lastX;
					int dy = (int) event.getRawY() - lastY;
					params.x = paramX + dx;
					params.y = paramY + dy;
					if(videoView != null)
						wm.updateViewLayout(videoView, params);
					break;
				}
				return true;
			}
		});

		wm.addView(videoView, params);
	}

	private void vibrate() {
		long[] pattern = new long[] { 500, 2000 };
		mVibrate = (android.os.Vibrator) this.getSystemService(Context.VIBRATOR_SERVICE);
		mVibrate.vibrate(pattern, 0);
	}

	private void startTest() {

		createElapseTimeView();
		if (mSharePreference.getBoolean("VS", true) && mSharePreference.getBoolean("VR", true)) {
			mHander.sendEmptyMessage(AUDIO_MODE_MSG);
			createVedioView();
		} else if (mSharePreference.getBoolean("VR", true)) {
			mAm.setMode(AudioManager.MODE_IN_CALL);
			mAm.setSpeakerphoneOn(false);
			mAm.setWiredHeadsetOn(false);
		//	int maxVol = mAm.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL);
		//	int defaultVol = mAm.getStreamVolume(AudioManager.STREAM_VOICE_CALL);
		//	mAm.setStreamVolume(AudioManager.STREAM_VOICE_CALL, maxVol, AudioManager.STREAM_VOICE_CALL);
			createVedioView();
		}else if(mSharePreference.getBoolean("VS", true))
		{
			mAm.setMode(AudioManager.MODE_NORMAL);			
			createVedioView();
		}

		if (mSharePreference.getBoolean("Virbate", true))
			vibrate();

		if (mSharePreference.getBoolean("FRONTCAMERA", true) && mSharePreference.getBoolean("BACKCAMERA", true))
			mHander.sendEmptyMessage(CAMERA_CHANGE_MSG);
		else if (mSharePreference.getBoolean("FRONTCAMERA", true)) {
			if (mPreview != null)
				mMainLayout.removeView(mPreview);
			mPreview = new CameraPreView(NbAgingTest.this);
			mMainLayout.addView(mPreview,
					new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT, 1));
		} else if (mSharePreference.getBoolean("BACKCAMERA", true)) {
			if (mPreview != null)
				mMainLayout.removeView(mPreview);
			mPreview = new CameraPreView(NbAgingTest.this, 0, 1);
			mMainLayout.addView(mPreview,
					new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT, 1));
		}
		
	/*	if(mSharePreference.getBoolean("MR", true))
			mAm.setParameters("SET_LOOPBACK_TYPE=21");
		else
			mAm.setParameters("SET_LOOPBACK_TYPE=0");*/


		mTimeCount = Integer.parseInt(mSharePreference.getString("TEST_Hours", "3")) * 3600
				+ Integer.parseInt(mSharePreference.getString("TEST_Mins", "0")) * 60;

		mHander.sendEmptyMessage(UPDATE_TIME_MSG);

	}

	private void saveTestTime() {
		if (mVibrate != null)
			mEditor.putString("VirbateTestTime",
					String.format("%02d:%02d:%02d", mElapseTime / 3600, (mElapseTime / 60) % 60, mElapseTime % 60));
	//	else
	//		mEditor.putString("VirbateTestTime", "00:00:00");
		if(mSharePreference.getBoolean("MR", true))
			 mEditor.putString("micReceiveTestTime",
					String.format("%02d:%02d:%02d", mElapseTime / 3600, (mElapseTime / 60) % 60, mElapseTime % 60));

		if (mSharePreference.getBoolean("VS", true) && mSharePreference.getBoolean("VR", true)) {
			mEditor.putString("vedioSpeakTestTime", String.format("%02d:%02d:%02d", mAudioNormalModeTestTime / 3600,
					(mAudioNormalModeTestTime / 60) % 60, mAudioNormalModeTestTime % 60));
			mEditor.putString("vedioReceiveTestTime", String.format("%02d:%02d:%02d", mAudioCallModeTestTime / 3600,
					(mAudioCallModeTestTime / 60) % 60, mAudioCallModeTestTime % 60));
		} else if (mSharePreference.getBoolean("VR", true)) {
			mEditor.putString("vedioReceiveTestTime",
					String.format("%02d:%02d:%02d", mElapseTime / 3600, (mElapseTime / 60) % 60, mElapseTime % 60));
		//	mEditor.putString("vedioSpeakTestTime", "00:00:00");
		} else if (mSharePreference.getBoolean("VS", true)) {
			mEditor.putString("vedioSpeakTestTime",
					String.format("%02d:%02d:%02d", mElapseTime / 3600, (mElapseTime / 60) % 60, mElapseTime % 60));
		//	mEditor.putString("vedioReceiveTestTime", "00:00:00");
		} else {
		//	mEditor.putString("vedioSpeakTestTime", "00:00:00");
		//	mEditor.putString("vedioReceiveTestTime", "00:00:00");
		}

		if (mSharePreference.getBoolean("FRONTCAMERA", true) && mSharePreference.getBoolean("BACKCAMERA", true)) {
			mEditor.putString("backCameraTestTime", String.format("%02d:%02d:%02d", mFrontCameraTestTime / 3600,
					(mFrontCameraTestTime / 60) % 60, mFrontCameraTestTime % 60));
			mEditor.putString("frontCameraTestTime", String.format("%02d:%02d:%02d", mBackCameraTestTime / 3600,
					(mBackCameraTestTime / 60) % 60, mBackCameraTestTime % 60));
		} else if (mSharePreference.getBoolean("FRONTCAMERA", true)) {
			mEditor.putString("frontCameraTestTime",
					String.format("%02d:%02d:%02d", mElapseTime / 3600, (mElapseTime / 60) % 60, mElapseTime % 60));
	//		mEditor.putString("backCameraTestTime", "00:00:00");
		} else if (mSharePreference.getBoolean("BACKCAMERA", true)) {
			mEditor.putString("backCameraTestTime",
					String.format("%02d:%02d:%02d", mElapseTime / 3600, (mElapseTime / 60) % 60, mElapseTime % 60));
	//		mEditor.putString("frontCameraTestTime", "00:00:00");
		} else {
	//		mEditor.putString("frontCameraTestTime", "00:00:00");
	//		mEditor.putString("backCameraTestTime", "00:00:00");
		}

		mEditor.commit();
	}
}
