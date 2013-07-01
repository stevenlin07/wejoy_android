package com.weibo.sdk.syncbox;

public final class BoxInstance extends BoxApiImpl {

	private final static BoxInstance boxInstance = new BoxInstance();

	private BoxInstance() {
	}

	public static BoxInstance getInstance() {
		return boxInstance;
	}
}
