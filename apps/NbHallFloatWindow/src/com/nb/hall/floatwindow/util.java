package com.nb.hall.floatwindow;

import java.io.BufferedReader;
import java.io.FileReader;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.provider.ContactsContract;
import android.util.Log;

public class util {

	static String readHallState() {
		try {
			int pos = 0;
			BufferedReader reader;
			for (int i = 0; i < 10; i++) {
				reader = new BufferedReader(new FileReader("/sys/class/input/input" + i + "/name"), 256);
				if ("cover".equals(reader.readLine())) {
					pos = i;
					reader.close();
					break;
				}
				reader.close();
			}
			reader = new BufferedReader(new FileReader("/sys/class/input/input" + pos + "/status"), 256);
			try {
				String state = reader.readLine();
				Log.d("lqh", "bll====>pos:" + pos + "  hall state: " + state);
				return state;
			} finally {
				reader.close();
			}
		} catch (Exception e) {

		}
		return "1";
	}

	public static String getContactNameFromPhoneBook(Context context, String phoneNum) {
		String contactName = "";
        String phone1 = new StringBuffer(phoneNum.subSequence(0, 3)).append(" ").append(phoneNum.substring(3, 7))  
                .append(" ").append(phoneNum.substring(7, 11)).toString();  
        String phone2 = new StringBuffer(phoneNum.subSequence(0, 3)).append("-").append(phoneNum.substring(3, 7))  
                .append("-").append(phoneNum.substring(7, 11)).toString();
        ContentResolver cr =context.getContentResolver();  
        Cursor pCur = cr.query(  
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null,  
                ContactsContract.CommonDataKinds.Phone.NUMBER + " in(?,?,?)", new String[] {  
                        phoneNum, phone1, phone2 }, null);  
        if (pCur!=null && pCur.moveToFirst()) {  
            contactName = pCur  
                    .getString(pCur  
                            .getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));  
            pCur.close();  
        }  
        return contactName;  
    } 
}
