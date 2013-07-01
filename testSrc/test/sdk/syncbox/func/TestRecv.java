package test.sdk.syncbox.func;
  

import test.sdk.utils.TestCaseUtils;

public class TestRecv extends TestCaseUtils{

    private String username = "meyoutest002@sina.cn";
    private String password = "meyoutest";
    private String touid = "2945912633";

    public void testSendText() throws Exception{
    	TestLogin testLogin = new TestLogin(username, password);
    	testLogin.initAndLogin(); 
		Thread.sleep(50000000); 
    }
   
}