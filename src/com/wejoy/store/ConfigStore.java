package com.wejoy.store;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.wejoy.module.UserConfig;
import com.wejoy.module.UserInfo;
import com.wejoy.module.WeJoySysConfig;

/**
 * 
 * @author WeJoy Group
 *
 */
public class ConfigStore {
	private static ConfigStore instace;
	private UserConfig userconfig;
	private WeJoySysConfig sysconfig;
	private SqliteStore sqlitedb = SqliteStore.INSTANCE;
	private ExecutorService worker = Executors.newFixedThreadPool(1);
	
	private ConfigStore() {
		
	}
	
	public static ConfigStore getInstance() {
		if(instace == null) {
			instace = new ConfigStore();
		}
		
		return instace;
	}
	
	public UserConfig getUserConfig() {
		if(userconfig == null) {
			userconfig = sqlitedb.getUsrConfig();
		}
		
		return userconfig;
	}
	
	public void updateUserSettings(final UserConfig config) {
		this.userconfig = config;
		
		worker.execute(new Runnable() {
			public void run() {
				sqlitedb.updateUsrConfig(config);
			}
		});
	}
	
	public WeJoySysConfig querySysConfig() {
		if(sysconfig == null) {
			sysconfig = sqlitedb.querySysConfig();
		}
		
		return sysconfig;
	}
	
	public void insertSysConfig(WeJoySysConfig config) {
		this.sysconfig = config;
		sysconfig = sqlitedb.updateSysConfig(config);
	}
}
