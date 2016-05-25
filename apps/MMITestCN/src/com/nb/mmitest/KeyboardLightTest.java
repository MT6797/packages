
package com.nb.mmitest;
import java.math.BigDecimal;

import android.view.WindowManager;

class KeyboardLightTest extends Test {
	
	private final int STATE_BRIGHT = INIT + 1;
	private final int STATE_DARK = INIT + 2;
	
	private static int MAX_LOOP_KEYPAD = 2;	   
    
    private TestLayout1 tl;
    int mLoop = 0;
    CallBack callback;
	KeyboardLightTest(ID pid, String s) {
		this(pid,s,0);
	}
	
	KeyboardLightTest(ID pid, String s,int timein ) {
		super(pid, s, timein, 0);	
	}
	

	
	@Override
	protected void Run() {
		switch (mState) {		
		case STATE_BRIGHT:
			setKeyboardBacklight(70);
			callback =  new CallBack() {
				public void c() {					
					mState = STATE_DARK;					
					Run();
				}
			};
			SetTimer(1000, callback);
			break;
		case STATE_DARK:
			if(mLoop < MAX_LOOP_KEYPAD){
				setKeyboardBacklight(0);
				callback = new CallBack() {
					public void c() {
						mLoop++;
						mState = STATE_BRIGHT;					
						Run();
					}
				};
				SetTimer(1000, callback);
			} else {
				mState = END;			
				Run();
			}
			
			break;
			
		case INIT:	
			mLoop = 0;
		    tl = new TestLayout1(mContext, mName, "KEYPAD BACKLIGHT OK?");		
			mContext.setContentView(tl.ll);
			tl.setEnabledButtons(false);	
			callback = new CallBack() {
				public void c() {
					mState = STATE_BRIGHT;
					Run();
				}
			};
			SetTimer(100, callback);
			break;
		case END:
			mLoop = 0;
			tl.setEnabledButtons(true);		
			mContext.setContentView(tl.ll);
			if(callback != null){
				StopTimer();
			}
			break;
		}
	}

	private void setKeyboardBacklight(int brightness) {
		double d = brightness * 255.0D / 100.0D;
		int i = new BigDecimal(d).setScale(0, 6).intValue();
		if (i < 0) {
			i = 0;
		}
		WindowManager.LayoutParams b = mContext.getWindow().getAttributes();
		b.buttonBrightness = (float) brightness / 100.0f;	
		mContext.getWindow().setAttributes(b);
	}
}
