package test.sdk.syncbox.func;
 
import java.util.HashSet; 

import com.weibo.sdk.syncbox.BoxInstance;
import com.weibo.sdk.syncbox.listener.SendListener;
import com.weibo.sdk.syncbox.type.pub.BoxResult;
import com.weibo.sdk.syncbox.type.pub.ErrorInfo;
import com.weibo.sdk.syncbox.type.pub.RequestType;
import com.weibo.sdk.syncbox.type.pub.ServerType;

import test.sdk.utils.TestCaseUtils; 

public class TestSendProxy extends TestCaseUtils implements SendListener{

    private String username = "meyoutest001@sina.com";
    private String password = "meyoutest";
//    private static String url = "http://i2.api.weibo.com/2/account/profile/basic.json";
//    private String params = "source=1124443769";
//    private String APPKEY = "1124443769";
    
	private static String url = "http://i2.api.weibo.com/2/statuses/update.json";
	private static String params = "source=2841080378&&status='测试文字1'";
	private static final String APPKEY = "2841080378";

    public void testSendProxy() throws Exception{
    	TestLogin testLogin = new TestLogin(username, password);
    	testLogin.initAndLogin();
    	BoxInstance instance = BoxInstance.getInstance();
		String withtag = "updatestatus";
		System.out.println("[TEST][TestSendCommon][msgId:"+withtag+"]");
		instance.proxyInterface(this,APPKEY,url, params,RequestType.POST,ServerType.weiboPlatform, null, null,120);
		Thread.sleep(15000); // 用来保证接收到消息的回执
    }

    @Override
	public void onSuccess(BoxResult boxResult) {
		System.out.println("[onFailed][请求成功]");
		System.out.println("调用返回：" + boxResult.result);
	}

	@Override
	public void onFailed(ErrorInfo errorInfo) {
		System.out.println("[onFailed][请求失败]");
		System.out.println("info:"+errorInfo.info);
		System.out.println("errorType:"+ errorInfo.errorType);
	}


	@Override
	public void onFile(String fileId, HashSet<Integer> hasSuccSend, int limit) {
		
	}
}