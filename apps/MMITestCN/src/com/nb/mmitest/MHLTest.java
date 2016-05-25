
package com.nb.mmitest;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
//import com.mediatek.hdmi;
//import com.mediatek.hdmi.HDMINative;
//import com.mediatek.hdmi.HDMILocalService;
//import com.mediatek.common.MediatekClassFactory;
//import com.mediatek.common.hdmi.IHDMINative;
import android.os.ServiceManager;
import android.os.RemoteException;
import android.util.Log;

public class MHLTest extends Test {
    /**
     * test state
     */
    public final static int RUN = 1;
   // private IHDMINative hdmiNative = null;
    private static boolean isCablePluged = false;
	private String mUsbPlugStatus;
	private HDMIServiceReceiver mReceiver = null;
	
	private String ONLINE="1";
	private String OFFLINE = "0";
	private boolean mRegistered=false;

    /**
     * HDMI state
     */
     private boolean mIsHDMIEnable = false;

    /**
     * call back
     */
    private CallBack mCallBack = new CallBack() {

        @Override
        public void c() {
            Run();
        }
    };

    /**
     * HDMI serviec
     */
    

    MHLTest(ID pid, String s) {
        super(pid, s);

    }

    @Override
    protected void Run() {
     /*   switch (mState) {
            case INIT:// initialize the HDMI service
                try {
                    // get instance of HDMI service
                	// hdmiNative = new IHDMINative();
                	hdmiNative =	MediatekClassFactory.createInstance(IHDMINative.class);
                     if(mReceiver==null){
                         mReceiver = new HDMIServiceReceiver();
                     }
                     IntentFilter filter = new IntentFilter();
                     filter.addAction(Intent.ACTION_HDMI_PLUG);
                     
                     if(!mRegistered)
                     {
                     mContext.registerReceiver(mReceiver, filter);
                     mRegistered=true;
                     }
                	 Log.d("HDMI test","isCablePluged="+isCablePluged);
                	 hdmiNative.enableHDMI(true);
                	 if(!isCablePluged)
                	 {
                	 TestLayout1 layout = new TestLayout1(mContext, "MHL test", "Please insert MHL cable");
                	 
                	 layout.setEnabledButtons(true, layout.brsk);
                	 mContext.setContentView(layout.ll);
                	 mState = RUN;
                    Log.d("HDMI test", "hdmi service running");
                	 }
                } catch (Exception e) {
                    Log.d("HDMI test", "hdmi service running excepiong");
                    // show the fail message when catch the exception and end
                    // this test
                    TestLayout1 layout = new TestLayout1(mContext, "MHL test", "running fail");
                    mContext.setContentView(layout.ll);
                }
                
               // SetTimer(500, mCallBack);
                break;
            case RUN:// start test
               // float asWidthRatio = 300.0f;
               // float asHeightRatio = 500.0f;
                try {
                    // if (!mIsHDMIEnable) {//open HDMI option
                    // asWidthRatio = intentData.getFloatExtra(INTENT_WIDTH,
                    // 0.0f);
                    // asHeightRatio =
                    // intentData.getFloatExtra(INTENT_HEIGHT,
                    // 0.0f);
                  //  mHdmiService.setActionsafeWidthRatio(asWidthRatio);
                  //  mHdmiService.setActionsafeHeightRatio(asHeightRatio);
                //hdmiNative.enableHDMI(true);
                     mIsHDMIEnable = true;
                	Log.d("HDMI test","mUsbPlugStatus="+mUsbPlugStatus);
                	Log.d("HDMI test","RUN  isCablePluged="+isCablePluged);
                	if(mUsbPlugStatus.equals(ONLINE))
                	{
                    TestLayout1 layout = new TestLayout1(mContext, "MHL test", "MHL service on\n Please remove MHL cable");
                   
                    layout.setEnabledButtons(true, layout.brsk);
                    mContext.setContentView(layout.ll);
                    Log.d("HDMI test", "hdmi open");
                    mState = END;
                	}
                    // } else {//close HDMI option
                    // mHdmiService.setHDMIOutput(false);
                    // mState = END;
                    // TestLayout1 layout = new TestLayout1(mContext,
                    // "HDMI test", "off");
                    // mContext.setContentView(layout.ll);
                    // Log.d("HDMI test","hdmi close");
                    // }
                } catch (Exception e) {
                    // show the fail message when catch the exception and end
                    // this test
                    TestLayout1 layout = new TestLayout1(mContext, "MHL test", "running fail");
                    mContext.setContentView(layout.ll);
                    Log.d("HDMI test", "RUN  hdmi test excepion when open");
                }
                // mState = END;
                // SetTimer(500, mCallBack);
                break;
            case END:
                 mIsHDMIEnable = false;
            	Log.d("HDMI test","HDMI test mState="+mState);
                try {
                	hdmiNative.enableHDMI(false);
                	if(mUsbPlugStatus.equals(OFFLINE))
                	{
                		if(mRegistered)
                		{
                		mContext.unregisterReceiver(mReceiver);
                		
                		}
                		mRegistered=false;
            			ExecuteTest.currentTest.Result=Test.PASSED;
            			ExecuteTest.currentTest.Exit();
                	}
                    Log.d("HDMI test", "hdmi test end");
                } catch (Exception e) {
                    TestLayout1 layout = new TestLayout1(mContext, "MHL test", "running fail");
                    mContext.setContentView(layout.ll);
                    Log.d("HDMI test", "hdmi test excepion when close");
                }
                break;
            default:

        }*/

    }
    
    private class HDMIServiceReceiver extends BroadcastReceiver{
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(Intent.ACTION_HDMI_PLUG.equals(action)){
                int HDMICableState = intent.getIntExtra("state", 0);
                isCablePluged = (HDMICableState==1);
                Log.d("HDMI test","receive *********** isCablePluged= "+isCablePluged);
//                Toast.makeText(context, "HDMI cable is plug in?"+isCablePluged, Toast.LENGTH_SHORT).show();
				if(!isCablePluged){
					
					mUsbPlugStatus = OFFLINE;
				}else{
					
					mUsbPlugStatus = ONLINE;
				}
				Run();
            }
        }
    }

}
