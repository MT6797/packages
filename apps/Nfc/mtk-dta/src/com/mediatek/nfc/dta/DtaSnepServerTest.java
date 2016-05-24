package com.mediatek.nfc.dta;

import java.io.IOException;

//import com.android.nfc.snep.SnepClient;
//import com.android.nfc.snep.SnepMessage;
//import com.android.nfc.snep.SnepServer;

//import com.mediatek.nfc.snep.SnepClient;
import com.mediatek.nfc.snep.SnepMessage;
import com.mediatek.nfc.snep.SnepServer;

import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
//import android.test.AndroidTestCase;
import android.util.Log;

//import java.lang.StringBuffer;

//import java.lang.StringBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class DtaSnepServerTest implements SnepServer.Callback {

    private static final String TAG = "DtaSnepServerTest";
    private static final boolean DBG = true;

    public static final String DEFAULT_SNEP_SERVER_NAME = "urn:nfc:sn:snep";
    public static final String EXTENDED_DTA_SERVER_NAME = "urn:nfc:sn:sneptest";

    private static final int SERVICE_SAP = 0x4;
    private static final int DEFAULT_MIU = 128;

    private Map<ByteArrayWrapper, NdefMessage> mStoredNdef =
        new HashMap<ByteArrayWrapper, NdefMessage>();
    private static final ByteArrayWrapper DEFAULT_NDEF = new ByteArrayWrapper(new byte[] {});

    private String mServerName;
    private int mScenario;
    private SnepServer mServer;

    public DtaSnepServerTest() {
        mServerName = DEFAULT_SNEP_SERVER_NAME;
    }

    public DtaSnepServerTest(String name) {
        mServerName = name;
    }

    public void testStartSnepServer() throws IOException {
        if (DBG) Log.d(TAG, "testStartSnepServer : mServerName = " + mServerName);

        mServer = new SnepServer(mServerName, SERVICE_SAP, this, DEFAULT_MIU);
        mServer.start();

        /*
        try {
            Thread.sleep(24 * 60 * 60 * 1000);
        } catch (InterruptedException e) {

        }
        */
    }

    public void testStopSnepServer() {
        if (mServer != null) {
            mServer.stop();
        }
    }

     private NdefMessage getData1Ndef() {
         return new NdefMessage(new NdefRecord[] { getRtdTextRecord(DtaSnepClientTest.DATA1) });
     }

     private NdefMessage getData2Ndef() {
         return new NdefMessage(new NdefRecord[] { getRtdTextRecord(DtaSnepClientTest.DATA2) });
     }

     private NdefRecord getRtdTextRecord(String text) {

         NdefRecord record = null;
         String lang;
         byte[] textBytes;
         byte[] langBytes;
         int    langLength;
         int    textLength;
         byte[] payload;
         try {
             lang       = "la";
             textBytes  = text.getBytes();
             langBytes  = lang.getBytes("US-ASCII");
             langLength = langBytes.length;
             textLength = textBytes.length;
             payload    = new byte[1 + langLength + textLength];

             // set status byte (see NDEF spec for actual bits)
             payload[0] = (byte) langLength;

             // copy langbytes and textbytes into payload
             System.arraycopy(langBytes, 0, payload, 1,              langLength);
             System.arraycopy(textBytes, 0, payload, 1 + langLength, textLength);

             record = new NdefRecord(NdefRecord.TNF_WELL_KNOWN,
                                                NdefRecord.RTD_TEXT,
                                                new byte[0],
                                                payload);
         }catch (Exception e) {
             Log.d(TAG, "getRtdTextRecord , exception " + e);
         }
         return record;
    }


    static class ByteArrayWrapper {
        private final byte[] data;

        public ByteArrayWrapper(byte[] data) {
            if (data == null) {
                throw new NullPointerException();
            }
            this.data = data;
        }

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof ByteArrayWrapper)) {
                return false;
            }
            return Arrays.equals(data, ((ByteArrayWrapper) other).data);
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(data);
        }
    }

    @Override
    public SnepMessage doPut(NdefMessage msg) {
        Log.d(TAG, "doPut()");
        NdefRecord record = msg.getRecords()[0];
        ByteArrayWrapper id = (record.getId().length > 0) ?
                new ByteArrayWrapper(record.getId()) : DEFAULT_NDEF;
        mStoredNdef.put(id, msg);
        return SnepMessage.getMessage(SnepMessage.RESPONSE_SUCCESS);
    }

    @Override
    public SnepMessage doGet(int acceptableLength, NdefMessage msg) {
        Log.d(TAG, "doGet()");
        NdefRecord record = msg.getRecords()[0];
        ByteArrayWrapper id = (record.getId().length > 0) ?
                new ByteArrayWrapper(record.getId()) : DEFAULT_NDEF;
        NdefMessage result = mStoredNdef.get(id);

        if (result == null) {

            // TC_S_RET_BV_04
            // Scenario 31. NFC FORUM Default SNEP Server rejects Get request
            if (mServerName.equals(DEFAULT_SNEP_SERVER_NAME)) {
                return SnepMessage.getMessage(SnepMessage.RESPONSE_NOT_IMPLEMENTED);
            }

            // TC_S_RET_BV_02
            // Scenario 29. Extended DTA Server returns Not Found response
            return SnepMessage.getMessage(SnepMessage.RESPONSE_NOT_FOUND);
        }
        if (acceptableLength < result.toByteArray().length) {
            return SnepMessage.getMessage(SnepMessage.RESPONSE_EXCESS_DATA);
        }
        return SnepMessage.getSuccessResponse(result);
    }

}


