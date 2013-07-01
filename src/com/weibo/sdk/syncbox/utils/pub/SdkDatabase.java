package com.weibo.sdk.syncbox.utils.pub;

import java.util.LinkedList;

import com.weibo.sdk.syncbox.type.pub.FixMetaInfo;

/**
 * 历史数据的接口，继承这个接口可以避免消息的重复拉取以及丢消息
 * @author liuzhao
 */
public interface SdkDatabase {
	
	/**
	 * 插入历史数据ID
	 * @param msgId 消息ID
	 * @param folderId 消息的组ID，可以作为索引
	 * @param timestamp 时间戳
	 */
	void insertHistoryId(String msgId,String folderId,int timestamp) throws Exception;
	
	/**
	 * 查询历史数据
	 * @param msgId 消息ID
	 * @param folderId 消息的组ID
	 * @return true 查询成功
	 */
	boolean containHistoryId(String msgId,String folderId)  throws Exception;

	/**
	 * 插入修复期间的meta，覆盖写
	 * @param msgId 消息ID
	 * @param folderId 消息的组ID，可以作为索引
	 * @param fixMeta byte数组
	 */
	void insertFixMeta(String msgId,String folderId,byte[] fixMeta) throws Exception;

	/** 查询修复状态*/
	boolean containFixMeta(String msgId,String folderId) throws Exception;
	
	/**
	 * 获取修复期间的所有meta
	 * @return LinkedList<FixMetaInfo>
	 */
	LinkedList<FixMetaInfo> getFixMetaAll() throws Exception;
	
	/**
	 * 删除修复期间的meta
	 * @param msgId 消息ID
	 * @param folderId 消息的组ID，可以作为索引
	 * @return 返回的结果
	 */
	void removeFixMeta(String msgId,String folderId) throws Exception;
	
	/**
	 * 用来回收资源
	 * 应该将数据库close放在这里
	 */
	void close() throws Exception;
}


