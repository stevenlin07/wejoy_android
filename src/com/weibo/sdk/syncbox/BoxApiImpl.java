package com.weibo.sdk.syncbox;
 
import java.util.HashSet;

import com.weibo.sdk.syncbox.api.BoxApi;
import com.weibo.sdk.syncbox.base.WesyncService;
import com.weibo.sdk.syncbox.listener.SendListener;
import com.weibo.sdk.syncbox.type.pub.ConvType; 
import com.weibo.sdk.syncbox.type.pub.MetaType;
import com.weibo.sdk.syncbox.type.pub.RequestType;
import com.weibo.sdk.syncbox.type.pub.SdkEvent;
import com.weibo.sdk.syncbox.type.pub.ServerType; 
import com.wejoy.util.ElasticNetUtil;

public class BoxApiImpl extends WesyncService implements BoxApi{
	
	@Override
	public void login(final SendListener sendListener,final String username, final String password, final int timeout) {
		pool.submit(new Runnable() {
			@Override
			public void run() {
				loginOnCreate(sendListener, AuthType.BASIC, null, username, password, timeout);
			}
		});
	}

	@Override
	public void login(final SendListener sendListener, final String token, final int timeout) {
		log.info("[login OAUTH]", SdkEvent.INVOKE);
		pool.submit(new Runnable() {
			@Override
			public void run() {
				loginOnCreate(sendListener, AuthType.OAUTH, token, null, null, timeout);
			}
		});
	}

	// 调用此接口，要求对输入内容进行信任！
			// 如果登录过一次，如果没成功就不会残留任何个人信息
			// 只要在登录界面登录过一次，客户端应该就保存了uid和token
			// 直接进入mainAcitivity的时候，sdk肯定没有uid和authHead，应该都存
			// 首次进入mainActivity，保存信息，但登录成功
			// 登录成功后，就会刷新uid信息到sdk，二次再调用的时候，就不需要再次保存
			// 首次进入mainActivity，保存信息，但没有登录成功，通过事件通知网络没有联通
			// 再次调用的时候，
	@Override
	public void loginOnResume(final String token, final String uid) {
		log.info("[loginOnResume]", SdkEvent.INVOKE);
		loginOnResumeBase(AuthType.OAUTH, token, null, null, uid);
	}
	
	@Override
	public void loginOnResume(final String username, final String password, final String uid) {
		loginOnResumeBase(AuthType.BASIC, null, username, password, uid);
	}

	
	@Override
	public void sendText(final SendListener sendListener, final String touid, final String text, final ConvType convType, final int timeout) {
		pool.submit(new Runnable() {
			@Override
			public void run() {
				sendTextBase(sendListener, touid, text,convType,timeout);
			}
		});
	}

	@Override
	public void sendMixed(final SendListener sendListener, final String touid, final String content, final ConvType convType, final int timeout) {
		pool.submit(new Runnable() {
			@Override
			public void run() {
				sendMixedBase(sendListener, touid, content,convType,timeout);
			}
		}); 
	}

	@Override
	public void sendAudio(final SendListener sendListener, final String touid, final String spanId, 
			final int spanSequenceNo, final boolean endTag, final byte[] audioData, final ConvType convType,
			final String content_ext, final int timeout) {
		pool.submit(new Runnable() {
			@Override
			public void run() {
				sendAudioBase(sendListener, touid, audioData, spanId, spanSequenceNo, endTag, convType, content_ext, timeout);
			}
		}); 
	}
	
	@Override
	public void sendFileMsg(final SendListener sendListener,final String fileId, final String touid,
			final ConvType convType, final MetaType metaType, final String filepath, final String filename,
			final byte[] thumbnail, final int timeout) {
		pool.submit(new Runnable() {
			@Override
			public void run() {
				sendFileMsgBase(sendListener, fileId, touid, convType, metaType, filepath,  filename, thumbnail,timeout);
			}
		}); 
	}
	
	@Override
	public void sendFile(final SendListener sendListener, final String touid, final String filepath, final int timeout) {
		pool.submit(new Runnable() {
			@Override
			public void run() {
				sendFileAllBase(sendListener, touid, filepath, ElasticNetUtil.getFileSlice(), timeout);
			}
		}); 
	}

	@Override
	public void getFile(final SendListener sendListener, final String fileId, final String getFilePath,
			final int fileLength, final int limit, final int timeout) {
		pool.submit(new Runnable() {
			@Override
			public void run() {
				getFileAllBase(sendListener, fileId, getFilePath, fileLength, limit );
			}
		}); 
	}

	@Override
	public void sendFilePart(final SendListener sendListener, final String fileId, final String filepath, 
			final HashSet<Integer> hasSuccSend, final int timeout) {
		pool.submit(new Runnable() {
			@Override
			public void run() {
				sendFilePartBase(sendListener, fileId, filepath, hasSuccSend, timeout);
			}
		}); 
	}

	@Override
	public void getFilePart(final SendListener sendListener, final String fileId, final String getFilePath, 
			final int fileLength, final int limit, final HashSet<Integer> hasSuccRecv, final int timeout) {
		pool.submit(new Runnable() {
			@Override
			public void run() {
				getFilePartBase(sendListener, fileId, getFilePath, fileLength, limit,hasSuccRecv);
			}
		});  
	}

	@Override
	public void proxyInterface(final SendListener sendListener, final String appKey, final String url, 
			final String params, final RequestType requestType, final ServerType serverType, 
			final String fileParam, final byte[] fileData, final int timeout) {
		pool.submit(new Runnable() {
			@Override
			public void run() {
				proxyInterfaceBase(sendListener, appKey, url, params, requestType, serverType, fileParam,
					 fileData, timeout);
			}
		}); 
	}

	@Override
	public void netChange(final boolean flag, final int apnType) {
		pool.submit(new Runnable() {
			@Override
			public void run() {
				net.netChange(flag, apnType);
			}
		});
	}
	
	public void wejoyInterface(final SendListener sendListener, final String requestString, final int timeout) {
		pool.submit(new Runnable() {
			@Override
			public void run() {
				wejoyInterfaceBase(sendListener, requestString, timeout);
			}
		}); 
	}


}

