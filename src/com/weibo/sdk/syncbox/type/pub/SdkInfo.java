package com.weibo.sdk.syncbox.type.pub;

/**
 * @ClassName: SdkInfo 
 * @Description: sdk基本信息
 * @author liuzhao
 * @date 2013-5-20 上午11:43:57 
 */
public final class SdkInfo {

	private String deviceType = "android";
	private String guid = "123456";
	private byte clientVersionMajor = 10;
	private byte clientVersionMinor = 10;

	/**
	 * @param deviceType 设备类型
	 * @param guid 设备唯一标识
	 * @param clientVersionMajor 客户端主版本号
	 * @param clientVersionMinor 客户端次版本号
	 */
	public SdkInfo(String deviceType, String guid, byte clientVersionMajor, byte clientVersionMinor) {
		this.deviceType = deviceType;
		this.guid = guid;
		this.clientVersionMajor = clientVersionMajor;
		this.clientVersionMinor = clientVersionMinor;
	}

	public String getDeviceType() {
		return deviceType;
	}

	public String getGuid() {
		return guid;
	}

	public byte getClientVersionMajor() {
		return clientVersionMajor;
	}

	public byte getClientVersionMinor() {
		return clientVersionMinor;
	}

}