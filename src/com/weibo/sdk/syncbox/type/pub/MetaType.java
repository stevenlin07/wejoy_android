package com.weibo.sdk.syncbox.type.pub;

public enum MetaType {
	text((byte) 0x01), 
	audio((byte) 0x02), 
	video((byte) 0x03), 
	image((byte) 0x04), 
	file((byte) 0x05),
	operation((byte) 0x07),
	location((byte) 0x08),
	property((byte) 0x09),
	html((byte) 0x10),
	mixed((byte) 0x11),
	subfolder((byte) 0x12),
	unknown((byte) 0x0);

	private final byte code;

	private MetaType(byte code) {
		this.code = code;
	}

	public byte toByte() {
		return code;
	}

	public static MetaType valueOf(final byte code) {
		for (MetaType t : MetaType.values()) {
			if (code == t.code) return t;
		}
		return unknown;
	}
}