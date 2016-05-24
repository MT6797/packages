package com.mediatek.galleryfeature.container;

import android.content.Intent;
import android.provider.MediaStore.Images.ImageColumns;

import com.mediatek.galleryframework.base.MediaFilter;
import com.mediatek.galleryframework.base.MediaFilter.IFilter;

public class ConshotFilter implements IFilter {
    private static final String TITLE_STYLE = "'IMG\\_[0-9]{"
            + ContainerHelper.COUNT_YYYYMMDD + "}\\_[0-9]{"
            + ContainerHelper.COUNT_HHMMSS + "}\\_[0-9]+CS'";

    private static final int TITLE_PREFIX_SUB_SOUNT = 6;    // Length of "IMG_", "_", "_"
    private static final int TITLE_PREFIX_COUNT = TITLE_PREFIX_SUB_SOUNT
            + ContainerHelper.COUNT_YYYYMMDD + ContainerHelper.COUNT_HHMMSS;
    public static final String GROUP_INDEX_STYLE = "CAST(TRIM(SUBSTR(" + ImageColumns.TITLE + ", "
            + (TITLE_PREFIX_COUNT + 1) + "), 'CS') as int)";

    public void setFlagFromIntent(Intent intent, MediaFilter filter) {
    }

    public void setDefaultFlag(MediaFilter filter) {
        filter.setFlagEnable(MediaFilter.INCLUDE_CONSHOT_GROUP);
    }

    public String getWhereClauseForImage(int flag, int bucketID) {
        if ((flag & MediaFilter.INCLUDE_CONSHOT_GROUP) != 0
                && bucketID != MediaFilter.INVALID_BUCKET_ID) {
            StringBuilder sb = new StringBuilder();
            sb.append("(" + ImageColumns.BUCKET_ID + " = " + bucketID + ")");
            sb.append(" AND (");
            sb.append("(" + ImageColumns.TITLE + " NOT REGEXP " + TITLE_STYLE + ")");
            sb.append(" OR (");
            sb.append(ImageColumns.TITLE
                    + " in (SELECT SUBSTR(" + ImageColumns.TITLE
                    + ", 1, " + TITLE_PREFIX_COUNT
                    +  ") || MIN(" + GROUP_INDEX_STYLE
                    + ") || 'CS' FROM images WHERE ");
            sb.append(ImageColumns.TITLE + " REGEXP " + TITLE_STYLE);
            sb.append(" AND ");
            sb.append(ImageColumns.BUCKET_ID + " = " + bucketID);
            sb.append(" AND ");
            sb.append(ImageColumns.MIME_TYPE + " = 'image/jpeg'");
            sb.append(" GROUP BY SUBSTR(" + ImageColumns.TITLE
                    + ", 1, " + TITLE_PREFIX_COUNT + ")");
            sb.append(")))");
            return sb.toString();
        }
        return null;
    }

    public String getWhereClauseForVideo(int flag, int bucketID) {
        return null;
    }

    public String getWhereClause(int flag, int bucketID) {
        return getWhereClauseForImage(flag, bucketID);
    }

    public String getDeleteWhereClauseForImage(int flag, int bucketID) {
        return null;
    }

    public String getDeleteWhereClauseForVideo(int flag, int bucketID) {
        return null;
    }

    public String convertFlagToString(int flag) {
        if ((flag & MediaFilter.INCLUDE_CONSHOT_GROUP) != 0) {
            return "INCLUDE_CONSHOT_GROUP, ";
        }
        return "";
    }
}
