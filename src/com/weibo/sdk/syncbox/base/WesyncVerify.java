package com.weibo.sdk.syncbox.base;

import java.util.HashSet;

import com.weibo.sdk.syncbox.type.pub.ConvType;
import com.weibo.sdk.syncbox.type.pub.MetaType;
import com.weibo.sdk.syncbox.utils.VerifyModule;

public class WesyncVerify extends VerifyModule {

	/** sendText和sendMixedText 接口参数校验.true 校验成功*/
	public static boolean sendTextVerify(String withtag, String touid, String text, ConvType convType) {
		if (!metaVerify(withtag, touid, convType) || !stringVerify(withtag,"text", text)) {
			return false;
		}
		return true;
	}
	
	/** sendText和sendMixedText 接口参数校验.true 校验成功*/
	public static boolean sendFileMsgVerify(String withtag, String fileId, String touid,ConvType convType,
			MetaType metaType, String filepath, String filename) {
		if (!metaVerify(withtag, touid, convType)
				|| !stringVerify(withtag,"fileId",fileId )
				|| !stringVerify(withtag,"filepath",filepath )
				|| !stringVerify(withtag,"filename",filename )
				|| isNull(withtag,"metaType",metaType )) {
			return false;
		}
		return true;
	}

	/** sendFileAll 接口的参数校验.*/
	public static boolean sendFileAllVerify(String withtag, String touid, String filepath) {
		if (!stringVerify(withtag, "touid", touid)
				|| !stringVerify(withtag, "filepath", filepath)) { 
			return false; 
		}
		return true;
	} 
	
	/** sendFileAll 接口的参数校验.*/
	public static boolean sendFilePartVerify(String withtag, String fileId, String filepath,HashSet<Integer> hasSuccSend) {
		if (!stringVerify(withtag, "fileId", fileId)
				|| !stringVerify(withtag, "filepath", filepath)
				|| isNull(withtag, "hasSuccSend", hasSuccSend)) { 
			return false; 
		}
		return true;
	} 
	
	/** getFileAll 接口的参数校验.*/
	public static boolean getFileAllVerify(String withtag, String fileId, String filepath, int fileLength) {
		if (!stringVerify(withtag, "fileId", fileId)
				|| !stringVerify(withtag, "filepath", filepath)
				|| isZero(withtag, "fileLength", fileLength)) { 
			return false; 
		}
		return true;
	}

	/** getFileAll 接口的参数校验.*/
	public static boolean getFilePartVerify(String withtag, String fileId, String filepath, int fileLength,HashSet<Integer> hasSuccRecv) {
		if (!stringVerify(withtag, "fileId", fileId)
				|| !stringVerify(withtag, "filepath", filepath)
				|| isZero(withtag, "fileLength", fileLength)
				|| isNull(withtag, "hasSuccRecv", hasSuccRecv)) { 
			return false; 
		}
		return true;
	}
	
	/** 语音接口的参数校验,true 校验成功*/
	public static boolean sendAudioVerify(String withtag, String touid, byte[] audioData,
			String spanId, int spanSequenceNo, boolean endTag, ConvType convType){
		if ( !metaVerify(withtag, touid, convType) 
			|| !stringVerify(withtag, "spanId", spanId)
			|| isZero(withtag, "spanSequenceNo", spanSequenceNo)) {
			return false;
		}
		return true;
	}

	/** 校验会发送meta的接口的基本参数 */
	private static boolean metaVerify(String withtag, String touid, ConvType convType) {
		if ( stringVerify(withtag, "touid", touid)
				&& !isNull(withtag, "convType", convType)) {
			return true;
		} else {
			return false;
		}
	}
}
