package com.weibo.sdk.syncbox.listener;
 
import com.weibo.sdk.syncbox.type.pub.EventInfo;
import com.weibo.sdk.syncbox.type.pub.SyncBoxMessage;

/**
 * @ClassName: RecvListener 
 * @Description: 客户端接收增量消息和sdk事件时回调
 * @author liuzhao
 * @date 2013-5-10 下午6:58:03 
 */
public interface RecvListener {
 
	/** 收到增量消息时回调 */
	void onReceiveMsg(final SyncBoxMessage syncBoxMessage);

	/** 发生SDK内部事件时回调 */
	void onEvent(final EventInfo eventInfo);
}
