package com.wejoy.util;

import java.lang.reflect.Field; 
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.wejoy.R;
import com.wejoy.module.Emotion;
import com.wejoy.store.DataStore;
import com.wejoy.store.EmotionStore;
import com.wejoy.ui.AppUtils; 

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ImageSpan;
import android.util.Log;


/**
 * @ClassName: EmotionalFaceUtil 
 * @Description: 颜艺
 * @author liuzhao
 * @date 2013-6-21 下午3:15:46 
 */
public class EmotionalFaceUtil {
	/**
	 * 对spanableString进行正则判断，如果符合要求，则以表情图片代替
	 * @param context
	 * @param spannableString
	 * @param patten
	 * @param start
	 * @throws SecurityException
	 * @throws NoSuchFieldException
	 * @throws NumberFormatException
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 */
	
	private static final int DEFAULT_FACE_HEIGHT = 50;
	private static final int DEFAULT_FACE_WIDTH = 50;
	
	public static Bitmap bmselfAdjust(Bitmap bm) {
		int h = bm.getHeight();
		int w = bm.getWidth();
		
		if (h > DEFAULT_FACE_HEIGHT || w > DEFAULT_FACE_WIDTH) {
			Matrix matrix = new Matrix();
			
			float scale = AppUtils.screenHeight < 500 ? 0.5f : 1f;
			
			matrix.postScale((float) scale * DEFAULT_FACE_HEIGHT / h, (float) scale * DEFAULT_FACE_WIDTH / w);
			Bitmap resizeBmp = Bitmap.createBitmap(bm, 0, 0, w, h, matrix, true);
			return resizeBmp;
		}
		else {
			return bm;
		}
	}
	
    public static void dealExpression(Context context, SpannableString spannableString, 
    	Pattern patten, int start) throws SecurityException, NoSuchFieldException, NumberFormatException, 
    	IllegalArgumentException, IllegalAccessException 
    {
    	Matcher matcher = patten.matcher(spannableString);
    	System.out.println(matcher.toString());
        
    	while (matcher.find()) {
            String keytmp = matcher.group();
            
            if (matcher.start() < start) {
                continue;
            }
            
            Emotion emotion = EmotionStore.getInstance().getEmotionByChineseStr(keytmp);
            
            if(emotion != null) {
            	String key = emotion.filename;
            	Field field = R.drawable.class.getDeclaredField(key);
            	int resId = Integer.parseInt(field.get(null).toString());		//通过上面匹配得到的字符串来生成图片资源id
            	
            	if (resId != 0) {
            		Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), resId);
            		Bitmap bm = bmselfAdjust(bitmap);
            		ImageSpan imageSpan = new ImageSpan(bm);				//通过图片资源id来得到bitmap，用一个ImageSpan来包装
            		int end = matcher.start() + keytmp.length();					//计算该图片名字的长度，也就是要替换的字符串的长度
            		spannableString.setSpan(imageSpan, matcher.start(), end, Spannable.SPAN_INCLUSIVE_EXCLUSIVE);	//将该图片替换字符串中规定的位置中
            		if (end < spannableString.length()) {						//如果整个字符串还未验证完，则继续。。
            			dealExpression(context,spannableString,  patten, end);
            		}
            		break;
            	}
            }
        }
    }
    
    /**
     * 得到一个SpanableString对象，通过传入的字符串,并进行正则判断
     * @param context
     * @param str
     * @return
     */
    public static SpannableString getExpressionString(Context context, String str) {
    	// @by Jichao, check if str is null
    	if(str == null) {
    		str = "";
    	}
    	 
    	SpannableString spannableString = new SpannableString(str);
        
        try {
            dealExpression(context,spannableString, getSinaPatten(), 0);
        } catch (Exception e) {
            Log.e("dealExpression", e.getMessage());
        }
        
        return spannableString;
    }
    
    private static Pattern sinaPatten = null;
    
    private static Pattern getSinaPatten() {
    	if(sinaPatten == null) {
    		sinaPatten = Pattern.compile(EmotionStore.getInstance().getEmotionZhengze(), Pattern.CASE_INSENSITIVE);
    	}
    	
    	return sinaPatten;
    }
}