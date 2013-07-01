package com.wejoy.store;

import java.io.File;

import android.os.Environment;

public class TempFolderStore implements StoreConstant {
	private static final String TEMP_FOLDER = "temp";
	
	public static String getTempFolder() {
		return Environment.getExternalStorageDirectory() + File.separator + STORE_ROOT + File.separator + TEMP_FOLDER;
	}
}
