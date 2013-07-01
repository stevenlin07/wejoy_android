package com.weibo.sdk.syncbox.sync;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.HashSet;

import com.google.protobuf.InvalidProtocolBufferException;
import com.weibo.sdk.syncbox.type.GetFileInfo;
import com.weibo.sdk.syncbox.type.SendFileInfo;
import com.weibo.sdk.syncbox.type.SyncMessage.DataSlice;
import com.weibo.sdk.syncbox.type.SyncMessage.FileData;
import com.weibo.sdk.syncbox.type.pub.BoxResult;
import com.weibo.sdk.syncbox.type.pub.ErrorInfo;
import com.weibo.sdk.syncbox.type.pub.ErrorType;
import com.weibo.sdk.syncbox.type.pub.SdkEvent;
import com.weibo.sdk.syncbox.utils.Constansts;

public class SyncRespFile extends SyncHandler {

	/** SendFile 请求的返回结果的处理 */
	public static void sendFile(byte[] response){
		log.info("[SendFile RESP]",SdkEvent.INVOKE);
		FileData resp;
		try {
			resp = FileData.parseFrom(response);
		} catch (InvalidProtocolBufferException e) {
			log.error("[SendFile][RESP][PARSE ERROR]",SdkEvent.WESYNC_PARSE_ERROR, e);
			return;
		}
		String fileId = resp.getId();
		if(!store.sendFileCallback.containsKey(fileId)){
			log.warn("[WARN][SENDFILE][maybe it's old response or request timeout]",SdkEvent.INVOKE);
			return;
		}
		SendFileInfo sendFileInfo = store.sendFileCallback.get(fileId);
		String withtag = sendFileInfo.getWithtag();
		int limit = sendFileInfo.getLimit();
		HashSet<Integer> hasSuccSend = sendFileInfo.getHasSuccSend();
		HashSet<Integer> hasRecvTemp = new HashSet<Integer>();
		HashSet<Integer> unRecvTemp = new HashSet<Integer>();
		if(!(hasSuccSend.size() == limit) && !resp.getSliceList().isEmpty()){
			for (DataSlice s : resp.getSliceList()) { // 如果缺少分片，说明缺少列表不为空
				unRecvTemp.add(s.getIndex()); // 可能缺少的分片列表
			}
			for(int i=1;i<= sendFileInfo.getLimit();i++){
				if(!unRecvTemp.contains(i)){
					hasRecvTemp.add(i);
				}
			}
			for(int i:hasRecvTemp){
				if(!hasSuccSend.contains(i)){
					hasSuccSend.add(i);
				}
			}
		}else{ // 列表为空，表示完全收到了
			for(int i=1;i<= sendFileInfo.getLimit();i++){
				hasSuccSend.add(i); 
			}
		}
		log.info("[SendFile RESP][可能缺少的分片序号:"+unRecvTemp+"]",SdkEvent.INVOKE);// 不能在里面排重，因为这是一个批次
		
		notify.onFile(withtag, fileId, hasSuccSend, limit);
		if(hasSuccSend.size() == limit){
			store.sendFileCallback.remove(fileId);
			BoxResult boxResult = new BoxResult();
			boxResult.msgId = fileId;
			notify.onSuccess(boxResult, withtag);
		} else {
			store.sendFileCallback.put(fileId, sendFileInfo);
		}
	}

	/** GetFile 请求的返回结果的处理 */
	public static void getFile(byte[] response){
		log.info("[GetFile][RESP]",SdkEvent.INVOKE);
		FileData resp = null;
		try {
		    resp = FileData.parseFrom(response);
		} catch (InvalidProtocolBufferException e) {
			log.error("[GetFile][RESP][PARSE ERROR]",SdkEvent.WESYNC_PARSE_ERROR, e);
		    return;
		}
		String fileId = resp.getId();// 获取缓存的文件下载方式
		// 如果超时之后，说明此文件已经被清理掉了，则试图清理掉这个等待文件下载的list
//		if(!store.tagTimeList.containsKey(fileId)){
//			log.warn("[GETFILE RESP][WARN][Receive old fileId]",SdkEvent.INVOKE);
//			store.getFileCallback.remove(fileId);
//		    return;
//		}
		GetFileInfo getFileInfo = store.getFileCallback.get(fileId);
		if (null == getFileInfo) {
			log.warn("[GETFILE RESP][WARN][Receive old fileId]",SdkEvent.INVOKE);
			return;
		}
		String withtag = getFileInfo.getWithtag();
		String getFilePath = getFileInfo.getGetFilePath();
		int fileLength = getFileInfo.getFileLength();
		int limit = getFileInfo.getLimit(); // 分片总数
		HashSet<Integer> hasSuccRecv = getFileInfo.getHasSuccRecv(); 

		File downloadFile = new File(getFilePath);
		boolean unCreate = true;   // 默认是没有创建过这个文件
		if(downloadFile.exists()){ // 如果这个文件存在说明创建过
		    unCreate = false;
		}
		RandomAccessFile raf = null;
		try {
		    raf = new RandomAccessFile(downloadFile,"rw");
		    if(unCreate){
		    	raf.setLength(fileLength);
		    }
		    if(null == resp.getSliceList() || 0 == resp.getSliceList().size()){
		    	log.warn("[GETFILE RESP][CONTENT IS NULL]!",SdkEvent.INVOKE);
		    	if(null != raf){
		    		raf.close();
		    	}
		    	return;
		    }
		    for(DataSlice s : resp.getSliceList()){
		    	int index = s.getIndex();
		    	if(hasSuccRecv.contains(index)){// 说明重复已经处理
		    		continue;
		    	}
		    	raf.seek(Constansts.SLICE_SIZE*(index-1));
		    	raf.write(s.getData().toByteArray());
		    	hasSuccRecv.add(index); 
		    }
		} catch (IOException e) { // 写本地文件出错，应该直接跳出逻辑即可
			ErrorInfo errorInfo = new ErrorInfo();
			errorInfo.info = "写本地文件出错";
			errorInfo.errorType = ErrorType.WRITE_ERROR;
			notify.onFailed(errorInfo, withtag);
			log.error("[GETFILE RESP][WRITE FILE FAILED]",SdkEvent.GETFILE_WRITE_ERROR, e);
		    return;
		}finally{
			try {
				if(null != raf){
					raf.close();
				}
			} catch (IOException e) {
				log.error("[GETFILE RESP][CLOSE FILE FAILED]",SdkEvent.GETFILE_CLOSE_ERROR, e);
			}
		}
		store.getFileCallback.put(fileId, getFileInfo);
		notify.onFile(withtag, fileId, hasSuccRecv, limit);
		if ( limit <= hasSuccRecv.size()){ // 如果下载完成
			store.getFileCallback.remove(fileId); // 清理掉这个等待文件下载的list
			log.info("[GETFILE RESP][SUCC][FILEID:"+fileId+"]",SdkEvent.INVOKE);
			BoxResult boxResult = new BoxResult();
			boxResult.msgId = fileId;
			notify.onSuccess(boxResult, withtag);
		}
	}
}
