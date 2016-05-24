/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 *
 * MediaTek Inc. (C) 2010. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER ON
 * AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL WARRANTIES,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NONINFRINGEMENT.
 * NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH RESPECT TO THE
 * SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY, INCORPORATED IN, OR
 * SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES TO LOOK ONLY TO SUCH
 * THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. RECEIVER EXPRESSLY ACKNOWLEDGES
 * THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES
 * CONTAINED IN MEDIATEK SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK
 * SOFTWARE RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND
 * CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE RELEASED HEREUNDER WILL BE,
 * AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE,
 * OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE CHARGE PAID BY RECEIVER TO
 * MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek Software")
 * have been modified by MediaTek Inc. All revisions are subject to any receiver's
 * applicable license agreements with MediaTek Inc.
 */

/*****************************************************************************
*  Copyright Statement:
*  --------------------
*  This software is protected by Copyright and the information contained
*  herein is confidential. The software may not be copied and the information
*  contained herein may not be used or disclosed except with the written
*  permission of MediaTek Inc. (C) 2005
*
*  BY OPENING THIS FILE, BUYER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
*  THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
*  RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO BUYER ON
*  AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL WARRANTIES,
*  EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF
*  MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NONINFRINGEMENT.
*  NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH RESPECT TO THE
*  SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY, INCORPORATED IN, OR
*  SUPPLIED WITH THE MEDIATEK SOFTWARE, AND BUYER AGREES TO LOOK ONLY TO SUCH
*  THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. MEDIATEK SHALL ALSO
*  NOT BE RESPONSIBLE FOR ANY MEDIATEK SOFTWARE RELEASES MADE TO BUYER'S
*  SPECIFICATION OR TO CONFORM TO A PARTICULAR STANDARD OR OPEN FORUM.
*
*  BUYER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND CUMULATIVE
*  LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE RELEASED HEREUNDER WILL BE,
*  AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE,
*  OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE CHARGE PAID BY BUYER TO
*  MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
*
*  THE TRANSACTION CONTEMPLATED HEREUNDER SHALL BE CONSTRUED IN ACCORDANCE
*  WITH THE LAWS OF THE STATE OF CALIFORNIA, USA, EXCLUDING ITS CONFLICT OF
*  LAWS PRINCIPLES.  ANY DISPUTES, CONTROVERSIES OR CLAIMS ARISING THEREOF AND
*  RELATED THERETO SHALL BE SETTLED BY ARBITRATION IN SAN FRANCISCO, CA, UNDER
*  THE RULES OF THE INTERNATIONAL CHAMBER OF COMMERCE (ICC).
*
*****************************************************************************/
/*******************************************************************************
 *
 * Filename:
 * ---------
 * BluetoothDunService.java
 *
 * Project:
 * --------
 *   BT Project
 *
 * Description:
 * ------------
 *   This file is used to provide DUN service api
 *
 * Author:
 * -------
 * Ting Zheng
 *
 *==============================================================================
 *             HISTORY
 * Below this line, this part is controlled by PVCS VM. DO NOT MODIFY!!
 *------------------------------------------------------------------------------
 * $Revision:
 * $Modtime:
 * $Log:
 *------------------------------------------------------------------------------
 * Upper this line, this part is controlled by PVCS VM. DO NOT MODIFY!!
 *==============================================================================
 *******************************************************************************/
package com.android.bluetooth.dun;

//import com.mediatek.bluetooth.BluetoothProfileManager;
//import com.mediatek.bluetooth.BluetoothProfileManager.Profile;
//import com.mediatek.bluetooth.BluetoothProfile;
//import com.mediatek.bluetooth.BluetoothTethering;

import android.app.Service;

import android.bluetooth.BluetoothDun;
import android.bluetooth.IBluetoothDun;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.util.Log;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.NetworkInfo.State;
import android.telephony.TelephonyManager;
import android.os.IBinder;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.bluetooth.BluetoothUuid;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.os.INetworkManagementService;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.os.Handler;
import android.os.Message;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

import java.io.OutputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.io.IOException;

import com.android.internal.telephony.Phone;
import com.android.bluetooth.BluetoothTethering;
import com.android.bluetooth.R;

//import android.net.INetworkManagementEventObserver;


public class BluetoothDunService extends Service {
    private static final String TAG = "BluetoothDunService";
    private static final boolean DBG = true;
    private static final boolean VERBOSE = true;

    private static int PPP_MAX_PKT_LEN = 32764;
    private static int PKT_TYPE_UNKNOWN = -1;
    private static int PKT_TYPE_AT = 1;
    private static int PKT_TYPE_PPP = 2;

    /**
     * State definition of DUN Service
     */
    private static int DUN_SERVICE_STATE_IDLE = 0;
    private static int DUN_SERVICE_STATE_LISTEN = 1;
    private static int DUN_SERVICE_STATE_AUTHORIZE = 2;
    private static int DUN_SERVICE_STATE_DIALING = 3;
    private static int DUN_SERVICE_STATE_PPP = 4;
    private static int DUN_SERVICE_STATE_HANGUP = 5;
    private static int DUN_SERVICE_STATE_DISCONNECTING = 6;

    /**
     * Intent indicating incoming connection request which is sent to
     * BluetoothSimapActivity
     */
    public static final String ACCESS_REQUEST_ACTION = "com.android.bluetooth.dun.accessrequest";

    /**
     * Intent indicating incoming connection request access action by user which is
     * sent from BluetoothDunActivity
     */
    public static final String ACCESS_RESPONSE_ACTION = "com.android.bluetooth.dun.accessresponse";

    /**
     * Intent indicating resend incoming connection notification action by user which is
     * sent from BluetoothDunActivity
     */
    public static final String RESEND_NOTIFICATION_ACTION =
        "com.android.bluetooth.dun.resendnotification";

    /**
     * Intent indicating incoming connection request access action result by user which is
     * sent from BluetoothDunActivity
     */
    public static final String EXTRA_ACCESS_RESULT = "com.android.bluetooth.dun.accessresult";

    /* Result codes */
    public static final int RESULT_USER_ACCEPT = 1;
    public static final int RESULT_USER_REJECT = 2;

    public static final String EXTRA_DEVICE = "com.android.bluetooth.dun.device";

    /**
     * the intent that gets sent when deleting the notification
     */
    public static final String ACTION_CLEAR_AUTH_NOTIFICATION =
        "com.android.bluetooth.dun.intent.action.CLEAR_AUTH";

    private static final String BLUETOOTH_PERM = android.Manifest.permission.BLUETOOTH;


    /* Start of DUN ID space */
    private static final int DUN_ID_START = -1000010;

    /* DUN Notification IDs, it shall be unique within one app */
    private static final int DUN_AUTHORIZE_NOTIFY = DUN_ID_START + 1;

    /* connection timeout in milliseconds */
    private static final int DUN_CONN_TIMEOUT = 60000;
    private static final int DUN_TETHER_RETRY = 500;

    /* message */
    private static final int MESSAGE_CONNECT_TIMEOUT = 1;
    private static final int MESSAGE_TETHER_RETRY = 2;

    private Context mContext;
    private BluetoothAdapter mAdapter;
    private BluetoothDevice mRemoteDevice;
    private boolean mHasInitiated = false;
    private int mStartId = -1;
    /**
     * Service state
     */
    private int mDunState = DUN_SERVICE_STATE_IDLE;


    private int mConnectionState = BluetoothDun.STATE_DISCONNECTED;
    private String mDunConnPath;
    //private String[] mDnsServers;
    private static final String DUN_Profile = "BluetoothDun";

    private static final String BLUETOOTH_IFACE_ADDR_START= "192.168.44.1";

    //private static BluetoothTethering mBTtethering;

    private boolean mTetheringOn;
    private static final String DUN_PREFERENCE_FILE = "DUNMGR";
    private static final String DUN_TETHER_SETTING = "TETHERSTATE";

    private BroadcastReceiver mTetheringReceiver = null;

    /**
     * Rfcomm server socket
     */
    private volatile BluetoothServerSocket mRfcommServerSocket = null;

    /**
     * Connected Rfcomm socket
     */
    private volatile BluetoothSocket mRfcommSocket = null;
    private OutputStream mRfcommOutputStream = null;

    private RfcommListenThread mListenThread = null;

    private UplinkDataThread mUplinkThread = null;

    private DownlinkDataThread mDownlinkThread = null;

    private volatile LocalSocket mPppdSocket = null;
    private OutputStream mPppdOutputStream = null;

    private class DUNPacket {
        public int mType;
        public byte[] mBuffer;
        public DUNPacket() {
            mType = PKT_TYPE_UNKNOWN;
            mBuffer = null;
        }
        public DUNPacket(int type, byte[] buffer) {
            mType = type;
            mBuffer = buffer;
        }
    }
    /**
     * Fragment ppp packets
     */
    private class DUNPacketReader {
        private byte[] mBuffer = null;
        private InputStream mInputStream = null;

        public DUNPacketReader(InputStream input) {
            if (input != null) {
                mInputStream = input;
                mBuffer = new byte[PPP_MAX_PKT_LEN];
            }
        }

        // Caller have to catche IOException to detect stream closed
        DUNPacket readPacket() throws IOException {
            int ret = 0;
            int i;
            byte c;
            DUNPacket pkt = null;

            //Log.d(TAG, "readPacket");
            //
            c = (byte)mInputStream.read();
            //Log.d(TAG, "first byte is " + c);
            mBuffer[0] = c;
            if (c == 0x7E) {
                for (i = 1; i < mBuffer.length; i++) {
                    mBuffer[i] = (byte)mInputStream.read();
                    if (mBuffer[i] == 0x7D) {
                        i++;
                        mBuffer[i] = (byte)mInputStream.read();
                    } else if (mBuffer[i] == 0x7E) {
                        //Log.d(TAG, "PPP terminated char found");
                        pkt = new DUNPacket();
                        pkt.mType = PKT_TYPE_PPP;
                        try {
                            pkt.mBuffer = Arrays.copyOf(mBuffer, i+1);
                        } catch (Exception ex) {
                            Log.e(TAG, "Arrays.CopyOf failed"+ex.toString());
                        }
                        break;
                    } else {
                    }
                }
            } else if (c == 'A' || c == 'a') {
                for (i = 1; i < mBuffer.length; i++) {
                    mBuffer[i] = (byte)mInputStream.read();
                    if (mBuffer[i] == 0x0D) {
                        //Log.d(TAG, "Terminate AT found");
                        pkt = new DUNPacket();
                        pkt.mType = PKT_TYPE_AT;
                        try {
                            pkt.mBuffer = Arrays.copyOf(mBuffer, i+1);
                        } catch (Exception ex) {
                            Log.e(TAG, "Arrays.CopyOf failed"+ex.toString());
                        }
                        break;
                    }
                }
            } else {
                Log.w(TAG, "Invalid packet, first byte is "+c);
                throw new IOException("Invalid packet format");
            }
            return pkt;
        }
    }

    private int sendRfcommPacket(byte[] buffer) {
        int ret = -1;
        synchronized (mRfcommSocket) {
            log("sendRfcommPacket : length="+buffer.length);
            try {
                if (mRfcommSocket != null && mRfcommOutputStream != null &&
                        mRfcommSocket.isConnected()) {
                    log("Start Write Rfcomm output Stream: length="+buffer.length);
                    mRfcommOutputStream.write(buffer);
                    mRfcommOutputStream.flush();
                    ret = buffer.length;
                }
            } catch (IOException ex) {
                Log.e(TAG, "sendRfcommPacket failed: " + ex.toString());
            }
        }
        return ret;
    }

    private int sendPppPacket(byte[] buffer) {
        int ret = -1;
        synchronized (mPppdSocket) {
            //log("sendPppPacket : length="+buffer.length);
            try {
                if (mPppdSocket != null && mPppdOutputStream != null &&
                        mPppdSocket.isConnected()) {
                    mPppdOutputStream.write(buffer);
                    mPppdOutputStream.flush();
                    ret = buffer.length;
                }
            } catch (IOException ex) {
                Log.e(TAG, "sendPppPacket failed: " + ex.toString());
            }
        }
        return ret;

    }

    /**
     * A thread that runs in the background waiting for remote rfcomm
     * connect.Once a remote socket connected, this thread shall be
     * shutdown.When the remote disconnect,this thread shall run again
     * waiting for next request.
     */
    private class RfcommListenThread extends Thread {

        private boolean stopped = false;

        @Override
        public void run() {
            BluetoothServerSocket listenSocket;
            try {
                if (mRfcommServerSocket == null) {
                    mRfcommServerSocket = mAdapter.listenUsingEncryptedRfcommWithServiceRecord(
                            "Dial up Networking", BluetoothUuid.DUN.getUuid());
                }
            } catch (IOException ex) {
                stopped = true;
                Log.e(TAG, "listenUsingEncryptedRfcommWithServiceRecord failed: " + ex.toString());
            }

            while (!stopped) {
                try {
                    mRfcommSocket = mRfcommServerSocket.accept();

                    synchronized (BluetoothDunService.this) {
                        if (mRfcommSocket == null) {
                            Log.w(TAG, " mRfcommSocket is null");
                            break;
                        }
                        mRemoteDevice = mRfcommSocket.getRemoteDevice();
                    }
                    if (mRemoteDevice == null) {
                        /* close the rfcomm socket */
                        mRfcommSocket.close();
                        mRfcommSocket = null;
                        Log.i(TAG, "getRemoteDevice() = null");
                        // Continue to Listen
                        //break;
                    } else {
                        stopped = true; // job done ,close this thread;
                        mRfcommOutputStream = mRfcommSocket.getOutputStream();
                        //onDunConnectReq(mRemoteDevice.getAddress());
                        // Try to auto accept DUN connection request
                        Intent intent = new Intent(BluetoothDunService.ACCESS_RESPONSE_ACTION);
                        Log.d(TAG, "incoming connection, mTetheringOn = "+mTetheringOn);
                        if (mTetheringOn) {
                            intent.putExtra(BluetoothDunService.EXTRA_ACCESS_RESULT,
                                    BluetoothDunService.RESULT_USER_ACCEPT);
                        } else {
                            intent.putExtra(BluetoothDunService.EXTRA_ACCESS_RESULT,
                                    BluetoothDunService.RESULT_USER_REJECT);
                        }
                        sendBroadcast(intent);
                        if (VERBOSE) Log.v(TAG, "RfcommListenThread stopped ");
                    }
                } catch (IOException ex) {
                    stopped=true;
                    if (VERBOSE) Log.v(TAG, "Listen thread exception: " + ex.toString());
                }
            }
            //disconnect();
            //mRfcommServerSocket.close();
            //mRfcommServerSocket = null;
        }

        void shutdown() {
            stopped = true;
            interrupt();
        }
    }

    private void startRfcommListenerThread() {
        if (VERBOSE) Log.v(TAG, "DUN Service startRfcommListenerThread");

        synchronized(BluetoothDunService.this) {
            if (mListenThread != null) {
                try {
                    mListenThread.shutdown();
                    mListenThread.join();
                    mListenThread = null;
                } catch (InterruptedException ex) {
                    Log.w(TAG, "mListenThread close error" + ex);
                }
            }
            if (mListenThread == null) {
                mListenThread = new RfcommListenThread();
                mListenThread.setName("DUNRfcommListenThread");
                mListenThread.start();
            }
        }
    }

    /**
     *  Thread which transfer data from rfcomm to ppp daemon
     */
    private class UplinkDataThread extends Thread {

        private OutputStream mPppOutputStream = null;
        private InputStream mRfcommInputStream = null;

        private DUNPacketReader mPacketReader = null;

        private boolean stopped = false;

        String[] AT_DunCmds = {
            "&C",    // Circuit 109 (Received line signal detector) (DCD)
            "&D",    // Circuit 108 (Data terminal ready) (DTR)
            "&F",    // Set to Factory-defined Configuration
            "+GCAP", // Request Complete Capabilities List
            "+GMI",  // Request Manufacturer Identification
            "+GMM",  // Request Model Identification
            "+GMR",  // Request Revision Identification
            "A",     // Answer
            "D",     // Dial
            "E",     // Command Echo
            "H",     // Hook Control
            "L",     // Monitor Speaker Loudness
            "M",     // Monitor Speaker Mode
            "O",     // Return to Online Data
            "P",     // Select Pulse Dialling
            "Q",     // Result Code Suppression
            "S0",    // Automatic Answer
            "S10",   // Automatic Disconnect Delay
            "S3",    // Command Line Termination Character
            "S4",    // Response Formatting Character
            "S5",    // Command Line Editing Character
            "S6",    // Pause Before Blind Dialling
            "S7",    // Connection Completion Timeout (may be ignored)
            "S8",    // Comma Dial Modifier Time
            "T",     // Select Tone Dialling
            "V",     // DCE Response Format
            "X",     // Result Code Selection and Call Progress Monitoring Control
            "Z",     // Reset To Default Configuration
        };

        private String handleATCommands(String atCommand) {
            String response = "\r\nERROR\r\n";
            log("handleATCommands : "+atCommand);
            atCommand = atCommand.trim().toUpperCase();
            if (atCommand.startsWith("AT") == true) {
                if(atCommand.substring(2).equals("")) {
                    response = "\r\nOK\r\n";
                } else {
                    for (int i = 0; i < AT_DunCmds.length; i++) {
                        if (atCommand.substring(2).startsWith(AT_DunCmds[i])) {
                            if(AT_DunCmds[i].equals("D")) {
                                response = onDunDialupReq();
                            } else {
                                response = "\r\nOK\r\n";
                            }
                            break;
                        }
                    }
                }
            }
            log("handleATCommands : resp="+response);
            return response;
        }

        @Override
        public void run() {
            DUNPacket pkt = null;
            String response;
            try {
                mRfcommInputStream = mRfcommSocket.getInputStream();
                mPacketReader = new DUNPacketReader(mRfcommInputStream);
            } catch (IOException ex) {
                log("UplinkDataThread exception: " + ex.toString());
                stopped = true;
            }
            while (!stopped) {
                try {
                    pkt = mPacketReader.readPacket();
                    if (pkt.mType == PKT_TYPE_AT) {
                        response = handleATCommands(new String(pkt.mBuffer));
                        if (response != null) {
                            if (response.equals("\r\nCONNECT\r\n")) {
                                LocalSocketAddress locSockAddr = new LocalSocketAddress(
                                        "ppp.bt.dun");
                                mPppdSocket = new LocalSocket();
                                mPppdSocket.connect(locSockAddr);
                                mPppdOutputStream = mPppdSocket.getOutputStream();
                                startDownlinkDataThread();
                            } else {
                            }
                            if (sendRfcommPacket(response.getBytes()) < 0) {
                                stopped=true;
                                log("sendRfcommPacket failed");
                            }
                        }
                    }
                    if(pkt.mType == PKT_TYPE_PPP) {
                        if (sendPppPacket(pkt.mBuffer) < 0) {
                            log("sendPppPacket failed");
                        }
                    }
                } catch (IOException ex) {
                    stopped=true;
                    if (VERBOSE) Log.v(TAG, "Uplink thread exception: " + ex.toString());
                }
            }
            disconnect(true);
        }

        void shutdown() {
            stopped = true;
            interrupt();
        }
    }

    private void startUplinkDataThread() {
        if (VERBOSE) Log.v(TAG, "DUN Service startUplinkDataThread");

        synchronized(BluetoothDunService.this) {
            if (mUplinkThread != null) {
                try {
                    mUplinkThread.shutdown();
                    mUplinkThread.join();
                    mUplinkThread = null;
                } catch (InterruptedException ex) {
                    Log.w(TAG, "mUplinkThread close error" + ex);
                }
            }
            if (mUplinkThread == null) {
                mUplinkThread = new UplinkDataThread();
                mUplinkThread.setName("DUNUplinkDataThread");
                mUplinkThread.start();
            }
        }
    }


    /**
     *  Thread which transfer data from ppp daemon to rfcomm
     */
    private class DownlinkDataThread extends Thread {

        private InputStream mPppdInputStream = null;

        private DUNPacketReader mPacketReader = null;

        private boolean stopped = false;

        @Override
        public void run() {
            DUNPacket pkt = null;
            try {
                mPppdInputStream = mPppdSocket.getInputStream();
                mPacketReader = new DUNPacketReader(mPppdInputStream);
            } catch (IOException ex) {
                log("DownlinkDataThread exception: " + ex.toString());
                stopped = true;
            }
            while (!stopped) {
                try {
                    log("Start read ppp packet");
                    pkt = mPacketReader.readPacket();
                    if(pkt != null && pkt.mBuffer != null)
                        log("Read ppp packet : len="+pkt.mBuffer.length);
                    if(pkt.mType == PKT_TYPE_PPP) {
                        if (sendRfcommPacket(pkt.mBuffer) < 0) {
                            log("sendPppPacket failed");
                            stopped=true;
                        }
                    } else {
                        Log.e(TAG, "Invalid PPP packet format");
                    }
                } catch (IOException ex) {
                    stopped=true;
                    if (VERBOSE) Log.v(TAG, "Down thread exception: " + ex.toString());
                }
            }
            //disconnect();
        }

        void shutdown() {
            stopped = true;
            interrupt();
        }
    }

    private void startDownlinkDataThread() {
        if (VERBOSE) Log.v(TAG, "DUN Service startDownlinkDataThread");

        synchronized(BluetoothDunService.this) {
            if (mDownlinkThread != null) {
                try {
                    mDownlinkThread.shutdown();
                    mDownlinkThread.join();
                    mDownlinkThread = null;
                } catch (InterruptedException ex) {
                    Log.w(TAG, "mUplinkThread close error" + ex);
                }
            }
            if (mDownlinkThread == null) {
                mDownlinkThread = new DownlinkDataThread();
                mDownlinkThread.setName("DownlinkDataThread");
                mDownlinkThread.start();
            }
        }
    }


    private int disconnect(boolean restartListener) {
        synchronized(BluetoothDunService.this) {
            log("disconnect : state is "+mConnectionState);
            if (mConnectionState == BluetoothDun.STATE_CONNECTED ||
                    mConnectionState == BluetoothDun.STATE_CONNECTING) {
                removeDunAuthNotification(DUN_AUTHORIZE_NOTIFY);
                dunSetState(BluetoothDun.STATE_DISCONNECTING);
                // Close rfcomm socket
                if(mRfcommSocket != null) {
                    log("Close rfcomm socket");
                    try {
                        try {
                            if (mRfcommOutputStream != null)
                                mRfcommOutputStream.close();
                        } catch (IOException ex) {
                            Log.w(TAG, "mRfcommOutputStream.close failed " + ex.toString());
                        }
                        mRfcommOutputStream = null;
                        mRfcommSocket.close();
                        mRfcommSocket = null;
                    } catch (IOException ex) {
                        Log.e(TAG, "mRfcommSocket.close failed " + ex.toString());
                    }
                    mRfcommSocket = null;
                }
                // Close ppp socket
                if(mPppdSocket != null) {
                    log("Close pppd socket");
                    try {
                        if (mPppdOutputStream != null)
                            mPppdOutputStream.close();
                    } catch (IOException ex) {
                        Log.w(TAG, "mPppdOutputStream.close failed " + ex.toString());
                    }
                    mPppdOutputStream = null;
                    try {
                        mPppdSocket.close();
                    } catch (IOException ex) {
                        Log.e(TAG, "mPppdSocket.close failed " + ex.toString());
                    }
                    mPppdSocket = null;
                }
                // Set state to disconnected
                dunSetState(BluetoothDun.STATE_DISCONNECTED);
                if (restartListener) {
                    startRfcommListenerThread();
                }
            }
        }
        return mConnectionState;
    }


    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (DBG) log("Receive intent action = " + action);
            if (BluetoothDunService.ACCESS_RESPONSE_ACTION.equals(action)) {
                int result = intent.getIntExtra(BluetoothDunService.EXTRA_ACCESS_RESULT,
                                                BluetoothDunService.RESULT_USER_REJECT);
                if (result == BluetoothDunService.RESULT_USER_ACCEPT) {
                    //dunConnectRspNative(mDunConnPath, true);
                    dunSetState(BluetoothDun.STATE_CONNECTED);
                    startUplinkDataThread();
                } else {
                    //dunConnectRspNative(mDunConnPath, false);
                    //dunSetState(BluetoothDun.STATE_DISCONNECTED);
                    disconnect(true);
                }
            } else if (BluetoothDunService.RESEND_NOTIFICATION_ACTION.equals(action)) {
                BluetoothDevice device = mAdapter.getRemoteDevice(mDunConnPath);
                createDunAuthNotification(context, device, true);
            } else if (ConnectivityManager.CONNECTIVITY_ACTION.equals(action)) {
                ConnectivityManager connmgr;

                connmgr = (ConnectivityManager)mContext.getSystemService(
                        Context.CONNECTIVITY_SERVICE);
                NetworkInfo MobileInfo = connmgr.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
                NetworkInfo WifiInfo = connmgr.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

                NetworkInfo.DetailedState MobileState = NetworkInfo.DetailedState.IDLE;
                NetworkInfo.DetailedState WifiState = NetworkInfo.DetailedState.IDLE;

                if(MobileInfo!=null)
                    MobileState=MobileInfo.getDetailedState();
                if(WifiInfo!=null)
                    WifiState=WifiInfo.getDetailedState();
                if (DBG) log("NetworkInfo broadcast, MobileState=" + MobileState + ",WifiState="
                        + WifiState);

                /* if get network service via wifi, get interface name by "wifi.interface" system
                *  property key String interfaceName = SystemProperties.get("wifi.interface",
                *  "tiwlan0");
                *  log("wifi interface name=" + interfaceName);
                */

                if (MobileState == NetworkInfo.DetailedState.IDLE &&
                        WifiState == NetworkInfo.DetailedState.IDLE) {
                    if (mConnectionState == BluetoothDun.STATE_CONNECTED) {
                        //dunDisconnectNative();
                        log("Network state changed to IDLE. DIsconnect DUN");
                        disconnect(true);
                    }
                }

                if (MobileState == NetworkInfo.DetailedState.DISCONNECTED) {
                    if (mConnectionState == BluetoothDun.STATE_CONNECTED) {
                        //dunNotifyNetworkTerminated();
                        log("Lost network connection. Send NO CARRIER to remote");
                        sendRfcommPacket(new String("\r\nNO CARRIER\r\n").getBytes());
                    }
                }
            } else if(BluetoothTethering.BLUETOOTH_INTERFACE_ADDED.equals(action)) {
                if (DBG) log("receiver BluetoothTethering.BLUETOOTH_INTERFACE_ADDED");
                if(mConnectionState == BluetoothDun.STATE_CONNECTED) {
                    String iface = intent.getStringExtra(
                            BluetoothTethering.BLUETOOTH_INTERFACE_NAME);
                    if (DBG) log("iface"+iface);
                } else {
                    if (DBG) log("DUN does not connected");
                }
            } else if(BluetoothTethering.BLUETOOTH_INTERFACE_LINK_UP.equals(action)) {
                if (DBG) log("receiver BluetoothTethering.BLUETOOTH_INTERFACE_INK_UP");
                if(mConnectionState == BluetoothDun.STATE_CONNECTED) {
                    String iface = intent.getStringExtra(
                            BluetoothTethering.BLUETOOTH_INTERFACE_NAME);
                    boolean up = intent.getBooleanExtra(
                            BluetoothTethering.BLUETOOTH_INTERFACE_STATE, false);
                    if (DBG) log("iface:"+iface+" up:"+up);
                    if(iface.equals("bt-dun") && up) {
                        ConnectivityManager cm = (ConnectivityManager)mContext.getSystemService(
                                                     Context.CONNECTIVITY_SERVICE);
                        if (cm.tether(iface) != ConnectivityManager.TETHER_ERROR_NO_ERROR) {
                            Log.e(TAG, "Error tethering "+iface+", retry...");
                            Message msg = Message.obtain(mHandler, MESSAGE_TETHER_RETRY);
                            msg.obj = iface;
                            mHandler.sendMessageDelayed(msg, DUN_TETHER_RETRY);
                        } else {
                            log("Tethering bt-dun success");
                        }
                    }
                } else {
                    if (DBG) log("DUN does not connected");
                }
            } else if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                if (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                        == BluetoothAdapter.STATE_TURNING_OFF) {
                    clearService();
                }

            }
        }
    };

    /* Proxy binder API */
    private final IBluetoothDun.Stub mServer = new IBluetoothDun.Stub() {
        public synchronized void dunDisconnect() {
            //check state
            //dunDisconnectNative();
            disconnect(true);
        }

        public synchronized int dunGetState() {
            return mConnectionState;
        }

        public synchronized BluetoothDevice dunGetConnectedDevice() {
            if ((mConnectionState == BluetoothDun.STATE_CONNECTED) &&
                    (mDunConnPath != null)) {
                BluetoothDevice device = mAdapter.getRemoteDevice(mDunConnPath);
                return device;
            }
            return null;
        }

        /* It is used for Settings application. Not update the value util BT is on.
        */
        public void setBluetoothTethering(boolean value) {
            if (!value) {
                //if (mConnectionState == BluetoothDun.STATE_CONNECTING) {
                //dunConnectRspNative(mDunConnPath, false);
                //} else if (mConnectionState == BluetoothDun.STATE_CONNECTED) {
                //dunDisconnectNative();
                //}
                disconnect(true);
            }

            if (mAdapter.getState() != BluetoothAdapter.STATE_ON && value) {
                IntentFilter filter = new IntentFilter();
                filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
                mTetheringReceiver = new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        if (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                                    BluetoothAdapter.STATE_OFF) == BluetoothAdapter.STATE_ON) {
                            mTetheringOn = true;
                            mContext.unregisterReceiver(mTetheringReceiver);
                        }
                    }
                };
                mContext.registerReceiver(mTetheringReceiver, filter);
            } else {
                mTetheringOn = value;
            }

            //SharedPreferences tetherSetting = getSharedPreferences(DUN_PREFERENCE_FILE, 0);
            //SharedPreferences.Editor editor = tetherSetting.edit();

            //editor.putBoolean(DUN_TETHER_SETTING, mTetheringOn);
            // Commit the edit!
            //editor.commit();
            if (DBG) log("Bluetooth Dun Service set tetherSetting(" + mTetheringOn + ")");
        }

        public boolean isTetheringOn() {
            return mTetheringOn;
        }

    };

    public BluetoothDunService() {
    }

    public void onCreate() {
        if (DBG) log("Bluetooth Dun Service is created");
        mContext = getApplicationContext();
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        mTetheringOn = false;
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = null;
        int state = BluetoothAdapter.ERROR;

        if (DBG) log("Bluetooth Dun Service is started : mHasInitiated="+mHasInitiated+" flags="
                +flags+" startId="+startId);

        String dunEnabled = SystemProperties.get("bt.profiles.dun.enabled");
        if (dunEnabled.isEmpty() || (Integer.parseInt(dunEnabled) == 0))
        {
            if (DBG) log("bt.profiles.dun.enabled is empty or 0. StopSelf");
            stopSelfResult(startId);
            return START_STICKY;
        }

        if (intent != null) {
            action = intent.getStringExtra("action");
            state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
            if (DBG) log("action="+action+"state="+state);
        }

        mStartId = startId;
        if (mAdapter == null || mAdapter.getState() != BluetoothAdapter.STATE_ON ||
                (action != null && action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)
                 && state == BluetoothAdapter.STATE_TURNING_OFF)) {
            clearService();
        } else {
            if (!mHasInitiated) {
                IntentFilter intentFilter = new IntentFilter();
                intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
                intentFilter.addAction(BluetoothDunService.ACCESS_RESPONSE_ACTION);
                intentFilter.addAction(BluetoothDunService.RESEND_NOTIFICATION_ACTION);
                intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
                intentFilter.addAction(BluetoothTethering.BLUETOOTH_INTERFACE_ADDED);
                intentFilter.addAction(BluetoothTethering.BLUETOOTH_INTERFACE_UP);
                intentFilter.addAction(BluetoothTethering.BLUETOOTH_INTERFACE_LINK_UP);
                mContext.registerReceiver(mReceiver, intentFilter);

                // TODO:Enable listener
                // dunEnableNative();
                // broadcast enabling to profilemanager
                // notifyProfileState(BluetoothProfileManager.STATE_ENABLING);
                startRfcommListenerThread();

                mHasInitiated = true;
                // Set mTetherOn based on the last saved tethering preference while starting the
                // DUN service
                //SharedPreferences tetherSetting = getSharedPreferences(DUN_PREFERENCE_FILE, 0);
                //mTetheringOn = tetherSetting.getBoolean(DUN_TETHER_SETTING, false);
                //if (DBG) log("Bluetooth Dun Service get tetherSetting(" + mTetheringOn + ")");
            } else {
                if (DBG) log("Already started, just return!");
                return START_STICKY;
            }
        }
        return START_STICKY;
    }

    public IBinder onBind(Intent intent) {
        Log.i(TAG, "Enter onBind()");
        if (IBluetoothDun.class.getName().equals(intent.getAction())) {
            return mServer;
        }
        return null;
    }

    public void onDestroy() {
        if (DBG) log("Bluetooth Dun Service is destroyed");

        clearService();
    }

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case MESSAGE_CONNECT_TIMEOUT:
                if (mConnectionState == BluetoothDun.STATE_CONNECTING) {
                    //dunConnectRspNative(mDunConnPath, false);
                    removeDunAuthNotification(DUN_AUTHORIZE_NOTIFY);
                    disconnect(true);
                    //dunSetState(BluetoothDun.STATE_DISCONNECTED);
                }
                break;
            case MESSAGE_TETHER_RETRY: {
                ConnectivityManager cm = (ConnectivityManager)mContext.getSystemService(
                                             Context.CONNECTIVITY_SERVICE);
                String iface = (String)msg.obj;
                if (cm.tether(iface) != ConnectivityManager.TETHER_ERROR_NO_ERROR) {
                    Log.e(TAG, "Error tethering again"+iface);
                }
            }
            break;
            }
        }
    };

    private synchronized void onDunConnectReq(String path) {
        //if (!mTetheringOn)
        //{
        //dunConnectRspNative(path, false);
        //return;
        //}

        // create dun activity to show authorize dialog
        log("dun connect request, device address: " + path);
        BluetoothDevice device = mAdapter.getRemoteDevice(path);

        createDunAuthNotification(mContext, device, false);

        mDunConnPath = path;
        dunSetState(BluetoothDun.STATE_CONNECTING);

        Message msg = Message.obtain(mHandler, MESSAGE_CONNECT_TIMEOUT);
        mHandler.sendMessageDelayed(msg, DUN_CONN_TIMEOUT);
    }

    private synchronized void onDunEnableCnf(boolean result) {

        if (result) {
            //notifyProfileState(BluetoothProfileManager.STATE_ENABLED);
        } else {
            //notifyProfileState(BluetoothProfileManager.STATE_ABNORMAL);
        }

    }

    private synchronized void onDunDisableCnf(boolean result) {

        if (result) {
            //notifyProfileState(BluetoothProfileManager.STATE_DISABLED);
        } else {
            //notifyProfileState(BluetoothProfileManager.STATE_ABNORMAL);
        }

    }

    private synchronized void onDunStateChanged(String path, String stateValues) {
        int state = convertStringtoState(stateValues);
        int prevstate = mConnectionState;
        BluetoothTethering btTethering;

        log("dun state changed to " + stateValues);
        if (state == BluetoothDun.STATE_CONNECTED) {
            //startNetworkService();
            mHandler.removeMessages(MESSAGE_CONNECT_TIMEOUT);
        } else if (state == BluetoothDun.STATE_DISCONNECTED) {
            btTethering = BluetoothTethering.getBluetoothTetheringInstance();
            if (btTethering != null) {
                btTethering.unregisterBTTether();
            } else {
                Log.w(TAG, "onDunStateChanged btTethering is null.");
            }

            mHandler.removeMessages(MESSAGE_TETHER_RETRY);

            if (prevstate == BluetoothDun.STATE_CONNECTING) {
                mHandler.removeMessages(MESSAGE_CONNECT_TIMEOUT);
                removeDunAuthNotification(DUN_AUTHORIZE_NOTIFY);
            }
        }

        dunSetState(state);
    }

    /*
    *  Return true for accept, otherwise reject
    */
    private synchronized String onDunDialupReq() {
        ConnectivityManager connmgr;
        String response;
        String[] dnsServers;
        BluetoothTethering btTethering;

        dnsServers = new String[2];
        connmgr = (ConnectivityManager)mContext.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo MobileInfo = connmgr.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
        NetworkInfo WifiInfo = connmgr.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        if (MobileInfo == null && WifiInfo == null) {
            Log.w(TAG, "dialup request, get network info failed");
            response = "\r\nNO CARRIER\r\n";
            //dunDialupRspNative(response, BLUETOOTH_IFACE_ADDR_START, dnsServers);
            return response;
        }
        NetworkInfo.State MobileState = NetworkInfo.State.UNKNOWN;
        NetworkInfo.State WifiState = NetworkInfo.State.UNKNOWN;

        if(MobileInfo!=null)
            MobileState=MobileInfo.getState();
        if(WifiInfo!=null)
            WifiState=WifiInfo.getState();

        TelephonyManager telmgr = (TelephonyManager)mContext.getSystemService(
                Context.TELEPHONY_SERVICE);
        int telDataState = telmgr.getDataState();

        if (DBG) log("[DUN]: telDataState =(" + telDataState + ")");

        if (telDataState == TelephonyManager.DATA_SUSPENDED) {
            response = "\r\nBUSY\r\n";
        } else if (MobileState == NetworkInfo.State.CONNECTED ||
                WifiState == NetworkInfo.State.CONNECTED) {

            if (DBG) log("[DUN]: Network isAvailable =("    +
                    connmgr.getActiveNetworkInfo().isAvailable() + ")");

            NetworkInfo.State state =
                (MobileState == NetworkInfo.State.CONNECTED)?MobileState:WifiState;
            if (DBG) log("startUsingNetworkFeature: ("  + state + ")");
            response = "\r\nCONNECT\r\n";
            btTethering = BluetoothTethering.getBluetoothTetheringInstance();
            btTethering.registerBTTether(this);
        } else if (MobileState == NetworkInfo.State.SUSPENDED ||
                WifiState == NetworkInfo.State.SUSPENDED) {
            response = "\r\nBUSY\r\n";
        } else if (MobileState == NetworkInfo.State.DISCONNECTED
                   || WifiState == NetworkInfo.State.DISCONNECTED) {
            response = "\r\nNO CARRIER\r\n";
        } else {
            response = "\r\nNO CARRIER\r\n";
        }
        if (DBG) log("dunDialupRspNative response: ("   + response + ")");
        //dunDialupRspNative(response, BLUETOOTH_IFACE_ADDR_START, dnsServers);
        //TC_GW_APS_BV_02_I
        //response = "\r\nBUSY\r\n";
        return response;
    }

    private void dunSetState(int state) {
        int prevstate = mConnectionState;
        //BluetoothDevice device = null;
        mConnectionState = state;

        //if (mDunConnPath != null) {
        //    device = mAdapter.getRemoteDevice(mDunConnPath);
        //}

        Intent intent = new Intent(BluetoothDun.STATE_CHANGED_ACTION);
        //intent.putExtra(BluetoothProfileManager.EXTRA_PROFILE, Profile.DUN);
        intent.putExtra(BluetoothDun.EXTRA_STATE, state);
        intent.putExtra(BluetoothDun.EXTRA_PREVIOUS_STATE, prevstate);
        intent.putExtra(BluetoothDevice.EXTRA_DEVICE, mRemoteDevice);
        mContext.sendBroadcast(intent, BLUETOOTH_PERM);
    }

    private void clearService() {
        if (VERBOSE) Log.v(TAG, "successfully stopped dun service : mHasInitiated="+mHasInitiated
                               +" mStartId="+mStartId);
        if (!mHasInitiated) {
            return;
        }

        if (mConnectionState != BluetoothDun.STATE_DISCONNECTED) {
            //stopNetworkService();
            mHandler.removeMessages(MESSAGE_TETHER_RETRY);

            if (mConnectionState == BluetoothDun.STATE_CONNECTING) {
                mHandler.removeMessages(MESSAGE_CONNECT_TIMEOUT);
                removeDunAuthNotification(DUN_AUTHORIZE_NOTIFY);
            }
        }
        disconnect(false);
        if (mListenThread != null) {
            mListenThread.shutdown();
        }
        //dunDisableNative();
        //cleanupNative();
        mHasInitiated = false;

        mContext.unregisterReceiver(mReceiver);

        if (mStartId != -1 && stopSelfResult(mStartId)) {
            if (VERBOSE) Log.v(TAG, "successfully stopped dun service");
            mStartId = -1;
        }
        if (VERBOSE) Log.v(TAG, "Dun Service clearService out");
    }

    private void createDunAuthNotification(Context context, BluetoothDevice device,
            boolean resend) {
        NotificationManager nm = (NotificationManager)context.getSystemService(
                                     Context.NOTIFICATION_SERVICE);
        Notification notification = null;

        // Create an intent triggered by clicking on the status icon
        Intent intent = new Intent();
        intent.setClass(context, BluetoothDunActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setAction(ACCESS_REQUEST_ACTION);
        intent.putExtra(BluetoothDunService.EXTRA_DEVICE, device);

        // Create an intent triggered by clicking on the
        // "Clear All Notifications" button
        Intent deleteIntent = new Intent(ACTION_CLEAR_AUTH_NOTIFICATION);
        deleteIntent.setClass(context, BluetoothDunReceiver.class);

        String name = device.getName();
        notification = new Notification(
                android.R.drawable.stat_sys_data_bluetooth,
                context.getString(R.string.bluetooth_dun_notification_connect_request_ticker),
                System.currentTimeMillis());
        notification.setLatestEventInfo(context,
                context.getString(R.string.bluetooth_dun_notification_connect_request_title),
                context.getString(R.string.bluetooth_dun_notification_connect_request_message,
                    name),
                PendingIntent.getActivity(context, 0, intent, 0));

        notification.flags |= Notification.FLAG_AUTO_CANCEL;
        notification.flags |= Notification.FLAG_ONLY_ALERT_ONCE;
        if (!resend) {
            notification.defaults |= Notification.DEFAULT_SOUND;
            notification.defaults |= Notification.DEFAULT_VIBRATE;
        } else {
            notification.defaults = 0;
        }
        notification.deleteIntent = PendingIntent.getBroadcast(context, 0, deleteIntent, 0);

        nm.notify(DUN_AUTHORIZE_NOTIFY, notification);
    }

    private void removeDunAuthNotification(int id) {
        NotificationManager nm = (NotificationManager)mContext
                                 .getSystemService(Context.NOTIFICATION_SERVICE);
        nm.cancel(id);
    }
    /*
        private boolean startNetworkService()
        {
            IBinder b = ServiceManager.getService(Context.NETWORKMANAGEMENT_SERVICE);
            INetworkManagementService service;

            if (b != null)
            {
                 service = INetworkManagementService.Stub.asInterface(b);
            }
            else
            {
                Log.w(TAG, "start network service, network management service is not available!");
                return false;
            }

            if (service == null)
            {
                Log.w(TAG, "start network service, get network management service failed!");
                return false;
            }

            String intIfname = "btn0";
            String extIfname = "ccmni0";

            try {
                service.setIpForwardingEnabled(true);
            } catch (Exception e) {
                Log.w(TAG, "set ip forward enabled error");
            }

            try {
                service.enableNat(intIfname, extIfname);
            } catch (Exception e) {
                Log.w(TAG, "enable nat error");
            }
            return true;
        }
    */
    /*
        private void stopNetworkService()
        {
            IBinder b = ServiceManager.getService(Context.NETWORKMANAGEMENT_SERVICE);
            INetworkManagementService service;

            if (b != null)
            {
                 service = INetworkManagementService.Stub.asInterface(b);
            }
            else
            {
                Log.w(TAG, "stop network service, network management service is not available!");
                return;
            }

            if (service == null)
            {
                Log.w(TAG, "stop network service, get network management service failed!");
                return;
            }

            String intIfname = "btn0";
            String extIfname = "ccmni0";

            try {
                service.setIpForwardingEnabled(false);
            } catch (Exception e) {
                Log.w(TAG, "set ip forward disable error");
            }

            try {
                service.disableNat(intIfname, extIfname);
            } catch (Exception e) {
                Log.w(TAG, "disable nat error");
            }
        }
    */
    private void notifyProfileState(int state) {
        log("notifyProfileState: " + state);

        //Intent intent = new Intent(BluetoothProfileManager.ACTION_PROFILE_STATE_UPDATE);
        //intent.putExtra(BluetoothProfileManager.EXTRA_PROFILE, Profile.DUN);
        //intent.putExtra(BluetoothProfileManager.EXTRA_NEW_STATE, state);
        //mContext.sendBroadcast(intent, BLUETOOTH_PERM);

    }

    private int convertStringtoState(String value) {
        if (value.equalsIgnoreCase("disconnected"))
            return BluetoothDun.STATE_DISCONNECTED;
        if (value.equalsIgnoreCase("connected"))
            return BluetoothDun.STATE_CONNECTED;
        return -1;
    }

    private static void log(String msg) {
        Log.d(TAG, msg);
    }

    private native boolean initNative();
    private native void cleanupNative();
    private synchronized native void dunEnableNative();
    private synchronized native void dunDisableNative();
    private synchronized native void dunDisconnectNative();
    private synchronized native void dunConnectRspNative(String path, boolean accept);
    private synchronized native void dunDialupRspNative(String response, String ipBase,
            String[] dnsServers);
    private synchronized native void dunNotifyNetworkTerminated();
}
