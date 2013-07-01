package com.weibo.sdk.syncbox.type.pub;

/**
 * @ClassName: BoxResult
 * @Description: 请求的回调结果
 * @author liuzhao
 * @date 2013-5-11 上午2:21:30
 */
public class BoxResult {
	/** 服务器下发的msgId */
	public String msgId;
	/** 服务器下发的时间戳 */
	public long timestamp;
	/** 消息的类型*/
	public MetaType metaType;
	/** 是否是最后一片语音 */
	public boolean isLastSlice;
	/** 请求的返回结果*/
	public String result;
	/** UID*/
	public String uid;
}
