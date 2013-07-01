package com.hzmc.testaudio;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * 
 * @author xzm 
 * Just For Test. receive the stream from client
 */
public class TestServer {

	private TestCallbk mTc;
	private Thread serv;
	private ServerSocket server;
	private Socket mClient;
	private int mPort = 1200;
	private boolean mExit = false;
	private static TestServer instance;

	public static TestServer getServer(TestCallbk tc) {
		if (instance == null)
			instance = new TestServer(tc);
		return instance;
	}

	public TestServer(TestCallbk tc) {
		mTc = tc;
		serv = new Thread(new Runnable() {

			@Override
			public void run() {
				try {

					server = new ServerSocket(mPort);
					Socket client = server.accept();

					BufferedReader in = new BufferedReader(
							new InputStreamReader(client.getInputStream()));
					PrintWriter out = new PrintWriter(client.getOutputStream());
					while (true) {
						String str = in.readLine();
						// System.out.println(str);
						System.out.print("has receive....");
						out.flush();
						if (mExit)
							break;
					}
					client.close();

				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
		serv.start();

		new Thread(new Runnable() {

			@Override
			public void run() {
				// TODO Auto-generated method stub
				try {
					mClient = new Socket(InetAddress.getLocalHost(), mPort);
					if (null != mTc)
						mTc.connect(mClient);
				} catch (UnknownHostException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}).start();
	}

	public void stopServer() {
		mExit = true;
	}

	public static interface TestCallbk {
		public void connect(Socket socket);
	}
}
