package com.weibo.sdk.syncbox.net;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

import com.weibo.sdk.syncbox.type.pub.EventInfo;
import com.weibo.sdk.syncbox.type.pub.EventType;
import com.weibo.sdk.syncbox.type.pub.SdkEvent;
import com.weibo.sdk.syncbox.utils.LogModule;
import com.weibo.sdk.syncbox.utils.MessageBlockingQueue.MessageEntity;
import com.weibo.sdk.syncbox.utils.NotifyModule;
import com.weibo.sdk.syncbox.utils.StoreModule;

/**
 * @ClassName: NioCore
 * @Description: 进行连接管理，采用NIO的方式：一个发送线程，一个接收线程
 * @author LiuZhao
 * @date 2012-9-13 下午1:22:11
 */
public enum NioCore {

	INSTANCE;

	private Selector selector;
	private SocketChannel socketChannel;
	private static LogModule log = LogModule.INSTANCE;
	private static NotifyModule notify = NotifyModule.INSTANCE;
	private static NetModule net = NetModule.INSTANCE;
	private Thread sendThread;
	private Thread receiverThread;
	
	private String ip;
	private int port;
	
	/** 终止NIO网络服务 */
	public void stop() {
		StoreModule.INSTANCE.nioSendQueue.clear();
		if (null != sendThread && sendThread.isAlive()) {
			log.info("[NIO][QUIT][sendThread]", SdkEvent.INVOKE);
			sendThread.interrupt(); // 发送线程退出
			sendThread = null;
		}
		if (null != selector && selector.isOpen()) {
			try {
				selector.close();
			} catch (IOException e) {
				log.error("[ERROR][NIO_STOP_EXCEPTION]",
						SdkEvent.NIO_STOP_EXCEPTION, e);
			}
			selector = null;
		}
		if (null != socketChannel && socketChannel.isOpen()) {
			try {
				socketChannel.close();
			} catch (IOException e) {
				log.error("[ERROR][NIO_STOP_EXCEPTION]",
						SdkEvent.NIO_STOP_EXCEPTION, e);
			}
			socketChannel = null;
		}
		log.info("[NIO][QUIT][receiverThread]", SdkEvent.INVOKE);// 接收线程退出，该线程无法捕获interrupt异常所以
																	// receiverThread.interrupt();
																	// 无效
		receiverThread = null;
	}

	/**
	 * 启动NIO网络
	 * 
	 * @param ip ip地址
	 * @param port 端口
	 * @param authorization 认证头
	 * @param authorBody 认证体
	 * @return
	 */
	public boolean start(String ip, int port, String authorization, String uri, byte[] authorBody) {
		stop(); // 终止网络
		log.info("[NIO START:" + ip + ":" + port + ":" + authorization +"]", SdkEvent.INVOKE);
		java.lang.System.setProperty("java.net.preferIPv6Addresses", "false");
		try {
			selector = Selector.open();
			socketChannel = SocketChannel.open();
			socketChannel.configureBlocking(false);
			socketChannel.connect(new InetSocketAddress(ip, port));
			socketChannel.register(selector, SelectionKey.OP_CONNECT);
			if (selector.select() > 0) {
				for (SelectionKey key : selector.selectedKeys()) {
					if (key.isConnectable()) {
						key.interestOps(SelectionKey.OP_READ);
					}
				}
			}
			socketChannel.finishConnect();
			// 要放在认证包，发送完成后启动，否则会引起连接错乱的问题
			sendThread = new Thread(new ClientSend());
			sendThread.start();
			receiverThread = new Thread(new ClientReceive());
			receiverThread.start();
		} catch (Exception e) {
			unConnected();
			log.error("[ERROR][NIO_START_EXCEPTION]", SdkEvent.NIO_START_EXCEPTION, e);
			stop();
			return false;
		}
		// socket建立成功，发送认证包
		byte[] header = NioUtils.setAuthHeader(ip, port, authorBody.length, authorization, uri).getBytes();
		if (!NioUtils.sendPacket(socketChannel, authorBody, header)){
			unConnected();
			return false;
		} else {
			log.info("[NIO][CONN SUCC]", SdkEvent.NIO_START_SUCC);
			connected();
			this.ip = ip;
			this.port = port;
			net.nioConnected.set(true);
			return true;
		}
	}
	
	private void connected() {
		EventInfo eventInfo = new EventInfo();
		eventInfo.info = "网络连接成功";
		eventInfo.eventType = EventType.NIO_CONNECTED;
		notify.onEvent(eventInfo);
	}
	
	private void unConnected() {
		log.error("[ERROR][NIO_START_EXCEPTION]", SdkEvent.NIO_START_EXCEPTION, null);
		EventInfo eventInfo = new EventInfo();
		eventInfo.info = "网络无法联通";
		eventInfo.eventType = EventType.NIO_UNABLE_CONNNECT;
		notify.onEvent(eventInfo);
	}

	private void disConnected(Exception e) {
		log.error("[ERROR][NIO_RECV_SELECTOR_IOEXCEPTION]", SdkEvent.NIO_RECV_SELECTOR_IOEXCEPTION, e);
		EventInfo eventInfo = new EventInfo();
		eventInfo.info = "网络连接断开";
		eventInfo.eventType = EventType.NIO_DISCONNECT;
		notify.onEvent(eventInfo);
	}
	
	class ClientReceive implements Runnable {
		private ByteBuffer receiveByteBuffer = ByteBuffer.allocate(512000);

		@Override
		public void run() {
			log.info("[NIO][REVC START]", SdkEvent.INVOKE);
			boolean flag = true;
			while (true) {
				try {
					selector.select();
					for (SelectionKey key : selector.selectedKeys()) { // 断网之后，会不断地返回可读事件。算是bug么？
						if (key.isReadable()) {
							selector.selectedKeys().remove(key);
							SocketChannel socketChannel = (SocketChannel) key.channel();
							if (!NioUtils.dealResult(receiveByteBuffer, socketChannel)) {
								flag = false;
								break;
							}
							key.interestOps(SelectionKey.OP_READ);
						}
					}
					if (flag) {
						log.info("[NIO][RECV DONE]", SdkEvent.INVOKE);
					} else {
						break;
					}
				} catch (Exception e) {
					disConnected(e);
					break;
				}
			}
			log.info("[NIO][RECV STOP]", SdkEvent.INVOKE);
			receiveByteBuffer.clear();
			net.nioConnected.set(false);
		}
	}
	
	class ClientSend implements Runnable {

		@Override
		public void run() {
			log.info("[NIO][SEND START]", SdkEvent.INVOKE);
			while (true) {
				MessageEntity messageEntity = null;
				try {
					messageEntity = StoreModule.INSTANCE.nioSendQueue.getMessageEntity();
				} catch (InterruptedException e) {
					log.error("[ERROR][NIO_SEND_ITERRUPTED]", SdkEvent.NIO_SEND_QUEUE_INTERRUPTED, e);
					break;
				}
				if (null == messageEntity) {
					continue;
				}
				byte[] body = messageEntity.getHttpEntity();
				byte[] header = NioUtils.setHttpHeader(ip, port, body.length,
						messageEntity.getUristr()).getBytes();
				if (!NioUtils.sendPacket(socketChannel,body, header)) {
					break;
				}
			}
			log.info("[NIO][SEND STOP]", SdkEvent.INVOKE);
			net.nioConnected.set(false);
		}
	}
}