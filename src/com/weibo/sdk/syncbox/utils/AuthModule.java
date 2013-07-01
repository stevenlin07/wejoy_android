package com.weibo.sdk.syncbox.utils;
 
import com.weibo.sdk.syncbox.type.pub.SdkEvent;


/**
 * <h1>认证服务模块.</h1>
 * <ol bgcolor="#C4C400">
 * <li>用户信息缓存(不存用户名和密码，只存加密的token)</li>
 * <li>接口调用的认证管理</li>
 * <li>用户的认证管理</li>
 * </ol>
 *
 * @author liuzhao
 */
public enum AuthModule {

	/** 实例. */
	INSTANCE;

	/** 用户的UID. */
	private String uid; 
	/** 当前所用的初始化认证头:头标识+空格+串 */
	private String authHead;
	/** 用来重连的认证头*/
	private String mtokenHeader;
 

	/** 清理掉AuthModule模块的所有资源. */
	public void clear() {
		LogModule.INSTANCE.info("[AuthModule][STOP]", SdkEvent.INVOKE);
		this.uid = null;
		this.authHead = null; 
		this.mtokenHeader = null;
	}

	/** 生成BASIC认证头内容:Basic+" "+ 已经加密basicToken.*/
	public String setBasicToken(final String username, final String password) {
		String encryptPassword = RsaTool.encryptPassword(password);
		if (null == encryptPassword) { return null; }
		String userPass = username + ":" + encryptPassword; 
		String basicToken =  Base64.encodeBase64String(userPass.getBytes());
		this.authHead = setAuthHeader(Constansts.AUTH_HEAD_BASIC, basicToken);
		return this.authHead;
	}
	
	/** 生成OAUTH认证头内容:OAuth+空格+加密的oatuhToken.*/
	public String setOatuhToken(final String oatuhToken) {
		String encryptToken = RsaTool.encryptPassword(oatuhToken);
		if (null == encryptToken) { return null; }
		String encryptOatuhToken = Base64.encodeBase64String(encryptToken.getBytes());
		this.authHead = setAuthHeader(Constansts.AUTH_HEAD_OAUTH, encryptOatuhToken);
		return this.authHead;
	}
	
	/** 生成MAUTH认证头内容:MAuth+空格+mtoken*/
	public void setMauthToken(final String mtoken) {
		if (mtoken == null){
			this.mtokenHeader = null;
		} else {
			this.mtokenHeader = setAuthHeader(Constansts.AUTH_HEAD_MAUTH, mtoken);
		}
	}
	
	private String setAuthHeader(final String authTag , final String authToken) {
		return authTag + " " + authToken;
	}
	
	/** 获取MauthHeader*/
	public String getMauthHeader() {
		return this.mtokenHeader;
	}
	
	/** 获取当前SDK所处于认证状态所具有的认证头内容. */
	public String getAuthHead() {
		return this.authHead;
	} 

	/** 获取用户的UID. */
	public String getUid() {
		return uid;
	}

	/** 设置用户的UID. */
	public void setUid(final String newUid) {
		this.uid = newUid;
	}
}
