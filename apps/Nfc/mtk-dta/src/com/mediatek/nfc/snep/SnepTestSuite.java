/**
 *  SnepTestSuite
 */

package com.mediatek.nfc.snep;

import android.content.Intent;
import android.content.Context;
import android.nfc.NdefMessage;
import android.util.Log;

import com.mediatek.nfc.Util;

public class SnepTestSuite {
    private static final String TAG = "DtaSnepTestSuite";
    private static final String DEFAULT_NAME = "urn:nfc:sn:snep";
    private static final int DEFAULT_SAP = 4;

    private static class DefaultServer implements SnepServer.Callback {
        private static final String NAME = "urn:nfc:sn:snep";
        private static final int SAP = 4;

        private Context mContext;
        private SnepServer mSnepServerImpl;

        private DefaultServer(Context context) {
            mContext = context;
            mSnepServerImpl = new SnepServer(NAME, SAP, this);
        }

        private void notifyDtaApk(int requestCode, NdefMessage ndef) {
            Log.d(TAG, NAME + ", req = " + requestCode);
            Intent intent = new Intent("com.mediatek.nfc.dta.ACTION_SNEP_SERVER_REQ");
            intent.putExtra("com.mediatek.nfc.dta.SNEP_SERVER_SN", NAME);
            intent.putExtra("com.mediatek.nfc.dta.SNEP_SERVER_REQ_CODE", requestCode);
            if (ndef != null) {
                intent.putExtra("com.mediatek.nfc.dta.SNEP_SERVER_REQ_DATA", ndef.toByteArray());
            } else {
                Log.d(TAG, "null ndef");
            }
            mContext.sendBroadcast(intent);
        }

        public void start() {
            mSnepServerImpl.start();
        }

        public void stop() {
            mSnepServerImpl.stop();
        }

        @Override
        public SnepMessage doPut(NdefMessage msg) {
            notifyDtaApk((int)SnepMessage.REQUEST_PUT, msg);
            return SnepMessage.getMessage(SnepMessage.RESPONSE_SUCCESS);
        }

        @Override
        public SnepMessage doGet(int acceptableLength, NdefMessage msg) {

        Log.d(TAG, "DefaultServer  doGet() acceptableLength:" + acceptableLength);
            notifyDtaApk((int)SnepMessage.REQUEST_GET, msg);
            return SnepMessage.getMessage(SnepMessage.RESPONSE_NOT_IMPLEMENTED);
        }
    }

    public static class ExtendedServer implements SnepServer.Callback {
        private static final String NAME = "urn:nfc:sn:sneptest";
        private static final int SAP = 0x13;

        private Context mContext;
        private SnepServer mSnepServerImpl;
        private NdefMessage mCachedNdefMessage;

        private ExtendedServer(Context context) {
            mContext = context;
            mSnepServerImpl = new SnepServer(NAME, SAP, this);
        }

        private void notifyDtaApk(int requestCode, NdefMessage ndef) {
            Log.d(TAG, NAME + ", req = " + requestCode);
            Intent intent = new Intent("com.mediatek.nfc.dta.ACTION_SNEP_SERVER_REQ");
            intent.putExtra("com.mediatek.nfc.dta.SNEP_SERVER_SN", NAME);
            intent.putExtra("com.mediatek.nfc.dta.SNEP_SERVER_REQ_CODE", requestCode);
            if (ndef != null) {
                intent.putExtra("com.mediatek.nfc.dta.SNEP_SERVER_REQ_DATA", ndef.toByteArray());
            } else {
                Log.d(TAG, "null ndef");
            }
            mContext.sendBroadcast(intent);
        }

        public void start() {
            mSnepServerImpl.start();
        }

        public void stop() {
            mSnepServerImpl.stop();
            mCachedNdefMessage = null;
        }

        @Override
        public SnepMessage doPut(NdefMessage msg) {
            mCachedNdefMessage = msg;
            notifyDtaApk((int)SnepMessage.REQUEST_PUT, msg);
            return SnepMessage.getMessage(SnepMessage.RESPONSE_SUCCESS);
        }

        @Override
        public SnepMessage doGet(int acceptableLength, NdefMessage msg) {

            Log.d(TAG, "DefaultServer  doGet() acceptableLength:"
                + acceptableLength+"  mCachedNdefMessage:"+mCachedNdefMessage);

            notifyDtaApk((int)SnepMessage.REQUEST_GET, msg);

            if (mCachedNdefMessage == null) {
                return SnepMessage.getMessage(SnepMessage.RESPONSE_NOT_FOUND);
            } else {
                NdefMessage cachedNdef = mCachedNdefMessage;
                mCachedNdefMessage = null;
                return SnepMessage.getSuccessResponse(cachedNdef);
            }
        }
    }

    private static DefaultServer sDefaultServer;
    private static ExtendedServer sExtendedServer;

    public static void initialize(Context context) {
        sDefaultServer = new DefaultServer(context);
        sExtendedServer = new ExtendedServer(context);
    }

    public static void startServers() {
        if (sDefaultServer != null && sExtendedServer != null) {
            sDefaultServer.start();
            sExtendedServer.start();
        }
    }

    public static void stopServers() {
        if (sDefaultServer != null && sExtendedServer != null) {
            sDefaultServer.stop();
            sExtendedServer.stop();
        }
    }

    public static boolean onLlcpActivated(final Context context, Intent cachedDtaIntent) {
        if (cachedDtaIntent.getAction().equals("com.mediatek.nfc.dta.ACTION_SNEP_CLIENT_REQUEST")) {
            final int requestCode = cachedDtaIntent.getIntExtra(
                "com.mediatek.nfc.dta.SNEP_CLIENT_REQ_CODE", -1);
            final byte[] data = cachedDtaIntent.getByteArrayExtra(
                "com.mediatek.nfc.dta.SNEP_CLIENT_REQ_DATA");
            final String sn = cachedDtaIntent.getStringExtra(
                "com.mediatek.nfc.dta.SNEP_CLIENT_SN");
            Log.d(TAG, "onDtaCommand: CLIENT_REQUEST " + sn + ", req = " + requestCode);
            Log.d(TAG, "              data: " + Util.printNdef(data));

            new Thread(new Runnable() {
                @Override
                public void run() {
                    SnepClient snepClient = null;
                    try {
                        Log.d(TAG, "ClientThread ENTRY");
                        snepClient = new SnepClient(sn);
                        snepClient.connect();
                        Log.d(TAG, "snepClient connected");

                        NdefMessage ndefMessage = new NdefMessage(data);
                        if (requestCode == (int)SnepMessage.REQUEST_PUT) {
                            Log.d(TAG, "sending PUT");
                            snepClient.put(ndefMessage);
                        } else if (requestCode == (int)SnepMessage.REQUEST_GET) {
                            Log.d(TAG, "sending GET");
                            SnepMessage response = snepClient.get(ndefMessage);
                            Log.d(TAG, "response confirmed");
                            Intent intent = new Intent(
                                "com.mediatek.nfc.dta.ACTION_SNEP_CLIENT_RESPONSE");
                            intent.putExtra("com.mediatek.nfc.dta.SNEP_CLIENT_RES_CODE",
                                (int)response.getField());
                            intent.putExtra("com.mediatek.nfc.dta.SNEP_CLIENT_RES_DATA",
                                response.getNdefMessage().toByteArray());
                            context.sendBroadcast(intent);
                        }
                    } catch (Exception e) {
                        Log.d(TAG, "!Thread Exception :"+e);
                        e.printStackTrace();
                    } finally {
                        snepClient.close();
                        Log.d(TAG, "ClientThread EXIT");
                    }
                }
            }).start();

            return true;
        }

        return false;
    }

    public static String[] getRelatedIntentActions() {
        return new String[] {
            "com.mediatek.nfc.dta.ACTION_SNEP_CLIENT_REQUEST",
        };
    }

}
