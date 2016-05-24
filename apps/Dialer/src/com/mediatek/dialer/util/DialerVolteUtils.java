package com.mediatek.dialer.util;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.AsyncTask;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.Data;
import android.telecom.PhoneAccount;
import android.telecom.TelecomManager;
import android.util.Log;

import com.android.dialer.DialtactsActivity;
import com.android.dialer.util.DialerUtils;
import com.android.dialer.util.IntentUtil;
import com.mediatek.contacts.util.ContactsIntent;
import com.mediatek.telecom.TelecomManagerEx;

import java.util.ArrayList;
import java.util.List;

/**
 * M: [VoLTE ConfCall] A util class for supporting the VOLTE features
 */
public class DialerVolteUtils {
    private static final String TAG = "VolteUtils";

    public static final int ACTIVITY_REQUEST_CODE_PICK_PHONE_CONTACTS = 101;

    /**
     * [VoLTE ConfCall] Launch the contacts choice activity to pick participants.
     */
    public static void handleMenuVolteConfCall(Activity activity) {
        Intent intent = new Intent();
        intent.setAction(ContactsIntent.LIST.ACTION_PICK_MULTI_PHONEANDIMSANDSIPCONTACTS);
        intent.setType(Phone.CONTENT_TYPE);
        intent.putExtra(ContactsIntent.CONFERENCE_CALL_LIMIT_NUMBER,
                ContactsIntent.CONFERENCE_CALL_LIMITES);
        DialerUtils.startActivityForResultWithErrorToast(activity, intent,
                ACTIVITY_REQUEST_CODE_PICK_PHONE_CONTACTS);
    }

    /**
     * [VoLTE ConfCall] Launch volte conference call according the picked contacts.
     */
    public static void launchVolteConfCall(Activity activity, Intent data) {
        final long[] dataIds = data.getLongArrayExtra(
                ContactsIntent.CONFERENCE_CALL_RESULT_INTENT_EXTRANAME);

        if (dataIds == null || dataIds.length <= 0) {
            Log.d(TAG, "Volte conf call, the selected contacts is empty");
            return;
        }
        new LaunchVolteConfCallTask(activity).execute(dataIds);
    }

    private static class LaunchVolteConfCallTask extends
            AsyncTask<long[], Void, ArrayList<String>> {

        Activity mActivity;
        LaunchVolteConfCallTask(Activity activity) {
            mActivity = activity;
        }
        @Override
        protected ArrayList<String> doInBackground(long[]... arg0) {
            return getPhoneNumberByDataIds(mActivity, arg0[0]);
        }

        @Override
        protected void onPostExecute(ArrayList<String> result) {
            if (mActivity.isFinishing()) {
                Log.d(TAG, "Volte conf call, Activity has finished");
                return;
            }
            if (result.size() <= 0) {
                Log.d(TAG, "Volte conf call, No phone numbers");
                return;
            }
            Intent confCallIntent = IntentUtil.getCallIntent(result.get(0),
                    mActivity instanceof DialtactsActivity ?
                    ((DialtactsActivity) mActivity).getCallOrigin() : null);
            confCallIntent.putExtra(TelecomManagerEx.EXTRA_VOLTE_CONF_CALL_DIAL, true);
            confCallIntent.putStringArrayListExtra(
                    TelecomManagerEx.EXTRA_VOLTE_CONF_CALL_NUMBERS, result);
            DialerUtils.startActivityWithErrorToast(mActivity, confCallIntent);
        }
    }

    private static ArrayList<String> getPhoneNumberByDataIds(
            Context context, long[] dataIds) {
        ArrayList<String> phoneNumbers = new ArrayList<String>();
        if (dataIds == null || dataIds.length <= 0) {
            return phoneNumbers;
        }
        StringBuilder selection = new StringBuilder();
        selection.append(Data._ID);
        selection.append(" IN (");
        selection.append(dataIds[0]);
        for (int i = 1; i < dataIds.length; i++) {
            selection.append(",");
            selection.append(dataIds[i]);
        }
        selection.append(")");
        Log.d(TAG, "getPhoneNumberByDataIds dataIds " + selection.toString());
        Cursor c = null;
        try {
            c = context.getContentResolver().query(Data.CONTENT_URI,
                    new String[]{Data._ID, Data.DATA1},
                    selection.toString(), null, null);
            if (c == null) {
                return phoneNumbers;
            }
            while (c.moveToNext()) {
                Log.d(TAG, "getPhoneNumberByDataIds got"
                        + " _ID=" + c.getInt(0)
                        + ", NUMBER=" + c.getInt(1));
                phoneNumbers.add(c.getString(1));
            }
        } finally {
            if (c != null) {
                c.close();
            }
        }
        return phoneNumbers;
    }

    /**
     * Returns whether the VoLTE conference call enabled.
     * @param context the context
     * @return true if the VOLTE is supported and has Volte phone account
     */
    public static boolean isVolteConfCallEnable(Context context) {
        if (!DialerFeatureOptions.isVolteEnhancedConfCallSupport() || context == null) {
            return false;
        }
        final TelecomManager telecomManager = (TelecomManager) context
                .getSystemService(Context.TELECOM_SERVICE);
        List<PhoneAccount> phoneAccouts = telecomManager.getAllPhoneAccounts();
        for (PhoneAccount phoneAccount : phoneAccouts) {
            if (phoneAccount.hasCapabilities(
                    PhoneAccount.CAPABILITY_VOLTE_CONFERENCE_ENHANCED)) {
                return true;
            }
        }
        return false;
    }
}
