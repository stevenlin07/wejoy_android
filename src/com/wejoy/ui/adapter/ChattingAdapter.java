package com.wejoy.ui.adapter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

import com.wejoy.R;
import android.widget.FrameLayout; 
import android.view.ViewGroup.LayoutParams;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.text.SpannableString;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.view.MotionEvent;

import com.wejoy.common.MediaManager;
import com.wejoy.module.ChatMessage;
import com.wejoy.module.ContactModule;
import com.wejoy.service.apphandler.SendFileHandler;
import com.wejoy.store.DataStore;
import com.wejoy.store.ImageStore;
import com.wejoy.ui.MainChatActivity;
import com.wejoy.ui.helper.AudioAnimationHandler;
import com.wejoy.ui.helper.ImageUpDownLoadAnimaHanlder;
import com.wejoy.ui.helper.UploadAnimationHandler;
import com.wejoy.util.CommonUtil;
import com.wejoy.util.DebugUtil;
import com.wejoy.util.EmotionalFaceUtil;
import com.wejoy.util.ImageUtils;
import com.wejoy.util.MD5;
/**
 * 
 * @author wejoy group
 *
 */
public class ChattingAdapter extends BaseAdapter {
	
	protected static final String TAG = "ChattingAdapter";
	private Context context;
	public List<ChatMessage> chatMessages;
	public View.OnClickListener imageChatClickListener;
	public View.OnLongClickListener onLongClickListener;

	//语音动画控制器  
	Timer mTimer = new Timer();
	//语音动画控制任务  
	AudioPlayTimerTask mTimerTask = null;
	Map<String, UploadTimerTask> uploadTimerTasks = new ConcurrentHashMap<String, UploadTimerTask>(); 
	AudioAnimationHandler audioAnimationHandler = null;
	UploadAnimationHandler uploadAnimationHandler = null;
	
	public ChattingAdapter(Context context, ListView chatHistoryLv, List<ChatMessage> messages) {
		super();
		this.context = context;
		this.chatMessages = messages;
	}

	@Override
	public int getCount() {
		return chatMessages.size();
	}

	@Override
	public Object getItem(int position) {
		return chatMessages.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}
	
	private View getView(View view, ChatMessage message) {
		if(view != null) {
			ChatAdapterViewHolder holder = (ChatAdapterViewHolder) view.getTag();
			holder.chat = message;
			return view;
		}
		else {
			view = LayoutInflater.from(context).inflate(R.layout.chattingitem, null);
			ChatAdapterViewHolder holder = new ChatAdapterViewHolder();
			view.setTag(holder);
			holder.populateView(view, message);
			return view;
		}
	}
	
	@Override
	public View getView(int position, View view, ViewGroup parent) {
		ChatMessage message = chatMessages.get(position);
		view = getView(view, message);
		
		if (message.getDirection() == ChatMessage.MESSAGE_SYSTEM || message.getDirection() == ChatMessage.MESSAGE_TIME_INFO)
		{
			ChatAdapterViewHolder holder = (ChatAdapterViewHolder) view.getTag();
			holder.setAllViewHide();
			holder.chat_system_layout.setVisibility(View.VISIBLE);
			holder.system_content.setVisibility(View.VISIBLE);
			holder.system_content.setText(message.getContent());
		}
		else if (message.getDirection() == ChatMessage.MESSAGE_FROM) {
			populateRecvMessage(view, message);
		} 
		else if(message.getDirection() == ChatMessage.MESSAGE_TO) {
			populateSendToMessage(view, message);
		}
		
		return view;
	}
	
	private void populateRecvMessage(View view, ChatMessage message) {
		ChatAdapterViewHolder holder = (ChatAdapterViewHolder) view.getTag();
		holder.setAllViewHide();
		holder.chat_from_layout.setVisibility(View.VISIBLE);
		
		if (message.getType() == ChatMessage.TYPE_TEXT){
			holder.chat_from_content_tv.setVisibility(View.VISIBLE);
			holder.chat_from_content_tv.setTag(holder);
			holder.chat_from_content_tv.setOnLongClickListener(onLongClickListener);

			String strtmp = (String) message.getContent();
			try{
				SpannableString spannableString = EmotionalFaceUtil.getExpressionString(context, strtmp);
				holder.chat_from_content_tv.setText(spannableString);
			} 
			catch (Exception e) {
				DebugUtil.error("", "", e);
			}
		}
		else if(message.getType() == ChatMessage.TYPE_AUDIO) {
			holder.chat_from_audio_linerlayout.setVisibility(View.VISIBLE);
			holder.chat_from_state.setVisibility(View.VISIBLE);
			holder.chatfrom_voice_text.setVisibility(View.VISIBLE);
			holder.chatfrom_voice_time.setVisibility(View.VISIBLE);
			
			holder.chatfrom_voice_time.setText(String.valueOf(message.audioTime) + "''");
			
			holder.chat_from_state.setImageResource(message.direction == ChatMessage.MESSAGE_FROM ? 
				R.drawable.chatfrom_voice_playing : R.drawable.chatto_voice_playing);
			
			holder.chatfrom_voice_text.setClickable(true);
			holder.chatfrom_voice_text.setText(getAudioText(message.audioTime));

			// @by jichao, here have to add tag again
			holder.chat_from_state.setTag(holder);
			holder.chat_from_state.setOnClickListener(audioClickListener);
			holder.chat_from_state.setOnLongClickListener(onLongClickListener);

			holder.chatfrom_voice_text.setTag(holder);
			holder.chatfrom_voice_text.setOnClickListener(audioClickListener);
			holder.chatfrom_voice_text.setOnLongClickListener(onLongClickListener);
			
			holder.chat_from_audio_linerlayout.setTag(holder);
			holder.chat_from_audio_linerlayout.setOnClickListener(audioClickListener);
			holder.chat_from_audio_linerlayout.setOnLongClickListener(onLongClickListener);
		}
		else if(message.getType() == ChatMessage.TYPE_PIC) {
			holder.chat_from_pic_frame.setVisibility(View.VISIBLE);
			holder.chat_from_content_pic.setVisibility(View.VISIBLE);
			
			if(message.thumbnail != null) {
				holder.chat_from_content_pic.setImageBitmap(message.thumbnail);
			}
			else {
				holder.chat_from_content_pic.setImageResource(R.drawable.ic_full_image_failed);
			}
			
			// @by jichao, MUST set holder for image, otherwize onclick image func was failed
			holder.chat_from_content_pic.setTag(holder);
			holder.chat_from_content_pic.setClickable(true);
			holder.chat_from_content_pic.setOnClickListener(imageChatClickListener);
			holder.chat_from_content_pic.setOnLongClickListener(onLongClickListener);

			// if image is too small, user can also click it
			holder.chat_from_pic_frame.setTag(holder);
			holder.chat_from_pic_frame.setClickable(true);
			holder.chat_from_pic_frame.setOnClickListener(imageChatClickListener);
			holder.chat_from_pic_frame.setOnLongClickListener(onLongClickListener);
		}
		
		String url = message.getUrl();
		holder.chat_from_face_iv.setVisibility(View.VISIBLE);
		Bitmap userface = ImageStore.getInstance().getImage(url);
		
		if(userface != null) {
			userface = ImageUtils.toRoundCorner(userface, 10);
			holder.chat_from_face_iv.setImageBitmap(userface);
		}
		else {
			holder.imageHeadIcon.setImageResource(R.drawable.ic_launcher);
		}
	}
	
	private void populateSendToMessage(View view, ChatMessage message) {
		ChatAdapterViewHolder holder = (ChatAdapterViewHolder) view.getTag();
		holder.setAllViewHide();
		holder.chat_to_layout.setVisibility(View.VISIBLE);
		
		if (message.getType() == ChatMessage.TYPE_TEXT) {
			holder.chattext.setVisibility(View.VISIBLE);
			holder.chattext.setOnLongClickListener(onLongClickListener);
			holder.chattext.setTag(holder);
			String strtmp = (String) message.getContent();
			
			try{
				SpannableString spannableString = EmotionalFaceUtil.getExpressionString(context, strtmp);
				holder.chattext.setText(spannableString);
			} 
			catch (Exception e) {
				DebugUtil.error("", "", e);
			}
		}
		else if(message.getType() == ChatMessage.TYPE_AUDIO) {
			holder.audioTime.setVisibility(View.VISIBLE);
			holder.chatStat.setVisibility(View.VISIBLE);
			holder.chatto_voice_text.setVisibility(View.VISIBLE);
			holder.chatting_audio_linerlayout.setVisibility(View.VISIBLE);
			
			holder.audioTime.setText(String.valueOf(message.audioTime) + "''");

			holder.chatStat.setImageResource(message.direction == ChatMessage.MESSAGE_FROM ? 
				R.drawable.chatfrom_voice_playing : R.drawable.chatto_voice_playing);
			holder.chatStat.setTag(holder);
			holder.chatStat.setOnClickListener(audioClickListener);
			holder.chatStat.setOnLongClickListener(onLongClickListener);

			holder.chatto_voice_text.setText(getAudioText(message.audioTime));
			holder.chatto_voice_text.setTag(holder);
			holder.chatto_voice_text.setClickable(true);
			holder.chatto_voice_text.setOnClickListener(audioClickListener);
			holder.chatto_voice_text.setOnLongClickListener(onLongClickListener);
			
			holder.chatting_audio_linerlayout.setTag(holder);
			holder.chatting_audio_linerlayout.setOnClickListener(audioClickListener);
			holder.chatting_audio_linerlayout.setOnLongClickListener(onLongClickListener);
		}
		else if(message.getType() == ChatMessage.TYPE_PIC) {
			holder.chat_content_pic.setVisibility(View.VISIBLE);
			holder.chat_content_pic.setImageBitmap(message.thumbnail);
			
			// @by jichao, MUST set holder for image, otherwize onclick image func was failed
			holder.chat_content_pic.setTag(holder);
			holder.chat_content_pic.setClickable(true);
			holder.chat_content_pic.setOnClickListener(imageChatClickListener);
			holder.chat_content_pic.setOnLongClickListener(onLongClickListener);
			
			// if image is too small, user can also click it
			holder.chatting_pic_frame.setVisibility(View.VISIBLE);
			holder.chatting_pic_frame.setTag(holder);
			holder.chatting_pic_frame.setClickable(true);
			holder.chatting_pic_frame.setOnClickListener(imageChatClickListener);
			holder.chatting_pic_frame.setOnLongClickListener(onLongClickListener);
		}
		
		if(message.sendState == ChatMessage.TO_SENDING) {
			playUploadAnimation(message.msgid, holder.stateIV);
			
			if(message.type == ChatMessage.TYPE_PIC) {
				ImageUpDownLoadAnimaHanlder uphandler = new ImageUpDownLoadAnimaHanlder();
				uphandler.holder = holder;
				uphandler.setProgress(0);
				SendFileHandler sender = MediaManager.getMediaPlayManager().getSendFileHandler(message.attachPath);
				
				if(sender != null) {
					sender.uploadAnimaHandler = uphandler;
				}
			}
		}
		else if(message.sendState == ChatMessage.TO_SEND_ERR) {
			stopUploadTimer(message.msgid);
			holder.stateIV.setImageResource(R.drawable.msg_state_fail);
		}
		else {
			stopUploadTimer(message.msgid);
			holder.stateIV.setVisibility(View.GONE);
		}
		
		holder.stateIV.setVisibility(message.sendState == ChatMessage.TO_SEND_SUCC ? View.GONE : View.VISIBLE);
		
		if(message.userFaceUrl == null && message.uid != null) {
			ContactModule sender = CommonUtil.getCurrUserId().equals(message.uid) ?
				DataStore.getInstance().getOwnerUser() :
				DataStore.getInstance().queryContactByContactId(message.uid);
			
			if(sender != null) {
				message.userFaceUrl = sender.faceurl;
			}
		}
		
		Bitmap userface = ImageStore.getInstance().getImage(message.userFaceUrl);
		
		if(userface != null) {
			userface = ImageUtils.toRoundCorner(userface, 10);
			holder.imageHeadIcon.setImageBitmap(userface);
		} 
		else {
			holder.imageHeadIcon.setImageResource(R.drawable.app_panel_friendcard_icon);
		}
	}

	View.OnClickListener audioClickListener = new View.OnClickListener() {
		public void onClick(View v) {
			ChatAdapterViewHolder holder = (ChatAdapterViewHolder) v.getTag();
			
			if(holder != null && holder.chat.type == ChatMessage.TYPE_AUDIO) {
				MediaManager mm = MediaManager.getMediaPlayManager();
				View view = holder.chat.direction == ChatMessage.MESSAGE_TO ?
					holder.chatting_audio_linerlayout :
					holder.chat_from_audio_linerlayout;
				ImageView iv = holder.chat.direction == ChatMessage.MESSAGE_TO ?
					holder.chatStat :
					holder.chat_from_state;
				
				playAudioAnimation(mm, iv, view, holder.chat.direction);
				
				String path = holder.chat.attachPath;
				try {
					mm.playAudioRealTime(new FileInputStream(path));
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	};
	
	
	private String getAudioText(int audioTime) {
		// 最大支持录音60秒
		audioTime = audioTime > 60 ? 60 : audioTime;
		StringBuilder sb = new StringBuilder(" ");
		
		for(int i = 1; i < audioTime && i < 3; i++) {
			sb.append("  ");
		}
		
		for(int i = 3; i < audioTime && i < 10; i++) {
			sb.append(" ");
		}
		
		if(audioTime - 10 > 0) {
			int time0 = (audioTime - 10) / 10;
			
			for(int i = 0; i < time0; i++) {
				sb.append(" ");
			}
		}
		
		return sb.toString();
	}
	
	/** 
     * 播放上传图标动画 
     */  
    private void playUploadAnimation(String msgid, final ImageView imageView) {  
        //定时器检查播放状态     
    	stopUploadTimer(msgid);  
        
        //将要关闭的语音图片归位  
        if(uploadAnimationHandler != null)  
        {  
            Message msg = new Message();  
            msg.what = 4;
            msg.arg1 = -1; // @by jichao, put an invalide value, don't remove
            uploadAnimationHandler.sendMessage(msg);  
        }  
          
        uploadAnimationHandler = new UploadAnimationHandler(imageView);
        UploadTimerTask uploadTimerTask = new UploadTimerTask();
        uploadTimerTasks.put(msgid, uploadTimerTask);
        //调用频率为600毫秒一次  
        mTimer.schedule(uploadTimerTask, 0, 600);  
    }
	
    private class UploadTimerTask extends TimerTask {
        // 记录语音动画图片  
    	int index = 0;
    	
        @Override      
        public void run() { 
        	index = (index + 1) % 4;  
            Message msg = new Message();
            msg.what = index;  
            uploadAnimationHandler.sendMessage(msg);  
        }  
    }
    
	/** 
	 * 停止 
	 */  
	private void stopUploadTimer(String msgid) {
		UploadTimerTask uploadTimerTask = uploadTimerTasks.remove(msgid);
		
		if (uploadTimerTask != null) {    
			uploadTimerTask.cancel();    
			uploadTimerTask = null;    
		}     
	} 
    
	/** 
     * 播放语音图标动画 
     */  
    private void playAudioAnimation(MediaManager mm, final ImageView imageView, final View view, int direction) {  
        //定时器检查播放状态     
        stopTimer();  
        
        //将要关闭的语音图片归位  
        if(audioAnimationHandler != null)  
        {  
            Message msg = new Message();  
            msg.what = 4;
            msg.arg1 = -1; // @by jichao, put an invalide value, don't remove
            audioAnimationHandler.sendMessage(msg);  
        }  
          
        audioAnimationHandler = new AudioAnimationHandler(imageView, view);  
        mTimerTask = new AudioPlayTimerTask();
        mTimerTask.direction = direction;
        mTimerTask.mediaManager = mm;
        //调用频率为400毫秒一次  
        mTimer.schedule(mTimerTask, 0, 400);  
    }
	
    private class AudioPlayTimerTask extends TimerTask {
    	public int direction;
        public boolean hasPlayed = false;
        public MediaManager mediaManager;
        // 记录语音动画图片  
    	int index = 1;
    	
        @Override      
        public void run() { 
            if(mediaManager.isAudioPlaying()) {    
                hasPlayed = true;  
                index = (index + 1) % 3;  
                Message msg = new Message();
                msg.arg1 = direction;
                msg.what = index;  
                audioAnimationHandler.sendMessage(msg);  
            }
            else {  
                //当播放完时  
                Message msg = new Message();  
                msg.what = 4;
                msg.arg1 = direction;
                audioAnimationHandler.sendMessage(msg);  
                
                //播放完毕时需要关闭Timer等  
                if(hasPlayed) { 
                    stopTimer();  
                }  
            }  
        }  
    }
    
	/** 
	 * 停止 
	 */  
	private void stopTimer() {    
		if (mTimerTask != null) {    
			mTimerTask.cancel();    
			mTimerTask = null;    
		}     
	} 
}