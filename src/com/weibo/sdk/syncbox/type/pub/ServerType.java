package com.weibo.sdk.syncbox.type.pub;

/**
 * @ClassName: ServerType
 * @Description: 通用接口的服务类型.
 * @author liuzhao
 * @date 2013-4-11 上午10:32:09
 */
public enum ServerType {
	/** 微米平台.*/
	weimiPlatform(1),
	/** 微米活动服务平台.*/
	weimiActivity(2),
	/** 微博平台.*/
	weiboPlatform(3);

	private final int value;

	ServerType(int value) {
		this.value = value;
	}

	public int get() {
		return value;
	}
}