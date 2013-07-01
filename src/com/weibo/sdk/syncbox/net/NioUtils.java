package com.weibo.sdk.syncbox.net;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import com.weibo.sdk.syncbox.type.pub.EventInfo;
import com.weibo.sdk.syncbox.type.pub.EventType;
import com.weibo.sdk.syncbox.type.pub.SdkEvent;
import com.weibo.sdk.syncbox.utils.LogModule;
import com.weibo.sdk.syncbox.utils.NotifyModule;

/**
 * @ClassName: HttpUtil
 * @Description: 进行连接管理，采用NIO的方式：一个发送线程，一个接收线程
 * @author LiuZhao
 * @date 2012-9-13 下午1:22:11
 */
public class NioUtils {
 
	private static LogModule log = LogModule.INSTANCE;
	private static NotifyModule notify = NotifyModule.INSTANCE;
	
	/** 设置认证头*/
	public static String setAuthHeader(String ip, int port, int length, String authorization, String uri) {
		StringBuilder sb = new StringBuilder();
		sb.append("POST /wesync?" + uri + " HTTP/1.1\r\n");
		sb.append("authorization: " + authorization + "=\r\n");
		return setBasicHeader(sb, ip, port, length);
	}
	
	/** 设置普通请求头*/
	public static String setHttpHeader(String ip, int port, int length, String uri) {
		StringBuilder sb = new StringBuilder();
		if (null != uri) {
			sb.append("POST /wesync?" + uri + " HTTP/1.1\r\n");
		} else {
			sb.append("POST /wesync HTTP/1.1\r\n");
		}
		
		return setBasicHeader(sb, ip, port, length);
	}
	
	/**封装必备信息*/
	private static String setBasicHeader(StringBuilder sb, String ip, int port, int length) {
		sb.append("User-Agent: Jakarta Commons-HttpClient/3.1\r\n");
		sb.append("Host: "+ip+":"+ port + "\r\n");
		sb.append("Content-Length: " + length + "\r\n\r\n");
		return sb.toString();
	}
	
	/** 发送数据包 */
	public static boolean sendPacket(SocketChannel socketChannel, byte[] body, byte[] header) {
		ByteBuffer bb = ByteBuffer.allocate(header.length + body.length);
		bb.put(header).put(body).flip();
		log.info("[NIO][SEND WILL:" + bb.limit() + "]", SdkEvent.INVOKE);
		int send = 0; // 发送出去的字节数
		int reminder = bb.limit(); // 尚未发送的字节数
		int tryNum = 1; // 尝试发送的次数
		while (reminder > 0) {
			try {
				send = socketChannel.write(bb); // 根据发送出去的字节数来进行判断
				if (0 > send) {
					log.error("[ERROR][NIO_SEND_LOW_ZERO]",SdkEvent.NIO_SEND_LOWZERO, null);
					return false;
				}

				if (0 == send) {
					try {
						if (tryNum >= 25) {
							Thread.sleep(100 * tryNum);
						} else {
							Thread.sleep(50 * tryNum);
						}
					} catch (InterruptedException e) {
						log.error("[ERROR][NIO_SEND_THREAD_INTERRUPTED]",SdkEvent.NIO_SEND_THREAD_INTERRUPTED, e);
						return false;
					}
				}
				reminder = reminder - send; // 未发送出去的字节数
				bb.position(bb.limit() - reminder); // 重新设定postison
				log.info("[NIO][SEND][SENDED:" + send + ",REMINDER:"+ reminder + ",TRY:" + tryNum + "]",SdkEvent.INVOKE);
				tryNum++;
			} catch (IOException e) {
				log.error("[ERROR][NIO_SEND_IO_EXCEPTION]", SdkEvent.NIO_SEND_IOEXCEPTION, e);
				return false;
			}
		}
		return true;
	}
	
	public static  boolean dealResult(ByteBuffer receiveByteBuffer, SocketChannel socketChannel) { // 进行结果预处理
		try {
			while ((socketChannel.read(receiveByteBuffer)) > 0) {}
		} catch (IOException e) { // 从channel中读取数据错误，在网络启动后断网，发生 Operation timed
			log.error("[ERROR][NIO_RECV_READ_IOEXCEPTION]",SdkEvent.NIO_RECV_READ_IOEXCEPTION, e);
			return false;
		}
		byte[] buf = new byte[receiveByteBuffer.position()];
		receiveByteBuffer.position(0); // 设置position = 0
		receiveByteBuffer.get(buf); // 将receiveByteBuffer的内容存入buf缓冲区中
		receiveByteBuffer.clear(); // 清空缓冲区即可
		if (buf.length <= 0) { // 网络中断收到空包
			log.error("[ERROR][NIO_RECV_READ_ZERO]",SdkEvent.NIO_RECV_READ_ZERO,null);
			return false;
		}
		// 进入下面的逻辑，因为没有收到空数据
		int packetLength = 0; // 一个独立的数据包的总长度
		int position = 0;
		log.info("[NIO][RECV][PACKET:" + buf.length+"]",SdkEvent.INVOKE);
		while (position < buf.length) { // 循环是因为包没有处理完毕
			byte[] tempPacket = new byte[buf.length - position];
			System.arraycopy(buf, position, tempPacket, 0, buf.length- position);
			int contentLength = getRespContentLength(tempPacket);
			log.info("[NIO][RECV][REMIND:"+tempPacket.length+",Position:"+position+",Content:"+contentLength+"]",SdkEvent.INVOKE);
			// 读取Head的ContentLength
			if (contentLength <= 0) { // ContentLength 读取失败，数据包不完整
				receiveByteBuffer.put(tempPacket, 0, tempPacket.length);
				log.info("[NIO][RECV][not completed,savebuffer again][RECVPOST:"+receiveByteBuffer.position()+"",SdkEvent.INVOKE);
				if (tempPacket.length >= 256) { // 经验值，如果在这么长的内容中仍然无法计算出contentLength，视为数据包乱序，
					log.error("[ERROR][NIO_RECV_NO_HEAD]",SdkEvent.NIO_RECV_NO_HEAD,null);
					return false; // 可以暂时停止接收数据包
				}
				break; // 继续攒数据包
			}
			packetLength = lastResult(tempPacket, contentLength); // 处理数据包
			if (0 == packetLength ) { // 可认为需要攒包，或者包处理出错，丢掉
				receiveByteBuffer.clear();
				receiveByteBuffer.put(buf, position, buf.length - position);
				break;
			}
			if(-1 == packetLength){ // 包处理出错
				receiveByteBuffer.clear();
				break;
			}
			position = packetLength + position;
		}
		return true;
	}

	static int getRespContentLength(byte[] resp) {
		int headoff = 0;
		for (int i = 0; i < resp.length; i++) {
			if (i + 1 < resp.length && resp[i] == '\r'&& resp[i + 1] == '\n') {
				byte[] temp = new byte[i];
				System.arraycopy(resp, headoff, temp, 0, i - headoff);
				String line = new String(temp);
				String[] tokens = line.split(": ");
				i += 2;
				headoff = i;
				if ("Content-Length".equals(tokens[0])) { return Integer.valueOf(tokens[1].trim()); }
			}
		}
		return 0;
	}

	/**
	 * @param buf
	 * @param contentLength
	 * @return 0 需要继续攒包 -1 解析失败 其他，正常解析的长度
	 */
	static int lastResult(byte[] buf, int contentLength) { // 进行结果处理
		int headLength = 0;
		int packetLength = 0;
		for (int i = 0; i < buf.length; i++) {
			if (i + 3 < buf.length && buf[i] == '\r' && buf[i + 1] == '\n'&& buf[i + 2] == '\r' && buf[i + 3] == '\n') {
				i += 4;
				byte[] temp = new byte[contentLength];
				headLength = i;
				packetLength = headLength + temp.length;
				log.info("[NIO][RECV Packet:" + packetLength +",Head:" + headLength+",Body:" + temp.length+"]",SdkEvent.INVOKE);
				if (buf.length < packetLength) { // 读到了不足1个包的内容，应该继续攒包
					log.info("[NIO][RECV BUF:"+buf.length+"< Packet:"+packetLength+"][需要继续攒包]",SdkEvent.INVOKE);
					return 0;
				}
				try {
					System.arraycopy(buf, headLength, temp, 0,contentLength);
					NetModule.INSTANCE.getNetHandler().nioProcess(temp);
					return packetLength;
				} catch (Exception e) {
					EventInfo eventInfo = new EventInfo();
					eventInfo.info = "网络连接断开";
					eventInfo.eventType = EventType.NIO_DISCONNECT;
					notify.onEvent(eventInfo);
					log.error("[ERROR][NIO_RECV_DEAL_ERROR]",SdkEvent.NIO_RECV_DEAL_ERROR, e);
					return -1; // 出错
				}
			}
		}
		return packetLength;
	}
}