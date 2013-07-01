package com.wejoy.service.serverhandler;

import java.util.Arrays;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;

import com.weibo.sdk.syncbox.type.pub.MetaType;
import com.weibo.sdk.syncbox.type.pub.SyncBoxMessage;
import com.wejoy.R;
import com.wejoy.module.ContactModule;
import com.wejoy.module.UserConfig;
import com.wejoy.service.WeJoyServiceConstants;
import com.wejoy.store.ConfigStore;
import com.wejoy.store.DataStore;
import com.wejoy.ui.AppUtils; 

/**
 * 
 * @author WeJoy Group
 *
 */
public class MessageNotifyHandler {
	private Context context;
	private UserConfig.NotifyConfig config = null;
	
	public MessageNotifyHandler(Context context) {
		this.context = context;
		config = ConfigStore.getInstance().getUserConfig().notifyConfig;
	}
	
	public void process(SyncBoxMessage syncMsg) {
		if(config.hasNotice()) {
			String msg = getNotifyMsg(syncMsg);
			
			if(msg != null) {
				showNotification(msg);
			}
		}
	}
	
	private String getNotifyMsg(SyncBoxMessage syncMsg) {
		StringBuilder sb = new StringBuilder();
		String fromuid = syncMsg.fromuid;
		
		// @by jichao, 系统消息不通知
		if(WeJoyServiceConstants.SYSEM_COUNT_ID.equals(fromuid)) {
			return null;
		}
		
		ContactModule sender = DataStore.getInstance().queryContactByContactId(fromuid);
		
		if(sender != null) {
			sb.append(sender.name).append("发来：");
		}
		
		if (MetaType.text == syncMsg.metaType ) {
			sb.append(syncMsg.text);
		} 
		else if (MetaType.mixed == syncMsg.metaType ) {
			System.out.println("********** 接收到一条离线富文本消息 **********");
			System.out.println("文本内容：" + syncMsg.text);
		} 
		else if (MetaType.audio == syncMsg.metaType ) {
			sb.append("一条新语音消息");
		} 
		else if (MetaType.file == syncMsg.metaType || MetaType.video == syncMsg.metaType
				|| MetaType.image == syncMsg.metaType) {
			sb.append("一个新文件");
		}
		
		return sb.toString();
	}
	
	// 显示Notification
    public void showNotification(String msg) {
        // 创建一个NotificationManager的引用
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(
             android.content.Context.NOTIFICATION_SERVICE);
        
        // 定义Notification的各种属性
        Notification notification = new Notification(R.drawable.icon_80, msg, System.currentTimeMillis());
        // 将此通知放到通知栏的"Ongoing"即"正在运行"组中
        notification.flags |= Notification.FLAG_ONGOING_EVENT;
        // 点击后自动清除Notification
        notification.flags |= Notification.FLAG_AUTO_CANCEL;
        notification.flags |= Notification.FLAG_SHOW_LIGHTS;
        notification.defaults = Notification.DEFAULT_LIGHTS;
        
        if(config.hasSound()) {
        	notification.defaults = notification.defaults | Notification.DEFAULT_SOUND;
        }
        
        if(config.hasShake()) {
        	notification.defaults = notification.defaults | Notification.DEFAULT_VIBRATE;
        }
        
        notification.ledARGB = Color.BLUE;
        notification.ledOnMS = 5000;
                
        // 设置通知的事件消息
        CharSequence contentTitle = "微聚"; // 通知栏标题
        
        Intent notificationIntent = new Intent(AppUtils.context, AppUtils.context.getClass());
        notificationIntent.setAction(Intent.ACTION_MAIN);
        notificationIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        PendingIntent contentIntent = PendingIntent.getActivity(
        		AppUtils.context, 0, notificationIntent,PendingIntent.FLAG_UPDATE_CURRENT);
        notification.setLatestEventInfo(AppUtils.context, contentTitle, msg, contentIntent);
        // 把Notification传递给NotificationManager
        notificationManager.notify(0, notification);
    }
}
