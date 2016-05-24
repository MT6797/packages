package com.mediatek.galleryfeature.raw;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore.Files;
import android.provider.MediaStore.Files.FileColumns;

import com.mediatek.galleryframework.base.MediaData;
import com.mediatek.galleryframework.util.MtkLog;

/** Class which provides the util for raw.
 */
class RawHelper {
    private static final String TAG = "MtkGallery2/RawHelper";

    private static final Uri CONTENT_URI = Files.getContentUri("external");
    private static final String[] PROJECTION = new String[] { FileColumns._ID, FileColumns.DATA };
    private static final String WHERE_CLAUSE = FileColumns.DATA + " LIKE ?";

    public static boolean isRawJpg(MediaData md) {
        if ("image/jpeg".equals(md.mimeType) && md.caption != null
                && md.caption.matches("^IMG.*_RAW$") && md.filePath != null
                && md.filePath.endsWith(".jpg")) {
            return true;
        }
        return false;
    }

    // jpgFileName, no suffix: such as IMG_20150101_010101
    public static boolean hasRawFile(Context context, String jpgFileName) {
        if (context == null) {
            MtkLog.e(TAG, "<hasRawFile> Context is null, return false");
            return false;
        }

        String dngFileName = jpgFileName + ".dng";
        Cursor cursor = context.getContentResolver().query(CONTENT_URI, PROJECTION, WHERE_CLAUSE,
                new String[] { "%" + dngFileName }, null);

        if (cursor == null) {
            return false;
        }

        if (cursor.moveToFirst() == false || cursor.getCount() == 0) {
            cursor.close();
            return false;
        }

        cursor.close();
        return true;
    }
}