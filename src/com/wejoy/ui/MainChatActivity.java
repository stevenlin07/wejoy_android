package com.wejoy.ui;
  
import com.wejoy.ui.adapter.ChatAdapterViewHolder;
import com.wejoy.ui.adapter.ChattingAdapter;
import com.wejoy.ui.helper.ImageUpDownLoadAnimaHanlder;
import com.wejoy.ui.helper.UIHelper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
 

import com.wejoy.common.MediaManager;
import com.wejoy.common.MediaManager.RecordCallBack;
import com.wejoy.module.ChatMessage;
import com.wejoy.module.ContactModule;
import com.wejoy.module.ConvertModule;
import com.wejoy.module.Emotion;
import com.wejoy.service.apphandler.SendAudioHandler;
import com.wejoy.service.apphandler.SendFileHandler;
import com.wejoy.service.apphandler.SendMessageHandler;
import com.wejoy.service.apphandler.WeiboUserInfoHandler;
import com.wejoy.service.serverhandler.GetFileHandler;
import com.wejoy.store.AudioStore;
import com.wejoy.store.DataStore;
import com.wejoy.store.EmotionStore;
import com.wejoy.store.ImageStore;
import com.wejoy.ui.view.PullToRefreshListView;
import com.wejoy.util.BroadcastId;
import com.wejoy.util.CommonUtil;
import com.wejoy.util.DebugUtil;
import com.wejoy.util.EmotionalFaceUtil;
import com.wejoy.util.ImageUtils;
import com.wejoy.util.MD5;
import com.wejoy.util.SDCardUtil;
import com.wejoy.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.text.InputType;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ImageSpan;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

/**
 * 
 * @author WeJoy Group
 *
 */
public class MainChatActivity extends Activity {
	@Override
	protected void onResume() {
		super.onResume();
		
		// @by jichao, setUnread方法会发广播，更新很多界面，不仅导致慢，而且导致更新逻辑错误
		// 不可以在这里调用
		// setUnreadInit();
	}

	protected static final String TAG = "MainActivity";
	private ChattingAdapter chatHistoryAdapter;
	private PullToRefreshListView chatHistoryLv;
	private Button sendBtn;
	private ImageView emotionalFace;
	private EditText textEditor;
	private ImageView sendImageIv;
	private ImageView captureImageIv;
	private ImageView editGroupBtn;
	private View recording;
	private PopupWindow recordingPopWin = null;
	private PopupWindow recordShortWarnPopWin = null;
	private View warnPopWinView;
	private TextView warnPopWinTV;
	private Bitmap photo;
	private ImageView backtohome;
	int[] faceIds = new int[156];
	private String iTempFileNameString = "iRecorder_"; 
	private File recAudioDir;
	private File recAudioFile;
	private MediaRecorder recorder;
	private long recordingTime = 0;
	private boolean isStopRecord;
	private List<ChatMessage> chatcache = new ArrayList<ChatMessage>();
	
	private static final String IMAGE_STORE = "wejoy/temp/images/";
	GridView faceGrid;
	InputMethodManager imm;

	private ConvertModule currentConversation = null;
	private Thread imageUploadProcess = null;
	private ActivityReceiver activityReceiver; // 接收系统的广播
	private StringBuilder sBuilder = new StringBuilder();

	// record views
	private MediaManager mdManager;
	private ImageView mRecordImage;
	private TextView mRecordText;
	private Handler mHandler;
	private int m_VolumLevel = 0;
	private View audioRecordingView;
	private View chatLV;
	
	private Handler imageTagHandler;
	// camera pic temp uri
	private Uri photoUri;
	
	public static final int PAGE_COUNT = 15;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		startBroadcast(); // 初始化
		
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);  
		super.onCreate(savedInstanceState);
		getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE|WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
		imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
		setContentView(R.layout.chatting);
		chatHistoryLv = (PullToRefreshListView) findViewById(R.id.chatting_history_lv);
		
		sendBtn = (Button) findViewById(R.id.send_button);
		textEditor = (EditText) findViewById(R.id.text_editor);
		sendImageIv = (ImageView) findViewById(R.id.send_image);
		emotionalFace = (ImageView) findViewById(R.id.sms_button_insert);
		captureImageIv = (ImageView) findViewById(R.id.capture_image);
		backtohome = (ImageView) findViewById(R.id.news_detail_home);
		editGroupBtn = (ImageView) findViewById(R.id.edit_group_btn);
		
		// populate recording views
		mHandler = new Handler();
		mdManager = MediaManager.getMediaPlayManager();
		audioRecordingView = LayoutInflater.from(this).inflate(R.layout.audiorecoderring, null);
		chatLV = (View) findViewById(R.id.chat_root);
		mRecordImage = (ImageView) audioRecordingView.findViewById(R.id.speak_image);
		mRecordText = (TextView) audioRecordingView.findViewById(R.id.speak_text);
		recording = findViewById(R.id.recording);
		initRecordBtnListener();
		
		// record short warnning pop win
		warnPopWinView = LayoutInflater.from(this).inflate(R.layout.audiorecordshort, null);
		warnPopWinTV = (TextView) warnPopWinView.findViewById(R.id.audio_short_warn_text);
		
		// populate chatting data
		Intent intent = getIntent();
		Bundle bl= intent.getExtras();
		String convjson = bl.getString("convjson");
		currentConversation = ConvertModule.parseJson(convjson);
		
		if(currentConversation.convtype == ConvertModule.READONLY_CONV) {
			setToReadOnly();
		}
		else {
			setToNormal();
		}
		
		initEmotionalFaceGrid();
		
		chatHistoryLv.setOnItemClickListener(new OnItemClickListener(){
			@Override
			public void onItemClick(AdapterView<?> adapter, View v, int position,
					long id) {
				imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
				faceGrid.setVisibility(View.GONE);
			}
		});
		
		TextView chattitle = (TextView) findViewById(R.id.chatting_contact_name);
		chattitle.setText(getChatTitle());
		
		// set adapter
		setAdapterForThis();
		
		imageTagHandler = new Handler(){
			@Override
			public void handleMessage(Message msg){
				if(msg.what == 0x0001) {
					chatHistoryAdapter.notifyDataSetChanged();
				}
			}
		};
	}
 
	private ChatMessage getTimeInfo(long now, long created_at) {
		long waittime = now - created_at;
		ChatMessage chat = new ChatMessage();
		chat.direction = ChatMessage.MESSAGE_TIME_INFO;
		chat.created_at = System.currentTimeMillis();
		chat.msgid = String.valueOf(chat.created_at);
		
		if(waittime >= CommonUtil.MIN_5 && waittime < CommonUtil.MIN_10) {
			chat.content = "5分钟之前";
		}
		else if(waittime >= CommonUtil.MIN_10 && waittime < CommonUtil.MIN_20) {
			chat.content = "10分钟之前";
		}
		else if(waittime >= CommonUtil.MIN_20 && waittime < CommonUtil.MIN_30) {
			chat.content = "20分钟之前";
		}
		else if(waittime >= CommonUtil.MIN_30 && waittime < CommonUtil.HOUR_1) {
			chat.content = "30分钟之前";
		}
		else if(waittime >= CommonUtil.HOUR_1) {
			chat.content = CommonUtil.getDisplayTime(created_at);
		}
		else {
			chat = null;
		}
		
		return chat;
	}
	
	/**
	 * @by jichao, always clear and reload
	 */
	private void refreshChatAdapter() {
		List<ChatMessage> chatMessages0 = DataStore.getInstance().queryChatMessage(currentConversation.convid, -1, PAGE_COUNT);
		chatcache.clear();
		long now = System.currentTimeMillis();
		long lasttimeinfo = 0;
	
		if(chatMessages0 != null) {
			for(ChatMessage chat : chatMessages0) {
				// 产生标志时间信息的临时系统消息
				// 今天的消息每20分钟打出时间一次
				if(Math.abs(now - chat.created_at) < CommonUtil.DAY_1 &&
						Math.abs(chat.created_at - lasttimeinfo) > CommonUtil.MIN_20) 
				{
					ChatMessage timeinfo = getTimeInfo(now, chat.created_at);
					
					if(timeinfo != null) {
						lasttimeinfo = chat.created_at;
						chatcache.add(timeinfo);
					}
				}
				// 超出一天的每一小时打出一次
				else if(Math.abs(now - chat.created_at) >= CommonUtil.DAY_1 &&
						Math.abs(chat.created_at - lasttimeinfo) > CommonUtil.HOUR_1) 
				{
					ChatMessage timeinfo = getTimeInfo(now, chat.created_at);
					
					if(timeinfo != null) {
						lasttimeinfo = chat.created_at;
						chatcache.add(timeinfo);
					}
				}
				
				chatcache.add(chat);
			}
			
			chatHistoryLv.setSelection(chatcache.size() - 1);
			chatHistoryAdapter.notifyDataSetChanged();
		}
	}
	
	private void setToReadOnly() {
		textEditor.setInputType(InputType.TYPE_NULL);
		sendBtn.setEnabled(false);
		sendBtn.setOnClickListener(null);
		emotionalFace.setOnClickListener(null);
		sendImageIv.setOnClickListener(null);
		captureImageIv.setOnClickListener(null);
		backtohome.setOnClickListener(null);
		editGroupBtn.setOnClickListener(null);
	}
	
	private void setToNormal() {
		textEditor.setInputType(InputType.TYPE_CLASS_TEXT);
		sendBtn.setEnabled(true);
		sendBtn.setOnClickListener(onClickListener);
		emotionalFace.setOnClickListener(onClickListener);
		sendImageIv.setOnClickListener(onClickListener);
		captureImageIv.setOnClickListener(onClickListener);
		backtohome.setOnClickListener(onClickListener);
		editGroupBtn.setOnClickListener(onClickListener);
	}
	
	public Runnable refreshCHatRunnable = new Runnable() {
		public void run() {
			refreshChatAdapter();
		}
	};
	
	public Handler refreshCHatHandler = new Handler();
	
	private void setAdapterForThis() {
		chatHistoryAdapter = new ChattingAdapter(this, chatHistoryLv, chatcache);
		chatHistoryAdapter.imageChatClickListener = imageChatClickListener;
		chatHistoryAdapter.onLongClickListener = onLongClickListener;
		chatHistoryLv.setSelection(3);
		chatHistoryLv.setAdapter(chatHistoryAdapter);
		chatHistoryLv.chatHistoryAdapter = chatHistoryAdapter;
		chatHistoryLv.currentConversation = currentConversation;
		
		refreshChatAdapter();
		
		chatHistoryAdapter.notifyDataSetChanged();
		currentConversation = DataStore.getInstance().getConvertById(currentConversation.convid);
		
		if(currentConversation == null || currentConversation.convtype != ConvertModule.READONLY_CONV) {
			setToNormal();
		}
		else {
			setToReadOnly();
		}
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
		super.onActivityResult(requestCode, resultCode, intent);
		BitmapFactory.Options opts = new BitmapFactory.Options();
		opts.inSampleSize = 4;
		
		if(requestCode == Activity.DEFAULT_KEYS_SHORTCUT) {
			if(resultCode == RESULT_OK) {
				Uri uri = intent.getData();
				
				if (uri != null) {
					photo = BitmapFactory.decodeFile(getContentFilePath(uri), opts);
				}
				
				if (photo == null) {
					Bundle bundle = intent.getExtras();
					
					if (bundle != null){
						photo=(Bitmap) bundle.get("data");
					} 
					else {
						Toast.makeText(MainChatActivity.this, getString(R.string.common_msg_get_photo_failure), Toast.LENGTH_LONG).show();
					}
				}
				
	            Bitmap thumbnail = ImageUtils.getThumbnail(photo);
				sendImageMessage(thumbnail, getContentFilePath(uri));
			}
		} 
		else if (requestCode == Activity.DEFAULT_KEYS_DIALER) {
			if(resultCode == RESULT_OK){
				String[] pojo = {MediaStore.Images.Media.DATA};
				Cursor cursor = managedQuery(photoUri, pojo, null, null,null);
				String picpath = null;
				
				if(cursor != null ) {
					int columnIndex = cursor.getColumnIndexOrThrow(pojo[0]);
					cursor.moveToFirst();
					picpath = cursor.getString(columnIndex);
				}
				
				try {
					photo = BitmapFactory.decodeStream(new FileInputStream(picpath), null, opts);
				} 
				catch (FileNotFoundException e) {
					DebugUtil.error("camera take picture", "", e);
				}

				Bitmap thumbnail = ImageUtils.getThumbnail(photo);
				sendImageMessage(thumbnail, picpath);
			}
		}
	}

	View.OnClickListener imageChatClickListener = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			ChatAdapterViewHolder holder = (ChatAdapterViewHolder) v.getTag();
			
			// show full image view
			if(holder.chat.type == ChatMessage.TYPE_PIC && holder.chat.attachPath != null) {
				Intent intent = new Intent();
				Bundle bl = new Bundle();
				bl.putString("chatmessage", holder.chat.toJson());
				intent.putExtras(bl);
				intent.setAction("FullSizeImageActivity");
				startActivity(intent);
			}
			// 点击下载那具体时间，地点问谁呢，有联系人吗？
			else if(holder.chat.type == ChatMessage.TYPE_PIC && holder.chat.direction == ChatMessage.MESSAGE_FROM) {
				ImageUpDownLoadAnimaHanlder uphandler = new ImageUpDownLoadAnimaHanlder(false);
				uphandler.holder = holder;
				uphandler.setProgress(0);
				
				ChatMessage chat = holder.chat;
				String path = ImageStore.getImageStorePath() + File.separator + MD5.getMD5(chat.fileId + chat.fileName);
				GetFileHandler getFileHandler = new GetFileHandler();
				getFileHandler.process(chat, path, uphandler, getImageFinishHandler);
			}
		}
	};

	private View.OnClickListener onClickListener = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			if(v.getId() == editGroupBtn.getId()) {
				Intent intent = new Intent();
				Bundle bl = new Bundle();
				bl.putString("groupid", currentConversation.chatwithid);
				bl.putString("isEditable", "true");
				intent.putExtras(bl);
				intent.setAction("ShowGroupMembers");
				startActivityForResult(intent, 0);
			}
			else if (v.getId() == sendBtn.getId()) {
				String str = textEditor.getText().toString();
				String sendStr;

				if (str != null	&& (sendStr = str.trim().replaceAll("\r", "").replaceAll("\t", "").replaceAll("\n", "")
					.replaceAll("\f", "")) != "") 
				{
					sendTextMessage(sendStr);
				}
				
				textEditor.setText("");
			} 
			else if (v.getId() == sendImageIv.getId()) {
				Intent i = new Intent();
				i.setType("image/*");
				i.setAction(Intent.ACTION_GET_CONTENT);
				
				startActivityForResult(i, Activity.DEFAULT_KEYS_SHORTCUT);
			} 
			else if (v.getId() == captureImageIv.getId()) {
				if(SDCardUtil.isSDCardWritable()) {
					Intent intent = new Intent("android.media.action.IMAGE_CAPTURE");
					/***
					 * 需要说明一下，以下操作使用照相机拍照，拍照后的图片会存放在相册中的
					 * 这里使用的这种方式有一个好处就是获取的图片是拍照后的原图
					 * 如果不实用ContentValues存放照片路径的话，拍照后获取的图片为缩略图不清晰
					 */
					ContentValues values = new ContentValues();
					values.put(android.provider.MediaStore.Images.Media.TITLE, getTempFileName());
					photoUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
					intent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, photoUri);
					
					startActivityForResult(intent, Activity.DEFAULT_KEYS_DIALER);
				}
				else {
					Toast.makeText(MainChatActivity.this, R.string.common_msg_nosdcard, Toast.LENGTH_LONG).show();
				}
			} 
			else if (v.getId() == backtohome.getId()){
				finish();
			} 
			else if (v.getId() == emotionalFace.getId()){
				if(faceGrid.getVisibility() == View.VISIBLE){
					faceGrid.setVisibility(View.GONE);
				} 
				else {
					faceGrid.setVisibility(View.VISIBLE);
				}
				
				imm.hideSoftInputFromWindow(textEditor.getWindowToken(), 0);
			}
		}
	};
	
	private String getChatTitle() {
		return currentConversation.convName != null ? 
			getShortTitle(currentConversation.convName) :
			getDefaultChatTitle() ;
	}
	
	private String getDefaultChatTitle() {
		List<ContactModule> members = DataStore.getInstance().queryMembersByGroupId(currentConversation.chatwithid);
		StringBuilder sb = new StringBuilder();
		
		if(members != null && !members.isEmpty()) {
			for(ContactModule c : members) {
				sb.append(c.name);
				
				if(sb.length() > 5) {
					break;
				}
			}
		}
		
		return getShortTitle(sb.toString());
	}
	
	private String getShortTitle(String title) {
		return title.length() > 5 ? 
			title.substring(0, 5) + "..." : 
			title; 
	}
	
	public static final String[] mainChatPopwinMenu = new String[] {"删除", "取消"};
	public static final String[] senderChatPopwinMenu = new String[] {"重发", "删除", "取消"};
	private ChatAdapterViewHolder holder = null;
	
	View.OnLongClickListener onLongClickListener = new View.OnLongClickListener() {
		public boolean onLongClick(View v) {
			holder = (ChatAdapterViewHolder) v.getTag();
			final String[] menu = holder.chat.direction == ChatMessage.MESSAGE_TO ?
				senderChatPopwinMenu : mainChatPopwinMenu;
			
			new AlertDialog.Builder(MainChatActivity.this)
				.setTitle(getChatTitle())
				.setItems(menu, new DialogInterface.OnClickListener() 
			{
				public void onClick(DialogInterface dialog, int which) {
					if (menu[which].equals("重发")) {
						String id = holder != null && holder.chat != null ? String.valueOf(holder.chat.id) : null;
						String convid = holder != null && holder.chat != null ? String.valueOf(holder.chat.convid) : null;

						if(id != null) {
							sendMessage(holder.chat.clone());
						}
					}
					else if (menu[which].equals("删除")) {
						UIHelper.showWaiting(MainChatActivity.this, "正在处理...", false);
						
						mHandler.post(new Runnable() {public void run() {
							final String id = holder != null && holder.chat != null ? String.valueOf(holder.chat.id) : null;
							final String convid = holder != null && holder.chat != null ? String.valueOf(holder.chat.convid) : null;
							
							if(id != null) {
								refreshCHatHandler.post(new Runnable() {public void run() {
									if(chatcache != null && chatcache.size() > 0) {
										Iterator<ChatMessage> iterator = chatcache.iterator();
										
										while(iterator.hasNext()) {
											ChatMessage chat = iterator.next();
											
											if(chat.id == holder.chat.id) {
												iterator.remove();
											}
										}
										
										chatHistoryLv.setSelection(chatcache.size() - 1);
										chatHistoryAdapter.notifyDataSetChanged();
										
										UIHelper.hideWaiting(MainChatActivity.this);
									}
								}}); // 实现数据的实时刷新
								
								mHandler.post(new Runnable() {public void run() {
									DataStore.getInstance().removeChatMessage(convid, id);
								}});
							}
						}});
					}
				}
			})
			.show();
			
			return true;
		}
	};
	
	private String getTempFileName() {
		return "wejoy_camera" + System.currentTimeMillis();
	}
	
	private void sendTextMessage(String text) {
		ChatMessage chat = new ChatMessage(currentConversation.convid, ChatMessage.MESSAGE_TO, ChatMessage.TYPE_TEXT, text);
		sendMessage(chat);
	}
	
	private void sendImageMessage(Bitmap thumbnail, String picpath) {
		ChatMessage chat = new ChatMessage(currentConversation.convid, ChatMessage.MESSAGE_TO, ChatMessage.TYPE_PIC, null);
		chat.thumbnail = thumbnail;
		chat.attachPath = picpath;
		sendMessage(chat);
	}
	
	static ThreadPoolExecutor pool = new ThreadPoolExecutor(Runtime.getRuntime().availableProcessors(), 
			Runtime.getRuntime().availableProcessors() * 2, 60, TimeUnit.SECONDS,
			new java.util.concurrent.LinkedBlockingQueue<Runnable>());
	
	private SendMessageHandler.SendMessageFinishListener sendMessageFinishListener = 
		new SendMessageHandler.SendMessageFinishListener() 
	{
		public void onSuccess(ChatMessage chat) {
			chat.sendState = ChatMessage.TO_SEND_SUCC;
			DataStore.getInstance().insertOrUpdateChatMessage(chat);
			refreshCHatHandler.post(refreshCHatRunnable);
		}

		public void onFailed(ChatMessage chat, String msg) {
			chat.sendState = ChatMessage.TO_SEND_ERR;
			DataStore.getInstance().insertOrUpdateChatMessage(chat);
			refreshCHatHandler.post(refreshCHatRunnable);
		}
	};
	
	private SendMessageHandler.SendMessageFinishListener getImageFinishHandler = 
		new SendMessageHandler.SendMessageFinishListener() 
	{
		public void onSuccess(ChatMessage chat) {
			showImage(chat);
		}
		
		public void onFailed(ChatMessage chat, String msg) {
			// UIHelper.showInfoPop();
		}
		
		private void showImage(ChatMessage chat) {
			Intent intent = new Intent();
			Bundle bl = new Bundle();
			bl.putString("chatmessage", chat.toJson());
			intent.putExtras(bl);
			intent.setAction("FullSizeImageActivity");
			startActivity(intent);
		}
	};
	
	private void sendMessage(final ChatMessage chat) {
		// set a msgid for chat message
		chat.msgid = String.valueOf(System.currentTimeMillis());
		
		// @by jichao, set state to be sending
		chat.sendState = ChatMessage.TO_SENDING;
		chat.uid = CommonUtil.getCurrUserId();
		
		if(DataStore.getInstance().getOwnerUser() != null) {
			chat.usrname = DataStore.getInstance().getOwnerUser().name;
			chat.userFaceUrl = DataStore.getInstance().getOwnerUser().faceurl;
		}
		else {
			WeiboUserInfoHandler uihandler = new WeiboUserInfoHandler();
			uihandler.process(chat.uid);
		}
		
		chatcache.add(chat);
		chatHistoryAdapter.notifyDataSetChanged();
		
		if(chat.type == ChatMessage.TYPE_TEXT) {
			SendMessageHandler sendMessageHandler = new SendMessageHandler();
			sendMessageHandler.sendText(chat, currentConversation.chatwithid, currentConversation.convtype, sendMessageFinishListener);
		}
		else if(chat.type == ChatMessage.TYPE_AUDIO) {
			SendAudioHandler audioSender = new SendAudioHandler();
			byte[] data = CommonUtil.readFile(chat.attachPath);
			
			if(data != null) {
				audioSender.processGroupChat(chat, currentConversation.chatwithid, 1, data, true, sendMessageFinishListener);
			}
			else {
				showWarningPop("录音失败，请重新录音");
			}
		}
		else if(chat.type == ChatMessage.TYPE_PIC) {
			SendFileHandler fileSender = new SendFileHandler();
			String filename = chat.attachPath != null && chat.attachPath.indexOf(File.separator) >= 0 ?
				chat.attachPath.substring(chat.attachPath.lastIndexOf(File.separator) + 1, chat.attachPath.length()) :
				null;
			fileSender.processImage(chat, currentConversation.chatwithid, 
				currentConversation.convtype, chat.thumbnail, filename, sendMessageFinishListener);
		
			MediaManager.getMediaPlayManager().setSendFileHandler(chat.attachPath, fileSender);
		}
	}

	private String getContentFilePath(Uri uri){
	    String[] projection = {MediaStore.MediaColumns.DATA};
	    Cursor cur = managedQuery(uri, projection, null, null, null);
	    int column_index_data = cur.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
	    cur.moveToFirst();
	    return cur.getString(column_index_data);
	}

	private void initEmotionalFaceGrid(){
		List<Map<String,Object>> listItems = new ArrayList<Map<String,Object>>();
		//生成107个表情的id，封装
		for(int i = 0; i < faceIds.length; i++){
			try {
				if(i<10){
					Field field = R.drawable.class.getDeclaredField("face00" + i);
					int resourceId = Integer.parseInt(field.get(null).toString());
					faceIds[i] = resourceId;
				}else if(i<100){
					Field field = R.drawable.class.getDeclaredField("face0" + i);
					int resourceId = Integer.parseInt(field.get(null).toString());
					faceIds[i] = resourceId;
				}else{
					Field field = R.drawable.class.getDeclaredField("face" + i);
					int resourceId = Integer.parseInt(field.get(null).toString());
					faceIds[i] = resourceId;
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			
	        Map<String,Object> listItem = new HashMap<String,Object>();
			listItem.put("image", faceIds[i]);
			listItems.add(listItem);
		}
	
		//创建一个SimpleAdapter
		SimpleAdapter simpleAdapter = new SimpleAdapter(this
			, listItems 
			, R.layout.emotionalface_cell
			, new String[]{"image"}
			, new int[]{R.id.image1});
		faceGrid = (GridView)findViewById(R.id.grid01);
		faceGrid.setAdapter(simpleAdapter);
		faceGrid.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> adapterView, View v, int position,
					long id) {
				Bitmap bitmap = BitmapFactory.decodeResource(getResources(), faceIds[position % faceIds.length]);
				Bitmap bm = EmotionalFaceUtil.bmselfAdjust(bitmap);
				ImageSpan imageSpan = new ImageSpan(MainChatActivity.this, bm);
				String str = null;
				Emotion emotion = EmotionStore.getInstance().getEmotionByPos(position);

				if(emotion != null){
					str = emotion.string_chinese;
				}

				SpannableString spannableString = new SpannableString(str);
				spannableString.setSpan(imageSpan, 0, 4, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
				textEditor.append(spannableString);
			}
		});
	}
	
	@Override  
	 public boolean onTouchEvent(MotionEvent event) {  
	  if(event.getAction() == MotionEvent.ACTION_DOWN){  
	     if(getCurrentFocus()!=null && getCurrentFocus().getWindowToken()!=null){  
	       imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);  
	     }  
	  }  
	  return super.onTouchEvent(event);  
	 } 
	
	@Override
	protected void onStop(){
		if(recorder != null && !isStopRecord){
			recorder.stop();
			recorder.reset();
			recorder.release();
			recorder = null;
		}
		super.onStop();
	}

	/** 接收收到新的会话消息的变更通知*/
	public class ActivityReceiver extends BroadcastReceiver{

		@Override
		public void onReceive(Context context, Intent intent) {
			refreshCHatHandler.post(refreshCHatRunnable);
		}
	}
	
	/** 初始化广播*/
	public void startBroadcast() {
		IntentFilter filter = new IntentFilter();
		activityReceiver = new ActivityReceiver();
		filter.addAction(BroadcastId.CONV_UPDATE); // 指定BroadcastReceiver监听的Action
		registerReceiver(activityReceiver,filter); // 注册广播
	}
	
	private void initRecordBtnListener() {
		recording.setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				int action = event.getAction();
				
				switch (action) {
				case MotionEvent.ACTION_DOWN:
					try {
						startRealTimeRecord();
					} catch (Exception e) {
						e.printStackTrace();
					}
					
					break;
				case MotionEvent.ACTION_UP:
					stopRealTimeRecord();
					break;
				}
				
				return true;
			}
		});
	}
	
	private void startRealTimeRecord() throws Exception {
		recordingPopWin = new PopupWindow(audioRecordingView, 220, 220);
		recordingPopWin.showAtLocation(chatLV, Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL, 0, 0);
		
		mdManager.setRecordCallBack(new RecordCallBack() {
			public void recordVolumeCallback(final long value) {
				mHandler.postDelayed(new Runnable() {

					@Override
					public void run() {
						// TODO Auto-generated method stub
						int level = (int) Math.sqrt(value / 70);
						level = level > 6 ? 6 : level;

						if (m_VolumLevel != level) {
							m_VolumLevel = level;
							mRecordImage.getDrawable().setLevel(m_VolumLevel);
						}
					}
				}, 0);
			}

			public void recordStopCallback(long size) {
				Log.i("rec", "stop rec: " + size);
				mHandler.post(new Runnable() {

					@Override
					public void run() {
						m_VolumLevel = 0;
						mRecordImage.getDrawable().setLevel(m_VolumLevel);
						
						// @by jichao, 停止录音后，显示正在上传状态
						refreshCHatHandler.post(refreshCHatRunnable);
					}
				});
			}

			@Override
			public void recordStartCallback(boolean bstarted) {
				// Log.i("rec", "start rec: " + bstarted);
			}
		});

		ChatMessage chat = AudioStore.getInstance().getNewAudioChat(
			currentConversation.convid, currentConversation.chatwithid);
		mdManager.startRealTimeRecord(chat, currentConversation.chatwithid, sendMessageFinishListener);
	}

	private void stopRealTimeRecord() {
		if (recordingPopWin != null) {
			recordingPopWin.dismiss();
		}
		
		showWarningPop("录音时间太短");
	}
	
	private void showWarningPop(String text) {
		if(!mdManager.stopRealTimeRecord()) {
			if (recordShortWarnPopWin != null) {
				recordShortWarnPopWin.dismiss();
			}
			
			warnPopWinTV.setText(text);
			recordShortWarnPopWin = new PopupWindow(warnPopWinView, 220, 220);
			recordShortWarnPopWin.showAtLocation(chatLV, Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL, 0, 0);
			
			mHandler.postDelayed(new Runnable() {
				public void run() {
					if (recordShortWarnPopWin != null) {
						recordShortWarnPopWin.dismiss();
					}
				}
			}, 1200);
		}
	}
	
	/** 发送消息广播*/
	public void sendMsgBroadcast() {
		Intent intent = new Intent();
		intent.setAction(BroadcastId.CONV_UPDATE);
		sendBroadcast(intent);
	}
	
	public void setUnreadInit() {
		// 清理计数
		currentConversation.unreadCount = 0 ;
		// 传递给mainActivity
		DataStore.getInstance().updateConvert(currentConversation);
		sendMsgBroadcast(); // 也可用startActivity
	}
	
	@Override  
	protected void onDestroy() {  
		if(activityReceiver!=null) {
			this.unregisterReceiver(activityReceiver); 
		}
		setUnreadInit();
	    super.onDestroy();  
	}
}