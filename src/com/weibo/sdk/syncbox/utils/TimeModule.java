package com.weibo.sdk.syncbox.utils;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.weibo.sdk.syncbox.net.NetModule;
import com.weibo.sdk.syncbox.net.NetModule.SdkRequest;
import com.weibo.sdk.syncbox.sync.SyncRespFix;
import com.weibo.sdk.syncbox.type.pub.ErrorInfo;
import com.weibo.sdk.syncbox.type.pub.ErrorType;
import com.weibo.sdk.syncbox.type.pub.SdkEvent;

/**
 * @ClassName: TimeModule
 * @Description: 时间管理模块
 * @author liuzhao
 * @date 2013-4-17 下午5:46:48
 */
public enum TimeModule {

	INSTANCE;
	
	private ScheduledExecutorService scheduledTimer;
	private static StoreModule store = StoreModule.INSTANCE;
	private static LogModule log = LogModule.INSTANCE;
	private static NetModule net = NetModule.INSTANCE;
	private static Timer heartbeat = new Timer();
	private HeartBeatTask heartBeatTask ;
	private static NotifyModule notify = NotifyModule.INSTANCE;

	TimeModule() { // 启动定时器
		scheduledTimer = Executors.newScheduledThreadPool(Constansts.TIME_THREAD_POOL); // 启动发送消息的线程池
	}
	
	/**
	 * 退出时间管理模块
	 */
	public void clear(){
		if (null != heartBeatTask) {
			heartBeatTask.cancel();
			heartBeatTask = null;
		}
		if (null != heartbeat) {
			heartbeat.cancel();
			heartbeat = null;
		}
	}

	/**
	 * 设定并启动定时器
	 * @param withtag 标记
	 * @param timeout 超时时间
	 */
	public void setTimer(final String withtag, int timeout,final SdkEvent sdkEvent) {
		TimerTask task = new TimerTask() {
			public void run() {
				System.out.println("add test");
				log.error(withtag,sdkEvent, null);
				store.tagTimeList.remove(withtag);
				if (SdkEvent.HEARTBEAT_TIMEOUT == sdkEvent) { // 重连网络
					net.resetConnect();
				} else if (SdkEvent.FIXSYNC_TIMEOUT == sdkEvent) {
					SyncRespFix.fixSyncTimeout(withtag);
				} else if (SdkEvent.PROXYSYNC_TIMEOUT == sdkEvent) {
						SyncRespFix.proxyFixSyncTimeout(withtag);
				} else {
					ErrorInfo  errorInfo = new ErrorInfo();
					errorInfo.info = "请求超时";
					errorInfo.errorType = ErrorType.TIMEOUT;
					notify.onFailed(errorInfo, withtag);
				}
			}
		};
		ScheduledFuture<?> taskTimer = scheduledTimer.schedule(task,timeout,TimeUnit.SECONDS);
		store.tagTimeList.put(withtag, taskTimer);
	}


	/**
	 * 取消定时器
	 * @param withtag 标记
	 * @return true 包含这个withtag并清除成功
	 */
	public boolean cancelTimer(String withtag){
		if (store.tagTimeList.containsKey(withtag)) {// 说明接收成功
			store.tagTimeList.get(withtag).cancel(true);
			store.tagTimeList.remove(withtag);
			return true;
		} else{
			log.warn("[WARN][MISS withtag:"+withtag+"][The net is so slow!!]",SdkEvent.INVOKE);
			return false;
		}
	}

	/**
	 * 启动心跳服务
	 * @param startTime 启动时间，单位秒
	 * @param timeslice 时间间隔，单位秒
	 */
	public void startHeartbeat() {
		if (null == heartbeat){
			heartbeat = new Timer();
		}
		if (null != heartBeatTask){
			heartBeatTask.cancel();
			heartBeatTask = null;
		}
		heartBeatTask = new HeartBeatTask(Constansts.heartbeat_timeout);
		heartbeat.schedule(heartBeatTask, Constansts.START_TIMER * 1000,Constansts.SLICE_TIMER * 1000);
	}

	/**
	 * 重置定时器
	 * @param startTime
	 * @param timeslice
	 */
	public void resetHeartbeat(long startTime, long timeslice,int heartbeat_timeout) {
		if (null == heartbeat){
			heartbeat = new Timer();
		}
		if (null != heartBeatTask){
			heartBeatTask.cancel();
			heartBeatTask = null;
		}
		heartBeatTask = new HeartBeatTask(heartbeat_timeout);
		log.info("[HEARTBEAT RESTART]", SdkEvent.INVOKE);
		heartbeat.schedule(heartBeatTask, startTime * 1000,timeslice * 1000);
	}

	class HeartBeatTask extends TimerTask {

		int heartbeat_timeout;

		public HeartBeatTask(int heartbeat_timeout){
			this.heartbeat_timeout = heartbeat_timeout;
		}

		@Override
		public void run() {
			if(!net.connectByRetry()) return;
			log.info("[HEARTBEAT][REQ]",SdkEvent.INVOKE);
			net.sendRequest(SdkRequest.HEARTBEAT,null, "Heartbeat", null, null);
			setTimer(Constansts.heartbeat_withtag,heartbeat_timeout,SdkEvent.HEARTBEAT_TIMEOUT);
		}
	}
}
