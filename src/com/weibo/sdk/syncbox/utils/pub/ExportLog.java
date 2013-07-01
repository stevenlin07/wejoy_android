package com.weibo.sdk.syncbox.utils.pub;

import com.weibo.sdk.syncbox.type.pub.SdkEvent;


/**
 * 日志接口，实现该接口即可获得日志信息
 * @author liuzhao
 *
 */
public interface ExportLog {
	/**
	 * SDK的info信息
	 * @param message
	 */
	void info(String message);

	/**
	 * SDK的warn信息
	 * @param message
	 */
	void warn(String message);


	/**
	 * SDK的error信息
	 * @param message 信息
	 * @param sdkEvent sdk发生的事件
	 * @param cause 原因
	 */
	void error(String message,SdkEvent sdkEvent,Throwable cause);
}
