package com.wejoy.util;

import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.map.JsonNode;
import org.codehaus.jackson.map.JsonTypeMapper;
import org.codehaus.jackson.map.impl.NumericNode;

import com.weibo.sdk.syncbox.type.pub.SyncBoxMessage;
import com.wejoy.module.ConvertModule;

public class JsonWrapper {
	
	private static final JsonWrapper emptyJsonWrapper = new JsonWrapper((JsonNode)null);
	
	private JsonNode root;
	
	public JsonWrapper(String json) throws IOException {
		if (json != null)
			root = new JsonTypeMapper().read(new JsonFactory().createJsonParser(new StringReader(json)));
	}
	
	public JsonWrapper(JsonNode root) {
		this.root = root;
	}
	
	@Override
	public String toString() {
		return (root != null ? root.toString() : "null");
	}

	public String get(String name) {
		JsonNode node = getJsonNode(name);
		return (node == null ? null : node.getValueAsText());
	}
	
	public int getInt(String name) {
		return getInt(name, 0);
	}
	
	public int getInt(String name, int defaultValue) {
		JsonNode node = getJsonNode(name);
		if (node == null)
			return defaultValue;
		else if (node instanceof NumericNode)
			return ((NumericNode)node).getIntValue();
		else
			return str2int(node.getValueAsText(), defaultValue);
	}
	
	public long getLong(String name) {
		return getLong(name, 0);
	}
	
	public long getLong(String name, long defaultValue) {
		JsonNode node = getJsonNode(name);
		if (node == null)
			return defaultValue;
		else if (node instanceof NumericNode)
			return ((NumericNode)node).getLongValue();
		else
			return str2long(node.getValueAsText(), defaultValue);
	}
	
	public JsonWrapper getNode(String name) {
		return new JsonWrapper(getJsonNode(name));
	}
	
	public JsonWrapper getArrayNode(int idx) {
		if (root != null && root.isArray()) {
			if (idx >= 0 && idx < root.size())
				return  new JsonWrapper(root.getElementValue(idx));
		}
		return emptyJsonWrapper;
	}
	
	public String getArrayNodeValue(int idx) {
		if (root != null && root.isArray()) {
			if (idx >= 0 && idx < root.size()) {
				JsonNode node = root.getElementValue(idx);
				return (node == null ? null : node.getValueAsText());
			}
		}
		return null;
	}
	
	public int getArrayNodeIntValue(int idx, int defaultValue) {
		if (root != null && root.isArray()) {
			if (idx >= 0 && idx < root.size()) {
				JsonNode node = root.getElementValue(idx);
				if (node == null)
					return defaultValue;
				else if (node instanceof NumericNode)
					return ((NumericNode)node).getIntValue();
				else
					return str2int(node.getValueAsText(), defaultValue);
			}
		}
		return defaultValue;
	}
	
	public long getArrayNodeLongValue(int idx, long defaultValue) {
		if (root != null && root.isArray()) {
			if (idx >= 0 && idx < root.size()) {
				JsonNode node = root.getElementValue(idx);
				if (node == null)
					return defaultValue;
				else if (node instanceof NumericNode)
					return ((NumericNode)node).getLongValue();
				else
					return str2long(node.getValueAsText(), defaultValue);
			}
		}
		return defaultValue;
	}
	
	public boolean isArray() {
		return (root != null && root.isArray());
	}
	
	public int size() {
		if (root != null)
			return root.size();
		else
			return 0;
	}
	
	public Map<String, String> values() {
		Map<String, String> map = new HashMap<String, String>();
		for (Iterator<String> iterator = root.getFieldNames(); iterator.hasNext();) {
			String field = iterator.next();
			String value = null;
			JsonNode node = root.getFieldValue(field);
			if (node != null)
				value = node.getValueAsText();
			map.put(field, value);
		}
		return map;
	}
	
	public boolean isEmpty() {
		return (root == null || root.size() == 0);
	}
	
	public boolean isNull() {
		return (root == null);
	}
	
	private JsonNode getJsonNode(String name) {
		if (name == null || root == null)
			return null;
		
		JsonNode n = root.getFieldValue(name); 
		if (n != null)
			return n;
		
		JsonNode node = root;
		StringTokenizer st = new StringTokenizer(name, ".");
		while (st.hasMoreTokens()) {
			String key = st.nextToken().trim();
			if ((key == null || key.length() == 0) || (node = node.getFieldValue(key)) == null)
				return null;
		}
		return node;
	}
	
	private int str2int(String s, int defaultValue) {
		try {
			return Integer.parseInt(s);
		} catch (Exception e) {
			return defaultValue;
		}
	}
	
	private long str2long(String s, long defaultValue) {
		try {
			return Long.parseLong(s);
		} catch (Exception e) {
			try {
				double d = Double.parseDouble(s);
				return (long)d;
			} catch (Exception ex) {
				//ignore
			}
			return defaultValue;
		}
	}
	
	public static String syncmsg2string(SyncBoxMessage syncMsg) {
		JsonBuilder json = new JsonBuilder();
		json.append("convtype", ConvertModule.SINGLE_CONV);
		json.append("convName", "新会话，来自" + syncMsg.fromuid);
		json.append("stickyTopic", syncMsg.text);
		json.append("latestUpdateTime", syncMsg.time);
		json.append("latestUpdateUserName", syncMsg.fromuid);//仅仅是uid还是不行
		json.append("latestUpdateMessage", syncMsg.text);
		json.append("unreadCount", 1); //未读数不对
		json.append("convListFacePic", "http://tp4.sinaimg.cn/" + syncMsg.fromuid + "/180/40000404629/0");
		json.append("convid", Integer.valueOf(syncMsg.convId));
		json.append("chatwithid", syncMsg.fromuid);
		String tmp = json.flip().toString();
		return tmp;
	}
}
