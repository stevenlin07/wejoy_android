package test.sdk.syncbox.func;

import java.util.HashSet;

import com.weibo.sdk.syncbox.BoxInstance;
import com.weibo.sdk.syncbox.listener.SendListener;
import com.weibo.sdk.syncbox.type.pub.BoxResult;
import com.weibo.sdk.syncbox.type.pub.ErrorInfo;
import com.weibo.sdk.syncbox.type.pub.SdkInfo;

import test.sdk.utils.BoxRecvListener;
import test.sdk.utils.DebugLog;
import test.sdk.utils.FakeStore;  

public class TestLogin implements SendListener {
 
	private FakeWait fakeWait = new FakeWait();
	private String username;
	private String password;
	
	public TestLogin(String username,String password) {
		this.username = username;
		this.password = password;
	}
	
	public void initAndLogin() throws Exception {
		BoxInstance boxInstance = BoxInstance.getInstance();
		boxInstance.register(new SdkInfo("android","ANDROID-MEYOU-TEST",(byte)10,(byte) 10),
				new DebugLog(), new FakeStore(), new BoxRecvListener()); 
		boxInstance.login(this, username, password,120); 
		fakeWait.startWaitResult();
	}

	@Override
	public void onSuccess(BoxResult boxResult) {
		System.out.println("登录成功");
		fakeWait.setSucc();
	}

	@Override
	public void onFailed(ErrorInfo errorInfo) {
		System.out.println("[onFailed][请求失败]");
		System.out.println("info:" + errorInfo.info);
		System.out.println("errorType:" + errorInfo.errorType);
		fakeWait.setFail();
	}

	@Override
	public void onFile(String fileId, HashSet<Integer> hasSuccSend, int limit) {
		// TODO Auto-generated method stub
		
	}
}