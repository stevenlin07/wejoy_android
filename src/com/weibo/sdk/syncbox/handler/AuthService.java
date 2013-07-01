package com.weibo.sdk.syncbox.handler;

import com.weibo.sdk.syncbox.net.NetModule; 
import com.weibo.sdk.syncbox.service.SyncService;
import com.weibo.sdk.syncbox.type.NioMessage.NioPacket;
import com.weibo.sdk.syncbox.type.pub.BoxResult;
import com.weibo.sdk.syncbox.type.pub.ErrorInfo;
import com.weibo.sdk.syncbox.type.pub.ErrorType;
import com.weibo.sdk.syncbox.type.pub.EventInfo;
import com.weibo.sdk.syncbox.type.pub.EventType;
import com.weibo.sdk.syncbox.type.pub.SdkEvent;
import com.weibo.sdk.syncbox.utils.AuthModule;
import com.weibo.sdk.syncbox.utils.LogModule;
import com.weibo.sdk.syncbox.utils.NotifyModule;
import com.weibo.sdk.syncbox.utils.ReqId;
import com.weibo.sdk.syncbox.utils.TimeModule;
 
public class AuthService {

	private static AuthModule auth = AuthModule.INSTANCE;
	private static TimeModule time = TimeModule.INSTANCE;
	private static NotifyModule notify = NotifyModule.INSTANCE;
	private static NetModule net = NetModule.INSTANCE;
	private static LogModule log = LogModule.INSTANCE;
	
	public static boolean process(String callbackId, byte[] response, NioPacket packet) {
		boolean flag = false;
		int code = packet.getCode();
		if ((callbackId.startsWith(ReqId.OnCreate_Auth))){ // 登录login调用的返回
			String[] splite = callbackId.split(":");
			String withtag = splite[1];
			if (code == 200) {
				log.info("[uid="+packet.getUid()+"][mtoken="+packet.getText()+"]", SdkEvent.INVOKE);
				auth.setUid(packet.getUid()); // 存储该用户的UID
				auth.setMauthToken(packet.getText()); // 存储该用户的MTOKEN
				time.startHeartbeat(); // 启动心跳管理，并发送通知，单位秒，包括：多少秒后启动，以后每多少秒执行一次
				SyncService.INSTANCE.loadFixMetaByDb(); // 将DB中缓存的数据推送到客户端
				BoxResult boxResult = new BoxResult();
				boxResult.uid = auth.getUid();
				notify.onSuccess(boxResult, withtag);
				flag = true;
			} else {
				ErrorInfo errorInfo = new ErrorInfo();
				errorInfo.info = "认证失败";
				errorInfo.errorType = ErrorType.AUTH_FAILED;
				notify.onFailed(errorInfo, withtag);
			}
		} else if ((ReqId.OnInvoke_Auth).equals(callbackId)) {
			if (code == 200) {
				auth.setUid(packet.getUid()); // 存储该用户的UID
				auth.setMauthToken(packet.getText()); // 存储该用户的MTOKEN
				SyncService.INSTANCE.loadFixMetaByDb(); // 将DB中缓存的数据推送到客户端
				flag = true;
			} else {
				auth.setMauthToken(null); // 置空当前的mtoken
				authLapse();
			}
		} else if ((ReqId.OnInvoke_MAuth).equals(callbackId)) {
			if (code == 200) {
				auth.setMauthToken(packet.getText()); // 获取刷新的token
				flag = true;
			} else if (code == 901) { // 该Mtoken过期，需要刷新该token
				auth.setMauthToken(null); // 置空当前的mtoken
				net.resetConnect(); // 认证过期，切断网络，重新进行认证，此时的状态：mtoken为空&&网络中断 -> 使用authHeader重连
			} else {
				auth.setMauthToken(null); // 置空当前的mtoken
				authLapse();
			} 
		} 
		return flag;
	}
	
	private static void authLapse() {
		EventInfo eventInfo = new EventInfo();
		eventInfo.info = "认证无效!请重新登录";
		eventInfo.eventType = EventType.AUTH_LAPSE;
		notify.onEvent(eventInfo);
	}
	
}
