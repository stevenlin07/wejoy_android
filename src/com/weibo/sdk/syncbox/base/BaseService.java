package com.weibo.sdk.syncbox.base;

import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.weibo.sdk.syncbox.handler.SdkHandler;
import com.weibo.sdk.syncbox.listener.RecvListener;
import com.weibo.sdk.syncbox.listener.SendListener;
import com.weibo.sdk.syncbox.net.NetModule;
import com.weibo.sdk.syncbox.net.NetModule.SdkRequest;
import com.weibo.sdk.syncbox.type.pub.RequestType;
import com.weibo.sdk.syncbox.type.pub.SdkEvent;
import com.weibo.sdk.syncbox.type.pub.SdkInfo;
import com.weibo.sdk.syncbox.type.pub.ServerType;
import com.weibo.sdk.syncbox.utils.AuthModule;
import com.weibo.sdk.syncbox.utils.JsonBuilder;
import com.weibo.sdk.syncbox.utils.LogModule;
import com.weibo.sdk.syncbox.utils.NotifyModule;
import com.weibo.sdk.syncbox.utils.ReqId;
import com.weibo.sdk.syncbox.utils.StoreModule;
import com.weibo.sdk.syncbox.utils.TimeModule;
import com.weibo.sdk.syncbox.utils.pub.ExportLog;
import com.weibo.sdk.syncbox.utils.pub.SdkDatabase;

/**
 * @ClassName: BasicApi
 * @Description: 基础实现类
 * @author LiuZhao
 * @date 2012-10-23 下午10:18:46
 */
public class BaseService {

	protected static LogModule log = LogModule.INSTANCE;
	protected static StoreModule store = StoreModule.INSTANCE;
	protected static NetModule net = NetModule.INSTANCE;
	protected static AuthModule auth = AuthModule.INSTANCE;
	protected static TimeModule time = TimeModule.INSTANCE;
	protected static NotifyModule notify = NotifyModule.INSTANCE;
	protected static ThreadPoolExecutor pool;

	protected BaseService() {
		net.setNetHandler(new SdkHandler());
		pool = new ThreadPoolExecutor(Runtime.getRuntime()
				.availableProcessors() * 2, Runtime.getRuntime()
				.availableProcessors() * 4, 60, TimeUnit.SECONDS,
				new java.util.concurrent.LinkedBlockingQueue<Runnable>());
	}

	/** 初始化资源 */
	public void register(SdkInfo sdkInfo, ExportLog exportLog, SdkDatabase sdkDatabase, RecvListener recvListener)
			throws Exception {
		log.setExportLog(exportLog);
		store.setSdkDatabase(sdkDatabase);
		notify.setRecvListener(recvListener);
		BaseVerify.sdkInfoVerify(sdkInfo);
	}

	/** 认证登录 */
	protected void loginOnCreate(SendListener sendListener, AuthType authType, String token, String username,
			String password, int timeout) {
		String withtag = notify.startListen(sendListener, timeout, SdkEvent.AUTH_TIMEOUT);
		String authHeader = null;
		if (AuthType.BASIC == authType) {
			if (!BaseVerify.registerBasicVerify(withtag, username, password)) return;
			authHeader = auth.setBasicToken(username, password);
		} else if (AuthType.OAUTH == authType) {
			if (!BaseVerify.registerOAuthVerify(withtag, token)) return;
			authHeader = auth.setOatuhToken(token);
		}
		if (null == authHeader) return;
		if (!net.startNio(ReqId.OnCreate_Auth + ":" + withtag, authHeader)) {
			net.connectFail(withtag);
		}
	}

	/** 重连登录 */
	protected void loginOnResumeBase(AuthType authType, String token, String username,
			String password, String uid) {
		if (isNull("uid", uid)) return;
		if (auth.getUid() == null || auth.getAuthHead() == null) { // 说明没有登录过
			auth.setUid(uid);
			if (AuthType.BASIC == authType ) {
				if (isNull("username", username) || isNull("password", password) ) return;
				auth.setBasicToken(username, password);
			} else if (AuthType.OAUTH == authType){
				if (isNull("token", token)) return;
				auth.setOatuhToken(token);
			}	
			time.startHeartbeat(); // 启动心跳管理，并发送通知，单位秒，包括：多少秒后启动，以后每多少秒执行一次
		}
		pool.submit(new Runnable() {
			@Override
			public void run() {
				net.connectByRetry();
			}
		});
	}
	
	/**true 是空的*/
	private boolean isNull(String paramName, Object param) {
		if (null == param){
			log.error("["+paramName+" is null]", SdkEvent.PARAMS_ERROR, null);
			return true;
		}
		return false;
	}

	public void logout() {
		log.info("[logout]", SdkEvent.INVOKE);
		try {
			NetModule.INSTANCE.clear(); // 网络资源清理
			Thread.sleep(1000);
			AuthModule.INSTANCE.clear(); // AuthModlue模块清空
			Thread.sleep(1000);
			StoreModule.INSTANCE.clear(); // SDK缓存清理
			Thread.sleep(1000);
			TimeModule.INSTANCE.clear(); // 定时器管理模块
		} catch (InterruptedException e) {
			LogModule.INSTANCE.error("[LOGOUT][Interrupted]",
					SdkEvent.ITERRUPTED_EXCEPTION, e);
		}
	}

	public void proxyInterfaceBase(SendListener sendListener, String appKey, String url, String params, RequestType requestType, ServerType serverType, String fileParam, byte[] fileData, int timeout) {
		String withtag = notify.startListen(sendListener, timeout, SdkEvent.PROXYINTERFACE_TIMEOUT);
		if (!net.connectByInvoke(withtag)) return;
		if (!BaseVerify.proxyInterfaceVerify(withtag, appKey, url, requestType, serverType)) return;

		JsonBuilder json = new JsonBuilder();
		json.append("url", url).append("requestType", requestType.get())
				.append("serverType", serverType.get());
		if (ServerType.weiboPlatform == serverType) {
			json.append("source", appKey);
		}
		if (null != params) {
			json.append("params", params);
		}
		if (null != fileParam) {
			json.append("fileParam", fileParam);
		}
		net.sendRequest(SdkRequest.APPPROXY, null, withtag, json.flip()
				.toString().getBytes(), fileData);
	}
	
	protected void wejoyInterfaceBase(SendListener sendListener, String requestString, int timeout) {
		String withtag = notify.startListen(sendListener, timeout,SdkEvent.WEJOYINTERFACE_TIMEOUT); 
		if (!net.connectByInvoke(withtag)) return;
		net.sendRequest(SdkRequest.PLUGIN, null, withtag, requestString.getBytes(), null);	
	}
	
	/**
	 * @ClassName: AuthType
	 * @Description: 认证类型
	 * @author liuzhao
	 * @date 2013-5-30 下午3:12:12
	 */
	protected enum AuthType {
		BASIC, OAUTH;
	}

}
