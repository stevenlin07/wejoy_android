package com.weibo.sdk.syncbox.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import javax.crypto.Cipher;

import com.weibo.sdk.syncbox.type.pub.SdkEvent;

/**
 * @ClassName: RsaUtil
 * @Description: 认证加密管理工具
 * @author LiuZhao
 * @date 2012-9-6 上午11:24:18
 */
public final class RsaTool {

	private char[] HEXCHAR = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
			'a', 'b', 'c', 'd', 'e', 'f'};
	private static LogModule log = LogModule.INSTANCE;
	private RsaTool() {

	}

	/**
	 * 对密码使用RSA公钥进行加密
	 *
	 * @param password
	 * @return
	 */
	public static String encryptPassword(String password) {
		String encryptPass = null;
		RsaTool rsaTool = new RsaTool();
		RSAPublicKey publicKey = rsaTool.loadPublicKey();
		if (null != publicKey) {
			encryptPass = rsaTool.encrypt(password.getBytes(), publicKey);
		}
		return encryptPass;
	}

	private RSAPublicKey loadPublicKey() {
		RSAPublicKey publicKey = null;
		InputStream is = this.getClass().getClassLoader()
				.getResourceAsStream("public.pem");
		BufferedReader br = new BufferedReader(new InputStreamReader(is));
		String readLine = null;
		StringBuilder sb = new StringBuilder();
		try {
			while ((readLine = br.readLine()) != null) {
				if (readLine.charAt(0) == '-') {
					continue;
				} else {
					sb.append(readLine).append('\r');
				}
			}
			byte[] buffer = Base64.decodeBase64(sb.toString());
			KeyFactory keyFactory = KeyFactory.getInstance("RSA");
			X509EncodedKeySpec keySpec = new X509EncodedKeySpec(buffer);
			publicKey = (RSAPublicKey) keyFactory.generatePublic(keySpec);
		} catch (Exception e) {
			log.error("[loadPublicKey][加密出错]",SdkEvent.CODE_ENCRYPT_ERROR, e);
		} finally {
			try {
				br.close();
				is.close();
			} catch (IOException e) {
				log.error("[loadPublicKey][加密文件关闭错误]",SdkEvent.CODE_ENCRYPT_ERROR, e);
			}
		}
		return publicKey;
	}

	private String encrypt(byte[] plainTextData, RSAPublicKey publicKey) {
		String encryptResult = null;
		Cipher cipher = null;
		try {
			cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding"); // 注意加密方式的选择，因为android上采用的虚拟机不同
			cipher.init(Cipher.ENCRYPT_MODE, publicKey);
			byte[] output = cipher.doFinal(plainTextData);
			encryptResult = toHexString(output);
		} catch (Exception e) {
			log.error("[encrypt][加密出错]",SdkEvent.CODE_ENCRYPT_ERROR, e);
		}
		return encryptResult;
	}

	/**
	 * 将字节数组转换成十六进制字符串
	 *
	 * @param b 字节数组
	 * @return 十六进制字符串
	 */
	private String toHexString(byte[] b) {
		StringBuilder sb = new StringBuilder(b.length * 2);
		for (int i = 0; i < b.length; i++) {
			sb.append(HEXCHAR[(b[i] & 0xf0) >>> 4]);
			sb.append(HEXCHAR[b[i] & 0x0f]);
		}
		return sb.toString();
	}
}
