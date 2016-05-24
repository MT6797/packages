package com.mediatek.nfc.dta;

import java.io.IOException;

//import com.android.nfc.snep.SnepClient;
//import com.android.nfc.snep.SnepMessage;
//import com.android.nfc.snep.SnepServer;

import com.mediatek.nfc.snep.SnepClient;
//import com.mediatek.nfc.snep.SnepMessage;
//import com.mediatek.nfc.snep.SnepServer;

import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
//import android.test.AndroidTestCase;
import android.util.Log;

//import java.lang.StringBuffer;

/**
 * Tests connectivity to a custom LLCP link, using a physical NFC device.
 */
public class DtaSnepLlcpTest {

    private static final String TAG = "DtaSnepLlcpTest";
    private static final boolean DBG = true;

    public static final String DATA1 = "Lorem ipsum dolor sit amet.";
    public static final String DATA2 = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. "
          +"Phasellus at lorem nunc, ut venenatis quam. Etiam id dolor quam, at viverra dolor." +
          " Phasellus eu lacus ligula, quis euismod erat. Sed feugiat, "+
          "ligula at mollis aliquet, justo lacus condimentum eros, "+
          "non tincidunt neque ipsum eu risus. Sed adipiscing dui euismod tellus ullamcorper "+
          "ornare. Phasellus mattis risus et lectus euismod eu fermentum sem cursus. "+
          "Phasellus tristique consectetur mauris eu porttitor. Sed lobortis porttitor orci.";

    public DtaSnepLlcpTest() {
    }

    public void testSendDataByLlcp() throws IOException {
        if (DBG) Log.d(TAG, "testSendDataByLlcp");
        SnepClient client = new SnepClient();
        client.connect();
        client.put(getData1Ndef());
        client.close();
    }

    public void testGetData1FromExtendedDtaServer() throws IOException {
        if (DBG) Log.d(TAG, "testGetData1FromExtendedDtaServer");
        SnepClient client = new SnepClient(DtaSnepServerTest.EXTENDED_DTA_SERVER_NAME);
        client.connect();
        client.get(getData1Ndef());
        client.close();
    }

    private NdefMessage getData1Ndef() {
        return new NdefMessage(new NdefRecord[] { getRtdTextRecord(DATA1) });
    }

    private NdefMessage getData2Ndef() {
        return new NdefMessage(new NdefRecord[] { getRtdTextRecord(DATA2) });
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

}


