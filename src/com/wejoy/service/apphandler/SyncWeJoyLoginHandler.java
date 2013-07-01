package com.wejoy.service.apphandler;

import java.util.HashSet;
import java.util.concurrent.Semaphore;

import android.content.Context;

import com.weibo.login.AccessTokenKeeper;
import com.weibo.login.Oauth2AccessToken;
import com.weibo.sdk.syncbox.BoxInstance;
import com.weibo.sdk.syncbox.listener.SendListener;
import com.weibo.sdk.syncbox.type.pub.BoxResult;
import com.weibo.sdk.syncbox.type.pub.ErrorInfo;
import com.weibo.sdk.syncbox.type.pub.ErrorType;
import com.weibo.sdk.syncbox.utils.AuthModule;
import com.wejoy.service.WeiboConstants;
import com.wejoy.store.DataStore;
import com.wejoy.util.DebugUtil;

public class SyncWeJoyLoginHandler implements SendListener {
	public Context context;
	private BoxInstance wejoy = BoxInstance.getInstance();
	private Semaphore sysmsgLock = null;
	private boolean success = false;
	
	public SyncWeJoyLoginHandler(Context context) {
		this.context = context;
	}
	
	public boolean process() {
		// 进行认证
		Oauth2AccessToken token = AccessTokenKeeper.readAccessToken(context);
		wejoy.login(this, token.getToken(), WeiboConstants.normal_cmd_timeout * 2);
		
		sysmsgLock = new Semaphore(0);			
		
		try {
			sysmsgLock.acquire();
		} 
		catch (InterruptedException e) {
			DebugUtil.error("", "", e);
		}
		
		return success;
	}
	
	@Override
	public void onFailed(ErrorInfo info) {
		success = false;
		try {
			// 不管是不是认证失败，重登sso
			AccessTokenKeeper.clear(context);
		}
		finally {
			sysmsgLock.release();
		}
	}
	
	@Override
	public void onFile(String arg0, HashSet<Integer> arg1, int arg2) {
		// Login don't need this callback
	}
	
	@Override
	public void onSuccess(BoxResult arg0) {
		try {
			AccessTokenKeeper.keepUid(context, arg0.uid);
			success = true;
		}
		finally {
			sysmsgLock.release();
		}
	}
}
