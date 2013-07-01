package com.weibo.sdk.syncbox.type.pub;

public enum EventType {

	/** 网络联通.*/
	NIO_CONNECTED,
	/** 网络无法联通.*/
	NIO_UNABLE_CONNNECT,
	/** 网络连接中断.*/
	NIO_DISCONNECT,
	
	/** 认证过期，请重新认证*/
	AUTH_LAPSE,
	
	/** 本地存储出错的事件.*/
	LOCAL_STORE_ERROR;
}
