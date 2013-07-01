package com.wejoy.service.apphandler;

import java.text.SimpleDateFormat;
import java.util.HashSet;
import java.util.Locale;

import android.graphics.Bitmap;
import android.os.Handler;

import com.weibo.sdk.syncbox.BoxInstance;
import com.weibo.sdk.syncbox.listener.SendListener;
import com.weibo.sdk.syncbox.type.pub.BoxResult;
import com.weibo.sdk.syncbox.type.pub.ConvType;
import com.weibo.sdk.syncbox.type.pub.ErrorInfo;
import com.weibo.sdk.syncbox.type.pub.MetaType;
import com.wejoy.common.MediaManager;
import com.wejoy.module.ChatMessage;
import com.wejoy.module.ConvertModule;
import com.wejoy.service.apphandler.SendMessageHandler.SendMessageFinishListener;
import com.wejoy.ui.helper.ImageUpDownLoadAnimaHanlder;
import com.wejoy.ui.helper.WeJoyChatInterface;
import com.wejoy.util.ImageUtils;
/**
 * @author WeJoy Group
 *
 */
public class SendFileHandler implements WeJoyChatInterface {
	private ChatMessage chat;
	private String touid;
	private String filepath;
	private ConvType convType;
	private SendMessageFinishListener listener;
	private String fileId;
	private Bitmap thumbnail;
	private String filename;
	private MetaType fileType;
	private final BoxInstance wejoy = BoxInstance.getInstance();
	private static final String DEFAULT_IMAGE_FILENAME = "wejoy_$time$.jpg";
	public ImageUpDownLoadAnimaHanlder uploadAnimaHandler;
	private int progress100;
	
	public void processImage(ChatMessage chat, String touid, int convtype, Bitmap thumbnail, String filename,
		SendMessageFinishListener listener) 
	{
		filename = filename == null ? 
			DEFAULT_IMAGE_FILENAME.replace("$time$", String.valueOf(System.currentTimeMillis())) :
			filename;
		process(chat, touid, chat.attachPath, convtype, thumbnail, filename, MetaType.image, listener);
	}
	
	private void process(ChatMessage chat, String touid, String filepath, int convtype, Bitmap thumbnail, String filename,
		MetaType fileType, SendMessageFinishListener listener) 
	{
		this.chat = chat;
		this.touid = touid;
		this.filepath = filepath;
		this.convType = parseConvType(convtype);
		this.listener = listener;
		this.thumbnail = thumbnail;
		this.filename = filename;
		this.fileType = fileType;
		
		FileSender fileSender = new FileSender();
		fileSender.process(touid, filepath);
	}
	
	private ConvType parseConvType(int convtype) {
		if(convtype == ConvertModule.GROUP_CONV) {
			return ConvType.GROUP;
		}
		else {
			return ConvType.SINGLE;
		}
	}
	
	private class FileSender implements SendListener {
		Handler handler = new Handler();
		
		public void process(String touid, String filepath)
		{
			wejoy.sendFile(this, touid, filepath, CHAT_TIMEOUT * 10);
		}
		
		@Override
		public void onSuccess(BoxResult boxResult) {
			if(MetaType.image == fileType) {
				byte[] thumbnailData = ImageUtils.Bitmap2Bytes(thumbnail);
				ImageFileMsgSender imageMsgSender = new ImageFileMsgSender();
				imageMsgSender.process(fileId, touid, convType, fileType, filepath, filename,
					thumbnailData, CHAT_TIMEOUT);
				
				uploadAnimaHandler.setProgress(progress100);
			}
		}
		
		@Override
		public void onFailed(ErrorInfo errorInfo) {
			MediaManager.getMediaPlayManager().removeSendFileHandler(chat.attachPath);
			
			chat.sendState = ChatMessage.TO_SEND_ERR;
			String msg = errorInfo.info;
			listener.onFailed(chat, msg);
			uploadAnimaHandler.setProgress(progress100);
		}
		
		@Override
		public void onFile(String fileId0, HashSet<Integer> hasSucc, int limit) {
			System.out.println("生成的文件ID：" + fileId);
			System.out.println("该文件的分片总数：" + limit);
			
			// important!
			fileId = fileId0;
			
			if(uploadAnimaHandler != null && hasSucc != null) {
				progress100 = limit;
				uploadAnimaHandler.setMax(limit);
				uploadAnimaHandler.setProgress(hasSucc.size());
			}
		}
	}
	
	private class ImageFileMsgSender implements SendListener {
		public void process(final String fileId, final String touid,
			final ConvType convType, final MetaType metaType, final String filepath, final String filename,
			final byte[] thumbnail, final int timeout)
		{
			wejoy.sendFileMsg(this, fileId, touid, convType, fileType, filepath, filename, thumbnail, 120);
	    }

	    @Override
		public void onSuccess(BoxResult boxResult) {
			System.out.println("[ImageFileMsgSender][请求成功]");
			System.out.println("服务端下发的msgId：" + boxResult.msgId);
			String timestr = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.SIMPLIFIED_CHINESE)
					.format(new java.util.Date(boxResult.timestamp));
			System.out.println("操作时间：" + timestr);
			
			MediaManager.getMediaPlayManager().removeSendFileHandler(chat.attachPath);
			chat.sendState = ChatMessage.TO_SEND_SUCC;
			listener.onSuccess(chat);
		}

		@Override
		public void onFailed(ErrorInfo errorInfo) {
			System.out.println("[onFailed][请求失败]");
			System.out.println("info:"+errorInfo.info);
			System.out.println("errorType:"+ errorInfo.errorType);
			
			MediaManager.getMediaPlayManager().removeSendFileHandler(chat.attachPath);
			chat.sendState = ChatMessage.TO_SEND_ERR;
			String msg = errorInfo.info;
			listener.onFailed(chat, msg);
		}

		public void onFile(String fileId, HashSet<Integer> hasSuccSend, int limit) {
		}
	}
}