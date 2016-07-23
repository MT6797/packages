package ma.calibrate;

import ma.calibrate.R;
import ma.release.Fprint;
import ma.release.Util;
import android.os.Bundle;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Bitmap.Config;
import android.graphics.Point;
import android.graphics.Rect;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Button;
import android.view.View;
import android.widget.LinearLayout;
import android.content.Intent;

public class CaptureActivity extends Activity implements View.OnClickListener{
//	private boolean bStop = false;
	private String TAG = "APP_CTAG";
    private LinearLayout captureLayout;
    String strExtra;
    private boolean isFactory = false;
    private Button btn_success,btn_failed;

    @Override
    protected void onPause() {
        super.onPause();
    }

	@Override
	protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        strExtra = intent.getStringExtra("flag");
        if (strExtra != null && strExtra.equals("factory")) {
            isFactory = true;
		    setContentView(R.layout.fpc_capture);
            captureLayout = (LinearLayout)findViewById(R.id.capture_layout);
            captureLayout.addView(new CaptureView(this));
            btn_success = (Button) findViewById(R.id.btn_success);
            btn_failed = (Button) findViewById(R.id.btn_failed);
            btn_success.setOnClickListener(this);
            btn_failed.setOnClickListener(this);
        } else {
            setContentView(new CaptureView(this));
        }
	}

	@Override
	protected void onDestroy() {
        super.onDestroy();
        Fprint.close();
        Util.dprint("JTAG", "onDestroy");
	}

	class CaptureView extends SurfaceView implements SurfaceHolder.Callback {
		private SurfaceHolder surHolder;
		private Paint mScorePaint = new Paint(); // 分数的画图句柄
		private Paint mTipsPaint = new Paint(); // 提示的画图句柄n
		private Paint mTitlePaint = new Paint(); // 标题的画图句柄n
		private Paint TitleRectPaint = new Paint(); // 标题区域的画图句柄
		private int W = 120; // 位图宽
		private int H = 120; // 位图高
		private int tipsHeight = 36; // 文字高
		private int tipsWidth = tipsHeight * 4; // 文字宽
		private int titleHeight = 42; // 标题文字高
		private int titleWidth = titleHeight * 4; // 标题文字宽
		private Rect titleRect; // 标题区域
		private byte srcBytes[] = null;
		private Canvas mCanvas = null;
		private float mScale = 2.0f;
		private Point userTipPoint = new Point();
		private Point scorePoint = new Point();
		private Point imagePoint = new Point();
		private Point titlePoint = new Point();

		public CaptureView(Context context) {
			super(context);
			this.setKeepScreenOn(true);
			this.setFocusable(true);
			surHolder = this.getHolder();
			surHolder.addCallback(this);
			mScorePaint.setAntiAlias(true);
			mScorePaint.setTextSize(tipsHeight);
			mScorePaint.setColor(Color.RED);
			mTipsPaint.setAntiAlias(true);
			mTipsPaint.setTextSize(tipsHeight);
			mTipsPaint.setColor(Color.BLUE);
			//mTitlePaint.setAntiAlias(true);
			//mTitlePaint.setTextSize(titleHeight);
			//mTitlePaint.setColor(Color.BLACK);
			//TitleRectPaint.setColor(Color.GRAY);
			srcBytes = new byte[W * H + 1078];
		}

		@Override
		public void surfaceCreated(SurfaceHolder holder) {
			imagePoint.set((getWidth() - ((int) (W * mScale))) / 2,(getHeight() - ((int) (H * mScale))) / 2);
			userTipPoint.set(32, getHeight() - 32);
			scorePoint.set((getWidth() - tipsWidth) / 2,(getHeight() + ((int) (H * mScale))) / 2 + tipsHeight);
			titlePoint.set((getWidth() - titleWidth) / 2, titleHeight + 12);
			titleRect = new Rect(0, 0, getWidth(), titleHeight + 32);

			drawBackground();
			Fprint.bStop = false;
			new MyThread().start();
		}

		@Override
		public void surfaceChanged(SurfaceHolder holder, int format, int width,int height) {
		}

		@Override
		public void surfaceDestroyed(SurfaceHolder holder) {
		}

		public int drawScore() {
			int ret = Fprint.testImage(srcBytes, srcBytes.length);
			Util.dprint("JTAG", "drawScore() ret="+ret);
            if(ret == Fprint.FP_CHK_FULL || ret == Fprint.FP_CHK_PART){
                if (Fprint.bStop){
                    Util.dprint("JTAG", "drawScore() stoped");
                    return -2;
                }
				mCanvas = surHolder.lockCanvas();
		    	if (mCanvas == null) return 0;
				mCanvas.drawColor(Color.WHITE);
				//mCanvas.drawRect(titleRect, TitleRectPaint);
				drawBmp(srcBytes, imagePoint.x, imagePoint.y);
				mCanvas.drawText(getResources().getString(R.string.ma_capture_pass),userTipPoint.x, userTipPoint.y, mTipsPaint);
				//mCanvas.drawText(getResources().getString(R.string.ma_capture_title), titlePoint.x,titlePoint.y, mTitlePaint);
				surHolder.unlockCanvasAndPost(mCanvas);
			}

			return ret;
		}

		// 绘制指纹位图
		private boolean drawBmp(byte bytes[], int x, int y) {
			Bitmap obmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
			if (obmp == null) {
				mCanvas.drawText("Bmp NULL", x, y, mScorePaint);
				return false;
			} else {
				Bitmap sbmp = scaleBmp(obmp, mScale);
				Bitmap tbmp = toGreyBmp(sbmp);
				mCanvas.drawBitmap(tbmp, x, y, mScorePaint);
				if (!obmp.isRecycled())
					obmp.recycle();
				if (!tbmp.isRecycled())
					tbmp.recycle();
				if (!sbmp.isRecycled())
					sbmp.recycle();
				return true;
			}
		}

		public void drawBackground() {
			mCanvas = surHolder.lockCanvas();
			mCanvas.drawColor(Color.WHITE);
			//mCanvas.drawRect(titleRect, TitleRectPaint);
			mCanvas.drawText(getResources().getString(R.string.ma_capture_tip_init),userTipPoint.x, userTipPoint.y, mTipsPaint);
			//mCanvas.drawText(getResources().getString(R.string.ma_capture_title), titlePoint.x,titlePoint.y, mTitlePaint);
			surHolder.unlockCanvasAndPost(mCanvas);
		}

		/**
		 * 将彩色图转换为灰度图
		 *
		 * @bmp 位图
		 * @return 返回转换好的位图
		 */
		private Bitmap toGreyBmp(Bitmap bmp) {
			int width = bmp.getWidth(); // 获取位图的宽
			int height = bmp.getHeight(); // 获取位图的高
			int[] pixels = new int[width * height]; // 通过位图的大小创建像素点数组
			bmp.getPixels(pixels, 0, width, 0, 0, width, height);
			int alpha = 0xFF << 24;
			for (int i = 0; i < height; i++) {
				for (int j = 0; j < width; j++) {
					int grey = pixels[width * i + j];
					int red = ((grey & 0x00FF0000) >> 16);
					int green = ((grey & 0x0000FF00) >> 8);
					int blue = (grey & 0x000000FF);
					grey = (red + green + blue) / 3;
					grey = alpha | (grey << 16) | (grey << 8) | grey;
					pixels[width * i + j] = grey;
				}
			}
			Bitmap result = Bitmap.createBitmap(width, height, Config.RGB_565);
			result.setPixels(pixels, 0, width, 0, 0, width, height);
			return result;
		}

		private Bitmap scaleBmp(Bitmap bmp, float scale) {
			Matrix matrix = new Matrix();
			matrix.postScale(scale, scale);
			Bitmap result = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(),bmp.getHeight(), matrix, true);
			return result;
		}

		class MyThread extends Thread {
			@Override
			public void run() {
				while (!Fprint.bStop) {
					drawScore();
					Util.sleep(25);
				}
			}
		}
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			Fprint.bStop = true;
			Util.dprint("JTAG", "onKeyDown BACK");
			finish();
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}

    @Override
    public void onClick(View v) {
        boolean isResult = false;
        Intent intent = new Intent("ma.fprint.fingerprinttest.result");
        switch (v.getId()) {
            case R.id.btn_success:
                isResult = true;
                break;
            case R.id.btn_failed:
                isResult = false;
                break;
        }
        intent.putExtra("result", isResult);
        sendBroadcast(intent);
        Fprint.bStop = true;
        Util.dprint("JTAG", "onClick");
        finish();
    }
}
