package com.abstractwombat.loglibrary;

import java.util.Vector;

public class SMSByContact {
	public String contactKey;
	public boolean isContact;
	Vector<SMS> smses;
	SMSByContact(String contactKey){
		this.contactKey = contactKey;
		smses = new Vector<SMS>();
	}
	@Override
	public boolean equals(Object o) {
		return ((SMSByContact)o).contactKey.equals(this.contactKey);
	}
}