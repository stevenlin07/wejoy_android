package com.wejoy.util;

import com.weibo.sdk.syncbox.utils.Constansts;
import com.wejoy.sdk.net.APNUtil;
import com.wejoy.ui.helper.WeJoyChatInterface;
/**
 * 
 * @author WeJoy Group
 *
 */
public class ElasticNetUtil {
	/**会根据网络情况判断超时时间*/
	public static int getTimeOut() {
		int time = WeJoyChatInterface.CHAT_TIMEOUT;
		if (!APNUtil.isWifi()) {
			time = WeJoyChatInterface.CHAT_TIMEOUT * 3;
		}
		return time;
	}
	
	/**会根据网络情况判断文件分片大小*/
	public static int getFileSlice() {
		if (APNUtil.isWifi()) {
			// @by jichao, 如果是wifi环境，分片大小为4MB
			return Constansts.SLICE_SIZE * 80;
		}
		
		return Constansts.SLICE_SIZE;
	}
}
