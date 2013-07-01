package test.sdk.syncbox.wejoy;
 
import java.util.HashSet;

import com.weibo.sdk.syncbox.BoxInstance;
import com.weibo.sdk.syncbox.listener.SendListener;
import com.weibo.sdk.syncbox.type.pub.BoxResult;
import com.weibo.sdk.syncbox.type.pub.ErrorInfo;
  
import test.sdk.utils.TestCaseUtils;
import test.sdk.syncbox.func.TestLogin;

public class TestWejoy extends TestCaseUtils implements SendListener{

    private String username = "meyoutest001@sina.com";
    private String password = "meyoutest";  

	//private String requestQuery = "{\"key\":\"chatInfo\",\"gid\":\"G$1678339034$132\"}";
	//private String requestQuery = "{\"key\":\"sendWeibo\", \"text\":\"helfsdfsdf\", \"uids\":\"2421579585,2945917017,2826394387\"}";
	private String requestQuery = "{\"key\":\"groups\"}"; 
	//private String requestQuery = "{\"key\":\"bi_users\", \"listId\":\"3473541883930864\"}";  
	//private String requestQuery = "{\"key\":\"userInfo\", \"uid\":\"2409913037\"}"; // 2132971593，2409913037
	//private String requestQuery = "{\"key\":\"statusInfo\", \"statusId\":\"3558609156260559\"}";
	
	public void testGroups() throws Exception{
		String requestQuery = "{\"key\":\"groups\"}"; 
		sendWejoy("Groups",requestQuery);
	}
	
    /** 创建群,加成员,群名称,群系统消息, 同步创建分组,在分组下发分组定向微博*/
//	public void testCreateGroup() throws Exception{
//		String requestQuery = "{\"key\":\"sendWeibo\", \"text\":\"testCreateGroup\", \"uids\":\"2421579585,2945917017,2826394387\"}";
//		sendWejoy("createGroup",requestQuery);
//	}

//	public void testAddGMember() throws Exception{
//		String requestQuery = "{\"key\":\"addGroupMember\", \"gid\":\"G$2945912633$3571048941357848\", \"members\":\"3243787713\"}";
//		sendWejoy("addMembersGroup",requestQuery);
//	}
	
//	public void testDelGMember() throws Exception{
//		String requestQuery = "{\"key\":\"delGroupMember\", \"gid\":\"G$2945912633$3571048941357848\", \"members\":\"3243787713\"}";
//		sendWejoy("delMembersGroup",requestQuery);
//	}
	
	public void testDelGroup() throws Exception{
		String requestQuery = "{\"key\":\"quitGroup\", \"gid\":\"G$2945912633$3575722453081233\"}";
		sendWejoy("delGroup",requestQuery);
	}
	
    public void sendWejoy(String requestTag,String requestQuery) throws Exception{
    	TestLogin testLogin = new TestLogin(username, password);
    	testLogin.initAndLogin();
    	BoxInstance instance = BoxInstance.getInstance();
		String withtag = "updatestatus";
		System.out.println("[TEST][TestSendCommon][msgId:"+withtag+"]");
		instance.wejoyInterface(this,requestQuery,120);
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
	public void onFile(String fileId, HashSet<Integer> hasSucc, int limit) {
		// TODO Auto-generated method stub
		
	}
}