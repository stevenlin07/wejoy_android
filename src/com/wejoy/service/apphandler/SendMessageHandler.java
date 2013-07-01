package com.wejoy.service.apphandler;
 
import java.text.SimpleDateFormat;
import java.util.HashSet;
import java.util.Locale;

import com.weibo.sdk.syncbox.BoxInstance;
import com.weibo.sdk.syncbox.listener.SendListener;
import com.weibo.sdk.syncbox.type.pub.BoxResult;
import com.weibo.sdk.syncbox.type.pub.ConvType;
import com.weibo.sdk.syncbox.type.pub.ErrorInfo;
import com.wejoy.module.ChatMessage;
import com.wejoy.module.ConvertModule;
import com.wejoy.ui.helper.WeJoyChatInterface;
/**
 * @author WeJoy Group
 *
 */
public class SendMessageHandler implements SendListener, WeJoyChatInterface  {
	private final BoxInstance wejoyService = BoxInstance.getInstance();
	private SendMessageFinishListener listener;
	private ChatMessage chat;
	
    public void sendText(ChatMessage chat, String toid, int convtype, SendMessageFinishListener listener) {
    	this.listener = listener;
    	this.chat = chat;
    	wejoyService.sendText(this, toid, chat.content, parseConvType(convtype), CHAT_TIMEOUT);
    }

	@Override
	public void onSuccess(BoxResult boxResult) {
		chat.sendState = ChatMessage.TO_SEND_SUCC;
		listener.onSuccess(chat);
	}

	@Override
	public void onFailed(ErrorInfo errorInfo) {
		chat.sendState = ChatMessage.TO_SEND_ERR;
		String msg = errorInfo.info;
		listener.onFailed(chat, msg);
	}

	@Override
	public void onFile(String fileId, HashSet<Integer> hasSuccSend, int limit) {
		
	}
	
	private ConvType parseConvType(int convtype) {
		if(convtype == ConvertModule.GROUP_CONV) {
			return ConvType.GROUP;
		}
		else {
			return ConvType.SINGLE;
		}
	}
	
	public static interface SendMessageFinishListener {
		public void onSuccess(ChatMessage chat);
		public void onFailed(ChatMessage chat, String msg);
	}
}