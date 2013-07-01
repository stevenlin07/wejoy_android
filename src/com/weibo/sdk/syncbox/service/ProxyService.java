package com.weibo.sdk.syncbox.service;

import com.weibo.sdk.syncbox.type.pub.SdkEvent;
import com.weibo.sdk.syncbox.utils.Constansts;
import com.weibo.sdk.syncbox.utils.TimeModule;


/**
 * @ClassName: ProxyService
 * @Description: 代理服务
 * @author liuzhao
 * @date 2013-4-15 下午6:19:00
 */
public enum ProxyService {

	INSTANCE;
 
//	private String GET_THE_MESSAGE_TAG = "GET_THE_MESAAGE";

	/** 获取指定的私信,该接口提供给内部修复使用 */
	public boolean getTheMessage (String folderId,String msgId) {
		TimeModule.INSTANCE.setTimer(folderId, Constansts.PROXYSYNC_TIME, SdkEvent.PROXYSYNC_TIMEOUT);
		
//		String callbackId = GET_THE_MESSAGE_TAG+":"+folderId+":"+msgId;
//		StringBuilder params = new StringBuilder();
//		params.append("msgId=").append(msgId);
		return true;
	}

}
