package com.abstractwombat.loglibrary;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.os.Build;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.util.Log;

import com.abstractwombat.library.ResourcePreference;
import com.github.danielnilsson9.colorpickerview.preference.ColorPreference;

public class SMSLogSourceFragment extends ALogSourcePreferenceFragment {
	private static final String TAG = "SMSLogSourceFragment";

    protected int getPreferences(){
        return R.xml.smslogsource_preferences;
    }

    protected void storeToFactory(){
		// Get all the preferences
		EditTextPreference editCount = (EditTextPreference)findPreference("smslogsource_count");
		CheckBoxPreference chkImage = (CheckBoxPreference)findPreference("smslogsource_show_contact_image");
		CheckBoxPreference chkName = (CheckBoxPreference)findPreference("smslogsource_show_name");
		CheckBoxPreference chkIn = (CheckBoxPreference)findPreference("smslogsource_show_incoming");
        CheckBoxPreference chkOut = (CheckBoxPreference)findPreference("smslogsource_show_outgoing");
        CheckBoxPreference chkMMS = (CheckBoxPreference)findPreference("smslogsource_show_mms");
		CheckBoxPreference chkDate = (CheckBoxPreference)findPreference("smslogsource_long_date");
        EditTextPreference editMaxLines = (EditTextPreference)findPreference("smslogsource_maxlines");
        ResourcePreference bubbleStyle = (ResourcePreference)findPreference("smslogsource_bubblestyle");
        ColorPreference rowColor = (ColorPreference)findPreference("smslogsource_rowcolor");
        ColorPreference textColor = (ColorPreference)findPreference("smslogsource_textcolor");
        ColorPreference bubbleColor = (ColorPreference)findPreference("smslogsource_bubblecolor");
        CheckBoxPreference chkEmblem = (CheckBoxPreference)findPreference("smslogsource_show_emblem");
        EditTextPreference textSize = (EditTextPreference)findPreference("smslogsource_textsize");

        // Get the current config
        ALogSource source = LogSourceFactory.get(this.context, this.mLogSourceId);
        LogSourceConfig initConfig = source.config();

		// Configure the new source
		SMSLogSourceConfig config = new SMSLogSourceConfig(initConfig);
        Resources res = getResources();
        try {
            config.count = Integer.parseInt(editCount.getText());
        }catch (NumberFormatException e){
            config.count = res.getInteger(R.integer.sms_log_source_default_count);
        }
		config.showImage = chkImage.isChecked();
		config.showName = chkName.isChecked();
		config.showIncoming = chkIn.isChecked();
        config.showOutgoing = chkOut.isChecked();
        config.showMMS = chkMMS.isChecked();
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

//        config.bubbleResourceOutgoing = MessageBubbleResourceResolver.getResource(context, R
//                .drawable.msg_bubble_right, bubbleColor.getValue());

        // Delete the source
        LogSourceFactory.deleteSource(this.context, this.mLogSourceId);

        // Create the new source
        LogSourceFactory.newSource(this.context, SMSLogSource.class, config);

        Log.d(TAG, "Stored: " + config.theme  + "," + config.showImage + "," + config.showName + "," + config.showIncoming + "," + config.showOutgoing + "," + config.longDataFormat);
	}

    protected void loadFromFactory(){
        // Get all the preferences
        EditTextPreference editCount = (EditTextPreference)findPreference("smslogsource_count");
        CheckBoxPreference chkImage = (CheckBoxPreference)findPreference("smslogsource_show_contact_image");
        CheckBoxPreference chkName = (CheckBoxPreference)findPreference("smslogsource_show_name");
        CheckBoxPreference chkIn = (CheckBoxPreference)findPreference("smslogsource_show_incoming");
        CheckBoxPreference chkOut = (CheckBoxPreference)findPreference("smslogsource_show_outgoing");
        CheckBoxPreference chkMMS = (CheckBoxPreference)findPreference("smslogsource_show_mms");
        CheckBoxPreference chkDate = (CheckBoxPreference)findPreference("smslogsource_long_date");
        EditTextPreference editMaxLines = (EditTextPreference)findPreference("smslogsource_maxlines");
        ResourcePreference bubbleStyle = (ResourcePreference)findPreference("smslogsource_bubblestyle");
        ColorPreference editRowColor = (ColorPreference)findPreference("smslogsource_rowcolor");
        ColorPreference editTextColor = (ColorPreference)findPreference("smslogsource_textcolor");
        ColorPreference bubbleColor = (ColorPreference)findPreference("smslogsource_bubblecolor");
        CheckBoxPreference chkEmblem = (CheckBoxPreference)findPreference("smslogsource_show_emblem");
        EditTextPreference textSize = (EditTextPreference)findPreference("smslogsource_textsize");

        // Hide the text size
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
            PreferenceCategory cat = (PreferenceCategory) findPreference("smslogsource_category_appearance");
            cat.removePreference(textSize);
            textSize = null;
        }

        ALogSource source = LogSourceFactory.get(this.context, this.mLogSourceId);
        if (source == null){
            // Source with this ID doesn't exist, set the default values
            Resources res = getResources();
            editCount.setText(Integer.toString(res.getInteger(R.integer.sms_log_source_default_count)));
            chkImage.setChecked(res.getBoolean(R.bool.sms_log_source_default_showimage));
            chkName.setChecked(res.getBoolean(R.bool.sms_log_source_default_showname));
            chkIn.setChecked(res.getBoolean(R.bool.sms_log_source_default_showincoming));
            chkOut.setChecked(res.getBoolean(R.bool.sms_log_source_default_showoutgoing));
            chkMMS.setChecked(res.getBoolean(R.bool.sms_log_source_default_showmms));
            chkDate.setChecked(res.getBoolean(R.bool.sms_log_source_default_longdate));
            editMaxLines.setText(Integer.toString(res.getInteger(R.integer
                    .sms_log_source_default_maxlines)));
            bubbleStyle.setDefaultValue(SMSLogSourceConfig.DEFAULT_BUBBLE_RESOURCE);
            chkEmblem.setChecked(res.getBoolean(R.bool.sms_log_source_default_showemblem));
            if (textSize != null) textSize.setText(Integer.toString(res.getInteger(R.integer.sms_log_source_default_textsize)));
            bubbleColor.setDefaultValue(SMSLogSourceConfig.DEFAULT_BUBBLE_COLOR);
            editRowColor.setDefaultValue(Integer.toString(res.getColor(R.color.sms_log_source_default_rowcolor)));
            editTextColor.setDefaultValue(Integer.toString(res.getColor(R.color.sms_log_source_default_textcolor)));
            Log.d(TAG, "Loaded defaults");
        }else{
            // Set from the config
            SMSLogSourceConfig config = (SMSLogSourceConfig)source.config();
            if (config.count < 0){
                config.count = getResources().getInteger(R.integer.combined_source_default_count) / 2;
            }
            editCount.setText(Integer.toString(config.count));
            chkImage.setChecked(config.showImage);
            chkName.setChecked(config.showName);
            chkIn.setChecked(config.showIncoming);
            chkOut.setChecked(config.showOutgoing);
            chkMMS.setChecked(config.showMMS);
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
            Log.d(TAG, "Loaded: " + config.theme  + "," + config.showImage + "," + config.showName + "," + config.showIncoming + "," + config.showOutgoing + "," + config.showMMS + "," + config.longDataFormat);
        }

        chkMMS.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                Boolean b = (Boolean)newValue;
                if (b == true){
                    AlertDialog.Builder builder = new AlertDialog.Builder(context);
                    builder.setTitle(R.string.mms_warning_title);
                    builder.setMessage(R.string.mms_warning_message);
                    builder.setPositiveButton(R.string.mms_warning_button, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
                    AlertDialog dialog = builder.create();
                    dialog.show();
                }
                return true;
            }
        });
    }

}