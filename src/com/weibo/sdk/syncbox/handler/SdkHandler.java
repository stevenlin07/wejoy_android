package com.weibo.sdk.syncbox.handler;
  
import com.weibo.sdk.syncbox.net.NetHandler;
import com.weibo.sdk.syncbox.net.NetModule.SdkRequest; 
import com.weibo.sdk.syncbox.sync.SyncRespFile;
import com.weibo.sdk.syncbox.sync.SyncRespFix;
import com.weibo.sdk.syncbox.sync.SyncRespNotice;
import com.weibo.sdk.syncbox.sync.SyncRespSend;
import com.weibo.sdk.syncbox.sync.SyncRespUnread;
import com.weibo.sdk.syncbox.type.NioMessage.NioPacket;
import com.weibo.sdk.syncbox.type.pub.BoxResult; 
import com.weibo.sdk.syncbox.type.pub.SdkEvent; 
import com.weibo.sdk.syncbox.utils.Constansts;
import com.weibo.sdk.syncbox.utils.LogModule;
import com.weibo.sdk.syncbox.utils.NotifyModule;
import com.weibo.sdk.syncbox.utils.ReqId;
import com.weibo.sdk.syncbox.utils.TimeModule;

import com.google.protobuf.InvalidProtocolBufferException;

/**
 * @ClassName: SdkHandler
 * @Description: 接收消息的处理框架
 * @author liuzhao
 * @date 2013-4-17 上午10:08:20
 */
public class SdkHandler implements NetHandler{

	private static LogModule log = LogModule.INSTANCE;
	private static NotifyModule notify = NotifyModule.INSTANCE;
	private static TimeModule time = TimeModule.INSTANCE;

	/**
	 * NIO 的返回处理逻辑
	 * @param nioResponse
	 */
	@Override
	public void nioProcess(byte[] nioResponse){
		NioPacket nioPacket = null ;
		try {
			nioPacket = NioPacket.parseFrom(nioResponse);
		} catch (InvalidProtocolBufferException e) {
			log.error("[NIO][PROCESS][PARSE ERROR]",SdkEvent.WESYNC_PARSE_ERROR,e);
			return;
		}
		int code = nioPacket.getCode();
		String callbackId = nioPacket.getCallbackId();
		log.info("[NIO PROCESS][CODE:" + code + "][callbackId:" +callbackId+"]",SdkEvent.INVOKE);
		if (null == callbackId) {
			log.warn("[NIO][PROCESS][callbackId NULL]",SdkEvent.INVOKE);
			return;
		}
		SdkRequest sdkRequest = SdkRequest.valueOf(nioPacket.getSort());
		byte[] response = nioPacket.getContent().toByteArray();
		switch (sdkRequest) {
			case HEARTBEAT :// 处理心跳包的回复
				time.cancelTimer(Constansts.heartbeat_withtag);
				break;
			case WESYNC :
				if ("".equals(callbackId)) { 
					SyncRespNotice.notice(response);
				} else if ((callbackId.startsWith(ReqId.OnCreate_Auth)) // 认证
						|| (ReqId.OnInvoke_Auth).equals(callbackId) 
						|| (ReqId.OnInvoke_MAuth).equals(callbackId)) { 
					if (AuthService.process(callbackId, response, nioPacket)){ // 认证成功则处理获取未读
						SyncRespUnread.getItemUnread(response);
					}
				} else if ("SendFile".equals(callbackId)) { 
					SyncRespFile.sendFile(response);
				} else if ("GetFile".equals(callbackId)) { 
					SyncRespFile.getFile(response);
				} else if ("GetFile".equals(callbackId)) { 
					SyncRespFile.getFile(response);
				} else if ("GetFile".equals(callbackId)) { 
					SyncRespFile.getFile(response);
				} else if (callbackId.startsWith("Sync")) {
					SyncRespUnread.sync(callbackId,response);
				} else if (callbackId.startsWith("FixSync")) {
					SyncRespFix.fixSync(callbackId,response);
				} else if (callbackId.startsWith("SendOnly")) {
					SyncRespSend.sendOnly(response);
				} else if (callbackId.startsWith("NoExpSync")){
					String msgId = callbackId.substring("NoExpSync".length()+1);
					log.info("[NoExpSync][msgId = "+msgId+"]",SdkEvent.INVOKE);
				} else {
					log.warn("[HTTP RESP][WESYNC CALLBACK IS UNKONWN]",SdkEvent.INVOKE);
				}
				break;
			case APPPROXY : 
				appProxyHandler(callbackId,response);
				break;
			case PLUGIN :
				appProxyHandler(callbackId,response);
				break;
			default :
				break;
		}
	}


	void appProxyHandler(String callbackId,byte[] response) {
		log.info("[APPPROXY RESP]["+new String(response)+"]",SdkEvent.INVOKE);
		
		if (callbackId.startsWith("GET_THE_MESSAGE_TAG")) {
			SyncRespFix.proxyFixSync(callbackId, response);
		} else {
			BoxResult boxResult = new BoxResult();
			boxResult.result = new String(response);
			notify.onSuccess(boxResult, callbackId);
		}
	}

}
