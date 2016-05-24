/**
 *  EchoCoInServer
 *      Connection-oriented Inbound LLCP Echo Server
 */

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
import android.widget.Toast;

import com.mediatek.nfc.dta.EchoServerDefine;

import com.mediatek.nfc.Util;

public class CTLEchoCoInServer implements Runnable, Handler.Callback {
    private static final String TAG = "CTLEchoCoInServer";
    private NfcService mService;
    private LlcpConnectionlessSocket CTLSocket;
    private boolean running;
    private boolean mConnectToCoOutServerFail;
    private ArrayList mDataList = new ArrayList<byte[]>();
    private Handler mHandler;
    private Context mContext;
    private int mRemoteSap;
    private Thread mThread;
    private int count;

    public static final int SET_TOAST_DISPLAY    = 2;

    private static CTLEchoCoInServer sStaticInstance = null;

    private CTLEchoCoOutClient mCTLEchoCoOutClient = null;


    private CTLEchoCoInServer(Context context) {
        Log.d(TAG, "CTLEchoCoInServer() ENTRY");

        mContext = context;
        mService = NfcService.getInstance();
        mHandler = new Handler(this);



    }

    public static void createSingleton(Context context) {
        if (sStaticInstance == null) {
            sStaticInstance = new CTLEchoCoInServer(context);
        }
    }

    public static CTLEchoCoInServer getInstance() {
        return sStaticInstance;
    }

    public void enable() {
        Log.d(TAG, " enable()");
        running = true;

        Log.d(TAG, "about create LLCP CTL service socket");

        try {
            CTLSocket = mService.createLlcpConnectionLessSocket(
                EchoServerDefine.CL_SAP_IN, EchoServerDefine.CL_NAME_IN);


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

        if(mCTLEchoCoOutClient != null){
            mCTLEchoCoOutClient.closeClientSocket();
            Log.d(TAG, " set mCTLEchoCoOutClient to null");
            mCTLEchoCoOutClient = null;
        }

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

        if(mCTLEchoCoOutClient != null){
            mCTLEchoCoOutClient.closeClientSocket();
            Log.d(TAG, " set mCTLEchoCoOutClient to null");
            mCTLEchoCoOutClient = null;
        }



    }

    public void run() {
        boolean connectionBroken = false;
        LlcpPacket packet;

        Log.d(TAG, "CTLEchoCoInServer.run() , start CTL receive data");

        while (!connectionBroken) {


            try {
                Log.d(TAG, "start CTL receive data");
                packet = CTLSocket.receive();
                if (packet == null || packet.getDataBuffer() == null) {

                    Log.d(TAG, "  packet:"+packet+
                        " packet.getDataBuffer():"+packet.getDataBuffer());

                    Log.d(TAG, "  TODO: should we close CL IN Bound socket");
                    if(mCTLEchoCoOutClient != null){
                        mCTLEchoCoOutClient.closeClientSocket();
                    }
                    break;
                }
                Log.d(TAG, " CTL receive packet");

                byte[] dataUnit = packet.getDataBuffer();
                int size = dataUnit.length;

                Log.d(TAG, "read " + packet.getDataBuffer().length + " bytes");
                if (size < 0) {
                    connectionBroken = true;
                    break;
                }


                byte[] echoData = new byte[size];
                //mRemoteSap = packet.getRemoteSap();
                System.arraycopy(dataUnit, 0, echoData, 0, size);
                //mHandler.sendMessageDelayed(mHandler.obtainMessage(
                //SET_TOAST_DISPLAY, echoData), 0);
                synchronized (this) {
                    Log.d(TAG, "echoData: " + Util.printNdef(echoData));

                    //
                    if(mCTLEchoCoOutClient == null){
                        mCTLEchoCoOutClient = new CTLEchoCoOutClient(mContext,echoData,size);
                        if(isSOT(echoData)==false){
                            new Thread(mCTLEchoCoOutClient.new EchoSession(echoData,size)).start();
                        }
                        continue;
                    }

                    Log.d(TAG, "call  CTLEchoCoOutClient to send echo data" );
                    new Thread(mCTLEchoCoOutClient.new EchoSession(echoData,size)).start();
                    //new Thread(mCTLEchoCoOutClient).start();
                }


            } catch (IOException e) {
                connectionBroken = true;
                Log.d(TAG, "CTL receive broken by IOException:", e);
                e.printStackTrace();

                if(mCTLEchoCoOutClient != null){
                    mCTLEchoCoOutClient.closeClientSocket();
                }
            } catch (Exception e) {
                connectionBroken = true;
                Log.d(TAG, "!!CTL receive broken by Exception:", e);
                e.printStackTrace();

                if(mCTLEchoCoOutClient != null){
                    mCTLEchoCoOutClient.closeClientSocket();
                }
            } finally {
                Log.d(TAG, "CTLEchoCoInServer.run()  finally");

                //connectionBroken = true;

                //if (CTLSocket != null) {
                //    try {
                //        CTLSocket.close();
                //    } catch (IOException e) {
                //    }
                //}
            }
        }

        Log.d(TAG, "CTLEchoCoInServer.run()  EXIT");

    }



    public boolean handleMessage(Message message) {
        Log.d(TAG, "message.what = " + message.what);
        if (message.what == 0) {
            //if (mEchoOutSequence != null) {
            //    mEchoOutSequence.sendDataToRemote();
            //}
        } else if (message.what == SET_TOAST_DISPLAY) {
            Toast.makeText(mContext, "CTL IN Server receive: "
                + new String((byte[])message.obj), Toast.LENGTH_SHORT).show();
        }
        return true;
    }



    public void onLlcpActivated() {
        Log.d(TAG, "onLlcpActivated");
        // dummy for now
        start();
    }

    public void onLlcpDeactivated() {
        Log.d(TAG, "onLlcpDeactivated");
        stop();

    }

    public boolean isSOT(byte[] data) {

        if(data[0]== 0x53 && data[1]== 0x4F && data[2]== 0x54){
            Log.d(TAG, "isSOT ,return true");
            return true;
        }else{
            Log.d(TAG, "isSOT ,return false");
            return false;

        }

    }



}
