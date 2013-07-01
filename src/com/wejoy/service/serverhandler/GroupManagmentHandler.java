package com.wejoy.service.serverhandler;

import com.wejoy.module.ChatMessage;
import com.wejoy.module.ConvertModule;
import com.wejoy.service.WeJoyServiceOpType;
import com.wejoy.store.DataStore;
import com.wejoy.util.CommonUtil;
/**
 * @author WeJoy Group
 *
 */
public class GroupManagmentHandler {
	public void process(String gid, String memberstr, WeJoyServiceOpType op) {
		if(op == null || gid == null || memberstr == null) {
			return;
		}
		
		if(op == WeJoyServiceOpType.delGroupMember) {
			String[] members = memberstr.split(",");
			String currentuid = CommonUtil.getCurrUserId();
			
			// 如果是当前用户被踢出，设置会话为只读
			for(String member : members) {
				if(currentuid.equals(member)) {
					ConvertModule conv = DataStore.getInstance().getConvertById(gid);
					conv.convtype = ConvertModule.READONLY_CONV;
					DataStore.getInstance().updateConvert(conv);
					
					ChatMessage chat = new ChatMessage();
					chat.convid = gid;
					chat.content = "很遗憾，你被请出了该聊天";
					chat.created_at = System.currentTimeMillis();
					chat.msgid = String.valueOf(chat.created_at);
					chat.direction = ChatMessage.MESSAGE_SYSTEM;
					DataStore.getInstance().insertChatMessage(chat);
					break;
				}
			}
		}
	}
}
