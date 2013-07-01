package com.wejoy.service.apphandler;

import java.util.concurrent.Semaphore;

import com.weibo.sdk.syncbox.type.pub.BoxResult;
import com.weibo.sdk.syncbox.type.pub.ErrorInfo;
import com.wejoy.util.DebugUtil;
/**
 * @author WeJoy Group
 *
 */
public class SyncUsrInfoHandler extends WeiboUserInfoHandler {
	private Semaphore sysmsgLock = null;
	
	public void process(String uid) {
		super.process(uid);
		sysmsgLock = new Semaphore(0);			
		
		try {
			sysmsgLock.acquire();
		} 
		catch (InterruptedException e) {
			DebugUtil.error("", "", e);
		}
	}
	
	@Override
	public void onSuccess(BoxResult boxResult) {
		try {
			super.onSuccess(boxResult);
		}
		finally {
			sysmsgLock.release();
		}
	}
	
	@Override
	public void onFailed(ErrorInfo info) {
		try {
			super.onFailed(info);
		}
		finally {
			sysmsgLock.release();
		}
	}
}