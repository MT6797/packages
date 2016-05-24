package com.mediatek.nfc.handover;

import java.util.LinkedList;

import android.content.Context;
import android.net.Uri;
import android.util.Log;
import android.os.Handler;
import android.os.Message;
import android.net.ConnectivityManager;
import android.content.Intent;

import com.android.nfc.NfcService;


import com.mediatek.nfc.addon.NfcRuntimeOptions;

public class BeamPlusHandover {

    private static final String TAG = "BeamPlusHandover";
    private static final int STATE_DISABLED = 0;
    private static final int STATE_ENABLING = 1;
    private static final int STATE_ENABLED = 2;
    private static final int STATE_CONNECTING = 3;
    private static final int STATE_CONNECTED = 4;
    private static final int STATE_DISCONNECTING = 5;

    private Context mContext;
    private IWifiP2pProxy mWifiP2pProxy;
    private int mState;

    private IWifiP2pProxy.IFastConnectInfo mTargetInfo;
    private IWifiP2pProxy.IFastConnectInfo mIncomingInfo;
    private IWifiP2pProxy.IFastConnectInfo mThisDeviceInfo;
    private IWifiP2pProxy.IWifiP2pDevice mConnectedDevice;

    private FileTransfer.IClient mFileTransferClient;
    private FileTransfer.IServer mFileTransferServer;

    private LinkedList<Uri[]> mUriList = new LinkedList<Uri[]>();
    private BeamPlusMonitor mMonitor;

    boolean mSessionDisconnecting = false;

    boolean mBeamSimultaneously = false;
    /**
     * BeamPlusHandover constructor
     * @param context
     * @param wifiP2pProxy
     */
    public BeamPlusHandover(Context context) {

        mContext = context;
        mWifiP2pProxy = new WifiP2pProxy(context);
        mWifiP2pProxy.addListener(mWifiP2pProxyListener);

        if (mWifiP2pProxy.isEnabled()) {
            mState = STATE_ENABLED;
        } else {
            mState = STATE_DISABLED;
        }

        mFileTransferClient = FileTransfer.createDefaultSender(mContext);
        mFileTransferClient
                .setClientEventListener(mFileTransferClientEventListener);
        mFileTransferServer = FileTransfer.createDefaultReceiver(mContext);
        mFileTransferServer
                .setServerEventListener(mFileTransferServerEventListener);
        mMonitor = new BeamPlusMonitor();

        FilePushRecord.createSingleton(context);
    }

    /**
     *  After File transfer, we have 6 seconds Session Monitor. It only reCount at startBeam,acceptIncomingBeam
     *  It is possible to recount at send Handover Request Message.  we add startNfcSession() to recount monitor session at send HRM.
     * @param null
     * @return
     */
    public void startNfcSession() {
        Log.d(TAG, "startNfcSession()");
        mMonitor.startNewNfcSession();
    }

    /**
     * Check if Beam with the same device
     * @param info
     * @return
     */
    public boolean isBeamWithTheSameDevice(IWifiP2pProxy.IFastConnectInfo info) {

        if (mTargetInfo != null
                && mTargetInfo.getDeviceAddress().equals(
                        info.getDeviceAddress())) {
            return true;
        }
        return false;
    }

    /**
     * Check if BeamPlus is under conneting state
     * @return
     */
    public boolean isConnecting() {
        if (mState == STATE_CONNECTING) {
            return true;
        }
        return false;
    }

    /**
     * Check if BeamPlus is under conneted state
     * @return
     */
    public boolean isConnected() {
        if (mState == STATE_CONNECTED) {
            return true;
        }
        return false;
    }

    /**
     * Check if BeamPlus is under Disconnecting state
     * @return
     */
    public boolean isDisconnecting() {
        Log.d(TAG, "isDisconnecting()  mSessionDisconnecting:" + mSessionDisconnecting);
        return mSessionDisconnecting;
    }

    /**
     * Check if BeamPlus is under conneted state
     * @return
     */
    public boolean isDeviceAlreadyConnected(IWifiP2pProxy.IFastConnectInfo info) {
        Log.d(TAG, "mConnectedDevice = " + mConnectedDevice.getDeviceAddress());
        Log.d(TAG, "info = " + info.getDeviceAddress());
        if (mConnectedDevice.getDeviceAddress().equalsIgnoreCase(info.getDeviceAddress())) {
            return true;
        }
        return false;
    }

    public void setThisDeviceInfo(IWifiP2pProxy.IFastConnectInfo info) {
        Log.d(TAG, "mThisDeviceInfo, addr = " + info.getDeviceAddress());
        Log.d(TAG, "mThisDeviceInfo, GOIP = " + info.getGoIpAddress());
        Log.d(TAG, "mThisDeviceInfo, GCIP = " + info.getGcIpAddress());
        mThisDeviceInfo = info;
    }

    /**
     * Start GC connect to GO
     * @param info
     * @param uris
     * @return
     */
    public int startBeam(IWifiP2pProxy.IFastConnectInfo info, Uri[] uris, boolean GoDisconnectingFlag) {

        Log.d(TAG, "StartBeam(), " + describeInternalState());
        Log.d(TAG, "StartBeam(), ===> new target = " + info.getDeviceAddress());
        Log.d(TAG, "StartBeam(), ===> GoDisconnectingFlag:" + GoDisconnectingFlag);

        if (!isBeamWithTheSameDevice(info)) {
            Log.d(TAG, "mUriList.clear()");
            mUriList.clear();
        }

        Log.d(TAG, "mUriList.push() uris:" + uris);

        mUriList.push(uris);
        mTargetInfo = info;

        int setupConnectionTime = NfcRuntimeOptions.getBeamPlusSetupConnectionTimeoutValue();
        mMonitor.startConnectionTimeoutCountDown((setupConnectionTime == 0) ? 30000 : setupConnectionTime);
        mMonitor.startMonitoring(BeamPlusMonitor.BEAM_SETUP_MONITOR);

        switch (mState) {
        case STATE_DISABLED:
            // Disable tethering if enabling Wifi
            if (mWifiP2pProxy.isSoftApEnable()) {
                mWifiP2pProxy.setSoftApEnabled(false);
            }
            mWifiP2pProxy.enable();
            mMonitor.enablePowerMonitoring();
            mState = STATE_ENABLING;
            break;
        case STATE_ENABLED:
            mWifiP2pProxy.fastConnect(mTargetInfo);
            mState = STATE_CONNECTING;
            break;
        case STATE_CONNECTED:

            if (GoDisconnectingFlag) {
                mState = STATE_CONNECTING;
                Log.d(TAG, "Go is Disconnecting, call fastConnect when STATE_CONNECTED ");
                mWifiP2pProxy.fastConnect(mTargetInfo);
                break;
            }

            if (isDeviceAlreadyConnected(mTargetInfo)) {
                mFileTransferClient.transferFiles(uris);

                Log.d(TAG, "mUriList.remove(uris) uris:" + uris);
                mUriList.remove(uris);
            } else {
                doDisconnectSequence();
                mState = STATE_DISCONNECTING;
            }
            break;
        case STATE_ENABLING:
            mMonitor.enablePowerMonitoring();
            break;
        case STATE_CONNECTING:
            Log.d(TAG, "StartBeam() call fastConnect when STATE_CONNECTING() ");
            mWifiP2pProxy.fastConnect(mTargetInfo);
            break;
        case STATE_DISCONNECTING:
            break;
        }
        return 0;
    }

    /**
     * Start Beam when connected for anti-collision case
     * @param info
     * @param uris
     * @return
     */
    public int startBeamWhenConnected(IWifiP2pProxy.IFastConnectInfo info,
            Uri[] uris) {
        // TODO: for anti-collision case
        mTargetInfo = info;
        //Log.d(TAG, "startBeamWhenConnected() , not to set mTargetInfo");

        Log.d(TAG, "set mBeamSimultaneously to true");
        mBeamSimultaneously = true;
        Log.d(TAG, "mUriList.push(uris) uris:" + uris);
        mUriList.push(uris);
        mMonitor.startMonitoring(BeamPlusMonitor.BEAM_SETUP_MONITOR);
        return 0;
    }

    /**
     * Accept GC is connected to GO
     * @param info
     * @return
     */
    public int acceptIncomingBeam(IWifiP2pProxy.IFastConnectInfo info) {
        Log.d(TAG, "acceptIncomingBeam()");
        mIncomingInfo = info;
        mMonitor.startMonitoring(BeamPlusMonitor.BEAM_SETUP_MONITOR);
        return 0;
    }

    // ///////////////////////////////////////
    // listeners
    // ///////////////////////////////////////
    private FileTransfer.IServerEventListener mFileTransferServerEventListener = new FileTransfer.IServerEventListener() {

        public void onServerStarted() {
            Log.d(TAG, "onServerStarted() from FileTransferServer "
                    + describeInternalState());
        }

        public void onServerShutdown() {
            Log.d(TAG, "onServerShutdown() from FileTransferServer "
                    + describeInternalState());
        }

        @Override
        public void onDisconnected() {
            Log.d(TAG, "onDisconnected() from FileTransferServer "
                    + describeInternalState());



        }
    };

    private FileTransfer.IClientEventListener mFileTransferClientEventListener = new FileTransfer.IClientEventListener() {
        public void onDisconnected(int message) {
            Log.d(TAG, "onClientDisconnected() from FileTransferClient "
                    + describeInternalState());
        }
    };

    private IWifiP2pProxy.WifiP2pProxyListener mWifiP2pProxyListener = new IWifiP2pProxy.WifiP2pProxyListener() {

        public void onEnabled() {
            Log.d(TAG, "onEnabled() from WifiP2pProxy "
                    + describeInternalState());
            switch (mState) {
            case STATE_DISABLED:
            case STATE_ENABLING:

                Log.d(TAG, "mTargetInfo:" + mTargetInfo + " mIncomingInfo:" + mIncomingInfo);
                if (mTargetInfo == null) {
                    mState = STATE_ENABLED;
                } else {
                    if (mIncomingInfo == null) {
                    mWifiP2pProxy.fastConnect(mTargetInfo);
                    mState = STATE_CONNECTING;
                    } else {
                        Log.d(TAG,
                            "mIncomingInfo != null , Collision case not to call fastConnect");
                        mState = STATE_ENABLED;
                    }
                }
                break;
            }
        }

        public void onDisabled() {
            Log.d(TAG, "onDisabled() from WifiP2pProxy "
                    + describeInternalState());
            switch (mState) {
            default:
                mState = STATE_DISABLED;
                mConnectedDevice = null;
                mTargetInfo = null;
                mIncomingInfo = null;
                Log.d(TAG, "set mBeamSimultaneously to false");
                mBeamSimultaneously = false;
                break;
            }
        }

        public void onConnected(IWifiP2pProxy.IWifiP2pDevice device, String goDeviceAddr) {

            Log.d(TAG, "onConnected() from WifiP2pProxy ,goDeviceAddr:" + goDeviceAddr + "  "
                    + describeInternalState());
            if (mConnectedDevice != null) {
                Log.d(TAG, "multiple device connection...");
            }
            mConnectedDevice = device;

            Log.d(TAG, " set mSessionDisconnecting to false");
            mSessionDisconnecting = false;

            Log.d(TAG, "device.getDeviceAddress():" + device.getDeviceAddress());


            switch (mState) {
            case STATE_CONNECTING:
                if (mTargetInfo.getDeviceAddress().equalsIgnoreCase(
                        device.getDeviceAddress())) {
                    Log.d(TAG, "start file trasfer client and server");

                    boolean isGoCase = false;
                    try {
                        Log.d(TAG, " mThisDeviceInfo.getDeviceAddress():"
                                + mThisDeviceInfo.getDeviceAddress());
                        Log.d(TAG, " mThisDeviceInfo. only set on GO side");

                        isGoCase = goDeviceAddr.equalsIgnoreCase
                                    (mThisDeviceInfo.getDeviceAddress());
                        Log.d(TAG, "  isGoCase:" + isGoCase);
                    } catch (Exception e) {
                        Log.e(TAG, "isGoCase exception:" + e);
                    }


                    if (mBeamSimultaneously == true && isGoCase) {
                        Log.d(TAG,
                        "mBeamSimultaneously == true , mFileTransferClient.connect GcIpAddress : "
                                    + mTargetInfo.getGcIpAddress());
                        mFileTransferClient.connect(mTargetInfo.getGcIpAddress());

                    } else {
                        Log.d(TAG, "[normal case], mFileTransferClient.connect GoIpAddress : "
                                    + mTargetInfo.getGoIpAddress());
                    mFileTransferClient.connect(mTargetInfo.getGoIpAddress());
                    }

                    mFileTransferServer.start();
                    Uri[] uris = null;
                    int count = 0;
                    while (null != (uris = mUriList.poll())) {

                        Log.d(TAG, "mUriList.poll() sender count:" + count);
                        count++;
                        mFileTransferClient.transferFiles(uris);
                    }
                    mState = STATE_CONNECTED;
                    mMonitor.startMonitoring(BeamPlusMonitor.BEAM_SESSION_MONITOR);
                } else {
                    Log.d(TAG, "connection is WRONG");
                    mWifiP2pProxy.disconnect();
                    mState = STATE_DISCONNECTING;
                }
                break;
            case STATE_ENABLED:
                if (mIncomingInfo != null) {
                    Log.d(TAG, "start file trasfer server");
                    // TODO: start file server
                    mState = STATE_CONNECTED;
                    mFileTransferServer.start();
                    if (mTargetInfo != null) {
                        Log.d(TAG, "collision is resolved, do the following transfer");
                        mFileTransferClient.connect(mTargetInfo.getGcIpAddress());
                        Uri[] uris = null;
                        int count = 0;
                        while (null != (uris = mUriList.poll())) {
                        Log.d(TAG, "mUriList.poll() receiver count:" + count);
                        count++;
                            mFileTransferClient.transferFiles(uris);
                        }
                    } else {
                        Log.d(TAG, "start client as well");
                        mFileTransferClient.connect(mThisDeviceInfo.getGcIpAddress());
                    }
                    mMonitor.startMonitoring(BeamPlusMonitor.BEAM_SESSION_MONITOR);
                } else {
                    mState = STATE_CONNECTED;
                }
                break;
            default:
                mState = STATE_CONNECTED;

                Log.d(TAG, "set mBeamSimultaneously to false");
                mBeamSimultaneously = false;
                break;
            }
        }

        public void onDisconnected(int reason) {
            Log.d(TAG, "onDisconnected(" + reason + ") from WifiP2pProxy "
                    + describeInternalState());
            if (reason == -3) {
                Log.d(TAG, "channel conflict, back to normal state");
            } else if (mState != STATE_CONNECTED && mState != STATE_DISCONNECTING) {
                Log.d(TAG, "false alarm, not even connected");
                return;
            }
            mConnectedDevice = null;
            mFileTransferClient.disconnect();
            mFileTransferServer.stop();

            Log.d(TAG, " mSessionDisconnecting:" + mSessionDisconnecting);

            switch (mState) {
            case STATE_DISCONNECTING:
                if (mTargetInfo != null) {
                    Log.d(TAG, "switch connection");
                    mWifiP2pProxy.fastConnect(mTargetInfo);
                    mState = STATE_CONNECTING;
                } else {
                    mState = STATE_ENABLED;
                }
                break;
            case STATE_DISABLED:
                Log.d(TAG, "obfused state, onDisconnected in DISABLED state");
                mConnectedDevice = null;
                mTargetInfo = null;
                mIncomingInfo = null;
                Log.d(TAG, "set mBeamSimultaneously to false");
                mBeamSimultaneously = false;
                break;
            default:
                mState = STATE_ENABLED;
                if (mSessionDisconnecting == false) {
                    Log.d(TAG, " set mTargetInfo , mIncomingInfo to null ");
                mTargetInfo = null;
                mIncomingInfo = null;
                    Log.d(TAG, "set mBeamSimultaneously to false");
                    mBeamSimultaneously = false;
                }
                break;
            }

            Log.d(TAG, " set mSessionDisconnecting to false  ");
            mSessionDisconnecting = false;
        }
    };

    public void onTimeout() {
        Log.d(TAG, "onTimeout() "
                + describeInternalState());
        switch (mState) {
        case STATE_CONNECTING:
            mConnectedDevice = null;
            mTargetInfo = null;
            mIncomingInfo = null;
            mState = STATE_ENABLED;
            Log.d(TAG, "set mBeamSimultaneously to false");
            mBeamSimultaneously = false;
            break;
        default:
            break;
        }
    }

    class BeamPlusMonitor implements Handler.Callback {
        public static final int BEAM_SETUP_MONITOR = 0;
        public static final int BEAM_SESSION_MONITOR = 1;
        private static final String ACTION_BEAM_STARTED = "com.mtk.beamplus.activated";
        private static final String ACTION_BEAM_FINISHED = "com.mtk.beamplus.deactivated";
        private static final int BEAM_STARTED = 1;
        private static final int BEAM_FINISHED = 2;
        private static final int MSG_TIMEOUT = 13;
        private static final int MSG_MONITOR = 14;
        private Handler mHandler;
        /**
         * we leave 1 minute for BeamPlus connection setup,
         * once the connection has been setup, change monitor time period to 2 sec.
         */
        private int mSetupMoniterTime = 60000;
        private int mSessionMoniterTime = 6000; //extend SessionMonitor to 6 seconds for Client retry 3 times
        private boolean mPowerMonitorEnabled;

        public BeamPlusMonitor() {
            mHandler = new Handler(this);
        }

        public void enablePowerMonitoring() {
            Log.d(TAG, "enablePowerMonitoring");
            synchronized (mHandler) {
                mPowerMonitorEnabled = true;
            }
        }

        public void startNewNfcSession() {
            Log.d(TAG, "startNewNFCSession()  mPowerMonitorEnabled=" + mPowerMonitorEnabled);

            if (mHandler.hasMessages(MSG_MONITOR)) {

                Log.d(TAG, "startNewNFCSession()  hasMessages(MSG_MONITOR)  ");
                mHandler.removeMessages(MSG_MONITOR);
                mHandler.sendEmptyMessageDelayed(MSG_MONITOR, mSessionMoniterTime);
            }

        }


        public void startMonitoring(int monitorMode) {
            Log.d(TAG, "startMonitoring, mPowerMonitorEnabled = " + mPowerMonitorEnabled + ", monitorMode = " + monitorMode);
            synchronized (mHandler) {
                if (mHandler.hasMessages(MSG_MONITOR)) {
                    mHandler.removeMessages(MSG_MONITOR);
                }

                if (monitorMode == BEAM_SETUP_MONITOR) {
                    mHandler.sendEmptyMessageDelayed(MSG_MONITOR, mSetupMoniterTime);
                    mWifiP2pProxy.stopReconnectAndScan(true);
                } else if (monitorMode == BEAM_SESSION_MONITOR) {
                    stopConnectionTimeoutCountDown();
                    mHandler.sendEmptyMessageDelayed(MSG_MONITOR, mSessionMoniterTime);
                }

                broadcastBeamPlusStatus(BEAM_STARTED);
            }
        }

        public void startConnectionTimeoutCountDown(int ms) {
            Log.d(TAG, "startConnectionTimeoutCountDown " + ms + "ms");
            if (mHandler.hasMessages(MSG_TIMEOUT)) {
                mHandler.removeMessages(MSG_TIMEOUT);
            }
            mHandler.sendEmptyMessageDelayed(MSG_TIMEOUT, ms);
        }

        public void stopConnectionTimeoutCountDown() {
            Log.d(TAG, "stopConnectionTimeoutCountDown");
            if (mHandler.hasMessages(MSG_TIMEOUT)) {
                mHandler.removeMessages(MSG_TIMEOUT);
            }
        }

        private void broadcastBeamPlusStatus(int status) {
            if (status == BEAM_STARTED) {
                Log.d(TAG, "broadcastBeamPlusStatus, " + ACTION_BEAM_STARTED);
                mContext.sendBroadcast(new Intent(ACTION_BEAM_STARTED));
            } else if (status == BEAM_FINISHED) {
                Log.d(TAG, "broadcastBeamPlusStatus, " + ACTION_BEAM_FINISHED);
                mContext.sendBroadcast(new Intent(ACTION_BEAM_FINISHED));
            }
        }

        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_TIMEOUT:
                    Log.d(TAG, "handleMessage: MSG_TIMEOUT");
                    onTimeout();
                    return true;
                case MSG_MONITOR:
                    Log.d(TAG, "handleMessage: MSG_MONITOR" + describeInternalState());
                    synchronized (mHandler) {
                        if (!mFileTransferClient.isAnySessionOngoing() &&
                            !mFileTransferServer.isAnySessionOngoing()) {
                            Log.d(TAG, "no active sessions");



                            if (mPowerMonitorEnabled) {

                                Log.d(TAG, "mPowerMonitorEnabled:" + mPowerMonitorEnabled +
                                    "  ,not to check wifi is connected or not");
                                //ConnectivityManager manager = (ConnectivityManager)
                                //mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
                                //if (!manager.getNetworkInfo(ConnectivityManager.TYPE_WIFI).
                                //isConnectedOrConnecting()) {
                                    Log.d(TAG,
                                    "WiFi is not connected or under connecting, disable WiFi");
                                    mWifiP2pProxy.disable();

                                    mSessionDisconnecting = true;
                                    Log.d(TAG, " set mSessionDisconnecting to true");
                                //}

                                if (mState == STATE_ENABLING || mState == STATE_DISABLED) {
                                    Log.d(TAG, "Set mTargetInfo,mIncomingInfo to null");
                                    mTargetInfo = null;
                                    mIncomingInfo = null;
                                    Log.d(TAG, "set mBeamSimultaneously to false");
                                    mBeamSimultaneously = false;
                                }
                            } else {
                                if (!NfcService.isWfdOngoing()) {
                                    Log.d(TAG, "disconnect Wi-Fi P2P connection");
                                    mWifiP2pProxy.disconnect();

                                    mSessionDisconnecting = true;
                                    Log.d(TAG, " set mSessionDisconnecting to true");
                                }
                            }

                            mWifiP2pProxy.stopReconnectAndScan(false);

                            broadcastBeamPlusStatus(BEAM_FINISHED);
                            mPowerMonitorEnabled = false;

                            Log.d(TAG, "!!!! mUriList.clear() !!!!");
                            mUriList.clear();

                        } else {
                            Log.d(TAG, "some sessions are active, continue monitoring...");
                            mHandler.sendEmptyMessageDelayed(MSG_MONITOR, mSessionMoniterTime);

                            if (mConnectedDevice != null) {
                                String remoteDeviceAddress = mConnectedDevice.getDeviceAddress();
                                Log.d(TAG, "requestWfdLinkInfo remoteDeviceAddress:" + remoteDeviceAddress);
                                mWifiP2pProxy.requestWfdLinkInfo(remoteDeviceAddress);
                            }

                        }
                    }
                    return true;
            }
            return false;
        }
    }

    public IWifiP2pProxy.IFastConnectInfo createDefaultFastConnectInfo() {
        Log.d(TAG, "createDefaultFastConnectInfo (forwarding call)");
        return mWifiP2pProxy.createDefaultFastConnectInfo();
    }

    public IWifiP2pProxy.IFastConnectInfo getFastConnectInfo(IWifiP2pProxy.IFastConnectInfo info) {
        Log.d(TAG, "getFastConnectInfo (forwarding call)");
        return mWifiP2pProxy.getFastConnectInfo(info);
    }

    public boolean isWifiEnabled() {
        Log.d(TAG, "isWifiEnabled (forwarding call)");
        return mWifiP2pProxy.isEnabled();
    }

    public int enableWifi() {
        Log.d(TAG, "enableWifi (forwarding call) and do power monitoring");
        mMonitor.enablePowerMonitoring();
        mMonitor.startMonitoring(BeamPlusMonitor.BEAM_SETUP_MONITOR);

        // Disable tethering if enabling Wifi
        if (mWifiP2pProxy.isSoftApEnable()) {
            mWifiP2pProxy.setSoftApEnabled(false);
        }

        int ret = mWifiP2pProxy.enable();
        mState = STATE_ENABLING;
        return ret;
    }

    // ///////////////////////////////////////
    // utilities
    // ///////////////////////////////////////
    private void doDisconnectSequence() {
        mFileTransferClient.disconnect();
        mFileTransferServer.stop();
        mWifiP2pProxy.disconnect();
    }

    private String describeInternalState() {
        String stateStr = "";
        switch (mState) {
        case STATE_DISABLED:
            stateStr = "===> mState = DISABLED";
            break;
        case STATE_ENABLED:
            stateStr = "===> mState = ENABLED";
            break;
        case STATE_CONNECTED:
            stateStr = "===> mState = CONNECTED";
            break;
        case STATE_ENABLING:
            stateStr = "===> mState = ENABLING";
            break;
        case STATE_CONNECTING:
            stateStr = "===> mState = CONNECTING";
            break;
        case STATE_DISCONNECTING:
            stateStr = "===> mState = DISCONNECTING";
            break;
        }
        if (mTargetInfo != null) {
            stateStr += ", mTargetInfo = " + mTargetInfo.getDeviceAddress();
        } else {
            stateStr += ", mTargetInfo = null";
        }
        return stateStr;
    }

}
