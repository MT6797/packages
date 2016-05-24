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

package com.android.dialer.calllog;

import android.content.ContentValues;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.CallLog.Calls;
import android.provider.ContactsContract;
import android.telecom.PhoneAccount;
import android.telecom.PhoneAccountHandle;

import com.android.contacts.common.model.Contact;
import com.android.contacts.common.model.ContactLoader;
import com.android.contacts.common.util.Constants;
import com.android.dialer.CallDetailActivity;
import com.android.dialer.DialtactsActivity;
import com.android.dialer.PhoneCallDetails;
import com.android.dialer.util.IntentUtil;
import com.android.dialer.util.TelecomUtil;

import com.mediatek.telecom.TelecomManagerEx;

import java.util.ArrayList;

/**
 * Used to create an intent to attach to an action in the call log.
 * <p>
 * The intent is constructed lazily with the given information.
 */
public abstract class IntentProvider {

    private static final String TAG = IntentProvider.class.getSimpleName();

    public abstract Intent getIntent(Context context);

    public static IntentProvider getReturnCallIntentProvider(final String number) {
        return getReturnCallIntentProvider(number, null);
    }

    public static IntentProvider getReturnCallIntentProvider(final String number,
            final PhoneAccountHandle accountHandle) {
        return new IntentProvider() {
            @Override
            public Intent getIntent(Context context) {
                return IntentUtil.getCallIntent(number, accountHandle);
            }
        };
    }

    public static IntentProvider getReturnVideoCallIntentProvider(final String number) {
        return getReturnVideoCallIntentProvider(number, null);
    }

    public static IntentProvider getReturnVideoCallIntentProvider(final String number,
            final PhoneAccountHandle accountHandle) {
        return new IntentProvider() {
            @Override
            public Intent getIntent(Context context) {
                return IntentUtil.getVideoCallIntent(number, accountHandle);
            }
        };
    }

    public static IntentProvider getReturnVoicemailCallIntentProvider() {
        return new IntentProvider() {
            @Override
            public Intent getIntent(Context context) {
                return IntentUtil.getVoicemailIntent();
            }
        };
    }

    public static IntentProvider getSendSmsIntentProvider(final String number) {
        return new IntentProvider() {
            @Override
            public Intent getIntent(Context context) {
                return IntentUtil.getSendSmsIntent(number);
            }
        };
    }

    /**
     * Retrieves the call details intent provider for an entry in the call log.
     *
     * @param id The call ID of the first call in the call group.
     * @param extraIds The call ID of the other calls grouped together with the call.
     * @param voicemailUri If call log entry is for a voicemail, the voicemail URI.
     * @return The call details intent provider.
     */
    public static IntentProvider getCallDetailIntentProvider(
            final long id, final long[] extraIds, final String voicemailUri) {
        return getCallDetailIntentProvider(id, extraIds, voicemailUri, false);
    }

    /**
     * M: [VoLTE ConfCallLog] For volte Conference callLog
     * Retrieves the call details intent provider for an entry in the call log.
     *
     * @param id The call ID of the first call in the call group.
     * @param extraIds The call ID of the other calls grouped together with the call.
     * @param voicemailUri If call log entry is for a voicemail, the voicemail URI.
     * @param isConferenceCall if it was conference call
     * @return The call details intent provider.
     */
    public static IntentProvider getCallDetailIntentProvider(
            final long id, final long[] extraIds, final String voicemailUri,
            final boolean isConferenceCall) {
        return new IntentProvider() {
            @Override
            public Intent getIntent(Context context) {
                Intent intent = new Intent(context, CallDetailActivity.class);
                // Check if the first item is a voicemail.
                if (voicemailUri != null) {
                    intent.putExtra(CallDetailActivity.EXTRA_VOICEMAIL_URI,
                            Uri.parse(voicemailUri));
                }

                if (extraIds != null && extraIds.length > 0) {
                    intent.putExtra(CallDetailActivity.EXTRA_CALL_LOG_IDS, extraIds);
                } else {
                    // If there is a single item, use the direct URI for it.
                    intent.setData(ContentUris.withAppendedId(TelecomUtil.getCallLogUri(context),
                            id));
                }
                ///M: [VoLTE ConfCallLog] For volte Conference callLog
                intent.putExtra(CallDetailActivity.EXTRA_IS_CONFERENCE_CALL, isConferenceCall);
                return intent;
            }
        };
    }

    /**
     * Retrieves an add contact intent for the given contact and phone call details.
     */
    public static IntentProvider getAddContactIntentProvider(
            final Uri lookupUri,
            final CharSequence name,
            final CharSequence number,
            final int numberType,
            final boolean isNewContact) {
        return new IntentProvider() {
            @Override
            public Intent getIntent(Context context) {
                Contact contactToSave = null;

                if (lookupUri != null) {
                    contactToSave = ContactLoader.parseEncodedContactEntity(lookupUri);
                }

                if (contactToSave != null) {
                    // Populate the intent with contact information stored in the lookup URI.
                    // Note: This code mirrors code in Contacts/QuickContactsActivity.
                    final Intent intent;
                    if (isNewContact) {
                        intent = IntentUtil.getNewContactIntent();
                    } else {
                        intent = IntentUtil.getAddToExistingContactIntent();
                    }

                    ArrayList<ContentValues> values = contactToSave.getContentValues();
                    // Only pre-fill the name field if the provided display name is an nickname
                    // or better (e.g. structured name, nickname)
                    if (contactToSave.getDisplayNameSource()
                            >= ContactsContract.DisplayNameSources.NICKNAME) {
                        intent.putExtra(ContactsContract.Intents.Insert.NAME,
                                contactToSave.getDisplayName());
                    } else if (contactToSave.getDisplayNameSource()
                            == ContactsContract.DisplayNameSources.ORGANIZATION) {
                        // This is probably an organization. Instead of copying the organization
                        // name into a name entry, copy it into the organization entry. This
                        // way we will still consider the contact an organization.
                        final ContentValues organization = new ContentValues();
                        organization.put(ContactsContract.CommonDataKinds.Organization.COMPANY,
                                contactToSave.getDisplayName());
                        organization.put(ContactsContract.Data.MIMETYPE,
                                ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE);
                        values.add(organization);
                    }

                    // Last time used and times used are aggregated values from the usage stat
                    // table. They need to be removed from data values so the SQL table can insert
                    // properly
                    for (ContentValues value : values) {
                        value.remove(ContactsContract.Data.LAST_TIME_USED);
                        value.remove(ContactsContract.Data.TIMES_USED);
                    }

                    intent.putExtra(ContactsContract.Intents.Insert.DATA, values);

                    return intent;
                } else {
                    // If no lookup uri is provided, rely on the available phone number and name.
                    if (isNewContact) {
                        return IntentUtil.getNewContactIntent(name, number, numberType);
                    } else {
                        return IntentUtil.getAddToExistingContactIntent(name, number, numberType);
                    }
                }
            }
        };
    }

    /** M: [IP Dial] get Call Intent for IP dial @{ */
    public static IntentProvider getIpDialCallIntentProvider(final String number) {
        return new IntentProvider() {
            @Override
            public Intent getIntent(Context context) {
                return IntentUtil.getCallIntent(IntentUtil.getCallUri(number), null,
                        Constants.DIAL_NUMBER_INTENT_IP);
            }
        };
    }
    /** @} */

    /**
     * M: [Suggested Account] Return an Intent for making a suggested phone call.
     * Scheme (e.g. tel, sip) will be determined automatically.
     */
    public static IntentProvider getSuggestedReturnCallIntentProvider(
            final String number, final PhoneAccountHandle accountHandle) {
        return new IntentProvider() {
            @Override
            public Intent getIntent(Context context) {
                Intent intent = IntentUtil.getCallIntent(number);
                if (accountHandle != null) {
                    intent.putExtra(
                            TelecomManagerEx.EXTRA_SUGGESTED_PHONE_ACCOUNT_HANDLE,
                            accountHandle);
                }
                return intent;
            }
        };
    }

    /**
     * M: [Suggested Account] Return an Intent for making a suggested video phone
     * call. Scheme (e.g. tel, sip) will be determined automatically.
     */
    public static IntentProvider getSuggestedReturnVideoCallIntentProvider(
            final String number, final PhoneAccountHandle accountHandle) {
        return new IntentProvider() {
            @Override
            public Intent getIntent(Context context) {
                Intent intent = IntentUtil.getVideoCallIntent(number, null,
                        null);
                if (accountHandle != null) {
                    intent.putExtra(
                            TelecomManagerEx.EXTRA_SUGGESTED_PHONE_ACCOUNT_HANDLE,
                            accountHandle);
                }
                return intent;
            }
        };
    }

    /**
     * M: [Suggested Account] Return an Intent for making a suggested IP phone
     * call. Scheme (e.g. tel, sip) will be determined automatically.
     */
    public static IntentProvider getSuggestedIpDialCallIntentProvider(
            final String number, final PhoneAccountHandle accountHandle) {
        return new IntentProvider() {
            @Override
            public Intent getIntent(Context context) {
                Intent intent = IntentUtil.getCallIntent(
                        IntentUtil.getCallUri(number), null,
                        Constants.DIAL_NUMBER_INTENT_IP);
                if (accountHandle != null) {
                    intent.putExtra(
                            TelecomManagerEx.EXTRA_SUGGESTED_PHONE_ACCOUNT_HANDLE,
                            accountHandle);
                }
                return intent;
            }
        };
    }

    /**
     * M: [VoLTE ConfCall] For Volte Conference Call.
     * Get the IntentProvider which would return the volte conference call intent
     * @param numbers the volte conference call numbers
     * @return the IntentProvider
     */
    public static IntentProvider getReturnVolteConfCallIntentProvider(
            final ArrayList<String> numbers) {
        return new IntentProvider() {
            @Override
            public Intent getIntent(Context context) {
                Intent confCallIntent = IntentUtil.getCallIntent(numbers.get(0), null, null);
                confCallIntent.putExtra(TelecomManagerEx.EXTRA_VOLTE_CONF_CALL_DIAL, true);
                confCallIntent.putStringArrayListExtra(
                        TelecomManagerEx.EXTRA_VOLTE_CONF_CALL_NUMBERS, numbers);
                return confCallIntent;
            }
        };
    }
    /** @} */

    /**
     * M: [IMS Call] For Volte IMS Call. @{
     * Get the IntentProvider which would return the volte IMS call intent
     * @param number the volte IMS call number
     * @return the IntentProvider
     */
    public static IntentProvider getReturnIMSCallIntentProvider(
            final String number) {
        return new IntentProvider() {
            @Override
            public Intent getIntent(Context context) {
                Intent confCallIntent = IntentUtil.getCallIntent(
                        Uri.fromParts(PhoneAccount.SCHEME_TEL, number, null),
                        null, Constants.DIAL_NUMBER_INTENT_IMS);
                return confCallIntent;
            }
        };
    }
    /** @} */
}
