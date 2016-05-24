package com.mediatek.nfc.dta;

import com.android.nfc.DeviceHost.LlcpConnectionlessSocket;
//import com.android.nfc.LlcpException;
//import com.android.nfc.DeviceHost.LlcpServerSocket;
import com.android.nfc.DeviceHost.LlcpSocket;
//import com.android.nfc.LlcpPacket;
import com.android.nfc.NfcService;

//import android.os.Handler;
//import android.os.Message;
import android.util.Log;

import java.io.IOException;
//import java.util.concurrent.BlockingQueue;
//import java.util.concurrent.LinkedBlockingQueue;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import java.util.List;
import java.util.ArrayList;

import com.mediatek.nfc.dta.EchoServerDefine;
import com.mediatek.nfc.dta.DtaP2pLinkManagerAdapter;
//import android.content.IntentFilter;

public class EchoClients {
    static private final String TAG = "NfcEchoCoClient";

    private boolean mIsClientWorking;
    private Context mContext;
    private int llcplink_Type;
    private IntentFilter mFilter;

    public EchoClients(Context context) {
        mContext = context;
        IntentFilter filter = new IntentFilter();
        mFilter = new IntentFilter(DtaP2pLinkManagerAdapter.ACTION_LLCP_TEST);
        for (String action : EchoServerDefine.mPlugfestCases) {
            filter.addAction(action);
        }
        mContext.registerReceiver(mReceiver, filter);
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(DtaP2pLinkManagerAdapter.ACTION_LLCP_TEST)) {
                llcplink_Type = intent.getIntExtra(
                    DtaP2pLinkManagerAdapter.EXTRA_TEST_LLCPLINK_TYPE,
                    DtaP2pLinkManagerAdapter.LLCP_CTO);
            }
            Log.d(TAG, "LLCP link type: " + llcplink_Type);
        }
    };

    public void enableDisable(boolean clientEnable) {
        Log.d(TAG, "EchoClients enableDisable : " + clientEnable);

        if (clientEnable) {
            mContext.registerReceiver(mReceiver, mFilter);

        } else {
            mContext.unregisterReceiver(mReceiver);
        }
    }

    public void onLlcpActivated() {
        Log.d(TAG, "onLlcpActivated");
        if (mIsClientWorking) {
            Log.d(TAG, "prevous client is still working");
        } else if (llcplink_Type == 0){
            Log.d(TAG, "start new ConnectRemoteCoEchoServerSequence");
            mIsClientWorking = true;
            new Thread(new ConnectRemoteCoEchoServerSequence()).start();
        } else {
            Log.d(TAG, "start new ConnectCTLCoEchoServerSequence");
            mIsClientWorking = true;
            new Thread(new ConnectCTLCoEchoServerSequence()).start();
        }
    }

    public void onLlcpDeactivated() {
        Log.d(TAG, "onLlcpDeactivated");
        mIsClientWorking = false;
    }

    class ConnectRemoteCoEchoServerSequence implements Runnable {
        private LlcpSocket mSocket;
        static private final String HELLO_MESSAGE = "CO ECHO client sending test data by CTO";

        public void run() {
            try {
                mSocket = NfcService.getInstance().createLlcpSocket(
                    0, EchoServerDefine.DEFAULT_LLCP_MIU,
                    EchoServerDefine.DEFAULT_LLCP_RWSIZE, 1024);
                if (mSocket == null) {
                    throw new IOException("Could not connect to socket.");
                }
                Log.d(TAG, "about to connect to service :" + EchoServerDefine.CO_NAME_IN);
                mSocket.connectToService(EchoServerDefine.CO_NAME_IN);
                Log.d(TAG, "connect done");
                mSocket.send(HELLO_MESSAGE.getBytes());
                Log.d(TAG, "send done");
            } catch (Exception e) {
                Log.d(TAG, "fail to connect to service :" + EchoServerDefine.CO_NAME_IN);
                e.printStackTrace();
            } finally {
                if (mSocket != null) {
                    try {
                        mSocket.close();
                    } catch (Exception e) {
                        Log.d(TAG, "wtf!? exception during close!?");
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    class ConnectCTLCoEchoServerSequence implements Runnable {
        private LlcpConnectionlessSocket mSocket;
        static private final String HELLO_MESSAGE = "CO ECHO client sending test data by CTL";

        public void run() {
            try {
                    mSocket = NfcService.getInstance().createLlcpConnectionLessSocket(
                        EchoServerDefine.CL_SAP_IN, EchoServerDefine.CL_NAME_IN);
                if (mSocket == null) {
                    throw new IOException("Could not connect to CTL socket.");
                }
                Log.d(TAG, "CTL socket created done");
                //mSocket.send(mSocket.getSap(),HELLO_MESSAGE.getBytes());
                mSocket.send(EchoServerDefine.CL_SAP_IN,HELLO_MESSAGE.getBytes());
                Log.d(TAG, "send done");
            } catch (Exception e) {
                Log.d(TAG, "fail CTL socket to service :" + EchoServerDefine.CL_NAME_IN);
                e.printStackTrace();
            } finally {
                if (mSocket != null) {
                    try {
                        mSocket.close();
                    } catch (Exception e) {
                        Log.d(TAG, "wtf!? exception during close!?");
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}