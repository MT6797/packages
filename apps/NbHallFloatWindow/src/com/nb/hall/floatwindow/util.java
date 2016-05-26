package com.nb.hall.floatwindow;

import java.io.BufferedReader;
import java.io.FileReader;

import android.util.Log;


public class util {

    static String readHallState()
    {	
    	try
    	{
        BufferedReader reader = new BufferedReader(new FileReader("/sys/class/input/input7/status"), 256);
        try {
        	String state = reader.readLine();
        		Log.d("lqh", "bll====>hall state: "+state);
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
