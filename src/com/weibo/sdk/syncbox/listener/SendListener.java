package com.weibo.sdk.syncbox.listener;

import java.util.HashSet;

import com.weibo.sdk.syncbox.type.pub.BoxResult;
import com.weibo.sdk.syncbox.type.pub.ErrorInfo;

/**
 * @ClassName: SendListener
 * @Description: 客户端请求的回调接口
 * @author liuzhao
 * @date 2013-5-10 上午10:01:06
 */
public interface SendListener {

	/**请求成功时回调*/
	void onSuccess(BoxResult boxResult);

	/**请求失败时回调*/
	void onFailed(ErrorInfo errorInfo);
 
	/**
	 * 发送/下载文件时回调
	 * @param fileId 文件ID
	 * @param hasSucc 已经成功上传/下载的分片序号
	 * @param limit 分片总数
	 */
	void onFile(final String fileId, final HashSet<Integer> hasSucc, final int limit);
}
