/*
 * Copyright (C) 2011-2014 MediaTek Inc.
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

package com.mediatek.dialer.calllog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.text.TextUtils;
import android.util.Log;

import com.android.dialer.R;
import com.android.dialer.calllog.CallLogActivity;
import com.mediatek.dialer.calllog.PhoneAccountInfoHelper.AccountInfoListener;

import java.util.ArrayList;
import java.util.List;

/** M: [Call Log Account Filter]
 * Dialog use to display and pick a PhoneAccount.
 * To receive the pick result, need to implement {@link #PhoneAccountPickListener}.
 */
public class PhoneAccountPickerDialog extends DialogFragment implements AccountInfoListener {
    public final static String TAG = "PhoneAccountPickerDialog";

    private final static String SELECTED_ACCOUNT_INDEX_KEY = "selectedAccoutIndexKey";
    private int mCurrentSelectedAccountIndex = -1;
    private PhoneAccountPickerAdapter mAdapter;

    public PhoneAccountPickerDialog() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        List<AccountItem> data = createPhoneAccountItems();
        // if the phone accounts count is less than 2(a phone account and a dummy all account)
        // dismiss fragment.
        if (data.size() <= 2) {
            dismiss();
        }
        PhoneAccountInfoHelper.getInstance(getActivity()).registerForAccountChange(this);
        mAdapter = new PhoneAccountPickerAdapter(getActivity());
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        List<AccountItem> data = createPhoneAccountItems();

        if (savedInstanceState != null) {
            mCurrentSelectedAccountIndex = savedInstanceState
                    .getInt(SELECTED_ACCOUNT_INDEX_KEY, -1);
        }
        if (mCurrentSelectedAccountIndex < 0) {
            mCurrentSelectedAccountIndex = getPreferedAccountItemIndex(data);
        }
        mAdapter.setItemData(data);
        mAdapter.setSelection(mCurrentSelectedAccountIndex);
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.select_account);
        builder.setSingleChoiceItems(mAdapter, mCurrentSelectedAccountIndex, new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mCurrentSelectedAccountIndex = which;
                mAdapter.setSelection(which);
                mAdapter.notifyDataSetChanged();
            }
        });

        builder.setNegativeButton(android.R.string.cancel, null);

        // only need "ok" button when show selection
        builder.setPositiveButton(android.R.string.ok, new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                final String selectedId = mAdapter.getSelectedAccountId();
                if (!TextUtils.isEmpty(selectedId)) {
                    PhoneAccountInfoHelper.getInstance(getActivity())
                            .setPreferAccountId(selectedId);
                }
                dialog.dismiss();
            }
        });
        return builder.create();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(SELECTED_ACCOUNT_INDEX_KEY, mCurrentSelectedAccountIndex);
    }

    @Override
    public void onDestroy() {
        PhoneAccountInfoHelper.getInstance(getActivity()).unRegisterForAccountChange(this);
        super.onDestroy();
    }

    /**
     * Data structure to encapsulation account info.
     */
    public static class AccountItem {
        public final PhoneAccountHandle accountHandle;
        public final int title;
        public final int type;
        public final String id;

        /**
         * create a phoneAccount type data Item it will be displayed as
         * icon/name/number
         */
        public AccountItem(PhoneAccountHandle accountHandle) {
            this.type = PhoneAccountPickerAdapter.ITEM_TYPE_ACCOUNT;
            this.accountHandle = accountHandle;
            this.id = accountHandle.getId();
            this.title = 0;
        }

        /**
         * create a text type data item it will be displayed as textview
         */
        public AccountItem(int title, String id) {
            this.type = PhoneAccountPickerAdapter.ITEM_TYPE_TEXT;
            this.title = title;
            this.id = id;
            this.accountHandle = null;
        }
    }

    @Override
    public void onAccountInfoUpdate() {
        List<AccountItem> data = createPhoneAccountItems();
        // if the phone accounts count is less than 2(a phone account and a dummy all account)
        // dismiss the picker dialog.
        if (data.size() <= 2 && getDialog() != null) {
            getDialog().dismiss();
            return;
        }
        final int selectIndex = getPreferedAccountItemIndex(data);
        mAdapter.setItemData(data);
        mAdapter.setSelection(selectIndex);
    }

    @Override
    public void onPreferAccountChanged(String id) {
        // Do noting to satisfy AccountInfoListener
    }

    /// M: [Call Log Account Filter] @{
    private int getPreferedAccountItemIndex(List<AccountItem> data) {
        String id = PhoneAccountInfoHelper.getInstance(getActivity()).getPreferAccountId();
        if (!TextUtils.isEmpty(id) && data != null) {
            for (int i = 0; i < data.size(); i++) {
                if (id.equals(data.get(i).id)) {
                    return i;
                }
            }
        }
        return 0;
    }

    private List<AccountItem> createPhoneAccountItems() {
        List<AccountItem> accountItems = new ArrayList<AccountItem>();
        // fist item is "all accounts"
        accountItems.add(new AccountItem(R.string.all_accounts,
                PhoneAccountInfoHelper.FILTER_ALL_ACCOUNT_ID));

        final TelecomManager telecomManager = (TelecomManager) getActivity()
                .getSystemService(Context.TELECOM_SERVICE);
        final List<PhoneAccountHandle> accounts = telecomManager.getCallCapablePhoneAccounts();
        for (PhoneAccountHandle account : accounts) {
            accountItems.add(new AccountItem(account));
        }
        return accountItems;
    }
    /// @}
}
/** @} */