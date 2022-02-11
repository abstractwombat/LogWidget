package com.abstractwombat.loglibrary;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;

public class CallLogSourceConfig extends LogSourceConfig {
	public boolean showImage;
	public boolean showName;
	public boolean showCallButton;
    public boolean showIncoming;
    public boolean showOutgoing;
    public boolean showMissed;
	public boolean longDataFormat;
	public int rowColor;
	public int textColor;
	public int bubbleResource;
	public int bubbleColor;
	public boolean showEmblem;
	public String bubbleResourceName;
	public float textSize;
	public static float DEFAULT_TEXT_SIZE = 15.f;
	public static int DEFAULT_BUBBLE_RESOURCE = R.drawable.call_box;
	public static int DEFAULT_BUBBLE_COLOR = Color.WHITE;

	public CallLogSourceConfig(){
		super();
		this.showImage = true;
		this.showName = true;
		this.showCallButton = true;
        this.showIncoming = true;
        this.showOutgoing = true;
        this.showMissed = true;
		this.longDataFormat = false;
		this.rowColor = Color.TRANSPARENT;
		this.textColor = Color.BLACK;
		this.bubbleResource = DEFAULT_BUBBLE_RESOURCE;
		this.bubbleColor = DEFAULT_BUBBLE_COLOR;
		this.bubbleResourceName = "";
		this.textSize = DEFAULT_TEXT_SIZE;
		this.showEmblem = false;
	}
	public CallLogSourceConfig(LogSourceConfig config){
		this.sourceID = config.sourceID;
		this.groupID = config.groupID;
		this.count = config.count;
		this.lookupKeyFilter = config.lookupKeyFilter;
		this.showImage = true;
		this.showName = true;
		this.showCallButton = true;
        this.showIncoming = true;
        this.showOutgoing = true;
        this.showMissed = true;
		this.longDataFormat = false;
		this.rowColor = Color.TRANSPARENT;
		this.textColor = Color.BLACK;
		this.bubbleResource = DEFAULT_BUBBLE_RESOURCE;
		this.bubbleColor = DEFAULT_BUBBLE_COLOR;
		this.bubbleResourceName = "";
		this.textSize = DEFAULT_TEXT_SIZE;
		this.showEmblem = false;
	}
	public CallLogSourceConfig(String sourceID, int groupID, Context context){
		super(sourceID, groupID, 5);
        this.setToDefaults(context);
    }

    public void setToDefaults(Context context){
        Resources res = context.getResources();
        this.count = res.getInteger(R.integer.call_log_source_default_count);
        this.showImage = res.getBoolean(R.bool.call_log_source_default_showimage);
        this.showName = res.getBoolean(R.bool.call_log_source_default_showname);
        this.showCallButton = res.getBoolean(R.bool.call_log_source_default_showcallbutton);
        this.showIncoming = res.getBoolean(R.bool.call_log_source_default_showincoming);
        this.showOutgoing = res.getBoolean(R.bool.call_log_source_default_showoutgoing);
        this.showMissed = res.getBoolean(R.bool.call_log_source_default_showmissed);
        this.longDataFormat = res.getBoolean(R.bool.call_log_source_default_longdate);
		this.rowColor = res.getColor(R.color.call_log_source_default_rowcolor);
		this.textColor = res.getColor(R.color.call_log_source_default_textcolor);
		this.bubbleResource = DEFAULT_BUBBLE_RESOURCE;
		this.bubbleColor = DEFAULT_BUBBLE_COLOR;
		this.bubbleResourceName = "";
		this.textSize = DEFAULT_TEXT_SIZE;
		this.showEmblem = res.getBoolean(R.bool.call_log_source_default_showemblem);
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
		if (this.showCallButton) base += "1";
		else base += "0";

        base += this.delimiter;
        if (this.showIncoming) base += "1";
        else base += "0";

        base += this.delimiter;
        if (this.showOutgoing) base += "1";
        else base += "0";

        base += this.delimiter;
        if (this.showMissed) base += "1";
        else base += "0";

        base += this.delimiter;
		if (this.longDataFormat) base += "1";
		else base += "0";

		base += this.delimiter;
		base += rowColor;

		base += this.delimiter;
		base += textColor;

		base += this.delimiter;
		base += bubbleResource;

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

		if (a[i++].equals("1")) this.showCallButton = true;
		else this.showCallButton = false;

        if (a[i++].equals("1")) this.showIncoming= true;
        else this.showIncoming = false;

        if (a[i++].equals("1")) this.showOutgoing= true;
        else this.showOutgoing = false;

        if (a[i++].equals("1")) this.showMissed= true;
        else this.showMissed = false;

		if (a[i++].equals("1")) this.longDataFormat = true;
		else this.longDataFormat = false;

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
        if (showIncoming && showOutgoing && showMissed){
            o += "all calls";
        }else{
            if (showIncoming) o += "incoming, ";
            if (showOutgoing) o += "outgoing, ";
            if (showMissed) o += "missed, ";
            if (o.endsWith(", ")) o = o.substring(0, o.length()-2);
            o += " calls";
        }
        return o;
    }
}
