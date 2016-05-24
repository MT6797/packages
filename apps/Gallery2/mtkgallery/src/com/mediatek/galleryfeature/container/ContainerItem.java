package com.mediatek.galleryfeature.container;

import android.content.Context;
import android.net.Uri;

import com.android.gallery3d.R;
import com.mediatek.galleryframework.base.ExtItem;
import com.mediatek.galleryframework.base.MediaData;
import com.mediatek.galleryframework.base.MediaData.SubType;
import com.mediatek.galleryframework.base.ThumbType;

import java.io.File;
import java.util.ArrayList;

public class ContainerItem extends ExtItem {
    private MediaData mData;

    public ContainerItem(Context context, MediaData md) {
        super(context, md);
        mData = md;
    }

    @Override
    public ArrayList<SupportOperation> getNotSupportedOperations() {
        ArrayList<MediaData> relateData = mData.relateData;
        if (relateData != null) {
            ArrayList<SupportOperation> nsp = new ArrayList<SupportOperation>();
            nsp.add(SupportOperation.CROP);
            nsp.add(SupportOperation.EDIT);
            nsp.add(SupportOperation.ROTATE);
            nsp.add(SupportOperation.FULL_IMAGE);
            if (mData.subType == SubType.CONSHOT) {
                nsp.add(SupportOperation.SETAS);
            }
            return nsp;
        }
        return null;
    }

    @Override
    public void delete() {
        ArrayList<MediaData> relateData = mData.relateData;
        if (relateData != null) {
            // delete files
            for (MediaData data : relateData) {
                File file = new File(data.filePath);
                if (!file.exists()) {
                    continue;
                }
                file.delete();
            }
            // delete record in media database
            // To avoid refresh frequently,
            // delete items in media database at a time.
            if (mData.subType == SubType.CONSHOT) {
                ContainerHelper.deleteConshotDatas(mContext, mData.groupID, mData.bucketId);
            }
        }
    }

    @Override
    public Uri[] getContentUris() {
        ArrayList<MediaData> relateData = mData.relateData;
        if (relateData != null) {
            ArrayList<Uri> uriList = new ArrayList<Uri>();

            for (MediaData data : relateData) {
                uriList.add(Uri.parse("file:/" + data.filePath));
            }

            Uri[] uris = uriList.toArray(new Uri[uriList.size()]);
            return uris;
        }
        return null;
    }

    @Override
    public String[] getDetails() {
        String title = null;
        if (mData.subType == MediaData.SubType.CONSHOT) {
            title = mContext.getResources()
                    .getString(R.string.m_conshots_title);
        } else {
            return null;
        }
        String[] res = new String[1];
        res[0] = title;
        return res;
    }

    @Override
    public boolean isNeedToCacheThumb(ThumbType thumbType) {
        return true;
    }

    @Override
    public boolean isAllowPQWhenDecodeCache(ThumbType thumbType) {
        return false;
    }
}