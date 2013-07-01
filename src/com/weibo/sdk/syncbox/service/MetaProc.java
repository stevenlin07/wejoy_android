package com.weibo.sdk.syncbox.service;

import com.weibo.sdk.syncbox.type.SyncMessage.*;
import com.weibo.sdk.syncbox.type.pub.ConvType;
import com.weibo.sdk.syncbox.type.pub.MetaType;
import com.weibo.sdk.syncbox.type.pub.SdkEvent;
import com.weibo.sdk.syncbox.type.pub.SyncBoxMessage;
import com.weibo.sdk.syncbox.utils.IdUtils;
import com.weibo.sdk.syncbox.utils.LogModule;

/**
 * @ClassName: MetaProc
 * @Description: meta消息的处理
 * @author liuzhao
 * @date 2013-4-17 下午2:24:41
 */
public class MetaProc {

	private static LogModule log = LogModule.INSTANCE;

	/** meta消息的处理*/
	public static SyncBoxMessage process(String folderId, Meta meta) {
		boolean flag = false;
		log.info("[SyncDecoder][processMeta][folderId:"+folderId+"]",SdkEvent.INVOKE);
		SyncBoxMessage syncBoxMessage = getMsg(folderId, meta);
		MetaType type = MetaType.valueOf((meta.getType().toByteArray())[0]);
		syncBoxMessage.metaType = type;
		if ( MetaType.text == type || MetaType.mixed == type) { // 普通/MIX文本
			syncBoxMessage.text = meta.getContent().toStringUtf8();
			flag = true;
		} else if ( MetaType.audio == type) { // 语音消息
			syncBoxMessage.audioData = meta.getContent().toByteArray();
			syncBoxMessage.contentExt = meta.getContentExt().toStringUtf8();
			
			if (meta.hasSpanId()) { // 分片语音的处理
				syncBoxMessage.isSpanAudio = true;
				syncBoxMessage.spanId = meta.getSpanId();
				syncBoxMessage.spanSeqNo = meta.getSpanSequenceNo();
				if (meta.getSpanLimit() != 0) {
					syncBoxMessage.isLast = true;
					syncBoxMessage.spanLimit = meta.getSpanLimit();
				}
			}else {
				syncBoxMessage.isSpanAudio = false;
			}
			flag = true;
		} else if ( MetaType.image == type || MetaType.video == type || MetaType.file == type) {
			getFileMsg(syncBoxMessage, meta);
			flag = true;
		} else if ( MetaType.subfolder == type ) {
		} else {
			log.warn("[WARN][This version can't decode this meta!]",SdkEvent.INVOKE);
			log.info(meta.toString(),SdkEvent.INVOKE);
		}
		if (!flag) {
			syncBoxMessage = null;
		}
		return syncBoxMessage;
	}

	/** 设置会话的类型*/
	static void setBasicType(String folderId,Meta meta,SyncBoxMessage syncBoxMessage) {
		String split[] = folderId.split(IdUtils.FOLDERID_SPLIT);
		String stem = split[1];
		syncBoxMessage.deliveryBox = stem;
		if ((IdUtils.CONV_TAG).equals(stem) || (IdUtils.CONV_TAG2).equals(stem)) { // 单聊会话标志
			syncBoxMessage.convType = ConvType.SINGLE;
			syncBoxMessage.convId = meta.getFrom();
		}else if((IdUtils.GROU_TAG).equals(stem)) { // 群聊会话标志
			syncBoxMessage.convType = ConvType.GROUP;
			syncBoxMessage.convId = meta.getTo();
		}
	}

	/** 基本的消息 */
	static SyncBoxMessage getMsg(String folderId, Meta meta) {
		SyncBoxMessage syncBoxMessage = new SyncBoxMessage() ;
		syncBoxMessage.msgId = meta.getId();
		syncBoxMessage.fromuid = meta.getFrom();
		syncBoxMessage.time = (long) meta.getTime() * 1000;
		setBasicType(folderId,meta,syncBoxMessage);
		return syncBoxMessage;
	}

	/** 文件消息*/
	static void getFileMsg(SyncBoxMessage syncBoxMessage, Meta meta) {
		syncBoxMessage.fileId = meta.getContent().toStringUtf8();
		syncBoxMessage.thumbData = null;
		if (null != meta.getThumbnail().toByteArray()) {
			syncBoxMessage.thumbData = meta.getThumbnail().toByteArray();
		}
		syncBoxMessage.fileName = meta.getFileName();
		syncBoxMessage.fileLength = meta.getFileLength();
		syncBoxMessage.fileLimit = meta.getFileLimit();
	}

}
