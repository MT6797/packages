
package com.nb.mmitest;

import com.nb.mmitest.R;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.Rect;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import com.nb.mmitest.ExecuteTest;
import com.nb.mmitest.MMITest;
import com.nb.mmitest.Test;



/*
 * Empty Test use as a default when no test was defined
 */
class TPTest extends Test {

	private TestLayout1 tl;

	final private TestRect[] tr = new TestRect[200];
	final private int MyUnit = 80;
	 private int count_x = 0;
	 private int count_y = 0;
	 private int count_total = 0;
			//(2 * count_y) + (4 * (count_x - 2));;
	

	private void initRect(){	
		int i, j;
		Log.d(TAG,"TCT Lcd.width()="+Lcd.width()+"   count_x="+count_x+"  Lcd.height="+Lcd.height()+"  count_y="+count_y);
		//two vertical lines.
		Point pa = new Point(0, 0);
		Point pb = new Point(MyUnit *(count_x - 1), 0);
		for (i=0; i < 2 *count_y; i+=2) {
			tr[i] = new TestRect(MyUnit, pa);
			tr[i+1] = new TestRect(MyUnit, pb);
			pa.y += MyUnit;
			pb.y += MyUnit;
		}
		
		//two horizontal line.
		pa.set(MyUnit, 0);
		pb.set(MyUnit, (MyUnit *(count_y - 1)));
		for (j=0; j < 2 *(count_x -1); j+=2, i+=2) {
			tr[i] = new TestRect(MyUnit, pa);
			tr[i+1] = new TestRect(MyUnit, pb);
			pa.x += MyUnit;
			pb.x += MyUnit;
		}
		
		//two X lines.
//		(Cx - 2) * MU = MU + ((Cy -2) - 1) * STEP
//		=> STEP = MU * (Cx - 3)/ (Cy - 3)
//		             _ _ _               x
//		            |     |              x
//		       _ _ _|     | MU
//		      |     |_ _ _|              x
//		 _ _ _|     |                    x
//		|     |_ _ _| STEP
//		|     |                          x
//		|_ _ _| STEP
		pa.set(MyUnit, MyUnit);
		pb.set(MyUnit, (MyUnit *(count_y - 2)));
		int step = 0;
		boolean isLandscape = false;
		int count_steps = 0;
		if (Lcd.height() > Lcd.width()){//Portrait
			step = (count_x - 3) * MyUnit / (count_y-3);
			count_steps = count_y;
		}else{
			step = (count_y - 3) * MyUnit / (count_x-3);
			isLandscape = true;
			count_steps = count_x;
		}
		
		for (j=0; j < 2 *(count_steps -1); j+=2, i+=2) {
			tr[i] = new TestRect(MyUnit, pa);
			tr[i+1] = new TestRect(MyUnit, pb);
			if(isLandscape){
				pa.y += step;
				pa.x += MyUnit;
				pb.y -= step;
				pb.x += MyUnit;
			}else{
				pa.x += step;
				pa.y += MyUnit;
				pb.x += step;
				pb.y -= MyUnit;
			}
		}
		count_total = i;
		Log.d(TAG,"TCT $$$$$$$$$$$$$$$$$ i="+i);
	}
	
	TPTest(ID pid, String s) {
		super(pid, s);
		 count_x = Lcd.width() / MyUnit;
		 count_y = Lcd.height() / MyUnit;
		initRect();
	}

	TPTest(ID pid, String s, int timein) {
		super(pid, s, timein, 0);
	}

	float mAverageX=0;
	float mAverageY=0;
	
	int mEcartType=0;

	@Override
	protected void Run() {
		// this function executes the test
		switch (mState) {
		case INIT: //
		
		    mTimeIn.start();

			// result will be set to false if the pen goes out of the shapes 
			Result = NOT_TESTED;
			if(MMITest.mode == MMITest.AUTO_MODE && false ){
			tl = new TestLayout1(mContext, "Please draw on the canvas",new MyView(mContext));
				mContext.setContentView(tl.ll);
			}else{
				mContext.setContentView(new MyView(mContext));
			}
			
			mState++;
			
			for (int i=0;i<count_total;i++){
				tr[i].cancelTested();				
			}
			break;

		case END:
			if(MMITest.mode == MMITest.AUTO_MODE){
			tl = new TestLayout1(mContext, mName, "test finished");
			mContext.setContentView(tl.ll);
			}
			// Exit();

			break;
		default:
		}
	}
	public class MyView extends View{

		private Bitmap  mBitmap;
		private Canvas  mCanvas;
		private Path    mPath;
		private Paint   mBitmapPaint;
		private Paint mPaint;
		private AlertDialog mAlertDialog, mAlertDialogMsg, mAlertDialogEnd,mAlertDialogOK;
		
		public MyView(Context c) {
			super(c);
			mPaint = new Paint();
			mPaint.setAntiAlias(true);
			//mPaint.setDither(true);
			mPaint.setColor(0xFFFF0000);
			mPaint.setStyle(Paint.Style.STROKE);
			mPaint.setStrokeJoin(Paint.Join.BEVEL);
			mPaint.setStrokeCap(Paint.Cap.ROUND);
			mPaint.setStrokeWidth(1);
			mBitmap = Bitmap.createBitmap(Lcd.width(), Lcd.height(), Bitmap.Config.ARGB_8888);
			mCanvas = new Canvas(mBitmap);
			mPath = new Path();
			mBitmapPaint = new Paint(Paint.DITHER_FLAG);
			
			 mAlertDialogOK = new AlertDialog.Builder(mContext)
			.setTitle("TEST RESULT")
			.setMessage("OK!")
			.setNegativeButton("PASS",
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog,
								int whichButton) {
							ExecuteTest.currentTest.Result = Test.PASSED;
							ExecuteTest.currentTest.Exit();
						}
					}).create();
		}
		
		@Override
		protected void onDraw(Canvas canvas) {
			canvas.drawColor(Color.WHITE/*0xFFAAAAAA*/);
			canvas.drawBitmap(mBitmap, 0, 0, mBitmapPaint);
			
			Paint p = new Paint();
			p.setColor(Color.BLACK);
			p.setStyle(Paint.Style.STROKE);
			p.setTextSize(20);
			
			/* draw 2 parallelepipede on the screen */
			for (int i=0;i<count_total;i++){
				tr[i].draw(canvas);
			}
			
			canvas.drawText(getResource(R.string.tp_test_tip), Lcd.width() / 2 - 100, 25, p);
			//canvas.drawText("shapes on the yellow area", Lcd.width() / 2 - 100, 50, p);
			/* draw the current pen position */
			canvas.drawPath(mPath, mPaint);
		}
		
		boolean Check_Result()
		{
		for (int i=0;i<count_total;i++){
			if(tr[i].isTested()==false){
				Log.d(TAG, "TCT  ==============done.count_total=="+count_total+"  i="+i);
			return false;				
				
			}
		}
		return true;
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
				mPath.quadTo(mX, mY, (x + mX)/2, (y + mY)/2);
				mX = x;
				mY = y;
			}
		}
		private void touch_up() {
			mPath.lineTo(mX, mY);
			// commit the path to our offscreen
			mCanvas.drawPath(mPath, mPaint);
			// kill this so we don't double draw
			//mPath.reset();
		}

		@Override
		public boolean onTouchEvent(MotionEvent event) {
			float x = event.getX();
			float y = event.getY();
			
            // check if the point is inside the bounds drawn on the screen
			for (int i=0;i<count_total;i++){
				if(tr[i].includePoint((int)x, (int)y)){
					tr[i].setTested();
				}
			}
			Log.d(TAG, "x = "+x+" y = "+y);

			switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				touch_start(x, y);
				invalidate();
				mAverageX=x;mAverageY=y;
				break;
			case MotionEvent.ACTION_MOVE:
				touch_move(x, y);
				invalidate();
				mAverageX=(x+mAverageX)/2;mAverageY=(y+mAverageY)/2;
				break;
			case MotionEvent.ACTION_UP:
				touch_up();
				invalidate();
				Log.d(TAG, "AVERAGES : x = "+mAverageX+" y = "+mAverageY);
				
				//check result

				Result = Check_Result() == true ? PASSED : FAILED;
				
				if(PASSED==Result)
				{
					mAlertDialog = mAlertDialogOK;
				
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

	class TestRect {
		private Path mPath;
		private Paint mPaint;
		private Point[] points= new Point[4];
		private boolean tested = false;

		TestRect(){
		}
		
		TestRect(int l, Point p){
			this(l, p.x, p.y);
		}
		
		TestRect(int l, int x, int y){
			points[0] = new Point(x, y);
			points[1] = new Point(x + l, y);
			points[2] = new Point(x + l, y + l);
			points[3] = new Point(x, y + l);

			mPath = new Path();

			mPaint = new Paint();
			mPaint.setAntiAlias(true);
			mPaint.setColor(Color.YELLOW);
			mPaint.setStyle(Paint.Style.FILL);
			mPaint.setStrokeJoin(Paint.Join.BEVEL);
			mPaint.setStrokeCap(Paint.Cap.ROUND);
			mPaint.setStrokeWidth(1);
		}
		
		void draw(Canvas c){
			mPath.reset();
			mPath.moveTo(points[0].x, points[0].y);
			for(int i=1;i<points.length;i++){
				mPath.lineTo(points[i].x, points[i].y);
			}
			mPath.close();
			c.drawPath(mPath, mPaint);
		}
		
		boolean includePoint(int x, int y){
			if((x > points[0].x) && (x < points[1].x)){
				if((y > points[0].y) && (y < points[3].y)){
					return true;
				}
			}
			return false;
		}
		
		void setTested(){
			tested = true;
			mPaint.setColor(Color.GREEN);
		}
		
		void cancelTested(){
			tested = false;
			mPaint.setColor(Color.YELLOW);
		}
		
		boolean isTested(){
			return tested;
		}
	}


}

