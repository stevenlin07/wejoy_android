package com.weibo.sdk.syncbox.sync;

import java.util.LinkedList;
import java.util.List;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.weibo.sdk.syncbox.type.SyncMessage.Meta;
import com.weibo.sdk.syncbox.type.SyncMessage.Notice;
import com.weibo.sdk.syncbox.type.SyncMessage.Unread;
import com.weibo.sdk.syncbox.type.pub.MetaType;
import com.weibo.sdk.syncbox.type.pub.SdkEvent; 
import com.weibo.sdk.syncbox.utils.StoreModule.ContainType;

public class SyncRespNotice extends SyncHandler{

	/** 处理服务端推送的notice */
	public static void notice(byte[] response){
		try {
			Notice notice = Notice.parseFrom(response);
			log.info("[Notice][RESP]["+ getDebugInfo(notice) +"]",SdkEvent.INVOKE);
			
			Meta meta = getMetaByNotice(notice);
			String folderId = getFolderIdByNotice(notice);
			String expectAck = getExpectAckByNotice(notice);
			if (null == meta || null == folderId ) {
				return;
			}
			String msgId = meta.getId(); // 当前消息的ID

			LinkedList<String> ackList = new LinkedList<String>(); // 返回服务器期望的ACK
			ackList.add(msgId); // 回复当前的消息

			if (null == expectAck){ 
				if (service.pollByNotice(folderId, meta, ackList)) {
					SyncCoreReq.sendNoAckSyncReq(folderId, ackList);
				}
			} else {
				// 判断是否在缓存中存在，会查询所有的缓存
				ContainType containType = store.contain(folderId, expectAck);
				if (ContainType.HISTORY == containType){ // 历史中存在，说明前继已经落地
					ackList.add(expectAck); //回执期望的ack
					if (service.pollByNotice(folderId, meta, ackList)) {
						SyncCoreReq.sendNoAckSyncReq(folderId, ackList);
					}
				} else if (ContainType.FIXCACHE == containType) { 
					if (!store.saveToMetaCache(folderId, meta, expectAck)) {
						return;
					}
				} else if (ContainType.UNFINDED == containType){ // 不存在则进行修复
					if (!store.saveToMetaCache(folderId, meta, expectAck)) {
						return;
					}
					if (store.fixTag.containsKey(folderId)) { // 正在修复，缓存notice，缓存这条消息，但不落地
						store.saveFixNotice(folderId, msgId);
					} else {
						store.fixTag.put(folderId, msgId); //设置修复标记位
						SyncCoreReq.sendFixSyncReq(folderId, ackList, false, SyncBuilder.TAG_SYNC_KEY); // 因为有文件夹的锁
					}
				}
			}
		} catch (InvalidProtocolBufferException e) {
			log.error("[Notice][RESP][PARSE ERROR]",SdkEvent.WESYNC_PARSE_ERROR,e);
			SyncCoreReq.getItemUnread();
		}
	}
	
	/** 根据notice解析出消息体 */
	private static Meta getMetaByNotice(Notice notice) {
		List<Meta> metaList = notice.getMessageList();
		if (null == metaList || metaList.isEmpty() || 1 < metaList.size()){
			log.warn("[Notice][metalist is empty]",SdkEvent.INVOKE);
			return null;
		}
		return metaList.get(0);
	}
	
	/*
	 * @by jichao, don't display too much binary data such as thumbnail, audio
	 */
	private static String getDebugInfo(Notice notice) {
		Notice.Builder noticeBuilder = Notice.newBuilder();
		
		try {
			List<Unread> unreads = notice.getUnreadList();
			
			for(int i = 0; unreads != null && i < unreads.size(); i++) {
				Unread unread = unreads.get(i);
				noticeBuilder.addUnread(unread);
			}
			
			List<Meta> messages = notice.getMessageList();

			if(messages != null) {
				for(Meta message : messages) {
					noticeBuilder.addMessage(getMetaForDebug(message));
				}
			}
		} catch (InvalidProtocolBufferException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		String info = noticeBuilder.build().toString();
		return info;
	}
	
	private static Meta getMetaForDebug(Meta meta) throws InvalidProtocolBufferException {
		if(meta != null && meta.getType() != null && meta.getType().toByteArray().length > 0) {
			MetaType type = MetaType.valueOf((meta.getType().toByteArray())[0]);
			
			if(type == MetaType.audio || type == MetaType.image || type == MetaType.file) {
				Meta.Builder metabuilder = Meta.newBuilder();
				metabuilder.mergeFrom(meta.toByteArray());
				
				if(type == MetaType.audio) {
					metabuilder.setContent(ByteString.copyFromUtf8(meta.getContent() != null ? 
							meta.getContent().size() + "bytes..." :
							""));
				}
				
				if(type == MetaType.image) {
					metabuilder.setThumbnail(ByteString.copyFromUtf8(meta.getThumbnail() != null ? 
							meta.getThumbnail().size() + "bytes..." :
							"..."));
				}
				
				return metabuilder.build();
			}
		}
		
		return meta;
	}
	
	/** 根据notice解析出文件夹名称 */
	private static String getFolderIdByNotice(Notice notice) {
		List<Unread> unreadList = notice.getUnreadList();
		if (null == unreadList || unreadList.isEmpty() || 1 < unreadList.size() ){
			log.warn("[Notice][unreadList is empty]",SdkEvent.INVOKE);
			return null;
		}
		Unread unread = unreadList.get(0);
		String folderId = unread.getFolderId();
		return folderId;
	}
	
	/** 根据notice解析出expectAck */
	private static String getExpectAckByNotice(Notice notice) {
		List<String> expectAckList = notice.getExpectAckList(); // 期望的ACK
		String expectAck = null;
		if (null != expectAckList && !expectAckList.isEmpty()){ // no expect
			if ( 1 < expectAckList.size()){
				log.warn("[Notice][expectAckList has over limit]",SdkEvent.INVOKE);
			} else {
				expectAck = expectAckList.get(0);
			}
		}
		return expectAck;
	}
}
