package com.weibo.sdk.syncbox.type.pub;

/**
 * @ClassName: FixMetaInfo 
 * @Description: 修复阶段的查询结果
 * @author liuzhao
 * @date 2013-4-24 下午1:29:42 
 */
public class FixMetaInfo {

	/** 会话文件夹ID*/
	private String folderId;
	/** 缓存信息*/
	private byte[] fixMetaByte;
	
	public FixMetaInfo(String folderId,byte[] fixMetaByte) {
		this.folderId = folderId;
		this.fixMetaByte = fixMetaByte;
	}
	
	public String getFolderId() {
		return folderId;
	}
	public byte[] getFixMetaByte() {
		return fixMetaByte;
	}
	
}
