package com.mediatek.galleryframework.base;

import com.mediatek.galleryfeature.config.FeatureConfig;

import android.content.Context;

public class MediaMember {
    protected Context mContext;

    public MediaMember(Context context) {
        mContext = context;
    }

    public boolean isMatching(MediaData md) {
        return true;
    }

    public Player getPlayer(MediaData md, ThumbType type) {
        return null;
    }

    public Generator getGenerator() {
        return null;
    }

    public Layer getLayer() {
        if (FeatureConfig.sSupportStereo) {
            return new BottomControlLayer();
        } else {
            return null;
        }
    }

    public ExtItem getItem(MediaData md) {
        return new ExtItem(mContext, md);
    }

    public MediaData.MediaType getMediaType() {
        return MediaData.MediaType.NORMAL;
    }
}