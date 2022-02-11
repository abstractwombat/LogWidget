package com.abstractwombat.library;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.abstractwombat.iab.IabHelper;

/**
 *  IabChecker
 *      Schedules an alarm to check the given package is valid using the IabHelper
 *
 * Created by Michael on 7/13/2015.
 */
public class IabChecker extends BroadcastReceiver{

    private Context mContext;
    private String mProductId;
    private Integer mRefreshMs;
    private String mSharedPreferenceFile;
    private String mVerificationUrl;

    public IabChecker(Context context, String productId){
        mContext = context;
        mProductId = productId;
        setAlarm(context);
    }

    public void setRefreshMs(Integer refreshMs) {
        this.mRefreshMs = refreshMs;
    }

    public void setSharedPreferenceFile(String sharedPreferenceFile) {
        this.mSharedPreferenceFile = sharedPreferenceFile;
    }

    public void setVerificationUrl(String verificationUrl) {
        this.mVerificationUrl = verificationUrl;
    }

    private void setAlarm(Context context){
        Intent updateIntent = new Intent(context, this.getClass());
        updateIntent.putExtra(context.getPackageName() + ".IabChecker.productId", mProductId);
        updateIntent.putExtra(context.getPackageName() + ".IabChecker.refreshMs", mRefreshMs);
        updateIntent.putExtra(context.getPackageName() + ".IabChecker.sharedPreferenceFile", mSharedPreferenceFile);
        PendingIntent refreshPendingIntent = PendingIntent.getBroadcast(context, 0, updateIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        am.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis()+mRefreshMs, mRefreshMs, refreshPendingIntent);
    }

    private void licenseCheckStart(){
        IabHelper helper = new IabHelper(mContext, mVerificationUrl);
        //mIabHelper.launchPurchaseFlow(this, IAP_PRODUCT_REMOVE_AD, 1001, this);
    }

    @Override
    public void onReceive(Context context, Intent intent) {

    }
}
