package com.wejoy.ui.settings;
 
import com.weibo.login.AccessTokenKeeper;
import com.weibo.sdk.syncbox.BoxInstance;
import com.wejoy.R; 
import com.wejoy.ui.AppUtils;  
import com.wejoy.ui.view.CornerListView;
import com.wejoy.util.BroadcastId; 

import android.app.Activity; 
import android.content.Context; 
import android.content.Intent; 
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView; 
import android.widget.BaseAdapter;
import android.widget.Button; 
import android.widget.ImageView; 
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

/**
 * WeJoy Group
 */
public class SysMenuActivity extends Activity
{
	private Context context;
	private LayoutInflater listContainer;
	private CornerListView cornerListView = null;
	private Button quit_account_btn = null;
	private static final int USR_SETTING_COUNT = 2;
	
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.sys_menu);
		this.context = this;
		this.listContainer = LayoutInflater.from(context);
		TextView gobackBtn = (TextView) findViewById(R.id.usr_config_back_btn);
		gobackBtn.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				finish();
			}
		});
		
		cornerListView = (CornerListView) findViewById(R.id.system_menu_list);
		cornerListView.setAdapter(new SysMenuAdapter());
		
		cornerListView.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View view,
			        int position, long id) 
			{
				ItemHolder ih = getMenuItem(position);
				
				if(ih != null && ih.itemVal != null) {
					Intent intent = new Intent();
					intent.setAction(ih.itemVal);
					startActivity(intent);
				}
			}
		});
		
		quit_account_btn = (Button) findViewById(R.id.quit_account_btn);
		quit_account_btn.setOnClickListener(new OnClickListener() {
			public void onClick(View view) {
				AccessTokenKeeper.clear(AppUtils.context);
				BoxInstance.getInstance().logout();
				startBroadcast();
				finish();
			}
		});
	}
	
	/** 初始化广播*/
	public void startBroadcast() {
		Intent intent = new Intent();
		// use the same id, bad smell code, TO DO
		intent.setAction(BroadcastId.QUIT_ACCOUTN);
		sendBroadcast(intent);
	}
	
    private class SysMenuAdapter extends BaseAdapter {
    	@Override
    	public View getView(int position, View cornerView, ViewGroup parent) {
    		ViewHolder holder = null;
    		
    		if(cornerView == null) {
    			cornerView = listContainer.inflate(R.layout.corner_list_cell, null);
    			
    			//获取控件对象
    			holder = new ViewHolder();
    			holder.item = (TextView) cornerView.findViewById(R.id.setting_list_item_text);
    			holder.itemVal = (TextView) cornerView.findViewById(R.id.setting_list_text_value);
    			holder.personFace = (ImageView) cornerView.findViewById(R.id.corner_big_pic);
    			
    			cornerView.setTag(holder);
    		}
    		else {
    			holder = (ViewHolder) cornerView.getTag();
    		}
    		
   			ItemHolder ih = (ItemHolder) getItem(position);
   			holder.item.setText(ih.item);
   			holder.personFace.setVisibility(View.GONE);
    		
    		return cornerView;
    	}
    	
		@Override
		public int getCount() {
			return USR_SETTING_COUNT;
		}
		
		public Object getItem(int pos) {
			return getMenuItem(pos);
		}
		
		@Override
		public long getItemId(int id) {
			return id;
		}
    }
    
    public ItemHolder getMenuItem(int pos) {
		ItemHolder ih = new ItemHolder();
		
		switch(pos) {
			case 0 :
				ih.item = "个人信息";
				ih.itemVal = "userinfo";
				break;
			case 1 :
				ih.item = "新消息提醒";
				ih.itemVal = "notifyconfig";
				break;
//			case 2 :
//				ih.item = "我的账号";
//				ih.itemVal = "";
//				break;
//			case 3 :
//				ih.item = "退出登录";
//				ih.itemVal = "";
//				break;
			default :
				break;
		}
		
		return ih;
	}
    
    private class ViewHolder {
    	public ImageView personFace;
    	public TextView item;
    	public TextView itemVal;
    }
    
    private class ItemHolder {
    	public String item;
    	public String itemVal;
    }
}