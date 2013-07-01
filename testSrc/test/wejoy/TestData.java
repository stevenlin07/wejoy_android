package test.wejoy;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.wejoy.R;
import com.wejoy.module.ConvertModule;
import com.wejoy.store.SqliteHelper;
import com.wejoy.store.SqliteStore;
import com.wejoy.util.DebugUtil;
import com.wejoy.util.JsonBuilder;
import com.wejoy.util.JsonWrapper;

public class TestData {
	private void cleardb() {
		SqliteStore ss = new SqliteStore();
		SQLiteDatabase sqlitedb = ss.getDatabase();
		sqlitedb.delete(SqliteHelper.TB_CONV_LIST, null, null);
		sqlitedb.delete(SqliteHelper.TB_CHAT_HISTORY, null, null);
		sqlitedb.delete(SqliteHelper.TB_ATTACH, null, null);
		sqlitedb.delete(SqliteHelper.TB_EMOTIONFACE, null, null);
		sqlitedb.delete(SqliteHelper.TB_CONTACT, null, null);
	}
	
	public void addTestData() {
		String s = null;
		
		try {
			SqliteStore ss = new SqliteStore();
			SQLiteDatabase sqlitedb = ss.getDatabase();
			sqlitedb.delete(SqliteHelper.TB_CONV_LIST, null, null);
			sqlitedb.delete(SqliteHelper.TB_CHAT_HISTORY, null, null);
			sqlitedb.delete(SqliteHelper.TB_ATTACH, null, null);
			sqlitedb.delete(SqliteHelper.TB_EMOTIONFACE, null, null);
			sqlitedb.delete(SqliteHelper.TB_CONTACT, null, null);
			android.content.res.Resources myResources = getResources();
			java.io.InputStream myFile = myResources.openRawResource(R.raw.test_data);
			java.io.InputStreamReader fr = new java.io.InputStreamReader(myFile);
			java.io.BufferedReader br = new java.io.BufferedReader(fr);
			sqlitedb.beginTransaction(); 
			
			while((s = br.readLine()) != null) {
				if(s.indexOf("convlist=") >= 0) {
					sqlitedb.execSQL(getInsertConvListSql(s.substring("convlist=".length())));
				}
				// comment
				else if(s.indexOf("convhistory=") >= 0) {
					sqlitedb.execSQL(getInsertConvHistorySql(s.substring("convhistory=".length())));
				}
				else if(s.indexOf("contact=") >= 0) {
					sqlitedb.execSQL(getInsertContactSql(s.substring("contact=".length())));
				}
				else if(s.indexOf("groupmember=") >= 0) {
					sqlitedb.execSQL(getInsertGroupMemberSql(s.substring("groupmember=".length())));
				}
			}
			
			sqlitedb.setTransactionSuccessful();
			sqlitedb.endTransaction();
			
			
			// prepare a image chat
			java.io.InputStream in = myResources.openRawResource(R.drawable.guide01);
			int bufSize = 8*1024;
			byte[] buffer  = new byte[bufSize];
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			
			try {
				int size = 0;
				
				while((size = in.read(buffer)) >= 0){
					out.write(buffer, 0, size);
				}
				
				out.flush();
				out.close();
			} catch (IOException e) {
				Log.e("ERROR", e.toString());
			}
			  
			insertMediaConv(sqlitedb, "101", ConvertModule.GROUP_CONV_IMAGE, "1qaz", out.toByteArray());
		}
		catch(Exception e) {
			DebugUtil.debug("SqliteStore.addTestData", "parse [" + s + "] failed caused by " + e.getMessage(), e);
		}
	}
	
	private String getInsertGroupMemberSql(String contact) throws IOException {
		JsonWrapper json = new JsonWrapper(contact);
		String Insert_Data="INSERT INTO groupmember ('groupid','contactid','position') VALUES('" +
			json.get("groupid") + "','" + json.get("contactid") + "'," + json.getInt("position") + ")";
		return Insert_Data;
	}
	
	private String getInsertContactSql(String contact) throws IOException {
		JsonWrapper json = new JsonWrapper(contact);
		String Insert_Data="INSERT INTO contact ('contactid','contactname','faceurl','description','contacttype'," +
			"'member_count') VALUES(" +
			json.get("contactid") + ",'" + json.get("contactname") + "','" + json.get("faceurl") + "','" + json.get("description") +
			"'," + json.getInt("contacttype") + "," + json.get("member_count") + ")";
		return Insert_Data;
	}
	
	private String getInsertConvListSql(String convlist01) throws IOException {
		JsonWrapper json = new JsonWrapper(convlist01);
		String Insert_Data="INSERT INTO convlist ('convid','stickytopic','latestupdateTime','latestupdateusername'," +
			"'latestupdatemessage','unreadcount','convlistfacepic','convtype','statusid','statususerid','chatwithid') VALUES(" +
			json.get("convid") + ",'" + json.get("stickytopic") + "'," + json.get("latestupdateTime") + ",'" + json.get("latestupdateusername") +
			"','" + json.get("latestupdatemessage") + "'," + json.get("unreadcount") + ",'" + json.get("convlistfacepic") +
			"'," + json.get("convtype") + "," + json.get("statusid") + "," + json.get("statususerid") + ",'" + json.get("chatwithid") +
			"')";
		return Insert_Data;
	}
	
	private String getInsertConvHistorySql(String convlist01) throws IOException {
		JsonWrapper json = new JsonWrapper(convlist01);
		String Insert_Data="INSERT INTO convhistory ('convid','msgid','created_at','text','sender_id','sender_name'," +
			"'sender_profile_image_url','convtype','direction') VALUES(" +
			json.get("convid") + ",'" + json.get("msgid") + "'," + json.get("created_at") + ",'" + json.get("text") + "'," + 
			json.getInt("sender_id") + ",'" + json.get("sender_name") + "','" + json.get("sender_profile_image_url") + "'," + 
			json.get("convtype") + "," + json.getInt("direction") + ")";
		return Insert_Data;
	}
	
	private void insertMediaConv(SQLiteDatabase sqlitedb, String firstConvId, int mediaType, String attachid, byte[] attach) throws IOException {
		JsonBuilder json = new JsonBuilder();
		
		if(mediaType == ConvertModule.GROUP_CONV_VOICE) {
			json.append("length", 5);
		}
		
		json.append("attachid", attachid);
		
		String Insert_Data="INSERT INTO convhistory ('convid','msgid','created_at','ext_text','sender_id','sender_name','sender_profile_image_url','convtype') " +
			"VALUES(" + firstConvId + ",'00xx001'," + System.currentTimeMillis() + ",'" + JsonBuilder.toJsonStr(json.flip().toString()) + "'," + 1000 + 
			",'吴际超','http://tp4.sinaimg.cn/1793692835/180/5604882954/1'," + mediaType + ")";
		sqlitedb.execSQL(getInsertConvHistorySql(Insert_Data));
		
		ContentValues values = new ContentValues();
		values.put("attachid", attachid);
		values.put("attachtype", mediaType);
		values.put("attach", attach);
		sqlitedb.insert(SqliteHelper.TB_ATTACH, null, values);
	}
}
