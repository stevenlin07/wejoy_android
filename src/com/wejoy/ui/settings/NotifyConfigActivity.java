package com.wejoy.ui.settings;
 

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.BaseAdapter; 
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

import com.wejoy.R; 
import com.wejoy.module.UserConfig;
import com.wejoy.store.ConfigStore; 
import com.wejoy.ui.view.CornerListView;
/**
 * WeJoy Group
 */
public class NotifyConfigActivity extends Activity {
	private ConfigStore configStore = ConfigStore.getInstance();
	private Context context;
	private CornerListView cornerListView = null;
	private UserConfigAdapter adapter = null;
	private static final int USR_SETTING_COUNT = 3;
	private UserConfig.NotifyConfig notifyConfig = null;
	
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.notify_config);
		this.context = this;
		
		TextView gobackBtn = (TextView) findViewById(R.id.notify_config_back_btn);
		gobackBtn.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				finish();
			}
		});
		
		notifyConfig = configStore.getUserConfig().notifyConfig;
		cornerListView = (CornerListView) findViewById(R.id.notify_config);
		adapter = new UserConfigAdapter();
		cornerListView.setAdapter(adapter);
		
		cornerListView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> adapterView, View v, int position, long positionId) {
				ViewHolder holder = (ViewHolder) v.getTag();
				
				if(getValueByPos(position)) {
					setValueByPos(false, position);
					updateConfig();
					holder.checkboxBtn.setBackgroundResource(R.drawable.btn_check_off_normal);
				}
				else {
					setValueByPos(true, position);
					updateConfig();
					holder.checkboxBtn.setBackgroundResource(R.drawable.btn_check_on_normal);
				}
			}
		});
	}
	
	private void updateConfig() {
		UserConfig config = configStore.getUserConfig();
		config.notifyConfig = notifyConfig;
		configStore.updateUserSettings(config);
	}
	
    private class UserConfigAdapter extends BaseAdapter {
    	private LayoutInflater listContainer;

    	public UserConfigAdapter() {
    		this.listContainer = LayoutInflater.from(context);	//创建视图容器并设置上下文
    	}
    	
    	@Override
    	public View getView(int position, View cornerView, ViewGroup parent) {
    		ViewHolder holder = null;
    		
    		if(cornerView == null) {
    			cornerView = listContainer.inflate(R.layout.notify_config_cell, null);
    			
    			//获取控件对象
    			holder = new ViewHolder();
    			holder.item = (TextView) cornerView.findViewById(R.id.setting_list_item_text);
    			holder.itemVal = (TextView) cornerView.findViewById(R.id.setting_list_op_value);
    			holder.checkboxBtn = (ImageView) cornerView.findViewById(R.id.checkbox_btn);
    			holder.rightArrowBtn = (ImageView) cornerView.findViewById(R.id.right_arrow_btn);
    			cornerView.setTag(holder);
    		}
    		else {
    			holder = (ViewHolder) cornerView.getTag();
    		}

    		// @by jichao, 小于3的设置是用checkbox形式
    		if(position < 3) {
    			ItemHolder ih = (ItemHolder) getItem(position);
    			holder.item.setText(ih.item);
    			
    			if(Boolean.parseBoolean(ih.itemVal)) {
    				holder.checkboxBtn.setBackgroundResource(R.drawable.btn_check_on_normal);
    			}
    			else {
    				holder.checkboxBtn.setBackgroundResource(R.drawable.btn_check_off_normal);
    			}
    			
    			holder.itemVal.setVisibility(View.GONE);
    			holder.rightArrowBtn.setVisibility(View.GONE);
    		}
    		
    		return cornerView;
    	}
    	
		@Override
		public int getCount() {
			return USR_SETTING_COUNT;
		}

		public Object getItem(int pos) {
			ItemHolder ih = new ItemHolder();
			
			switch(pos) {
				case 0 :
					ih.item = "新消息通知";
					ih.itemVal = String.valueOf(notifyConfig.hasNotice());
					break;
				case 1 :
					ih.item = "声音";
					ih.itemVal = String.valueOf(notifyConfig.hasSound());
					break;
				case 2 :
					ih.item = "震动";
					ih.itemVal = String.valueOf(notifyConfig.hasShake());
					break;
				default :
					break;
			}
			
			return ih;
		}

		@Override
		public long getItemId(int id) {
			return id;
		}
    }
    
    private class ViewHolder {
    	public ImageView rightArrowBtn;
    	public ImageView checkboxBtn;
    	public TextView item;
    	public TextView itemVal;
    }
    
    private class ItemHolder {
    	public String item;
    	public String itemVal;
    }
    
    private boolean getValueByPos(int pos) {
    	boolean val = false;
    	
    	switch(pos) {
    	case 0 :
    		val = notifyConfig.hasNotice();
    		break;
    	case 1 :
    		val = notifyConfig.hasSound();
    		break;
    	case 2 :
    		val = notifyConfig.hasShake();
    		break;
    	}
    	
    	return val;
    }
    
    private void setValueByPos(boolean val, int pos) {
    	switch(pos) {
    	case 0 :
    		notifyConfig.setNotice(val);
    		break;
    	case 1 :
    		notifyConfig.setSound(val);
    		break;
    	case 2 :
    		notifyConfig.setShake(val);
    		break;
    	}
    }
}
