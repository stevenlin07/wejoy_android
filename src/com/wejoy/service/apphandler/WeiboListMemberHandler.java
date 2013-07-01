package com.wejoy.service.apphandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.os.Handler;

import com.weibo.sdk.syncbox.BoxInstance;
import com.weibo.sdk.syncbox.listener.SendListener;
import com.weibo.sdk.syncbox.type.pub.BoxResult;
import com.weibo.sdk.syncbox.type.pub.ErrorInfo;
import com.wejoy.module.ContactModule;
import com.wejoy.service.WeJoyServiceOpType;
import com.wejoy.service.WeiboConstants;
import com.wejoy.store.DataStore;
import com.wejoy.ui.MainContactListActivity;
import com.wejoy.util.DebugUtil;
/**
 * This class is not used yet
 * @author WeJoy Group
 *
 */
public class WeiboListMemberHandler implements SendListener {
	public Context context;
	public DataStore dataStore;
	public BoxInstance wejoy;
	public String listId;
	public Semaphore groupMemberLoadLock;
	
	public void process() {
		Map<String, String> params = new HashMap<String, String>();
		params.put("listId", listId);
		wejoy.wejoyInterface(this, WeJoyServiceOpType.parseToJson(WeJoyServiceOpType.groupMembers, params), WeiboConstants.normal_cmd_timeout);
	}
	
	@Override
	public void onSuccess(BoxResult boxResult) {
		try {
			JSONObject json = new JSONObject(boxResult.result);
			final JSONArray ids = json.getJSONArray("group_member_ids");	
			List<ContactModule> contacts = new ArrayList<ContactModule>();
			StringBuilder faceurl = new StringBuilder();
			int facecount = 0;
			
			for(int i = 0; i < ids.length(); i++) {
				long memberId = ids.getLong(i);
				ContactModule contact = dataStore.queryContactByContactId(String.valueOf(memberId));
				
				if(contact != null) {
					if(contact != null && facecount < 4) {
						if(facecount < 3) {
							faceurl.append(contact.faceurl).append(",");
						}
						else {
							faceurl.append(contact.faceurl);
						}
						
						facecount ++;
					}
					
				}
				else {
					WeiboUserInfoHandler usrHandler = new WeiboUserInfoHandler();
					usrHandler.context = context;
					usrHandler.wejoy = wejoy;
					// usrHandler.process(listId, String.valueOf(memberId));
					
					contact = new ContactModule();
					contact.contactId = String.valueOf(memberId);
				}
				
				contacts.add(contact);
			}
			
			// @jichao,liuzhao, very slow op here
			ContactModule group = dataStore.queryContactByContactId(listId);
			group.faceurl = faceurl.toString();
			group.memberCount = ids.length();
			dataStore.updateContact(group);
			
			// TO DO? append? -> reload?
			// dataStore.addGroupMembers(listId, contacts);
		} 
		catch (JSONException e1) {
			DebugUtil.error("dbwriting error", e1.toString());
		}
		finally {
			groupMemberLoadLock.release();
		}
	}

	@Override
	public void onFailed(ErrorInfo errorInfo) {
		DebugUtil.toast(context, errorInfo.info);
	}

	@Override
	public void onFile(String arg0, HashSet<Integer> arg1, int arg2) {
			
	}
}
