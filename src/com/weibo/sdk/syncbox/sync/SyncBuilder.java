package com.weibo.sdk.syncbox.sync;

import java.util.List;

import com.google.protobuf.ByteString;
import com.weibo.sdk.syncbox.type.SyncMessage.*;
import com.weibo.sdk.syncbox.type.pub.MetaType;

public class SyncBuilder {

	public static String TAG_SYNC_KEY = "0";

	private static Meta.Builder buildMeta(String withtag,String fromuid,String touid,MetaType metaType){
		Meta.Builder metaBuilder = Meta
				.newBuilder()
				.setFrom(fromuid)
				.setId(withtag)
				.setTo(touid)
				.setTime((int)(System.currentTimeMillis()/1000))
				.setType(ByteString.copyFrom(new byte[] { (metaType.toByte()) }));
		return metaBuilder;
	}

	/** TextMeta */
	public static Meta buildTextMeta(String withtag,String fromuid,String touid,String text){
		Meta.Builder metaBuilder = buildMeta(withtag,fromuid,touid,MetaType.text);
		metaBuilder.setContent(ByteString.copyFromUtf8(text));
		return metaBuilder.build();
	}

	/** MixedMeta */
	public static Meta buildMixedMeta(String withtag,String fromuid,String touid,String text){
		Meta.Builder metaBuilder = buildMeta(withtag,fromuid,touid,MetaType.mixed);
		metaBuilder.setContent(ByteString.copyFromUtf8(text));
		return metaBuilder.build();
	}

	/** AudioMeta分片 */
	public static Meta buildAudioMeta(String withtag,String fromuid,String touid,String spanId,int spanSequenceNo,
			int sapnLimit, String content_ext, byte[] audioData){
		Meta.Builder metaBuilder = buildMeta(withtag,fromuid,touid,MetaType.audio);
		metaBuilder.setSpanId(spanId)
			.setContent(ByteString.copyFrom(audioData))
			.setSpanSequenceNo(spanSequenceNo)
			.setSpanLimit(sapnLimit)
			.setContentExt(ByteString.copyFromUtf8(content_ext));
		return metaBuilder.build();
	}

	/** AudioMeta */
	public static Meta buildAudioMeta(String withtag,String fromuid,String touid,byte[] audioData){
		Meta.Builder metaBuilder = buildMeta(withtag,fromuid,touid,MetaType.audio);
		metaBuilder.setContent(ByteString.copyFrom(audioData));
		return metaBuilder.build();
	}

	/** FileMeta */
	public static Meta buildFileMeta(String withtag,String fileId,String fromuid,String touid,MetaType metaType,int fileLength,
			int limit,String filename,byte[] thumbnail){
		Meta.Builder metaBuilder = buildMeta(withtag,fromuid,touid,metaType);
		metaBuilder.setContent(ByteString.copyFromUtf8(fileId))
			.setFileName(filename).setFileLength(fileLength).setFileLimit(limit);
		if (null != thumbnail) {
			metaBuilder.setThumbnail(ByteString.copyFrom(thumbnail));
		}
		return metaBuilder.build();
	}

	/** SendFile的请求消息体*/
	public static byte[] sendFile(int index, int limit, byte[] sendBuf, String fileId) {
		DataSlice slice = DataSlice.newBuilder().setIndex(index)
				.setLimit(limit).setData(ByteString.copyFrom(sendBuf)).build();
		FileData req = FileData.newBuilder().setId(fileId).addSlice(slice)
				.build();
		return req.toByteArray();
	}

	/** GetFile的请求消息体 */
	public static byte[] getFile(String fileId, int limit, int index) {
		DataSlice slice = DataSlice.newBuilder().setIndex(index)
				.setLimit(limit).build();
		FileData req = FileData.newBuilder().setId(fileId).addSlice(slice)
				.build();
		return req.toByteArray();
	}

	/** Sync的请求消息体 */
	public static byte[] buildSync(String folderId,boolean isFullSync,String syncKey,Meta meta){
		SyncReq.Builder syncreqBuilder = SyncReq.newBuilder()
				.setFolderId(folderId).setIsFullSync(isFullSync)
				.setKey(syncKey);
		if(null != meta){
			syncreqBuilder.setIsSendOnly(true).addClientChanges(meta);
		}
		return syncreqBuilder.build().toByteArray();
	}

	/** SyncAck的请求消息体 */
	public static byte[] buildSyncAck(String folderId, List<String> acks, boolean isSiblingInHarmony,String syncKey){
		SyncReq.Builder reqBuilder = SyncReq.newBuilder()
				.setFolderId(folderId)
				.setIsFullSync(false)
				.setIsSiblingInHarmony(isSiblingInHarmony)
				.setKey(syncKey);
		if(null != acks){
			reqBuilder.addAllSelectiveAck(acks);
		}
		return reqBuilder.build().toByteArray();
	}

}
