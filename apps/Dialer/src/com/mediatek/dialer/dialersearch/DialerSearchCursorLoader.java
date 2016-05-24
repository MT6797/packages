package com.mediatek.dialer.dialersearch;

import android.content.AsyncTaskLoader;
import android.content.Context;
import android.database.Cursor;
import android.util.Log;

import com.android.dialer.database.DialerDatabaseHelper;
import com.android.dialer.dialpad.SmartDialNameMatcher;
import com.android.dialer.dialpad.SmartDialPrefix;

import com.mediatek.dialer.util.DialerSearchUtils;

/**
 * M: [MTK Dialer Search]
 * Implements a Loader<Cursor> class to asynchronously load SmartDial search results.
 */
public class DialerSearchCursorLoader extends AsyncTaskLoader<Cursor> {

    private final String TAG = "DialerSearchCursorLoader";
    private final boolean DEBUG = true;
    private final Context mContext;
    private Cursor mCursor;
    private String mQuery;
    private boolean mUseCallableUri = false;
    // Set true if it can not apply MTK dialer search, and go to default flow.
    private boolean mEnableDefaultSearch = false;

    public DialerSearchCursorLoader(Context context, boolean useCallable) {
        super(context);
        mContext = context;
        mUseCallableUri = useCallable;
    }

    /**
     * Configures the query string to be used to find SmartDial matches.
     * @param query The query string user typed.
     */
    public void configureQuery(String query, boolean isSmartQuery) {

        Log.d(TAG, "MTK-DialerSearch, Configure new query to be " + query);

        mQuery = query;
        if (!isSmartQuery) {
            mQuery = DialerSearchUtils.stripTeleSeparators(query);
        }
        if (!DialerSearchUtils.isValidDialerSearchString(mQuery)) {
            mEnableDefaultSearch = true;
        }
    }

    /**
     * Queries the Contacts database and loads results in background.
     * @return Cursor of contacts that matches the SmartDial query.
     */
    @Override
    public Cursor loadInBackground() {

        Log.d(TAG, "MTK-DialerSearch, Load in background. mQuery: " + mQuery);

        final DialerSearchHelper dialerSearchHelper = DialerSearchHelper.getInstance(mContext);
        Cursor cursor = null;
        if (mEnableDefaultSearch) {
            cursor = dialerSearchHelper.getRegularDialerSearchResults(mQuery, mUseCallableUri);
        } else {
            cursor = dialerSearchHelper.getSmartDialerSearchResults(mQuery);
        }
        if (cursor != null) {
            Log.d(TAG, "MTK-DialerSearch, loadInBackground, result.getCount: "
                    + cursor.getCount());

            return cursor;
        } else {
            Log.w(TAG, "MTK-DialerSearch, ----cursor is null----");
            return null;
        }
    }

    @Override
    public void deliverResult(Cursor cursor) {
        if (isReset()) {
            Log.d(TAG, "MTK-DialerSearch, deliverResult releaseResources " + this);
            /** The Loader has been reset; ignore the result and invalidate the data. */
            releaseResources(cursor);
            return;
        }

        /** Hold a reference to the old data so it doesn't get garbage collected. */
        Cursor oldCursor = mCursor;
        mCursor = cursor;

        if (isStarted()) {
            /** If the Loader is in a started state, deliver the results to the client. */
            super.deliverResult(cursor);
        }

        /** Invalidate the old data as we don't need it any more. */
        if (oldCursor != null && oldCursor != cursor) {
            releaseResources(oldCursor);
        }
    }

    @Override
    protected void onStartLoading() {
        if (mCursor != null) {
            /** Deliver any previously loaded data immediately. */
            deliverResult(mCursor);
        }
        if (mCursor == null) {
            /** Force loads every time as our results change with queries. */
            forceLoad();
        }
    }

    @Override
    protected void onStopLoading() {
        /** The Loader is in a stopped state, so we should attempt to cancel the current load. */
        cancelLoad();
    }

    @Override
    protected void onReset() {
        Log.d(TAG, "MTK-DialerSearch, onReset() "  + this);
        /** Ensure the loader has been stopped. */
        onStopLoading();

        /** Release all previously saved query results. */
        if (mCursor != null) {
            Log.d(TAG, "MTK-DialerSearch, onReset() releaseResources "  + this);
            releaseResources(mCursor);
            mCursor = null;
        }
    }

    @Override
    public void onCanceled(Cursor cursor) {
        super.onCanceled(cursor);

        Log.d(TAG, "MTK-DialerSearch, onCanceled() " + this);

        /** The load has been canceled, so we should release the resources associated with 'data'.*/
        releaseResources(cursor);
    }

    private void releaseResources(Cursor cursor) {
        if (cursor != null) {
            Log.w(TAG, "MTK-DialerSearch, releaseResources close cursor " + this);
            cursor.close();
            cursor = null;
        }
    }
}
