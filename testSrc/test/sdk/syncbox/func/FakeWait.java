package test.sdk.syncbox.func;

public class FakeWait {

	private int flag = 0;
	
	public void startWaitResult() throws InterruptedException {
		while(true) {
			Thread.sleep(500);
			if (isStop()) {
				break;
			} 
		}
	}
	
	private boolean isStop() {
		return (flag == 1 || flag == 2);
	}
	
	public void setSucc() {
		flag = 1;
	}
	
	public void setFail() {
		flag = 2;
	}
}
