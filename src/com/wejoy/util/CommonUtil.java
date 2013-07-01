package com.wejoy.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import com.weibo.login.AccessTokenKeeper;
import com.weibo.login.Oauth2AccessToken;
import com.weibo.sdk.syncbox.utils.AuthModule;
import com.wejoy.module.ContactModule;
import com.wejoy.store.DataStore;
import com.wejoy.store.TempFolderStore;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.content.Context;
import android.graphics.Bitmap;
import android.media.MediaPlayer;
import android.os.Environment;
import android.text.TextUtils;
/**
 * 
 * @author WeJoy Group
 *
 */
public class CommonUtil {
	public static final String WEJOY_AUDIO_PREFIX = "wejoy_audio";
	
	public static final String WEJOY_SOURCE = "1124443769";
	private static final Calendar calendar = Calendar.getInstance();
	
	/**
	 * standard time 
	 */
	public static final long MIN_5 = 1000 * 300L;
	public static final long MIN_10 = MIN_5 * 2;
	public static final long MIN_20 = MIN_10 * 2;
	public static final long MIN_30 = MIN_10 * 3;
	public static final long HOUR_1 = MIN_10 * 6;
	public static final long DAY_1 = HOUR_1 * 24l;
	
	public static String getDisplayTime(long time) {
		calendar.setTime(new Date(time));
		int min = calendar.get(Calendar.MINUTE);
		String minstr = min < 10 ? 
				"0" + min : 
				min + "";
		
		String s = new StringBuilder().append(
			(Calendar.getInstance().get(Calendar.DAY_OF_YEAR) == calendar.get(Calendar.DAY_OF_YEAR) ?
				"今天" :
				calendar.get(Calendar.MONTH) + "月" + calendar.get(Calendar.DAY_OF_MONTH) + "日  "))
			.append( 
			(calendar.get(Calendar.AM_PM) == 0 ? 
				(calendar.get(Calendar.HOUR_OF_DAY) <= 9 ? "早上" : "上午") : 
				(calendar.get(Calendar.HOUR_OF_DAY) <= 19 ? "下午" : "晚上")) + " ").append( 
			calendar.get(Calendar.HOUR)).append(":").append(minstr).toString();
		
		return s;
	}
	
	public static boolean doesExisted(String filepath) {
	    if (TextUtils.isEmpty(filepath)) {
	    	return false;
	    }
	    
	    return doesExisted(new File(filepath));
	}
	
	public static boolean doesExisted(File file) {
	    return ((file != null) && (file.exists()));
	}
	
	public static boolean deleteDependon(File file, int maxRetryCount) {
	    int retryCount = 1;
	    maxRetryCount = (maxRetryCount < 1) ? 5 : maxRetryCount;
	    boolean isDeleted = false;

	    if (file != null) {
	    	while ((!(isDeleted)) && (retryCount <= maxRetryCount) && (file.isFile()) && (file.exists())) {
	    		if (!((isDeleted = file.delete()))) {
	    			// LogUtils.i(file.getAbsolutePath() + "删除失败，失败次数为:" + retryCount);
	    			++retryCount;
	    		}
	    	}
	    }

	    return isDeleted;
	}
	
	public static boolean deleteDependon(String filepath, int maxRetryCount) {
	    if (TextUtils.isEmpty(filepath)) return false;
	    return deleteDependon(new File(filepath), maxRetryCount);
	}
	
	public static boolean deleteDependon(String filepath) {
	    return deleteDependon(filepath, 0);
	}
	
	public static void makesureFileExist(File file) {
		if(file==null) {
			return;
		}
		
	    if (!(file.exists())) {
	    	makesureParentExist(file);
	    	createNewFile(file);
	    }
	}

	public static void makesureParentExist(File file_) {
		if(file_==null){
			return;
		}
		
		File parent = file_.getParentFile();
		
		if ((parent != null) && (!(parent.exists()))) {
			mkdirs(parent);
		}
	}
	
	public static void makesureFileExist(String filePath_) {
		if(filePath_==null) {
			return;
		}
		
		makesureFileExist(new File(filePath_));
	}
	
	public static void mkdirs(File dir_) {
		if(dir_==null){
			return;
		}
		
		if ((!(dir_.exists())) && (!(dir_.mkdirs())) ) {
			throw new RuntimeException("fail to make " + dir_.getAbsolutePath());
		}
	}
	
	public static void createNewFile(File file_) {
		if(file_==null){
			return;
		}
		
		if (!(__createNewFile(file_))) {
			throw new RuntimeException(file_.getAbsolutePath() + " doesn't be created!");
		}
	}
	
	public static void delete(File f) {
		if ((f != null) && (f.exists()) && (!(f.delete())) ) {
			throw new RuntimeException(f.getAbsolutePath() + " doesn't be deleted!");
		}
		
	}
	
	public static boolean __createNewFile(File file_) {
		if(file_==null) {
			return false;
		}
		
		makesureParentExist(file_);
		if (file_.exists())
			delete(file_);
		try
		{
			return file_.createNewFile();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		
		return false;
	}
	
	public static byte[] bitmap2Bytes(Bitmap bm){
		if(bm == null) {
			return null;
		}
		
		ByteArrayOutputStream baos = new ByteArrayOutputStream();    
		
		try {
			bm.compress(Bitmap.CompressFormat.PNG, 100, baos);    
			return baos.toByteArray();
		}
		finally {
			try {
				baos.close();
			} 
			catch (IOException e) {
				// ignore
			}
		}
	}  
	
	public static void playAudio(byte[] content) {
		MediaPlayer player = new MediaPlayer();
        File tempMp3 = null;
        File temppath = new File(TempFolderStore.getTempFolder());
        FileInputStream fis = null;
		try {
			tempMp3 = File.createTempFile(WEJOY_AUDIO_PREFIX, ".amr", temppath);
			tempMp3.deleteOnExit();
			FileOutputStream fos = new FileOutputStream(tempMp3);
			fos.write(content);
			fos.close();
			
			fis = new FileInputStream(tempMp3);
			player.setDataSource(fis.getFD());
			player.prepare();
			player.start();
		} 
		catch (IOException e) {
			DebugUtil.error("CommonUtil", "playAudio", e);
		}
		finally {
			if(tempMp3 != null) {
				tempMp3.delete();
			}
			if (fis != null) {
				try {
					fis.close();
				} catch (IOException e) { 
					e.printStackTrace();
				}
			}
		}
	}
	
	public static void playAudio(MediaPlayer player, byte[] content) {
		if(player == null || content == null) {
			return;
		}
		
        File tempMp3 = null;
        File temppath = new File(TempFolderStore.getTempFolder());
    		
		try {
			tempMp3 = File.createTempFile(WEJOY_AUDIO_PREFIX, ".amr", temppath);
			tempMp3.deleteOnExit();
			FileOutputStream fos = new FileOutputStream(tempMp3);
			fos.write(content);
			fos.close();
			
			FileInputStream fis = new FileInputStream(tempMp3);
			player.setDataSource(fis.getFD());
			player.prepare();
			player.start();
		} 
		catch (IOException e) {
			DebugUtil.error("CommonUtil", "playAudio", e);
		}
		finally {
			if(tempMp3 != null) {
				tempMp3.delete();
			}
		}
	}
	
	public static void writeImageFile(Bitmap bitmap, String path, String filename) throws Exception {
		if(!SDCardUtil.isSDCardWritable()) {
			throw new Exception();
		}
		
		String filepath = filename == null ?
			TempFolderStore.getTempFolder() + File.separator + System.currentTimeMillis():
			Environment.getExternalStorageDirectory().getPath() + File.separator + 
				path + File.separator + filename;
		FileOutputStream fout = null;
		
        try {
        	fout = new FileOutputStream(filepath);
        	bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fout);// 把数据写入文件
        } 
        finally {
        	fout.flush();
        	fout.close();
        }            
	}
	
	public static String getCurrUserId() {
		return AuthModule.INSTANCE.getUid();
	}
	
	public static String getGroupFaceUrl(String groupid) {
		DataStore ds = DataStore.getInstance();
		ContactModule group = ds.queryGroupByContactId(groupid);
		List<ContactModule> members = ds.queryMembersByGroupId(groupid);
		StringBuilder sb = new StringBuilder();
		int limit = Math.min(4, members.size());
		
		for(int i = 0; i < limit; i++) {
			ContactModule c = ds.queryContactByContactId(members.get(i).contactId);
			sb.append(c.faceurl);
			
			if(i < limit - 1) {
				sb.append(",");
			}
		}
		
		if(limit < 4) {
			sb.append(",").append(group.faceurl);
		}
		
		return sb.toString();
	}
	
	public static boolean isWeiboLoginSucc(Activity parent) {
		Oauth2AccessToken token = AccessTokenKeeper.readAccessToken(parent);
		
		if(token.isSessionValid()) {
			return true;
		}
		
		return false;
	}
	
	/**
	 * 读取文件的帮助
	 * @param filepath
	 * @return
	 */
	public static byte[] readFile(String filepath){
		FileInputStream fis = null;
		byte[] fileData = null;
		try {
			fis = new FileInputStream(new File(filepath));
			fileData = new byte[fis.available()];
			fis.read(fileData);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if(null != fis){
				try {
					fis.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return fileData;
	}
}
