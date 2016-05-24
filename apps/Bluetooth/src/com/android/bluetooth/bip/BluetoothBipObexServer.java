
package com.android.bluetooth.bip;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.android.bluetooth.BluetoothObexTransport;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

import javax.obex.HeaderSet;
import javax.obex.Operation;
import javax.obex.ResponseCodes;
import javax.obex.ServerRequestHandler;
import javax.obex.ServerSession;

/**
 * BIP obex server implementation,
 * include the algorithm to determine the return pixel size of image
 * according to indication in the ImageDescriptor.
 */
public class BluetoothBipObexServer extends ServerRequestHandler {

    private static final String TAG = "BluetoothBipObexServer";
    private static final boolean D = true;
    private static final boolean V = true;
    private Context mContext = null;
    private BluetoothServerSocket mListeningSocket = null;
    private BluetoothSocket mConnSocket = null;
    private SocketAcceptThread mL2capThread = null;
    private ServerSession mServerSession = null;
    private int mState = STATE_NONE;
    private int mFeatureSelect = 0;
    private boolean mIsAborted = false;

    // for cover art feature
    private BluetoothBipCoverArt mCoverArt = null;
    private Handler mCoverArtHandler = null;
    private static final int FEATURE_COVER_ART = 1;
    // Constants that indicate the current connection state
    public static final int STATE_NONE = 0;
    public static final int STATE_CONNECTED = 3;  // now connected to a remote device
    public static final int STATE_OBEX_CONNECTED = 4;  // after obex connect request success
    public static final int STATE_OBEX_DISCONNECTED = 5;  // after obex disconnect request success
    // Handler message
    public static final int MSG_SOCK_CONNECTED = 3;
    public static final int MSG_OBEX_CONNECTED = 4;
    public static final int MSG_SOCK_CLOSED = 5;
    public static final int MSG_SOCK_ERROR = 6;

    // OBEX header of BIP
    private static final int UUID_LENGTH = 16;
    private static final String TYPE_GET_IMG_PROPERTIES = "x-bt/img-properties";
    private static final String TYPE_GET_IMG_IMG = "x-bt/img-img";
    private static final String TYPE_GET_IMG_THM = "x-bt/img-thm";
    // OBEX header index of BIP
    private static final int IMG_HANDLE = 0x30;
    private static final int IMG_DESCRIPTOR = 0x71;
    // 128 bit UUID for CoverArt
    private static final byte[] AVRCP_COVER_ART_UUID = new byte[] {
             (byte) 0x71, (byte) 0x63, (byte) 0xDD, (byte) 0x54,
             (byte) 0x4A, (byte) 0x7E, (byte) 0x11, (byte) 0xE2,
             (byte) 0xB4, (byte) 0x7C, (byte) 0x00, (byte) 0x50,
             (byte) 0xC2, (byte) 0x49, (byte) 0x00, (byte) 0x48
             };

    /**
     * Set image properties by image uri.
     * @param context context
     * @param handler handler
     * @param coverArt coverArt feature caller
     */
    public BluetoothBipObexServer(Context context,
        Handler handler, BluetoothBipCoverArt coverArt) {
        mContext = context;
        mCoverArtHandler = handler;
        mCoverArt = coverArt;
        mFeatureSelect = FEATURE_COVER_ART;
    }

    /** Listen to L2CAP channel.
     * @param l2capPsm l2capPsm
     */
    public void startSocketListener(int l2capPsm) {

        BluetoothAdapter bt = BluetoothAdapter.getDefaultAdapter();
        // close socket before listen if it exists
        closeConnectionSocket();

        Log.i(TAG, "startSocketListener... (l2capPsm = " + l2capPsm + ")");
        try {
            if (mListeningSocket == null) {
                mListeningSocket = bt.listenUsingL2capOn(l2capPsm);
            }
        } catch (IOException e) {
            Log.e(TAG, "Error create ServerSockets: ", e);
        }
        Log.i(TAG, "startSocketListener... listenUsingL2capOn() ok");

        mL2capThread = new SocketAcceptThread();
        mL2capThread.start();
    }

    /** Create thread to listen to the socket. */
    private class SocketAcceptThread extends Thread {

        public void run() {
            while (mState != STATE_CONNECTED) {
                try {
                    // This is a blocking call and will only return on a
                    // successful connection or an exception
                    Log.i(TAG, "Accepting socket connection...");
                    mConnSocket = mListeningSocket.accept();
                    Log.i(TAG, "Accepted socket connection...");
                } catch (IOException e) {
                    Log.e(TAG, "Socket accept() failed: ", e);
                    break;
                }
                if (mConnSocket != null) {
                    mState = STATE_CONNECTED;
                    Message msg = mBipHandler.obtainMessage(MSG_SOCK_CONNECTED);
                    mBipHandler.sendMessage(msg);
                    break;
                }
            }
            Log.i(TAG, "END SocketAcceptThread");
        }

    }

    private void startObexServerSession() {
        try {
            BluetoothObexTransport transport = new BluetoothObexTransport(mConnSocket);
            mServerSession = new ServerSession(transport, this, null);
            if (D) { Log.d(TAG, "BIP ServerSession started."); }
        } catch (IOException e) {
            Message msg = mBipHandler.obtainMessage(MSG_SOCK_ERROR);
            mBipHandler.sendMessage(msg);
        }
    }

    public int getState() {
         return mState;
     }

    @Override
    public void onClose() {
        // onClose() will be called if ServerRequestHandler get socket end situation
        Log.d(TAG, "onClose(): enter");
        mState = STATE_NONE;
        Message msg = Message.obtain(mBipHandler);
        msg.what = BluetoothBipObexServer.MSG_SOCK_CLOSED;
        msg.sendToTarget();
    }

    private void closeConnectionSocket() {
        Log.d(TAG, "closeConnectionSocket(): " + mConnSocket);
        try {
            if (mConnSocket != null) {
                mConnSocket.close();
            }
            if (mListeningSocket != null) {
                mListeningSocket.close();
            }
            if (mServerSession != null) {
                mServerSession.close();
            }
        } catch (IOException e) {
            Log.e(TAG, "closeConnectionSocket(): Close Connection Socket error: ", e);
        } finally {
            mConnSocket = null;
            mListeningSocket = null;
            mServerSession = null;
        }
    }

    private final Handler mBipHandler = new Handler() {
     @Override
        public void handleMessage(Message msg) {
            if (D) { Log.d(TAG, "mBipHandler got msg=" + msg.what); }
            Message msgSend = null;
            switch (msg.what) {
                case MSG_SOCK_CONNECTED:
                    startObexServerSession();
                    break;
                case MSG_SOCK_CLOSED:
                    closeConnectionSocket();
                    msgSend = Message.obtain(mCoverArtHandler);
                    msgSend.what = BluetoothBipObexServer.MSG_SOCK_CLOSED;
                    msgSend.sendToTarget();
                    break;
                case MSG_SOCK_ERROR:
                    closeConnectionSocket();
                    msgSend = Message.obtain(mCoverArtHandler);
                    msgSend.what = BluetoothBipObexServer.MSG_SOCK_CLOSED;
                    msgSend.sendToTarget();
                    break;
                default:
                    break;
            }
        }
    };

    @Override
    public int onConnect(final HeaderSet request, HeaderSet reply) {
        if (D) { Log.d(TAG, "onConnect(): enter"); }
        if (V) { logHeader(request); }

        try {
            byte[] uuid = (byte[]) request.getHeader(HeaderSet.TARGET);

            if (uuid == null) {
                return ResponseCodes.OBEX_HTTP_NOT_ACCEPTABLE;
            }
            if (D) { Log.d(TAG, "onConnect(): uuid=" + Arrays.toString(uuid)); }

            if (uuid.length != UUID_LENGTH) {
                Log.w(TAG, "Wrong UUID length");
                return ResponseCodes.OBEX_HTTP_NOT_ACCEPTABLE;
            }
            for (int i = 0; i < UUID_LENGTH; i++) {
                if (uuid[i] != AVRCP_COVER_ART_UUID[i]) {
                    Log.w(TAG, "Wrong UUID");
                    return ResponseCodes.OBEX_HTTP_NOT_ACCEPTABLE;
                }
            }
            reply.setHeader(HeaderSet.WHO, uuid);
        } catch (IOException e) {
            Log.e(TAG, "Exception during onConnect:", e);
            return ResponseCodes.OBEX_HTTP_INTERNAL_ERROR;
        }

        try {
            byte[] remote = (byte[]) request.getHeader(HeaderSet.WHO);
            if (remote != null) {
                if (D) { Log.d(TAG, "onConnect(): remote=" + Arrays.toString(remote)); }
                reply.setHeader(HeaderSet.TARGET, remote);
            }
        } catch (IOException e) {
            Log.e(TAG, "Exception during onConnect:", e);
            return ResponseCodes.OBEX_HTTP_INTERNAL_ERROR;
        }

        if (V) { Log.v(TAG, "onConnect(): uuid is ok"); }
        mState = STATE_OBEX_CONNECTED;
        Message msg = Message.obtain(mCoverArtHandler);
        msg.what = BluetoothBipObexServer.MSG_OBEX_CONNECTED;
        msg.sendToTarget();
        return ResponseCodes.OBEX_HTTP_OK;
    }

    @Override
    public void onDisconnect(final HeaderSet req, final HeaderSet resp) {
        if (D) { Log.d(TAG, "onDisconnect(): enter"); }
        if (V) { logHeader(req); }
        mState = STATE_OBEX_DISCONNECTED;
        resp.responseCode = ResponseCodes.OBEX_HTTP_OK;
    }

    /**
     * To get the image-relate object,
     * always obtain the image information by using getImageProperties(),
     * it will related to the process of corresponding feature.
     * Only after the information is successfully obtained, other BIP obex request
     * can be executed normally.
     * @param op Obex operation module
     * @return Obex response code
     */
    @Override
    public int onGet(Operation op) {

        OutputStream outStream = null;
        byte[] outBytes = null;
        int maxChunkSize = 0;
        int bytesWritten = 0;
        int bytesToWrite = 0;
        mIsAborted = false;

        HeaderSet request;
        HeaderSet replyHeaders = new HeaderSet();
        String type = null;
        String imgHandle = null;
        byte[] imgDescriptor = null;
        if (D) { Log.d(TAG, "onGet(): enter"); }

        try {
            request = op.getReceivedHeader();
            type = (String) request.getHeader(HeaderSet.TYPE);
            imgHandle = (String) request.getHeader(IMG_HANDLE);
            imgDescriptor = (byte[]) request.getHeader(IMG_DESCRIPTOR);

            Log.i(TAG, "OnGet() type is " + type);
            if (V) { logHeader(request); }
            if (V) { Log.v(TAG, "ImgHandle is " + imgHandle); }

            if (type.equals(TYPE_GET_IMG_PROPERTIES)) {
                if (imgHandle == null) {
                    return ResponseCodes.OBEX_HTTP_BAD_REQUEST;
                }
                /* get properties of local image */
                BluetoothBipObjImageProperties objImgProp = getImageProperties(imgHandle);

                if ((objImgProp == null) || (objImgProp.getIsValid() == false)) {
                    return ResponseCodes.OBEX_HTTP_NOT_FOUND;
                } else {
                    outBytes = objImgProp.encodeImageProperties();
                }
            } else if (type.equals(TYPE_GET_IMG_IMG)) {
                if (imgHandle == null) {
                    return ResponseCodes.OBEX_HTTP_BAD_REQUEST;
                }
                /* get properties of local image */
                BluetoothBipObjImageProperties objImgProp = getImageProperties(imgHandle);
                BluetoothBipObjImageDescriptor objImgDesc = null;
                int pixel[] = {0, 0};
                String encoding = null;
                Bitmap.CompressFormat encodeAs = null;
                long maxSize = 0;
                int result = ResponseCodes.OBEX_HTTP_BAD_REQUEST;
                boolean error = false;

                if (!error && ((objImgProp == null) || (objImgProp.getIsValid() == false))) {
                    error = true;
                    result = ResponseCodes.OBEX_HTTP_NOT_FOUND;
                    Log.d(TAG, "get image properties fail!");
                }
                if (!error) {
                    /* get the requested properties from client */
                    objImgDesc = new BluetoothBipObjImageDescriptor(mContext, imgDescriptor);
                    if (objImgDesc.getDescriptorLength() > 0) {
                        /* return the requested format if client asked */
                        objImgDesc.parseImageDescriptor(new String(imgDescriptor));
                        if ((objImgDesc.getPixel() != null) && (!objImgDesc.getIsValidPixel())) {
                            error = true;
                            result = ResponseCodes.OBEX_HTTP_BAD_REQUEST;
                        }
                    } else {
                        /* return the native format if client do not assign */
                        Log.i(TAG, "ImgDescriptor not request!");
                        outBytes = objImgProp.encodeBitmap(
                            objImgProp.mDefaultEncoding, objImgProp.mDefaultQuality);

                        replyHeaders.setHeader(HeaderSet.LENGTH, (long) outBytes.length);
                        op.sendHeaders(replyHeaders);
                        InputStream inStream = new ByteArrayInputStream(outBytes);
                        return sendGetImageRsp(op, inStream, outBytes.length);
                    }
                }
                if (!error) {
                    /* determine the pixel size and encoding */
                    pixel = determinePixel(objImgProp, objImgDesc);
                    Log.i(TAG, "OnGet() the return pixel will be width="
                        + pixel[0] + " and height=" + pixel[1]);

                    encoding = objImgDesc.getEncoding();
                    if (encoding == null) {
                        encodeAs = Bitmap.CompressFormat.JPEG;
                    } else if ((encoding != null) && (encoding.toUpperCase().equals("JPEG"))) {
                        encodeAs = Bitmap.CompressFormat.JPEG;
                    } else if ((encoding != null) && (encoding.toUpperCase().equals("PNG"))) {
                        encodeAs = Bitmap.CompressFormat.PNG;
                    } else {
                        Log.d(TAG, "OnGet() bad encoding!");
                        error = true;
                        result = ResponseCodes.OBEX_HTTP_BAD_REQUEST;
                    }
                }
                if (!error) {
                    /* transfer the requested image format and return to client */
                    outBytes = objImgProp.encodeResizedBitmap(pixel[0], pixel[1],
                        encodeAs, objImgProp.mDefaultQuality);

                    maxSize = objImgDesc.getMaxsize();
                    /* check maxsize, the image can be return if
                    maxsize is large enough or not assigned */
                    if ((maxSize >= outBytes.length) ||
                        (maxSize == BluetoothBipObjImageDescriptor.INVALID_VALUE)) {
                        replyHeaders.setHeader(HeaderSet.LENGTH, (long) outBytes.length);
                        op.sendHeaders(replyHeaders);
                        InputStream inStream = new ByteArrayInputStream(outBytes);
                        return sendGetImageRsp(op, inStream, outBytes.length);
                    } else {
                        Log.d(TAG, "maxSize in ImageDescriptor = " + maxSize +
                            ", which is smaller than the request image format file = " +
                            outBytes.length + ", response OBEX_HTTP_BAD_REQUEST!");
                        error = true;
                        result = ResponseCodes.OBEX_HTTP_BAD_REQUEST;
                    }
                }
                return result;
            } else if (type.equals(TYPE_GET_IMG_THM)) {
                if (imgHandle == null) {
                    return ResponseCodes.OBEX_HTTP_BAD_REQUEST;
                }
                /* get properties of local image */
                BluetoothBipObjImageProperties objImgProp = getImageProperties(imgHandle);

                if ((objImgProp == null) || (objImgProp.getIsValid() == false)) {
                    Log.d(TAG, "get image properties fail!");
                    return ResponseCodes.OBEX_HTTP_NOT_FOUND;
                } else {
                    /* transfer image to the thumbnail format and return to client */
                    outBytes = objImgProp.encodeResizedBitmap(
                        BluetoothBipCoverArt.AVRCP_ART_WIDTH,
                        BluetoothBipCoverArt.AVRCP_ART_HEIGHT,
                        Bitmap.CompressFormat.JPEG,
                        100);

                    InputStream inStream = new ByteArrayInputStream(outBytes);
                    return sendGetImageRsp(op, inStream, outBytes.length);
                }
            } else {
                Log.w(TAG, "unknown type request: " + type);
                return ResponseCodes.OBEX_HTTP_NOT_ACCEPTABLE;
            }
        } catch (Exception e) {
            Log.e(TAG, "onGet(), Exception:", e);
            return ResponseCodes.OBEX_HTTP_INTERNAL_ERROR;
        }

        try {
            outStream = op.openOutputStream();
            maxChunkSize = op.getMaxPacketSize(); // This must be called after setting the headers.
        } catch (Exception e1) {
            Log.e(TAG, "onGet() streaming, Exception:", e1);
            if (outStream != null) {
                try {
                    outStream.close();
                } catch (Exception e2) {
                    Log.e(TAG, "onGet() streaming close, Exception:", e2);
                }
            }
            return ResponseCodes.OBEX_HTTP_INTERNAL_ERROR;
        }

        if (outBytes != null) {
            try {
                while (bytesWritten < outBytes.length && mIsAborted == false) {
                    bytesToWrite = Math.min(maxChunkSize, outBytes.length - bytesWritten);
                    outStream.write(outBytes, bytesWritten, bytesToWrite);
                    bytesWritten += bytesToWrite;
                }
            } catch (Exception e) {
                // We were probably aborted or disconnected
                Log.e(TAG, "onGet() streaming write, Exception:", e);
            } finally {
                if (outStream != null) {
                    try {
                        outStream.close();
                    } catch (Exception e) {
                        Log.e(TAG, "onGet() stream close, Exception:", e);
                    }
                }
            }

            if (D) {
                Log.d(TAG, "onGet() sent " + bytesWritten + " bytes out of " + outBytes.length);
            }
            if (bytesWritten == outBytes.length || mIsAborted) {
                return ResponseCodes.OBEX_HTTP_OK;
            } else {
                return ResponseCodes.OBEX_HTTP_BAD_REQUEST;
            }
        }

        return ResponseCodes.OBEX_HTTP_OK;
    }


    @Override
    public int onAbort(HeaderSet request, HeaderSet reply) {
        if (D) { Log.d(TAG, "onAbort(): enter."); }
        mIsAborted = true;
        return ResponseCodes.OBEX_HTTP_OK;
    }

    /**
     * @param op Obex operation class
     * @param inStream The stream to be written out
     * @param fileLength Length of stream to be written out
     * @return Obex response code
     */
    private int sendGetImageRsp(Operation op, InputStream inStream, long fileLength) {
        byte[] buffer = null;
        int outputBufferSize = 0;
        OutputStream outStream = null;
        BufferedInputStream binStream = null;
        int readLength = 0;
        long writePosition = 0;

        long timestamp = 0;
        mIsAborted = false;
        if (D) { Log.d(TAG, "sendGetImageRsp(): enter"); }

        try {
            outStream = op.openOutputStream();
            outputBufferSize = op.getMaxPacketSize();
            buffer = new byte[outputBufferSize];
            binStream = new BufferedInputStream(inStream, 0x4000);

            Log.d(TAG, "outputBufferSize = " + outputBufferSize +
                ", fileLength = " + fileLength);
            if (D) {
                Log.d(TAG, "start! readLength = " + readLength +
                ", writePosition = " + writePosition);
            }

            timestamp = System.currentTimeMillis();
            while ((writePosition != fileLength) && mIsAborted == false) {
                readLength = binStream.read(buffer, 0, outputBufferSize);
                outStream.write(buffer, 0, readLength);
                writePosition += readLength;
                if (D) { Log.d(TAG, "writePosition = " + writePosition); }
            }
        } catch (Exception e1) {
            Log.e(TAG, "sendGetImageRsp() Exception:", e1);
            if (mIsAborted == true) {
                Log.e(TAG, "sendGetImageRsp Operation Aborted");
            }
        } finally {
            try {
                if (outStream != null) { outStream.close(); }
                if (binStream != null) { binStream.close(); }
            } catch (Exception e) {
                Log.e(TAG, "sendGetImageRsp() Exception:", e);
            }
        }
        Log.d(TAG, "sendGetImageRsp sent " + writePosition +
            " bytes in" + (System.currentTimeMillis() - timestamp) + "ms");

        if (writePosition == fileLength || mIsAborted) {
            return ResponseCodes.OBEX_HTTP_OK;
        } else {
            return ResponseCodes.OBEX_HTTP_BAD_REQUEST;
        }
    }

    /**
     * @param op Obex operation class
     * @param Prop Properties of the image
     * @param Request Variables of client request
     * @return Width and height of the image determined by the request
     */
    private int[] determinePixel(BluetoothBipObjImageProperties prop,
        BluetoothBipObjImageDescriptor request) {

        int pixel[] = {0, 0};

        if (request.getPixel() == null) {
            // use native pixel
            Log.d(TAG, "determinePixel(), no pixel provide! use native pixel");
            pixel[0] = prop.getNativeWidth();
            pixel[1] = prop.getNativeHeight();
        } else if (request.getPixelType() ==
            BluetoothBipObjImageDescriptor.PIXEL_TYPE.FixPixel) {
            // request is a fix pixel
            pixel[0] = request.getWidth1();
            pixel[1] = request.getHeight1();
        } else if (request.getPixelType() ==
            BluetoothBipObjImageDescriptor.PIXEL_TYPE.RangePixel) {
            // request is a range pixel
            if (request.getWidth1() < prop.getNativeWidth() &&
                request.getWidth2() > prop.getNativeWidth() &&
                request.getHeight1() < prop.getNativeHeight() &&
                request.getHeight2() > prop.getNativeHeight()) {
                // native pixel is in the requested range, return the native pixel
                pixel[0] = prop.getNativeWidth();
                pixel[1] = prop.getNativeHeight();
            } else {
                // return the pixel with the middle value of the range
                pixel[0] = (request.getWidth1() + request.getWidth2()) / 2;
                pixel[1] = (request.getHeight1() + request.getHeight2()) / 2;
            }
        } else if (request.getPixelType() ==
            BluetoothBipObjImageDescriptor.PIXEL_TYPE.RatioAssign) {
            // request is a assigned ratio, use the middle value of the range
            pixel[0] = (request.getWidth1() + request.getWidth2()) / 2;
            float ratio = (float) request.getHeight2() / request.getWidth2();
            pixel[1] = (int) ((float) pixel[0] * ratio);
        }
        return pixel;
    }

    /**
     * Get image related function is selected by supported feature.
     * @param handle Image handle string
     * @return BIP image properties object
     */
    public BluetoothBipObjImageProperties getImageProperties(String handle) {
        if (mFeatureSelect == FEATURE_COVER_ART) {
            return mCoverArt.getImageProperties(handle);
        }
        return null;
    }

    private static final void logHeader(HeaderSet hs) {
        Log.v(TAG, "Dumping HeaderSet " + hs.toString());
        try {
            Log.v(TAG, "CONNECTION_ID : " + hs.getHeader(HeaderSet.CONNECTION_ID));
            Log.v(TAG, "NAME : " + hs.getHeader(HeaderSet.NAME));
            Log.v(TAG, "TYPE : " + hs.getHeader(HeaderSet.TYPE));
            Log.v(TAG, "TARGET : " + hs.getHeader(HeaderSet.TARGET));
            Log.v(TAG, "WHO : " + hs.getHeader(HeaderSet.WHO));
            Log.v(TAG, "APPLICATION_PARAMETER : " + hs.getHeader(HeaderSet.APPLICATION_PARAMETER));
        } catch (IOException e) {
            Log.e(TAG, "dump HeaderSet error " + e);
        }
        Log.v(TAG, "NEW!!! Dumping HeaderSet END");
    }

    /** Log a byte array for debug.
     * @param buffer byte array
     * @param len buffer length
     * @return log string
     */
    public String logBuffer(byte[] buffer, int len) {
        int cnt = 0;
        StringBuilder str = new StringBuilder();
        while (cnt != len) {
            if ((cnt % 16) == 0 && (cnt != 0)) {
                str.append("\r\n");
            }
            str.append(String.format("%02X", buffer[cnt]));
            str.append(" ");
            cnt++;
        }
        return str.toString();
    }

    /** Write byte array to file for debug.
     * @param buffer byte array
     * @param name file name to be write
     */
    public void write2File(byte[] buffer, String name) {
        try {
            BufferedOutputStream bos =
            new BufferedOutputStream(new FileOutputStream("/sdcard/" + name));
        bos.write(buffer);
        bos.flush();
        bos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}



