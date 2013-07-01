package com.weibo.sdk.syncbox.utils;
 
import com.weibo.sdk.syncbox.type.pub.ErrorInfo;
import com.weibo.sdk.syncbox.type.pub.ErrorType;
import com.weibo.sdk.syncbox.type.pub.SdkEvent;

/**
 * @author liuzhao 参数校验的原则一定是通过.
 */
public class VerifyModule {

	protected static LogModule log = LogModule.INSTANCE;
	protected static AuthModule auth = AuthModule.INSTANCE;
	protected static StoreModule store = StoreModule.INSTANCE;
	protected static NotifyModule notify = NotifyModule.INSTANCE;
	
	/** String 类型的参数校验,校验成功*/
	public static boolean stringVerify(String withtag, String paramName, String paramValue) {
		if (isNull(withtag, paramName, paramValue)) return false; 
		if (isEmpty(withtag, paramName, paramValue)) return false;
		return true;
	}

	/**  判断对象是否是空的, true 是空的*/
	public static boolean isNull(String withtag, String paramName, Object obj) {
		if (null == obj) {
			ErrorInfo errorInfo = new ErrorInfo();
			errorInfo.info = "[ERROR][" + paramName + " is null]";
			errorInfo.errorType = ErrorType.INPUT_ERROR;
			log.error(errorInfo.info, SdkEvent.PARAMS_ERROR, null);
			notify.onFailed(errorInfo, withtag);
			return true;
		}
		return false;
	}

	/** 判断String的长度是都是空的,true 是空的 */
	protected static boolean isEmpty(String withtag, String paramName, String paramValue) {
		if (0 == paramValue.length() || 0 == paramValue.trim().length()) {
			ErrorInfo errorInfo = new ErrorInfo();
			errorInfo.info = "[ERROR][" + paramName+ " is empty]";
			errorInfo.errorType = ErrorType.INPUT_ERROR;
			log.error(errorInfo.info, SdkEvent.PARAMS_ERROR, null);
			notify.onFailed(errorInfo, withtag);
			return true;
		}
		return false;
	}

	/** 判断参数是否不大于0 true 不大于0*/
	public static boolean isZero(String withtag, String paramName, int paramValue) {
		if (0 >= paramValue) {
			ErrorInfo errorInfo = new ErrorInfo();
			errorInfo.info = "[ERROR][" + paramName + " is zero]";
			errorInfo.errorType = ErrorType.INPUT_ERROR;
			log.error(errorInfo.info, SdkEvent.PARAMS_ERROR, null);
			notify.onFailed(errorInfo, withtag);
			return true;
		}
		return false;
	}
}
