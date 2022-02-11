package com.abstractwombat.logwidget;

import android.app.Notification;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import com.abstractwombat.contacts.ContactUtilities;
import com.abstractwombat.loglibrary.ALogSource;
import com.abstractwombat.loglibrary.Call;
import com.abstractwombat.loglibrary.CallLogSource;
import com.abstractwombat.loglibrary.CombinedLogSource;
import com.abstractwombat.loglibrary.HangoutsSource;
import com.abstractwombat.loglibrary.LogSourceFactory;
import com.abstractwombat.loglibrary.NotificationSource;
import com.abstractwombat.loglibrary.SMS;
import com.abstractwombat.loglibrary.SMSLogSource;

import java.util.Map;
import java.util.Random;

/**
 * Created by Michael on 9/14/2015.
 */
public class Testing {
    private static String TAG = "Testing";
    private static Random random = new Random();
    private static final int testEntryCount = 8;
    private static final int testMessageCount = 14;
    private static final int testNameCount = 8;

    public static void createTestEntries(Context context, int widgetID){
        SharedPreferences settings = context.getSharedPreferences("State", Context
                .MODE_MULTI_PROCESS);
        String rootIdKey = context.getPackageName() + "." + Integer.toString(widgetID) + ".RootId";
        Log.d(TAG, "Getting root id source - " + rootIdKey);
        String combinedID = settings.getString(rootIdKey, "");
        if (combinedID == ""){
            Log.d(TAG, "No root source found");
            return;
        }
        ALogSource rootSource = LogSourceFactory.get(context, combinedID);
        ALogSource[] sources = LogSourceFactory.get(context, widgetID);
        if (sources.length == 0) return;


        long time = System.currentTimeMillis();
        long timeIncrement = 1000 * 60 * 4;
        int lastNameIndex = -1;
        int count = rootSource.config().count;
        if (count == 0) return;
        int sourceInc = 0;
        for (int i = 0; i < count; ++i) {
            ALogSource source = sources[ /*sourceInc++*/random.nextInt(sources.length)];
            if (sourceInc >= sources.length) sourceInc = 0;
            if (source instanceof CombinedLogSource) continue;
            int messageIndex = random.nextInt(testMessageCount + 1);
            int nameIndex = -1;
            do {
                nameIndex = random.nextInt(testNameCount + 1);
            } while (nameIndex == lastNameIndex);
            lastNameIndex = nameIndex;
            String message = getTestMessage(messageIndex);
            String name = getTestName(nameIndex);
            addTestEntry(context, source, time, name, message);
            Log.d(TAG, "Created entry at " + time);
            time -= timeIncrement;
        }
    }

    private static void addTestEntry(Context context, ALogSource source, long time, String name, String message){
        if (source instanceof NotificationSource){
            addTestEntry(context, (NotificationSource)source, time, name, message);
        }else if (source instanceof SMSLogSource){
            addTestEntry(context, (SMSLogSource)source, time, name, message);
        }else if (source instanceof CallLogSource){
            addTestEntry(context, (CallLogSource)source, time, name, message);
        }
    }

    private static void addTestEntry(Context context, CallLogSource source, long time, String name, String message) {
        Call call = new Call();
        call.date = time;
        do {
            call.type = random.nextInt(4);
        }while (call.type <= 0);
        call.duration = (long)random.nextInt(500 * 1000);
        String lookup = ContactUtilities.getContactByName(name, context);
        if (lookup == null) {
            call.number = "303";
            for (int i=0; i<7; i++) call.number += Integer.toString(random.nextInt(10));
            call.location = "Colorado";
        }else{
            Map<String,String> numbers = ContactUtilities.getContactPhoneNumbers(lookup, context, false);
            if (numbers != null) {
                call.number = numbers.entrySet().iterator().next().getValue();
            }
        }
        source.insertCall(call, "Mobile");
    }

    private static void addTestEntry(Context context, SMSLogSource source, long time, String name, String message) {
        SMS sms = new SMS();
        sms.date = time;
        sms.message = message;
        sms.incoming = true;
        sms.name = name;
        sms.contactLookupKey = ContactUtilities.getContactByName(name, context);
        if (sms.contactLookupKey != null) {
            Map<String, String> numbers = ContactUtilities.getContactPhoneNumbers(sms.contactLookupKey, context, false);
            if (numbers != null) {
                sms.number = numbers.entrySet().iterator().next().getValue();
            }
        }
        source.insertSMS(sms);
    }

    private static void addTestEntry(Context context, NotificationSource source, long time, String name, String message) {
        Notification notification = new Notification();
        notification.when = time;

        Bundle bundle = new Bundle();
        bundle.putString(Notification.EXTRA_TITLE, name);
        bundle.putStringArray(Notification.EXTRA_PEOPLE, new String[]{name});
        bundle.putCharSequence(Notification.EXTRA_TEXT, message);
        if (source instanceof HangoutsSource && name.equals("Karen")){
            Bitmap bm = BitmapFactory.decodeResource(context.getResources(), R.drawable.abstract_wombat);
            bundle.putParcelable(Notification.EXTRA_PICTURE, bm);
        }
        notification.extras = bundle;

        StatusBarNotification sbNotification = new StatusBarNotification(source.getPackage(), source.getPackage(), 0, "", 0, 0, 0, notification, android.os.Process.myUserHandle(), time);
        source.addNotification(sbNotification);
    }

    private static String getTestName(int index){
        switch(index){
            case 0: return "Aunt Mira";
            case 1: return "Eddie";
            case 2: return "Charlie";
            case 3: return "Graham";
            case 4: return "Grampa Joe";
            case 5: return "Uncle Bill";
            case 6: return "Karol";
            case 7: return "Linda";
        }
        return "Scott";
    }

    private static String getTestMessage(int index){
        switch(index){
            case 0: return "Hey this is a test message for testing";
            case 1: return "Hey, how's it going. Could you call me?";
            case 2: return "Hey, I'm going to ramble a little so that this is a long message... hence the rambling. So I'm still going, maybe that's enough?";
            case 3: return "Hi there!";
            case 4: return "Call me!";
            case 5: return "Jennifer is looking for you. \uD83D\uDE1E Where are you? You should probably be here, what with the stuff and all that.";
            case 6: return "LMAO!";
            case 7: return "I love horses!?";
            case 8: return "Pancakes ARE delicious! How dare you!";
            case 9: return "Yep, I'll be there...";
            case 10: return "Could you bring a frisbee? We really need a frisbee.";
            case 11: return "But where did all the fish come from?";
            case 12: return "Wrong number! Stop messaging me!";
            case 13: return "Yeah, he's fine. Why?";
            case 14: return "Where's Jim? He never answers me... Is Jim real?";
        }
        return "Yo!";
    }

}
