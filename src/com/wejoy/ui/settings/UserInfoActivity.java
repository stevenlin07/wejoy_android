package com.wejoy.ui.settings;
 
import java.util.List;
import java.util.Map; 

import com.weibo.login.AccessTokenKeeper;
import com.wejoy.R; 
import com.wejoy.module.ContactModule; 
import com.wejoy.module.Sex;
import com.wejoy.module.UserInfo;
import com.wejoy.service.apphandler.WeiboUserInfoHandler;
import com.wejoy.store.DataStore;
import com.wejoy.store.ImageStore; 
import com.wejoy.ui.view.CornerListView; 
import com.wejoy.util.ImageUtils; 

import android.app.Activity; 
import android.content.Context; 
import android.graphics.Bitmap; 
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window; 
import android.view.ViewGroup;
import android.widget.AdapterView; 
import android.widget.BaseAdapter; 
import android.widget.ImageView; 
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

/**
 * WeJoy Group
 */
public class UserInfoActivity extends Activity
{
	private List<ContactModule> members = null;
	private ImageStore imageStore = ImageStore.getInstance();
	private Context context;
	private String groupid = null;
	private boolean isEditable = false;
	private List<Map<String,String>> listData = null;
	private CornerListView cornerListView = null;
	private UserInfoAdapter adapter = null;
	private static final int USR_SETTING_COUNT = 4;
	
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.user_config);
		this.context = this;
		
		TextView gobackBtn = (TextView) findViewById(R.id.usr_config_back_btn);
		gobackBtn.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				finish();
			}
		});
		
		ContactModule curusr = DataStore.getInstance().getOwnerUser();
		UserInfo settings = null;
		
		if(curusr != null && curusr.extend != null) {
			settings = UserInfo.parseJson(curusr.extend);
		}
		else if(curusr != null) {
			settings = new UserInfo(curusr);
		}
		else {
			// reload owner user info in case it's failed before
			if(DataStore.getInstance().getOwnerUser() == null) {
				WeiboUserInfoHandler usrInfoHandler = new WeiboUserInfoHandler();
				String uid = AccessTokenKeeper.getUid(this);
				usrInfoHandler.process(uid);
			}
			
			settings = new UserInfo();
		}
		
		cornerListView = (CornerListView) findViewById(R.id.usr_setting_list);
		adapter = new UserInfoAdapter(this, settings);
		cornerListView.setAdapter(adapter);
		
		cornerListView.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View view,
			        int position, long id) 
			{
				ViewHolder vh = (ViewHolder) view.getTag();
				System.out.println(parent +"|"+ view +"|"+ position +"|"+ id);
			      // When clicked, show a toast with the TextView text
//			      DebugUtil.toast(getApplicationContext(), ((TextView) view).getText());
			}
		});
	}
	
    private class UserInfoAdapter extends BaseAdapter {
    	private Context context;
    	private UserInfo setttings;
    	private LayoutInflater listContainer;

    	public UserInfoAdapter(Context context, UserInfo settings) {
    		this.context = context;
    		this.setttings = settings;
    		this.listContainer = LayoutInflater.from(context);	//创建视图容器并设置上下文
    	}
    	
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
    		
    		// position 0 always for personal face image
    		if(position == 0) {
    			holder.item.setText("头像");
    			holder.itemVal.setText("");
    			Bitmap bitmap = ImageStore.getInstance().getImage(setttings.faceurl);
    			
    			if(bitmap != null) {
    				bitmap = ImageUtils.toRoundCorner(bitmap, 5);
    				bitmap = ImageUtils.drawShadow(bitmap);
    				holder.personFace.setImageBitmap(bitmap);
    			}
    			
    			holder.personFace.setVisibility(View.VISIBLE);
    		}
    		else {
    			ItemHolder ih = (ItemHolder) getItem(position);
    			holder.item.setText(ih.item);
    			holder.itemVal.setText(ih.itemVal);
    			holder.itemVal.setVisibility(View.VISIBLE);
    			holder.personFace.setVisibility(View.GONE);
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
				case 1 :
					ih.item = "名字";
					ih.itemVal = setttings.name;
					break;
				case 2 :
					ih.item = "性别";
					ih.itemVal = setttings.sex == Sex.Mail ? "男" : "女";
					break;
				case 3 :
					ih.item = "个性签名";
					ih.itemVal = setttings.desc;
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
    	public ImageView personFace;
    	public TextView item;
    	public TextView itemVal;
    }
    
    private class ItemHolder {
    	public String item;
    	public String itemVal;
    }
}