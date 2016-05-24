package com.mediatek.galleryfeature.gif;

import android.content.Context;

import com.mediatek.galleryfeature.platform.PlatformHelper;
import com.mediatek.galleryframework.base.ExtItem;
import com.mediatek.galleryframework.base.Layer;
import com.mediatek.galleryframework.base.MediaData;
import com.mediatek.galleryframework.base.MediaMember;
import com.mediatek.galleryframework.base.Player;
import com.mediatek.galleryframework.base.ThumbType;
import com.mediatek.galleryframework.gl.GLIdleExecuter;
import com.mediatek.galleryframework.util.MtkLog;

public class GifMember extends MediaMember {
    private final static String TAG = "MtkGallery2/GifMember";
    private GLIdleExecuter mGlIdleExecuter;

    public GifMember(Context context, GLIdleExecuter exe) {
        super(context);
        mGlIdleExecuter = exe;
    }

    public GifMember(Context context) {
        this(context, null);
    }

    @Override
    public boolean isMatching(MediaData md) {
        return md.mimeType.equals("image/gif");
    }

    @Override
    public Player getPlayer(MediaData md, ThumbType type) {
        if (PlatformHelper.isOutOfDecodeSpec(md.fileSize, md.width,
                md.height, md.mimeType)) {
            MtkLog.i(TAG, "<getPlayer>, outof decode spec, return null!");
            return null;
        }
        return new GifPlayer(mContext, md, Player.OutputType.TEXTURE, type, mGlIdleExecuter);
    }

    @Override
    public ExtItem getItem(MediaData md) {
        return new GifItem(md, mContext);
    }

    public MediaData.MediaType getMediaType() {
        return MediaData.MediaType.GIF;
    }

    @Override
    public Layer getLayer() {
        return null;
    }
}
