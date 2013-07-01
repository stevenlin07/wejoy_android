package com.wejoy.ui.helper;

import com.wejoy.R;
import com.wejoy.util.DebugUtil;

import greendroid.widget.MyQuickAction;
import greendroid.widget.QuickAction;
import android.app.Activity;
import android.content.Context;
import android.graphics.PixelFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.TextView;

public class UIHelper {
	
	public static String ReqGroupInfo = "{\"key\":\"groups\"}";
	public static String ReqListUserPrefix = "{\"key\":\"bi_users\", \"listId\":\"";
	public static String ReqListUserSuffix = "\"}";
	private static View waitView = null;
	
	/**
	 * 快捷栏显示登录与登出，需要做用户是否登录判断，若登录采用上面方法
	 * @param activity
	 * @param qa
	 */
	public static void showSettingLoginOrLogout(Activity activity,QuickAction qa)
	{
		qa.setIcon(MyQuickAction.buildDrawable(activity, R.drawable.ic_menu_login));
		qa.setTitle(activity.getString(R.string.main_menu_login));
	}
	
	private static WindowManager.LayoutParams getWinLayoutParams(boolean isBlur) {
		WindowManager.LayoutParams lp = null;
        if (isBlur) {
            lp = new WindowManager.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, 
            	ViewGroup.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.TYPE_APPLICATION, 
            	WindowManager.LayoutParams.FLAG_FULLSCREEN | WindowManager.LayoutParams.FLAG_BLUR_BEHIND | 
            	WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, PixelFormat.TRANSLUCENT);
        }
        else {
            lp = new WindowManager.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, 
            	ViewGroup.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.TYPE_APPLICATION, 
            	WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE | 
            	WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, PixelFormat.TRANSLUCENT);
        }
        
        return lp;
	}

	public static void showInfoPop() {
		
	}
	
	public static void showWaiting(Activity parent, String msg, boolean isBlur) {
		try {
	        WindowManager.LayoutParams lp = getWinLayoutParams(isBlur);
	         
	        WindowManager mWindowManager = (WindowManager) parent.getSystemService(Context.WINDOW_SERVICE);
	        
	        if (waitView == null) {
	            LayoutInflater inflate = (LayoutInflater) LayoutInflater.from(parent);
	            waitView = inflate.inflate(R.layout.waiting_popwin, null);
	            TextView label = (TextView) waitView.findViewById(R.id.waiting_win_label);
	            label.setText(msg);
	        }
	         
	        mWindowManager.addView(waitView, lp);
	    }
	    catch (Throwable e) {
	        DebugUtil.error("UIHelper", "[showWaiting]", e);
	    }
	}
	   
	public static void showWaiting(Context parent, String msg, boolean isBlur) {
		
	    try {
	        WindowManager.LayoutParams lp = getWinLayoutParams(isBlur);
	         
	        WindowManager mWindowManager = (WindowManager) parent.getSystemService(Context.WINDOW_SERVICE);
	        
	        if (waitView == null) {
	            LayoutInflater inflate = (LayoutInflater) LayoutInflater.from(parent);
	            waitView = inflate.inflate(R.layout.waiting_popwin, null);
	            TextView label = (TextView) waitView.findViewById(R.id.waiting_win_label);
	            label.setText(msg);
	        }
	         
	        mWindowManager.addView(waitView, lp);
	    }
	    catch (Throwable e) {
	        DebugUtil.error("UIHelper", "[showWaiting]", e);
	    }
	}
	
	public static void hideWaiting(Activity parent) {
	    try {
	        if (waitView != null) {
	            WindowManager mWindowManager = (WindowManager) parent.getSystemService(Context.WINDOW_SERVICE);
	            mWindowManager.removeView(waitView);
	            waitView = null;
	        }
	    }
	    catch (Throwable e) {
	    	DebugUtil.error("UIHelper", "[showWaiting]", e);
	    }
	}
	
	public static void hideWaiting(Context parent) {
	    try {
	        if (waitView != null) {
	            WindowManager mWindowManager = (WindowManager) parent.getSystemService(Context.WINDOW_SERVICE);
	            mWindowManager.removeView(waitView);
	            waitView = null;
	        }
	    }
	    catch (Throwable e) {
	    	DebugUtil.error("UIHelper", "[showWaiting]", e);
	    }
	}
}
