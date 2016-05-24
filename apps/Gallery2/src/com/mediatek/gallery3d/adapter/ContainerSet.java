package com.mediatek.gallery3d.adapter;

import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore.Images;

import com.android.gallery3d.app.GalleryApp;
import com.android.gallery3d.data.ChangeNotifier;
import com.android.gallery3d.data.LocalImage;
import com.android.gallery3d.data.LocalMediaItem;
import com.android.gallery3d.data.MediaItem;
import com.android.gallery3d.data.MediaSet;
import com.android.gallery3d.data.Path;
import com.mediatek.galleryfeature.container.ContainerHelper;
import com.mediatek.galleryframework.base.MediaData;
import com.mediatek.galleryframework.util.MtkLog;

import java.util.ArrayList;

public class ContainerSet extends MediaSet {
    private static final String TAG = "MtkGallery2/ContainerSet";

    private GalleryApp mApp;
    private MediaData.SubType mType;
    private Uri mBaseUri;
    private ChangeNotifier mNotifier;

    private String mName;
    private LocalImage mParentItem;
    private ArrayList<MediaItem> mMediaItem;

    // special for conshot
    private int mBucketId;
    private long mGroupId;

    // constructor for conshot
    public ContainerSet(Path path, GalleryApp application, int bucketId, long groupId,
            boolean isConshot) {
        super(path, nextVersionNumber());
        assert (isConshot);
        MtkLog.i(TAG, "<ContainerSet> path = " + path + ", bucketId = " + bucketId + ", groupId = "
                + groupId);

        mApp = application;
        mType = MediaData.SubType.CONSHOT;

        mBaseUri = Images.Media.EXTERNAL_CONTENT_URI;
        mNotifier = new ChangeNotifier(this, mBaseUri, application);

        mBucketId = bucketId;
        mGroupId = groupId;

        reloadMediaItem();
    }

    @Override
    public String getName() {
        return mName;
    }

    @Override
    public long reload() {
        if (mNotifier.isDirty()) {
            mDataVersion = nextVersionNumber();
            reloadMediaItem();
        }
        return mDataVersion;
    }

    @Override
    public int getMediaItemCount() {
        if (mMediaItem != null) {
            return mMediaItem.size();
        }
        return 0;
    }

    @Override
    public ArrayList<MediaItem> getMediaItem(int start, int count) {
        ArrayList<MediaItem> list = new ArrayList<MediaItem>();
        int size = getMediaItemCount();
        if (start >= size) {
            MtkLog.i(TAG, "<getMediaItem> start = " + start + ", size = "
                    + size + ", start >= size, return");
            return list;
        }
        if ((start + count) > size) {
            count = size - start;
        }

        for (int i = 0; i < count; i++) {
            list.add(mMediaItem.get(i + start));
        }
        return list;
    }

    @Override
    public boolean isLeafAlbum() {
        return true;
    }

    public MediaItem getParentItem() {
        return mParentItem;
    }

    private void reloadMediaItem() {
        if (mType == MediaData.SubType.CONSHOT) {
            reloadConshots();
        } else {
            MtkLog.i(TAG, "<reloadMediaItem> Error!! Not container");
        }
    }

    private void reloadConshots() {
        MtkLog.i(TAG, "<reloadConshots>");
        if (mType != MediaData.SubType.CONSHOT) {
            MtkLog.i(TAG, "<reloadConshots> not CONSHOT, return");
            return;
        }

        Cursor cursor = ContainerHelper.getConShotsCursor(mApp.getAndroidContext(),
                mGroupId, mBucketId);
        if (cursor == null || !cursor.moveToFirst()) {
            mMediaItem = null;
            mParentItem = null;
            if (cursor != null) {
                cursor.close();
            }
            MtkLog.i(TAG, "<reloadConshots> getConShotsCursor return null,"
                    + "set mMediaItem and mParentItem null, return");
            return;
        }

        Path path = Path.fromString("/local/image/item").getChild(
                cursor.getLong(MediaData.IMAGE_INDEX_ID));
        mParentItem = (LocalImage) mApp.getDataManager().getMediaObject(path);
        MtkLog.i(TAG, "<reloadConshots> mParentItem = " + mParentItem + ", path = " + path);
        if (mParentItem != null) {
            mName = mParentItem.getName();
        }

        ArrayList<MediaItem> tempMediaItem = new ArrayList<MediaItem>();
        ArrayList<MediaData> subData = MediaData.parseImageMediaDatas(cursor, false);
        for (MediaData md : subData) {
            ContainerHelper.checkAndInitGroupInfo(md);
        }
        cursor.moveToFirst();
        for (MediaData data : subData) {
            Path mediaPath = Path.fromString(ContainerSource.CONTAINER_CONSHOT_ITEM)
                    .getChild(data.id);
            // if reloadConshots & delete cs image are running at the same time,
            // item may be null, so if item is null, do not update and not add to mMediaItem
            LocalMediaItem item = (LocalMediaItem) mApp.getDataManager().getMediaObject(
                    mediaPath);
            if (item != null) {
                item.updateContent(cursor);
                tempMediaItem.add(item);
            } else {
                MtkLog.i(TAG, "<reloadConshots> The item of path [" + mediaPath
                        + "] is null, not add to mMediaItem");
            }
            cursor.moveToNext();
        }
        cursor.close();

        // update media item
        mMediaItem = tempMediaItem;
    }
}
