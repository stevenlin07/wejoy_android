package com.wejoy.sdk.service;

import java.io.File;
import java.io.IOException;

import com.wejoy.util.DebugUtil;

import junit.framework.Assert;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteStatement;

public class SqliteHelper {

	public static final String DATABASE_DIR = "database";
	public static final String DATABASE_NAME = "sdkdb.db3";
	public static final String HISTORYID_TABLE = "historyId";
	public static final String HISTORYID_INDEX = "idxHistoryId";
	public static final String FIXMETA_TABLE = "fixMeta";
	public static final String FIXMETA_INDEX = "idxFixMeta";
	public static final String COLUMNS_MSGID = "msgId";
	public static final String COLUMNS_FOLDERID = "folderId";
	public static final String COLUMNS_TIMESTAMP = "timestamp";
	public static final String COLUMNS_META = "meta";
	
	public static final String createTableHistoryId = "create table "+HISTORYID_TABLE+"(id int,"+COLUMNS_MSGID
			+" varchar(255),"+COLUMNS_FOLDERID+" varchar(255),"+COLUMNS_TIMESTAMP+" varchar(255))";
	public static final String createIndexHistoryId = "create index "+HISTORYID_INDEX+" on "+HISTORYID_TABLE
			+"("+COLUMNS_FOLDERID+","+COLUMNS_MSGID+")";
	public static final String createTableFixMeta = "create table "+FIXMETA_TABLE+"(id int,"+COLUMNS_MSGID
			+" varchar(255),"+COLUMNS_FOLDERID+" varchar(255),"+COLUMNS_META+" blob)";
	public static final String createIndexFixMeta = "create index "+FIXMETA_INDEX+" on "+FIXMETA_TABLE
			+"("+COLUMNS_FOLDERID+","+COLUMNS_MSGID+")";
	
	public static void execSql(SQLiteDatabase sqlitedb, String sql) {
	    try {
	    	sqlitedb.execSQL(sql);
	    } catch (SQLiteException e){
	    	DebugUtil.debug("The sql has been created " + sql);
	    }
	}
	
	public static boolean contain(SQLiteDatabase sqlitedb,String TableName,String msgId,String folderId){
		Cursor cursor = null ;
		boolean flag = false;
		try {
			String selection = SqliteHelper.COLUMNS_FOLDERID+"= ? AND "+SqliteHelper.COLUMNS_MSGID+"=?";
			String[] selectionArgs = {folderId,msgId}; 
			cursor = sqlitedb.query(TableName, null,selection,selectionArgs, null, null, "id asc");
			DebugUtil.debug("[SQLITE QUERY]["+TableName+"-"+folderId+","+msgId+","+cursor.getCount()+"]");
			if (null != cursor){
				if(cursor.getCount() >= 1){
					flag = true;
				}
			}
		} catch (Exception e){
			e.printStackTrace();
		} finally {
			if (null != cursor) {
				try {
					cursor.close();
				} catch (Exception e) {
					
				}
			}
		}
		return flag;
	}
	
	public static SQLiteDatabase initDatabase(){
		Assert.assertTrue(android.os.Environment.MEDIA_MOUNTED.equals(android.os.Environment.getExternalStorageState()));
		String dbDirPath = android.os.Environment.getExternalStorageDirectory().getAbsolutePath()+File.separator+DATABASE_DIR;
		File dbDir = new File(dbDirPath);
		if(!dbDir.exists()){
			dbDir.mkdir();
		}
        boolean isFileCreateSuccess=false; 
        String dbFilePath = dbDirPath +File.separator+DATABASE_NAME;
	    File dbFile =new File(dbFilePath);
	    if(!dbFile.exists()){
	    	try{                 
	    		isFileCreateSuccess=dbFile.createNewFile();
	    	} catch(IOException e){
	            e.printStackTrace();  
	        }	                    
	    } else{
	    	isFileCreateSuccess=true;
	    }
	    SQLiteDatabase sqlitedb = null;
	    if(isFileCreateSuccess){
	    	sqlitedb = SQLiteDatabase.openOrCreateDatabase(dbFile, null);
	    }
	    
	    return sqlitedb;
	}
	
	
	/**获取历史记录数**/
	public static long getHistoryCounts(SQLiteDatabase sqlitedb) {
		String sql = "SELECT COUNT(*) FROM " + HISTORYID_TABLE;
	    SQLiteStatement statement = sqlitedb.compileStatement(sql);
	    long count = statement.simpleQueryForLong();
	    return count;
	}
	
	public static void deleteHistory() {
		
	}
}
