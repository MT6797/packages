
package com.nb.mmitest;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentFilter.MalformedMimeTypeException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.nfc.tech.MifareClassic;
import android.nfc.tech.MifareUltralight;
import android.nfc.tech.Ndef;
import android.nfc.tech.NdefFormatable;
import android.nfc.tech.NfcA;
import android.nfc.tech.NfcB;
import android.nfc.tech.NfcF;
import android.nfc.tech.NfcV;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.LinearLayout.LayoutParams;

import java.nio.charset.Charset;
import java.util.Locale;

class NFCactive extends Test {
    private TestLayout1 mTestLayout1 = null;

    private NfcAdapter mNfcAdapter = null;

    private IntentFilter[] mIntentFilter = null;

    private PendingIntent mPendingIntent;

    private String[][] mTechLists;

    private boolean isNewIntent = false;
    
    private boolean nfcInitStatus = true;
    
    private boolean flag = true;
    
    private final static long SELEEP_TIME = 1000;
    
	//add state for NFC  begin
    private final static int PAUSE = 0xFFFC;
    private final static int NEWINTENT = 0xFFFB;
    private boolean mIsNFC=false;
    private Intent mNfcIntent=null;
    private static boolean has_NFC = true; //false;
    //add state for NFC  end


    NFCactive(ID pid, String s) {
        super(pid, s);
    }

    Handler enableHandler = new Handler()
    {

        @Override
        public void handleMessage(Message msg) {
            // TODO Auto-generated method stub
            switch (msg.what) {
                case 1:
                    mState = INIT;
                    Run();
                    break;
                default:
                    setContentView("NFC is opening...");
                    break;
            }
            super.handleMessage(msg);
        }
        
    };
    @Override
    protected void Run() {
        switch (mState) {
            case INIT:
            	//Check whether this phone should have NFC, slhuang
            	if (com.nb.mmitest.NFCactive.has_NFC == false){
            		setContentView("This phone needn't test NFC!" + "\n");
            		mState++;
            	}else{
            		if (isNewIntent) {
            			if (mNfcAdapter != null) {
            				mNfcAdapter.enableForegroundDispatch(mContext, mPendingIntent,
            						mIntentFilter, mTechLists);
            			}
            			// isNewIntent = false;
            		} else {
            			mIsNFC = true;
            			initNFC();
            			/*modify to just open NFC
                    if (mNfcAdapter != null) {
                        mNfcAdapter.enableForegroundDispatch(mContext, mPendingIntent,
                                mIntentFilter, mTechLists);
                    }

                    setContentView("Wait test...");
            			 */
            			//add to just open NFC
			
            			setContentView("NFC open success");
            		}
            	}
                break;
            case PAUSE:
                if (mNfcAdapter != null) {
                    mNfcAdapter.disableForegroundDispatch(mContext);
                }
                break;
            case NEWINTENT:
                isNewIntent = true;
                Tag tag = mNfcIntent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
                byte[] tagID = tag.getId();
                setContentView("ID:" + new String(tagID));
                break;
            case END:
                mIsNFC = false;
//                if(!nfcInitStatus) {
//                    mNfcAdapter.disable();
//                }
                break;
            default:
                break;
        }
    }

    private void setContentView(String msg) {
//modify   to just open NFC
    	//        mTestLayout1 = new TestLayout1(mContext, mName, new MyView().getBodyView(mContext, msg,
//                "Please put the phone on the simple fixture"));
    	 mTestLayout1 = new TestLayout1(mContext, mName, new MyView().getBodyView(mContext, msg,
         ""));
        mContext.setContentView(mTestLayout1.ll);
    }

    private void initNFC() {
        if (null == mContext) {
            Log.v(TAG, "mContext is null");
            return;
        }
        mNfcAdapter = NfcAdapter.getDefaultAdapter(mContext);
        if (mNfcAdapter == null) {
            // Toast.makeText(mContext, "Not NFC device",
            // Toast.LENGTH_LONG).show();
            // mContext.finish();
            return;
        }
        if (!mNfcAdapter.isEnabled()) {
            Log.i(TAG, "nfc is not enabled");
            new Thread(new Runnable() {
                
                @Override
                public void run() {
                    // TODO Auto-generated method stub
                    mNfcAdapter.enable();
                    while (flag) {
                    	try{
                    		Thread.sleep(SELEEP_TIME);
                            if(mNfcAdapter.isEnabled()) {
                                Message message = Message.obtain();
                                message.what = 1;
                                enableHandler.sendMessage(message);
                                flag = false;
                                Thread.currentThread().interrupt();
                            }
                    	}
                    	catch(Exception e) {
                    		e.printStackTrace();
                    	}
                    }
                    
                }
            }).start();
            nfcInitStatus = false;
            return;
        }
        /* modify  to just open the NFC
        Intent intent = new Intent(mContext, mContext.getClass());
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        mPendingIntent = PendingIntent.getActivity(mContext, 0, intent, 0);
        IntentFilter ndefFilter = new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED);
        IntentFilter techFilter = new IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED);
        IntentFilter tagFilter = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);
        try {
            ndefFilter.addDataType("text/plain ");
        } catch (MalformedMimeTypeException e) {
            e.printStackTrace();
        }
        mIntentFilter = new IntentFilter[] {
                ndefFilter, techFilter, tagFilter
        };
        mTechLists = new String[][] {
                new String[] {
                        Ndef.class.getName(), NdefFormatable.class.getName(),
                        MifareClassic.class.getName(), MifareUltralight.class.getName()
                },
                new String[] {
                        NfcA.class.getName(), NfcB.class.getName(), NfcF.class.getName(),
                        NfcV.class.getName(), IsoDep.class.getName()
                }
        };*/
    }
    // add state for NFC  begin
    public void Pause() {
        if (mIsNFC) {
            mState = PAUSE;
            Run();
        }
    };

    public void NewIntent(Intent intent) {
        if (mIsNFC) {
            mState = NEWINTENT;
            mNfcIntent=intent;
            Run();
        }
    };

    // add state for NFC - end

}

class MyView {
    public View getBodyView(Context mContext, String body, String explain) {
        LinearLayout ll = new LinearLayout(mContext);
        ll.setOrientation(LinearLayout.VERTICAL);
        ll.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT,
                LayoutParams.FILL_PARENT, 1.0f));

        TextView bodyTV = new TextView(mContext);
        bodyTV.setGravity(Gravity.CENTER);
        // tvbody.setTypeface(Typeface.MONOSPACE, 1);
        bodyTV.setTextAppearance(mContext, android.R.style.TextAppearance_Large);
        bodyTV.setText(body);

        TextView explainTV = new TextView(mContext);
        explainTV.setTextAppearance(mContext, android.R.style.TextAppearance_Small);
        explainTV.setText(explain);

        ll.setGravity(Gravity.TOP);
        ll.addView(bodyTV, new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT,
                LayoutParams.WRAP_CONTENT, 1.0f));
        ll.setGravity(Gravity.BOTTOM);
        ll.addView(explainTV);
        return ll;
    }
}

class NFCpassive extends Test {
    private NfcAdapter mNfcAdapter = null;

    private TestLayout1 mTestLayout1 = null;

    private NdefMessage mMessage;
    
	//add state for NFC  begin
    private final static int PAUSE = 0xFFFC;
    private final static int NEWINTENT = 0xFFFB;
    private boolean mIsNFC=false;
    private static boolean has_NFC = true; //false;
    private Intent mNfcIntent=null;
    //add state for NFC  end


    NFCpassive(ID pid, String s) {
        super(pid, s);
    }

    @Override
    protected void Run() {
        switch (mState) {
            case INIT:
            	//Check whether this phone should have NFC, slhuang
            	if (com.nb.mmitest.NFCpassive.has_NFC == false){
               	 	mTestLayout1 = new TestLayout1(mContext, mName, 
               	 			new MyView().getBodyView(mContext, "This phone needn't test NFC!",
                 ""));
               	 	mContext.setContentView(mTestLayout1.ll);
            		mState++;
            	}else{

            		mIsNFC = true;
            		initNFC();
            		//add  to just open NFC
			
            		if(mNfcAdapter!=null&&mNfcAdapter.isEnabled()){
            			mTestLayout1 = new TestLayout1(mContext, mName, new MyView().getBodyView(mContext, "NFC open success",
            			""));
            			mContext.setContentView(mTestLayout1.ll);
            		}else{	
			
            			mTestLayout1 = new TestLayout1(mContext, mName, new MyView().getBodyView(mContext, "NFC open fault",
            			""));
            			mContext.setContentView(mTestLayout1.ll);
            		}

            		/*modify  to just open NFC
                if (mNfcAdapter != null) {
                    mNfcAdapter.enableForegroundNdefPush(mContext, mMessage);
                }
                mTestLayout1 = new TestLayout1(mContext, mName, new MyView().getBodyView(mContext,
                        "Wait test...", "Please put the phone on the simple fixture."
                                + "please check the light of the fixture on the right. "
                                + "If the light is green, there is OK. "
                                + "if the light is red, there is Fail."));
                mContext.setContentView(mTestLayout1.ll);*/
            	}
                break;
            case PAUSE:
                if (mNfcAdapter != null) {
                    mNfcAdapter.disableForegroundNdefPush(mContext);
                }
                break;
            case NEWINTENT:
                break;
            case END:
                break;
            default:
                break;
        }
    }

    private void initNFC() {
        mNfcAdapter = NfcAdapter.getDefaultAdapter(mContext);
        if (mNfcAdapter == null) {
            // Toast.makeText(mContext, "Not NFC device",
            // Toast.LENGTH_LONG).show();
            // mContext.finish();
            return;
        }
        /*modify  to just open NFC
        mMessage = new NdefMessage(new NdefRecord[] {
            newTextRecord("devil may cry", Locale.CHINA, true)
        });
        */
    }

    private NdefRecord newTextRecord(String text, Locale locale, boolean encodeInUtf8) {
        byte[] langBytes = locale.getLanguage().getBytes(Charset.forName("UTF-8"));

        Charset utfEncoding = encodeInUtf8 ? Charset.forName("UTF-8") : Charset.forName("UTF-16");
        byte[] textBytes = text.getBytes(utfEncoding);

        int utfBit = encodeInUtf8 ? 0 : (1 << 7);
        char status = (char) (utfBit + langBytes.length);

        byte[] data = new byte[1 + langBytes.length + textBytes.length];
        data[0] = (byte) status;
        System.arraycopy(langBytes, 0, data, 1, langBytes.length);
        System.arraycopy(textBytes, 0, data, 1 + langBytes.length, textBytes.length);

        return new NdefRecord(NdefRecord.TNF_WELL_KNOWN, NdefRecord.RTD_TEXT, new byte[0], data);
    }
    // add state for NFC  begin
    public void Pause() {
        if (mIsNFC) {
            mState = PAUSE;
            Run();
        }
    };

    public void NewIntent(Intent intent) {
        if (mIsNFC) {
            mState = NEWINTENT;
            mNfcIntent=intent;
            Run();
        }
    };

    // add state for NFC - end

}
