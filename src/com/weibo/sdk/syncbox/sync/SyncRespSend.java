package com.weibo.sdk.syncbox.sync;
 
import com.google.protobuf.InvalidProtocolBufferException;  
import com.weibo.sdk.syncbox.type.SyncMessage.Meta;
import com.weibo.sdk.syncbox.type.SyncMessage.SyncResp;
import com.weibo.sdk.syncbox.type.pub.BoxResult;
import com.weibo.sdk.syncbox.type.pub.MetaType;
import com.weibo.sdk.syncbox.type.pub.SdkEvent;

public class SyncRespSend extends SyncHandler{

	/**  sendOnly 请求的返回结果的处理，绝对不会带下来结果*/
	public static void sendOnly(byte[] response) {
		log.info("[SendOnly RESP]",SdkEvent.INVOKE);
		SyncResp resp;
		try {
			resp = SyncResp.parseFrom(response);
		} catch (InvalidProtocolBufferException e) {
			log.error("[SendOnly][RESP][PARSE ERROR]",SdkEvent.WESYNC_PARSE_ERROR, e);
			return;
		}
		log.info("[SendOnly RESP][回执消息:"+ resp.getClientChangesList().size()+"]",SdkEvent.INVOKE);
		System.out.println(resp);
		for (Meta meta : resp.getClientChangesList()) {
			if (meta.hasContent() && meta.hasId()) {
				String withtag = meta.getId(); 
				BoxResult boxResult = new BoxResult();
				boxResult.msgId =  meta.getContent().toStringUtf8();
				boxResult.timestamp = (long) meta.getTime() * 1000;
				boxResult.metaType = MetaType.valueOf( meta.getType().byteAt(0) );
				if (MetaType.audio == boxResult.metaType && meta.hasSpanId() ) {
					if (meta.hasSpanLimit() && meta.getSpanLimit() != 0) {
						boxResult.isLastSlice = true; // 最后一片
					}
				}
				if (MetaType.subfolder == boxResult.metaType) {
					continue;
				}
				notify.onSuccess(boxResult, withtag);
			}
		}
	}
}