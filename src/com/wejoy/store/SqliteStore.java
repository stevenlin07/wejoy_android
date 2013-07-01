package com.wejoy.store;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.LinkedBlockingQueue;

import net.sourceforge.pinyin4j.format.exception.BadHanyuPinyinOutputFormatCombination;

import org.json.JSONArray;
import org.json.JSONObject;

import com.weibo.login.AccessTokenKeeper;
import com.weibo.sdk.syncbox.type.pub.SyncBoxMessage;
import com.weibo.sdk.syncbox.utils.AuthModule;
import com.wejoy.module.ChatMessage;
import com.wejoy.module.ContactModule;
import com.wejoy.module.ConvertModule;
import com.wejoy.module.Emotion;
import com.wejoy.module.UserConfig;
import com.wejoy.module.UserInfo;
import com.wejoy.module.WeJoySysConfig;
import com.wejoy.sdk.service.SqliteModule;
import com.wejoy.ui.AppUtils; 
import com.wejoy.util.CommonUtil;
import com.wejoy.util.DebugUtil;
import com.wejoy.util.HanziToPingtin;
import com.wejoy.util.JsonBuilder;
import com.wejoy.util.JsonWrapper;
import com.wejoy.util.MD5;
import com.wejoy.util.SDCardUtil;
import com.wejoy.util.StringUtils;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.util.Log;
/**
 * 
 * @author WeJoy Group
 *
 */
public enum SqliteStore implements StoreConstant {
	
	INSTANCE;
	
	public static final String DATABASE_DIR = STORE_ROOT + "/database";
	/**
	 * @by jichao, 为了支持多用户登录，并且保存聊天历史记录，需要支持多db，每个用户账号拥有一个自己的db文件
	 */
	public static final String USR_DB = "wejoydb_$uid$.db";
	
	private static SqliteStore instance = null;
	private SQLiteDatabase sqlitedb = null;
	public static final String STORE_ERR = "存储不足或不可用，可能影响微聚的使用";
	private static final LinkedBlockingQueue<ChatMessage> updateImageStoreQ = new LinkedBlockingQueue<ChatMessage>();
	private static UpdateImageStoreT updateImageStoreT = null;
	private Map<String, String> uid2url = new HashMap<String, String>();
	
	private SQLiteDatabase getSqlitedb() {
		if(sqlitedb == null) {
			synchronized(SqliteStore.class) {
				if(sqlitedb == null) {
					updateImageStoreT = new UpdateImageStoreT();
					updateImageStoreT.start();
					
					if(DebugUtil.reBuildSchema) {
						// test, shoud be removed
						try {
							String currentuid = AccessTokenKeeper.getUid(AppUtils.context);
							initDatabase(currentuid);
							createSchema();
						} 
						catch (Exception e) {
							DebugUtil.error("SqliteStore", "rebuild schema", e);
						}
					}
				}
			}
		}
		System.out.println("sqlitedb:"+sqlitedb);
		return sqlitedb;
	}
	
	private SqliteStore() {
		getSqlitedb();
	}
	
	/** 只会在首次使用的时候调用*/
	public void createDatabase (String uid) {
		if (uid == null) {
			DebugUtil.error("SqliteStore", "createDatabase uid is null", null);
			return;
		}
		String dbFilePath = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + DATABASE_DIR;
		String dbFileName = USR_DB.replace("$uid$", uid);
		
		
	}
	
	public void initDatabase(String uid) {
		try {
			String dbFilePath = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + DATABASE_DIR;
			if(uid != null) {
				String dbFileName = USR_DB.replace("$uid$", uid);
				File dbfile = SDCardUtil.createFile(dbFilePath, dbFileName);
				
				if(sqlitedb != null) {
					sqlitedb.close();  
				}
				
				sqlitedb = SQLiteDatabase.openOrCreateDatabase(dbfile, null);
			}
			
			if(!isSqliteStoreInit()) {
				createSchema();
			}
		} 
		catch (Exception e) {
			DebugUtil.error("SqliteStore", "get database failed", e);
		}
	} 
	
	public boolean isSqliteStoreInit() {
		WeJoySysConfig config = querySysConfig();
		
		// TO DO
		if(config != null) {
			return true;
		}
		
		return false; 
	}
	
	public void createSchema() throws Exception {
		if(!SDCardUtil.isSDCardWritable()) {
			throw new Exception("sdcard error");
		}

		SqliteModule.createTable();
		
		// clear environment
		SqliteHelper.dropTable(sqlitedb, SqliteHelper.TB_CONV_LIST);
		SqliteHelper.dropTable(sqlitedb, SqliteHelper.TB_CHAT_HISTORY);
		SqliteHelper.dropTable(sqlitedb, SqliteHelper.TB_GROUP_STATUS);
		SqliteHelper.dropTable(sqlitedb, SqliteHelper.TB_ATTACH);
		SqliteHelper.dropTable(sqlitedb, SqliteHelper.TB_EMOTIONFACE);
		SqliteHelper.dropTable(sqlitedb, SqliteHelper.TB_CONTACT);
		SqliteHelper.dropTable(sqlitedb, SqliteHelper.TB_GROUPMEMBER);
		SqliteHelper.dropTable(sqlitedb, SqliteHelper.TB_EMOTIONFACE);
		SqliteHelper.dropTable(sqlitedb, SqliteHelper.TB_USR_CONFIG);
		
		// create schema
		SqliteHelper.execSql(sqlitedb, SqliteHelper.createTableConvList);
		SqliteHelper.execSql(sqlitedb, SqliteHelper.createIndexConvList);
		SqliteHelper.execSql(sqlitedb, SqliteHelper.createTableContact);
		SqliteHelper.execSql(sqlitedb, SqliteHelper.createIndexContact);
		SqliteHelper.execSql(sqlitedb, SqliteHelper.createIndexContactName);
		SqliteHelper.execSql(sqlitedb, SqliteHelper.createTableGroupMember);
		SqliteHelper.execSql(sqlitedb, SqliteHelper.createIndexGroupMember);
		SqliteHelper.execSql(sqlitedb, SqliteHelper.createIndexGroupMemberPos);
		SqliteHelper.execSql(sqlitedb, SqliteHelper.createTableConvHistory);
		SqliteHelper.execSql(sqlitedb, SqliteHelper.createIndexConvHistory_msgid);
		SqliteHelper.execSql(sqlitedb, SqliteHelper.createIndexConvHistory_updatetime);
		SqliteHelper.execSql(sqlitedb, SqliteHelper.createIndexConvHistory_convid);
		SqliteHelper.execSql(sqlitedb, SqliteHelper.createTableConvAttach);
		SqliteHelper.execSql(sqlitedb, SqliteHelper.createIndexConvAttach);
		SqliteHelper.execSql(sqlitedb, SqliteHelper.createTableEMOTIONFACE);
		SqliteHelper.execSql(sqlitedb, SqliteHelper.createTableUsrConfig);
		
		// @by jichao, 最后创建sys config，用来标识创建结束
		SqliteHelper.execSql(sqlitedb, SqliteHelper.createTableSysConfig);
		initSysConfig();
		initResources();
	}

	private void initSysConfig() {
		ContentValues cv = new ContentValues();
		cv.put("version", "01");
		cv.put("state", "init");
		cv.put("extend", "");
		sqlitedb.insert(SqliteHelper.TB_SYS_CONFIG, null, cv);
	}
	
	private void initResources() {
		EmotionStore.getInstance().initEmotionFromResource();
	}
	
	public ConvertModule queryConvById(String convid) {
		String selection = "convid=?";
		String[] selectionArgs = new String[] {String.valueOf(convid)};
		Cursor cursor = null;
		
		try {
			cursor = getSqlitedb().query(SqliteHelper.TB_CONV_LIST, null, selection, selectionArgs, null, null, null, null);

			if (cursor.getCount() >= 1) {
				cursor.moveToFirst();
				ConvertModule conv = getConvertFromCursor(cursor);
				return conv;
			}
		}
		catch(Exception e) {
			DebugUtil.debug("SqliteStore", "queryConvById", e);
		}
		
		return null;
	}
	
	public List<ConvertModule> queryConvList(int id, int count) {
		String selection = id >= 0 ? "_id<?" : null;
		String[] selectionArgs = id >= 0 ? new String[] {String.valueOf(id)} : null;
		Cursor cursor = null;
		List<ConvertModule> list = new ArrayList<ConvertModule>();
		try {
			cursor = getSqlitedb().query(SqliteHelper.TB_CONV_LIST, null, selection, selectionArgs, null, null, 
					"latestupdateTime desc", String.valueOf(count));
			if (cursor.getCount() >= 1) {
				cursor.moveToFirst();
				
				do {
					ConvertModule conv = getConvertFromCursor(cursor);
					list.add(conv);
		        } 
				while (cursor.moveToNext());
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (null != cursor) {
				try {
					cursor.close();
				}catch(Exception e) {
					
				}
			}
		}
		return list;
	}
	
	private ConvertModule getConvertFromCursor(Cursor cursor) {
		ConvertModule conv = new ConvertModule();
		conv.convName = cursor.getString(cursor.getColumnIndex("convname"));
		// conv.stickyTopic = cursor.getString(cursor.getColumnIndex("stickytopic"));
		conv.convName = (conv.convName != null && conv.convName.length() > 10) ?
			conv.convName.substring(0, 10) + "..." :
			conv.convName;
		conv.latestUpdateUserName = cursor.getString(cursor.getColumnIndex("latestupdateusername"));
		conv.setLatestUpdateMessage(cursor.getString(cursor.getColumnIndex("latestupdatemessage")));
		conv.latestUpdateTime = cursor.getLong(cursor.getColumnIndex("latestupdateTime"));  
		conv.unreadCount = cursor.getInt(cursor.getColumnIndex("unreadcount"));  
		conv.convListFacePic = cursor.getString(cursor.getColumnIndex("convlistfacepic"));
		conv.convid = cursor.getString(cursor.getColumnIndex("convid"));
		conv.convtype = cursor.getInt(cursor.getColumnIndex("convtype"));
		conv.position = cursor.getInt(cursor.getColumnIndex("position"));
		conv.chatwithid = cursor.getString(cursor.getColumnIndex("chatwithid"));
		
		return conv;
	}
	
	public void saveEmotions(List<Emotion> emotions) {
		sqlitedb.beginTransaction();
		
		for(Emotion e : emotions) {
			ContentValues cv = new ContentValues();
			cv.put("filename", e.filename);
			cv.put("string_chinese", e.string_chinese);
			cv.put("category", e.category);
			cv.put("pos", e.pos);
			sqlitedb.insert(SqliteHelper.TB_EMOTIONFACE, null, cv);
		}
		
		sqlitedb.setTransactionSuccessful();
		sqlitedb.endTransaction();
	}
	
	public List<Emotion> getEmotions() {
		List<Emotion> emotions = new ArrayList<Emotion>();
		Cursor cursor = null;
		
		try {
			cursor = getSqlitedb().query(SqliteHelper.TB_EMOTIONFACE, null, null, null, null, null, null, null);

			if (cursor.getCount() >= 1) {
				cursor.moveToFirst();
				
				do {
					Emotion emotion = new Emotion();
					emotion.filename = cursor.getString(cursor.getColumnIndex("filename"));
					emotion.string_chinese = cursor.getString(cursor.getColumnIndex("string_chinese"));
					emotion.pos = cursor.getInt(cursor.getColumnIndex("pos"));
					emotion.category = cursor.getString(cursor.getColumnIndex("category"));
					emotions.add(emotion);
		        } 
				while (cursor.moveToNext());

			}
		}
		catch(Exception e) {
			DebugUtil.debug("SqliteStore", "queryConvById", e);
		}
		
		return emotions;
	}
	
	public String getEmotionCNFromDB (int position) {
		String strtmp = null;
		
		if(position <10) {
			strtmp = "face00" + position;
		}
		else if(position < 100) {
			strtmp = "face0" + position;
		}
		else {
			strtmp = "face" + position;
		}
		
		Cursor cursor = getSqlitedb().query(SqliteHelper.TB_EMOTIONFACE, new String[]{"string_chinese"}, "filename like ?", 
										new String[]{strtmp}, null,null, "category desc");
		
		while(cursor.moveToNext()){
			int index = cursor.getColumnIndex("string_chinese");
			String str = cursor.getString(index);
			return str;
		}
		
		return null;
	}
	
	public void insertGroupContact(String listid, String name, List<ContactModule> groupInfoList) {
		StringBuilder sb = new StringBuilder();
		
		if(groupInfoList.size() >= 4) {
			for(int i=0; i<4; i++) {
				String str = uid2url.get(groupInfoList.get(i).contactId);
				if(str == null) {
					sb.append("");
				} else {
					sb.append(str);
					if (i < 3)
					sb.append(",");
				}
			}
		}
		else {
			for(int i=0; i<groupInfoList.size();i++) {
				String str = uid2url.get(groupInfoList.get(i).contactId);
				if(str == null) {
					sb.append("");
				} else {
					sb.append(str);
					if (i < groupInfoList.size()-1)
					sb.append(",");
				}
			}
		}

		String urltmp = sb.toString();
		
		String whereClause = "contactid=?";
		String whereArgs[] = new String[] {listid};
		
		try {
			ContentValues values = new ContentValues();
			values.put("faceurl", urltmp);
			values.put("member_count", groupInfoList.size());
			getSqlitedb().update(SqliteHelper.TB_CONTACT, values, whereClause, whereArgs);
		}
		catch (Exception e) {
			DebugUtil.error("dbwriting error", e.toString());
		}
		
	}
	
	private ContentValues getContactCV(ContactModule contact) {
		ContentValues cv = new ContentValues();
		cv.put("contactid", contact.contactId);
		cv.put("contactname", contact.name);
		cv.put("faceurl", contact.faceurl);
		cv.put("description", contact.description);
		cv.put("contacttype", contact.contacttype);
		cv.put("extend", contact.extend);

		try {
			String pinyin = HanziToPingtin.getEname(contact.name).toLowerCase();
			cv.put("namepinyin", pinyin);
		} 
		catch (BadHanyuPinyinOutputFormatCombination e) {
			DebugUtil.error("", "", e);
		}
		
		if(contact.contacttype == ContactModule.CONTACT_TYPE_GROUP) {
			cv.put("member_count", contact.memberCount);
		}
		
		return cv;
	}
	
	public void insertContact(ContactModule contact) {
		ContentValues cv = getContactCV(contact);

		getSqlitedb().insert(SqliteHelper.TB_CONTACT, null, cv);
	}
	
	public void insertContactInBatch(List<ContactModule> contacts) {
		getSqlitedb().beginTransaction();
		
		for(ContactModule contact : contacts) {
			ContentValues cv = getContactCV(contact);
			getSqlitedb().insert(SqliteHelper.TB_CONTACT, null, cv);
		}
		
		getSqlitedb().setTransactionSuccessful();        //设置事务处理成功，不设置会自动回滚不提交
		getSqlitedb().endTransaction();
	}
	
//	public void insertGroupAsContact(JSONObject jvalue) {
//		try {
//			String Insert_Data_GroupAsContact="INSERT INTO contact ('contactid','contactname','faceurl','description','contacttype'," +
//					"'member_count') VALUES(" +
//					Long.valueOf(jvalue.getString("list_id")) + ",'" + jvalue.getString("name") + "','" + "" + "','" + "empty" +
//					"'," + ContactModule.CONTACT_TYPE_GROUP + "," + 1 + ")";
//			sqlitedb.execSQL(Insert_Data_GroupAsContact);
//		}
//		catch (Exception e) {
//			DebugUtil.error("dbwriting error", e.toString());
//		}
//	}
//	
//	public void insertFriendContact(JSONObject jb) {
//		try {
//			String Insert_Data="INSERT INTO contact ('contactid','contactname','faceurl','description','contacttype'," +
//					"'member_count') VALUES(" +
//					jb.getLong("id") + ",'" + jb.getString("name") + "','" + jb.getString("profile_image_url") + "','" + jb.getString("screen_name") +
//					"'," + ContactModule.CONTACT_TYPE_SINGLE + "," + 1 + ")";
//			sqlitedb.execSQL(Insert_Data);
//			
//			uid2url.put(String.valueOf(jb.getLong("id")), jb.getString("profile_image_url"));	
//			
///*			String Insert_Data_Group="INSERT INTO groupmember ('groupid','contactid','position') VALUES('" +       //插入到组中
//					listIdString + "','" + jvalue.getString("user_id") + "'," + i + ")";
//			sqlitedb.execSQL(Insert_Data_Group);*/
//		}
//		catch (Exception e) {
//			DebugUtil.error("dbwriting error", e.toString());
//		}
//	}
	
	public byte[] getAttachBlob(String attachid) {
		String selection = "_id=?";
		String selectionArgs[] = new String[] {String.valueOf(attachid)};
		Cursor cursor = null;
		byte[] data = null;
		try {
			cursor = getSqlitedb().query(SqliteHelper.TB_ATTACH, null, selection, selectionArgs, null, null, null, null);
			if (cursor.getCount() >= 1) {
				cursor.moveToFirst();
				data = cursor.getBlob(cursor.getColumnIndex("attach"));
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (null != cursor) {
				try {
					cursor.close();
				}catch(Exception e) {
					
				}
			}
		} 
		return data;
	}
	
	public ChatMessage queryChatMessage(long msgid) {
		String[] selectionArgs = new String[] {String.valueOf(msgid)};
		String selection = "msgid=?";
		ChatMessage msg = null;
		Cursor cursor = null;
		try {
			cursor = getSqlitedb().query(SqliteHelper.TB_CHAT_HISTORY, null, selection, selectionArgs, null, null, null, null);
			if (cursor.getCount() >= 1) {
				cursor.moveToFirst();
				msg = getMessageFromCursor(cursor);
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (null != cursor) {
				try {
					cursor.close();
				}catch(Exception e) {
					
				}
			}
		} 
		return msg;
	}
	
	public ChatMessage queryChatMessageById(String _id) {
		String selection = "_id=?";
		String[] selectionArgs = new String[] {_id};
		Cursor cursor = null;
		ChatMessage msg = null;
		try {
			cursor = getSqlitedb().query(SqliteHelper.TB_CHAT_HISTORY, null, selection, selectionArgs, null, null, null, null);
			msg = cursor.getCount() >= 1 ? getMessageFromCursor(cursor) : null;
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (null != cursor) {
				try {
					cursor.close();
				}catch(Exception e) {
					
				}
			}
		} 
		return msg;
	}
	
	public void updateStateByChatMsgId(ChatMessage chat) {
		ContentValues cv = new ContentValues();
		cv.put("sendsucc", chat.sendState);
		String whereClause = "msgid=?";
		String[] whereArgs = new String[] {String.valueOf(chat.msgid)};
		sqlitedb.update(SqliteHelper.TB_CHAT_HISTORY, cv, whereClause, whereArgs);
	}
	
	public void updateContact(ContactModule contact) {
		String whereClause = "contactid=?";
		String[] whereArgs = new String[] {contact.contactId};
		ContentValues cv = getContactCV(contact);
		
		getSqlitedb().update(SqliteHelper.TB_CONTACT, cv, whereClause, whereArgs);
	}
	
	public void updateConvert(ConvertModule convert) {
		String whereClause = "convid=?";
		String[] whereArgs = new String[] {convert.convid};
		ContentValues cv = getConvertCVForUpdate(convert);

		getSqlitedb().update(SqliteHelper.TB_CONV_LIST, cv, whereClause, whereArgs);
	}	
	/**
	 * 倒序获取聊天历史记录
	 * @param convid
	 * @param id
	 * @param count
	 * @return
	 */
	public List<ChatMessage> queryConvHistory(String convid, long created_at, int count) {
		String[] selectionArgs = null;
		String selection = "convid=?";
		
		if(created_at >= 0) {
			selection += " and created_at<?";
			selectionArgs = new String[2];
			selectionArgs[0] = String.valueOf(convid);
			selectionArgs[1] = String.valueOf(created_at);
		}
		else {
			selectionArgs = new String[1];
			selectionArgs[0] = String.valueOf(convid);
		}
		Cursor cursor = null;
		List<ChatMessage> list = null;
		try {
			cursor = getSqlitedb().query(SqliteHelper.TB_CHAT_HISTORY, null, selection, selectionArgs, null, null, 
					"created_at desc", String.valueOf(count));
			list = new ArrayList<ChatMessage>();
			if (cursor.getCount() >= 1) {
				cursor.moveToFirst();
				
				do {
					ChatMessage msg = getMessageFromCursor(cursor);
					list.add(msg);
		        } while (cursor.moveToNext());
				java.util.Collections.reverse(list);
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (null != cursor) {
				try {
					cursor.close();
				}catch(Exception e) {
					
				}
			}
		} 
		return list;
	}
	
	private ChatMessage getMessageFromCursor(Cursor cursor) {
		ChatMessage msg = new ChatMessage();
		msg.id = cursor.getInt(cursor.getColumnIndex("_id"));
		msg.convid = cursor.getString(cursor.getColumnIndex("convid"));
		msg.direction = cursor.getInt(cursor.getColumnIndex("direction"));
		msg.type =  cursor.getInt(cursor.getColumnIndex("convtype"));
		msg.sendState = cursor.getInt(cursor.getColumnIndex("sendsucc"));
		msg.created_at = cursor.getLong(cursor.getColumnIndex("created_at"));
		msg.msgid = cursor.getString(cursor.getColumnIndex("msgid"));

		msg.uid = cursor.getString(cursor.getColumnIndex("sender_id"));
		msg.usrname = cursor.getString(cursor.getColumnIndex("sender_name"));
		msg.userFaceUrl = cursor.getString(cursor.getColumnIndex("sender_profile_image_url"));
		
		if(msg.type == ChatMessage.TYPE_AUDIO) {
			String ext_text = cursor.getString(cursor.getColumnIndex("ext_text"));
			msg.parseContentExt(ext_text);
		}
		else if(msg.type == ChatMessage.TYPE_PIC) {
			String ext_text = cursor.getString(cursor.getColumnIndex("ext_text"));
			msg.parseContentExt(ext_text);
			byte[] data = cursor.getBlob(cursor.getColumnIndex("thumbnail"));
			msg.thumbnail = BitmapFactory.decodeByteArray(data, 0, data.length);
		}
		else {
			msg.content = cursor.getString(cursor.getColumnIndex("text"));	
		}
		
		return msg;
	}
	
	public List<ContactModule> queryAllContact() {
		return queryAllContact(ContactModule.CONTACT_TYPE_SINGLE);
	}
	
	public List<ContactModule> queryAllContact(int contactType) {
		Cursor cursor = null;
		List<ContactModule> list = new ArrayList<ContactModule>();
		try {
			
			cursor = getSqlitedb().query(SqliteHelper.TB_CONTACT, null, null, null, null, null, "namepinyin");
			
			if (cursor.getCount() >= 1) {
				cursor.moveToFirst();
				
				do {
					ContactModule contact = new ContactModule();
					contact._id = cursor.getInt(cursor.getColumnIndex("_id"));
					contact.contactId = cursor.getString(cursor.getColumnIndex("contactid"));
					contact.name = cursor.getString(cursor.getColumnIndex("contactname"));
					contact.faceurl =  cursor.getString(cursor.getColumnIndex("faceurl"));
					contact.position = cursor.getInt(cursor.getColumnIndex("position"));
					contact.description = cursor.getString(cursor.getColumnIndex("description"));
					contact.memberCount = cursor.getInt(cursor.getColumnIndex("member_count"));
					contact.contacttype = cursor.getInt(cursor.getColumnIndex("contacttype"));
					contact.extend = cursor.getString(cursor.getColumnIndex("extend"));
					
					if(contact.contacttype != contactType) {
						continue;
					}
					
					// don't add owner into contact list
					if(contact.contactId == null ||
						contact.contactId.equals(AuthModule.INSTANCE.getUid())) 
					{
						continue;
					}
					
					list.add(contact);
		        } while (cursor.moveToNext());
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (null != cursor) {
				try {
					cursor.close();
				}catch(Exception e) {
					
				}
			}
		} 
		return list;
	}
	
	public ContactModule queryContactById(String contactid) {
		String whereClause = "contactid=?";
		String whereArgs[] = new String[] {contactid};
		ContactModule contact = null;
		Cursor cursor = null;
		try {
			cursor = getSqlitedb().query(SqliteHelper.TB_CONTACT, null, whereClause, whereArgs, null, null, null, null);
			if(cursor.getCount() >= 1) {
				cursor.moveToFirst();
				
				contact = new ContactModule();
				contact._id = cursor.getInt(cursor.getColumnIndex("_id"));
				contact.contactId = cursor.getString(cursor.getColumnIndex("contactid"));
				contact.name = cursor.getString(cursor.getColumnIndex("contactname"));
				contact.faceurl =  cursor.getString(cursor.getColumnIndex("faceurl"));
				contact.position = cursor.getInt(cursor.getColumnIndex("position"));
				contact.description = cursor.getString(cursor.getColumnIndex("description"));
				contact.memberCount = cursor.getInt(cursor.getColumnIndex("member_count"));
				contact.contacttype = cursor.getInt(cursor.getColumnIndex("contacttype"));
				contact.extend = cursor.getString(cursor.getColumnIndex("extend"));
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (null != cursor) {
				try {
					cursor.close();
				}catch(Exception e) {
					
				}
			}
		} 
		return contact;
	}
	
	public void removeChatMessage(String _id, String _attachid) {
		String whereClause = "_id=?";
		String whereArgs[] = new String[] {_id};
		
		sqlitedb.delete(SqliteHelper.TB_CHAT_HISTORY, whereClause, whereArgs);
		
		if(_attachid != null) {
			whereArgs = new String[] {_attachid};
			sqlitedb.delete(SqliteHelper.TB_ATTACH, whereClause, whereArgs);
		}
	}
	
	public void removeChatMessage(String _id) {
		ChatMessage chat = this.queryChatMessageById(_id);
		
		String whereClause = "_id=?";
		String whereArgs[] = new String[] {_id};
		sqlitedb.delete(SqliteHelper.TB_CHAT_HISTORY, whereClause, whereArgs);
		
		if(chat != null && chat.attachId != null) {
			whereClause = "_id=?";
			whereArgs = new String[] {chat.attachId};
			sqlitedb.delete(SqliteHelper.TB_ATTACH, whereClause, whereArgs);
		}
	}
	
	public void removeGroupMember(String groupId, String contactId){
		String whereClause = "contactid=? and groupid=?";
		String whereArgs[] = new String[] {contactId, groupId};
		
		sqlitedb.delete(SqliteHelper.TB_GROUPMEMBER, whereClause, whereArgs);
	}
	
	public void removeGroupByGroupId(String groupid){
		String whereClause = "groupid=?";
		String whereArgs[] = new String[] {groupid};
		
		sqlitedb.delete(SqliteHelper.TB_GROUPMEMBER, whereClause, whereArgs);
	}
	
	public void removeContact(String _id) {
		String whereClause = "contactid=?";
		String whereArgs[] = new String[] {_id};
		
		sqlitedb.delete(SqliteHelper.TB_CONTACT, whereClause, whereArgs);
	}
	
	public void removeAllGroupMember(){
		sqlitedb.delete(SqliteHelper.TB_GROUPMEMBER, null, null);
	}
	
	public void removeAllContact() {
		sqlitedb.delete(SqliteHelper.TB_CONTACT, null, null);
	}
	
	public List<ContactModule> queryGroupMember(String groupid) {
		String selection = "groupid=?";
		String selectionArgs[] = new String[] {groupid};
		List<ContactModule> list = new ArrayList<ContactModule>();
		Cursor cursor = null;
		try {
			cursor = sqlitedb.query(SqliteHelper.TB_GROUPMEMBER, null, selection, selectionArgs, null, null,"position asc", null);
			if (cursor.getCount() >= 1) {
				cursor.moveToFirst();
				
				do {
					String contactid = cursor.getString(cursor.getColumnIndex("contactid"));
					ContactModule contact = queryContactById(contactid);
					
					if(contact != null) {
						list.add(contact);
					}
		        } while (cursor.moveToNext());
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (null != cursor) {
				try {
					cursor.close();
				}catch(Exception e) {
					
				}
			}
		} 
		return list;
	}
	
	public void insertNewConversation(ConvertModule conv) {
		ContentValues cv = getConvertCVForUpdate(conv);

		// set fields that can't be updated
		cv.put("convid", conv.convid);
		cv.put("convtype", conv.convtype);
		cv.put("convname", conv.convName);
		cv.put("chatwithid", conv.chatwithid);
		
		sqlitedb.insert(SqliteHelper.TB_CONV_LIST, null, cv);
	}
	
	/**
	 * Only a few fields could be updated
	 * @param conv
	 * @return
	 */
	private ContentValues getConvertCVForUpdate(ConvertModule conv) {
		ContentValues cv = new ContentValues();
		cv.put("stickytopic", conv.stickyTopic);
		cv.put("latestupdateusername", conv.latestUpdateUserName);
		cv.put("latestupdatemessage", conv.getLatestUpdateMessage());
		cv.put("latestupdateTime", conv.latestUpdateTime);
		cv.put("unreadcount", conv.unreadCount);
		cv.put("convlistfacepic", conv.convListFacePic);
		cv.put("position", conv.position);
		
		return cv;
	}
	
	public void clearGroupMembers(String groupid) {
		String whereClause = "groupid=?";
		String[] whereArgs = new String[] {groupid};
		sqlitedb.delete(SqliteHelper.TB_GROUPMEMBER, whereClause, whereArgs);
	}
	
	public void removeConversation(String convid) {
		String whereClause = "convid=?";
		String[] whereArgs = new String[] {convid};
		sqlitedb.delete(SqliteHelper.TB_CONV_LIST, whereClause, whereArgs);
	}
	
	public void addGroupMember(String groupid, List<String> memberIds) {
		sqlitedb.beginTransaction();
		
		for(String id : memberIds) {
			String sql = "INSERT INTO groupmember ('groupid','contactid') VALUES('" + groupid + "','" + id + "')";
			sqlitedb.execSQL(sql);
		}
		
		sqlitedb.setTransactionSuccessful();        //设置事务处理成功，不设置会自动回滚不提交
		sqlitedb.endTransaction(); 
	}
	
	public void insertNewGroup(ContactModule group, List<String> memberIds) {
		sqlitedb.beginTransaction();
		
		this.insertContact(group);
		String groupid = group.contactId;
		
		for(String id : memberIds) {
			String sql = "INSERT INTO groupmember ('groupid','contactid') VALUES('" + groupid + "','" + id + "')";
			sqlitedb.execSQL(sql);
		}
		
		sqlitedb.setTransactionSuccessful();        //设置事务处理成功，不设置会自动回滚不提交
		sqlitedb.endTransaction(); 
	}
	public void insertOrUpdateChatMessage(ChatMessage chat) {
		ContentValues cv = getChatContentValues(chat);
		
		String selection = "msgid=?";
		String selectionArgs[] = new String[] {chat.msgid};
		Cursor cursor = null;
		boolean hasExistBefore = false;
		
		try {
			cursor = sqlitedb.query(SqliteHelper.TB_CHAT_HISTORY, null, selection, selectionArgs, null, null, null);
			
			if (cursor.getCount() >= 1) {
				chat.id = sqlitedb.update(SqliteHelper.TB_CHAT_HISTORY, cv, selection, selectionArgs);
				hasExistBefore = true;
			}
		}
		catch(Exception e) {
		}
		
		if(!hasExistBefore) {
			insertChatMessage(chat, cv);
		}
	}
	
	private ContentValues getChatContentValues(ChatMessage chat) {
		ContentValues cv = new ContentValues();
		cv.put("convid", chat.convid);
		cv.put("created_at", chat.created_at);
		cv.put("sender_id", chat.uid);
		cv.put("sender_name", chat.usrname);
		cv.put("sendsucc", chat.sendState);
		cv.put("sender_profile_image_url", chat.userFaceUrl);
		cv.put("direction", chat.direction);
		cv.put("msgid", chat.msgid);
		cv.put("convtype", chat.type);
		cv.put("text", chat.content);
		
		if(chat.type == ChatMessage.TYPE_AUDIO) {
			cv.put("ext_text", chat.getContentExt());
		}
		else if(chat.type == ChatMessage.TYPE_PIC) { 
			cv.put("ext_text", chat.getContentExt());
			cv.put("thumbnail", CommonUtil.bitmap2Bytes(chat.thumbnail));
		}
		
		return cv;
	}
	
	public void insertChatMessage(ChatMessage chat) 
	{
		ContentValues cv = getChatContentValues(chat);
		insertChatMessage(chat, cv);
	}
	
	private void insertChatMessage(ChatMessage chat, ContentValues cv) {
		chat.id = sqlitedb.insert(SqliteHelper.TB_CHAT_HISTORY, null, cv);
			
		// @by jichao, need to update image path from temparay folder
		if(chat.type == ChatMessage.TYPE_PIC && chat.attachPath != null) {
			updateImageStoreQ.add(chat);
		}
	}
	
	public WeJoySysConfig querySysConfig() {
		WeJoySysConfig config  = null;
		Cursor cursor = null;
		
		try {
			cursor = sqlitedb.query(SqliteHelper.TB_SYS_CONFIG, null, null, null, null, null, null, null);

			if (cursor.getCount() >= 1) {
				cursor.moveToFirst();
				config = new WeJoySysConfig();
				config.version = cursor.getString(cursor.getColumnIndex("version"));
				config.state = cursor.getString(cursor.getColumnIndex("state"));
				config.extend = cursor.getString(cursor.getColumnIndex("extend"));
			}
		}
		catch(Exception e) {
			DebugUtil.debug("SqliteStore", "querySysConfig", e);
		}
		
		return config;
	}
	
	public WeJoySysConfig updateSysConfig(WeJoySysConfig config) {
		try {
			ContentValues cv = new ContentValues();
			cv.put("version", config.version);
			cv.put("state", config.state);
			cv.put("extend", config.extend);
			
			sqlitedb.delete(SqliteHelper.TB_SYS_CONFIG, null, null);
			sqlitedb.insert(SqliteHelper.TB_SYS_CONFIG, null, cv);
		}
		catch(Exception e) {
			DebugUtil.debug("SqliteStore", "querySysConfig", e);
		}
		
		return config;
	}
	
	public void updateUsrConfig(UserConfig config) {
		try {
			ContentValues cv = new ContentValues();
			cv.put("version", config.version);
			cv.put("basic", config.toJson());
			// @by jichao, no extend so far, all info of userconfig is put into basic
			// cv.put("extend", config.extend);
			
			sqlitedb.delete(SqliteHelper.TB_USR_CONFIG, null, null);
			sqlitedb.insert(SqliteHelper.TB_USR_CONFIG, null, cv);
		}
		catch(Exception e) {
			DebugUtil.debug("SqliteStore", "querySysConfig", e);
		}
	}
	
	public UserConfig getUsrConfig() {
		UserConfig config = new UserConfig();
		
		// 当用户第一次注册时, db有可能还没有建立
		if(sqlitedb == null) {
			return config;
		}
		
		Cursor cursor = null;
		
		try {
			cursor = sqlitedb.query(SqliteHelper.TB_USR_CONFIG, null, null, null, null, null, null, null);

			if (cursor.getCount() >= 1) {
				cursor.moveToFirst();
				String basic = cursor.getString(cursor.getColumnIndex("basic"));
				config.parseJson(basic);
			}
		}
		catch(Exception e) {
			DebugUtil.debug("SqliteStore", "queryConvById", e);
		}
		
		return config;
	}
	
	private class UpdateImageStoreT extends Thread {
		public void run() {
			ChatMessage chat = null;
			
			while(true) {
				try {
					chat = updateImageStoreQ.take();
					if(chat.type == ChatMessage.TYPE_PIC) {
						FileOutputStream fout = null; 
						FileInputStream fin = null;
						
						try {
							String newfilepath = ImageStore.getImageStorePath() + File.separator + MD5.getMD5(chat.attachPath);
							fout = new FileOutputStream(newfilepath);
							fin = new FileInputStream(chat.attachPath);
							byte[] buffer = new byte[1024];
							int numRead;
							
							while ( (numRead = fin.read(buffer) ) >= 0) {
								fout.write(buffer, 0, numRead);
							}
						}
						finally {
							if(fout != null) {
								fout.close();
							}
							
							if(fin != null) {
								fin.close();
							}
						}
					}
				}
				catch(Exception e) {
					DebugUtil.error("", "", e);
				}
			}
		}
	}
}
