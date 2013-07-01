package com.wejoy.common;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

import org.sipdroid.codecs.Codec;
import org.sipdroid.codecs.Speex;

import com.hzmc.testaudio.TestAudioApp;
import com.wejoy.common.audio.AudioStreamRecorder;
import com.wejoy.common.audio.RtpStreamReceiver;
import com.wejoy.module.ChatMessage;
import com.wejoy.service.apphandler.SendFileHandler;
import com.wejoy.service.apphandler.SendMessageHandler;

import android.content.Context;
import android.hardware.SensorManager;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;
/**
 * 
 * @author WeJoy Group
 *
 */
public class MediaManager {
	private static MediaManager mInstanceManager;

	private MediaRecorder mRecorder;
	private MediaPlayer mediaPlayer;
	private boolean mbPlaying = false;
	private String mAudioPath;
	private String mFileTime;
	private PlayCallBack mplayCallBack;
	private RecordCallBack mRecordCallBack;
	private int mRecordDuration = 0;
	private long mRecordMaxSize = 0;
	private boolean mbStarted = false;
	private Codec mCodec;
	AudioStreamRecorder streamRecorder;
	RtpStreamReceiver mReceiver;
	private Map<String, SendFileHandler> sendFileHandlers = new ConcurrentHashMap<String, SendFileHandler>();

	public static MediaManager getMediaPlayManager() {
		if (mInstanceManager == null) {
			mInstanceManager = new MediaManager();
		}
		return mInstanceManager;
	}

	public MediaManager() {
		mediaPlayer = new MediaPlayer();
		mCodec = new Speex();
	}

	public void setSendFileHandler(String key, SendFileHandler handler) {
		sendFileHandlers.put(key, handler);
	}
	
	public SendFileHandler getSendFileHandler(String key) {
		return sendFileHandlers.get(key);
	}
	
	public SendFileHandler removeSendFileHandler(String key) {
		return sendFileHandlers.remove(key);
	}
	
	private static byte[] zipBytes(byte[] input) throws IOException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		BufferedOutputStream bufos = new BufferedOutputStream(
				new DeflaterOutputStream(bos));
		bufos.write(input);
		bufos.close();
		byte[] retval = bos.toByteArray();
		bos.close();
		return retval;
	}

	private static byte[] unzipByteFromBytes(byte[] bytes) throws IOException {
		ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
		BufferedInputStream bufis = new BufferedInputStream(
				new InflaterInputStream(bis));
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		byte[] buf = new byte[1024];
		int len;
		while ((len = bufis.read(buf)) > 0) {
			bos.write(buf, 0, len);
		}
		byte[] retval = bos.toByteArray();
		bis.close();
		bufis.close();
		bos.close();
		return retval;
	}

	private void stopMediaRecord() {
		try {
			{
				mRecorder.stop();
				mRecorder.release();
				mbStarted = false;
			}
			// if(mSocket != null)
			// mSocket.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public void setRecordMaxDuration(int second) {
		mRecordDuration = second;
	}

	public void setRecordMaxSize(long size) {
		mRecordMaxSize = size;
	}

	public long getRecordMaxSize() {
		return mRecordMaxSize;
	}

	public int getRecordMaxDuration() {
		return mRecordDuration;
	}

	public boolean testReadWhenRecord(String path) {
		File file = new File(path);
		return file.canRead();
	}

	private boolean openMediaRecord(String path) {
		mRecorder = new MediaRecorder();
		mRecorder.reset();
		mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
		mRecorder.setOutputFormat(MediaRecorder.OutputFormat.RAW_AMR);// THREE_GPP
		mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);// DEFAULT
		if (mRecordDuration == 0)
			mRecordDuration = 30;// s
		mRecorder.setMaxDuration(mRecordDuration * 1000);// default 30000ms
		// if(mRecordMaxSize == 0)
		// mRecordMaxSize = 50*1024;//bytes
		// mRecorder.setMaxFileSize(mRecordMaxSize);

		mRecorder.setOnInfoListener(onRecordListener);

		// Get ready!
		try {
			mRecorder.setOutputFile(path);
			mRecorder.prepare();

		} catch (IllegalStateException e) {
			e.printStackTrace();

			return false;
		} catch (IOException e) {
			e.printStackTrace();

			return false;
		}
		try {
			mRecorder.start();
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}

		return true;
	}

	public void stopRecord() {
		stopMediaRecord();
		// stopAudioRecord();
	}

	public boolean openRecord(String path) {
		return openMediaRecord(path);

		// return openAudioRecord(path);
		// openDialogRecord();
	}

	MediaRecorder.OnInfoListener onRecordListener = new MediaRecorder.OnInfoListener() {

		@Override
		public void onInfo(MediaRecorder mr, int what, int extra) {
			Log.i("Audio", "play: " + what + "\n extra:" + extra);
			if (MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED == what) {
				Toast.makeText(TestAudioApp.getContext(),
						mRecordDuration + "ms time reached", 1000).show();
			}
			if (MediaRecorder.MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED == what) {
				Toast.makeText(TestAudioApp.getContext(),
						mRecordMaxSize + "bytes size reached", 1000).show();

			}

		}

	};

	private void setPlaySource(final FileDescriptor fdp) {

		new Thread(new Runnable() {

			@Override
			public void run() {
				boolean bprepared = false;
				int iCount = 0;
				while (!bprepared) {
					if (iCount >= 5)
						break;
					iCount++;
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					try {
						mediaPlayer.reset();
						mediaPlayer
								.setAudioStreamType(AudioManager.STREAM_MUSIC);
						mediaPlayer.setDataSource(fdp);
						mediaPlayer.prepare();
						bprepared = true;
						mediaPlayer.start();
					} catch (IllegalArgumentException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (IllegalStateException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}

			}
		}).start();

	}

	private void setPlaySource(String path) {
		try {
			FileInputStream fis = new FileInputStream(path);
			mediaPlayer.setDataSource(fis.getFD());
			mediaPlayer.prepare();
			fis.close();
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalStateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void playMedia(PlayCallBack callBack, String path, String time) {
		mFileTime = time;

		mediaPlay(callBack, path);

	}

	public void mediaPlayRealTime(Socket socket) {

		Log.i("Audio", "socket: " + socket);
		ParcelFileDescriptor pfd = ParcelFileDescriptor.fromSocket(socket);

		setPlaySource(pfd.getFileDescriptor());

	}

	public void mediaPlayRealTime(File file) {

		Log.i("Audio", "socket: " + file);

		setPlaySource(file.getPath());

	}

	public void stopPlayMedia() {
		mediaStop();
	}

	private void mediaPlay(PlayCallBack callBack, String path) {
		mplayCallBack = callBack;
		Log.i("Audio", "play: " + path);
		mediaPlayer = new MediaPlayer();
		mediaPlayer.reset();
		setPlaySource(path);
		mAudioPath = path;

		mediaPlayer.start();

		mbPlaying = true;
		MediaPlayCompleted listener = new MediaPlayCompleted(path);
		mediaPlayer.setOnCompletionListener(listener);

	}

	private void mediaStop() {
		Log.i("Audio", "stop");
		mbPlaying = false;
		if (mediaPlayer != null) {
			mediaPlayer.stop();
			mediaPlayer.release();
			mediaPlayer = null;
		}

		mplayCallBack.palyComplete(mAudioPath);
	}

	public boolean hasMediaPlaying() {
		return mbPlaying;
	}

	public boolean isAudioPlaying(String path, String time) {
		if (mbPlaying && path.equals(mAudioPath) && mFileTime.equals(time))
			return true;
		return false;
	}

	class MediaPlayCompleted implements OnCompletionListener {

		String mMediaPath;

		MediaPlayCompleted(String path) {
			mMediaPath = path;
		}

		@Override
		public void onCompletion(MediaPlayer mp) {
			mp.release();
			mp = null;
			mbPlaying = false;
			if (mplayCallBack != null)
				mplayCallBack.palyComplete(mMediaPath);
		}

	}

	public interface PlayCallBack {
		public void palyComplete(String path);
	}

	public interface RecordCallBack {
		public void recordStartCallback(boolean bstarted);

		public void recordStopCallback(long size);

		public void recordVolumeCallback(long value);
	}

	public void startRealTimeRecord(ChatMessage chat, String touid, 
		SendMessageHandler.SendMessageFinishListener sendMessageFinishListener) 
	{
		if (streamRecorder != null) {
			stopRealTimeRecord();
		}
		
		streamRecorder = new AudioStreamRecorder(chat, touid, sendMessageFinishListener);
		streamRecorder.setRecocrdCallback(mRecordCallBack);
		streamRecorder.start();
	}

	public boolean stopRealTimeRecord() {
		boolean result = false;
		
		if (streamRecorder != null) {
			result = streamRecorder.haltWithResult();
			streamRecorder = null;
		}
		
		return result;
	}

	public void setRecordCallBack(RecordCallBack callBack) {
		mRecordCallBack = callBack;
	}

	private void startRtpReceive(InputStream inputStream) {
		mReceiver = new RtpStreamReceiver(mCodec);
		mReceiver.setPlayCallbak(mplayCallBack);
		mReceiver.setDataInputStream(inputStream);
		mReceiver.start();
	}

	private void stopRtpReceive() {
		if (mReceiver != null) {
			mReceiver.halt();
			mReceiver = null;
		}
	}

	public void setPlayCompleteCallback(PlayCallBack callBack) {
		mplayCallBack = callBack;
	}

	public void playAudioRealTime(InputStream inputStream) {
		stopRtpReceive();
		startRtpReceive(inputStream);
	}
	
	public boolean isAudioPlaying() {
		if (mReceiver != null)
			return mReceiver.isPlaying();
		return false;
	}
	
	public void stopAudioPlay() {
		Log.i("Audio_Play", "stop");
		stopRtpReceive();
	}

	public boolean isAudioRealTimeplay() {
		if (mReceiver != null) {
			return mReceiver.isRunning();
		}
		return false;
	}

	public boolean isAudioRecord() {
		if (streamRecorder != null)
			return streamRecorder.isRunning();
		return false;
	}

}
