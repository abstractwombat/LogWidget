package com.abstractwombat.loglibrary;
import android.graphics.Bitmap;

import java.lang.Comparable;
import java.lang.Long;

public class SMS implements Comparable<SMS>{
	public long date;
	public String number;
	public String message;
	public boolean incoming;
    public String name;
    public String contactLookupKey;
    public Bitmap bitmap;
    public SMS(){
        date=0;
        number="";
        message="";
        incoming=true;
        name="";
        contactLookupKey="";
    }
    SMS(SMS toCopy){
        this.date = toCopy.date;
        this.number = toCopy.number;
        this.message = toCopy.message;
        this.incoming = toCopy.incoming;
        this.name = toCopy.name;
        this.contactLookupKey = toCopy.contactLookupKey;
        this.bitmap = toCopy.bitmap;
    }
    public boolean valid(){
        return this.date > 0;
    }
	public int compareTo(SMS sms) {
		return (new Long(this.date)).compareTo(((SMS)sms).date);
	}
}