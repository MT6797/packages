package com.nb.hall.floatwindow;

import java.io.BufferedReader;
import java.io.FileReader;

import android.util.Log;


public class util {

   static String readHallState()
    {	
    	try
    	{
    	  int pos =0;
    	  BufferedReader reader;
    	 for(int i=0; i < 10; i++)
    	 {
    		 reader = new BufferedReader(new FileReader("/sys/class/input/input"+i+"/name"), 256);
    		 if("cover".equals(reader.readLine()))
    		 {
    			 pos = i;
    			 break;
    		 }
    	 }
        reader = new BufferedReader(new FileReader("/sys/class/input/input"+pos+"/status"), 256);
        try {
        	String state = reader.readLine();
        	Log.d("lqh", "bll====>pos:"+pos+"  hall state: "+state);
            return state;
        } finally {
            reader.close();
        } 
    	}catch(Exception e)
    	{
    		
    	}
    	return "1";
    }
}
