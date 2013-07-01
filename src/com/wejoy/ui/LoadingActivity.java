package com.wejoy.ui;

import com.weibo.login.AccessTokenKeeper;
import com.weibo.login.Oauth2AccessToken;
import com.wejoy.R;
import com.wejoy.module.ChatMessage;
import com.wejoy.store.SqliteStore;
import com.wejoy.ui.view.MyScrollLayout;
import com.wejoy.ui.view.OnViewChangeListener;
import com.wejoy.util.DebugUtil;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
/**
 * 
 * @author WeJoy Group
 *
 */
public class LoadingActivity extends Activity implements OnViewChangeListener, OnClickListener{
    /** Called when the activity is first created. */

	private MyScrollLayout mScrollLayout;	
	private ImageView[] mImageViews;	
	private int mViewCount;	
	private int mCurSel;
	private Button startBtn;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.loading_layout);        
    	
    	// 直接进入conversation list
//    	if(SqliteStore.isSqliteDBReady()) {
    		// test
//    		try {
//    			new SqliteStore().createSchema();
//    		}
//    		catch(Exception e) {
//    			DebugUtil.toast(getBaseContext(), SqliteStore.STORE_ERR);
//    			DebugUtil.error("SqliteStore", "initDatabase failed", e);
//    		}
    		
//    		Intent intent = new Intent();
//			intent.setAction("MainActivity");
//			startActivity(intent);
//    	}
//    	// 进入loading
//    	else {
//    		try {
//    			new SqliteStore().createSchema();
//    		}
//    		catch(Exception e) {
//    			DebugUtil.toast(getBaseContext(), SqliteStore.STORE_ERR);
//    			DebugUtil.error("SqliteStore", "initDatabase failed", e);
//    		}
//    		
//            init();
//    	}
    }
    
    private void init()
    {
    	mScrollLayout = (MyScrollLayout) findViewById(R.id.ScrollLayout); 	
    	LinearLayout linearLayout = (LinearLayout) findViewById(R.id.llayout);   	
    	mViewCount = mScrollLayout.getChildCount();
    	mImageViews = new ImageView[mViewCount];
    	
    	for(int i = 0; i < mViewCount; i++)    	{
    		mImageViews[i] = (ImageView) linearLayout.getChildAt(i);
    		mImageViews[i].setEnabled(true);
    		mImageViews[i].setOnClickListener(this);
    		mImageViews[i].setTag(i);
    	}
    	
    	mCurSel = 0;
    	mImageViews[mCurSel].setEnabled(false);    	
    	mScrollLayout.SetOnViewChangeListener(this);
    	
		startBtn = (Button) findViewById(R.id.loadingFinish_StartBtn);
		startBtn.setOnClickListener(clickListener);
    }

    private View.OnClickListener clickListener = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			if (v.getId() == startBtn.getId()) {
				Oauth2AccessToken weiboToken = AccessTokenKeeper.readAccessToken(v.getContext());
				Intent intent = new Intent();
				
				if(weiboToken.isSessionValid()) {
					intent.setAction("MainActivity");
				}
				else {
					intent.setAction("WeiboLoginActivity");
				}
				
				startActivity(intent);
			}
		}
	};
    
    private void setCurPoint(int index)
    {
    	if(index == mViewCount - 1) {
			startBtn.setVisibility(View.VISIBLE);
		}

    	if (index < 0 || index > mViewCount - 1 || mCurSel == index) {
    		return;
    	}
    	
    	mImageViews[mCurSel].setEnabled(true);
    	mImageViews[index].setEnabled(false);    	
    	mCurSel = index;
    }

    @Override
	public void onViewChange(int view) {
		setCurPoint(view);
	}

	@Override
	public void onClick(View v) {
		int pos = (Integer)(v.getTag());
		setCurPoint(pos);
		mScrollLayout.snapToScreen(pos);
	}
}