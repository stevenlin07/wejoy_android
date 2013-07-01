package com.wejoy.service.apphandler;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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
import com.wejoy.util.DebugUtil;
/**
 * Isn't used so far, by @jichao
 * @author WeJoy Group
 *
 */
public class WeiboListHandler implements SendListener {
	public Context context;
	public Handler refreshHandler;
	public DataStore dataStore;
	public BoxInstance wejoy;
	public Runnable contactRefresh;
	public Handler loginFinishHandler;
	public Runnable loginFinishListener;
	
	public void process() {
		wejoy.wejoyInterface(this, WeJoyServiceOpType.parseToJson(WeJoyServiceOpType.groups, null), WeiboConstants.normal_cmd_timeout);
	}
	
	@Override
	public void onSuccess(BoxResult boxResult) {
		try {
			JSONObject json = new JSONObject(boxResult.result);
			final JSONArray ja = json.getJSONArray("value");	
			List<ContactModule> contacts = new ArrayList<ContactModule>();
			List<WeiboListMemberHandler> loadMemberTasks = new ArrayList<WeiboListMemberHandler>();
			
			if(ja.length() > 0) {
				Semaphore groupMemberLoadLock = new Semaphore(1 - ja.length());			
				
				for(int i =0; i < ja.length(); i++) {
					final JSONObject json0 = ja.getJSONObject(i);
					ContactModule contact = new ContactModule();
					contact.contactId = json0.getString("list_id");
					contact.name = json0.getString("name");
					contact.contacttype = ContactModule.CONTACT_TYPE_GROUP;
					contacts.add(contact);
					
					WeiboListMemberHandler memberHandler = new WeiboListMemberHandler();
					memberHandler.context = context;
					memberHandler.dataStore = dataStore;
					memberHandler.wejoy = wejoy;
					memberHandler.groupMemberLoadLock = groupMemberLoadLock;
					memberHandler.listId = contact.contactId;
					loadMemberTasks.add(memberHandler);
				}
				
				// dataStore.insertContactInBatch(contacts);
				
				for(WeiboListMemberHandler memberHandler : loadMemberTasks) {
					memberHandler.process();
				}
				
				try {
					groupMemberLoadLock.acquire();
				} 
				catch (InterruptedException e) {
					DebugUtil.debug("", "", e);
				}
			}
		} 
		catch (JSONException e1) {
			DebugUtil.error("dbwriting error", e1.toString());
		}
		
		loginFinishHandler.post(loginFinishListener);
	}

	@Override
	public void onFailed(ErrorInfo errorInfo) {
		
	}

	@Override
	public void onFile(String arg0, HashSet<Integer> arg1, int arg2) {
			
	}
}
