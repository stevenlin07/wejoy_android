package com.wejoy.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import android.app.ActivityManager;
import android.app.Service;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;

import com.weibo.sdk.syncbox.BoxInstance;
import com.weibo.sdk.syncbox.listener.RecvListener;
import com.weibo.sdk.syncbox.type.pub.EventInfo;
import com.weibo.sdk.syncbox.type.pub.MetaType;
import com.weibo.sdk.syncbox.type.pub.SdkInfo;
import com.weibo.sdk.syncbox.type.pub.SyncBoxMessage;
import com.wejoy.common.MediaManager;
import com.wejoy.module.ConvertModule;
import com.wejoy.sdk.service.DebugLog;
import com.wejoy.sdk.service.SqliteModule;
import com.wejoy.service.serverhandler.AudioMessageHandler;
import com.wejoy.service.serverhandler.ImageMessageHandler;
import com.wejoy.service.serverhandler.MessageHandler;
import com.wejoy.service.serverhandler.MessageNotifyHandler;
import com.wejoy.service.serverhandler.SystemMessageHandler;
import com.wejoy.service.serverhandler.TextMessageHandler;
import com.wejoy.util.BroadcastId;
import com.wejoy.util.MD5;
/**
 * 
 * @author WeJoy Group
 *
 */
public class WeJoyService extends Service implements RecvListener {
	private static ActivityManager activityManager = null;
	private static WeJoyService instance = null;
	private TextMessageHandler textMsgHandler = new TextMessageHandler();
	private SystemMessageHandler systemMsgHandler = new SystemMessageHandler();
	private AudioMessageHandler audioMsgHandler = new AudioMessageHandler();
	private ImageMessageHandler imageMsgHandler = new ImageMessageHandler();
	
	private MessageNotifyHandler notifyHandler = new MessageNotifyHandler(this);
	public BoxInstance wejoy = BoxInstance.getInstance();
	
	@Override
    public int onStartCommand(Intent intent, int flags, int startId) {
		try {
			wejoy.register(new SdkInfo("android","ANDROID-MEYOU-TEST",(byte) 10,(byte) 10),
				new DebugLog(), new SqliteModule(), this);
		} catch (Exception e) {
			e.printStackTrace();
		} 
		
    	activityManager = (ActivityManager) this.getSystemService(Context.ACTIVITY_SERVICE); 
    	return super.onStartCommand(intent, flags, startId);
	}
	
	/** 发送消息广播*/
	public void sendMsgBroadcast() {
		Intent intent = new Intent();
		intent.setAction(BroadcastId.CONV_UPDATE);
		sendBroadcast(intent);
	}
	
	
	// @by jichao, 暂时使用单线程, 后续有可能增加多线程处理
	@Override
	public void onReceiveMsg(SyncBoxMessage syncMsg) {
		// @by jichao, 因为Android实际上不存在离线推送，如果是后台，显示通知之后，其他项目都需要照常更新
		if(isBackground(WeJoyService.this)) {
			notifyHandler.process(syncMsg);
		}
		
		if (MetaType.text == syncMsg.metaType ) {
			System.out.println("********** 接收到一条文本消息 **********");
			System.out.println("文本内容：" + syncMsg.text);
			String fromuid = syncMsg.fromuid;
			
			if(WeJoyServiceConstants.SYSEM_COUNT_ID.equals(fromuid)) {
				systemMsgHandler.process(fromuid, syncMsg);
			}
			else {
				textMsgHandler.process(fromuid, syncMsg);
			}
		} 
		else if (MetaType.mixed == syncMsg.metaType ) {
			System.out.println("********** 接收到一条富文本消息 **********");
			System.out.println("文本内容：" + syncMsg.text);
		} 
		else if (MetaType.audio == syncMsg.metaType ) {
			System.out.println("********** 接收到一条语音消息 **********");
			String fromuid = syncMsg.fromuid;
			audioMsgHandler.process(fromuid, syncMsg);
		} 
		else if (MetaType.file == syncMsg.metaType || MetaType.video == syncMsg.metaType
				|| MetaType.image == syncMsg.metaType) {
			System.out.println("********** 接收到一条文件消息 **********");
			if (null != syncMsg.thumbData) {
				System.out.println("缩略图存在：");
			}
			System.out.println("文件类型：" + syncMsg.metaType.name());
			System.out.println("文件的ID：" + syncMsg.fileId);
			System.out.println("文件的大小：" + syncMsg.fileLength);
			System.out.println("文件的分片总数：" + syncMsg.fileLimit);
			System.out.println("文件的名称：" + syncMsg.fileName);
			
			String fromuid = syncMsg.fromuid;
			imageMsgHandler.process(fromuid, syncMsg);
		}
		
		sendMsgBroadcast(); // 发送变更广播
	}
	
	private void updateConvBySyncMsg(ConvertModule conv, SyncBoxMessage syncMsg) {
		conv.setLatestUpdateMessage(syncMsg.text);
		conv.latestUpdateTime = System.currentTimeMillis();
		conv.unreadCount ++;
	}
	
	private void printDebugInfo(SyncBoxMessage syncMsg) {
		System.out.println("会话类型:" + syncMsg.convType.name()+",会话ID:" + syncMsg.convId);
		System.out.println("投递箱类型：" + syncMsg.deliveryBox);
		System.out.println("消息ID：" + syncMsg.msgId);
		System.out.println("发送者：" + syncMsg.fromuid);
		String timestr = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss",Locale.SIMPLIFIED_CHINESE)
			.format(new java.util.Date(syncMsg.time));
		System.out.println("发送时间：" + timestr);
	}

	@Override
	public void onEvent(EventInfo eventInfo) {
		switch(eventInfo.eventType) { 
		case NIO_CONNECTED:
//			if(DataStore.getInstance().queryOwnerUser() == null) {
//				WeiboUserInfoHandler usrInfoHandler = new WeiboUserInfoHandler();
//				usrInfoHandler.process(CommonUtil.getCurrUserId());
//			}
			
		break;
		case NIO_UNABLE_CONNNECT:break;
		case NIO_DISCONNECT:break;
		case AUTH_LAPSE:break; 
		case LOCAL_STORE_ERROR:break;
		default :
			break;
		}
	}

	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return null;
	}
	
	public static boolean isBackground(Context context) {
		List<RunningAppProcessInfo> appProcesses = activityManager.getRunningAppProcesses();
		
		for (RunningAppProcessInfo appProcess : appProcesses) {
			if (appProcess.processName.equals(context.getPackageName())) {
				
				if(appProcess.importance == RunningAppProcessInfo.IMPORTANCE_BACKGROUND) {
					System.out.println("Background App:" + appProcess.processName);
					return true;
				}
				else {
					System.out.println("Foreground App:" + appProcess.processName);
					return false;
				}
			}
		}
		
		return false;
	}
}
