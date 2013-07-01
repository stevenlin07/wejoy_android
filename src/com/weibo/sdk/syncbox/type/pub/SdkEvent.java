package com.weibo.sdk.syncbox.type.pub;

/**
 * <br>
 * <table border="1">
 * <caption><b>SDK内部的事件类型(黄色表示上报)</b></caption>
 * <tr bgcolor="#d0d0d0"><td>序号</td><td>事件类型</td><td>事件说明</td></tr>
 * <tr bgcolor="#FFFF37"><td>1</td><td>NIO_START_EXCEPTION</td><td>[NIO]启动时候发生的异常</td></tr>
 * <tr><td>2</td><td>NIO_STOP_EXCEPTION</td><td>[NIO]关闭时候发生的异常</td></tr>
 * <tr><td>3</td><td>NIO_SEND_QUEUE_INTERRUPTED</td><td>[NIO]发送队列被中断, 属于客户端主动停止发送队列导致的</td></tr>
 * <tr><td>4</td><td>NIO_SEND_THREAD_INTERRUPTED</td><td>[NIO]发送数据时候的停顿被终止</td></tr>
 * <tr bgcolor="#FFFF37"><td>5</td><td>NIO_SEND_IOEXCEPTION</td><td>[NIO]发送信息发生的IO异常</td></tr>
 * <tr bgcolor="#FFFF37"><td>6</td><td>NIO_SEND_LOWZERO</td><td>[NIO]发送时候小于0，属于IO异常</td></tr>
 * <tr bgcolor="#FFFF37"><td>7</td><td>NIO_RECV_SELECTOR_IOEXCEPTION</td><td>[NIO]接收SELECT IO异常</td></tr>
 * <tr bgcolor="#FFFF37"><td>8</td><td>NIO_RECV_SELECTOR_CLOSEException</td><td>[NIO]select关闭</td></tr>
 * <tr bgcolor="#FFFF37"><td>9</td><td>NIO_RECV_READ_IOEXCEPTION</td><td>[NIO]接收READ IO异常</td></tr>
 * <tr bgcolor="#FFFF37"><td>10</td><td>NIO_RECV_READ_ZERO</td><td>[NIO]收到空包，说明当前网络中断</td></tr>
 * <tr bgcolor="#FFFF37"><td>11</td><td>NIO_RECV_NO_HEAD</td><td>[NIO]没有识别包出头,接收包错误，可能是服务器出错</td></tr>
 * <tr><td>12</td><td>NIO_RECV_DEAL_ERROR</td><td>[NIO]接收的数据包处理出错,丢弃该数据包，继续接收即可</td></tr>
 * <tr bgcolor="#FFFF37"><td>13</td><td>NIO_RECV_NO_HEAD</td><td>[NIO]认证超时</td></tr>
 * <tr bgcolor="#FFFF37"><td>14</td><td>AUTH_BADINPUT</td><td>认证码返回不是200,用户名密码/TOKEN错误或服务当前不可用</td></tr>
 * <tr bgcolor="#FFFF37"><td>15</td><td>NIO_RECV_NO_HEAD</td><td>调用接口没有经过认证</td></tr>
 * <tr><td>16</td><td>SDK_NOTIFY_ITERRUPTED</td><td>通知通道被中断</td></tr>
 * <tr bgcolor="#FFFF37"><td>17</td><td>PARAMS_ERROR</td><td>输入参数错误</td></tr>
 * <tr><td>18</td><td>ITERRUPTED_EXCEPTION</td><td>中断错误</td></tr>
 * <tr bgcolor="#FFFF37"><td>19</td><td>SYNC_TIMEOUT</td><td>SYNC协议的请求返回超时</td></tr>
 * <tr bgcolor="#FFFF37"><td>20</td><td>OP_SYNC_TIMEOUT</td><td>同步操作请求返回超时</td></tr>
 * <tr bgcolor="#FFFF37"><td>21</td><td>SENDFILE_READ_EMPTY</td><td>要发送的文件长度为空</td></tr>
 * <tr bgcolor="#FFFF37"><td>22</td><td>SENDFILE_READ_ERROR</td><td>要发送的文件读取错误</td></tr>
 * <tr><td>23</td><td>SENDFILE_CLOSE_ERROR</td><td>要发送的文件关闭错误</td></tr>
 * <tr bgcolor="#FFFF37"><td>24</td><td>GETFILE_WRITE_ERROR</td><td>下载文件写入错误</td></tr>
 * <tr><td>25</td><td>GETFILE_CLOSE_ERROR</td><td>下载文件关闭错误</td></tr>
 * <tr><td>26</td><td>WESYNC_PARSE_ERROR</td><td>WESYNC请求的返回结果的解析出错</td></tr>
 * <tr><td>27</td><td>CODE_ENCRYPT_ERROR</td><td>协议编码错误，通知没有意义</td></tr>
 * <tr><td>28</td><td>CODE_URI_ERROR</td><td>下载文件写入错误</td></tr>
 *
 * </table>
 * <br>
 */
public enum SdkEvent {
	// 1-50 是异常
	/**[NIO]启动时候发生的异常*/
	NIO_START_EXCEPTION (1),
	/**[NIO]关闭时候发生的异常*/
	NIO_STOP_EXCEPTION (2),
	NIO_SEND_QUEUE_INTERRUPTED (3),
	NIO_SEND_THREAD_INTERRUPTED (4),
	NIO_SEND_IOEXCEPTION (5),
	NIO_SEND_LOWZERO (6),
	NIO_RECV_SELECTOR_IOEXCEPTION (7),
	NIO_RECV_SELECTOR_CLOSEException (8),
	NIO_RECV_READ_IOEXCEPTION (9),
	NIO_RECV_READ_ZERO (10),
	NIO_RECV_NO_HEAD (11),
	NIO_RECV_DEAL_ERROR (12),
	AUTH_TIMEOUT (13),
	AUTH_BADINPUT (14),
	AUTH_FORBID (15),
	SDK_NOTIFY_ITERRUPTED (16),
	PARAMS_ERROR (17),
	ITERRUPTED_EXCEPTION (18),
	OP_SYNC_TIMEOUT (20),
	SENDFILE_READ_EMPTY (21),
	SENDFILE_READ_ERROR (22),
	SENDFILE_CLOSE_ERROR (23),
	GETFILE_WRITE_ERROR (24),
	GETFILE_CLOSE_ERROR (25),
	WESYNC_PARSE_ERROR (26),
	CODE_ENCRYPT_ERROR (27),
	/**下载文件写入错误*/
	CODE_URI_ERROR (28),

	/**SD卡出错*/
	LOCAL_STORE_ERROR (29),
	EXPROTLOG_ERROR (30),
	/**认证过期*/
	AUTH_LAPSE(31),
	/**sync修复请求超时*/
	FIXSYNC_TIMEOUT(32),
	/**代理修复请求超时*/
	PROXYSYNC_TIMEOUT(33),
	
	// 50 -100 普通事件
	/**方法被调用*/
	INVOKE (51),
	/**网络连接成功*/
	NIO_START_SUCC (52),

	/** 心跳超时*/
	HEARTBEAT_TIMEOUT (60),
	SENDFILE_TIMEOUT (61),
	PROXYINTERFACE_TIMEOUT (62),
	SYNC_TIMEOUT (63),
	GETFILE_TIMEOUT (64),
	WEJOYINTERFACE_TIMEOUT (65);

	private final int value;

	private SdkEvent(int value) {
		this.value = value;
	}

	public int get() {
		return value;
	}
}
