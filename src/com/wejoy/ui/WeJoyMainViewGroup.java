package com.wejoy.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.View.MeasureSpec;
import android.widget.Scroller;
/**
 * 支持滑动的主界面
 * @author WeJoy Group
 *
 */
public class WeJoyMainViewGroup extends ViewGroup {
	public static int NEXT_SCREEN = 1;
	public static int PRE_SCREEN = -1;
	public static int SCREEN_SYS_MENU_VIEW = 0;
	public static int SCREEN_CONV_LIST_VIEW = 1;
	public static int SCREEN_CONTACT_LIST_VIEW = 2;
	
	private Scroller scroller;
	// 滑动距离
	private int distance;
	private int duration = 500;
	public int curScreen;
	private Context context;
	private VelocityTracker mVelocityTracker;
	private float mLastMotionX;
	private float mLastMotionY;
	private static final int SNAP_VELOCITY = 50;
	/*
	 * 最左侧menu菜单是否打开
	 */
	public static boolean isMenuOpned = false;

	public WeJoyMainViewGroup(Context context) {
		super(context, null);
	}

	public WeJoyMainViewGroup(Context context, AttributeSet attrs) {
		super(context, attrs);
		this.context = context;
		scroller = new Scroller(context);
	}

	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		int childLeft = 0;
		final int childCount = getChildCount();
		for (int i = 0; i < childCount; i++) {
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

		// @by jichao, go to the default page
		curScreen = 1;
		scrollTo(curScreen * width, 0);
	}
	
	public void setDistance(int distance) {
		this.distance = distance;
	}

	@Override
	public void computeScroll() {
		if (scroller.computeScrollOffset()) {
			scrollTo(scroller.getCurrX(), scroller.getCurrY());
			postInvalidate();
		}
	}

	public void showMenu() {
		isMenuOpned = true;
		scroller.startScroll(getScrollX(), 0, -distance, 0, duration);
		invalidate();
	}

	public void scrollTo(int direction) {
		if(direction == NEXT_SCREEN) {
			scrollToScreen(curScreen + 1);
		}
		else if(direction == PRE_SCREEN) {
			scrollToScreen(curScreen - 1);
		}
		
		// do nothing
	}
	
	public void scrollToScreen(int whichScreen) {	
		// get the valid layout page
		whichScreen = Math.max(0, Math.min(whichScreen, getChildCount() - 1));
		int scrollX = getScrollX();
		
		if (getScrollX() != (whichScreen * getWidth())) {
			final int delta = whichScreen * getWidth() - getScrollX();
			scroller.startScroll(getScrollX(), 0, delta, 0, Math.abs(delta) * 1);//持续滚动时间 以毫秒为单位
			curScreen = whichScreen;
			invalidate(); // Redraw the layout
		}
	}
	
	public void clickMenuSlideToLeft() {
		isMenuOpned = false;
		scroller.startScroll(getScrollX(), 0, distance - getWidth(), 0, duration);
		invalidate();
	}

	public void clickMenuToRight() {
		isMenuOpned = false;
		scroller.startScroll(getScrollX(), 0, getWidth(), 0, duration);
		invalidate();
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		final int action = event.getAction();
		final float x = event.getX();
		final float y = event.getY();
		
		if(curScreen == SCREEN_CONV_LIST_VIEW) {
			return false;
		}
		
		switch (action) {
		case MotionEvent.ACTION_DOWN:
			if (mVelocityTracker == null) {
				mVelocityTracker = VelocityTracker.obtain();
				mVelocityTracker.addMovement(event);
			}

			if (!scroller.isFinished()) {
				scroller.abortAnimation();
			}
			
			mLastMotionX = x;
			mLastMotionY = y;
			break;
		case MotionEvent.ACTION_MOVE:
			int deltaX = (int) (mLastMotionX - x);
			int deltaY = (int) (mLastMotionY - y);
			
			if (IsCanMove(deltaX)) {
				if (mVelocityTracker != null) {
					mVelocityTracker.addMovement(event);
				}

				mLastMotionX = x;
				scrollBy(deltaX, 0);
			}
			break;
		case MotionEvent.ACTION_UP:
			int velocityX = 0;
			
			if (mVelocityTracker != null) {
				mVelocityTracker.addMovement(event);
				//��ʼ�����ʵĵ�λ
				mVelocityTracker.computeCurrentVelocity(1000);
				// �õ�X�᷽����ָ�ƶ��ٶ�
				velocityX = (int) mVelocityTracker.getXVelocity();
			}

			if (velocityX > SNAP_VELOCITY && curScreen > 0) {
				// Fling enough to move left
				scrollToScreen(curScreen - 1);
			} 
			else if (velocityX < -SNAP_VELOCITY
					&& curScreen < getChildCount() - 1) {
				// Fling enough to move right
				scrollToScreen(curScreen + 1);
			}
			else{
				snapToDestination();
			}
			
			if (mVelocityTracker != null) {
				mVelocityTracker.recycle();
				mVelocityTracker = null;
			}
			
			break;
		}
		return true;
	}
	
	public void snapToDestination() {
		final int screenWidth = getWidth();
		final int destScreen = (getScrollX() + (screenWidth/2))/ screenWidth;
		scrollToScreen(destScreen);
	}
	
	private boolean IsCanMove(int deltaX) {
		if (getScrollX() < 10 && deltaX <= 0) {
			return false;
		}

		if (getScrollX() >= (getChildCount() - 1) * getWidth() - 10 && deltaX >= 0) {
			return false;
		}
		return true;
	}
}
