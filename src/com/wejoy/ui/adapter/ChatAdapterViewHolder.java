package com.wejoy.ui.adapter;

import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.wejoy.R;
import com.wejoy.module.ChatMessage;
import com.wejoy.ui.view.LoadingCircleView;
import com.wejoy.ui.view.LoadingImageView;

public class ChatAdapterViewHolder {
	/**
	 * chat_system_layout
	 */
	public View chat_system_layout;
	public TextView system_content;
	/**
	 * chat_from_layout
	 */
	public View chat_from_layout;
	public ImageView chat_from_face_iv;
	public TextView chat_from_content_tv;
	public View chat_from_pic_frame;
	public LoadingImageView chat_from_content_pic;
	public LoadingCircleView downloadingCircleView;
	
	public View chat_from_audio_linerlayout;
	public ImageView chat_from_state;
	public TextView chatfrom_voice_text;
	public TextView chatfrom_voice_time;
	
	/**
	 * chat_to_layout
	 */
	public View chat_to_layout;
	public View chatting_pic_frame;
	public View chatting_audio_linerlayout;
	public TextView chattext;
	public TextView chatto_voice_text;
	public TextView audioTime;
	public ImageView chatStat;
	public LoadingImageView chat_content_pic;
	public LoadingCircleView loadingCircleView;
	public ImageView imageHeadIcon;
	public ProgressBar imagePB;
	public ImageView stateIV;
	public ChatMessage chat;
	
	public void populateView(View view, ChatMessage message) {
		chat = message;
		
		/**
		 * chat_system_layout
		 */
		chat_system_layout = (View) view.findViewById(R.id.chat_system_layout);
		system_content = (TextView) view.findViewById(R.id.system_content);
		/**
		 * chat_from_layout
		 */
		chat_from_layout = (View) view.findViewById(R.id.chat_from_layout);
		chat_from_face_iv = (ImageView) view.findViewById(R.id.chat_from_face_iv);
		chat_from_content_tv = (TextView) view.findViewById(R.id.chat_from_content_tv);
		chat_from_pic_frame = (View) view.findViewById(R.id.chat_from_pic_frame);
		chat_from_content_pic = (LoadingImageView) view.findViewById(R.id.chat_from_content_pic);
		downloadingCircleView = (LoadingCircleView) view.findViewById(R.id.downloading_cirle_view);
		
		chat_from_audio_linerlayout = (View) view.findViewById(R.id.chat_from_audio_linerlayout);
		chat_from_state = (ImageView) view.findViewById(R.id.chat_from_state);
		chatfrom_voice_text = (TextView) view.findViewById(R.id.chatfrom_voice_text);
		chatfrom_voice_time = (TextView) view.findViewById(R.id.chatfrom_voice_time);
		/**
		 * chat to
		 */
		chat_to_layout = (View) view.findViewById(R.id.chat_to_layout);
		chattext = (TextView) view.findViewById(R.id.chatting_content_itv);
		chatting_pic_frame = (View) view.findViewById(R.id.chatting_pic_frame);
		chatting_audio_linerlayout = (View) view.findViewById(R.id.chatting_audio_linerlayout);
		chatto_voice_text = (TextView) view.findViewById(R.id.chatto_voice_text);
		chatStat = (ImageView) view.findViewById(R.id.chatting_state);
		audioTime = (TextView) view.findViewById(R.id.chatto_voice_time);
		
		chat_content_pic = (LoadingImageView) view.findViewById(R.id.chatting_content_pic);
		loadingCircleView = (LoadingCircleView) view.findViewById(R.id.loading_cirle_view);
		
		imageHeadIcon = (ImageView) view.findViewById(R.id.chatting_face_iv);
		imagePB = (ProgressBar) view.findViewById(R.id.uploading_pb);
		stateIV = (ImageView) view.findViewById(R.id.chatting_state_iv);
	}
	
	public void setAllViewHide() {
		/**
		 * chat_system_layout
		 */
		chat_system_layout.setVisibility(View.GONE);
		system_content.setVisibility(View.GONE);
		/**
		 * chat_from_layout
		 */
		chat_from_layout.setVisibility(View.GONE);
		chat_from_face_iv.setVisibility(View.GONE);
		chat_from_content_tv.setVisibility(View.GONE);
		chat_from_pic_frame.setVisibility(View.GONE);
		chat_from_content_pic.setVisibility(View.GONE);
		chat_from_audio_linerlayout.setVisibility(View.GONE);
		chat_from_state.setVisibility(View.GONE);
		chatfrom_voice_text.setVisibility(View.GONE);
		chatfrom_voice_time.setVisibility(View.GONE);
		/**
		 * chat_to_layout
		 */
		chattext.setVisibility(View.GONE);
		audioTime.setVisibility(View.GONE);
		chatStat.setVisibility(View.GONE);
		chat_content_pic.setVisibility(View.GONE);
		imagePB.setVisibility(View.GONE);
		stateIV.setVisibility(View.GONE);
		chatting_pic_frame.setVisibility(View.GONE);
		chatting_audio_linerlayout.setVisibility(View.GONE);
		chatto_voice_text.setVisibility(View.GONE);
		chat_to_layout.setVisibility(View.GONE);
		/**
		 * others
		 */
		loadingCircleView.setVisibility(View.GONE);
		downloadingCircleView.setVisibility(View.GONE);
	}
}
