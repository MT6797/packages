package com.nb.mmitest;

import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.BufferedOutputStream;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import android.util.Log;

/*
 *  This Class can be used only if MMITest is root
 * 
 * 
 * 
 */

class BackLight {

	private final String TAG = "MMITest:BackLight";
	// static private FileWriter LcdBacklightFile, KbdBacklightFile;
	static private FileOutputStream LcdBacklightFile, KbdBacklightFile;
	private int Version = 0;

	BackLight() {

		try {
			// LcdBacklightFile = new
			// FileWriter("/sys/class/leds/lcd-backlight/brightness");
			// KbdBacklightFile = new
			// FileWriter("/sys/class/leds/keyboard-backlight/brightness");
			LcdBacklightFile = new FileOutputStream(
					"/sys/class/leds/lcd-backlight/brightness");
			KbdBacklightFile = new FileOutputStream(
					"/sys/class/leds/button-backlight/brightness");
		} catch (FileNotFoundException fnfe) {
			Log.d(TAG, "Specified file not found" + fnfe);
		} catch (IOException ioe) {
			Log.d(TAG, "cannot write file " + ioe);
		}

	}

	BackLight(int ver) {
		Version = ver;
	}

	private void setBacklight(int value, FileOutputStream file) {
		try {
			file.write(Integer.toString(value).getBytes());
			Log.d(TAG, "write " + value + " to " + file.toString());
		} catch (IOException ioe) {
			Log.d(TAG, "Error while writing file" + ioe);
		}

	}

	public void setKbdBacklight(int value) {
		if (Version == 2) {
			WXKJRapi.setKbdBacklight(value);
			return;
		}

		if (KbdBacklightFile != null) {
			setBacklight(value, KbdBacklightFile);
		} else {
			Log.d(TAG, "KbdBacklightFile is null");
		}
	}

	public void setLcdBacklight(int value) {
		if (Version == 2) {
			WXKJRapi.setLcdBacklight(value);
			return;
		}

		if (LcdBacklightFile != null) {
			setBacklight(value, LcdBacklightFile);
		} else {
			Log.d(TAG, "LcdBacklightFile is null");
		}
	}

}
