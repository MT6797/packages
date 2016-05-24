package com.mediatek.nfc.dta;

//import com.android.nfc.DeviceHost.LlcpConnectionlessSocket;
import com.android.nfc.LlcpException;
//import com.android.nfc.DeviceHost.LlcpServerSocket;
import com.android.nfc.DeviceHost.LlcpSocket;
//import com.android.nfc.LlcpPacket;
import com.android.nfc.NfcService;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

//import java.io.IOException;
//import java.util.concurrent.BlockingQueue;
//import java.util.concurrent.LinkedBlockingQueue;

//import android.content.BroadcastReceiver;
import android.content.Context;
//import android.content.Intent;
//import android.content.IntentFilter;
//import java.util.List;
//import java.util.ArrayList;

//import com.mediatek.nfc.dta.EchoServerDefine;
import android.widget.Toast;
//import com.mediatek.nfc.Util;

import com.mediatek.nfc.addon.MtkNfcAddonSequence;


public class EchoCoOutClient implements Runnable, Handler.Callback {

    public static final String TAG = "NfcEchoCoOutClient";
    private NfcService mService;
    //private LlcpServerSocket serverSocket;
    private boolean running;
    private Context mContext;
    //private Handler mHandler;

    final Object mDelayWaiter = new Object();


    LlcpSocket mSocket;

    public static final int SET_ECHO_DATA        = 1;
    public static final int SET_TOAST_DISPLAY    = 2;

    //private CoEchoOutSequence mEchoOutSequence;

    //private static EchoCoOutClient sStaticInstance = null;

    byte[] mSendData;
    int mPatternNumber;

    public EchoCoOutClient(Context context,int patternNumber) {
        Log.d(TAG, "EchoCoOutClient()  patternNumber:"+patternNumber);

        mContext = context;
        mService = NfcService.getInstance();
        //mHandler = new Handler(this);

        mPatternNumber = patternNumber;

        //mSendData = new byte[size];
        //System.arraycopy(mSendData, 0, data, 0, size);


        try {

            //socket
            mSocket = NfcService.getInstance().createLlcpSocket(
                EchoServerDefine.CO_SAP_SRC, EchoServerDefine.DEFAULT_LLCP_MIU,
                EchoServerDefine.DEFAULT_LLCP_RWSIZE, 1024);
            if (mSocket == null) {
                Log.e(TAG, "Could not createLlcpSocket.:" );
            }



            switch(mPatternNumber){
                case EchoServerDefine.CO_CONNECT_BY_NAME:
                    Log.d(TAG, "about to connect to service name:" + EchoServerDefine.CO_NAME_OUT);
                    mSocket.connectToService(EchoServerDefine.CO_NAME_OUT);
                    break;

                case EchoServerDefine.CO_CONNECT_BY_SNL:

                    Log.d(TAG, " serviceName lookup :"+EchoServerDefine.CO_NAME_OUT );

                    int sap = MtkNfcAddonSequence.getInstance().
                        serviceNameLookup(EchoServerDefine.CO_NAME_OUT);

                    Log.d(TAG, "about to connect to SAP :" + EchoServerDefine.CO_SAP_OUT);
                    mSocket.connectToSap(sap);
                case EchoServerDefine.CO_CONNECT_BY_SAP:
                    Log.d(TAG, "about to connect to SAP :" + EchoServerDefine.CO_SAP_OUT);
                    mSocket.connectToSap(EchoServerDefine.CO_SAP_OUT);
                    break;

            }

        } catch (LlcpException e) {
            Log.e(TAG, "LlcpException :", e);
            e.printStackTrace();

        } catch (Exception e) {
                Log.e(TAG, "Exception :", e);
                e.printStackTrace();
        }


    }

    public void closeClientSocket() {

        Log.d(TAG, "closeClientSocket :"+mSocket);
        if (mSocket != null) {
            try {
                Log.d(TAG, "mSocket.close() ");
                mSocket.close();
            } catch (Exception e) {
                Log.e(TAG, "exception during socket close");
                e.printStackTrace();
            }
        }


    }


    public void setEchoData(byte[] data,int size) {
        Log.d(TAG, "setEchoData(data) size:"+size);

        mSendData = new byte[size];
        System.arraycopy(data, 0, mSendData, 0, size);

        //mHandler.sendMessageDelayed(mHandler.obtainMessage(
        //SET_ECHO_DATA, data), EchoServerDefine.CO_ECHO_OUT_DELAY);

    }

    public boolean handleMessage(Message message) {

        Log.d(TAG, "message.what = " + message.what);
        if (message.what == SET_ECHO_DATA) {

            //Log.d(TAG, "sendToRemoteCoOutServer() " );
            //sendToRemoteCoOutServer((byte[])message.obj);


        }else if(message.what == SET_TOAST_DISPLAY){
            Toast.makeText(mContext, "CO OUT Server receive: "
                + new String((byte[])message.obj), Toast.LENGTH_SHORT).show();
        }
        return true;
    }

    public void start() {
        running = true;
        new Thread(this).start();
    }

    public void stop() {
        running = false;
        closeClientSocket();
    }

    public void run() {
        Log.d(TAG, "EchoCoOutClient run() ");

        try {

            //Thread sleep

            synchronized (mDelayWaiter) {
                Log.d(TAG, "EchoCoOutClient run(),Delay "+
                    EchoServerDefine.CO_ECHO_OUT_DELAY+" to send");
                mDelayWaiter.wait(EchoServerDefine.CO_ECHO_OUT_DELAY);
                Log.d(TAG, "EchoCoOutClient run(),Delay done, Ready to Send");
            }




            mSocket.send(mSendData);

            Log.d(TAG, " mSocket.send complete");

        }catch (Exception e) {
                Log.e(TAG, "Exception :", e);
                e.printStackTrace();
        }finally{

            Log.d(TAG, "finally  mSocket:"+mSocket);
            Log.d(TAG, "not to close EchoCoOutClient mSocket  ");

            /*
            if (mSocket != null) {
                try {
                    Log.d(TAG, "mSocket.close() ");
                    mSocket.close();
                } catch (Exception e) {
                    Log.e(TAG, "exception during socket close");
                    e.printStackTrace();
                }
            }
            */


        }


        Log.d(TAG, "EchoCoOutClient Thread EXIT");


    }

    public void onLlcpActivated() {
        Log.d(TAG, "onLlcpActivated");
        // dummy for now
    }

    public void onLlcpDeactivated() {
        Log.d(TAG, "onLlcpDeactivated");
        //if (mEchoOutSequence != null) {
        //    mEchoOutSequence.terminateSequence();
        //    mEchoOutSequence = null;
        //}
    }

/*
    private void handleDtaCoOut(LlcpSocket clientSocket) {
        byte[] dataUnit = new byte[1024];
        boolean connectionBroken = false;
        while (!connectionBroken) {
            try {
                int bytes = clientSocket.receive(dataUnit);
                if (bytes < 0) {
                    Log.d(TAG, "fail to receive, return = " + bytes);
                    connectionBroken = true;
                } else {
                    byte[] data2Print = new byte[bytes];
                    System.arraycopy(dataUnit, 0, data2Print, 0, bytes);
                    Log.d(TAG, "receive bytes = " + bytes);
                    Log.d(TAG, data2Print.toString());
                    mHandler.sendMessageDelayed(mHandler.obtainMessage(
                        SET_TOAST_DISPLAY, data2Print), 0);
                }
            } catch (Exception e) {
                Log.d(TAG, "exception during handleCoOutClient");
                e.printStackTrace();
                connectionBroken = true;
            }
        }

        try {
            clientSocket.close();
        } catch (Exception e) {
            Log.d(TAG, "exception during termination or socket close");
            e.printStackTrace();
        }
    }
*/
    /*
    private void handleCoOutClient(LlcpSocket clientSocket) {
        byte[] dataUnit = new byte[1024];
        boolean connectionBroken = false;
        while (!connectionBroken) {
            try {
                int bytes = clientSocket.receive(dataUnit);
                if (bytes < 0) {
                    Log.d(TAG, "fail to receive, return = " + bytes);
                    connectionBroken = true;
                } else {
                    byte[] data2Print = new byte[bytes];
                    System.arraycopy(dataUnit, 0, data2Print, 0, bytes);
                    Log.d(TAG, "receive bytes = " + bytes);
                    Log.d(TAG, data2Print.toString());
                    mHandler.sendMessageDelayed(mHandler.obtainMessage(
                    SET_TOAST_DISPLAY, data2Print), 0);
                }
            } catch (Exception e) {
                Log.d(TAG, "exception during handleCoOutClient");
                e.printStackTrace();
                connectionBroken = true;
            }
        }

        try {
            clientSocket.close();
        } catch (Exception e) {
            Log.d(TAG, "exception during termination or socket close");
            e.printStackTrace();
        }
    }
    */





}
