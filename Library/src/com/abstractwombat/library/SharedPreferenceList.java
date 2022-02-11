package com.abstractwombat.library;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

public class SharedPreferenceList {
	private static final String TAG = "SharedPreferenceList";

	public SharedPreferenceList(Context context, String name) {
		if (name == null) return;
		sp = context.getSharedPreferences(name, context.MODE_MULTI_PROCESS);
		this.context = context;
	}

	public void setDelimeter(String delimeter) {
		this.delimeter = delimeter;
	}

	public String[] get(String key) {
		Log.v(TAG, "Get: " + key);
		String[] output = null;
		String list = sp.getString(key, "");
		if (list != null && !list.equals("")) {
			output = list.split(";");
		}
		Log.v(TAG, "Get: " + key + " = " + list);
		if (output == null) return new String[0];
		else return output;
	}

	public void set(String key, String[] values) {
		String listString = "";
		for (String i : values) {
			listString += i + ";";
		}
		listString = listString.substring(0, listString.length() - 1);
		SharedPreferences.Editor editor = sp.edit();
		editor.putString(key, listString);
		editor.commit();
		Log.v(TAG, "Set: " + key + " = " + listString);
	}

	public boolean contains(String key, String valueToFind) {
		Log.v(TAG, "Contains: " + valueToFind + " in " + key);
		String[] list = get(key);
		if (list == null)
			return false;
		for (String i : list) {
			Log.v(TAG, "Contains -> Searching " + i);
			if (i.equals(valueToFind))
				return true;
		}
		return false;
	}
	
	public void remove(String key) {
		SharedPreferences.Editor editor = sp.edit();
		editor.remove(key);
		editor.commit();	
	}
	
	public void removeFrom(String key, String valueToRemove) {
		String listString = "";
		String[] list = get(key);
		if (list == null)
			return;
		for (String i : list) {
			if (!i.equals(valueToRemove)) {
				listString += i + ";";
			}
		}
		if (!listString.isEmpty()) {
			listString = listString.substring(0, listString.length() - 1);
		}
		SharedPreferences.Editor editor = sp.edit();
		editor.putString(key, listString);
		boolean removed = editor.commit();
		if (!removed){
            Log.e(TAG, "Failed to removing [" + valueToRemove + "] " + " from [" + key + "] (state:" + listString + ")");
            //Toast.makeText(context, "ERROR!\nFailed to remove " + valueToRemove + " from SharedPreferenceList " + key, Toast.LENGTH_LONG).show();
        }

		Log.v(TAG, "RemoveFrom: " + key + " = " + listString + "(removed: "
				+ valueToRemove + ")");
	}

	public void addTo(String key, String valueToAdd) {
		String[] list = get(key);
		String listString = "";
		if (list == null) {
			listString = valueToAdd;
		} else {
			boolean foundMe = false;
			for (String p : list) {
				listString += p + ";";
				if (p.equals(valueToAdd))
					foundMe = true;
			}
			if (!foundMe) {
				listString += valueToAdd;
			} else {
				listString = listString.substring(0, listString.length() - 1);
			}
		}
		SharedPreferences.Editor editor = sp.edit();
		editor.putString(key, listString);
		editor.commit();

//		CharSequence text ="Adding [" + valueToAdd + "] " + " to [" + key + "] (state:"
//				+ listString + ")";
//		Toast.makeText(context, text, Toast.LENGTH_SHORT).show();

		Log.v(TAG, "AddTo: " + key + " = " + listString + "(added: "
				+ valueToAdd + ")");
	}

	private Context context;
	private SharedPreferences sp;
	private String delimeter;
}
