package com.wejoy.ui.adapter;

import java.util.List;
 
import com.wejoy.R;
import com.wejoy.module.ChatMessage;
import com.wejoy.module.ContactModule;
import com.wejoy.module.ConvertModule;
import com.wejoy.store.DataStore;
import com.wejoy.store.ImageStore;
import com.wejoy.ui.view.BadgeView;
import com.wejoy.util.DebugUtil;
import com.wejoy.util.ImageUtils;
import com.wejoy.util.MD5;
import com.wejoy.util.StringUtils;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;
/**
 * 
 * @author WeJoy group
 *
 */
public class ConvertAdapter extends BaseAdapter {
	public List<ConvertModule> convertlist;
	private LayoutInflater listContainer;//视图容器
	private Context context;//运行上下文
	private int itemViewResource;//自定义项视图源 
	private ImageStore imageStore;
	//自定义视图
	private ListView listView = null;
	private boolean scrollAction = false;
	
	public View.OnLongClickListener onLongClickListenernew;
	public OnClickListener onItemClickListener;
	
	/**
	 * 实例化Adapter
	 * @param context
	 * @param data
	 * @param resource
	 */
	public ConvertAdapter(Context context, List<ConvertModule> data, ListView listView, int resource) {
		this.context = context;			
		this.listContainer = LayoutInflater.from(context);	//创建视图容器并设置上下文
		this.itemViewResource = resource;
		this.convertlist = data;
		
		imageStore = ImageStore.getInstance();
		this.listView = listView;
		this.listView.setOnScrollListener(onScrollListener);
	}
	
	/**
	 * 目前没有做事情，为扩展用。
	 */
	AbsListView.OnScrollListener onScrollListener = new AbsListView.OnScrollListener() {
		@Override
		public void onScrollStateChanged(AbsListView view, int scrollState) {
			switch (scrollState) {
				case AbsListView.OnScrollListener.SCROLL_STATE_FLING:
					break;
				case AbsListView.OnScrollListener.SCROLL_STATE_IDLE:
					scrollAction = true;
					break;
				case AbsListView.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL:
					break;
				default:
					break;
			}
		}
		
		@Override
		public void onScroll(AbsListView view, int firstVisibleItem,
				int visibleItemCount, int totalItemCount) {
			// TODO Auto-generated method stub
		}
	};
	
	@Override
	public int getCount() {
		return convertlist.size();
	}
	
	@Override
	public Object getItem(int position) {
		return convertlist.get(position);
	}
	
	@Override
	public long getItemId(int position) {
		return position;
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ConvertModule conv = convertlist.get(position);
		ViewHolder holder = null;
		
		if(convertView == null) {
			convertView = listContainer.inflate(this.itemViewResource, null);
			
			//获取控件对象
			holder = new ViewHolder();
			holder.flag = (ImageView) convertView.findViewById(R.id.news_listitem_flag);
			holder.convFace = (ImageView) convertView.findViewById(R.id.convlist_face); // 头像
			holder.convName = (TextView) convertView.findViewById(R.id.news_listitem_title);
			holder.latestUpdateUserName = (TextView) convertView.findViewById(R.id.news_listitem_author);
			holder.latestUpdateMessage = (TextView) convertView.findViewById(R.id.news_listitem_date);
			holder.unreadCount = (TextView) convertView.findViewById(R.id.news_listitem_commentCount);
			holder.latestUpdateTime = (TextView) convertView.findViewById(R.id.news_latest_update_time);
			// badgeView 数字小图标
			holder.badgeView = new BadgeView(this.context, holder.convFace);
			holder.badgeView.setBadgeMargin(0, 0); 
			holder.badgeView.setBadgePosition(BadgeView.POSITION_TOP_RIGHT);
			holder.badgeView.setTextSize(12);
			
			convertView.setTag(holder);
		}
		else {
			holder = (ViewHolder) convertView.getTag();
		}
		
		//设置文字和图片
		holder.convName.setText(conv.convName);
		
		// @by jichao, don't show username if system message
		if(conv.latestUpdateUserName != null && !"".equals(conv.latestUpdateUserName)) {
			holder.latestUpdateUserName.setText(conv.latestUpdateUserName + ":");
			holder.latestUpdateUserName.setVisibility(View.VISIBLE);
		}
		else {
			holder.latestUpdateUserName.setVisibility(View.GONE);
		}
		
		holder.latestUpdateMessage.setText(conv.getLatestUpdateMessage());
		
		int count = conv.unreadCount;
		holder.badgeView.setVisibility(View.INVISIBLE); // 设置不可见
		// 设置未读数，如果为0就不显示
		if (count >0) {
			System.out.println("count:"+ count); 
			holder.badgeView.setText(String.valueOf(count));
			holder.badgeView.show(); 
		}
		holder.unreadCount.setText(String.valueOf(count));
		
		
		holder.latestUpdateTime.setText(StringUtils.friendly_time(conv.latestUpdateTime));
		holder.conv = conv;

		if(conv.convtype == ConvertModule.RECOMMAND) {
			holder.flag.setVisibility(View.VISIBLE);
			holder.convFace.setVisibility(View.GONE);
		}
		else if(conv.convtype == ConvertModule.SINGLE_CONV) {
			holder.flag.setVisibility(View.GONE);
			holder.convFace.setVisibility(View.VISIBLE);
			imageStore.reLoadImage(holder.convFace, conv.convListFacePic, imageLoadListener, null);
		}
		else if(conv.convtype == ConvertModule.GROUP_CONV) {
			holder.flag.setVisibility(View.GONE);
			holder.convFace.setVisibility(View.VISIBLE);
			ImageUtils.getDefaultGroupConvFaceImage(holder.convFace, conv.convListFacePic, 40, 40, imageLoadListener);
		}
		
		convertView.setOnClickListener(onItemClickListener);
		convertView.setOnLongClickListener(onLongClickListenernew);
		
		return convertView;
	}
	
	/**
	 * 异步获取image listener
	 */
	ImageStore.OnImageLoadListener imageLoadListener = new ImageStore.OnImageLoadListener(){
		@Override
		public void onImageLoad(ImageView iv, Bitmap bm) {
			if(iv != null) {
				iv.setImageBitmap(bm);
			}
		}
		
		@Override
		public void onError(ImageView iv, String msg) {
			// scrollAction防止出现过多次toast
			if(scrollAction) {
				DebugUtil.toast(context, msg);
				scrollAction = false;
			}
			
			if(iv != null) {
				iv.setImageResource(R.drawable.app_panel_friendcard_icon);
			}
		}
	};
	
//	private ImageStore.OnImageLoadListener listener = new ImageStore.OnImageLoadListener() {
//		public void onImageLoad(View convertView, Bitmap bm) {
//			ViewHolder holder = (ViewHolder) convertView.getTag();
//			ImageView iv = (ImageView) convertView.findViewById(R.id.group_member_face);
//			iv.setImageBitmap(decorate(bm));
//		}
//		
//		public void onError(String msg) {
//			DebugUtil.toast(context, msg);
//		}
//	};
	
	@Override()
	public void notifyDataSetChanged(){
		super.notifyDataSetChanged(); // 更新
	}
	
	public static class ViewHolder {
		public ImageView flag;
		public ImageView convFace;
		public TextView convName;
		public TextView latestUpdateUserName;
		public TextView latestUpdateMessage;
		public TextView unreadCount;
		public TextView latestUpdateTime;
		public ConvertModule conv;
		public BadgeView badgeView; // 小计数
	}
}
