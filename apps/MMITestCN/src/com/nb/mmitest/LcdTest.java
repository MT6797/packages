package com.nb.mmitest;

import static android.provider.Settings.System.SCREEN_OFF_TIMEOUT;
import com.nb.mmitest.R;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.AssetFileDescriptor;
import android.content.res.Resources;
import android.content.pm.ActivityInfo;
import android.graphics.*;
import android.graphics.Bitmap.Config;
//import android.hardware.Camera;
import android.media.AudioManager;
import android.media.AudioSystem;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.IPowerManager;
import android.os.PowerManager;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.Vibrator;
import android.os.BatteryManager;
import android.os.SystemClock;
import android.os.Looper;
import android.provider.Settings;


import com.android.internal.telephony.RILConstants;
import com.android.internal.telephony.TelephonyIntents;
import com.android.internal.telephony.TelephonyProperties;
import android.telephony.TelephonyManager;
import android.telephony.PhoneStateListener;

import android.text.TextUtils;
import android.text.method.MetaKeyKeyListener;
import android.util.Log;
import android.view.Gravity;
import android.util.SparseArray;

import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import java.io.File;
import java.io.FileWriter;
import java.io.FileReader;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.BufferedReader;
import java.util.Arrays;
import java.util.List;
import java.util.Iterator;

import android.view.Gravity;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.ImageView;
import android.widget.Button;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.LinearLayout; //import android.widget.LinearLayout.LayoutParams;
import android.widget.FrameLayout;
import android.view.ViewGroup.LayoutParams;
//import android.view.ViewGroup;
import java.util.ArrayList;
import java.util.Hashtable;
//import java.util.Map;
//import java.util.List;
//import java.io.IOException;
import android.widget.Toast;
import android.os.ServiceManager;

//import com.android.server.*;
/*add by stephen*/
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

//for wifi
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiManager;
import android.content.Intent;
import android.content.IntentFilter;
/*end add*/

import android.os.Vibrator;
import android.hardware.Camera;

import android.app.KeyguardManager;
import android.app.KeyguardManager.KeyguardLock;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorListener;
import android.hardware.SensorManager;

import android.location.*;

import android.content.res.Configuration;

import com.android.fmradio.FmNative;

/*
 * ColorView
 */
class ColorView extends View {

	private Paint mPainter = new Paint();

	private Rect mTempRect = new Rect();

	private CallBack mPreDrawCb;

	private int[] Colors = { Color.BLACK };

	private Bitmap LcdBitmap;

	ColorView(Context c) {
		super(c);
	}

	ColorView(Context c, int[] colors) {
		super(c);
		Colors = colors;
	}

	ColorView(Context c, int color) {
		super(c);
		Colors[0] = color;
	}

	ColorView(Context c, Bitmap b, CallBack cb) {
		super(c);
		LcdBitmap = b;
		mPreDrawCb = cb;
	}

	@Override
	protected void onDraw(Canvas canvas) {

		mPainter.setStyle(Paint.Style.FILL);

		if (LcdBitmap == null) {

			int rectLength = Lcd.height() / Colors.length;
			int FirstRectLength = Lcd.height() - (Colors.length - 1)
					* rectLength;

			mPainter.setColor(Colors[0]);
			// mPainter.setAlpha(0x20);
			// draw background rect
			mTempRect.set(0, 0, Lcd.width(), FirstRectLength);
			canvas.drawRect(mTempRect, mPainter);

			for (int i = 1; i < Colors.length; i++) {
				mPainter.setColor(Colors[i]);
				// draw rect : the function just fills INSIDE the rectangle,
				// thus the rectangle
				// has to be one more pixel wide/long
				mTempRect.set(0, FirstRectLength + rectLength * (i - 1),
						Lcd.width(), FirstRectLength + rectLength * i);
				canvas.drawRect(mTempRect, mPainter);
			}
		} else {
			// canvas.drawBitmap(LcdBitmap, 0, 0, mPainter);
			mPainter.setColor(Color.WHITE);
			mTempRect.set(0, 0, Lcd.width(), Lcd.height());
			canvas.drawRect(mTempRect, mPainter);

			for (int x = 0; x < Lcd.width(); x++) {
				for (int y = (x & 1); y < Lcd.height(); y += 2) {
					mPainter.setColor(Color.BLACK);
					canvas.drawPoint(x, y, mPainter);
				}
			}
			/*
			 * canvas.drawText(text, start, end, x, y, paint)
			 */
		}
		if (mPreDrawCb != null)
			mPreDrawCb.c();
	}

}

class LcdMireRgbTest extends Test {

	int[] Colors = { Color.RED, Color.GREEN, Color.BLUE };
	private TestLayout1 tl = null; // Add by changmei.chen@tcl.com 2013-01-19 for MMITest spec 2.0.

	LcdMireRgbTest(ID pid, String s, int min) {
		super(pid, s, min, 0);

        // Modify by changmei.chen@tcl.com 2013-01-19 for stability.
		/*hKey = new KeyHandler() {
			public boolean handleKey(KeyEvent event) {
				if (event.getKeyCode() == KeyEvent.KEYCODE_MENU)
					return !mTimeIn.isFinished();
				if (event.getKeyCode() == KeyEvent.KEYCODE_VOLUME_DOWN) {
					Exit();
					return true;
				}
				return false;
			}
		};*/

	}

	LcdMireRgbTest(ID pid, String s) {
		super(pid, s);
		// Modify by changmei.chen@tcl.com 2013-01-19 for stability.
		/*hKey = new KeyHandler() {
			public boolean handleKey(KeyEvent event) {
				if (event.getKeyCode() == KeyEvent.KEYCODE_MENU) {
					Exit();
					return true;
				}
				return false;
			}
		};*/
	}

	@Override
	protected void Run() {
		// this function executes the test
		switch (mState) {
		case INIT: // init the test, shows the first screen
			//mTimeIn.start(); // Modify by changmei.chen@tcl.com 2013-01-19 for MMITest spec 2.0.
			
			/*ColorView mrgbv = new ColorView(mContext, Colors);
			mContext.setContentView(mrgbv);*/
			
			FrameLayout fl = new FrameLayout(mContext);
			ColorView cv = new ColorView(mContext, Colors);
			fl.addView(cv);
			tl = new TestLayout1(mContext, "    ", "    ", getResource(R.string.fail), getResource(R.string.pass));
			fl.addView(tl.ll);
			
			mContext.setContentView(fl);
			// Add by changmei.chen@tcl.com 2013-01-19 for MMITest spec 2.0 begin.
			mTimeIn.start();
			if (!mTimeIn.isFinished()) 
				tl.hideButtons();
			
			//Result = Test.PASSED;
			// Add by changmei.chen@tcl.com 2013-01-19 for MMITest spec 2.0 end.
			break;
		default:
			break;
		}
	}
	// Add by changmei.chen@tcl.com 2013-01-19 for MMITest spec 2.0 begin.
	@Override
	protected void onTimeInFinished() {
	    if (tl != null)
			tl.showButtons();
	}
	// Add by changmei.chen@tcl.com 2013-01-19 for MMITest spec 2.0 end.
}

class LcdGreyChartTest extends Test {

	private final int MaxRows = 16;
	private TestLayout1 tl = null;// Add by changmei.chen@tcl.com 2013-01-19 for MMITest spec 2.0.

	int[] Colors = new int[MaxRows];

	LcdGreyChartTest(ID pid, String s, int timein) {
		super(pid, s, timein, 0);

		Colors = new int[MaxRows];

		for (int i = 0; i < Colors.length; i++) {
			Colors[i] = Color.BLACK + 0x111111 * i;
		}

        // Modify by changmei.chen@tcl.com 2013-01-19 for stability.
		/*hKey = new KeyHandler() {
			public boolean handleKey(KeyEvent event) {
				if (event.getKeyCode() == KeyEvent.KEYCODE_MENU)
					return !mTimeIn.isFinished();
				if (event.getKeyCode() == KeyEvent.KEYCODE_VOLUME_DOWN) {
					Exit();
					return true;
				}
				return false;
			}
		};*/
	}

	LcdGreyChartTest(ID pid, String s) {
		this(pid, s, 0);
		// Modify by changmei.chen@tcl.com 2013-01-19 for stability.
		/*hKey = new KeyHandler() {
			public boolean handleKey(KeyEvent event) {
				if (event.getKeyCode() == KeyEvent.KEYCODE_MENU) {
					Exit();
					return true;
				}
				return false;
			}
		};*/
	}

	/* variante */
	/*
	 * LcdGreyChartTest(ID pid, String s, int n) { // this just works for power
	 * 2 integers super(pid, s); Colors = new int[Math.min(MaxRows, n)];
	 * 
	 * for (int i = 0; i < Colors.length; i++) { Colors[i] = Color.BLACK +
	 * 0x111111 * (16 / Colors.length); } }
	 */
	@Override
	protected void Run() {
		// this function executes the test
		switch (mState) {
		case INIT: // init the test, shows the first screen
			//mTimeIn.start(); // Modify by changmei.chen@tcl.com 2013-01-19 for stability.

			/*ColorView mrgbv = new ColorView(mContext, Colors);
			mContext.setContentView(mrgbv);*/
			
			FrameLayout fl = new FrameLayout(mContext);
			ColorView cv = new ColorView(mContext, Colors);
			fl.addView(cv);	
			tl = new TestLayout1(mContext, "    ", "    ", getResource(R.string.fail), getResource(R.string.pass));
			fl.addView(tl.ll);
			
			mContext.setContentView(fl);
			// Add by changmei.chen@tcl.com 2013-01-19 for MMITest spec 2.0 begin.
			mTimeIn.start();
			if (!mTimeIn.isFinished()) 
				tl.hideButtons();
			
			//Result = Test.PASSED;
			// Add by changmei.chen@tcl.com 2013-01-19 for MMITest spec 2.0 end.
			break;
		default:
			break;
		}

	}
	// Add by changmei.chen@tcl.com 2013-01-19 for MMITest spec 2.0 begin.
	@Override
	protected void onTimeInFinished() {
	    if (tl != null)
			tl.showButtons();
	}
	// Add by changmei.chen@tcl.com 2013-01-19 for MMITest spec 2.0 end.
}

class LcdCheckerTest extends Test {

	static Bitmap b;
	private ColorView cv;

	LcdCheckerTest(ID pid, String s) {
		this(pid, s, 0);
	}

	LcdCheckerTest(ID pid, String s, int timein) {
		super(pid, s, timein, 0);

		int[] tc = new int[Lcd.height() * Lcd.width()];
		for (int i = 0; i < tc.length; i++) {
			tc[i] = ((i & 1) == 0 ? 0xFFFFFFFF/* Color.BLACK */: 0x00 /*
																	 * Color.WHITE
																	 */);
		}
		b = Bitmap.createBitmap(tc, Lcd.width(), Lcd.height(), Config.ALPHA_8);

		hKey = new KeyHandler() {
			public boolean handleKey(KeyEvent event) {
				if (event.getKeyCode() == KeyEvent.KEYCODE_MENU)
					return !mTimeIn.isFinished();
				return false;
			}
		};

	}

	@Override
	protected void Run() {
		// this function executes the test
		switch (mState) {
		case INIT: // init the test, shows the first screen

			cv = new ColorView(mContext, b, new CallBack() {
				public void c() {
					mTimeIn.start();
				}
			});
			mContext.setContentView(cv);
			Result = PASSED;
			break;
		default:
			break;
		}
	}

}

class LcdColorTest extends Test {

	int Color;
	private TestLayout1 tl = null; // Add by changmei.chen@tcl.com 2013-01-19 for MMITest spec 2.0.

	/*
	 * CallBack mtimeCB = new CallBack() { public void c() {
	 * Log.d(TAG,"min time elapsed : " + mTimeIn.getTime());
	 * 
	 * } };
	 */
	LcdColorTest(ID pid, String s, int color, int min) {
		super(pid, s, min, 0);
		Color = color;

		// TimeIn = new TimeCheck(5, mtimeCB);
		/* to prevent the user from exiting the test before min time */
		// Modify by changmei.chen@tcl.com 2013-01-19 for stability.
		/*hKey = new KeyHandler() {
			public boolean handleKey(KeyEvent event) {
				if (event.getKeyCode() == KeyEvent.KEYCODE_MENU)
					return !mTimeIn.isFinished();
				if (event.getKeyCode() == KeyEvent.KEYCODE_VOLUME_DOWN) {
					Exit();
					return true;
				}
				return false;
			}
		};*/

	}

	LcdColorTest(ID pid, String s, int color) {
		super(pid, s, 1, 0);
		Color = color;
		// Modify by changmei.chen@tcl.com 2013-01-19 for stability.
		/*hKey = new KeyHandler() {
			public boolean handleKey(KeyEvent event) {
				if (event.getKeyCode() == KeyEvent.KEYCODE_MENU) {
					Exit();
					return true;
				}
				return false;
			}
		};*/
	}

	@Override
	protected void Run() {
		// this function executes the test
		switch (mState) {
		case INIT: // init the test, shows the first screen
			//mTimeIn.start(); // Modify by changmei.chen@tcl.com 2013-01-19 for stability.
			
			FrameLayout fl = new FrameLayout(mContext);
			ColorView cv = new ColorView(mContext, Color);
			fl.addView(cv);	
			tl = new TestLayout1(mContext, "    ", "    ", getResource(R.string.fail), getResource(R.string.pass));
			fl.addView(tl.ll);
			
			mContext.setContentView(fl);
			// Add by changmei.chen@tcl.com 2013-01-19 for MMITest spec 2.0 begin.
			mTimeIn.start();
			if (!mTimeIn.isFinished()) 
				tl.hideButtons();
			
			//Result = PASSED;
			// Add by changmei.chen@tcl.com 2013-01-19 for MMITest spec 2.0 end.
			break;
		default:
			break;
		}
	}
	// Add by changmei.chen@tcl.com 2013-01-19 for MMITest spec 2.0 begin.
	@Override
	protected void onTimeInFinished() {
	    if (tl != null)
			tl.showButtons();
	}
	// Add by changmei.chen@tcl.com 2013-01-19 for MMITest spec 2.0 end.

}
