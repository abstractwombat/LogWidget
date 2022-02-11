package com.abstractwombat.loglibrary;

import java.util.Vector;

public class CallsByContact{
	public String contactKey;
	public boolean isContact;
	public Vector<Call> calls;
	CallsByContact(String contactKey){
		this.contactKey = contactKey;
		this.calls = new Vector<Call>(); 
	};
	@Override
	public boolean equals(Object o) {
		return ((CallsByContact)o).contactKey.equals(this.contactKey);
	}
}

