package com.abstractwombat.loglibrary;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;

public class SkypeSourceConfig extends LogSourceConfig {
	public boolean showImage;
	public boolean showName;
	public boolean longDataFormat;
	public int maxLines;
	public int rowColor;
	public int textColor;
	public int bubbleResource;
	public int bubbleColor;
	public boolean showEmblem;
	public String bubbleResourceName;
	public float textSize;
	public static float DEFAULT_TEXT_SIZE = 15.f;
	public static int DEFAULT_BUBBLE_RESOURCE = R.drawable.chat_bubble_left;
	public static int DEFAULT_BUBBLE_COLOR = Color.WHITE;

	public SkypeSourceConfig(){
		super();
		this.showImage = true;
		this.showName = true;
		this.longDataFormat = false;
		this.maxLines = 4;
		this.rowColor = Color.TRANSPARENT;
		this.textColor = Color.BLACK;
		this.bubbleResource = DEFAULT_BUBBLE_RESOURCE;
		this.bubbleColor = DEFAULT_BUBBLE_COLOR;
		this.bubbleResourceName = "";
		this.textSize = DEFAULT_TEXT_SIZE;
		this.showEmblem = false;
	}
	public SkypeSourceConfig(LogSourceConfig config){
		this.sourceID = config.sourceID;
		this.groupID = config.groupID;
		this.count = config.count;
		this.lookupKeyFilter = config.lookupKeyFilter;
		this.showImage = true;
		this.showName = true;
		this.longDataFormat = false;
		this.maxLines = 4;
		this.rowColor = Color.TRANSPARENT;
		this.textColor = Color.BLACK;
		this.bubbleResource = DEFAULT_BUBBLE_RESOURCE;
		this.bubbleColor = DEFAULT_BUBBLE_COLOR;
		this.bubbleResourceName = "";
		this.textSize = DEFAULT_TEXT_SIZE;
		this.showEmblem = false;
	}
	public SkypeSourceConfig(String sourceID, int groupID, Context context){
		super(sourceID, groupID, 5);
        this.setToDefaults(context);
    }

    public void setToDefaults(Context context){
        Resources res = context.getResources();
        this.count = res.getInteger(R.integer.call_log_source_default_count);
        this.showImage = res.getBoolean(R.bool.skype_source_default_showimage);
        this.showName = res.getBoolean(R.bool.skype_source_default_showname);
        this.longDataFormat = res.getBoolean(R.bool.skype_source_default_longdate);
		this.maxLines = res.getInteger(R.integer.skype_source_default_maxlines);
		this.rowColor = res.getColor(R.color.skype_source_default_rowcolor);
		this.textColor = res.getColor(R.color.skype_source_default_textcolor);
		this.bubbleResource = DEFAULT_BUBBLE_RESOURCE;
		this.bubbleColor = DEFAULT_BUBBLE_COLOR;
		this.bubbleResourceName = "";
		this.textSize = DEFAULT_TEXT_SIZE;
		this.showEmblem = res.getBoolean(R.bool.skype_source_default_showemblem);
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

		if (a[i++].equals("1")) this.longDataFormat = true;
		else this.longDataFormat = false;

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
        String o = "" + this.count + " items - ";
        return o;
    }
}
