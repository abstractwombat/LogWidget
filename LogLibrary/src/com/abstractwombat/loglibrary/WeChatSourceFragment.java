package com.abstractwombat.loglibrary;

import android.content.res.Resources;
import android.os.Build;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.util.Log;

import com.abstractwombat.library.ResourcePreference;
import com.github.danielnilsson9.colorpickerview.preference.ColorPreference;

public class WeChatSourceFragment extends ALogSourcePreferenceFragment {
	private static final String TAG = "WeChatSourceFragment";

    @Override
    protected int getPreferences() {
        return R.xml.wechatsource_preferences;
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");

        WeChatSource source = (WeChatSource)LogSourceFactory.get(this.context, this.mLogSourceId);
        boolean enabled = source.enabled();

        // Enable/Disable all the preferences
        Preference enablePreference = null;
        PreferenceScreen screen = getPreferenceScreen();
        for (int p=0; p<screen.getPreferenceCount(); p++){
            Preference preference = screen.getPreference(p);
            if (preference == null) continue;
            String key = preference.getKey();
            if (key == null || key.equals("wechatsource_enable")){
                enablePreference = preference;
                continue;
            }
            preference.setEnabled(enabled);
            Log.d(TAG, "Enabled/Disabled preference with key=" + key);
        }

        // Set the summary on the enablePreference to indicate the current state of access
        if (enablePreference != null) {
            String appName = context.getString(R.string.app_name);
            String summary;
            if (enabled) {
                summary = "Notification access has been enabled for this widget.";
            } else {
                summary = "Touch here to enable notification access for this widget. You must " +
                        "check " +
                        "'" + appName + "' on the next screen.";
            }
            enablePreference.setSummary(summary);
        }

    }


    protected void storeToFactory(){
		// Get all the preferences
		EditTextPreference editCount = (EditTextPreference)findPreference("wechatsource_count");
		CheckBoxPreference chkImage = (CheckBoxPreference)findPreference("wechatsource_show_contact_image");
		CheckBoxPreference chkName = (CheckBoxPreference)findPreference("wechatsource_show_name");
		CheckBoxPreference chkDate = (CheckBoxPreference)findPreference("wechatsource_long_date");
        EditTextPreference editMaxLines = (EditTextPreference)findPreference("wechatsource_maxlines");
        ResourcePreference bubbleStyle = (ResourcePreference)findPreference("wechatsource_bubblestyle");
        ColorPreference rowColor = (ColorPreference)findPreference("wechatsource_rowcolor");
        ColorPreference textColor = (ColorPreference)findPreference("wechatsource_textcolor");
        ColorPreference bubbleColor = (ColorPreference)findPreference("wechatsource_bubblecolor");
        CheckBoxPreference chkEmblem = (CheckBoxPreference)findPreference("wechatsource_show_emblem");
        EditTextPreference textSize = (EditTextPreference)findPreference("wechatsource_textsize");

        // Get the current config
        ALogSource source = LogSourceFactory.get(this.context, this.mLogSourceId);
        LogSourceConfig initConfig = source.config();

		// Configure the new source
		WeChatSourceConfig config = new WeChatSourceConfig(initConfig);
        Resources res = getResources();
        try {
            config.count = Integer.parseInt(editCount.getText());
        }catch (NumberFormatException e){
            config.count = res.getInteger(R.integer.wechat_source_default_count);
        }
		config.showImage = chkImage.isChecked();
		config.showName = chkName.isChecked();
		config.longDataFormat = chkDate.isChecked();
        config.maxLines = Integer.parseInt(editMaxLines.getText());
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
		LogSourceFactory.newSource(this.context, WeChatSource.class, config);

		Log.d(TAG, "Stored: " + config.showImage + "," + config.showName + "," + config.longDataFormat);
	}
    protected void loadFromFactory(){
		// Get all the preferences
		EditTextPreference editCount = (EditTextPreference)findPreference("wechatsource_count");
		CheckBoxPreference chkImage = (CheckBoxPreference)findPreference("wechatsource_show_contact_image");
		CheckBoxPreference chkName = (CheckBoxPreference)findPreference("wechatsource_show_name");
		CheckBoxPreference chkDate = (CheckBoxPreference)findPreference("wechatsource_long_date");
        EditTextPreference editMaxLines = (EditTextPreference)findPreference("wechatsource_maxlines");
        ResourcePreference bubbleStyle = (ResourcePreference)findPreference("wechatsource_bubblestyle");
        ColorPreference editRowColor = (ColorPreference)findPreference("wechatsource_rowcolor");
        ColorPreference editTextColor = (ColorPreference)findPreference("wechatsource_textcolor");
        ColorPreference bubbleColor = (ColorPreference)findPreference("wechatsource_bubblecolor");
        CheckBoxPreference chkEmblem = (CheckBoxPreference)findPreference("wechatsource_show_emblem");
        EditTextPreference textSize = (EditTextPreference)findPreference("wechatsource_textsize");

        // Hide the text size
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
            PreferenceCategory cat = (PreferenceCategory) findPreference("wechatsource_category_appearance");
            cat.removePreference(textSize);
            textSize = null;
        }

		ALogSource source = LogSourceFactory.get(this.context, this.mLogSourceId);
		if (source == null){
			// Source with this ID doesn't exist, set the default values
			Resources res = getResources();
			editCount.setText(Integer.toString(res.getInteger(R.integer.wechat_source_default_count)));
			chkImage.setChecked(res.getBoolean(R.bool.wechat_source_default_showimage));
			chkName.setChecked(res.getBoolean(R.bool.wechat_source_default_showname));
			chkDate.setChecked(res.getBoolean(R.bool.wechat_source_default_longdate));
            editMaxLines.setText(Integer.toString(res.getInteger(R.integer
                    .wechat_source_default_maxlines)));
            bubbleStyle.setDefaultValue(WeChatSourceConfig.DEFAULT_BUBBLE_RESOURCE);
            chkEmblem.setChecked(res.getBoolean(R.bool.wechat_source_default_showemblem));
            if (textSize != null) textSize.setText(Integer.toString(res.getInteger(R.integer.wechat_source_default_textsize)));
            bubbleColor.setDefaultValue(WeChatSourceConfig.DEFAULT_BUBBLE_COLOR);
            editRowColor.setDefaultValue(Integer.toString(res.getColor(R.color
                    .wechat_source_default_rowcolor)));
            editTextColor.setDefaultValue(Integer.toString(res.getColor(R.color.wechat_source_default_textcolor)));
            Log.d(TAG, "Loaded defaults");
		}else{
			// Set from the config
			WeChatSourceConfig config = (WeChatSourceConfig)source.config();
            if (config.count < 0){
                config.count = getResources().getInteger(R.integer.combined_source_default_count) / 2;
            }
            editCount.setText(Integer.toString(config.count));
			chkImage.setChecked(config.showImage);
			chkName.setChecked(config.showName);
			chkDate.setChecked(config.longDataFormat);
            editMaxLines.setText(Integer.toString(config.maxLines));
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
            Log.d(TAG, "Loaded: " + config.showImage + "," + config.showName + "," + config.longDataFormat);
		}
	}

}