package com.mediatek.gallery3d.video;

import java.util.ArrayList;
import android.util.Log;

public class DefaultMovieList implements IMovieList {
    private static final String TAG = "Gallery2/DefaultMovieList";
    private static final boolean LOG = true;

    private final ArrayList<IMovieItem> mItems = new ArrayList<IMovieItem>();
    private static final int UNKNOWN = -1;

    @Override
    public void add(IMovieItem item) {
        if (LOG) {
            Log.v(TAG, "add(" + item + ")");
        }
        mItems.add(item);
    }

    @Override
    public int index(IMovieItem item) {
        int find = UNKNOWN;
        int size = mItems.size();
        for (int i = 0; i < size; i++) {
            if (item == mItems.get(i)) {
                find = i;
                break;
            }
        }
        if (LOG) {
            Log.v(TAG, "index(" + item + ") return " + find);
        }
        return find;
    }

    @Override
    public int size() {
        return mItems.size();
    }

    @Override
    public IMovieItem getNext(IMovieItem item) {
        IMovieItem next = null;
        int find = index(item);
        if (find >= 0 && find < size() - 1) {
            next = mItems.get(++find);
        }
        return next;
    }

    @Override
    public IMovieItem getPrevious(IMovieItem item) {
        IMovieItem prev = null;
        int find = index(item);
        if (find > 0 && find < size()) {
            prev = mItems.get(--find);
        }
        return prev;
    }

}
