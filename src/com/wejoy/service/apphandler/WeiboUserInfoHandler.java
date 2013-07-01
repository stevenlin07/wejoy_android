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
import com.wejoy.module.ContactModule;
import com.wejoy.service.WeJoyServiceOpType;
import com.wejoy.service.WeiboConstants;
import com.wejoy.store.DataStore;
import com.wejoy.util.DebugUtil;
import com.wejoy.util.JsonWrapper;
/**
 * @author WeJoy Group
 *
 */
public class WeiboUserInfoHandler implements SendListener {
	public Context context;
	private DataStore dataStore = DataStore.getInstance();
	public BoxInstance wejoy = BoxInstance.getInstance();
	private String uid;
	
	public void process(String uid) {
		wejoy = BoxInstance.getInstance();
		this.uid = uid;
		Map<String, String> params = new HashMap<String, String>();
		params.put("uid", uid);
		wejoy.wejoyInterface(this, WeJoyServiceOpType.parseToJson(WeJoyServiceOpType.userInfo, params), 
			WeiboConstants.normal_cmd_timeout);
	}
	
	@Override
	public void onFailed(ErrorInfo info) {
		if(context != null) {
			DebugUtil.toast(context, info.info);
		}
		
		DebugUtil.error(info.info);
	}
	
	@Override
	public void onFile(String arg0, HashSet<Integer> arg1, int arg2) {
		
	}
	
	@Override
	public void onSuccess(BoxResult boxResult) {
		try {
			JsonWrapper jw = new JsonWrapper(boxResult.result);
			String code = jw.get("code");
			
			if(code != null) {
				DebugUtil.warn("get user info failed", boxResult.result);
			}
			else {
				JsonWrapper node = jw.getNode("value");
				ContactModule contact = parseContact(node);
				DataStore.getInstance().insertOrUpdateContact(contact);
			}
		} 
		catch (IOException e1) {
			DebugUtil.error("dbwriting error", e1.toString());
		}
	}
	
	private ContactModule parseContact(JsonWrapper json) {
		// {"user_id":"1793692835","name":"吴际超","remark":""}
		ContactModule c = new ContactModule();
		c.contactId = json.get("user_id");
		c.name = json.get("name");
		c.faceurl = json.get("profile_image_url");
		c.description = json.get("description");
		c.contacttype = ContactModule.CONTACT_TYPE_SINGLE;
		return c;
	}
}
