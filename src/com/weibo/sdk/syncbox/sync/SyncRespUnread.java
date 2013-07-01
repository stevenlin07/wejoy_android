package com.weibo.sdk.syncbox.sync;

import java.util.List;

import com.google.protobuf.InvalidProtocolBufferException;
import com.weibo.sdk.syncbox.net.NetModule.SdkRequest;
import com.weibo.sdk.syncbox.type.SyncMessage.GetItemUnreadResp;
import com.weibo.sdk.syncbox.type.SyncMessage.Meta;
import com.weibo.sdk.syncbox.type.SyncMessage.SyncResp;
import com.weibo.sdk.syncbox.type.SyncMessage.Unread;
import com.weibo.sdk.syncbox.type.pub.SdkEvent;

public class SyncRespUnread extends SyncHandler{

	/**  GetItemUnread 请求的返回结果的处理 */
	public static void getItemUnread(byte[] response){
		log.info("[GetItemUnread][RESP]",SdkEvent.INVOKE);
		GetItemUnreadResp getItemUnreadResp = null;
		try {
			getItemUnreadResp = GetItemUnreadResp.parseFrom(response);
		} catch (InvalidProtocolBufferException e) {
			log.error("[GetItemUnread][RESP][PARSE ERROR]",SdkEvent.WESYNC_PARSE_ERROR,e);
			return;
		}
		List<Unread> unreadList = getItemUnreadResp.getUnreadList();
		if (unreadList.size() == 0) {
			log.info("[GetItemUnread][RESP][NO MESSAGE]",SdkEvent.INVOKE);
			return;
		}
		for (Unread u : unreadList) {
			String folderId = u.getFolderId();
			log.info("[GetItemUnread][RESP][folderId:" + folderId + "][unread:" + u.getNum() + "]",SdkEvent.INVOKE);
			if (u.getNum() <= 0) continue;
			SyncCoreReq.sync(folderId,null);
		}
	}
	
	/** Sync 请求的返回结果的处理 */
	public static void sync(String callbackId,byte[] response) {
		SyncResp resp;
		try {
			resp = SyncResp.parseFrom(response);
		} catch (InvalidProtocolBufferException e) { // 内部错误不向客户端发送，发到SDK的全局通知通道，考虑log
			log.error("[SYNC][RESP][PARSE ERROR]["+callbackId+"]",SdkEvent.WESYNC_PARSE_ERROR, e);
			return;
		}
		
		String folderId = resp.getFolderId();
		String syncKey = SyncBuilder.TAG_SYNC_KEY;
		if (store.syncKeys.containsKey(folderId)) {
			syncKey = store.syncKeys.get(folderId);
		}
		String nextSyncKey = resp.getNextKey();
		List<Meta> serverList = resp.getServerChangesList();
		log.info("[SYNC RESP][接收消息:"+serverList.size()+"]",SdkEvent.INVOKE);
		log.info("[SYNC RESP][回执消息:"+resp.getClientChangesList().size()+"]",SdkEvent.INVOKE);
		service.pollDirectBySync(folderId,serverList);	
		log.info("[SYNC RESP][RECEIVE][folder:"+folderId+"][synckey:"+syncKey+"][isFullSync:"
				+resp.getIsFullSync()+"][Nextsynckey:"+ nextSyncKey+"]",SdkEvent.INVOKE);
		store.syncKeys.put(folderId, nextSyncKey);
		
		if (!(SyncBuilder.TAG_SYNC_KEY).equals(syncKey) && SyncKey.isEmpty(nextSyncKey)) { // 到达了sync的终止条件
			log.info("[SYNC RESP][SYNC STOP][folderId: "+ folderId+"]",SdkEvent.INVOKE); // REAL QUIT
			store.syncKeys.put(folderId,SyncBuilder.TAG_SYNC_KEY); // 重新置零
		} else { // SYNC CONTINUE
			log.info("[SYNC RESP][SYNC CONTINUE][folder:"+folderId+"][synckey:"+syncKey+"]",SdkEvent.INVOKE);
			net.sendRequest(SdkRequest.WESYNC,store.uriMap.get("Sync"),"Sync:"+folderId,
					SyncBuilder.buildSync(folderId, false, nextSyncKey, null),null);
		}
	}
}
