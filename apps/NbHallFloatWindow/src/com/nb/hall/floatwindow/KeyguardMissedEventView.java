/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.nb.hall.floatwindow;

import android.app.ActivityManagerNative;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.provider.CallLog;
import android.provider.CallLog.Calls;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;

public class KeyguardMissedEventView extends LinearLayout {

    private static final String TAG = "KeyguardMissedEventView";

    private static final int QUERY_CALL_REQUEST = 1;
    private static final int QUERY_MESSAGE_REQUEST = 2;

    private static final int QUERY_CALL_COMPLETE = 1;
    private static final int QUERY_MESSAGE_COMPLETE = 2;

    private static Looper sLooper = null;

    private QueryHandler mQueryHandler;

    private class QueryHandler extends Handler {

        public QueryHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            Log.i(TAG, "sQueryHandler#handleMessage: what=" + msg.what);
            switch (msg.what) {
                case QUERY_CALL_REQUEST:
                    Cursor callCursor = mContext.getContentResolver().query(Calls.CONTENT_URI,
                            new String[] {
                                    Calls._ID, Calls.NUMBER, Calls.TYPE, Calls.NEW, Calls.DATE
                            },
                            Calls.TYPE + " = " + Calls.MISSED_TYPE + " and " + Calls.NEW + " = 1",
                            null,
                            Calls.DATE + " DESC");
                    mHandler.obtainMessage(QUERY_CALL_COMPLETE, 0, 0, callCursor).sendToTarget();
                    break;
                case QUERY_MESSAGE_REQUEST:
                    Cursor messageCursor = mContext.getContentResolver().query(
                            Uri.parse("content://sms"), new String[] {
                                    "_id", "address", "type", "read", "body", "date"
                            }, "type = 1 and read = 0", null, "date" + " DESC");
                    mHandler.obtainMessage(QUERY_MESSAGE_COMPLETE, 0, 0, messageCursor)
                            .sendToTarget();
                    break;
                default:
                    break;
            }
        };
    };

    private Context mContext;

    private View mMissedCallView;
    private View mMissedMmsView;

    private Handler mHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            Log.i(TAG, "mHandler#handleMessage: msg.what=" + msg.what);
            switch (msg.what) {
                case QUERY_CALL_COMPLETE:
                    Cursor callCursor = (Cursor) msg.obj;
                    ArrayList<Integer> callIds = new ArrayList<Integer>();
                    if (callCursor != null) {
                        while (callCursor.moveToNext()) {
                            int id = callCursor.getInt(callCursor.getColumnIndex(Calls._ID));
                            callIds.add(id);
                        }

                        callCursor.close();
                    }
                    Log.i(TAG, "callIds.size(): " + callIds.size());
                    if (callIds.size() > 0) {
                        if (mMissedCallView == null) {
                            mMissedCallView = LayoutInflater.from(mContext)
                                    .inflate(R.layout.missed_event_item, null);
                            ImageView icon = (ImageView) mMissedCallView
                                    .findViewById(R.id.icon);
                            icon.setImageResource(R.drawable.icon_missed_call);
                       /*     mMissedCallView.setOnClickListener(new OnClickListener() {
                                @Override
                                public void onClick(View arg0) {
                                    Intent intent = new Intent(
                                            Intent.ACTION_VIEW);
                                    intent.setType("vnd.android.cursor.dir/calls");
                                    ComponentName component = intent
                                            .resolveActivity(mContext
                                                    .getPackageManager());
                                    if (component == null) {
                                        // Not in android
                                        // 4.4 kitkat, so
                                        // use old version.
                                        intent.setAction(Intent.ACTION_CALL_BUTTON);
                                        intent.setType(null);
                                    }
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                                            | Intent.FLAG_ACTIVITY_CLEAR_TASK
                                            | Intent.FLAG_ACTIVITY_TASK_ON_HOME);
                                    try {
                                       // ActivityManagerNative.getDefault().dismissKeyguardOnNextActivity();
                                        mContext.startActivity(intent);
                                    } catch (Exception e) {
                                        Log.w(TAG,
                                                "Dialer activity not found!!!");
                                    }
                                }
                            });*/
                            addView(mMissedCallView);
                            Log.i(TAG, "add missed call view");
                        }
                        mMissedCallView.setTag(callIds);
                        TextView count = (TextView) mMissedCallView.findViewById(R.id.count);
                        count.setText(String.valueOf(callIds.size()));
                    } else {
                        if (mMissedCallView != null) {
                            removeView(mMissedCallView);
                            mMissedCallView = null;
                            Log.i(TAG, "remove missed call view");
                        }
                    }
                    break;
                case QUERY_MESSAGE_COMPLETE:
                    Cursor messageCursor = (Cursor) msg.obj;
                    final ArrayList<String> address = new ArrayList<String>();
                    ArrayList<Integer> ids = new ArrayList<Integer>();
                    if (messageCursor != null) {
                        while (messageCursor.moveToNext()) {
                            int id = messageCursor.getInt(messageCursor.getColumnIndex("_id"));
                            ids.add(id);
                            address.add(messageCursor.getString(messageCursor
                                    .getColumnIndex("address")));
                        }

                        messageCursor.close();
                    }

                    int mms_id = -1;
                    messageCursor = mContext.getContentResolver().query(Uri.parse("content://mms"),
                            new String[] {
                                    "_id", "read", "sub", "date"
                            }, "read = 0", null, "date" + " DESC");
                    if (messageCursor != null) {
                        while (messageCursor.moveToNext()) {
                            int id = messageCursor.getInt(messageCursor.getColumnIndex("_id"));
                            ids.add(id);
                            if (address.size() == 0) {
                                mms_id = id;
                            }
                            Uri uriAddr = Uri.parse("content://mms/" + id + "/addr");
                            Cursor c = mContext.getContentResolver().query(uriAddr, null,
                                    "msg_id = " + id, null, null);
                            if (c != null && c.moveToFirst()) {
                                address.add(c.getString(c.getColumnIndex("address")));
                                c.close();
                            } else {
                                address.add("unknown");
                            }
                        }

                        messageCursor.close();
                    }

                    if (address.size() > 0) {
                        if (mMissedMmsView == null) {
                            mMissedMmsView = LayoutInflater.from(mContext)
                                    .inflate(R.layout.missed_event_item, null);
                            ImageView icon = (ImageView) mMissedMmsView.findViewById(R.id.icon);
                            icon.setImageResource(R.drawable.icon_new_message);
                            addView(mMissedMmsView, 0);
                            Log.i(TAG, "add missed mms view");
                        }
                        mMissedMmsView.setTag(ids);
                        final int id = mms_id;
                       /* mMissedMmsView.setOnClickListener(new OnClickListener() {
                            @Override
                            public void onClick(View arg0) {
                                final Intent intent = new Intent(Intent.ACTION_VIEW);
                                intent.setType("vnd.android-dir/mms-sms");
                                ComponentName component = intent.resolveActivity(mContext
                                        .getPackageManager());
                                if (component == null) {
                                    intent.setType("vnd.android.cursor.dir/gtalk-messages");
                                    component = intent.resolveActivity(mContext
                                            .getPackageManager());
                                }
                                // Component maybe still null, but we ignore it
                                // anyway.
                                intent.setComponent(component);
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                try {
                                    intent.putExtra(
                                            "thread_id",
                                            Long.parseLong(getThreadId(
                                                    address.get(0), id)));
                                    try {
                                        //ActivityManagerNative.getDefault().dismissKeyguardOnNextActivity();
                                        mContext.startActivity(intent);
                                    } catch (Exception e) {

                                    }
                                } catch (Exception e) {
                                    Log.e("debug", Log.getStackTraceString(e));
                                }
                            }
                        });*/
                        TextView count = (TextView) mMissedMmsView.findViewById(R.id.count);
                        count.setText(String.valueOf(address.size()));
                    } else {
                        if (mMissedMmsView != null) {
                            removeView(mMissedMmsView);
                            mMissedMmsView = null;
                        }
                    }
                    break;
                default:
                    break;
            }
        };
    };

    private ContentObserver mMissedCallObserver = new ContentObserver(mHandler) {
        public void onChange(boolean selfChange) {
            try {
                updateMissedCall();
            } catch (Exception e) {

            }
        };
    };

    private ContentObserver mMissedMessageObserver = new ContentObserver(mHandler) {
        public void onChange(boolean selfChange) {
            try {
                updateMissedMessage();
            } catch (Exception e) {

            }
        };
    };

    public KeyguardMissedEventView(Context context) {
        this(context, null);
    }

    public KeyguardMissedEventView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public KeyguardMissedEventView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mContext = context;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        try {
            updateMissedMessage();
            updateMissedCall();
        } catch (Exception e) {

        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        mContext.getContentResolver().registerContentObserver(
                CallLog.Calls.CONTENT_URI, false, mMissedCallObserver);
        mContext.getContentResolver().registerContentObserver(
                Uri.parse("content://mms-sms/"), true, mMissedMessageObserver);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        mContext.getContentResolver().unregisterContentObserver(
                mMissedCallObserver);
        mContext.getContentResolver().unregisterContentObserver(
                mMissedMessageObserver);
    }

    private void sendQueryRequest(int what) {
        Log.i(TAG, "sendQueryRequest: what=" + what);
        if (sLooper == null) {
            HandlerThread thread = new HandlerThread("QueryThread");
            thread.start();

            sLooper = thread.getLooper();
        }
        if (mQueryHandler == null) {
            mQueryHandler = new QueryHandler(sLooper);
        }
        mQueryHandler.sendEmptyMessage(what);
    }

    private void updateMissedCall() {
        sendQueryRequest(QUERY_CALL_REQUEST);
    }

    private void updateMissedMessage() {
        sendQueryRequest(QUERY_MESSAGE_REQUEST);
    }

    private String getThreadId(String number, int mms_id) {
        // return null;
        // SmsMessage.
        Cursor cursor = mContext.getContentResolver()
                .query(Uri.parse("content://sms"),
                        new String[] {
                                "thread_id", "_id"
                        },
                        "address = '" + number + "'", null,
                        "date" + " DESC");
        String thread_id = null;
        if (cursor != null && cursor.moveToFirst()) {
            thread_id = cursor.getString(cursor.getColumnIndex("thread_id"));
            cursor.close();
        }

        if (thread_id == null && mms_id != -1) {
            cursor = mContext.getContentResolver()
                    .query(Uri.parse("content://mms"),
                            new String[] {
                                "thread_id"
                            },
                            "_id = " + mms_id, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                thread_id = cursor.getString(cursor.getColumnIndex("thread_id"));
                cursor.close();
            }
            // Uri uriAddr = Uri.parse("content://mms/" + id + "/addr");
            // Cursor c = mContext.getContentResolver().query(uriAddr, null,
            // "msg_id = " + id, null, null);
            // if (c != null && c.moveToFirst()) {
            // address.add(c.getString(c.getColumnIndex("address")));
            // c.close();
            // }
        }
        return thread_id != null ? thread_id : number;
    }
}
