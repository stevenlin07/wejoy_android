package com.weibo.sdk.syncbox.sync;

import com.weibo.sdk.syncbox.net.NetModule;
import com.weibo.sdk.syncbox.service.SyncService;
import com.weibo.sdk.syncbox.utils.LogModule;
import com.weibo.sdk.syncbox.utils.NotifyModule;
import com.weibo.sdk.syncbox.utils.StoreModule;
import com.weibo.sdk.syncbox.utils.TimeModule;

public class SyncHandler {

	protected static LogModule log = LogModule.INSTANCE;
	protected static TimeModule time = TimeModule.INSTANCE;
	protected static NotifyModule notify = NotifyModule.INSTANCE;
	protected static StoreModule store = StoreModule.INSTANCE;
	protected static NetModule net = NetModule.INSTANCE;
	protected static SyncService service = SyncService.INSTANCE;
	
}

class SyncKey {
	private final static char SYNC_KEY_SPLIT = '.';
	public static boolean isEmpty(String syncKey) {
		int idx = syncKey.lastIndexOf(SYNC_KEY_SPLIT);
		if (idx >= syncKey.length() - 2) {
			return true;
		}
		return false;
	}
}