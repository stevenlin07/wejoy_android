package test.sdk.utils;

import com.weibo.sdk.syncbox.type.pub.SdkEvent;
import com.weibo.sdk.syncbox.utils.pub.ExportLog;


public class DebugLog implements ExportLog {

	@Override
	public void info(String message) {
		if(message.startsWith("[NIO]")){
			return;
		}
		if(message.startsWith("[SyncDecoder][processFolder]")){
			return;
		}
		System.out.println(message);
	}

	@Override
	public void warn(String message) {
		System.out.println(message);
	}

	@Override
	public void error(String message, SdkEvent sdkEvent, Throwable cause) {
		System.out.println("ErrType:"+sdkEvent.name()+";REASON:"+message);
//		if(null != cause){
//			//cause.printStackTrace();
//		}
	}

}
