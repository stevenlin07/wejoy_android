package com.wejoy.ui.helper;

import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.ImageView;

import com.wejoy.R;
import com.wejoy.module.ChatMessage;
/**
 * 
 * @author WeJoy Group
 *
 */
public class AudioAnimationHandler extends Handler {  
    ImageView imageView;
    View view;
    boolean isFrom;
    
    public AudioAnimationHandler(ImageView imageView, View view)  
    {  
        this.imageView = imageView;
        this.view = view;
    }
    
    @Override  
    public void handleMessage(Message msg) {
        super.handleMessage(msg);
        
        // @by jichao, 如果既不是from也不是to，就不要赋值，可能会使用上次赋给的值
        if(msg.arg1 == ChatMessage.MESSAGE_FROM || msg.arg1 == ChatMessage.MESSAGE_TO) {
        	isFrom = msg.arg1 == ChatMessage.MESSAGE_FROM ? true : false;
        }
        
        //根据msg.what来替换图片，达到动画效果  
        switch (msg.what) {  
            case 0 :  
                imageView.setImageResource(isFrom? R.drawable.chatfrom_voice_playing_f1:R.drawable.chatto_voice_playing_f1);
				view.setBackgroundResource(isFrom ? R.drawable.chattingfrom_bg_focused : R.drawable.chattingto_bg_focused);
                break;  
            case 1 :  
                imageView.setImageResource(isFrom? R.drawable.chatfrom_voice_playing_f2:R.drawable.chatto_voice_playing_f2);
                view.setBackgroundResource(isFrom ? R.drawable.chattingfrom_bg_focused : R.drawable.chattingto_bg_focused);
                break;  
            case 2 :  
                imageView.setImageResource(isFrom? R.drawable.chatfrom_voice_playing_f3:R.drawable.chatto_voice_playing_f3);
                view.setBackgroundResource(isFrom ? R.drawable.chattingfrom_bg_focused : R.drawable.chattingto_bg_focused);
                break;
            case 3 :  
                imageView.setImageResource(isFrom? R.drawable.chatfrom_voice_playing_f3:R.drawable.chatto_voice_playing_f3);
                view.setBackgroundResource(isFrom ? R.drawable.chattingfrom_bg_focused : R.drawable.chattingto_bg_focused);
                break;
            default:
            	imageView.setImageResource(isFrom ? R.drawable.chatfrom_voice_playing : R.drawable.chatto_voice_playing);
            	view.setBackgroundResource(isFrom ? R.drawable.chattingfrom_bg_normal : R.drawable.chattingto_bg_normal);
            	break;
        }  
    }  
}
