package com.wejoy.module;

import java.io.IOException;

import com.wejoy.util.JsonBuilder;
import com.wejoy.util.JsonWrapper;
/**
 * 
 * @author WeJoy Group
 *
 */
public class UserInfo {
	public String version = "10";
	
	public String faceurl;
	public String name;
	public Sex sex;
	public String constellation;
	public String desc;
	// TO DO
//	public String account
	
	public UserInfo() {
		
	}
	
	public UserInfo(ContactModule usr) {
		this.faceurl = usr.faceurl;
		this.name = usr.name;
		// TO DO
//		this.sex = 
		this.desc = usr.description;
	}
	
	public String toJson() {
		JsonBuilder json = new JsonBuilder();
		json.append("faceurl", faceurl);
		json.append("name", name);
		json.append("sex", sex.ordinal());
		json.append("constellation", constellation);
		json.append("desc", desc);
		
		return json.flip().toString();
	}
	
	public static UserInfo parseJson(String s) {
		if(s == null) {
			return null;
		}
		
		try {
			JsonWrapper json = new JsonWrapper(s);
			UserInfo us = new UserInfo();
			us.faceurl = json.get("faceurl");
			us.name = json.get("name");
			us.sex = Sex.valueOf(json.getInt("sex"));
			us.constellation = json.get("constellation");
			us.desc = json.get("desc");
			
			return us;
		} 
		catch (IOException e) {
			// ignore
		}
		
		return null;
	}
}
