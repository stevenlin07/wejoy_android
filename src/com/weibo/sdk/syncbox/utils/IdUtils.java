package com.weibo.sdk.syncbox.utils;


/**
 * contain:Group,FolderId,FileId
 * @author liuzhao
 *
 */
public class IdUtils {

	public static char FILE_SPLIT = '-';
	// FolderId
	public final static char FOLDER_SPLIT = '-';
	public final static String FOLDERID_SPLIT = "-";
	public static String ROOT_TAG = "root";
	public static String PROP_TAG = "prop";
	public static String CONV_TAG = "conv";
	public static String CONV_TAG2 = "con2";
	public static String GROU_TAG = "grou";
	// GroupId
	public final static String ID_PREFIX = "G";
	public final static char ID_SPLIT = '$'; // different with folder split
	// Properties
	public final static String PROP_MEMBERS = ID_PREFIX + "Members";
	public final static String PROP_HISTORY = ID_PREFIX + "History";


	public static String getFileId(String username, String userChatWith, String suffix){
		return username+FILE_SPLIT+userChatWith+FILE_SPLIT+suffix;
	}

	public static String onRoot(String username) {
		return username+FOLDER_SPLIT+ROOT_TAG;
	}

	public static String onProperty(String username, String propName) {
		return createFolderId(username, PROP_TAG, propName);
	}

	public static String onConversation(String username, String userChatWith) {
		return createFolderId(username, CONV_TAG, userChatWith);
	}

	public static String onGroup(String username, String groupId){
		return createFolderId(username, GROU_TAG, groupId);
	}

	//Private utilities
	private static String createFolderId(String prefix, String tag, String suffix){
		return prefix+FOLDER_SPLIT+tag+FOLDER_SPLIT+suffix;
	}

	public static String getGroupId(String folderId){
		String[] split = folderId.split(FOLDERID_SPLIT);
		return split[2];
	}

	public static String memberFolderId(String groupId) {
		return onProperty(groupId, PROP_MEMBERS);
	}

}
