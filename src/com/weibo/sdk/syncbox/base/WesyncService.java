package com.weibo.sdk.syncbox.base;

import java.io.File;
import java.io.FileInputStream; 
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
 
import com.weibo.sdk.syncbox.listener.SendListener;
import com.weibo.sdk.syncbox.net.NetModule.SdkRequest;
import com.weibo.sdk.syncbox.sync.SyncBuilder;
import com.weibo.sdk.syncbox.sync.SyncCoreReq;
import com.weibo.sdk.syncbox.type.GetFileInfo;
import com.weibo.sdk.syncbox.type.SendFileInfo;
import com.weibo.sdk.syncbox.type.SyncMessage.Meta;
import com.weibo.sdk.syncbox.type.pub.ConvType;
import com.weibo.sdk.syncbox.type.pub.ErrorInfo;
import com.weibo.sdk.syncbox.type.pub.ErrorType;
import com.weibo.sdk.syncbox.type.pub.MetaType;
import com.weibo.sdk.syncbox.type.pub.SdkEvent;
import com.weibo.sdk.syncbox.utils.Constansts;
import com.weibo.sdk.syncbox.utils.IdUtils; 

public class WesyncService extends BaseService {

	protected void sendAudioBase(SendListener sendListener, String touid,
			byte[] audioData, String spanId, int spanSequenceNo, boolean endTag,
			ConvType convType, String content_ext, int timeout){
		String withtag = notify.startListen(sendListener, timeout,SdkEvent.SYNC_TIMEOUT); 
		if (!net.connectByInvoke(withtag)) return;
		if (!WesyncVerify.sendAudioVerify(withtag, touid, audioData, spanId, spanSequenceNo, endTag, convType)) return;

		int spanLimit = 0;
		if (endTag) {
			spanLimit = spanSequenceNo;
		} 
		Meta meta = SyncBuilder.buildAudioMeta(withtag, auth.getUid(),
				touid, spanId, spanSequenceNo, spanLimit, content_ext, audioData);
		sendMeta(sendListener, touid, convType, meta, timeout);
	}

	protected void sendTextBase(SendListener sendListener, String touid, String text,ConvType convType,
			int timeout){ 
		String withtag = notify.startListen(sendListener, timeout,SdkEvent.SYNC_TIMEOUT); 
		if (!net.connectByInvoke(withtag)) return;
		if (!WesyncVerify.sendTextVerify(withtag, touid, text, convType)) return; 
		Meta meta = SyncBuilder.buildTextMeta(withtag, auth.getUid(), touid, text);
		sendMeta(sendListener, touid, convType, meta, timeout);
	}
	
	protected void sendMixedBase(SendListener sendListener,String touid, String text,ConvType convType,int timeout){
		String withtag = notify.startListen(sendListener, timeout,SdkEvent.SYNC_TIMEOUT); 
		if ( !net.connectByInvoke(withtag) ) return;
		if ( !WesyncVerify.sendTextVerify(withtag, touid, text, convType)) return; 
		
		Meta meta = SyncBuilder.buildMixedMeta(withtag, auth.getUid(), touid,text);
		sendMeta(sendListener, touid,convType, meta, timeout);
	}
	
	protected void sendFileMsgBase(SendListener sendListener,String fileId, String touid,ConvType convType,
			MetaType metaType, String filepath, String filename,byte[] thumbnail,int timeout) {
		String withtag = notify.startListen(sendListener, timeout,SdkEvent.SYNC_TIMEOUT); 
		if (!net.connectByInvoke(withtag) ) return;
		if (!WesyncVerify.sendFileMsgVerify(withtag, fileId, touid, convType, metaType, filepath, filename)) return;
		
		FileInfo fileInfo = divideFile(withtag,filepath, fileId);
		int limit = fileInfo.getLimit();
		Meta meta = SyncBuilder.buildFileMeta(withtag, fileId, auth.getUid(),
				touid, metaType, fileInfo.getFileLengthByte(),
				limit, filename, thumbnail);
		sendMeta(sendListener, touid, convType, meta, timeout);
	}
	
	/** 发送Meta到sendMeta缓存 */
	boolean sendMeta(SendListener sendListener, String touid,ConvType convType, Meta meta, int timeout) {
		String folderId = SyncCoreReq.createConversation(touid, convType); // getfolderId
		SyncCoreReq.sync(folderId, meta);
		return true;
	}

	protected void sendFileAllBase(SendListener sendListener, String touid, String filepath, int sliceSize, int timeout) {
		String withtag = notify.startListen(sendListener, timeout,SdkEvent.SYNC_TIMEOUT); 
		if (!net.connectByInvoke(withtag) ) return;
		if (!WesyncVerify.sendFileAllVerify(withtag, touid, filepath)) return;
		
		final String fileId = IdUtils.getFileId(auth.getUid(), touid, withtag);
		FileInfo fileInfo = divideFile(withtag, filepath, fileId, sliceSize);
		if (null == fileInfo) return;

		// 缓存所有信息用来确认发送逻辑
		int limit = fileInfo.getLimit();
		HashSet<Integer> hasSuccSend = new HashSet<Integer>();
		SendFileInfo sendFileInfo = new SendFileInfo(withtag,fileId, hasSuccSend,limit);
		store.sendFileCallback.put(fileId, sendFileInfo);
		sendFile(fileId, fileInfo.getSyncFileBuf()); 
	}

	/** 发送文件分片*/
	protected void sendFilePartBase(SendListener sendListener, String fileId,String filepath,
			HashSet<Integer> hasSuccSend, int timeout) {
		String withtag = notify.startListen(sendListener, timeout,SdkEvent.SYNC_TIMEOUT); 
		if (!net.connectByInvoke(withtag) ) return;
		if (!WesyncVerify.sendFilePartVerify(withtag, fileId, filepath,hasSuccSend)) return ;
		
		FileInfo fileInfo = divideFile(withtag,filepath, fileId);
		if (null == fileInfo) { return; }
		for (int hasSendIndex : hasSuccSend) { // 去掉已经发送过的
			fileInfo.getSyncFileBuf().remove(hasSendIndex);
		}
		SendFileInfo sendFileInfo = new SendFileInfo(withtag,fileId, hasSuccSend,fileInfo.getLimit());
		store.sendFileCallback.put(fileId, sendFileInfo);
		sendFile(fileId, fileInfo.getSyncFileBuf());
	}

	private void sendFile(String fileId, HashMap<Integer, byte[]> fileBuf) {
		for (byte[] buf : fileBuf.values()) { 
			log.info("[SENDFILE][fileId:" + fileId + ",len:" + buf.length + "]",SdkEvent.INVOKE);
			net.sendRequest(SdkRequest.WESYNC, store.uriMap.get("SendFile"),
					"SendFile", buf, null);
		}
	}

	protected void getFileAllBase(SendListener sendListener, String fileId, String filepath, int fileLength,int limit) {
		String withtag = notify.startListen(sendListener,setFileTimeout(fileLength),SdkEvent.GETFILE_TIMEOUT); 
		if (!net.connectByInvoke(withtag) ) return;
		if (!WesyncVerify.getFileAllVerify(withtag, fileId, filepath, fileLength)) return; 
		 
		HashSet<Integer> hasSuccRecv = new HashSet<Integer>();
		GetFileInfo getFileInfo = new GetFileInfo(withtag, fileId, hasSuccRecv,filepath, limit, fileLength);
		store.getFileCallback.put(fileId, getFileInfo);

		for (int i = 1; i <= limit; i++) {
			log.info("[GETFILE][FILEID:" + fileId + "][LIMIT:" + limit + "][INDEX:" + i + "]", SdkEvent.INVOKE);
			net.sendRequest(SdkRequest.WESYNC, store.uriMap.get("GetFile"),
					"GetFile", SyncBuilder.getFile(fileId, limit, i), null);
		}
	}

	/** 文件超时的设置*/
	private int setFileTimeout(int fileLength) {
		return fileLength * 60;
	}
	
	protected void getFilePartBase(SendListener sendListener, String fileId, String filepath, int fileLength,int limit,HashSet<Integer> hasSuccRecv) {
		String withtag = notify.startListen(sendListener,setFileTimeout(fileLength),SdkEvent.GETFILE_TIMEOUT); 
		if (!net.connectByInvoke(withtag) ) return;
		if (!WesyncVerify.getFilePartVerify(withtag, fileId, filepath, fileLength, hasSuccRecv)) return;
		
		GetFileInfo getFileInfo = new GetFileInfo(withtag,fileId,hasSuccRecv,filepath, limit, fileLength);
		store.getFileCallback.put(fileId, getFileInfo);
		
		for (int i=1;i<= limit;i++) {
			if (!hasSuccRecv.contains(i)) {
				log.info("[GETFILE][FILEID:" + fileId + "][LIMIT:" + limit + "][INDEX:" + i + "]", SdkEvent.INVOKE);
				net.sendRequest(SdkRequest.WESYNC, store.uriMap.get("GetFile"),
						"GetFile", SyncBuilder.getFile(fileId, limit, i), null);
			}
		}
	}

	/** 切分文件 */
	private FileInfo divideFile(String withtag,String filepath,String fileId) {
		return divideFile(withtag, filepath, fileId, 0);
	}
	
	/** 切分文件 */
	private FileInfo divideFile(String withtag,String filepath,String fileId, int sliceSize) {
		FileInfo fileInfo = null;
		FileInputStream fis = null;
		HashMap<Integer,byte[]> syncFileBuf = new HashMap<Integer,byte[]>();
		try {
			fis = new FileInputStream(new File(filepath));
			sliceSize = Constansts.SLICE_SIZE;
			int fileLengthByte = fis.available();
			if (0 >= fileLengthByte) {
				log.error("[ERROR][SENDFILE][the file is empty]",SdkEvent.SENDFILE_READ_EMPTY, null);
				return null;
			}
			int remainder = (int) ((fileLengthByte) % sliceSize); // 分片的余数
			int limit = (0 == remainder
					? (fileLengthByte / sliceSize)
					: (fileLengthByte / sliceSize + 1));// 确定分片的数量

			byte[] sendBuf = new byte[sliceSize]; // 根据分片的大小来划分分片
			byte[] lastSendBuf = new byte[fileLengthByte - (limit - 1) * sliceSize];
			for (int i = 1; i <= limit; i++) {
				if (i != limit) {
					fis.read(sendBuf);
					syncFileBuf.put(i,SyncBuilder.sendFile(i, limit, sendBuf,fileId));
				} else {
					fis.read(lastSendBuf);
					syncFileBuf.put(i,SyncBuilder.sendFile(i, limit, lastSendBuf,fileId));
				}
			}

			fileInfo = new FileInfo(fileLengthByte,limit,syncFileBuf);

		} catch (IOException e) {
			ErrorInfo errorInfo = new ErrorInfo();
			errorInfo.info = "[ERROR][SENDFILE][Can't read,maybe the path is wrong]";
			errorInfo.errorType = ErrorType.READ_ERROR;
			notify.onFailed(errorInfo, withtag);
			log.error("[ERROR][SENDFILE][Can't read,maybe the path is wrong]", SdkEvent.SENDFILE_READ_ERROR, e);
		}  finally {
			try {
				if (null != fis) {
					fis.close();
				}
			} catch (IOException e) {
				log.error("[SENDFILE][CLOSE][ERROR]",SdkEvent.SENDFILE_CLOSE_ERROR, e);
			}
		}
		return fileInfo;
	}
	
	void sendAudioAll(SendListener sendListener, String touid, byte[] audioData, String spanId, int spanSequenceNo, boolean endTag,
			ConvType convType, int timeout) {
		String withtag = notify.startListen(sendListener, timeout,SdkEvent.SYNC_TIMEOUT); 
		if (!net.connectByInvoke(withtag)) return;
		if (!WesyncVerify.sendAudioVerify(withtag, touid, audioData, spanId, spanSequenceNo, endTag, convType)) return;
		Meta meta = SyncBuilder.buildAudioMeta(withtag, auth.getUid(),touid, audioData);
		sendMeta(sendListener, touid, convType, meta, timeout);
	}
}

/**
 * @ClassName: FileInfo
 * @Description: 文件信息
 * @author liuzhao
 * @date 2013-4-11 上午10:41:42
 */
class FileInfo {

	/** 文件长度*/
	private int fileLengthByte;
	/** 文件分片总数*/
	private int limit;
	/** 缓存文件的分片信息 index-byte[] 分片序号-分片内容*/
	private HashMap<Integer,byte[]> syncFileBuf;

	public FileInfo(int fileLengthByte, int limit, HashMap<Integer,byte[]> syncFileBuf) {
		this.fileLengthByte = fileLengthByte;
		this.limit = limit;
		this.syncFileBuf = syncFileBuf;
	}

	public int getLimit() {
		return limit;
	}

	public int getFileLengthByte() {
		return fileLengthByte;
	}

	public HashMap<Integer,byte[]> getSyncFileBuf() {
		return syncFileBuf;
	}
}
