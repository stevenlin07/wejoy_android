package com.wejoy.store;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Bitmap.CompressFormat;
import android.os.Environment;

import com.wejoy.module.ChatMessage;
import com.wejoy.module.ContactModule;
import com.wejoy.net.WeJoyHTTPManager;
import com.wejoy.store.ImageStore.OnImageLoadListener;
import com.wejoy.util.DebugUtil;
import com.wejoy.util.MD5;
import com.wejoy.util.SDCardUtil;

/**
 * 
 * @author WeJoy Group
 *
 */
public class AudioStore implements StoreConstant {
	private static AudioStore instance = null;
	public static final String AUDIO_STORE = STORE_ROOT + File.separator + "temp" + File.separator +
		"audio" + File.separator;
	public static final String WEJOY_AUDIO_PREFIX = "msg_";
	private static final String OUT_STORE_ROOT = Environment.getExternalStorageDirectory() + File.separator;
	
	public static AudioStore getInstance() {
		if(instance == null) {
			instance = new AudioStore();
		}
		
		return instance;
	}
	
	/**
	 * 
	 * @param convid
	 * @param fromuid
	 * @param touid
	 * @return audio chat message
	 */
	public ChatMessage getNewAudioChat(String convid, String touid) {
		ChatMessage chat = new ChatMessage(convid, ChatMessage.MESSAGE_TO, ChatMessage.TYPE_AUDIO, null);
		ContactModule sender = DataStore.getInstance().getOwnerUser();
		chat.uid = sender.contactId;
		chat.userFaceUrl = sender.faceurl;
		chat.usrname = sender.name;
		chat.attachPath = getAudioFullPath(convid);
		chat.created_at = System.currentTimeMillis();
		chat.msgid = String.valueOf(chat.created_at);
		chat.sendState = ChatMessage.TO_SENDING;
		
		return chat;
	}
	
	/**
	 * save audio message and return message path
	 */
	public String saveAudioMessage(String msgId, String convid, byte[] data) {
		String filename = MD5.getMD5(msgId); 
		String path = getAudioFullPath(convid, filename);
		FileOutputStream fis = null;
		
		try {
			fis = new FileOutputStream(new File(path));
			fis.write(data);
		} 
		catch (Exception e) {
			e.printStackTrace();
		} 
		finally {
			if (null != fis ){
				try {
					fis.flush();
					fis.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		
		return path;
	}
	
	public static String getAudioFullPath(String convid) {
		return getAudioFullPath(convid, null);
	}
	
	public static String getAudioFullPath(String convid, String filename) {
		String path = getAudioPathOnly(convid);

		if(SDCardUtil.isSDCardWritable()) {
			SDCardUtil.createFolder(path);
		}
		
		filename = filename == null ? 
			MD5.getMD5(String.valueOf(System.currentTimeMillis())) :
			filename;
		return path + WEJOY_AUDIO_PREFIX + filename + ".amr";
	}
	
	private static String getAudioPathOnly(String convid) {
		return OUT_STORE_ROOT + AUDIO_STORE + MD5.getMD5(convid) + File.separator;
	}
}
