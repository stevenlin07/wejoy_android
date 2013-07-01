package com.wejoy.ui;

import com.weibo.login.AccessTokenKeeper;
import com.weibo.sdk.syncbox.BoxInstance; 
import com.wejoy.R; 
import com.wejoy.service.WeJoyService;
import com.wejoy.service.apphandler.SyncWeJoyLoginHandler;
import com.wejoy.service.apphandler.WeiboFriendsHandler;
import com.wejoy.service.apphandler.WeiboUserInfoHandler;
import com.wejoy.store.DataStore;
import com.wejoy.store.SqliteStore;
import com.wejoy.ui.helper.UIHelper;
import com.wejoy.util.CommonUtil;
import com.wejoy.util.DebugUtil;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.view.Window;

public class LoadActivity extends Activity {

	public static Context context = null;
	public Handler reloadMainViewHandler = new Handler();
	public BoxInstance wejoy = BoxInstance.getInstance();
	private Intent intent = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE); // 去掉标题栏
		setContentView(R.layout.load);
		context = this;

		// 初始化服务
		intent = new Intent(LoadActivity.this, WeJoyService.class);
		startService(intent);

		new CountDownTimer(1000, 500) {
			@Override
			public void onTick(long millisUntilFinished) {}

			@Override
			public void onFinish() { // 倒计时结束后在这里实现activity跳转
				String uid = AccessTokenKeeper.getUid(context);
				if (CommonUtil.isWeiboLoginSucc(LoadActivity.this) && uid != null) {
					startConvActivity();
				} else {
					startWeiboLogin();
				}
			}
		}.start();
	}

	/** 跳转到会话的Activity */
	private void startConvActivity() {
		 Intent intent = new Intent();    
         intent.setClass(LoadActivity.this, ConvActivity.class);   
         LoadActivity.this.startActivity(intent);
         this.finish();
	}
	
	/** WEIBO登录 */
	private void startWeiboLogin() {
		Intent intent0 = new Intent();
		intent0.setAction("LoginActivity");
		intent0.addCategory("LoginCategory");
		startActivityForResult(intent0, RequestCode.LOGIN);
	}

	/** WESYNC登录 */
	private void startWesyncLogin(Context context) {
		SyncWeJoyLoginHandler loginHandler = new SyncWeJoyLoginHandler(context);
		if (loginHandler.process()) { // 如果登录成功
			String uid = AccessTokenKeeper.getUid(context);
			DataStore.getInstance().initDatabase(uid);
			if (DataStore.getInstance().getOwnerUser() == null) { // 获取用户信息
				WeiboUserInfoHandler usrInfoHandler = new WeiboUserInfoHandler();
				usrInfoHandler.process(uid);
			}
			// 拉取向关注好友信息
			WeiboFriendsHandler handler = new WeiboFriendsHandler(context);
			handler.process();
			reloadMainViewHandler.post(finishLogin);// 结束登录
		} else {
			reloadMainViewHandler.post(finishLogin);
			reloadMainViewHandler.post(loginFailedMessage);
			startWeiboLogin();
		}
	}

	/** 终止登录 */
	Runnable finishLogin = new Runnable() {
		public void run() {
			UIHelper.hideWaiting(context);
			startConvActivity();
		}
	};

	/** 登录失败 */
	Runnable loginFailedMessage = new Runnable() {
		public void run() {
			DebugUtil.toast(context, "登录失败，请重试...");
		}
	};

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
			case RequestCode.LOGIN :
				if (CommonUtil.isWeiboLoginSucc(LoadActivity.this)) {
					UIHelper.hideWaiting(context);
					UIHelper.showWaiting(context, "正在登录...", false);
					new Thread(new Runnable() {
						public void run() {
							startWesyncLogin(context);
						}
					}).start();
				} else {
					startWeiboLogin();
				}
				break;
			default :
				break;
		}
	}
}
