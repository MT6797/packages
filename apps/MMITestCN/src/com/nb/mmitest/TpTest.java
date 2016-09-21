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

class TpTest1 extends Test {

	TestLayout1 tl;

	private int tolerance = (Lcd.width() == 1080)?180:(Lcd.width() == 1440)?230:80; //16;// 8;

	private int mGoodLinesCount;

	/*
	 * private Rect rTop = new Rect(-1, 50-tolerance, Lcd.width(),
	 * 50+tolerance); private Rect rBottom = new Rect(-1,
	 * Lcd.height()-50-tolerance, Lcd.width(), Lcd.height()-50+tolerance);
	 * private Rect rLeft = new Rect(50-tolerance, -1, 50+tolerance,
	 * Lcd.height()); private Rect rRight = new Rect(Lcd.width()-50-tolerance,
	 * -1, Lcd.width()-50+tolerance, Lcd.height());
	 */
	int margin = 10;

	private int dist = tolerance;
	private int start_x1 = 0;
	private int start_x2 = Lcd.width() - tolerance;
	//private int start_x3 = start_x2 + tolerance + dist;
	private int start_y1 = 0;
	private int start_y2 = Lcd.height() - tolerance;
	private int mLinesCount = 2;
	
	private Point[] p1 = {
			new Point(start_x1,0),
			new Point(start_x1, Lcd.height()-75 ),
			new Point(start_x1 + tolerance, Lcd.height()-75 ),
			new Point(start_x1 + tolerance, 0) };

	private Point[] p2 = {
			new Point(start_x2, 0),
			new Point(start_x2, Lcd.height()-75 ),
			new Point(start_x2 + tolerance, Lcd.height()-75 ),
			new Point(start_x2 + tolerance, 0) };
	
	private Point[] v1 = {
			new Point(start_y1, 0),
			new Point(start_y1, tolerance),
			new Point(Lcd.width(),start_y1 + tolerance),
			new Point(Lcd.width(), 0) };

	private Point[] v2 = {
			new Point(0,start_y2-85),
			new Point(start_x1, Lcd.height()-75),
			new Point(start_x2 + tolerance, Lcd.height()-75),
			new Point(Lcd.width(),start_y2-85) };
	
	private Point[] vp1 = {
			new Point(0, tolerance),
			new Point(start_x2, Lcd.height()),
			new Point(Lcd.width(),start_y2),
			new Point(tolerance, 0) };

	private Point[] vp2 = {
			new Point(0,start_y2),
			new Point(tolerance, Lcd.height()),
			new Point(Lcd.width(), tolerance),
			new Point(start_x2,0) };
	
	private Point[] t1 = {
			new Point(0,0),
			new Point(0, tolerance),
			new Point(tolerance, 0) };
	
	private Point[] t2 = {
			new Point(Lcd.width()-tolerance, 0),
			new Point(Lcd.width(), 0),
			new Point(Lcd.width(), tolerance) };

	private Point[] t3 = {
			new Point(0,Lcd.height()),
			new Point(0, Lcd.height() - tolerance),
			new Point(tolerance, Lcd.height()) };
	
	private Point[] t4 = {
			new Point(Lcd.width(), Lcd.height()),
			new Point(Lcd.width() - tolerance, Lcd.height()),
			new Point(Lcd.width(), Lcd.height() - tolerance) };
	
	final Parallelepipede vpl1 = new Parallelepipede(vp1);
	final Parallelepipede vpl2 = new Parallelepipede(vp2);
	final Parallelepipede tl1 = new Parallelepipede(t1);
	final Parallelepipede tl2 = new Parallelepipede(t2);
	final Parallelepipede tl3 = new Parallelepipede(t3);
	final Parallelepipede tl4 = new Parallelepipede(t4);

	TpTest1(ID pid, String s) {
		super(pid, s);

	}

	TpTest1(ID pid, String s, int timein) {
		super(pid, s, timein, 0);

	}

	float mAverageX = 0;
	float mAverageY = 0;

	int mEcartType = 0;

	@Override
	protected void Run() {
		// this function executes the test
		switch (mState) {
		case INIT: //

			mTimeIn.start();
			Log.d("bll","Lcd.width():"+Lcd.width()+"  Lcd.height:"+Lcd.height());
			// result will be set to false if the pen goes out of the shapes
			Result = NOT_TESTED;
			if (MMITest.mode == MMITest.AUTO_MODE && false) {
				tl = new TestLayout1(mContext, "Please draw on the canvas",
						new MyView(mContext));
				tl.setEnabledButtons(true);
				mContext.setContentView(tl.ll);
			} else {
				mContext.setContentView(new MyView(mContext));
			}

			mState++;

			mGoodLinesCount = 0;
			vpl1.setFinish(false);
			vpl2.setFinish(false);
			tl1.setFinish(false);
			tl2.setFinish(false);
			tl3.setFinish(false);
			tl4.setFinish(false);

			break;

		case END:

			/*if (MMITest.mode == MMITest.AUTO_MODE) {
				tl = new TestLayout1(mContext, mName, "test finished");
				mContext.setContentView(tl.ll);
			}*/
			// Exit();

			break;
		default:
		}
	}

	public class MyView extends View {

		private Bitmap mBitmap;
		private Canvas mCanvas;
		private Path mPath;
		private Paint mBitmapPaint;
		private Paint mPaint;
		private AlertDialog mAlertDialog, mAlertDialogMsg, mAlertDialogEnd, mAlertDialogOK;

		public MyView(Context c) {
			super(c);
			mPaint = new Paint();
			mPaint.setAntiAlias(true);
			// mPaint.setDither(true);
			mPaint.setColor(0xFFFF0000);
			mPaint.setStyle(Paint.Style.STROKE);
			mPaint.setStrokeJoin(Paint.Join.BEVEL);
			mPaint.setStrokeCap(Paint.Cap.ROUND);
			mPaint.setStrokeWidth(1);
			mBitmap = Bitmap.createBitmap(Lcd.width(), Lcd.height(),
					Bitmap.Config.ARGB_8888);
			mCanvas = new Canvas(mBitmap);
			mPath = new Path();
			mBitmapPaint = new Paint(Paint.DITHER_FLAG);

			mAlertDialogEnd = new AlertDialog.Builder(mContext)
					.setTitle(getResource(R.string.test_result))
					.setMessage(getResource(R.string.error1))
					/*.setPositiveButton("RETRY",
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int whichButton) {
								}
							})*/
					.setNegativeButton(getResource(R.string.fail),
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int whichButton) {
									ExecuteTest.currentTest.Result = Test.FAILED;
									ExecuteTest.currentTest.Exit();
								}
							}).create();

			mAlertDialogOK = new AlertDialog.Builder(mContext)
					.setTitle(getResource(R.string.test_result))
					.setMessage("OK!")
					.setPositiveButton(getResource(R.string.pass),
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int whichButton) {
									ExecuteTest.currentTest.Result = Test.PASSED;
									ExecuteTest.currentTest.Exit();
								}
							})
					.setNegativeButton(getResource(R.string.fail),
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int whichButton) {
									ExecuteTest.currentTest.Result = Test.FAILED;
									ExecuteTest.currentTest.Exit();
								}
							}).create();

			mAlertDialogMsg = new AlertDialog.Builder(mContext)
					.setTitle(getResource(R.string.test_result))
					.setMessage(getResource(R.string.error2))
					.setNeutralButton("OK",
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int whichButton) {
								}
							})/*
							 * .setOnKeyListener(new
							 * DialogInterface.OnKeyListener() { public boolean
							 * onKey(DialogInterface dialog, int keyCode,
							 * KeyEvent event) { if ( keyCode ==
							 * KeyEvent.KEYCODE_BACK && event.getAction() ==
							 * KeyEvent.ACTION_UP ) return false; else return
							 * true; } })
							 */
					.create();

		}

		@Override
		protected void onDraw(Canvas canvas) {
			canvas.drawColor(Color.WHITE/* 0xFFAAAAAA */);

			canvas.drawBitmap(mBitmap, 0, 0, mBitmapPaint);

			/* draw 2 parallelepipede on the screen */
			vpl1.draw(canvas);
			vpl2.draw(canvas);
			tl1.draw(canvas);
			tl2.draw(canvas);
			tl3.draw(canvas);
			tl4.draw(canvas);

			/* draw references lines on the screen */
			Paint p = new Paint();
			p.setColor(Color.BLACK);
			p.setStyle(Paint.Style.STROKE);
			p.setTextSize(20);

			canvas.drawText(getResource(R.string.tp_test_tip), Lcd.width() / 2 - 35, 25, p);
			//canvas.drawText("the yellow area", Lcd.width() / 2 - 35, 50, p);
			// footer text
			canvas.drawText((Result == FAILED ? "FAILED" : ""),
					Lcd.width() / 2 - 20, Lcd.height() - 25, p);

			/* draw the current pen position */
			canvas.drawPath(mPath, mPaint);

		}

		private float mX, mY;
		private static final float TOUCH_TOLERANCE = 1;

		private void touch_start(float x, float y) {
			mPath.reset();
			mPath.moveTo(x, y);
			mX = x;
			mY = y;
		}

		private void touch_move(float x, float y) {
			float dx = Math.abs(x - mX);
			float dy = Math.abs(y - mY);
			if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
				mPath.quadTo(mX, mY, (x + mX) / 2, (y + mY) / 2);
				mX = x;
				mY = y;
			}
		}

		private void touch_up() {
			mPath.lineTo(mX, mY);
			// commit the path to our offscreen
			mCanvas.drawPath(mPath, mPaint);
			// kill this so we don't double draw
			// mPath.reset();
		}

		@Override
		public boolean onTouchEvent(MotionEvent event) {
			float x = event.getX();
			float y = event.getY();

			// check if the point is inside the bounds drawn on the screen

			if( !vpl1.includePoint(x, y) && !vpl2.includePointv(x, y)
					&& !tl1.includePointTriangle(x, y) && !tl2.includePointTriangle(x, y)
					&& !tl3.includePointTriangle(x, y) && !tl4.includePointTriangle(x, y) ) {
			//else if( !vpl1.includePoint(x, y) && !vpl2.includePointv(x, y)) {
				Result = FAILED;
			}

			Log.d(TAG, "x = " + x + " y = " + y);

			switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				touch_start(x, y);
				invalidate();
				mAverageX = x;
				mAverageY = y;
				break;
			case MotionEvent.ACTION_MOVE:
				touch_move(x, y);
				invalidate();
				mAverageX = (x + mAverageX) / 2;
				mAverageY = (y + mAverageY) / 2;
				break;
			case MotionEvent.ACTION_UP:
				touch_up();
				invalidate();

				Log.d(TAG, "AVERAGES : x = " + mAverageX + " y = " + mAverageY);

				/* check the length of the path */
				RectF rect = new RectF(0, 0, 0, 0);
				mPath.computeBounds(rect, true);
				float mPathLength = (float) Math.sqrt(rect.height()
						* rect.height() + rect.width() * rect.width());
				//add by liliang.bao begin				  
				  float rechLen = (float) Math.sqrt(Lcd.height()
							* Lcd.height() + Lcd.width() * Lcd.width());				 
				//add by liliang.bao end

				Log.i(TAG, "path length is " + mPathLength+" rechLen: "+rechLen);

				mAlertDialog = mAlertDialogEnd;

				if (Result == FAILED) {
					mAlertDialog.setMessage(getResource(R.string.error1));
                               
				} else if (mPathLength < (rechLen-rechLen/10)) {
                               
					mAlertDialog = mAlertDialogMsg;
				} else {
					if (vpl1.includePoint(x, y) || tl1.includePointTriangle(x, y) || tl4.includePointTriangle(x, y)) {
						vpl1.setFinish(true);
					}
					if (vpl2.includePointv(x, y) || tl2.includePointTriangle(x, y) || tl3.includePointTriangle(x, y)) {
						vpl2.setFinish(true);
					}
					if (vpl1.getFinish() && vpl2.getFinish()) {
						mAlertDialog = mAlertDialogOK;
					} else {
						mAlertDialog = null;
					}
				}

				Result = NOT_TESTED;

				if (mAlertDialog == null) {

				} else if (!mAlertDialog.isShowing()) {
					if (mTimeIn.isFinished())
						mAlertDialog.show();
				}

				break;
			}
			return true;
		}

	}



	class Parallelepipede {
		private Path mPath;
		private Paint mPaint;
		private Point[] points;
		boolean isFinished = false;
		
		Parallelepipede(Point[] p) {
			points = p.clone();
			mPath = new Path();

			mPaint = new Paint();
			mPaint.setAntiAlias(true);
			mPaint.setColor(Color.YELLOW);
			mPaint.setStyle(Paint.Style.FILL);
			mPaint.setStrokeJoin(Paint.Join.BEVEL);
			mPaint.setStrokeCap(Paint.Cap.ROUND);
			mPaint.setStrokeWidth(1);
		}
		
		void setFinish(boolean b) {
			isFinished = b;
		}
		
		boolean getFinish() {
			return isFinished;
		}

		void draw(Canvas c) {
			mPath.reset();
			mPath.moveTo(points[0].x, points[0].y);
			for (int i = 1; i < points.length; i++) {
				mPath.lineTo(points[i].x, points[i].y);
			}
			mPath.close();
			c.drawPath(mPath, mPaint);
		}
		
	    private int crossProduct(Point p1, Point p2)
	    {
	        return (p1.x * p2.y - p1.y * p2.x);
	    }

	    public boolean includePointTriangle(float x, float y) {
	    	Point p = new Point((int) x, (int) y);
	    	
	        int u1 = crossProduct( new Point(points[1].x - points[0].x, points[1].y - points[0].y),
	                             new Point(p.x - points[0].x, p.y - points[0].y));
	        int u2 = crossProduct( new Point(points[2].x - points[1].x, points[2].y - points[1].y),
	                             new Point(p.x - points[1].x, p.y - points[1].y));
	        int u3 = crossProduct( new Point(points[0].x - points[2].x, points[0].y - points[2].y),
	                             new Point(p.x - points[2].x, p.y - points[2].y));

	        if ( (u1 > 0 && u2 > 0 && u3 > 0) || (u1 < 0 && u2 < 0 && u3 < 0) )
	        {
	            /* point is inside the triangle */
	            return true;
	        }
	        else
	        {
	            return false;
	        }
	    }

		/*
		 * checks if the point (x,y) is included in the Parallelepipede
		 */

		public boolean includePoint(float x, float y) {
			Point p = new Point((int) x, (int) y);
			double d1 = distLineToPoint(points[0], points[1], p);
			double d2 = distLineToPoint(points[2], points[3], p);
			double range = distLineToPoint(points[0], points[1], points[2]);
			
			Log.d(TAG,"Lcd.height():"+Lcd.height()+"    Lcd.width():"+Lcd.width());
			Log.d(TAG,"points:"+points[0]+"  "+points[1]+"  "+points[2]+"  "+p);
			Log.d(TAG, "includePoint: " + d1 + " " + d2 + " " + range);
			/*
			 * to be included in the shape, the distance from (x,y) to the
			 * bottom or top line should not exceed the distance between the
			 * bottom to top line
			 */
			Log.d(TAG,"Math.max(d1, d2):"+Math.max(d1, d2)+"   range:"+range);
			if (Math.max(d1, d2) < range) {
				return true;
				
			}

			return false;
		}
		public boolean includePointv(float x, float y) {
			Point p = new Point((int) x, (int) y);
			double d1 = distLineToPoint(points[0], points[3], p);
			double d2 = distLineToPoint(points[1], points[2], p);
			double range = distLineToPoint(points[1], points[2], points[3]);
			
			Log.d(TAG,"Lcd.height():"+Lcd.height()+"    Lcd.width():"+Lcd.width());
			Log.d(TAG,"points:"+points[0]+"  "+points[1]+"  "+points[2]+"  "+p);
			Log.d(TAG, "includePoint: " + d1 + " " + d2 + " " + range);
			/*
			 * to be included in the shape, the distance from (x,y) to the
			 * bottom or top line should not exceed the distance between the
			 * bottom to top line
			 */
			Log.d(TAG,"Math.max(d1, d2):"+Math.max(d1, d2)+"   range:"+range);
			if (Math.max(d1, d2) < range) {
				return true;
				
			}
			
			return false;
		}
		/* computes the shortest distance form a point to a line */
		/*                                                       
		 * 
		 */
		private double distLineToPoint(Point A, Point B, Point p) {

			/*
			 * let [AB] be the segment and C the projection of C on (AB) AC * AB
			 * (Cx-Ax)(Bx-Ax) + (Cy-Ay)(By-Ay) u = ------- =
			 * ------------------------------- ||AB||^2 ||AB||^2
			 */
			double det = Math.pow(B.x - A.x, 2) + Math.pow(B.y - A.y, 2);
			if (det == 0) {
				return 0;
			}

			double u = ((p.x - A.x) * (B.x - A.x) + (p.y - A.y) * (B.y - A.y))
					/ det;

			/*
			 * The projection point P can then be found:
			 * 
			 * Px = Ax + r(Bx-Ax) Py = Ay + r(By-Ay)
			 */
			double Px = A.x + u * (B.x - A.x);
			double Py = A.y + u * (B.y - A.y);

			// Log.d(TAG,"distLineToPoint : u="+u+" Px=" +Px +" Py=" +Py);

			/* the distance to (AB) is the the [Pp] segment length */

			double distance = Math.sqrt(Math.pow(Px - p.x, 2)
					+ Math.pow(Py - p.y, 2));

			return distance;
		}

	}/* Parallelepipede */
}

class TpTest2 extends Test {

	TestLayout1 tl;

	private int tolerance = (Lcd.width() == 1080)?200:(Lcd.width() == 1440)?250:100; //16;// 8;

	private int mGoodLinesCount;

	/*
	 * private Rect rTop = new Rect(-1, 50-tolerance, Lcd.width(),
	 * 50+tolerance); private Rect rBottom = new Rect(-1,
	 * Lcd.height()-50-tolerance, Lcd.width(), Lcd.height()-50+tolerance);
	 * private Rect rLeft = new Rect(50-tolerance, -1, 50+tolerance,
	 * Lcd.height()); private Rect rRight = new Rect(Lcd.width()-50-tolerance,
	 * -1, Lcd.width()-50+tolerance, Lcd.height());
	 */
	int margin = 10;

	private int dist = tolerance;
	private int start_x1 = 0;
	private int start_x2 = Lcd.width()/2 - tolerance/2;
	private int start_x3 = Lcd.width() - tolerance;
	private int mLinesCount = 3;

	private Point[] p1 = {
			new Point(start_x1, 0),
			new Point(start_x1, Lcd.height() ),
			new Point(start_x1 + tolerance, Lcd.height() ),
			new Point(start_x1 + tolerance, 0) };

	private Point[] p2 = {
			new Point(start_x2, 0),
			new Point(start_x2, Lcd.height() ),
			new Point(start_x2 + tolerance, Lcd.height() ),
			new Point(start_x2 + tolerance, 0) };

	private Point[] p3 = {
			new Point(start_x3, 0),
			new Point(start_x3, Lcd.height() ),
			new Point(start_x3 + tolerance, Lcd.height() ),
			new Point(start_x3 + tolerance, 0) };

	final Parallelepipede pl1 = new Parallelepipede(p1);
	final Parallelepipede pl2 = new Parallelepipede(p2);
	final Parallelepipede pl3 = new Parallelepipede(p3);

	TpTest2(ID pid, String s) {
		super(pid, s);

	}

	TpTest2(ID pid, String s, int timein) {
		super(pid, s, timein, 0);

	}

	float mAverageX = 0;
	float mAverageY = 0;

	int mEcartType = 0;

	@Override
	protected void Run() {
		// this function executes the test
		switch (mState) {
		case INIT: //

			mTimeIn.start();

			// result will be set to false if the pen goes out of the shapes
			Result = NOT_TESTED;
			if (MMITest.mode == MMITest.AUTO_MODE && false) {
				tl = new TestLayout1(mContext, "Please draw on the canvas",
						new MyView(mContext));
				mContext.setContentView(tl.ll);
			} else {
				mContext.setContentView(new MyView(mContext));
			}

			mState++;

			pl1.setFinish(false);
			pl2.setFinish(false);
			pl3.setFinish(false);

			break;

		case END:

			/*if (MMITest.mode == MMITest.AUTO_MODE) {
				tl = new TestLayout1(mContext, mName, "test finished");
				mContext.setContentView(tl.ll);
			}*/
			// Exit();

			break;
		default:
		}
	}

	public class MyView extends View {

		private Bitmap mBitmap;
		private Canvas mCanvas;
		private Path mPath;
		private Paint mBitmapPaint;
		private Paint mPaint;
		private AlertDialog mAlertDialog, mAlertDialogMsg, mAlertDialogEnd, mAlertDialogOK;

		public MyView(Context c) {
			super(c);
			mPaint = new Paint();
			mPaint.setAntiAlias(true);
			// mPaint.setDither(true);
			mPaint.setColor(0xFFFF0000);
			mPaint.setStyle(Paint.Style.STROKE);
			mPaint.setStrokeJoin(Paint.Join.BEVEL);
			mPaint.setStrokeCap(Paint.Cap.ROUND);
			mPaint.setStrokeWidth(1);
			mBitmap = Bitmap.createBitmap(Lcd.width(), Lcd.height(),
					Bitmap.Config.ARGB_8888);
			mCanvas = new Canvas(mBitmap);
			mPath = new Path();
			mBitmapPaint = new Paint(Paint.DITHER_FLAG);

			mAlertDialogEnd = new AlertDialog.Builder(mContext)
					.setTitle(getResource(R.string.test_result))
					.setMessage(getResource(R.string.error1))
/*
					.setNegativeButton("PASS",
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int whichButton) {
									ExecuteTest.currentTest.Result = Test.PASSED;
									ExecuteTest.currentTest.Exit();
								}
							})
*/
					.setPositiveButton(getResource(R.string.fail),
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int whichButton) {
									ExecuteTest.currentTest.Result = Test.FAILED;
									ExecuteTest.currentTest.Exit();
								}
							}).create();

			mAlertDialogOK = new AlertDialog.Builder(mContext)
			.setTitle(getResource(R.string.test_result))
			.setMessage("OK!")
			.setPositiveButton(getResource(R.string.pass),
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog,
								int whichButton) {
							ExecuteTest.currentTest.Result = Test.PASSED;
							ExecuteTest.currentTest.Exit();
						}
					})
			.setNegativeButton(getResource(R.string.fail),
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog,
								int whichButton) {
							ExecuteTest.currentTest.Result = Test.FAILED;
							ExecuteTest.currentTest.Exit();
						}
					}).create();

			mAlertDialogMsg = new AlertDialog.Builder(mContext)
					.setTitle(getResource(R.string.test_result))
					.setMessage(getResource(R.string.error2))
					.setNeutralButton("OK",
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int whichButton) {
								}
							})/*
							 * .setOnKeyListener(new
							 * DialogInterface.OnKeyListener() { public boolean
							 * onKey(DialogInterface dialog, int keyCode,
							 * KeyEvent event) { if ( keyCode ==
							 * KeyEvent.KEYCODE_BACK && event.getAction() ==
							 * KeyEvent.ACTION_UP ) return false; else return
							 * true; } })
							 */
					.create();

		}

		@Override
		protected void onDraw(Canvas canvas) {
			canvas.drawColor(Color.WHITE/* 0xFFAAAAAA */);

			canvas.drawBitmap(mBitmap, 0, 0, mBitmapPaint);

			/* draw 2 parallelepipede on the screen */
			pl1.draw(canvas);
			pl2.draw(canvas);
			pl3.draw(canvas);

			/* draw references lines on the screen */
			Paint p = new Paint();
			p.setColor(Color.BLACK);
			p.setStyle(Paint.Style.STROKE);
			p.setTextSize(20);
			

			/* draw lines start / end point on the screen */
/*			float Xborder = 24;
			float Yborder = 32;
			float width = 20;// 10;
			canvas.drawLine(Xborder - width, Yborder, Xborder + width, Yborder,
					p);
			canvas.drawLine(Xborder, Yborder - width, Xborder, Yborder + width,
					p);

			canvas.drawLine(Lcd.width() - Xborder - width, Yborder, Lcd.width()
					- Xborder + width, Yborder, p);
			canvas.drawLine(Lcd.width() - Xborder, Yborder - width, Lcd.width()
					- Xborder, Yborder + width, p);

			canvas.drawLine(Lcd.width() - Xborder - width, Lcd.height()
					- Yborder, Lcd.width() - Xborder + width, Lcd.height()
					- Yborder, p);
			canvas.drawLine(Lcd.width() - Xborder, Lcd.height() - Yborder
					- width, Lcd.width() - Xborder, Lcd.height() - Yborder
					+ width, p);

			canvas.drawLine(Xborder - width, Lcd.height() - Yborder, Xborder
					+ width, Lcd.height() - Yborder, p);
			canvas.drawLine(Xborder, Lcd.height() - Yborder - width, Xborder,
					Lcd.height() - Yborder + width, p);
*/
			// header text
			canvas.drawText(getResource(R.string.tp_test_tip), Lcd.width() / 2 - 35, 25, p);
			//canvas.drawText("the yellow area", Lcd.width() / 2 - 35, 50, p);
			// footer text
			canvas.drawText((Result == FAILED ? getResource(R.string.fail) : ""),
					Lcd.width() / 2 - 20, Lcd.height() - 25, p);

			/* draw the current pen position */
			canvas.drawPath(mPath, mPaint);

		}

		private float mX, mY;
		private static final float TOUCH_TOLERANCE = 1;

		private void touch_start(float x, float y) {
			mPath.reset();
			mPath.moveTo(x, y);
			mX = x;
			mY = y;
		}

		private void touch_move(float x, float y) {
			float dx = Math.abs(x - mX);
			float dy = Math.abs(y - mY);
			if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
				mPath.quadTo(mX, mY, (x + mX) / 2, (y + mY) / 2);
				mX = x;
				mY = y;
			}
		}

		private void touch_up() {
			mPath.lineTo(mX, mY);
			// commit the path to our offscreen
			mCanvas.drawPath(mPath, mPaint);
			// kill this so we don't double draw
			// mPath.reset();
		}

		@Override
		public boolean onTouchEvent(MotionEvent event) {
			float x = event.getX();
			float y = event.getY();

			// check if the point is inside the bounds drawn on the screen
			if (!pl1.includePoint(x, y) && !pl2.includePoint(x, y) && !pl3.includePoint(x, y)) {
				Result = FAILED;
			}

			Log.d(TAG, "x = " + x + " y = " + y);

			switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				touch_start(x, y);
				invalidate();
				mAverageX = x;
				mAverageY = y;
				break;
			case MotionEvent.ACTION_MOVE:
				touch_move(x, y);
				invalidate();
				mAverageX = (x + mAverageX) / 2;
				mAverageY = (y + mAverageY) / 2;
				break;
			case MotionEvent.ACTION_UP:
				touch_up();
				invalidate();

				Log.d(TAG, "AVERAGES : x = " + mAverageX + " y = " + mAverageY);

				/* check the length of the path */
				RectF rect = new RectF(0, 0, 0, 0);
				mPath.computeBounds(rect, true);
				float mPathLength = (float) Math.sqrt(rect.height()
						* rect.height() + rect.width() * rect.width());
				Log.e(TAG, "height:"+rect.height()+" width:"+rect.width());
				Log.i(TAG, "path length is " + mPathLength);				
				

				mAlertDialog = mAlertDialogEnd;

				if (Result == FAILED) {
					mAlertDialog.setMessage(getResource(R.string.error1));
                   //modify by liliang.bao   begin        
				} else if (mPathLength < Lcd.height()-Lcd.height()/10) {
                   //modify by liliang.bao end        
					mAlertDialog = mAlertDialogMsg;
				} else {
					if (pl1.includePoint(x, y)) {
						pl1.setFinish(true);
					}
					if (pl2.includePoint(x, y)) {
						pl2.setFinish(true);
					}
					if (pl3.includePoint(x, y)) {
						pl3.setFinish(true);
					}
					if (pl1.getFinish() && pl2.getFinish() && pl3.getFinish()) {
						mAlertDialog = mAlertDialogOK;
					} else {
						mAlertDialog = null;
					}
				}

				Result = NOT_TESTED;

				if (mAlertDialog == null) {

				} else if (!mAlertDialog.isShowing()) {
					if (mTimeIn.isFinished())
						mAlertDialog.show();
				}

				break;
			}
			return true;
		}

	}

	class Parallelepipede {
		private Path mPath;
		private Paint mPaint;
		private Point[] points;
		private boolean isFinished;

		Parallelepipede(Point[] p) {
			points = p.clone();
			mPath = new Path();

			mPaint = new Paint();
			mPaint.setAntiAlias(true);
			mPaint.setColor(Color.YELLOW);
			mPaint.setStyle(Paint.Style.FILL);
			mPaint.setStrokeJoin(Paint.Join.BEVEL);
			mPaint.setStrokeCap(Paint.Cap.ROUND);
			mPaint.setStrokeWidth(1);
		}

		void draw(Canvas c) {
			mPath.reset();
			mPath.moveTo(points[0].x, points[0].y);
			for (int i = 1; i < points.length; i++) {
				mPath.lineTo(points[i].x, points[i].y);
			}
			mPath.close();
			c.drawPath(mPath, mPaint);
		}

		void setFinish(boolean b) {
			isFinished = b;
		}
		
		boolean getFinish() {
			return isFinished;
		}
	
		/*
		 * checks if the point (x,y) is included in the Parallelepipede
		 */

		public boolean includePoint(float x, float y) {
			Point p = new Point((int) x, (int) y);
			double d1 = distLineToPoint(points[0], points[1], p);
			double d2 = distLineToPoint(points[2], points[3], p);
			double range = distLineToPoint(points[0], points[1], points[2]);
			Log.d(TAG, "includePoint: " + d1 + " " + d2 + " " + range);
			/*
			 * to be included in the shape, the distance from (x,y) to the
			 * bottom or top line should not exceed the distance between the
			 * bottom to top line
			 */
			if (Math.max(d1, d2) < range) {
				return true;
			}
			return false;
		}

		/* computes the shortest distance form a point to a line */
		/*                                                       
		 * 
		 */
		private double distLineToPoint(Point A, Point B, Point p) {

			/*
			 * let [AB] be the segment and C the projection of C on (AB) AC * AB
			 * (Cx-Ax)(Bx-Ax) + (Cy-Ay)(By-Ay) u = ------- =
			 * ------------------------------- ||AB||^2 ||AB||^2
			 */
			double det = Math.pow(B.x - A.x, 2) + Math.pow(B.y - A.y, 2);
			if (det == 0) {
				return 0;
			}

			double u = ((p.x - A.x) * (B.x - A.x) + (p.y - A.y) * (B.y - A.y))
					/ det;

			/*
			 * The projection point P can then be found:
			 * 
			 * Px = Ax + r(Bx-Ax) Py = Ay + r(By-Ay)
			 */
			double Px = A.x + u * (B.x - A.x);
			double Py = A.y + u * (B.y - A.y);

			// Log.d(TAG,"distLineToPoint : u="+u+" Px=" +Px +" Py=" +Py);

			/* the distance to (AB) is the the [Pp] segment length */

			double distance = Math.sqrt(Math.pow(Px - p.x, 2)
					+ Math.pow(Py - p.y, 2));

			return distance;
		}

	}/* Parallelepipede */

}
