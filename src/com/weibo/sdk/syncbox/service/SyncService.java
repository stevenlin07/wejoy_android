package com.weibo.sdk.syncbox.service;

import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
 
import com.weibo.sdk.syncbox.type.SyncMessage.Meta;
import com.weibo.sdk.syncbox.type.pub.EventInfo;
import com.weibo.sdk.syncbox.type.pub.EventType;
import com.weibo.sdk.syncbox.type.pub.FixMetaInfo;
import com.weibo.sdk.syncbox.type.pub.MetaType;
import com.weibo.sdk.syncbox.type.pub.SdkEvent;
import com.weibo.sdk.syncbox.type.pub.SyncBoxMessage;
import com.weibo.sdk.syncbox.utils.LogModule; 
import com.weibo.sdk.syncbox.utils.NotifyModule;
import com.weibo.sdk.syncbox.utils.StoreModule; 

public enum SyncService {

	INSTANCE;
 
	private static final int historyCacheMaxSize = 1000;
	private static final int historyCacheLimitSize = 500;

	private static StoreModule store = StoreModule.INSTANCE;
	private static LogModule log = LogModule.INSTANCE;
	private static NotifyModule notify = NotifyModule.INSTANCE;
	
	/** 推送到客户端，从MetaCache中获取，请求来自notice, 调用后有回执*/
	public boolean pollByNotice (String folderId,Meta meta,LinkedList<String> ackList) {
		if ( !pushToClient(folderId, meta)) {
			return false;
		}
		String msgId = meta.getId();
		LinkedList<String> expAckTree = store.getExpectAckKeySet(msgId); // 查看该消息是否存在被依赖
		LinkedHashSet<String> pollList = new LinkedHashSet<String>();
		ackList.addAll(expAckTree);
		pollList.addAll(ackList);
		if ( !pollFromMetaCacheToClient(folderId, pollList)) {
			return false;
		}
		store.removeExpectAckTree(expAckTree);// 清理掉依赖关系
		return true;
	}
	
	/** 推送到客户端,修复的直接推送，其依赖的从缓存中获取后推送，请求来自FixSync，调用后有回执*/
	public boolean pollByFixSync (String folderId,List<Meta> fixSyncList,LinkedList<String> ackList) {
		for (Meta meta:fixSyncList) {
			if (!pollDirectByFix(folderId,meta,ackList)) {
				return false;
			}
		}
		return true;
	}
	
	public boolean pollByProxySync (String folderId,Meta meta) {
		return pollDirectByFix(folderId,meta,null);
	}
	
	public boolean pollDirectByFix (String folderId,Meta meta,LinkedList<String> ackList) {
		LinkedHashSet<String> pollList = new LinkedHashSet<String>();
		if ( !pushToClient(folderId, meta)) {
			return false;
		}
		pollList.addAll(store.getExpectAckKeySet(meta.getId()));
		if ( !pollFromMetaCacheToClient(folderId, pollList)) {
			return false;
		}
		// 进行回执
		if (null != ackList) {
			ackList.addAll(pollList);
		}
		pollList.clear();
		return true;
	}
	
	/** 推送到客户端,从获取未读中获取，请求来自Sync，调用后有回执 */
	public boolean pollDirectBySync (String folderId,List<Meta> syncList) {
		for (Meta meta:syncList) {
			if ( !pushToClient(folderId, meta)) {
				return false;
			}
		}
		return true;
	}
	
	/** 推送到客户端,从MetaCache中获取 */
	private boolean pollFromMetaCacheToClient(String folderId, LinkedHashSet<String> pollList) {
		for(String msgId:pollList){
			Meta meta = store.getFromMetaCache(msgId);
			if (null == meta) {
				continue;
			}
			if(!pushToClient(folderId,meta)){
				return false;
			}
		}
		store.removeFromMetaCache(pollList,folderId); // 然后删除这些修复的消息
		store.removeExpectAckTree(pollList);
		return true;
	}
	
	/** 写数据逻辑顺序:（按重要程度排序）,客户端->历史ID存储->历史ID缓存->回执->发送到客户端,是否发送到客户端成功 true*/
	private boolean pushToClient(String folderId,Meta meta){
		if(null == meta){
			log.warn("[PUSHFILTER][META NULL]",SdkEvent.INVOKE);
			return true; // push
		}
		String msgId = meta.getId();
		try {
			if (store.database.containHistoryId(msgId,folderId)){
				log.info("[PUSHFILTER][historyIdDB has it "+msgId+"]",SdkEvent.INVOKE);
				return true;
			}
			SyncBoxMessage syncBoxMessage = MetaProc.process(folderId, meta);
			notify.onReceiveMsg(syncBoxMessage);
			
			MetaType type = MetaType.valueOf((meta.getType().toByteArray())[0]);
			String typestr = MetaType.audio == type ? "audio" : 
				(MetaType.subfolder == type ? "subfolder" : "" + type);
			log.info("[PUSHFILTER][insert historyIdDB]" + msgId + "," + folderId + "," + typestr, SdkEvent.INVOKE);
			store.database.insertHistoryId(msgId,folderId,meta.getTime());
		} catch (Exception e) { //TODO DB 查，写失败 && sdcard = true
			log.error("[Database Exception]", SdkEvent.LOCAL_STORE_ERROR, e);
			return true; // 不需要回复客户端及服务端
		}
		store.historyCache.add(msgId); // 缓存历史记录
		if (store.historyCache.size() > historyCacheMaxSize){ // 控制记录长度
			for(String historyId:store.historyCache){
				store.historyCache.remove(historyId);
				if (historyCacheLimitSize <= store.historyCache.size()){
					break;
				}
			}
		}
		return true;
	}

	/** 注册成功后将fixMeta DB中的内容load出来，推送到客户端，然后删掉推送成功的这条 */
	public void loadFixMetaByDb() {
		log.info("[CACHE][fixMetaDb has it]",SdkEvent.INVOKE);
		LinkedList<FixMetaInfo> fixMetaList;
		try {
			fixMetaList = store.database.getFixMetaAll();
			if (null == fixMetaList) {
				return;
			}
			for (FixMetaInfo fixMetaInfo:fixMetaList) {
				String folderId = fixMetaInfo.getFolderId();
				Meta fixMeta = Meta.parseFrom(fixMetaInfo.getFixMetaByte());
				String msgId = fixMeta.getId();
				if (SyncService.INSTANCE.pushToClient(folderId,fixMeta)){
					store.database.removeFixMeta(msgId,folderId);
				}
			}
		} catch (Exception e) {
			EventInfo eventInfo = new EventInfo();
			eventInfo.info = "本地存储失效";
			eventInfo.eventType = EventType.LOCAL_STORE_ERROR;
			notify.onEvent(eventInfo);
			log.error("[LOAD FIXMETA]", SdkEvent.LOCAL_STORE_ERROR, e);
		}
	}
}
