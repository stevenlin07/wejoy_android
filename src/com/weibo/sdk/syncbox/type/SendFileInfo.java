package com.weibo.sdk.syncbox.type;

import java.io.Serializable;
import java.util.HashSet;
 
/**
 * 发送文件的信息.
 * @author liuzhao
 *
 */
public final class SendFileInfo implements Serializable{

	/** serialVersionUID*/
	private static final long serialVersionUID = 921990756144040547L;
	/**客户端的发送文件标记.*/
	private String withtag;
	/**文件ID.*/
	private String fileId; 
	/**已经得到确认的分片序号.*/
	private HashSet<Integer> hasSuccSend; 
	/**发送文件的分片总数.*/
	private int limit; 


	public SendFileInfo(String withtag,String fileId ,HashSet<Integer> hasSuccSend,int limit) {
		this.withtag = withtag;
		this.fileId = fileId; 
		this.setHasSuccSend(hasSuccSend); 
		this.limit = limit;  
	}

	public String getWithtag() {
		return withtag;
	}

	public int getLimit() {
		return limit;
	}

	/**
	 * @return the hasSuccSend
	 */
	public HashSet<Integer> getHasSuccSend() {
		return hasSuccSend;
	}

	/**
	 * @param hasSuccSend the hasSuccSend to set
	 */
	public void setHasSuccSend(HashSet<Integer> hasSuccSend) {
		this.hasSuccSend = hasSuccSend;
	}

	/**
	 * @return the fileId
	 */
	public String getFileId() {
		return fileId;
	}
 
}
