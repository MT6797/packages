package com.nb.mmitest;

import android.annotation.Nullable;
import android.app.Activity;
import android.app.Fragment;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.Handler;
import android.util.Log;
import android.content.*;

public class FingerprintEnrollSidecar {
	private int mEnrollmentSteps = -1;
	private int mEnrollmentRemaining = 0;
	private Listener mListener;
	private boolean mEnrolling;
	private CancellationSignal mEnrollmentCancel;
	private Handler mHandler = new Handler();
	private byte[] mToken = new byte[69];
	private boolean mDone;
	private Context mContext;

	FingerprintEnrollSidecar(Context context) {
		// mToken = activity.getIntent().getByteArrayExtra(
		// ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE_TOKEN);

		mContext = context;

	}

	public static byte[] hexStringToByte(String hex) {
		int len = (hex.length() / 2);
		byte[] result = new byte[len];
		char[] achar = hex.toCharArray();
		for (int i = 0; i < len; i++) {
			int pos = i * 2;
			result[i] = (byte) (toByte(achar[pos]) << 4 | toByte(achar[pos + 1]));
		}
		return result;
	}

	private static int toByte(char c) {
		byte b = (byte) "0123456789ABCDEF".indexOf(c);
		return b;
	}

	public void onStart() {
		byte[] challenge = startPreEnroll();
		Log.d("bll", "challenge: " + bytesToHexString(challenge));

		String str = "00C6237B3267458B6B014CEF79FDFBD464000000000000000000000001000000000007065D0000000000000000000000000000000000000000000000000000000000000000";
		mToken = hexStringToByte(str);
		for (int i = 1; i < 9; i++)
			mToken[i] = challenge[i - 1];

		if (!mEnrolling) {
			startEnrollment();
		}
		Log.d("bll", "111 Token: " + bytesToHexString(mToken));
	}

	public static final String bytesToHexString(byte[] bArray) {
		StringBuffer sb = new StringBuffer(bArray.length);
		String sTemp;
		for (int i = 0; i < bArray.length; i++) {
			sTemp = Integer.toHexString(0xFF & bArray[i]);
			if (sTemp.length() < 2)
				sb.append(0);
			sb.append(sTemp.toUpperCase());
		}
		return sb.toString();
	}

	public void onStop() {

		cancelEnrollment();

	}

	private byte[] startPreEnroll() {
		long challenge = mContext.getSystemService(FingerprintManager.class)
				.preEnroll();
		return getBytes(challenge);
	}

	public static byte[] getBytes(long data) {
		byte[] bytes = new byte[8];
		bytes[0] = (byte) (data & 0xff);
		bytes[1] = (byte) ((data >> 8) & 0xff);
		bytes[2] = (byte) ((data >> 16) & 0xff);
		bytes[3] = (byte) ((data >> 24) & 0xff);
		bytes[4] = (byte) ((data >> 32) & 0xff);
		bytes[5] = (byte) ((data >> 40) & 0xff);
		bytes[6] = (byte) ((data >> 48) & 0xff);
		bytes[7] = (byte) ((data >> 56) & 0xff);
		return bytes;
	}

	private void startEnrollment() {
		mHandler.removeCallbacks(mTimeoutRunnable);
		mEnrollmentSteps = -1;
		mEnrollmentCancel = new CancellationSignal();
		mContext.getSystemService(FingerprintManager.class).enroll(mToken,
				mEnrollmentCancel, 0 /* flags */, mEnrollmentCallback);
		mEnrolling = true;
	}

	private void cancelEnrollment() {
		mHandler.removeCallbacks(mTimeoutRunnable);
		if (mEnrolling) {
			mEnrollmentCancel.cancel();
			mEnrolling = false;
			mEnrollmentSteps = -1;
		}
	}

	public void setListener(Listener listener) {
		mListener = listener;
	}

	public int getEnrollmentSteps() {
		return mEnrollmentSteps;
	}

	public int getEnrollmentRemaining() {
		return mEnrollmentRemaining;
	}

	public boolean isDone() {
		return mDone;
	}

	private FingerprintManager.EnrollmentCallback mEnrollmentCallback = new FingerprintManager.EnrollmentCallback() {

		@Override
		public void onEnrollmentProgress(int remaining) {
			if (mEnrollmentSteps == -1) {
				mEnrollmentSteps = remaining;
			}
			mEnrollmentRemaining = remaining;
			mDone = remaining == 0;
			if (mListener != null) {
				mListener.onEnrollmentProgressChange(mEnrollmentSteps,
						remaining);
			}
		}

		@Override
		public void onEnrollmentHelp(int helpMsgId, CharSequence helpString) {
			if (mListener != null) {
				mListener.onEnrollmentHelp(helpString);
			}
		}

		@Override
		public void onEnrollmentError(int errMsgId, CharSequence errString) {
			if (mListener != null) {
				mListener.onEnrollmentError(errString);
			}
		}
	};

	private final Runnable mTimeoutRunnable = new Runnable() {
		@Override
		public void run() {
			cancelEnrollment();
		}
	};

	public interface Listener {
		void onEnrollmentHelp(CharSequence helpString);

		void onEnrollmentError(CharSequence errString);

		void onEnrollmentProgressChange(int steps, int remaining);
	}
}
