package com.wejoy.ui.adapter;

public class GroupItem {
	public String groupName;
	public String groupIconURL;
	public int groupNo;
	
	public GroupItem(String groupName, String groupIconURL, int groupNo) {
		this.groupName = groupName;
		this.groupIconURL = groupIconURL;
		this.groupNo = groupNo;
	}
	
	public String getGroupName(){
		return groupName;
	}
	
	public void setGroupName(String groupName){
		this.groupName = groupName;
	}
	
	public String getGroupIconURL(){
		return groupIconURL;
	}
	
	public void setGroupIconURL(String groupIconURL){
		this.groupIconURL = groupIconURL;
	}
	
	public int getGroupNo(){
		return groupNo;
	}
	
	public void setGroupNo(int groupNo){
		this.groupNo = groupNo;
	}
}
