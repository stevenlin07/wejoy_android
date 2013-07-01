package com.wejoy.ui;
 
import java.util.List;
import com.weibo.sdk.syncbox.BoxInstance;
import com.wejoy.R;
import com.wejoy.module.ConvertModule;
import com.wejoy.ui.MainContactListActivity.ContactShowMode;
import com.wejoy.ui.adapter.ConvertAdapter;
import com.wejoy.ui.helper.UIHelper;
import com.wejoy.util.DebugUtil; 
import android.util.AttributeSet;

import com.wejoy.service.apphandler.SyncQuitGroupHandler;
import com.wejoy.store.DataStore; 
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.app.Activity; 
import android.app.ProgressDialog; 
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent; 
import android.view.View; 
import android.widget.ImageView;
import android.widget.ListView;
/**
 * 
 * @author wejoy group
 *
 */
public class MainConvListView extends ListView {
	private static final String[] convListViewPopwinMenu = new String[] {"删除该聊天", "聊天置顶"};
    
    public ConvertAdapter convertAdapter;
    public List<ConvertModule> data;
    private DataStore dataStore = DataStore.getInstance();
    private Handler mainHandler;
    private Context context;
    public MainConvViewHolder viewHolder = new MainConvViewHolder();
    final BoxInstance wejoyInstance = BoxInstance.getInstance();
    
    public MainConvListView(Context context, AttributeSet attrs, int defStyle) {
    	super(context, attrs, defStyle);
    	this.context = context;
    }
    
    public MainConvListView(Context context, AttributeSet attrs) {
    	super(context, attrs);					
    	this.context = context;
    }
    
    public MainConvListView(Context context) {
    	this(context, null);
    	this.context = context;
    }
	
	public void init() {
		populateHeader();
		populateConvList();
	}

	private void populateHeader() {
		viewHolder.convNewBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				MainContactListActivity.ContactShowMode showMode = new MainContactListActivity.ContactShowMode();
				showMode.isSelection = true;
				showMode.contactType = ContactShowMode.ALL;
				
				Intent intent = new Intent();
    			Bundle bundle = new Bundle();
    			bundle.putString("showMode", showMode.toJson());
    			intent.putExtras(bundle);
    			intent.setAction("ShowContactList");
    			viewHolder.parent.startActivityForResult(intent, RequestCode.NEW_CONV);
			}
		});
		
		viewHolder.titleMenuBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// @by jichao, scroll in future
				// viewHolder.mainViewGroup.scrollToScreen(WeJoyMainViewGroup.SCREEN_SYS_MENU_VIEW);
				
				Intent intent = new Intent();
    			intent.setAction("sysmenu");
    			viewHolder.parent.startActivity(intent);
			}
		});
	}
	
	public void notifyDataChanged() {
		if(convertAdapter == null || dataStore == null) {
			return;
		}
		
		convertAdapter.convertlist = dataStore.queryConvList();
		convertAdapter.notifyDataSetChanged();
	}
	
	private void populateConvList() {
		data = dataStore.queryConvList();
		convertAdapter = new ConvertAdapter(context, data, this, R.layout.convlist_item);
		
		convertAdapter.onLongClickListenernew = onLongClickListenernew;
		convertAdapter.onItemClickListener = onItemClickListener;
		
		setAdapter(convertAdapter);
	}
    
	OnClickListener onItemClickListener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			final ConvertAdapter.ViewHolder holder = (ConvertAdapter.ViewHolder) v.getTag();
			
			ConvertModule conv = holder.conv;
			Bundle bl = new Bundle();
			bl.putString("convjson", conv.toJson());
			Intent intent = new Intent();
			intent.putExtras(bl);
			intent.setAction("Chatting");
			intent.addCategory("CategoryChatting");
			viewHolder.parent.startActivityForResult(intent, RequestCode.INIT);
		}
	};
	
	private String opresult;
	final Handler handler = new Handler() {  
        public void handleMessage(Message msg) {  
        	DebugUtil.toast(viewHolder.parent, 
        		"remove group on server failed caused by " + opresult);
        }  
    };  
	
    
	View.OnLongClickListener onLongClickListenernew = new View.OnLongClickListener() {
		public boolean onLongClick(View v) {
			final ConvertAdapter.ViewHolder holder = (ConvertAdapter.ViewHolder) v.getTag();
			
			new ProgressDialog.Builder(context)
				.setTitle(holder.conv.convName)
				.setItems(convListViewPopwinMenu, new DialogInterface.OnClickListener() 
			{
				public void onClick(DialogInterface dialog, final int which) {
					UIHelper.showWaiting(viewHolder.parent, "正在处理...", false);
					
					new Thread(new Runnable() {public void run() {
						switch(which) {
						// 删除会话
						case 0:
							String convid = holder.conv.convid;
							ConvertModule conv = DataStore.getInstance().getConvertById(convid);
							
		        			if(conv != null && conv.convtype == ConvertModule.GROUP_CONV) {
		        				SyncQuitGroupHandler syncQuitGroup = new SyncQuitGroupHandler();
		        				String result = syncQuitGroup.process(conv.convid);
		        				
		        				if(result != null) {
		        					DataStore.getInstance().removeGroup(convid);
		        				}
		        				else {
		        					opresult = result;
		        				}
		        			}

		        			DataStore.getInstance().removeConversation(convid);
		        			
							refreshMain(0, holder.conv.convid);
							break;
							// 置顶
						case 1:
							DataStore.getInstance().setConveTop(holder.conv.convid);
							refreshMain(1, holder.conv.convid);
							break;
						default:
							break;
						}
					}}).start();
				}
			})
			.show();
			
			return true;
		}
	};
	
	private void refreshMain(int subop, String convid) {
		ConvActivity main = ((ConvActivity) viewHolder.parent);
		main.reloadMainViewRunnable.op = ConvActivity.ReloadMainViewRunnable.CONV_LIST_OP;
		main.reloadMainViewRunnable.subop = subop;
		main.reloadMainViewRunnable.convid = convid;
		main.reloadMainViewHandler.post(main.reloadMainViewRunnable);
	}
	
	public static class MainConvViewHolder {
		public Activity parent;
		public ImageView convNewBtn;
		public ImageView titleMenuBtn;
		public WeJoyMainViewGroup mainViewGroup;
	}
}