package com.nb.mmitest;

import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.File;
import java.io.InputStream;
import java.io.FileOutputStream;

import java.lang.SecurityException;

import android.util.Log;
import android.content.Context;
import android.media.AudioSystem;

import java.util.regex.*;
import java.lang.StringBuffer;

public class WXKJRapi {

	private final String TAG = "WXKJRapi";

	public final static boolean DEBUG = true;

	private final short OEM_RAPI_CLIENT_EVENT_NONE = 0;
	/* -------------------------------- */

	/* Enumerate OEM client events here : max is 0xFF !! */
	private final short OEM_RAPI_CLIENT_EVENT_GET_MODEM_VERSION = 1;
	private final short OEM_RAPI_CLIENT_AUDIO_LOOP_TEST = 2;
	private final short OEM_RAPI_CLIENT_AUDIO_SET_PARAM = 3;
	private final short OEM_RAPI_CLIENT_SYS_OPRT_MODE_SET_MODE = 4;
	private final short OEM_RAPI_CLIENT_SYS_OPRT_MODE_GET_MODE = 5;

	/* -------------------------------- */
	private final short OEM_RAPI_CLIENT_EVENT_MAX = 6;

	public enum OprtMode {

		SYS_OPRT_MODE_NONE(-1),
		/** < FOR INTERNAL USE OF CM ONLY! */

		SYS_OPRT_MODE_PWROFF,
		/** < Phone is powering off */

		SYS_OPRT_MODE_FTM,
		/** < Phone is in factory test mode */

		SYS_OPRT_MODE_OFFLINE,
		/** < Phone is offline */

		SYS_OPRT_MODE_OFFLINE_AMPS,
		/** < Phone is offline analog */

		SYS_OPRT_MODE_OFFLINE_CDMA,
		/** < Phone is offline cdma */

		SYS_OPRT_MODE_ONLINE,
		/** < Phone is online */

		SYS_OPRT_MODE_LPM,
		/** < Phone is in LPM - Low Power Mode */

		SYS_OPRT_MODE_RESET,
		/** < Phone is resetting - i.e. power-cycling */

		SYS_OPRT_MODE_NET_TEST_GW,
		/** < Phone is conducting network test for GSM/WCDMA. */
		/** < This mode can NOT be set by the clients. It can */
		/** < only be set by the lower layers of the stack. */

		SYS_OPRT_MODE_OFFLINE_IF_NOT_FTM,
		/** < This mode is only set when there is offline */
		/** < request during powerup. */
		/** < This mode can not be set by the clients. It can */
		/** < only be set by task mode controller. */

		SYS_OPRT_MODE_PSEUDO_ONLINE,
		/** < Phone is pseudo online; tx disabled */

		SYS_OPRT_MODE_RESET_MODEM,
		/** < Phone is resetting the modem processor. */

		SYS_OPRT_MODE_MAX;
		/** < FOR INTERNAL USE OF CM ONLY! */

		private int value;
		private int last = 0;

		OprtMode(int val) {
			value = val;
			this.last = val;
		}

		OprtMode() {
			this.last++;
			value = this.last;
		}

		public int getVal() {
			return value;
		}

		public static String valToString(int val) {
			for (OprtMode o : OprtMode.values())
				if (o.getVal() == val)
					return o.toString().split("SYS_OPRT_MODE_")[1];

			return null;
		}

		public String print() {
			return this + " val = " + value + " ord = " + this.ordinal();
		}

	}

	/* snd devices */
	private final short SND_DEVICE_DEFAULT = 0;
	private final short SND_DEVICE_HANDSET = SND_DEVICE_DEFAULT + 0;
	private final short SND_DEVICE_HFK = SND_DEVICE_DEFAULT + 1;
	private final short SND_DEVICE_HEADSET = SND_DEVICE_DEFAULT + 2; /*
																	 * Mono
																	 * headset
																	 */
	private final short SND_DEVICE_STEREO_HEADSET = SND_DEVICE_DEFAULT + 3; /*
																			 * Stereo
																			 * headset
																			 */

	private final short LOOP_ON = 1;
	private final short LOOP_OFF = 0;

	private final short MUTE = 1;
	private final short UNMUTE = 0;

	private String mPipeName = null;
	private File prog = new File("/system/bin/WXKJ_rapiproxy");

	private static WXKJProxyServerThread mProxyServerInstance;

	WXKJRapi(Context c) throws FileNotFoundException {

		try {
			mPipeName = c.getFileStreamPath("fifo").getAbsolutePath();
			// mPipeOutName = c.getFileStreamPath("fifo_out").getAbsolutePath();
		} catch (SecurityException e) {

		}

		if (!prog.exists()) {
			prog = c.getFileStreamPath("rapi");// default prog is not here, look
												// in local dir

			if (!prog.exists()) {
				// not found in local dir : try to copy the rapi proxy from
				// resources to local directory to use it in audio loop

				try {
					InputStream fd = c.getResources().getAssets()
							.open("WXKJ_rapiproxy");
					byte[] buffer = new byte[fd.available()];

					int len = fd.read(buffer);
					FileOutputStream mOutputStream = new FileOutputStream(prog);
					mOutputStream.write(buffer);

					Log.d(TAG, "read " + len + " bytes of WXKJ_rapiproxy into "
							+ prog.getName());

					// change file to exec mode
					Process p = Runtime.getRuntime().exec(
							"chmod 777 " + prog.getAbsolutePath());
					p.waitFor();

				} catch (Exception e) {

					if (e.getClass() == IOException.class)
						Log.e(TAG, "can't open WXKJ_rapiproxy from assets");
					if (e.getClass() == SecurityException.class)
						Log.e(TAG, "can't get 'rapi' full path name");
					if (e.getClass() == InterruptedException.class)
						Log.e(TAG, "chmod interrupted");

					throw new FileNotFoundException(
							"/system/bin/WXKJ_rapiproxy nor " + prog.getName()
									+ " exist");
				}
			}
		} else {
			Log.d(TAG, "using proxy :" + prog.getAbsolutePath());
		}

		/* proxy program was found, start execution */
		if (mProxyServerInstance == null)
			try {
				mProxyServerInstance = new WXKJProxyServerThread();
			} catch (Exception e) {
				Log.e(TAG, "WXKJProxyServerThread init error : " + e);
				return;
			}

	}

	static {
		// The runtime will add "lib" on the front and ".o" on the end of
		// the name supplied to loadLibrary.
		System.loadLibrary("WXKJrapijni");
	}

	static native byte[] read_trace_parti();

	private static native int sendcmd(String pipe, short cmd, short a, short b,
			short c);

	private static native String sendcmdForStrResult(String pipe, short cmd,
			short a, short b, short c);

	public void setAudioHandsetLoop(boolean enable) {
		sendToProxy(OEM_RAPI_CLIENT_AUDIO_LOOP_TEST, enable ? LOOP_ON
				: LOOP_OFF, SND_DEVICE_HANDSET, enable ? UNMUTE : MUTE);
	}

	public void setAudioHeadsetLoop(boolean enable) {
		sendToProxy(OEM_RAPI_CLIENT_AUDIO_LOOP_TEST, enable ? LOOP_ON
				: LOOP_OFF, SND_DEVICE_HEADSET, enable ? UNMUTE : MUTE);
	}

	public void setAudioStereoHeadsetLoop(boolean enable) {
		sendToProxy(OEM_RAPI_CLIENT_AUDIO_LOOP_TEST, enable ? LOOP_ON
				: LOOP_OFF, SND_DEVICE_STEREO_HEADSET, enable ? UNMUTE : MUTE);
	}

	public void setFTMMode() {
		sendToProxy(OEM_RAPI_CLIENT_SYS_OPRT_MODE_SET_MODE,
				(short) OprtMode.SYS_OPRT_MODE_FTM.getVal(), (short) 0,
				(short) 0);
	}

	public void setOnlineMode() {
		sendToProxy(OEM_RAPI_CLIENT_SYS_OPRT_MODE_SET_MODE,
				(short) OprtMode.SYS_OPRT_MODE_ONLINE.getVal(), (short) 0,
				(short) 0);
	}

	public int getOprtMode() {
		StringBuffer sb = new StringBuffer();
		int mode;
		sendToProxy(OEM_RAPI_CLIENT_SYS_OPRT_MODE_GET_MODE, sb, (short) 0,
				(short) 0, (short) 0);

		Pattern p = Pattern.compile("\\s*=\\s*([-0-9]*)");
		Matcher m = p.matcher(sb);
		if (m.find()) {
			return Integer.parseInt(m.group(m.groupCount()), 10);
		} else {
			return -2;
		}
	}

	private void sendToProxy(short cmd, short a, short b, short c) {
		synchronized (mProxyServerInstance) {

			CmdThread command = new CmdThread(cmd, a, b, c);
			android.os.SystemClock.sleep(500);
			try {
				command.join(2000); // block fot 2s max till command thread dies
			} catch (Exception e) {
				Log.e(TAG, "CmdThread interrupted");
			}

			// while(command.isAlive()) {
			// Log.e(TAG,"waiting for sendcmd()");
			// android.os.SystemClock.sleep(500);
			// }
		}
	}

	private void sendToProxy(short cmd, StringBuffer sb, short a, short b,
			short c) {
		synchronized (mProxyServerInstance) {
			CallbackThread command = new CallbackThread(cmd, sb, a, b, c);
			android.os.SystemClock.sleep(500);
			try {
				command.join(2000); // block fot 2s max till command thread dies
			} catch (Exception e) {
				Log.e(TAG, "CmdThread interrupted");
			}

		}
	}

	class CmdThread extends Thread {

		private short[] para = new short[4];
		private int result = 0;

		CmdThread(short cmd, short a, short b, short c) {
			para[0] = cmd;
			para[1] = a;
			para[2] = b;
			para[3] = c;

			start();
		}

		public void run() {
			result = sendcmd(mPipeName, para[0], para[1], para[2], para[3]);
			Log.d(TAG, "sendcmd returned " + result);
		}
	}

	class CallbackThread extends Thread {

		private short[] para = new short[4];
		private int mResult;

		private CallBack cb;

		CallbackThread(short cmd, short a, short b, short c) {
			para[0] = cmd;
			para[1] = a;
			para[2] = b;
			para[3] = c;
			cb = new CallBack() {
				public void c() {
					mResult = sendcmd(mPipeName, para[0], para[1], para[2],
							para[3]);
					Log.d(TAG, "sendcmd returned " + mResult);
				}
			};

			start();
		}

		CallbackThread(short cmd, final StringBuffer res, short a, short b,
				short c) {
			para[0] = cmd;
			para[1] = a;
			para[2] = b;
			para[3] = c;
			cb = new CallBack() {
				public void c() {
					res.append(sendcmdForStrResult(mPipeName, para[0], para[1],
							para[2], para[3]));
					Log.d(TAG, "sendcmdForStrResult returned " + res);
				}
			};

			start();
		}

		public void run() {
			cb.c();
		}
	}

	/*                                               */
	/* WXKJProxyServerThread executes WXKJ_rapiproxy */
	/* and polls its output for result */

	private class WXKJProxyServerThread extends Thread {

		private boolean poll;

		private Process proc;

		private WXKJProxyServerThread() throws SecurityException, IOException,
				FileNotFoundException {

			if (!prog.exists()) {
				throw new FileNotFoundException("WXKJProxyServer executable : "
						+ prog.getAbsolutePath() + " doesn't exist");
			}

			proc = Runtime.getRuntime().exec(
					prog.getAbsolutePath() + " " + mPipeName
							+ (WXKJRapi.DEBUG ? " --debug " : ""));

			// check the pipe has been created
			// WXKJ_rapiproxy creates two pipes _read and _write
			File pipe = new File(mPipeName + "_read");
			int retry = 0;

			while (!pipe.exists() && retry++ < 4) {
				android.os.SystemClock.sleep(500);
			}

			if (!pipe.exists()) {
				throw new FileNotFoundException("create " + mPipeName
						+ " failed");

			} else {
				poll = true;
				start();
			}

		}

		public void run() {

			try {

				BufferedReader reader = new BufferedReader(
						new InputStreamReader(proc.getInputStream()));

				while (poll) {
					if (reader.ready()) {
						String line = reader.readLine();
						if (line != null)
							Log.i(TAG, prog.getName() + " stdout : " + line);
					}
				}

				Log.i(TAG, "stop proxy Thread");
				reader.close();
				proc.destroy();

			} catch (IOException e) {
				// TODO Auto-generated catch block
				Log.e(TAG, "can't read stdout of " + prog.getAbsolutePath()
						+ " " + e);
			}
		}

		public void exit() {
			poll = false;
		}

	}

	static native int doNvRead(int itemId, byte[] nvItem);

	static native int doNvWrite(int itemId, byte[] nvItem);

	static void callAudioSystem(byte cmd, byte arg) {
		int DATA_SIZE = 1444;
		byte[] data = new byte[DATA_SIZE];
		int i = 0;

		data[i++] = 'P';
		data[i++] = 'a';
		data[i++] = 'r';
		data[i++] = 'a';

		data[i++] = cmd;
		data[i] = arg;

		AudioSystem.getEmParameter(data, DATA_SIZE);
	}

	static void doAudioHandsetLoop(boolean enable) {
		byte cmd = 0; // RecieverLoopbackTest
		byte arg;

		if (enable)
			arg = 1;
		else
			arg = 0;

		callAudioSystem(cmd, arg);
	}

	static public void doAudioHeadsetLoop(boolean enable) {
		byte cmd = 1; // EarphoneLoopbackTest
		byte arg;

		if (enable)
			arg = 1;
		else
			arg = 0;

		callAudioSystem(cmd, arg);
	}

	static public void setKbdBacklight(int value) {
		byte cmd = 2;
		byte arg = (byte) value;

		callAudioSystem(cmd, arg);
	}

	static public void setLcdBacklight(int value) {
		byte cmd = 3;
		byte arg = (byte) value;

		callAudioSystem(cmd, arg);
	}

}
