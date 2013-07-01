package greendroid.widget;


import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.Scroller;

/**
 * 左右滑动切换屏幕控件
 * @author Yao.GUET date: 2011-05-04
 * @modify liux (http://my.oschina.net/liux)
 */
public class ScrollLayout extends ViewGroup {
	private static final String TAG = "ScrollLayout";
	private Scroller mScroller;
	private VelocityTracker mVelocityTracker;
	private int mCurScreen;
	private int mDefaultScreen = 0;
	private static final int TOUCH_STATE_REST = 0;
	private static final int TOUCH_STATE_SCROLLING = 1;
	private static final int SNAP_VELOCITY = 600;
	private int mTouchState = TOUCH_STATE_REST;
	private int mTouchSlop;
	private float mLastMotionX;
	private float mLastMotionY;
    private OnViewChangeListener mOnViewChangeListener;
    
    //add
    private int distance;// 滑动距离
    private int miLength;
    private int duration = 500;
    private CloseAnimation closeAnimation;
    
    public static boolean isMenuOpned = false;// 菜单是否打开

    /**
     * 设置是否可左右滑动
     * @author liux
     */
    private boolean isScroll = true;
    public void setIsScroll(boolean b) {
    	this.isScroll = b;
    }
    
	public ScrollLayout(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public ScrollLayout(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		mScroller = new Scroller(context);
		mCurScreen = mDefaultScreen;
		mTouchSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
	}

	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		//miLength
		//menu_view.measure(0, 0);
		View childView0 = getChildAt(0);
		childView0.layout(-distance, 0, 0,  childView0.getMeasuredHeight());
		int childLeft = 0;
		final int childCount = getChildCount();
		for (int i = 1; i < childCount; i++) {
			final View childView = getChildAt(i);
			if (childView.getVisibility() != View.GONE) {
				final int childWidth = childView.getMeasuredWidth();
				childView.layout(childLeft, 0, childLeft + childWidth,
						childView.getMeasuredHeight());
				childLeft += childWidth;
			}
		}
	
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		//Log.e(TAG, "onMeasure");
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		final int width = MeasureSpec.getSize(widthMeasureSpec);
		final int widthMode = MeasureSpec.getMode(widthMeasureSpec);
		if (widthMode != MeasureSpec.EXACTLY) {
			throw new IllegalStateException(
					"ScrollLayout only canmCurScreen run at EXACTLY mode!");
		}
		final int heightMode = MeasureSpec.getMode(heightMeasureSpec);
		if (heightMode != MeasureSpec.EXACTLY) {
			throw new IllegalStateException(
					"ScrollLayout only can run at EXACTLY mode!");
		}

		// The children are given the same width and height as the scrollLayout
		final int count = getChildCount();
		for (int i = 0; i < count; i++) {
			getChildAt(i).measure(widthMeasureSpec, heightMeasureSpec);
		}
		// Log.e(TAG, "moving to screen "+mCurScreen);
		scrollTo(mCurScreen * width, 0);
	}

	/**
	 * According to the position of current layout scroll to the destination
	 * page.
	 */
	public void snapToDestination() {
		final int screenWidth = getWidth();
		final int destScreen = (getScrollX() + screenWidth / 2) / screenWidth;
		snapToScreen(destScreen);
	}

	public void snapToScreen(int whichScreen) {
		//是否可滑动
		if(!isScroll) {
			this.setToScreen(whichScreen);
			return;
		}
		
		scrollToScreen(whichScreen);
	}
	
	public void setDistance(int distance) {
		this.distance = distance;
	}
	
	public void setMilength(int miLength){
		this.miLength = miLength;
	}
	
	public void scrollToScreen(int whichScreen) {		
		// get the valid layout page
		whichScreen = Math.max(0, Math.min(whichScreen, getChildCount() - 1));
		if (getScrollX() != (whichScreen * getWidth())) {
			final int delta = whichScreen * getWidth() - getScrollX();
			mScroller.startScroll(getScrollX(), 0, delta, 0,
					Math.abs(delta) * 1);//持续滚动时间 以毫秒为单位
			mCurScreen = whichScreen;
			invalidate(); // Redraw the layout
            
			if (mOnViewChangeListener != null)
            {
            	mOnViewChangeListener.OnViewChange(mCurScreen);
            }
		}
	}
	
	public void setToScreen(int whichScreen) {
		whichScreen = Math.max(0, Math.min(whichScreen, getChildCount() - 1));
		mCurScreen = whichScreen;
		scrollTo(whichScreen * getWidth(), 0);
		
        if (mOnViewChangeListener != null)
        {
        	mOnViewChangeListener.OnViewChange(mCurScreen);
        }
	}

	public int getCurScreen() {
		return mCurScreen;
	}

	@Override
	public void computeScroll() {
		if (mScroller.computeScrollOffset()) {
			scrollTo(mScroller.getCurrX(), mScroller.getCurrY());
			postInvalidate();
			if (closeAnimation != null)
				closeAnimation.closeMenuAnimation();
		}
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		//是否可滑动
		if(!isScroll) {
			return false;
		}
		
		if (mVelocityTracker == null) {
			mVelocityTracker = VelocityTracker.obtain();
		}
		mVelocityTracker.addMovement(event);
		final int action = event.getAction();
		final float x = event.getX();
		final float y = event.getY();
		switch (action) {
		case MotionEvent.ACTION_DOWN:
			//Log.e(TAG, "event down!");
			if (!mScroller.isFinished()) {
				mScroller.abortAnimation();
			}
			mLastMotionX = x;
			
			//---------------New Code----------------------
			mLastMotionY = y;
			//---------------------------------------------
			
			break;
		case MotionEvent.ACTION_MOVE:
			int deltaX = (int) (mLastMotionX - x);
			
			//---------------New Code----------------------
			int deltaY = (int) (mLastMotionY - y);
			if(Math.abs(deltaX) < 200 && Math.abs(deltaY) > 10)
				break;
			mLastMotionY = y;
			//-------------------------------------
			
			mLastMotionX = x;
			scrollBy(deltaX, 0);
			break;
		case MotionEvent.ACTION_UP:
			//Log.e(TAG, "event : up");
			// if (mTouchState == TOUCH_STATE_SCROLLING) {
			final VelocityTracker velocityTracker = mVelocityTracker;
			velocityTracker.computeCurrentVelocity(1000);
			int velocityX = (int) velocityTracker.getXVelocity();
			//Log.e(TAG, "velocityX:" + velocityX);
			if (velocityX > SNAP_VELOCITY && mCurScreen > 0) {
				// Fling enough to move left
				//Log.e(TAG, "snap left");
				snapToScreen(mCurScreen - 1);
			} else if (velocityX < -SNAP_VELOCITY
					&& mCurScreen < getChildCount() - 1) {
				// Fling enough to move right
				//Log.e(TAG, "snap right");
				snapToScreen(mCurScreen + 1);
			} else {
				snapToDestination();
			}
			if (mVelocityTracker != null) {
				mVelocityTracker.recycle();
				mVelocityTracker = null;
			}
			// }
			mTouchState = TOUCH_STATE_REST;
			break;
		case MotionEvent.ACTION_CANCEL:
			mTouchState = TOUCH_STATE_REST;
			break;
		}
		return true;
	}

	@Override
	public boolean onInterceptTouchEvent(MotionEvent ev) {
		//Log.e(TAG, "onInterceptTouchEvent-slop:" + mTouchSlop);
		final int action = ev.getAction();
		if ((action == MotionEvent.ACTION_MOVE)
				&& (mTouchState != TOUCH_STATE_REST)) {
			return true;
		}
		final float x = ev.getX();
		final float y = ev.getY();
		switch (action) {
		case MotionEvent.ACTION_MOVE:
			final int xDiff = (int) Math.abs(mLastMotionX - x);
			if (xDiff > mTouchSlop) {
				mTouchState = TOUCH_STATE_SCROLLING;
			}
			break;
		case MotionEvent.ACTION_DOWN:
			mLastMotionX = x;
			mLastMotionY = y;
			mTouchState = mScroller.isFinished() ? TOUCH_STATE_REST
					: TOUCH_STATE_SCROLLING;
			break;
		case MotionEvent.ACTION_CANCEL:
		case MotionEvent.ACTION_UP:
			mTouchState = TOUCH_STATE_REST;
			break;
		}
		return mTouchState != TOUCH_STATE_REST;
	}
	
	/**
	 * 设置屏幕切换监听器
	 * @param listener
	 */
	public void SetOnViewChangeListener(OnViewChangeListener listener)
	{
		mOnViewChangeListener = listener;
	}

	/**
	 * 屏幕切换监听器
	 * @author liux
	 */
	public interface OnViewChangeListener {
		public void OnViewChange(int view);
	}
	
	public void showMenu() {
		isMenuOpned = true;
		mScroller.startScroll(getScrollX(), 0, -distance, 0, duration);
		invalidate();// 刷新
	}
	
	// 关闭菜单（执行自定义动画）
	public void closeMenu() {
		isMenuOpned = false;
		mScroller.startScroll(getScrollX(), 0, distance, 0, duration);

		invalidate();// 刷新
	}

	// 关闭菜单（执行自定义动画）
	void closeMenu_1() {
		isMenuOpned = false;
		mScroller.startScroll(getScrollX(), 0, distance - getWidth(), 0,
				duration);
		invalidate();// 刷新
	}
	
	// 关闭菜单（执行自定义动画）
		void closeMenu_2() {
			isMenuOpned = false;
			mScroller.startScroll(getScrollX(), 0, getWidth(), 0, duration);
			invalidate();// 刷新
		}
		
	
	/***
	 * Menu startScroll(startX, startY, dx, dy)
	 * 
	 * dx=e1的减去e2的x,所以右移为负，左移动为正 dx为移动的距离，如果为正，则标识向左移动|dx|，如果为负，则标识向右移动|dx|
	 */
	void slidingMenu() {

		// 没有超过半屏
		if (getScrollX() > -getWidth() / 2) {
			mScroller.startScroll(getScrollX(), 0, -getScrollX(), 0, duration);
			isMenuOpned = false;
		}
		// 超过半屏
		else if (getScrollX() <= -getWidth() / 2) {
			mScroller.startScroll(getScrollX(), 0, -(distance + getScrollX()),
					0, duration);
			isMenuOpned = true;
		}

		invalidate();// 刷新
		Log.v("jj", "getScrollX()=" + getScrollX());
	}
}

	abstract class CloseAnimation {
		// 点击list item 关闭menu动画
		public void closeMenuAnimation() {
	
		};
	}