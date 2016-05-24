package com.mediatek.nfc.dta;

public class EchoServerDefine {
    static public final String mPlugfestCases[] = {
        "nfc.plugfest.no.action",
        "nfc.plugfest.CO.TESTER",
        "nfc.plugfest.CO.TESTEE",
    };

    public static final int CO_SAP_IN = 0x16;//SAP iut,co-in-dest
    public static final String CO_NAME_IN = "urn:nfc:sn:dta-co-echo-in";



    public static final int CO_SAP_SRC = 0x23; //SAP iut,co-out-src
    public static final int CO_SAP_OUT = 0x12;//SAP lt,co-out-dest
    public static final String CO_NAME_OUT = "urn:nfc:sn:dta-co-echo-out"; //"what-the-fuck";

    public static final int CO_CONNECT_BY_NAME = 0x1240;
    public static final int CO_CONNECT_BY_SAP  = 0x1200;
    public static final int CO_CONNECT_BY_SNL  = 0x1280;



    public static final int MAX_ECHO_CO_BUFFER_SIZE = 1;
    public static final int CO_ECHO_OUT_DELAY = 800;
    public static final int DEFAULT_LLCP_MIU = 128; //from NativeNfcManager
    public static final int DEFAULT_LLCP_RWSIZE = 1; //from NativeNfcManager

    public static final int CL_SAP_IN = 0x15;//SAP iut,cl-in-dest
    public static final String CL_NAME_IN = "urn:nfc:sn:dta-cl-echo-in";


    public static final int CL_SAP_OUT = 0x11;//SAP lt,cl-out-dest
    public static final int CL_SAP_SRC = 0x22;//SAP lt,cl-out-src

    public static final String CL_NAME_OUT = "urn:nfc:sn:dta-cl-echo-out";

    //public static final String CL_NAME_SN = "urn:nfc:sn:cl-echo";




    public static final int DEFAULT_CL_SAP = 0x12;


    public static final String ACTION_SET_PATTERN_NUMBER =
       "com.mediatek.nfc.dta.ACTION_LLCP_SET_PATTERN";
    public static final String EXTRA_LLCP_PATTERN        = "com.mediatek.nfc.dta.LLCP_PATTERN";

    public static final int LLCP_PATTERN_INVALID         = -1;


    public static final String ACTION_LLCP_NOTIFY = "com.mediatek.nfc.dta.ACTION_LLCP_EVT";
    public static final String EXTRA_LLCP_STATUS = "com.mediatek.nfc.dta.LLCP_STATUS";
    //public static final String EXTRA_TEST_SCENARIO = "com.mediatek.nfc.dta.EXTRA_TEST_SCENARIO";


}
