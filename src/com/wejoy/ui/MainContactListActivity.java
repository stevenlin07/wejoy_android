package com.wejoy.ui;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.regex.Pattern;

import net.sourceforge.pinyin4j.format.exception.BadHanyuPinyinOutputFormatCombination;

import com.wejoy.R;
import com.wejoy.module.ContactModule;
import com.wejoy.module.ConvertModule;
import com.wejoy.service.apphandler.AddGroupMemberHandler;
import com.wejoy.service.apphandler.CreateGroupChatHandler;
import com.wejoy.store.DataStore;
import com.wejoy.store.ImageStore;
import com.wejoy.ui.helper.UIHelper;
import com.wejoy.util.CommonUtil;
import com.wejoy.util.DebugUtil;
import com.wejoy.util.HanziToPingtin;
import com.wejoy.util.ImageUtils;
import com.wejoy.util.JsonBuilder;
import com.wejoy.util.JsonWrapper;
import com.wejoy.util.MD5;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
/**
 * @author WeJoy Group
 * 联系人分章节显示、ListView快速滑动显示联系人首字母、附带字母表快速查找的例子
 * 查阅网络资源，实现方式都是比较复杂，尤其有些还实现了SectionIndex接口，很多人不怎么能理解，研究后发现此种类型的例子没必要也不应该那么实现
 * 
 */
public class MainContactListActivity extends Activity {

	private ListAdapter adapter;
	private ListView contactLV;
	private Handler handler;
	private static final int MSG_KEY = 0x1234;
	private ImageStore imageStore = ImageStore.getInstance();
	private LinkedBlockingQueue<GroupFaceLoadTask> imageLoadTasks = new LinkedBlockingQueue<GroupFaceLoadTask>();
	private GroupFaceT groupFaceT = new GroupFaceT();
	private List<ContactModule> contactList = null;
	public ContactShowMode showMode = new ContactShowMode();
	
	private List<ContactModule> newmembers;
	private ProgressDialog progressDialog;
	private ContactPageReceiver contactPageReceiver;
	
	private View selectFooter;
	private TextView finishBtn;
	private TextView group_list_back_btn;
	private EditText searchInput;
	
	public static String uid_local = "123124123124";
	public static String url_local = "http://tp2.sinaimg.cn/1757173257/50/5603727511/1";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main_contact_view);
		init();
	}
	
	public void init() {
		selectFooter = (View) findViewById(R.id.contactlist_select_footer);
		finishBtn = (TextView) selectFooter.findViewById(R.id.contact_select_finish_btn);
		finishBtn.setBackgroundResource(R.drawable.black_btn_selector);
		group_list_back_btn = (TextView) findViewById(R.id.group_list_back_btn);
		searchInput = (EditText) findViewById(R.id.txt_input);
		
		contactLV = (ListView) findViewById(R.id.main_contact_listView);
		newmembers = new ArrayList<ContactModule>(); 
		
		group_list_back_btn.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				UIHelper.hideWaiting(MainContactListActivity.this);
				finish();
			}
		});
		
		Intent intent = getIntent();
		Bundle bl= intent.getExtras();
		String modejson = bl.getString("showMode");
		
		if(modejson != null) {
			showMode = ContactShowMode.parseJson(modejson);
		}
		
		adapter = new ListAdapter(this);
		contactLV.setAdapter(adapter);
		
		if(showMode.isSelection) {
			initSelectionContactList();
		}
		else {
			initNormalContactList();
		}
		
		searchInput.addTextChangedListener(new TextWatcher() {
			public void afterTextChanged(Editable editer) {
			}

			public void beforeTextChanged(CharSequence value, int arg0,
					int arg1, int arg2) {
			}

			public void onTextChanged(CharSequence value, int arg0, int arg1, int arg2) {
				Message msg = new Message();
				msg.what = MSG_KEY;
				Bundle data = new Bundle();
				data.putString("value", value.toString());
				msg.setData(data);
				handler.sendMessage(msg);
			}
		});

		handler = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				switch (msg.what) {
				case MSG_KEY:
					refreshListView(msg.getData().get("value").toString());
				case 0x0001:
					adapter.notifyDataSetChanged();
				}
			}
		};
	}
	
	//接收广播
	public class ContactPageReceiver extends BroadcastReceiver{
		@Override
		public void onReceive(Context context, Intent intent) {
			// TODO Auto-generated method stub
			int what = intent.getIntExtra("Msg_What", 0);
			
			if (what ==3) {
				Message msg = new Message();
				msg.what = 0x0001;
				handler.sendMessage(msg);
			}
			else {
				return;
			}
		}
	} 
	
	public Handler serverRequestFinishHandler = new Handler();
	public ServerRequestFinishListener serverRequestFinishListener = new ServerRequestFinishListener();
	
	public class ServerRequestFinishListener implements Runnable {
		public void run() {
			UIHelper.hideWaiting(MainContactListActivity.this);
			finish();
		}
	};

	private void initNormalContactList() {
		contactLV.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> adapterView, View v, int position, long positionId) {
				List<ContactModule> contactList = DataStore.getInstance().queryContacts();
				contactList = decorateForShowing(contactList);
				ContactModule contact = contactList.get(position);
				
				if(contact.contacttype == ContactModule.CONTACT_TYPE_GROUP) {
					Intent intent = new Intent();
					Bundle bl = new Bundle();
					bl.putString("groupid", contact.contactId);
					bl.putString("isEditable", "false");
					intent.putExtras(bl);
					intent.setAction("ShowGroupMembers");
					startActivity(intent);
				}
			}
		});
		
		contactList = DataStore.getInstance().queryContacts();
		contactList = decorateForShowing(contactList);
		adapter.populateData(contactList);
		adapter.notifyDataSetChanged();
		selectFooter.setVisibility(View.GONE);
	}
	
	private void initSelectionContactList() {
		selectFooter.setVisibility(View.VISIBLE);
		
		if (showMode.contactType == ContactShowMode.ALL) {
			contactList = DataStore.getInstance().queryContacts();
			contactList = decorateForShowing(contactList);
		} 
		else {
			// don't add the members already in the group
			List<ContactModule> contactList0 = DataStore.getInstance().queryContactsByType(ContactModule.CONTACT_TYPE_SINGLE);
			contactList0 = decorateForShowing(contactList0);
			
			List<ContactModule> originalContactList = showMode.theGroupIdToBeEditable != null ?
				DataStore.getInstance().queryMembersByGroupId(showMode.theGroupIdToBeEditable) :
				new ArrayList<ContactModule>();
			
			Map<String, ContactModule> originalMembers = new HashMap<String, ContactModule>();
					
			for(ContactModule c : originalContactList) {
				originalMembers.put(c.contactId, c);
			}
					
			contactList = new ArrayList<ContactModule>();
					
			for(ContactModule c : contactList0) {
				if(!originalMembers.containsKey(c.contactId)) {
					contactList.add(c);
				}
			}
		}
		
		adapter.populateData(contactList);
		adapter.notifyDataSetChanged();
		
		finishBtn.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				ArrayList<String> newMemberIds = new ArrayList<String>();
				
				for(ContactModule c : contactList) {
					if(c.isSelected) {
						newMemberIds.add(c.contactId);
						// @by jichao, clear to initialized state
						c.isSelected = false;
					}					
				}
				
				if(newMemberIds.isEmpty()) {
					Toast.makeText(MainContactListActivity.this, "请选择群组或者用户", Toast.LENGTH_LONG).show();
					return;
				}
				
				if(showMode != null && showMode.contactType == ContactShowMode.ALL) {
					UIHelper.showWaiting(MainContactListActivity.this, "正在创建...", false);
					CreateGroupChatHandler createGroupHandler = new CreateGroupChatHandler();
					createGroupHandler.parent = MainContactListActivity.this;
					
					// create group/one to one chat on server side
					createGroupHandler.process(newMemberIds);
				}
				else if(showMode != null && showMode.contactType == ContactShowMode.ADD_GROUP_MEMBERS) {
					UIHelper.showWaiting(MainContactListActivity.this, "正在添加...", false);
					AddGroupMemberHandler addGroupMemberHandler = new AddGroupMemberHandler();
					addGroupMemberHandler.parent = MainContactListActivity.this;
					addGroupMemberHandler.process(showMode.theGroupIdToBeEditable, newMemberIds);
				}
			}
		});
		
		contactLV.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> adapterView, View v, int position, long positionId) {
				ViewHolder holder = (ViewHolder) v.getTag();
				ContactModule c = holder.contact;
				
				if(c.isSelected) {
					c.isSelected = false;
					holder.cbBtn.setBackgroundResource(R.drawable.btn_check_off_normal);
				}
				else {
					c.isSelected = true;
					holder.cbBtn.setBackgroundResource(R.drawable.btn_check_on_normal);
				}
			}
		});
	}
	
	private List<ContactModule> decorateForShowing(List<ContactModule> contacts) {
		// contacts don't include owner user
		Iterator<ContactModule> iterator = contacts.iterator();
		String id = CommonUtil.getCurrUserId();
		
		while(iterator.hasNext()) {
			if(iterator.next().contactId.equals(id)) {
				iterator.remove();
			}
		}
		
		return contacts;
	}
	
	public void refreshListView(String value) {
		if (value == null || value.trim().length() == 0) {
			List<ContactModule> contactList = DataStore.getInstance().queryContacts();
			contactList = decorateForShowing(contactList);
			adapter.populateData(contactList);
			adapter.notifyDataSetChanged();
			return;
		}
		
		ArrayList<ContactModule> tmpList = new ArrayList<ContactModule>();
		List<ContactModule> contactList = DataStore.getInstance().queryContacts();
		contactList = decorateForShowing(contactList);
		
		for (ContactModule contact : contactList) {
			try {
				if (contact.name.indexOf(value) >= 0 || HanziToPingtin.getEname(contact.name).toLowerCase().indexOf(value) >=0 || 
					HanziToPingtin.getEname(contact.name).toUpperCase().indexOf(value) >=0) 
				{
					tmpList.add(contact);
				}
			} catch (BadHanyuPinyinOutputFormatCombination e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		adapter.populateData(tmpList);
		adapter.notifyDataSetChanged();
	}
	
	private static class ViewHolder {
		ImageView faceImage;
		TextView alpha;
		TextView name;
		Button cbBtn;
		int position;
		ContactModule contact;
	}
	
	private class ListAdapter extends BaseAdapter {
		private LayoutInflater inflater;
		private List<ContactModule> list;
		private HashMap<String, Integer> alphaIndexer;//保存每个索引在list中的位置【#-0，A-4，B-10】
		private String[] sections;//每个分组的索引表【A,B,C,F...】

		public ListAdapter(Context context)  {
			this.inflater = LayoutInflater.from(context);
			this.list = list; // 该list是已经排序过的集合，有些项目中的数据必须要自己进行排序。
		}
		
		public void populateData(List<ContactModule> list) {
			if(list == null || list.isEmpty()) {
				return;
			}
			
			this.list = list;
			this.alphaIndexer = new HashMap<String, Integer>();
			this.sections = new String[list.size()];

			for (int i =0; i < list.size(); i++) {
				ContactModule contact = list.get(i);
				String name = contact.name;
				
				try {
					name = getAlpha(HanziToPingtin.getEname(contact.name).toLowerCase().substring(0,1));
				}
				catch(Exception e) {
					DebugUtil.error("", "", e);
				}
				
				if(!alphaIndexer.containsKey(name)){//只记录在list中首次出现的位置
					alphaIndexer.put(name, i);
				}
			}
			
			Set<String> sectionLetters = alphaIndexer.keySet();
			ArrayList<String> sectionList = new ArrayList<String>(sectionLetters);
			Collections.sort(sectionList);
			sections = new String[sectionList.size()];
			sectionList.toArray(sections);
		}

		@Override
		public int getCount() {
			return list == null ? 0 : list.size();
		}

		@Override
		public Object getItem(int position) {
			return list == null ? null : list.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ViewHolder holder;
			ContactModule contact = list.get(position);
			Bitmap bm = null;

			if (convertView == null) {
				convertView = inflater.inflate(R.layout.show_groups_list_item, null);
				holder = new ViewHolder();
				holder.alpha = (TextView) convertView.findViewById(R.id.alpha);
				holder.faceImage = (ImageView) convertView.findViewById(R.id.contactFace);
				holder.name = (TextView) convertView.findViewById(R.id.name);
				holder.cbBtn = (Button) convertView.findViewById(R.id.contact_cb);
				
				holder.cbBtn.setTag(holder);
				holder.cbBtn.setOnClickListener(new OnClickListener() {
					public void onClick(View view) {
						ViewHolder holder = (ViewHolder) view.getTag();
						ContactModule c = holder.contact;
						
						if(c.isSelected) {
							c.isSelected = false;
							holder.cbBtn.setBackgroundResource(R.drawable.btn_check_off_normal);
						}
						else {
							c.isSelected = true;
							holder.cbBtn.setBackgroundResource(R.drawable.btn_check_on_normal);
						}
					}
				});
				
				convertView.setTag(holder);
			}
			else {
				holder = (ViewHolder) convertView.getTag();
			}
			
			holder.contact = contact;
			
			try {
				if(contact.contacttype == ContactModule.CONTACT_TYPE_SINGLE) {
					if((bm = imageStore.getImage(contact.faceurl)) != null) {
						bm = ImageUtils.toRoundCorner(bm, 10);
						holder.faceImage.setImageBitmap(bm);
					}
					else {
						imageStore.reLoadImage(holder.faceImage, contact.faceurl, imageLoadListener, null);
					}
				}
				else if(contact.contacttype == ContactModule.CONTACT_TYPE_GROUP) {
					// try to reload if face if not loaded yet
					if(contact.faceurl == null || "".equals(contact.faceurl)) {
						ContactModule contact0 = DataStore.getInstance().queryContactByContactId(contact.contactId);
						
//						if(contact0.faceurl == null) TO DO
						contact.faceurl = contact0.faceurl;
					}
					
					if((bm = getGroupFace(contact.faceurl, convertView)) != null) {
						bm = ImageUtils.toRoundCorner(bm, 10);
						holder.faceImage.setImageBitmap(bm);
					}
					else {
						holder.faceImage.setImageResource(R.drawable.app_panel_friendcard_icon);
					}
				}
			} 
			catch (Exception e) {
				DebugUtil.error("", "", e);
			}
			
			holder.name.setText(contact.name);
			holder.position = position;
			
			if(showMode.isSelection) {
				if(contact.isSelected) {
					holder.cbBtn.setBackgroundResource(R.drawable.btn_check_on_normal);
				}
				else {
					holder.cbBtn.setBackgroundResource(R.drawable.btn_check_off_normal);
				}
				
				if(contact.contacttype == ContactModule.CONTACT_TYPE_GROUP) {
					holder.cbBtn.setVisibility(View.GONE);
				}
				else {
					holder.cbBtn.setVisibility(View.VISIBLE);
				}
				
			}
			else {
				holder.cbBtn.setVisibility(View.GONE);
			}
			
			// 当前联系人的sortKey
			String currentStr = null;
			try {
				if(contact.contacttype == ContactModule.CONTACT_TYPE_GROUP) {
					currentStr = "Group";
				}
				else {
					currentStr = getAlpha(HanziToPingtin.getEname(contact.name).toLowerCase().substring(0,1));
				}
			} 
			catch (BadHanyuPinyinOutputFormatCombination e) {
				DebugUtil.error(e.toString());
			}
			
			// 上一个联系人的sortKey
			String previewStr = null;
			try {
				if(contact.contacttype == ContactModule.CONTACT_TYPE_GROUP) {
					if ((position - 1) >=0) {
						previewStr = "Group";
					} 
					else {
						previewStr = " ";
					}
				}
				else {
					previewStr = (position - 1) >= 0 ? 
							getAlpha(HanziToPingtin.getEname(list.get(position-1).name).toLowerCase().substring(0,1)) : 
									" ";
				}

			} catch (BadHanyuPinyinOutputFormatCombination e) {
				DebugUtil.error(e.toString());
			}
			
			/**
			 * 判断显示#、A-Z的TextView隐藏与可见
			 */
			if (contact.contacttype == ContactModule.CONTACT_TYPE_GROUP) {
				if (!previewStr.equals(currentStr)) {
					holder.alpha.setVisibility(View.VISIBLE);
					holder.alpha.setText("Group");
				}
				else {
					holder.alpha.setVisibility(View.GONE);
				}
				return convertView;
			}
			
			if (!previewStr.equals(currentStr)) { // 当前联系人的sortKey！=上一个联系人的sortKey，说明当前联系人是新组。
				holder.alpha.setVisibility(View.VISIBLE);
				holder.alpha.setText(currentStr);
			} 
			else {
				holder.alpha.setVisibility(View.GONE);
			}
			
			return convertView;
		}
	}

	private Bitmap getGroupFace(String groupFaceUrl, View convertView) throws Exception 
	{
		if(groupFaceUrl == null) {
			return null;
		}
		
		Bitmap bm = imageStore.getImage(groupFaceUrl);
		
		if(bm != null) {
			return bm;
		}
		
		GroupFaceLoadTask task = new GroupFaceLoadTask();
		task.groupFaceUrl = groupFaceUrl;
		task.listener = imageLoadListener;
		task.itemView = convertView;
		imageLoadTasks.add(task);
		
		return null;
	}
	
	private class GroupFaceLoadTask {
		public String groupFaceUrl;
		public ImageStore.OnImageLoadListener listener;
		public View itemView;
	}
	
	private class GroupFaceT extends Thread {
		public boolean isRunning = false;
		
		public void run() {
			while(true) {
				isRunning = true;
				
				try {
					GroupFaceLoadTask task = imageLoadTasks.take();
					String[] urls = task.groupFaceUrl.split(",");
					List<Bitmap> bms = new ArrayList<Bitmap>(); 
					
					for(String url : urls) {
						Bitmap bm0 = imageStore.getImageByUrl(url, imageLoadListener);
						bm0 = ImageUtils.toRoundCorner(bm0, 20);
						bms.add(bm0);
					}
					
					ImageView iv = (ImageView) task.itemView.findViewById(R.id.contactFace);
					int h = iv.getHeight();
					int w = iv.getWidth();
					Bitmap groupface = ImageUtils.createCombinedBitmap(bms, w, h);
					
					String key = MD5.getMD5(task.groupFaceUrl);
					imageStore.setImage(key, groupface);
					task.listener.onImageLoad(iv, groupface);
				}
				catch(Exception e) {
					DebugUtil.error("", "", e);
				}
			}
		}
	}
	
	/**
	 * 提取英文的首字母，非英文字母用#代替。
	 * 
	 * @param str
	 * @return
	 */
	private String getAlpha(String str) {
		if (str == null) {
			return "#";
		}

		if (str.trim().length() == 0) {
			return "#";
		}

		char c = str.trim().substring(0, 1).charAt(0);
		// 正则表达式，判断首字母是否是英文字母
		Pattern pattern = Pattern.compile("^[A-Za-z]+$");
		if (pattern.matcher(c + "").matches()) {
			return (c + "").toUpperCase(); // 大写输出
		} else {
			return "#";
		}
	}
	
	/**
	 * 异步获取image listener
	 */
	ImageStore.OnImageLoadListener imageLoadListener = new ImageStore.OnImageLoadListener(){
		@Override
		public void onImageLoad(ImageView iv, Bitmap bm) {
			bm = ImageUtils.toRoundCorner(bm, 10);
			iv.setImageBitmap(bm);
		}
		
		@Override
		public void onError(ImageView iv, String msg) {
//			DebugUtil.toast(ShowGroupListActivity.this, msg);
			// TO DO
		}
	};

	public static class ContactShowMode {
		public static final int ALL = 0x00;
		public static final int ADD_GROUP_MEMBERS = 0x10;
		
		public boolean isSelection = false;
		public int contactType = ContactModule.CONTACT_TYPE_MIXED;
		public String theGroupIdToBeEditable;
		
		public String toJson() {
			JsonBuilder json = new JsonBuilder();
			json.append("isSelection", String.valueOf(isSelection));
			json.append("contactType", contactType);
			json.append("theGroupIdToBeEditable", theGroupIdToBeEditable);
			
			return json.flip().toString();
		}
		
		public static ContactShowMode parseJson(String json) {
			ContactShowMode mode  = new ContactShowMode();

			try {
				JsonWrapper jw = new JsonWrapper(json);
				String selstr = jw.get("isSelection");
				mode.isSelection = Boolean.parseBoolean(selstr);
				mode.contactType = jw.getInt("contactType");
				mode.theGroupIdToBeEditable = jw.get("theGroupIdToBeEditable");
			} 
			catch (IOException e) {
				DebugUtil.error("ContactListActivity", "ContactShowMode.parseJson", e);
			}
			
			return mode;
		}
	}
}