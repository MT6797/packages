package com.mediatek.nfc.dta;

import android.os.UserHandle;

import android.content.Context;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
//import android.content.SharedPreferences;
import android.nfc.IAppCallback;
import android.util.Log;
import java.io.FileDescriptor;
//import java.io.IOException;
import java.io.PrintWriter;
//import com.android.nfc.handover.HandoverManager;
//import com.android.nfc.P2pLinkManagerCreator;
import com.mediatek.nfc.dta.Dta2EchoServer;

//import com.android.nfc.echoserver.EchoCoInServer;
//import com.android.nfc.echoserver.EchoCoOutServer;
//import com.android.nfc.echoserver.CTLEchoCoInServer;
//import com.android.nfc.echoserver.CTLEchoCoOutServer;
//import com.android.nfc.echoserver.EchoClients;

import android.nfc.BeamShareData;

import com.mediatek.nfc.handover.MtkNfcEntry;

import com.mediatek.nfc.snep.SnepTestSuite;

//import android.widget.Toast;
import com.mediatek.nfc.Util;

public class DtaP2pLinkManagerAdapter implements MtkNfcEntry.IP2pLinkManager {
    private static final String TAG = "DtaP2pLinkManagerAdapter";
    private static final boolean DBG = true;

    private Context mContext;
    private DtaClientManager mClientManager;
    final Dta2EchoServer mDta2EchoServer;
    private EchoCoInServer mEchoCoInServer;
    private EchoCoOutServer mEchoCoOutServer;
    private CTLEchoCoInServer mCTLEchoCoInServer;
    private CTLEchoCoOutServer mCTLEchoCoOutServer;
    private EchoClients mEchoClients;

    private IntentFilter mFilter;
    public static final String ACTION_DTA_TEST = "com.mediatek.nfc.dta.ACTION_DTA_TEST";
    public static final String EXTRA_TEST_TYPE = "com.mediatek.nfc.dta.EXTRA_TEST_TYPE";
    public static final String EXTRA_TEST_SCENARIO = "com.mediatek.nfc.dta.EXTRA_TEST_SCENARIO";
    public static final int TEST_SCENARIO_DEFAULT = 1;
    public static final int SNEP_TEST = 0;
    public static final int LLCP_TEST = 1;

    public static final String ACTION_LLCP_TEST = "com.mediatek.nfc.dta.ACTION_LLCP_TEST";
    public static final String EXTRA_TEST_LLCPLINK_TYPE =
        "com.mediatek.nfc.dta.EXTRA_TEST_LLCPLINK_TYPE";
    public static final int LLCP_CTO = 0;//Connection-Oriented
    public static final int LLCP_CTL = 1;//Connectionless

    private Intent mCachedDtaCmdIntent;

    private final BroadcastReceiver mDta2Receiver = new BroadcastReceiver() {
        @Override
            public void onReceive(Context context, Intent intent) {
                Log.d(TAG, "mDta2Receiver.receive :"+intent.getAction());
                mCachedDtaCmdIntent = intent;

                int requestCode = intent.getIntExtra(
                    "com.mediatek.nfc.dta.SNEP_CLIENT_REQ_CODE", -1);
                byte[] data = intent.getByteArrayExtra("com.mediatek.nfc.dta.SNEP_CLIENT_REQ_DATA");
                String sn = intent.getStringExtra("com.mediatek.nfc.dta.SNEP_CLIENT_SN");
                Log.d(TAG, " receive: CLIENT_REQUEST " + sn + ", req = " + requestCode);
                Log.d(TAG, " receive: data: " + Util.printNdef(data));
            }
    };

    public DtaP2pLinkManagerAdapter(Context context) {
        Log.d(TAG, "DtaP2pLinkManagerAdapter");

        mContext = context;
        mClientManager = new DtaClientManager(mContext);
        //mFilter = new IntentFilter(ACTION_SET_PATTERN_NUMBER);
        mDta2EchoServer = new Dta2EchoServer(context);

        //mContext.registerReceiver(mDta2Receiver, mFilter);
        SnepTestSuite.initialize(context);

            IntentFilter filter = new IntentFilter();
            for (String actionString : SnepTestSuite.getRelatedIntentActions()) {
                filter.addAction(actionString);
            }

            mContext.registerReceiver(mDta2Receiver, filter);
    }

    public void enableDisable(boolean sendEnable, boolean receiveEnable) {
        if (DBG) Log.d(TAG, "enableDisable : sendEnable="
            + sendEnable + ", receiveEnable=" + receiveEnable);

        if (receiveEnable) {
            mClientManager.enable();
            mDta2EchoServer.enable();
            SnepTestSuite.startServers();


        } else {
            mClientManager.disable();
            mDta2EchoServer.disable();
            SnepTestSuite.stopServers();

        }
    }

        public void onLlcpActivated(byte peerLlcpVersion) {
            mClientManager.onLlcpActivated();
            if (DBG) Log.d(TAG, "onLlcpActivated");
            sendLlcpNotify(true);

            synchronized (this) {
                if (mCachedDtaCmdIntent != null) {
                    boolean processed = SnepTestSuite.onLlcpActivated(
                        mContext, mCachedDtaCmdIntent);
                    Log.d(TAG, "processed by SnepTestSuite ? " + processed);
                } else {
                    Log.d(TAG, "no cmd has been set");
                }

                if (mDta2EchoServer != null) {
                    mDta2EchoServer.onLlcpActivated();
                }
                if (mEchoClients != null) {
                    mEchoClients.onLlcpActivated();
                }

            }
        }

        public void onLlcpDeactivated() {
            mClientManager.onLlcpDeactivated();
            if (DBG) Log.d(TAG, "onLlcpDeactivated");
            sendLlcpNotify(false);

            synchronized (this) {
                mCachedDtaCmdIntent = null;
                if (mDta2EchoServer != null) {
                    mDta2EchoServer.onLlcpDeactivated();
                }

                if (mEchoClients != null) {
                    mEchoClients.onLlcpDeactivated();
                }
            }
        }

    public void setNdefCallback(IAppCallback callbackNdef, int callingUid) {
        if (DBG) Log.d(TAG, "setNdefCallback");
        /// dummy
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        if (DBG) Log.d(TAG, "dump");
        /// dummy
    }

    public void onLlcpFirstPacketReceived(){
        if (DBG) Log.d(TAG, "onLlcpFirstPacketReceived");
        /// dummy

    }
    public void onUserSwitched(int userId){
        if (DBG) Log.d(TAG, "onUserSwitched userId:"+userId);
        /// dummy


    }

    public void onManualBeamInvoke(BeamShareData shareData){
        if (DBG) Log.d(TAG, "onManualBeamInvoke shareData:"+shareData);
        /// dummy

    }
    public int getP2pState(){
        if (DBG) Log.d(TAG, "getP2pState()   return 1");
        return 1;
        /// dummy

    }




    //LLCP Activated     : 1
    //LLCP DeActivated : 0
    private void sendLlcpNotify (boolean activated) {

        Log.d(TAG, "sendLlcpNotify() activated:"+activated);

        Intent intent = new Intent(EchoServerDefine.ACTION_LLCP_NOTIFY);
        intent.putExtra(EchoServerDefine.EXTRA_LLCP_STATUS, activated?1:0);
        mContext.sendBroadcastAsUser(intent, UserHandle.CURRENT);

    }




}
