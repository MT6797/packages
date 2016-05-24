package com.mediatek.nfc.dta;

import com.android.nfc.DeviceHost.LlcpConnectionlessSocket;
import com.android.nfc.LlcpException;
import com.android.nfc.DeviceHost.LlcpServerSocket;
import com.android.nfc.DeviceHost.LlcpSocket;
import com.android.nfc.LlcpPacket;
import com.android.nfc.NfcService;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import java.util.List;
import java.util.ArrayList;

import com.mediatek.nfc.dta.EchoServerDefine;
import android.widget.Toast;

import com.mediatek.nfc.Util;

public class CTLEchoCoOutServer implements Runnable, Handler.Callback {

    public static final String TAG = "CTLEchoCoOutServer";
    private NfcService mService;
    private LlcpConnectionlessSocket CTLSocket;
    private boolean running;
    private Context mContext;
    private Handler mHandler;
    private Thread mThread;
    private int count;

    public static final int SET_TOAST_DISPLAY    = 2;

    private static CTLEchoCoOutServer sStaticInstance = null;

    public CTLEchoCoOutServer(Context context) {
        mContext = context;
        mService = NfcService.getInstance();
        mHandler = new Handler(this);
    }


    public static void createSingleton(Context context) {
        if (sStaticInstance == null) {
            sStaticInstance = new CTLEchoCoOutServer(context);
        }
    }

    public static CTLEchoCoOutServer getInstance() {
        return sStaticInstance;
    }

    public void enable() {
        Log.d(TAG, " enable()");
        running = true;

        Log.d(TAG, "about create LLCP CTL service socket");

        try {
            CTLSocket = mService.createLlcpConnectionLessSocket(
                EchoServerDefine.CL_SAP_OUT, EchoServerDefine.CL_NAME_OUT);


        } catch (Exception e) {
            Log.d(TAG, "Exception :"+e);
            e.printStackTrace();
        }

        if (CTLSocket == null) {
            Log.d(TAG, "failed to create LLCP CTL service socket");
            return;
        }

    }

    public void disable() {
        Log.d(TAG, " disable()");
        if (CTLSocket != null) {
            try {
                CTLSocket.close();
            } catch (Exception e) {
                Log.d(TAG, "Exception :"+e);
                e.printStackTrace();
            }
        }
    }


    public boolean handleMessage(Message message) {
        Toast.makeText(mContext, "CO OUT CTL Server receive: "
            + new String((byte[])message.obj), Toast.LENGTH_SHORT).show();
        return true;
    }

    public void start() {
        Log.d(TAG, " start()");
        running = true;


        try {
            mThread = new Thread(sStaticInstance);

            mThread.start();
            count++;
        } catch (Exception e) {
            Log.d(TAG, "Exception :"+e);
            e.printStackTrace();
        }

        Log.d(TAG, "mThread:"+mThread+" count:"+count);

    }

    public void stop() {
        Log.d(TAG, " stop()");

        running = false;

        Log.d(TAG, "mThread:"+mThread+" count:"+count);
        try {
            if(mThread != null){
                mThread.stop();
                count--;

                Log.d(TAG, "set mThread to null");
                mThread = null;
            }
        } catch (Exception e) {
            Log.d(TAG, "Exception :"+e);
            e.printStackTrace();
        }


    }

    public void run() {
        boolean connectionBroken = false;
        LlcpPacket packet;

        Log.d(TAG, "CTLEchoCoOutServer.run() , start CTL receive data");

        while (!connectionBroken) {
            try {
                packet = CTLSocket.receive();
                if (packet == null || packet.getDataBuffer() == null) {
                    break;
                }
                byte[] dataUnit = packet.getDataBuffer();
                int size = dataUnit.length;

                Log.d(TAG, "read " + dataUnit.length + " bytes");
                if (size < 0) {
                    connectionBroken = true;
                    break;
                } else {
                    byte[] echoData = new byte[size];
                    System.arraycopy(dataUnit, 0, echoData, 0, size);
                    Log.d(TAG, "receive bytes = " + size);
                    Log.d(TAG, "echoData: " + Util.printNdef(echoData));
                    mHandler.sendMessageDelayed(mHandler.obtainMessage(
                        SET_TOAST_DISPLAY, echoData), 0);
                }
            } catch (Exception e) {
                Log.d(TAG, "exception during handleCoOutClient");
                e.printStackTrace();
                connectionBroken = true;
            }finally {
                Log.d(TAG, "CTLEchoCoOutServer.run()  finally");

                connectionBroken = true;

            }
        }

        try {
            CTLSocket.close();
        } catch (Exception e) {
            Log.d(TAG, "exception during termination or socket close");
            e.printStackTrace();
        }

        Log.d(TAG, "CTLEchoCoOutServer.run()  EXIT");
    }

    public void onLlcpActivated() {
        Log.d(TAG, "onLlcpActivated");
        start();
    }

    public void onLlcpDeactivated() {
        Log.d(TAG, "onLlcpDeactivated");
        stop();
    }


}
