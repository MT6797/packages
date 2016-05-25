/******************************************************************************
 * @file    QcNvItems.java
 * @brief   Implementation of the IQcNvItems interface functions used to get/set
 *          various NV parameters
 *
 * ---------------------------------------------------------------------------
 *  Copyright (C) 2009 QUALCOMM Incorporated.
 *  All Rights Reserved. QUALCOMM Proprietary and Confidential.
 * ---------------------------------------------------------------------------
 *
 *******************************************************************************/

package com.nb.mmitest;

import android.util.Log;

import java.io.IOException;

public class FactoryInfo {
	public int nvi_length = 0;
	public byte[] data;
	public boolean isSync = false;
	private int[] mNvItemsIds;

	static QcNvItemsWXKJ nvio;

	private static String TAG = "FactoryInfo";

	private FactoryInfo(int size) {
		data = new byte[size];
		isSync = false;

		if (nvio == null) {
			nvio = new QcNvItemsWXKJ();
		}

	}

	FactoryInfo(int size, int[] ids) {

		this(size);
		nvi_length = size / ids.length;
		mNvItemsIds = ids;

	}

	FactoryInfo(int size, int id) {

		this(size);
		nvi_length = size;
		mNvItemsIds = new int[] { id };
	}

	public synchronized byte[] read() {

		if (!isSync) {

			int offset = 0;
			byte[] temp = new byte[nvi_length];

			for (int i = 0; i < mNvItemsIds.length; i++) {
				try {
					temp = nvio.doNvRead(mNvItemsIds[i]);
					System.arraycopy(temp, 0, data, offset, nvi_length);
					offset += nvi_length;
				} catch (IOException e) {
					Log.e(TAG, "Unable to read item " + mNvItemsIds[i]
							+ " Value from NV ");
				}
			}

			isSync = true;
		}

		return data;
	}

	public synchronized void write(byte[] newdata, int pos) {

		isSync = false;

		try {
			System.arraycopy(newdata, 0, data, pos, newdata.length);
		} catch (Exception e) {
			Log.e(TAG, "can't copy " + newdata.length + "bytes to data[" + pos
					+ "--" + data.length + "]");
		}
		int offset = 0;
		byte[] temp = new byte[nvi_length];

		for (int i = 0; i < mNvItemsIds.length; i++) {
			try {
				System.arraycopy(data, offset, temp, 0, nvi_length);
				nvio.doNvWrite(mNvItemsIds[i], temp);
				offset += nvi_length;
			} catch (IOException e) {
				Log.e(TAG, "Unable to write item " + mNvItemsIds[i]
						+ " Value from NV ");
			}
		}

		isSync = true;

	}

}
