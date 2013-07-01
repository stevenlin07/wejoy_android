package com.wejoy.util;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;
/**
 * 
 * @author WeJoy Group
 *
 */
public class DebugUtil {
	
    public static final String TAG = "DebugUtil";
    public static LOG_LEVEL logLevel = LOG_LEVEL.DEBUG;
    public static enum LOG_LEVEL { 
    	DEBUG,
    	INFO,
    	WARN,
    	ERROR
    };
    
    public static boolean reBuildSchema = false;
    
    public static void toast(Context context,String content){
        Toast.makeText(context, content, Toast.LENGTH_SHORT).show();
    }
    
    public static void debug(String msg) {
    	debug(TAG, msg);
    }
    
    public static void debug(String tag, String msg){
        if (LOG_LEVEL.DEBUG.equals(logLevel)) {
            Log.d(tag, msg);
        }
    }
    
    public static void debug(String tag, String msg, Throwable error){
        if (LOG_LEVEL.DEBUG.equals(logLevel)) {
            Log.d(tag, msg, error);
        }
    }
     
    public static void info(String classname, String methodname, String msg){
        if (LOG_LEVEL.DEBUG.equals(logLevel) || LOG_LEVEL.INFO.equals(logLevel)) {
            Log.d(classname + "." + methodname, msg);
        }
    }
    
    public static void warn(String tag, String msg){
        if (LOG_LEVEL.DEBUG.equals(logLevel) || LOG_LEVEL.INFO.equals(logLevel) || LOG_LEVEL.WARN.equals(logLevel)) {
            Log.d(tag, msg);
        }
    }
    
    public static void warn(String classname, Throwable warn){
        if (LOG_LEVEL.DEBUG.equals(logLevel) || LOG_LEVEL.INFO.equals(logLevel) || LOG_LEVEL.WARN.equals(logLevel)) {
            Log.w(classname, warn);
        }
    }
    
    public static void error(String tag, String error){
        Log.e(tag, error);
    }
     
    public static void error(String error){
        Log.e(TAG, error);
    }
    
    public static void error(String classname, String msg, Throwable error){
        Log.e(classname, msg, error);
    }
}
