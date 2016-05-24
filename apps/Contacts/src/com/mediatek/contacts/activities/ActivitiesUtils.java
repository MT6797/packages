/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 *
 * MediaTek Inc. (C) 2010. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER ON
 * AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL WARRANTIES,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NONINFRINGEMENT.
 * NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH RESPECT TO THE
 * SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY, INCORPORATED IN, OR
 * SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES TO LOOK ONLY TO SUCH
 * THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. RECEIVER EXPRESSLY ACKNOWLEDGES
 * THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES
 * CONTAINED IN MEDIATEK SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK
 * SOFTWARE RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND
 * CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE RELEASED HEREUNDER WILL BE,
 * AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE,
 * OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE CHARGE PAID BY RECEIVER TO
 * MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek Software")
 * have been modified by MediaTek Inc. All revisions are subject to any receiver's
 * applicable license agreements with MediaTek Inc.
 */
package com.mediatek.contacts.activities;

import android.app.Activity;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.storage.IMountService;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.provider.ContactsContract.ProviderStatus;
import android.support.v4.view.ViewPager;
import android.telephony.PhoneNumberUtils;
import android.text.TextUtils.TruncateAt;
//import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.android.contacts.ContactSaveService;
import com.android.contacts.R;
import com.android.contacts.activities.PeopleActivity;
import com.android.contacts.common.list.ContactEntryListFragment;
import com.android.contacts.common.model.AccountTypeManager;
import com.android.contacts.common.model.account.AccountWithDataSet;
import com.android.contacts.common.vcard.VCardCommonArguments;
import com.android.contacts.common.vcard.VCardService;
import com.android.contacts.list.ContactPickerFragment;
import com.android.contacts.list.ContactsUnavailableFragment;
import com.android.contacts.list.DefaultContactBrowseListFragment;
import com.android.contacts.list.ProviderStatusWatcher;
import com.android.contacts.util.PhoneCapabilityTester;

import com.mediatek.contacts.ContactsApplicationEx;
import com.mediatek.contacts.list.MultiGroupPickerFragment;
import com.mediatek.contacts.list.service.MultiChoiceService;
import com.mediatek.contacts.simcontact.SimCardUtils;
import com.mediatek.contacts.simservice.SIMEditProcessor;
import com.mediatek.contacts.util.AccountTypeUtils;
import com.mediatek.contacts.util.ContactsSettingsUtils;
import com.mediatek.contacts.util.Log;
import com.mediatek.contacts.util.MtkToast;
import com.mediatek.contacts.vcs.VcsController;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ActivitiesUtils {

    private static final String TAG = "ActivitiesUtils";

    /** For CR ALPS01541210 */
    public static boolean checkContactsProcessIsBusy(final Activity activity) {
        // Since busy return directly no receiver is registered
        boolean isProcessBusy = ContactsApplicationEx.isContactsApplicationBusy();
        Log.d(TAG, "[checkContactsProcessIsBusy]isProcessBusy = " + isProcessBusy);
        if (isProcessBusy) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    MtkToast.toast(activity.getApplicationContext(), R.string.phone_book_busy);
                }
            });
            activity.finish();
            return true;
        }
        return false;
    }

    public static boolean checkPhoneBookReady(final Activity activity,
            Bundle savedState, int subId) {
        if (subId > 0 && !SimCardUtils.isPhoneBookReady(subId)) {
            Log.w(TAG, "[checkPhoneBookReady] phone book is not ready. mSubId:" + subId);
            activity.finish();
            return true;
        }

        if ((MultiChoiceService.isProcessing(MultiChoiceService.TYPE_DELETE)
                || MultiChoiceService.isProcessing(MultiChoiceService.TYPE_COPY)
                || VCardService.isProcessing(VCardService.TYPE_IMPORT) ||
                MultiGroupPickerFragment.isMoveContactsInProcessing() //M:FixedALPS00567939
                )
                && (savedState == null)) {
            Log.d(TAG, "[checkPhoneBookReady]delete or copy is processing ");
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(activity.getApplicationContext(), R.string.phone_book_busy,
                            Toast.LENGTH_SHORT).show();
                }
            });
            activity.finish();
            return true;
        }
        return false;
    }

    public static boolean isDeleteingContact(final Activity activity) {
        if (MultiChoiceService.isProcessing(MultiChoiceService.TYPE_DELETE)
                || MultiChoiceService.isProcessing(MultiChoiceService.TYPE_COPY)
                || VCardService.isProcessing(VCardService.TYPE_IMPORT)
                || MultiGroupPickerFragment.isMoveContactsInProcessing()// M:ALPS00567939
                || ContactSaveService.isGroupTransactionProcessing()) { // / M: Fixed ALPS00542175
            Log.d(TAG, "delete or copy is processing ");
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(activity.getApplicationContext(), R.string.phone_book_busy,
                            Toast.LENGTH_SHORT).show();
                }
            });
            activity.finish();
            return true;
        }

        return false;
    }

    public static Handler initHandler(final Activity activity) {
        SIMEditProcessor.Listener l = (SIMEditProcessor.Listener) activity;
        Handler handler = new Handler() {
            public void handleMessage(Message msg) {
                String content = null;
                int contentId = msg.arg1;
                Bundle bundle = msg.getData();
                if (bundle != null) {
                    content = bundle.getString("content");
                }
                onShowToast(activity, content, contentId);
            }
        };
        SIMEditProcessor.registerListener(l, handler);

        return handler;
    }

    /** Add for SIM Service refactory */
    public static void onShowToast(Activity activity, String msg, int resId) {
        Log.d(TAG, "[onShowToast]msg: " + msg + " ,resId: " + resId);
        if (msg != null) {
            MtkToast.toast(activity, msg);
        } else if (resId != -1) {
            MtkToast.toast(activity, resId);
        }
    }

    public static void setPickerFragmentAccountType(Activity activity,
            ContactEntryListFragment<?> listFragment) {
        if (listFragment instanceof ContactPickerFragment) {
            ContactPickerFragment fragment = (ContactPickerFragment) listFragment;
            int accountTypeShow = activity.getIntent().getIntExtra(
                    ContactsSettingsUtils.ACCOUNT_TYPE, ContactsSettingsUtils.ALL_TYPE_ACCOUNT);
            Log.d(TAG, "[setPickerFragmentAccountType]accountTypeShow:" + accountTypeShow);
            fragment.setAccountType(accountTypeShow);
        }
    }

    /** New Feature */
    public static boolean checkSimNumberValid(Activity activity, String ssp) {
        if (ssp != null && !PhoneNumberUtils.isGlobalPhoneNumber(ssp)) {
            Toast.makeText(activity.getApplicationContext(), R.string.sim_invalid_number,
                    Toast.LENGTH_SHORT).show();
            activity.finish();
            return true;
        }
        return false;
    }

    public static void prepareVcsMenu(Menu menu, VcsController vcsController) {
        if (vcsController != null) {
            vcsController.onPrepareOptionsMenuVcs(menu);
        } else {
            MenuItem item = menu.findItem(com.android.contacts.R.id.menu_vcs);
            if (item != null) {
                item.setVisible(false);
            }
        }
    }

    public static boolean doImportExport(Activity activity) {
        Log.i(TAG, "[doImportExport]...");
        if (MultiChoiceService.isProcessing(MultiChoiceService.TYPE_DELETE)) {
            Toast.makeText(activity, R.string.contact_delete_all_tips, Toast.LENGTH_SHORT).show();
            return true;
        }
        final Intent intent = new Intent(activity, ContactImportExportActivity.class);

        /* add callingActivity extra for enter ContactImportExportActivity from PeopleActivity,
         * cause by Dialer can hung up ContactImportExportActivity so we should distinguish which
         * Activity start ContactImportExportActivity by using callingActivity.@{
         */
        intent.putExtra(VCardCommonArguments.ARG_CALLING_ACTIVITY,
                PeopleActivity.class.getName());
        //@}

        activity.startActivity(intent);
        return true;
    }

    public static boolean deleteContact(Activity activity) {
        Log.i(TAG, "[deleteContact]...");
        if (MultiChoiceService.isProcessing(MultiChoiceService.TYPE_DELETE)) {
            Toast.makeText(activity, R.string.contact_delete_all_tips, Toast.LENGTH_SHORT).show();
            return true;
        } else if (VCardService.isProcessing(VCardService.TYPE_IMPORT)
                || VCardService.isProcessing(VCardService.TYPE_EXPORT)) {
            Toast.makeText(activity, R.string.contact_import_export_tips, Toast.LENGTH_SHORT)
                    .show();
            return true;
        }
        activity.startActivity(new Intent().setClassName(activity.getApplicationContext(),
                "com.mediatek.contacts.list.ContactListMultiChoiceActivity").setAction(
                com.mediatek.contacts.util.ContactsIntent.LIST.ACTION_DELETE_MULTI_CONTACTS));
        return true;
    }

    public static boolean conferenceCall(Activity activity) {
        Log.i(TAG, "[conferenceCall]...");
        final Intent intent = new Intent();
        intent.setClassName(activity, "com.mediatek.contacts.list.ContactListMultiChoiceActivity")
                .setAction(
                        com.mediatek.contacts.util.
                        ContactsIntent.LIST.ACTION_PICK_MULTI_PHONEANDIMSANDSIPCONTACTS);
        intent.putExtra(com.mediatek.contacts.util.ContactsIntent.CONFERENCE_SENDER,
                com.mediatek.contacts.util.ContactsIntent.CONFERENCE_CONTACTS);
        activity.startActivity(intent);

        return true;
    }

    public static void setAllFramgmentShow(View contactsUnavailableView,
            DefaultContactBrowseListFragment allFragment, Activity activity, ViewPager tabPager,
            ContactsUnavailableFragment contactsUnavailableFragment,
            Integer providerStatus) {

        boolean isNeedShow = showContactsUnavailableView(contactsUnavailableView,
                contactsUnavailableFragment, providerStatus);

        boolean isUsingTwoPanes = PhoneCapabilityTester.isUsingTwoPanes(activity);
        Log.i(TAG, "[setAllFramgmentShow]isNeedShow = " + isNeedShow + ",isUsingTwoPanes = "
                + isUsingTwoPanes);
        if (isNeedShow) {
            if (null != allFragment) {
                // mTabPager only exists while 1-pane thus the code should be
                // modified for 2-panes
                if (isUsingTwoPanes && tabPager != null) {
                    tabPager.setVisibility(View.VISIBLE);
                }
                allFragment.setEnabled(true);
                allFragment.closeWaitCursor();
                if (!isUsingTwoPanes && tabPager != null) {
                    allFragment.setProfileHeader();
                }
            } else {
                Log.e(TAG, "[setAllFramgmentShow]mAllFragment is null");
            }
            isNeedShow = false;
        } else {
            if (!isUsingTwoPanes && tabPager != null) {
                tabPager.setVisibility(View.GONE);
            }
        }
    }

    private static boolean showContactsUnavailableView(View contactsUnavailableView,
            ContactsUnavailableFragment contactsUnavailableFragment,
            Integer providerStatus) {
        boolean mDestroyed = contactsUnavailableFragment.mDestroyed;
        boolean isNeedShow = false;
        Log.d(TAG, "[showContactsUnavailableView]mProviderStatus: " + providerStatus);
       // if (providerStatus == ProviderStatus.STATUS_NO_ACCOUNTS_NO_CONTACTS || mDestroyed) {
        //TODO: STATUS_NO_ACCOUNTS_NO_CONTACTS is 4?
        if (providerStatus == 4 || mDestroyed) {
            contactsUnavailableView.setVisibility(View.GONE);
            isNeedShow = true;
            if (mDestroyed) {
                contactsUnavailableFragment.mDestroyed = false;
            }
        } else {
            contactsUnavailableView.setVisibility(View.VISIBLE);
        }
        return isNeedShow;
    }

    private static int getAvailableStorageCount(Activity activity) {
        int storageCount = 0;
        final StorageManager storageManager = (StorageManager) activity.getApplicationContext()
                .getSystemService(activity.STORAGE_SERVICE);
        if (null == storageManager) {
            Log.w(TAG, "[getAvailableStorageCount]storageManager is null,return 0.");
            return 0;
        }
        StorageVolume[] volumes = storageManager.getVolumeList();
        for (StorageVolume volume : volumes) {
            String path = volume.getPath();
            if (!Environment.MEDIA_MOUNTED.equals(storageManager.getVolumeState(path))) {
                    continue;
            }
            storageCount++;
        }
        Log.d(TAG, "[getAvailableStorageCount]storageCount = " + storageCount);
        return storageCount;
    }

    public static boolean showImportExportMenu(Activity activity) {
        int availableStorageCount = getAvailableStorageCount(activity);
        int accountSize = AccountTypeManager
                .getInstance(activity).getAccounts(false).size();
        Log.d(TAG, "[showImportExportMenu]availableStorageCount = " + availableStorageCount
                + ",accountSize = " + accountSize);
        return !((availableStorageCount == 0) && (accountSize <= 1));
    }

    public static Integer checkSelectedDeleted(Integer result,
            final ArrayList<ContentProviderOperation> diff, ContentProviderResult[] results,
            final int failed) {
        if (results != null && results.length > 0 && !diff.isEmpty()) {
            // Version asserts failure if there is no contact item.
            if (diff.get(0).getType() == ContentProviderOperation.TYPE_ASSERT
                    && results[0].count != null && results[0].count <= 0) {
                result = failed;
                Log.e(TAG, "[checkSelectedDeleted]the selected contact has been deleted!");
            }
        }
        return result;
    }

    /** Fix CR ALPS00839693,the "Phone" should be translated into Chinese */
    public static void setAccountName(final TextView textView, final AccountWithDataSet account,
            Activity activity) {
        if (AccountTypeUtils.ACCOUNT_NAME_LOCAL_PHONE.equals(account.name)) {
            textView.setText(activity.getString(R.string.contact_editor_prompt_one_account,
                    activity.getString(R.string.account_phone_only)));
        } else {
            textView.setText(activity.getString(R.string.contact_editor_prompt_one_account,
                    account.name));
        }
        // set ellip size for extra large font size
        textView.setSingleLine();
        textView.setEllipsize(TruncateAt.END);
    }

    // / This function is to customize the account list for differenct account
    // type
    public static void customAccountsList(List<AccountWithDataSet> accountList, Activity activity) {
        if (accountList != null) {
            int type = activity.getIntent().getIntExtra(ContactsSettingsUtils.ACCOUNT_TYPE,
                    ContactsSettingsUtils.ALL_TYPE_ACCOUNT);
            switch (type) {
            case ContactsSettingsUtils.PHONE_TYPE_ACCOUNT:
                Iterator<AccountWithDataSet> iterator = accountList.iterator();
                while (iterator.hasNext()) {
                    AccountWithDataSet data = iterator.next();
                    // Only sim account is AccountWithDataSetEx, so remove any
                    // type is AccountWithDataSetEx
                    if (isSimType(data.type)) {
                        iterator.remove();
                    }
                }
                break;
            default:
                Log.i(TAG, "[customAccountsList]default all type account");
                break;
            }
        }
    }

    private static boolean isSimType(String type) {
        if (AccountTypeUtils.isAccountTypeIccCard(type)) {
            return true;
        }
        return false;
    }
}
