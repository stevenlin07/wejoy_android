package com.weibo.sdk.syncbox.api;

import java.util.HashSet;

import com.weibo.sdk.syncbox.listener.RecvListener;
import com.weibo.sdk.syncbox.listener.SendListener; 
import com.weibo.sdk.syncbox.type.pub.ConvType;
import com.weibo.sdk.syncbox.type.pub.MetaType;
import com.weibo.sdk.syncbox.type.pub.RequestType;
import com.weibo.sdk.syncbox.type.pub.SdkInfo;
import com.weibo.sdk.syncbox.type.pub.ServerType;
import com.weibo.sdk.syncbox.utils.pub.ExportLog;
import com.weibo.sdk.syncbox.utils.pub.SdkDatabase;

/**
 * 消息箱的sdk逻辑接口.
 * @author liuzhao
 * 
 */
public interface BoxApi {

	/**
	 * 注册sdk，绑定资源
	 * 
	 * @param sdkInfo SDK信息
	 * @param exportLog 实现日志接口，获取日志
	 * @param sdkDatabase 实现存储接口，存储sdk缓存信息
	 * @param sdkListener 实现监听接口，监听sdk的消息
	 * @return
	 */
	void register(SdkInfo sdkInfo, ExportLog exportLog, SdkDatabase sdkDatabase, RecvListener recvListener) throws Exception;

	/**
	 * 采用用户名密码认证方式
	 * 
	 * @param sendListener 实现用以接收请求结果
	 * @param username 用户名 (必填)
	 * @param password 密码 (必填)
	 * @param timeout 超时时间（秒）(必填)
	 * @return true 请求发送成功
	 */
	void login(SendListener sendListener, String username, String password, int timeout);

	/**
	 * 采用OAUTH认证方式，并注册SDK的基本信息.
	 * 
	 * @param sendListener 实现用以接收请求结果
	 * @param token token串 (必填)
	 * @param timeout 超时时间（秒）(必填)
	 * @return true 请求发送成功
	 */
	void login(SendListener sendListener, String token, int timeout);

	/**
	 * 在主acitivity的onResume中进行调用
	 * @param token
	 * @param uid
	 */
	void loginOnResume(String token,String uid) ;
	void loginOnResume(String username, String password, String uid) ;
	
	/** 退出登录 */
	void logout();

	/**
	 * 发送普通文本消息. 
	 * 发送消息之后，服务端会生成一个新的消息ID返还给客户端，客户端将这个ID作为本地存储消息的ID
	 * 
	 * @param sendListener 实现用以接收请求结果
	 * @param touid 接收方 (必填)
	 * @param text 发送的文本内容 (必填)
	 * @param convType 会话类型 (必填)
	 * @param timeout 超时时间 (必填)
	 * @return true 发送请求出去成功
	 */
	void sendText(SendListener sendListener, String touid, String text, ConvType convType, int timeout);

	/**
	 * 发送富文本消息 Tips:要求请求体大小不超过50K
	 * 
	 * @param sendListener 实现用以接收请求结果
	 * @param touid 接收方 (必填)
	 * @param content 发送的内容 (必填)
	 * @param convType 会话类型 (必填)
	 * @param timeout 超时时间 (必填)
	 * @return 发送请求出去成功
	 */
	void sendMixed(SendListener sendListener, String touid, String content, ConvType convType, int timeout);

	/**
	 * 发送文件--图片/文件/视频
	 * 
	 * @param sendListener 实现用以接收请求结果
	 * @param touid 接收方 (必填)(UID/GID)
	 * @param filepath 文件的保存路径 (必填)
	 * @param timeout 超时时间 (必填)
	 * @return
	 */
	void sendFile(SendListener sendListener, String touid, String filepath, int timeout);

	/**
	 * 续传文件
	 * 
	 * @param sendListener 实现用以接收请求结果
	 * @param touid 接收方 (必填)(UID/GID)
	 * @param filepath 文件的保存路径 (必填)
	 * @param hasSuccSend 已发送的分片序号 (必填)
	 * @param timeout 超时时间 (必填)
	 * @return
	 */
	void sendFilePart(SendListener sendListener, String fileId, String filepath, HashSet<Integer> hasSuccSend, int timeout);

	/**
	 * 文件全部传输完毕需要调用这个接口,发送消息给接收方
	 * 
	 * @param sendListener 实现用以接收请求结果
	 * @param fileId 文件ID (必填)
	 * @param touid 接收方 (必填)(UID/GID)
	 * @param convType 会话类型(必填)
	 * @param metaType 文件类型 (必填)
	 * @param filepath 文件的保存路径 (必填)
	 * @param filename 文件名 (必填)
	 * @param thumbnail 缩略图的存储路径 (可选)
	 * @param timeout 超时时间 (必填)
	 * @return
	 */
	void sendFileMsg(SendListener sendListener, String fileId, String touid, ConvType convType, MetaType metaType, String filepath, String filename, byte[] thumbnail, int timeout);

	/**
	 * getFile 下载文件.
	 * 
	 * @param sendListener 实现用以接收请求结果
	 * @param fileId 需要的文件ID (必填)
	 * @param getFilePath 指定下载到的路径 (必填)
	 * @param fileLength 文件长度(单位byte)(必填)
	 * @param limit 分片总数 (必填)
	 * @param timeout 超时时间 (必填)
	 * @return 发送请求出去成功
	 */
	void getFile(SendListener sendListener, String fileId, String getFilePath, int fileLength, int limit, int timeout);

	/**
	 * getFile 下载续传.
	 * 
	 * @param sendListener 实现用以接收请求结果
	 * @param fileId 需要的文件ID (必填)
	 * @param getFilePath 指定下载到的路径 (必填)
	 * @param fileLength 文件长度(单位byte)(必填)
	 * @param limit 分片总数 (必填)
	 * @param hasSuccRecv 已经接收的分片序号(必填)
	 * @param timeout 超时时间 (必填)
	 * @return 发送请求出去成功
	 */
	void getFilePart(SendListener sendListener, String fileId, String getFilePath, int fileLength, int limit, HashSet<Integer> hasSuccRecv, int timeout);

	/**
	 * 发送语音分片 对于一组语音分片，每个分片的，包含一个消息ID和一个分片序号 一组语音的所有分片，拥有相同的spanId.
	 * 
	 * @param sendListener 实现用以接收请求结果
	 * @param touid 接收方 (必填)(UID/GID)
	 * @param spanId 分片的组ID，一组语音分片的组ID相同 (必填)
	 * @param spanSequenceNo 分片序号 (必填) 从1开始计数
	 * @param endTag 终止标记 true 表示终止 (必填)
	 * @param audioData 语音数据 (必填)
	 * @param convType 会话类型 (必填)
	 * @param content_ext 会话附加信息，在声音文件中主要用于存储语音时长等信息（可选）
	 * @param timeout 超时时间 (必填)
	 * @return 发送请求出去成功
	 */
	void sendAudio(SendListener sendListener, String touid, String spanId, int spanSequenceNo, boolean endTag, 
			byte[] audioData, ConvType convType, String content_ext, int timeout);

	/**
	 * 通用的代理服务接口.
	 * 
	 * @param sendListener 实现用以接收请求结果
	 * @param appKey 微博的appKey (必填)
	 * @param url 服务的URL (必填)
	 * @param params 参数的串 (选填)
	 * @param requestType 请求类型 (必填)
	 * @param serverType 服务类型 (必填)
	 * @param fileParam 文件参数名称 (选填)
	 * @param fileData 文件数据 (选填)
	 * @param timeout 超时时间 (必填)
	 * @return 发送请求出去成功
	 */
	void proxyInterface(SendListener sendListener, String appKey, String url, String params, RequestType requestType, ServerType serverType, 
			String fileParam, byte[] fileData, int timeout);

	/**
	 * 发送dove服务的请求
	 * @param requestString 请求串
	 * @param withtag 请求标记
	 * @param timeout 超时时间
	 */
	void wejoyInterface(SendListener sendListener, String requestString, int timeout);

	/**
	 * 网络连接变化管理接口
	 * @param flag 连通/断开
	 * @param int apn类型
	 */
	void netChange(boolean flag,int apnType);
}
