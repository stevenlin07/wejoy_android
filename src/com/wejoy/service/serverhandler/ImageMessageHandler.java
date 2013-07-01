package com.wejoy.service.serverhandler;

import com.weibo.sdk.syncbox.type.pub.ConvType;
import com.weibo.sdk.syncbox.type.pub.SyncBoxMessage;
import com.wejoy.module.ChatMessage;
import com.wejoy.module.ContactModule;
import com.wejoy.module.ConvertModule;
import com.wejoy.service.apphandler.SyncGetGroupMemberHandler;
import com.wejoy.store.DataStore;
import com.wejoy.util.DebugUtil;
import com.wejoy.util.ImageUtils;
/**
 * 
 * @author WeJoy Group
 *
 */
public class ImageMessageHandler extends MessageHandler {
	protected void insertChatMessage(String fromuid, SyncBoxMessage syncMsg) {
		ChatMessage chat = getAbstractChat(fromuid, syncMsg);
		
		// TO DO
		chat.type = ChatMessage.TYPE_PIC;
		chat.content = syncMsg.text;
		chat.thumbnail = ImageUtils.byteToBitmap(syncMsg.thumbData);
		chat.fileId= syncMsg.fileId;
		chat.fileLength = syncMsg.fileLength;
		chat.fileLimit = syncMsg.fileLimit;
		chat.fileName = syncMsg.fileName;
		
		insertChatMessage(chat, fromuid, syncMsg);
	}
	
	protected void updateConversation(String fromuid, SyncBoxMessage syncMsg) {
		ConvertModule conv = DataStore.getInstance().getConvertById(syncMsg.convId);
		
		if(conv == null) {
			conv = new ConvertModule();
			conv.convid = syncMsg.convId;
			
			if(ConvType.GROUP == syncMsg.convType) {
				conv.convtype = ConvertModule.GROUP_CONV;
				conv.chatwithid = syncMsg.convId;
			}
			else {
				// single process as group, so should not go here
				DebugUtil.error("single process as group, so should not go here");
				conv.chatwithid = fromuid;
				conv.convtype = ConvertModule.SINGLE_CONV;
			}
			
			SyncGetGroupMemberHandler getMember = new SyncGetGroupMemberHandler();
			ContactModule group = getMember.process(conv.convid);
			conv.convListFacePic = group.faceurl;
			conv.convName = group.name;
			
			updateConvert(conv, "发来一张图片");
			DataStore.getInstance().insertNewConversation(conv);
		}
		else {
			updateConvert(conv, "发来一张图片");
			DataStore.getInstance().updateConvert(conv);
		}
	}
}
