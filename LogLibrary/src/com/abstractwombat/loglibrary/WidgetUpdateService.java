package com.abstractwombat.loglibrary;

import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import java.util.HashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * This service is designed to handle many startService calls. First call to startService will
 * start a thread that waits, then updates the widgets. Another startService call will kill that
 * thread and start a new thread to wait, then update. And so on.
 */
public class WidgetUpdateService extends Service {
    private final static String TAG = "WidgetUpdateService";

    private final int WAIT_DELAY = 2000;
    private Context mContext;
    private Lock mLock = new ReentrantLock();
    private HashMap<String, Thread> mThreads;

    public WidgetUpdateService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mLock.lock();
        Log.d(TAG, "Lock");
        mThreads = new HashMap<>();
        Log.d(TAG, "Unlock");
        mLock.unlock();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand");
        mContext = getApplicationContext();
        Bundle extras = intent.getExtras();

        // Get the widget ids
        int[] widgetIds = extras.getIntArray(mContext.getPackageName() + ".WidgetIDs");
        if (widgetIds == null || widgetIds.length == 0) {
            Log.d(TAG, "Invalid widget ids");
            return START_REDELIVER_INTENT;
        }
        String key = getKey(widgetIds);

        // Get the view id
        int viewId = extras.getInt(mContext.getPackageName() + ".ViewID", 0);
        if (viewId == 0) {
            Log.d(TAG, "Invalid view id");
            return START_REDELIVER_INTENT;
        }

        boolean startThread = false;
        // Check if the thread is already running
        mLock.lock();
        Log.d(TAG, "Lock");
        if (mThreads.containsKey(key)) {
            Thread thread = mThreads.get(key);
            if (thread.isInterrupted()) {
                Log.d(TAG, "Thread is already interrupted");
                return START_REDELIVER_INTENT;
            }
            if (thread.isAlive()) {
                thread.interrupt();
                try {
                    thread.join();
                } catch (InterruptedException e) {
                    Log.d(TAG, "Thread join was interrupted");
                    return START_REDELIVER_INTENT;
                }
            }
        }

        Thread thread = new Thread(new WaitAndUpdate(widgetIds, viewId));
        mThreads.put(key, thread);
        Log.d(TAG, "Unlock");
        mLock.unlock();
        thread.start();

        return START_REDELIVER_INTENT;
    }

    private class WaitAndUpdate implements Runnable {
        private final int[] mWidgetIds;
        private int mViewId;

        WaitAndUpdate(int[] widgetIds, int viewId){
            mWidgetIds = widgetIds;
            mViewId = viewId;
        }
        @Override
        public void run() {
            Log.d(TAG, "Thread run");
            // Wait
            try {
                synchronized (mWidgetIds) {
                    mWidgetIds.wait(WAIT_DELAY);
                }
            } catch (InterruptedException e) {
                Log.d(TAG, "Thread interrupted");
                return;
            }

            // Update
            for (int i=0; i<mWidgetIds.length; i++) {
                updateWidget(mContext, mWidgetIds[i], mViewId);
            }

            // Remove the data entry
            mLock.lock();
            Log.d(TAG, "Lock");
            String key = getKey(mWidgetIds);
            mThreads.remove(key);
            if (mThreads.isEmpty()){
                Log.d(TAG, "No threads left, stopping service");
                stopSelf();
            }
            Log.d(TAG, "Unlock");
            mLock.unlock();
        }
    }

    private static void updateWidget(Context context, int widgetId, int viewId) {
        Log.d(TAG, "Updating widget id " + widgetId);
        if (viewId != 0) {
            AppWidgetManager man = AppWidgetManager.getInstance(context);
            Log.d(TAG, "Invalidating view: " + viewId);
            man.notifyAppWidgetViewDataChanged(widgetId, viewId);
        }
    }

    private String getKey(int[] widgetIds){
        String key = "";
        for (int i=0; i<widgetIds.length; i++){
            key += Integer.toString(widgetIds[i]) + ",";
        }
        return key;
    }


    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
