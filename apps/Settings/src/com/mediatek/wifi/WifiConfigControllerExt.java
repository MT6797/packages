package com.mediatek.wifi;

import static android.net.wifi.WifiConfiguration.INVALID_NETWORK_ID;

import android.app.AlertDialog;
import android.content.Context;
import android.net.StaticIpConfiguration;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiConfiguration.GroupCipher;
import android.net.wifi.WifiConfiguration.KeyMgmt;
import android.net.wifi.WifiConfiguration.PairwiseCipher;
import android.net.wifi.WifiConfiguration.Protocol;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.security.Credentials;
import android.security.KeyStore;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.android.settings.R;
import com.android.settingslib.wifi.AccessPoint;
import com.android.settings.wifi.WifiConfigController;
import com.android.settings.wifi.WifiConfigUiBase;
import com.mediatek.settings.ext.IWifiExt;
import com.mediatek.settings.FeatureOption;
import com.mediatek.settings.UtilsExt;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

public class WifiConfigControllerExt {
    private static final String TAG = "WifiConfigControllerExt";

    //sim/aka
    public static final int WIFI_EAP_METHOD_SIM = 4;
    public static final int WIFI_EAP_METHOD_AKA = 5;
    public static final int WIFI_EAP_METHOD_AKA_PLUS = 6;
    private static final String SIM_STRING = "SIM";
    private static final String AKA_STRING = "AKA";
    private static final String AKA_PLUS_STRING = "AKA\'";
    public static final int WIFI_EAP_METHOD_DUAL_SIM = 2;

    //add for EAP_SIM/AKA
    private Spinner mSimSlot;
    private TelephonyManager mTelephonyManager;

    // add for WAPI
    private Spinner mWapiAsCert;
    private Spinner mWapiClientCert;
    private boolean mHex;
    private static final String WLAN_PROP_KEY = "persist.sys.wlan";
    private static final String WIFI = "wifi";
    private static final String WAPI = "wapi";
    private static final String WIFI_WAPI = "wifi-wapi";
    private static final String DEFAULT_WLAN_PROP = WIFI_WAPI;
    public static final int SECURITY_WAPI_PSK = 4;
    public static final int SECURITY_WAPI_CERT = 5;

    /* WFA test support */
    private static final String KEY_PROP_WFA_TEST_SUPPORT = "persist.radio.wifi.wpa2wpaalone";
    private static final String KEY_PROP_WFA_TEST_VALUE = "true";
    private static String sWFATestFlag = null;

    // add for plug in
    private IWifiExt mExt;

    private Context mContext;
    private View mView;
    private WifiConfigUiBase mConfigUi;
    private WifiConfigController mController;

    public WifiConfigControllerExt(WifiConfigController controller,
            WifiConfigUiBase configUi, View view) {
        mController = controller;
        mConfigUi = configUi;
        mContext = mConfigUi.getContext();
        mView = view;
        mExt = UtilsExt.getWifiPlugin(mContext);
        // get telephonyManager
        mTelephonyManager = (TelephonyManager) mContext.
                getSystemService(Context.TELEPHONY_SERVICE);
    }

    public void addViews(WifiConfigUiBase configUi, String security) {
        ViewGroup group = (ViewGroup) mView.findViewById(R.id.info);
        //add security information
        View row = configUi.getLayoutInflater().inflate(
                    R.layout.wifi_dialog_row, group, false);
        ((TextView) row.findViewById(R.id.name)).setText(
                    configUi.getContext().getString(R.string.wifi_security));
        mExt.setSecurityText((TextView) row.findViewById(R.id.name));
        ((TextView) row.findViewById(R.id.value)).setText(security);
        group.addView(row);
    }

    /**
     *add quote for strings
     * @param string
     * @return add quote to the string
     */
    public static String addQuote(String s) {
          return "\"" + s + "\"";
    }

    public boolean enableSubmitIfAppropriate(TextView passwordView,
            int accessPointSecurity, boolean pwInvalid) {
        boolean passwordInvalid = pwInvalid;

        if (passwordView != null
            && ((accessPointSecurity == AccessPoint.SECURITY_WEP
            && !isWEPKeyValid(passwordView.getText().toString()))
               || ((accessPointSecurity == AccessPoint.SECURITY_PSK
               && passwordView.length() < 8)
               || (accessPointSecurity == SECURITY_WAPI_PSK
               && (passwordView.length() < 8
               || 64 < passwordView.length() || (mHex && !passwordView
                   .getText().toString().matches("[0-9A-Fa-f]*"))))))) {
            passwordInvalid = true;
        }

        //verify WAPI information
        if (accessPointSecurity == SECURITY_WAPI_CERT
            && (mWapiAsCert != null
                && mWapiAsCert.getSelectedItemPosition() == 0
                || mWapiClientCert != null
                && mWapiClientCert.getSelectedItemPosition() == 0)) {
              passwordInvalid = true;
        }

        return passwordInvalid;

    }

    /**
     * verify password check whether we have got a valid WEP key
     *
     * @param password
     * @return
     */
    private boolean isWEPKeyValid(String password) {
          if (password == null || password.length() == 0) {
                return false;
          }
          int keyLength = password.length();
          if (((keyLength == 10 || keyLength == 26 || keyLength == 32)
                  && password.matches("[0-9A-Fa-f]*"))
                  || (keyLength == 5 || keyLength == 13 || keyLength == 16)) {
                return true;
          }
          return false;
    }

    public void setConfig(WifiConfiguration config,
            int accessPointSecurity, TextView passwordView,
            Spinner eapMethodSpinner) {
        //get priority of configuration
        config.priority = mExt.getPriority(config.priority);

        switch (accessPointSecurity) {
            case AccessPoint.SECURITY_EAP:
                config.simSlot = addQuote("-1");
                Log.d(TAG, "(String) eapMethodSpinner.getSelectedItem()="
                    + (String) eapMethodSpinner.getSelectedItem());
                if (AKA_STRING.equals((String) eapMethodSpinner.getSelectedItem())
                    || SIM_STRING.equals((String) eapMethodSpinner.getSelectedItem())
                    || AKA_PLUS_STRING.equals((String) eapMethodSpinner.getSelectedItem())) {
                    eapSimAkaSimSlotConfig(config, eapMethodSpinner);
                    Log.d(TAG, "eap-sim/aka, config.toString(): "
                        + config.toString());
                }
                break;

            // add WAPI_PSK & WAPI_CERT
            case SECURITY_WAPI_PSK:
                  config.allowedKeyManagement.set(KeyMgmt.WAPI_PSK);
                  config.allowedProtocols.set(Protocol.WAPI);
                  config.allowedPairwiseCiphers.set(PairwiseCipher.SMS4);
                  config.allowedGroupCiphers.set(GroupCipher.SMS4);
                  if (passwordView.length() != 0) {
                        String password = passwordView.getText().toString();
                        Log.v(TAG, "getConfig(), mHex=" + mHex);
                        if (mHex) { /* Hexadecimal */
                            config.preSharedKey = password;
                        } else { /* ASCII */
                            config.preSharedKey = '"' + password + '"';
                        }
                  }
                  break;

            case SECURITY_WAPI_CERT:
                  config.allowedKeyManagement.set(KeyMgmt.WAPI_CERT);
                  config.allowedProtocols.set(Protocol.WAPI);
                  config.allowedPairwiseCiphers.set(PairwiseCipher.SMS4);
                  config.allowedGroupCiphers.set(GroupCipher.SMS4);
                  config.enterpriseConfig.setCaCertificateWapiAlias(
                      (mWapiAsCert.getSelectedItemPosition() == 0) ? ""
                      : (String) mWapiAsCert.getSelectedItem());
                  config.enterpriseConfig.setClientCertificateWapiAlias(
                      (mWapiClientCert.getSelectedItemPosition() == 0) ? ""
                      : (String) mWapiClientCert.getSelectedItem());
                  break;
            default:
                  break;
        }
    }

    /**
     * Geminu plus
     */
     private void eapSimAkaSimSlotConfig(WifiConfiguration config,
             Spinner eapMethodSpinner) {
       if (mSimSlot == null) {
           Log.d(TAG, "mSimSlot is null");
           mSimSlot = (Spinner) mView.findViewById(R.id.sim_slot);
       }
       String strSimAka = (String) eapMethodSpinner.getSelectedItem();
       if (TelephonyManager.getDefault().getPhoneCount()
           == WIFI_EAP_METHOD_DUAL_SIM) {
           Log.d(TAG, "((String) mSimSlot.getSelectedItem()) "
               + ((String) mSimSlot.getSelectedItem()));
           simSlotConfig(config, strSimAka);
           Log.d(TAG, "eap-sim, choose sim_slot"
               + (String) mSimSlot.getSelectedItem());
       }
       Log.d(TAG, "eap-sim, config.simSlot: " + config.simSlot);
   }

   /**
    *  Geminu plus
    */
   private void simSlotConfig(WifiConfiguration config, String strSimAka) {
       int simSlot = mSimSlot.getSelectedItemPosition() - 1;
       if (simSlot > -1) {
           //marked for FW not ready
           config.simSlot = addQuote("" + simSlot);
           Log.d(TAG, "config.simSlot " + addQuote("" + simSlot));
       }
   }

   public void setEapmethodSpinnerAdapter() {
       Spinner eapMethodSpinner = (Spinner) mView.findViewById(R.id.method);

       //set array for eap method spinner. CMCC will show only eap and sim
       int spinnerId = R.array.wifi_eap_method;

       Context context = mConfigUi.getContext();
       String[] eapString = context.getResources().getStringArray(spinnerId);
       ArrayList<String> eapList = new ArrayList<String>(Arrays.asList(eapString));
       final ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                context, android.R.layout.simple_spinner_item, eapList);
       if (mController.getAccessPoint() != null) {
           mExt.setEapMethodArray(
               adapter,
               mController.getAccessPointSsid(),
               mController.getAccessPointSecurity());
       }
       adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
       //M:added by Parish Li for triggering onItemSelected
       eapMethodSpinner.setAdapter(adapter);
   }

   //M:added by Parish Li for hidding the fields when eap-fast,sim and aka
   public void setEapMethodFields(boolean edit) {
       Spinner eapMethodSpinner = (Spinner) mView.findViewById(R.id.method);

       // eap-sim/aka/aka'
       int eapMethod = eapMethodSpinner.getSelectedItemPosition();
       //for CMCC-AUTO eap Method config information
       if (mController.getAccessPoint() != null) {
           eapMethod = mExt.getEapMethodbySpinnerPos(
               eapMethod,
               mController.getAccessPointSsid(),
               mController.getAccessPointSecurity());
       }
       Log.d(TAG, "showSecurityFields modify method = " + eapMethod);

       //for CMCC ignore some config information
       mExt.hideWifiConfigInfo(new IWifiExt.Builder()
            .setAccessPoint(mController.getAccessPoint())
            .setEdit(edit)
            .setViews(mView), mConfigUi.getContext());
   }

   /**
    *  Geminu plus
    */
   public void setGEMINI(int eapMethod) {
       Spinner eapMethodSpinner = (Spinner) mView.findViewById(R.id.method);

       //for CMCC-AUTO eap Method config information
       if (mController.getAccessPoint() != null) {
           eapMethod = mExt.getEapMethodbySpinnerPos(
               eapMethod,
               mController.getAccessPointSsid(),
               mController.getAccessPointSecurity());
       }

       if (eapMethod == WIFI_EAP_METHOD_SIM
               || eapMethod == WIFI_EAP_METHOD_AKA
               || eapMethod == WIFI_EAP_METHOD_AKA_PLUS) {
           if (TelephonyManager.getDefault().getPhoneCount()
               == WIFI_EAP_METHOD_DUAL_SIM) {
               mView.findViewById(R.id.sim_slot_fields).
                   setVisibility(View.VISIBLE);
               mSimSlot = (Spinner) mView.findViewById(R.id.sim_slot);
               //Geminu plus
               Context context = mConfigUi.getContext();
               String[] tempSimAkaMethods = context.getResources().
                   getStringArray(R.array.sim_slot);
               int sum = mTelephonyManager.getSimCount();
               Log.d(TAG, "the num of sim slot is :" + sum);
               String[] simAkaMethods = new String[sum + 1];
               for (int i = 0; i < (sum + 1); i++) {
                   if (i < tempSimAkaMethods.length) {
                       simAkaMethods[i] = tempSimAkaMethods[i];
                   } else {
                       simAkaMethods[i] = tempSimAkaMethods[1].
                           replaceAll("1", "" + i);
                   }
               }
               final ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                   context, android.R.layout.simple_spinner_item, simAkaMethods);
               adapter.setDropDownViewResource(
                   android.R.layout.simple_spinner_dropdown_item);
               mSimSlot.setAdapter(adapter);

               //setting had selected simslot
               if (mController.getAccessPoint() != null
                   && mController.getAccessPoint().isSaved()) {
                   WifiConfiguration config = mController.getAccessPointConfig();
                   //marked for FW not ready
                   if (config != null && config.simSlot != null) {
                       String[] simslots = config.simSlot.split("\"");
                       if (simslots.length > 1) {
                           int slot = Integer.parseInt(simslots[1]) + 1;
                           mSimSlot.setSelection(slot);
                       }
                   }
               }
           }
       } else {
           if (TelephonyManager.getDefault().getPhoneCount()
               == WIFI_EAP_METHOD_DUAL_SIM) {
               mView.findViewById(R.id.sim_slot_fields).setVisibility(
                           View.GONE);
           }
       }
   }

   /**
    *  add for WAPI
    */
   public boolean showSecurityFields(int accessPointSecurity, boolean edit) {
       Log.d(TAG, "showSecurityFields, accessPointSecurity = "
           + accessPointSecurity);
       Log.d(TAG, "showSecurityFields, edit = " + edit);

       if (accessPointSecurity != AccessPoint.SECURITY_EAP) {
           ((TextView) mView.findViewById(R.id.identity)).setEnabled(true);
           ((CheckBox) mView.findViewById(R.id.show_password)).setEnabled(true);
           //hide eap fileds
           mView.findViewById(R.id.eap).setVisibility(View.GONE);
           mView.findViewById(R.id.eap_identity).setVisibility(View.GONE);
       }

       // Hexadecimal checkbox only for WAPI_PSK
       mView.findViewById(R.id.hex_password).setVisibility(View.GONE);
       if (accessPointSecurity == SECURITY_WAPI_PSK) {
             mView.findViewById(R.id.hex_password).setVisibility(View.VISIBLE);
             ((CheckBox) mView.findViewById(R.id.hex_password)).setChecked(mHex);
       }

       // show WAPI CERT field
       if (accessPointSecurity == SECURITY_WAPI_CERT) {
             mView.findViewById(R.id.security_fields).setVisibility(View.GONE);
             mView.findViewById(R.id.wapi_cert_fields).setVisibility(
                         View.VISIBLE);
             mWapiAsCert = (Spinner) mView.findViewById(R.id.wapi_as_cert);
             mWapiClientCert = (Spinner) mView.findViewById(R.id.wapi_user_cert);
             mWapiAsCert.setOnItemSelectedListener(mController);
             mWapiClientCert.setOnItemSelectedListener(mController);
             loadCertificates(mWapiAsCert, Credentials.WAPI_SERVER_CERTIFICATE);
             loadCertificates(mWapiClientCert, Credentials.WAPI_USER_CERTIFICATE);

             if (mController.getAccessPoint() != null
                 && mController.getAccessPoint().isSaved()) {
                   WifiConfiguration config = mController.getAccessPointConfig();
                   setCertificate(mWapiAsCert, Credentials.WAPI_SERVER_CERTIFICATE,
                           config.enterpriseConfig.getCaCertificateWapiAlias());
                   setCertificate(mWapiClientCert,
                       Credentials.WAPI_USER_CERTIFICATE,
                       config.enterpriseConfig.getClientCertificateWapiAlias());
             }
             return true;
       }

       // show eap identity field
       if (accessPointSecurity == AccessPoint.SECURITY_EAP) {
           mView.findViewById(R.id.eap_identity).setVisibility(View.VISIBLE);
       }

       // set setOnClickListener for hex password
       setHexCheckBoxListener();

       //for CMCC ignore some config information
       mExt.hideWifiConfigInfo(new IWifiExt.Builder()
                   .setAccessPoint(mController.getAccessPoint())
                   .setEdit(edit)
                   .setViews(mView), mConfigUi.getContext());

       return false;
   }

    /**
     *  add for WAPI
     */
    public void setWapiCertSpinnerInvisible(int accessPointSecurity) {
        if (accessPointSecurity != SECURITY_WAPI_CERT) {
            /// M: hide WAPI_CERT fileds
            mView.findViewById(R.id.wapi_cert_fields).setVisibility(View.GONE);
        }
    }

    /**
     *  add for WAPI
     */
    public void setHexCheckBoxListener() {
        // set setOnClickListener for hex password
        ((CheckBox) mView.findViewById(R.id.hex_password)).
            setOnCheckedChangeListener(mController);
    }

    private void setCertificate(Spinner spinner, String prefix, String cert) {
        if (cert != null && cert.startsWith(prefix)) {
            setSelection(spinner, cert.substring(prefix.length()));
        }
    }

    private void setSelection(Spinner spinner, String value) {
        if (value != null) {
            @SuppressWarnings("unchecked")
            ArrayAdapter<String> adapter = (ArrayAdapter<String>) spinner
                         .getAdapter();
            for (int i = adapter.getCount() - 1; i >= 0; --i) {
                if (value.equals(adapter.getItem(i))) {
                    spinner.setSelection(i);
                        break;
                }
            }
        }
    }

    private void loadCertificates(Spinner spinner, String prefix) {
        final Context context = mConfigUi.getContext();
        String unspecifiedCert = context.getString(R.string.wifi_unspecified);

        String[] certs = KeyStore.getInstance().list(prefix,
            android.os.Process.WIFI_UID);
        if (certs == null || certs.length == 0) {
            certs = new String[] {unspecifiedCert};
        } else {
            final String[] array = new String[certs.length + 1];
            array[0] = unspecifiedCert;
            System.arraycopy(certs, 0, array, 1, certs.length);
            certs = array;
        }

        final ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                context, android.R.layout.simple_spinner_item, certs);
        adapter.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
    }

    private void addRow(ViewGroup group, int name, String value) {
        View row = mConfigUi.getLayoutInflater().
            inflate(R.layout.wifi_dialog_row, group, false);
        ((TextView) row.findViewById(R.id.name)).setText(name);
        ((TextView) row.findViewById(R.id.value)).setText(value);
        group.addView(row);
    }

    public void setHex(boolean hexEnabled) {
        mHex = hexEnabled;
    }

     public int getEapMethod(int eapMethod) {
         Log.d(TAG, "getEapMethod, eapMethod = " + eapMethod);
         int result = eapMethod;
         if (mController.getAccessPoint() != null) {
             result = mExt.getEapMethodbySpinnerPos(
                 eapMethod, mController.getAccessPointSsid(),
                 mController.getAccessPointSecurity());
         }
         Log.d(TAG, "getEapMethod, result = " + result);
         return result;
     }

     public void setEapMethodSelection(Spinner eapMethodSpinner, int eapMethod) {
         int eapMethodPos = eapMethod;
         if (mController.getAccessPoint() != null) {
             eapMethodPos = mExt.getPosByEapMethod(eapMethod,
                 mController.getAccessPointSsid(),
                 mController.getAccessPointSecurity());
         }
         eapMethodSpinner.setSelection(eapMethodPos);
         Log.d(TAG, "[skyfyx]showSecurityFields modify pos = "
             + eapMethodPos);
         Log.d(TAG, "[skyfyx]showSecurityFields modify method = "
             + eapMethod);

     }

     public void setProxyText(View view) {
         //set text of proxy exclusion list
         TextView proxyText = (TextView) view.
             findViewById(R.id.proxy_exclusionlist_text);
         mExt.setProxyText(proxyText);
     }

     public void restrictIpv4View(WifiConfiguration config) {
         TextView ipAddressView = (TextView) mView.findViewById(R.id.ipaddress);
         TextView gatewayView = (TextView) mView.findViewById(R.id.gateway);
         TextView networkPrefixLengthView = (TextView) mView.
             findViewById(R.id.network_prefix_length);
         TextView dns1View = (TextView) mView.findViewById(R.id.dns1);
         TextView dns2View = (TextView) mView.findViewById(R.id.dns2);
         //restrict static IP to IPv4
         StaticIpConfiguration staticConfig = config.getStaticIpConfiguration();
         Log.d(TAG, "staticConfig = " + staticConfig);
         if (staticConfig != null) {
             Log.d(TAG, "IpAddressView = " + staticConfig.ipAddress);
             if (staticConfig.ipAddress != null
                 && (staticConfig.ipAddress.getAddress() instanceof Inet4Address)) {
                 ipAddressView.setText(
                         staticConfig.ipAddress.getAddress().getHostAddress());
                 networkPrefixLengthView.setText(Integer.toString(
                     staticConfig.ipAddress
                     .getNetworkPrefixLength()));
             }

             Log.d(TAG, "gatewayView = " + staticConfig.gateway);
             if (staticConfig.gateway != null
                 && (staticConfig.gateway instanceof Inet4Address)) {
                 gatewayView.setText(staticConfig.gateway.getHostAddress());
             }

             Iterator<InetAddress> dnsIterator = staticConfig.
                 dnsServers.iterator();
             while (dnsIterator.hasNext()) {
                 InetAddress dsn1 = dnsIterator.next();
                 Log.d(TAG, "dsn1 = " + dsn1);
                 if (dsn1 instanceof Inet4Address) {
                     dns1View.setText(dsn1.getHostAddress());
                     break;
                 }
             }
             while (dnsIterator.hasNext()) {
                 InetAddress dsn2 = dnsIterator.next();
                 Log.d(TAG, "dsn2 = " + dsn2);
               if (dsn2 instanceof Inet4Address) {
                   dns2View.setText(dsn2.getHostAddress());
                     break;
               }
             }
         }
     }

    /**
     * 1.add some more security spinners to support WFA test and wapi
     * 2.switch spinner according to WFA test and WIFI & WAPI config
     */
    public void addWifiConfigView(boolean edit) {
        //set security text
        TextView securityText = (TextView) mView.findViewById(R.id.security_text);
        mExt.setSecurityText(securityText);
        if (mController.getAccessPoint() == null) {
            // set array for wifi security
            int viewId = R.id.security;
            if (FeatureOption.MTK_WAPI_SUPPORT) {
                  String type = SystemProperties.get(WLAN_PROP_KEY,
                              DEFAULT_WLAN_PROP);
                  if (type.equals(WIFI_WAPI)) {
                        if (isWFATestSupported()) {
                              viewId = R.id.security_wfa; // WIFI + WAPI, support
                              // separate WPA2 PSK
                              // security
                        } else {
                              viewId = R.id.security; // WIFI + WAPI
                        }
                  } else if (type.equals(WIFI)) {
                        if (isWFATestSupported()) {
                              viewId = R.id.wpa_security_wfa; // WIFI only, support
                              // separate WPA2 PSK
                              // security
                        } else {
                              viewId = R.id.wpa_security; // WIFI only
                        }
                  } else if (type.equals(WAPI)) {
                        viewId = R.id.wapi_security; // WAPI only
                  }
            } else {
                  if (isWFATestSupported()) {
                        viewId = R.id.wpa_security_wfa; // WIFI only, support
                        // separate WPA and WPA2 PSK
                        // security
                  } else {
                        viewId = R.id.wpa_security; // WIFI only
                  }
            }
            switchWlanSecuritySpinner((Spinner) mView.findViewById(viewId));
        } else {
            WifiConfiguration config = mController.getAccessPointConfig();
            Log.d(TAG, "addWifiConfigView, config = " + config);
            // get plug in,whether to show access point priority select
            // spinner.
            mExt.setAPNetworkId(config);
            if (mController.getAccessPoint().isSaved() && config != null) {
                  Log.d(TAG, "priority=" + config.priority);
                  mExt.setAPPriority(config.priority);
            }

            mExt.setPriorityView((LinearLayout) mView.findViewById(R.id.priority_field),
                    config,
                    edit);
        }
        mExt.addDisconnectButton((AlertDialog) mConfigUi, edit, mController.getAccessPointState(),
                mController.getAccessPointConfig());

        //for CMCC ignore some config information
        mExt.hideWifiConfigInfo(new IWifiExt.Builder()
                    .setAccessPoint(mController.getAccessPoint())
                    .setEdit(edit)
                    .setViews(mView), mConfigUi.getContext());
    }

    /**
     * 1.all security spinners are set visible in wifi_dialog.xml
     * 2.switch WLAN security spinner in different config
     * 3.the config includes whether to pass WFA test and how wifi and wapi
     * are configured
     */
    private void switchWlanSecuritySpinner(Spinner securitySpinner) {
          ((Spinner) mView.findViewById(R.id.security)).setVisibility(View.GONE);
          ((Spinner) mView.findViewById(R.id.wapi_security))
                      .setVisibility(View.GONE);
          ((Spinner) mView.findViewById(R.id.wpa_security))
                      .setVisibility(View.GONE);
          ((Spinner) mView.findViewById(R.id.security_wfa))
                      .setVisibility(View.GONE);
          ((Spinner) mView.findViewById(R.id.wpa_security_wfa))
                      .setVisibility(View.GONE);

          securitySpinner.setVisibility(View.VISIBLE);
          securitySpinner.setOnItemSelectedListener(mController);
    }

    /**
     *  get the security to its corresponding security spinner position
     */
     public int getSecurity(int accessPointSecurity) {
         Log.d(TAG, "getSecurity, accessPointSecurity = " + accessPointSecurity);
         // only WPAI supported
         if (FeatureOption.MTK_WAPI_SUPPORT) {
             String type = SystemProperties.get(WLAN_PROP_KEY,
                     DEFAULT_WLAN_PROP);
             if(type.equals(WAPI) && accessPointSecurity > 0) {
                 accessPointSecurity += SECURITY_WAPI_PSK
                            - AccessPoint.SECURITY_WEP;
             } else if(type.equals(WIFI)) {
                 if (isWFATestSupported()
                     && accessPointSecurity > AccessPoint.SECURITY_PSK) {
                     accessPointSecurity -= 1;
                 }
             } else if (type.equals(WIFI_WAPI)) {
                 if (isWFATestSupported()
                     && accessPointSecurity > AccessPoint.SECURITY_PSK) {
                     accessPointSecurity -= 1;
                 }
             }
         } else if (isWFATestSupported()) {
             if (accessPointSecurity > AccessPoint.SECURITY_PSK) {
                 accessPointSecurity -= 1;
             }
         }
         Log.d(TAG, "getSecurity, accessPointSecurity = " + accessPointSecurity);
         return accessPointSecurity;
     }

    /**
     *  support WFA test
     */
    private static boolean isWFATestSupported() {
        if (sWFATestFlag == null) {
            sWFATestFlag = SystemProperties.get(KEY_PROP_WFA_TEST_SUPPORT, "");
            Log.d(TAG, "isWFATestSupported(), sWFATestFlag=" + sWFATestFlag);
        }
        return KEY_PROP_WFA_TEST_VALUE.equals(sWFATestFlag);
    }
}
