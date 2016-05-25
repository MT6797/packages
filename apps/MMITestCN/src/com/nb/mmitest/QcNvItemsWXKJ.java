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

//import android.os.AsyncResult;
import android.util.Log;
/*
 //import com.android.internal.telephony.IccUtils;
 import com.android.qualcomm.qcnvitems.*;
 import com.android.qualcomm.qcrilhook.IQcRilHook;
 import com.android.qualcomm.qcrilhook.QcRilHook;
 */
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class QcNvItemsWXKJ /* extends QcNvItems */{

	private static String LOG_TAG = "QC_NV_ITEMS_WXKJ";

	private static final int HEADER_SIZE = 8;

	// private QcRilHook mQcRilOemHook;
	private WXKJRapi mNvIo;

	private static final boolean enableVLog = true;

	private String TAG = "QcNvItemsWXKJ";

	// OEM items start at 50000
	public static final int NV_TRACABILITY_I = 50000;

	public static final int NV_TRACABILITY_1_I = 50001;

	public static final int NV_TRACABILITY_2_I = 50002;

	public static final int NV_TRACABILITY_3_I = 50003;

	public static final int NV_MMITEST_INFO_I = 50004;

	private final boolean DEBUG = false;

	public QcNvItemsWXKJ() {
		super();
		Log.i(TAG, "QcNvItemsWXKJ instance created.");

		// mQcRilOemHook = new QcRilHook();
	}

	private void LOGD(String s) {
		if (DEBUG) {
			Log.d(TAG, s);
		}
	}

	public void doNvWrite(int itemId, byte[] nvItem) throws IOException {
		LOGD(java.util.Arrays.toString(nvItem));
		/*
		 * ByteBuffer buf = ByteBuffer.allocate(HEADER_SIZE + nvItem.length);
		 * buf.order(ByteOrder.nativeOrder()); buf.putInt(itemId);
		 * buf.putInt(nvItem.length); buf.put(nvItem);
		 * 
		 * AsyncResult result =
		 * mQcRilOemHook.sendQcRilHookMsg(IQcRilHook.QCRILHOOK_NV_WRITE, buf
		 * .array());
		 * 
		 * if (result.exception != null) { Log.e(LOG_TAG,
		 * String.format("doNvWrite(item = "+itemId+") Failed : %s",
		 * result.exception.toString())); result.exception.printStackTrace();
		 * throw new IOException(); }
		 */
		WXKJRapi.doNvWrite(itemId, nvItem);
	}

	public byte[] doNvRead(int itemId) throws IOException {
		/*
		 * AsyncResult result =
		 * mQcRilOemHook.sendQcRilHookMsg(IQcRilHook.QCRILHOOK_NV_READ, itemId);
		 * if (result == null || result.exception != null) { Log.e(LOG_TAG,
		 * String.format("doNvRead() Failed : %s",
		 * result.exception.toString())); result.exception.printStackTrace();
		 * throw new IOException(); }
		 * 
		 * Log.i(TAG,"Received: " +
		 * IccUtils.bytesToHexString((byte[])result.result));
		 * 
		 * return (byte[])result.result;
		 */

		byte[] nvItem = new byte[512];
		WXKJRapi.doNvRead(itemId, nvItem);

		return nvItem;
	}

}
