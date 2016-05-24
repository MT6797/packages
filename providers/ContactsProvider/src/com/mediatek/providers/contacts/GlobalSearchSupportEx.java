/*
 * Copyright (C) 2009 The Android Open Source Project
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

package com.mediatek.providers.contacts;

import android.app.SearchManager;
import android.database.Cursor;
import android.database.MatrixCursor;

/**
 * M: Support for global search integration for Contacts.
 */
public class GlobalSearchSupportEx {
    private static final String TAG = "GlobalSearchSupportEx";
    private GlobalSearchSupportEx() {
    }

    /**
     * M: process the Cursor
     * @param c
     * @param projection
     * @param lookupKey
     * @param searchSuggestionsColumns
     * @return
     */
    public static Cursor processCursor(Cursor c, String[] projection, String lookupKey,
            String[] searchSuggestionsColumns) {
      if (c != null && c.getCount() == 1) {
            c.moveToFirst();
            int index = c.getColumnIndex(SearchManager.SUGGEST_COLUMN_SHORTCUT_ID);
            if (index >= 0) {
                String lookup = c.getString(index);
                LogUtils.d(TAG, "[handleSearchShortcutRefresh]new lookupKey:" + lookup
                        + "||It is NE old:" + (lookup != null && !lookup.equals(lookupKey)));
                if (lookup != null && !lookup.equals(lookupKey)) {
                    c.close();
                    return new MatrixCursor(
                            projection == null ? searchSuggestionsColumns
                                    : projection);
                }
            }
            c.moveToPosition(-1);
        }
        return c;
    }
}