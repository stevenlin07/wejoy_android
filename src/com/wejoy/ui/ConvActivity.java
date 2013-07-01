package com.wejoy.ui; 

import com.weibo.login.AccessTokenKeeper;
import com.weibo.sdk.syncbox.BoxInstance;
import com.wejoy.R;
import com.wejoy.module.ConvertModule;
import com.wejoy.sdk.net.APNUtil;
import com.wejoy.store.DataStore;
import com.wejoy.store.ImageStore; 
import com.wejoy.store.SqliteStore;
import com.wejoy.ui.helper.UIHelper;
import com.wejoy.util.BroadcastId; 
import com.wejoy.util.DebugUtil;
import com.wejoy.util.ImageUtils; 
 
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap; 
import android.os.Bundle;
import android.os.Handler;
import android.view.KeyEvent;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.ListView; 

public class ConvActivity extends Activity {

	private ActivityReceiver activityReceiver; // 接收系统的广播
	private MainConvListView convListView;  // 会话的List
	public Handler reloadMainViewHandler = new Handler(); 
	public ReloadMainViewRunnable reloadMainViewRunnable = new ReloadMainViewRunnable();
	
	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.main);
		// 初始化DB
		String uid = AccessTokenKeeper.getUid(this);
		SqliteStore.INSTANCE.initDatabase(uid);
		
		AppUtils.context = this; // 赋值到全局共用的context
		AppUtils.screenHeight = this.getWindowManager().getDefaultDisplay().getHeight();
		convListView = (MainConvListView) findViewById(R.id.main_conv_listview);
		
		DataStore.getInstance().asyncLoadContacts(); // 只是为了提前获取联系人信息
		initConvListView();
		startBroadcast(); // 启动广播监听
	}

	@Override
	protected void onResume() { // Activity创建或者从被覆盖、后台重新回到前台时被调用
		super.onResume();
		String uid = AccessTokenKeeper.getUid(this);
		if (uid != null) {
			String token = AccessTokenKeeper.readAccessToken(this).getToken();
			BoxInstance.getInstance().loginOnResume(token, uid);
			APNUtil.isConnected(this);
		}
	}
	
	
	
	public class ReloadMainViewRunnable implements Runnable {
		
		public int op; // 操作
		public int subop;
		public String convid;

		public static final int CONV_LIST_CHANGE = 0x00;
		public static final int START_CHAT = 0x11;
		public static final int CONV_LIST_OP = 0x12;
		public static final int QUIT_ACCOUNT = 0xff;

		public void run() {
			if (op == CONV_LIST_CHANGE) {
				convListView.notifyDataChanged();
				UIHelper.hideWaiting(ConvActivity.this);
			} else if (op == START_CHAT) {
				ConvertModule conv = convListView.data.get(0);

				Intent intent = new Intent();
				Bundle bundle = new Bundle();
				bundle.putString("convjson", conv.toJson());
				intent.putExtras(bundle);
				intent.setAction("Chatting");
				intent.addCategory("CategoryChatting");
				startActivityForResult(intent, RequestCode.INIT);
				UIHelper.hideWaiting(ConvActivity.this);
			} else if (op == CONV_LIST_OP) {
				convListView.notifyDataChanged();
				UIHelper.hideWaiting(ConvActivity.this);
			} else if (op == QUIT_ACCOUNT) {
				Intent intent0 = new Intent();
				intent0.setAction("WeiboLogin");
				intent0.addCategory("WeiboLoginCategory");

				startActivityForResult(intent0, RequestCode.LOGIN);
			}
		}
	};

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
			case RequestCode.INIT :
				// @by jichao, 看似不可思议，实际上存在这种情况，即没有初始化完就会调用onActivityResult
				if (convListView != null) {
					convListView.notifyDataChanged();
				}
				break;
			case RequestCode.NEW_CONV :
				if (convListView != null) {
					convListView.notifyDataChanged();

					if (convListView.data != null && !convListView.data.isEmpty()) {
						ConvertModule conv = convListView.data.get(0);

						Intent intent = new Intent();
						Bundle bundle = new Bundle();
						bundle.putString("convjson", conv.toJson());
						intent.putExtras(bundle);
						intent.setAction("Chatting");
						intent.addCategory("CategoryChatting");
						startActivityForResult(intent, RequestCode.INIT);
					}
				}

				break;
			default :
				break;
		}
	}

	private void initConvListView() {
		ImageView convNewBtn = (ImageView) findViewById(R.id.convert_new_btn);
		ImageView titleMenuBtn = (ImageView) findViewById(R.id.title_menu);
		MainConvListView.MainConvViewHolder vh = new MainConvListView.MainConvViewHolder();

		vh.parent = this;
		vh.convNewBtn = convNewBtn;
		vh.titleMenuBtn = titleMenuBtn;
		
		convListView.viewHolder = vh;
		convListView.init();
	}

	@SuppressWarnings("deprecation")
	public void setListViewHeightBaseOnChildren(ListView listView) {
		ViewGroup.LayoutParams layoutParams = listView.getLayoutParams();
		layoutParams.height = getWindowManager().getDefaultDisplay().getHeight();
		listView.setLayoutParams(layoutParams);
	}

	/** 异步获取image listener */
	ImageStore.OnImageLoadListener imageLoadListener = new ImageStore.OnImageLoadListener() {
	
		@Override
		public void onImageLoad(ImageView iv, Bitmap bm) {
			bm = ImageUtils.toRoundCorner(bm, 10);
			iv.setImageBitmap(bm);
		}

		@Override
		public void onError(ImageView iv, String msg) {
			DebugUtil.toast(ConvActivity.this, msg);
		}
	};

	/** 接收收到新的会话消息的变更通知 */
	public class ActivityReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			String act = intent.getAction();

			if (BroadcastId.CONV_UPDATE.equals(act)) {
				reloadMainViewRunnable.op = ReloadMainViewRunnable.CONV_LIST_CHANGE;
			} else if (BroadcastId.QUIT_ACCOUTN.equals(act)) {
				reloadMainViewRunnable.op = ReloadMainViewRunnable.QUIT_ACCOUNT;
			}
			reloadMainViewHandler.post(reloadMainViewRunnable);
		}
	}

	/** 初始化广播 */
	public void startBroadcast() {
		IntentFilter filter = new IntentFilter();
		activityReceiver = new ActivityReceiver();
		filter.addAction(BroadcastId.CONV_UPDATE); // 会话更新
		filter.addAction(BroadcastId.QUIT_ACCOUTN); // 退出账户
		this.registerReceiver(activityReceiver, filter); // 注册广播
	}

	@Override
	protected void onDestroy() {
		if (activityReceiver != null) {
			this.unregisterReceiver(activityReceiver);
		}
		super.onDestroy();
	}
	
	@Override
    public boolean onKeyDown(int keyCode, KeyEvent event) { // 按返回键不退出应用
        if(keyCode == KeyEvent.KEYCODE_BACK){
            moveTaskToBack(true);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
}
