package com.wejoy.ui;

import java.io.FileInputStream; 

import com.wejoy.R;
import com.wejoy.module.ChatMessage; 
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.Window;
import android.widget.ImageView; 
import android.widget.ProgressBar;

public class FullSizeImageActivity extends Activity {
	private ChatMessage chat = null;
	private int SUCCESS = 200;
	private Bitmap fullSizeImage = null;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
    	this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.full_image_view);
        
        Intent intent = getIntent();
		Bundle bl= intent.getExtras();
		String chatmessage = bl.getString("chatmessage");
		chat = ChatMessage.parseJson(chatmessage);
		ProgressBar progressBar = (ProgressBar) findViewById(R.id.fullImageViewcircleProgressBar);
		progressBar.setVisibility(View.VISIBLE);
		
		new Thread(new Runnable() {
			public void run() {
				
				try {
					BitmapFactory.Options options = new BitmapFactory.Options();
				    options.inJustDecodeBounds = false;
				    options.inSampleSize = 2;
					fullSizeImage = BitmapFactory.decodeStream(new FileInputStream(chat.attachPath), null, options);
					Message msg = new Message();  
                    msg.what = SUCCESS;
                    mHandler.sendMessage(msg); 
				} 
				catch (Exception e) {
					Message msg = new Message();  
                    mHandler.sendMessage(msg); 
				}
			}
		}).start();
	}

	private Handler mHandler = new Handler() {
		public void handleMessage(Message msg) {
			ImageView fullImageView = (ImageView) findViewById(R.id.full_image_imageview);
			ProgressBar progressBar = (ProgressBar) findViewById(R.id.fullImageViewcircleProgressBar);
			ImageView failedImage = (ImageView) findViewById(R.id.ic_full_image_failed);
			progressBar.setVisibility(View.GONE);
			
			 if(msg.what == SUCCESS) {
				 fullImageView.setImageBitmap(fullSizeImage);
				 fullImageView.setVisibility(View.VISIBLE);
				 failedImage.setVisibility(View.GONE);
			 }
			 else {  
	            failedImage.setVisibility(View.VISIBLE);
	            fullImageView.setVisibility(View.GONE);
			 }	            
		}
	};
}
