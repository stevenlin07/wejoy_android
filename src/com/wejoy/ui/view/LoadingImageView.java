package com.wejoy.ui.view;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.ImageView;
import android.widget.ProgressBar;

/**
 * 带加载进度条的ImageView
 * 
 * @author LiangJunYing email:junying.hao@163.com
 * 
 */
public class LoadingImageView extends ImageView {
	/*** 背景图片 */
	private Drawable bgDrawable;
	/** 前景图片 */
	private Drawable fgDrawable;
	/** 是否显示加载进度条 */
	private boolean isShowProgress;

	private Resources rsc;
	private int progress = 0;
	private int max = 100;
	private int progressHeight;
	private int progressLeft;
	private int progressTop;
	private int progressRight;
	private int progressBottom;

	public LoadingImageView(Context context) {
		this(context, null);
	}

	public LoadingImageView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public LoadingImageView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		rsc = getResources();
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		if (bgDrawable == null) {
			return;
		}
		progressLeft = getMeasuredWidth() / 2
				- (fgDrawable.getIntrinsicWidth() / 2);
		progressTop = getMeasuredHeight() / 2
				- (fgDrawable.getIntrinsicHeight() / 2);
		progressRight = getMeasuredWidth() / 2
				+ (fgDrawable.getIntrinsicWidth() / 2);
		progressBottom = getMeasuredHeight() / 2
				+ (fgDrawable.getIntrinsicHeight() / 2);
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		return super.onTouchEvent(event);
	}

	/**
	 * 设置背景图片
	 * 
	 * @param drawableRes
	 */
	public void setBgDrawableRes(int drawableRes) {
		bgDrawable = rsc.getDrawable(drawableRes);
		invalidate();
	}

	public void setFgDrawableRes(int drawableRes) {
		fgDrawable = rsc.getDrawable(drawableRes);
		invalidate();
	}

	public void setMax(int max) {
		if (max < 0)
			max = 0;
		if (progress > max)
			progress = max;
		this.max = max;
	}

	public void setProgress(int progress, boolean flag) {
		isShowProgress = flag;
		if (progress >= 0 & progress <= max) {
			this.progress = progress;
			invalidate();
		}
	}

	@Override
	protected void onDraw(Canvas canvas) {
		if (bgDrawable != null) {
			bgDrawable.setBounds(progressLeft, progressTop, progressRight,
					progressBottom);
			bgDrawable.draw(canvas);
		}
		super.onDraw(canvas);
		if (bgDrawable != null && isShowProgress) {
			bgDrawable.setBounds(progressLeft, progressTop, progressRight,
					progressBottom);
			bgDrawable.draw(canvas);
		}
		if (fgDrawable != null && isShowProgress) {
			// 根据进度计算图片显示的高的比
			progressHeight = fgDrawable.getIntrinsicHeight() * progress / max;
			// 关键代码，设置图片的显示区域
			canvas.clipRect(progressLeft, progressBottom - progressHeight,
					progressRight, progressBottom);
			fgDrawable.setBounds(progressLeft, progressTop, progressRight,
					progressBottom);
			fgDrawable.draw(canvas);
		}
	}

}
