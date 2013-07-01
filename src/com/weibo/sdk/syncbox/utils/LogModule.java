package com.weibo.sdk.syncbox.utils;

import com.weibo.sdk.syncbox.type.pub.SdkEvent;
import com.weibo.sdk.syncbox.utils.pub.ExportLog;

/**
 * <h4>日志管理模块(单例)</h4>
 * <ol>
 * <li>ExportEvent:对客户端进行的异常通知类型</li>
 * <li>SdkEvent:SDK内部的运行日志</li>
 * <li>ExportLog:导出日志的结果，客户端通过自己实现该接口，获取SDK的内部日志</li>
 * </ol>
 *  @author liuzhao
 */
public enum LogModule {

	INSTANCE;

	LogModule() {
	}
	
	private ExportLog exportLog ;

	/** 注入实例. */
	public void setExportLog(ExportLog exportLog) {
		this.exportLog = exportLog;
	}

	enum Level{
		INFO,
		WARN,
		ERROR;
	}

	private void pushExportLog(Level level,String message,SdkEvent sdkEvent, Throwable cause) {
		if (null == exportLog){ return; }
		try {
			if (Level.INFO == level) {
				exportLog.info(message);
			} else if (Level.WARN == level) {
				exportLog.warn(message);
 			} else if (Level.ERROR == level) {
 				exportLog.error(message, sdkEvent, cause);
 			}
		}catch (Exception e){
			error("[exportLog]["+level.name()+"]",SdkEvent.EXPROTLOG_ERROR, e);
		}
	}

	/** INFO级别的日志  */
	public void info(final String message,final SdkEvent sdkEvent) {
		pushExportLog(Level.INFO,message,sdkEvent,null);
	}

	/** WARN级别的日志  */
	public void warn(final String message,final SdkEvent sdkEvent) {
		pushExportLog(Level.WARN,message,sdkEvent,null);
	}

	/**
	 * ERROR级别的日志
	 * @param message 日志摘要
	 * @param SdkEvent SDK内部异常类型
	 * @param cause 异常原因
	 */
	public void error(final String message, final SdkEvent sdkEvent, final Throwable cause) {
		pushExportLog(Level.ERROR, message, sdkEvent, cause);
	}
}
