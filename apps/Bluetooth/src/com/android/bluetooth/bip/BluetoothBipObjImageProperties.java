
package com.android.bluetooth.bip;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

import com.android.internal.util.FastXmlSerializer;

import org.xmlpull.v1.XmlSerializer;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

/**
 * This class is used to produce ImageProperties object of BIP spec.
 * A bitmap object is include in the member objects,
 * function to perform image encoding and re-size are also provided.
 */
public class BluetoothBipObjImageProperties {

    private static final String TAG = "BluetoothBipObjImageProperties";
    private static final boolean D = true;
    private static final boolean V = true;
    private Context mContext = null;

    /* Varables of local imgae */
    private String mHandleStr = null;
    private Uri mUri;
    private String mFileName = null;
    private String mFilePath = null;
    private ImageProperties mNative = null;
    private ArrayList<ImageProperties> mVariant = null;
    private boolean mIsValid = false;
    private Bitmap mBitmap = null;
    public Bitmap.CompressFormat mDefaultEncoding = Bitmap.CompressFormat.JPEG;
    public int mDefaultQuality = 100;

    /** Image properties for native and variant format. */
    public class ImageProperties {
        public String mEncoding = null;
        public int mWidth = 0;
        public int mHeight = 0;
        /* This field means "size" in native tag and
           means "maxsize" in variant tag of BIP spec */
        public long mSize = 0;

        ImageProperties(String encoding, int width, int height, long size) {
            mEncoding = encoding;
            mWidth = width;
            mHeight = height;
            mSize = size;
        }
    }

    /**
     * Set image properties by image uri.
     * @param context context
     * @param handle image handle string
     * @param uri image uri
     */
    public BluetoothBipObjImageProperties(Context context, String handle, Uri uri) {
        mContext = context;
        mHandleStr = handle;
        mUri = uri;
        ContentResolver cr = mContext.getContentResolver();
        try {
            InputStream is = cr.openInputStream(mUri);
            Bitmap bitmap = BitmapFactory.decodeStream(is);
            is.close();
            mFilePath = getRealPathFromUri(mUri);
            mFileName = (Uri.parse(mFilePath)).getLastPathSegment();
            //TODO: encoding from getType() must be translated to BIP protocol
            File file = new File(mFilePath);
            mNative = new ImageProperties(
                mContext.getContentResolver().getType(mUri),
                bitmap.getWidth(),
                bitmap.getHeight(),
                file.length());
            if (bitmap != null) {
                mIsValid = true;
            }

            if (V) {
                Log.v(TAG, "BluetoothBipObjImageProperties initiated:");
                if (mUri != null) { Log.v(TAG, "Uri = " + mUri.toString()); }
                Log.v(TAG, "mFilePath = " + mFilePath);
                Log.v(TAG, "mFileName = " + mFileName);
                Log.v(TAG, "mNative.mEncoding = " + mNative.mEncoding);
                Log.v(TAG, "mNative.mWidth = " + mNative.mWidth);
                Log.v(TAG, "mNative.mHeight = " + mNative.mHeight);
                Log.v(TAG, "mNative.mSize = " + mNative.mSize);
                Log.v(TAG, "mIsValid = " + mIsValid);
            }
        } catch (Exception e) {
            Log.e(TAG, "Exception:", e);
            mIsValid = false;
        }
    }

    /** Set image properties by bitmap.
    * @param fileName file name
    * @param handle image handle string
    * @param bitmap image bitmap
    */
    public BluetoothBipObjImageProperties(String fileName, String handle, Bitmap bitmap) {
        mHandleStr = handle;
        mBitmap = bitmap;
        try {
            mFileName = fileName;
            /* set native properties, use jpeg as default */
            byte buf[] = encodeBitmap(bitmap, mDefaultEncoding, mDefaultQuality);
            mNative = new ImageProperties(
                "JPEG",
                bitmap.getWidth(),
                bitmap.getHeight(),
                buf.length);
            if (bitmap != null) {
                mIsValid = true;
            }

            if (V) {
                Log.v(TAG, "BluetoothBipObjImageProperties initiated:");
                if (mUri != null) { Log.v(TAG, "Uri = " + mUri.toString()); }
                Log.v(TAG, "mFilePath = " + mFilePath);
                Log.v(TAG, "mFileName = " + mFileName);
                Log.v(TAG, "mNative.mEncoding = " + mNative.mEncoding);
                Log.v(TAG, "mNative.mWidth = " + mNative.mWidth);
                Log.v(TAG, "mNative.mHeight = " + mNative.mHeight);
                Log.v(TAG, "mNative.mSize = " + mNative.mSize);
                Log.v(TAG, "mIsValid = " + mIsValid);
            }
        } catch (Exception e) {
            Log.e(TAG, "Exception:", e);
            mIsValid = false;
        }
    }

    /** Add variant format element.
    * @param encoding encoding
    * @param width width
    * @param height height
    * @param size size
    */
    public void addVariant(String encoding, int width, int height, long size) {
        if (mVariant == null) {
            mVariant = new ArrayList<ImageProperties>();
        }
        mVariant.add(new ImageProperties(encoding, width, height, size));
    }

    /** Encode to xml.
     * @return byte array
     * @throws UnsupportedEncodingException UnsupportedEncodingException
     */
    public byte[] encodeImageProperties() throws UnsupportedEncodingException {
        StringWriter sw = new StringWriter();
        XmlSerializer xmlMsgElement = new FastXmlSerializer();
        try {
            /* image-properties part */
            xmlMsgElement.setOutput(sw);
            xmlMsgElement.startDocument("UTF-8", true);
            xmlMsgElement.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
            xmlMsgElement.startTag(null, "image-properties");
            xmlMsgElement.attribute(null, "version", "1.0");
            xmlMsgElement.attribute(null, "handle", mHandleStr);
            xmlMsgElement.attribute(null, "friendly-name", mFileName);
            /* native part */
            xmlMsgElement.startTag(null, "native");
            xmlMsgElement.attribute(null, "encoding", mNative.mEncoding);
            xmlMsgElement.attribute(null, "pixel", Integer.toString(mNative.mWidth) +
                "*" + Integer.toString(mNative.mHeight));
            xmlMsgElement.attribute(null, "size", Long.toString(mNative.mSize));
            xmlMsgElement.endTag(null, "native");
            /* variant part */
            if (mVariant != null && mVariant.size() != 0) {
                for (int i = 0; i < mVariant.size(); i++) {
                    xmlMsgElement.startTag(null, "variant");
                    xmlMsgElement.attribute(null, "encoding", mVariant.get(i).mEncoding);
                    xmlMsgElement.attribute(null,
                        "pixel", Integer.toString(mVariant.get(i).mWidth) +
                        "*" + Integer.toString(mVariant.get(i).mHeight));
                    xmlMsgElement.endTag(null, "variant");
                }
            }

            /* end of image-properties */
            xmlMsgElement.endTag(null, "image-properties");
            xmlMsgElement.endDocument();
        } catch (IllegalArgumentException e) {
            Log.w(TAG, "encodeImageProperties() Exception:" + e.toString());
        } catch (IllegalStateException e) {
            Log.w(TAG, "encodeImageProperties() Exception:" + e.toString());
        } catch (IOException e) {
            Log.w(TAG, "encodeImageProperties() Exception:" + e.toString());
        }
        return sw.toString().getBytes("UTF-8");
        }

    /** Get file path from uri.
     * @param contentUri contentUri
     * @return file path string
     */
    public String getRealPathFromUri(Uri contentUri) {
        Cursor cursor = null;
        try {
            String[] proj = { MediaStore.Images.Media.DATA };
            cursor = mContext.getContentResolver().query(contentUri, proj, null, null, null);
            int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            return cursor.getString(columnIndex);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    /**
     * Do encode of member bitmap object.
     * @param format CompressFormat
     * @param quality image quality
     * @return byte array
     */
    public byte[] encodeBitmap(Bitmap.CompressFormat format, int quality) {
        return encodeBitmap(mBitmap, format, quality);
    }

    /**
     * Do encode & re-size of member bitmap object.
     * @param width width
     * @param height height
     * @param format CompressFormat
     * @param quality image quality
     * @return byte array
     */
    public byte[] encodeResizedBitmap(int width, int height,
        Bitmap.CompressFormat format, int quality) {
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(
                        mBitmap, width, height, false);
        return encodeBitmap(resizedBitmap, format, quality);
    }

    /**
     * Encode & transfer bitmap to byte array.
     * @param bitmap bitmap
     * @param format CompressFormat
     * @param quality quality
     * @return byte array
     */
    public byte[] encodeBitmap(Bitmap bitmap, Bitmap.CompressFormat format, int quality) {
        byte buf[] = null;
        Log.i(TAG, "encodeBitmap() start...");
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        try {
            bitmap.compress(format, quality, stream);
            buf = stream.toByteArray();
            stream.close();
        } catch (Exception e) {
            Log.e(TAG, "encodeBitmap(), Exception:", e);
        }
        Log.i(TAG, "encodeBitmap() end...");
        return buf;
    }

    public String getFilePath() {
        return mFilePath;
    }

    public long getNativeSize() {
        return mNative.mSize;
    }

    public int getNativeWidth() {
        return mNative.mWidth;
    }

    public int getNativeHeight() {
        return mNative.mHeight;
    }

    public boolean getIsValid() {
        return mIsValid;
    }


}

