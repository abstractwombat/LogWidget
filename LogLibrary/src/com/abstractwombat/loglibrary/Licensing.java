package com.abstractwombat.loglibrary;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

/**
 * Created by Michael on 8/19/2015.
 */
public class Licensing {
    private static final String TAG = "Licensing";
    private static final String STATE_FILE = "State";

    public static boolean sourcesLicensed(Context context){
        return true;
//        String packageName = context.getPackageName();
//        String premiumKey = packageName + ".IsPremium";
//        String unlockAllKey = packageName + ".UnlockAll";
//        String buildTypeKey = packageName + ".BuildType";
//        SharedPreferences state = context.getSharedPreferences(STATE_FILE, Context.MODE_MULTI_PROCESS);
//        if (state.getInt(buildTypeKey, 0) != 0){
//            return true;
//        }
//        boolean licensed = state.getBoolean(premiumKey, false) || state.getBoolean(unlockAllKey, false);
//        if (licensed) return true;
//        if (LicenseTrial.trialStarted(context) && !LicenseTrial.isExpired(context)){
//            return true;
//        }else {
//            return false;
//        }
    }

    public static boolean adsRemoved(Context context){
        return true;
    }

    public static boolean requiresPremiumLicense(Context context, ALogSource source){
        return false;
//        String buildTypeKey = context.getPackageName() + ".BuildType";
//        SharedPreferences state = context.getSharedPreferences(STATE_FILE, Context.MODE_MULTI_PROCESS);
//        if (state.getInt(buildTypeKey, 0) != 0){
//            return false;
//        }
//        Class c = source.getClass();
//        if (HangoutsSource.class.equals(c) || WhatsAppSource.class.equals(c) ||
//                FacebookMessengerSource.class.equals(c) || ViberSource.class.equals(c) ||
//                WeChatSource.class.equals(c) || SkypeSource.class.equals(c)) {
//            return true;
//        }
//        return false;
    }

    public static boolean isLicensed(ALogSource source, Context context) {
        return true;
//        if (requiresPremiumLicense(context, source)){
//            // Check if licensed
//            if (sourcesLicensed(context)){
//                Log.d(TAG, "Source is licensed");
//                return true;
//            }
//            // Check if the trial hasn't started
//            if (!LicenseTrial.trialStarted(context)){
//                Log.d(TAG, "Trial hasn't started");
//                return false;
//            }
//            // Check if the trial is expired
//            if (LicenseTrial.isExpired(context)){
//                Log.d(TAG, "Trial is expired");
//                return false;
//            }else{
//                Log.d(TAG, "Trial is ongoing");
//                return true;
//            }
//        }else {
//            Log.d(TAG, "Source doesn't require a license");
//            return true;
//        }
    }
}
