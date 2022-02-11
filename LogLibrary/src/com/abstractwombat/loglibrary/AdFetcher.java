package com.abstractwombat.loglibrary;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.util.Log;

import com.abstractwombat.networking.HttpForJson;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONObject;

/**
 * Created by Mike on 4/24/2015.
 */
public class AdFetcher implements HttpForJson.PostReceiver {
    private final String TAG = "AdFetcher";
    private final String POST_URL = "http://www.abstractwombat.com/android/logwidget.php";
    private final String POST_URL_DEBUG = "http://www.abstractwombat.com/android/logwidget.debug.php";
    private final String SHARED_PREFERENCE_FILE = "State";

    private Context mContext;
    private SharedPreferences mPrefs;

    public class AdData {
        public String text;
        public Bitmap image;
        public String uri;
        public Long reassert;
        public boolean closeable;
        public Long lastUpdate;
    }

    public AdFetcher(Context context) {
        mContext = context;
        mPrefs = mContext.getSharedPreferences(SHARED_PREFERENCE_FILE, Context.MODE_MULTI_PROCESS);
        if (!isAvailable()){
            refresh();
        }else {
            // Reaffirm the update alarm
            String packageName = mContext.getPackageName();
            long lastUpdate = mPrefs.getLong(packageName + ".BannerLastUpdate", 0);
            long ttl = mPrefs.getLong(packageName + ".BannerTTL", 0);
            if (lastUpdate > 0 && ttl > 0) {
                scheduleUpdate(lastUpdate + ttl);
            }
        }
    }

    public void refresh(){
        Log.d(TAG, "Refresh");
        NameValuePair packagePair = new BasicNameValuePair("package", mContext.getPackageName());
        NameValuePair keyPair = new BasicNameValuePair("key", generateKey(System.currentTimeMillis()));
        if (mContext.getPackageName().contains("debug")){
            Log.d(TAG, "Requesting banner from " + POST_URL_DEBUG);
            new HttpForJson(POST_URL_DEBUG).preformAsync(this, packagePair, keyPair);
        }else {
            Log.d(TAG, "Requesting banner from " + POST_URL);
            new HttpForJson(POST_URL).preformAsync(this, packagePair, keyPair);
        }
    }

    public boolean isAvailable(){
        String text = mPrefs.getString(mContext.getPackageName() + ".BannerText", "");
        Log.d(TAG, "isAvailable, text=" + text);
        return text.length() > 0;
    }

    public AdData getData(){
        Log.d(TAG, "getData");

        AdData d = new AdData();
//        d.text = "This is a test of the banner thingy, did it work?";
//        d.image = null;
//        d.uri = "";
//        d.closeable = true;
//        return d;

        String packageName = mContext.getPackageName();
        d.text = mPrefs.getString(packageName + ".BannerText", "");
        d.uri = mPrefs.getString(packageName + ".BannerUri", "");
        d.closeable = mPrefs.getBoolean(packageName + ".BannerCloseable", false);
        d.reassert = mPrefs.getLong(packageName + ".BannerReassert", 0);
        d.lastUpdate = mPrefs.getLong(packageName + ".BannerLastUpdate", 0);

        String imageString = mPrefs.getString(packageName + ".BannerImage", "");
        byte[] decoded = Base64.decode(imageString, Base64.DEFAULT);
        d.image = BitmapFactory.decodeByteArray(decoded, 0, decoded.length);

        Log.d(TAG, "Image dimensions: " + d.image.getWidth() + "x" + d.image.getHeight());
        return d;
    }

    public void delete(){
        Log.d(TAG, "Delete");
        AlarmManager am = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);
        am.cancel(getUpdateIntent());
    }

    private String generateKey(long msTime){
        long msInAnHour = 1000 * 60 * 60;
        long hour = msTime / msInAnHour;

        hour *= 453;
        hour ^= 34532;
        Log.d(TAG, "Generated key: " + hour);
        return String.valueOf(hour);
    }

    @Override
    public void receiveJSON(JSONObject json) {
        if (json == null || json.length() == 0) {
            Log.d(TAG, "Null JSON received");
            return;
        }
        String text = json.optString("text", "");
        String image = json.optString("image", "");
        String uri = json.optString("uri", "");
        String key = json.optString("key", "");
        long ttl = json.optLong("ttl", 0);
        long reassert = json.optLong("reassert", 0);
        boolean closeable = json.optBoolean("closeable", true);
        long lastUpdate = System.currentTimeMillis();

        Log.d(TAG, "receiveJSON, text=" + text);

        // Verify the key
        long msTime = System.currentTimeMillis();
        String myKey = generateKey(msTime);
        Log.d(TAG, "Received key: " + key + " Generated key: " + myKey);
        if (!myKey.equals(key)){
            // Check the previous hour
            myKey = generateKey(msTime-(1000*60*60));
            if (!myKey.equals(key)) {
                Log.d(TAG, "Invalid key from server!");
                return;
            }
        }

        // Store the data in shared preferences
        SharedPreferences.Editor e = mPrefs.edit();
        String packageName = mContext.getPackageName();
        e.putString(packageName + ".BannerText", text);
        e.putString(packageName + ".BannerImage", image);
        e.putString(packageName + ".BannerUri", uri);
        e.putBoolean(packageName + ".BannerCloseable", closeable);
        e.putLong(packageName + ".BannerReassert", reassert);
        e.putLong(packageName + ".BannerLastUpdate", lastUpdate);
        e.putLong(packageName + ".BannerTTL", ttl);
        e.commit();

        // Schedule the next update
        if (ttl > 0) {
            scheduleUpdate(lastUpdate + ttl);
        }

        Log.d(TAG, "TTL: " + ttl + " Last: " + lastUpdate + " Reassert: " + reassert);
    }

    // Schedule an ad fetch
    private void scheduleUpdate(long when){
        if (when <= System.currentTimeMillis()){
            Log.d(TAG, "Failed to schedule an ad update (time in the past)");
            return;
        }
        AlarmManager am = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);
        PendingIntent updateIntent = getUpdateIntent();
        am.set(AlarmManager.RTC_WAKEUP, when, updateIntent);
        Log.d(TAG, "Scheduling ad update at " + when);
    }

    private PendingIntent getUpdateIntent(){
        Intent updateIntent = new Intent(mContext, AdUpdater.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(mContext, 0, updateIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        return pendingIntent;
    }

    // Receiver to update the ad data
    public static class AdUpdater extends BroadcastReceiver{
        public AdUpdater(){}

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d("AdUpdater", "AdUpdater onReceive");
            AdFetcher adFetcher = new AdFetcher(context);
            adFetcher.refresh();
        }
    }
}
