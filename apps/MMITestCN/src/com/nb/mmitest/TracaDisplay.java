package com.nb.mmitest;

import static android.provider.Settings.System.SCREEN_OFF_TIMEOUT;
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
import android.os.IBinder;
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

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
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
import android.net.wifi.WifiInfo;
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
//import com.mediatek.common.featureoption.FeatureOption;


import android.os.SystemProperties;
import android.os.IBinder;


class TracaDisplayTest extends Test{
	private String 				TAG = "MMITEST:TracaDisplayTest";
	private TracabilityStruct 	mTStruct;
	private TestLayout1 		mLayout;
	private String 				mDisplayString;
        private static final int GEMINI_SIM_1 = 0;
        private static final int GEMINI_SIM_2 = 1;
        private final String GSensorItem="12";
        
        private final int FACTORY_HEADER_SAVE_OFFSET=256*1024;
        private final int FACTORY_HEADER_SAVE_SIZE=1*1024;
        //take up 257k-258k
        private final int FACTORY_REPORT_SAVE_OFFSET=(FACTORY_HEADER_SAVE_OFFSET + FACTORY_HEADER_SAVE_SIZE);

	TracaDisplayTest(ID pid, String str){
		super(pid, str);
		try{
			mTStruct = new TracabilityStruct();
		}catch(Exception e){
			Log.d(TAG, e.toString());
		}
	}

	/* for Tracability test, the auto test and manual test display different
	 * information,so we have 2 Run function
	 */
	@Override
	protected void Run(){
		/* check if auto test mode, Run2 is for auto test*/
	/*	if(MMITest.mgMode == MMITest.AUTO_MODE){
			Run2();
			return;
		}*/
		
		/* manual test only */
		switch(mState){
		case INIT:	
			
			String BT_address=getLocalBluetoothMacAddress(); 
			String WIFI_address=getLocalWifiMacAddress();
		/*	for(int i=0;i < 6;i++)
			{
				BT_address+=String.format("%02X", 0xFF&(new Byte(mTStruct.getItem(TracabilityStruct.ID.BT_ADDR_I)[i]).intValue()));
				WIFI_address+=String.format("%02X", 0xFF&new Byte(mTStruct.getItem(TracabilityStruct.ID.WIFI_ADDR_I)[i]).intValue());
					if(i!=5)
					{
							BT_address+=":";
							WIFI_address+=":";
					}
			}*/
			mDisplayString = "";
			if (SystemProperties.get("ro.mtk_gemini_support").equals("1") == true)
			 {
				 mDisplayString += "IMEI1:" +new TelephonyManager(mContext).getDeviceId(GEMINI_SIM_1)+ "\n";
				 mDisplayString += "IMEI2:" +new TelephonyManager(mContext).getDeviceId(GEMINI_SIM_2)+ "\n";
			 }
			 else
			 {
				 mDisplayString += "IMEI:" + new TelephonyManager(mContext).getDeviceId() + "\n";
			 }
/*			mDisplayString += "BSN:" + GetASCStringFromTrace(TracabilityStruct.ID.SHORT_CODE_I) +
					GetASCStringFromTrace(TracabilityStruct.ID.ICS_I) +
					GetASCStringFromTrace(TracabilityStruct.ID.SITE_FAC_PCBA_I) +
					GetASCStringFromTrace(TracabilityStruct.ID.LINE_FAC_PCBA_I) +
					GetASCStringFromTrace(TracabilityStruct.ID.DATE_PROD_PCBA_I) +
					GetASCStringFromTrace(TracabilityStruct.ID.SN_PCBA_I) + "\n";*/
			mDisplayString += "BT:" + BT_address + "\n";
			mDisplayString += "WIFI:" + WIFI_address + "\n";
			String serial = SystemProperties.get("gsm.serial", "0");
			int pos = serial.lastIndexOf("10P");
			if(pos >30)
				mDisplayString += "modem 校准: 成功\n";
			else
				mDisplayString += "modem 校准: 失败\n";
			
			int res = getGSensorTestResult();
			int result = getGSensorTestFileResult();
			Log.d("bll", "getGSensorTestFileResult: "+result+"  getGSensorTestResult:"+res);
			String value = SystemProperties.get("persist.radio.gsensor.cali","0");
			//if("1".equals(value)||res==1||result == 1)
			//	mDisplayString += "G-Sensor 校准: 成功\n";
			//else if(result == 2)
			//	mDisplayString += "G-Sensor 校准: 未校准\n";
			//else {
			//	mDisplayString += "G-Sensor 校准: 失败\n";
			//}
			String version = SystemProperties.get("ro.internal.build.version", "");
			mDisplayString += "内部版本:" + version + "\n";
/*			mDisplayString += "CU-REF:" + GetASCStringFromTrace(TracabilityStruct.ID.INFO_COMM_REF_I) + "\n";
			mDisplayString += "H/S PN:" + GetASCStringFromTrace(TracabilityStruct.ID.INDUS_REF_HANDSET_I) + "\n";
			mDisplayString += "PTH= " + GetASCStringFromTrace(TracabilityStruct.ID.INFO_PTM_I) + "\n";
			mDisplayString += "PT: " +GetStringFromTrace(TracabilityStruct.ID.INFO_STATUS_PARA_SYS_I) + "\n";
			mDisplayString += "PFT: " + GetStringFromTrace(TracabilityStruct.ID.INFO_STATUS_PARA_SYS_2_I) + "\n";
			mDisplayString += "BW: " + GetStringFromTrace(TracabilityStruct.ID.INFO_STATUS_BW_I) + "\n";
			mDisplayString += "MMI: " + GetStringFromTrace(TracabilityStruct.ID.INFO_STATUS_MMI_TEST_I) + "\n";
			mDisplayString += "FT: " + GetStringFromTrace(TracabilityStruct.ID.INFO_STATUS_FINAL_I) + "\n";
			mDisplayString += "Date Code: " + GetHDTDownloadTime(TracabilityStruct.ID.INFO_DATE_PASS_HDT_I) + "\n";*/
			if(!" ".equals(getTpStatus()))
				mDisplayString += "TP状态:" + getTpStatus() + "\n";
			
			mState = END;
			
			break;
		case INIT+1:
			break;
		case END:
			Exit();
			disableWifiAndBluetooth();
			break;
		default:
			break;
		}

		mLayout = new TestLayout1(mContext, mName, mDisplayString, "Fail", "Pass",
				new View.OnClickListener() {
					public void onClick(View v) {
						Result = FAILED;
						Exit();
						disableWifiAndBluetooth();
						
					}
				},
				new View.OnClickListener() {
					public void onClick(View v) {
						if(END == mState)
							Result = PASSED;
						Run();
					}
				}
		);
		
		mContext.setContentView(mLayout.ll);
	}
	
	/* for auto test handle */
	private void Run2(){
		switch(mState){
		case END:
			Exit();
			disableWifiAndBluetooth();
			break;
		default:
			mDisplayString = "";
			String BT_address=getLocalBluetoothMacAddress(); 
			String WIFI_address=getLocalWifiMacAddress();
			if (SystemProperties.get("ro.mtk_gemini_support").equals("1") == true)
			 {
				 mDisplayString += "IMEI1:" +new TelephonyManager(mContext).getDeviceId(GEMINI_SIM_1)+ "\n";
				 mDisplayString += "IMEI2:" +new TelephonyManager(mContext).getDeviceId(GEMINI_SIM_2)+ "\n";
			 }
			 else
			 {
				 mDisplayString += "IMEI:" + new TelephonyManager(mContext).getDeviceId() + "\n";
			 }
			mDisplayString += "BT:" + BT_address + "\n";
			mDisplayString += "WIFI:" + WIFI_address + "\n";
			mDisplayString += "PT:"
				 + Byte.toString(mTStruct.getItem(TracabilityStruct.ID.INFO_STATUS_PARA_SYS_I)[0]) /*print dec char*/
				 + "\n";
			mDisplayString += "PFT:"
				 + Byte.toString(mTStruct.getItem(TracabilityStruct.ID.INFO_STATUS_PARA_SYS_2_I)[0]) /*print dec char*/
				 + "\n";
			mDisplayString += "BW:"
				 + Byte.toString(mTStruct.getItem(TracabilityStruct.ID.INFO_STATUS_BW_I)[0]) /*print dec char*/
				 + "\n";
			mDisplayString += "MMI:"
				 + Byte.toString(mTStruct.getItem(TracabilityStruct.ID.INFO_STATUS_MMI_TEST_I)[0]) /*print dec char*/
				 + "\n";
			mDisplayString += "FT:"
				 + Byte.toString(mTStruct.getItem(TracabilityStruct.ID.INFO_STATUS_FINAL_I)[0]) /*print dec char*/
				 + "\n";

//			mDisplayString += "FT2:"
//				 + Byte.toString(mTStruct.getItem(TracabilityStruct.ID.INFO_STATUS_FINAL_2_I)[0]) /*print dec char*/
//				 + "-"
//				 + Integer.toHexString(new Byte(mTStruct.getItem(TracabilityStruct.ID.INFO_NBRE_PASS_FINAL_2_I)[0]).intValue()) /*print hex char*/
//				 + "-"
//				 + GetStringFromTrace(TracabilityStruct.ID.INFO_PROD_BAIE_FINAL_2_I) /*print ascii string*/
//				 + "\n";
			mState = END;
			break;
		}
		
		mLayout = new TestLayout1(mContext, mName, mDisplayString, "Fail", "Pass",
				new View.OnClickListener() {
					public void onClick(View v) {
						Result = FAILED;
						Exit();
						disableWifiAndBluetooth();
					}
				},
				new View.OnClickListener() {
					public void onClick(View v) {
						if(END == mState)
							Result = PASSED;
						Run2();
					}
				}
		);
		
		mContext.setContentView(mLayout.ll);
	}
	
	private String GetStringFromTrace(TracabilityStruct.ID id){
		String strReturn = "";
		byte[] resArr = mTStruct.getItem(id);
		for(int i=0; i<resArr.length; i++) {
			strReturn += String.format("%02X", resArr[i]);
		}
		if(strReturn.length() < 1)
			strReturn = "NA";
		return strReturn;
	}
	
	private String GetASCStringFromTrace(TracabilityStruct.ID id){
		byte[] resArr = mTStruct.getItem(id);
		String strReturn = new String(resArr);
		if(strReturn.length() < 1)
			strReturn = "NA";
		//Modified by changmei.chen@tcl.com 2013-01-21 (for the CU-REF display is abnormal) begin
		if (TracabilityStruct.ID.INFO_COMM_REF_I == id) {
			String sCommetialRef=new String(resArr);
            int i=0;
            while(resArr[i]>0 && i<resArr.length){
                i++;
                if (i== resArr.length){
                    break;
                  }

            }
            strReturn = sCommetialRef.substring(0, i);
		}
		//Modified by changmei.chen@tcl.com 2013-01-21 (for the CU-REF display is abnormal) end
		return strReturn;
	}
	
	private String GetHDTDownloadTime(TracabilityStruct.ID id) {
		String dateTable = "123456789ABCDEFGHIJKLMNOPQRSTUV";
		String monTable = "EFGHIJKLMNOP";
		String yearTable = "KLMNOPQRSTUVWXYZ";

		byte[] resArr = mTStruct.getItem(id);
		//byte[] resArr = {'1','E','K'};
		
		int day = dateTable.indexOf(resArr[0]);
		int mon = monTable.indexOf(resArr[1]);
		int year = yearTable.indexOf(resArr[2]);
		
		if (-1 == day)
			return "date error";
		if (-1 == mon)
			return "month error";
		if (-1 == year)
			return "year error";

		day += 1;
		mon += 1;
		year += 2001;
		
		return String.format("%d%02d%02d", year, mon, day);
	}
	
	public String getLocalWifiMacAddress() { 
		         WifiManager wifi = (WifiManager)mContext.getSystemService(Context.WIFI_SERVICE); 
		         WifiInfo info = wifi.getConnectionInfo(); 
		         return info.getMacAddress(); 
		     } 
	public String getLocalBluetoothMacAddress()
	{
		BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		return bluetoothAdapter.getAddress();
	}
	public void disableWifiAndBluetooth()
	{
		/*WifiManager wifi = (WifiManager)mContext.getSystemService(Context.WIFI_SERVICE);
		wifi.setWifiEnabled(false);
		BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		bluetoothAdapter.disable();*/
	}
	
	public int getGSensorTestResult()
	{
		try
		{
Log.d("bll","getGSensorTestResult");
		 IBinder binder=ServiceManager.getService("NvRAMAgent");
         NvRAMAgent agent = NvRAMAgent.Stub.asInterface (binder);
         byte[] buff = agent.readFile((int)72);//12
         for (int i = 0; i < buff.length; i++) { 
		     String hex = Integer.toHexString(buff[i] & 0xFF); 
		     if (hex.length() == 1) { 
		       hex = '0' + hex; 
		     } 
		     
		    if(GSensorItem.equals(hex.toUpperCase()))
		     {
		    	 String result = Integer.toHexString(buff[i+4] & 0xFF); 
		    	 if (result.length() == 1) { 
		    		 	result = '0' + result; 
				     } 
		    	 if(result.equals("01"))
		    		 return 1;
		    	 else if(result.equals("02"))
		    	 {
		    		 return 2;
		    	 }
		    	 else
		    		 return 0;
		     }
		     Log.d("bll", hex.toUpperCase());
		   } 
		}catch(Exception e)
		{
			
		}
         return 0;
	}
	
	public int getGSensorTestResultFromMsic()
	{
		try{
			File file = new File("/dev/misc");
			FileInputStream dis=new FileInputStream(file);
			dis.skip(FACTORY_REPORT_SAVE_OFFSET);
			byte []buff = new byte[128*8];
			dis.read(buff);
			dis.close();
		    for (int i = 0; i < buff.length; i++) { 
				     String hex = Integer.toHexString(buff[i] & 0xFF); 
				     if (hex.length() == 1) { 
				       hex = '0' + hex; 
				     } 				     
				    if(GSensorItem.equals(hex.toUpperCase()))
				     {
				    	 String result = Integer.toHexString(buff[i+4] & 0xFF); 
				    	 if (result.length() == 1) { 
				    		 	result = '0' + result; 
						     } 
				    	 if(result.equals("01"))
				    		 return 1;
				    	 else if(result.equals("02"))
				    	 {
				    		 return 2;
				    	 }
				    	 else
				    		 return 0;
				     }
				    Log.d("bll", "getGSensorTestResultFromMsic: "+hex.toUpperCase());
			}
	  }catch(Exception e){  
          e.printStackTrace();
          Log.d("bll", "exception: "+e.toString());
      } 
         return 0;
	}
	
	public int getGSensorTestFileResult()
	{

		try{
			File file = new File("/data/nvram/APCFG/APRDEB/Factory_test_result");
			FileInputStream dis=new FileInputStream(file);
			byte []buff = new byte[128*8];
			dis.read(buff);
			dis.close();
		    for (int i = 0; i < buff.length; i++) { 
				     String hex = Integer.toHexString(buff[i] & 0xFF); 
				     if (hex.length() == 1) { 
				       hex = '0' + hex; 
				     } 				     
				    if(GSensorItem.equals(hex.toUpperCase()))
				     {
				    	 String result = Integer.toHexString(buff[i+4] & 0xFF); 
				    	 if (result.length() == 1) { 
				    		 	result = '0' + result; 
						     } 
				    	 if(result.equals("01"))
				    		 return 1;
				    	 else if(result.equals("02"))
				    	 {
				    		 return 2;
				    	 }
				    	 else
				    		 return 0;
				     }
				    Log.d("bll", "getGSensorTestFileResult: "+hex.toUpperCase());
			}
	  }catch(Exception e){  
          e.printStackTrace();
          Log.d("bll", "exception: "+e.toString());
      } 
         return 0;
	}
	
	public String getTpStatus()
	{
		try
    	{
        BufferedReader reader = new BufferedReader(new FileReader("/sys/tp/fw_status"), 256);
        try {
        	String state = reader.readLine();
        	Log.d("bll", "bll====>tp state: "+state);
            return state;
        } finally {
            reader.close();
        } 
    	}catch(Exception e)
    	{
    		
    	}
    	return " ";
	}
}
