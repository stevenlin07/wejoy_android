package com.weibo.sdk.syncbox.utils;

/**
 * @ClassName: NetHandler
 * @Description: 接收网络的数据包
 * @author liuzhao
 * @date 2013-4-17 上午11:03:54
 */
public interface NetHandler {

	void nioProcess(byte[] packet);
}
