package com.mediatek.nfc.dta;

//import com.android.nfc.DeviceHost.LlcpConnectionlessSocket;
import com.android.nfc.LlcpException;
import com.android.nfc.DeviceHost.LlcpServerSocket;
import com.android.nfc.DeviceHost.LlcpSocket;
//import com.android.nfc.LlcpPacket;
import com.android.nfc.NfcService;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
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

public class EchoCoOutServer implements Runnable, Handler.Callback {

    public static final String TAG = "NfcEchoCoOutServer";
    private NfcService mService;
    private LlcpServerSocket serverSocket;
    private boolean running;
    private Context mContext;
    private Handler mHandler;

    public static final int SET_ECHO_DATA        = 1;
    public static final int SET_TOAST_DISPLAY    = 2;

    //private CoEchoOutSequence mEchoOutSequence;

    private static EchoCoOutServer sStaticInstance = null;


    private EchoCoOutServer(Context context) {
        Log.d(TAG, "EchoCoOutServer()");

        mContext = context;
        mService = NfcService.getInstance();
        mHandler = new Handler(this);
    }

    public static void createSingleton(Context context) {
        if (sStaticInstance == null) {
            sStaticInstance = new EchoCoOutServer(context);
        }
    }

    public static EchoCoOutServer getInstance() {
        return sStaticInstance;
    }

    /*
    public void sendEchoData(byte[] data) {
        Log.d(TAG, "sendEchoData(data) delay "+EchoServerDefine.CO_ECHO_OUT_DELAY);

        mHandler.sendMessageDelayed(mHandler.obtainMessage(
        SET_ECHO_DATA, data), EchoServerDefine.CO_ECHO_OUT_DELAY);

    }
    */

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
        Log.d(TAG, "start()");
        running = true;
        new Thread(sStaticInstance).start();
    }

    public void stop() {
        Log.d(TAG, "stop()");
        running = false;
        try {
            serverSocket.close();
        } catch (Exception e) {
            Log.d(TAG, "exception during termination or socket close");
            e.printStackTrace();
        }
    }

    public void run() {
        Log.d(TAG, "EchoCoOutServer.run() about create LLCP service socket");
        try {
            serverSocket = mService.createLlcpServerSocket(
                EchoServerDefine.CO_SAP_OUT, EchoServerDefine.CO_NAME_OUT,
                EchoServerDefine.DEFAULT_LLCP_MIU, 1, 1024);
        } catch (LlcpException e) {
            e.printStackTrace();
            return;
        }
        if (serverSocket == null) {
            Log.d(TAG, "failed to create LLCP service socket");
            return;
        }
        Log.d(TAG, "created LLCP service socket");

        while (running) {
            try {
                Log.d(TAG, "about to accept");
                LlcpSocket clientSocket = serverSocket.accept();
                Log.d(TAG, "accept returned " + clientSocket);
                //handleCoOutClient(clientSocket);
                handleDtaCoOut(clientSocket);
            } catch (LlcpException e) {
                Log.e(TAG, "llcp error", e);
                e.printStackTrace();
                running = false;
            } catch (IOException e) {
                Log.e(TAG, "IO error", e);
                e.printStackTrace();
                running = false;
            }
        }

        try {
            serverSocket.close();
        } catch (Exception e) {
            Log.d(TAG, "exception during termination or socket close");
            e.printStackTrace();
        }

        Log.d(TAG, "EchoCoOutServer.run() EXIT");
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

}
