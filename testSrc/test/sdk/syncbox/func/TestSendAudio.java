package test.sdk.syncbox.func;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.HashSet;
import java.util.Locale;

import com.weibo.sdk.syncbox.BoxInstance;
import com.weibo.sdk.syncbox.listener.SendListener;
import com.weibo.sdk.syncbox.type.pub.BoxResult;
import com.weibo.sdk.syncbox.type.pub.ConvType;
import com.weibo.sdk.syncbox.type.pub.ErrorInfo;

import test.sdk.utils.TestCaseUtils;

public class TestSendAudio extends TestCaseUtils implements SendListener{

    private String username = "meyoutest001@sina.com";
    private String password = "meyoutest";
    private String touid = "2945917017";
    private String audioPath = "/Volumes/resource/source/meyou/meyou_sdk/syncbox_sdk_java/testSrc/audioStyle.amr";

    public void testSendAudio() throws Exception{
    	TestLogin testLogin = new TestLogin(username, password);
    	testLogin.initAndLogin();
    	BoxInstance instance = BoxInstance.getInstance();
		byte[] audioData = readFile(audioPath); 
		instance.sendAudio(this, touid,"spanId",1, true, audioData, ConvType.SINGLE, 120);
		Thread.sleep(50000); // 用来保证接收到消息的回执 
    }

    @Override
	public void onSuccess(BoxResult boxResult) {
		System.out.println("[onFailed][请求成功]");
		System.out.println("是否是最后一片：" + boxResult.isLastSlice);
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
	
	/**
	 * 读取文件的帮助
	 * @param filepath
	 * @return
	 */
	public static byte[] readFile(String filepath){
		FileInputStream fis = null;
		byte[] fileData = null;
		try {
			fis = new FileInputStream(new File(filepath));
			fileData = new byte[fis.available()];
			fis.read(fileData);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if(null != fis){
				try {
					fis.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return fileData;
	}
}