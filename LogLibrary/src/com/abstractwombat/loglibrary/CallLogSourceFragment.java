package com.abstractwombat.loglibrary;

import android.content.res.Resources;
import android.os.Build;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.PreferenceCategory;
import android.util.Log;

import com.abstractwombat.library.ResourcePreference;
import com.github.danielnilsson9.colorpickerview.preference.ColorPreference;

public class CallLogSourceFragment extends ALogSourcePreferenceFragment {
	private static final String TAG = "CallLogSourceFragment";

    @Override
    protected int getPreferences() {
        return R.xml.calllogsource_preferences;
    }

    protected void storeToFactory(){
		// Get all the preferences
		EditTextPreference editCount = (EditTextPreference)findPreference("calllogsource_count");
		CheckBoxPreference chkImage = (CheckBoxPreference)findPreference("calllogsource_show_contact_image");
		CheckBoxPreference chkName = (CheckBoxPreference)findPreference("calllogsource_show_name");
		CheckBoxPreference chkBut = (CheckBoxPreference)findPreference("calllogsource_show_call_button");
        CheckBoxPreference chkIncoming = (CheckBoxPreference)findPreference("calllogsource_show_incoming");
        CheckBoxPreference chkOutgoing = (CheckBoxPreference)findPreference("calllogsource_show_outgoing");
        CheckBoxPreference chkMissed = (CheckBoxPreference)findPreference("calllogsource_show_missed");
		CheckBoxPreference chkDate = (CheckBoxPreference)findPreference("calllogsource_long_date");
		ResourcePreference bubbleStyle = (ResourcePreference)findPreference("calllogsource_bubblestyle");
		ColorPreference rowColor = (ColorPreference)findPreference("calllogsource_rowcolor");
		ColorPreference textColor = (ColorPreference)findPreference("calllogsource_textcolor");
		ColorPreference bubbleColor = (ColorPreference)findPreference("calllogsource_bubblecolor");
		CheckBoxPreference chkEmblem = (CheckBoxPreference)findPreference("calllogsource_show_emblem");
		EditTextPreference textSize = (EditTextPreference)findPreference("calllogsource_textsize");

        // Get the current config
        ALogSource source = LogSourceFactory.get(this.context, this.mLogSourceId);
        LogSourceConfig initConfig = source.config();

		// Configure the new source
		CallLogSourceConfig config = new CallLogSourceConfig(initConfig);
		Resources res = getResources();
		try {
			config.count = Integer.parseInt(editCount.getText());
		}catch (NumberFormatException e){
			config.count = res.getInteger(R.integer.call_log_source_default_count);
		}
		config.showImage = chkImage.isChecked();
		config.showName = chkName.isChecked();
		config.showCallButton = chkBut.isChecked();
        config.showIncoming = chkIncoming.isChecked();
        config.showOutgoing = chkOutgoing.isChecked();
        config.showMissed = chkMissed.isChecked();
		config.longDataFormat = chkDate.isChecked();
		config.rowColor = rowColor.getValue();
		config.textColor = textColor.getValue();
		config.bubbleResource = bubbleStyle.getValue();
		config.bubbleColor = bubbleColor.getValue();
		config.showEmblem = chkEmblem.isChecked();
		config.bubbleResourceName = context.getResources().getResourceEntryName(config.bubbleResource);
		if (textSize != null){
			float temp = config.textSize;
			try {
				config.textSize = Float.parseFloat(textSize.getText());
			}catch (NumberFormatException e){
				config.textSize = temp;
			}
		}

        // Delete the source
        LogSourceFactory.deleteSource(this.context, this.mLogSourceId);

        // Create the new source
		LogSourceFactory.newSource(this.context, CallLogSource.class, config);

		Log.d(TAG, "Stored: " + config.showImage + "," + config.showName + "," + config.showCallButton + "," + config.longDataFormat);
	}
    protected void loadFromFactory(){
		// Get all the preferences
		EditTextPreference editCount = (EditTextPreference)findPreference("calllogsource_count");
		CheckBoxPreference chkImage = (CheckBoxPreference)findPreference("calllogsource_show_contact_image");
		CheckBoxPreference chkName = (CheckBoxPreference)findPreference("calllogsource_show_name");
		CheckBoxPreference chkBut = (CheckBoxPreference)findPreference("calllogsource_show_call_button");
        CheckBoxPreference chkIncoming = (CheckBoxPreference)findPreference("calllogsource_show_incoming");
        CheckBoxPreference chkOutgoing = (CheckBoxPreference)findPreference("calllogsource_show_outgoing");
        CheckBoxPreference chkMissed = (CheckBoxPreference)findPreference("calllogsource_show_missed");
		CheckBoxPreference chkDate = (CheckBoxPreference)findPreference("calllogsource_long_date");
		ResourcePreference bubbleStyle = (ResourcePreference)findPreference("calllogsource_bubblestyle");
		ColorPreference editRowColor = (ColorPreference)findPreference("calllogsource_rowcolor");
		ColorPreference editTextColor = (ColorPreference)findPreference("calllogsource_textcolor");
		ColorPreference bubbleColor = (ColorPreference)findPreference("calllogsource_bubblecolor");
		CheckBoxPreference chkEmblem = (CheckBoxPreference)findPreference("calllogsource_show_emblem");
		EditTextPreference textSize = (EditTextPreference)findPreference("calllogsource_textsize");

		// Hide the text size
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
			PreferenceCategory cat = (PreferenceCategory) findPreference("calllogsource_category_appearance");
			cat.removePreference(textSize);
			textSize = null;
		}

		ALogSource source = LogSourceFactory.get(this.context, this.mLogSourceId);
		if (source == null){
			// Source with this ID doesn't exist, set the default values
			Resources res = getResources();
			editCount.setText(Integer.toString(res.getInteger(R.integer.call_log_source_default_count)));
			chkImage.setChecked(res.getBoolean(R.bool.call_log_source_default_showimage));
			chkName.setChecked(res.getBoolean(R.bool.call_log_source_default_showname));
			chkBut.setChecked(res.getBoolean(R.bool.call_log_source_default_showcallbutton));
            chkIncoming.setChecked(res.getBoolean(R.bool.call_log_source_default_showincoming));
            chkOutgoing.setChecked(res.getBoolean(R.bool.call_log_source_default_showoutgoing));
            chkMissed.setChecked(res.getBoolean(R.bool.call_log_source_default_showmissed));
			chkDate.setChecked(res.getBoolean(R.bool.call_log_source_default_longdate));
			bubbleStyle.setDefaultValue(CallLogSourceConfig.DEFAULT_BUBBLE_RESOURCE);
			chkEmblem.setChecked(res.getBoolean(R.bool.call_log_source_default_showemblem));
			if (textSize != null) textSize.setText(Integer.toString(res.getInteger(R.integer.call_log_source_default_textsize)));
			bubbleColor.setDefaultValue(CallLogSourceConfig.DEFAULT_BUBBLE_COLOR);
			editRowColor.setDefaultValue(Integer.toString(res.getColor(R.color
					.call_log_source_default_rowcolor)));
			editTextColor.setDefaultValue(Integer.toString(res.getColor(R.color
					.call_log_source_default_textcolor)));
			Log.d(TAG, "Loaded defaults");
		}else{
			// Set from the config
			CallLogSourceConfig config = (CallLogSourceConfig)source.config();
			if (config.count < 0){
				config.count = getResources().getInteger(R.integer.combined_source_default_count) / 2;
			}
			editCount.setText(Integer.toString(config.count));
			chkImage.setChecked(config.showImage);
			chkName.setChecked(config.showName);
			chkBut.setChecked(config.showCallButton);
            chkIncoming.setChecked(config.showIncoming);
            chkOutgoing.setChecked(config.showOutgoing);
            chkMissed.setChecked(config.showMissed);
			chkDate.setChecked(config.longDataFormat);
			chkEmblem.setChecked(config.showEmblem);
			if (textSize != null) textSize.setText(Float.toString(config.textSize));
			if (config.bubbleResourceName.isEmpty()) {
				bubbleStyle.setDefaultValue(config.bubbleResource);
			}else{
				bubbleStyle.setDefaultValue(context.getResources().getIdentifier("drawable/" + config.bubbleResourceName, null, context.getPackageName()));
			}
			bubbleColor.setDefaultValue(config.bubbleColor);
			editRowColor.setDefaultValue(config.rowColor);
			editTextColor.setDefaultValue(config.textColor);
			Log.d(TAG, "Loaded: " + config.showImage + "," + config.showName + "," + config.showCallButton + "," + config.longDataFormat);
		}
	}

}