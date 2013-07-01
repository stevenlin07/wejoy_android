package com.wejoy.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle; 
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

import com.weibo.login.AccessTokenKeeper;
import com.weibo.login.Oauth2AccessToken;
import com.weibo.login.SsoHandler;
import com.weibo.login.Weibo;
import com.weibo.login.WeiboAuthListener;
import com.weibo.login.WeiboDialogError;
import com.weibo.login.WeiboException;
import com.wejoy.R;
import com.wejoy.ui.view.CornerListView;

public class LoginActivity extends Activity {
 
	public Handler loginFinishHandler = new Handler();
	/** 列表数据 */
	private List<Map<String, String>> listData;
	/** weibo */
	private Weibo mWeibo;
	/** 替换开发者的appkey */
	private static final String CONSUMER_KEY = "1124443769";
	private static final String REDIRECT_URL = "https://api.weibo.com/oauth2/default.html";
	public static Oauth2AccessToken accessToken;
	public static final String TAG = "sinasdk";
	/** SsoHandler 仅当sdk支持sso时有效 */
	private SsoHandler mSsoHandler;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.weibologin_activity);

		setListData();
		LoginAdapter adapter = new LoginAdapter(getApplicationContext(), listData, R.layout.corner_list_cell);
		CornerListView cornerListView = (CornerListView) findViewById(R.id.login_list_view);
		cornerListView.setAdapter(adapter);
		mWeibo = Weibo.getInstance(CONSUMER_KEY, REDIRECT_URL);

		cornerListView.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				ListView lv = (ListView) parent;
				@SuppressWarnings({"unchecked", "unused"})
				HashMap<String, Object> person = (HashMap<String, Object>) lv.getItemAtPosition(position);
				mSsoHandler = new SsoHandler(LoginActivity.this, mWeibo);
				mSsoHandler.authorize(new AuthDialogListener());
			}
		});
	}

	/** 设置列表框的数据，微博登陆的按钮 */
	private void setListData() {
		listData = new ArrayList<Map<String, String>>();
		Map<String, String> map = new HashMap<String, String>();
		map.put("text", "微博登陆");
		map.put("logo", String.valueOf(R.drawable.weibosdk_title_logo));
		listData.add(map);
	}

	

	/** 微博认证的结果 */
	class AuthDialogListener implements WeiboAuthListener {

		@Override
		public void onComplete(Bundle values) { // 微博认证成功
			String token = values.getString("access_token");
			String expires_in = values.getString("expires_in");
			LoginActivity.accessToken = new Oauth2AccessToken(token, expires_in);
			AccessTokenKeeper.keepAccessToken(LoginActivity.this, accessToken);
			Toast.makeText(LoginActivity.this, "认证成功", Toast.LENGTH_SHORT).show();
			finish();
		}

		@Override
		public void onError(WeiboDialogError e) {
			Toast.makeText(getApplicationContext(),"Auth error : " + e.getMessage(), Toast.LENGTH_LONG).show();
		}

		@Override
		public void onCancel() {
			Toast.makeText(getApplicationContext(), "Auth cancel",Toast.LENGTH_LONG).show();
		}

		@Override
		public void onWeiboException(WeiboException e) {
			Toast.makeText(getApplicationContext(),"Auth exception : " + e.getMessage(), Toast.LENGTH_LONG).show();
		}
	}

	public class LoginAdapter extends BaseAdapter { 
		private int itemViewResource;
		private List<Map<String, String>> listData = null;
		private LayoutInflater listContainer;// 视图容器

		/** 实例化Adapter */
		public LoginAdapter(Context context, List<Map<String, String>> data, int itemViewResource) {
			this.listData = data;
			this.itemViewResource = itemViewResource;
			this.listContainer = LayoutInflater.from(context);
		}

		@Override
		public View getView(int position, View view, ViewGroup parent) {
			ViewHolder vh = null; 
			Map<String, String> data = (Map<String, String>) listData.get(position);

			View opview = view == null ? listContainer.inflate(this.itemViewResource, null) : view;

			if (opview.getTag() != null) {
				vh = (ViewHolder) opview.getTag();
			} else {
				vh = new ViewHolder();
				opview.setTag(vh);

				// 获取控件对象
				ImageView logo = (ImageView) opview.findViewById(R.id.corner_op_logo);
				logo.setVisibility(View.VISIBLE);
				TextView label = (TextView) opview.findViewById(R.id.setting_list_item_text);
				vh.label = label;
				vh.logo = logo;
			}

			vh.label.setText(data.get("text"));
			vh.logo.setBackgroundResource(Integer.parseInt(data.get("logo")));

			return opview;
		}

		@Override
		public int getCount() {
			return listData.size();
		}

		@Override
		public Object getItem(int pos) {
			return listData.get(pos);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}
	}

	 @Override
	    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
	        super.onActivityResult(requestCode, resultCode, data);

	        /**
	         * 下面两个注释掉的代码，仅当sdk支持sso时有效，
	         */
	        if (mSsoHandler != null) {
	            mSsoHandler.authorizeCallBack(requestCode, resultCode, data);
	        }
	    }
	 
	private class ViewHolder {
		ImageView logo;
		TextView label;
	}
}
