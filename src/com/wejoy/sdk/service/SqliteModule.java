package com.wejoy.sdk.service;

import java.util.LinkedList;
   
import com.weibo.sdk.syncbox.type.pub.FixMetaInfo;
import com.weibo.sdk.syncbox.utils.pub.SdkDatabase;
import com.wejoy.util.DebugUtil;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

public class SqliteModule implements SdkDatabase{

	private static SQLiteDatabase sqlitedb;
	
	public SqliteModule(){
		if (initDatabase()) {
			new Thread(new Runnable(){
				@Override
				public void run() {
					long count = SqliteHelper.getHistoryCounts(sqlitedb) ;
					if (count > 100) {
						
					}
				}
			}).start();
		}
	}

	public static boolean initDatabase() {
		if (sqlitedb == null) {
			sqlitedb = SqliteHelper.initDatabase();
			if (null == sqlitedb){
				System.out.println("create sqldb false!");
				DebugUtil.debug("init database failed!");
				return false;
			}
		}
		System.out.println("create sqldb true!");
		return true;
	}
	
	public static void createTable() {
		if(initDatabase()) {
			SqliteHelper.execSql(sqlitedb, SqliteHelper.createTableHistoryId);
			SqliteHelper.execSql(sqlitedb, SqliteHelper.createIndexHistoryId);
			SqliteHelper.execSql(sqlitedb, SqliteHelper.createTableFixMeta);
			SqliteHelper.execSql(sqlitedb, SqliteHelper.createIndexFixMeta);
		}
	}
	
	
	@Override
	public void close() throws Exception {
		if (null != sqlitedb){
			try {
				sqlitedb.close();
			} catch (Exception e) {
				
			}
			sqlitedb = null;
		}
	}

	@Override
	public boolean containFixMeta(String msgId, String folderId) throws Exception {
		return SqliteHelper.contain(sqlitedb, SqliteHelper.FIXMETA_TABLE, msgId, folderId);
	}

	@Override
	public boolean containHistoryId(String msgId, String folderId) throws Exception {
		return SqliteHelper.contain(sqlitedb, SqliteHelper.HISTORYID_TABLE, msgId, folderId);
	}
	
	@Override
	public void insertFixMeta(String msgId, String folderId, byte[] fixMeta) throws Exception {
		ContentValues values = new ContentValues();
		values.put(SqliteHelper.COLUMNS_MSGID,msgId);
		values.put(SqliteHelper.COLUMNS_FOLDERID, folderId);
		values.put(SqliteHelper.COLUMNS_META,fixMeta);
		if ( !containFixMeta(msgId,folderId)){
			long rowid = sqlitedb.insert(SqliteHelper.FIXMETA_TABLE, null, values);
			if (-1 == rowid){
				Log.d("FIXTAG","[insert failed]["+folderId+"-"+ msgId +"]");
			}
		} else {
			Log.d("FIXTAG","[insert failed][it has the same]["+folderId+"-"+ msgId +"]");
		}
	}

	@Override
	public void insertHistoryId(String msgId, String folderId, int timestamp) throws Exception {
		ContentValues values = new ContentValues();
		values.put(SqliteHelper.COLUMNS_MSGID,msgId);
		values.put(SqliteHelper.COLUMNS_FOLDERID, folderId);
		values.put(SqliteHelper.COLUMNS_TIMESTAMP,timestamp);
		if ( !containHistoryId(msgId,folderId)){
			long rowid = sqlitedb.insert(SqliteHelper.HISTORYID_TABLE, null, values);
			if (-1 == rowid){
				Log.d("FIXTAG","[insert failed]["+folderId+"-"+ msgId +"]");
			}
		} else {
			Log.d("FIXTAG","[insert failed][it has the same]["+folderId+"-"+ msgId +"]");
		}
	}

	@Override
	public void removeFixMeta(String msgId, String folderId) throws Exception {
			String whereClause = SqliteHelper.COLUMNS_FOLDERID+"= ? AND "+SqliteHelper.COLUMNS_MSGID+"=?";;
			String[] whereArgs = {folderId,msgId};
			int result = sqlitedb.delete(SqliteHelper.FIXMETA_TABLE, whereClause, whereArgs);
			if (0 == result){
				DebugUtil.debug("delete failed "+"folderId = "+folderId+",msgId = "+msgId);
			} else {
				DebugUtil.debug("delete succ "+"folderId = "+folderId+",msgId = "+msgId);
			}
	}

	@Override
	public LinkedList<FixMetaInfo> getFixMetaAll() throws Exception {
		LinkedList<FixMetaInfo> fixMetaInfoList = new LinkedList<FixMetaInfo>();
		Cursor cursor = null;
		try{
			cursor = sqlitedb.query(SqliteHelper.FIXMETA_TABLE, null,null,null, null, null, "id asc");
			if (null != cursor && cursor.getCount() >= 1){
				DebugUtil.debug("count####################:"+cursor.getCount());
				while (cursor.moveToNext()) {
					int fixMetaIndex = cursor.getColumnIndex(SqliteHelper.COLUMNS_META);
					byte[] fixMeta = cursor.getBlob(fixMetaIndex);
					int folderIdIndex = cursor.getColumnIndex(SqliteHelper.COLUMNS_FOLDERID);
					String folderId = cursor.getString(folderIdIndex);
					int msgIdIndex = cursor.getColumnIndex(SqliteHelper.COLUMNS_MSGID);
					String msgId = cursor.getString(msgIdIndex);
					DebugUtil.debug("msgIdIndex:"+msgId);
					FixMetaInfo fixMetaInfo = new FixMetaInfo(folderId,fixMeta);
					fixMetaInfoList.add(fixMetaInfo);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (null != cursor) {
				try {
					cursor.close();
				} catch (Exception e) {
					
				}
			}
		}
		return fixMetaInfoList;
	}

}
