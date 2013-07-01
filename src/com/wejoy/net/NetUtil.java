package com.wejoy.net;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
/**
 * @author WeJoy Group
 *
 */
public class NetUtil {
	public static Bundle parseUrl(String url) {
		try {
			URL u = new URL(url);
			Bundle b = decodeUrl(u.getQuery());
			b.putAll(decodeUrl(u.getRef()));
			return b;
		} 
		catch (MalformedURLException e) {
			return new Bundle();
		}
	}

	public static Bundle decodeUrl(String s) {
		Bundle params = new Bundle();
		if (s != null) {
			String array[] = s.split("&");
			for (String parameter : array) {
				String v[] = parameter.split("=");
				params.putString(URLDecoder.decode(v[0]), URLDecoder.decode(v[1]));
			}
		}
		
		return params;
	}

	public static String encodeUrl(HTTPParameters parameters) {
		if (parameters == null) {
			return "";
		}

		StringBuilder sb = new StringBuilder();
		boolean first = true;
		
		for (int loc = 0; loc < parameters.size(); loc++) {
			if (first) {
			    first = false;
			}
			else {
			    sb.append("&");
			}
			String _key=parameters.getKey(loc);
			String _value=parameters.getValue(_key);
			
			if(_value==null) {
			    Log.i("encodeUrl", "key:"+_key+" 's value is null");
			}
			else {
			    sb.append(URLEncoder.encode(parameters.getKey(loc)) + "="
                    + URLEncoder.encode(parameters.getValue(loc)));
			}
			
		}
		
		return sb.toString();
	}
	
	//判断当前网络是否为wifi
    public static boolean isWifi(Context mContext) {  
    	ConnectivityManager connectivityManager = 
    		(ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);  
    	NetworkInfo activeNetInfo = connectivityManager.getActiveNetworkInfo();  

    	if (activeNetInfo != null  && activeNetInfo.getType() == ConnectivityManager.TYPE_WIFI) {  
    		return true;  
    	}  
    	
    	return false;  
    }
}
