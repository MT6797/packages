package com.mediatek.services.telephony;

import android.os.SystemProperties;
import android.telecom.PhoneAccountHandle;
import android.telephony.PhoneNumberUtils;
import android.telephony.ServiceState;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;

import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.SubscriptionController;
import com.android.phone.PhoneUtils;

import com.mediatek.telephony.TelephonyManagerEx;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

/**
 * The emergency call handler.
 * Selected the proper Phone for setting up the ecc call.
 */
public class EmergencyRuleHandler {
    static final String TAG = "EmergencyRuleHandler";
    static final boolean DBG = true;

    private Phone mGsmPhone = null;
    private Phone mCdmaPhone = null;
    private Phone mEccRetryPhone = null;
    private Phone mTargetPhone = null;

    private String mOp;
    private boolean mIsDualPhoneCdmaExist;
    private boolean mIsGsmAlwaysNumber;
    private boolean mIsCdmaAlwaysNumber;
    private boolean mIsGsmOnlyNumber;
    private boolean mIsCdmaOnlyNumber;
    private boolean mIsGsmPreferredNumber;
    private boolean mIsCdmaPreferredNumber;

    private List<RuleHandler> mRuleList;

    private String mNumber;
    private boolean mIsEccRetry;

    public static final String ECC_LIST_PREFERENCE_PATH = "/system/etc/ecc_list_preference.xml";
    public static final String OPERATOR_ATTR = "Operator";
    public static final String ECC_LIST_ATTR  = "EccList";
    public static final String ECC_GSM_ONLY_TAG       = "GsmOnly";
    public static final String ECC_GSM_PREFERRED_TAG  = "GsmPref";
    public static final String ECC_CDMA_ONLY_TAG      = "CdmaOnly";
    public static final String ECC_CDMA_PREFERRED_TAG = "CdmaPref";

    private static HashMap<String, String> sGsmOnlyEccMap       = new HashMap<String, String>();
    private static HashMap<String, String> sCdmaOnlyEccMap      = new HashMap<String, String>();
    private static HashMap<String, String> sGsmPreferredEccMap  = new HashMap<String, String>();
    private static HashMap<String, String> sCdmaPreferredEccMap = new HashMap<String, String>();

    private static final boolean MTK_C2K_SUPPORT =
            "1".equals(SystemProperties.get("ro.mtk_c2k_support"));

    void log(String s) {
        Log.d(TAG, s);
    }

    /**
     * The common interface for ECC rule.
     */
    public interface RuleHandler {
        /**
         * Handle the ecc reqeust.
         * @param number The Ecc number will be dialed.
         * @return Phone The Phone object used for ecc.
         */
        public Phone handleRequest();
    }

    /**
     * Init the EmergencyRuleHandler.
     * @param accountHandle The target PhoneAccountHandle.
     * @param number The Ecc number.
     * @param isEccRetry whether this is ECC Retry.
     */
    public EmergencyRuleHandler(
            PhoneAccountHandle accountHandle,
            String number,
            boolean isEccRetry) {

        mNumber = number;
        mIsEccRetry = isEccRetry;
        initPhones(accountHandle);

        mOp = SystemProperties.get("ro.operator.optr", "OM");
        log("ro.operator.optr= " + mOp + ", number:" + number);
        mIsDualPhoneCdmaExist = isDualPhoneCdmaExist();
        parseEccListPreference();
        buildAlwaysNumber();
        mIsGsmOnlyNumber = isGsmOnlyNumber();
        mIsCdmaOnlyNumber = isCdmaOnlyNumber();
        mIsGsmPreferredNumber = isGsmPreferredNumber();
        mIsCdmaPreferredNumber = isCdmaPreferredNumber();
    }

    /**
     * Check if this is evdo dualtalk solution.
     * @return A boolean value indicate we can handle internal.
     */
    public static boolean isDualPhoneCdmaExist() {
        /// M: SVLTE+G solution, voice dual talk only @{
        if (MTK_C2K_SUPPORT) {
            boolean result = false;
            /** M: Bug Fix for ALPS01944336 @{ */
            // Check the CDMA phone status. In some roaming place, the CDMA
            // phone maybe is null
            if (TelephonyManager.getDefault().getPhoneCount() >= 2) {
                Phone[] phones = PhoneFactory.getPhones();
                for (Phone p : phones) {
                    if (null != p && p.getPhoneType() == PhoneConstants.PHONE_TYPE_CDMA) {
                        result = true;
                        break;
                    }
                }
            }
            /** @} */
            return result;
        } else {
        /// @}
            return false;
        }
    }

    private void initPhones(PhoneAccountHandle accountHandle) {
        mGsmPhone = getProperPhone(PhoneConstants.PHONE_TYPE_GSM);
        mCdmaPhone = getProperPhone(PhoneConstants.PHONE_TYPE_CDMA);

        if (mGsmPhone != null) {
            log("GSM Network State == " +
                    serviceStateToString(mGsmPhone.getServiceState().getState()));
        } else {
            log("No GSM Phone exist.");
        }
        if (mCdmaPhone != null) {
            log("CDMA Network State == " +
                    serviceStateToString(mCdmaPhone.getServiceState().getState()));
        } else {
            log("No CDMA Phone exist.");
        }

        if (mIsEccRetry) {
            int phoneId = Integer.parseInt(accountHandle.getId());
            mEccRetryPhone = PhoneFactory.getPhone(phoneId);
            log("EccRetry Phone = " + mEccRetryPhone);
        } else {
            int subId = PhoneUtils.getSubIdForPhoneAccountHandle(accountHandle);
            log("Provided phone account, subId:" + subId);
            if (subId != SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
                int phoneId = SubscriptionController.getInstance().getPhoneId(subId);
                mTargetPhone = PhoneFactory.getPhone(phoneId);
                log("Provided phone account = " + mTargetPhone);
            }
        }
    }

    private Phone getProperPhone(int phoneType) {
        Phone[] phones = PhoneFactory.getPhones();
        Phone phone = null;
        log("phone list size = " + phones.length);
        TelephonyManagerEx tmEx = TelephonyManagerEx.getDefault();
        int simCount = TelephonyManager.getDefault().getSimCount();
        if (phoneType == PhoneConstants.PHONE_TYPE_GSM) {
            for (int i = 0; i < simCount; i++) {
                if (tmEx.getIccAppFamily(i) == TelephonyManagerEx.APP_FAM_3GPP) {
                    phone = PhoneFactory.getPhone(i);
                    log("getProperSlot with phoneType GSM, slotid:" + i);
                    return phone;
                }
            }
            int cdmaSlot = SystemProperties.getInt("persist.radio.cdma_slot", -1);
            log("getProperSlot, cdmaSlot:" + cdmaSlot);
            if (cdmaSlot != -1) {
                for (int i = 0; i < simCount; i++) {
                    if (i != cdmaSlot - 1) {
                        phone = PhoneFactory.getPhone(i);
                        log("getProperSlot with non-C slot, slotid:" + i);
                        return phone;
                    }
                }
            }
        }

        for (Phone p : phones) {
            if (p.getPhoneType() == phoneType) {
                phone = p;
                break;
            }
        }
        log("getProperSlot with phoneType = " + phoneType + " and return phone = " + phone);
        return phone;
    }

    /**
     * Check if gsm has registered to network.
     * @return indicates the register status.
     */
    private boolean isGsmNetworkReady() {
        if (mGsmPhone != null) {
            return ServiceState.STATE_IN_SERVICE
                    == mGsmPhone.getServiceState().getState();
        }

        return false;
    }

    /**
     * Check if cdma has registered to network.
     * @return indicates the register status.
     */
    private boolean isCdmaNetworkReady() {
        if (mCdmaPhone != null) {
            return ServiceState.STATE_IN_SERVICE
                    == mCdmaPhone.getServiceState().getState();
        }

        return false;
    }

    String serviceStateToString(int state) {
        String s = null;
        if (state < ServiceState.STATE_IN_SERVICE
                || state > ServiceState.STATE_POWER_OFF) {
            log("serviceStateToString: invalid state = " + state);
            s = "INVALIDE_STATE";
            return s;
        }

        switch (state) {
            case ServiceState.STATE_IN_SERVICE:
                s = "STATE_IN_SERVICE";
                break;
            case ServiceState.STATE_OUT_OF_SERVICE:
                s = "STATE_OUT_OF_SERVICE";
                break;
            case ServiceState.STATE_EMERGENCY_ONLY:
                s = "STATE_EMERGENCY_ONLY";
                break;
            case ServiceState.STATE_POWER_OFF:
                s = "STATE_POWER_OFF";
                break;
            default:
                s = "UNKNOWN_STATE";
                break;
        }

        return s;
    }

    /**
     * Get the proper Phone for ecc dial.
     * @return A object for Phone that used for setup call.
     */
    public Phone getPreferredPhone() {
        if (mIsDualPhoneCdmaExist) {
            generateRuleList();
            return getPhoneFromRuleList();
        } else {
            if (mIsEccRetry) {
                return mEccRetryPhone;
            } else {
                if (MTK_C2K_SUPPORT) {
                    boolean allGsmPhone = true;
                    for (Phone p : PhoneFactory.getPhones()) {
                        if (p.getPhoneType() != PhoneConstants.PHONE_TYPE_GSM) {
                            allGsmPhone = false;
                            break;
                        }
                    }
                    log("getPreferredPhone, allGsmPhone:" + allGsmPhone);
                    if (allGsmPhone) {
                        return getProperPhone(PhoneConstants.PHONE_TYPE_GSM);
                    } else {
                        return mTargetPhone;
                    }
                } else {
                    log("return null for non-c2k project");
                    return null;
                }
            }
        }
    }

    private void generateRuleList() {
        if (mRuleList != null) {
            mRuleList.clear();
        }
        mRuleList = new ArrayList<RuleHandler>();

        // 0. Select phone based on always number rule
        mRuleList.add(new AlwaysNumberRule());
        // 1. Select phone based on only number rule
        mRuleList.add(new OnlyNumberRule());
        // 2. Select ECC retry phone
        mRuleList.add(new EccRetryRule());
        // 3. Select phone based on GSM/CDMA service state
        mRuleList.add(new CdmaAndGsmReadyRule());
        mRuleList.add(new GsmReadyOnlyRule());
        mRuleList.add(new CdmaReadyOnlyRule());
        mRuleList.add(new GCUnReadyRule());
    }

    private Phone getPhoneFromRuleList() {
        for (RuleHandler rule : mRuleList) {
            Phone phone = rule.handleRequest();
            if (phone != null) {
                log("handleRequest find prefered phone = " + phone);
                return phone;
            }
        }
        return null;
    }

    private void buildAlwaysNumber() {
        Phone[] phones = PhoneFactory.getPhones();
        HashMap<Integer, Boolean> isEccMap = new HashMap<Integer, Boolean>();
        for (Phone p : phones) {
            if (null != p) {
                boolean isEcc = PhoneNumberUtils.isEmergencyNumber(p.getSubId(), mNumber);
                isEccMap.put(p.getPhoneType(), isEcc);
            }
        }
        mIsGsmAlwaysNumber = false;
        mIsCdmaAlwaysNumber = false;
        if (isEccMap.containsKey(PhoneConstants.PHONE_TYPE_GSM) &&
                isEccMap.containsKey(PhoneConstants.PHONE_TYPE_CDMA)) {
            if (isEccMap.get(PhoneConstants.PHONE_TYPE_GSM) &&
                    !isEccMap.get(PhoneConstants.PHONE_TYPE_CDMA)) {
                mIsGsmAlwaysNumber = true;
            }
            if (!isEccMap.get(PhoneConstants.PHONE_TYPE_GSM) &&
                    isEccMap.get(PhoneConstants.PHONE_TYPE_CDMA)) {
                mIsCdmaAlwaysNumber = true;
            }
        }
        log("isGsmAlwaysNumber = " + mIsGsmAlwaysNumber);
        log("isCdmaAlwaysNumber = " + mIsCdmaAlwaysNumber);
    }

/*
<?xml version="1.0" encoding="utf-8"?>
<EccPrefTable>
    <!--
        The attribute definition for tag GsmOnly, GsmPref, CdmaOnly, CdmaPref:
        - Operator: OM or OPXX
        - EccList: the preferred ECC list
    -->
    <GsmOnly Operator="OM" EccList="112,000,08,118" />
    <GsmOnly Operator="OP09" EccList="112,000,08,118" />
    <GsmOnly Operator="OP01" EccList="112" />
    <CdmaOnly Operator="OM" EccList="" />
    <CdmaOnly Operator="OP09" EccList="" />
    <CdmaOnly Operator="OP01" EccList="" />
    <GsmPref Operator="OM" EccList="911,999" />
    <GsmPref Operator="OP09" EccList="911,999" />
    <GsmPref Operator="OP01" EccList="000,08,118,911,999" />
    <CdmaPref Operator="OM" EccList="110,119,120,122" />
    <CdmaPref Operator="OP09" EccList="110,119,120,122" />
    <CdmaPref Operator="OP01" EccList="110,119,120,122" />
</EccPrefTable>
*/
    /**
     * Parse Ecc List Preference From XML File
     *
     * @param none.
     * @return none.
     * @hide
     */
    private static void parseEccListPreference() {
        sGsmOnlyEccMap.clear();
        sCdmaOnlyEccMap.clear();
        sGsmPreferredEccMap.clear();
        sCdmaPreferredEccMap.clear();

        // Parse GSM ECC list
        try {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            XmlPullParser parser = factory.newPullParser();
            if (parser == null) {
                return;
            }
            FileReader fileReader = new FileReader(ECC_LIST_PREFERENCE_PATH);
            parser.setInput(fileReader);
            int eventType = parser.getEventType();

            while (eventType != XmlPullParser.END_DOCUMENT) {
                switch (eventType) {
                    case XmlPullParser.START_TAG:
                        String eccTag = parser.getName();
                        String op = null;
                        String eccList = null;
                        int attrNum = parser.getAttributeCount();
                        for (int i = 0; i < attrNum; ++i) {
                            String name = parser.getAttributeName(i);
                            String value = parser.getAttributeValue(i);
                            if (name.equals(OPERATOR_ATTR)) {
                                op = value;
                            } else if (name.equals(ECC_LIST_ATTR)) {
                                eccList = value;
                            }
                        }
                        if (op != null && eccList != null) {
                            if (eccTag.equals(ECC_GSM_ONLY_TAG)) {
                                sGsmOnlyEccMap.put(op, eccList);
                            } else if (eccTag.equals(ECC_CDMA_ONLY_TAG)) {
                                sCdmaOnlyEccMap.put(op, eccList);
                            } else if (eccTag.equals(ECC_GSM_PREFERRED_TAG)) {
                                sGsmPreferredEccMap.put(op, eccList);
                            } else if (eccTag.equals(ECC_CDMA_PREFERRED_TAG)) {
                                sCdmaPreferredEccMap.put(op, eccList);
                            }
                        }
                        break;
                    default:
                        break;
                }
                eventType = parser.next();
            }
            fileReader.close();
        } catch (FileNotFoundException e) {
            Log.e(TAG, "Ecc List Preference file not found");
            sGsmOnlyEccMap.put("OM", "112,000,08,118");
            sGsmOnlyEccMap.put("OP09", "112,000,08,118");
            sGsmOnlyEccMap.put("OP01", "112");
            sCdmaOnlyEccMap.put("OM", "");
            sCdmaOnlyEccMap.put("OP09", "");
            sCdmaOnlyEccMap.put("OP01", "");
            sGsmPreferredEccMap.put("OM", "911,999");
            sGsmPreferredEccMap.put("OP09", "911,999");
            sGsmPreferredEccMap.put("OP01", "000,08,118,911,999");
            sCdmaPreferredEccMap.put("OM", "110,119,120,122");
            sCdmaPreferredEccMap.put("OP09", "110,119,120,122");
            sCdmaPreferredEccMap.put("OP01", "110,119,120,122");
        } catch (XmlPullParserException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean isNumberMatched(String number, String[] eccList) {
        for (String eccNumber : eccList) {
            if (number.equals(eccNumber)) {
                return true;
            }
        }
        return false;
    }

    private boolean isGsmOnlyNumber() {
        String eccList = "";
        if (sGsmOnlyEccMap.containsKey(mOp)) {
            eccList = sGsmOnlyEccMap.get(mOp);
        } else {
            eccList = sGsmOnlyEccMap.get("OM");
        }
        log("isGsmOnlyNumber eccList = " + eccList);
        boolean bMatched = TextUtils.isEmpty(eccList) ?
                false : isNumberMatched(mNumber, eccList.split(","));
        log("isGsmOnlyNumber = " + bMatched);
        return bMatched;
    }

    private boolean isCdmaOnlyNumber() {
        String eccList = "";
        if (sCdmaOnlyEccMap.containsKey(mOp)) {
            eccList = sCdmaOnlyEccMap.get(mOp);
        } else {
            eccList = sCdmaOnlyEccMap.get("OM");
        }
        log("isCdmaOnlyNumber eccList = " + eccList);
        boolean bMatched = TextUtils.isEmpty(eccList) ?
                false : isNumberMatched(mNumber, eccList.split(","));
        log("isCdmaOnlyNumber = " + bMatched);
        return bMatched;
    }

    private boolean isGsmPreferredNumber() {
        String eccList = "";
        if (sGsmPreferredEccMap.containsKey(mOp)) {
            eccList = sGsmPreferredEccMap.get(mOp);
        } else {
            eccList = sGsmPreferredEccMap.get("OM");
        }
        log("isGsmPreferredNumber eccList = " + eccList);
        boolean bMatched = TextUtils.isEmpty(eccList) ?
                false : isNumberMatched(mNumber, eccList.split(","));
        log("isGsmPreferredNumber = " + bMatched);
        return bMatched;
    }

    private boolean isCdmaPreferredNumber() {
        String eccList = "";
        if (sCdmaPreferredEccMap.containsKey(mOp)) {
            eccList = sCdmaPreferredEccMap.get(mOp);
        } else {
            eccList = sCdmaPreferredEccMap.get("OM");
        }
        log("isCdmaPreferredNumber eccList = " + eccList);
        boolean bMatched = TextUtils.isEmpty(eccList) ?
                false : isNumberMatched(mNumber, eccList.split(","));
        log("isCdmaPreferredNumber = " + bMatched);
        return bMatched;
    }

    /**
     * DualTalk AlwaysNumberRule
     *
     */
    class AlwaysNumberRule implements RuleHandler {
        public Phone handleRequest() {
            log("AlwaysNumberRule: handleRequest...");
            if (mIsGsmAlwaysNumber) {
                return getProperPhone(PhoneConstants.PHONE_TYPE_GSM);
            }

            if (mIsCdmaAlwaysNumber) {
                return getProperPhone(PhoneConstants.PHONE_TYPE_CDMA);
            }

            return null;
        }
    }

    /**
     * DualTalk OnlyNumberRule
     *
     */
    class OnlyNumberRule implements RuleHandler {
        public Phone handleRequest() {
            log("OnlyNumberRule: handleRequest...");
            if (mIsGsmOnlyNumber) {
                return getProperPhone(PhoneConstants.PHONE_TYPE_GSM);
            }

            if (mIsCdmaOnlyNumber) {
                return getProperPhone(PhoneConstants.PHONE_TYPE_CDMA);
            }

            return null;
        }
    }

    /**
     * DualTalk ECC Retry rule
     *
     */
    class EccRetryRule implements RuleHandler {
        public Phone handleRequest() {
            if (mIsEccRetry) {
                log("EccRetryRule: handleRequest...");
                return mEccRetryPhone;
            }

            return null;
        }
    }

    /**
     * CDMA and GSM register to network.
     */
    class CdmaAndGsmReadyRule implements RuleHandler {
        public Phone handleRequest() {
            if (mIsGsmOnlyNumber || mIsCdmaOnlyNumber) {
                return null;
            }

            log("CdmaAndGsmReadyRule: handleRequest...");

            // No operator requirement here
            if (isCdmaNetworkReady() && isGsmNetworkReady()) {
                if (isGsmPreferredNumber()) {
                    return getProperPhone(PhoneConstants.PHONE_TYPE_GSM);
                }

                if (isCdmaPreferredNumber()) {
                    return getProperPhone(PhoneConstants.PHONE_TYPE_CDMA);
                }
                return getProperPhone(PhoneConstants.PHONE_TYPE_GSM);
            }

            return null;
        }
    }

    /**
     * There is only gsm inserted.
     */
    class GsmReadyOnlyRule implements RuleHandler {
        public Phone handleRequest() {
            if (mIsGsmOnlyNumber || mIsCdmaOnlyNumber) {
                return null;
            }

            log("GsmReadyOnlyRule: handleRequest...");
            if (isGsmNetworkReady() && !isCdmaNetworkReady()) {
                return getProperPhone(PhoneConstants.PHONE_TYPE_GSM);
            }

            return null;
        }
    }

    /**
     * There is only cdma inserted.
     */
    class CdmaReadyOnlyRule implements RuleHandler {
        public Phone handleRequest() {
            if (mIsGsmOnlyNumber || mIsCdmaOnlyNumber) {
                return null;
            }

            log("CdmaReadyOnlyRule: handleRequest...");
            if (isCdmaNetworkReady() && !isGsmNetworkReady()) {
                return getProperPhone(PhoneConstants.PHONE_TYPE_CDMA);
            }

            return null;
        }
    }

    /**
     * DualTalk G+C no sim insert rule
     *
     */
    class GCUnReadyRule implements RuleHandler {
        public Phone handleRequest() {
            if (mIsGsmOnlyNumber || mIsCdmaOnlyNumber) {
                return null;
            }

            log("GCUnReadyRule: handleRequest...");

            // No operator requirement here
            if (!isCdmaNetworkReady() && !isGsmNetworkReady()) {
                if (isGsmPreferredNumber()) {
                    return getProperPhone(PhoneConstants.PHONE_TYPE_GSM);
                }

                if (isCdmaPreferredNumber()) {
                    return getProperPhone(PhoneConstants.PHONE_TYPE_CDMA);
                }
                return getProperPhone(PhoneConstants.PHONE_TYPE_GSM);
            }

            return null;

        }
    }

    /**
     * Handle ECC default case.
     */
    class DefaultHandler implements RuleHandler {
        public Phone handleRequest() {
            log("Can't got here! something is wrong!");
            return getProperPhone(PhoneConstants.PHONE_TYPE_GSM);
        }
    }
}
