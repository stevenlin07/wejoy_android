package com.wejoy.service.apphandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;

import com.weibo.sdk.syncbox.BoxInstance;
import com.weibo.sdk.syncbox.listener.SendListener;
import com.weibo.sdk.syncbox.type.pub.BoxResult;
import com.weibo.sdk.syncbox.type.pub.ErrorInfo;
import com.weibo.sdk.syncbox.utils.AuthModule;
import com.wejoy.module.ContactModule;
import com.wejoy.service.WeJoyServiceOpType;
import com.wejoy.service.WeiboConstants;
import com.wejoy.store.DataStore;
import com.wejoy.util.DebugUtil;
/**
 * @author WeJoy Group
 *
 */
public class SyncGetGroupMemberHandler implements SendListener {
	private BoxInstance wejoy = BoxInstance.getInstance();
	private String gid;
	private Semaphore groupMemberLoadLock = null; 
	private ContactModule group = null;
	
	public ContactModule process(String gid) {
		this.gid = gid;
		Map<String, String> params = new HashMap<String, String>();
		params.put("gid", gid);
		wejoy.wejoyInterface(this, WeJoyServiceOpType.parseToJson(WeJoyServiceOpType.chatMembers, params), 
				WeiboConstants.normal_cmd_timeout);
		
		groupMemberLoadLock = new Semaphore(0);			
		
		try {
			groupMemberLoadLock.acquire();
		} 
		catch (InterruptedException e) {
			DebugUtil.error("", "", e);
		}
		
		return group;
	}
	
	public void onSuccess(BoxResult result) {
		try {
			// 1793692835,1737089987
			String result0 = result.result;
			DataStore ds = DataStore.getInstance();
			
			if(result0 != null && result0.indexOf(",") > 0) {
				String[] uids = result0.split(",");
				List<String> memberIds = new ArrayList<String>();
				StringBuilder faceurl = new StringBuilder();
				StringBuilder gname = new StringBuilder();
				int friendcount = processGroupMembers(uids, faceurl, gname, memberIds);
				
				// 处理极端情况，就是群众一个自己的双向好友都没有
				for(int i = 1; i <= 3 && friendcount < 1; i++) {
					try {
						Thread.sleep(2000 * i);
					} 
					catch (InterruptedException e) {
						// ignore
					}
					
					friendcount = processGroupMembers(uids, faceurl, gname, memberIds);
				}
				
				group = new ContactModule();
				group.contacttype = ContactModule.CONTACT_TYPE_GROUP;
				group.faceurl = faceurl.toString();
				group.name = gname.toString();
				group.contactId = gid;
				group.memberCount = uids.length;
				
				ds.insertNewGroup(group, memberIds);
			}
		}
		finally {
			groupMemberLoadLock.release();
		}
	}
	
	private int processGroupMembers(String[] uids, StringBuilder faceurl, StringBuilder gname, List<String> memberIds) {
		int friendcount = 0;
		DataStore ds = DataStore.getInstance();
		
		for(int i = 0; i < uids.length; i++) {
			String id = uids[i];
			memberIds.add(id);
			ContactModule c = ds.queryContactByContactId(id);
			
			if(c != null) {
				// 不把当前用户的名字和头像加入会话列表的显示当中
				if(c.contactId.equals(AuthModule.INSTANCE.getUid())) {
					continue;
				}
				
				friendcount ++;
				faceurl.append(c.faceurl);
				gname.append(c.name);
				
				if(i < uids.length - 1) {
					faceurl.append(",");
					gname.append(",");
				}
			}
			else {
				WeiboUserInfoHandler usrHandler = new WeiboUserInfoHandler();
				usrHandler.process(id);
			}
		}
		
		return friendcount;
	}
	
	public void onFailed(ErrorInfo info) {
		DebugUtil.error(info.info);
		groupMemberLoadLock.release();
	}

	public void onFile(String fileId, HashSet<Integer> hasSucc, int limit) {
		// do nothing
	}
}
