package test.sdk.syncbox.func;
 
import java.util.HashSet;

import com.weibo.sdk.syncbox.BoxInstance;
import com.weibo.sdk.syncbox.listener.SendListener;
import com.weibo.sdk.syncbox.type.pub.BoxResult;
import com.weibo.sdk.syncbox.type.pub.ErrorInfo;

import test.sdk.utils.TestCaseUtils; 

public class TestGetFile extends TestCaseUtils implements SendListener{

    private String username = "meyoutest002@sina.cn";
    private String password = "meyoutest";
    private String fileId = "2945912633-2945917017-1361976047485";
    private String downloadPath = "/Volumes/resource/source/meyou/meyou_sdk/syncbox_sdk_java/testSrc/imageReceive.png";
    private int fileLength = 185787;
    
    public void testSend() throws Exception{
    	TestLogin testLogin = new TestLogin(username, password);
    	testLogin.initAndLogin();
    	BoxInstance instance = BoxInstance.getInstance();
    	int limit = 4;
    	instance.getFile(this,fileId, downloadPath, fileLength, limit, 120);
		Thread.sleep(10000); 
    }

	@Override
	public void onSuccess(BoxResult boxResult) {
		System.out.println("文件fileId:"+boxResult.msgId+" 发送成功！");
	}

	@Override
	public void onFailed(ErrorInfo errorInfo) {
		System.out.println("[onFailed][请求失败]");
		System.out.println("info:"+errorInfo.info);
		System.out.println("errorType:"+ errorInfo.errorType);
	}

	@Override
	public void onFile(String fileId, HashSet<Integer> hasSuccSend, int limit) {
		System.out.println("生成的文件ID：" + fileId);
		System.out.println("该文件的分片总数：" + limit);
		System.out.println("收到的分片号：" + hasSuccSend.toString());
		double completed = (double)(hasSuccSend.size()) / (double)limit;

		if (hasSuccSend.size() == limit) { 
			// TODO 文件传输完成后要向接收方发送消息通知其获取该文件
		}
		System.out.println("下载文件:"+fileId+" 的完成度:" + completed * 100 + "%");
	}
}
