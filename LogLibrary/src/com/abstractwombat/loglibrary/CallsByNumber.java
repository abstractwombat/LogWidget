package com.abstractwombat.loglibrary;

import java.util.Vector;

public class CallsByNumber{
	public String number;
	public Vector<Call> calls;
	CallsByNumber(String number){
		this.number = number;
		this.calls = new Vector<Call>(); 
	};
	@Override
	public boolean equals(Object o) {
		return ((CallsByNumber)o).number.equals(this.number);
	}
}