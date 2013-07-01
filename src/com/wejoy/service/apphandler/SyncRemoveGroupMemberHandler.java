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
 *
 */
public class SyncRemoveGroupMemberHandler implements SendListener {
	private BoxInstance wejoy = BoxInstance.getInstance();
	private String gid;
	private List<String> members;
	private Semaphore sysmsgLock = null;
	private String result;
	
	public String process(String gid, List<String> members) {
		this.gid = gid;
		this.members = members;
		// {"key":"delGroupMember","gid":"G$32Q3$32","members":"uis1,uid2,uid3"}
		
		Map<String, String> params = new HashMap<String, String>();
		StringBuilder sb = new StringBuilder();
		
		for(int i = 0; i < members.size(); i++) {
			sb.append(members.get(i));
			
			if(i < members.size() - 1) {
				sb.append(",");
			}
		}
		
		params.put("gid", gid);
		params.put("members", sb.toString());
		
		wejoy.wejoyInterface(this, WeJoyServiceOpType.parseToJson(WeJoyServiceOpType.delGroupMember, params), 
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
		sysmsgLock.release();
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
