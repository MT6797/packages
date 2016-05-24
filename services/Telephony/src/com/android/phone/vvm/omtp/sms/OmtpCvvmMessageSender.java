/*
 * Copyright (C) 2015 Google Inc. All Rights Reserved.
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
 * limitations under the License
 */
package com.android.phone.vvm.omtp.sms;

import android.annotation.Nullable;
import android.app.PendingIntent;
import android.telephony.SmsManager;

import com.android.phone.vvm.omtp.OmtpConstants;

/**
 * An implementation of the OmtpMessageSender for T-Mobile.
 */
public class OmtpCvvmMessageSender extends OmtpMessageSender {
    public OmtpCvvmMessageSender(SmsManager smsManager, short applicationPort,
            String destinationNumber) {
        super(smsManager, applicationPort, destinationNumber);
    }

    @Override
    public void requestVvmActivation(@Nullable PendingIntent sentIntent) {
        sendCvvmMessage(OmtpConstants.ACTIVATE_REQUEST, sentIntent);
    }

    @Override
    public void requestVvmDeactivation(@Nullable PendingIntent sentIntent) {
        sendCvvmMessage(OmtpConstants.DEACTIVATE_REQUEST, sentIntent);
    }

    @Override
    public void requestVvmStatus(@Nullable PendingIntent sentIntent) {
        sendCvvmMessage(OmtpConstants.STATUS_REQUEST, sentIntent);
    }

    private void sendCvvmMessage(String request, PendingIntent sentIntent) {
        StringBuilder sb = new StringBuilder().append(request);
        sb.append(OmtpConstants.SMS_PREFIX_SEPARATOR);
        appendField(sb, "dt", "6");
        sendSms(sb.toString(), sentIntent);
    }
}
