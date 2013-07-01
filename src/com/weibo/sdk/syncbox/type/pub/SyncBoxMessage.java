package com.weibo.sdk.syncbox.type.pub;

/**
 * 通过Sync方式获得的消息体 message from other
 * @author liuzhao
 */
public final class SyncBoxMessage { //Sync 协议相关的消息体
	/** 服务端标记的消息ID*/
	public String msgId; 
	/** 消息发送者 */
	public String fromuid;
	/** 会话ID*/
	public String convId;  
	/**会话类型*/
	public ConvType convType;
	/**投递箱类型*/
	public String deliveryBox;
	/**消息类型*/
	public MetaType metaType;
	/**时间戳*/
	public long time;
	
	/** 文本内容 */
	public String text; 
	/** 扩展信息 */
	public String contentExt;
	/** 音频数据 */
	public byte[] audioData; 
	/** 是否是分片语音*/
	public boolean isSpanAudio;
	/** 音频组ID*/
	public String spanId;
	/** 音频总数*/
	public int spanLimit;
	/** 音频序号*/
	public int spanSeqNo;
	/** 是否是最后一片*/
	public boolean isLast;
	
	/** 文件的ID */
	public String fileId; 
	/** 缩略图的数据*/
	public byte[] thumbData; 
	/** 文件长度*/
	public int fileLength; 
	/** 文件名 */
	public String fileName; 
	/** 文件分片*/
	public int fileLimit; 
}