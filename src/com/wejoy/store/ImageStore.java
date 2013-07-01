package com.wejoy.store;
 
import java.io.File;
import java.io.FileInputStream; 
import java.io.FileOutputStream;
import java.io.IOException; 
import java.lang.ref.SoftReference; 
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

import com.wejoy.net.WeJoyHTTPManager;
import com.wejoy.util.DebugUtil; 
import com.wejoy.util.MD5;
import com.wejoy.util.SDCardUtil;

import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory; 
import android.os.Environment; 
import android.widget.ImageView;
/**
 * 图片存储
 * @author WeJoy Group
 *
 */
public class ImageStore implements StoreConstant {
	/** 用来异步加载请求*/
	private ExecutorService imageThreadPool = Executors.newCachedThreadPool();
	private HashMap<String, SoftReference<Bitmap>> imageCache = new HashMap<String, SoftReference<Bitmap>>();
	private LinkedBlockingQueue<ImageLoadTask> imageLoadTasks = new LinkedBlockingQueue<ImageLoadTask>();
	private static final String IMAGE_STORE = STORE_ROOT + File.separator + "temp" + File.separator +
		"images" + File.separator;
	private Thread loadImageT = null;
	private static ImageStore instance = null;
	
	public static ImageStore getInstance() {
		if(instance == null) {
			instance = new ImageStore();
		}
		
		return instance;
	}
	
	private ImageStore() {
		loadImageT = new Thread(new Runnable() {
			@Override
			public void run() {
				
				ImageLoadTask task = null;
				
				while(true) {
					try {
						while((task = imageLoadTasks.take()) != null) {
							String debuginfo = new StringBuilder("imageUrl=").append(task.imageUrl).toString();
							DebugUtil.debug("SyncImageLoader.loadImageT", debuginfo);
							loadImage(task.iv, task.imageUrl, task.listener, task.options);
						}
					} 
					catch (InterruptedException e) {
						DebugUtil.warn("SyncImageLoader.loadImageT", e);
					}
				}
			}

		});
		
		loadImageT.start();
	}
	
	public interface OnImageLoadListener {
		public void onImageLoad(ImageView iv, Bitmap bm);
		public void onError(ImageView iv, String msg);
	}
	
	public void reLoadImage(final ImageView iv, final String mImageUrl, final OnImageLoadListener listener, 
		ImageStoreOptions options)
	{
		ImageLoadTask task = new ImageLoadTask();
		task.imageUrl = mImageUrl;
		task.iv = iv;
		task.listener = listener;
		task.options = options;
		
		try {
			imageLoadTasks.put(task);
		} 
		catch (InterruptedException e) {
			DebugUtil.warn("SyncImageLoader。reLoadImage", e);
		}
	}

	private class ImageLoadTask {
		public ImageView iv;
		public String imageUrl;
		public OnImageLoadListener listener;
		public ImageStoreOptions options;
	}
	
	public Bitmap getImageByUrl(String url, final OnImageLoadListener listener) throws Exception {
		Bitmap bm = getImage(url);
		
		if(bm != null) {
			return bm;
		}
		else {
			bm = loadImageFromUrl(url, listener);
			return bm;
		}
	}
	
	public Bitmap getImage(String key) {
		if(key == null) {
			return null;
		}
		
		key = MD5.getMD5(key);
		Bitmap bm = null;
		
		try {
			if (imageCache.containsKey(key)) { 
				SoftReference<Bitmap> softReference = imageCache.get(key);  
				bm = softReference.get();
			}
			
			if(bm == null && SDCardUtil.isSDCardReadable()) {
				File imageFile = new File(Environment.getExternalStorageDirectory() + File.separator + IMAGE_STORE + key);
				
				if(imageFile.exists()){
					FileInputStream fis = new FileInputStream(imageFile);
					bm = BitmapFactory.decodeStream(fis);
				}
				
				imageCache.put(key, new SoftReference<Bitmap>(bm));
			}
		}
		catch(Exception e) {
			DebugUtil.error("", "", e);
		}
		
		return bm;
	}
	
	public void setImage(String key, Bitmap bm) throws Exception {
		if(SDCardUtil.isSDCardWritable()) {
			File file = SDCardUtil.createFolder(Environment.getExternalStorageDirectory() + File.separator + IMAGE_STORE);
			
			if (file != null && file.exists()) {
				try {
					File imageFile = new File(Environment.getExternalStorageDirectory() + File.separator + IMAGE_STORE + key);
					imageFile.createNewFile();
					FileOutputStream fos = new FileOutputStream(imageFile);
					/*
					 * Hint to the compressor, 0-100. 0 meaning compress for small size, 100 meaning compress for max quality. 
					 * Some formats, like PNG which is lossless, will ignore the quality setting
					 */
					bm.compress(CompressFormat.PNG, 100, fos);
					fos.flush();
					fos.close();
				} catch (IOException e) {
					DebugUtil.error("SyncImageLoader", "write to sdcard failed", e);
				}
			}
		}

		imageCache.put(key, new SoftReference<Bitmap>(bm));
	}
	
	public void loadImage(final ImageView iv, final String mImageUrl, final OnImageLoadListener listener, ImageStoreOptions options) {
		if(mImageUrl == null) {
			return;
		}
		
		String urlkey = MD5.getMD5(mImageUrl);
		
		if (imageCache.containsKey(urlkey)) { 
            SoftReference<Bitmap> softReference = imageCache.get(urlkey);  
            final Bitmap d = softReference.get();
            
            if (d != null) {  
            	imageThreadPool.submit(new Runnable() {
    				@Override
    				public void run() {
   						listener.onImageLoad(iv, d);
    				}
    			});
                return;  
            }  
        }  
		
		try {
			final Bitmap d = loadImageFromUrl(mImageUrl, listener);
			
			if(d != null) {
                imageCache.put(urlkey, new SoftReference<Bitmap>(d));
                imageThreadPool.submit(new Runnable() {
    				@Override
    				public void run() {
   						listener.onImageLoad(iv, d);
    				}
    			});
			}
		}
		catch (Exception e) {
			imageThreadPool.submit(new Runnable() {
				public void run() {
					listener.onError(iv, "加载图片失败");
				}
			});
		
			DebugUtil.error("SyncImageLoader", "loadImage failed", e);
		}
	}

	public static class ImageStoreOptions {
		public boolean isRoundRectangle;
		public int roundPix;
	}
	
	public Bitmap loadImageFromUrl(String url, final OnImageLoadListener listener) throws Exception {
		if(SDCardUtil.isSDCardReadable()) {
			File imageFile = new File(Environment.getExternalStorageDirectory() + File.separator + IMAGE_STORE + MD5.getMD5(url));
			
			if(imageFile.exists()){
				FileInputStream fis = new FileInputStream(imageFile);
				return BitmapFactory.decodeStream(fis);
			}
		}
		
		byte[] image = WeJoyHTTPManager.getImage(url);
		Bitmap bitmap = BitmapFactory.decodeByteArray(image, 0, image.length);
		setImage(MD5.getMD5(url), bitmap);
		
		if(!SDCardUtil.isSDCardWritable()) {
			imageThreadPool.submit(new Runnable() {
				public void run() {
					listener.onError(null, "存储空间不足，可能影响微聚的使用");
				}
			});
		}
//		else {
//			File file = SDCardUtil.createFolder(Environment.getExternalStorageDirectory() + File.separator + IMAGE_STORE);
//			
//			if (file != null && file.exists()) {
//				try {
//					File imageFile = new File(Environment.getExternalStorageDirectory() +File.separator + IMAGE_STORE + MD5.getMD5(url));
//					imageFile.createNewFile();
//					FileOutputStream fos = new FileOutputStream(imageFile);
//					/*
//					 * Hint to the compressor, 0-100. 0 meaning compress for small size, 100 meaning compress for max quality. 
//					 * Some formats, like PNG which is lossless, will ignore the quality setting
//					 */
//					bitmap.compress(CompressFormat.PNG, 100, fos);
//					fos.flush();
//					fos.close();
//				} catch (IOException e) {
//					DebugUtil.error("SyncImageLoader", "write to sdcard failed", e);
//				}
//			}
//		}

		return bitmap;
	}
	
	public static String getImageStorePath() {
		return Environment.getExternalStorageDirectory() + File.separator + IMAGE_STORE;
	}
}
