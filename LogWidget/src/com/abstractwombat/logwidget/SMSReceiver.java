package com.abstractwombat.logwidget;

import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import com.abstractwombat.loglibrary.ALogSource;
import com.abstractwombat.loglibrary.LogSourceFactory;
import com.abstractwombat.loglibrary.SMS;
import com.abstractwombat.loglibrary.SMSLog;
import com.abstractwombat.loglibrary.SMSLogSource;

import java.util.ArrayList;

/**
 * Updates the Log Widget when an SMS is received 
 */
public class SMSReceiver extends BroadcastReceiver {
	private static final String TAG = "SMSReceiver";

	@Override
	public void onReceive(Context context, Intent intent) {
		// Wait for the SMS Log to change
		final String STATE_FILE = "State";
		final Integer POLL_COUNT = 2;
		final Integer POLL_INTERVAL_MS = 1000;

		// Get all the SMSLogSources
		ALogSource[] sources = LogSourceFactory.get(context, SMSLogSource.class);
		if (sources == null || sources.length == 0){
			// Nothing to update
			Log.d(TAG, "No SMSLogSource found");
			return;
		}
		
		// Get the last sms time
		SharedPreferences settings = context.getSharedPreferences(STATE_FILE, context.MODE_MULTI_PROCESS);
		Long lastCallTime = settings.getLong(context.getPackageName() + ".LastSMSTime", 0);

		// Poll the SMS data until the last one is not at lastCallTime
		SMS[] smses = null;
		Integer tryCount = 0;
		do{
			try {
				synchronized (this) {
					wait(POLL_INTERVAL_MS);
				}
			} catch (InterruptedException e1) {
				e1.printStackTrace();
				break;
			}
			smses = SMSLog.getSMSLog(context, 1);
			tryCount++;
		}while(tryCount < POLL_COUNT && smses != null && smses.length > 0 && lastCallTime.equals(smses[0].date));			

		// Set the last sms time
		if (smses != null && smses.length > 0){
			Log.d(TAG, "Found new SMS, time stamped " + smses[0].date);
			SharedPreferences.Editor editor = settings.edit();
			editor.putLong(context.getPackageName() + ".LastSMSTime", smses[0].date);
			editor.commit();
		}
			
		// Keep a list of all the widget IDs
		ArrayList<Integer> widgetIDs = new ArrayList<Integer>();
		for (ALogSource source : sources){
			widgetIDs.add(source.config().groupID);
		}
			
		// Update all the widgets
		AppWidgetManager man = AppWidgetManager.getInstance(context);
		for (Integer widgetID : widgetIDs){
			Log.d(TAG, "Updating widget ID " + widgetID);
			man.notifyAppWidgetViewDataChanged(widgetID, R.id.list_view);
		}
		
	}
}
