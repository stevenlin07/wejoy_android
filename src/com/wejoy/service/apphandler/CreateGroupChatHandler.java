package com.wejoy.service.apphandler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import android.content.Context;

import com.weibo.sdk.syncbox.BoxInstance;
import com.weibo.sdk.syncbox.listener.SendListener;
import com.weibo.sdk.syncbox.type.pub.BoxResult;
import com.weibo.sdk.syncbox.type.pub.ErrorInfo;
import com.weibo.sdk.syncbox.utils.AuthModule;
import com.wejoy.module.ChatMessage;
import com.wejoy.module.ContactModule;
import com.wejoy.module.ConvertModule;
import com.wejoy.service.WeJoyServiceOpType;
import com.wejoy.service.WeiboConstants;
import com.wejoy.store.DataStore;
import com.wejoy.ui.MainContactListActivity;
import com.wejoy.ui.helper.UIHelper;
import com.wejoy.util.DebugUtil;
import com.wejoy.util.JsonWrapper;
/**
 * @author WeJoy Group
 *
 */
public class CreateGroupChatHandler implements SendListener {
	private BoxInstance wejoy = BoxInstance.getInstance();
	private List<String> memberIds;
	public MainContactListActivity parent;
	
	public void process(List<String> memberIds) {
		this.memberIds = memberIds;
		Map<String, String> params = new HashMap<String, String>();
		StringBuilder sb = new StringBuilder();
		
		for(int i = 0; i < memberIds.size(); i++) {
			sb.append(memberIds.get(i));
			
			if(i < memberIds.size() - 1) {
				sb.append(",");
			}
		}
		
		params.put("uids", sb.toString());
		
		wejoy.wejoyInterface(this, WeJoyServiceOpType.parseToJson(WeJoyServiceOpType.createGroupChat, params), 
			WeiboConstants.normal_cmd_timeout);
	}
	
	public String getGroupMemberNameShort() {
		StringBuilder sb = new StringBuilder();
		int size = Math.min(3, memberIds.size());
		
		for(int i = 0; i < size; i++) {
			String id = memberIds.get(i);
			ContactModule c = DataStore.getInstance().queryContactByContactId(id);
			sb.append(c.name);
			
			if(i < size - 1) {
				sb.append(",");
			}
		}
		
		return sb.length() > 20 ? sb.substring(0, 20) + "..." : sb.toString();
	}
	
	public String getGroupFaceUrl() {
		StringBuilder sb = new StringBuilder();
		int size = Math.min(4, memberIds.size());
		
		for(int i = 0; i < size; i++) {
			String id = memberIds.get(i);
			ContactModule c = DataStore.getInstance().queryContactByContactId(id);
			sb.append(c.faceurl);
			
			if(i < size - 1) {
				sb.append(",");
			}
		}
		
		return sb.toString();
	}
	
	@Override
	public void onFailed(ErrorInfo info) {
		UIHelper.hideWaiting(parent);
		DebugUtil.toast(parent, info.info);
	}
	
	@Override
	public void onFile(String arg0, HashSet<Integer> arg1, int arg2) {
		
	}
	
	@Override
	public void onSuccess(BoxResult boxResult) {
		try {
			JsonWrapper jw = new JsonWrapper(boxResult.result);
			String code = jw.get("code");
			
			if(code != null) {
				DebugUtil.warn("get user info failed", boxResult.result);
			}
			else {
				// return {"key":"createGroupChat","gid":"G$1793692835$27","num":2}
				String gid = jw.get("gid");
				
				if(gid == null) {
					UIHelper.hideWaiting(parent);
					DebugUtil.toast(parent, "创建群没有成功,请再试试吧");
					return;
				}
				
				ContactModule group = new ContactModule();
				group.contactId = gid;
				group.name = getGroupMemberNameShort();
				group.contacttype = ContactModule.CONTACT_TYPE_GROUP;
				group.faceurl = getGroupFaceUrl();
				
				DataStore.getInstance().insertNewGroup(group, memberIds);
				ContactModule contact = DataStore.getInstance().getOwnerUser();
				
				String msg = "你邀请" + getGroupMemberNameShort() + "加入了群聊";
				ConvertModule conv = new ConvertModule();
				conv.stickyTopic = group.name;
				conv.convName = group.name;
				
				if(contact != null) {
					conv.latestUpdateUserName = contact.name;
				}
				
				conv.setLatestUpdateMessage(msg);
				conv.latestUpdateTime = System.currentTimeMillis();  
				conv.unreadCount = 0;  
				conv.convListFacePic = group.faceurl;
				conv.convid = group.contactId;
				conv.convtype = ConvertModule.GROUP_CONV;
				conv.chatwithid = group.contactId;
				
				DataStore.getInstance().insertNewConversation(conv);
				
				// send the creater a system message
				ChatMessage chat = new ChatMessage();
				chat.created_at = System.currentTimeMillis();
				chat.convid = conv.convid;
				chat.msgid = String.valueOf(chat.created_at);
				chat.content = msg;
				chat.direction = ChatMessage.MESSAGE_SYSTEM;
				DataStore.getInstance().insertChatMessage(chat);
				
				parent.serverRequestFinishHandler.post(parent.serverRequestFinishListener);
			}
		} 
		catch (IOException e1) {
			DebugUtil.error("dbwriting error", e1.toString());
			UIHelper.hideWaiting(parent);
			DebugUtil.toast(parent, "创建群失败。。。");
		}
	}
}
