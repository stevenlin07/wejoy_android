package com.weibo.sdk.syncbox.type.pub;

/**
 * @ClassName: RequestType
 * @Description: 代理服务的HTTP请求类型。
 * @author liuzhao
 * @date 2013-4-11 上午10:30:48
 */
public enum RequestType {
	
	/** POST请求.*/
	POST(0),
	/** GET请求.*/
	GET(1),
	/** MUTIPART请求.*/
	MUTIPART(2);

	private final int value;

	private RequestType(int value) {
		this.value = value;
	}

	public int get() {
		return value;
	}
}
