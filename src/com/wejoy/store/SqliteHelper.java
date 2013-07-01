package com.wejoy.store;

import java.io.File;
import com.wejoy.util.DebugUtil;
import com.wejoy.util.SDCardUtil;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.os.Environment;
/**
 * 
 * @author WeJoy Group
 *
 */
public class SqliteHelper implements StoreConstant {

	public static final String DATABASE_DIR = STORE_ROOT + "/database";
	public static final String DATABASE_NAME = "wejoydb.db1";
	
	public static final String TB_SYS_CONFIG = "sysconfig";
	public static final String TB_USR_CONFIG = "usrconfig";
	public static final String TB_CONV_LIST = "convlist";
	public static final String TB_CONTACT = "contact";
	public static final String TB_GROUPMEMBER = "groupmember";
	public static final String TB_GROUP_STATUS = "groupstatus"; // should be removed later
	public static final String TB_CHAT_HISTORY = "convhistory";
	public static final String TB_ATTACH = "convattach";
	public static final String TB_EMOTIONFACE = "emotionface";
	
	public static final String HISTORYID_TABLE = "historyId";
	public static final String HISTORYID_INDEX = "idxHistoryId";
	public static final String FIXMETA_TABLE = "fixMeta";
	public static final String FIXMETA_INDEX = "idxFixMeta";
	public static final String COLUMNS_MSGID = "msgId";
	public static final String COLUMNS_FOLDERID = "folderId";
	public static final String COLUMNS_TIMESTAMP = "timestamp";
	public static final String COLUMNS_META = "meta";
	
	public static final String createTableSysConfig = "create table " + TB_SYS_CONFIG + 
		" (_id INTEGER primary key autoincrement, version varchar(128), state varchar(256), extend varchar(1024))";
	
	public static final String createTableUsrConfig = "create table " + TB_USR_CONFIG + 
		" (_id INTEGER primary key autoincrement, version varchar(128), basic varchar(1024), extend varchar(1024))";
	/**
	 * create conversation list, like recent contacts in direct message module
	 * if convtype is group conv, chatwithid is the group id; if convtype is single, chatwithid is the guy you are talking to
	 */
	public static final String createTableConvList = "create table " + TB_CONV_LIST + " (_id INTEGER primary key autoincrement, " +
		"convid varchar(128), position int, chatwithid varchar(128), stickytopic varchar(512), convname varchar(256), " +
		"latestupdateTime int8, latestupdateusername varchar(256), latestupdatemessage varchar(256), " +
		"unreadcount int, convlistfacepic varchar(512), convtype int, statusid int8, " +
		"statususerid int8, extend varchar(512))";
	public static final String createIndexConvList = "create index idx_convlist_latestupdatetime on " + 
		TB_CONV_LIST + " (latestUpdateTime)";
	
	/**
	 * create emotion face list
	 */
	public static final String createTableEMOTIONFACE = "create table " + TB_EMOTIONFACE + " (_id INTEGER primary key autoincrement, " +
		"category varchar(128), pos int, filename varchar(256), string_chinese varchar(256), prop varchar(256))";
	
	/**
	 * create contact table
	 */
	public static final String createTableContact = "create table " + TB_CONTACT + " (_id INTEGER primary key autoincrement, " +
		"contactid varchar(128), contactname varchar(512), namepinyin varchar(512), faceurl varchar(512), position int, description varchar(128), " +
		"contacttype int, member_count int, extend varchar(512))";
	public static final String createIndexContact = "create index idx_contactid on " + TB_CONTACT + " (contactid)";
	public static final String createIndexContactName = "create index idx_contactname on " + TB_CONTACT + " (contactname)";
	
	/**
	 * create contact table
	 */
	public static final String createTableGroupMember = "create table " + TB_GROUPMEMBER + " (_id INTEGER primary key autoincrement, " +
		"groupid varchar(128), contactid varchar(128), position int, extend varchar(512))";
	public static final String createIndexGroupMember = "create index idx_groupmember_groupid on " + TB_GROUPMEMBER + " (groupid)";
	public static final String createIndexGroupMemberPos = "create index idx_groupmember_position on " + TB_GROUPMEMBER + " (position)";
	
	/**
	 * create user member table
	 */
//	public static final String createTableUser = "create table " + TB_USER + " (_id INTEGER primary key autoincrement, " +
//		"groupid varchar(128), groupname varchar(512), member_count int)";
//	public static final String createIndexUser = "create index idx_groupid on " + TB_USER + " (groupid)";
	
	/**
	 * create table for person or group converstaion history
	 */
	public static final String createTableConvHistory = "create table " + TB_CHAT_HISTORY + 
		" (_id INTEGER primary key autoincrement, convid varchar(128), direction int, msgid varchar(128), created_at int8, text varchar(4096), " +
		"sender_id int8, sender_name varchar(256), sender_profile_image_url varchar(256), ext_text varchar(1024), convtype int, " +
		"sendsucc int, thumbnail blob, extend varchar(512))";
	public static final String createIndexConvHistory_convid = "create index idx_convlist_convid on " + 
		TB_CHAT_HISTORY + " (convid)";
	public static final String createIndexConvHistory_updatetime = "create index idx_convlist_latestupdatetime on " + 
		TB_CHAT_HISTORY + " (created_at)";
	public static final String createIndexConvHistory_msgid = "create index idx_convlist_msgid on " + 
		TB_CHAT_HISTORY + " (msgid)";
	
	/**
	 * create table for chat images and voice
	 */
	public static final String createTableConvAttach = "create table " + TB_ATTACH + " (_id INTEGER primary key autoincrement, " +
		"attachtype int, attach blob, ext_attach_1 blob, ext_attach_2 blob, ext_attach_3 blob, ext_attach_4 blob, ext_attach_5 blob," +
		"extend varchar(512))";
	public static final String createIndexConvAttach = "create index idx_attach on " + TB_CHAT_HISTORY + " (attachtype)";
		
	private static String droptablesql = "drop table if exists $tbname$";
	
	public static void execSql(SQLiteDatabase sqlitedb, String sql){
	    try {
	    	sqlitedb.execSQL(sql);
	    } 
	    catch (SQLiteException e){
	    	DebugUtil.debug("The sql has been created "+sql);
	    }
	}
	
	public static void dropTable(SQLiteDatabase sqlitedb, String tablename) {
		String sql = droptablesql.replace("$tbname$", tablename);
		
		try {
	    	sqlitedb.execSQL(sql);
	    } 
		catch (SQLiteException e) {
	    	DebugUtil.debug("The sql has been created " + sql);
	    }
	}
	
	public static boolean contain(SQLiteDatabase sqlitedb,String TableName,String msgId,String folderId){
//		Cursor cursor = query(sqlitedb,TableName,msgId,folderId);
//		boolean flag = false;
//		if (null != cursor){
//			if(cursor.getCount() >= 1){
//				flag = true;
//			}
//			cursor.close();
//		}
//		return flag;
		return true;
	}
}
