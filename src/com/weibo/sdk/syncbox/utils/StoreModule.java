package com.weibo.sdk.syncbox.utils;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList; 
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;  
import java.util.concurrent.ScheduledFuture;
 
import com.weibo.sdk.syncbox.listener.SendListener;
import com.weibo.sdk.syncbox.type.GetFileInfo;
import com.weibo.sdk.syncbox.type.SendFileInfo;
import com.weibo.sdk.syncbox.type.SyncMessage.Meta;
import com.weibo.sdk.syncbox.type.pub.EventInfo;
import com.weibo.sdk.syncbox.type.pub.EventType;
import com.weibo.sdk.syncbox.type.pub.SdkEvent; 
import com.weibo.sdk.syncbox.utils.pub.SdkDatabase;

/**
 * @author liuzhao
 *
 */
public enum StoreModule {

	INSTANCE;
	private static LogModule log = LogModule.INSTANCE;
	private static NotifyModule notify = NotifyModule.INSTANCE;
	
	public void clear() {
		nioSendQueue.clear();
		syncKeys.clear();
		for (String withtag : tagTimeList.keySet()) {
			ScheduledFuture<?> t = tagTimeList.get(withtag);
			t.cancel(true);
			tagTimeList.remove(withtag);
		}
		tagTimeList.clear();
		getFileCallback.clear();
		sendFileCallback.clear();
		listener.clear();
		historyCache.clear();
		expectAckMap.clear();
		metaCache.clear();
		metaCache.clear();
		fixNotice.clear();
		fixTag.clear();
	}
	
	/***/
	public HashMap<String,SendListener> listener = new HashMap<String,SendListener>();
	
	/** 缓存所有fix阶段的meta */
	private HashMap<String,Meta> metaCache = new HashMap<String,Meta>();
	/** msgId - expectAck*/
	public ConcurrentHashMap<String,String> expectAckMap = new ConcurrentHashMap<String,String>(); 
	/** 缓存修复阶段产生的通知 */
	public HashMap<String, LinkedList<String>> fixNotice = new HashMap<String, LinkedList<String>>();
	/** 缓存当前产生修复的tag(msgId) */
	public HashMap<String,String> fixTag = new HashMap<String,String>();
	
	public LinkedHashSet<String> historyCache = new LinkedHashSet<String>();
	public SdkDatabase database ;
	
	/** 与NIO对接的两级队列 */
	public MessageBlockingQueue nioSendQueue = new MessageBlockingQueue();
//	public LinkedBlockingQueue<MessageEntity> nioSendQueue = new LinkedBlockingQueue<MessageEntity>();
	/** folderId -- syncKey 用来标识每个文件的读取进度，存在竞争，需要同步 */
	public Map<String, String> syncKeys = new ConcurrentHashMap<String, String>();
	/** 下载文件fileId -- FileMessage */
	public Map<String, GetFileInfo> getFileCallback = new ConcurrentHashMap<String, GetFileInfo>();
	/** 上传文件fileId -- FileMessage */
	public Map<String, SendFileInfo> sendFileCallback = new ConcurrentHashMap<String, SendFileInfo>();
	/** 存储发送出去的消息ID，以及对应的定时器  */
	public Map<String, ScheduledFuture<?>> tagTimeList = new ConcurrentHashMap<String, ScheduledFuture<?>>();
	/** 命令对应的base64编码后的信息 */
	public Map<String, String> uriMap = new ConcurrentHashMap<String, String>();
	
	/**
	 * @param sdkDatabase 
	 */
	public void setSdkDatabase(SdkDatabase sdkDatabase){
		this.database = sdkDatabase; 
	}
	
	/** 保存到metaCache中，先落地insertFixMeta，后MetaCache ,来自于Notice处理,*/
	public boolean saveToMetaCache(String folderId, Meta meta,String expectAck) {
		expectAckMap.put(meta.getId(), expectAck); // 保存修复关系 
		boolean flag = false;
		String msgId = meta.getId();
		try { 
			database.insertFixMeta(msgId, folderId, meta.toByteArray());
			metaCache.put(msgId, meta);
			flag = true;
		} catch (Exception e) {
			EventInfo eventInfo = new EventInfo();
			eventInfo.info = "本地存储失效";
			eventInfo.eventType = EventType.LOCAL_STORE_ERROR;
			notify.onEvent(eventInfo);
			log.error("[META][SAVE][ERROR]", SdkEvent.LOCAL_STORE_ERROR, e) ;
		}
		return flag;
	}
	
	/** 从metaCache中获取meta*/
	public Meta getFromMetaCache(String msgId) {	
		return metaCache.get(msgId);
	}
	
	/** 从metaCache中删除meta*/
	public void removeFromMetaCache(Collection<String> pollList,String folderId) {
		metaCache.keySet().removeAll(pollList);
		try {
			for (String msgId :pollList) {
				database.removeFixMeta(msgId,folderId);
			}
		} catch (Exception e) { // sdcard = true && 失败
			// 这个remove失败是不需要处理的
		}
	}
	
	/** 迭代获取一条消息被哪些消息依赖，满足树的结构，注意，树的数目是不一定的  */
	public LinkedList<String> getExpectAckKeySet(String expectAck) {
		LinkedList<String> expectAckKeyList = new LinkedList<String>();
		HashSet<String> fatherIdSet = new HashSet<String>();
		fatherIdSet.add(expectAck); // root
		HashSet<String> newFatherSet = new HashSet<String>();
		while (fatherIdSet.size() != 0) {
			for (String fatherId:fatherIdSet) {
				getChildId(fatherId,expectAckKeyList,newFatherSet);
			}
			fatherIdSet.clear();
			fatherIdSet.addAll(newFatherSet);
			newFatherSet.clear();
		}
		return expectAckKeyList;
	}
	
	/** 清除链表中的依赖关系*/
	public void removeExpectAckTree(Collection<String> expectAckKeyList) {
		expectAckMap.keySet().removeAll(expectAckKeyList);
	}
	
	/** 存储是一个逆序的多叉树，获取依赖该节点的ID*/
	private void getChildId (String fatherId,LinkedList<String> expectAckKeyList,HashSet<String> newFatherSet) {
		if (expectAckMap.containsValue(fatherId)) { 
			for (String treeId:expectAckMap.keySet()) {
				if (expectAckMap.get(treeId).equals(fatherId)) {
					expectAckKeyList.add(treeId);
					newFatherSet.add(treeId);
				}
			}
		}
	}
	
	/** 存储需要修复的msgId标记，会修复msgId之前的所有没有ack的消息, 在需要修复，并且正在修复的时候，才会存这个notice. */
	public void saveFixNotice(String folderId,String msgId){
		log.info("[saveFixNotice][folderId "+folderId+",msgId "+msgId+"]",SdkEvent.INVOKE);
		LinkedList<String> noticeQueue = fixNotice.get(folderId);
		if (null == noticeQueue) {
			noticeQueue = new LinkedList<String>();
		}
		noticeQueue.offer(msgId);
		fixNotice.put(folderId, noticeQueue);
	}

	/** 获取一个需要修复的msgId,按照队列来取,如果失败，返回null */
	public String pollFixNotice(String folderId){
		String tag = null;
		LinkedList<String> noticeQueue = fixNotice.get(folderId);
		if (null != noticeQueue && 0 < noticeQueue.size()){
			tag = noticeQueue.poll();
			log.info("[pollFixNotice][folderId "+folderId+",msgId "+tag+"]",SdkEvent.INVOKE);
		}
		return tag;
	}
	
	/** 删除该folder对应的修复队列*/
	public void removeFixNotice(String folderId) {
		fixNotice.remove(folderId);
	}

	/**
	 * @ClassName: ContainType 
	 * @Description: 查询的返回类型
	 * @author liuzhao
	 * @date 2013-4-22 下午3:36:07 
	 */
	public enum ContainType {
		/**从metaCache中命中*/
		FIXCACHE,
		/**从历史记录中命中*/
		HISTORY,
		/**未命中*/
		UNFINDED,
		/**查询过程中出错失败*/
		FAILED;
	}
	
	/** 查数据逻辑:（按照命中率排序,修复缓存->历史ID缓存->历史ID存储->修复Meta存储),判断是否收到过这条消息，会查所有的cache */
	public ContainType contain(String folderId,String msgId){
		ContainType containType = ContainType.UNFINDED;
		if (metaCache.containsKey(msgId)){
			log.info("[CACHE][metaCache has it]",SdkEvent.INVOKE);
			containType =  ContainType.FIXCACHE;
		} else if (historyCache.contains(msgId)){
			log.info("[CACHE][historyCache has it]",SdkEvent.INVOKE);
			containType = ContainType.HISTORY;
		} else 
		try {
			if (database.containHistoryId(msgId,folderId)){
				log.info("[CACHE][historyIdDb has it]",SdkEvent.INVOKE);
				containType = ContainType.HISTORY;
			} 
		} catch (Exception e) {
			containType = ContainType.FAILED;
		}
		return containType;
	}

}
