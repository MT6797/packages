package com.mediatek.settings.cdma;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class OmhEventHandler extends Handler {
    private static final String TAG = "OmhEventHandler";

    private static OmhEventHandler mHandler;
    private int mState = IDLE;
    private Context mContext;

    // state definition
    private static final int IDLE = 0;
    private static final int BUSY = IDLE +1;
    private static final int PENDING = BUSY +1;
    private static final int INPROGRESS = PENDING +1;

    // event type
    public static final int SET_BUSY = 100;
    public static final int NEW_REQUEST = SET_BUSY + 1;
    public static final int CLEAR_BUSY = NEW_REQUEST + 1;
    public static final int FINISH_REQUEST = CLEAR_BUSY + 1;

    // argument type
    public static final int TYPE_OMH_WARNING = 1000;
    public static final int TYPE_OMH_DATA_PICK = TYPE_OMH_WARNING + 1;

    private OmhEventHandler(Context context) {
        super(context.getMainLooper());
        mContext = context;
    }

    private synchronized static void createInstance(Context context) {
        if (mHandler == null) {
            mHandler = new OmhEventHandler(context);
        }
    }

    public static OmhEventHandler getInstance(Context context) {
        if (mHandler == null) {
            createInstance(context);
        }
        return mHandler;
    }

    public void handleMessage(Message msg) {
        Log.d(TAG, "handleMessage, msg = " + msg + ", while state = " + mState);
        switch (msg.what) {
        case SET_BUSY:
            if (mState == IDLE) {
                mState = BUSY;
            } else {
                Log.w(TAG, "SET_BUSY when state = " + mState);
            }
            break;
        case NEW_REQUEST:
            if (mState == IDLE) {
                if (msg.arg1 == TYPE_OMH_WARNING) {
                    mState = INPROGRESS;
                    CdmaUtils.startOmhWarningDialog(mContext);
                } else if (msg.arg1 == TYPE_OMH_DATA_PICK) {
                    CdmaUtils.startOmhDataPickDialog(mContext, msg.arg2);
                }
            } else if (mState == BUSY) {
                if (msg.arg1 == TYPE_OMH_WARNING) {
                    mState = PENDING;
                }
            } else {
                Log.w(TAG, "NEW_REQUEST when state = " + mState);
            }
            break;
        case CLEAR_BUSY:
            if (mState == BUSY) {
                mState = IDLE;
            } else if (mState == PENDING) {
                mState = INPROGRESS;
                CdmaUtils.startOmhWarningDialog(mContext);
            } else {
                Log.w(TAG, "CLEAR_BUSY when state = " + mState);
            }
            break;
        case FINISH_REQUEST:
            if (mState == INPROGRESS) {
                mState = IDLE;
            } else {
                Log.w(TAG, "FINISH_REQUEST when state = " + mState);
                mState = IDLE;
            }
            break;
        default:
            break;
        }
    };
}
