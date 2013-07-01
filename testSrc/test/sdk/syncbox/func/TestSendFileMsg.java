package test.sdk.syncbox.func;
 
import java.text.SimpleDateFormat;
import java.util.HashSet;
import java.util.Locale;

import com.weibo.sdk.syncbox.BoxInstance;
import com.weibo.sdk.syncbox.listener.SendListener;
import com.weibo.sdk.syncbox.type.pub.BoxResult;
import com.weibo.sdk.syncbox.type.pub.ConvType;
import com.weibo.sdk.syncbox.type.pub.ErrorInfo;
import com.weibo.sdk.syncbox.type.pub.MetaType;

import test.sdk.utils.TestCaseUtils;

public class TestSendFileMsg extends TestCaseUtils implements SendListener{

    private String username = "meyoutest001@sina.com";
    private String password = "meyoutest";
    private String touid = "2945917017";

    private static String fileId = "2945912633-2945917017-1368083145720";
	private static String filepath = "/Volumes/resource/source/meyou/meyou_sdk/syncbox_sdk_java/testSrc/imageTest.png";
	private static String filename = "imageTest.png";
	private static byte[] thumbnail = new byte[] { 23, 55, -43, 43, 54, -76 }; // 只是用来保证传输
    
    public void testSendText() throws Exception{
    	TestLogin testLogin = new TestLogin(username, password);
    	testLogin.initAndLogin();
    	BoxInstance instance = BoxInstance.getInstance();
		instance.sendFileMsg(this, fileId, touid, ConvType.SINGLE, MetaType.image, filepath,
				filename, thumbnail, 120);
		Thread.sleep(500);
    }

    @Override
	public void onSuccess(BoxResult boxResult) {
		System.out.println("[onFailed][请求成功]");
		System.out.println("服务端下发的msgId：" + boxResult.msgId);
		String timestr = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss",Locale.SIMPLIFIED_CHINESE)
				.format(new java.util.Date(boxResult.timestamp));
		System.out.println("操作时间：" + timestr);
	}

	@Override
	public void onFailed(ErrorInfo errorInfo) {
		System.out.println("[onFailed][请求失败]");
		System.out.println("info:"+errorInfo.info);
		System.out.println("errorType:"+ errorInfo.errorType);
	}


	@Override
	public void onFile(String fileId, HashSet<Integer> hasSuccSend, int limit) {
		// TODO Auto-generated method stub
		
	}
}