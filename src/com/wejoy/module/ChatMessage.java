package com.wejoy.module;

import java.io.IOException;
import java.util.Comparator;

import com.wejoy.util.DebugUtil;
import com.wejoy.util.JsonBuilder;
import com.wejoy.util.JsonWrapper;

import android.graphics.Bitmap;

/**
 * 
 * @author wejoy group
 *
 */
public class ChatMessage {
	private static final String AUDIO_TIME_KEY = "audioTime";
	private static final String ATTACH_PATH = "attachpath";
	private static final String FILE_ID_KEY = "fileId";
	private static final String FILE_LENGTH_KEY = "fileLength";
	private static final String FILE_LIMIT_KEY = "fileLimit";
	private static final String FILE_NAME_KEY = "fileName";
	
	/**
	 * direction
	 */
	public static final int MESSAGE_FROM = 0;
	public static final int MESSAGE_TO = 1;
	public static final int MESSAGE_SYSTEM = 3;
	public static final int MESSAGE_TIME_INFO = 4;
	/**
	 * Message type
	 */
	public static final int TYPE_TEXT = 0;
	public static final int TYPE_PIC = 1;
	public static final int TYPE_AUDIO = 2;
	/**
	 * Default has 3 types of send state
	 */
	public static final int TO_SEND_SUCC = 0x00;
	public static final int TO_SEND_ERR = 0x01;
	public static final int TO_SENDING = 0x10;
	
	public static final int FROM_UNREADED = 0;
	public static final int FROM_READED = 1;
	
	public long id;
	public int direction;
	public int type;
	public String content;
	public String uid;
	public String usrname;
	public String userFaceUrl;
	public int audioTime;
	public int sendState;
	public long created_at;
	public String msgid;
	public String convid;

	// attach id to audio in convattch table
	public String attachId;
	// attach path to pic
	public String attachPath;
	// 因为图片比较大，不能直接显示原始图片，图片只存缩略图图，点击图片查看大图才去文件中取
	public Bitmap thumbnail;
	//　声音文件比较小，可以直接存储
	public byte[] audio;
	
	// file relative parameters
	public String fileId;
	public int fileLength;
	public int fileLimit;
	public String fileName;
	
	public ChatMessage() {
		
	}
	
	public ChatMessage(String convid, int direction, int type, String content) {
		super();
		
		this.convid = convid;
		this.direction = direction;
		this.type = type;
		this.content = content;
		
		this.created_at = System.currentTimeMillis();
	}

	public int getDirection() {
		return direction;
	}

	public void setDirection(int direction) {
		this.direction = direction;
	}
	
	public int getType() {
		return type;
	}
	
	public void setType(int type) {
		this.type = type;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public CharSequence getContent() {
		return content;
	}
	
	public void setPic(Bitmap thumbnail){
		this.thumbnail = thumbnail;
	}
	
	public Bitmap getPic(){
		return thumbnail;
	}
	
	public void setUid(String uid){
		this.uid = uid;
	}
	
	public String getUid(){
		return uid;
	}
	
	public void setUrl(String url){
		this.userFaceUrl = url;
	}
	
	public String getUrl(){
		return userFaceUrl;
	}
	
	public void setAudioTime(int audioTime){
		this.audioTime = audioTime;
	}
	
	public int getAudioTime(){
		return audioTime;
	}
	
	public long getCreatedAt(){
		return created_at;
	}
	
	public String getContentExt() {
		JsonBuilder json = new JsonBuilder();
		
		if(type == ChatMessage.TYPE_AUDIO) {
			json.append(AUDIO_TIME_KEY, audioTime);
			json.append(ATTACH_PATH, attachPath);
		}
		else if(type == ChatMessage.TYPE_PIC) {
			json.append(ATTACH_PATH, attachPath);
			json.append(FILE_ID_KEY, fileId);
			json.append(FILE_LENGTH_KEY, fileLength);
			json.append(FILE_LIMIT_KEY, fileLimit);
			json.append(FILE_NAME_KEY, fileName);
		}
		
		String content_ext = json.flip().toString();
		return content_ext;
	}
	
	public void parseContentExt(String ext) {
		if(ext == null) {
			return;
		}
		
		try {
			JsonWrapper json = new JsonWrapper(ext);
			
			if(type == ChatMessage.TYPE_AUDIO) {
				audioTime = json.getInt(AUDIO_TIME_KEY);
				attachPath = json.get(ATTACH_PATH);
			}
			else if(type == ChatMessage.TYPE_PIC) {
				attachPath = json.get(ATTACH_PATH);
				fileId = json.get(FILE_ID_KEY);
				fileLength = json.getInt(FILE_LENGTH_KEY);
				fileLimit = json.getInt(FILE_LIMIT_KEY);
				fileName = json.get(FILE_NAME_KEY);
			}
		} 
		catch (IOException e) {
			DebugUtil.error("", "", e);
		}
	}
	
	public String toJson() {
		JsonBuilder json = new JsonBuilder();
		json.append("direction", direction);
		json.append("type", type);
		json.append("content", content);
		json.append("attachPath", attachPath);
		json.append("audioTime", audioTime);
		json.append("sendSucc", sendState);
		json.append("created_at", created_at);
		
		return json.flip().toString();
	}
	
	public static ChatMessage parseJson(String json) {
		JsonWrapper jw;
		ChatMessage cm = new ChatMessage();
		
		try {
			jw = new JsonWrapper(json);
			cm.direction = jw.getInt("direction");
			cm.type = jw.getInt("type");
			cm.content = jw.get("content");
			cm.attachPath = jw.get("attachPath");
			cm.audioTime = jw.getInt("audioTime");
			cm.sendState = jw.getInt("sendSucc");
			cm.created_at = jw.getInt("created_at");
		} 
		catch (IOException e) {
			DebugUtil.debug("ConverModule", "parseJson failed caused by " + e.getMessage(), e);
		}
		
		return cm;
	}
	
	public ChatMessage clone() {
		ChatMessage newchat = new ChatMessage();
		newchat.attachId = attachId;
		newchat.attachPath = attachPath;
		newchat.audio = audio;
		newchat.audioTime = audioTime;
		newchat.content = content;
		newchat.convid = convid;
		newchat.created_at = created_at;
		newchat.direction = direction;
		// don't clone id?
		// newchat.id
		newchat.msgid = msgid;
		newchat.sendState = sendState;
		newchat.thumbnail = thumbnail;
		newchat.type = type;
		newchat.uid = uid;
		newchat.userFaceUrl = userFaceUrl;
		newchat.usrname = usrname;
		newchat.fileId = fileId;
		newchat.fileLength = fileLength;
		newchat.fileLimit = fileLimit;
		newchat.fileName = fileName;
		
		return newchat;
	}
	
	public void cloneFrom(ChatMessage source) {
		attachId = source.attachId;
		attachPath = source.attachPath;
		audio = source.audio;
		audioTime = source.audioTime;
		content = source.content;
		convid = source.convid;
		created_at = source.created_at;
		direction = source.direction;
		// don't clone id?
		// id
		msgid = source.msgid;
		sendState = source.sendState;
		thumbnail = source.thumbnail;
		type = source.type;
		uid = source.uid;
		userFaceUrl = source.userFaceUrl;
		usrname = source.usrname;
		fileId = source.fileId;
		fileLength = source.fileLength;
		fileLimit = source.fileLimit;
		fileName = source.fileName;
	}	
	
	public static class CreateTimeComparator implements Comparator {
		public int compare(Object arg0, Object arg1) {
			ChatMessage c0 = (ChatMessage) arg0;
			ChatMessage c1 = (ChatMessage) arg1;
			
			return (int) (c0.created_at - c1.created_at);
		}
	}
	
	public static class MsgIdComparator implements Comparator {
		public int compare(Object arg0, Object arg1) {
			ChatMessage c0 = (ChatMessage) arg0;
			ChatMessage c1 = (ChatMessage) arg1;
			
			if(c0.msgid != null && c1.msgid != null) {
				return c0.msgid.compareTo(c1.msgid);
			}
			else {
				return 1;
			}
		}
	}
}