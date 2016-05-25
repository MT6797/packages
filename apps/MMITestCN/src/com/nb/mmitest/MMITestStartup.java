package com.nb.mmitest;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Window;
import android.view.WindowManager;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MMITestStartup extends Activity {
	/** Called when the activity is first created. */

	private String TAG = "MMITestStartup";

	@Override
	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

		// set screen appearance
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		int mask = WindowManager.LayoutParams.FLAG_FULLSCREEN;
		mask |= WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
		getWindow().setFlags(mask, mask);

		/* create MMITest first screen dynamically */
		LinearLayout ll = new LinearLayout(this);
		// ll.setLayoutParams(new
		// LinearLayout.LayoutParams(LayoutParams.FILL_PARENT,LayoutParams.FILL_PARENT));
		ll.setOrientation(LinearLayout.VERTICAL);

		LinearLayout llsk = new LinearLayout(this);
		llsk.setOrientation(LinearLayout.HORIZONTAL);

		TextView tvtitle = new TextView(this);
		tvtitle.setGravity(Gravity.CENTER);
		tvtitle.setTypeface(Typeface.MONOSPACE, 1);
		// tvtitle.setTextAppearance(mActivity,
		// android.R.style.TextAppearance_Large);
		tvtitle.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
		tvtitle.setTextSize(20);
		tvtitle.setText("ALCATEL MOBILE PHONES");

		TextView tvbody = new TextView(this);
		tvbody.setGravity(Gravity.CENTER);
		// tvbody.setTypeface(Typeface.MONOSPACE, 1);
		tvbody.setTextAppearance(this, android.R.style.TextAppearance_Large);
		tvbody.setText("OPAL MMITEST Startup\n");

		// add everything to layout with accurate weights
		ll.addView(tvtitle, new LinearLayout.LayoutParams(
				LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT, 3));
		ll.addView(tvbody, new LinearLayout.LayoutParams(
				LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT, 1));

		setContentView(ll);

		BackLight bl = new BackLight();
		bl.setLcdBacklight(255);
		bl.setKbdBacklight(255);

	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent msg) {
		boolean result = false;

		switch (keyCode) {
		// here we switch to the appropriate key handler
		default:
			finish();
			break;

		}

		return result;
	}

	private void StartManualActivity() {
	}

	private void StartAutoActivity() {
	}

}
