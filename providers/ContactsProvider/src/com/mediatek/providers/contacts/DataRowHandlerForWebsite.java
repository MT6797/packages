/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License
 */
package com.mediatek.providers.contacts;

import android.content.ContentValues;

import android.content.Context;
import android.provider.ContactsContract.CommonDataKinds.Website;

import com.android.providers.contacts.ContactsDatabaseHelper;
import com.android.providers.contacts.DataRowHandler;
import com.android.providers.contacts.SearchIndexManager.IndexBuilder;
import com.android.providers.contacts.aggregation.AbstractContactAggregator;

/**
 * Handler for note data rows.
 */
public class DataRowHandlerForWebsite extends DataRowHandler {

    /**
     * Structure function.
     * @param context context
     * @param dbHelper dbHelper
     * @param aggregator aggregator
     */
    public DataRowHandlerForWebsite(Context context, ContactsDatabaseHelper dbHelper,
            AbstractContactAggregator aggregator) {
        super(context, dbHelper, aggregator, Website.CONTENT_ITEM_TYPE);
    }

    @Override
    public boolean hasSearchableData() {
        return true;
    }

    @Override
    public boolean containsSearchableColumns(ContentValues values) {
        return values.containsKey(Website.URL);
    }

    @Override
    public void appendSearchableData(IndexBuilder builder) {
        builder.appendContentFromColumn(Website.URL);
    }
}
