package com.wejoy.ui.helper;

import android.os.Handler;
import android.os.Message;
import android.widget.ImageView;
import com.wejoy.R;
/**
 * 
 * @author WeJoy Group
 *
 */
public class UploadAnimationHandler extends Handler {  
    ImageView imageView;
    
    public UploadAnimationHandler(ImageView imageView)  
    {  
        this.imageView = imageView;
    }
    
    @Override  
    public void handleMessage(Message msg) {
        super.handleMessage(msg);
        
        //根据msg.what来替换图片，达到动画效果  
        switch (msg.what) {  
            case 0 :  
                imageView.setImageResource(R.drawable.upload_gif4);
                break;  
            case 1 :  
                imageView.setImageResource(R.drawable.upload_gif1);
                break;  
            case 2 :  
                imageView.setImageResource(R.drawable.upload_gif2);
                break;
            case 3 :  
                imageView.setImageResource(R.drawable.upload_gif3);
                break;
            default:
            	imageView.setImageResource(R.drawable.upload_gif4);
            	break;
        }  
    }  
}
