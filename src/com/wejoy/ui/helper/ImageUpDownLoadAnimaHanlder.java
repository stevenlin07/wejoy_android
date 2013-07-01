package com.wejoy.ui.helper;

import com.wejoy.ui.adapter.ChatAdapterViewHolder;

import android.os.Handler;
import android.os.Message;
import android.view.View;
/**
 * 
 * @author WeJoy Group
 *
 */
public class ImageUpDownLoadAnimaHanlder extends Handler {
	public ChatAdapterViewHolder holder;
	private boolean upload = true;
	
	public ImageUpDownLoadAnimaHanlder() {
	}
	
	public ImageUpDownLoadAnimaHanlder(boolean upload) {
		this.upload = upload;
	}
	
	public void setProgress(int progress) {
		Message msg = new Message();
		msg.what = progress;
		
		if(upload) {
			holder.loadingCircleView.setVisibility(View.VISIBLE);
		}
		else {
			holder.downloadingCircleView.setVisibility(View.VISIBLE);
		}
		
		super.sendMessage(msg);
	}
	
	public void setMax(int max) {
		if(holder != null) {
			if(upload) {
				holder.chat_content_pic.setMax(max);
				holder.loadingCircleView.setMax(max);
			}
			else {
				holder.chat_from_content_pic.setMax(max);
				holder.downloadingCircleView.setMax(max);
			}
		}
	}
	
	public void handleMessage(android.os.Message msg) {
		if(holder != null) {
			if(upload) {
				holder.chat_content_pic.setProgress(msg.what, true);
				holder.loadingCircleView.setProgress(msg.what);
				
				if (msg.what == holder.loadingCircleView.getMax()) {
					holder.loadingCircleView.setVisibility(View.GONE);
				}
			}
			else {
				holder.chat_from_content_pic.setProgress(msg.what, true);
				holder.downloadingCircleView.setProgress(msg.what);
				
				if (msg.what == holder.downloadingCircleView.getMax()) {
					holder.downloadingCircleView.setVisibility(View.GONE);
				}
			}
		}
	};
}
