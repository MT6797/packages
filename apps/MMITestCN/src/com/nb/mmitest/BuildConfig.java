package com.nb.mmitest;

import android.os.SystemProperties;
//add by xianfeng.xu for CR364979 begin
import java.util.ArrayList;
import java.util.List;
import android.util.Log;
//add by xianfeng.xu for CR364979 end

public class BuildConfig {
	static boolean isSW = true;
	static String mmitest = SystemProperties.get("ro.mmitest");
	//add by xianfeng.xu for CR364979 begin
	private static String mShortCode_dual ="U0J";//dual simcard, no sdcard, no MHL
	private static String mShortCode_single ="4YJ";//EU single simcard,have sdcard and MHL
	private static String mShortCode_single_ignoremhl ="6YJ";//US single simcard,have sdcard, no MHL
	static Test[] dual_List = {
	    new MemorycardTest(Test.ID.MEMORYCARD, "SD CARD"),
	    new MHLTest(Test.ID.MHL, "MHL")
	};
	static Test[] single_ignoremhl_List = {
        new MHLTest(Test.ID.MHL, "MHL")
	};
	//add by xianfeng.xu for CR364979 end
	static boolean getMmiTest()
	{
		//modify by liliang.bao begin 
		return true;
		/*
		if (mmitest.equals("true"))
			return true;
		else
			return false;*/
		//modify by liliang.bao end
	}
	//add by xianfeng.xu for CR364979 begin
	/**
	 * filter the Mini List by the shortcode
	 */
	public static Test[] getList(Test[] mtest,String str){
        Test[] test=null;
        if(mtest==null){
            return null;
        }
        if(str==null||str.equals(mShortCode_single)){
            return mtest;
        }
        if(str.equals(mShortCode_dual)){
            test =dual_List;
        }else if(str.equals(mShortCode_single_ignoremhl)){
            test =single_ignoremhl_List;
        }else{
            return mtest;
        }
        List<Test> mList = new ArrayList<Test>();
        for (int i=0; i<mtest.length; i++) {
            boolean addenable=true;
            for(int j=0;j<test.length;j++){
                if(mtest[i]==null){
                    break;
                }
                if(mtest[i].mName.equals(test[j].mName)){
                    addenable =false;
                }
            }
            if(addenable){
                mList.add(mtest[i]);
            }
        }
        Test[] mTestsList=mList.toArray(new Test[mList.size()]);
        return mTestsList;
    }
	/**
	 * get the product info
	 */
	public static String GetASCStringFromTrace(TracabilityStruct.ID id){
	    try {
	        TracabilityStruct mTStruct = new TracabilityStruct();
	        byte[] resArr = mTStruct.getItem(id);
	        String strReturn = new String(resArr);
	        if(strReturn!=null&&strReturn.length() < 1)
	            strReturn = "NA";
	        return strReturn;
        } catch (Exception e) {
            // TODO: handle exception
        }
        return null;
    }
	//add by xianfeng.xu for CR364979 end
}