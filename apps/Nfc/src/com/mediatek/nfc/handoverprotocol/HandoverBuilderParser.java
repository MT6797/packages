package com.mediatek.nfc.handoverprotocol;

import java.io.UnsupportedEncodingException;

import com.mediatek.nfc.handoverprotocol.CarrierData.HandoverCarrierRecord;
import com.mediatek.nfc.handoverprotocol.HandoverMessage.HandoverCarrier;
import com.mediatek.nfc.handoverprotocol.HandoverMessage.HandoverRequest;
import com.mediatek.nfc.handoverprotocol.HandoverMessage.HandoverSelect;
import com.mediatek.nfc.handoverprotocol.WifiCarrierConfiguration.Credential;
import com.mediatek.nfc.handoverprotocol.WifiCarrierConfiguration.TLV;

import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.util.Log;

public class HandoverBuilderParser {
    static final String TAG = "HandoverBuilderParser";

    //
    public static final short WPS_ATTRIBUTE_TYPE_VENDOR_ID    = 0x5001;
    public static final short WPS_ATTRIBUTE_TYPE_GO_IP        = 0x5002;
    public static final short WPS_ATTRIBUTE_TYPE_GC_IP        = 0x5003;
    public static final short WPS_ATTRIBUTE_TYPE_MAX_HEIGHT   = 0x5004;
    public static final short WPS_ATTRIBUTE_TYPE_MAX_WIDTH    = 0x5005;
    public static final short WPS_ATTRIBUTE_TYPE_CLIENT_TABLE = 0x5006;

    public static final short WPS_ATTRIBUTE_EXTRA_INFO_CHANGE = 0x10D0;

    // create a HrM for establish a Wi-Fi Simple Configuration Protocol.
    public static NdefMessage createWfLegacyHrM(boolean wifiCPS) {
        Log.i(TAG, "    createWfLegacyHrM  wifiCPS:" + wifiCPS);

        // create WiFi HcR
        HandoverCarrierRecord hc = HandoverCarrierRecord.newInstance(WifiCarrierConfiguration.TYPE);

        // create HrM
        HandoverMessage message_mtk_request = new HandoverMessage();
        message_mtk_request.appendAlternativeCarrier(wifiCPS ? HandoverMessage.CARRIER_POWER_STATE_ACTIVE : HandoverMessage.CARRIER_POWER_STATE_ACTIVATING, hc);
        NdefMessage mtk_hr = message_mtk_request.createHandoverRequestMessage();

        return mtk_hr;
    }// end of createWiFiLegacyHrM

    // create a HrM for P2P file transfer with BT & Wi-Fi carrier.
    public static NdefMessage createP2PHrM(String bt_MacAddress, byte[][] wifi_Aux) {

        // create BT ccR
        BTCarrierConfiguration btCCR = new BTCarrierConfiguration(bt_MacAddress);

        // create WiFi HcR
        HandoverCarrierRecord HcR = HandoverCarrierRecord.newInstance("application/vnd.wfa.wsc");

        // get WiFi Crendential.
        //Credential credential_mtk_R = new Credential();

        // create HrM
        HandoverMessage message_mtk_request = new HandoverMessage();
        message_mtk_request.appendAlternativeCarrier(HandoverMessage.CARRIER_POWER_STATE_ACTIVE, btCCR);
        message_mtk_request.appendAlternativeCarrier(HandoverMessage.CARRIER_POWER_STATE_ACTIVE, HcR, wifi_Aux);
        NdefMessage mtk_hr = message_mtk_request.createHandoverRequestMessage();

        return mtk_hr;
    }// end of createP2PHrM

    // create a HrM for Wi-Fi Display.
    public static NdefMessage createWfDisplayHrM(byte[][] wifi_Aux) {

        // create WiFi HcR
        HandoverCarrierRecord HcR = HandoverCarrierRecord.newInstance("application/vnd.wfa.wsc");

        // create HrM
        HandoverMessage message_mtk_request = new HandoverMessage();
        message_mtk_request.appendAlternativeCarrier(HandoverMessage.CARRIER_POWER_STATE_ACTIVE, HcR, wifi_Aux);
        NdefMessage mtk_hr = message_mtk_request.createHandoverRequestMessage();
        return mtk_hr;
    }// end of createWiFiDisplayHrM

    // create a HsM for establish a Wi-Fi Simple Configuration Protocol.
/*
    public static NdefMessage createWfLegacyHsM(String wifi_NetworkKey,
            String wifi_SSID,
            byte[] wifi_MACAddress,
            short AuthType,
            short EnrcType){
        // set WiFi basic Crendential TLVs.
        Credential credential = new Credential();
        credential.setNetworkKey(wifi_NetworkKey);
        credential.setSSID(wifi_SSID);
        credential.setMacAddress(wifi_MACAddress);

        credential.setAuthenticationType(AuthType);
        credential.setEncryptionType(EnrcType);


        // create HsM
        HandoverMessage msg_mtk_select = new HandoverMessage();
        WifiCarrierConfiguration CCR = new WifiCarrierConfiguration(credential);
        msg_mtk_select.appendAlternativeCarrier(HandoverMessage.CARRIER_POWER_STATE_ACTIVE, CCR);
        NdefMessage mtk_hs = msg_mtk_select.createHandoverSelectMessage();

        return mtk_hs;
    }// end of createWiFiLegacyHsM
*/


    // create a HsM for P2P file transfer with BT & Wi-Fi carrier.
    public static NdefMessage createP2PHsM(String bt_MacAddress,
            String wifi_NetworkKey,
            String wifi_SSID,
            Short AuthType,
            Short EncType,
            byte[] wifi_MACAddress,
            byte[] vendorID,
            byte[] GOIP,
            byte[] GCIP,
            byte[][] clientTable,
            boolean btCPS,
            boolean wifiCPS,
            byte[] extraExchangeInfo) {

        // create BT ccR
        BTCarrierConfiguration btCCR = new BTCarrierConfiguration(bt_MacAddress);

        // set WiFi basic Crendential TLVs.
        Credential credential = new Credential();

        if (wifi_NetworkKey != null)
            credential.setNetworkKey(wifi_NetworkKey);

        if (wifi_SSID != null)
            credential.setSSID(wifi_SSID);

        credential.setMacAddress(wifi_MACAddress);

        if (AuthType != null)
            credential.setAuthenticationType(AuthType);

        if (EncType != null)
            credential.setEncryptionType(EncType);

        // set extension Credential TLVs
        credential.addExtensionTLV(WPS_ATTRIBUTE_TYPE_VENDOR_ID, vendorID);
        credential.addExtensionTLV(WPS_ATTRIBUTE_TYPE_GO_IP, GOIP);
        credential.addExtensionTLV(WPS_ATTRIBUTE_TYPE_GC_IP, GCIP);
        credential.addExtensionTLV(WPS_ATTRIBUTE_EXTRA_INFO_CHANGE, extraExchangeInfo);
        if (clientTable != null) {
            for (int i = 0; i < clientTable.length; i++) {
                credential.addExtensionTLV(HandoverBuilderParser.WPS_ATTRIBUTE_TYPE_CLIENT_TABLE, clientTable[i]);
            }
        }
        // create HsM with BT + WiFiCCR(with extension TLVs)
        HandoverMessage msg_mtk_select = new HandoverMessage();
        WifiCarrierConfiguration CCR = new WifiCarrierConfiguration(credential);
        msg_mtk_select.appendAlternativeCarrier(btCPS ? HandoverMessage.CARRIER_POWER_STATE_ACTIVE : HandoverMessage.CARRIER_POWER_STATE_ACTIVATING, btCCR);
        msg_mtk_select.appendAlternativeCarrier(wifiCPS ? HandoverMessage.CARRIER_POWER_STATE_ACTIVE : HandoverMessage.CARRIER_POWER_STATE_ACTIVATING, CCR);
        NdefMessage mtk_hs = msg_mtk_select.createHandoverSelectMessage();

        return mtk_hs;
    }// end of createP2PHsM

    // create a HsM for Wi-Fi Display.
    public static NdefMessage createWfDisplayHsM(String wifi_NetworkKey,
            String wifi_SSID,
            byte[] wifi_MACAddress,
            byte[] vendorID,
            byte[] GOIP,
            byte[] GCIP,
            byte[] maxH,
            byte[] maxW,
            byte[][] clientTable) {

        // set WiFi basic Crendential TLVs.
        Credential credential = new Credential();
        credential.setNetworkKey(wifi_NetworkKey);
        credential.setSSID(wifi_SSID);
        credential.setMacAddress(wifi_MACAddress);

        // set extension Credential TLVs
        credential.addExtensionTLV(WPS_ATTRIBUTE_TYPE_VENDOR_ID, vendorID);
        credential.addExtensionTLV(WPS_ATTRIBUTE_TYPE_GO_IP, GOIP);
        credential.addExtensionTLV(WPS_ATTRIBUTE_TYPE_GC_IP, GCIP);
        credential.addExtensionTLV(WPS_ATTRIBUTE_TYPE_MAX_HEIGHT, maxH);
        credential.addExtensionTLV(WPS_ATTRIBUTE_TYPE_MAX_WIDTH, maxW);
        for (int i = 0; i < clientTable.length; i++) {
            credential.addExtensionTLV(HandoverBuilderParser.WPS_ATTRIBUTE_TYPE_CLIENT_TABLE, clientTable[i]);
        }

        // create HsM with BT + WiFiCCR(with extension TLVs)
        HandoverMessage msg_mtk_select = new HandoverMessage();
        WifiCarrierConfiguration CCR = new WifiCarrierConfiguration(credential);
        msg_mtk_select.appendAlternativeCarrier(HandoverMessage.CARRIER_POWER_STATE_ACTIVE, CCR);
        NdefMessage mtk_hs = msg_mtk_select.createHandoverSelectMessage();

        return mtk_hs;
    }// end of createWiFiDisplayHsM


    // create a Error HsM .
    public static NdefMessage createErrorHsM(byte reason, byte data) {

        HandoverMessage msg_mtk_select = new HandoverMessage();
        NdefMessage mtk_hs = msg_mtk_select.createErrorHandoverSelectMessage(reason, data);

        return mtk_hs;
    }// end of createErrorHsM

    // create a Error HsM .
    public static NdefMessage createMtkSpecificHsM(byte reason, byte[] arrayData) {

        HandoverMessage msg_mtk_select = new HandoverMessage();
        NdefMessage mtk_hs = msg_mtk_select.createMtkSpecificHandoverSelectMessage(reason, arrayData);

        return mtk_hs;
    }// end of createErrorHsM


/*
    // result of parse WfLegacy HrM
    public static boolean parseWfLegacyHrM(NdefMessage wfLegacyHrM) throws FormatException {
        HandoverRequest r;
        HandoverCarrier[] carrier_R;
        HandoverCarrierRecord hc_wf_p2p;

        try {
            r = HandoverMessage.tryParseRequest(wfLegacyHrM);
            carrier_R = r.getCarriers();
            hc_wf_p2p = HandoverCarrierRecord.tryParse(carrier_R[0]);

            if(hc_wf_p2p == null){
                Log.i(TAG, "hc_wf_p2p == null  return false");
                return false;
            }

        if (hc_wf_p2p.getCarrierType().equals("application/vnd.wfa.wsc")) {
            return true;
        }
        else
            return false;

        }catch (Exception ex) {
            Log.e(TAG, " Exception:" + ex);
            ex.printStackTrace();
            Log.e(TAG, "parseWfLegacyHrM   return false");
            return false;
        }


    }// end of auxFromParseWfLegacy HrM
*/
    // get Aux data by parse P2p HrM
    public static byte[][] auxFromParseP2pHrM(NdefMessage p2pHrM) throws FormatException {
        HandoverRequest r;
        HandoverCarrier[] carrier_R;
        HandoverCarrierRecord hc_wf_p2p;

        try {
            r = HandoverMessage.tryParseRequest(p2pHrM);
            carrier_R = r.getCarriers();
            hc_wf_p2p = HandoverCarrierRecord.tryParse(carrier_R[1]);


            if(hc_wf_p2p == null){
                Log.i(TAG, "hc_wf_p2p == null  return null");
                return null;
            }

        if (hc_wf_p2p.getCarrierType().equals("application/vnd.wfa.wsc")) {
            return r.getAuxiliaryData();
        }
        else
            return null;

        }catch (Exception ex) {
            Log.e(TAG, " Exception:" + ex);
            ex.printStackTrace();
            Log.e(TAG, "auxFromParseWfDisplayHrM   return null");
            return null;
        }


    }// end of auxFromParseP2pHrM

    // get Aux data by parse WfDisplay HrM
    public static byte[][] auxFromParseWfDisplayHrM(NdefMessage wfDisplayHrM) throws FormatException {
        HandoverRequest r;
        HandoverCarrier[] carrier_R;
        HandoverCarrierRecord hc_wf_p2p;


        try {
            r = HandoverMessage.tryParseRequest(wfDisplayHrM);
            carrier_R = r.getCarriers();
            hc_wf_p2p = HandoverCarrierRecord.tryParse(carrier_R[0]);


            if(hc_wf_p2p == null){
                Log.i(TAG, "hc_wf_p2p == null  return null");
                return null;
            }

        if (hc_wf_p2p.getCarrierType().equals("application/vnd.wfa.wsc")) {
            return r.getAuxiliaryData();
        }
        else
            return null;
        }catch (Exception ex) {
            Log.e(TAG, " Exception:" + ex);
            ex.printStackTrace();
            Log.e(TAG, "auxFromParseWfDisplayHrM   return null");
            return null;
        }


    }// end of auxFromParseWfDisplayHrM


    /**
    *       this function is used when Wifi Alternative Record exist, you want to judge WFD case or Legacy case..
    *
    */
    public static boolean isAuxExistOnWifiHrM(NdefMessage wifiHrM) throws FormatException {
        Log.i(TAG, "    isAuxExistonWifiHrM   ");
        HandoverRequest r;
        HandoverCarrier[] carrier_R;
        HandoverCarrierRecord hc_wf_p2p;

        try {
            r = HandoverMessage.tryParseRequest(wifiHrM);
            carrier_R = r.getCarriers();
            hc_wf_p2p = HandoverCarrierRecord.tryParse(carrier_R[0]);


        byte[][] mAuxData = null;

        if (hc_wf_p2p.getCarrierType().equals("application/vnd.wfa.wsc") == false) {
            throw new FormatException(" Wifi CCR type not match :" + hc_wf_p2p.getCarrierType());
        }

        mAuxData = r.getAuxiliaryData();
        if (mAuxData == null) {
            Log.i(TAG, "    mAuxData == null");
            return false;
        }
        else {
            Log.i(TAG, "    mAuxData Exist");
            return true;
        }

        }catch (Exception ex) {
            Log.e(TAG, " Exception:" + ex);
            ex.printStackTrace();
            Log.e(TAG, "isAuxExistonWifiHrM   return false");
            return false;
        }


    }// end of isAuxExistonWifiHrM

/*
    // get Credential by parse WfLegacy HsM
    public static Credential credentialFromParseWfLegacyHsM(NdefMessage wfLegacyHsM) throws FormatException, UnsupportedEncodingException{
        HandoverSelect s = HandoverMessage.tryParseSelect(wfLegacyHsM);
        HandoverCarrier[] carriers_S = s.getCarriers();
        WifiCarrierConfiguration ccr_wf_p2p = WifiCarrierConfiguration.tryParse(carriers_S[0]);

        if(new String(ccr_wf_p2p.getType(), "UTF-8").equals("application/vnd.wfa.wsc")){
            return ccr_wf_p2p.getCredential();
        }
        else
            return null;
    }// end of credentialFromParseWfLegacyHsM
*/
    // get Credential by parse P2p HsM
    public static Credential credentialFromParseP2pHsM(NdefMessage p2pHsM) throws FormatException, UnsupportedEncodingException {
        HandoverSelect s;
        HandoverCarrier[] carriers_S;
        WifiCarrierConfiguration ccr_wf_p2p;


        try {
            s = HandoverMessage.tryParseSelect(p2pHsM);
            carriers_S = s.getCarriers();
            ccr_wf_p2p = WifiCarrierConfiguration.tryParse(carriers_S[1]);


            if(ccr_wf_p2p == null){
                Log.i(TAG, "ccr_wf_p2p == null  return null");
                return null;
            }



        if (new String(ccr_wf_p2p.getType(), "UTF-8").equals("application/vnd.wfa.wsc")) {
            return ccr_wf_p2p.getCredential();
        }
        else
            return null;

        }catch (Exception ex) {
            Log.e(TAG, " Exception:" + ex);
            ex.printStackTrace();
            Log.e(TAG, "credentialFromParseP2pHsM   return null");
            return null;
        }


    }// end of credentialFromParseP2pHsM

    // get Credential by parse WfDisplay HsM
    public static Credential credentialFromParseWfDisplayHsM(NdefMessage wfDisplayHsM) throws FormatException, UnsupportedEncodingException {
        HandoverSelect s;
        HandoverCarrier[] carriers_S;
        WifiCarrierConfiguration ccr_wf_p2p;

        try {
            s = HandoverMessage.tryParseSelect(wfDisplayHsM);
            carriers_S = s.getCarriers();
            ccr_wf_p2p = WifiCarrierConfiguration.tryParse(carriers_S[0]);

            if(ccr_wf_p2p == null){
                Log.e(TAG, "ccr_wf_p2p == null  return null");
                return null;
            }



        if (new String(ccr_wf_p2p.getType(), "UTF-8").equals("application/vnd.wfa.wsc")) {
            return ccr_wf_p2p.getCredential();
        }
        else
            return null;


        }catch (Exception ex) {
            Log.e(TAG, " Exception:" + ex);
            ex.printStackTrace();
            Log.e(TAG, "credentialFromParseWfDisplayHsM   return null");
            return null;
        }


    }// end of credentialFromParseWfDisplayHsM

    // get AuxData by parse WfDisplay HsM
    public static TLV[] auxFromParseWfDisplayHsM(NdefMessage wfDisplayHsM) throws FormatException, UnsupportedEncodingException {
        HandoverSelect s;
        HandoverCarrier[] carriers_S;
        WifiCarrierConfiguration ccr_wf_p2p ;

        try {
            s = HandoverMessage.tryParseSelect(wfDisplayHsM);
            carriers_S = s.getCarriers();
            ccr_wf_p2p = WifiCarrierConfiguration.tryParse(carriers_S[0]);

        if (new String(ccr_wf_p2p.getType(), "UTF-8").equals("application/vnd.wfa.wsc")) {
            return ccr_wf_p2p.getExtensions();
        }
        else
            return null;

        }catch (Exception ex) {
            Log.e(TAG, " Exception:" + ex);
            ex.printStackTrace();
            Log.e(TAG, "auxFromParseWfDisplayHsM   return null");
            return null;
        }


    }// end of credentialFromParseWfDisplayHsM



} // end of HandoverBuilderParser
