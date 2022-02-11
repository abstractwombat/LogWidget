package com.abstractwombat.logwidget;

import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.telephony.TelephonyManager;
import android.util.Log;

import java.util.ArrayList;

import com.abstractwombat.loglibrary.ALogSource;
import com.abstractwombat.loglibrary.Call;
import com.abstractwombat.loglibrary.CallLog;
import com.abstractwombat.loglibrary.CallLogSource;
import com.abstractwombat.loglibrary.LogSourceFactory;

import com.abstractwombat.logwidget.R;


/**
 * Updates the Log Widget after a phone call
 */
public class CallStateChangedReceiver extends BroadcastReceiver {
    private static final String TAG = "CallStateChangedReceiver";

    @Override
	public void onReceive(Context context, Intent intent) {
		// Update the widget after a phone call
		TelephonyManager tm = (TelephonyManager) context
				.getSystemService(Context.TELEPHONY_SERVICE);
		if (tm.getCallState() != TelephonyManager.CALL_STATE_IDLE) {
            return;
        }

        // Wait for the CallLog to change
        final String STATE_FILE = "State";
        final Integer POLL_COUNT = 10;
        final Integer POLL_INTERVAL_MS = 500;

        // Get all the CallLogSources
        ALogSource[] sources = LogSourceFactory.get(context, CallLogSource.class);
        if (sources == null || sources.length == 0){
            // Nothing to update
            return;
        }

        // Get the last call time
        SharedPreferences settings = context.getSharedPreferences(STATE_FILE, context.MODE_MULTI_PROCESS);
        Long lastCallTime = settings.getLong(context.getPackageName() + ".LastCallTime", -1);
        Long lastCallTimeFound = 0L;

        // Poll the Call Log data until the last call is not at lastCallTime
        Call[] lastCall = null;
        Integer tryCount = 0;
        do{
            try {
                synchronized (this) {
                    wait(POLL_INTERVAL_MS);
                }
            } catch (InterruptedException e1) {
                Log.d(TAG, "InterruptedException during poll interval");
                break;
            }
            lastCall = null;
            lastCall = CallLog.getCalls(context, 1);
            if (lastCall != null && lastCall.length > 0){
                lastCallTimeFound = lastCall[0].date;
                if (!lastCallTimeFound.equals(lastCallTime)){
                    Log.d(TAG, "Try: " + tryCount + " Found Last Call Time: " + lastCallTimeFound + " Previous last call time: " + lastCallTime);
                    break;
                }
                lastCall = null;
            }
            tryCount++;
        }while(tryCount < POLL_COUNT);

        // Set the last call time
        if (lastCall != null && lastCall.length > 0){
            SharedPreferences.Editor editor = settings.edit();
            editor.putLong(context.getPackageName() + ".LastCallTime", lastCall[0].date);
            editor.commit();
        }else{
            Log.d(TAG, "Failed to get a new call from the log (" + tryCount + " tries made)");
            return;
        }
        Log.d(TAG, "Got new call (" + lastCall[0].number + "); took " + tryCount + " polls of the call log");

        // Keep a list of all the widget IDs
        ArrayList<Integer> widgetIDs = new ArrayList<Integer>();
        for (ALogSource source : sources){
            widgetIDs.add(source.config().groupID);
        }

        // Update all the widgets
        AppWidgetManager man = AppWidgetManager.getInstance(context);
        for (Integer widgetID : widgetIDs){
            man.notifyAppWidgetViewDataChanged(widgetID, R.id.list_view);
            Log.d(TAG, "Updated Widget ID: " + widgetID);
        }

        return;
	}
}