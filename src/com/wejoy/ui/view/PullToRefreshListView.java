package com.wejoy.ui.view;

import java.util.List;

import com.wejoy.R;
import com.wejoy.module.ChatMessage;
import com.wejoy.module.ConvertModule;
import com.wejoy.store.DataStore;
import com.wejoy.store.SqliteStore;
import com.wejoy.ui.MainChatActivity;
import com.wejoy.ui.adapter.ChattingAdapter;

import android.content.Context;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
/**
 * 下拉刷新控件
 * @author WeJoy Group
 * @version 1.0
 * @created 2012-3-21
 */
public class PullToRefreshListView extends ListView implements OnScrollListener {  
    // 下拉刷新标志   
    private final static int PULL_To_REFRESH = 0; 
    // 松开刷新标志   
    private final static int RELEASE_To_REFRESH = 1; 
    // 正在刷新标志   
    private final static int REFRESHING = 2;  
    // 刷新完成标志   
    private final static int DONE = 3;  
  
    private LayoutInflater inflater;  
  
    private LinearLayout headView;  
    private ProgressBar progressBar;  
  
    // 用于保证startY的值在一个完整的touch事件中只被记录一次   
    private boolean isRecored;  
  
    private int headContentWidth;  
    private int headContentHeight;  
    private int headContentOriginalTopPadding;
  
    private int startY;  
    private int firstItemIndex;  
    private int currentScrollState;
    private int state;  
    public ChattingAdapter chatHistoryAdapter;
  
    public ConvertModule currentConversation;
    
    public PullToRefreshListView(Context context, AttributeSet attrs) {  
        super(context, attrs);  
        init(context);  
    }  
    
    public PullToRefreshListView(Context context, AttributeSet attrs, int defStyle) {  
        super(context, attrs, defStyle);  
        init(context);  
    }  
  
    private void init(Context context) {   
        inflater = LayoutInflater.from(context);  
        headView = (LinearLayout) inflater.inflate(R.layout.pull_to_refresh_head, null);  
  
        progressBar = (ProgressBar) headView.findViewById(R.id.head_progressBar);  
        progressBar.setMinimumWidth(50);  
        progressBar.setMinimumHeight(50);
        
        headContentOriginalTopPadding = headView.getPaddingTop();  
        
        measureView(headView);  
        headContentHeight = headView.getMeasuredHeight();  
        headContentWidth = headView.getMeasuredWidth(); 
        
        headView.setPadding(headView.getPaddingLeft(), -1 * headContentHeight, 
        	headView.getPaddingRight(), headView.getPaddingBottom());  
        headView.invalidate();  
        addHeaderView(headView);        
        setOnScrollListener(this); 
    }
  
    public void onScroll(AbsListView view, int firstVisiableItem, int visibleItemCount,  int totalItemCount) {  
        firstItemIndex = firstVisiableItem;  
    }  
  
    public void onScrollStateChanged(AbsListView view, int scrollState) {  
    	currentScrollState = scrollState;
    }  
  
    public boolean onTouchEvent(MotionEvent event) {  
        switch (event.getAction()) {  
        case MotionEvent.ACTION_DOWN:
            if (firstItemIndex == 0 && !isRecored) {  
                startY = (int) event.getY();  
                isRecored = true;  
            }
            
            break;  
        case MotionEvent.ACTION_CANCEL://失去焦点&取消动作
        case MotionEvent.ACTION_UP:
            if (state != REFRESHING) {  
            	// 当前-抬起-ACTION_UP：DONE什么都不做
                if (state == DONE) {  
                }  
                // 当前-抬起-ACTION_UP：PULL_To_REFRESH-->DONE-由下拉刷新状态到刷新完成状态
                else if (state == PULL_To_REFRESH) {  
                	
                    state = DONE;  
                    changeHeaderViewByState();                      
                }  
                else if (state == RELEASE_To_REFRESH) {  
                	//System.out.println("当前-抬起-ACTION_UP：RELEASE_To_REFRESH-->REFRESHING-由松开刷新状态，到刷新完成状态");
                    state = REFRESHING;  
                    changeHeaderViewByState();  
               		loadNewData();
                }  
            }  
  
            isRecored = false;  
            break;  
        case MotionEvent.ACTION_MOVE:
        	int tempY = (int) event.getY(); 
        	//System.out.println("当前-滑动-ACTION_MOVE Y："+tempY);
        	if (!isRecored && firstItemIndex == 0) {  
        		//System.out.println("当前-滑动-记录拖拽时的位置 Y："+tempY);
        		isRecored = true;  
        		startY = tempY;  
        	}

        	if(state != REFRESHING && isRecored && firstItemIndex == 0) {  
        		if (tempY - startY >= headContentHeight+20 && currentScrollState == SCROLL_STATE_TOUCH_SCROLL) {  
        			state = RELEASE_To_REFRESH;  
        			changeHeaderViewByState();
        			//System.out.println("当前-滑动-PULL_To_REFRESH--》RELEASE_To_REFRESH-由done或者下拉刷新状态转变到松开刷新");
        		}
        		// 上推到顶了   
        		else if (tempY - startY <= 0) {  
        			state = DONE;  
        			changeHeaderViewByState();   
        			//System.out.println("当前-滑动-PULL_To_REFRESH--》DONE-由Done或者下拉刷新状态转变到done状态");
        		}  
        		// done状态下   
        		else if (state == DONE) {  
        			if (tempY - startY > 0) {  
        				state = PULL_To_REFRESH;  
        				changeHeaderViewByState();
        			}  
        		}  
                
                // 更新headView的size   
                if (state == PULL_To_REFRESH) { 
                	int topPadding = (int)((-1 * headContentHeight + (tempY - startY)));
                	headView.setPadding(headView.getPaddingLeft(), topPadding, headView.getPaddingRight(), headView.getPaddingBottom());   
                    headView.invalidate();  
                    //System.out.println("当前-下拉刷新PULL_To_REFRESH-TopPad："+topPadding);
                }  
  
                // 更新headView的paddingTop   
                if (state == RELEASE_To_REFRESH) {  
                	int topPadding = (int)((tempY - startY - headContentHeight));
                	headView.setPadding(headView.getPaddingLeft(), topPadding, headView.getPaddingRight(), headView.getPaddingBottom());    
                    headView.invalidate();  
                    //System.out.println("当前-释放刷新RELEASE_To_REFRESH-TopPad："+topPadding);
                }  
            }  
            break;  
        } 
        
        return super.onTouchEvent(event);  
    }  
  
	public void loadNewData() {
		List<ChatMessage> chatlist0 = null;
		
		if(chatHistoryAdapter.chatMessages == null || chatHistoryAdapter.chatMessages.isEmpty()) {
			chatlist0 = DataStore.getInstance().queryChatMessage(currentConversation.convid, 
					-1, MainChatActivity.PAGE_COUNT);
		}
		else {
			ChatMessage chat0 = null;
			
			// @by jichao, 标记时间的系统消息是每次显示时临时生成的，没有落地，因此这类消息的生成时间基本就是当前时间，
			// 计算下一页时需要过滤掉
			for(int i = 0; i < chatHistoryAdapter.chatMessages.size(); i++) {
				chat0 = chatHistoryAdapter.chatMessages.get(i);
				
				if(chat0.direction == ChatMessage.MESSAGE_TIME_INFO) {
					chat0 = null;
					continue;
				}
				else {
					break;
				}
			}
			
			if(chat0 != null) {
				chatlist0 = DataStore.getInstance().queryChatMessage(currentConversation.convid, 
						chat0 == null ? -1 : chat0.created_at, MainChatActivity.PAGE_COUNT);
			}
		}
		
		if(chatlist0 == null || chatlist0.isEmpty()) {
			state = DONE;  
			changeHeaderViewByState();
		}
		else {
			for(int i = 0; i < chatlist0.size(); i++) {
				ChatMessage chat = chatlist0.get(i);
				chatHistoryAdapter.chatMessages.add(i, chat);
			}
			
			chatHistoryAdapter.notifyDataSetChanged();
			setSelection(chatlist0.size());
			state = DONE;  
			changeHeaderViewByState();
		}
	}
    
    // 当状态改变时候，调用该方法，以更新界面   
    private void changeHeaderViewByState() {  
        switch (state) {  
        case RELEASE_To_REFRESH:  
            progressBar.setVisibility(View.VISIBLE);  
            break;
        case PULL_To_REFRESH:
            progressBar.setVisibility(View.VISIBLE);  
            break;  
        case REFRESHING:   
        	//System.out.println("刷新REFRESHING-TopPad："+headContentOriginalTopPadding);
        	headView.setPadding(headView.getPaddingLeft(), headContentOriginalTopPadding, 
        		headView.getPaddingRight(), headView.getPaddingBottom());   
            headView.invalidate();  
  
            progressBar.setVisibility(View.VISIBLE);
            break;  
        case DONE:  
        	//System.out.println("完成DONE-TopPad："+(-1 * headContentHeight));
        	headView.setPadding(headView.getPaddingLeft(), -1 * headContentHeight, headView.getPaddingRight(), headView.getPaddingBottom());  
            headView.invalidate();  
            progressBar.setVisibility(View.GONE);  
            break;  
        }  
    }  
  
    // 计算headView的width及height值  
    private void measureView(View child) {  
        ViewGroup.LayoutParams p = child.getLayoutParams();  
        if (p == null) {  
            p = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT,  
                    ViewGroup.LayoutParams.WRAP_CONTENT);  
        }  
        int childWidthSpec = ViewGroup.getChildMeasureSpec(0, 0 + 0, p.width);  
        int lpHeight = p.height;  
        int childHeightSpec;  
        if (lpHeight > 0) {  
            childHeightSpec = MeasureSpec.makeMeasureSpec(lpHeight,  
                    MeasureSpec.EXACTLY);  
        } else {  
            childHeightSpec = MeasureSpec.makeMeasureSpec(0,  
                    MeasureSpec.UNSPECIFIED);  
        }  
        child.measure(childWidthSpec, childHeightSpec);  
    }
}
