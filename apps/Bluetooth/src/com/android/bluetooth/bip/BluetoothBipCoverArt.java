
package com.android.bluetooth.bip;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.media.RemoteController;
import android.media.RemoteController.MetadataEditor;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore.Images;
import android.provider.MediaStore.MediaColumns;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;

/**
 * This class implement the behavior of AVRCP cover art function using BIP spec.
 *  - Register a interface to get the metadata of the current playing media on music player.
 *  - Maintain and update the local metadata.
 *  - Provide static function to get the cover art handle from local metadata.
 */
public final class BluetoothBipCoverArt {
    private static final boolean D = true;
    private static final String TAG = "BluetoothBipCoverArt";

    private Context mContext;
    private BluetoothBipObexServer mBipObexServer;
    private int mL2capPsm = 0x1077;
    private static Metadata mMetadata;

    private final AudioManager mAudioManager;
    private RemoteController mRemoteController;
    private RemoteControllerWeak mRemoteControllerCb;
    private static final int RC_ART_WIDTH = 800;
    private static final int RC_ART_HEIGHT = 800;
    public static final int AVRCP_ART_WIDTH = 200;
    public static final int AVRCP_ART_HEIGHT = 200;
    private static final int MSG_SET_METADATA = 101;
    public static final String GET_ART_FAIL = "GET_ART_FAIL";

    /**
     * Constructor, register remote controller.
     * @param context context
     */
    public BluetoothBipCoverArt(Context context) {
        mContext = context;

        // obex server to handle connection and protocol
        mBipObexServer = null;
        mBipObexServer = new BluetoothBipObexServer(mContext, mBipCoverArtHandler, this);
        mBipObexServer.startSocketListener(mL2capPsm);
        mMetadata = new Metadata();
        // register remote controller of music player
        mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        mRemoteControllerCb = new RemoteControllerWeak(mBipCoverArtHandler);
        mRemoteController = new RemoteController(mContext, mRemoteControllerCb);
        mAudioManager.registerRemoteController(mRemoteController);
        mRemoteController.setSynchronizationMode(RemoteController.POSITION_SYNCHRONIZATION_CHECK);
        // set the artwork configuration(pixel) of remote controller
        mRemoteController.setArtworkConfiguration(RC_ART_WIDTH, RC_ART_HEIGHT);
    }

    /** Close function, unregister remote controller. */
    public void close() {
        mAudioManager.unregisterRemoteController(mRemoteController);
    }

    /**
     * Get cover art handle of current playing media.
     * @return return "" if get fail or bip obex not connected, return
     * "1000001"(for example) if get success(7 digitals string)
     */
    public static String getCoverArtHandle() {
        String handle;
        if (GET_ART_FAIL.equals(mMetadata.getArtWorkHandleString())) {
            handle = "";
        } else {
            handle = mMetadata.getArtWorkHandleString();
            /* Set the state so the client can get image by handle successfully */
            mMetadata.setArtWorkState(BluetoothBipCoverArt.GET_STATE.GetSuccess);
        }
        Log.i(TAG, "getCoverArtHandle(), current mMetadata:" +
            mMetadata.toStringLog());
        return handle;
    }

    private final Handler mBipCoverArtHandler = new Handler() {
     @Override
        public void handleMessage(Message msg) {
            Log.i(TAG, "mBipCoverArtHandler got msg=" + msg.what);

            switch (msg.what) {
                case BluetoothBipObexServer.MSG_OBEX_CONNECTED:
                    /*
                     * In case that the metadata has been update before obex connect success
                     * We set the string value back when obex connected
                     */
                    if ((mMetadata.mArtWorkHandle > mMetadata.ART_WORK_HANLE_INITIAL_VALUE) &&
                        (mMetadata.mArtWork != null)) {
                        mMetadata.setArtWorkHandleString();
                    }
                    break;
                case BluetoothBipObexServer.MSG_SOCK_CLOSED:
                    /*
                     * Restart socket listen after original one disconnected
                     */
                    mMetadata.setArtWorkState(BluetoothBipCoverArt.GET_STATE.NotGet);
                    threadSleep();
                    mBipObexServer.startSocketListener(mL2capPsm);
                    break;
                case MSG_SET_METADATA:
                    updateMetadata((MetadataEditor) msg.obj);
                    break;
                default:
                    break;
            }
        }
    };

    private void threadSleep() {
        try {
            Thread.sleep(100);
        } catch (Exception e) {
        }
    }

    /**
     * This function will be called when music player update playing status.
     */
    private void updateMetadata(MetadataEditor data) {
        Log.i(TAG, "updateMetadata() enter:");

        mMetadata.mArtist = data.getString(MediaMetadataRetriever.METADATA_KEY_ARTIST, null);
        mMetadata.mTrackTitle = data.getString(MediaMetadataRetriever.METADATA_KEY_TITLE, null);
        mMetadata.mAlbumTitle = data.getString(MediaMetadataRetriever.METADATA_KEY_ALBUM, null);

        // Set art work information
        // The playing media is updated, so reset the get state and add the handle vaule anywhere
        Bitmap bitmapTmp = data.getBitmap(RemoteController.MetadataEditor.BITMAP_KEY_ARTWORK, null);
        mMetadata.mArtWork = null;
        mMetadata.mArtWorkState = GET_STATE.NotGet;
        mMetadata.addArtWorkHandle();

        if (bitmapTmp != null) {
            /*
             * The handle string is only valid after obex connect request has been performed
             * set the handle string and save to file
             */
            mMetadata.mArtWork = bitmapTmp;
            if (mBipObexServer.getState() == BluetoothBipObexServer.STATE_OBEX_CONNECTED) {
                mMetadata.setArtWorkHandleString();
            } else {
                mMetadata.setArtWorkHandleString(GET_ART_FAIL);
            }
        } else {
            mMetadata.setArtWorkHandleString(GET_ART_FAIL);
        }
        Log.i(TAG, "updateMetadata() done, current mMetadata:" + mMetadata.toStringLog());
    }

    public Metadata getMetadata() {
        return mMetadata;
    }

    /**
     * This function link to BIP obex protocol with cover art feature.
     * GetImage & GetImageProperties & GetLinkedThumbnail will run this function firstly
     * In cover art feature, we will not get a correct response
     * if avrcp contoller do not get a image handle before,
     * so the return value will be null if GET_STATE is not GetSuccess
     * @param handle Image handle string
     * @return BIP image properties object
     */
    public BluetoothBipObjImageProperties getImageProperties(String handle) {
        Log.i(TAG, "getImageProperties(), current mMetadata:" + mMetadata.toStringLog());
        BluetoothBipObjImageProperties obj = null;

        if (mMetadata.mArtWorkState == GET_STATE.NotGet) {
            obj = null;
        } else if (mMetadata.mArtWorkState == GET_STATE.GetFail) {
            obj = null;
        } else if (mMetadata.mArtWorkState == GET_STATE.GetSuccess) {
            /* check handle value is match to metadata */
            if (handle.equals(mMetadata.mArtWorkHandleStr)) {
                obj = new BluetoothBipObjImageProperties(mMetadata.mArtWorkHandleStr,
                    mMetadata.mArtWorkHandleStr, mMetadata.mArtWork);
                /* add variant */
                obj.addVariant("PNG", RC_ART_WIDTH, RC_ART_HEIGHT, 0);
                obj.addVariant("JPEG", AVRCP_ART_WIDTH, AVRCP_ART_HEIGHT, 0);
            } else {
                obj = null;
            }
        }
        if (obj == null) { Log.i(TAG, "getImageProperties(), return obj = null !"); }
        return obj;
    }


    /** Debug function.
     * @param inContext Context
     * @param bmp Bitmap to be save
     * @param filename to be save
     * @return file uri
     */
    public Uri saveBitmapToFile(Context inContext, Bitmap bmp, String filename) {
        CompressFormat format = Bitmap.CompressFormat.JPEG;
        int quality = 100;
        boolean result = false;
        Uri uri = null;
        OutputStream stream = null;

        try {
            Log.d(TAG, "saveBitmapToFile(), start");
            stream = new FileOutputStream("/sdcard/" + filename + ".jpg");
            result = bmp.compress(format, quality, stream);
            stream.close();
            Log.d(TAG, "saveBitmapToFile(), end");
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (result == true) {
            uri = getImageUriFromFile(inContext, new File("/sdcard/" + filename + ".jpg"));
        }
        Log.d(TAG, "saveBitmapToFile(), uri = " + uri.toString());
        return uri;
    }

    /**
     * Gets the content uri from the given corresponding path to a file.
     * @param context context
     * @param imageFile imageFile
     * @return content Uri
     */
    public static Uri getImageUriFromFile(Context context, File imageFile) {
        String filePath = imageFile.getAbsolutePath();
        Uri uri = null;
        Cursor cursor = context.getContentResolver().query(
                Images.Media.EXTERNAL_CONTENT_URI,
                new String[] { Images.Media._ID },
                Images.Media.DATA + "=? ",
                new String[] { filePath }, null);
        if (cursor != null && cursor.moveToFirst()) {
            int id = cursor.getInt(cursor.getColumnIndex(MediaColumns._ID));
            Uri baseUri = Uri.parse("content://media/external/images/media");
            uri = Uri.withAppendedPath(baseUri, "" + id);
        } else {
            if (imageFile.exists()) {
                ContentValues values = new ContentValues();
                values.put(Images.Media.DATA, filePath);
                uri = context.getContentResolver().insert(
                    Images.Media.EXTERNAL_CONTENT_URI, values);
            } else {
                uri = null;
            }
        }
        cursor.close();
        return uri;
    }

    /** Debug function.
     * @param inContext Context
     * @param uri file uri to be resolved
     * @return Bitmap
     */
    public static Bitmap getBitmapFromUri(Context inContext, Uri uri) {
        ContentResolver cr = inContext.getContentResolver();
        Bitmap tempBitmap = null;

        try {
            InputStream in = cr.openInputStream(uri);
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = 1;
            tempBitmap = BitmapFactory.decodeStream(in, null, options);
            in.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return (tempBitmap == null ? null : tempBitmap);
    }

    /** Debug function.
     * @return uri
     */
    public static Uri getImageUriTest() {
        try {
            FileInputStream fin = new FileInputStream("/storage/emulated/0/CoverArt.txt");
            ByteArrayOutputStream fout = new ByteArrayOutputStream();

            byte buf[] = new byte[128];
            int bufSize = 0;
            while (fin.available() > 0) {
                bufSize = fin.read(buf);
                fout.write(buf, 0, bufSize);
            }
            Uri uri = Uri.parse(fout.toString());
            fin.close();
            fout.close();
            return uri;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /** Debug function.
     * @return byte array
     */
    public static byte[] getImagePropertiesTest() {
        try {
            FileInputStream fin = new FileInputStream("/storage/emulated/0/ImgDescTest.xml");
            ByteArrayOutputStream fout = new ByteArrayOutputStream();

            byte buf[] = new byte[128];
            int bufSize = 0;
            while (fin.available() > 0) {
                bufSize = fin.read(buf);
                fout.write(buf, 0, bufSize);
                Log.i(TAG, "getImagePropertiesTest(), bufSize = " + bufSize);
            }
            fin.close();
            fout.close();
            byte bufOut[] = fout.toByteArray();
            Log.i(TAG, "getImagePropertiesTest(), bufOut.length = " + bufOut.length);
            return bufOut;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Implement this interface to obtain the update of metadata.
     */
    private static class RemoteControllerWeak implements RemoteController.OnClientUpdateListener {
        private final WeakReference<Handler> mLocalHandler;

        public RemoteControllerWeak(Handler handler) {
            mLocalHandler = new WeakReference<Handler>(handler);
        }

        @Override
        public void onClientChange(boolean clearing) {
            // do nothing
        }

        @Override
        public void onClientPlaybackStateUpdate(int state) {
            // do nothing
        }

        @Override
        public void onClientPlaybackStateUpdate(int state, long stateChangeTimeMs,
                long currentPosMs, float speed) {
            // do nothing
        }

        @Override
        public void onClientTransportControlUpdate(int transportControlFlags) {
            // do nothing
        }

        @Override
        public void onClientMetadataUpdate(MetadataEditor metadataEditor) {
            Handler handler = mLocalHandler.get();
            if (handler != null) {
                handler.obtainMessage(MSG_SET_METADATA, 0, 0, metadataEditor).sendToTarget();
            }
        }
    }

    /**
     * State enum of get image hamdle.
     */
    public enum GET_STATE {
        NotGet,
        GetFail,
        GetSuccess;
    }

    /**
     * Metadata store the information of current playing media.
     */
    static class Metadata {
        // handle value is a 7-digits string
        private static final int ART_WORK_HANLE_INITIAL_VALUE = 1000000;

        private static String mArtist = null;
        private static String mTrackTitle = null;
        private static String mAlbumTitle = null;
        private static Bitmap mArtWork = null;
        private static long mArtWorkHandle = ART_WORK_HANLE_INITIAL_VALUE;
        private static String mArtWorkHandleStr = GET_ART_FAIL;
        // use the state variable to record the get handle state from avrcp
        private static GET_STATE mArtWorkState = GET_STATE.NotGet;

        public void reset() {
            mArtist = null;
            mTrackTitle = null;
            mAlbumTitle = null;
            mArtWork = null;
            mArtWorkHandle = ART_WORK_HANLE_INITIAL_VALUE;
            mArtWorkHandleStr = GET_ART_FAIL;
            mArtWorkState = GET_STATE.NotGet;
        }

        public void addArtWorkHandle() {
            mArtWorkHandle += 1;
            if (mArtWorkHandle > 9999999) {
                mArtWorkHandle = ART_WORK_HANLE_INITIAL_VALUE;
            }
        }

        public static void setArtWorkState(GET_STATE state) {
            mArtWorkState = state;
        }

        public static String getArtWorkHandleString() {
            return mArtWorkHandleStr;
        }

        public void setArtWorkHandleString() {
            mArtWorkHandleStr = String.valueOf(mArtWorkHandle);
        }

        public void setArtWorkHandleString(String str) {
            mArtWorkHandleStr = str;
        }

        public static String toStringLog() {
            StringBuilder str = new StringBuilder();
            str.append("\r\n");
            str.append("mArtist=" + mArtist + ", mTrackTitle=" +
                mTrackTitle + ", mAlbumTitle=" + mAlbumTitle);
            str.append("\r\n");
            str.append("mArtWorkHandle = " + mArtWorkHandle +
                ", mArtWorkHandleStr = " + mArtWorkHandleStr);
            str.append("\r\n");
            str.append("mArtWorkState = " + mArtWorkState);
            str.append("\r\n");
            return str.toString();
        }

    }

}
