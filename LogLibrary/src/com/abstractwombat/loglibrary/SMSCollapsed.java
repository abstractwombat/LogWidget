package com.abstractwombat.loglibrary;

import java.util.ArrayList;

public class SMSCollapsed extends SMS{
    public ArrayList<SMS> collapsed;
    SMSCollapsed(){
        super();
        collapsed = new ArrayList<SMS>();
    }
    SMSCollapsed(SMS sms){
        super(sms);
        collapsed = new ArrayList<SMS>();
    }
    SMSCollapsed(SMSCollapsed sms){
        super(sms);
        collapsed = sms.collapsed;
    }

    public void push(SMS s){
        if (this.valid()){
            this.collapsed.add(s);
        }else{
            this.set(s);
        }
    }
    private void set(SMS s){
        this.date = s.date;
        this.number = s.number;
        this.message = s.message;
        this.incoming = s.incoming;
        this.name = s.name;
        this.contactLookupKey = s.contactLookupKey;
    }
}
