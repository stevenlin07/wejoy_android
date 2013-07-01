package com.wejoy.sdk.net;

import java.util.Locale;

import com.wejoy.ui.AppUtils; 

import android.content.Context;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.NetworkInfo.State;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;
/**
 * APN工具类
 * <p>
 * 来自腾讯游戏
 * </p>
 */
public class APNUtil {
	private static final String TAG = "APNUtil";
	/**cmwap*/
	public static final int MPROXYTYPE_CMWAP = 1;
	/**wifi*/
	public static final int MPROXYTYPE_WIFI = 2;
	/** cmnet */
	public static final int MPROXYTYPE_CMNET = 4;
	/** uninet服务器列表  */
	public static final int MPROXYTYPE_UNINET = 8;
	/** uniwap服务器列表 */
	public static final int MPROXYTYPE_UNIWAP = 16;
	/** net类服务器列表 */
	public static final int MPROXYTYPE_NET = 32;
	/** wap类服务器列表 */
	public static final int MPROXYTYPE_WAP = 64;
	/** 默认服务器列表 */
	public static final int MPROXYTYPE_DEFAULT = 128;
	/** cmda net */
	public static final int MPROXYTYPE_CTNET = 256;
	/**  cmda wap */
	public static final int MPROXYTYPE_CTWAP = 512;
	/**  联通 3gwap  */
	public static final int MPROXYTYPE_3GWAP = 1024;
	/** 联通 3gnet */
	public static final int MPROXYTYPE_3GNET = 2048;

	public static final String ANP_NAME_WIFI = "wifi"; // 中国移动wap APN名称
	public static final String ANP_NAME_CMWAP = "cmwap"; // 中国移动wap APN名称
	public static final String ANP_NAME_CMNET = "cmnet"; // 中国移动net APN名称
	public static final String ANP_NAME_UNIWAP = "uniwap"; // 中国联通wap APN名称
	public static final String ANP_NAME_UNINET = "uninet"; // 中国联通net APN名称
	public static final String ANP_NAME_WAP = "wap"; // 中国电信wap APN名称
	public static final String ANP_NAME_NET = "net"; // 中国电信net APN名称
	public static final String ANP_NAME_CTWAP = "ctwap"; // wap APN名称
	public static final String ANP_NAME_CTNET = "ctnet"; // net APN名称
	public static final String ANP_NAME_NONE = "none"; // net APN名称

	// apn地址
	private static Uri PREFERRED_APN_URI = Uri
	.parse("content://telephony/carriers/preferapn");

	// apn属性类型
	public static final String APN_PROP_APN = "apn";
	// apn属性代理
	public static final String APN_PROP_PROXY = "proxy";
	// apn属性端口
	public static final String APN_PROP_PORT = "port";

	public static final byte APNTYPE_NONE   = 0 ;//未知类型 
	public static final byte APNTYPE_CMNET  = 1 ;//cmnet  
	public static final byte APNTYPE_CMWAP  = 2 ;//cmwap  
	public static final byte APNTYPE_WIFI   = 3 ;//WiFi  
	public static final byte APNTYPE_UNINET = 4 ;//uninet 
	public static final byte APNTYPE_UNIWAP = 5 ;//uniwap 
	public static final byte APNTYPE_NET    = 6 ;//net类接入点 
	public static final byte APNTYPE_WAP    = 7 ;//wap类接入点 
	public static final byte APNTYPE_CTNET = 8; //ctnet
	public static final byte APNTYPE_CTWAP = 9; //ctwap
	public static final byte APNTYPE_3GWAP = 10; //3gwap
	public static final byte APNTYPE_3GNET = 11; //3gnet
	
	//jce接入点类型
	public static final int JCE_APNTYPE_UNKNOWN = 0;
	public static final int JCE_APNTYPE_DEFAULT = 1;
	public static final int JCE_APNTYPE_CMNET = 2;
	public static final int JCE_APNTYPE_CMWAP = 4;
	public static final int JCE_APNTYPE_WIFI = 8;
	public static final int JCE_APNTYPE_UNINET = 16;
	public static final int JCE_APNTYPE_UNIWAP = 32;
	public static final int JCE_APNTYPE_NET = 64;
	public static final int JCE_APNTYPE_WAP = 128;
	public static final int JCE_APNTYPE_CTNET = 256;
	public static final int JCE_APNTYPE_CTWAP = 512;	
	
	/**
	 *  获取jce协议的接入点类型
	 *  老协议的
	 * @param context
	 * @return
	 */
	public static int getJceApnType(Context context){
		int netType = getMProxyType(context);
		if(netType == MPROXYTYPE_WIFI){
			return JCE_APNTYPE_WIFI;
		}else if (netType == MPROXYTYPE_CMWAP) {
			return JCE_APNTYPE_CMWAP;
		} else if (netType == MPROXYTYPE_CMNET) {
			return JCE_APNTYPE_CMNET;
		} else if (netType == MPROXYTYPE_UNIWAP) {
			return JCE_APNTYPE_UNIWAP;
		} else if (netType == MPROXYTYPE_UNINET) {
			return JCE_APNTYPE_UNINET;
		}else if (netType == MPROXYTYPE_WAP) {
			return JCE_APNTYPE_WAP;
		} else if (netType == MPROXYTYPE_NET) {
			return JCE_APNTYPE_NET;
		} else if (netType == MPROXYTYPE_CTWAP) {
			return JCE_APNTYPE_CTWAP;
		} else if (netType == MPROXYTYPE_CTNET) {
			return JCE_APNTYPE_CTNET;
		}
		return JCE_APNTYPE_DEFAULT;
	}
	

	/**
	 * 将jce定义的接入点类型转化为普通(老协议定义的)接入点类型
	 * @param jceApnType
	 * @return
	 */
	public static byte jceApnTypeToNormalapnType(int jceApnType){
		if(jceApnType == JCE_APNTYPE_UNKNOWN){
			return APNTYPE_NONE;
		}else if (jceApnType == JCE_APNTYPE_DEFAULT) {
			return JCE_APNTYPE_CMWAP;
		} else if (jceApnType == JCE_APNTYPE_CMNET) {
			return APNTYPE_CMNET;
		} else if (jceApnType == JCE_APNTYPE_CMWAP) {
			return APNTYPE_CMWAP;
		} else if (jceApnType == JCE_APNTYPE_WIFI) {
			return APNTYPE_WIFI;
		}else if (jceApnType == JCE_APNTYPE_UNINET) {
			return APNTYPE_UNINET;
		} else if (jceApnType == JCE_APNTYPE_UNIWAP) {
			return APNTYPE_UNIWAP;
		} else if (jceApnType == JCE_APNTYPE_NET) {
			return APNTYPE_NET;
		} else if (jceApnType == JCE_APNTYPE_WAP) {
			return APNTYPE_WAP;
		} else if (jceApnType == JCE_APNTYPE_CTWAP) {
			return APNTYPE_CTNET;
		} else if (jceApnType == JCE_APNTYPE_CTNET) {
			return APNTYPE_CTWAP;
		}
		return APNTYPE_NONE;
	}
	
	
	/**
	 *  将普通(老协议定义的)接入点类型转化为jce定义的接入点类型
	 *  老协议的
	 * @param apnType
	 * @return
	 */
	public static int normalApnTypeToJceApnType(byte apnType){
		if(apnType == APNTYPE_NONE){
			return JCE_APNTYPE_UNKNOWN;
		}else if (apnType == JCE_APNTYPE_CMWAP) {
			return JCE_APNTYPE_DEFAULT;
		} else if (apnType == APNTYPE_CMNET) {
			return JCE_APNTYPE_CMNET;
		} else if (apnType == APNTYPE_CMWAP) {
			return JCE_APNTYPE_CMWAP;
		} else if (apnType == APNTYPE_WIFI) {
			return JCE_APNTYPE_WIFI;
		}else if (apnType == APNTYPE_UNINET) {
			return JCE_APNTYPE_UNINET;
		} else if (apnType == APNTYPE_UNIWAP) {
			return JCE_APNTYPE_UNIWAP;
		} else if (apnType == APNTYPE_NET) {
			return JCE_APNTYPE_NET;
		} else if (apnType == APNTYPE_WAP) {
			return JCE_APNTYPE_WAP;
		} else if (apnType == APNTYPE_CTWAP) {
			return JCE_APNTYPE_CTWAP;
		} else if (apnType == APNTYPE_CTNET) {
			return JCE_APNTYPE_CTNET;
		}
		return JCE_APNTYPE_UNKNOWN;
	}
	
	
	
	/**
	 * 获取自定义APN名称
	 * 
	 * @param context
	 * @return
	 */
	public static String getApnName(Context context) {
		int netType = getMProxyType(context);

		if(netType == MPROXYTYPE_WIFI){
			return ANP_NAME_WIFI;
		}else if (netType == MPROXYTYPE_CMWAP) {
			return ANP_NAME_CMWAP;
		} else if (netType == MPROXYTYPE_CMNET) {
			return ANP_NAME_CMNET;
		} else if (netType == MPROXYTYPE_UNIWAP) {
			return ANP_NAME_UNIWAP;
		} else if (netType == MPROXYTYPE_UNINET) {
			return ANP_NAME_UNINET;
		}else if (netType == MPROXYTYPE_WAP) {
			return ANP_NAME_WAP;
		} else if (netType == MPROXYTYPE_NET) {
			return ANP_NAME_NET;
		} else if (netType == MPROXYTYPE_CTWAP) {
			return ANP_NAME_CTWAP;
		} else if (netType == MPROXYTYPE_CTNET) {
			return ANP_NAME_CTNET;
		}
		//获取系统apn名称
		String apn = getApn(context);
		if(apn == null || apn.length() == 0)
			return apn;
		return ANP_NAME_NONE;
	}
	
	/**
	 * 获取自定义apn类型
	 * @param context
	 * @return
	 */
	public static byte getApnType(Context context){
		int netType = getMProxyType(context);

		if(netType == MPROXYTYPE_WIFI){
			return APNTYPE_WIFI;
		}else if (netType == MPROXYTYPE_CMWAP) {
			return APNTYPE_CMWAP;
		} else if (netType == MPROXYTYPE_CMNET) {
			return APNTYPE_CMNET;
		} else if (netType == MPROXYTYPE_UNIWAP) {
			return APNTYPE_UNIWAP;
		} else if (netType == MPROXYTYPE_UNINET) {
			return APNTYPE_UNINET;
		}else if (netType == MPROXYTYPE_WAP) {
			return APNTYPE_WAP;
		} else if (netType == MPROXYTYPE_NET) {
			return APNTYPE_NET;
		} else if (netType == MPROXYTYPE_CTWAP) {
			return APNTYPE_CTWAP;
		} else if (netType == MPROXYTYPE_CTNET) {
			return APNTYPE_CTNET;
		} else if (netType == MPROXYTYPE_3GWAP) {
			return APNTYPE_3GWAP;
		} else if (netType == MPROXYTYPE_3GNET) {
			return APNTYPE_3GNET;
		}
		return APNTYPE_NONE;
	}

	/**
	 * 获取系统APN
	 * @param context
	 * @return
	 */
	public static String getApn(Context context){
		Cursor c = context.getContentResolver().query(PREFERRED_APN_URI, null,
				null, null, null);
		c.moveToFirst();
		if (c.isAfterLast()) {
			c.close();
			return null;
		}
		
		String strResult = c.getString(c.getColumnIndex(APN_PROP_APN));
		c.close();
		return strResult;
	}
	
	/** 获取系统APN代理IP */
	public static String getApnProxyIp(Context context) {
		byte apnType = APNUtil.getApnType(context);
		if (apnType==APNUtil.APNTYPE_CMWAP||apnType==APNUtil.APNTYPE_UNIWAP||apnType==APNUtil.APNTYPE_3GWAP)
			return "10.0.0.172";
		
		if (apnType==APNUtil.APNTYPE_CTWAP) 
			return "10.0.0.200";
		
		return getApnProxy(context);
	}

	/** 获取系统APN代理IP */
	public static String getApnProxy(Context context) {
		Cursor c = context.getContentResolver().query(PREFERRED_APN_URI, null,
				null, null, null);
		c.moveToFirst();
		if (c.isAfterLast()) {
			c.close();
			return null;
		}
		String strResult = c.getString(c.getColumnIndex(APN_PROP_PROXY));
		c.close();
		return strResult;
	}
	
	/** 获取系统APN代理端口*/
	public static String getApnPort(Context context) {
		Cursor c = context.getContentResolver().query(PREFERRED_APN_URI, null,
				null, null, null);
		c.moveToFirst();
		if (c.isAfterLast()) {
			c.close();
			return "80";
		}
		
		String port = null;
		port = c.getString(c.getColumnIndex(APN_PROP_PORT));
		if(port == null){
			c.close();
			port = "80";
		}
		c.close();
		return port;
	}

	/**
	 * 获取系统APN代理端口
	 * 
	 * @param context
	 * @return
	 */
	public static int getApnPortInt(Context context) {
		Cursor c = context.getContentResolver().query(PREFERRED_APN_URI, null,
				null, null, null);
		c.moveToFirst();
		if (c.isAfterLast()) {
			c.close();
			return -1;
		}
		int result = c.getInt(c.getColumnIndex(APN_PROP_PORT));
		return result;
	}
	
	/**
	 * 是否有网关代理
	 * 
	 * @param context
	 * @return
	 */
	public static boolean hasProxy(Context context) {
		int netType = getMProxyType(context);
		//#if ${polish.debug}
		Log.d(TAG, "netType:" + netType);
		//#endif
		if (netType == MPROXYTYPE_CMWAP || netType == MPROXYTYPE_UNIWAP
				|| netType == MPROXYTYPE_WAP || netType == MPROXYTYPE_CTWAP || netType == MPROXYTYPE_3GWAP) {
			return true;
		}
		return false;
	}

	/**
	 * 获取自定义当前联网类型
	 * 
	 * @param act
	 *            当前活动Activity
	 * @return 联网类型 -1表示未知的联网类型, 正确类型： MPROXYTYPE_WIFI | MPROXYTYPE_CMWAP |
	 *         MPROXYTYPE_CMNET
	 */
	public static int getMProxyType(Context act) {
		try {
			ConnectivityManager cm = (ConnectivityManager) act.getSystemService(Context.CONNECTIVITY_SERVICE);
			if(cm == null)
				return MPROXYTYPE_DEFAULT;
			
			NetworkInfo info = cm.getActiveNetworkInfo();
			if(info == null)
				return MPROXYTYPE_DEFAULT;
			String typeName = info.getTypeName();
			//#if ${polish.debug}
			Log.d(TAG, "typeName:" + typeName);
			//#endif
			if (typeName.toUpperCase(Locale.getDefault()).equals("WIFI")) { // wifi网络
				return MPROXYTYPE_WIFI;
			} else {
				String extraInfo = info.getExtraInfo().toLowerCase(Locale.getDefault());
				//#if ${polish.debug}
				Log.d(TAG, "extraInfo:" + extraInfo);
				//#endif
				if (extraInfo.startsWith("cmwap")) { // cmwap
					return MPROXYTYPE_CMWAP;
				} else if (extraInfo.startsWith("cmnet")
						|| extraInfo.startsWith("epc.tmobile.com")) { // cmnet
					return MPROXYTYPE_CMNET;
				} else if (extraInfo.startsWith("uniwap")) {
					return MPROXYTYPE_UNIWAP;
				} else if (extraInfo.startsWith("uninet")) {
					return MPROXYTYPE_UNINET;
				} else if (extraInfo.startsWith("wap")) {
					return MPROXYTYPE_WAP;
				} else if (extraInfo.startsWith("net")) {
					return MPROXYTYPE_NET;
				} else if(extraInfo.startsWith("ctwap")){
					return MPROXYTYPE_CTWAP;
				}else if(extraInfo.startsWith("ctnet")){
					return MPROXYTYPE_CTNET;
				} else if(extraInfo.startsWith("3gwap")) {
					return MPROXYTYPE_3GWAP;
				} else if(extraInfo.startsWith("3gnet")) {
					return MPROXYTYPE_3GNET;
				}
				else if (extraInfo.startsWith("#777")) { // cdma
					String proxy = getApnProxy(act);
					if (proxy != null && proxy.length() > 0) {
						return MPROXYTYPE_CTWAP;
					} else {
						return MPROXYTYPE_CTNET;
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return MPROXYTYPE_DEFAULT;
	}

	/** 获取网络的主名称*/
	public static String getNetWorkName(Context context){
		ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		if(cm == null)
			return "MOBILE";
		NetworkInfo info = cm.getActiveNetworkInfo();
		if(info != null)
		    return info.getTypeName();
		else
			return "MOBILE";
	}
	
	/** 检测是否有网络  */
	public static boolean isNetworkAvailable(Context act) {
		ConnectivityManager cm = (ConnectivityManager) act.getSystemService(Context.CONNECTIVITY_SERVICE);
		if(cm == null)
			return false;
		NetworkInfo info = cm.getActiveNetworkInfo();
		if(info != null && info.isAvailable())
			return true;
		return false;
	}

	public static State getNetworkState(Context act) {
		ConnectivityManager cm = (ConnectivityManager) act.getSystemService(Context.CONNECTIVITY_SERVICE);
		if(cm == null)
			return null;
		NetworkInfo info = cm.getActiveNetworkInfo();
		if(info != null)
			return info.getState();
		return null;
	}
	
	public static String getNetworkInfo(Context act) {
		ConnectivityManager cm = (ConnectivityManager) act.getSystemService(Context.CONNECTIVITY_SERVICE);
		if(cm == null)
			return null;
		NetworkInfo info = cm.getActiveNetworkInfo();
		if(info != null)
			return info.toString();
		return null;
	}
	
	
	public static boolean isWifi() {
		Context context = AppUtils.context;
		int netType = getMProxyType(context);
		if(netType == MPROXYTYPE_WIFI){
			return true;
		} 
		return false;
	}
	
	/** 判断网络是否联通*/
	public static boolean isConnected(Context context) {
		final ConnectivityManager connMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		final android.net.NetworkInfo wifi = connMgr.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
		final android.net.NetworkInfo mobile = connMgr.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
		if (wifi.isConnected() || mobile.isConnected()) {
			return true;
		} else {
			Toast.makeText(context, "网络不可用", Toast.LENGTH_SHORT).show();
			return false;
		}
	}
}
