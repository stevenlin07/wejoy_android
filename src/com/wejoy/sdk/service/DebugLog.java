package com.wejoy.sdk.service;
   

import com.weibo.sdk.syncbox.type.pub.SdkEvent;
import com.weibo.sdk.syncbox.utils.pub.ExportLog;

import android.util.Log;

public class DebugLog implements ExportLog {

	@Override
	public void info(String message) {
		if(message.startsWith("[NIO]")){
			return;
		}
		if(message.startsWith("[SyncDecoder][processFolder]")){
			return;
		}
		Log.d("SYNCBOX",message);
	}

	@Override
	public void warn(String message) {
		Log.d("SYNCBOX",message);
	}

	@Override
	public void error(String message, SdkEvent sdkEvent, Throwable cause) {
		Log.d("SYNCBOX","EeventType:"+sdkEvent+";REASON:"+message);
		if(null != cause){
			cause.printStackTrace();
		}
	}
 
}
