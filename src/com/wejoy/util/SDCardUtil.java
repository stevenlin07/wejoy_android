package com.wejoy.util;

import java.io.File;
import java.io.IOException;

import android.os.Environment;
import android.os.StatFs;

/**
 * 
 * @author WeJoy Group
 *
 */
public class SDCardUtil {
	public static final int MB = 1024 * 1024;
	public static final int MINIMUM_SDCARD_SPACE = 5; // 5MB
	
	public static int freeSpaceOnSd() { 
	    StatFs stat = new StatFs(Environment.getExternalStorageDirectory().getPath()); 
	    double sdFreeMB = ((double)stat.getAvailableBlocks() * (double) stat.getBlockSize()) / MB; 
	    return (int) sdFreeMB; 
	}
	
	public static boolean isSDCardReadable() {
		return Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
	}
	
	public static boolean isSDCardWritable() {
		return Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED) && freeSpaceOnSd() >= MINIMUM_SDCARD_SPACE;
	}
	
	public static File createFolder(String folder) {
		File file = new File(folder);
		
		if (!file.exists()) {
			String[] subfolder = folder.split(File.separator);
			StringBuilder sb = new StringBuilder();
			
			for(String subfolder0 : subfolder) {
				sb.append(subfolder0).append(File.separator);
				file = new File(sb.toString());
				
				if (!file.exists()) {
					file.mkdir();
				}
			}		
		}
		
		return file;
	}
	
	public static File createFile(String path, String file) throws Exception {
		createFolder(path);
		File filef = new File(path + File.separator + file);
		
		if (!filef.exists()) {
			if(filef.createNewFile()) {
				return filef;
			}
			else {
				throw new Exception("create file failed");
			}
		}
		
		return filef;
	}
	
	public static boolean isFileExisted(String filepath) {
		File file = new File(filepath);
		return file.exists();
	}
	
	public static void removeFile(String attachPath) {
		// TO DO
	}
}
