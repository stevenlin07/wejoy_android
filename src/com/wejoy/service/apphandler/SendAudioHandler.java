package com.wejoy.service.apphandler;
 
import java.text.SimpleDateFormat;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.weibo.sdk.syncbox.BoxInstance;
import com.weibo.sdk.syncbox.listener.SendListener;
import com.weibo.sdk.syncbox.type.pub.BoxResult;
import com.weibo.sdk.syncbox.type.pub.ConvType;
import com.weibo.sdk.syncbox.type.pub.ErrorInfo;
import com.wejoy.module.ChatMessage;
import com.wejoy.module.ConvertModule;
import com.wejoy.service.apphandler.SendMessageHandler.SendMessageFinishListener;
import com.wejoy.ui.helper.WeJoyChatInterface; 
import com.wejoy.util.ElasticNetUtil;

/**
 * 
 * @author WeJoy Group
 *
 */
public class SendAudioHandler implements WeJoyChatInterface {
	private BoxInstance wesync = BoxInstance.getInstance();
	private Map<String, DoSendAudioHandler> localCache = new ConcurrentHashMap<String, DoSendAudioHandler>();
	private int totalRetryTimes = 0;
	private static final int MAX_RETRY_TIMES= 1;
	private ChatMessage chat;
	private SendMessageFinishListener listener;

	/**
	 * frameid start from 1
	 */
	public void process(ChatMessage chat, String touid, int convtype, int frameId, byte[] data,
		boolean finalFrame, SendMessageFinishListener listener)
	{
		this.chat = chat;
		this.listener = listener;
		DoSendAudioHandler handler = new DoSendAudioHandler();
		String frameIdStr = String.valueOf(frameId);
		localCache.put(frameIdStr, handler);
		
		String contentExtStr = chat.getContentExt();
		
		handler.process(touid, convtype, frameIdStr, data, contentExtStr, finalFrame);
	}
	
	public void processGroupChat(ChatMessage chat, String touid, int frameId, byte[] data,
		boolean finalFrame, SendMessageFinishListener listener)
	{
		process(chat, touid, ConvertModule.GROUP_CONV, frameId, data, finalFrame, listener);
	}
	
	public void processSingleChat(ChatMessage chat, String touid, int convtype, byte[] data, 
		SendMessageFinishListener listener) 
	{
		process(chat, touid, convtype, 1, data, true, listener);
	}

	private void processSendSuccess() {
		chat.sendState = ChatMessage.TO_SEND_SUCC;
		listener.onSuccess(chat);
	}
	
	private void processSendFailed() {
		chat.sendState = ChatMessage.TO_SEND_ERR;
		String msg = "发送失败";
		listener.onFailed(chat, msg);
	}
	
	private class DoSendAudioHandler implements SendListener {
		public byte[] data;
		public String frameId;
		public boolean finalFrame;
		public String touid;
		public ConvType convtype;
		private String content_ext;
		
		public void process(String touid, int convtypecode, String frameId, byte[] data, String content_ext, boolean finalFrame) {
			this.data = data;
			this.frameId = frameId;
			this.finalFrame = finalFrame;
			this.convtype = parseConvType(convtypecode);
			this.touid = touid;
			this.content_ext = content_ext;
			
			// @by jichao, 使用msgid作为spanid
			wesync.sendAudio(this, touid, chat.msgid, Integer.parseInt(frameId), 
				finalFrame, data, this.convtype, content_ext, ElasticNetUtil.getTimeOut());
		}	
		
	    private ConvType parseConvType(int convtype) {
			if(convtype == ConvertModule.GROUP_CONV) {
				return ConvType.GROUP;
			}
			else {
				return ConvType.SINGLE;
			}
		}
		
		@Override
		public void onSuccess(BoxResult boxResult) {
			System.out.println("[AUDIO] [onSuccess][请求成功]");
			System.out.println("是否是最后一片：" + boxResult.isLastSlice);
			System.out.println("服务端下发的msgId：" + boxResult.msgId);
			String timestr = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss",Locale.SIMPLIFIED_CHINESE)
			.format(new java.util.Date(boxResult.timestamp));
			System.out.println("操作时间：" + timestr);
			
			localCache.remove(frameId);
			
			if(boxResult.isLastSlice) {
				if(localCache.isEmpty()) {
					processSendSuccess();
				}
			}
		}
		
		@Override
		public void onFailed(ErrorInfo errorInfo) {
			System.out.println("[AUDIO] [onFailed][请求失败]" + errorInfo.info);
			System.out.println("errorType:"+ errorInfo.errorType);
			
			if(totalRetryTimes ++ > MAX_RETRY_TIMES) {
				processSendFailed();
			}
			// retry
			else {
				wesync.sendAudio(this, touid, chat.msgid, Integer.parseInt(frameId), 
					finalFrame, data, convtype, content_ext, CHAT_TIMEOUT);
			}
		}
		
		@Override
		public void onFile(String fileId, HashSet<Integer> hasSuccSend, int limit) {
			// TODO Auto-generated method stub
			
		}
	}
}