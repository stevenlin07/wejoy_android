package com.weibo.sdk.syncbox.sync;

import java.util.LinkedList;
import java.util.List;

import com.google.protobuf.InvalidProtocolBufferException;
import com.weibo.sdk.syncbox.service.ProxyService;
import com.weibo.sdk.syncbox.type.SyncMessage.*;
import com.weibo.sdk.syncbox.type.pub.SdkEvent;
import com.weibo.sdk.syncbox.utils.StoreModule.ContainType;
import com.weibo.sdk.syncbox.utils.TimeModule;

public class SyncRespFix extends SyncHandler{

	/** Sync 请求的返回结果的处理 */
	public static void fixSync(String callback,byte[] response) {
		log.info("[FIXSYNC RESP]",SdkEvent.INVOKE);
		SyncResp resp;
		try {
			resp = SyncResp.parseFrom(response);
		} catch (InvalidProtocolBufferException e) { // 内部错误不向客户端发送，发到SDK的全局通知通道，考虑log
			log.error("[FIXSYNC][RESP][PARSE ERROR]",SdkEvent.WESYNC_PARSE_ERROR, e);
			String[] respTag = callback.split(":");
			if (3 == respTag.length ){ //FixSync:folderId:msgId
				TimeModule.INSTANCE.cancelTimer(respTag[1]); //取消超时
				stopFixSync(respTag[1]);
			}
			return;
		}

		String folderId = resp.getFolderId();
		TimeModule.INSTANCE.cancelTimer(folderId); //取消超时
		String nextSyncKey = resp.getNextKey();
		String syncKey = SyncBuilder.TAG_SYNC_KEY;
		if (store.syncKeys.containsKey(folderId)) {
			syncKey = store.syncKeys.get(folderId);
		}
		store.syncKeys.put(folderId, nextSyncKey);
		log.info("[FIXSYNC RESP][RECEIVE][folder:"+folderId+"][synckey:"+syncKey+"][isFullSync:"
				+resp.getIsFullSync()+"][Nextsynckey:"+ nextSyncKey+"]",SdkEvent.INVOKE);
		List<Meta> serverList = resp.getServerChangesList();
		
		log.info("[FIXSYNC RESP][接收消息:"+serverList.size()+"]",SdkEvent.INVOKE);
		LinkedList<String> ackList = new LinkedList<String>();
		
		if (serverList.size() > 0) {
			if (!service.pollByFixSync(folderId, serverList,ackList)) {
				log.info("[FIXSYNC STOP][isSdcard = true && DB failed]", SdkEvent.INVOKE);
				store.removeFixNotice(folderId);
				store.fixTag.remove(folderId); // 停止修复状态
				return; // 退出
			}
		}
		if (!syncKey.equals("0") && SyncKey.isEmpty(nextSyncKey)) { // 到达了sync的终止条件
			stopFixSync(folderId);
		} else { //SYNC CONTINUE
			log.info("[FIXSYNC RESP][FIXSYNC CONTINUE][folder:"+folderId+"][synckey:"+syncKey+"]",SdkEvent.INVOKE);
			SyncCoreReq.sendFixSyncReq(folderId, ackList, !resp.hasHasNext(), store.syncKeys.get(folderId));
		}
	}
	
	private static void stopFixSync (String folderId) {
		log.info("[FIXSYNC RESP][SYNC STOP][folderId: "+ folderId+"]",SdkEvent.INVOKE); // REAL QUIT
		store.syncKeys.put(folderId,SyncBuilder.TAG_SYNC_KEY); // 重新置零
		String tag = store.fixTag.get(folderId);
		String expectAck = store.expectAckMap.get(tag); // 取出引起的expect

		if (null == expectAck) {  // 说明其expect的消息已经推出到客户端
			pollByFixTag(folderId); 
			finallyFixSync(folderId);
		} else {
			if (ContainType.UNFINDED == store.contain(folderId, expectAck) ){ // 但是服务端的change没法保证，可能已经被删掉了
				ProxyService.INSTANCE.getTheMessage(folderId,expectAck);// 发送请求来获取这条消息
			} else {
				pollByFixTag(folderId); 
				finallyFixSync(folderId);
			}
		}
	}
	
	/** 推送到客户端,修复tag的直接推送，其依赖的从缓存中获取后推送，请求来自FixSync，调用前已经有回执 */
	private static boolean pollByFixTag(String folderId) {
		String fixTagId = store.fixTag.get(folderId);
		ContainType containTypeTag = store.contain(folderId, fixTagId);
		if (ContainType.FIXCACHE != containTypeTag) { // 如果该消息在历史或者不存在，则不需要推送给客户端了
			return true;
		} 
		Meta meta = store.getFromMetaCache(fixTagId);
		LinkedList<String> ackList = new LinkedList<String>();
		if (null != meta) {
			return service.pollDirectByFix(folderId,meta,ackList);
		}
		SyncCoreReq.sendNoAckSyncReq(folderId, ackList);
		return true;
	}
	
	public static void finallyFixSync (String folderId) {
		while (true){ // 判断是否还要继续修复
			String tag = store.pollFixNotice(folderId); //如果poll出来东西了就需要修复
			if (null != tag){
				store.fixTag.put(folderId, tag); // 覆盖掉原有的tag，保持修复逻辑
				String expectAck = store.expectAckMap.get(tag); // 当前修复状态的期望ack
				
				if (null == expectAck) {  
					pollByFixTag(folderId);
					continue;
				} else {
					ContainType containTypeExp = store.contain(folderId, expectAck);
					if (ContainType.HISTORY ==  containTypeExp){
						pollByFixTag(folderId);
						continue;
					}  	
				}
				LinkedList<String> ackList = new LinkedList<String>();
				ackList.add(tag);
				SyncCoreReq.sendFixSyncReq(folderId, ackList, false, SyncBuilder.TAG_SYNC_KEY);
				break;
			}else{
				store.fixTag.remove(folderId); // 无需修复，该folder退出修复逻辑
				break;
			}
		}
	}

	/**  代理修复的逻辑，进入的callback不能为空 */
	public static void proxyFixSync(String callbackId,byte[] response) {
		String[] splite = callbackId.split(":");
		String expectAck = splite[2];
		String folderId = splite[1];
		TimeModule.INSTANCE.cancelTimer(folderId); // 超时
		ContainType containType = store.contain(folderId, expectAck);
		if (ContainType.UNFINDED == containType) {
			Meta expectMeta;
			try {
				expectMeta = Meta.parseFrom(response);
			} catch (InvalidProtocolBufferException e) {
				// 2 -> 1 2依赖于1，但1没有到达
				// 如果1通过fixSync进行修复，没有成功，因为change被删除掉了
				// 再通过代理来获取1，仍然没有成功，说明这条消息确实不存在
				// 只能把2相关的消息全部推送出来了，忽略1
				pollByFixTag(folderId); 
				finallyFixSync(folderId);
				return;
			}
			if (!service.pollByProxySync(folderId,expectMeta)){ // 以exp为根，找依赖关系树，会包括引发修复的消息
				log.info("[FIXSYNC STOP][isSdcard = true && DB failed]", SdkEvent.INVOKE);
				store.removeFixNotice(folderId);
				store.fixTag.remove(folderId); // 停止修复状态
				return; // 退出
			}
		} 
		// 如果1在历史记录，则2必然已经推出
		// 如果1在修复记录中，说明1的依赖必然仍未满足，可以认为2的修复完成，但仍不能呈现
		finallyFixSync(folderId);
	}

	/** fixsync超时*/
	public static void fixSyncTimeout(String folderId) {
		stopFixSync(folderId);
	}
	
	/** 代理请求超时*/
	public static void proxyFixSyncTimeout(String folderId) {
		pollByFixTag(folderId); 
		finallyFixSync(folderId);
	}
}

