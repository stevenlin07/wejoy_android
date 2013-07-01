package com.wejoy.module;

import java.util.Comparator;

import net.sourceforge.pinyin4j.format.exception.BadHanyuPinyinOutputFormatCombination;

import com.wejoy.util.DebugUtil;
import com.wejoy.util.HanziToPingtin;

public class ContactModule {
	public static final int POS_TOP = 0x1000;
	public static final int POS_FOLDER = 0x0F00;
	public static final int CONTACT_TYPE_GROUP = 0x0100;
	public static final int CONTACT_TYPE_SINGLE = 0x0010;
	public static final int CONTACT_TYPE_ADD_FAKE_MEMBER = 0xffff;
	public static final int CONTACT_TYPE_MIXED = 0x0001;
	public static final int CONTACT_TYPE_NEW_CONV = 0x0002;
	
	public int _id; // _id是sqlite数据库主键
	public String contactId;
	public String name;
	public String faceurl;
	public int contacttype;
	public int position;
	public String description;
	public int memberCount;
	public String extend;
	
	public boolean isSelected = false;
	
	public static ContactModule getAddBtn() {
		ContactModule addbtn = new ContactModule();
		addbtn.contacttype = ContactModule.CONTACT_TYPE_ADD_FAKE_MEMBER;
		addbtn.name = "";
		return addbtn;
	}
	
	public void copyFrom(ContactModule newcontact) {
		_id = newcontact._id; // _id是sqlite数据库主键
		contactId = newcontact.contactId;
		name = newcontact.name;
		faceurl = newcontact.faceurl;
		contacttype = newcontact.contacttype;
		position = newcontact.position;
		description = newcontact.description;
		memberCount = newcontact.memberCount;
	}
	
	public static class ContactComparator implements Comparator {
		public int compare(Object arg0, Object arg1) {
			ContactModule c0 = (ContactModule) arg0;
			ContactModule c1 = (ContactModule) arg1;
			
			if(c0.contactId != null && c1.contactId != null) {
				return c0.contactId.compareTo(c1.contactId);
			}
			
			return 1;
		}
	}
}
