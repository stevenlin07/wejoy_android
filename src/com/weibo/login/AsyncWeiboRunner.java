package com.weibo.login;

import com.wejoy.net.HTTPParameters;
import com.wejoy.net.RequestListener;
import com.wejoy.net.WeJoyHTTPManager;

/**
 * 
 * @author luopeng (luopeng@staff.sina.com.cn)
 */
public class AsyncWeiboRunner {
    /**
     * 请求接口数据，并在获取到数据后通过RequestListener将responsetext回传给调用者
     * @param url 服务器地址
     * @param params 存放参数的容器
     * @param httpMethod "GET"or “POST”
     * @param listener 回调对象
     */
	public static void request(final String url, final HTTPParameters params,
			final String httpMethod, final RequestListener listener) {
		new Thread() 
	{
			@Override
			public void run() {
				try {
					String resp = WeJoyHTTPManager.openUrl(url, httpMethod, params,
							params.getValue("pic"));
					listener.onComplete(resp);
				} catch (WeiboException e) {
					listener.onError(e);
				}
			}
		}.start();

	}

}
