package com.mediatek.nfc.dta;

import com.android.nfc.DeviceHost.LlcpConnectionlessSocket;
import com.android.nfc.LlcpException;
//import com.android.nfc.DeviceHost.LlcpServerSocket;
//import com.android.nfc.DeviceHost.LlcpSocket;
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
import java.util.List;
import java.util.ArrayList;

//import com.mediatek.nfc.dta.EchoServerDefine;
import android.widget.Toast;
import com.mediatek.nfc.Util;

import com.mediatek.nfc.addon.MtkNfcAddonSequence;


public class CTLEchoCoOutClient implements Runnable, Handler.Callback {

    public static final String TAG = "CTLEchoCoOutClient";
    private NfcService mService;
    //private LlcpServerSocket serverSocket;
    private boolean running;
    private Context mContext;
    //private Handler mHandler;

    //final Object mDelayWaiter = new Object();

    LlcpConnectionlessSocket mSocket;
    int mSapValue;

    //private static CTLEchoCoOutClient sStaticInstance = null;

    //byte[] mSendData;
    //int mPatternNumber;

    public CTLEchoCoOutClient(Context context,byte[] data,int size) {
        Log.d(TAG, "CTLEchoCoOutClient() size:"+size);

        mContext = context;
        mService = NfcService.getInstance();

        Log.d(TAG, "data: " + Util.printNdef(data));
        if(data[0]== 0x53 && data[1]== 0x4F && data[2]== 0x54){

            Log.d(TAG, "SOT pattern match, serviceName lookup :"+EchoServerDefine.CL_NAME_OUT );

            mSapValue = MtkNfcAddonSequence.getInstance().
                serviceNameLookup(EchoServerDefine.CL_NAME_OUT);

        }else{
            mSapValue = EchoServerDefine.CL_SAP_OUT;

        }
        Log.d(TAG, "about to connect to mSapValue :" + mSapValue);

        Log.d(TAG, "createLlcpConnectionLessSocket src_sap:" + EchoServerDefine.CL_SAP_SRC);

        try {
            mSocket = NfcService.getInstance().createLlcpConnectionLessSocket(
                        EchoServerDefine.CL_SAP_SRC, EchoServerDefine.CL_NAME_OUT);
        } catch (LlcpException e) {
            Log.e(TAG, "LlcpException :", e);
            e.printStackTrace();
        }


        if (mSocket == null) {
            Log.e(TAG,"Could not connect to socket.");
        }

        Log.d(TAG, "CTL mSocket create , ");


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


    public boolean handleMessage(Message message) {

        Log.d(TAG, "message.what = " + message.what);
        /*
        if (message.what == SET_ECHO_DATA) {

            //Log.d(TAG, "sendToRemoteCoOutServer() " );
            //sendToRemoteCoOutServer((byte[])message.obj);


        }else if(message.what == SET_TOAST_DISPLAY){
            Toast.makeText(mContext, "CO OUT Server receive: "
            + new String((byte[])message.obj), Toast.LENGTH_SHORT).show();
        }
        */
        return true;
    }


    public void start() {
        running = true;
        new Thread(this).start();
    }

    public void stop() {
        running = false;
    }

    public void run() {
        Log.d(TAG, "CTLEchoCoOutClient run(), echo");


        Log.d(TAG, "CTLEchoCoOutClient Thread EXIT");


    }

    public void onLlcpActivated() {
        Log.d(TAG, "onLlcpActivated");

    }

    public void onLlcpDeactivated() {
        Log.d(TAG, "onLlcpDeactivated");

    }




    public class EchoSession implements Runnable {

        //public static final String TAG = "CTLEchoCoOutClient";
        private boolean running;

        final Object mDelayWaiter = new Object();

        //LlcpConnectionlessSocket mSocket;

        byte[] mSendData;

        public EchoSession(byte[] data,int size) {
            Log.d(TAG, "EchoSession() size:"+size);

            mSendData = new byte[size];
            //mPatternNumber = patternNumber;
            System.arraycopy(data, 0, mSendData, 0, size);

        }

        public void start() {
            running = true;
            new Thread(this).start();
        }

        public void stop() {
            running = false;
        }

        public void run() {
            Log.d(TAG, "EchoSession run(), echo");

            try {


                //Thread sleep
                synchronized (mDelayWaiter) {
                    Log.d(TAG, "EchoSession run(),Delay "
                        +EchoServerDefine.CO_ECHO_OUT_DELAY+" to send");
                    mDelayWaiter.wait(EchoServerDefine.CO_ECHO_OUT_DELAY);
                    Log.d(TAG, "EchoSession run(),Delay done, Ready to Send ,mSapValue:"+mSapValue);
                }


                mSocket.send(mSapValue,mSendData);

                Log.d(TAG, " mSocket.send complete");

            } catch (Exception e) {
                    Log.e(TAG, "Exception :", e);
                    e.printStackTrace();
            }finally{

                Log.d(TAG, "finally  mSocket:"+mSocket);
                Log.d(TAG, "not to close mSocket  ");

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

            Log.d(TAG, "EchoSession Thread EXIT");


        }


    }

}
