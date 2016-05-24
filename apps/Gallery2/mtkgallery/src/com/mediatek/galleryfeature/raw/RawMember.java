package com.mediatek.galleryfeature.raw;

import android.content.Context;

import com.mediatek.galleryfeature.config.FeatureConfig;
import com.mediatek.galleryframework.base.BottomControlLayer;
import com.mediatek.galleryframework.base.ComboLayer;
import com.mediatek.galleryframework.base.ExtItem;
import com.mediatek.galleryframework.base.Layer;
import com.mediatek.galleryframework.base.MediaData;
import com.mediatek.galleryframework.base.MediaMember;

import java.util.ArrayList;

/** One type of MediaMember special for raw.
 * 1. Check if one MediaData is raw
 * 2. Return the special layer for raw
 */
public class RawMember extends MediaMember {
    private static final String TAG = "MtkGallery2/RawMember";

    /** Constructor for RawMember, no special operation, but same as parent.
     * @param context The context of current application environment
     */
    public RawMember(Context context) {
        super(context);
    }

    @Override
    public boolean isMatching(MediaData md) {
        return RawHelper.isRawJpg(md) && RawHelper.hasRawFile(mContext, md.caption);
    }

    @Override
    public ExtItem getItem(MediaData md) {
        return new ExtItem(md);
    }

    @Override
    public Layer getLayer() {
        Layer rawLayer = new RawLayer();
        if (FeatureConfig.sSupportStereo) {
            Layer bottomControlLayer = new BottomControlLayer();
            ArrayList<Layer> layers = new ArrayList<Layer>();
            layers.add(rawLayer);
            layers.add(bottomControlLayer);
            return new ComboLayer(mContext, layers);
        } else {
            return rawLayer;
        }
    }

    @Override
    public MediaData.MediaType getMediaType() {
        return MediaData.MediaType.RAW;
    }
}