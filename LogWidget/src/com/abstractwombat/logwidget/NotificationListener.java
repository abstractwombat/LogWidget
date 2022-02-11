package com.abstractwombat.logwidget;

import android.annotation.TargetApi;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import com.abstractwombat.loglibrary.ALogSource;
import com.abstractwombat.loglibrary.LogSourceFactory;
import com.abstractwombat.loglibrary.NotificationSource;

import java.util.HashSet;
import java.util.Set;

@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class NotificationListener extends NotificationListenerService {
    private static final String TAG = "NotificationListener";
    private Context mContext;
    private Handler mHandler;

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = getApplicationContext();
        mHandler = new Handler();
    }
    @Override
    public void onNotificationPosted(final StatusBarNotification statusBarNotification) {
        Log.d(TAG, "onNotificationPosted - package:" + statusBarNotification.getPackageName());

        mHandler.post(new Runnable() {
            @Override
            public void run() {
                // Get all the sources from the factory
                ALogSource[] sources = LogSourceFactory.get(mContext);
                if (sources.length == 0){
                    // Nothing to update
                    return;
                }

                // Keep a list of all the widget IDs so we know who to update later
                Set<Integer> widgetIDs = new HashSet<>();

                for (ALogSource source : sources) {
                    if (source instanceof NotificationSource){
                        if (((NotificationSource)source).addNotification(statusBarNotification)) {
                            widgetIDs.add(source.config().groupID);
                        }
                    }
                }

                // Update all the widgets
                AppWidgetManager man = AppWidgetManager.getInstance(mContext);
                for (Integer widgetID : widgetIDs){
                    man.notifyAppWidgetViewDataChanged(widgetID, R.id.list_view);
                    Log.d(TAG, "Updated Widget ID: " + widgetID);
                }
            }
        });
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification statusBarNotification) {

    }

}
