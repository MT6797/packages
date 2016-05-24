/**
 *  EchoCoInServer
 *      Connection-oriented Inbound LLCP Echo Server
 */

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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
//import java.util.List;
import java.util.ArrayList;
import android.widget.Toast;

import com.mediatek.nfc.Util;

//import com.mediatek.nfc.dta.EchoServerDefine;
import com.mediatek.nfc.addon.MtkNfcAddonSequence;

public class EchoCoInServer implements Runnable, Handler.Callback {
    private static final String TAG = "NfcEchoCoInServer";
    private NfcService mService;
    private LlcpServerSocket mServerSocket;
    private boolean running;
    private boolean mConnectToCoOutServerFail;
    private ArrayList mDataList = new ArrayList<byte[]>();
    //private CoEchoOutSequence mEchoOutSequence;
    private Handler mHandler;
    private Context mContext;

    private IntentFilter mFilter;

    int mLlcpPatternNumber;

    private static EchoCoInServer sStaticInstance;


    private EchoCoInServer(Context context) {
        Log.d(TAG, "EchoCoInServer()");
        mContext = context;
        mService = NfcService.getInstance();
        mHandler = new Handler(this);

        mFilter = new IntentFilter(EchoServerDefine.ACTION_SET_PATTERN_NUMBER);

        mContext.registerReceiver(mEchoCoInReceiver, mFilter);


    }

    public static void createSingleton(Context context) {
        if (sStaticInstance == null) {
            sStaticInstance = new EchoCoInServer(context);
        }
    }

    public static EchoCoInServer getInstance() {
        return sStaticInstance;
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
            mServerSocket.close();
        } catch (Exception e) {
            Log.d(TAG, "exception during termination or socket close");
            e.printStackTrace();
        }

        if(mEchoCoOutClient != null){
            mEchoCoOutClient.closeClientSocket();
            mEchoCoOutClient = null;
        }

    }

    public void run() {
        Log.d(TAG, "EchoCoInServer.run() , about create LLCP service socket");
        try {
            mServerSocket = mService.createLlcpServerSocket(
                EchoServerDefine.CO_SAP_IN, EchoServerDefine.CO_NAME_IN,
                EchoServerDefine.DEFAULT_LLCP_MIU, 1, 1024);
        } catch (LlcpException e) {
            e.printStackTrace();
            return;
        }
        if (mServerSocket == null) {
            Log.d(TAG, "failed to create LLCP service socket");
            return;
        }
        Log.d(TAG, "created LLCP service socket done");

        while (running) {
            try {
                Log.d(TAG, "about to accept");
                LlcpSocket clientSocket = mServerSocket.accept();
                Log.d(TAG, "accept returned " + clientSocket);
                handleDta2ClientByCoInServer(clientSocket);
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
            mServerSocket.close();
            mEchoCoOutClient = null;
        } catch (Exception e) {
            Log.d(TAG, "exception during termination or socket close");
            e.printStackTrace();
        }

        Log.d(TAG, "EchoCoInServer.run() , EXIT");


    }


    EchoCoOutClient mEchoCoOutClient;

    private void handleDta2ClientByCoInServer(LlcpSocket clientSocket) {

        mConnectToCoOutServerFail = false;
        mDataList.clear();

        /**
         * By NFC Forum Device Test Application, section 7.2.2
         * We should immediatly connect to out bound echo server
         */
        //CoEchoOutSequence echoOutSequence = new CoEchoOutSequence();
        //new Thread(echoOutSequence).start();
        //mEchoOutSequence = echoOutSequence;


        mEchoCoOutClient = new EchoCoOutClient(mContext,mLlcpPatternNumber);

        Log.d(TAG, "EchoCoOutClient connected ");


        boolean connectionBroken = false;
        // TODO::
        byte[] dataUnit = new byte[1024];

        while (!connectionBroken) {
            try {
                int size = clientSocket.receive(dataUnit);
                Log.d(TAG, "read " + size + " bytes, dataUnit = " + dataUnit.toString());
                if (size < 0) {
                    connectionBroken = true;
                    // TODO: should we close Server socket
                    closeInOutBoundSocket();
                    break;
                } else {
                    byte[] echoData = new byte[size];
                    System.arraycopy(dataUnit, 0, echoData, 0, size);
                    mHandler.sendMessageDelayed(mHandler.obtainMessage(1, echoData), 0);
                    synchronized (this) {
                        if (mDataList.size() > EchoServerDefine.MAX_ECHO_CO_BUFFER_SIZE) {
                            Log.d(TAG, "discard data units more then "
                                + EchoServerDefine.MAX_ECHO_CO_BUFFER_SIZE);
                        } else {
                            mDataList.add(echoData);

                            Log.d(TAG, "mDataList.size(): " + mDataList.size());
                            if (mDataList.size() == 1) {
                                //echoWithDelay();
                                //Let out boound to send

                                //byte[] data2Send = (byte[]) mDataList.get(mDataList.size()-1);
                                Log.d(TAG, "echoData: " + Util.printNdef(echoData));
                                Log.d(TAG, "call  EchoCoOutClient to send echo data" );

                                mEchoCoOutClient.setEchoData(echoData,size);
                                new Thread(mEchoCoOutClient).start();
                                mDataList.clear();
                                //mDataList.remove(data2Send);
                            }else{
                                Log.e(TAG, "!!!! mDataList.size() != 1, exception , do nothing");
                            }

                        }
                    }
                }

                /**
                 * if we received bytes, we should make the tester know
                 */
                if (mConnectToCoOutServerFail) {
                    Log.d(TAG, "connect To Co Out Server Fail");
                    throw new IOException();
                }

            } catch (IOException e) {
                connectionBroken = true;
                Log.d(TAG, "connection broken by IOException", e);
                e.printStackTrace();
                mEchoCoOutClient.closeClientSocket();
            } catch (Exception e) {
                connectionBroken = true;
                Log.d(TAG, "connection broken by Exception", e);
                e.printStackTrace();
                mEchoCoOutClient.closeClientSocket();
            }
        }


        Log.d(TAG, "not to clientSocket.close");
        /*
        try {

            clientSocket.close();
        } catch (Exception e) {
            Log.d(TAG, "exception during termination or socket close");
            e.printStackTrace();
        }
        */
    }

    /*
    private void handleClientByCoInServer(LlcpSocket clientSocket) {

        mConnectToCoOutServerFail = false;
        mDataList.clear();


         // By NFC Forum Device Test Application, section 7.2.2
         // We should immediatly connect to out bound echo server
         //
        CoEchoOutSequence echoOutSequence = new CoEchoOutSequence();
        new Thread(echoOutSequence).start();
        mEchoOutSequence = echoOutSequence;

        boolean connectionBroken = false;
        byte[] dataUnit = new byte[1024];

        while (!connectionBroken) {
            try {
                int size = clientSocket.receive(dataUnit);
                Log.d(TAG, "read " + size + " bytes, dataUnit = " + dataUnit.toString());
                if (size < 0) {
                    connectionBroken = true;
                    break;
                } else {
                    byte[] echoData = new byte[size];
                    System.arraycopy(dataUnit, 0, echoData, 0, size);
                    mHandler.sendMessageDelayed(mHandler.obtainMessage(1, echoData), 0);
                    synchronized (this) {
                        if (mDataList.size() > EchoServerDefine.MAX_ECHO_CO_BUFFER_SIZE) {
                            Log.d(TAG, "discard data units more then "
                            + EchoServerDefine.MAX_ECHO_CO_BUFFER_SIZE);
                        } else {
                            mDataList.add(echoData);
                            if (mDataList.size() == 1) {
                                echoWithDelay();
                            }
                        }
                    }
                }


                  //if we received bytes, we should make the tester know

                if (mConnectToCoOutServerFail) {
                    Log.d(TAG, "connect To Co Out Server Fail");
                    throw new IOException();
                }

            } catch (IOException e) {
                connectionBroken = true;
                Log.d(TAG, "connection broken by IOException", e);
                e.printStackTrace();
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

    public boolean handleMessage(Message message) {
        Log.d(TAG, "message.what = " + message.what);
        if (message.what == 0) {
            //if (mEchoOutSequence != null) {
            //    mEchoOutSequence.sendDataToRemote();
            //}
        } else if (message.what == 1) {
            Toast.makeText(mContext, "CO IN Server receive: " + new String((byte[]) message.obj)
                , Toast.LENGTH_SHORT).show();
        }
        return true;
    }


    /**
     * when InBound Socket.receive get receive size :-1 ,
     * we will close InBound Socket & OutBound Socket
     */
    private void closeInOutBoundSocket() {
        Log.d(TAG, "close CO InOutBoundSocket()");

        try {
            mServerSocket.close();
        } catch (Exception e) {
            Log.e(TAG, "exception during termination or socket close");
            e.printStackTrace();
        }

        if (mEchoCoOutClient != null)
            mEchoCoOutClient.closeClientSocket();

    }

    /*
    public void echoWithDelay() {
        mHandler.sendEmptyMessageDelayed(0, EchoServerDefine.CO_ECHO_OUT_DELAY);
    }
    */

    private final BroadcastReceiver mEchoCoInReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d(TAG, "mEchoCoInReceiver() rec. action:"+action);

            if (action.equals(EchoServerDefine.ACTION_SET_PATTERN_NUMBER)) {
                int totalPatternNumber = intent.getIntExtra(
                    EchoServerDefine.EXTRA_LLCP_PATTERN, EchoServerDefine.LLCP_PATTERN_INVALID);
                Log.d(TAG, " totalPatternNumber = " + totalPatternNumber);

                if(totalPatternNumber != EchoServerDefine.LLCP_PATTERN_INVALID){
                    mLlcpPatternNumber = totalPatternNumber & 0x0000FFFF;
                    Log.d(TAG, " mLlcpPatternNumber = " + mLlcpPatternNumber);
                    MtkNfcAddonSequence.getInstance().storeDtaPatternNumber(totalPatternNumber);
                }

            }
        }
    };


    /*
    public void onLlcpActivated() {
        Log.d(TAG, "onLlcpActivated");
        // dummy for now
    }

    public void onLlcpDeactivated() {
        Log.d(TAG, "onLlcpDeactivated");
        if (mEchoOutSequence != null) {
            mEchoOutSequence.terminateSequence();
            mEchoOutSequence = null;
        }
    }
    */


}
