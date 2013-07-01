package com.wejoy.sdk.net;

import android.net.NetworkInfo.State;

public class NetInfo {

	public State state; // 网络状态
	public String mainTypeName; // 网络主类型名称
	public int mainNetType; // 网络类型-主类型
	public int subNetType;  // 网络类型-子类型
	public String subTypeName ; // 网络子类型名称
}
