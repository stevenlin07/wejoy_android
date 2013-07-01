package com.wejoy.common.audio;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;

import org.sipdroid.codecs.Codec;
import org.sipdroid.codecs.Speex;

import com.wejoy.module.ChatMessage;
import com.wejoy.service.apphandler.SendAudioHandler;
import com.wejoy.service.apphandler.SendMessageHandler;
import com.wejoy.store.DataStore;

public class AudioStreamRecorder extends AudioStreamRecordBase {
	protected static Codec mCodec = new Speex();
	/*
	 * 默认6.1s一片
	 */
	private static final long DEFAULT_TIME_FRAME = 6100L; 
	protected long time_frame;
	private ByteArrayOutputStream bos = new ByteArrayOutputStream();
	private long lastRecordTime = -1;
	private SendAudioHandler audioSender = new SendAudioHandler();
	
	private long startTime = 0;
	private boolean finalFrame = false;
	private int frameId = 1;
	private ChatMessage chat;
	private String touid;
	private SendMessageHandler.SendMessageFinishListener sendMessageFinishListener;
	
	public AudioStreamRecorder(ChatMessage chat, String touid, 
		SendMessageHandler.SendMessageFinishListener sendMessageFinishListener) 
	{
		this(DEFAULT_TIME_FRAME);
		this.chat = chat;
		setSavePath(chat.attachPath);
		
		this.touid = touid;
		this.sendMessageFinishListener = sendMessageFinishListener;
	}
	
	public void start() {
		startTime = System.currentTimeMillis();
		
		super.start();
	}
	
	/*
	 * @by jichao, default use Speex as audio encoder
	 */
	public AudioStreamRecorder(long time_frame) {
		super(true, mCodec, mCodec.samp_rate(),	mCodec.frame_size());
		this.time_frame = time_frame;
		setOutputSteam(bos);
	}
	
	protected void recordStreamFrame() {
		if(lastRecordTime < 0) {
			lastRecordTime = System.currentTimeMillis();
		}
		else if(!finalFrame && System.currentTimeMillis() - lastRecordTime >= time_frame) {
			chat.audioTime = (int) (System.currentTimeMillis() - lastRecordTime) / 1000;
			lastRecordTime = System.currentTimeMillis();
			byte[] data = bos.toByteArray();
			bos.reset();
			
			System.out.println("[AUDIO] send audio spanSequence=" + frameId);
			audioSender.processGroupChat(chat, touid, frameId, data, finalFrame, sendMessageFinishListener);
			frameId ++;
		}
	}
	
	protected void finishRecord() {
		if(finalFrame) {
			chat.audioTime = (int) ((System.currentTimeMillis() - startTime)/ 1000) ;
			
			// @by jichao, discard recording if audio time is less than 1 s
			if(chat.audioTime <= 0) {
				return;
			}
			
			byte[] data = bos.toByteArray();
			bos.reset();
			System.out.println("[AUDIO] send audio finish");
			audioSender.processGroupChat(chat, touid, frameId, data, finalFrame, sendMessageFinishListener);
			
			// update chat
			DataStore.getInstance().insertChatMessage(chat);
		}
	}
	
	public boolean haltWithResult() {
		finalFrame = true;

		// @by jichao, sleep 100ms for better recording effect
		try {
			Thread.sleep(100);
		} 
		catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		super.halt();
		
		// @by jichao, 如果录音时间小于1s，则认为录音时间太短，放弃此次录音
		return System.currentTimeMillis() - startTime >= 1000;
	}
}
