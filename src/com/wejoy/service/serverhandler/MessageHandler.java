package com.wejoy.service.serverhandler;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;

import com.weibo.sdk.syncbox.type.pub.BoxResult;
import com.weibo.sdk.syncbox.type.pub.ConvType;
import com.weibo.sdk.syncbox.type.pub.ErrorInfo;
import com.weibo.sdk.syncbox.type.pub.SyncBoxMessage;
import com.weibo.sdk.syncbox.utils.AuthModule;
import com.wejoy.module.ChatMessage;
import com.wejoy.module.ContactModule;
import com.wejoy.module.ConvertModule;
import com.wejoy.service.WeJoyServiceConstants;
import com.wejoy.service.apphandler.SyncGetGroupMemberHandler;
import com.wejoy.service.apphandler.SyncUsrInfoHandler;
import com.wejoy.service.apphandler.WeiboUserInfoHandler;
import com.wejoy.store.DataStore;
import com.wejoy.util.DebugUtil;
/**
 * 
 * @author WeJoy Group
 *
 */
public abstract class MessageHandler {
	public void process(String fromuid, SyncBoxMessage syncMsg) {
		updateConversation(fromuid, syncMsg);
		insertChatMessage(fromuid, syncMsg);
	}
	
	protected abstract void updateConversation(String fromuid, SyncBoxMessage syncMsg);
	
	protected abstract void insertChatMessage(String fromuid, SyncBoxMessage syncMsg);
	
	protected void updateConvert(ConvertModule conv, String text) {
		conv.setLatestUpdateMessage(text);
		conv.latestUpdateTime = System.currentTimeMillis();
		conv.unreadCount ++;
	}
	
	protected void insertChatMessage(ChatMessage chat, String fromuid, SyncBoxMessage syncMsg) {
		ContactModule c = DataStore.getInstance().queryContactByContactId(fromuid);
		chat.userFaceUrl = c != null ? c.faceurl : null;
		chat.direction = ChatMessage.MESSAGE_FROM;
		ContactModule curusr = DataStore.getInstance().queryContactByContactId(fromuid);
		
		if(curusr != null) {
			chat.uid = fromuid;
			chat.usrname = curusr.name;
			chat.userFaceUrl = curusr.faceurl;
		}
		else {
			SyncUsrInfoHandler handler = new SyncUsrInfoHandler();
			handler.process(fromuid);
			
			curusr = DataStore.getInstance().queryContactByContactId(fromuid);
			
			// try again
			if(curusr != null) {
				chat.uid = fromuid;
				chat.usrname = curusr.name;
				chat.userFaceUrl = curusr.faceurl;
			}
		}
		
		DataStore.getInstance().insertChatMessage(chat);
	}
	
	protected ChatMessage getAbstractChat(String fromuid, SyncBoxMessage syncMsg) {
		ChatMessage chat = new ChatMessage();
		chat.convid = syncMsg.convId;
		chat.msgid = syncMsg.msgId;
		chat.created_at = syncMsg.time;
		chat.uid = fromuid;
		
		return chat;
	}
}
