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
package com.mediatek.dialer.util;

import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;


import java.util.ArrayList;

/**
 * M: [Dialer Global Search] Highlights the text in a text field.
 */
public class CallLogHighlighter {

    private final int mHighlightColor;

    private ForegroundColorSpan mColorSpan;

    public CallLogHighlighter(int highlightColor) {
        mHighlightColor = highlightColor;
    }

    /**
     * Returns a CharSequence which highlights the given prefix if found in the given text.
     *
     * @param text the text to which to apply the highlight
     * @param prefix the prefix to look for
     */
    public CharSequence applyName(CharSequence text, char[] textForHighlight) {
        if (null == textForHighlight) {
            return text;
        }
        return applyNameText(text, textForHighlight);
    }

    /**
     * Returns a CharSequence which highlights the given prefix if found in the given text.
     *
     * @param text the text to which to apply the highlight
     * @param prefix the prefix to look for
     */
    public CharSequence applyNumber(CharSequence text, char[] textForHighlight) {
        if (null == textForHighlight) {
            return text;
        }
        if (String.valueOf(text).contains("@")) {
            return applyInternationalCallText(text, textForHighlight);
        } else {
            return applyNumberText(text, textForHighlight);
        }
    }

    private CharSequence applyNumberText(CharSequence text, char[] textForHighlight) {
        ArrayList<Integer> ignore = new ArrayList<Integer>();
        if (null == textForHighlight) {
            return text;
        }
        char[] handledText = lettersAndDigitsOnly(textForHighlight);
        int index = CallLogSearchUtils.indexOfWordForLetterOrDigit(text, handledText, ignore);

        if (index != -1) {
            SpannableString stringBuilder = new SpannableString(text);
            for (int i = 0; i <= ignore.size(); ++ i) {
                int start = (0 == i) ? index : (ignore.get(i - 1) + 1);
                int end = (i == ignore.size()) ? (index + handledText.length + ignore.size())
                        : ignore.get(i);
                if (start <= end) {
                    stringBuilder.setSpan(new ForegroundColorSpan(mHighlightColor), start, end, 0);
                }
            }
            return stringBuilder;
        } else {
            return text;
        }
    }

    private CharSequence applyNameText(CharSequence text, char[] textForHighlight) {
        int index = CallLogSearchUtils.indexOfWordForLetterOrDigit(text, textForHighlight);
        if (index != -1) {
            if (mColorSpan == null) {
                mColorSpan = new ForegroundColorSpan(mHighlightColor);
            }

            SpannableString result = new SpannableString(text);
            result.setSpan(mColorSpan, index, index + textForHighlight.length, 0 /* flags */);
            return result;
        } else {
            return text;
        }
    }

    private CharSequence applyInternationalCallText(CharSequence text, char[] textForHighlight) {
        int index = CallLogSearchUtils.indexOfWordForInternationalCall(text, textForHighlight);
        if (index != -1) {
            if (mColorSpan == null) {
                mColorSpan = new ForegroundColorSpan(mHighlightColor);
            }

            SpannableString result = new SpannableString(text);
            result.setSpan(mColorSpan, index, index + textForHighlight.length, 0 /* flags */);
            return result;
        } else {
            return text;
        }
    }

    private static char[] lettersAndDigitsOnly(char[] lettersOriginal) {
        char[] letters = lettersOriginal.clone();
        int length = 0;
        for (int i = 0; i < letters.length; i++) {
            final char c = letters[i];
            if (Character.isLetterOrDigit(c)) {
                letters[length++] = c;
            }
        }

        if (length != letters.length) {
            return new String(letters, 0, length).toCharArray();
        }

        return letters;
    }
}
