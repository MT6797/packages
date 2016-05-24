package com.android.soundrecorder.tests;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Random;

import junit.framework.Assert;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.storage.IMountService;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;

import com.android.soundrecorder.Recorder;
import com.android.soundrecorder.LogUtils;
import com.android.soundrecorder.SoundRecorder;
import com.android.soundrecorder.RecordingFileList;
import com.android.soundrecorder.SoundRecorderService;
import com.mediatek.storage.StorageManagerEx;

public class SoundRecorderTestUtils {
    private final static String TAG = "SR/SoundRecorderTestUtils";
    private final static Random mRandom = new Random();

//    private static int mRandomTimes = 0;
//
//    private static final List<Integer> RANDOM_ARRAY = new ArrayList<Integer>(11);
//    static {
//      RANDOM_ARRAY.add(1); //list
//      RANDOM_ARRAY.add(0); //list-play
//      RANDOM_ARRAY.add(0);
//      RANDOM_ARRAY.add(2); //stop-play
//      RANDOM_ARRAY.add(2); //list
//      RANDOM_ARRAY.add(2);
//      RANDOM_ARRAY.add(1);
//      RANDOM_ARRAY.add(2);
//      RANDOM_ARRAY.add(1);
//      RANDOM_ARRAY.add(0);
//      RANDOM_ARRAY.add(0);
//    }

    public static String getCurrentFilePath(SoundRecorder sr) {
        SoundRecorderService service = null;
        try {
            Field serviceState = SoundRecorder.class.getDeclaredField("mService");
            serviceState.setAccessible(true);
            service = (SoundRecorderService) serviceState.get(sr);
            if (null == service) {
                return null;
            } else {
                return service.getCurrentFilePath();
            }
        } catch (NoSuchFieldException e) {
            Log.e(TAG, "No such field in runTestOnUiThread:", e);
            return null;
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Illegal argument exception in runTestOnUiThread:", e);
            return null;
        } catch (IllegalAccessException e) {
            Log.e(TAG, "Illegal access exception in runTestOnUiThread:", e);
            return null;
        }
    }

    public static int getCurrentState(SoundRecorder sr) {
        SoundRecorderService service = null;
        int EXCEPTION = -1;
        try {
            Field serviceState = SoundRecorder.class
                    .getDeclaredField("mService");
            serviceState.setAccessible(true);
            service = (SoundRecorderService) serviceState.get(sr);
            return service.getCurrentState();
        } catch (NoSuchFieldException e) {
            Log.e(TAG, "No such field in runTestOnUiThread:", e);
            return EXCEPTION;
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Illegal argument exception in runTestOnUiThread:", e);
            return EXCEPTION;
        } catch (IllegalAccessException e) {
            Log.e(TAG, "Illegal access exception in runTestOnUiThread:", e);
            return EXCEPTION;
        } catch (NullPointerException e) {
            Log.e(TAG, "Null Pointer exception in runTestOnUiThread:", e);
            return EXCEPTION;
        }
    }

    public static boolean isInEditMode(RecordingFileList recordingFileList) {
        boolean EXCEPTION = false;
        try {
            Field modeField = RecordingFileList.class
                    .getDeclaredField("mCurrentAdapterMode");
            modeField.setAccessible(true);
            Integer mode = (Integer) modeField.get(recordingFileList);
            return (mode == RecordingFileList.EDIT);
        } catch (NoSuchFieldException e) {
            Log.e(TAG, "No such field in runTestOnUiThread:", e);
            return EXCEPTION;
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Illegal argument exception in runTestOnUiThread:", e);
            return EXCEPTION;
        } catch (IllegalAccessException e) {
            Log.e(TAG, "Illegal access exception in runTestOnUiThread:", e);
            return EXCEPTION;
        } catch (NullPointerException e) {
            Log.e(TAG, "Null Pointer exception in runTestOnUiThread:", e);
            return EXCEPTION;
        }
    }

    public static void mountSDCard(Context context) {
        try {
            getMountService().mountVolume(StorageManagerEx.getDefaultPath());
        } catch (RemoteException e) {
            Log.i(TAG, e.toString());
        }
    }

    public static void unmountSDCard(Context context) {
        try {
            getMountService().unmountVolume(StorageManagerEx.getDefaultPath(),
                    true, false);
        } catch (RemoteException e) {
            Log.i(TAG, e.toString());
        }
    }

    public static void checkFileExist(String filePath) {
        Assert.assertNotNull(filePath);
        File file = new File(filePath);
        Assert.assertTrue(file.exists());
    }

    public static void checkFileNotExist(String filePath) {
        Assert.assertNotNull(filePath);
        File file = new File(filePath);
        Assert.assertTrue(!file.exists());
    }

    public static void checkFileInMediaDB(Context context, String filePath,
            boolean isInMediaDB) {
        Assert.assertNotNull(filePath);

        ContentResolver resolver = context.getContentResolver();
        Uri base = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        final String[] ids = new String[] { MediaStore.Audio.Media._ID };
        final String where = MediaStore.Audio.Media.DATA + " LIKE '%"
                + filePath + "'";

        Cursor cursor = resolver.query(base, ids, where, null, null);
        Assert.assertNotNull(cursor);
        if (isInMediaDB) {
            Assert.assertTrue(cursor.getCount() == 1);
        } else {
            Assert.assertTrue(cursor.getCount() == 0);
        }
        cursor.close();
    }

    public static void deleteFile(String filePath) {
        Assert.assertNotNull(filePath);
        File file = new File(filePath);
        if (file.exists()) {
            boolean isDeleted = file.delete();
            if (isDeleted) {
                Log.d(TAG, "file is deleted");
            }
        }
    }

    public static void deleteFromMediaDB(Context context, String filePath) {
        Assert.assertNotNull(filePath);
        ContentResolver resolver = context.getContentResolver();
        Uri base = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        final String where = MediaStore.Audio.Media.DATA + " LIKE '%"
                + filePath + "'";
        int deleteNum = resolver.delete(base, where, null);
        LogUtils.i(TAG, "<deleteFromMediaDB> delete "
                + String.valueOf(deleteNum) + " items in db");
    }

    public static int getRandom(int max) {
//      int random = RANDOM_ARRAY.get(mRandomTimes);
//      mRandomTimes++;
//      return random;
        return Math.abs(mRandom.nextInt() % max);
    }

    public static boolean isMediaPlayerPlaying(SoundRecorder sr) {
        Recorder recorder = null;
        MediaPlayer mediaPlayer = null;
        try {
            Field recorderField = SoundRecorder.class
                    .getDeclaredField("mRecorder");
            recorderField.setAccessible(true);
            recorder = (Recorder) recorderField.get(sr);
            if (recorder == null) {
                return false;
            }
            Field mediaPlayerField = Recorder.class.getDeclaredField("mPlayer");
            mediaPlayerField.setAccessible(true);
            mediaPlayer = (MediaPlayer) mediaPlayerField.get(recorder);
            if (mediaPlayer == null) {
                return false;
            }
            return mediaPlayer.isPlaying();
        } catch (NoSuchFieldException e) {
            LogUtils.e(TAG, "No such field in runTestOnUiThread:", e);
            return false;
        } catch (IllegalArgumentException e) {
            LogUtils.e(TAG,
                    "Illegal argument exception in runTestOnUiThread:", e);
            return false;
        } catch (IllegalAccessException e) {
            LogUtils.e(TAG, "Illegal access exception in runTestOnUiThread:",
                    e);
            return false;
        } catch (NullPointerException e) {
            LogUtils
                    .e(TAG, "Null Pointer exception in runTestOnUiThread:", e);
            return false;
        }
    }

    private static IMountService getMountService() {
        IMountService mountService = null;
        IBinder service = ServiceManager.getService("mount");
        Assert.assertNotNull(service);
        mountService = IMountService.Stub.asInterface(service);
        return mountService;
    }

    public static class ScreenShot {
        private static Bitmap takeScreenShot(Activity activity) {
            View view = activity.getWindow().getDecorView();
            view.setDrawingCacheEnabled(true);
            view.buildDrawingCache();
            Bitmap b1 = view.getDrawingCache();
            Rect frame = new Rect();
            activity.getWindow().getDecorView().getWindowVisibleDisplayFrame(
                    frame);
            int statusBarHeight = frame.top;
            System.out.println(statusBarHeight);
            int width = activity.getWindowManager().getDefaultDisplay()
                    .getWidth();
            int height = activity.getWindowManager().getDefaultDisplay()
                    .getHeight();
            Bitmap b = Bitmap.createBitmap(b1, 0, statusBarHeight, width,
                    height - statusBarHeight);
            view.destroyDrawingCache();
            return b;
        }

        private static void savePic(Bitmap b, String strFileName) {
            FileOutputStream fos = null;
            try {
                fos = new FileOutputStream(strFileName);
                if (null != fos) {
                    b.compress(Bitmap.CompressFormat.PNG, 90, fos);
                    fos.flush();
                    fos.close();
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void screenShot(Activity a, String path) {
        ScreenShot.savePic(ScreenShot.takeScreenShot(a), path);
    }
}