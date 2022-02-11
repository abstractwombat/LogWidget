package com.abstractwombat.logwidget;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;

import com.abstractwombat.loglibrary.ALogSource;
import com.abstractwombat.loglibrary.LogSourceFactory;
import com.abstractwombat.loglibrary.SMSLogSource;
import com.abstractwombat.loglibrary.WidgetUpdateService;

/**
 * Updates the Log Widget when an MMS is received
 */
public class MMSReceiver extends BroadcastReceiver {
    private static final String TAG = "MMSReceiver";
    private static final String UriString = "content://mms-sms/conversations";

	@Override
	public void onReceive(Context context, Intent intent) {
		Log.d(TAG, "onReceive");
        observeMms(context, R.id.list_view);
	}

    private void observeMms(Context context, int viewId) {
        Log.d(TAG, "Starting content observer for " + UriString);
        // Register the observer
        Uri uri = Uri.parse(UriString);
        MmsContentObserver observer = new MmsContentObserver
                (null, context, viewId);
        context.getContentResolver().registerContentObserver(uri, true, observer);
        // Schedule the un-registering of the observer
        new Cleanup().execute(context, observer, 2*60*1000);   // Cleanup in 10 minutes
    }

    private static void removeMmmObserver(Context context, ContentObserver mmsObserver){
        Log.d(TAG, "Removing content observer for " + UriString);
        Uri uri = Uri.parse(UriString);
        context.getContentResolver().unregisterContentObserver(mmsObserver);
    }

    class Cleanup extends AsyncTask<Object, Void, Void> {
        @Override
        protected Void doInBackground(Object... params) {
            Log.d(TAG, "Cleanup");
            Context context = (Context)params[0];
            ContentObserver observer = (ContentObserver)params[1];
            Integer delay = (Integer)params[2];
            try {
                synchronized (observer) {
                    observer.wait(delay);
                }
            } catch (InterruptedException e) {
                Log.d(TAG, "RemoveListenerAsync - wait interrupted");
            }
            removeMmmObserver(context, observer);
            return null;
        }
    }

    private class MmsContentObserver extends ContentObserver {
        private Context mContext;
        private int mViewId;
        private ContentObserver mThis;

        public MmsContentObserver(Handler handler, Context context, int viewId) {
            super(handler);
            mContext = context;
            mViewId = viewId;
            mThis = this;
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            Log.d(TAG, "MmsContentObserver::onChange (selfChange=" + selfChange + ")");
            if (uri != null) {
                Log.d(TAG, "onChange uri " + uri.toString());
            }
            int[] widgetIds = getWidgetIds(mContext);

            Intent sIntent = new Intent(mContext, WidgetUpdateService.class);
            sIntent.putExtra(mContext.getPackageName() + ".WidgetIDs", widgetIds);
            sIntent.putExtra(mContext.getPackageName() + ".ViewID", mViewId);
            mContext.startService(sIntent);
        }

        @Override
        public void onChange(boolean selfChange) {
            onChange(selfChange, null);
        }
    }

    private static int[] getWidgetIds(Context context){
        // Get all the SMSLogSources
        ALogSource[] sources = LogSourceFactory.get(context, SMSLogSource.class);
        if (sources == null || sources.length == 0){
            // Nothing to update
            Log.d(TAG, "No SMSLogSource found");
            return new int[0];
        }
        // Create a list of all the widget IDs that need updating
        int i=0;
        int[] widgetIds = new int[sources.length];
        for (ALogSource source : sources){
            widgetIds[i++] = source.config().groupID;
        }
        return widgetIds;
    }

}
