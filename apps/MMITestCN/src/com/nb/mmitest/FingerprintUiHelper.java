package com.nb.mmitest;
import android.hardware.fingerprint.FingerprintManager;
import android.os.CancellationSignal;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.content.*;
import android.util.Log;
public class FingerprintUiHelper extends FingerprintManager.AuthenticationCallback {

    private static final long ERROR_TIMEOUT = 1300;

    private ImageView mIcon;
    private TextView mErrorTextView;
    private CancellationSignal mCancellationSignal;

    private Callback mCallback;
    private FingerprintManager mFingerprintManager;
    private final String TAG = "FingerprintUiHelper";

    public FingerprintUiHelper(Context context, Callback callback) {
        mFingerprintManager = context.getSystemService(FingerprintManager.class);
        mCallback = callback;
    }

    public void startListening() {
        if (mFingerprintManager.getEnrolledFingerprints().size() > 0) {
            mCancellationSignal = new CancellationSignal();
            mFingerprintManager.authenticate(null, mCancellationSignal, 0 /* flags */, this, null);
            setFingerprintIconVisibility(true);
           
        }
    }

    public void stopListening() {
        if (mCancellationSignal != null) {
            mCancellationSignal.cancel();
            mCancellationSignal = null;
        }
    }

    private boolean isListening() {
        return mCancellationSignal != null && !mCancellationSignal.isCanceled();
    }

    private void setFingerprintIconVisibility(boolean visible) {
       
        mCallback.onFingerprintIconVisibilityChanged(visible);
    }

    @Override
    public void onAuthenticationError(int errMsgId, CharSequence errString) {
        showError(errString);
        setFingerprintIconVisibility(false);
        Log.d(TAG, "onAuthenticationError");
    }

    @Override
    public void onAuthenticationHelp(int helpMsgId, CharSequence helpString) {
        showError(helpString);
        Log.d(TAG, "onAuthenticationHelp");
    }

    @Override
    public void onAuthenticationFailed() {
    	Log.d(TAG, "onAuthenticationFailed");
    }

    @Override
    public void onAuthenticationSucceeded(FingerprintManager.AuthenticationResult result) {
       // mIcon.setImageResource(R.drawable.ic_fingerprint_success);
        mCallback.onAuthenticated();
        Log.d(TAG, "onAuthenticationSucceeded");
    }

    private void showError(CharSequence error) {
        if (!isListening()) {
            return;
        }

    }

    private Runnable mResetErrorTextRunnable = new Runnable() {
        @Override
        public void run() {

        }
    };

    public interface Callback {
        void onAuthenticated();
        void onFingerprintIconVisibilityChanged(boolean visible);
    }
}
