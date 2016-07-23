package ma.release;

import android.util.Log;

public class Util {

	public static void dprint(final String TAG, String msg) {
		Log.d(TAG, msg);
	}

	public static void sleep(int time) {
		try {
			Thread.sleep(time);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

}
