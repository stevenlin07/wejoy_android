package com.wejoy.sdk.net;

import com.weibo.sdk.syncbox.BoxInstance;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo.State;
import android.widget.Toast;

/*
 * 平时，3G和WIFI都开着的时候，Android默认使用Wifi，但现实环境中不可能到处都有wifi，所以手机会经常自动切换网络
 * 有的时候，手机一开始使用wifi上网，当进入待机后10-30分钟，会自动从Wifi切换到3G网络。
 * 如果编写网络程序，网络自动切换对程序的影响是非常明显的，IP地址肯定会变化。
 * 
 */
public class NetworkStateReceiver extends BroadcastReceiver{
 
	private BoxInstance boxInstance;
	
	@Override
	public void onReceive(Context context, Intent intent) {
		try {
			String action = intent.getAction(); // 接收到的广播
			if (action.equals(ConnectivityManager.CONNECTIVITY_ACTION)){
				State state = APNUtil.getNetworkState(context);
				int netType = APNUtil.getMProxyType(context);
				Toast.makeText(context,APNUtil.getNetworkInfo(context), Toast.LENGTH_SHORT).show();
				boxInstance = BoxInstance.getInstance();
				if(State.DISCONNECTED == state || State.DISCONNECTING == state) {
	    			boxInstance.netChange(false, netType);
	    		}else if(State.CONNECTED == state){ 
	    			boxInstance.netChange(true, netType);
	    		}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
}