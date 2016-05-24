
package com.android.bluetooth.bip;

import android.content.Context;
import android.util.Log;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.StringReader;

/**
 * This class is used to parse variables from ImageDescriptor of BIP spec.
 */
public class BluetoothBipObjImageDescriptor {

    private static final String TAG = "BluetoothBipObjImageDescriptor";
    private static final boolean D = true;
    private static final boolean V = true;
    private Context mContext = null;

    /* Varables of request ImageDescriptor */
    private int mLength = 0;
    private String mEncoding = null;
    private String mPixel = null;
    private int mSize = INVALID_VALUE; // only make sense for PutImage function
    private int mMaxsize = INVALID_VALUE; // only make sense for GetImage function
    private String mTranformation = null; // not implement
    /* mWidth1 & mHeight1 are used for fix pixel */
    /* mWidth2 & mHeight2 are used for range pixel */
    private int mWidth1 = INVALID_VALUE;
    private int mHeight1 = INVALID_VALUE;
    private int mWidth2 = INVALID_VALUE;
    private int mHeight2 = INVALID_VALUE;
    private boolean mIsValidPixel = false;
    private PIXEL_TYPE mPixelType = PIXEL_TYPE.FixPixel;
    // default values
    public static final int INVALID_VALUE = -1;

    /** Enum value for request type of image from client. */
    public enum PIXEL_TYPE {
        FixPixel,
        RangePixel,
        RatioAssign;
    }

    /**
     * Set image properties by image uri.
     * @param context context
     * @param input input descriptor in byte array
     */
    public BluetoothBipObjImageDescriptor(Context context, byte[] input) {
        mContext = context;
        mLength = input.length;
    }

    /**
     * Parse the request parameter into local variables.
     * The string variable such as mEncoding & mPixel will remain null
     * if the value parsed inside is empty.
     * @param input The descriptor string
     */
    public void parseImageDescriptor(String input) {
        Log.v(TAG, "parseImageDescriptor:");
        try {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(true);
            XmlPullParser xpp = factory.newPullParser();
            //ImageDescriptor request = new ImageDescriptor();
            String tmpTag = null;
            String tmpAttr = null;

            xpp.setInput(new StringReader(input));
            int eventType = xpp.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_DOCUMENT) {
                } else if (eventType == XmlPullParser.START_TAG) {
                    tmpTag = xpp.getName();
                    if (tmpTag.equals("image")) {
                        tmpAttr = xpp.getAttributeValue(null, "encoding");
                        if ((tmpAttr != null) && (tmpAttr.length() > 0)) {
                            mEncoding = tmpAttr;
                        }

                        tmpAttr = xpp.getAttributeValue(null, "pixel");
                        if ((tmpAttr != null) && (tmpAttr.length() > 0)) {
                            mPixel = tmpAttr;
                            Log.v(TAG, "mPixel: " + mPixel);
                            parsePixel(mPixel);
                        }

                        tmpAttr = xpp.getAttributeValue(null, "size");
                        if ((tmpAttr != null) && (tmpAttr.length() > 0)) {
                            mSize = Integer.parseInt(tmpAttr.trim());
                        }

                        tmpAttr = xpp.getAttributeValue(null, "maxsize");
                        if ((tmpAttr != null) && (tmpAttr.length() > 0)) {
                            mMaxsize = Integer.parseInt(tmpAttr.trim());
                        }

                        tmpAttr = xpp.getAttributeValue(null, "transformation");
                        if ((tmpAttr != null) && (tmpAttr.length() > 0)) {
                            mTranformation = tmpAttr;
                        }
                    }
                } else if (eventType == XmlPullParser.END_TAG) {
                }
                eventType = xpp.next();
            }

        } catch (Exception e) {
            Log.e(TAG, "parseImageDescriptor(), Exception:", e);
        }
        logImageDescriptor();
    }

    /**
     * Parse the pixel string to internal variables.
     * @param input The pixel string
     * Pixel string may be the following format:
     *  1. "360*480"
     *  2. "0*0-360*480", "120*240-360*480"
     *  3. "0**-360*480" (assign ratio)
     *  4. "60**-360*480": actually means "60*80-360*480" (assign ratio)
     */
    public void parsePixel(String input) {
        if (input.contains("-")) {
            // pixel string is a range
            String arg[] = input.split("-");
            mIsValidPixel = false;
            if (arg != null && arg.length == 2) {
                if (parseWidthHeightRange(arg[0], arg[1])) {
                    if ((mWidth1 >= 0) && (mHeight1 >= 0) &&
                        (mWidth2 > 0) && (mHeight2 > 0) &&
                        (mWidth1 < mWidth2) && (mHeight1 < mHeight2)) {
                        mIsValidPixel = true;
                    }
                }
            }
        } else {
            // pixel string is fix pixel
            String arg = input;
            mIsValidPixel = false;
            if (parseWidthHeight(arg)) {
                if ((mWidth1 > 0) && (mHeight1 > 0)) {
                    mIsValidPixel = true;
                }
            }
        }
    }

    /** Parse function for fix pixel.
     * @param input string input for parsing
     * @return true or false
     */
    public boolean parseWidthHeight(String input) {
        if (!input.contains("*")) {
            return false;
        }
        String arg[] = input.split("\\*");
        if (arg != null && arg.length == 2) {
            try {
                mWidth1 = Integer.parseInt(arg[0].trim());
                mHeight1 = Integer.parseInt(arg[1].trim());
            } catch (NumberFormatException e) {
                Log.e(TAG, "parseWidthHeight(), NumberFormatException:", e);
            }
        } else {
            return false;
        }
        mPixelType = PIXEL_TYPE.FixPixel;
        return true;
    }

    /** Parse function for range pixel.
     * @param input1 string input for parsing
     * @param input2 string input for parsing
     * @return true or false
     */
    public boolean parseWidthHeightRange(String input1, String input2) {
        if (!input1.contains("*") || !input2.contains("*")) {
            return false;
        }

        try {
            /* first part */
            if (input1.equals("0**")) {
                mWidth1 = 0;
                mHeight1 = 0;
                mPixelType = PIXEL_TYPE.RatioAssign;
            } else if (input1.contains("**")) {
                String arg[] = input1.split("\\*\\*");
                if (V) { Log.v(TAG, "parseWidthHeightRange(): case: **, arg[0]=" + arg[0]); }
                mWidth1 = Integer.parseInt(arg[0].trim());
                mHeight1 = 0;
                mPixelType = PIXEL_TYPE.RatioAssign;
            } else {
                String arg1[] = input1.split("\\*");
                if (arg1 != null && arg1.length == 2) {
                        mWidth1 = Integer.parseInt(arg1[0].trim());
                        mHeight1 = Integer.parseInt(arg1[1].trim());
                } else {
                    return false;
                }
                mPixelType = PIXEL_TYPE.RangePixel;
            }
            /* second part */
            String arg2[] = input2.split("\\*");
            if (arg2 != null && arg2.length == 2) {
                mWidth2 = Integer.parseInt(arg2[0].trim());
                mHeight2 = Integer.parseInt(arg2[1].trim());
            } else {
                return false;
            }
            /* end part */
            if (mPixelType == PIXEL_TYPE.RatioAssign) {
                float ratio = (float) mHeight2 / mWidth2;
                mHeight1 = (int) ((float) mWidth1 * ratio);
            }
        } catch (NumberFormatException e) {
            Log.e(TAG, "parseWidthHeightRange(), NumberFormatException:", e);
        }
        return true;
    }

    public long getMaxsize() {
        return mMaxsize;
    }

    public String getEncoding() {
        return mEncoding;
    }

    public String getPixel() {
        return mPixel;
    }

    public boolean getIsValidPixel() {
        return mIsValidPixel;
    }

    public PIXEL_TYPE getPixelType() {
        return mPixelType;
    }

    public int getWidth1() {
        return mWidth1;
    }

    public int getHeight1() {
        return mHeight1;
    }

    public int getWidth2() {
        return mWidth2;
    }

    public int getHeight2() {
        return mHeight2;
    }

    public int getDescriptorLength() {
        return mLength;
    }

    /** Log current descriptor for debug. */
    public void logImageDescriptor() {
        if (D) {
            Log.d(TAG, "logImageDescriptor():");
            Log.d(TAG, "mEncoding = " + mEncoding);
            Log.d(TAG, "mPixel = " + mPixel);
            Log.d(TAG, "mSize = " + mSize);
            Log.d(TAG, "mMaxsize = " + mMaxsize);
            Log.d(TAG, "mWidth1 = " + mWidth1 + ", mHeight1 = " + mHeight1);
            Log.d(TAG, "mWidth2 = " + mWidth2 + ", mHeight2 = " + mHeight2);
            Log.d(TAG, "mIsValidPixel = " + mIsValidPixel + ", mPixelType = " + mPixelType);
        }
    }

}





