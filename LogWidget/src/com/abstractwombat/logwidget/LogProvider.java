package com.abstractwombat.logwidget;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import com.abstractwombat.images.ImageCache;
import com.abstractwombat.images.ImageUtilities;
import com.abstractwombat.library.SharedPreferenceList;
import com.abstractwombat.loglibrary.ALogSource;
import com.abstractwombat.loglibrary.AdFetcher;
import com.abstractwombat.loglibrary.CallLogSource;
import com.abstractwombat.loglibrary.CallLogSourceConfig;
import com.abstractwombat.loglibrary.CombinedLogSource;
import com.abstractwombat.loglibrary.CombinedLogSourceConfig;
import com.abstractwombat.loglibrary.FacebookMessengerSource;
import com.abstractwombat.loglibrary.FacebookMessengerSourceConfig;
import com.abstractwombat.loglibrary.HangoutsSource;
import com.abstractwombat.loglibrary.HangoutsSourceConfig;
import com.abstractwombat.loglibrary.LogReceiver;
import com.abstractwombat.loglibrary.LogSourceConfig;
import com.abstractwombat.loglibrary.LogSourceFactory;
import com.abstractwombat.loglibrary.SMSLogSource;
import com.abstractwombat.loglibrary.SMSLogSourceConfig;
import com.abstractwombat.loglibrary.SkypeSource;
import com.abstractwombat.loglibrary.SkypeSourceConfig;
import com.abstractwombat.loglibrary.ViberSource;
import com.abstractwombat.loglibrary.ViberSourceConfig;
import com.abstractwombat.loglibrary.WeChatSource;
import com.abstractwombat.loglibrary.WeChatSourceConfig;
import com.abstractwombat.loglibrary.WhatsAppSource;
import com.abstractwombat.loglibrary.WhatsAppSourceConfig;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class LogProvider extends AppWidgetProvider {
    private static final String TAG = "LogProvider";
    public static String WIDGET_ID_FILE = "WidgetIDs";
    private final String STATE_FILE = "State";

    public final static String ACTION_FORCE_UPDATE = "update";
    private final static String ACTION_CLOSE_TIP = "tip_closed";
    private final static String ACTION_CLOSE_AD = "ad_closed";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        Log.d(TAG, "onUpdate");

        // Create the ad fetcher
//        AdFetcher adFetcher = new AdFetcher(context);

        long currentTime = System.currentTimeMillis();
        SharedPreferenceList prefs = new SharedPreferenceList(context, WIDGET_ID_FILE);
        SharedPreferences state = context.getSharedPreferences(STATE_FILE, Context.MODE_MULTI_PROCESS);
        String packageName = context.getPackageName();

        // Get the stored removal of the ads
        String adRemoveKey = packageName + ".AdsRemoved";
        String unlockAllKey = packageName + ".UnlockAll";
        boolean adRemovePurchased = state.getBoolean(adRemoveKey, false);
        boolean unlockAllPurchase = state.getBoolean(unlockAllKey, false);
        boolean adsRemoved = adRemovePurchased || unlockAllPurchase;
        Log.d(TAG, "Ad removal purchased: " + adRemovePurchased);
        Log.d(TAG, "Unlock all purchased: " + unlockAllPurchase);

        for (int appWidgetId : appWidgetIds) {
            Log.d(TAG, "onUpdate: " + appWidgetId);
            if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
                continue;
            }
            String widgetIdString = Integer.toString(appWidgetId);

            // Check if this widget is active (passed configuration activity)
            boolean active = state.getBoolean(packageName + "." + widgetIdString + ".Active", false);
            if (!active) {
                Log.d(TAG, "Not done configuring - " + widgetIdString);
                continue;
            }

            // Instantiate the RemoteViews object for the App Widget layout.
            int theme = state.getInt(packageName + "." + widgetIdString + ".Theme", context.getResources().getInteger(R.integer.default_theme));
            Log.d(TAG, "Loaded Theme (" + theme + ") fm " + packageName + "." + widgetIdString + ".Theme");

            RemoteViews rv;
            switch (theme) {
                case 0:
                    rv = new RemoteViews(packageName, R.layout.widget_layout_dark);
                    break;
                case 1:
                    rv = new RemoteViews(packageName, R.layout.widget_layout_light);
                    break;
                case 2:
                    rv = new RemoteViews(packageName, R.layout.widget_layout_material);
                    break;
                default:
                    rv = new RemoteViews(packageName, R.layout.widget_layout);
                    break;
            }

            // Setup the banner
            long creationTime = state.getLong(packageName + "." + widgetIdString + ".WidgetCreationTime", 0);
            long adClosed = state.getLong(packageName + "." + widgetIdString + ".AdClosed", 0);
            long tipClosed = state.getLong(packageName + "." + widgetIdString + ".TipClosed", 0);

            float topPaddingWithBanner = ImageUtilities.convertDpToPixel(68);

            // Create an intent to call this function
            Intent updateWidgetIntent = new Intent(context, LogProvider.class);
            updateWidgetIntent.putExtra(context.getPackageName() + ".WidgetID", appWidgetId);

            // Hide banner
            rv.setViewVisibility(R.id.banner, View.GONE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                rv.setViewPadding(R.id.list_view, 0, 0, 0, 0);
            }

            // Set the background color
            int backColor = state.getInt(packageName + "." + widgetIdString + ".BackColor", Color.TRANSPARENT);
            rv.setInt(R.id.list_view, "setBackgroundColor", backColor);

            Log.d(TAG, "Widget " + widgetIdString + " created at " + creationTime);
            rv.setViewVisibility(R.id.banner, View.GONE);
            long scheduledUpdateTime = 0;
            if (creationTime == 0 || tipClosed == 0) {
                // First time run... show the initial tip
                rv.setViewVisibility(R.id.banner, View.VISIBLE);
                rv.setViewVisibility(R.id.banner_image, View.VISIBLE);

                SharedPreferences settings = context.getSharedPreferences(STATE_FILE, Context
                        .MODE_MULTI_PROCESS);
                String directionKey = context.getPackageName() + "." + widgetIdString + ".Direction";
                int dir = settings.getInt(directionKey, 0);
                if (dir == 0){
                    rv.setImageViewResource(R.id.banner_image, R.drawable.ic_arrow_up_bold_grey600_36dp);
                    rv.setTextViewText(R.id.banner_text, context.getString(R.string.banner_initial_tip));
                }else{
                    rv.setImageViewResource(R.id.banner_image, R.drawable.ic_arrow_down_bold_grey600_36dp);
                    rv.setTextViewText(R.id.banner_text, context.getString(R.string.banner_initial_tip_bottom));
                }

                // Close intent
                updateWidgetIntent.setAction(ACTION_CLOSE_TIP);

                int piFlag =  PendingIntent.FLAG_UPDATE_CURRENT;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    piFlag |= PendingIntent.FLAG_MUTABLE;
                }
                PendingIntent closePendingIntent = PendingIntent.getBroadcast(context, appWidgetId, updateWidgetIntent, piFlag);
                rv.setOnClickPendingIntent(R.id.banner_close, closePendingIntent);
                // Top padding
                rv.setViewPadding(R.id.list_view, 0, (int) topPaddingWithBanner, 0, 0);
                // Store the creation time
                if (creationTime == 0) {
                    SharedPreferences.Editor edit = state.edit();
                    edit.putLong(packageName + "." + widgetIdString + ".WidgetCreationTime", currentTime);
                    edit.commit();
                }
                Log.d(TAG, "Tip shown");
//            } else if (adFetcher.isAvailable()) {
//                // Get the data
//                AdFetcher.AdData data = adFetcher.getData();
//
//                // Figure out when to show the ad
//                long timeToShowAd = tipClosed + data.reassert;
//                if (adClosed > 0) timeToShowAd = adClosed + data.reassert;
//                Log.d(TAG, "Current Time: " + currentTime + " Creation Time: " + creationTime + "Tip closed: " + tipClosed + " Ad Closed: " + adClosed + " Show at: " + timeToShowAd + " Reassert every: " + data.reassert);
//
//                if (!adsRemoved && data.reassert > 0) {
//                    if (currentTime > timeToShowAd) {
//                        // Time to show the ad
//                        rv.setViewVisibility(R.id.banner, View.VISIBLE);
//                        // Image
//                        if (data.image != null) {
//                            rv.setViewVisibility(R.id.banner_image, View.VISIBLE);
//                            rv.setImageViewBitmap(R.id.banner_image, data.image);
//                        } else {
//                            rv.setViewVisibility(R.id.banner_image, View.GONE);
//                        }
//                        // Text
//                        rv.setViewVisibility(R.id.banner_text, View.VISIBLE);
//                        rv.setTextViewText(R.id.banner_text, data.text);
//                        // Show / Hide close button
//                        if (data.closeable) {
//                            rv.setViewVisibility(R.id.banner_close, View.VISIBLE);
//                            Intent closeAdWidgetIntent = new Intent(context, LogProvider.class);
//                            closeAdWidgetIntent.setAction(ACTION_CLOSE_AD);
//                            closeAdWidgetIntent.putExtra(context.getPackageName() + ".WidgetID", appWidgetId);
//
//                            PendingIntent closePendingIntent = PendingIntent.getBroadcast(context, appWidgetId, closeAdWidgetIntent, PendingIntent.FLAG_UPDATE_CURRENT);
//                            rv.setOnClickPendingIntent(R.id.banner_close, closePendingIntent);
//                        } else {
//                            rv.setViewVisibility(R.id.banner_close, View.GONE);
//                        }
//                        // Touch event
//                        if (data.uri.length() > 0) {
//                            Log.d(TAG, "Banner uri: " + data.uri);
//                            Intent bannerIntent = new Intent(Intent.ACTION_VIEW);
//                            bannerIntent.setData(Uri.parse(data.uri)); //< "market://details?id
//                            // =package"
//                            PendingIntent clickBannerPI = PendingIntent.getActivity(context, appWidgetId, bannerIntent, PendingIntent.FLAG_UPDATE_CURRENT);
//                            rv.setOnClickPendingIntent(R.id.banner, clickBannerPI);
//                        }
//
//                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
//                            rv.setViewPadding(R.id.list_view, 0, (int) topPaddingWithBanner, 0, 0);
//                        }
//                        Log.d(TAG, "Ad shown");
//                    } else {
//                        Log.d(TAG, "Not time to show the ad yet");
//                        // Schedule to update when the ad needs to be shown
//                        scheduledUpdateTime = timeToShowAd;
//                        Log.d(TAG, "Scheduling potential ad reassert at " + timeToShowAd);
//                    }
//                }
            } else {
                Log.d(TAG, "Ad not available");
            }

            // Set up the RemoteViews object to use a RemoteViews adapter.
            Intent intent = new Intent(context, LogWidgetService.class);
            intent.setData(Uri.fromParts("content", String.valueOf(appWidgetId), null));
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            rv.setRemoteAdapter(R.id.list_view, intent);

            // The empty view is displayed when the collection has no items.
            rv.setEmptyView(R.id.list_view, R.id.empty_view);

            // Create the template intent for each list item
            Intent templateIntent = new Intent(context, LogReceiver.class);
            templateIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            templateIntent.putExtra(packageName + ".WidgetID", appWidgetId);
            int piFlag =  PendingIntent.FLAG_CANCEL_CURRENT;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                piFlag |= PendingIntent.FLAG_MUTABLE;
            }
            PendingIntent templatePendingIntent = PendingIntent.getBroadcast(context, appWidgetId, templateIntent, piFlag);
            rv.setPendingIntentTemplate(R.id.list_view, templatePendingIntent);

            // Register the widget ID
            prefs.addTo(packageName + ".WidgetIDs", Integer.toString(appWidgetId));

            // Update the widget's data
            appWidgetManager.updateAppWidget(appWidgetId, rv);
            String updateReason = state.getString(packageName + "." + widgetIdString + ".UpdateReason", "");
            Log.d(TAG, "Update Reason: " + updateReason);
            if (updateReason.length() == 0) {
                // The data must be updated
                appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.list_view);
            }

            // Calculate time of nightly update
            Calendar midnightTonight = Calendar.getInstance();
            //midnightTonight.add(Calendar.MINUTE, 2);
            midnightTonight.add(Calendar.DATE, 1);
            midnightTonight.set(Calendar.HOUR_OF_DAY, 0);
            midnightTonight.set(Calendar.MINUTE, 10);
            long midnightUpdate = midnightTonight.getTimeInMillis();
            Log.d(TAG, "Scheduling potential nightly update for " + midnightUpdate);
            if (scheduledUpdateTime == 0 || midnightUpdate < scheduledUpdateTime){
                scheduledUpdateTime = midnightUpdate;
            }

            // Schedule next update
            Intent adUpdateWidgetIntent = new Intent(context, LogProvider.class);
            adUpdateWidgetIntent.putExtra(context.getPackageName() + ".WidgetID", appWidgetId);
            adUpdateWidgetIntent.setAction(ACTION_FORCE_UPDATE);
            int piFlag2 =  PendingIntent.FLAG_UPDATE_CURRENT;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                piFlag2 |= PendingIntent.FLAG_MUTABLE;
            }
            PendingIntent refreshPendingIntent = PendingIntent.getBroadcast(context, appWidgetId, adUpdateWidgetIntent, piFlag2);
            AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            am.set(AlarmManager.RTC_WAKEUP, scheduledUpdateTime, refreshPendingIntent);
            Log.d(TAG, "Scheduling update for " + scheduledUpdateTime);

            Log.d(TAG, "Created: " + appWidgetId);
        }
        super.onUpdate(context, appWidgetManager, appWidgetIds);
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        Log.d(TAG, "onDeleted");
        String packageString = context.getPackageName();

        SharedPreferences state = context.getSharedPreferences(STATE_FILE, Context.MODE_MULTI_PROCESS);
        SharedPreferences.Editor stateEditor = state.edit();
        stateEditor.remove(packageString + ".LastSMSTime");
        stateEditor.remove(packageString + ".LastMMSTime");
        stateEditor.remove(packageString + ".LastCallTime");

        SharedPreferenceList ids = new SharedPreferenceList(context, WIDGET_ID_FILE);
        for (int id : appWidgetIds) {
            String idStr = Integer.toString(id);
            // Remove entries in the state file
            stateEditor.remove(packageString + "." + idStr + ".Active");
            stateEditor.remove(packageString + "." + idStr + ".Theme");
            stateEditor.remove(packageString + "." + idStr + ".RootId");
            stateEditor.remove(packageString + "." + idStr + ".WidgetCreationTime");
            stateEditor.remove(packageString + "." + idStr + ".AdClosed");
            stateEditor.remove(packageString + "." + idStr + ".TipClosed");
            stateEditor.remove(packageString + "." + idStr + ".UpdateReason");
            stateEditor.remove(packageString + "." + idStr + ".Direction");

            // Delete the all the Log Sources
            LogSourceFactory.deleteGroup(context, id);

            // Remove from the list of widgets
            ids.removeFrom(packageString + ".WidgetIDs", idStr);
            Log.d(TAG, "Removed: " + idStr);
        }

        if (!stateEditor.commit()) {
            Log.w(TAG, "Failed to remove widget settings");
            //Toast.makeText(context, "Failed to remove widget settings", Toast.LENGTH_SHORT).show();
        }
        super.onDeleted(context, appWidgetIds);
    }

    @Override
    public void onEnabled(Context context) {
        Log.d(TAG, "onEnabled");
        super.onEnabled(context);
    }

    @Override
    public void onDisabled(Context context) {
        super.onDisabled(context);
        Log.d(TAG, "onDisabled");

        // Make sure the WidgetIDs are all removed
        SharedPreferenceList ids = new SharedPreferenceList(context, WIDGET_ID_FILE);
        int count = ids.get(context.getPackageName() + ".WidgetIDs").length;
        if (count > 0) {
            Log.w(TAG, context.getPackageName() + ".WidgetIDs should have been empty (I'll clean it up)");
            //Toast.makeText(context, "ERROR!\n" + context.getPackageName() + ".WidgetIDs should have been empty (I'll clean it up)", Toast.LENGTH_LONG).show();
        }
        ids.remove(context.getPackageName() + ".WidgetIDs");

        // Delete the state shared preferences
        SharedPreferences state = context.getSharedPreferences(STATE_FILE, Context.MODE_MULTI_PROCESS);
        SharedPreferences.Editor stateEditor = state.edit();
        stateEditor.clear();
        stateEditor.commit();

        // Delete the ad fetcher
//        AdFetcher adFetcher = new AdFetcher(context);
//        adFetcher.delete();

        // Clear all caches
        new ImageCache("NotificationCache").clear(context);
        new ImageCache("FacebookMessengerLogSource").clear(context);
        new ImageCache("HangoutsLogSource").clear(context);
        new ImageCache("WhatsAppLogSource").clear(context);
        new ImageCache("ViberLogSource").clear(context);
        new ImageCache("WeChatLogSource").clear(context);
        new ImageCache("SkypeLogSource").clear(context);
        new ImageCache("MMSParts").clear(context);
        new ImageCache("SMSLogSource").clear(context);
        new ImageCache("CallLogSource").clear(context);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        Log.d(TAG, "onReceive");
        Bundle extras = intent.getExtras();
        if (extras == null) return;
        String action = intent.getAction();

        if (Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)){
            String packageName = context.getPackageName();
            Log.d(TAG, "PACKAGE REPLACED " + packageName);

            // Clear all caches
            new ImageCache("NotificationCache").clear(context);
            new ImageCache("FacebookMessengerLogSource").clear(context);
            new ImageCache("HangoutsLogSource").clear(context);
            new ImageCache("WhatsAppLogSource").clear(context);
            new ImageCache("ViberLogSource").clear(context);
            new ImageCache("WeChatLogSource").clear(context);
            new ImageCache("SkypeLogSource").clear(context);
            new ImageCache("MMSParts").clear(context);
            new ImageCache("SMSLogSource").clear(context);
            new ImageCache("CallLogSource").clear(context);

            // Get all the combined sources from the factory
            ALogSource[] sources = LogSourceFactory.get(context, CombinedLogSource.class);
            if (sources.length == 0){
                // Nothing to update
                Log.d(TAG, "Found 0 CombinedLogSources");
                return;
            }

            // Initialize arrays that define the sources that should be in a CombinedLogSource
            Boolean[] foundSources = {false, false, false, false, false, false, false, false};
            Class[] sourcesRequired = {SMSLogSource.class, CallLogSource.class, HangoutsSource
                    .class, WhatsAppSource.class, FacebookMessengerSource.class, ViberSource.class, WeChatSource.class, SkypeSource.class};
            Class[] sourceConfigsRequired = {SMSLogSourceConfig.class, CallLogSourceConfig.class,
                    HangoutsSourceConfig.class, WhatsAppSourceConfig.class,
                    FacebookMessengerSourceConfig.class, ViberSourceConfig.class, WeChatSourceConfig.class, SkypeSourceConfig.class};
            List<Class> sourcesRequiredList = Arrays.asList(sourcesRequired);

            // Get all the widget ids
            int count = context.getResources().getInteger(R.integer.combined_source_default_count);
            Set<Integer> widgetIDs = new HashSet<>();
            TypedArray drawableArray = context.getResources().obtainTypedArray(R.array
                    .bubble_styles);
            for (ALogSource source : sources) {
                // Create a list of widget ids to later update
                Log.d(TAG, "Updating widget id " + source.config().groupID);
                widgetIDs.add(source.config().groupID);
                LogSourceConfig config = source.config();
                if (!(config instanceof CombinedLogSourceConfig)) {
                    continue;
                }
                CombinedLogSourceConfig cConfig = (CombinedLogSourceConfig) config;
                if (cConfig.sources.length <= 1) {
                    // This shouldn't really happen
                    continue;
                }
                // Initialize the foundSources array
                for (int fS=0; fS<foundSources.length; ++fS) foundSources[fS] = false;
                // Check for a source of each type
                Log.d(TAG, "    CombinedLogSource has " + cConfig.sources.length + " children");
                for (String sId : cConfig.sources){
                    ALogSource s = LogSourceFactory.get(context, sId);
                    if (s != null && sourcesRequiredList.contains(s.getClass())){
                        foundSources[sourcesRequiredList.indexOf(s.getClass())] = true;
                        Log.d(TAG, "    found child (" + s.getClass().toString() + ")");
                    }
                }
                int i=0;
                // Find any source types that are
                ArrayList<String> newIds = new ArrayList<>();
                for (Boolean f : foundSources){
                    if (!f){
                        try {
                            Log.d(TAG, "Creating a new source (" + sourceConfigsRequired[i].getClass().toString() + ")");
                            LogSourceConfig newSConfig = (LogSourceConfig) sourceConfigsRequired[i].newInstance();
                            newSConfig.sourceID = UUID.randomUUID().toString();
                            newSConfig.groupID = cConfig.groupID;
                            newSConfig.count = 0;
                            ALogSource newS = LogSourceFactory.newSource(context, sourcesRequired[i], newSConfig);
                            newIds.add(newSConfig.sourceID);
                        }catch (IllegalAccessException e){
                            Log.d(TAG, "IllegalAccessException creating the missing source type at index " + i);
                        } catch (InstantiationException e) {
                            Log.d(TAG, "InstantiationException creating the missing source type at index " + i);
                        }
                    }
                    ++i;
                }
                if (!newIds.isEmpty()) {
                    newIds.addAll(Arrays.asList(cConfig.sources));
                    LogSourceFactory.deleteSource(context, source.config().sourceID);
                    CombinedLogSourceConfig configCombined = new CombinedLogSourceConfig(cConfig.sourceID,
                            cConfig.groupID, count, newIds.toArray(new String[newIds.size()]));
                    LogSourceFactory.newSource(context, source.getClass(), configCombined);
                }
            }

            if (drawableArray != null) drawableArray.recycle();
            Log.d(TAG, "Found " + widgetIDs.size() + " widgets");

            // Update all the widgets
            AppWidgetManager man = AppWidgetManager.getInstance(context);
            int[] appWidgetIds = new int[widgetIDs.size()];
            int wIndex = 0;
            for (int wId : widgetIDs){
                appWidgetIds[wIndex++] = wId;
            }
            onUpdate(context, man, appWidgetIds);

            return;
        }

        int widgetID = extras.getInt(context.getPackageName() + ".WidgetID");
        SharedPreferences state = context.getSharedPreferences(STATE_FILE, Context.MODE_MULTI_PROCESS);
        SharedPreferences.Editor edit = state.edit();
        edit.putString(context.getPackageName() + "." + widgetID + ".UpdateReason", "");

        if (ACTION_FORCE_UPDATE.equals(action)) {
            edit.commit();
            // Update the widget
            int[] appWidgetIds = {widgetID};
            AppWidgetManager man = AppWidgetManager.getInstance(context);
            onUpdate(context, man, appWidgetIds);
        }else if (ACTION_CLOSE_TIP.equals(action) || ACTION_CLOSE_AD.equals(action)){
            long currentTime = System.currentTimeMillis();
            if (ACTION_CLOSE_AD.equals(action)){
                Log.d(TAG, "Closing ad");
                edit.putLong(context.getPackageName() + "." + widgetID + ".AdClosed", currentTime);
            }else if (ACTION_CLOSE_TIP.equals(action)) {
                Log.d(TAG, "Closing tip");
                edit.putLong(context.getPackageName() + "." + widgetID + ".TipClosed", currentTime);
            }
            edit.putString(context.getPackageName() + "." + widgetID + ".UpdateReason", "BannerClose");
            edit.commit();
            // Update the widget
            int[] appWidgetIds = {widgetID};
            AppWidgetManager man = AppWidgetManager.getInstance(context);
            onUpdate(context, man, appWidgetIds);
        }
        edit.commit();
    }

}