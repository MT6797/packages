
package com.android.bluetooth.bip;

import android.content.Context;
import android.util.Log;

import com.android.internal.util.FastXmlSerializer;

import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;

/** This object is not implement complete. */
public class BluetoothBipObjImagingCapabilities {

    private static final String TAG = "BluetoothBipObjImagingCapabilities";
    private static final boolean D = true;
    private static final boolean V = true;
    private Context mContext = null;

    /**
     * Constructor.
     * @param context context
     */
    public  BluetoothBipObjImagingCapabilities(Context context) {
        mContext = context;
    }

    /**
     * Encode to xml.
     * @return byte array
     * @throws UnsupportedEncodingException UnsupportedEncodingException
     */
    public byte[] encodeImagingCapabilities() throws UnsupportedEncodingException {
        StringWriter sw = new StringWriter();
        XmlSerializer xmlMsgElement = new FastXmlSerializer();
        int i;
        int stopIndex;
        try {
            /* image-properties part */
            xmlMsgElement.setOutput(sw);
            xmlMsgElement.startDocument("UTF-8", true);
            xmlMsgElement.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
            xmlMsgElement.startTag(null, "imaging-capabilities");
            xmlMsgElement.attribute(null, "version", "1.0");
            /* preferred-format part */
            xmlMsgElement.startTag(null, "preferred-format");
            xmlMsgElement.attribute(null, "encoding", "");
            xmlMsgElement.attribute(null, "pixel", "");
            xmlMsgElement.attribute(null, "transformation", "");
            xmlMsgElement.attribute(null, "maxsize", "");
            xmlMsgElement.endTag(null, "preferred-format");
            /* image-formats part */
            xmlMsgElement.startTag(null, "image-formats");
            xmlMsgElement.attribute(null, "encoding", "");
            xmlMsgElement.attribute(null, "pixel", "");
            xmlMsgElement.attribute(null, "maxsize", "");
            xmlMsgElement.endTag(null, "image-formats");
            /* attachment-formats part */
            xmlMsgElement.startTag(null, "attachment-formats");
            xmlMsgElement.attribute(null, "content-type", "");
            xmlMsgElement.attribute(null, "charset", "");
            xmlMsgElement.endTag(null, "attachment-formats");
            /* filtering-parameters part */
            xmlMsgElement.startTag(null, "filtering-parameters");
            xmlMsgElement.attribute(null, "created", "");
            xmlMsgElement.attribute(null, "modified", "");
            xmlMsgElement.attribute(null, "encoding", "");
            xmlMsgElement.attribute(null, "pixel", "");
            xmlMsgElement.endTag(null, "filtering-parameters");
            /* end of image-properties */
            xmlMsgElement.endTag(null, "imaging-capabilities");
            xmlMsgElement.endDocument();
        } catch (IllegalArgumentException e) {
            Log.w(TAG, "encodeImagingCapabilities() Exception:" + e.toString());
        } catch (IllegalStateException e) {
            Log.w(TAG, "encodeImagingCapabilities() Exception:" + e.toString());
        } catch (IOException e) {
            Log.w(TAG, "encodeImagingCapabilities() Exception:" + e.toString());
        }
        return sw.toString().getBytes("UTF-8");
    }

}





