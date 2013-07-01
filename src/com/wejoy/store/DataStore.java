package com.wejoy.store;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors; 
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.widget.ImageView;

import com.wejoy.module.ChatMessage;
import com.wejoy.module.ContactModule;
import com.wejoy.module.ConvertModule; 
import com.wejoy.service.apphandler.WeiboUserInfoHandler;  
import com.wejoy.util.CommonUtil; 
import com.wejoy.util.SDCardUtil;
/**
 * 
 * @author WeJoy Group
 *
 */
public class DataStore {
	private static SqliteStore sqlitedb = null;
	private static DataStore instance = null;
	private ContactModule ownerUser = null;
	private List<ContactModule> groups = new ArrayList<ContactModule>();
	private List<ContactModule> contacts = new ArrayList<ContactModule>();
	private List<ConvertModule> conversations = new ArrayList<ConvertModule>();
	private Map<String, List<ContactModule>> groupMembers = new HashMap<String, List<ContactModule>>();
	private Map<String, List<ChatMessage>> chatMessages = new HashMap<String, List<ChatMessage>>();
	private ExecutorService pool = Executors.newFixedThreadPool(4);
//	/**
//	 * @by jichao, 这里使用一个单独的单线程pool主要目的是防止并发产生的更新问题
//	 */
//	private ExecutorService asyncChatTaskPool = Executors.newFixedThreadPool(1);
//	
	public static final int GROUP_MEMBER_RELOAD = 0x0f;
	public static final int GROUP_MEMBER_ADD = 0x10;
	public static final int GROUP_MEMBER_APPEND = 0x11;
	public static final int GROUP_MEMBER_REMOVE = 0x31;
	public static final int MODE_WORLD_READABLE = 1;
	
	SharedPreferences pref;
    int count;
	
	public static DataStore getInstance() {
		if(instance == null) {
			synchronized(DataStore.class) {
				if(instance == null) {
					instance = new DataStore();
				}
			}
		}
		
		return instance;
	}
	
	private DataStore() {
		sqlitedb = SqliteStore.INSTANCE;
	}
	
	public void initDatabase(String uid) {
		// clear cache
		ownerUser = null;
		groups = new ArrayList<ContactModule>();
		contacts = new ArrayList<ContactModule>();
		conversations = new ArrayList<ConvertModule>();
		groupMembers = new HashMap<String, List<ContactModule>>();
		chatMessages = new HashMap<String, List<ChatMessage>>();
		
		sqlitedb.initDatabase(uid);
	}
	
	public void asyncLoadContacts() {
		// start auto load to make cache warm up
		new Thread(new Runnable() {
			public void run() {
				queryContacts();
			}
		}).start();
	}
	
	private boolean hasOnlyOwner() {
		if(isEmpty(contacts) || contacts.size() != 1) {
			return false;
		}
		
		if(contacts.get(0).contactId.equals(CommonUtil.getCurrUserId())) {
			return true;
		}
		
		return false;
	}
	
	public ContactModule getOwnerUser() {
		if(ownerUser == null) {
			ownerUser = sqlitedb.queryContactById(CommonUtil.getCurrUserId());
		}
		
		if(ownerUser != null && ownerUser.faceurl != null && ImageStore.getInstance().getImage(ownerUser.faceurl) == null) {
			try {
				ImageStore.getInstance().loadImageFromUrl(ownerUser.faceurl, fakeImageLoadL);
			} 
			catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		else {
			WeiboUserInfoHandler usrInfoHandler = new WeiboUserInfoHandler();
			usrInfoHandler.process(CommonUtil.getCurrUserId());
		}
		
		return ownerUser;
	}
	
	ImageStore.OnImageLoadListener fakeImageLoadL = new ImageStore.OnImageLoadListener() {
		@Override
		public void onImageLoad(ImageView iv, Bitmap bm) {
		}
		
		@Override
		public void onError(ImageView iv, String msg) {
		}
	};
	
	public List<ContactModule> queryContacts() {
		if(isEmpty(contacts) || hasOnlyOwner()) {
			synchronized(contacts) {
				if(isEmpty(contacts) || hasOnlyOwner()) {
					queryFromDB();
				}
			}
		}
		
		return contacts;
	}
	
	private void queryFromDB() {
		contacts = sqlitedb.queryAllContact();
		
		if(!isEmpty(contacts)) {
			
		}
	}
	
	public void clearContacts() {
		sqlitedb.removeAllContact();
		
		if(contacts != null) {
			contacts.clear();
		}
		else {
			contacts = new ArrayList<ContactModule>();
		}
	}
	
	public List<ContactModule> queryContactsByType(int type) {
		List<ContactModule> list = queryContacts();
		List<ContactModule> list0 = new ArrayList<ContactModule>();
		
		for(ContactModule c : list) {
			if(c.contacttype == type) {
				list0.add(c);
			}
		}
		
		return list0;
	}
	
	public ContactModule queryGroupByContactId(String contactId) {
		if(isEmpty(groups)) {
			groups = sqlitedb.queryAllContact(ContactModule.CONTACT_TYPE_GROUP);
		}
		
		if(!isEmpty(groups) && contactId != null) {
			int idx = searchContactIndexByContactId(groups, contactId);
			
			if(idx >= 0) {
				return groups.get(idx);
			}
		}
		
		return null;
	}
	
	private boolean isEmpty(List list) {
		return list == null || list.isEmpty();
	}
	
	public ContactModule queryContactByContactId(String contactId) {
		if(contactId == null || isEmpty(contacts)) {
			return null;
		}
		
		return searchContactByContactId(contactId);
	}
	
	private ContactModule searchContactByContactId(String contactId) {
		if(!isEmpty(contacts) && contactId != null) {
			for(ContactModule c : contacts) {
				if(contactId.equals(c.contactId)) {
					return c;
				}
			}
		}
		
		return null;
	}
	
	private int searchContactIndexByContactId(String contactId) {
		return searchContactIndexByContactId(null, contactId);
	}
	
	private int searchContactIndexByContactId(List<ContactModule> list, String contactId) {
		list = list == null ? contacts : list;
		
		if(!isEmpty(list) && contactId != null) {
			for(int i = 0;i < list.size(); i++) {
				if(contactId.equals(list.get(i).contactId)) {
					return i;
				}
			}
		}
		
		return -1;
	}
	
	public void updateContact(ContactModule contact) {
		if(contact.contacttype == ContactModule.CONTACT_TYPE_GROUP) {
			updateGroup(contact);
		}
		else {
			if(!isEmpty(contacts)) {
				int idx = searchContactIndexByContactId(contact.contactId);
				
				if(idx >= 0) {
					contacts.remove(idx);
				}
			}
			
			contacts.add(contact);
			pool.execute(new AsyncUpdateContactTask(contact));
		}
	}
	
	public void updateGroup(ContactModule contact) {
		if(!isEmpty(groups) && contact != null) {
			int idx = searchContactIndexByContactId(groups, contact.contactId);
			
			if(idx >= 0) {
				groups.remove(idx);
			}
		}
		
		groups.add(contact);
		pool.execute(new AsyncUpdateContactTask(contact));;
	}
	
	public void removeGroupMember(String groupid, String contactId) {
		List<ContactModule> members = queryMembersByGroupId(groupid);
		
		if(members != null) {
			for(Iterator<ContactModule> iterator = members.iterator(); iterator.hasNext(); ) {
				if(iterator.next().contactId.equals(contactId)) {
					iterator.remove();
					break;
				}
			}
		}
		
		sqlitedb.removeGroupMember(groupid, contactId);
	}
	
	public List<ContactModule> queryMembersByGroupId(String groupid) {
		List<ContactModule> members = null;
		
		if((members = groupMembers.get(groupid)) == null) {
			members = sqlitedb.queryGroupMember(groupid);
			
			groupMembers.put(groupid, members);
		}
		
		return members;
	}
	
	public void insertNewGroup(ContactModule group, List<String> memberIds) {
		// update sqlitedb
		groups.add(group);
		sqlitedb.insertNewGroup(group, memberIds);
		
		// update local cache
		List<ContactModule> members = new ArrayList<ContactModule>();
		
		for(String id : memberIds) {
			ContactModule member = this.queryContactByContactId(id);
			members.add(member);
		}
		
		groupMembers.put(group.contactId, members);
	}
	
	public void addGroupMember(String groupid, List<String> memberIds) {
		// update local cache
		List<ContactModule> members = groupMembers.get(groupid);
		
		for(String id : memberIds) {
			ContactModule member = this.queryContactByContactId(id); 
			members.add(member);
		}
		
		pool.execute(new AsyncAddGroupMemberTask(groupid, memberIds));
	}
	
	public synchronized void insertOrUpdateContact(ContactModule contact) {
		ContactModule oldContact = this.queryContactByContactId(contact.contactId);
		
		if(oldContact != null) {
			sqlitedb.updateContact(contact);
			
			// update cache
			oldContact.copyFrom(contact);
		}
		else {
			List<ContactModule> newContacts = new ArrayList<ContactModule>();
			newContacts.add(contact);
			insertContactInBatch(newContacts);
		}
	}
	
	public void initContacts(List<ContactModule> newContacts) {
		sqlitedb.insertContactInBatch(newContacts);
		queryContacts();
	}
	
	private void insertContactInBatch(List<ContactModule> newContacts) {
		if(!isEmpty(newContacts)) {
			String curusrid = CommonUtil.getCurrUserId();
			
			for(ContactModule c : newContacts) {
				if(c.contactId.equals(curusrid)) {
					ownerUser = c;
				}
				else {
					contacts.add(c);
				}
			}
			
			pool.execute(new AsyncInsertContactsTask(newContacts));
		}
	}
	
	public void insertNewConversation(ConvertModule conv) {
		conversations.add(0, conv);
		sqlitedb.insertNewConversation(conv);
	}
	
	public void removeGroup(String id) {
		if(!isEmpty(groups)) {
			int idx = searchContactIndexByContactId(groups, id);
			
			if(idx >= 0) {
				groups.remove(idx);
			}
		}
		
		pool.execute(new AsyncRemoveGroupTask(id));
	}
	
	public void removeConversation(String convid) {
		if(isEmpty(conversations) || convid == null || "".equals(convid)) {
			return;
		}
		
		Iterator iterator = conversations.iterator();
		
		while(iterator.hasNext()) {
			ConvertModule convert = (ConvertModule) iterator.next();

			if(convert.convid.equals(convid)) {
				if(convert.convtype == ConvertModule.GROUP_CONV) {
					sqlitedb.removeGroupByGroupId(convid);
				}
				
				iterator.remove();
				break;
			}
		}
		
		sqlitedb.removeConversation(convid);
	}
	
	public void setConveTop(String convid) {
		if(isEmpty(conversations) || convid == null || "".equals(convid)) {
			return;
		}
		
		Iterator iterator = conversations.iterator();
		int idx = -1;
		
		while(iterator.hasNext()) {
			ConvertModule convert = (ConvertModule) iterator.next();

			if(convert.convid.equals(convid)) {
				idx = conversations.indexOf(convert);
				break;
			}
		}
		
		if(idx > 0) {
			int curTopPos = conversations.get(0).position;
			List<ConvertModule> list = new ArrayList<ConvertModule>();
			ConvertModule conv = conversations.get(idx);
			conv.position = ++curTopPos;
			list.add(conv);
			
			if(idx > 0) {
				list.addAll(conversations.subList(0, idx));
			}
			
			if(idx < conversations.size() - 1) {
				list.addAll(conversations.subList(idx + 1, conversations.size() - 1));
			}
			
			conversations = list;
			
			sqlitedb.updateConvert(conv);
		}
	}
	
	public List<ConvertModule> queryConvList() {
		if(isEmpty(conversations)) {
			conversations = sqlitedb.queryConvList(-1, 40);
			java.util.Collections.sort(conversations, new ConvertModule.ConvertComparator());
		}
		
		return conversations;
	}
	
	public ConvertModule getConvertById(String convid) {
		ConvertModule conv = null;
		
		if(isEmpty(conversations) || convid == null || "".equals(convid)) {
			conv = null;
		}
		else {
			for(ConvertModule conv0 : conversations) {
				if(convid.equals(conv0.convid)) {
					conv = conv0;
					break;
				}
			}
		}
				
		if(conv == null) {
			conv = sqlitedb.queryConvById(convid);
			
			if(conv != null) {
				conversations.add(0, conv);
			}
		}
		
		return conv;
	}
	
	public void updateConvert(ConvertModule conv) {
		if(conv == null || conv.convid == null) {
			return;
		}
		
		if(isEmpty(conversations)) {
			this.queryConvList();
		}
		
		if(isEmpty(conversations)) {
			return;
		}
		
		Iterator iterator = conversations.iterator();
		int idx = -1;
		
		while(iterator.hasNext()) {
			ConvertModule convert = (ConvertModule) iterator.next();

			if(convert.convid.equals(conv.convid)) {
				idx = conversations.indexOf(convert);
				break;
			}
		}
		
		if(idx > 0) {
			List<ConvertModule> list = new ArrayList<ConvertModule>();
			list.add(conv);
			
			if(idx > 0) {
				list.addAll(conversations.subList(0, idx));
			}
			
			if(idx < conversations.size() - 1) {
				list.addAll(conversations.subList(idx + 1, conversations.size() - 1));
			}
			
			conversations = list;
		}
		else if(idx == 0) {
			ConvertModule conv0 = conversations.get(idx);
			conv0.latestUpdateTime = conv.latestUpdateTime;
			conv0.latestUpdateUserName = conv.latestUpdateUserName;
			conv0.setLatestUpdateMessage(conv.getLatestUpdateMessage());
		}
		
		pool.execute(new AsyncUpdateConvTask(conv));
	}
	
	/**
	 * update message by message id but not _id from sqlitedb
	 */
	public void updateChatStateByMsgId(ChatMessage chat) {
		List<ChatMessage> messages = chatMessages.get(chat.convid);
		
		if(messages != null) {
			int idx = Collections.binarySearch(messages, chat, new ChatMessage.MsgIdComparator());
			
			if(idx >= 0) {
				ChatMessage chat0 = messages.get(idx);
				chat0.sendState = chat.sendState;
			}
		}
		
		sqlitedb.updateStateByChatMsgId(chat);
	}
	
	public List<ChatMessage> queryChatMessage(String convid, long created_at, int count) {
		List<ChatMessage> messages = chatMessages.get(convid);
		
		if(messages != null) {
			List<ChatMessage> newmessages = new ArrayList<ChatMessage>();
			
			if(created_at >= 0) {
				ChatMessage object = new ChatMessage();
				object.created_at = created_at;
				int idx = Collections.binarySearch(messages, object, new ChatMessage.CreateTimeComparator());
				
				System.out.println("idx=" + idx);
				// @by jichao, cache is ready
				if(idx >= 0 && messages.size() > idx + count + 1 && idx >= 0) {
					for(int i = idx + 1; i <= idx + count && i < messages.size(); i++) {
						ChatMessage chat0 = messages.get(i);
						newmessages.add(chat0);
					}
					
					return newmessages;
				}
			}
			else if(count <= messages.size()) {
				ChatMessage object = new ChatMessage();
				
				for(int i = messages.size() - count; i < messages.size(); i++) {
					ChatMessage chat0 = messages.get(i);
					newmessages.add(chat0);
				}
				
				return newmessages;
			}
		}
		
		// @by jichao, in order to make logic not too complicated, clear and refresh the cache totally
		messages = sqlitedb.queryConvHistory(convid, created_at, count);
		chatMessages.put(convid, messages);
		
		return messages;
	}
	
	public void insertOrUpdateChatMessage(ChatMessage chat) {
		if(chat == null) {
			return;
		}
		
		List<ChatMessage> messages = chatMessages.get(chat.convid);
		boolean hasExistBefore = false; 
		 
		if(messages == null) {
			messages = new ArrayList<ChatMessage>();
			chatMessages.put(chat.convid, messages);
		}
		else {
			for(ChatMessage chat0 : messages) {
				if(chat.msgid.equals(chat0.msgid)) {
					chat0.cloneFrom(chat);
					chat = chat0;
					hasExistBefore = true;
					break;
				}
			}
			
			if(!hasExistBefore) {
				messages.add(chat);
			}
		}
		
		if(hasExistBefore) {
			sqlitedb.insertOrUpdateChatMessage(chat);
		}
		else {
			sqlitedb.insertChatMessage(chat);
			
			ConvertModule conv = getConvertById(chat.convid);
			conv.latestUpdateTime = System.currentTimeMillis();
			conv.latestUpdateUserName = chat.usrname;
			conv.setLatestUpdateMessage(chat.content);
			updateConvert(conv);
		}
	}
	
	public void insertChatMessage(ChatMessage chat) {
		List<ChatMessage> messages = chatMessages.get(chat.convid);
		 
		if(messages == null) {
			messages = new ArrayList<ChatMessage>();
			chatMessages.put(chat.convid, messages);
		}
		
		messages.add(chat);
		sqlitedb.insertChatMessage(chat);
		
		ConvertModule conv = getConvertById(chat.convid);
		conv.latestUpdateTime = System.currentTimeMillis();
		conv.latestUpdateUserName = chat.usrname;
		conv.setLatestUpdateMessage(chat.content);
		updateConvert(conv);
	}
	
	public void removeChatMessage(String convid, String _id) {
		if(_id == null || "".equals(_id.trim()) || convid == null) {
			return;
		}
		
		int _id0 = Integer.parseInt(_id);
		ChatMessage chat0 = null;
		List<ChatMessage> messages = chatMessages.get(convid);
		
		if(messages != null) {
			Iterator<ChatMessage> iterator = messages.iterator();
			
			while(iterator.hasNext()) {
				ChatMessage chat = iterator.next();
				
				if(chat.id == _id0) {
					chat0 = chat;
					
					// remove audio file at the same time
					SDCardUtil.removeFile(chat.attachPath);
					
					iterator.remove();
					break;
				}
			}
		}
		
		if(chat0 != null) {
			sqlitedb.removeChatMessage(String.valueOf(chat0.id), chat0.attachId);
		}
		else {
			sqlitedb.removeChatMessage(_id);
		}
	}
	
	private class AsyncUpdateConvTask implements Runnable {
		private ConvertModule conv;
		
		public AsyncUpdateConvTask(ConvertModule conv) {
			this.conv = conv;
		}
		
		public void run() {
			sqlitedb.updateConvert(conv);
		}
	}
	
	private class AsyncInsertChatTask implements Runnable {
		private ChatMessage chat;
		
		public AsyncInsertChatTask(ChatMessage chat) {
			this.chat = chat;
		}
		
		public void run() {
			sqlitedb.insertChatMessage(chat);
			
			ConvertModule conv = getConvertById(chat.convid);
			conv.latestUpdateTime = System.currentTimeMillis();
			conv.latestUpdateUserName = chat.usrname;
			conv.setLatestUpdateMessage(chat.content);
			updateConvert(conv);
		}
	}	
	
	private class AsyncUpdateChatTask implements Runnable {
		private ChatMessage chat;
		
		public AsyncUpdateChatTask(ChatMessage chat) {
			this.chat = chat;
		}
		
		public void run() {
			sqlitedb.updateStateByChatMsgId(chat);
		}
	}	
	
	private class AsyncInsertContactsTask implements Runnable {
		private List<ContactModule> contacts0;
		
		public AsyncInsertContactsTask(List<ContactModule> contacts) {
			this.contacts0 = contacts;
		}
		
		public void run() {
			sqlitedb.insertContactInBatch(contacts0);
		}
	}
	
	private class AsyncAddGroupMemberTask implements Runnable {
		private String groupid;
		private List<String> memberIds;
		
		public AsyncAddGroupMemberTask(String groupid, List<String> memberIds) {
			this.groupid = groupid;
			this.memberIds = memberIds;
		}
		
		public void run() {
			sqlitedb.addGroupMember(groupid, memberIds);
		}
	}	
	
	private class AsyncRemoveGroupTask implements Runnable {
		private String groupid;
		
		public AsyncRemoveGroupTask(String groupid) {
			this.groupid = groupid;
		}
		
		public void run() {
			sqlitedb.removeGroupByGroupId(groupid);
		}
	}	
	
	private class AsyncUpdateContactTask implements Runnable {
		private ContactModule contact;
		
		public AsyncUpdateContactTask(ContactModule contact) {
			this.contact = contact;
		}
		
		public void run() {
			sqlitedb.updateContact(contact);
		}
	}
}
