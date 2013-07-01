package test.sdk.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays; 
import java.util.HashMap;
import java.util.Locale;
 
import com.weibo.sdk.syncbox.listener.RecvListener;
import com.weibo.sdk.syncbox.type.pub.EventInfo;
import com.weibo.sdk.syncbox.type.pub.MetaType;
import com.weibo.sdk.syncbox.type.pub.SyncBoxMessage; 

public class BoxRecvListener implements RecvListener {
 
	public void onReceiveMsg(SyncBoxMessage syncMsg) {
		System.out.println("会话类型:" + syncMsg.convType.name()+",会话ID:" + syncMsg.convId);
		System.out.println("投递箱类型：" + syncMsg.deliveryBox);
		System.out.println("消息ID：" + syncMsg.msgId);
		System.out.println("发送者：" + syncMsg.fromuid);
		String timestr = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss",Locale.SIMPLIFIED_CHINESE)
			.format(new java.util.Date(syncMsg.time));
		System.out.println("发送时间：" + timestr);
		if (MetaType.text == syncMsg.metaType ) {
			System.out.println("********** 接收到一条文本消息 **********");
			System.out.println("文本内容：" + syncMsg.text);
		} else if (MetaType.mixed == syncMsg.metaType ) {
			System.out.println("********** 接收到一条富文本消息 **********");
			System.out.println("文本内容：" + syncMsg.text);
		} else if (MetaType.audio == syncMsg.metaType ) {
			System.out.println("********** 接收到一条语音消息 **********");
			receiveAll(syncMsg);
		} else if (MetaType.file == syncMsg.metaType || MetaType.video == syncMsg.metaType
				|| MetaType.image == syncMsg.metaType) {
			System.out.println("********** 接收到一条文件消息 **********");
			if (null != syncMsg.thumbData) {
				System.out.println("缩略图存在："	+ Arrays.toString(syncMsg.thumbData));
			}
			System.out.println("文件类型：" + syncMsg.metaType.name());
			System.out.println("文件的ID：" + syncMsg.fileId);
			System.out.println("文件的大小：" + syncMsg.fileLength);
			System.out.println("文件的分片总数：" + syncMsg.fileLimit);
			System.out.println("文件的名称：" + syncMsg.fileName);
		}
	}

	
	public void onEvent(EventInfo eventInfo) {
		System.out.println("********** 接收到一条SDK事件消息 **********");  
		System.out.println("info:"+ eventInfo.info); 
		System.out.println("event:"+ eventInfo.eventType);
		switch(eventInfo.eventType) { 
			case NIO_CONNECTED:break;
			case NIO_UNABLE_CONNNECT:break;
			case NIO_DISCONNECT:break;
			case AUTH_LAPSE:break; 
			case LOCAL_STORE_ERROR:break;
			default :
				break;
		}
	}

	protected void receiveAll(SyncBoxMessage audioMessage) {
		if (!spanAudioRecv(audioMessage)) { // 是语音分片
			System.out.println("处理分片语音");
			return;
		}
		System.out.println("接收到完整的语音信息");
		String filepath = audioMessage.msgId + ".amr";
		FileOutputStream fis = null;
		try {
			fis = new FileOutputStream(new File(filepath));
			fis.write(audioMessage.audioData);
			System.out.println("语音存储在：" + filepath);
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (null != fis ){
				try {
					fis.flush();
					fis.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
	public boolean spanAudioRecv(SyncBoxMessage audioMessage) {
		boolean flag = false;
		if (audioMessage.isSpanAudio) { // 是语音分片
			String spanId = audioMessage.spanId;
			int spanSeqNo = audioMessage.spanSeqNo;
			byte[] audioByte = audioMessage.audioData;
			
			// 缓存到达的语音
			// TODO 客户端可以根据这个落地存储语音分片
			HashMap<Integer,byte[]> spanSeqCache = audioSliceCache.get(spanId);
			if (null == spanSeqCache){
				spanSeqCache = new HashMap<Integer,byte[]>();
			}
			System.out.println("spanSeqNo:"+spanSeqNo);
			spanSeqCache.put(spanSeqNo, audioByte);
			audioSliceCache.put(spanId, spanSeqCache); 
			
			if(audioMessage.isLast) { // 最后一片的到达处理
				int spanLimit = audioMessage.spanLimit;
				audioSliceLimit.put(spanId, spanLimit);
				audioPartMsgId.put(spanId, audioMessage.msgId);
			}
			
			if (audioSliceLimit.containsKey(spanId)) { // 说明最后一片已经到达
				int spanLimit = audioSliceLimit.get(spanId);
				
				if (spanLimit == spanSeqCache.size()) {
					int lengthAudio = 0;
					for (byte[] audioSlice:spanSeqCache.values()){
						lengthAudio = lengthAudio + audioSlice.length;
					}
					audioMessage.audioData = new byte[lengthAudio];
					int destPos = 0;
					for (int i =1;i <= spanLimit;i++) {
						byte[] audioSlice = spanSeqCache.get(i);
						System.arraycopy(audioSlice, 0, audioMessage.audioData, destPos, audioSlice.length);
						destPos = destPos + audioSlice.length;
					}
					audioMessage.msgId = audioPartMsgId.get(spanId);
					flag = true;
					audioPartMsgId.remove(spanId);
					audioSliceCache.remove(spanId);
					audioSliceLimit.remove(spanId);
				}
			}
		} else {
			flag = true;
		}
		return flag;
	}
	
	/** spanId - sliceMap<seq,byte[]> */
	public static HashMap<String,HashMap<Integer,byte[]>> audioSliceCache = new HashMap<String,HashMap<Integer,byte[]>>();
	/** spanId - spanLimit */
	public static HashMap<String,Integer> audioSliceLimit = new HashMap<String,Integer>();
	/** spanId - msgId */
	public static HashMap<String,String> audioPartMsgId = new HashMap<String,String>();
}
