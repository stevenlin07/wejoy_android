package com.weibo.sdk.syncbox.utils;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import com.weibo.sdk.syncbox.type.pub.SdkEvent;

/**
 * @ClassName: MessageBlockingQueue
 * @Description: TODO(这里用一句话描述这个类的作用)
 * @author LiuZhao
 * @date 2012-9-9 下午4:36:30
 */
public class MessageBlockingQueue implements java.io.Serializable {

	private static final long serialVersionUID = -6740214940942692648L;
	private Queue<MessageEntity> fileQueue = new LinkedList<MessageEntity>();
	private Queue<MessageEntity> messageQueue = new LinkedList<MessageEntity>();
	private final ReentrantLock takeLoke = new ReentrantLock();// 用于读取的独占锁
	private final ReentrantLock putLock = new ReentrantLock();// 用于写入的独占锁
	private final Condition notEmpty = takeLoke.newCondition();
	private final AtomicInteger count = new AtomicInteger(0);
	private static LogModule log = LogModule.INSTANCE;

	/**
	 * 发送小块儿消息
	 * @param msgEntity
	 * @return
	 */
	public boolean putMsg(MessageEntity msgEntity) {
		if (null == msgEntity) throw new NullPointerException();
		boolean flag = true;
		final AtomicInteger count = this.count;
		final ReentrantLock putLock = this.putLock;
		final ReentrantLock takeLoke = this.takeLoke;
		try {
			takeLoke.lockInterruptibly();
			putLock.lockInterruptibly();
			messageQueue.add(msgEntity);
			count.getAndIncrement();
			notEmpty.signal();
		} catch (InterruptedException e) {
			log.error("[REQ][Interrupted]", SdkEvent.ITERRUPTED_EXCEPTION, e);
			flag = false;
		} finally {
			takeLoke.unlock();
			putLock.unlock();
		}
		return flag;
	}

	/**
	 * 发送文件块
	 * @param messageEntity
	 * @return
	 */
	public boolean putBlock(MessageEntity messageEntity){
		if (null == messageEntity) throw new NullPointerException();
		boolean flag = true;
		final AtomicInteger count = this.count;
		final ReentrantLock putLock = this.putLock;
		final ReentrantLock takeLoke = this.takeLoke;
		try {
			putLock.lockInterruptibly();
			takeLoke.lockInterruptibly();
			fileQueue.add(messageEntity);
			count.getAndIncrement();
			notEmpty.signal();
		} catch (InterruptedException e) {
			log.error("[REQ][Interrupted]", SdkEvent.ITERRUPTED_EXCEPTION, e);
			flag = false;
		} finally {
			takeLoke.unlock();
			putLock.unlock();
		}
		return flag;
	}

	public MessageEntity getMessageEntity() throws InterruptedException {
		MessageEntity messageEntity;
		int c = -1;
		final AtomicInteger count = this.count;
		final ReentrantLock takeLoke = this.takeLoke;
		takeLoke.lockInterruptibly();
		try {
			while (count.get() == 0) {
				notEmpty.await();
			}
			messageEntity = messageQueue.poll();
			if (null == messageEntity) {
				messageEntity = fileQueue.poll();
			}
			c = count.getAndDecrement();
			if (c > 1) {
				notEmpty.signal();
			}
		} catch (InterruptedException e) {
			throw e;
		} finally {
			takeLoke.unlock();
		}
		return messageEntity;
	}

	public void clear() {
		fileQueue.clear();
		messageQueue.clear();
	}

	public static final class MessageEntity implements java.io.Serializable{
		/**
		 *
		 */
		private static final long serialVersionUID = 1L;
		private String uristr;
		private byte[] httpBody;

		public MessageEntity(String uristr, byte[] httpBody) {
			this.uristr = uristr;
			this.httpBody = httpBody;
		}

		public String getUristr() {
			return uristr;
		}

		public byte[] getHttpEntity() {
			return httpBody;
		}
	}
}