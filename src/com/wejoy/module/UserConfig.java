package com.wejoy.module;

import java.io.IOException;

import com.wejoy.util.JsonBuilder;
import com.wejoy.util.JsonWrapper;
/**
 * 
 * @author WeJoy Group
 *
 */
public class UserConfig {
	public String version = "10";
	public NotifyConfig notifyConfig = new NotifyConfig();
	public String name;
	public Sex sex;
	public String constellation;
	public String desc;
	// TO DO
//	public String account
	
	public UserConfig() {
		
	}
	
	public String toJson() {
		JsonBuilder json = new JsonBuilder();

		if(notifyConfig != null) {
			json.append(NotifyConfig.NODE_KEY, notifyConfig.toJson());
		}
		
		return json.flip().toString();
	}
	
	public void parseJson(String s) {
		try {
			JsonWrapper json = new JsonWrapper(s);
			JsonWrapper jsonnode = json.getNode(NotifyConfig.NODE_KEY);
			notifyConfig.parseJson(jsonnode);
		} 
		catch (IOException e) {
			// ignore
		}
	}
	
	public static class NotifyConfig {
		public static final String NODE_KEY = "NotifyConfig";
		private static final int HAS_NOTICE = 0x0001;
		private static final int HAS_SOUND = 0x0010;
		private static final int HAS_SHAKE = 0x0100;
		
		// 默认全都有
		private int notifyType = HAS_NOTICE | HAS_SOUND | HAS_SHAKE; 
		public String soundName;
		public String notifyTime;
		
		public boolean hasNotice() {
			return (notifyType & HAS_NOTICE) != 0;
		}
		
		public boolean hasSound() {
			return (notifyType & HAS_SOUND) != 0;
		}
		
		public boolean hasShake() {
			return (notifyType & HAS_SHAKE) != 0;
		}
		
		public void setNotice(boolean on) {
			notifyType = on ?
				notifyType | HAS_NOTICE :
				notifyType ^ HAS_NOTICE;
		}
		
		public void setSound(boolean on) {
			notifyType = on ?
				notifyType | HAS_SOUND :
				notifyType ^ HAS_SOUND;
		}
		
		public void setShake(boolean on) {
			notifyType = on ?
				notifyType | HAS_SHAKE :
				notifyType ^ HAS_SHAKE;
		}
		
		public void parseJson(JsonWrapper json) {
			notifyType = json.getInt("notifyType");
			soundName = json.get("soundName");
			notifyTime = json.get("notifyTime");
		}
		
		public JsonBuilder toJson() {
			JsonBuilder json = new JsonBuilder();
			json.append("notifyType", notifyType);
			json.append("soundName", soundName);
			json.append("notifyTime", notifyTime);
			return json;
		}
	}
}
