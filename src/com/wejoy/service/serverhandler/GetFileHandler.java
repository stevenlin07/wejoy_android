package com.wejoy.service.serverhandler;

import java.util.HashSet;

import com.weibo.sdk.syncbox.BoxInstance;
import com.weibo.sdk.syncbox.listener.SendListener;
import com.weibo.sdk.syncbox.type.pub.BoxResult;
import com.weibo.sdk.syncbox.type.pub.ErrorInfo;
import com.wejoy.module.ChatMessage;
import com.wejoy.service.apphandler.SendMessageHandler;
import com.wejoy.store.DataStore;
import com.wejoy.ui.helper.ImageUpDownLoadAnimaHanlder;
/**
 * 
 * @author WeJoy Group
 *
 */
public class GetFileHandler implements SendListener{
	BoxInstance wejoy = BoxInstance.getInstance();
	ImageUpDownLoadAnimaHanlder animaHandler;
	SendMessageHandler.SendMessageFinishListener getImageFinishHandler;
	ChatMessage chat;
	String path;
	
    public void process(ChatMessage chat, String path, ImageUpDownLoadAnimaHanlder animaHandler, 
    	SendMessageHandler.SendMessageFinishListener getImageFinishHandler) 
    {
    	this.chat = chat;
    	this.path = path;
    	this.animaHandler = animaHandler;
    	this.getImageFinishHandler = getImageFinishHandler;
    	wejoy.getFile(this, chat.fileId, path, chat.fileLength, chat.fileLimit, 120);
    }

	@Override
	public void onSuccess(BoxResult boxResult) {
		System.out.println("文件fileId:"+boxResult.msgId+" 发送成功！");
		
		chat.attachPath = path;
		DataStore.getInstance().insertOrUpdateChatMessage(chat);
		
		getImageFinishHandler.onSuccess(chat);
	}

	@Override
	public void onFailed(ErrorInfo errorInfo) {
		System.out.println("[onFailed][请求失败]");
		System.out.println("info:"+errorInfo.info);
		System.out.println("errorType:"+ errorInfo.errorType);
		
		chat.attachPath = null;
	}

	@Override
	public void onFile(String fileId, HashSet<Integer> hasSuccSend, int limit) {
		System.out.println("生成的文件ID：" + fileId);
		System.out.println("该文件的分片总数：" + limit);
		System.out.println("收到的分片号：" + hasSuccSend.toString());
		double completed = (double)(hasSuccSend.size()) / (double)limit;

		animaHandler.setMax(limit);
		animaHandler.setProgress(hasSuccSend.size());

		System.out.println("下载文件:"+fileId+" 的完成度:" + completed * 100 + "%");
	}
}
