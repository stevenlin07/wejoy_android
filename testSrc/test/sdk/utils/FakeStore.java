package test.sdk.utils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;

import com.weibo.sdk.syncbox.type.pub.FixMetaInfo;
import com.weibo.sdk.syncbox.utils.pub.SdkDatabase;


public class FakeStore implements SdkDatabase{

	private HashSet<String> historyIdDb = new HashSet<String>();
	private HashMap<String,byte[]> fixMetaDb = new HashMap<String,byte[]>();
	private HashMap<String,String> fixMetaDbIndex = new HashMap<String,String>();

	@Override
	public void insertHistoryId(String msgId, String folderId, int timestamp) throws Exception {
		if (containHistoryId(msgId,folderId)) return;
		historyIdDb.add(msgId);
	}

	@Override
	public boolean containHistoryId(String msgId, String folderId) throws Exception {
		return historyIdDb.contains(msgId);
	}

	@Override
	public void insertFixMeta(String msgId, String folderId, byte[] fixMeta) throws Exception {
		if (containFixMeta(msgId,folderId)) return;
		fixMetaDbIndex.put(msgId, folderId);
		fixMetaDb.put(msgId, fixMeta);
	}

	@Override
	public void removeFixMeta(String msgId, String folderId) throws Exception {
		fixMetaDb.remove(msgId);
		fixMetaDbIndex.remove(msgId);
	}

	@Override
	public void close() throws Exception {
		 clean();
	}

	public void clean(){
		historyIdDb.clear();
		fixMetaDb.clear();
	}

	@Override
	public LinkedList<FixMetaInfo> getFixMetaAll() throws Exception {
		LinkedList<FixMetaInfo> fixMetaAll= new LinkedList<FixMetaInfo>();
		for (String fixMetaId:fixMetaDb.keySet()) {
			FixMetaInfo fixMetaInfo = new FixMetaInfo(fixMetaDbIndex.get(fixMetaId),fixMetaDb.get(fixMetaId));
			fixMetaAll.add(fixMetaInfo);
		}
		return fixMetaAll;
	}

	@Override
	public boolean containFixMeta(String msgId, String folderId) throws Exception {
		return fixMetaDb.containsKey(msgId);
	}
}
