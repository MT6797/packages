package com.mediatek.gallery3d.adapter;

import com.android.gallery3d.app.GalleryApp;
import com.android.gallery3d.data.LocalImage;
import com.android.gallery3d.data.MediaObject;
import com.android.gallery3d.data.MediaSource;
import com.android.gallery3d.data.Path;
import com.android.gallery3d.data.PathMatcher;
import com.mediatek.galleryframework.util.MtkLog;

public class ContainerSource extends MediaSource {

    private static final String TAG = "MtkGallery2/ContainerSource";

    private static final int CONTAINER_BY_CONSHOT_ITEM = 0;
    private static final int CONTAINER_BY_CONSHOT_SET = 2;

    public static final String CONTAINER_CONSHOT_ITEM = "/container/conshot/item";
    public static final String CONTAINER_CONSHOT_SET = "/container/conshot/set";
    // conshot set path - /container/conshot/set/xx1/xx2, xx1 is bucket id, xx2 is group id

    private GalleryApp mApplication;
    private PathMatcher mMatcher;

    public ContainerSource(GalleryApp application) {
        super("container");
        mApplication = application;
        mMatcher = new PathMatcher();
        mMatcher.add(CONTAINER_CONSHOT_ITEM + "/*", CONTAINER_BY_CONSHOT_ITEM);
        mMatcher.add(CONTAINER_CONSHOT_SET + "/*/*", CONTAINER_BY_CONSHOT_SET);
    }

    @Override
    public MediaObject createMediaObject(Path path) {
        MtkLog.w(TAG, "<createMediaObject> path = " + path);
        switch (mMatcher.match(path)) {
        case CONTAINER_BY_CONSHOT_ITEM:
            return new LocalImage(path, mApplication, Integer.parseInt(mMatcher.getVar(0)), true);
        case CONTAINER_BY_CONSHOT_SET:
            return new ContainerSet(path, mApplication, Integer.parseInt(mMatcher.getVar(0)), Long
                    .parseLong(mMatcher.getVar(1)), true);
        default:
            throw new RuntimeException("bad path: " + path);
        }
    }
}
