package com.wejoy.util;

/*
 * @author WeJoy Group
 */
public class JsonBuilder {
	
	private StringBuilder sb;
	private boolean flip = false;
	
	public JsonBuilder() {
		sb = new StringBuilder();
		sb.append("{");
	}
	
	public JsonBuilder(int initCapacity) {
		sb = new StringBuilder(initCapacity);
		sb.append("{");
	}
	
	public JsonBuilder(String content, boolean flip) {
		sb = new StringBuilder(content);
		this.flip = flip;
	}
	
	public JsonBuilder(JsonBuilder json) {
		sb = new StringBuilder(json.sb);
		flip = json.flip;
	}
	
	public JsonBuilder append(String name, String value) {
		if (name == null || value == null)
			return this;
		
		if (sb.length() > 1)
			sb.append(",");
		sb.append("\"").append(name).append("\":\"").append(toJsonStr(value)).append("\"");
		return this;
	}
	
	public JsonBuilder append(String name, long value) {
		if (sb.length() > 1)
			sb.append(",");
		sb.append("\"").append(name).append("\":").append(value);
		return this;
	}
	
	public JsonBuilder append(String name, int value) {
		if (sb.length() > 1)
			sb.append(",");
		sb.append("\"").append(name).append("\":").append(value);
		return this;
	}
	
	public JsonBuilder append(String name, JsonBuilder value) {
		return appendJsonValue(name, value == null ? null : value.toString());
	}
	
	public JsonBuilder appendJsonValue(String name, String jsonValue) {
		if (name == null || jsonValue == null)
			return this;
		
		if (sb.length() > 1)
			sb.append(",");
		sb.append("\"").append(name).append("\":").append(jsonValue);
		return this;
	}
	
	public JsonBuilder flip() {
		sb.append('}');
		flip = true;
		return this;
	}
	
	public JsonBuilder reset() {
		sb.setLength(0);
		sb.append("{");
		return this;
	}
	
	public static String toJsonStr(String value) {
		if (value == null)
			return null;
		boolean valid = true;
		for (int i = 0; i < value.length(); i++) {
			char c = value.charAt(i);
			if (c < 32 || c == '"' || c == '\\' || c == '\n' || c == '\r' || c == '\t' || c == '\f' || c == '\b') {
				valid = false;
				break;
			}
		}
		if (valid)
			return value;
		
		StringBuilder buf = new StringBuilder(value.length());
		for (int i = 0; i < value.length(); i++) {
			char c = value.charAt(i);
            switch(c) {
                case '"':
                	buf.append("\\\"");
                    break;
                case '\\':
                    buf.append("\\\\");
                    break;
                case '\n':
                    buf.append("\\n");
                    break;
                case '\r':
                    buf.append("\\r");
                    break;
                case '\t':
                    buf.append("\\t");
                    break;
                case '\f':
                    buf.append("\\f");
                    break;
                case '\b':
                    buf.append("\\b");
                    break;
                    
                default:
                	if (c < 32) {
                		buf.append("\\u00");
                		String str = Integer.toHexString(c);
                		if (str.length() == 1)
                			buf.append('0');
                		buf.append(str);
                	} else {
                		buf.append(c);
                	}
            }
		}
		return buf.toString();
	}

	@Override
	public String toString() {
		if (!flip)
			flip();
		return sb.toString();
	}
}