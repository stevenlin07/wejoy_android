package com.weibo.sdk.syncbox.type;

import java.io.Serializable;
import java.util.HashSet;

/**
 * @ClassName: GetFileInfo 
 * @Description: 下载文件的信息
 * @author liuzhao
 * @date 2013-4-24 下午1:31:08 
 */
public final class GetFileInfo implements Serializable{

	/** serialVersionUID*/
	private static final long serialVersionUID = -3548200029726703607L;
	private String withtag;
	/** 文件ID */
	private String fileId;
	/** 分片总数,从1计数*/
	private int limit;
	/** 已经收到的分片序号 */
	private HashSet<Integer> hasSuccRecv; 
	/** 文件长度 */
	private int fileLength;
	/** 收到的文件写入位置 */
	private String getFilePath;

	public GetFileInfo(String withtag,String fileId, HashSet<Integer> hasSuccRecv,String getFilePath,int limit,int fileLength) {
		this.withtag = withtag;
		this.fileId = fileId;
		this.hasSuccRecv = hasSuccRecv; 
		this.getFilePath = getFilePath;
		this.limit = limit;
		this.fileLength = fileLength;
	}
 
	public String getFileId() {
		return fileId;
	}
 
	public int getLimit() {
		return limit;
	}
 
	public HashSet<Integer> getHasSuccRecv() {
		return hasSuccRecv;
	}
 
	public int getFileLength() {
		return fileLength;
	}
 
	public String getGetFilePath() {
		return getFilePath;
	}

	public String getWithtag() {
		return withtag;
	}

	public void setWithtag(String withtag) {
		this.withtag = withtag;
	}


}