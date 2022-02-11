package com.abstractwombat.loglibrary;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

/**
 * Created by Michael on 8/4/2015.
 */
public class LicenseTrial {
    private final static String TAG = "LicenseTrial";
    private final static String SP_FILE = "PersistentState";

    public static long trialStartTime(Context context){
        SharedPreferences settings = context.getSharedPreferences(SP_FILE, Activity.MODE_MULTI_PROCESS);
        final String trialKey = context.getPackageName() + ".trial";
        if (settings.contains(trialKey)){
            long startTime = settings.getLong(trialKey, 0);
            return startTime;
        }else{
            return 0;
        }
    }

    public static boolean trialStarted(Context context){
        SharedPreferences settings = context.getSharedPreferences(SP_FILE, Activity.MODE_MULTI_PROCESS);
        final String trialKey = context.getPackageName() + ".trial";
        if (settings.contains(trialKey) && settings.getLong(trialKey, 0) != 0){
            Log.d(TAG, "trialStarted: true");
            return true;
        }else{
            Log.d(TAG, "trialStarted: false");
            return false;
        }
    }

    public static long startTrial(Context context, int widgetID){
        Log.d(TAG, "Starting trial");
        SharedPreferences settings = context.getSharedPreferences(SP_FILE, Activity.MODE_MULTI_PROCESS);
        final String trialKey = context.getPackageName() + ".trial";
        long trialStartTime = System.currentTimeMillis();

        // Store the start time
        SharedPreferences.Editor editor = settings.edit();
        editor.putLong(trialKey, trialStartTime);
        editor.apply();

        // Calculate expiration time
        long trialLength = getTrialLength(context);
        long trialExpiry = trialStartTime + trialLength;

        // Schedule an alarm when the trial ends
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent endIntent = new Intent(context, LogReceiver.class);
        endIntent.setAction(context.getPackageName() + ".trial_end");
        endIntent.putExtra(context.getPackageName() + ".WidgetID", widgetID);
        PendingIntent pendingEndIntent = PendingIntent.getBroadcast(context, 0, endIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        am.set(AlarmManager.RTC_WAKEUP, trialExpiry, pendingEndIntent);

        return trialStartTime + trialLength;
    }

    public static boolean isExpired(Context context){
        if (!trialStarted(context)){
            Log.d(TAG, "isExpired: false (trial not started)");
            return false;
        }
        if (timeRemaining(context) > 0){
            Log.d(TAG, "isExpired: false (trial ongoing)");
            return false;
        }else{
            Log.d(TAG, "isExpired: true");
            return true;
        }
    }

    public static long timeRemaining(Context context){
        // Check if the trial is ongoing
        SharedPreferences settings = context.getSharedPreferences(SP_FILE, Activity.MODE_MULTI_PROCESS);
        String trialKey = context.getPackageName() + ".trial";
        if (settings.contains(trialKey)){
            // Trial has started
            long trialStartTime = settings.getLong(trialKey, 0);
            long trialLength = getTrialLength(context);
            long trialExpiry = trialStartTime + trialLength;
            long r = trialExpiry - System.currentTimeMillis();
            Log.d(TAG, "timeRemaining: " + r);
            return r;
        }else{
            // Trial hasn't started
            Log.d(TAG, "timeRemaining: 0 (trial hasn't started)");
            return 0;
        }

    }

    private static long getTrialLength(Context context) {
        return (long) (context.getResources().getInteger(R.integer.trial_length_days) * 24 * 60 * 60 * 1000);
    }
}
