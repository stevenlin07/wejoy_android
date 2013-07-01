package com.hzmc.testaudio;

import android.app.Application;
import android.content.Context;
import android.media.AudioManager;

public class TestAudioApp extends Application {

	static Context mContext;

	@Override
	public void onCreate() {
		super.onCreate();
		mContext = this;
	}

	public static Context getContext() {
		// TODO Auto-generated method stub
		return mContext;
	}

	public static void setMediaVolume(int streamType, int flag) {
		AudioManager am = (AudioManager) mContext
				.getSystemService(Context.AUDIO_SERVICE);
		// int StreamType = AudioManager.STREAM_MUSIC;
		int iCurrent = am.getStreamVolume(streamType);
		int ivol = am.getStreamMaxVolume(streamType) * 2 / 3;
		if (iCurrent < ivol) {

			am.setStreamVolume(streamType, ivol, flag);// AudioManager.FLAG_PLAY_SOUND
		}
	}

}
