package com.wejoy.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;

import com.wejoy.R;
import com.wejoy.module.ChatMessage;
import com.wejoy.module.ContactModule;
import com.wejoy.module.ConvertModule;
import com.wejoy.module.Sex;
import com.wejoy.module.UserInfo;
import com.wejoy.service.apphandler.SyncRemoveGroupMemberHandler;
import com.wejoy.store.DataStore;
import com.wejoy.store.ImageStore;
import com.wejoy.store.SqliteStore;
import com.wejoy.ui.MainContactListActivity.ContactShowMode;
import com.wejoy.ui.view.CornerListView;
import com.wejoy.util.CommonUtil;
import com.wejoy.util.DebugUtil;
import com.wejoy.util.ImageUtils;
import com.wejoy.util.MD5;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

/**
 * WeJoy Group
 */
public class ShowGroupMembersActivity extends Activity
{
	private List<ContactModule> members = null;
	private ImageStore imageStore = ImageStore.getInstance();
	private Context context;
	private String groupid = null;
	private boolean isEditable = false;
	private GroupMemberAdapter gmAdapter = null;
	private List<Map<String,String>> listData = null;
	private static final int GROUP_SETTING_COUNT = 3;
	
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.group_members);
		this.context = this;
		
		Intent intent = getIntent();
		Bundle bl= intent.getExtras();
		groupid = bl.getString("groupid");
		isEditable = "true".equalsIgnoreCase(bl.getString("isEditable"));
		initGroupMember();
		
		gmAdapter = new GroupMemberAdapter(this);
		GridView grid = (GridView) findViewById(R.id.group_member_grid);
		grid.setAdapter(gmAdapter);
		
		grid.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> adapterView, View v, int position, long positionId) {
				ViewHolder vh = (ViewHolder) v.getTag();
				
				if(vh.contact.contacttype == ContactModule.CONTACT_TYPE_ADD_FAKE_MEMBER) {
					// add member to this group
					MainContactListActivity.ContactShowMode showMode = new MainContactListActivity.ContactShowMode();
					showMode.isSelection = true;
					showMode.contactType = ContactShowMode.ADD_GROUP_MEMBERS;
					showMode.theGroupIdToBeEditable = groupid;
							
					Intent intent = new Intent();
	    			Bundle bundle = new Bundle();
	    			bundle.putString("showMode", showMode.toJson());
	    			intent.putExtras(bundle);
	    			intent.setAction("ShowContactList");
					
					startActivityForResult(intent, ContactShowMode.ADD_GROUP_MEMBERS);
				}
			}
		});
		
		final String[] mainChatPopwinMenu = new String[] {"删除", "取消"};
		
		grid.setOnItemLongClickListener(new OnItemLongClickListener() {
			public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
				final ViewHolder vh = (ViewHolder) view.getTag();
				
				if(vh.contact.contacttype == ContactModule.CONTACT_TYPE_ADD_FAKE_MEMBER) {
					return true;
				}
				
    			new AlertDialog.Builder(ShowGroupMembersActivity.this)
							.setTitle("确定删除此成员么？")
							.setItems(mainChatPopwinMenu, new DialogInterface.OnClickListener() 
				{
					public void onClick(DialogInterface dialog, int which) {
					
						if (mainChatPopwinMenu[which].equals("删除")) {
							String contactId = vh != null && vh.contact != null ? String.valueOf(vh.contact.contactId) : null;
							if(contactId != null) {
								// send to server firstly
								SyncRemoveGroupMemberHandler syncRemove = new SyncRemoveGroupMemberHandler();
								List<String> members = new ArrayList<String>();
								members.add(contactId);
								String result = syncRemove.process(groupid, members);
								
								if(result != null) {
									DebugUtil.toast(ShowGroupMembersActivity.this, result);
								}
								else {
									// @by jichao, 异步处理增加速度
									DataStore.getInstance().removeGroupMember(groupid, contactId);
									initGroupMember();
									gmAdapter.notifyDataSetChanged(); // 实现数据的实时刷新
									
									ConvertModule conv = DataStore.getInstance().getConvertById(groupid);
									conv.convListFacePic = CommonUtil.getGroupFaceUrl(groupid);
									DataStore.getInstance().updateConvert(conv);
								}
							}
						}
					}
				})
				.show();
    			return true;
			}
		});
		
		TextView backtohome = (TextView) findViewById(R.id.contact_list_home_btn);
		
		backtohome.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				finish();
			}
		});
		
		Button start_group_chat_btn = (Button) findViewById(R.id.start_group_chat_btn);
		
		start_group_chat_btn.setOnClickListener(new OnClickListener() {
			public void onClick(View view) {
				finish();
			}
		});
		
		ContactModule curusr = DataStore.getInstance().queryContactByContactId(groupid);
		UserInfo settings = null;
		
		if(curusr != null && curusr.extend != null) {
			settings = UserInfo.parseJson(curusr.extend);
		}
		else if(curusr != null) {
			settings = new UserInfo(curusr);
		}
		else {
			settings = new UserInfo();
		}
		
		GroupSettingsAdapter adapter = new GroupSettingsAdapter(this, settings);
		CornerListView cornerListView = (CornerListView) findViewById(R.id.group_setting_list);
		cornerListView.setAdapter(adapter);
	}
	
	private void initGroupMember() {
		decorateForShowing(DataStore.getInstance().queryMembersByGroupId(groupid));
		ContactModule lastmbr = members.get(members.size() - 1);
		
		// add btn fake member
		if(isEditable) {
			if(lastmbr.contacttype != ContactModule.CONTACT_TYPE_ADD_FAKE_MEMBER) {
				ContactModule addbtn = ContactModule.getAddBtn();
				members.add(addbtn);
			}
		}
		// remove btn fake member
		else {
			if(lastmbr.contacttype == ContactModule.CONTACT_TYPE_ADD_FAKE_MEMBER) {
				members.remove(members.size() - 1);
			}
		}
	}
	
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {  
	    //如果requestCode==0说明这个activity是从上一个acitvity跳转回来的。  
	    if(requestCode == ContactShowMode.ADD_GROUP_MEMBERS){
	    	initGroupMember();
	    	gmAdapter.notifyDataSetChanged();
	    }  
	}

	private void decorateForShowing(List<ContactModule> list) {
		if(list == null || list.isEmpty()) {
			return;
		}
		
		if(members == null) {
			members = new ArrayList<ContactModule>();
		}
		else {
			members.clear();
		}
		
		String curusrId = CommonUtil.getCurrUserId();
		
		for(ContactModule c : list) {
			// @by jichao, don't show the current user in group member management 
			// 否则如果用户选择删除自己，跟退出群逻辑冲突
			if(!c.contactId.equals(curusrId)) {
				members.add(c);
			}
		}
	}
	
	public class GroupMemberAdapter extends BaseAdapter {
	    private LayoutInflater inflater;
	 
	    public GroupMemberAdapter(Context context) {
	        this.inflater = LayoutInflater.from(context);
	    }
	 
	    @Override
	    public int getCount() {
	        return members.size();
	    }
	 
	    @Override
	    public Object getItem(int position) {
	        return position;
	    }
	 
	    @Override
	    public long getItemId(int position) {
	        return position;
	    }
	 
	    @Override
	    public View getView(int position, View convertView, ViewGroup parent) {
	        ViewHolder holder;
	        
	        if (convertView == null) {
	            holder = new ViewHolder();
	            convertView = this.inflater.inflate(R.layout.show_group_members_cell, null);
	            holder.iv = (ImageView) convertView.findViewById(R.id.group_member_face);
	            holder.tv = (TextView) convertView.findViewById(R.id.item_text);
	            convertView.setTag(holder);
	        }
	        else {
	           holder = (ViewHolder) convertView.getTag();
	        }
	        
	        ContactModule member = members.get(position);
	        holder.contact = member;
	        
	        if(member.contacttype == ContactModule.CONTACT_TYPE_ADD_FAKE_MEMBER) {
	        	holder.iv.setImageResource(R.drawable.groupchat_add_selector);
	        	holder.tv.setText("");
	        }
	        else {
	        	Bitmap bm = null;
	        	
	        	if((bm = imageStore.getImage(member.faceurl)) != null) {
	        		holder.iv.setImageBitmap(decorate(bm));
	        	}
	        	else {
	        		imageStore.reLoadImage(holder.iv, member.faceurl, listener, null);
	        	}
	        	
	        	holder.tv.setText(member.name != null && member.name.length() > 10 ? member.name.substring(0, 8) + "..." : member.name);
	        }
	        
	        return convertView;
	    }
	}
	
	private class ViewHolder {
        ImageView iv;
        TextView tv;
        ContactModule contact;
    }
	
	/**
	 * @by jichao, 增加圆角和阴影，以增强立体效果
	 * @param bm
	 * @return
	 */
	private Bitmap decorate(Bitmap bm) {
		Bitmap bm0 = ImageUtils.toRoundCorner(bm, 7);
        Bitmap bm1 = ImageUtils.drawShadow(bm0);
        return bm1;
	}
	
	private ImageStore.OnImageLoadListener listener = new ImageStore.OnImageLoadListener() {
		public void onImageLoad(ImageView iv, Bitmap bm) {
			iv.setImageBitmap(decorate(bm));
		}
		
		public void onError(ImageView iv, String msg) {
			DebugUtil.toast(context, msg);
			// TO DO
		}
	};
	
	private class SqliteDBOp {
		public static final int DEL = 0x10;
		
		public int op;
		public ContactModule cm;
		
		public int getOp() {
			return op;
		}
		
		public SqliteDBOp setOp(int op) {
			this.op = op;
			return this;
		}
		
		public ContactModule getCM() {
			return cm;
		}

		public SqliteDBOp setCM(ContactModule cm) {
			this.cm = cm;
			return this;
		}
	}
	
	private class GroupSettingsAdapter extends BaseAdapter {
    	private Context context;
    	private UserInfo setttings;
    	private LayoutInflater listContainer;

    	public GroupSettingsAdapter(Context context, UserInfo settings) {
    		this.context = context;
    		this.setttings = settings;
    		this.listContainer = LayoutInflater.from(context);	//创建视图容器并设置上下文
    	}
    	
    	@Override
    	public View getView(int position, View cornerView, ViewGroup parent) {
    		GroupSettingsViewHolder holder = null;
    		
    		if(cornerView == null) {
    			cornerView = listContainer.inflate(R.layout.corner_list_cell, null);
    			
    			//获取控件对象
    			holder = new GroupSettingsViewHolder();
    			holder.item = (TextView) cornerView.findViewById(R.id.setting_list_item_text);
    			holder.itemVal = (TextView) cornerView.findViewById(R.id.setting_list_text_value);
    			holder.personFace = (ImageView) cornerView.findViewById(R.id.corner_big_pic);
    			
    			cornerView.setTag(holder);
    		}
    		else {
    			holder = (GroupSettingsViewHolder) cornerView.getTag();
    		}
    		
    		ItemHolder ih = (ItemHolder) getItem(position);
    		holder.item.setText(ih.item);
    		holder.itemVal.setText(ih.itemVal);
    		holder.itemVal.setVisibility(View.VISIBLE);
    		
    		return cornerView;
    	}
    	
		@Override
		public int getCount() {
			return GROUP_SETTING_COUNT;
		}

		public Object getItem(int pos) {
			ItemHolder ih = new ItemHolder();
			
			switch(pos) {
				case 0 :
					ih.item = "群聊名字";
					ih.itemVal = setttings.name;
					break;
				case 1 :
					ih.item = "二维码";
					ih.itemVal = "";
					break;
				case 2 :
					ih.item = "群聊设置";
					ih.itemVal = "";
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
    
    private class GroupSettingsViewHolder {
    	public ImageView personFace;
    	public TextView item;
    	public TextView itemVal;
    }
    
    private class ItemHolder {
    	public String item;
    	public String itemVal;
    }
}