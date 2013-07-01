package com.weibo.sdk.syncbox.type.pub;

/**
 * @ClassName: ErrorType 
 * @Description: 错误类型
 * @author liuzhao
 * @date 2013-5-20 下午1:47:38 
 */
public enum ErrorType {

	/** 认证失败，用户名密码/token无效或服务不可用，或者认证过期.*/
	AUTH_FAILED,
	/** 调用接口没有经过认证，调用被阻止.*/
	AUTH_FORBID,
	/** 调用接口输入参数错误.*/
	INPUT_ERROR,
	/** 读取错误.*/
	READ_ERROR,
	/** 写入错误.*/
	WRITE_ERROR,
	/** 网络无法联通.*/
	NET_UNCONNECTED,
	/**超时错误*/
	TIMEOUT;
}
