package com.wejoy.ui.helper;

import java.util.Comparator;

import com.wejoy.module.ChatMessage;

public class ComparatorChatMessage implements Comparator<ChatMessage> {

	@Override
	public int compare(ChatMessage lhs, ChatMessage rhs) {
		
		int flag = ((Long)lhs.getCreatedAt()).compareTo(rhs.getCreatedAt());
		return flag;
	}

}