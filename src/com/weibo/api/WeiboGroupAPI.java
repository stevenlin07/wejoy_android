package com.weibo.api;

import com.weibo.login.Oauth2AccessToken;
import com.wejoy.net.HTTPParameters;
import com.wejoy.net.RequestListener;

public class WeiboGroupAPI extends WeiboAPI {
	public static final String GROUP_API_SERVER = API_SERVER + "/friendships/groups";
	
	public WeiboGroupAPI(Oauth2AccessToken accessToken) {
        super(accessToken);
    }
	
	public void getMyGroup(RequestListener listener) {
		HTTPParameters params = new HTTPParameters();
		request( GROUP_API_SERVER + ".json", params, HTTPMETHOD_GET, listener);
	}
}
