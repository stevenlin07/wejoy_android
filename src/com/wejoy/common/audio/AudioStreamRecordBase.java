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

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Random;

import org.sipdroid.codecs.Codec;

import com.wejoy.common.MediaManager.RecordCallBack;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

/**
 * RtpStreamSender is a generic stream sender. It takes an InputStream and sends
 * it through RTP.
 */
public abstract class AudioStreamRecordBase extends Thread {
	/** Whether working in debug mode. */
	public static boolean DEBUG = true;

	/** The RtpSocket */
	// RtpSocket rtp_socket = null;
	protected DataOutputStream dout;

	protected DataOutputStream m_saveStream;

	RecordCallBack m_RecordCallBack;
	/** Payload type */
	Codec p_type;

	/** Number of frame per second */
	int frame_rate;

	/** Number of bytes per frame */
	int frame_size;

	/**
	 * Whether it works synchronously with a local clock, or it it acts as slave
	 * of the InputStream
	 */
	boolean do_sync = true;

	/**
	 * Synchronization correction value, in milliseconds. It accellarates the
	 * sending rate respect to the nominal value, in order to compensate program
	 * latencies.
	 */
	int sync_adj = 0;

	/** Whether it is running */
	boolean running = false;
	boolean muted = false;

	// DTMF change
	String dtmf = "";
	int dtmf_payload_type = 101;

	/**
	 * Constructs a RtpStreamSender.
	 * 
	 * @param input_stream
	 *            the stream to be sent
	 * @param do_sync
	 *            whether time synchronization must be performed by the
	 *            RtpStreamSender, or it is performed by the InputStream (e.g.
	 *            the system audio input)
	 * @param payload_type
	 *            the payload type
	 * @param frame_rate
	 *            the frame rate, i.e. the number of frames that should be sent
	 *            per second; it is used to calculate the nominal packet time
	 *            and,in case of do_sync==true, the next departure time
	 * @param frame_size
	 *            the size of the payload
	 * @param src_socket
	 *            the socket used to send the RTP packet
	 * @param dest_addr
	 *            the destination address
	 * @param dest_port
	 *            the destination port
	 */
	public AudioStreamRecordBase(boolean do_sync, Codec payload_type,
			long frame_rate, int frame_size) {
		init(do_sync, payload_type, frame_rate, frame_size);
	}

	/** Inits the RtpStreamSender */
	private void init(boolean do_sync, Codec payload_type, long frame_rate,
			int frame_size) {
		this.p_type = payload_type;
		this.frame_rate = (int) frame_rate;
		this.frame_size = frame_size;
		this.do_sync = do_sync;

	}

	public void setOutputSteam(OutputStream out) {
		dout = new DataOutputStream(out);
	}

	/** Sets the synchronization adjustment time (in milliseconds). */
	public void setSyncAdj(int millisecs) {
		sync_adj = millisecs;
	}

	/** Whether is running */
	public boolean isRunning() {
		return running;
	}

	public boolean mute() {
		return muted = !muted;
	}

	public static int delay = 0;
	public static boolean changed;

	/** Stops running */
	public void halt() {
		running = false;
	}

	Random random;
	double smin = 200, s;
	int nearend;

	void calc(short[] lin, int off, int len) {
		int i, j;
		double sm = 30000, r;

		for (i = 0; i < len; i += 5) {
			j = lin[i + off];
			s = 0.03 * Math.abs(j) + 0.97 * s;
			if (s < sm)
				sm = s;
			if (s > smin)
				nearend = 3000 * mu / 5;
			else if (nearend > 0)
				nearend--;
		}
		r = (double) len / (100000 * mu);
		if (sm > 2 * smin || sm < smin / 2)
			smin = sm * r + smin * (1 - r);
	}

	void calc1(short[] lin, int off, int len) {
		// Log.i("CACLC", "call---calc1");
		int i, j;

		for (i = 0; i < len; i++) {
			j = lin[i + off];
			lin[i + off] = (short) (j >> 2);
		}
	}

	void calc2(short[] lin, int off, int len) {
		// Log.i("CACLC", "call---calc2");
		int i, j;

		for (i = 0; i < len; i++) {
			j = lin[i + off];
			lin[i + off] = (short) (j >> 1);
		}
	}

	void calc10(short[] lin, int off, int len) {
		// Log.i("CACLC", "call---calc10");
		int i, j;

		for (i = 0; i < len; i++) {
			j = lin[i + off];
			if (j > 16350)
				lin[i + off] = 16350 << 1;
			else if (j < -16350)
				lin[i + off] = -16350 << 1;
			else
				lin[i + off] = (short) (j << 1);
		}
	}

	void noise(short[] lin, int off, int len, double power) {
		int i, r = (int) (power * 2);
		short ran;

		if (r == 0)
			r = 1;
		for (i = 0; i < len; i += 4) {
			ran = (short) (random.nextInt(r * 2) - r);
			lin[i + off] = ran;
			lin[i + off + 1] = ran;
			lin[i + off + 2] = ran;
			lin[i + off + 3] = ran;
		}
	}

	private void putInt(byte[] buf, int offset, int value) {
		buf[offset + 0] = (byte) ((value >> 24) & 0xff);
		buf[offset + 1] = (byte) ((value >> 16) & 0xff);
		buf[offset + 2] = (byte) ((value >> 8) & 0xff);
		buf[offset + 3] = (byte) ((value >> 0) & 0xff);
	}

	public static int m;
	int mu;
	
	protected abstract void recordStreamFrame();
	protected abstract void finishRecord();

	/** Runs it in a new Thread. */
	public void run() {

		int seqn = 0;
		long time = 0;
		double p = 0;
		int micgain = 0;
		long last_tx_time = 0;
		long next_tx_delay;
		long now;
		running = true;
		m = 1;
		android.os.Process
				.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
		mu = p_type.samp_rate() / 8000;
		int min = AudioRecord.getMinBufferSize(p_type.samp_rate(),
				AudioFormat.CHANNEL_CONFIGURATION_MONO,
				AudioFormat.ENCODING_PCM_16BIT);
		if (min == 640) {
			if (frame_size == 960)
				frame_size = 320;
			if (frame_size == 1024)
				frame_size = 160;
			min = 4096 * 3 / 2;
		} else if (min < 4096) {
			if (min <= 2048 && frame_size == 1024)
				frame_size /= 2;
			min = 4096 * 3 / 2;
		} else if (min == 4096) {
			min *= 3 / 2;
			if (frame_size == 960)
				frame_size = 320;
		} else {
			if (frame_size == 960)
				frame_size = 320;
			if (frame_size == 1024)
				frame_size *= 2;
		}
		frame_rate = p_type.samp_rate() / frame_size;
		long frame_period = 1000 / frame_rate;
		frame_rate *= 1.5;
		byte[] buffer = new byte[frame_size + 12];
		// RtpPacket rtp_packet = new RtpPacket(buffer, 0);
		// rtp_packet.setPayloadType(p_type.number());
		if (DEBUG)
			println("Reading blocks of " + buffer.length + " bytes");

		println("Sample rate  = " + p_type.samp_rate());
		println("Buffer size = " + min);

		AudioRecord record = null;

		short[] lin = new short[frame_size * (frame_rate + 1)];
		int index = 0, num = 0, ring = 0, pos;
		random = new Random();

		String header = "ums";
		byte[] headerByte = header.getBytes();
		byte[] encodeL = new byte[4];

		p_type.init();
		long ttt = System.currentTimeMillis();
		long audiosize = 0;
		
		while (running) {
			if (changed || record == null) {
				if (record != null) {
					record.stop();
					record.release();
					// if (RtpStreamReceiver.samsung) {
					// AudioManager am = (AudioManager) UMS_Function_Utility
					// .getAppContext().getSystemService(
					// Context.AUDIO_SERVICE);
					// am.setMode(AudioManager.MODE_IN_CALL);
					// am.setMode(AudioManager.MODE_NORMAL);
					// }
				}
				changed = false;
				record = new AudioRecord(MediaRecorder.AudioSource.MIC,
						p_type.samp_rate(),
						AudioFormat.CHANNEL_CONFIGURATION_MONO,
						AudioFormat.ENCODING_PCM_16BIT, min);
				if (record.getState() != AudioRecord.STATE_INITIALIZED) {
					Log.i("AudioRecord", "STATE_INITIALIZED Fail");
					record = null;
					m_RecordCallBack.recordStartCallback(false);
					break;
				}
				record.startRecording();
				m_RecordCallBack.recordStartCallback(true);
				micgain = 2;
			}
			// if (muted || Receiver.call_state == UserAgent.UA_STATE_HOLD) {
			// if (Receiver.call_state == UserAgent.UA_STATE_HOLD)
			// RtpStreamReceiver.restoreMode();
			// record.stop();
			// while (running && (muted || Receiver.call_state ==
			// UserAgent.UA_STATE_HOLD)) {
			// try {
			// sleep(1000);
			// } catch (InterruptedException e1) {
			// }
			// }
			// record.startRecording();
			// }

			if (frame_size < 480) {
				now = System.currentTimeMillis();
				next_tx_delay = frame_period - (now - last_tx_time);
				last_tx_time = now;
				if (next_tx_delay > 0) {
					try {
						sleep(next_tx_delay);
					} catch (InterruptedException e1) {
					}
					last_tx_time += next_tx_delay - sync_adj;
				}
			}
			pos = (ring + delay * frame_rate * frame_size)
					% (frame_size * (frame_rate + 1));
			num = record.read(lin, pos, frame_size);
			
			if (num == -1)
				break;
			else if(num == 0) {
				continue;
			}
			
			if(index ++ % 2 == 0) {
				printAverage(lin, pos, num);
			}

			// if (!p_type.isValid())
			// continue;

			// if (RtpStreamReceiver.speakermode == AudioManager.MODE_NORMAL) {
			// calc(lin, pos, num);
			// Log.i("CACLC", "call---calc");
			// if (RtpStreamReceiver.nearend != 0
			// && RtpStreamReceiver.down_time == 0)
			// noise(lin, pos, num, p / 2);
			// else if (nearend == 0)
			// p = 0.9 * p + 0.1 * s;
			// }
			// else
			switch (micgain) {
			case 1:
				calc1(lin, pos, num);
				break;
			case 2:
				calc2(lin, pos, num);
				break;
			case 10:
				calc10(lin, pos, num);
				break;
			}
			
//			Log.i("SIP_R", "start:" + (System.currentTimeMillis() - ttt)
//					+ "\n ring = " + ring);
			num = p_type.encode(lin, ring % (frame_size * (frame_rate + 1)),
					buffer, num);

			ttt = System.currentTimeMillis();

			ring += frame_size;
			// rtp_packet.setSequenceNumber(seqn++);
			// rtp_packet.setTimestamp(time);
			// rtp_packet.setPayloadLength(num);

			putInt(encodeL, 0, num + 12);
			audiosize += headerByte.length + encodeL.length + num + 12;
			try {
				dout.write(headerByte, 0, headerByte.length);
				dout.write(encodeL, 0, encodeL.length);
				dout.write(buffer, 0, num + 12);
				
				// @by jichao, 输出分片
				recordStreamFrame();

				m_saveStream.write(headerByte, 0, headerByte.length);
				m_saveStream.write(encodeL, 0, encodeL.length);
				m_saveStream.write(buffer, 0, num + 12);

			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				break;
			}

			// rtp_packet.getPayload();

			if (p_type.number() == 9)
				time += frame_size / 2;
			else
				time += frame_size;
			// if (RtpStreamReceiver.good != 0 &&
			// RtpStreamReceiver.loss/RtpStreamReceiver.good > 0.01) {
			// if (selectWifi && Receiver.on_wlan &&
			// SystemClock.elapsedRealtime()-lastscan > 10000) {
			// wm.startScan();
			// lastscan = SystemClock.elapsedRealtime();
			// }
			// if (improve && delay == 0 &&
			// (p_type.number() == 0 || p_type.number() == 8 || p_type.number()
			// == 9))
			// m = 2;
			// else
			// m = 1;
			// } else
			// m = 1;
		}
		
		finishRecord(); 
		
		if (record != null) {
			record.stop();
			record.release();
		}
		m = 0;

		p_type.close();
		m_RecordCallBack.recordStopCallback(audiosize);
		if (DEBUG)
			println("rtp sender terminated");
	}

	private void printAverage(short[] lin, int pos, int num) {
		long average = 0;
		for (int i = pos; i < pos + num; i++) {
			average += Math.abs(lin[i]);
		}
		m_RecordCallBack.recordVolumeCallback((long) (average / (num * 1.0)));
	}

	/** Debug output */
	private static void println(String str) {
		if (DEBUG)
			System.out.println("RtpStreamSender: " + str);
	}

	public void setSavePath(String savepath) {
		try {
			m_saveStream = new DataOutputStream(new FileOutputStream(new File(
					savepath)));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public void setRecocrdCallback(RecordCallBack callBack) {
		m_RecordCallBack = callBack;
	}

}
