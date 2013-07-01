/*
 * Copyright (C) 2009 The Sipdroid Open Source Project
 * Copyright (C) 2005 Luca Veltri - University of Parma - Italy
 * 
 * This file is part of Sipdroid (http://www.sipdroid.org)
 * 
 * Sipdroid is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This source code is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this source code; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package com.wejoy.common.audio;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.sipdroid.codecs.Codec;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.ToneGenerator;
import android.os.Build;
import android.os.PowerManager;
import android.util.Log;

import com.hzmc.testaudio.TestAudioApp;
import com.wejoy.common.MediaManager.PlayCallBack;
import com.wejoy.ui.AppUtils; 

/**
 * RtpStreamReceiver is a generic stream receiver. It receives packets from RTP
 * and writes them into an OutputStream.
 */
public class RtpStreamReceiver extends Thread {

	/** Whether working in debug mode. */
	public static boolean DEBUG = true;

	/** Payload type */
	Codec p_type;

	private PlayCallBack mCallBack;
	private DataInputStream din;
	protected DataOutputStream dout;

	static String codec = "";

	/** Size of the read buffer */
	public static final int BUFFER_SIZE = 1024;

	/**
	 * Maximum blocking time, spent waiting for reading new bytes [milliseconds]
	 */
	public static final int SO_TIMEOUT = 1000;

	/** Whether it is running */
	boolean running;
	AudioManager am;
	public static int speakermode = -1;
	public static boolean bluetoothmode;

	/**
	 * Constructs a RtpStreamReceiver.
	 * 
	 * @param output_stream
	 *            the stream sink
	 * @param socket
	 *            the local receiver SipdroidSocket
	 */
	public RtpStreamReceiver(Codec payload_type) {

		p_type = payload_type;

	}

	public void setDataInputStream(InputStream in) {
		din = new DataInputStream(in);
		// try {
		// din = new DataInputStream(new FileInputStream(new
		// File("sdcard/audoistream.temp")));
		// } catch (FileNotFoundException e) {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// }
		//
		// try {
		// dout = new DataOutputStream(new FileOutputStream(new
		// File("sdcard/audoistream.temp")));
		// } catch (FileNotFoundException e) {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// }
	}

	/** Whether is running */
	public boolean isRunning() {
		return running;
	}

	/** Stops running */
	public void halt() {
		running = false;
	}

	static boolean was_enabled;

	static ToneGenerator ringbackPlayer;
	static int oldvol = -1;

	static int stream() {
		return speakermode == AudioManager.MODE_IN_CALL ? AudioManager.STREAM_VOICE_CALL
				: AudioManager.STREAM_MUSIC;
	}

	double smin = 200, s;
	public static int nearend;

	void calc(short[] lin, int off, int len) {
		int i, j;
		double sm = 30000, r;

		for (i = 0; i < len; i += 5) {
			j = lin[i + off];
			s = 0.03 * Math.abs(j) + 0.97 * s;
			if (s < sm)
				sm = s;
			if (s > smin)
				nearend = 6000 * mu / 5;
			else if (nearend > 0)
				nearend--;
		}
		for (i = 0; i < len; i++) {
			j = lin[i + off];
			if (j > 6550)
				lin[i + off] = 6550 * 5;
			else if (j < -6550)
				lin[i + off] = -6550 * 5;
			else
				lin[i + off] = (short) (j * 5);
		}
		r = (double) len / (100000 * mu);
		if (sm > 2 * smin || sm < smin / 2)
			smin = sm * r + smin * (1 - r);
	}

	static long down_time;

	static void setStreamVolume(final int stream, final int vol, final int flags) {
		(new Thread() {
			public void run() {
				AudioManager am = (AudioManager) TestAudioApp.getContext()
						.getSystemService(Context.AUDIO_SERVICE);
				am.setStreamVolume(stream, vol, flags);
				if (stream == stream())
					restored = true;
			}
		}).start();
	}

	static boolean restored;

	void restoreVolume() {
		switch (getMode()) {
		case AudioManager.MODE_IN_CALL:
			track.setStereoVolume((float) (AudioTrack.getMaxVolume() * 0.25),
					(float) (AudioTrack.getMaxVolume() * 1.0));
			break;
		case AudioManager.MODE_NORMAL:
			track.setStereoVolume(AudioTrack.getMaxVolume(),
					AudioTrack.getMaxVolume());
			break;
		}
		setStreamVolume(stream(), am.getStreamMaxVolume(stream())
				* (speakermode == AudioManager.MODE_NORMAL ? 4 : 3) / 4, 0);
	}

	public static int getMode() {
		AudioManager am = (AudioManager) AppUtils.context.getSystemService(Context.AUDIO_SERVICE);
		if (Integer.parseInt(Build.VERSION.SDK) >= 5)
			return am.isSpeakerphoneOn() ? AudioManager.MODE_NORMAL
					: AudioManager.MODE_IN_CALL;
		else
			return am.getMode();
	}

	static boolean samsung;

	public static void setMode(int mode) {
		AudioManager am = (AudioManager) AppUtils.context.getSystemService(Context.AUDIO_SERVICE);
		if (Integer.parseInt(Build.VERSION.SDK) >= 5) {
			am.setSpeakerphoneOn(mode == AudioManager.MODE_NORMAL);
			if (samsung)
				AudioStreamRecordBase.changed = true;
		} else
			am.setMode(mode);
		speakermode = mode;

		setMediaVolume(AudioManager.STREAM_MUSIC,
				AudioManager.ADJUST_RAISE);

	}
	
	public static void setMediaVolume(int streamType, int flag) {
		AudioManager am = (AudioManager) AppUtils.context
				.getSystemService(Context.AUDIO_SERVICE);
		// int StreamType = AudioManager.STREAM_MUSIC;
		int iCurrent = am.getStreamVolume(streamType);
		int ivol = am.getStreamMaxVolume(streamType) * 2 / 3;
		if (iCurrent < ivol) {

			am.setStreamVolume(streamType, ivol, flag);// AudioManager.FLAG_PLAY_SOUND
		}
	}

	public static void restoreMode() {
		AudioManager am = (AudioManager) AppUtils.context.getSystemService(Context.AUDIO_SERVICE);
		if (Integer.parseInt(Build.VERSION.SDK) >= 5)
			am.setSpeakerphoneOn(false);
		else
			am.setMode(AudioManager.MODE_NORMAL);
	}

	void initMode() {
		samsung = Build.MODEL.contains("SAMSUNG")
				|| Build.MODEL.contains("SPH-") || Build.MODEL.contains("SGH-")
				|| Build.MODEL.contains("GT-");

		setMode(AudioManager.MODE_NORMAL);
	}

	public static void restoreSettings() {

		restoreMode();
	}

	public static float good, late, lost, loss;
	double avgheadroom;
	public static int timeout;
	int seq;

	RtpPacket rtp_packet;
	AudioTrack track;
	int maxjitter, minjitter, minjitteradjust, minheadroom;
	int cnt, cnt2, user, luser, luser2, lserver;
	public static int jitter, mu;

	void setCodec() {
		synchronized (this) {
			AudioTrack oldtrack;

			p_type.init();
			codec = p_type.getTitle();
			mu = p_type.samp_rate() / 8000;
			maxjitter = AudioTrack.getMinBufferSize(p_type.samp_rate(),
					AudioFormat.CHANNEL_CONFIGURATION_MONO,
					AudioFormat.ENCODING_PCM_16BIT);
			if (maxjitter < 2 * 2 * BUFFER_SIZE * 3 * mu)
				maxjitter = 2 * 2 * BUFFER_SIZE * 3 * mu;
			oldtrack = track;
			track = new AudioTrack(stream(), p_type.samp_rate(),
					AudioFormat.CHANNEL_CONFIGURATION_MONO,
					AudioFormat.ENCODING_PCM_16BIT, maxjitter,
					AudioTrack.MODE_STREAM);
			maxjitter /= 2 * 2;
			minjitter = minjitteradjust = 500 * mu;
			jitter = 875 * mu;
			minheadroom = maxjitter * 2;
			timeout = 1;
			luser = luser2 = -8000 * mu;
			cnt = cnt2 = user = lserver = 0;
			if (oldtrack != null) {
				oldtrack.stop();
				oldtrack.release();
			}
		}
	}

	void write(short a[], int b, int c) {
		// synchronized (this)
		{
			user += track.write(a, b, c);
		}
	}

	private int toInt(byte b) {
		if (b >= 0)
			return (int) b;
		else
			return (int) (b + 256);
	}

	// 4 byte Array to int
	private int byteArray4ToInt(byte[] byteValue) {
		if (byteValue.length != 4)
			return 0;

		int intValue = 0;
		try {
			intValue = toInt(byteValue[0]);
			intValue = (intValue << 8) + toInt(byteValue[1]);
			intValue = (intValue << 8) + toInt(byteValue[2]);
			intValue = (intValue << 8) + toInt(byteValue[3]);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return intValue;
	}

	String getByteString(byte[] bytes, int len) {

		StringBuilder buf = new StringBuilder(bytes.length * 2);
		int i;

		if (len == 0)
			len = bytes.length;
		for (i = 0; i < len; i++) {
			if (((int) bytes[i] & 0xff) < 0x10) {
				buf.append("0");
			}
			buf.append(Long.toString((int) bytes[i] & 0xff, 16));
		}
		return buf.toString();
	}

	PowerManager.WakeLock pwl, pwl2;
	static final int PROXIMITY_SCREEN_OFF_WAKE_LOCK = 32;
	boolean lockLast, lockFirst;

	boolean keepon;

	/** Runs it in a new Thread. */
	public void run() {

		byte[] buffer = new byte[BUFFER_SIZE + 12];
		// rtp_packet = new RtpPacket(buffer, 0);

		if (DEBUG)
			println("Reading blocks of max " + buffer.length + " bytes");

		running = true;
		restored = false;

		android.os.Process
				.setThreadPriority(android.os.Process.THREAD_PRIORITY_AUDIO);
		am = (AudioManager) AppUtils.context.getSystemService(
				Context.AUDIO_SERVICE);

		am.setVibrateSetting(AudioManager.VIBRATE_TYPE_RINGER,
				AudioManager.VIBRATE_SETTING_OFF);
		am.setVibrateSetting(AudioManager.VIBRATE_TYPE_NOTIFICATION,
				AudioManager.VIBRATE_SETTING_OFF);
		if (oldvol == -1)
			oldvol = am.getStreamVolume(AudioManager.STREAM_MUSIC);
		initMode();
		setCodec();
		short lin[] = new short[BUFFER_SIZE];
		short lin2[] = new short[BUFFER_SIZE];
		int server, headroom, todo, len = 0, m = 1, expseq, getseq, vm = 1, gap, gseq;
		// ToneGenerator tg = new
		// ToneGenerator(AudioManager.STREAM_VOICE_CALL,(int)(ToneGenerator.MAX_VOLUME*2*org.sipdroid.sipua.ui.Settings.getEarGain()));
		track.play();
		System.gc();
		lockFirst = true;

		byte[] bytes_pkg = new byte[BUFFER_SIZE + 100];
		byte[] header = new byte[3];
		byte[] encodeL = new byte[4];
		int posHeader = 0;
		int iencodeL = 0;
		boolean bEnd = false;
		byte[] tempdata = new byte[BUFFER_SIZE + 100];
		;
		long lt = System.currentTimeMillis();
		int startPos = 0;
		int remainL = 0;
		byte[] data = new byte[100];
		while (running) {

			try {
				len = din.read(buffer, 0, BUFFER_SIZE);
				if (-1 == len) {
					break;
				}

				for (int i = 0; i < bytes_pkg.length; i++) {
					bytes_pkg[i] = 0;
				}
				Log.i("Cycle", "len = " + len);
				if (remainL != 0) {
					Log.i("Cycle", "remain = " + remainL);
					System.arraycopy(tempdata, 0, bytes_pkg, 0, remainL);
					System.arraycopy(buffer, 0, bytes_pkg, remainL, len);
					len += remainL;
				} else {
					System.arraycopy(buffer, 0, bytes_pkg, 0, len);
				}

				while (!bEnd && running) {

					if (len <= 7) {
						// tempdata = bytes_pkg.clone();
						break;
					}
					server = track.getPlaybackHeadPosition();
					headroom = user - server;

					Log.i("Cycle", "playback = " + server + "\nuse = " + user
							+ "\nheadroom " + headroom);

					// if(server == 0 && headroom == 0){
					// for(int i=0; i < 5; i++){
					// write(lin2, 0, BUFFER_SIZE);
					// }
					// write(lin2, 0, 38*160-5*BUFFER_SIZE);
					// }
					long timesleep = 0;
					if (headroom > 3200) {
						if (server > 1000) {
							if (headroom > 4000)
								timesleep = 400;
							else
								timesleep = headroom / 16;
							try {
								Log.i("Cycle", "sleeptime = " + timesleep);
								sleep(timesleep);
							} catch (InterruptedException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}

					}
					System.arraycopy(bytes_pkg, 0, header, 0, header.length);

					String shead = new String(header);
					Log.i("Cycle", "shead = " + shead);
					if (shead.equals("ums")) {

						System.arraycopy(bytes_pkg, header.length, encodeL, 0,
								encodeL.length);

						iencodeL = byteArray4ToInt(encodeL);
						Log.i("Cycle", "iencodeL = " + iencodeL + " len = "
								+ len);
						if (iencodeL == 0) {
							bEnd = true;
							break;
						}

						posHeader = 7 + iencodeL;

						if (posHeader >= len)
							break;

						System.arraycopy(bytes_pkg, header.length
								+ encodeL.length, data, 0, iencodeL);
						long elapse = System.currentTimeMillis() - lt;
//						Log.i("Play", "elapse="	+ (elapse)
//							+ " d="	+ getByteString(bytes_pkg,
//							header.length + encodeL.length + iencodeL));
						lt = System.currentTimeMillis();

						int size = p_type.decode(data, lin, iencodeL - 12);
//						Log.i("Cycle", "decode_time_cost" + (System.currentTimeMillis() - lt));

						if (speakermode == AudioManager.MODE_NORMAL)
							calc(lin, 0, size);

						long st = System.currentTimeMillis();
						write(lin, 0, size);
//						Log.i("Cycle", "time_cost ===" + (System.currentTimeMillis() - st));
						// track.write(lin, 0, size);

						// writeIntoFile(lin, 0, size);

						remainL = len - posHeader;
						len = remainL;

						System.arraycopy(bytes_pkg, posHeader, tempdata, 0, len);

						System.arraycopy(tempdata, 0, bytes_pkg, 0, len);
					} else {
						break;
					}
				}

			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}

		track.stop();
		track.release();
		am.setStreamVolume(AudioManager.STREAM_MUSIC, oldvol, 0);
		restoreSettings();
		am.setStreamVolume(AudioManager.STREAM_MUSIC, oldvol, 0);
		oldvol = -1;
		p_type.close();
		codec = "";

		if (mCallBack != null)
			mCallBack.palyComplete("");
		// Call recording: stop incoming receive.

		if (DEBUG)
			println("rtp receiver terminated");

	}

	private void writeIntoFile(short[] lin, int ifrom, int size) {
		for (int i = ifrom; i < ifrom + size; i++) {
			try {
				Short s = new Short(lin[i]);

				dout.writeByte(s.byteValue());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	public boolean isPlaying() {
		if(track != null) {
			return track.getPlayState() == AudioTrack.PLAYSTATE_PLAYING;
		}
		else {
			return false;
		}
	}

	/** Debug output */
	private static void println(String str) {
		if (DEBUG)
			System.out.println("RtpStreamReceiver: " + str);
	}

	public static int byte2int(byte b) { // return (b>=0)? b : -((b^0xFF)+1);
		// return (b>=0)? b : b+0x100;
		return (b + 0x100) % 0x100;
	}

	public static int byte2int(byte b1, byte b2) {
		return (((b1 + 0x100) % 0x100) << 8) + (b2 + 0x100) % 0x100;
	}

	public static String getCodec() {
		return codec;
	}

	public void setPlayCallbak(PlayCallBack callBack) {
		mCallBack = callBack;
	}
}
