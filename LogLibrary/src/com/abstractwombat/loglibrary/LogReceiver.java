package com.abstractwombat.loglibrary;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Base64;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class LogReceiver extends BroadcastReceiver {
	private static final String TAG = "LogReceiver";
	public static String APP_WIDGET_UPDATE = "app_widget_update";
    private final String STATE_FILE = "State";

	@Override
	public void onReceive(Context context, Intent intent) {
        String packageName = context.getPackageName();
		String action = intent.getAction();
		String[] actionArray = action.split("\\.");
		if (actionArray.length < 1) return;
		String actionKey = actionArray[actionArray.length-1];
        Log.d(TAG, "Action: " + actionKey);

        if (actionKey.equals("refresh")){
			// Get the widget ID
			int widgetID = intent.getIntExtra(packageName + ".WidgetID", AppWidgetManager
                    .INVALID_APPWIDGET_ID);
            Log.d(TAG, "Refreshing widget id " + widgetID);

			// Get the view ID to update
			int viewID = intent.getIntExtra(packageName + ".ViewID", 0);
			if (viewID != 0){
                Log.d(TAG, "Invalidating view: " + viewID);
                AppWidgetManager man = AppWidgetManager.getInstance(context);
				man.notifyAppWidgetViewDataChanged(widgetID, viewID);
			}
			return;
		}
		if (actionKey.equals("config")){
			// Get the widget ID
			int widgetID = intent.getIntExtra(packageName +".WidgetID", AppWidgetManager.INVALID_APPWIDGET_ID);
			
			String configActivityString = context.getResources().getString(R.string.configuration_activity);//.getPackageName()+".ConfigurationActivity";
			Class<?> configActivity = null;
			try{
				configActivity = Class.forName(configActivityString);
			} catch (ClassNotFoundException x){
				x.printStackTrace();
			}
			if (configActivity != null){
				Intent configIntent = new Intent(context, configActivity);
				configIntent.putExtra(packageName + ".WidgetID", widgetID);
				configIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				context.startActivity(configIntent);
			}
			return;
		}
        if (actionKey.equals("launch_package")){
            // Get the package
            String launchPackage = intent.getStringExtra(packageName + ".PackageName");
            if (launchPackage != null && !launchPackage.isEmpty()){
                PackageManager manager = context.getPackageManager();
                Intent launchIntent = manager.getLaunchIntentForPackage(launchPackage);
                if (launchIntent != null) {
                    launchIntent.addCategory(Intent.CATEGORY_LAUNCHER);
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(launchIntent);
                }
            }
        }
        if (actionKey.equals("click_banner")){
            String uri = intent.getStringExtra(packageName + ".uri");
            if (uri == null || uri.length() == 0) {
                return;
            }
            Intent bannerIntent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(uri)); //< "market://details?id=package"
            context.startActivity(bannerIntent);
            return;
        }
        if (actionKey.equals("upgrade")){
            String upgradePackage = intent.getStringExtra(packageName + ".UpgradePackage");
            if (upgradePackage == null || upgradePackage.length() == 0){
                Log.d(TAG, "Invalid upgrade package");
                return;
            }
            Intent upgradeIntent = new Intent(Intent.ACTION_VIEW);
            upgradeIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            upgradeIntent.setData(Uri.parse("market://details?id=" + upgradePackage));
            context.startActivity(upgradeIntent);
            return;
        }
        if (actionKey.equals("trial_end")){
            Log.d(TAG, "Trial has expired");
            int widgetID = intent.getIntExtra(packageName +".WidgetID", AppWidgetManager.INVALID_APPWIDGET_ID);
            ALogSource[] sources = LogSourceFactory.get(context, widgetID);
            // Disable premium sources
            for (ALogSource source : sources){
                if (Licensing.requiresPremiumLicense(context, source)){
                    source.config().count = 0;
                    LogSourceFactory.deleteSource(context, source.config().sourceID);
                    LogSourceFactory.newSource(context, source.getClass(), source.config());
                }
            }
        }
		
		
		// The action must be a source ID
		String sourceID = actionKey;

		// Route the intent to the given Source ID using the Widget ID's factory
		ALogSource source = LogSourceFactory.get(context, sourceID);
		if (source != null){
			source.receiveIntent(context, intent);
		}

        showMarketingToast(context);
	}

    void showMarketingToast(final Context context){
        SharedPreferences sp = context.getSharedPreferences("marketing", Activity.MODE_MULTI_PROCESS);
        String adText = sp.getString("ad_text", "");
        String adImage = sp.getString("ad_image", "");
        String adLink = sp.getString("ad_link", "");
        if (adText.length() > 0 || adText.length() > 0){
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View layout = inflater.inflate(R.layout.marketing_toast, null);

            LinearLayout root = (LinearLayout) layout.findViewById(R.id.marketing_toast_root);
            TextView text = (TextView) layout.findViewById(R.id.marketing_toast_text);
            ImageView image = (ImageView) layout.findViewById(R.id.marketing_toast_image);

            if (adText.length() > 0) {
                text.setText("This is a custom toast");
            }else{
                text.setVisibility(View.GONE);
            }
            if (adImage.length() > 0){
                byte[] b = Base64.decode(adImage, Base64.DEFAULT);
                Bitmap bitmap = BitmapFactory.decodeByteArray(b, 0, b.length);
                image.setImageBitmap(bitmap);
            }else{
                image.setVisibility(View.GONE);
            }
            if (adLink.length() > 0){
                final String fAdLink = adLink;
                root.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        intent.setData(Uri.parse(fAdLink)); //< "market://details?id=package"
                        context.startActivity(intent);
                    }
                });
            }

            Toast toast = new Toast(context);
            toast.setGravity(Gravity.BOTTOM | Gravity.FILL_HORIZONTAL | Gravity.LEFT, 0, 0);
            toast.setDuration(Toast.LENGTH_LONG);
            toast.setView(layout);
            toast.show();
        }
    }
}
