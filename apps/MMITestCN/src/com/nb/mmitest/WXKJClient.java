package com.nb.mmitest;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.util.Log;

public class WXKJClient {
    private static final String TAG = "WXKJClient";
    
    //define MMI test commands
    public static final int MMICMD_SDRW = 0x7800;
    public static final int MMICMD_KEYBL_FLICKER = 0x7801;

    public static final int MMICMD_AUDIOLOOP_ON = 0x7810;
    public static final int MMICMD_AUDIOLOOP_OFF = 0x7811;
    public static final int MMICMD_SUB_AUDIOLOOP_ON = 0x7812;
    public static final int MMICMD_SUB_AUDIOLOOP_OFF = 0x7813;
    public static final int MMICMD_HEADSET_AUDIOLOOP_ON = 0x7820;
    public static final int MMICMD_HEADSET_AUDIOLOOP_OFF = 0x7821;

    //define reply values
    private final int MMIREP_OK=0x6600;
    private final int MMIREP_UNKNOWN=0x6601;
    private final int MMIREP_ERROR=0x6602;
    
	private InputStream mIn;
	private OutputStream mOut;
	private LocalSocket mSocket;
	private byte cmd_buf[] = new byte[4];
	private byte reply_buf[] = new byte[4];

	public WXKJClient() {
		mSocket = new LocalSocket();
		
	}

    protected boolean connect() {
        if (mSocket == null) {
            return false;
        }
        Log.i(TAG, "connecting...");
        try {
        	
            LocalSocketAddress address = new LocalSocketAddress(
                "mmi_proxy", LocalSocketAddress.Namespace.RESERVED);

            mSocket.connect(address);

            mIn = mSocket.getInputStream();
            mOut = mSocket.getOutputStream();
        } catch (IOException ex) {
            disconnect();
            return false;
        }
        return true;
    }

    protected void disconnect() {
        Log.i(TAG,"disconnecting...");
		try {
			if (mSocket != null) mSocket.close();
		} catch (IOException ex) { }
		try {
			if (mIn != null) mIn.close();
		} catch (IOException ex) { }
		try {
			if (mOut != null) mOut.close();
		} catch (IOException ex) { }
		mSocket = null;
		mIn = null;
		mOut = null;
	}

    protected boolean readReply() {
		int count;
		try {
			count = mIn.read(reply_buf, 0, 4);
			if (count <= 0) {
                Log.e(TAG, "read error " + count);
			}
		} catch (IOException ex) {
            Log.e(TAG,"read exception");
            return false;
		}
		int reply = (((int) reply_buf[0]) & 0xff) | ((((int) reply_buf[1]) & 0xff) << 8);
		Log.i(TAG, "Reply is 0x" + Integer.toHexString(reply));		
		if (reply == MMIREP_OK)
			return true;
		else if (reply == MMIREP_ERROR)
			return false;
		else if (reply == MMIREP_UNKNOWN)
			return false;
		else 
			return false;
	}

    protected boolean sendCommand(int cmd) {
		
        cmd_buf[0] = (byte) (cmd & 0xff);
        cmd_buf[1] = (byte) ((cmd >> 8) & 0xff);
        cmd_buf[2] = (byte) ((cmd >> 16) & 0xff);
        cmd_buf[3] = (byte) ((cmd >> 24) & 0xff);

		try {
			mOut.write(cmd_buf, 0, 4);
		} catch (IOException ex) {
            Log.e(TAG,"write error");
			return false;
		}
		return true;
	}	
}
