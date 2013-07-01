package com.weibo.sdk.syncbox.net;

import java.util.concurrent.atomic.AtomicBoolean;

import com.google.protobuf.ByteString; 
import com.weibo.sdk.syncbox.type.NioMessage;
import com.weibo.sdk.syncbox.type.pub.APNType;
import com.weibo.sdk.syncbox.type.pub.ErrorInfo;
import com.weibo.sdk.syncbox.type.pub.ErrorType;
import com.weibo.sdk.syncbox.type.pub.SdkEvent; 
import com.weibo.sdk.syncbox.utils.AuthModule;
import com.weibo.sdk.syncbox.utils.Constansts;
import com.weibo.sdk.syncbox.utils.LogModule; 
import com.weibo.sdk.syncbox.utils.NotifyModule;
import com.weibo.sdk.syncbox.utils.ReqId;
import com.weibo.sdk.syncbox.utils.StoreModule;
import com.weibo.sdk.syncbox.utils.MessageBlockingQueue.MessageEntity;

/**
 * @ClassName: NetModule
 * @Description: 网络的管理
 * @author liuzhao
 * @date 2013-4-17 下午5:46:15
 */
public enum NetModule {

	INSTANCE;
	
	private static LogModule log = LogModule.INSTANCE;
	private static StoreModule store = StoreModule.INSTANCE;
	private static NotifyModule notify = NotifyModule.INSTANCE;
	private static AuthModule auth = AuthModule.INSTANCE;
	private static NioCore nio = NioCore.INSTANCE;
	
	private NetHandler netHandler;
	public AtomicBoolean nioConnected = new AtomicBoolean(false); 
	private int apnType = APNType.APNTYPE_NONE;
	

	public NetHandler getNetHandler() {
		return netHandler;
	}

	public void setNetHandler(NetHandler netHandler) {
		this.netHandler = netHandler;
	}

	public void clear() {
		log.info("[NetModule][STOP]",SdkEvent.INVOKE);
		nio.stop();
		this.nioConnected.getAndSet(false);
	}

	/**
	 * 用于区分请求
	 * @author liuzhao
	 *
	 */
	public enum SdkRequest {
	    HEARTBEAT(0x001),
		CLOSE (0x011),
		WESYNC (0x101),
		HANDSHAKE (0x111),
		NOTICE (0x112),
		CONTACT (0x141),
		PLUGIN (0x181),
		APPPROXY(0x184);

		private final int value;

		private SdkRequest(int value) {
			this.value = value;
		}

		public int get() {
			return value;
		}

		public static SdkRequest valueOf(final int code) {
			for (SdkRequest t : SdkRequest.values()) {
				if (code == t.value) return t;
			}
			return null;
		}
	}

	/** 发送请求*/
	public boolean sendRequest(SdkRequest sdkRequest,String uri,String callbackId, byte[] content, byte[] attach) {
		LogModule.INSTANCE.info("[NetModule][sendRequest][callbackId "+callbackId+"]",SdkEvent.INVOKE);
		MessageEntity messageEntity = new MessageEntity(uri,buildNioReq(callbackId,sdkRequest.get(),content,attach));
		if("SendFile".equals(callbackId)){
			return store.nioSendQueue.putBlock(messageEntity);
		} else {
			return store.nioSendQueue.putMsg(messageEntity);
		}
	} 
	
	/** 连接网络并发送请求 */
	public synchronized boolean startNio(String callbackId, String authorization) {
		if (this.nioConnected.get()) {
			log.info("[NET IS CONNECTED]", SdkEvent.INVOKE);
			return true;
		}
		byte[] authorBody = buildNioReq(callbackId, SdkRequest.WESYNC.get(), null, null);
		String uri = store.uriMap.get("GetItemUnread");
		nio.start(Constansts.SERV_URL_NIO_IP, Constansts.SERV_URL_NIO_PORT, authorization, uri, authorBody);
		return this.nioConnected.get();
	}

	/** 包装网关包 */
	private byte[] buildNioReq(String callbackId, int sort, byte[] content, byte[] attach) {
		NioMessage.NioPacket.Builder meyouPacketBuild = NioMessage.NioPacket
				.newBuilder().setCallbackId(callbackId).setSort(sort);
		if (null != content) meyouPacketBuild.setContent(ByteString.copyFrom(content));
		if (null != attach) meyouPacketBuild.setAttache(ByteString.copyFrom(attach));
		return meyouPacketBuild.build().toByteArray();
	}
	
	/** SDK调用引起的网络连接 */
	public boolean connectByInvoke(String withtag) {
		boolean flag = connectByRetry();
		if (!flag) connectFail(withtag); 
		return flag;
	}

	/** OnResume和网络变化引发的网络连接 */
	public boolean connectByRetry() {
		boolean flag = false;
		if (auth.getMauthHeader() != null) { // 如果不为空，说明之前登录成功过 
			flag = startNio(ReqId.OnInvoke_MAuth, auth.getMauthHeader());
		} else if (auth.getAuthHead() != null ){ // 如果为空，说明之前没登录，或者登录失败
			flag = startNio(ReqId.OnInvoke_Auth, auth.getAuthHead());
		}
		return flag;
	}

	/** 网络重连*/
	public void resetConnect() {
		clear() ;
		connectByRetry();
	}
	
	/** 登录失败 */
	public void connectFail(String withtag) {
		ErrorInfo errorInfo = new ErrorInfo();
		errorInfo.info = "网络无法联通";
		errorInfo.errorType = ErrorType.NET_UNCONNECTED;
		notify.onFailed(errorInfo, withtag);
	}
	
	/** 网络情况变更*/
	public void netChange(boolean isConnected, int apnTypeNew) {
		if (auth.getUid() == null) return;
		if (this.nioConnected.get()) {
			if (isConnected) {
				if ( apnType == apnTypeNew ) {

				} else { // restart
					resetConnect();
				}
			} else {
				clear(); // stop
			}
		} else {
			if (isConnected) { // start
				connectByRetry();
			}
		}
		apnType = apnTypeNew;
	}
 
}

