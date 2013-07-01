package com.weibo.sdk.syncbox.base;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
 
import com.weibo.sdk.syncbox.type.pub.RequestType;
import com.weibo.sdk.syncbox.type.pub.SdkInfo;
import com.weibo.sdk.syncbox.type.pub.ServerType;
import com.weibo.sdk.syncbox.utils.Base64;
import com.weibo.sdk.syncbox.utils.Constansts;
import com.weibo.sdk.syncbox.utils.VerifyModule;

public class BaseVerify extends VerifyModule {

	/** registerBasic 接口参数校验,true 校验成功 */
	static boolean registerBasicVerify(String withtag, String username, String password) {
		if (stringVerify(withtag, "username", username)
				&& stringVerify(withtag, "password", password)) {
			return true;
		}
		return false;
	}

	/** registerOAuth 接口参数校验,true 校验成功 */
	static boolean registerOAuthVerify(String withtag, String token) {
		if ( stringVerify(withtag, "token", token)) {
			return true;
		} else {
			return false;
		}
	}

	/**  commonInterfaceVerify 接口参数校验 */
	static boolean proxyInterfaceVerify(String withtag, String appKey, String url, RequestType requestType, 
			ServerType serverType) {
		if (!stringVerify(withtag, "url", url)
				|| !stringVerify(withtag, "appKey", appKey)
				|| isNull(withtag, "requestType", requestType)
				|| isNull(withtag, "serverType", serverType)) {
			return false;
		}
		return true;
	}

	/**
	 * sdkInfo参数校验及初始化
	 *
	 * @param sdkInfo sdk基本信息
	 * @return true 校验成功
	 * @throws Exception 
	 */
	public static boolean sdkInfoVerify(SdkInfo sdkInfo) throws Exception {
		WeSyncURI uri = new WeSyncURI();
		uri.protocolVersion = Constansts.SYNC_VERSION;
		uri.guid = sdkInfo.getGuid();
		uri.deviceType = sdkInfo.getDeviceType();
		uri.clientVersionMajor = sdkInfo.getClientVersionMajor();
		uri.clientVersionMinor = sdkInfo.getClientVersionMinor();
		for (Command command : Command.values()) {
			uri.command = command.toByte();
			byte[] uridata;
			uridata = WeSyncURI.toBytes(uri);
			String uristr = new String(Base64.encodeBase64(uridata));
			store.uriMap.put(command.toString(), uristr);
		}
		return true;
	}
}

final class WeSyncURI {
	public byte protocolVersion;
	public byte command;
	public byte clientVersionMajor;
	public byte clientVersionMinor;
	public String guid;
	public String deviceType;

	public static int MAX_ARGUMENT_NUMBER = 8;
	public String[] args = new String[MAX_ARGUMENT_NUMBER];

	public static byte[] toBytes(WeSyncURI uri) throws IOException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		bos.write(uri.protocolVersion);
		bos.write(uri.command);
		bos.write(uri.clientVersionMajor);
		bos.write(uri.clientVersionMinor);
		int guidLen = uri.guid.getBytes().length;
		if (guidLen > 0xFF) throw new RuntimeException();
		bos.write((byte) guidLen);
		bos.write(uri.guid.getBytes());

		int dtLen = uri.deviceType.getBytes().length;
		if (dtLen > 0xFF) throw new RuntimeException();
		bos.write((byte) dtLen);
		bos.write(uri.deviceType.getBytes());

		for (int i = 0; i < uri.args.length; i++) {
			if (null == uri.args[i]) continue;
			bos.write((byte) i);
			int len = uri.args[i].getBytes().length;
			if (len > 0xFF) throw new RuntimeException();
			bos.write((byte) len);
			bos.write(uri.args[i].getBytes());
		}

		bos.close();
		return bos.toByteArray();
	}
}

enum Command {
	Sync((byte) 0x0),
	SendFile((byte) 0x1),
	FolderSync((byte) 0x2),
	FolderCreate((byte) 0x3),
	FolderDelete((byte) 0x4),
	GetItemUnread((byte) 0x5),
	ItemOperations((byte) 0x6),
	Provision((byte) 0x7),
	Settings((byte) 0x8),
	GetFile((byte) 0x9),
	Unknown((byte) 0xFF);

	private final byte code;

	private Command(byte code) {
		this.code = code;
	}

	public byte toByte() {
		return code;
	}

	public static Command valueOf(final byte code) {
		for (Command c : Command.values()) {
			if (code == c.code) return c;
		}
		return Unknown;
	}
}
