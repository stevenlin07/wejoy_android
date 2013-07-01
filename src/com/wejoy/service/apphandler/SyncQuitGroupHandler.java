package com.wejoy.service.apphandler;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;

import com.weibo.sdk.syncbox.BoxInstance;
import com.weibo.sdk.syncbox.listener.SendListener;
import com.weibo.sdk.syncbox.type.pub.BoxResult;
import com.weibo.sdk.syncbox.type.pub.ErrorInfo;
import com.wejoy.module.ContactModule;
import com.wejoy.module.ConvertModule;
import com.wejoy.service.WeJoyServiceOpType;
import com.wejoy.service.WeiboConstants;
import com.wejoy.store.DataStore;
import com.wejoy.ui.MainContactListActivity;
import com.wejoy.ui.helper.UIHelper;
import com.wejoy.util.DebugUtil;
import com.wejoy.util.JsonWrapper;
/**
 * @author WeJoy Group
 * @by jichao 根据目前IM广泛使用的逻辑，比如微信，删除群聊实际上不会真的讲整个群聊删除，而只是删除者自己退出群，并从此不再接收
 * 群消息而已
 */
public class SyncQuitGroupHandler implements SendListener {
	private BoxInstance wejoy = BoxInstance.getInstance();
	private String gid;
	private String result = null;
	private Semaphore sysmsgLock = null;
	
	public String process(String gid) {
		this.gid = gid;
		Map<String, String> params = new HashMap<String, String>();
		params.put("gid", gid);
		
		wejoy.wejoyInterface(this, WeJoyServiceOpType.parseToJson(WeJoyServiceOpType.quitGroup, params), 
			WeiboConstants.normal_cmd_timeout);
		
		sysmsgLock = new Semaphore(0);			
		
		try {
			sysmsgLock.acquire();
		} 
		catch (InterruptedException e) {
			DebugUtil.error("", "", e);
		}
		
		return result;
	}
	
	@Override
	public void onFailed(ErrorInfo info) {
		try {
			result = info.info;
		}
		finally {
			sysmsgLock.release();
		}
	}
	
	@Override
	public void onFile(String arg0, HashSet<Integer> arg1, int arg2) {
		
	}
	
	@Override
	public void onSuccess(BoxResult boxResult) {
		try {
			JsonWrapper jw = new JsonWrapper(boxResult.result);
			int code = jw.getInt("code");
			// return {\"code\":200,\"key\":\"" + quitGroup +"!\"}
			result = code != 200 ? "resp code is not 200, " + boxResult.result : null;
		} 
		catch (IOException e) {
			DebugUtil.error("dbwriting error", e.toString());
			result = e.toString();
		}
		finally {
			sysmsgLock.release();
		}
	}
}
