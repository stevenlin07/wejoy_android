package com.wejoy.service;

import java.util.Map;

import com.wejoy.util.JsonBuilder;

/**
 * Op types are copied from wejoy service
 * @author WeJoy Group
 *
 */
public enum WeJoyServiceOpType {
	chatInfo(0),
	createGroupChat(1),
	register(2),
	groups(3),
	bi_users(4),
	userInfo(5),
	statusInfo(6),
	groupMembers(7),
	
	addGroupMember(20),
	delGroupMember(21),
	quitGroup(22),
	chatMembers(23),
	
	UNKNOWN(99);

	private final int value;

	private WeJoyServiceOpType(int value) {
		this.value = value;
	}

	public int get() {
		return value;
	}
	
	public static WeJoyServiceOpType valueOf(int value){
		for( WeJoyServiceOpType type: WeJoyServiceOpType.values() ){
			if ( value == type.value ) return type;
		}
		return UNKNOWN;
	}
	
	public static WeJoyServiceOpType valueOfStr(String value){
		for( WeJoyServiceOpType type: WeJoyServiceOpType.values() ){
			if ( value.equals(type.name()) ) return type;
		}
		return UNKNOWN;
	}
	
	public static String parseToJson(WeJoyServiceOpType type, Map<String, String> params) {
		JsonBuilder json = new JsonBuilder();
		json.append("key", type.name());
		
		if(params != null && !params.isEmpty()) {
			for(Map.Entry<String, String> entry : params.entrySet()) {
				json.append(entry.getKey(), entry.getValue());
			}
		}
		
		json.flip();
		return json.toString();
	}
}
