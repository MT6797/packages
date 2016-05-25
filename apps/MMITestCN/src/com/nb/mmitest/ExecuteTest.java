package com.nb.mmitest;

import android.app.Activity;
//import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
//import android.widget.TableLayout;
import android.widget.TextView;
import android.content.res.Configuration;
import com.nb.mmitest.R;
import android.os.SystemProperties;
public class ExecuteTest extends Activity {

	static Test currentTest = null; // points to the test currently executed

	String TAG = "ExecuteTest";

	private final boolean DEBUG = false;

	private static int lockCount = 0;

	public static void lock() {
		if (lockCount == 0)
			lockCount=1;
		//else
		//	while (lockCount != 0)
		//		;
		/*
		 * try { wait(); }catch ( InterruptedException e ) { }
		 */
	}
	
	public static int getLock()
	{
		return lockCount;
	}

	public static void unlock() {
		if (lockCount > 0)
			lockCount=0;
		if (lockCount == 0)
			;// notify();
	}

	private void LOGD(String s) {
		if (DEBUG) {
			Log.d(TAG, s);
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Test pl = getIntent().getParcelableExtra("CurrentTest");
		// TestParcel pl = getIntent().getParcelableExtra("CurrentTest");
		// Test pl = (Test) getIntent().getSerializableExtra("CurrentTest");
		// if (pl != null ) {
		// Log.d(TAG, pl.toString() );
		// }

		// set screen appearance
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		int mask = WindowManager.LayoutParams.FLAG_FULLSCREEN;
		mask |= WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
		getWindow().setFlags(mask, mask);

		// TextView tv = new TextView(this);
		// tv.setText("Empty");

		// setContentView(tv);

		currentTest.Create(this);
	//	if (currentTest.getId() == Test.ID.KEYPAD.ordinal())
		{
        //               getWindow().setFlags(WindowManager.LayoutParams.FLAG_HOMEKEY_DISPATCHED, WindowManager.LayoutParams.FLAG_HOMEKEY_DISPATCHED);

		}

	}

	@Override
	protected void onStart() {
		super.onStart();
		SystemProperties.set("sys.config.mmitest", "1");
		// execute the current test
		currentTest.Start();

		LOGD("onStart");
	}

	@Override
	public void onResume() {
		super.onResume();
		currentTest.onResume();
		LOGD("onResume");
		SystemProperties.set("sys.config.mmitest", "1");

	}

	@Override
	public void onPause() {
		super.onPause();

	}

	@Override
	public void onStop() {
		super.onStop();
		// currentTest.Stop(); no idle timeout!
		//SystemProperties.set("sys.config.mmitest", "0");
	}
	
		
	@Override
	public void onDestroy() {
		super.onDestroy();
		Log.d(TAG, "onDestroy() currentTest = " + currentTest);
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		// Retrieve Data from Event
		boolean result = true;
		final int eventAction = event.getAction();
		if (eventAction == MotionEvent.ACTION_UP) {
		} else if (eventAction == MotionEvent.ACTION_DOWN) {
		}
                
                // if there is a handler for the event in the test
                 if (currentTest.hTouch != null){
                               if (currentTest.hTouch.handleTouch(event))
                                            return true;
                 }
                
		return result;
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		// TODO Auto-generated method stub
		super.onConfigurationChanged(newConfig);
		currentTest.onConfigurationChanged(newConfig);
	}

	@Override
	public boolean dispatchKeyEvent(KeyEvent event) {
		// in case we want to filter keys, return true to indicate the event was
		// consumed
		int keycode = event.getKeyCode();
		if (currentTest.getId() == Test.ID.KEYPAD.ordinal())
{
       //                getWindow().setFlags(WindowManager.LayoutParams.FLAG_HOMEKEY_DISPATCHED, WindowManager.LayoutParams.FLAG_HOMEKEY_DISPATCHED);
			return (currentTest.hKey.handleKey(event));
}
		
		if(currentTest.getId() == Test.ID.HEADSET.ordinal()){
			//don't handle the key of volume in headset test to avoid mediaplayer been reset
			if(keycode == KeyEvent.KEYCODE_VOLUME_UP|| keycode == KeyEvent.KEYCODE_VOLUME_DOWN)
				return true;
		}

		if (MMITest.mgMode == MMITest.MANU_MODE) {
			// MANUAL MODE DEFAULT KEY BEHAVIOR
			if (event.isLongPress()) {// is it a long press?
				Log.i(currentTest.toString(),
						Long.toString(event.getEventTime()
								- event.getDownTime())
								+ " repeat:"
								+ Integer.toString(event.getRepeatCount()));
				// long press just manages some exit keys
				switch (keycode) {
				case KeyEvent.KEYCODE_BACK:
					currentTest.Exit();
					finish();// always end the test if long press on back
					break;

				case KeyEvent.KEYCODE_ENDCALL:
					break;
				case KeyEvent.KEYCODE_HOME:
					break;
				default:
					break;
				}
				return true;// always handle long press on these events
			} else {
				// if there is a handler for the event in the test
				if (currentTest.hKey != null)
					if (currentTest.hKey.handleKey(event))
						return true;

				if (event.getAction() == KeyEvent.ACTION_DOWN) {
					return super.onKeyDown(keycode, event);
				} else if (keycode == KeyEvent.KEYCODE_BACK) {
					currentTest.Exit();
					finish();
					return true;// after all we don't want any other app to
								// handle it!
				}else if(keycode == KeyEvent.KEYCODE_HOME)
					return true;
			}
		} else if (MMITest.mgMode == MMITest.AUTO_MODE
				&& KeyEvent.ACTION_UP == event.getAction()) {
			// AUTO MODE DEFAULT KEY BEHAVIOR
			if (event.getEventTime() - event.getDownTime() > 500) {// is it a
																	// long
																	// press?
				Log.i(currentTest.toString(),
						Long.toString(event.getEventTime()
								- event.getDownTime())
								+ " repeat:"
								+ Integer.toString(event.getRepeatCount()));
				// long press just manages some exit keys
				switch (keycode) {
				case KeyEvent.KEYCODE_BACK:
					onTestInterrupted();
					break;
				case KeyEvent.KEYCODE_ENDCALL:
					break;
				case KeyEvent.KEYCODE_HOME:
					break;
				default:
					break;
				}
				return true;// always handle long press on these events
			} else {
				// if there is a handler for the event in the test
				if (currentTest.hKey != null)
					if (currentTest.hKey.handleKey(event))
						return true;
				// in auto mode we can set some default key behaviors
				switch (keycode) {
				case KeyEvent.KEYCODE_BACK:
					//currentTest.Start();// retest
					break;
				case KeyEvent.KEYCODE_MENU:
					// currentTest.Exit();// normal case
					break;
				case KeyEvent.KEYCODE_ENDCALL:
					break;
				case KeyEvent.KEYCODE_HOME:
					//currentTest.Exit();// normal case
					break;
				default:
					break;
				}
				return true;
			}
		}// AUTO MODE
		return true;
	}

	private void onTestInterrupted() {

		Log.i(currentTest.toString(), "test " + currentTest.toString()
				+ "interrupted!!\n");

		// first stop the test here to stop all timers
		currentTest.Stop();

		View.OnClickListener leftButton = new View.OnClickListener() {
			public void onClick(View v) {
				currentTest.Result = Test.FAILED;
				currentTest.Exit();
			}
		};

		View.OnClickListener midButton = new View.OnClickListener() {
			public void onClick(View v) {
				currentTest.Start();
			}
		};

		View.OnClickListener RightButton = new View.OnClickListener() {
			public void onClick(View v) {
				currentTest.Result = Test.PASSED;
				currentTest.Exit();
			}
		};

		TestLayout1 tl = new TestLayout1(this, AutoTest.TAG,
				"Test interrupted!\n", this.getResources().getString(R.string.fail), this.getResources().getString(R.string.restart), this.getResources().getString(R.string.pass), leftButton,
				midButton, RightButton);
		setContentView(tl.ll);
	}

}
