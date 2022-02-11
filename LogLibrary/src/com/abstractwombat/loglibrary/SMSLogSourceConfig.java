package com.abstractwombat.loglibrary;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;

public class SMSLogSourceConfig extends LogSourceConfig {
	public boolean showImage;
	public boolean showName;
	public boolean showIncoming;
	public boolean showOutgoing;
	public boolean showMMS;
	public boolean longDataFormat;
	public int maxLines;
	public int rowColor;
	public int textColor;
	public int bubbleResource;
	public int bubbleResourceOutgoing;
	public int bubbleColor;
	public boolean showEmblem;
	public String bubbleResourceName;
	public float textSize;
	public static float DEFAULT_TEXT_SIZE = 15.f;
	public static int DEFAULT_BUBBLE_RESOURCE = R.drawable.msg_bubble_left;
	public static int DEFAULT_BUBBLE_RESOURCE_OUTGOING = R.drawable.msg_bubble_right;
	public static int DEFAULT_BUBBLE_COLOR = Color.WHITE;

	public SMSLogSourceConfig(){
		super();
		this.showImage = true;
		this.showName = true;
		this.showIncoming = true;
		this.showOutgoing = false;
		this.showMMS = false;
		this.longDataFormat = false;
		this.maxLines = 4;
		this.rowColor = Color.TRANSPARENT;
		this.textColor = Color.BLACK;
		this.bubbleResource = DEFAULT_BUBBLE_RESOURCE;
		this.bubbleResourceOutgoing = DEFAULT_BUBBLE_RESOURCE_OUTGOING;
		this.bubbleColor = DEFAULT_BUBBLE_COLOR;
		this.bubbleResourceName = "";
		this.textSize = DEFAULT_TEXT_SIZE;
		this.showEmblem = false;
	}
	public SMSLogSourceConfig(LogSourceConfig config){
		this.sourceID = config.sourceID;
		this.groupID = config.groupID;
		this.count = config.count;
		this.lookupKeyFilter = config.lookupKeyFilter;
		this.showImage = true;
		this.showName = true;
		this.showIncoming = true;
		this.showOutgoing = false;
		this.showMMS = false;
		this.longDataFormat = false;
        this.maxLines = 4;
		this.rowColor = Color.TRANSPARENT;
		this.textColor = Color.BLACK;
		this.bubbleResource = DEFAULT_BUBBLE_RESOURCE;
		this.bubbleResourceOutgoing = DEFAULT_BUBBLE_RESOURCE_OUTGOING;
		this.bubbleColor = DEFAULT_BUBBLE_COLOR;
		this.bubbleResourceName = "";
		this.textSize = DEFAULT_TEXT_SIZE;
		this.showEmblem = false;
	}
	public SMSLogSourceConfig(String sourceID, int groupID, Context context){
		super(sourceID, groupID, 5);
        this.setToDefaults(context);
	}

    public void setToDefaults(Context context){
        Resources res = context.getResources();
        this.count = res.getInteger(R.integer.sms_log_source_default_count);
        this.showImage = res.getBoolean(R.bool.sms_log_source_default_showimage);
        this.showName = res.getBoolean(R.bool.sms_log_source_default_showname);
        this.showIncoming = res.getBoolean(R.bool.sms_log_source_default_showincoming);
		this.showOutgoing = res.getBoolean(R.bool.sms_log_source_default_showoutgoing);
		this.showMMS = res.getBoolean(R.bool.sms_log_source_default_showmms);
        this.longDataFormat = res.getBoolean(R.bool.sms_log_source_default_longdate);
        this.maxLines = res.getInteger(R.integer.sms_log_source_default_maxlines);
		this.rowColor = res.getColor(R.color.sms_log_source_default_rowcolor);
		this.textColor = res.getColor(R.color.sms_log_source_default_textcolor);
		this.bubbleResource = DEFAULT_BUBBLE_RESOURCE;
		this.bubbleResourceOutgoing = DEFAULT_BUBBLE_RESOURCE_OUTGOING;
		this.bubbleColor = DEFAULT_BUBBLE_COLOR;
		this.bubbleResourceName = "";
		this.textSize = DEFAULT_TEXT_SIZE;
		this.showEmblem = res.getBoolean(R.bool.sms_log_source_default_showemblem);
	}

	@Override
	public String serialize(){
		String base = super.serialize();

		base += this.delimiter;
		if (this.showImage) base += "1";
		else base += "0";

		base += this.delimiter;
		if (this.showName) base += "1";
		else base += "0";

		base += this.delimiter;
		if (this.showIncoming) base += "1";
		else base += "0";

		base += this.delimiter;
		if (this.showOutgoing) base += "1";
		else base += "0";

		base += this.delimiter;
		if (this.showMMS) base += "1";
		else base += "0";

		base += this.delimiter;
		if (this.longDataFormat) base += "1";
		else base += "0";

        base += this.delimiter;
        base += maxLines;

		base += this.delimiter;
		base += rowColor;

		base += this.delimiter;
		base += textColor;

		base += this.delimiter;
		base += bubbleResource;

		base += this.delimiter;
		base += bubbleResourceOutgoing;

		base += this.delimiter;
		base += bubbleColor;

		base += this.delimiter;
		base += bubbleResourceName;

		base += this.delimiter;
		base += showEmblem ? "1" : "0";

		base += this.delimiter;
		base += textSize;

		return base;
	}
	@Override
	public int unserialize(String s){
		int baseCount = super.unserialize(s);
		if (baseCount == 0){
			return 0;
		}

		String[] a = split(s, this.delimiter, 0);
		if (a.length < baseCount+1) {
			return baseCount;
		}
		int i=baseCount;
		
		if (a[i++].equals("1")) this.showImage = true;
		else this.showImage = false;
		
		if (a[i++].equals("1")) this.showName = true;
		else this.showName = false;

		if (a[i++].equals("1")) this.showIncoming = true;
		else this.showIncoming = false;

		if (a[i++].equals("1")) this.showOutgoing = true;
		else this.showOutgoing = false;

		if (i+1 >= a.length) {
			this.showMMS = false;
			if (a[i++].equals("1")) this.longDataFormat = true;
			else this.longDataFormat = false;
		} else {
			if (a[i++].equals("1")) this.showMMS = true;
			else this.showMMS = false;

			if (a[i++].equals("1")) this.longDataFormat = true;
			else this.longDataFormat = false;
		}

        if (a.length > i){
            this.maxLines = Integer.parseInt(a[i++]);
        }

		if (a.length > i){
			this.rowColor = Integer.parseInt(a[i++]);
		}

		if (a.length > i){
			this.textColor = Integer.parseInt(a[i++]);
		}

		if (a.length > i){
			this.bubbleResource = Integer.parseInt(a[i++]);
		}

		if (a.length > i){
			this.bubbleResourceOutgoing = Integer.parseInt(a[i++]);
		}

		if (a.length > i){
			this.bubbleColor = Integer.parseInt(a[i++]);
		}

		if (a.length > i){
			this.bubbleResourceName = a[i++];
		}

		if (a.length > i){
			this.showEmblem = a[i++].equals("1");
		}

		if (a.length > i){
			this.textSize = Float.parseFloat(a[i++]);
		}

		return i;
	}

    @Override
    public String getSummary(){
        String o = "" + this.count + " items -  Showing ";
        if (showIncoming && showOutgoing){
            o += "all ";
        }else if (showIncoming){
            o += "received ";
        }else if (showOutgoing){
            o += "sent ";
        }
		if (showMMS) {
			o += " and mms";
		}
        return o + "messages";
    }
}
