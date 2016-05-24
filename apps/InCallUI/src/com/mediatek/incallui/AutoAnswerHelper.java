package com.mediatek.incallui;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;

import com.android.incallui.Call;
import com.android.incallui.InCallPresenter;
import com.android.incallui.Log;

/**
 * M: for EngineerMode testing.
 */
public class AutoAnswerHelper implements InCallPresenter.IncomingCallListener {

    private static final long AUTO_ANSWER_DELAY_MILLIS = 5 * 1000;
    private Context mContext;
    private Handler mAutoAnswerHandler = new AutoAnswerHandler();

    /**
     * Constructor.
     * @param context the context is required for the application context.
     */
    public AutoAnswerHelper(Context context) {
        mContext = context.getApplicationContext();
    }

    @Override
    public void onIncomingCall(InCallPresenter.InCallState oldState,
                               InCallPresenter.InCallState newState, Call call) {
        if (isEnabled()) {
            log("would answer the call in a few seconds: " + call.getId());
            Message msg = mAutoAnswerHandler.obtainMessage(0, call);
            mAutoAnswerHandler.sendMessageDelayed(msg, AUTO_ANSWER_DELAY_MILLIS);
        }
    }

    /**
     * The property persist.auto_answer_incoming_call has 2 users,
     * one is InCallUI, the other is the EngineerMode.
     * @return true if auto answer enabled.
     */
    private boolean isEnabled() {
        return SystemProperties.getInt("persist.auto_answer", -1) > 0;
    }

    /**
     * M: Handler to delay auto answer.
     */
    private class AutoAnswerHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            Call call = (Call) msg.obj;
            if (call != null && Call.State.isIncoming(call.getState())) {
                log("answer incoming call as: " + call.getVideoState());
                InCallPresenter.getInstance().answerIncomingCall(mContext, call.getVideoState());
            }
        }
    }

    private static void log(String msg) {
        Log.i(AutoAnswerHelper.class.getSimpleName(), msg);
    }
}
