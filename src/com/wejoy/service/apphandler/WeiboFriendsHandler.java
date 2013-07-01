package com.wejoy.service.apphandler;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context; 

import com.weibo.sdk.syncbox.BoxInstance;
import com.weibo.sdk.syncbox.listener.SendListener;
import com.weibo.sdk.syncbox.type.pub.BoxResult;
import com.weibo.sdk.syncbox.type.pub.ErrorInfo;
import com.weibo.sdk.syncbox.type.pub.RequestType;
import com.weibo.sdk.syncbox.type.pub.ServerType;
import com.wejoy.module.ContactModule;
import com.wejoy.service.WeiboConstants;
import com.wejoy.store.DataStore;
import com.wejoy.util.DebugUtil;
/**
 * 
 * @author WeJoy Group
 *
 */
public class WeiboFriendsHandler implements SendListener {
	private static final String getBilateralUrl = "http://api.weibo.com/2/friendships/friends/bilateral.json";
	private Context context;
	private DataStore dataStore = DataStore.getInstance();
	private BoxInstance wejoy = BoxInstance.getInstance();
	
	public WeiboFriendsHandler(Context context) {
		this.context = context;
	}
	
	public void process() {
		wejoy.proxyInterface(this, WeiboConstants.APPKEY, getBilateralUrl, 
				"source=" + WeiboConstants.APPKEY, RequestType.GET, ServerType.weiboPlatform, null, null,
				WeiboConstants.normal_cmd_timeout);
	}
	
	@Override
	public void onFailed(ErrorInfo info) {
		DebugUtil.toast(context, info.info);
	}
	
	@Override
	public void onFile(String arg0, HashSet<Integer> arg1, int arg2) {
		
	}
	
	@Override
	public void onSuccess(BoxResult boxResult) {
		System.out.println("获取双向关注好友成功");
		try {
			JSONObject jb = new JSONObject(boxResult.result);
			JSONArray ja = jb.getJSONArray("users");
			List<ContactModule> contacts = new ArrayList<ContactModule>();
			
			for(int i =0; i<ja.length(); i++) {
				JSONObject json = ja.getJSONObject(i);
				ContactModule contact = parseContact(json);
				contacts.add(contact);
			}
			
			// initContacts方法只更新db,但是不更新缓存,主要是为了query的时候可以利用sqlite进行排序
			dataStore.clearContacts();
			dataStore.initContacts(contacts);
		} 
		catch (JSONException e1) {
			DebugUtil.error("dbwriting error", e1.toString());
		}
		
		// 双向好友加载完毕，加载群，减少群成员重复加载机会
		System.out.println("更新双向关注好友存储成功");
	}
	
	private ContactModule parseContact(JSONObject json) throws JSONException {
		ContactModule c = new ContactModule();
		c.contactId = json.getString("id");
		c.name = json.getString("screen_name");
		c.faceurl = json.getString("profile_image_url");
		c.description = json.getString("description");
		c.contacttype = ContactModule.CONTACT_TYPE_SINGLE;
		return c;
	}
}
