package com.weibo.sdk.syncbox.sync;

import java.util.LinkedList;

import com.weibo.sdk.syncbox.net.NetModule;
import com.weibo.sdk.syncbox.net.NetModule.SdkRequest;
import com.weibo.sdk.syncbox.type.SyncMessage.Meta;
import com.weibo.sdk.syncbox.type.pub.ConvType;
import com.weibo.sdk.syncbox.type.pub.SdkEvent;
import com.weibo.sdk.syncbox.utils.AuthModule;
import com.weibo.sdk.syncbox.utils.Constansts;
import com.weibo.sdk.syncbox.utils.IdUtils;
import com.weibo.sdk.syncbox.utils.LogModule;
import com.weibo.sdk.syncbox.utils.StoreModule;
import com.weibo.sdk.syncbox.utils.TimeModule;


public class SyncCoreReq {

	private static StoreModule store = StoreModule.INSTANCE;
	private static LogModule log = LogModule.INSTANCE;
	private static NetModule net = NetModule.INSTANCE;
	private static AuthModule auth = AuthModule.INSTANCE;
	private static TimeModule time = TimeModule.INSTANCE;
	
	/** 适合于单聊和群聊的会话同步*/
	public static String createConversation(String touid, ConvType convType){
		log.info("[FolderCreate][REQ][TO:" + touid+"]",SdkEvent.INVOKE);
		String folderId = null;
		if (ConvType.SINGLE == convType) { // 单人聊天
			folderId = IdUtils.onConversation(auth.getUid(),touid);
		} else if (ConvType.GROUP == convType) { // 群组聊天
			folderId = IdUtils.onGroup(auth.getUid(), touid);
		}
		if(!store.syncKeys.containsKey(folderId)) {
			store.syncKeys.put(folderId, SyncBuilder.TAG_SYNC_KEY);
		}
		return folderId;
	}

	/**
	 * 发送sync请求
	 * @param folderId
	 * @param meta 包含时候就直接发送
	 * @return true 立即发送SYNC请求
	 */
	public synchronized static void sync(String folderId,Meta meta){
		String syncKey = store.syncKeys.get(folderId);
		Boolean isFullSync = false;
		if (null == syncKey) {
			log.info("[SYNC][REQ][INIT SYNCKEY]",SdkEvent.INVOKE);
			syncKey = SyncBuilder.TAG_SYNC_KEY;
			store.syncKeys.put(folderId, syncKey);
		}
		if(null != meta){
			net.sendRequest(SdkRequest.WESYNC,store.uriMap.get("Sync"),"SendOnly",
					SyncBuilder.buildSync(folderId, isFullSync, syncKey,meta),null);
		}else{
			net.sendRequest(SdkRequest.WESYNC,store.uriMap.get("Sync"),"Sync:"+folderId, // 用来标记解锁文件夹
					SyncBuilder.buildSync(folderId, isFullSync, syncKey,meta),null);
		}
	}

	/** 发送获取未读数的请求*/
	public static void getItemUnread() {
		net.sendRequest(SdkRequest.WESYNC, store.uriMap.get("GetItemUnread"), "GetItemUnread",null, null);
	}
	
	/** 发送修复sync的请求*/
	public static void sendFixSyncReq(String folderId, LinkedList<String> ackList, boolean flag, String syncKey) {
		time.setTimer(folderId, Constansts.FIXSYNC_TIME, SdkEvent.FIXSYNC_TIMEOUT);
		net.sendRequest(SdkRequest.WESYNC,store.uriMap.get("Sync"),"FixSync:"+folderId+":"+store.fixTag.get(folderId),
				SyncBuilder.buildSyncAck(folderId, ackList, flag, syncKey),null);
	}
	
	/** 发送无回执的sync请求*/
	public static void sendNoAckSyncReq(String folderId, LinkedList<String> ackList) {
		net.sendRequest(SdkRequest.WESYNC,store.uriMap.get("Sync"),"NoExpSync:ExpectAck",
				SyncBuilder.buildSyncAck(folderId, ackList, true, SyncBuilder.TAG_SYNC_KEY),null);
	}
}