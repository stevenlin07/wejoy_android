package com.wejoy.module;

import java.io.IOException;
import java.util.Comparator;

import net.sourceforge.pinyin4j.format.exception.BadHanyuPinyinOutputFormatCombination;

import com.wejoy.ui.MainChatActivity;
import com.wejoy.util.DebugUtil;
import com.wejoy.util.HanziToPingtin;
import com.wejoy.util.JsonBuilder;
import com.wejoy.util.JsonWrapper;

public class ConvertModule {
	public static final int SINGLE_CONV = 0x10;
	public static final int GROUP_CONV = 0x01;
	public static final int READONLY_CONV = 0xff;
	
	public static final int GROUP_CONV_IMAGE = 0x02;
	public static final int GROUP_CONV_VOICE = 0x03;
	public static final int RECOMMAND = 0x10;
	
	public String convid;
	public int convtype = GROUP_CONV;

	/**
	 * 会话名字实际是分组定向微博的缩写形式，出于兼容性考虑，暂时把它单独列为一个attribute
	 */
	public String convName;
	/**
	 * 置顶话题
	 */
	public String stickyTopic;
	public long latestUpdateTime;
	public String latestUpdateUserName;
	private String latestUpdateMessage;
	
	public String getLatestUpdateMessage() {
		return latestUpdateMessage;
	}

	public void setLatestUpdateMessage(String msg) {
		this.latestUpdateMessage = msg != null && msg.length() > 10 ?
			msg.substring(0, 10) + "..." :
			(msg == null ? "" : msg);
	}

	public int unreadCount;
	public int position;
	public String convListFacePic;
	public String chatwithid;
	
	public int statusid;
	public int statususerid;
	
	public String toJson() {
		JsonBuilder json = new JsonBuilder();
		json.append("convtype", convtype);
		json.append("convName", convName);
		json.append("stickyTopic", stickyTopic);
		json.append("latestUpdateTime", latestUpdateTime);
		json.append("latestUpdateUserName", latestUpdateUserName);
		json.append("latestUpdateMessage", latestUpdateMessage);
		json.append("unreadCount", unreadCount);
		json.append("convListFacePic", convListFacePic);
		json.append("convid", convid);
		json.append("chatwithid", chatwithid);
		json.append("position", position);
		
		return json.flip().toString();
	}
	
	public static ConvertModule parseJson(String json) {
		JsonWrapper jw;
		ConvertModule cm = new ConvertModule();
		
		try {
			jw = new JsonWrapper(json);
			cm.convListFacePic = jw.get("convListFacePic");
			cm.unreadCount = jw.getInt("unreadCount");
			cm.latestUpdateMessage = jw.get("latestUpdateMessage");
			cm.latestUpdateUserName = jw.get("latestUpdateUserName");
			cm.latestUpdateTime = jw.getLong("latestUpdateTime");
			cm.stickyTopic = jw.get("stickyTopic");
			cm.convName = jw.get("convName");
			cm.convtype = jw.getInt("convtype");
			cm.convid = jw.get("convid");
			cm.chatwithid = jw.get("chatwithid");
			cm.position = jw.getInt("position");
		} 
		catch (IOException e) {
			DebugUtil.debug("ConverModule", "parseJson failed caused by " + e.getMessage(), e);
		}
		
		return cm;
	}
	
	public static class ConvertComparator implements Comparator {
		public int compare(Object arg0, Object arg1) {
			ConvertModule c0 = (ConvertModule) arg0;
			ConvertModule c1 = (ConvertModule) arg1;
			
			if(c0.position != c1.position) {
				return  c1.position - c0.position;
			}
			else {
				return 0;
			}
		}
	}
}
