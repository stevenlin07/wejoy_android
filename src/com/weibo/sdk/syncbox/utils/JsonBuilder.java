package com.weibo.sdk.syncbox.utils;

/*
 * @ClassName: JsonBuilder
 * @Description: 从api-common中拆出来的JsonBuilder工具类
 * @author LiuZhao
 * @date 2012-9-13 下午11:26:21
 */
public class JsonBuilder {

	private StringBuilder sb;

	public JsonBuilder() {
		this.sb = new StringBuilder();
		this.sb.append("{");
	}

	public JsonBuilder append(String name, String value) {
		if (name == null) {
			return this;
		}
		if (this.sb.length() > 1)
			this.sb.append(",");
		this.sb.append("\"").append(name).append("\":");
		if (value != null) {
			this.sb.append("\"").append(toJsonStr(value)).append("\"");
		} else {
			this.sb.append("null");
		}
		return this;
	}

	public JsonBuilder append(String name, int value) {
		if (this.sb.length() > 1)
			this.sb.append(",");
		this.sb.append("\"").append(name).append("\":").append(value);
		return this;
	}

	public JsonBuilder flip() {
		this.sb.append('}');
		return this;
	}

	public String toString() {
		return this.sb.toString();
	}

	public static String toJsonStr(String value) {
		if (value == null) {
			return null;
		}
		StringBuilder buf = new StringBuilder(value.length());
		for (int i = 0; i < value.length(); i++) {
			char c = value.charAt(i);
			switch (c) {
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
				if ((c < ' ') || (c == ''))
					buf.append(" ");
				else
					buf.append(c);
				break;
			}
		}
		return buf.toString();
	}
}
