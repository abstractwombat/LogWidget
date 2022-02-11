package com.abstractwombat.loglibrary;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.abstractwombat.images.ImageUtilities;

import java.util.ArrayList;

public class LogSourceConfig {
	public String sourceID;
	public int groupID;
	public int count;
    public int theme;
	public String[] lookupKeyFilter;

    public static int defaultTheme;

	public LogSourceConfig(){
		this.sourceID = "";
		this.groupID = -1;
		this.count = -1;
        this.theme = defaultTheme;
		this.lookupKeyFilter = null;
	}
	public LogSourceConfig(String sourceID, int groupID, int count){
		this.sourceID = sourceID;
		this.groupID = groupID;
		this.count = count;
        this.theme = defaultTheme;
		this.lookupKeyFilter = null;
	}
	@Override
	public boolean equals(Object o) {
		return ((LogSourceConfig)o).sourceID.equals(this.sourceID);
	}
	
	protected final String delimiter = "%delimiter%";
	public String serialize(){
		String s = sourceID;
		s += this.delimiter + Integer.toString(groupID);
		s += this.delimiter + Integer.toString(count);
        s += this.delimiter + Integer.toString(theme);
		if (this.lookupKeyFilter != null){
			s += this.delimiter + this.lookupKeyFilter.length;
			for (String k : this.lookupKeyFilter){
				s += this.delimiter + k;
			}
		}else{
			s += this.delimiter + "0";
		}
		return s;
	}
	public int unserialize(String s){
		String[] a = split(s, this.delimiter, 0);
		int i=0;
		this.sourceID = a[i++];
		this.groupID = Integer.parseInt(a[i++]);
		this.count = Integer.parseInt(a[i++]);
        this.theme = Integer.parseInt(a[i++]);
		int lookupKeyFilterCount = Integer.parseInt(a[i++]);
		if (lookupKeyFilterCount > 0){
			this.lookupKeyFilter = new String[lookupKeyFilterCount];
			for (int key=0; key<lookupKeyFilterCount; key++){
				this.lookupKeyFilter[key] = a[i++];
			}
		}
		return i;
	}

	public static String[] split(String s, String delimiter, int startIndex) {
		ArrayList<String> temp = new ArrayList<>();
		int dLength = delimiter.length();
		int last = startIndex;
		int pos = s.indexOf(delimiter, startIndex);
		while (pos > -1){
			temp.add(s.substring(last, pos));
			last = pos + dLength;
			pos = s.indexOf(delimiter, pos + dLength);
		}
		if (last < s.length()-1){
			temp.add(s.substring(last, s.length()));
		}
		return temp.toArray(new String[temp.size()]);
	}

	public static void setDefaultSpacing(Context context, int widgetID) {
		final SharedPreferences settings = context.getSharedPreferences("State", Context
				.MODE_MULTI_PROCESS);
		final String spacingKey = context.getPackageName() + "." + widgetID + ".Spacing";
		int value = context.getResources().getInteger(R.integer.default_spacing);
		int pixels = Math.round(ImageUtilities.convertDpToPixel(value));
		SharedPreferences.Editor editor = settings.edit();
		Log.d("setDefaultSpacing", "Setting spacing to " + value + "dp");
		editor.putInt(spacingKey, pixels);
		editor.apply();
	}

	public String getSummary(){
        return "";
    }
}