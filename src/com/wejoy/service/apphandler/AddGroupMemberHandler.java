package com.wejoy.service.apphandler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import android.content.Context;

import com.weibo.sdk.syncbox.BoxInstance;
import com.weibo.sdk.syncbox.listener.SendListener;
import com.weibo.sdk.syncbox.type.pub.BoxResult;
import com.weibo.sdk.syncbox.type.pub.ErrorInfo;
import com.weibo.sdk.syncbox.utils.AuthModule;
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
public class AddGroupMemberHandler implements SendListener {
	private BoxInstance wejoy = BoxInstance.getInstance();
	public MainContactListActivity parent;
	private String gid;
	private List<String> members;
	
	public void process(String gid, List<String> members) {
		this.gid = gid;
		this.members = members;
		
		// server side params
		// String gid = json.get("gid");
		// String membersStr = json.get("members");
		
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
		
		wejoy.wejoyInterface(this, WeJoyServiceOpType.parseToJson(WeJoyServiceOpType.addGroupMember, params), 
			WeiboConstants.normal_cmd_timeout);
	}
	
	@Override
	public void onFailed(ErrorInfo info) {
		UIHelper.hideWaiting(parent);
		DebugUtil.toast(parent, info.info);
	}
	
	@Override
	public void onFile(String arg0, HashSet<Integer> arg1, int arg2) {
		
	}
	
	@Override
	public void onSuccess(BoxResult boxResult) {
		try {
			JsonWrapper jw = new JsonWrapper(boxResult.result);
			int code = jw.getInt("code");
			
			if(code != 200) {
				DebugUtil.warn("get user info failed", boxResult.result);
			}
			else {
				// return {\"code\":200,\"key\":\"" + addGroupMember +"!\"}
				DataStore ds = DataStore.getInstance();
				ds.addGroupMember(gid, members);
				ContactModule group = ds.queryGroupByContactId(gid);
				group.faceurl = getGroupFaceUrl(group.faceurl, members);
				ds.updateGroup(group);
				ConvertModule conv = ds.getConvertById(gid);
				conv.convListFacePic = group.faceurl;
				ds.updateConvert(conv);
				
				parent.serverRequestFinishHandler.post(parent.serverRequestFinishListener);
			}
		} 
		catch (IOException e1) {
			DebugUtil.error("dbwriting error", e1.toString());
			UIHelper.hideWaiting(parent);
			DebugUtil.toast(parent, "创建群失败。。。");
		}
	}
	
	private String getGroupFaceUrl(String url, List<String> members) {
		StringBuilder sb = new StringBuilder();
		int limit = Math.min(4, members.size());
		
		for(int i = 0; i < limit; i++) {
			ContactModule c = DataStore.getInstance().queryContactByContactId(members.get(i));
			sb.append(c.faceurl);
			
			if(i < limit - 1) {
				sb.append(",");
			}
		}
		
		if(limit < 4) {
			sb.append(",").append(url);
		}
		
		return sb.toString();
	}
}
