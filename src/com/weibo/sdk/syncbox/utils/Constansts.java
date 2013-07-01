package com.weibo.sdk.syncbox.utils;

public final class Constansts {

	public static String AUTH_HEAD_MAUTH = "MAuth";
	public static int TIME_THREAD_POOL = 3;

	public static int SLICE_SIZE = 50 * 1024; // 发送文件分片 B
	public static int META_SIZE = 50 * 1024; // meta的最大长度限制 50K

	public static byte SYNC_VERSION = 20;

	// 服务器信息
	public static String SERV_URL_NIO_IP = "123.126.42.25";
	public static int SERV_URL_NIO_PORT = 7070;

	// 心跳
	public static int START_TIMER = 120;
	public static int SLICE_TIMER = 120;
	public static int heartbeat_timeout = 30;
	public static String heartbeat_withtag = "HEARTBEAT";
	public static int connnect_again = 5;
	
	// 修复请求超时
	public static int FIXSYNC_TIME = 120;
	public static int PROXYSYNC_TIME = 120;
	
	
	public static String AUTH_HEAD_BASIC = "Basic";
	public static String AUTH_HEAD_BASIC_AG = "MBasic";
	public static String AUTH_HEAD_OAUTH = "OAuth";
	public static String AUTH_HEAD_OAUTH_AG = "MOAuth";
}
