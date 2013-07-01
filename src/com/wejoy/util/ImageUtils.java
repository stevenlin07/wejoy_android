package com.wejoy.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;

import com.weibo.login.BitmapHelper;
import com.weibo.login.Weibo;
import com.wejoy.R;
import com.wejoy.store.ImageStore;
/**
 * 
 * @author WeJoy Group
 *
 */
public class ImageUtils {
	private static void revitionImageSizeHD( String picfile, int size , int quality ) throws IOException {
		if (size <= 0) {
			throw new IllegalArgumentException("size must be greater than 0!");
		}

		if (!CommonUtil.doesExisted(picfile)) {
			throw new FileNotFoundException(picfile == null ? "null" : picfile );
		}
		
		if (!BitmapHelper.verifyBitmap(picfile)) {
			throw new IOException("");
		}
		
		int photoSizesOrg = 2 * size;
		FileInputStream input = new FileInputStream(picfile);
		final BitmapFactory.Options opts = new BitmapFactory.Options();
		opts.inJustDecodeBounds = true;
		BitmapFactory.decodeStream(input, null, opts);
		
		try {
			input.close();
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		int rate = 0;
		for (int i = 0;; i++) {
			if ((opts.outWidth >> i <= photoSizesOrg && (opts.outHeight >> i <= photoSizesOrg))) {
				rate = i;
				break;
			}
		}
		
		opts.inSampleSize = (int) Math.pow(2, rate);
		opts.inJustDecodeBounds = false;
		
		Bitmap temp = safeDecodeBimtapFile( picfile, opts );
		
		if (temp == null) {
			throw new IOException("Bitmap decode error!");
		}
		
		CommonUtil.deleteDependon(picfile);
		CommonUtil.makesureFileExist(picfile);
		
		int org = temp.getWidth()>temp.getHeight()?temp.getWidth():temp.getHeight();
		float rateOutPut = size/(float)org;
		
		if(rateOutPut < 1){
			Bitmap outputBitmap;
			
			while(true) {
				try {
					outputBitmap = Bitmap.createBitmap(((int)(temp.getWidth()*rateOutPut)),
						((int)(temp.getHeight()*rateOutPut)), Bitmap.Config.ARGB_8888);
					break;
				} catch (OutOfMemoryError e) {
					System.gc();
					rateOutPut = (float)(rateOutPut * 0.8); 
				}
			}
			
			if(outputBitmap == null){
				temp.recycle();
			}
			
			Canvas canvas = new Canvas(outputBitmap);
			Matrix matrix = new Matrix();
			matrix.setScale(rateOutPut, rateOutPut);
			canvas.drawBitmap(temp, matrix, new Paint());
			temp.recycle();
			temp = outputBitmap;
		}
		
		final FileOutputStream output = new FileOutputStream(picfile);
		
		if (opts != null && opts.outMimeType != null
				&& opts.outMimeType.contains("png")) {
			temp.compress(Bitmap.CompressFormat.PNG, quality, output);
		} else {
			temp.compress(Bitmap.CompressFormat.JPEG, quality, output);
		}
		
		try {
			output.close();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		temp.recycle();
	}
	
	private static void revitionImageSize( String picfile, int size ,int quality) throws IOException {
		if (size <= 0) {
			throw new IllegalArgumentException("size must be greater than 0!");
		}
		
		if (!CommonUtil.doesExisted(picfile)) {
			throw new FileNotFoundException(picfile == null ? "null" : picfile );
		}
		
		if (!BitmapHelper.verifyBitmap(picfile)) {
			throw new IOException("");
		}
		
		FileInputStream input = new FileInputStream(picfile);
		final BitmapFactory.Options opts = new BitmapFactory.Options();
		opts.inJustDecodeBounds = true;
		BitmapFactory.decodeStream(input, null, opts);
		try {
			input.close();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		int rate = 0;
		for (int i = 0;; i++) {
			if ((opts.outWidth >> i <= size) && (opts.outHeight >> i <= size)) {
				rate = i;
				break;
			}
		}
		
		opts.inSampleSize = (int) Math.pow(2, rate);
		opts.inJustDecodeBounds = false;
		
		Bitmap temp = safeDecodeBimtapFile( picfile, opts );
		
		if (temp == null) {
			throw new IOException("Bitmap decode error!");
		}
		
		CommonUtil.deleteDependon(picfile);
		CommonUtil.makesureFileExist(picfile);
		final FileOutputStream output = new FileOutputStream(picfile);
		if (opts != null && opts.outMimeType != null
				&& opts.outMimeType.contains("png")) {
			temp.compress(Bitmap.CompressFormat.PNG, quality, output);
		} else {
			temp.compress(Bitmap.CompressFormat.JPEG, quality, output);
		}
		try {
			output.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		temp.recycle();
	}
	
	/**
	 * 调整图片大小
	 * @param picfile
	 * @return
	 */
	public static boolean revisionPostImageSize( String picfile) {
		try {
			if(Weibo.isWifi){
				revitionImageSizeHD(picfile, 1600 , 75);
			}
			else{
				revitionImageSize( picfile, 1024 , 75);
			}
			
			return true;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}    
	
	/**
	 * 如果加载时遇到OutOfMemoryError,则将图片加载尺寸缩小一半并重新加载
	 * @param bmpFile
	 * @param opts 注意：opts.inSampleSize 可能会被改变
	 * @return
	 */
	private static Bitmap safeDecodeBimtapFile( String bmpFile, BitmapFactory.Options opts ) {
		BitmapFactory.Options optsTmp = opts;
		if ( optsTmp == null ) {
			optsTmp = new BitmapFactory.Options();
			optsTmp.inSampleSize = 1;
		}
		
		Bitmap bmp = null;
		FileInputStream input = null;
		
		final int MAX_TRIAL = 5;
		for( int i = 0; i < MAX_TRIAL; ++i ) {
			try {
				input = new FileInputStream( bmpFile );
				bmp = BitmapFactory.decodeStream(input, null, opts);
				try {
					input.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
				break;
			}
			catch( OutOfMemoryError e ) {
				e.printStackTrace();
				optsTmp.inSampleSize *= 2;
				try {
					input.close();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}
			catch (FileNotFoundException e) {
				break;
			}
		}
		
		return bmp;
	}
	
	/** 
     * 从二进制数据中得到位图 
     *  
     * @param byteArray 
     *            二进制数据 
     * @return 位图 
     */  
	public static Bitmap byteToBitmap(byte[] byteArray) {
        if (byteArray != null && byteArray.length != 0) {  
            return BitmapFactory  
                    .decodeByteArray(byteArray, 0, byteArray.length);  
        } else {  
            return null;  
        }  
    }
	
	/** 
     * 从二进制数据中得到Drawable对象 
     *  
     * @param byteArray 
     *            二进制数据 
     * @return Drawable对象 
     */  
    public static Drawable byteToDrawable(byte[] byteArray) {  
        ByteArrayInputStream ins = new ByteArrayInputStream(byteArray);  
        return Drawable.createFromStream(ins, null);  
    }  
  
    /** 
     * 把位图转换称二进制数据 
     *  
     * @param bm 
     *            位图 
     * @return 二进制数据 
     */  
    public static byte[] Bitmap2Bytes(Bitmap bm) {  
        ByteArrayOutputStream baos = new ByteArrayOutputStream();  
        bm.compress(Bitmap.CompressFormat.PNG, 100, baos);  
        return baos.toByteArray();  
    }  
  
    /** 
     * 把Drawable对象转换称位图 
     *  
     * @param drawable 
     *            Drawable对象 
     * @return 位图 
     */  
    public static Bitmap drawableToBitmap(Drawable drawable) {  
  
        Bitmap bitmap = Bitmap  
                .createBitmap(  
                        drawable.getIntrinsicWidth(),  
                        drawable.getIntrinsicHeight(),  
                        drawable.getOpacity() != PixelFormat.OPAQUE ? Bitmap.Config.ARGB_8888  
                                : Bitmap.Config.RGB_565);  
        Canvas canvas = new Canvas(bitmap);  
        drawable.setBounds(0, 0, drawable.getIntrinsicWidth(),  
                drawable.getIntrinsicHeight());  
        drawable.draw(canvas);  
        return bitmap;  
    }  
  
    /** 
     * 图片去色,返回灰度图片 
     *  
     * @param bmpOriginal 
     *            传入的图片 
     * @return 去色后的图片 
     */  
    public static Bitmap toGrayscale(Bitmap bmpOriginal) {  
        int width, height;  
        height = bmpOriginal.getHeight();  
        width = bmpOriginal.getWidth();  
  
        Bitmap bmpGrayscale = Bitmap.createBitmap(width, height,  
                Bitmap.Config.RGB_565);  
        Canvas c = new Canvas(bmpGrayscale);  
        Paint paint = new Paint();  
        ColorMatrix cm = new ColorMatrix();  
        cm.setSaturation(0);  
        ColorMatrixColorFilter f = new ColorMatrixColorFilter(cm);  
        paint.setColorFilter(f);  
        c.drawBitmap(bmpOriginal, 0, 0, paint);  
        return bmpGrayscale;  
    }  
  
    /** 
     * 去色同时加圆角 
     *  
     * @param bmpOriginal 
     *            原图 
     * @param pixels 
     *            圆角弧度 
     * @return 修改后的图片 
     */  
    public static Bitmap toGrayscale(Bitmap bmpOriginal, int pixels) {  
        return toRoundCorner(toGrayscale(bmpOriginal), pixels);  
    }  
  
    public static Bitmap toRoundCorner(Bitmap bitmap) {
    	return toRoundCorner(bitmap, 20);
    }
    
    /** 
     * 把位图变成圆角位图 
     *  
     * @param bitmap 
     *            需要修改的位图 
     * @param pixels 
     *            圆角的弧度 
     * @return 圆角位图 
     */  
    public static Bitmap toRoundCorner(Bitmap bitmap, int pixels) {  
    	if(bitmap == null) {
    		return null;
    	}
    	
        Bitmap output = Bitmap.createBitmap(bitmap.getWidth(),  
                bitmap.getHeight(), Config.ARGB_8888);  
        Canvas canvas = new Canvas(output);  
  
        final int color = 0xff424242;  
        final Paint paint = new Paint();  
        final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());  
        final RectF rectF = new RectF(rect);  
        final float roundPx = pixels;  
  
        paint.setAntiAlias(true);  
        canvas.drawARGB(0, 0, 0, 0);  
        paint.setColor(color);  
        canvas.drawRoundRect(rectF, roundPx, roundPx, paint);  
  
        paint.setXfermode(new PorterDuffXfermode(Mode.SRC_IN));  
        canvas.drawBitmap(bitmap, rect, rect, paint);  
  
        return output;  
    }
  
    /** 
     * 将BitampDrawable转换成圆角的BitampDrawable 
     *  
     * @param bitmapDrawable 
     *            原生BitampDrawable对象 
     * @param pixels 
     *            圆角弧度 
     * @return 圆角BitampDrawable对象 
     */  
    public static BitmapDrawable toRoundCorner(BitmapDrawable bitmapDrawable,  
            int pixels) {  
        Bitmap bitmap = bitmapDrawable.getBitmap();  
        bitmapDrawable = new BitmapDrawable(toRoundCorner(bitmap, pixels));  
        return bitmapDrawable;  
    }  
  
    /** 
     * 图片水印生成的方法 
     *  
     * @param src 
     *            源图片位图 
     * @param watermark 
     *            水印图片位图 
     * @return 返回一个加了水印的图片 
     */  
    public static Bitmap createBitmap(Bitmap src, Bitmap watermark) {  
        if (src == null)  
            return null;  
        int w = src.getWidth();  
        int h = src.getHeight();  
        int ww = watermark.getWidth();  
        int wh = watermark.getHeight();  
        Bitmap newb = Bitmap.createBitmap(w, h, Config.ARGB_8888);// 创建一个新的和SRC长度宽度一样的位图  
        Canvas cv = new Canvas(newb);// 初始化画布  
        cv.drawBitmap(src, 0, 0, null);// 在 0，0坐标开始画入src  
        cv.drawBitmap(watermark, w - ww + 5, h - wh + 5, null);// 在src的右下角画入水印  
        cv.save(Canvas.ALL_SAVE_FLAG);// 保存，用来保存Canvas的状态。save之后，可以调用Canvas的平移、放缩、旋转、错切、裁剪等操作。  
        cv.restore();// 存储，用来恢复Canvas之前保存的状态。防止save后对Canvas执行的操作对后续的绘制有影响。  
        return newb;  
    }
    
    /** 
     * 图片水印生成的方法 
     *  
     * @param src 
     *            源图片位图 
     * @param watermark 
     *            水印图片位图 
     * @return 返回一个加了水印的图片 
     */  
    public static Bitmap createCombinedBitmap(List<Bitmap> src, int w, int h) {  
        if (src == null) {
            return null;  
        }
        
        Bitmap newb = Bitmap.createBitmap(w, h, Config.ARGB_8888);// 创建一个新的和SRC长度宽度一样的位图  
        Canvas cv = new Canvas(newb);// 初始化画布
        int midw = w / 2;
        int midh = h / 2;
        
        if(src.size() == 1) {
        	return src.get(0);
        }
        else if(src.size() >= 3) {
        	for(int i = 0; i < src.size() && i < 4; i ++) {
        		Bitmap bm = src.get(i);
        		Bitmap bm0 = Bitmap.createScaledBitmap(bm, midw - 1, midh - 1, false);
        		
        		if(i == 0) {
        			cv.drawBitmap(bm0, 0, 0, null);// 在 0，0坐标开始画入src
        		}
        		else if(i == 1) {
        			cv.drawBitmap(bm0, midw + 1, 0, null);
        		}
        		else if(i == 2) {
        			cv.drawBitmap(bm0, 0, midh + 1, null);
        		}
        		else {
        			cv.drawBitmap(bm0, midw + 1, midh + 1, null);
        		}
        	}
        }
        else if(src.size() == 2) {
        	for(int i = 0; i < src.size() && i < 2; i ++) {
        		Bitmap bm = src.get(i);
        		Bitmap bm0 = Bitmap.createScaledBitmap(bm, midw - 1, midh - 1, false);
        		
        		if(i == 0) {
        			cv.drawBitmap(bm0, 0, midh / 2, null);// 在 0，0坐标开始画入src
        		}
        		else if(i == 1) {
        			cv.drawBitmap(bm0, midw + 1, midh / 2, null);
        		}
        	}
        }
        
        cv.save(Canvas.ALL_SAVE_FLAG);// 保存，用来保存Canvas的状态。save之后，可以调用Canvas的平移、放缩、旋转、错切、裁剪等操作。  
        cv.restore();// 存储，用来恢复Canvas之前保存的状态。防止save后对Canvas执行的操作对后续的绘制有影响。  
        return newb;  
    }
    
    public static Bitmap drawShadow(Bitmap map) {
    	return drawShadow(map, 0.2f);
    }
    
   /**
    *  为指定图片增加阴影
     * 
     * @param map　图片
     * @param radius　阴影的半径
     * @return
     */
    public static Bitmap drawShadow(Bitmap map, float radius) {
        if (map == null)
            return null;

        BlurMaskFilter blurFilter = new BlurMaskFilter(radius, BlurMaskFilter.Blur.NORMAL);
        Paint shadowPaint = new Paint();
        shadowPaint.setMaskFilter(blurFilter);

        int[] offsetXY = new int[2];
        Bitmap shadowImage = map.extractAlpha(shadowPaint, offsetXY);
        shadowImage = shadowImage.copy(Config.ARGB_8888, true);
        Canvas c = new Canvas(shadowImage);
        c.drawBitmap(map, 0, 0, null);
        return shadowImage;
    }
    
    public static void getDefaultGroupConvFaceImage(final ImageView iv, String groupFaceUrl, int w, int h,
    	ImageStore.OnImageLoadListener imageLoadListener) 
    {
    	if(groupFaceUrl == null || iv == null || imageLoadListener == null) {
    		return;
    	}
    	
    	String[] urls = groupFaceUrl.split(",");
		List<Bitmap> bms = new ArrayList<Bitmap>(); 
		
		for(String url : urls) {
			try {
				Bitmap bm0;
				bm0 = ImageStore.getInstance().getImageByUrl(url, imageLoadListener);
				bm0 = ImageUtils.toRoundCorner(bm0, 10);
				bms.add(bm0);
			} 
			catch (Exception e) {
				DebugUtil.error("ImageUtils", "getDefaultGroupConvFaceImage", e);
			}
		}
		
		Bitmap groupface = ImageUtils.createCombinedBitmap(bms, w, h);
		imageLoadListener.onImageLoad(iv, groupface);
		
		String key = MD5.getMD5(groupFaceUrl);
		
		try {
			ImageStore.getInstance().setImage(key, groupface);
		} 
		catch (Exception e) {
			// ignore
		}
    }
    
    //计算图片的缩放值
    public static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
        	final int heightRatio = Math.round((float) height/ (float) reqHeight);
            final int widthRatio = Math.round((float) width / (float) reqWidth);
            inSampleSize = heightRatio < widthRatio ? heightRatio : widthRatio;
        }
            
        return inSampleSize;
    }
    
    public static Bitmap getThumbnail(Bitmap raw) {
    	if(raw == null) {
    		return null;
    	}
    	
    	BitmapFactory.Options options = new BitmapFactory.Options();
    	byte[] rawdata = CommonUtil.bitmap2Bytes(raw);
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(rawdata, 0, rawdata.length, options);
        // 一般手机的分辨率为 480*800 ，所以我们压缩后图片期望的宽带定为480，高度设为800
        int reqHeight = 800;
        int reqWidth = 480;
        //在内存中创建bitmap对象，这个对象按照缩放大小创建的
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
        options.inJustDecodeBounds = false;
        Bitmap bitmap = BitmapFactory.decodeByteArray(rawdata, 0, rawdata.length, options);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 40, baos);
        Bitmap bm = ImageUtils.byteToBitmap(baos.toByteArray());
        return bm;
    }
}
