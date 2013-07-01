package com.wejoy.module;

import java.util.List;

/**
 * 
 * @author WeJoy Group
 *
 */
public class GroupModule {
	public String groupId;
	public String groupName;
	public String groupDescription;
	public String memberCount;
	
	public UserModule groupOwner;
	public List<UserModule> groupMembers;
}
