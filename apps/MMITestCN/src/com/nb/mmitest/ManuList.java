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

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;

import android.app.ActivityManager;
import android.app.ListActivity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.ListView;
import android.hardware.Camera;
import android.view.View;
//import android.view.KeyEvent;
//import android.view.MotionEvent;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

import com.nb.mmitest.BuildConfig;

import android.nfc.NfcAdapter;
import android.os.SystemProperties;
/**
 * A list view example where the data for the list comes from an array of
 * strings.
 */
public class ManuList extends ListActivity {

	String TAG = "ManuList";

	int MINI_AUTOTEST_MAX_ITEMS = 31;
	int a = Camera.getNumberOfCameras();
	private Test[] TestsList;
	private int mStatusBits = 0;
	private int mSelectBits = 0;
	private int mStatusBitsExt = 0;
	private int mSelectBitsExt = 0;
	private TestStatus ts;
	private TelephonyManager mTelManager;
	private MyPhoneStateListener myPhoneStateListen;
	private static final byte STATUS_NOT_TESTED = (byte) 0xFF;
	static Test[] TestsListMmitest = {
			// Maximum name length = MAX_TEST_ITEM_NAME_LEN*ENCODING_LENGTH
			new TracaDisplayTest(Test.ID.TRACABILITY, "TRACABILITY"),
			new LcdMireRgbTest(Test.ID.LCD_MIRERGB, "LCD MIRE"),
			new LcdColorTest(Test.ID.LCD_BLACK, "LCD BLACK", Color.BLACK),
			// new LcdCheckerTest(Test.ID.LCD_CHECKER,"LCD CHECKER"),
			new LcdGreyChartTest(Test.ID.LCD_GREYCHART, "LCD GREYCHART"),
			new LcdColorTest(Test.ID.LCD_WHITE, "LCD WHITE", Color.WHITE),
			// new ImageTest(Test.ID.LCD_MENU,"LCD MENU",R.drawable.menu) ,
			// new ImageTest(Test.ID.LCD_MACBETH,"LCD MACBETH",R.drawable.macbeth) ,
			new KeypadTest(Test.ID.KEYPAD, "KEYPAD"),
			new LightTest(Test.ID.BACKLIGHT, "BACKLIGHT Level"),
			// new LightTest(Test.ID.KBD_BACKLIGHT, "KBD BACKLIGHT"),
			new VibratorTest(Test.ID.VIB, "VIBRATOR"),
			new CameraTest(Test.ID.CAMERA_IMG, "CAMERA IMG", 0, 0),
			new CameraTest(Test.ID.CAMERA_IMG_FRONT, "CAMERA IMG FRONT", 0, 1),
			// new SlideTest(Test.ID.SLIDE,"SLIDE"),
			// new ChargerInTest(Test.ID.CHARGER_PRES,"CHARGER PRES"),
			// new ChargerOutTest(Test.ID.CHARGER_MISS,"CHARGER MISS"),
			// new HeadInTest(Test.ID.HEADSET_IN,"HEADSET IN"),
			// new HeadLeftTest(Test.ID.HEADSET_LEFT,"HEADSET LEFT"),
			// new HeadRightTest(Test.ID.HEADSET_RIGHT,"HEADSET RIGHT"),
			// new HeadOutTest(Test.ID.HEADSET_OUT,"HEADSET OUT"),
			// new MicTest(Test.ID.MIC, "MIC"),
			new MelodyTest(Test.ID.MELODY, "MELODY"),
			new HeadsetTest(Test.ID.HEADSET, "HEADSET"),
			new USBTest(Test.ID.USB, "Charge & USB"),
			new GSensorTest(Test.ID.GSENSOR, "GSENSOR"),
			new CompassTest(Test.ID.COMPASS, "COMPASS"),
			new AlsPsTest(Test.ID.ALSPS, "ALS/PS"),
			new SIMTest(Test.ID.SIM, "SIM"),
			new MemorycardTest(Test.ID.MEMORYCARD, "MEMORYCARD"),
			new BatteryTempTest(Test.ID.TEMPBAT, "Battery temp"),
			new BluetoothTest(Test.ID.BT, "BT"),/* modify by stephen.huang */
			new WIFITest(Test.ID.WIFI, "WIFI"),/* add by stephen.huang */
			new GPSSensorTest(Test.ID.GPS, "GPS"),
			// new MiscTest(Test.ID.MISC,"MISC"),
			// new EmptyTest(Test.ID.TS_CALIBRATION,"TS Calibration"),
			// new EmptyTest(Test.ID.NWSETTING,"nw settings"),
			// new AudioLoopTest(Test.ID.MELODY,"loop test")
			// new FMRadioTest(Test.ID.FMRADIO, "FMRadio TEST"),
			new EmptyTest(Test.ID.EMERGENCY_CALL, "CALL"), };

	static Test[] TestsListMmitestS = {
			// Maximum name length = MAX_TEST_ITEM_NAME_LEN*ENCODING_LENGTH
			new TracaDisplayTest(Test.ID.TRACABILITY, "TRACABILITY"),
			new LcdMireRgbTest(Test.ID.LCD_MIRERGB, "LCD MIRE"),
			new LcdColorTest(Test.ID.LCD_BLACK, "LCD BLACK", Color.BLACK),
			// new LcdCheckerTest(Test.ID.LCD_CHECKER,"LCD CHECKER"),
			new LcdGreyChartTest(Test.ID.LCD_GREYCHART, "LCD GREYCHART"),
			new LcdColorTest(Test.ID.LCD_WHITE, "LCD WHITE", Color.WHITE),
			// new ImageTest(Test.ID.LCD_MENU,"LCD MENU",R.drawable.menu) ,
			// new ImageTest(Test.ID.LCD_MACBETH,"LCD MACBETH",R.drawable.macbeth) ,
			new KeypadTest(Test.ID.KEYPAD, "KEYPAD"),
			new LightTest(Test.ID.BACKLIGHT, "BACKLIGHT Level"),
			// new LightTest(Test.ID.KBD_BACKLIGHT, "KBD BACKLIGHT"),
			new FlashLEDTest(Test.ID.CAMERA_LED, "CAMERA LED"),
			new VibratorTest(Test.ID.VIB, "VIBRATOR"),
			new CameraTest(Test.ID.CAMERA_IMG, "CAMERA IMG", 0, 0),
			new CameraTest(Test.ID.CAMERA_IMG_FRONT, "CAMERA IMG FRONT", 0, 1),
			// new SlideTest(Test.ID.SLIDE,"SLIDE"),
			// new ChargerInTest(Test.ID.CHARGER_PRES,"CHARGER PRES"),
			// new ChargerOutTest(Test.ID.CHARGER_MISS,"CHARGER MISS"),
			// new HeadInTest(Test.ID.HEADSET_IN,"HEADSET IN"),
			// new HeadLeftTest(Test.ID.HEADSET_LEFT,"HEADSET LEFT"),
			// new HeadRightTest(Test.ID.HEADSET_RIGHT,"HEADSET RIGHT"),
			// new HeadOutTest(Test.ID.HEADSET_OUT,"HEADSET OUT"),
			// new MicTest(Test.ID.MIC, "MIC"),
			new MelodyTest(Test.ID.MELODY, "MELODY"),
			new HeadsetTest(Test.ID.HEADSET, "HEADSET"),
			new USBTest(Test.ID.USB, "Charge & USB"),
			new GSensorTest(Test.ID.GSENSOR, "GSENSOR"),
			new CompassTest(Test.ID.COMPASS, "COMPASS"),
			new AlsPsTest(Test.ID.ALSPS, "ALS/PS"),
			new SIMTest(Test.ID.SIM, "SIM"),
			new MemorycardTest(Test.ID.MEMORYCARD, "MEMORYCARD"),
			new BatteryTempTest(Test.ID.TEMPBAT, "Battery temp"),
			new BluetoothTest(Test.ID.BT, "BT"),/* modify by stephen.huang */
			new WIFITest(Test.ID.WIFI, "WIFI"),/* add by stephen.huang */
			new GPSSensorTest(Test.ID.GPS, "GPS"),
			// new MiscTest(Test.ID.MISC,"MISC"),
			// new EmptyTest(Test.ID.TS_CALIBRATION,"TS Calibration"),
			// new EmptyTest(Test.ID.NWSETTING,"nw settings"),
			// new AudioLoopTest(Test.ID.MELODY,"loop test")
			// new FMRadioTest(Test.ID.FMRADIO, "FMRadio TEST"),
			new EmptyTest(Test.ID.EMERGENCY_CALL, "CALL"), };

			static Test[] TestsListMmitest_Nfc = {
			// Maximum name length = MAX_TEST_ITEM_NAME_LEN*ENCODING_LENGTH
			new TracaDisplayTest(Test.ID.TRACABILITY, "TRACABILITY"),
			new LcdMireRgbTest(Test.ID.LCD_MIRERGB, "LCD MIRE"),
			new LcdColorTest(Test.ID.LCD_BLACK, "LCD BLACK", Color.BLACK),
			// new LcdCheckerTest(Test.ID.LCD_CHECKER,"LCD CHECKER"),
			new LcdGreyChartTest(Test.ID.LCD_GREYCHART, "LCD GREYCHART"),
			new LcdColorTest(Test.ID.LCD_WHITE, "LCD WHITE", Color.WHITE),
			// new ImageTest(Test.ID.LCD_MENU,"LCD MENU",R.drawable.menu) ,
			// new
			// ImageTest(Test.ID.LCD_MACBETH,"LCD MACBETH",R.drawable.macbeth) ,
			new KeypadTest(Test.ID.KEYPAD, "KEYPAD"),
			new LightTest(Test.ID.BACKLIGHT, "BACKLIGHT Level"),
			// new LightTest(Test.ID.KBD_BACKLIGHT, "KBD BACKLIGHT"),
			new VibratorTest(Test.ID.VIB, "VIBRATOR"),
			new CameraTest(Test.ID.CAMERA_IMG, "CAMERA IMG", 0, 0),
			new CameraTest(Test.ID.CAMERA_IMG_FRONT, "CAMERA IMG FRONT", 0, 1),
			// new SlideTest(Test.ID.SLIDE,"SLIDE"),
			// new ChargerInTest(Test.ID.CHARGER_PRES,"CHARGER PRES"),
			// new ChargerOutTest(Test.ID.CHARGER_MISS,"CHARGER MISS"),
			// new HeadInTest(Test.ID.HEADSET_IN,"HEADSET IN"),
			// new HeadLeftTest(Test.ID.HEADSET_LEFT,"HEADSET LEFT"),
			// new HeadRightTest(Test.ID.HEADSET_RIGHT,"HEADSET RIGHT"),
			// new HeadOutTest(Test.ID.HEADSET_OUT,"HEADSET OUT"),
			// new MicTest(Test.ID.MIC, "MIC"),
			new MelodyTest(Test.ID.MELODY, "MELODY"),
			new HeadsetTest(Test.ID.HEADSET, "HEADSET"),
			new USBTest(Test.ID.USB, "Charge & USB"),
			new GSensorTest(Test.ID.GSENSOR, "GSENSOR"),
			new CompassTest(Test.ID.COMPASS, "COMPASS"),
			new AlsPsTest(Test.ID.ALSPS, "ALS/PS"),
			new SIMTest(Test.ID.SIM, "SIM"),
			new MemorycardTest(Test.ID.MEMORYCARD, "MEMORYCARD"),
			new BatteryTempTest(Test.ID.TEMPBAT, "Battery temp"),
			new BluetoothTest(Test.ID.BT, "BT"),/* modify by stephen.huang */
			new WIFITest(Test.ID.WIFI, "WIFI"),/* add by stephen.huang */
			new GPSSensorTest(Test.ID.GPS, "GPS"),
			new NFCactive(Test.ID.NFC_ACTIVE,"NFC ACTIVE"),
			new NFCpassive(Test.ID.NFC_PASSIVE,"NFC Passive"),
			// new MiscTest(Test.ID.MISC,"MISC"),
			// new EmptyTest(Test.ID.TS_CALIBRATION,"TS Calibration"),
			// new EmptyTest(Test.ID.NWSETTING,"nw settings"),
			// new AudioLoopTest(Test.ID.MELODY,"loop test")
			// new FMRadioTest(Test.ID.FMRADIO, "FMRadio TEST"),
			new EmptyTest(Test.ID.EMERGENCY_CALL, "CALL"), };
	static Test[] TestsListMmitestS_Nfc = {
			// Maximum name length = MAX_TEST_ITEM_NAME_LEN*ENCODING_LENGTH
			new TracaDisplayTest(Test.ID.TRACABILITY, "TRACABILITY"),
			new LcdMireRgbTest(Test.ID.LCD_MIRERGB, "LCD MIRE"),
			new LcdColorTest(Test.ID.LCD_BLACK, "LCD BLACK", Color.BLACK),
			// new LcdCheckerTest(Test.ID.LCD_CHECKER,"LCD CHECKER"),
			new LcdGreyChartTest(Test.ID.LCD_GREYCHART, "LCD GREYCHART"),
			new LcdColorTest(Test.ID.LCD_WHITE, "LCD WHITE", Color.WHITE),
			// new ImageTest(Test.ID.LCD_MENU,"LCD MENU",R.drawable.menu) ,
			// new
			// ImageTest(Test.ID.LCD_MACBETH,"LCD MACBETH",R.drawable.macbeth) ,
			new KeypadTest(Test.ID.KEYPAD, "KEYPAD"),
			new LightTest(Test.ID.BACKLIGHT, "BACKLIGHT Level"),
			// new LightTest(Test.ID.KBD_BACKLIGHT, "KBD BACKLIGHT"),
			new FlashLEDTest(Test.ID.CAMERA_LED, "CAMERA LED"),
			new VibratorTest(Test.ID.VIB, "VIBRATOR"),
			new CameraTest(Test.ID.CAMERA_IMG, "CAMERA IMG", 0, 0),
			new CameraTest(Test.ID.CAMERA_IMG_FRONT, "CAMERA IMG FRONT", 0, 1),
			// new SlideTest(Test.ID.SLIDE,"SLIDE"),
			// new ChargerInTest(Test.ID.CHARGER_PRES,"CHARGER PRES"),
			// new ChargerOutTest(Test.ID.CHARGER_MISS,"CHARGER MISS"),
			// new HeadInTest(Test.ID.HEADSET_IN,"HEADSET IN"),
			// new HeadLeftTest(Test.ID.HEADSET_LEFT,"HEADSET LEFT"),
			// new HeadRightTest(Test.ID.HEADSET_RIGHT,"HEADSET RIGHT"),
			// new HeadOutTest(Test.ID.HEADSET_OUT,"HEADSET OUT"),
			// new MicTest(Test.ID.MIC, "MIC"),
			new MelodyTest(Test.ID.MELODY, "MELODY"),
			new HeadsetTest(Test.ID.HEADSET, "HEADSET"),
			new USBTest(Test.ID.USB, "Charge & USB"),
			new GSensorTest(Test.ID.GSENSOR, "GSENSOR"),
			new CompassTest(Test.ID.COMPASS, "COMPASS"),
			new AlsPsTest(Test.ID.ALSPS, "ALS/PS"),
			new SIMTest(Test.ID.SIM, "SIM"),
			new MemorycardTest(Test.ID.MEMORYCARD, "MEMORYCARD"),
			new BatteryTempTest(Test.ID.TEMPBAT, "Battery temp"),
			new BluetoothTest(Test.ID.BT, "BT"),/* modify by stephen.huang */
			new WIFITest(Test.ID.WIFI, "WIFI"),/* add by stephen.huang */
			new GPSSensorTest(Test.ID.GPS, "GPS"),
			new NFCactive(Test.ID.NFC_ACTIVE,"NFC ACTIVE"),
			new NFCpassive(Test.ID.NFC_PASSIVE,"NFC Passive"),
			// new MiscTest(Test.ID.MISC,"MISC"),
			// new EmptyTest(Test.ID.TS_CALIBRATION,"TS Calibration"),
			// new EmptyTest(Test.ID.NWSETTING,"nw settings"),
			// new AudioLoopTest(Test.ID.MELODY,"loop test")
			// new FMRadioTest(Test.ID.FMRADIO, "FMRadio TEST"),
			new EmptyTest(Test.ID.EMERGENCY_CALL, "CALL"), };

	static Test[] TestsListMmitest_noSub = {
			// Maximum name length = MAX_TEST_ITEM_NAME_LEN*ENCODING_LENGTH
			new TracaDisplayTest(Test.ID.TRACABILITY, "TRACABILITY"),
			new LcdMireRgbTest(Test.ID.LCD_MIRERGB, "LCD MIRE"),
			new LcdColorTest(Test.ID.LCD_BLACK, "LCD BLACK", Color.BLACK),
			// new LcdCheckerTest(Test.ID.LCD_CHECKER,"LCD CHECKER"),
			new LcdGreyChartTest(Test.ID.LCD_GREYCHART, "LCD GREYCHART"),
			new LcdColorTest(Test.ID.LCD_WHITE, "LCD WHITE", Color.WHITE),
			// new ImageTest(Test.ID.LCD_MENU,"LCD MENU",R.drawable.menu) ,
			// new ImageTest(Test.ID.LCD_MACBETH,"LCD MACBETH",R.drawable.macbeth) ,
			new BatteryTempTest(Test.ID.TEMPBAT, "Battery temp"),
			new LightTest(Test.ID.BACKLIGHT, "BACKLIGHT Level"),
			new LightTest(Test.ID.KBD_BACKLIGHT, "KBD BACKLIGHT"),
			new VibratorTest(Test.ID.VIB, "VIBRATOR"),
			new CameraTest(Test.ID.CAMERA_IMG, "CAMERA IMG", 0, 0),
			// new CameraTest(Test.ID.CAMERA_IMG_FRONT,"CAMERA IMG FRONT",0,1),
			// new SlideTest(Test.ID.SLIDE,"SLIDE"),
			// new ChargerInTest(Test.ID.CHARGER_PRES,"CHARGER PRES"),
			// new ChargerOutTest(Test.ID.CHARGER_MISS,"CHARGER MISS"),
			// new HeadInTest(Test.ID.HEADSET_IN,"HEADSET IN"),
			// new HeadLeftTest(Test.ID.HEADSET_LEFT,"HEADSET LEFT"),
			// new HeadRightTest(Test.ID.HEADSET_RIGHT,"HEADSET RIGHT"),
			// new HeadOutTest(Test.ID.HEADSET_OUT,"HEADSET OUT"),
			new MicTest(Test.ID.MIC, "MIC"),
	
			new NFCactive(Test.ID.NFC_ACTIVE,"NFC ACTIVE"),
     			new NFCpassive(Test.ID.NFC_PASSIVE,"NFC Passive"),

			new MelodyTest(Test.ID.MELODY, "MELODY"),
			new HeadsetTest(Test.ID.HEADSET, "HEADSET"),
			new SIMTest(Test.ID.SIM, "SIM"),
			new MemorycardTest(Test.ID.MEMORYCARD, "MEMORYCARD"),
			new USBTest(Test.ID.USB, "Charge & USB"),
			new BluetoothTest(Test.ID.BT, "BT"),/* modify by stephen.huang */
			new WIFITest(Test.ID.WIFI, "WIFI"),/* add by stephen.huang */
			new GSensorTest(Test.ID.GSENSOR, "GSENSOR"),
			new CompassTest(Test.ID.COMPASS, "COMPASS"),
			new GPSSensorTest(Test.ID.GPS, "GPS"),
			new EmptyTest(Test.ID.EMERGENCY_CALL, "CALL"),
			// new MiscTest(Test.ID.MISC,"MISC"),
			// new EmptyTest(Test.ID.TS_CALIBRATION,"TS Calibration"),
			// new EmptyTest(Test.ID.NWSETTING,"nw settings"),
			// new AudioLoopTest(Test.ID.MELODY,"loop test")
			// new FMRadioTest(Test.ID.FMRADIO, "FMRadio TEST"),
			new KeypadTest(Test.ID.KEYPAD, "KEYPAD"),
			new AlsPsTest(Test.ID.ALSPS, "ALS/PS"), };

	static Test[] TestsListMmitest_Scribe5HD_mini = {
	    //changed Mmitest list by xianfeng.xu for CR353014
		// Maximum name length = MAX_TEST_ITEM_NAME_LEN*ENCODING_LENGTH
		new TracaDisplayTest(Test.ID.TRACABILITY, "Traceability"),
		new TpTest1(Test.ID.TP1, "Touch Panel 1"),
		new TpTest2(Test.ID.TP2, "Touch Panel 2"),
		new LcdMireRgbTest(Test.ID.LCD_MIRERGB, "LCD MIRE"),
		new LcdColorTest(Test.ID.LCD_BLACK, "LCD BLACK", Color.BLACK),
		// new LcdCheckerTest(Test.ID.LCD_CHECKER ,"LCD CHECKER"),
		//modify by xianfeng.xu for CR364979
		new LcdColorTest(Test.ID.LCD_GREYCHART, "LCD GREYCHART", Color.GRAY),
		new LcdGreyChartTest(Test.ID.LCD_LEVEL, "LCD GRAYLEVEL"),
		new LcdColorTest(Test.ID.LCD_WHITE, "LCD WHITE", Color.WHITE),
		// new ImageTest(Test.ID.LCD_MENU ,"LCD MENU" , R.drawable.menu),
		// new ImageTest(Test.ID.LCD_MACBETH ,"LCD MACBETH", R.drawable.macbeth) ,
		new KeypadTest(Test.ID.KEYPAD, "Keypad"),
		new LightTest(Test.ID.BACKLIGHT, "LCD Backlight"),
		
		//new ChargerLedTest(Test.ID.CHARGER_LED, "CHARGER LED"),
		new FlashLEDTest(Test.ID.CAMERA_LED, "Camera LED"),
		new CameraTest(Test.ID.CAMERA_IMG, "Main Camera", 0, 0),		
		new CameraTest(Test.ID.CAMERA_IMG_FRONT, "Front Camera", 0, 1),
		
		
		//new MelodyTest(Test.ID.MELODY, "AUDIO"), // includes loop test		
		new ReceiverTest(Test.ID.RECEIVER, "Receiver"), // includes loop test
		new SpeakerTest(Test.ID.SPEAKER, "Speaker"),
		new MicMainTest(Test.ID.MICMAIN, "Mic"),
	//	new MicSubTest(Test.ID.MICSUB, "Mic2"),
		
		new VibratorTest(Test.ID.VIB, "Vibrator"),
		new HeadsetTest(Test.ID.HEADSET, "Headset & FM"),
		// new LightTest(Test.ID.KBD_BACKLIGHT, "KBD BACKLIGHT"),
		new USBTest(Test.ID.USB, "Charge & USB"),
		new GSensorTest(Test.ID.GSENSOR, "G-Sensor"),
		//new CompassTest(Test.ID.COMPASS, "E-Compass"),  modify by liliang.bao
		
		new LightSensorTest(Test.ID.LIGHTSENSOR,"Light Sensor"),
		new AlsPsTest(Test.ID.ALSPS, "Proximity Sensor"),
		new SIMTest(Test.ID.SIM, "SIM Card"),
		new MemorycardTest(Test.ID.MEMORYCARD, "Memory Card"),
		new BatteryTempTest(Test.ID.TEMPBAT, "Battery temp"),
		new BluetoothTest(Test.ID.BT, "Bluetooth"),/* modify by stephen.huang */
		new WIFITest(Test.ID.WIFI, "WIFI"),/* add by stephen.huang */
	//	new GPSSensorTest(Test.ID.GPS, "GPS"),
                
		//new MHLTest(Test.ID.MHL, "MHL"),
                
		new EmptyTest(Test.ID.EMERGENCY_CALL, "Call"),

       	};
	private final static Test[] TestsListMmitest_Scribe5HD_mini_cn = {
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
		//new LightTest(Test.ID.KBD_BACKLIGHT,"按键灯测试",600),
		new KeypadTest(Test.ID.KEYPAD, "按键测试"),

		new LightTest(Test.ID.BACKLIGHT, "LCD 背光测试", 600),

		new FlashLEDTest(Test.ID.CAMERA_LED, "摄像头闪光灯测试", 600),
		new CameraTest(Test.ID.CAMERA_IMG, "主摄像头测试", 600, 0),
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
	//	new FpTest(Test.ID.FP, "指纹测试"),
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
		};
	
	private final static Test[] TestsListMmitest_K551S = {
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
	//	new BatteryTempTest(Test.ID.TEMPBAT, "电池温度测试"),
		new BluetoothTest(Test.ID.BT, "蓝牙测试"),/* modify by stephen.huang */
		new WIFITest(Test.ID.WIFI, "WIFI测试"),/* add by stephen.huang */
		new GPSSensorTest(Test.ID.GPS, "GPS测试"),
   
		new EmptyTest(Test.ID.EMERGENCY_CALL, "呼叫"),
		};

	private final static Test[] TestsListMmitest_K01TS = {
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
	//	new BatteryTempTest(Test.ID.TEMPBAT, "电池温度测试"),
		new BluetoothTest(Test.ID.BT, "蓝牙测试"),/* modify by stephen.huang */
		new WIFITest(Test.ID.WIFI, "WIFI测试"),/* add by stephen.huang */
		new GPSSensorTest(Test.ID.GPS, "GPS测试"),
   
		new EmptyTest(Test.ID.EMERGENCY_CALL, "呼叫"),
		};
	
	private final static Test[] TestsListMmitest_K506Q = {
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
	//	new BatteryTempTest(Test.ID.TEMPBAT, "电池温度测试"),
		new BluetoothTest(Test.ID.BT, "蓝牙测试"),/* modify by stephen.huang */
		new WIFITest(Test.ID.WIFI, "WIFI测试"),/* add by stephen.huang */
		new GPSSensorTest(Test.ID.GPS, "GPS测试"),
   
		new EmptyTest(Test.ID.EMERGENCY_CALL, "呼叫"),
		};
	
	private final static Test[] TestsListMmitest_K506TE_A = {
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
		//	new BatteryTempTest(Test.ID.TEMPBAT, "电池温度测试"),
			new BluetoothTest(Test.ID.BT, "蓝牙测试"),/* modify by stephen.huang */
			new WIFITest(Test.ID.WIFI, "WIFI测试"),/* add by stephen.huang */
			new GPSSensorTest(Test.ID.GPS, "GPS测试"),
	   
			new EmptyTest(Test.ID.EMERGENCY_CALL, "呼叫"),
			};
	static Test[] TestsListSw = { new VibratorTest(Test.ID.VIB, "VIB"),
			new SlideTest(Test.ID.SLIDE, "SLIDE"),
			new KeypadTest(Test.ID.KEYPAD, "KEYPAD"),
			new BluetoothTest(Test.ID.BT, "BT"),
			new WIFITest(Test.ID.WIFI, "WIFI"),
			new MiscTest(Test.ID.MISC, "MISC"),
			new CompassTest(Test.ID.COMPASS, "COMPASS"),
			new GPSSensorTest(Test.ID.GPS, "GPS"),
			new MicTest(Test.ID.MIC, "MIC"), };

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		NfcAdapter mNfcAdapter = null;
		mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
	    myPhoneStateListen = new MyPhoneStateListener();

		/*if (!BuildConfig.isSW) {
			// production MMITEST test list 
			if (a == 2) {
				if (FeatureOption.MTK_GEMINI_SUPPORT == false) {
					if (mNfcAdapter == null)
						TestsList = TestsListMmitestS;
					else
						TestsList = TestsListMmitestS_Nfc;
				} else {
					if (mNfcAdapter == null)
						TestsList = TestsListMmitest;
					else
						TestsList = TestsListMmitest_Nfc;
				}
			} else {
				TestsListMmitest = TestsListMmitest_noSub;
				TestsList = TestsListMmitest;
			}
		} else {
			// SW engineermode test list 
			if (a == 2) {
				if (FeatureOption.MTK_GEMINI_SUPPORT == false) {
					if (mNfcAdapter == null)
						TestsList = TestsListMmitestS;
					else
						TestsList = TestsListMmitestS_Nfc;
				} else {
					if (mNfcAdapter == null)
						TestsList = TestsListMmitest;
					else
						TestsList = TestsListMmitest_Nfc;
				}
			} else {
				TestsListMmitest = TestsListMmitest_noSub;
				TestsList = TestsListMmitest;
			}
		}*/



		Locale locale = Locale.getDefault();
		 if (locale.equals(Locale.CHINA))
		 {
			 TestsListMmitest = TestsListMmitest_Scribe5HD_mini_cn; 
		 }else
			 if (BuildConfig.getMmiTest()) {
			TestsListMmitest = TestsListMmitest_Scribe5HD_mini;
		} 
		 if("1".equals(SystemProperties.get("ro.mmi.type", "0")))
			 TestsListMmitest = TestsListMmitest_K453;
		 else if("2".equals(SystemProperties.get("ro.mmi.type", "0")))
		 {
			 TestsListMmitest = TestsListMmitest_K551S;
		 }else if("3".equals(SystemProperties.get("ro.mmi.type", "0")))
		 {
			 TestsListMmitest = TestsListMmitest_K01TS;
		 }else if("4".equals(SystemProperties.get("ro.mmi.type", "0")))
		 {
			 TestsListMmitest = TestsListMmitest_K506Q;
		 }else if("5".equals(SystemProperties.get("ro.mmi.type", "0")))
		 {
			 TestsListMmitest = TestsListMmitest_K506TE_A;
		 }else
			 TestsListMmitest = TestsListMmitest_Scribe5HD_mini_cn;
		TestsList = TestsListMmitest;
		//add by xianfeng.xu for CR364979 begin
		Intent mIntent =getIntent();
		if(mIntent!=null){
		    String mShortCode = mIntent.getStringExtra("ShortCode");
		    TestsList = BuildConfig.getList(TestsList, mShortCode);
		}
		//add by xianfeng.xu for CR364979 end
		// set screen appearance
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		int mask = WindowManager.LayoutParams.FLAG_FULLSCREEN;
		mask |= WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
		getWindow().setFlags(mask, mask);
		
		getListView().setTextFilterEnabled(true);

		// PHONE SETTINGS for MMI test
		// this is not necessary in case of factory test mode
		// try {
		// Settings.System.putInt(getContentResolver(),
		// SCREEN_OFF_TIMEOUT, -1 /* always on */);
		// } catch (NumberFormatException e) {
		// Log.e(TAG, "could not change screen timeout setting", e);
		// }
		// TODO change to key brightness
		// SCREEN_BRIGHTNESS is currently setting the keypad backlight intensity
		// we set it to 0 to avoid flashing every time a key is pressed
		// try {
		// Settings.System.putInt(getContentResolver(),
		// SCREEN_BRIGHTNESS, 0 );
		// } catch (NumberFormatException e) {
		// Log.e(TAG, "could not change brightness setting", e);
		// }

	}
	@Override
	public void onResume()
	{
		
		ts = new TestStatus();
		mSelectBits = ts.getMmitestStatus();
		if(mSelectBits==-1)
			mSelectBits=0;
		
		mSelectBitsExt = ts.getExtMmitestStatus();
		if(mSelectBitsExt==-1)
			mSelectBitsExt=0;
		
		mStatusBits = ts.getMmitestInfo();
		Log.d("bll","mStatusBits:"+Integer.toHexString(mStatusBits));
		
		mStatusBitsExt = ts.getExtMmitestInfo();
		Log.d("bll","mStatusBitsExt:"+Integer.toHexString(mStatusBitsExt));
			for (int i = 0; i < TestsList.length&& i< MINI_AUTOTEST_MAX_ITEMS; i++) {
	    	TestsList[i].mPosition = 0;
	    	TestsList[i].passFlag = false;
	    	if(mStatusBits!=0)
			{
	    		if (TestsList[i] != null && ((mSelectBits >> i & 1) == 0x1)&&((mStatusBits >>i &1)==0x1)) {
				TestsList[i].mPosition = i;
				TestsList[i].passFlag = true;
				Log.d("bll","i:"+i+"   status:"+(mStatusBits >>i));
	    		}
			}
			}
		
			for (int i = MINI_AUTOTEST_MAX_ITEMS; i < TestsList.length; i++) {
		    	TestsList[i].mPosition = 0;
		    	TestsList[i].passFlag = false;
		    	if(mStatusBitsExt!=0)
				{
		    		if (TestsList[i] != null && ((mSelectBitsExt >> (i-MINI_AUTOTEST_MAX_ITEMS) & 1) == 0x1)&&((mStatusBitsExt >>(i-MINI_AUTOTEST_MAX_ITEMS) &1)==0x1)) {
					TestsList[i].mPosition = i;
					TestsList[i].passFlag = true;
					Log.d("bll","i:"+i+"   status:"+(mStatusBitsExt >>(i-MINI_AUTOTEST_MAX_ITEMS)));
		    		}
				}
			}
		// Use an existing ListAdapter that will map an array
		// of strings to TextViews
	    Log.d("bll","onResume");
	    int firstPositon = this.getListView().getFirstVisiblePosition();
		setListAdapter(new NewsListAdapter(ManuList.this, TestsList,0));
		this.getListView().setSelection(firstPositon);
		super.onResume();
	}
	@Override
	protected void onListItemClick(ListView lv, View v, int position, long id) {
		if(ExecuteTest.getLock()>0 && isTopExcute())
		{
			Log.i(TAG, "onListItemClick return");
			return;
		}
		Test currentTest = TestsList[position];
		if (currentTest.getId() == Test.ID.TS_CALIBRATION.ordinal()) {
			Intent Execution = new Intent(Intent.ACTION_MAIN, null);
			// Execution.setClassName("touchscreen.test",
			// "touchscreen.test.CalibrationTest");
			Execution.setClassName("com.mediatek.app.touchpanel",
					"com.mediatek.app.touchpanel.Calibrator");
			startActivity(Execution);
		} else if (currentTest.getId() == Test.ID.EMERGENCY_CALL.ordinal()) {
			Intent intent = new Intent(Intent.ACTION_CALL_EMERGENCY);
			intent.setData(android.net.Uri.fromParts("tel", "112", null));
			startActivity(intent);
			
			Log.i(TAG, "start test " + TestsList[position].toString());
			currentTest.mPosition = position;
			ExecuteTest.currentTest = currentTest;
			
			// 对电话的来电状态进行监听
			  mTelManager = (TelephonyManager) this
			    .getSystemService(Context.TELEPHONY_SERVICE);
			  // 注册一个监听器对电话状态进行监听
			  mTelManager.listen(myPhoneStateListen,
			    PhoneStateListener.LISTEN_CALL_STATE);
			//startActivity(Execution);
		} else if (currentTest.getId() == Test.ID.NWSETTING.ordinal()) {
			// Intent intent = new Intent(Intent.ACTION_CALL_EMERGENCY);
			// intent.setData(android.net.Uri.fromParts("tel", "112", null));
			Intent intent = new Intent(Intent.ACTION_MAIN);
			intent.setClassName("com.android.phone",
					"com.android.phone.Settings");
			startActivity(intent);
		} else if (currentTest.getId() < Test.ID.MAX_ITEMS.ordinal()) {
			// Normal case
			Log.i(TAG, "start test " + TestsList[position].toString());
			currentTest.mPosition = position;
			ExecuteTest.currentTest = currentTest;

			Intent Execution = new Intent(Intent.ACTION_MAIN, null);
			Execution.setClassName(this, "com.nb.mmitest.ExecuteTest");
			//startActivity(Execution);
			ExecuteTest.lock();
			startActivityForResult(Execution, 0);
		}

	}
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {

		ExecuteTest.unlock();
		if(ts == null)
			reInit();

		if (requestCode == 0||requestCode==10) {
			Log.d("bll", "11");
			if (resultCode == RESULT_OK) {
				// the Test ended up normally, check the result
				// update the current global status
				if(requestCode == 10)
				{
					ExecuteTest.currentTest.mPosition = TestsList.length-1;
					 mTelManager.listen(myPhoneStateListen,
							    PhoneStateListener.LISTEN_NONE);
				}
				if (ExecuteTest.currentTest.getResult() == Test.PASSED) {
					Log.d("bll", "222");
					if(ExecuteTest.currentTest.mPosition < MINI_AUTOTEST_MAX_ITEMS)
					{
						mStatusBits |= (int) (1 << ExecuteTest.currentTest.mPosition);
						mSelectBits |= (int) (1 << ExecuteTest.currentTest.mPosition);
					}
					else
					{
						mStatusBitsExt |= (int) (1 << (ExecuteTest.currentTest.mPosition-MINI_AUTOTEST_MAX_ITEMS));
						mSelectBitsExt |= (int) (1 << (ExecuteTest.currentTest.mPosition-MINI_AUTOTEST_MAX_ITEMS));
					}
				} else if (ExecuteTest.currentTest.getResult() == Test.FAILED) {
					if(ExecuteTest.currentTest.mPosition < MINI_AUTOTEST_MAX_ITEMS)
					{
						mStatusBits &= (int) ~(1 << ExecuteTest.currentTest.mPosition);
						mSelectBits |= (int) (1 << ExecuteTest.currentTest.mPosition);
					}
					else
					{
						mStatusBitsExt &= (int) ~(1 << (ExecuteTest.currentTest.mPosition-MINI_AUTOTEST_MAX_ITEMS));
						mSelectBitsExt |= (int) (1 << (ExecuteTest.currentTest.mPosition-MINI_AUTOTEST_MAX_ITEMS));		
					}
					Log.d("bll", "333");
				} else {
					Log.d("bll", "test " + ExecuteTest.currentTest.toString()
							+ "had an unexpected result: force to failed\n");
					if(ExecuteTest.currentTest.mPosition < MINI_AUTOTEST_MAX_ITEMS)
					{
						mStatusBits &= (int) ~(1 << ExecuteTest.currentTest.mPosition);
						mSelectBits |= (int) (1 << ExecuteTest.currentTest.mPosition);
					}else
					{
						mStatusBitsExt &= (int) ~(1 << (ExecuteTest.currentTest.mPosition-MINI_AUTOTEST_MAX_ITEMS));
						mSelectBitsExt |= (int) (1 << (ExecuteTest.currentTest.mPosition-MINI_AUTOTEST_MAX_ITEMS));
					}
				}
				
				if(ExecuteTest.currentTest.mPosition< MINI_AUTOTEST_MAX_ITEMS)
				{
					Log.d("bll","mStatus:"+Integer.toHexString(mStatusBits)+"  ExecuteTest.currentTest.mPosition:"+ExecuteTest.currentTest.mPosition);
					ts.setMmitestInfo(mStatusBits);
					ts.setMmitestStatus(mSelectBits);
				}else
				{
					Log.d("bll","mStatusExt:"+Integer.toHexString(mStatusBitsExt)+"  ExecuteTest.currentTest.mPosition:"+ExecuteTest.currentTest.mPosition);
					ts.setExtMmitestInfo(mStatusBitsExt);
					ts.setExtMmitestStatus(mSelectBitsExt);
				}

			}	
		}
	}
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		// TODO Auto-generated method stub
		super.onConfigurationChanged(newConfig);
	}
    public class NewsListAdapter extends ArrayAdapter<Test> {
        
        private Context mContext;
       
        private LayoutInflater mInflater;
       
        public NewsListAdapter(Context context, Test[] objects,int itemType) {
               
                super(context, 0, objects);
               
                mContext = context;
                mInflater =
                        (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
                // TODO Auto-generated method stub
                Test it = getItem(position);               
                TextView mListTitle=null;
                Log.d("bll", "NewsListAdapter getView "+it.toString());
                Log.d("bll", "mPosition:"+it.mPosition+"    position:"+(((mSelectBits >> position) & 1) == 0x1)+" passFlag:"+it.passFlag);
                Test currentTest = TestsList[position];
                if (convertView == null) {
                        convertView = mInflater.inflate(android.R.layout.simple_list_item_1, null);                                           
                        mListTitle = (TextView) convertView.findViewById(android.R.id.text1);  
                        mListTitle.setText(it.toString());
                      
                } 
               else
                {
                	
                	  mListTitle = (TextView) convertView.findViewById(android.R.id.text1);  
                    mListTitle.setText(it.toString());
                   // mListTitle.setTextColor(android.R.color.holo_red_light);
                }
              if(position < MINI_AUTOTEST_MAX_ITEMS)
                {
                if(!it.passFlag&&((mSelectBits >> position) & 1) == 0x1)
                	convertView.setBackgroundColor(Color.RED);
                else if((((mSelectBits >> position) & 1) == 0x1)&&it.passFlag)
                	{
                	  convertView.setBackgroundColor(Color.GREEN);
                 }else
                	convertView.setBackgroundColor(Color.TRANSPARENT);
                }
               else
                {
                    if(!it.passFlag&&((mSelectBitsExt >> (position -MINI_AUTOTEST_MAX_ITEMS)) & 1) == 0x1)
                    	convertView.setBackgroundColor(Color.RED);
                    else if((((mSelectBitsExt >> (position-MINI_AUTOTEST_MAX_ITEMS)) & 1) == 0x1)&&it.passFlag)
                    	{
                    	  convertView.setBackgroundColor(Color.GREEN);
                     }else
                    	convertView.setBackgroundColor(Color.TRANSPARENT);
                }

                return convertView;
        }     
    }
    class TestStatus {

		private File mStatusFile;

		String TAG = "MMITEST:TestStatus";

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
    	private  boolean flag=false;
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
                		Log.d("bll", "not lock");
            			Intent Execution = new Intent(Intent.ACTION_MAIN, null);
            			Execution.setClassName(ManuList.this, "com.nb.mmitest.ExecuteTest");
            			ExecuteTest.lock();		
            			startActivityForResult(Execution, 10);
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
	@Override
	public void onPause() {
		SystemProperties.set("sys.config.mmitest", "0");
		super.onPause();

	}
	@Override
	public void onDestroy()
	{
		SystemProperties.set("sys.config.mmitest", "0");
		super.onDestroy();
	}
	
	private boolean isTopExcute()
	{
		// added by liliang.bao, for MMITest. It's a temporary solution, need to been fixed in app.
        ActivityManager am = (ActivityManager)this.getSystemService(Context.ACTIVITY_SERVICE);
        ComponentName cn = am.getRunningTasks(1).get(0).topActivity;
        if (cn != null &&
                cn.getClassName() != null &&(
                cn.getClassName().equals("com.wxkjcom.mmitest.ExecuteTest")||cn.getClassName().equals("com.nb.mmitest.ExecuteTest"))){
            Log.i(TAG, "now the top activity is com.wxkjcom.mmitest.ExecuteTest, so give it the key.");
            return true;
        }
        return false;
        // end added by liliang.bao
	}
	
	private void reInit()
	{
		ts = new TestStatus();
		mSelectBits = ts.getMmitestStatus();
		if(mSelectBits==-1)
			mSelectBits=0;
		mStatusBits = ts.getMmitestInfo();
		Log.d("bll","mStatusBits:"+Integer.toHexString(mStatusBits));
		
			for (int i = 0; i < TestsList.length&& i<MINI_AUTOTEST_MAX_ITEMS ; i++) {
	    	TestsList[i].mPosition = 0;
	    	TestsList[i].passFlag = false;
	    	if(mStatusBits!=0)
			{
	    		if (TestsList[i] != null && ((mSelectBits >> i & 1) == 0x1)&&((mStatusBits >>i &1)==0x1)) {
				TestsList[i].mPosition = i;
				TestsList[i].passFlag = true;
				Log.d("bll","i:"+i+"   status:"+(mStatusBits >>i));
	    		}
			}
	       Log.d("bll","reInit");
	}
			
			mSelectBitsExt = ts.getExtMmitestStatus();
			if(mSelectBitsExt==-1)
				mSelectBitsExt=0;
			mStatusBitsExt= ts.getExtMmitestInfo();
			Log.d("bll","mStatusBitsExt:"+Integer.toHexString(mStatusBitsExt));
			
				for (int i = MINI_AUTOTEST_MAX_ITEMS; i < TestsList.length; i++) {
		    	TestsList[i].mPosition = 0;
		    	TestsList[i].passFlag = false;
		    	if(mStatusBitsExt!=0)
				{
		    		if (TestsList[i] != null && ((mSelectBitsExt >> (i-MINI_AUTOTEST_MAX_ITEMS) & 1) == 0x1)&&((mStatusBitsExt >>(i-MINI_AUTOTEST_MAX_ITEMS) &1)==0x1)) {
					TestsList[i].mPosition = i;
					TestsList[i].passFlag = true;
					Log.d("bll","i:"+i+"   status:"+(mStatusBitsExt >>(i-MINI_AUTOTEST_MAX_ITEMS)));
		    		}
				}
		       Log.d("bll","reInit");
		}
 }
}
