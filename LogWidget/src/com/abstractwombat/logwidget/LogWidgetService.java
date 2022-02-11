package com.abstractwombat.logwidget;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.abstractwombat.loglibrary.ALogSource;
import com.abstractwombat.loglibrary.LogSourceFactory;
import com.abstractwombat.loglibrary.NotificationSource;

public class LogWidgetService extends RemoteViewsService {
    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new LogRemoteViewsFactory(this.getApplicationContext(), intent);
    }
}

class LogRemoteViewsFactory implements RemoteViewsService.RemoteViewsFactory {
    private static final String TAG = "LogRemoteViewsFactory";
	private Context context;
	private int appWidgetId;
	private int rowCount;
	private final String STATE_FILE = "State";

	public LogRemoteViewsFactory(Context context, Intent intent) {
		Log.d(TAG, "Constructing");
		this.context = context;
        this.appWidgetId = Integer.valueOf(intent.getData().getSchemeSpecificPart());
	}

	public void onCreate() {
		Log.d(TAG, "Creating");
		computeCounts(true, false);
	}

	public RemoteViews getViewAt(int position) {
        SharedPreferences settings = context.getSharedPreferences(STATE_FILE, context
				.MODE_MULTI_PROCESS);
        int defaultTheme = context.getResources().getInteger(R.integer.default_theme);

        int p = position;
        int settingsPosition = this.rowCount;
        boolean fillFromBottom = getFillFromBottomSetting();
        if (fillFromBottom){
            p = this.rowCount - position;
            settingsPosition = 0;
        }

		if (this.rowCount == 0) {
			int placeHolderPosition = 1;
			if (!fillFromBottom) {
				placeHolderPosition = 0;
				settingsPosition = 1;
			}
			if (position == placeHolderPosition) {
				Log.d(TAG, "Showing the placeholder");
				// If it's a NotificationSource, then get the package for the view's intent
				String sourcePackage = "";
				String combinedID = settings.getString(context.getPackageName() + "." + Integer
						.toString(this.appWidgetId) + ".RootId", "");
				if (combinedID != "") {
					ALogSource combinedSource = LogSourceFactory.get(this.context, combinedID);
					if (combinedSource instanceof NotificationSource){
						sourcePackage = ((NotificationSource)combinedSource).getPackage();
						Log.d(TAG, "Got the source's package: " + sourcePackage);
					}
				}

				RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.placeholder_row);
				rv.setImageViewResource(R.id.image, R.mipmap.ic_launcher);
				if (!sourcePackage.isEmpty()) {
					Intent intent = new Intent();
					intent.setAction(context.getPackageName() + ".launch_package");
					intent.putExtra(context.getPackageName() + ".PackageName", sourcePackage);
					rv.setOnClickFillInIntent(R.id.placeholder, intent);
				}
				return rv;
			}
		}

        // Settings row
		if (position == settingsPosition){
            Log.d(TAG, "Showing settings at position " + p);
            // Set the layout
			RemoteViews rv = null;
			int value = settings.getInt(context.getPackageName() + "." + Integer.toString(this
					.appWidgetId) + ".SettingsStyle", 0);
			if (value == 1) {
				rv = new RemoteViews(context.getPackageName(), R.layout.settings_row_material);
			} else {
				rv = new RemoteViews(context.getPackageName(), R.layout
						.settings_row_material_small);
			}

			// Settings button intent
			Intent configIntent = new Intent();
			configIntent.setAction(context.getPackageName()+".config");
			rv.setOnClickFillInIntent(R.id.settings_button, configIntent);

			// Refresh button intent
			Intent refreshIntent = new Intent();
			refreshIntent.setAction(context.getPackageName()+".refresh");
			refreshIntent.putExtra(context.getPackageName() + ".ViewID", R.id.list_view);
			rv.setOnClickFillInIntent(R.id.refresh_button, refreshIntent);

			return rv;
		}

		// Empty place holder
		if (this.rowCount == 0){
			// Place holder position
			if (fillFromBottom) {
			}
		}

		// Get this widget's CombinedLogSource ID
        String combinedID = settings.getString(context.getPackageName() + "." + Integer.toString(this.appWidgetId) + ".RootId", "");
		if (combinedID == "") return null;
		
		// Get the view from the CombinedLogSource
		ALogSource combinedSource = LogSourceFactory.get(this.context, combinedID);
		if (combinedSource == null) return null;
        Log.d(TAG, "Showing source item at position " + p);
        RemoteViews rv = combinedSource.getViewAt(p);
        if (rv == null){
            Log.d(TAG, "Null source item at position " + p);
        }
        return rv;
	}

	@Override
	public int getCount() {
        int count = this.rowCount;
        count += 1; // Settings row
        if (count == 1){
            count += 1; // Empty widget placeholder
        }
        Log.d(TAG, "getCount: " + count);
		return count;
	}

	@Override
	public long getItemId(int position) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public RemoteViews getLoadingView() {
		return new RemoteViews(this.context.getPackageName(), R.layout.loading_row);
	}

	@Override
	public int getViewTypeCount() {
        int viewCount = LogSourceFactory.getTotalViewTypes(this.context);   // Sources
        viewCount += 2;   // Settings row and place holder
        return viewCount;
	}

	@Override
	public boolean hasStableIds() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void onDataSetChanged() {
		Log.d(TAG, "onDataSetChanged (widget ID: " + this.appWidgetId + ")");
		computeCounts(true, true);
	}

	public static class scrollToPosition extends AsyncTask<Object, Void, Void>{
		@Override
		protected Void doInBackground(Object[] params) {
			Log.d(TAG, "Starting thread to scroll after update");
			Context context = (Context) params[0];
			int widgetId = (int) params[1];
			int scrollPosition = (int) params[2];
			int sleepTime = 1200;
			if (params.length > 3){
				sleepTime = (int) params[3];
			}

			try {
				Log.d(TAG, "Sleeping for " + sleepTime + "ms");
				Thread.sleep(sleepTime);
			} catch (InterruptedException e) {
				Log.d(TAG, "Scroll thread was interrupted");
			}
			String packageName = context.getPackageName();
			RemoteViews rv = new RemoteViews(packageName, R.layout.widget_layout_material);
			rv.setInt(R.id.list_view, "smoothScrollToPosition", scrollPosition);
			AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
			appWidgetManager.partiallyUpdateAppWidget(widgetId, rv);
			return null;
		}
	};

	@Override
	public void onDestroy() {
	}

	private void computeCounts(boolean update, boolean scrollTo){
		SharedPreferences settings = context.getSharedPreferences(STATE_FILE, Context.MODE_MULTI_PROCESS);
		String rootIdKey = context.getPackageName() + "." + Integer.toString(this.appWidgetId) + ".RootId";
		Log.d(TAG, "Getting root id source - " + rootIdKey);
		String combinedID = settings.getString(rootIdKey, "");
		if (combinedID == ""){
			Log.d(TAG, "No root source found");
			return;		
		}
		ALogSource rootSource = LogSourceFactory.get(this.context, combinedID);
		if (rootSource == null) {
			Log.d(TAG, "Failed to instantiate root source");
			return;
		}

		if (update){
			rootSource.update();
		}
		//Testing.createTestEntries(context, appWidgetId);
		this.rowCount = rootSource.size();

		if (scrollTo) {
			boolean fillFromBottom = getFillFromBottomSetting();
			int position = 0;
			if (fillFromBottom) {
				position = this.rowCount;
			}
			Log.d(TAG, "Starting the scrollToPosition thread");
			new scrollToPosition().execute(this.context, this.appWidgetId, position);
		}
        Log.d(TAG, "Root Count: " + this.rowCount);
	}

	private boolean getFillFromBottomSetting(){
		SharedPreferences settings = context.getSharedPreferences(STATE_FILE, Context
				.MODE_MULTI_PROCESS);
		String directionKey = context.getPackageName() + "." + this.appWidgetId + ".Direction";
		int dir = settings.getInt(directionKey, 0);
		if (dir == 0) return true;
		else return false;
	}
}