package test.sdk.syncbox.func;
 
import java.text.SimpleDateFormat;
import java.util.HashSet;
import java.util.Locale;

import com.weibo.sdk.syncbox.BoxInstance;
import com.weibo.sdk.syncbox.listener.SendListener;
import com.weibo.sdk.syncbox.type.pub.BoxResult;
import com.weibo.sdk.syncbox.type.pub.ConvType;
import com.weibo.sdk.syncbox.type.pub.ErrorInfo;

import test.sdk.utils.TestCaseUtils;

public class TestSendText extends TestCaseUtils implements SendListener  {

	private FakeWait fakeWait = new FakeWait();
    private String username = "meyoutest001@sina.com";
    private String password = "meyoutest";
    private String touid = "2945917017";

    public void testSendText() throws Exception{
    	TestLogin testLogin = new TestLogin(username, password);
    	testLogin.initAndLogin();
    	BoxInstance boxInstance = BoxInstance.getInstance();
		boxInstance.sendText(this, touid,"syncboxtest:0",ConvType.SINGLE,120);
		fakeWait.startWaitResult();
    }

	@Override
	public void onSuccess(BoxResult boxResult) {
		System.out.println("[onFailed][请求成功]");
		System.out.println("服务端下发的msgId：" + boxResult.msgId);
		String timestr = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss",Locale.SIMPLIFIED_CHINESE)
				.format(new java.util.Date(boxResult.timestamp));
		System.out.println("操作时间：" + timestr);
		fakeWait.setSucc();
	}

	@Override
	public void onFailed(ErrorInfo errorInfo) {
		System.out.println("[onFailed][请求失败]");
		System.out.println("info:"+errorInfo.info);
		System.out.println("errorType:"+ errorInfo.errorType);
		fakeWait.setFail();
	}

	@Override
	public void onFile(String fileId, HashSet<Integer> hasSuccSend, int limit) {
		
	} 
}