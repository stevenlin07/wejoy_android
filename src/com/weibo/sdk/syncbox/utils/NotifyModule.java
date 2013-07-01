package com.weibo.sdk.syncbox.utils;
 
import java.util.HashSet; 
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
 
import com.weibo.sdk.syncbox.listener.RecvListener;
import com.weibo.sdk.syncbox.listener.SendListener;
import com.weibo.sdk.syncbox.type.pub.BoxResult;
import com.weibo.sdk.syncbox.type.pub.ErrorInfo;
import com.weibo.sdk.syncbox.type.pub.EventInfo;
import com.weibo.sdk.syncbox.type.pub.SdkEvent;
import com.weibo.sdk.syncbox.type.pub.SyncBoxMessage;

/*
 * @ClassName: NotifyModule
 * @Description: SDK对客户端的通知中心
 * @author LiuZhao
 * @date 2012-9-10 上午1:21:13
 */
public enum NotifyModule {
	
	INSTANCE;

	private RecvListener recvListener;
	private static StoreModule store = StoreModule.INSTANCE;
	private static TimeModule time = TimeModule.INSTANCE;
	private ThreadPoolExecutor pool;
	private final AtomicInteger count = new AtomicInteger(0);
	
	/**短时任务池，60s未被使用会被JVM回收*/
	NotifyModule() {
		pool = new ThreadPoolExecutor(Runtime.getRuntime().availableProcessors() * 20, 
				Runtime.getRuntime().availableProcessors() * 20, 60, TimeUnit.SECONDS,
				new java.util.concurrent.LinkedBlockingQueue<Runnable>());
	}
	
	public void setRecvListener(RecvListener recvListener) {
		this.recvListener = recvListener;
	}
	 
	public void onSuccess(final BoxResult boxResult,final String withtag){
		System.out.println("withtag:"+withtag+",pool:"+pool.getPoolSize());
		final SendListener sendListener = stopListen(withtag);
		pool.submit(new Runnable(){ 
			@Override
			public void run() {
				System.out.println("pool:START:"+withtag);
				sendListener.onSuccess(boxResult);
				System.out.println("pool:STOP:"+withtag);
			}
		});
	}
	
	public void onFailed(final ErrorInfo errorInfo,final String withtag) {
		final SendListener sendListener = stopListen(withtag);
		pool.submit(new Runnable(){
			@Override
			public void run() {
				sendListener.onFailed(errorInfo);
			}
		});
	}
	
	public void onFile(final String withtag,final String fileId, final HashSet<Integer> hasSuccSend, final int limit) {
		pool.submit(new Runnable(){
			@Override
			public void run() {
				SendListener sendListener = store.listener.get(withtag);
				sendListener.onFile(fileId, hasSuccSend, limit);
			}
		});
	}
	
	public void onReceiveMsg(final SyncBoxMessage syncBoxMessage) {
		if (null == syncBoxMessage) {
			return;
		}
		pool.submit(new Runnable(){
			@Override
			public void run() {
				recvListener.onReceiveMsg(syncBoxMessage);
			}
		});
	}
 
	public void onEvent(final EventInfo eventInfo) {
		pool.submit(new Runnable(){
			@Override
			public void run() {
				recvListener.onEvent(eventInfo);
			}
		});
	}
	
	/** 发号  */
	private String getWithtag() {
		return count.getAndIncrement()+"";
	}
	
	/**开启监听，并启动超时服务，返回tag*/
	public String startListen(SendListener sendListener,int timeout, SdkEvent sdkEvent) {
		String withtag = getWithtag();
		store.listener.put(withtag, sendListener);
		time.setTimer(withtag, timeout, sdkEvent); // 定时器
		return withtag;
	}
 
	/**关闭监听，并关闭超时服务*/
	public SendListener stopListen(String withtag) { 
		SendListener sendListener = store.listener.remove(withtag);
		time.cancelTimer(withtag);
		return sendListener;
	}
}