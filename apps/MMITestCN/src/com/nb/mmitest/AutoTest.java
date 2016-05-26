/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.nb.mmitest;

import static android.provider.Settings.System.*;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.AbstractQueue;
import java.util.ListIterator;
import java.util.Locale;

import java.io.File;
import java.util.regex.*;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnKeyListener;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.Settings;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.ListView;

import android.view.View;
//import android.view.KeyEvent;
//import android.view.MotionEvent;
import android.view.KeyEvent;
import android.view.Window;
import android.view.WindowManager;

//import com.android.qualcomm.qcnvitems.IQcNvItems;
//import com.android.qualcomm.qcnvitems.QcNvItemTypes;
//import com.android.qualcomm.qcnvitems.QcNvItemsWXKJ;
import com.nb.mmitest.BuildConfig;
import com.nb.mmitest.R;



import android.nfc.NfcAdapter;
import android.os.SystemProperties;
/**
 * A list view example where the data for the list comes from an array of
 * strings.
 */
public class AutoTest extends Activity {

	public static final String TAG = "AUTOTEST";

	public static final boolean DEBUG = true;

	int MINI_AUTOTEST_MAX_ITEMS = 31;

	// the global test status 1bit stands for each tests
	// this should be synchrinized with
	private int mStatusBits = 0;
	private int mSelectBits = 0;
	private int mStatusBitsExt = 0;
	private int mSelectBitsExt = 0;

	private static final byte STATUS_NOT_TESTED = (byte) 0xFF;
	private static final byte STATUS_FAILED = 0;
	private static final byte STATUS_PASSED = 1;

	static public byte mINFO_STATUS_MMI_TEST = STATUS_NOT_TESTED;

	private TracabilityStruct mTracabilityStruct;

	private byte[] mMmitestInfo = new byte[4];

	private Test mCurrentTest = null;

	private ArrayList<Test> mTestList = new ArrayList<Test>();
	private ListIterator<Test> mTestListIt;
	private TelephonyManager mTelManager;
	private MyPhoneStateListener myPhoneStateListen;
	/* this array should be exactly 32 in size */
	// the order of the test here is the same as their status bits and selection
	// bits
	// allocated in tracability
	// in mStatusBits

	private final static Test[] AutoTestsListMmitest = {
			// Maximum name length = MAX_TEST_ITEM_NAME_LEN*ENCODING_LENGTH
			new TracaDisplayTest(Test.ID.TRACABILITY, "TRACABILITY"),
			new LcdMireRgbTest(Test.ID.LCD_MIRERGB, "LCD MIRE", 1),
			new LcdColorTest(Test.ID.LCD_BLACK, "LCD BLACK", Color.BLACK, 1),
			new LcdCheckerTest(Test.ID.LCD_CHECKER, "LCD CHECKER", 1),
			new LcdGreyChartTest(Test.ID.LCD_GREYCHART, "LCD GREYCHART", 1),
			new LcdColorTest(Test.ID.LCD_WHITE, "LCD WHITE", Color.WHITE, 1),
			new ImageTest(Test.ID.LCD_MENU, "LCD MENU", R.drawable.menu, 1),
			new ImageTest(Test.ID.LCD_MACBETH, "LCD MACBETH",
					R.drawable.macbeth, 1),
			new KeypadTest(Test.ID.KEYPAD, "KEYPAD", 10),
			new CameraTest(Test.ID.CAMERA_IMG, "CAMERA IMG", 5),
			new CameraTest(Test.ID.CAMERA_IMG_FRONT, "CAMERA IMG FRONT", 5, 1),
			new LightTest(Test.ID.BACKLIGHT, "BACKLIGHTS", 2),
			new LightTest(Test.ID.KBD_BACKLIGHT, "KBD BACKLIGHT", 2),
			new VibratorTest(Test.ID.VIB, "VIB", 2),
			// new SlideTest(Test.ID.SLIDE ,"SLIDE" ,2),
			new SIMTest(Test.ID.SIM, "SIM"),
			new ChargerInTest(Test.ID.CHARGER_PRES, "CHARGER PRES"),
			new ChargerOutTest(Test.ID.CHARGER_MISS, "CHARGER MISS"),

			// new HeadInTest(Test.ID.HEADSET_IN,"HEADSET IN"),
			// new HeadLeftTest(Test.ID.HEADSET_LEFT,"HEADSET LEFT"),
			// new HeadRightTest(Test.ID.HEADSET_RIGHT,"HEADSET RIGHT"),
			// new HeadOutTest(Test.ID.HEADSET_OUT,"HEADSET OUT"),
			new HeadsetTest(Test.ID.HEADSET, "ACCESSORY"),
			new MelodyTest(Test.ID.MELODY, "AUDIO", 5), // includes loop test
			new MicTest(Test.ID.MIC, "MIC"),
			
			new NFCactive(Test.ID.NFC_ACTIVE,"NFC ACTIVE"),
     			new NFCpassive(Test.ID.NFC_PASSIVE,"NFC Passive"),

			new BluetoothTest(Test.ID.BT, "BT"),/* modify by stephen.huang */
			new WIFITest(Test.ID.WIFI, "WIFI", 5),/* add by stephen.huang */
			new MemorycardTest(Test.ID.MEMORYCARD, "MEMORYCARD"),
			new CompassTest(Test.ID.COMPASS, "COMPASS", 5),
			new GPSSensorTest(Test.ID.GPS, "GPS"),
			new GSensorTest(Test.ID.GSENSOR, "GSENSOR", 5),
			new USBTest(Test.ID.USB, "USB"),
			new MiscTest(Test.ID.MISC, "MISC", 5),
			new BatteryTempTest(Test.ID.TEMPBAT, "BATTERY TEMPERATURE"), null };

	private final static Test[] AutoTestsListMmitest2 = {
			// Maximum name length = MAX_TEST_ITEM_NAME_LEN*ENCODING_LENGTH
			new TracaDisplayTest(Test.ID.TRACABILITY, "TRACABILITY"),
			new LcdMireRgbTest(Test.ID.LCD_MIRERGB, "LCD MIRE", 1),
			new LcdColorTest(Test.ID.LCD_BLACK, "LCD BLACK", Color.BLACK, 1),
			// new LcdCheckerTest(Test.ID.LCD_CHECKER ,"LCD CHECKER", 1),
			new LcdGreyChartTest(Test.ID.LCD_GREYCHART, "LCD GREYCHART", 1),
			new LcdColorTest(Test.ID.LCD_WHITE, "LCD WHITE", Color.WHITE, 1),
			// new ImageTest(Test.ID.LCD_MENU ,"LCD MENU" , R.drawable.menu , 1),
			// new ImageTest(Test.ID.LCD_MACBETH ,"LCD MACBETH", R.drawable.macbeth, 1) ,
			new KeypadTest(Test.ID.KEYPAD, "KEYPAD", 1),
			new LightTest(Test.ID.BACKLIGHT, "BACKLIGHTS", 1),
			// new LightTest(Test.ID.KBD_BACKLIGHT, "KBD BACKLIGHT", 1),
			new VibratorTest(Test.ID.VIB, "VIB", 2),
			new CameraTest(Test.ID.CAMERA_IMG, "CAMERA IMG", 1),
			new CameraTest(Test.ID.CAMERA_IMG_FRONT, "CAMERA IMG FRONT", 5, 1),
			// new SlideTest(Test.ID.SLIDE ,"SLIDE" ,2),
			// new ChargerInTest(Test.ID.CHARGER_PRES ,"CHARGER PRES"),
			// new ChargerOutTest(Test.ID.CHARGER_MISS ,"CHARGER MISS"),
			// new HeadInTest(Test.ID.HEADSET_IN,"HEADSET IN"),
			// new HeadLeftTest(Test.ID.HEADSET_LEFT,"HEADSET LEFT"),
			// new HeadRightTest(Test.ID.HEADSET_RIGHT,"HEADSET RIGHT"),
			// new HeadOutTest(Test.ID.HEADSET_OUT,"HEADSET OUT"),
			// new MicTest(Test.ID.MIC, "MIC"),
			new MelodyTest(Test.ID.MELODY, "AUDIO", 1), // includes loop test
			new HeadsetTest(Test.ID.HEADSET, "ACCESSORY"),
			new USBTest(Test.ID.USB, "USB"),
			new GSensorTest(Test.ID.GSENSOR, "GSENSOR", 1),
			new CompassTest(Test.ID.COMPASS, "COMPASS", 1),
			new AlsPsTest(Test.ID.ALSPS, "ALS/PS"),
			new SIMTest(Test.ID.SIM, "SIM"),
			new MemorycardTest(Test.ID.MEMORYCARD, "MEMORYCARD"),
			new BatteryTempTest(Test.ID.TEMPBAT, "BATTERY TEMPERATURE"),
			new BluetoothTest(Test.ID.BT, "BT"),/* modify by stephen.huang */
			new WIFITest(Test.ID.WIFI, "WIFI", 1),/* add by stephen.huang */
			new GPSSensorTest(Test.ID.GPS, "GPS"),
			// new MiscTest(Test.ID.MISC, "MISC", 1),
			null };

	private final static Test[] AutoTestsListMmitest2S = {
			// Maximum name length = MAX_TEST_ITEM_NAME_LEN*ENCODING_LENGTH
			new TracaDisplayTest(Test.ID.TRACABILITY, "TRACABILITY"),
			new LcdMireRgbTest(Test.ID.LCD_MIRERGB, "LCD MIRE", 1),
			new LcdColorTest(Test.ID.LCD_BLACK, "LCD BLACK", Color.BLACK, 1),
			// new LcdCheckerTest(Test.ID.LCD_CHECKER ,"LCD CHECKER", 1),
			new LcdGreyChartTest(Test.ID.LCD_GREYCHART, "LCD GREYCHART", 1),
			new LcdColorTest(Test.ID.LCD_WHITE, "LCD WHITE", Color.WHITE, 1),
			// new ImageTest(Test.ID.LCD_MENU ,"LCD MENU" , R.drawable.menu , 1),
			// new ImageTest(Test.ID.LCD_MACBETH ,"LCD MACBETH", R.drawable.macbeth, 1) ,
			new KeypadTest(Test.ID.KEYPAD, "KEYPAD", 1),
			new LightTest(Test.ID.BACKLIGHT, "BACKLIGHTS", 1),
			// new LightTest(Test.ID.KBD_BACKLIGHT, "KBD BACKLIGHT", 1),
			new FlashLEDTest(Test.ID.CAMERA_LED, "CAMERA LED"),
			new VibratorTest(Test.ID.VIB, "VIB", 2),
			new CameraTest(Test.ID.CAMERA_IMG, "CAMERA IMG", 1),
			new CameraTest(Test.ID.CAMERA_IMG_FRONT, "CAMERA IMG FRONT", 5, 1),
			// new SlideTest(Test.ID.SLIDE ,"SLIDE" ,2),
			// new ChargerInTest(Test.ID.CHARGER_PRES ,"CHARGER PRES"),
			// new ChargerOutTest(Test.ID.CHARGER_MISS ,"CHARGER MISS"),
			// new HeadInTest(Test.ID.HEADSET_IN,"HEADSET IN"),
			// new HeadLeftTest(Test.ID.HEADSET_LEFT,"HEADSET LEFT"),
			// new HeadRightTest(Test.ID.HEADSET_RIGHT,"HEADSET RIGHT"),
			// new HeadOutTest(Test.ID.HEADSET_OUT,"HEADSET OUT"),
			// new MicTest(Test.ID.MIC, "MIC"),
			new MelodyTest(Test.ID.MELODY, "AUDIO", 1), // includes loop test
			new HeadsetTest(Test.ID.HEADSET, "ACCESSORY"),
			new USBTest(Test.ID.USB, "USB"),
			new GSensorTest(Test.ID.GSENSOR, "GSENSOR", 1),
			new CompassTest(Test.ID.COMPASS, "COMPASS", 1),
			new AlsPsTest(Test.ID.ALSPS, "ALS/PS"),
			new SIMTest(Test.ID.SIM, "SIM"),
			new MemorycardTest(Test.ID.MEMORYCARD, "MEMORYCARD"),
			new BatteryTempTest(Test.ID.TEMPBAT, "BATTERY TEMPERATURE"),
			new BluetoothTest(Test.ID.BT, "BT"),/* modify by stephen.huang */
			new WIFITest(Test.ID.WIFI, "WIFI", 1),/* add by stephen.huang */
			new GPSSensorTest(Test.ID.GPS, "GPS"),
			// new MiscTest(Test.ID.MISC, "MISC", 1),
			null };

			private final static Test[] AutoTestsListMmitest2_Nfc = {
			// Maximum name length = MAX_TEST_ITEM_NAME_LEN*ENCODING_LENGTH
			new TracaDisplayTest(Test.ID.TRACABILITY, "TRACABILITY"),
			new LcdMireRgbTest(Test.ID.LCD_MIRERGB, "LCD MIRE", 1),
			new LcdColorTest(Test.ID.LCD_BLACK, "LCD BLACK", Color.BLACK, 1),
			// new LcdCheckerTest(Test.ID.LCD_CHECKER ,"LCD CHECKER", 1),
			new LcdGreyChartTest(Test.ID.LCD_GREYCHART, "LCD GREYCHART", 1),
			new LcdColorTest(Test.ID.LCD_WHITE, "LCD WHITE", Color.WHITE, 1),
			// new ImageTest(Test.ID.LCD_MENU ,"LCD MENU" , R.drawable.menu , 1)
			// ,
			// new ImageTest(Test.ID.LCD_MACBETH ,"LCD MACBETH",
			// R.drawable.macbeth, 1) ,
			new KeypadTest(Test.ID.KEYPAD, "KEYPAD", 1),
			new LightTest(Test.ID.BACKLIGHT, "BACKLIGHTS", 1),
			// new LightTest(Test.ID.KBD_BACKLIGHT, "KBD BACKLIGHT", 1),
			new VibratorTest(Test.ID.VIB, "VIB", 2),
			new CameraTest(Test.ID.CAMERA_IMG, "CAMERA IMG", 1),
			new CameraTest(Test.ID.CAMERA_IMG_FRONT, "CAMERA IMG FRONT", 5, 1),
			// new SlideTest(Test.ID.SLIDE ,"SLIDE" ,2),
			// new ChargerInTest(Test.ID.CHARGER_PRES ,"CHARGER PRES"),
			// new ChargerOutTest(Test.ID.CHARGER_MISS ,"CHARGER MISS"),
			// new HeadInTest(Test.ID.HEADSET_IN,"HEADSET IN"),
			// new HeadLeftTest(Test.ID.HEADSET_LEFT,"HEADSET LEFT"),
			// new HeadRightTest(Test.ID.HEADSET_RIGHT,"HEADSET RIGHT"),
			// new HeadOutTest(Test.ID.HEADSET_OUT,"HEADSET OUT"),
			// new MicTest(Test.ID.MIC, "MIC"),
			new MelodyTest(Test.ID.MELODY, "AUDIO", 1), // includes loop test
			new HeadsetTest(Test.ID.HEADSET, "ACCESSORY"),
			new USBTest(Test.ID.USB, "USB"),
			new GSensorTest(Test.ID.GSENSOR, "GSENSOR", 1),
			new CompassTest(Test.ID.COMPASS, "COMPASS", 1),
			new AlsPsTest(Test.ID.ALSPS, "ALS/PS"),
			new SIMTest(Test.ID.SIM, "SIM"),
			new MemorycardTest(Test.ID.MEMORYCARD, "MEMORYCARD"),
			new BatteryTempTest(Test.ID.TEMPBAT, "BATTERY TEMPERATURE"),
			new BluetoothTest(Test.ID.BT, "BT"),/* modify by stephen.huang */
			new WIFITest(Test.ID.WIFI, "WIFI", 1),/* add by stephen.huang */
			new GPSSensorTest(Test.ID.GPS, "GPS"),
			new NFCactive(Test.ID.NFC_ACTIVE,"NFC ACTIVE"),
			new NFCpassive(Test.ID.NFC_PASSIVE,"NFC Passive"),
			// new MiscTest(Test.ID.MISC, "MISC", 1),
			null };
	private final static Test[] AutoTestsListMmitest2S_Nfc = {
			// Maximum name length = MAX_TEST_ITEM_NAME_LEN*ENCODING_LENGTH
			new TracaDisplayTest(Test.ID.TRACABILITY, "TRACABILITY"),
			new LcdMireRgbTest(Test.ID.LCD_MIRERGB, "LCD MIRE", 1),
			new LcdColorTest(Test.ID.LCD_BLACK, "LCD BLACK", Color.BLACK, 1),
			// new LcdCheckerTest(Test.ID.LCD_CHECKER ,"LCD CHECKER", 1),
			new LcdGreyChartTest(Test.ID.LCD_GREYCHART, "LCD GREYCHART", 1),
			new LcdColorTest(Test.ID.LCD_WHITE, "LCD WHITE", Color.WHITE, 1),
			// new ImageTest(Test.ID.LCD_MENU ,"LCD MENU" , R.drawable.menu , 1)
			// ,
			// new ImageTest(Test.ID.LCD_MACBETH ,"LCD MACBETH",
			// R.drawable.macbeth, 1) ,
			new KeypadTest(Test.ID.KEYPAD, "KEYPAD", 1),
			new LightTest(Test.ID.BACKLIGHT, "BACKLIGHTS", 1),
			// new LightTest(Test.ID.KBD_BACKLIGHT, "KBD BACKLIGHT", 1),
			new FlashLEDTest(Test.ID.CAMERA_LED, "CAMERA LED"),
			new VibratorTest(Test.ID.VIB, "VIB", 2),
			new CameraTest(Test.ID.CAMERA_IMG, "CAMERA IMG", 1),
			new CameraTest(Test.ID.CAMERA_IMG_FRONT, "CAMERA IMG FRONT", 5, 1),
			// new SlideTest(Test.ID.SLIDE ,"SLIDE" ,2),
			// new ChargerInTest(Test.ID.CHARGER_PRES ,"CHARGER PRES"),
			// new ChargerOutTest(Test.ID.CHARGER_MISS ,"CHARGER MISS"),
			// new HeadInTest(Test.ID.HEADSET_IN,"HEADSET IN"),
			// new HeadLeftTest(Test.ID.HEADSET_LEFT,"HEADSET LEFT"),
			// new HeadRightTest(Test.ID.HEADSET_RIGHT,"HEADSET RIGHT"),
			// new HeadOutTest(Test.ID.HEADSET_OUT,"HEADSET OUT"),
			// new MicTest(Test.ID.MIC, "MIC"),
			new MelodyTest(Test.ID.MELODY, "AUDIO", 1), // includes loop test
			new HeadsetTest(Test.ID.HEADSET, "ACCESSORY"),
			new USBTest(Test.ID.USB, "USB"),
			new GSensorTest(Test.ID.GSENSOR, "GSENSOR", 1),
			new CompassTest(Test.ID.COMPASS, "COMPASS", 1),
			new AlsPsTest(Test.ID.ALSPS, "ALS/PS"),
			new SIMTest(Test.ID.SIM, "SIM"),
			new MemorycardTest(Test.ID.MEMORYCARD, "MEMORYCARD"),
			new BatteryTempTest(Test.ID.TEMPBAT, "BATTERY TEMPERATURE"),
			new BluetoothTest(Test.ID.BT, "BT"),/* modify by stephen.huang */
			new WIFITest(Test.ID.WIFI, "WIFI", 1),/* add by stephen.huang */
			new GPSSensorTest(Test.ID.GPS, "GPS"),
			new NFCactive(Test.ID.NFC_ACTIVE,"NFC ACTIVE"),
			new NFCpassive(Test.ID.NFC_PASSIVE,"NFC Passive"),
			// new MiscTest(Test.ID.MISC, "MISC", 1),
			null };
    // Modified by changmei.chen@tcl.com 2013-01-19 for MMITest spec 2.0 begin.
	private final static Test[] AutoTestsListMmitest_Scribe5HD_mini = {
	        //changed Mmitest list by xianfeng.xu for CR353014
			// Maximum name length = MAX_TEST_ITEM_NAME_LEN*ENCODING_LENGTH
			new TracaDisplayTest(Test.ID.TRACABILITY, "Traceability"),
		    new TpTest1(Test.ID.TP1, "Touch Panel 1", 1000),
		    new TpTest2(Test.ID.TP2, "Touch Panel 2", 1000),
			new LcdMireRgbTest(Test.ID.LCD_MIRERGB, "LCD MIRE", 600),
			new LcdColorTest(Test.ID.LCD_BLACK, "LCD BLACK", Color.BLACK, 600),
			
			new LcdColorTest(Test.ID.LCD_GREYCHART, "LCD GREYCHART", Color.GRAY,600),
			new LcdGreyChartTest(Test.ID.LCD_LEVEL, "LCD GRAYLEVEL", 600),
			new LcdColorTest(Test.ID.LCD_WHITE, "LCD WHITE", Color.WHITE, 600),
			
			new KeypadTest(Test.ID.KEYPAD, "Keypad"),
			new LightTest(Test.ID.BACKLIGHT, "LCD Backlight", 1500),

			new FlashLEDTest(Test.ID.CAMERA_LED, "Camera LED", 1500),
			new CameraTest(Test.ID.CAMERA_IMG, "Main Camera", 3000, 0),
			new CameraTest(Test.ID.CAMERA_IMG_FRONT, "Front Camera", 3000, 1),	
			
			//new ChargerLedTest(Test.ID.CHARGER_LED, "CHARGER LED"),	
			
			//new MelodyTest(Test.ID.MELODY, "AUDIO"), // includes loop test
			
			new ReceiverTest(Test.ID.RECEIVER, "Receiver",4000), // includes loop test
		    new SpeakerTest(Test.ID.SPEAKER, "Speaker",4000),
		    new MicMainTest(Test.ID.MICMAIN, "Mic",4000),
		 //   new MicSubTest(Test.ID.MICSUB, "Mic2",4000),
		    
			new VibratorTest(Test.ID.VIB, "Vibrator",1500),
			new HeadsetTest(Test.ID.HEADSET, "Headset & FM"),
			// new LightTest(Test.ID.KBD_BACKLIGHT, "KBD BACKLIGHT"),
			new USBTest(Test.ID.USB, "Charge & USB"),
			new GSensorTest(Test.ID.GSENSOR, "G-Sensor"),
		//	new CompassTest(Test.ID.COMPASS, "E-Compass"),  modify by liliang.bao
			
			new LightSensorTest(Test.ID.LIGHTSENSOR,"Light Sensor"),
			new AlsPsTest(Test.ID.ALSPS, "Proximity Sensor"),
			new SIMTest(Test.ID.SIM, "SIM Card"),
			new MemorycardTest(Test.ID.MEMORYCARD, "Memory Card"),
			new BatteryTempTest(Test.ID.TEMPBAT, "Battery temp"),
			new BluetoothTest(Test.ID.BT, "Bluetooth"),/* modify by stephen.huang */
			new WIFITest(Test.ID.WIFI, "WIFI"),/* add by stephen.huang */
			new GPSSensorTest(Test.ID.GPS, "GPS"),
			new EmptyTest(Test.ID.EMERGENCY_CALL, "CALL"),
			null
			};
	private final static Test[] AutoTestsListMmitest_Scribe5HD_mini_cn = {
		new TracaDisplayTest(Test.ID.TRACABILITY, "机器信息"),
	    new TpTest1(Test.ID.TP1, "触摸屏测试1", 600),
	    new TpTest2(Test.ID.TP2, "触摸屏测试2", 600),
		new LcdMireRgbTest(Test.ID.LCD_MIRERGB, "LCD 红绿蓝测试", 600),
		new LcdColorTest(Test.ID.LCD_BLACK, "LCD 黑色测试", Color.BLACK, 600),
		
		new LcdColorTest(Test.ID.LCD_GREYCHART, "LCD 灰色测试", Color.GRAY,600),
		new LcdGreyChartTest(Test.ID.LCD_LEVEL, "LCD 灰色渐变测试", 600),
		new LcdColorTest(Test.ID.LCD_WHITE, "LCD 白色测试", Color.WHITE, 600),
		//new LightTest(Test.ID.KBD_BACKLIGHT,"按键灯测试",600),
		new FlashLEDTest(Test.ID.CAMERA_LED, "摄像头闪光灯测试", 600),
		new CameraTest(Test.ID.CAMERA_IMG, "主摄像头测试", 600, 0),
		new KeypadTest(Test.ID.KEYPAD, "按键测试"),

		new LightTest(Test.ID.BACKLIGHT, "LCD 背光测试", 600),


		new CameraTest(Test.ID.CAMERA_IMG_FRONT, "前摄像头测试", 600, 1),	
		
		new ChargerLedTest(Test.ID.CHARGER_LED, "通知灯测试"),	
		
		//new MelodyTest(Test.ID.MELODY, "AUDIO"), // includes loop test
		
		new ReceiverTest(Test.ID.RECEIVER, "听筒测试",600), // includes loop test
	    new SpeakerTest(Test.ID.SPEAKER, "扬声器测试",600),
	    new MicMainTest(Test.ID.MICMAIN, "麦克风测试",600),
	 //   new MicSubTest(Test.ID.MICSUB, "Mic2",4000),
	    
		new VibratorTest(Test.ID.VIB, "振动器测试",600),
		new HeadsetTest(Test.ID.HEADSET, "有线耳机及收音机测试"),
		// new LightTest(Test.ID.KBD_BACKLIGHT, "KBD BACKLIGHT"),
		new OtgTest(Test.ID.OTG, "OTG测试"),
		new USBTest(Test.ID.USB, "充电于USB测试"),
		new GSensorTest(Test.ID.GSENSOR, "重力感应器测试"),
		new CompassTest(Test.ID.COMPASS, "指南针测试"),
		new GyroscopeSensorTest(Test.ID.GYROSCOPE, "陀螺仪测试"),
		new LightSensorTest(Test.ID.LIGHTSENSOR,"光感应"),
		new AlsPsTest(Test.ID.ALSPS, "距离感应"),
		new HallTest(Test.ID.HALL, "霍尔测试"),
		new FpTest(Test.ID.FP, "指纹测试"),		
		new SIMTest(Test.ID.SIM, "SIM卡测试"),
		new MemorycardTest(Test.ID.MEMORYCARD, "内存卡测试"),
		//new BatteryTempTest(Test.ID.TEMPBAT, "电池温度测试"),
		new BluetoothTest(Test.ID.BT, "蓝牙测试"),/* modify by stephen.huang */
		new WIFITest(Test.ID.WIFI, "WIFI测试"),/* add by stephen.huang */
		new GPSSensorTest(Test.ID.GPS, "GPS测试"),
   
		new EmptyTest(Test.ID.EMERGENCY_CALL, "呼叫"),
		};
	private final static Test[] TestsListMmitest_K453 = {
        //changed Mmitest list by xianfeng.xu for CR353014
		// Maximum name length = MAX_TEST_ITEM_NAME_LEN*ENCODING_LENGTH
		new TracaDisplayTest(Test.ID.TRACABILITY, "机器信息"),
	    new TpTest1(Test.ID.TP1, "触摸屏测试1", 600),
	    new TpTest2(Test.ID.TP2, "触摸屏测试2", 600),
		new LcdMireRgbTest(Test.ID.LCD_MIRERGB, "LCD 红绿蓝测试", 600),
		new LcdColorTest(Test.ID.LCD_BLACK, "LCD 黑色测试", Color.BLACK, 600),
		
		new LcdColorTest(Test.ID.LCD_GREYCHART, "LCD 灰色测试", Color.GRAY,600),
		new LcdGreyChartTest(Test.ID.LCD_LEVEL, "LCD 灰色渐变测试", 600),
		new LcdColorTest(Test.ID.LCD_WHITE, "LCD 白色测试", Color.WHITE, 600),
		new KeypadTest(Test.ID.KEYPAD, "按键测试"),

		new LightTest(Test.ID.BACKLIGHT, "LCD 背光测试", 600),

		new FlashLEDTest(Test.ID.CAMERA_LED, "摄像头闪光灯测试", 600),
		new CameraTest(Test.ID.CAMERA_IMG, "主摄像头测试", 600, 0),
		new CameraTest(Test.ID.CAMERA_IMG_FRONT, "前摄像头测试", 600, 1),	
		
		//new ChargerLedTest(Test.ID.CHARGER_LED, "CHARGER LED"),	
		
		//new MelodyTest(Test.ID.MELODY, "AUDIO"), // includes loop test
		
		new ReceiverTest(Test.ID.RECEIVER, "听筒测试",600), // includes loop test
	    new SpeakerTest(Test.ID.SPEAKER, "扬声器测试",600),
	    new MicMainTest(Test.ID.MICMAIN, "麦克风测试",600),
	 //   new MicSubTest(Test.ID.MICSUB, "Mic2",4000),
	    
		new VibratorTest(Test.ID.VIB, "振动器测试",600),
		new HeadsetTest(Test.ID.HEADSET, "有线耳机及收音机测试"),
		// new LightTest(Test.ID.KBD_BACKLIGHT, "KBD BACKLIGHT"),
		new USBTest(Test.ID.USB, "充电于USB测试"),
		new GSensorTest(Test.ID.GSENSOR, "重力感应器测试"),
	//	new CompassTest(Test.ID.COMPASS, "E-Compass"),  modify by liliang.bao
		
	//	new LightSensorTest(Test.ID.LIGHTSENSOR,"光感应"),
		new AlsPsTest(Test.ID.ALSPS, "距离感应"),
		new SIMTest(Test.ID.SIM, "SIM卡测试"),
		new MemorycardTest(Test.ID.MEMORYCARD, "内存卡测试"),
		//new BatteryTempTest(Test.ID.TEMPBAT, "电池温度测试"),
		new BluetoothTest(Test.ID.BT, "蓝牙测试"),/* modify by stephen.huang */
		new WIFITest(Test.ID.WIFI, "WIFI测试"),/* add by stephen.huang */
		new GPSSensorTest(Test.ID.GPS, "GPS测试"),
   
		new EmptyTest(Test.ID.EMERGENCY_CALL, "呼叫"),
		null
		};
	
	private final static Test[] AutoTestsListMmitest_K453 = {
        //changed Mmitest list by xianfeng.xu for CR353014
		// Maximum name length = MAX_TEST_ITEM_NAME_LEN*ENCODING_LENGTH
		new TracaDisplayTest(Test.ID.TRACABILITY, "机器信息"),
	    new TpTest1(Test.ID.TP1, "触摸屏测试1", 600),
	    new TpTest2(Test.ID.TP2, "触摸屏测试2", 600),
		new LcdMireRgbTest(Test.ID.LCD_MIRERGB, "LCD 红绿蓝测试", 600),
		new LcdColorTest(Test.ID.LCD_BLACK, "LCD 黑色测试", Color.BLACK, 600),
		
		new LcdColorTest(Test.ID.LCD_GREYCHART, "LCD 灰色测试", Color.GRAY,600),
		new LcdGreyChartTest(Test.ID.LCD_LEVEL, "LCD 灰色渐变测试", 600),
		new LcdColorTest(Test.ID.LCD_WHITE, "LCD 白色测试", Color.WHITE, 600),
		new KeypadTest(Test.ID.KEYPAD, "按键测试"),

		new LightTest(Test.ID.BACKLIGHT, "LCD 背光测试", 600),

		new FlashLEDTest(Test.ID.CAMERA_LED, "摄像头闪光灯测试", 600),
		new CameraTest(Test.ID.CAMERA_IMG, "主摄像头测试", 600, 0),
		new CameraTest(Test.ID.CAMERA_IMG_FRONT, "前摄像头测试", 600, 1),	
		
		//new ChargerLedTest(Test.ID.CHARGER_LED, "CHARGER LED"),	
		
		//new MelodyTest(Test.ID.MELODY, "AUDIO"), // includes loop test
		
		new ReceiverTest(Test.ID.RECEIVER, "听筒测试",600), // includes loop test
	    new SpeakerTest(Test.ID.SPEAKER, "扬声器测试",600),
	    new MicMainTest(Test.ID.MICMAIN, "麦克风测试",600),
	 //   new MicSubTest(Test.ID.MICSUB, "Mic2",4000),
	    
		new VibratorTest(Test.ID.VIB, "振动器测试",600),
		new HeadsetTest(Test.ID.HEADSET, "有线耳机及收音机测试"),
		// new LightTest(Test.ID.KBD_BACKLIGHT, "KBD BACKLIGHT"),
		new USBTest(Test.ID.USB, "充电于USB测试"),
		new GSensorTest(Test.ID.GSENSOR, "感应器测试"),
	//	new CompassTest(Test.ID.COMPASS, "E-Compass"),  modify by liliang.bao
		
	//	new LightSensorTest(Test.ID.LIGHTSENSOR,"光感应"),
		new AlsPsTest(Test.ID.ALSPS, "距离感应"),
		new SIMTest(Test.ID.SIM, "SIM卡测试"),
		new MemorycardTest(Test.ID.MEMORYCARD, "内存卡测试"),
	//	new BatteryTempTest(Test.ID.TEMPBAT, "电池温度测试"),
		new BluetoothTest(Test.ID.BT, "蓝牙测试"),/* modify by stephen.huang */
		new WIFITest(Test.ID.WIFI, "WIFI测试"),/* add by stephen.huang */
		new GPSSensorTest(Test.ID.GPS, "GPS测试"),
		new EmptyTest(Test.ID.EMERGENCY_CALL, "CALL"),
		null
		};
	
	private final static Test[] AutoTestsListMmitest_K551S = {
		new TracaDisplayTest(Test.ID.TRACABILITY, "机器信息"),
	    new TpTest1(Test.ID.TP1, "触摸屏测试1", 600),
	    new TpTest2(Test.ID.TP2, "触摸屏测试2", 600),
		new LcdMireRgbTest(Test.ID.LCD_MIRERGB, "LCD 红绿蓝测试", 600),
		new LcdColorTest(Test.ID.LCD_BLACK, "LCD 黑色测试", Color.BLACK, 600),
		
		new LcdColorTest(Test.ID.LCD_GREYCHART, "LCD 灰色测试", Color.GRAY,600),
		new LcdGreyChartTest(Test.ID.LCD_LEVEL, "LCD 灰色渐变测试", 600),
		new LcdColorTest(Test.ID.LCD_WHITE, "LCD 白色测试", Color.WHITE, 600),
		new LightTest(Test.ID.KBD_BACKLIGHT,"按键灯测试",600),
		new KeypadTest(Test.ID.KEYPAD, "按键测试"),

		new LightTest(Test.ID.BACKLIGHT, "LCD 背光测试", 600),

		new FlashLEDTest(Test.ID.CAMERA_LED, "摄像头闪光灯测试", 600),
		new CameraTest(Test.ID.CAMERA_IMG, "主摄像头测试", 600, 0),
		new CameraTest(Test.ID.CAMERA_IMG_FRONT, "前摄像头测试", 600, 1),	
		
		
		new ReceiverTest(Test.ID.RECEIVER, "听筒测试",600), // includes loop test
	    new SpeakerTest(Test.ID.SPEAKER, "扬声器测试",600),
	    new MicMainTest(Test.ID.MICMAIN, "麦克风测试",600),
	 //   new MicSubTest(Test.ID.MICSUB, "Mic2",4000),
	    
		new VibratorTest(Test.ID.VIB, "振动器测试",600),
		new HeadsetTest(Test.ID.HEADSET, "有线耳机及收音机测试"),
		// new LightTest(Test.ID.KBD_BACKLIGHT, "KBD BACKLIGHT"),
		new USBTest(Test.ID.USB, "充电于USB测试"),
		new GSensorTest(Test.ID.GSENSOR, "重力感应器测试"),
	//	new CompassTest(Test.ID.COMPASS, "E-Compass"),  modify by liliang.bao
		
		new LightSensorTest(Test.ID.LIGHTSENSOR,"光感应"),
		new AlsPsTest(Test.ID.ALSPS, "距离感应"),
		new HallTest(Test.ID.HALL, "霍尔测试"),
		new FpTest(Test.ID.FP, "指纹测试"),
		new SIMTest(Test.ID.SIM, "SIM卡测试"),
		new MemorycardTest(Test.ID.MEMORYCARD, "内存卡测试"),
		//new BatteryTempTest(Test.ID.TEMPBAT, "电池温度测试"),
		new BluetoothTest(Test.ID.BT, "蓝牙测试"),/* modify by stephen.huang */
		new WIFITest(Test.ID.WIFI, "WIFI测试"),/* add by stephen.huang */
		new GPSSensorTest(Test.ID.GPS, "GPS测试"),
   
		new EmptyTest(Test.ID.EMERGENCY_CALL, "呼叫"),
		};
	private final static Test[] AutoTestsListMmitest_K506Q = {
		new TracaDisplayTest(Test.ID.TRACABILITY, "机器信息"),
	    new TpTest1(Test.ID.TP1, "触摸屏测试1", 600),
	    new TpTest2(Test.ID.TP2, "触摸屏测试2", 600),
		new LcdMireRgbTest(Test.ID.LCD_MIRERGB, "LCD 红绿蓝测试", 600),
		new LcdColorTest(Test.ID.LCD_BLACK, "LCD 黑色测试", Color.BLACK, 600),
		
		new LcdColorTest(Test.ID.LCD_GREYCHART, "LCD 灰色测试", Color.GRAY,600),
		new LcdGreyChartTest(Test.ID.LCD_LEVEL, "LCD 灰色渐变测试", 600),
		new LcdColorTest(Test.ID.LCD_WHITE, "LCD 白色测试", Color.WHITE, 600),

		new KeypadTest(Test.ID.KEYPAD, "按键测试"),

		new LightTest(Test.ID.BACKLIGHT, "LCD 背光测试", 600),

		new FlashLEDTest(Test.ID.CAMERA_LED, "摄像头闪光灯测试", 600),
		new CameraTest(Test.ID.CAMERA_IMG, "主摄像头测试", 600, 0),
		new CameraTest(Test.ID.CAMERA_IMG_FRONT, "前摄像头测试", 600, 1),	
		
		
		new ReceiverTest(Test.ID.RECEIVER, "听筒测试",600), // includes loop test
	    new SpeakerTest(Test.ID.SPEAKER, "扬声器测试",600),
	    new MicMainTest(Test.ID.MICMAIN, "麦克风测试",600),
	 //   new MicSubTest(Test.ID.MICSUB, "Mic2",4000),
	    
		new VibratorTest(Test.ID.VIB, "振动器测试",600),
		new HeadsetTest(Test.ID.HEADSET, "有线耳机及收音机测试"),
		// new LightTest(Test.ID.KBD_BACKLIGHT, "KBD BACKLIGHT"),
		new OtgTest(Test.ID.OTG, "OTG测试"),
		new USBTest(Test.ID.USB, "充电于USB测试"),
		new GSensorTest(Test.ID.GSENSOR, "重力感应器测试"),
	//	new CompassTest(Test.ID.COMPASS, "E-Compass"),  modify by liliang.bao
		
		new LightSensorTest(Test.ID.LIGHTSENSOR,"光感应"),
		new AlsPsTest(Test.ID.ALSPS, "距离感应"),
		new FpTest(Test.ID.FP, "指纹测试"),
		new SIMTest(Test.ID.SIM, "SIM卡测试"),
		new MemorycardTest(Test.ID.MEMORYCARD, "内存卡测试"),
		//new BatteryTempTest(Test.ID.TEMPBAT, "电池温度测试"),
		new BluetoothTest(Test.ID.BT, "蓝牙测试"),/* modify by stephen.huang */
		new WIFITest(Test.ID.WIFI, "WIFI测试"),/* add by stephen.huang */
		new GPSSensorTest(Test.ID.GPS, "GPS测试"),
   
		new EmptyTest(Test.ID.EMERGENCY_CALL, "呼叫"),
		};
	private final static Test[] AutoTestsListMmitest_K01TS = {
		new TracaDisplayTest(Test.ID.TRACABILITY, "机器信息"),
	    new TpTest1(Test.ID.TP1, "触摸屏测试1", 600),
	    new TpTest2(Test.ID.TP2, "触摸屏测试2", 600),
		new LcdMireRgbTest(Test.ID.LCD_MIRERGB, "LCD 红绿蓝测试", 600),
		new LcdColorTest(Test.ID.LCD_BLACK, "LCD 黑色测试", Color.BLACK, 600),
		
		new LcdColorTest(Test.ID.LCD_GREYCHART, "LCD 灰色测试", Color.GRAY,600),
		new LcdGreyChartTest(Test.ID.LCD_LEVEL, "LCD 灰色渐变测试", 600),
		new LcdColorTest(Test.ID.LCD_WHITE, "LCD 白色测试", Color.WHITE, 600),
		new LightTest(Test.ID.KBD_BACKLIGHT,"按键灯测试",600),
		new KeypadTest(Test.ID.KEYPAD, "按键测试"),

		new LightTest(Test.ID.BACKLIGHT, "LCD 背光测试", 600),

		new FlashLEDTest(Test.ID.CAMERA_LED, "摄像头闪光灯测试", 600),
		new CameraTest(Test.ID.CAMERA_IMG, "主摄像头测试", 600, 0),
		new CameraTest(Test.ID.CAMERA_IMG_FRONT, "前摄像头测试", 600, 1),	
		
		
		new ReceiverTest(Test.ID.RECEIVER, "听筒测试",600), // includes loop test
	    new SpeakerTest(Test.ID.SPEAKER, "扬声器测试",600),
	    new MicMainTest(Test.ID.MICMAIN, "麦克风测试",600),
	 //   new MicSubTest(Test.ID.MICSUB, "Mic2",4000),
	    
		new VibratorTest(Test.ID.VIB, "振动器测试",600),
		new HeadsetTest(Test.ID.HEADSET, "有线耳机及收音机测试"),
		// new LightTest(Test.ID.KBD_BACKLIGHT, "KBD BACKLIGHT"),
		new USBTest(Test.ID.USB, "充电于USB测试"),
		new GSensorTest(Test.ID.GSENSOR, "重力感应器测试"),
	//	new CompassTest(Test.ID.COMPASS, "E-Compass"),  modify by liliang.bao
		
		new LightSensorTest(Test.ID.LIGHTSENSOR,"光感应"),
		new AlsPsTest(Test.ID.ALSPS, "距离感应"),
		new HallTest(Test.ID.HALL, "霍尔测试"),

		new SIMTest(Test.ID.SIM, "SIM卡测试"),
		new MemorycardTest(Test.ID.MEMORYCARD, "内存卡测试"),
		//new BatteryTempTest(Test.ID.TEMPBAT, "电池温度测试"),
		new BluetoothTest(Test.ID.BT, "蓝牙测试"),/* modify by stephen.huang */
		new WIFITest(Test.ID.WIFI, "WIFI测试"),/* add by stephen.huang */
		new GPSSensorTest(Test.ID.GPS, "GPS测试"),
   
		new EmptyTest(Test.ID.EMERGENCY_CALL, "呼叫"),
		};
	
	private final static Test[] AutoTestsListMmitest_K506TE_A = {
			new TracaDisplayTest(Test.ID.TRACABILITY, "机器信息"),
		    new TpTest1(Test.ID.TP1, "触摸屏测试1", 600),
		    new TpTest2(Test.ID.TP2, "触摸屏测试2", 600),
			new LcdMireRgbTest(Test.ID.LCD_MIRERGB, "LCD 红绿蓝测试", 600),
			new LcdColorTest(Test.ID.LCD_BLACK, "LCD 黑色测试", Color.BLACK, 600),
			
			new LcdColorTest(Test.ID.LCD_GREYCHART, "LCD 灰色测试", Color.GRAY,600),
			new LcdGreyChartTest(Test.ID.LCD_LEVEL, "LCD 灰色渐变测试", 600),
			new LcdColorTest(Test.ID.LCD_WHITE, "LCD 白色测试", Color.WHITE, 600),

			new KeypadTest(Test.ID.KEYPAD, "按键测试"),

			new LightTest(Test.ID.BACKLIGHT, "LCD 背光测试", 600),

			new FlashLEDTest(Test.ID.CAMERA_LED, "摄像头闪光灯测试", 600),
			new CameraTest(Test.ID.CAMERA_IMG, "主摄像头测试", 600, 0),
			new CameraTest(Test.ID.CAMERA_IMG_FRONT, "前摄像头测试", 600, 1),	
			
			
			new ReceiverTest(Test.ID.RECEIVER, "听筒测试",600), // includes loop test
		    new SpeakerTest(Test.ID.SPEAKER, "扬声器测试",600),
		    new MicMainTest(Test.ID.MICMAIN, "麦克风测试",600),
		 //   new MicSubTest(Test.ID.MICSUB, "Mic2",4000),
		    
			new VibratorTest(Test.ID.VIB, "振动器测试",600),
			new HeadsetTest(Test.ID.HEADSET, "有线耳机及收音机测试"),
			// new LightTest(Test.ID.KBD_BACKLIGHT, "KBD BACKLIGHT"),
			new OtgTest(Test.ID.OTG, "OTG测试"),
			new USBTest(Test.ID.USB, "充电于USB测试"),
			new GSensorTest(Test.ID.GSENSOR, "重力感应器测试"),
		//	new CompassTest(Test.ID.COMPASS, "E-Compass"),  modify by liliang.bao
			
			new LightSensorTest(Test.ID.LIGHTSENSOR,"光感应"),
			new AlsPsTest(Test.ID.ALSPS, "距离感应"),
			new HallTest(Test.ID.HALL, "霍尔测试"),
			new FpTest(Test.ID.FP, "指纹测试"),
			new SIMTest(Test.ID.SIM, "SIM卡测试"),
			new MemorycardTest(Test.ID.MEMORYCARD, "内存卡测试"),
			//new BatteryTempTest(Test.ID.TEMPBAT, "电池温度测试"),
			new BluetoothTest(Test.ID.BT, "蓝牙测试"),/* modify by stephen.huang */
			new WIFITest(Test.ID.WIFI, "WIFI测试"),/* add by stephen.huang */
			new GPSSensorTest(Test.ID.GPS, "GPS测试"),
	   
			new EmptyTest(Test.ID.EMERGENCY_CALL, "呼叫"),
			};
	private final static Test[] AutoTestsListSw = {
			new VibratorTest(Test.ID.VIB, "VIB", 2),
			new SlideTest(Test.ID.SLIDE, "SLIDE", 2),
			new KeypadTest(Test.ID.KEYPAD, "KEYPAD", 15),
			new BluetoothTest(Test.ID.BT, "BT"),
			new WIFITest(Test.ID.WIFI, "WIFI"),
			new MiscTest(Test.ID.MISC, "MISC", 5),
			new CompassTest(Test.ID.COMPASS, "COMPASS", 5),
			new GPSSensorTest(Test.ID.GPS, "GPS"),
			new MicTest(Test.ID.MIC, "MIC") };

	private static Test[] AutoTestsList;

	private TestStatus ts;
	private TracabilityStruct mAuto;
	//20121210 ying.pang for PR371543
	private int mCurrentTestId = -1;

    // Add by changmei.chen@tcl.com 2013-01-19 for the bug that if tap the pass button quickly will raise exception sometimes begin.
	private TestLayout1 tlStart = null;
	private TestLayout1 tlCont = null;
	private TestLayout1 tlEnd = null;
	// Add by changmei.chen@tcl.com 2013-01-19 for the bug that if tap the pass button quickly will raise exception sometimes end.
	
	// in case we execute autotest from app SW, test results are saved in
	// Android file system

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//20121210 ying.pang for PR371543
		Log.d(TAG,"onCreate");
		myPhoneStateListen = new MyPhoneStateListener();
		if(savedInstanceState != null){
			mCurrentTestId = savedInstanceState.getInt("CurrentId", -1);			
		}
		
		//NfcAdapter mNfcAdapter = null;
		//mNfcAdapter = NfcAdapter.getDefaultAdapter(this);

		// set the Test list according to build config
		/*if (BuildConfig.isSW) {
			if (FeatureOption.MTK_GEMINI_SUPPORT == false) {
				if (mNfcAdapter == null)
					AutoTestsList = AutoTestsListMmitest2S;
				else
					AutoTestsList = AutoTestsListMmitest2S_Nfc;
			} else {
				if (mNfcAdapter == null)
					AutoTestsList = AutoTestsListMmitest2;// AutoTestsListSw;
				else
					AutoTestsList = AutoTestsListMmitest2_Nfc; 
			}
			ts = new TestStatus();
		} else {
			if (FeatureOption.MTK_GEMINI_SUPPORT == false) {
				if (mNfcAdapter == null)
					AutoTestsList = AutoTestsListMmitest2S;
				else
					AutoTestsList = AutoTestsListMmitest2S_Nfc;
			} else {
				if (mNfcAdapter == null)
					AutoTestsList = AutoTestsListMmitest2;
				else
					AutoTestsList = AutoTestsListMmitest2_Nfc;
			}
			ts = new TestStatus();
		}*/

		ts = new TestStatus();
		 Locale locale = Locale.getDefault();
		 if (locale.equals(Locale.CHINA))
		 {
			 AutoTestsList = AutoTestsListMmitest_Scribe5HD_mini_cn; 
		 }else if (BuildConfig.getMmiTest()) {
			AutoTestsList = AutoTestsListMmitest_Scribe5HD_mini;
		} 
		 
		 if("1".equals(SystemProperties.get("ro.mmi.type", "0")))
				 AutoTestsList = AutoTestsListMmitest_K453;
		 else if("2".equals(SystemProperties.get("ro.mmi.type", "0")))
		 {
			 AutoTestsList = AutoTestsListMmitest_K551S;
		 }else if("3".equals(SystemProperties.get("ro.mmi.type", "0")))
		 {
			 AutoTestsList = AutoTestsListMmitest_K01TS;			 
		 }else if("4".equals(SystemProperties.get("ro.mmi.type", "0")))
		 {
			 AutoTestsList = AutoTestsListMmitest_K506Q;
		 }else if("5".equals(SystemProperties.get("ro.mmi.type", "0")))
		 {
			 AutoTestsList = AutoTestsListMmitest_K506TE_A;
		 }else
               AutoTestsList = AutoTestsListMmitest_Scribe5HD_mini_cn; 
		Intent mIntent =getIntent();
		if(mIntent!=null){
		    String mShortCode = mIntent.getStringExtra("ShortCode");
		  //  AutoTestsList = BuildConfig.getList(AutoTestsList, mShortCode);
		}
		//add by xianfeng.xu for CR364979 end
		// set screen appearance
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		int mask = WindowManager.LayoutParams.FLAG_FULLSCREEN;
		mask |= WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
		getWindow().setFlags(mask, mask);

		// PHONE SETTINGS for MMI test
		// this is not necessary in case of factory test mode
/*		try {
			Settings.System
					.putInt(getContentResolver(), SCREEN_OFF_TIMEOUT, -1 
																		 * always
																		 * on
																		 );
		} catch (NumberFormatException e) {
			Log.e(TAG, "could not change screen timeout setting", e);
		}*/

		try {
			mTracabilityStruct = new TracabilityStruct();
		} catch (Exception e) {
			Log.e(TAG, e.toString());
		}

		// Log.d(TAG,mTracabilityStruct.toString());

		getTestsResultsFromNV();
		//getMmitestStatusFromNv();
		// these ones are not currently defines in tracability
		Log.d(TAG, "read status:"+ts.getMmitestStatus());
		mSelectBits = ts.getMmitestStatus();
		if(mSelectBits ==-1)
			mSelectBits=0;
		Log.d(TAG, "read Ext status:"+ts.getExtMmitestStatus());
		mSelectBitsExt = ts.getExtMmitestStatus();
		if(mSelectBitsExt ==-1)
			mSelectBitsExt=0;

		StartTesting();

	}
	@Override
	public void onPause() {
	//	SystemProperties.set("sys.config.mmitest", "0");
		super.onPause();

	}
	@Override
	public void onDestroy()
	{
		SystemProperties.set("sys.config.mmitest", "0");
		super.onDestroy();
	}
	//20121210 ying.pang for PR371543
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		// TODO Auto-generated method stub
		super.onSaveInstanceState(outState);		
		if(mCurrentTest != null){
			int currentId = mCurrentTest.mPosition;			
			outState.putInt("CurrentId", currentId);
			Log.d(TAG, "CURRENTiD " + currentId);
		}		
	}
	/*
	 * onActivityResult
	 * 
	 * get the result of the last ExecuteTest application and Sequences the Test
	 * the resultCode is set in Test class according to the function called to
	 * exit/stop the test
	 * 
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onActivityResult(int, int,
	 * android.content.Intent)
	 */

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {

		ExecuteTest.unlock();

		if (requestCode == 0) {
			// on test has ended : update the test results;

			/* save current status in nvram */
			//20121210 ying.pang for PR371543 begin
			if(mCurrentTestId != -1 && mCurrentTest == null){
			   if(mTestListIt != null ){
					int nextId = -1;
					Test tempTest = null;
					while(mTestListIt.hasNext()) {
						nextId = mTestListIt.nextIndex();
						mCurrentTest = mTestListIt.next();						
						if(nextId == mCurrentTestId){
							break;
						}
					}					
				}
				mCurrentTestId = -1;
			}
			
			 if(mCurrentTest.getId()==Test.ID.EMERGENCY_CALL.ordinal())
				{
					  mTelManager.listen(myPhoneStateListen,
							    PhoneStateListener.LISTEN_NONE);
					  Log.d("bll","list none");
				}
			//20121210 ying.pang for PR371543 end
			if (resultCode == RESULT_OK) {
				// the Test ended up normally, check the result
				// update the current global status
				if (mCurrentTest.getResult() == Test.PASSED) {
					if(mCurrentTest.mPosition < MINI_AUTOTEST_MAX_ITEMS)
					{
					   mStatusBits |= (int) (1 << mCurrentTest.mPosition);
					   mSelectBits |= (int) (1 << mCurrentTest.mPosition);
					}else
					{
						mStatusBitsExt |= (int) (1 << (mCurrentTest.mPosition-MINI_AUTOTEST_MAX_ITEMS));
						mSelectBitsExt |= (int) (1 << (mCurrentTest.mPosition-MINI_AUTOTEST_MAX_ITEMS));		
					}
				} else if (mCurrentTest.getResult() == Test.FAILED) {
					if(mCurrentTest.mPosition < MINI_AUTOTEST_MAX_ITEMS)
					{
					   mStatusBits &= (int) ~(1 << mCurrentTest.mPosition);
					   mSelectBits |= (int) (1 << mCurrentTest.mPosition);
					}else
					{
						mStatusBitsExt &= (int) ~(1 << (mCurrentTest.mPosition-MINI_AUTOTEST_MAX_ITEMS));
						mSelectBitsExt |= (int) (1 << (mCurrentTest.mPosition-MINI_AUTOTEST_MAX_ITEMS));		
					}
				} else {
					Log.d(TAG, "test " + mCurrentTest.toString()
							+ "had an unexpected result: force to failed\n");
					if(mCurrentTest.mPosition < MINI_AUTOTEST_MAX_ITEMS)
					{
					   mStatusBits &= (int) ~(1 << mCurrentTest.mPosition);
					   mSelectBits |= (int) (1 << mCurrentTest.mPosition);
					}else
					{
						mStatusBitsExt &= (int) ~(1 << (mCurrentTest.mPosition-MINI_AUTOTEST_MAX_ITEMS));
						mSelectBitsExt |= (int) (1 << (mCurrentTest.mPosition-MINI_AUTOTEST_MAX_ITEMS));					
					}
					
					
				}

				writeTestsResultsToNV();
				writeMmitestStatusToNv();
				// prepare the new test
				// if(mCurrentTest.mPosition < AutoTestsList.length-1){
				if (mTestListIt.hasNext()) {

					if (mCurrentTest.getResult() == Test.FAILED) {
						// if the current test failed we have to restart from
						// beginning
						AutoTest.mINFO_STATUS_MMI_TEST = STATUS_FAILED;
						// sync with NV
						writeMmitestStatusToNv();

						// StartTesting();
						ContTesting();
					} else {
						// last test was OK, go to the next test!
						RunNextTest();
					}
				} else {
					// this is the end of Auto tests
					if ((mCurrentTest.getResult() == Test.PASSED)
							&& (getLastFailed() == null)) {
						AutoTest.mINFO_STATUS_MMI_TEST = STATUS_PASSED;
						writeMmitestStatusToNv();
					} else {/* last test failed of unknown status */
						AutoTest.mINFO_STATUS_MMI_TEST = STATUS_FAILED;
						writeMmitestStatusToNv();
					}
					EndTesting();
				}

			}else if(mCurrentTest.getId()==Test.ID.EMERGENCY_CALL.ordinal())
			{
				  mTelManager.listen(myPhoneStateListen,
						    PhoneStateListener.LISTEN_NONE);
				  Log.d("bll","list none2");
			}
			else if (resultCode == RESULT_CANCELED) {
				finish();
			} else {
				finish();
			}
		}
	}

	// if this function is not declared then the application is restarted on
	// Configuration
	// change and thus loses the current status!!
	public void onConfigurationChanged(Configuration newConfig) {
		// TODO Auto-generated method stub
		super.onConfigurationChanged(newConfig);
	}

	/*
	 * StartTesting(void)
	 */

	private void StartTesting() {
		// Start the tests from the beginning
		// mAutoTestsListIndex = 0;
		// reads the last test global status in nvram

		// clear and re-build the test List with all the selected and non-null
		// test
		mTestList.clear();

		for (int i = 0; i < MINI_AUTOTEST_MAX_ITEMS&&i<AutoTestsList.length; i++) {
			//modify by liliang.bao  for restart fails items   begin
			if (AutoTestsList[i] != null&&((mStatusBits >>i &1)==0x0)) {
		    //modify by liliang.bao end
				AutoTestsList[i].mPosition = i;
				mTestList.add(AutoTestsList[i]);
			}
		}
		
		for (int i = MINI_AUTOTEST_MAX_ITEMS; i < AutoTestsList.length; i++) {
			//modify by liliang.bao  for restart fails items   begin
			if (AutoTestsList[i] != null&&((mStatusBitsExt >>(i-MINI_AUTOTEST_MAX_ITEMS) &1)==0x0)) {
		    //modify by liliang.bao end
				AutoTestsList[i].mPosition = i;
				mTestList.add(AutoTestsList[i]);
			}
		}

		mTestListIt = mTestList.listIterator();

		if (mINFO_STATUS_MMI_TEST != STATUS_NOT_TESTED) {
			// if MMI test was already executed, then display the last result
			// and let the user choose to restart or not

			View.OnClickListener leftButton = new View.OnClickListener() {
				public void onClick(View v) {
				    if (tlStart != null)
					    tlStart.setEnabledButtons(false); // Add by changmei.chen@tcl.com 2013-01-19 for the bug that if tap the pass button quickly will raise exception sometimes.
					reStart();
				}
			};

			View.OnClickListener RightButton = new View.OnClickListener() {
				public void onClick(View v) {
					finish();// do nothing and go back to main screen
				}
			};
			//Begin Add by jiqian.shi@tcl for shutdown 20130121
			View.OnClickListener ShutDownButton = new View.OnClickListener() {
				public void onClick(View v) {
				Intent intent = new Intent(Intent.ACTION_REQUEST_SHUTDOWN);
				intent.putExtra(Intent.EXTRA_KEY_CONFIRM, false); 
				intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				startActivity(intent);
				}
			};
			//End Add by jiqian.shi@tcl for shutdown 20130121
			String Display;

			if (mINFO_STATUS_MMI_TEST == STATUS_PASSED) {
				Display = this.getResources().getString(R.string.test_ok);
			} else {
				if (getLastFailed() != null)
					Display = this.getResources().getString(R.string.test)+"[" + getLastFailed().mPosition + "] "
							+ getLastFailed() + this.getResources().getString(R.string.auto_test_restart);
				else
					Display = this.getResources().getString(R.string.test_done);

			}
			//Begin Modify by jiqian.shi@tcl for shutdown 20130121
			tlStart = new TestLayout1(this, this.getResources().getString(R.string.auto), Display, this.getResources().getString(R.string.yes), this.getResources().getString(R.string.no), this.getResources().getString(R.string.shut),
					leftButton, RightButton,ShutDownButton);
			//End Modify by jiqian.shi@tcl for shutdown 20130121
			/*
           		// Add by changmei.chen@tcl.com 2013-01-19 for the bug that if tap the pass button quickly will raise exception sometimes.
			tlStart = new TestLayout1(this, TAG, Display, "YES", "NO",
					leftButton, RightButton);
			*/
			setContentView(tlStart.ll);

		} else {
			// fresh start :go directly to tests
			reStart();
		}
		//

	}

	private void ContTesting() {
		View.OnClickListener leftButton = new View.OnClickListener() {
			public void onClick(View v) {
			    if (tlCont != null)
				    tlCont.setEnabledButtons(false);// Add by changmei.chen@tcl.com 2013-01-19 for the bug that if tap the pass button quickly will raise exception sometimes.

				mTestList.clear();

				for (int i = 0; i < MINI_AUTOTEST_MAX_ITEMS&&i<AutoTestsList.length; i++) {
					//modify by liliang.bao  for restart fails items   begin
					if (AutoTestsList[i] != null
							&& (mStatusBits >>i &1)==0x0) {
					//modify by liliang.bao  for restart fails items   end
						AutoTestsList[i].mPosition = i;
						mTestList.add(AutoTestsList[i]);
					}
				}
				
				for (int i = MINI_AUTOTEST_MAX_ITEMS; i < AutoTestsList.length; i++) {
					//modify by liliang.bao  for restart fails items   begin
					if (AutoTestsList[i] != null
							&& (mStatusBitsExt >>(i-MINI_AUTOTEST_MAX_ITEMS) &1)==0x0) {
					//modify by liliang.bao  for restart fails items   end
						AutoTestsList[i].mPosition = i;
						mTestList.add(AutoTestsList[i]);
					}
				}

				mTestListIt = mTestList.listIterator();

				reStart();
			}
		};

		View.OnClickListener MiddleButton = new View.OnClickListener() {
			public void onClick(View v) {
			    if (tlCont != null)
					tlCont.setEnabledButtons(false);// Add by changmei.chen@tcl.com 2013-01-19 for the bug that if tap the pass button quickly will raise exception sometimes.
				RunNextTest();// do nothing and go back to main screen
			}
		};

		View.OnClickListener RightButton = new View.OnClickListener() {
			public void onClick(View v) {
				finish();// do nothing and go back to main screen
			}
		};

		String Display;
		Display = this.getResources().getString(R.string.test)+"[" + mCurrentTest.mPosition + "] " + mCurrentTest;
		Display = Display + this.getResources().getString(R.string.auto_test_restart);
        // Add by changmei.chen@tcl.com 2013-01-19 for the bug that if tap the pass button quickly will raise exception sometimes.
		tlCont = new TestLayout1(this, this.getResources().getString(R.string.auto), Display, this.getResources().getString(R.string.yes), this.getResources().getString(R.string.no),
				this.getResources().getString(R.string.next), leftButton, RightButton, MiddleButton);
		setContentView(tlCont.ll);
	}

	private void EndTesting() {
		View.OnClickListener leftButton = new View.OnClickListener() {
			public void onClick(View v) {
			    if (tlEnd != null)
					tlEnd.setEnabledButtons(false);// Add by changmei.chen@tcl.com 2013-01-19 for the bug that if tap the pass button quickly will raise exception sometimes.

				mTestList.clear();

				for (int i = 0; i < MINI_AUTOTEST_MAX_ITEMS&&i<AutoTestsList.length; i++) {
					//modify by liliang.bao  for restart fails items   begin
					if (AutoTestsList[i] != null
							&& ((mStatusBits >>i &1)==0x0)) {
					//modify by liliang.bao  for restart fails items   end
						AutoTestsList[i].mPosition = i;
						mTestList.add(AutoTestsList[i]);
					}
				}
				
				for (int i = MINI_AUTOTEST_MAX_ITEMS; i < AutoTestsList.length; i++) {
					//modify by liliang.bao  for restart fails items   begin
					if (AutoTestsList[i] != null
							&& ((mStatusBitsExt >>(i-MINI_AUTOTEST_MAX_ITEMS) &1)==0x0)) {
					//modify by liliang.bao  for restart fails items   end
						AutoTestsList[i].mPosition = i;
						mTestList.add(AutoTestsList[i]);
					}
				}

				mTestListIt = mTestList.listIterator();

				reStart();
			}
		};

		View.OnClickListener RightButton = new View.OnClickListener() {
			public void onClick(View v) {
				finish();// do nothing and go back to main screen
			}
		};
		//Begin Add by jiqian.shi@tcl for shutdown 20130121
		View.OnClickListener ShutDownButton = new View.OnClickListener() {
			public void onClick(View v) {
			Intent intent = new Intent(Intent.ACTION_REQUEST_SHUTDOWN);
			intent.putExtra(Intent.EXTRA_KEY_CONFIRM, false); 
			intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			startActivity(intent);
			}
		};

		String Display;
		if (AutoTest.mINFO_STATUS_MMI_TEST == STATUS_FAILED) {
			Display = "";
			for (int i = 0; i<AutoTestsList.length&&i<MINI_AUTOTEST_MAX_ITEMS&&AutoTestsList[i] != null; i++) {
				if ((mStatusBits >> i & 0x1) == 0x0) {
					Display = Display + this.getResources().getString(R.string.test)+"[" + AutoTestsList[i].mPosition
							+ "] " + AutoTestsList[i] + "\n";
				}
			}
			
			for (int i = MINI_AUTOTEST_MAX_ITEMS; i<AutoTestsList.length&&AutoTestsList[i] != null&&Display.isEmpty(); i++) {
				if ((mStatusBitsExt >> (i-MINI_AUTOTEST_MAX_ITEMS) & 0x1) == 0x0) {
					Display = Display + this.getResources().getString(R.string.test)+"[" + AutoTestsList[i].mPosition
							+ "] " + AutoTestsList[i] + "\n";
				}
			}
			
			Display = Display + this.getResources().getString(R.string.auto_test_restart);
		} else {
			Display = this.getResources().getString(R.string.test_ok);
		}
		//Begin Modify by jiqian.shi@tcl for shutdown 20130121
		tlEnd = new TestLayout1(this, this.getResources().getString(R.string.auto), Display, this.getResources().getString(R.string.yes), this.getResources().getString(R.string.no), this.getResources().getString(R.string.shut),
				leftButton, RightButton,ShutDownButton);
		//End Modify by jiqian.shi@tcl for shutdown 20130121
		/*
       	// Add by changmei.chen@tcl.com 2013-01-19 for the bug that if tap the pass button quickly will raise exception sometimes.
		tlEnd= new TestLayout1(this, TAG, Display, "YES", "NO",
				leftButton, RightButton);
		*/
		setContentView(tlEnd.ll);
	}

	private void reStart() {
		// when we restart testing we first have to clean the status bits
		//modify by liliang.bao  for restart fails items   begin
	//	mStatusBits = 0;
		//modify by liliang.bao  for restart fails items   end
		if(!mTestListIt.hasNext())
		{
			mStatusBits=0;
			mStatusBitsExt =0;
			for (int i = 0; i < MINI_AUTOTEST_MAX_ITEMS&&i<AutoTestsList.length; i++) {
				if (AutoTestsList[i] != null) {		
					mTestList.add(AutoTestsList[i]);
				}
			}
			
			for (int i = MINI_AUTOTEST_MAX_ITEMS; i < AutoTestsList.length; i++) {
				if (AutoTestsList[i] != null) {		
					mTestList.add(AutoTestsList[i]);
				}
			}
			Log.d(TAG, "all test ok ,restart");
			mTestListIt = mTestList.listIterator();
		}
		writeTestsResultsToNV();
		writeMmitestStatusToNv();
		RunNextTest();
	}

	private void RunNextTest() {

		mCurrentTest = mTestListIt.next();

		if (mCurrentTest == null) {
			throw new RuntimeException("Test "
					+ Integer.toString(mTestListIt.previousIndex()) + " empty");
		}
	 if (mCurrentTest.getId() == Test.ID.EMERGENCY_CALL.ordinal()) {
		    ExecuteTest.currentTest = mCurrentTest;
			Intent intent = new Intent(Intent.ACTION_CALL_EMERGENCY);
			intent.setData(android.net.Uri.fromParts("tel", "112", null));
			startActivity(intent);
			
			Log.d(TAG, "start test " + mCurrentTest.toString());
			ExecuteTest.currentTest = mCurrentTest;

			//Intent Execution = new Intent(Intent.ACTION_MAIN, null);
			// Execution.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			//Execution.setClassName(this, "com.nb.mmitest.ExecuteTest");

			// TestParcel tp = new TestParcel(mCurrentTest);
			// Execution.putExtra("CurrentTest",mCurrentTest);

			//ExecuteTest.lock();

			//startActivityForResult(Execution, 0);
			// 对电话的来电状态进行监听
			  mTelManager = (TelephonyManager) this
			    .getSystemService(Context.TELEPHONY_SERVICE);
			  // 注册一个监听器对电话状态进行监听
			  mTelManager.listen(myPhoneStateListen,
			    PhoneStateListener.LISTEN_CALL_STATE);
			  Log.d("bll","call 112");

	 }else if (mCurrentTest.getId() < Test.ID.MAX_ITEMS.ordinal()) {
			Log.d(TAG, "start test " + mCurrentTest.toString());
			ExecuteTest.currentTest = mCurrentTest;

			Intent Execution = new Intent(Intent.ACTION_MAIN, null);
			// Execution.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			Execution.setClassName(this, "com.nb.mmitest.ExecuteTest");

			// TestParcel tp = new TestParcel(mCurrentTest);
			// Execution.putExtra("CurrentTest",mCurrentTest);

			ExecuteTest.lock();

			startActivityForResult(Execution, 0);

		} else {
			throw new RuntimeException("TEST "
					+ Integer.toString(mTestListIt.previousIndex())
					+ " ID > MAX_ITEMS");
		}
	}

	/*
	 * getLastFailed()
	 * 
	 * get the first non-null failed test in AutoTestsList, using the status
	 * bits read from NV_MMITEST_INFO_I
	 */

	private Test getLastFailed() {

		for (int i = 0; ((i < MINI_AUTOTEST_MAX_ITEMS) && (i < AutoTestsList.length)); i++) {
			if ((mStatusBits >> i & 0x1) == 0x0 && AutoTestsList[i] != null) {
				return AutoTestsList[i];
			}
		}
		
		for (int i = MINI_AUTOTEST_MAX_ITEMS;  i < AutoTestsList.length; i++) {
			if ((mStatusBitsExt >> (i-MINI_AUTOTEST_MAX_ITEMS) & 0x1) == 0x0 && AutoTestsList[i] != null) {
				return AutoTestsList[i];
			}
		}
		return null;// no test failed
	}

	private void Failed() {
		mStatusBits &= (int) ~(1 << mTestListIt.previousIndex());
		mCurrentTest.Result = Test.FAILED;
		AutoTest.mINFO_STATUS_MMI_TEST = STATUS_FAILED;
		StartTesting();// restart

	}

	private void Passed() {
		mStatusBits |= (int) (1 << mTestListIt.previousIndex());
		mCurrentTest.Result = Test.PASSED;
		// mAutoTestsListIndex++;
		RunNextTest();
	}

	private void getTestsResultsFromNV() {

		if (true || BuildConfig.isSW) {
			mStatusBits = ts.getMmitestInfo();
			mStatusBitsExt = ts.getExtMmitestInfo();
		} else {
			mStatusBits = mTracabilityStruct.getMmitestInfo();
		}
		Log.d(TAG, "READ:StatusBits=0x" + Integer.toHexString(mStatusBits));
		Log.d(TAG, "READ:StatusBitsExt=0x" + Integer.toHexString(mStatusBitsExt));

	}

	private void writeTestsResultsToNV() {

		if (true || BuildConfig.isSW) {
			ts.setMmitestInfo(mStatusBits);
			ts.setExtMmitestInfo(mStatusBitsExt);
		} else {
			mTracabilityStruct.setMmitestInfo(mStatusBits);
		}
		Log.d(TAG, "WRITE: StatusBits=0x" + Integer.toHexString(mStatusBits)
				+ " mMmitestInfo.data=" + Arrays.toString(mMmitestInfo) + ")");
		Log.d(TAG, "WRITE: StatusBitsExt=0x" + Integer.toHexString(mStatusBitsExt)
				+ " mMmitestInfo.data=" + Arrays.toString(mMmitestInfo) + ")");
	}

	private void writeMmitestStatusToNv() {

		ts.setMmitestStatus(mSelectBits);
		ts.setExtMmitestStatus(mSelectBitsExt);
		
		Log.d(TAG, "WRITE: mSelectBits=0x" + Integer.toHexString(mSelectBits)
				+ " mMmitestInfo.data=" + Arrays.toString(mMmitestInfo) + ")");
		Log.d(TAG, "WRITE: mSelectBitsExt=0x" + Integer.toHexString(mSelectBitsExt)
				+ " mMmitestInfo.data=" + Arrays.toString(mMmitestInfo) + ")");

	}

	private void getMmitestStatusFromNv() {

		byte[] data = { 0 };

		
		  if ( BuildConfig.isSW ) { mINFO_STATUS_MMI_TEST = (byte)
		  (ts.getMmitestStatus() & 0xFF); }else
		 {
			data = mTracabilityStruct
					.getItem(TracabilityStruct.ID.INFO_STATUS_MMI_TEST_I);
			mINFO_STATUS_MMI_TEST = data[0];
		}

		Log.d(TAG,
				"mINFO_STATUS_MMI_TEST=0x"
						+ Integer.toHexString(mINFO_STATUS_MMI_TEST & 0xFF));

	}

	class TestStatus {

		private File mStatusFile;


		TestStatus() {
			try {
				mStatusFile = getFileStreamPath("status.cfg");
			} catch (Exception e) {
				Log.e(TAG, "TestStatus() :" + e);
			}

			if (!mStatusFile.exists()) {

				Log.i(TAG, "create status file");
				// status file is created with default values : nmmitest not
				// tested and all tests results to false

				writeField(MMITEST_STATUS, STATUS_NOT_TESTED);
				writeField(MMITEST_INFO, 0);
				writeField(MMITEST_EXT_STATUS, STATUS_NOT_TESTED);
				writeField(MMITEST_EXT_INFO, 0);
			}
		}

		private String MMITEST_STATUS = "[STATUS]";
		private String MMITEST_INFO = "[INFO]";
		private String MMITEST_EXT_STATUS = "[EXT_STATUS]";
		private String MMITEST_EXT_INFO = "[EXT_INFO]";

		private int readField(String field) {
			HashMap<String, String> lines = new HashMap<String, String>(0);
			RandomAccessFile fa;

			try {
				fa = new RandomAccessFile(mStatusFile, "r");

				while (fa.getFilePointer() < fa.length() - 1) {
					String s = new String(fa.readLine());
					lines.put(s.split("=")[0], s.split("=")[1]);
				}

				if (lines.containsKey(field)) {
					fa.close();
					return Integer.parseInt(lines.get(field), 16);
				}

			} catch (Exception e) {
				Log.e(TAG, "readField in " + mStatusFile + "failed : " + e);
			}

			return -1;

		}

		private void writeField(String field, int value) {
			HashMap<String, String> lines = new HashMap<String, String>(0);
			int count = 0;
			/* read the entire file as a hasmap */
			RandomAccessFile fa;

			try {

				fa = new RandomAccessFile(mStatusFile, "rw");

				while (fa.getFilePointer() < fa.length() - 1) {
					String s = new String(fa.readLine());
					lines.put(s.split("=")[0], s.split("=")[1]);
				}
				/* replace the entry matched by field */
				lines.put(field, Integer.toHexString(value & 0xFFFFFFFF));
				if (DEBUG)
					Log.i(TAG,
							"writeField() hashmap values:" + lines.toString());

				fa.seek(0);
				fa.getChannel().truncate(0);

				/* output all hashmao to file */
				Iterator iterator = lines.keySet().iterator();
				while (iterator.hasNext()) {
					String key = new String((String) iterator.next());
					/* write String as ascii */
					fa.writeBytes(key + "=" + lines.get(key) + "\n");
				}

				fa.close();

			} catch (IOException e) {
				Log.e(TAG, "writeField() failed : " + e);
			}

		}

		public void setMmitestInfo(int status) {
			writeField(MMITEST_INFO, status);
		}

		public int getMmitestInfo() {
			return readField(MMITEST_INFO);
		}

		public void setMmitestStatus(int status) {
			writeField(MMITEST_STATUS, status);
		}

		public int getMmitestStatus() {
			return readField(MMITEST_STATUS);
		}
		
		public void setExtMmitestInfo(int status) {
			writeField(MMITEST_EXT_INFO, status);
		}

		public int getExtMmitestInfo() {
			return readField(MMITEST_EXT_INFO);
		}

		public void setExtMmitestStatus(int status) {
			writeField(MMITEST_EXT_STATUS, status);
		}

		public int getExtMmitestStatus() {
			return readField(MMITEST_EXT_STATUS);
		}

	}
	
    private class MyPhoneStateListener extends PhoneStateListener {
    	 private boolean flag=false;
        public void onCallStateChanged(int state, String incomingNumber) {
            switch (state) {
            case TelephonyManager.CALL_STATE_IDLE: /* 无任何状态时 */
        		{
        			Log.d("bll", "call state idle.");
        			if(!flag)
        				break;
        			    flag = false;
        			    if(ExecuteTest.getLock()==0)
        			    {
        				Intent Execution = new Intent(Intent.ACTION_MAIN, null);
        				Execution.setClassName(AutoTest.this, "com.nb.mmitest.ExecuteTest");
        				ExecuteTest.lock();		
        				startActivityForResult(Execution, 0);
        			    }
        							
        		}
        		break;
            case TelephonyManager.CALL_STATE_OFFHOOK: /* 接起电话时 */
            	Log.d("bll", "CALL_STATE_OFFHOOK.");
            	 flag=true;
 
                break;
            default:
                break;
            }
            super.onCallStateChanged(state, incomingNumber);
        }
    }

}

class ParcelableTest implements Parcelable {
	public static final Parcelable.Creator<ParcelableTest> CREATOR = new Parcelable.Creator<ParcelableTest>() {
		public ParcelableTest createFromParcel(Parcel source) {
			final ParcelableTest plt = new ParcelableTest();
			plt.str = (String) source.readValue(ParcelableTest.class
					.getClassLoader());
			return plt;
		}

		public ParcelableTest[] newArray(int size) {
			throw new UnsupportedOperationException();
		}

	};

	public String str;

	public ParcelableTest() {
	}

	public ParcelableTest(String s) {
		str = s;
	}

	public int describeContents() {
		return 0;
	}

	public void writeToParcel(Parcel dest, int ignored) {
		dest.writeValue(str);
	}

}

class TestParcel implements Parcelable {
	public static final Parcelable.Creator<TestParcel> CREATOR = new Parcelable.Creator<TestParcel>() {
		public TestParcel createFromParcel(Parcel source) {
			final TestParcel plt = new TestParcel();
			plt.test = (Test) source.readValue(TestParcel.class
					.getClassLoader());
			return plt;
		}

		public TestParcel[] newArray(int size) {
			throw new UnsupportedOperationException();
		}

	};

	public Test test;

	public TestParcel() {
	}

	public TestParcel(Test t) {
		test = t;
	}

	public int describeContents() {
		return 0;
	}

	public void writeToParcel(Parcel dest, int ignored) {
		dest.writeValue(test);
	}

}
