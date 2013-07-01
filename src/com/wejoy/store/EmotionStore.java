package com.wejoy.store;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;

import com.wejoy.R;
import com.wejoy.module.Emotion;
import com.wejoy.ui.AppUtils; 
import com.wejoy.util.DebugUtil;
import com.wejoy.util.JsonWrapper;
/**
 * 
 * @author WeJoy Group
 *
 */
public class EmotionStore {
	private static String emotionZhengze;
	private static EmotionStore instance;
	private static HashMap<Integer, Emotion> emotionCacheByPos = new HashMap<Integer, Emotion>();
	private static HashMap<String, Emotion> emotionCacheByChinese = new HashMap<String, Emotion>();
	private SqliteStore sqlitedb = null;
	
	public static EmotionStore getInstance() {
		if(instance == null) {
			instance = new EmotionStore();
		}
		
		return instance;
	}
	
	private EmotionStore() {
		sqlitedb = SqliteStore.INSTANCE;
		initEmotion();
	}
	
	private void initEmotion() {
		List<Emotion> emotions = sqlitedb.getEmotions();
		StringBuilder sb = new StringBuilder("(");
		emotionCacheByPos.clear();
		
		for(Emotion emotion : emotions) {
			emotionCacheByPos.put(emotion.pos, emotion);
			emotionCacheByChinese.put(emotion.string_chinese, emotion);
			String squote = Pattern.quote(emotion.string_chinese);
			sb.append(squote).append("|");
		}
	
		sb.replace(sb.length() - 1, sb.length(), ")");
		emotionZhengze = sb.toString();
	}
	
	public String getEmotionZhengze() {
		return emotionZhengze;
	}
	
	public Emotion getEmotionByPos(int pos) {
		return emotionCacheByPos.get(pos);
	}
	
	public Emotion getEmotionByChineseStr(String cs) {
		return emotionCacheByChinese.get(cs);
	}
	
	public static void initEmotionFromResource() {
		String s = null;
		try {
			if(AppUtils.context != null) {
				android.content.res.Resources emotionResources = AppUtils.context.getResources();
				java.io.InputStream emotionDataFile = emotionResources.openRawResource(R.raw.emotion_face_data);
				java.io.InputStreamReader efr = null;
				java.io.BufferedReader ebr = null;
				
				try {
					List<Emotion> list = new ArrayList<Emotion>();
					efr = new java.io.InputStreamReader(emotionDataFile);
					ebr = new java.io.BufferedReader(efr);
					StringBuilder sb = new StringBuilder();
					
					while((s = ebr.readLine()) != null) {
						
						if(s.indexOf("emotionfacelist=") >= 0) {
							Emotion emotion = new Emotion();
							JsonWrapper json = new JsonWrapper(s.substring("emotionfacelist=".length()));
							emotion.filename = json.get("filename");
							emotion.string_chinese = json.get("string_chinese");
							emotion.category = json.get("category");
							emotion.pos = json.getInt("pos");
							list.add(emotion);
							
							emotionCacheByPos.put(emotion.pos, emotion);
							emotionCacheByChinese.put(emotion.string_chinese, emotion);
							sb.append(emotion.string_chinese).append("|");
						}
					}
					
					emotionZhengze = sb.toString();
					SqliteStore.INSTANCE.saveEmotions(list);
				}
				finally {
					if(ebr != null) {
						ebr.close();
					}
					
					if(efr != null) {
						efr.close();
					}
				}
				
				// since every thins ok, don't init once more
				instance = new EmotionStore();
			}
		}
		catch(Exception e) {
			DebugUtil.error(e.toString());
		}
	}
}
