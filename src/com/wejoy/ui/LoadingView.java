package com.wejoy.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.widget.ImageView;
/**
 * 
 * @author WeJoy Group
 *
 */
public class LoadingView extends ImageView implements Runnable
{
	private boolean isStop = false;
	
	private int[] imageIds;
	private int index = 0;
	private int length = 1;
	
	public LoadingView(Context context)
	{
		this(context, null);
	}
	
	public LoadingView(Context context, AttributeSet attrs)
	{
		super(context, attrs);
	}
	
	public void setImageIds(int[] imageId)
	{
		this.imageIds = imageId;
		if(imageIds != null && imageIds.length > 0)
		{
			length = imageIds.length;
		}
	}
	
	@Override
	protected void onDetachedFromWindow()
	{
		// TODO Auto-generated method stub
		super.onDetachedFromWindow();
		isStop = true;
	}
	
	@Override
	protected void onDraw(Canvas canvas)
	{
		// TODO Auto-generated method stub
		super.onDraw(canvas);
		if(imageIds != null && imageIds.length > 0)
		{
			this.setImageResource(imageIds[index]);
		}
	}
	
	@Override
	public void run()
	{
		while(!isStop)
		{
			//计算要显示图片的位置
			index = ++index % length;
			postInvalidate();
			try
			{
				Thread.sleep(400);
			}
			catch (InterruptedException e)
			{
				e.printStackTrace();
			}
		}
	}
	
	//启动图片轮换线程
	public void startAnim()
	{
		new Thread(this).start();
	}
}

