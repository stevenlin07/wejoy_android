package com.wejoy.service.serverhandler;

import java.util.HashMap;

import android.annotation.SuppressLint;

import com.weibo.sdk.syncbox.type.pub.SyncBoxMessage;
import com.wejoy.module.ChatMessage;
import com.wejoy.module.ContactModule;
import com.wejoy.module.ConvertModule;
import com.wejoy.store.AudioStore;
import com.wejoy.store.DataStore;
/**
 * @author WeJoy Group
 *
 */
public class AudioMessageHandler extends MessageHandler {
	/** spanId - sliceMap<seq,byte[]> */
	public static HashMap<String,HashMap<Integer, byte[]>> audioSliceCache = new HashMap<String,HashMap<Integer, byte[]>>();
	/** spanId - spanLimit */
	public static HashMap<String, Integer> audioSliceLimit = new HashMap<String, Integer>();
	/** spanId - msgId */
	public static HashMap<String, String> audioPartMsgId = new HashMap<String, String>();
	public static HashMap<String, ChatMessage> audioChatCache = new HashMap<String, ChatMessage>();
	
	protected void insertChatMessage(String fromuid, SyncBoxMessage syncMsg) {
		// 处理分片语音
		if(syncMsg.isSpanAudio) {
			if(recvSpanAudio(syncMsg)) {
				String cacheKey = getAudioCacheKey(syncMsg);
				ChatMessage chat = audioChatCache.get(cacheKey);
				
				String path = AudioStore.getInstance().saveAudioMessage(syncMsg.msgId, syncMsg.convId, chat.audio);
				chat.attachPath = path;
				chat.type = ChatMessage.TYPE_AUDIO;
				insertChatMessage(chat, fromuid, syncMsg);
			}
		}
		else {
			ChatMessage chat = getAbstractChat(fromuid, syncMsg);
			chat.setType(ChatMessage.TYPE_AUDIO);
			String path = AudioStore.getInstance().saveAudioMessage(syncMsg.msgId, syncMsg.convId, syncMsg.audioData);
			chat.attachPath = path;
			chat.parseContentExt(syncMsg.contentExt);
			
			// TO DO
			chat.type = ChatMessage.TYPE_AUDIO;
			insertChatMessage(chat, fromuid, syncMsg);
		}
	}
	
	// @by jichao, 即使同时收到很多消息，同一个会话下spanId重复的可能性很小
	// 【注意】不能使用msgId作为key，因为在分片投递过程中每个分片都一个新msgId
	protected void updateConversation(String fromuid, SyncBoxMessage syncMsg) {
		ConvertModule conv = DataStore.getInstance().getConvertById(syncMsg.convId);
		
		if(conv != null) {
			String s = "发来一条语音消息";
			updateConvert(conv, s);
			DataStore.getInstance().updateConvert(conv);
		}
	}
	
	private String getAudioCacheKey(SyncBoxMessage audioMessage) {
		String cacheKey = audioMessage.convId + "|" + audioMessage.spanId;
		return cacheKey;
	}
	
	@SuppressLint("UseSparseArrays")
	public boolean recvSpanAudio(SyncBoxMessage audioMessage) {
		boolean finished = false;
		
		String cacheKey = getAudioCacheKey(audioMessage);
		int spanSeqNo = audioMessage.spanSeqNo;
		byte[] audioByte = audioMessage.audioData;
		
		// 缓存到达的语音
		// TODO 客户端可以根据这个落地存储语音分片
		HashMap<Integer,byte[]> spanSeqCache = audioSliceCache.get(cacheKey);
		ChatMessage chat = audioChatCache.get(cacheKey);
		
		if (null == spanSeqCache) {
			chat = getAbstractChat(audioMessage.fromuid, audioMessage);
			chat.setType(ChatMessage.TYPE_AUDIO);
			chat.parseContentExt(audioMessage.contentExt);
			audioChatCache.put(cacheKey, chat);
			spanSeqCache = new HashMap<Integer, byte[]>();
			audioSliceCache.put(cacheKey, spanSeqCache); 
		}
		else {
			chat.parseContentExt(audioMessage.contentExt);
		}
		
		System.out.println("spanSeqNo:"+spanSeqNo);
		spanSeqCache.put(spanSeqNo, audioByte);
		
		if(audioMessage.isLast) { // 最后一片的到达处理
			int spanLimit = audioMessage.spanLimit;
			audioSliceLimit.put(cacheKey, spanLimit);
			audioPartMsgId.put(cacheKey, audioMessage.msgId);
		}
		
		if (audioSliceLimit.containsKey(cacheKey)) { // 说明最后一片已经到达
			int spanLimit = audioSliceLimit.get(cacheKey);
			
			if (spanLimit == spanSeqCache.size()) {
				int lengthAudio = 0;
				
				for (byte[] audioSlice : spanSeqCache.values()){
					lengthAudio = lengthAudio + audioSlice.length;
				}
				
				chat.audio = new byte[lengthAudio];
				
				int destPos = 0;
				
				for (int i = 1; i <= spanLimit; i++) {
					byte[] audioSlice = spanSeqCache.get(i);
					System.arraycopy(audioSlice, 0, chat.audio, destPos, audioSlice.length);
					destPos = destPos + audioSlice.length;
				}
				
				audioMessage.msgId = audioPartMsgId.get(cacheKey);
				finished = true;
				audioPartMsgId.remove(cacheKey);
				audioSliceCache.remove(cacheKey);
				audioSliceLimit.remove(cacheKey);
			}
		}
		
		return finished;
	}
}
