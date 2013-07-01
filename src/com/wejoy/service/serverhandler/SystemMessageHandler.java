package com.wejoy.service.serverhandler;

import java.io.IOException;
import java.util.concurrent.Semaphore;

import com.weibo.sdk.syncbox.type.pub.BoxResult;
import com.weibo.sdk.syncbox.type.pub.ErrorInfo;
import com.weibo.sdk.syncbox.type.pub.SyncBoxMessage;
import com.wejoy.module.ChatMessage;
import com.wejoy.module.ContactModule;
import com.wejoy.module.ConvertModule;
import com.wejoy.service.WeJoyServiceConstants;
import com.wejoy.service.WeJoyServiceOpType;
import com.wejoy.service.apphandler.SyncUsrInfoHandler;
import com.wejoy.service.apphandler.WeiboUserInfoHandler;
import com.wejoy.store.DataStore;
import com.wejoy.util.DebugUtil;
import com.wejoy.util.JsonWrapper;

public class SystemMessageHandler extends MessageHandler {
	GroupManagmentHandler groupManagementHandler = new GroupManagmentHandler();
	
	protected void insertChatMessage(String fromuid, SyncBoxMessage syncMsg) {
		ChatMessage chat = getAbstractChat(fromuid, syncMsg);
		chat.direction = ChatMessage.MESSAGE_SYSTEM;
		chat.content = syncMsg.text;
		
		try {
			processSystemMsg(chat, syncMsg);
		}
		catch(Exception e) {
			DebugUtil.error("", "", e);
		}
	}
	
	protected void updateConversation(String fromuid, SyncBoxMessage syncMsg) {
		// do nothing
	}
	
	private void processSystemMsg(ChatMessage chat, SyncBoxMessage syncMsg) throws IOException {
		JsonWrapper json = new JsonWrapper(syncMsg.text);
		String key = json.get("key");
		WeJoyServiceOpType optype = WeJoyServiceOpType.valueOfStr(key);
		
		switch(optype) {
		case addGroupMember :
		case delGroupMember :
			String gid = json.get("gid");
			String members = json.get("members");
			String operator = json.get("operator");
			String memberNames = getMemberNames(members);
			String operatorName = getUserName(operator);
			
			chat.convid = gid;
			chat.content = getUserMessage(operatorName, optype, memberNames);
			
			if(chat.content != null && chat.convid != null) {
				ConvertModule conv = DataStore.getInstance().getConvertById(chat.convid);
				
				if(conv != null) {
					chat.direction = ChatMessage.MESSAGE_SYSTEM;
					DataStore.getInstance().insertChatMessage(chat);
					
					updateConvert(conv, syncMsg.text);
					DataStore.getInstance().updateConvert(conv);
				}
			}
			
			groupManagementHandler.process(gid, members, optype);
			
			break;
		case quitGroup :
			DebugUtil.debug("not support quite group so far");
			break;
		case UNKNOWN :
			if("newGroup".equals(key)) {
				// ignore
			}
			
			break;
		default:
			break;
		}
	}
	
	private String getUserMessage(String operatorName, WeJoyServiceOpType optype, String memberNames) {
		if(operatorName == null || memberNames == null) {
			return null;
		}
		
		String op = null;
		
		switch(optype) {
		case addGroupMember :
			op = " 把$name$ 邀请进了群";
			break;
		case delGroupMember :
			op = " 把 $name$ 请出了群";
			break;
		default :
			break;
		}
		
		String msg = op == null ? null : operatorName + op.replace("$name$", memberNames);
		return msg;
	}
	
	private String getUserName(String ouid) {
		ContactModule c = DataStore.getInstance().queryContactByContactId(ouid);
		
		if(c == null) {
			SyncUsrInfoHandler handler = new SyncUsrInfoHandler();
			handler.process(ouid);
		}
		
		c = DataStore.getInstance().queryContactByContactId(ouid);
		
		return c == null ? null : c.name;
	}
	
	private String getMemberNames(String members) {
		StringBuilder name = null;
		
		if(members != null || members.indexOf(",") > 0) {
			String[] memberIds = members.split(",");
			
			for(int i = 0; i < memberIds.length; i++) {
				String uid = memberIds[i];
				ContactModule c = DataStore.getInstance().queryContactByContactId(uid);
				
				if(c == null) {
					SyncUsrInfoHandler handler = new SyncUsrInfoHandler();
					handler.process(uid);
				}
				
				c = DataStore.getInstance().queryContactByContactId(uid);
				
				if(c != null && c.name != null) {
					if(name == null) {
						name = new StringBuilder();
					}
					
					name.append(c.name);
					
					if(i < memberIds.length - 1) {
						name.append(",");
					}
				}
			}
			
			return name == null ? null : name.toString();
		}
		
		return null;
	}
	
	/**
	 * //给非群主成员发消息
	public static void sendMessage2GroupMembers(HashSet<String> uidsSet, String fromuid, 
		String statusId, String gid)
	{
		JsonBuilder builder = new JsonBuilder();
		int num = uidsSet.size();
		String message = String.format("%s邀请您加入此聊天群", fromuid);
		builder.append("key", "newGroup");
		builder.append("message", message);
		builder.append("gid", gid);
		builder.append("statusId", statusId);
		builder.append("num", num);
		String content = builder.flip().toString();
		
		WeJoyServiceOpType
		
		
		WeJoyServiceOpType.addGroupMember
		
	String content = getRecommend(reco, RECONUM);
				builder.append("type", RECO);
				builder.append("statusIds", content);
				builder.append("uid", uid);
				wesyncInstance.sendToUser(SYSTEM, uid, builder.flip().toString());
	 */
}
