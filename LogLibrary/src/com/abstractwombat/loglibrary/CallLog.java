package com.abstractwombat.loglibrary;

import android.content.Context;
import android.database.Cursor;
import android.provider.ContactsContract;
import android.telephony.PhoneNumberUtils;
import android.util.Log;

import com.abstractwombat.contacts.ContactUtilities;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

public class CallLog {

    static final String[] CALL_LOG_PROJECTION = new String[]{
            android.provider.CallLog.Calls.DATE,
            android.provider.CallLog.Calls.DURATION,
            android.provider.CallLog.Calls.NUMBER,
            android.provider.CallLog.Calls.TYPE,
            "countryiso", //android.provider.CallLog.Calls.COUNTRY_ISO,
            "geocoded_location", //android.provider.CallLog.Calls.GEOCODED_LOCATION,
    };
    static final String[] CALL_LOG_PROJECTION_SAFER = new String[]{
            android.provider.CallLog.Calls.DATE,
            android.provider.CallLog.Calls.DURATION,
            android.provider.CallLog.Calls.NUMBER,
            android.provider.CallLog.Calls.TYPE,
    };
    static final int DATE_COLUMN_INDEX = 0;
    static final int DURATION_COLUMN_INDEX = 1;
    static final int NUMBER_COLUMN_INDEX = 2;
    static final int CALL_TYPE_COLUMN_INDEX = 3;
    static final int COUNTRY_ISO_COLUMN_INDEX = 4;
    static final int GEOCODED_LOCATION_COLUMN_INDEX = 5;

    private static Cursor getCallLogCursor(Context context, String whereQuery, String sortQuery) {
        // Get the CallLog Cursor
        Cursor callLogCursor;
        try {
            callLogCursor = context.getContentResolver().query(
                    android.provider.CallLog.Calls.CONTENT_URI, CALL_LOG_PROJECTION,
                    whereQuery, null, sortQuery);
        } catch (Exception e) {
            callLogCursor = context.getContentResolver().query(
                    android.provider.CallLog.Calls.CONTENT_URI, CALL_LOG_PROJECTION_SAFER,
                    whereQuery, null, sortQuery);
        }

        if (callLogCursor == null) {
            Log.d("CallLog", "Failed to get Call Log cursor");
            return null;
        }
        if (!callLogCursor.moveToFirst()) {
            callLogCursor.close();
            Log.d("CallLog", "Failed to move Call Log cursor");
            return null;
        }

        return callLogCursor;
    }

    public static CallsByNumber[] getCallsByNumber(Context context, boolean removeAllDuplicates,
                                                   String[] contactLookupKeys,
                                                   long minDate, int max) {
        int[] callTypes = {};
        return getCallsByNumber(context, removeAllDuplicates, contactLookupKeys, callTypes,
                minDate, max);
    }

    public static CallsByNumber[] getCallsByNumber(Context context, boolean removeAllDuplicates,
                                                   String[] contactLookupKeys, int[] callTypes,
                                                   long minDate, int max) {
        Log.d("CallLog", "Getting Call Log by Number");

        String sort = android.provider.CallLog.Calls.DATE + " DESC";
        String where = null;
        if (minDate > 0) {
            where = android.provider.CallLog.Calls.DATE + " > " + Long.toString(minDate);
        }
        Cursor callLogCursor = getCallLogCursor(context, where, sort);
        if (callLogCursor == null) {
            return new CallsByNumber[0];
        }
        Vector<CallsByNumber> calls = new Vector<CallsByNumber>();

        // Find all the phone numbers for the given contacts
        Set<String> validNumbers = null;
        if (contactLookupKeys != null && contactLookupKeys.length > 0){
            validNumbers = new HashSet<String>();
            for (String key : contactLookupKeys){
                Map<String,String> ns = ContactUtilities.getContactPhoneNumbers(key, context, false);
                if (ns != null){
                    for (String number : ns.values()) {
                        validNumbers.add(number);
                    }
                }
            }
        }

        // Create a list of valid call types
        ArrayList<Integer> validCallTypes = new ArrayList<Integer>();
        for (int t : callTypes) {
            validCallTypes.add(t);
        }
        if (validCallTypes.isEmpty()) {
            validCallTypes.add(android.provider.CallLog.Calls.INCOMING_TYPE);
            validCallTypes.add(android.provider.CallLog.Calls.OUTGOING_TYPE);
            validCallTypes.add(android.provider.CallLog.Calls.MISSED_TYPE);
        }

        do {
            Call c = cursorToCall(callLogCursor);
            Log.d("CallLog", "Call Itr - num:" + c.number);

            if (!validCallTypes.contains(c.type)) {
                continue;
            }

            // Strip off the country code if it's from the same country as the SIM card
            c.number = ContactUtilities.formatPhoneNumberForStorage(c.number);

            boolean goodNumber = false;
            if (validNumbers == null){
                goodNumber = true;
            }else{
                for (String number : validNumbers) {
                    if (ContactUtilities.equalPhoneNumbers(number, c.number)){
                        goodNumber = true;
                        break;
                    }
                }
            }
            if (!goodNumber){
                continue;
            }

            CallsByNumber callByNumber = null;
            if (calls.size() > 0) {
                CallsByNumber tempCBN = new CallsByNumber(c.number);
                if (removeAllDuplicates) {
                    // All duplicates
                    int i = calls.indexOf(tempCBN);
                    if (i != -1) {
                        callByNumber = calls.elementAt(i);
                    }
                } else {
                    // Just consecutive duplicates
                    if (tempCBN.equals(calls.get(calls.size() - 1))) {
                        callByNumber = calls.get(calls.size() - 1);
                    }
                }
            }

            if (callByNumber == null) {
                callByNumber = new CallsByNumber(c.number);
                callByNumber.calls.add(c);
                calls.add(callByNumber);
            } else {
                callByNumber.calls.add(c);
            }
        } while (calls.size() < max && callLogCursor.moveToNext());

        callLogCursor.close();

        int i = 0;
        for (CallsByNumber cb : calls) {
            Call c = cb.calls.elementAt(0);
            Log.d("CallLog", "Call #" + i++ + "(" + cb.calls.size() + ") at " + c.date + " from "
                    + c.number + " for duration " + c.duration);
        }

        CallsByNumber[] entries = new CallsByNumber[calls.size()];
        entries = calls.toArray(entries);
        return entries;
    }

    public static CallsByContact[] getCallsByContact(Context context,
                                                     boolean removeAllDuplicates, long minDate,
                                                     int max) {
        Log.d("CallLog", "Getting Call Log by Contact");

        String sort = android.provider.CallLog.Calls.DATE + " DESC";
        String where = null;
        if (minDate > 0) {
            where = android.provider.CallLog.Calls.DATE + " > " + Long.toString(minDate);
        }
        Cursor callLogCursor = getCallLogCursor(context, where, sort);
        if (callLogCursor == null) {
            return null;
        }
        Vector<CallsByContact> calls = new Vector<CallsByContact>();
        Map<String, String> keyMap = new HashMap<String, String>();

        do {
            Call c = cursorToCall(callLogCursor);

            // Look up the contact's quick lookup key by number
            String key = "";
            boolean foundKey = false;
            if (keyMap.containsKey(c.number)) {
                key = keyMap.get(c.number);
            } else {
                String[] contactData = ContactUtilities.getContactDataByPhoneNumber(c.number,
                        context, new String[]{ContactsContract.Contacts.LOOKUP_KEY});
                if (contactData != null && contactData.length > 0) {
                    key = contactData[0];
                    keyMap.put(c.number, key);
                }
            }
            if (key.equals("")) {
                key = c.number;
            } else {
                foundKey = true;
            }

            CallsByContact callByContact = null;
            if (calls.size() > 0) {
                CallsByContact tempCBC = new CallsByContact(key);
                if (removeAllDuplicates) {
                    // All duplicates
                    int i = calls.indexOf(tempCBC);
                    if (i != -1) {
                        callByContact = calls.elementAt(i);
                    }
                } else {
                    // Just consecutive duplicates
                    CallsByContact lastCBC = calls.get(calls.size() - 1);
                    if (tempCBC.equals(lastCBC)) {
                        callByContact = lastCBC;
                    }
                }
            }

            if (calls.size() == 0 || callByContact == null) {
                callByContact = new CallsByContact(key);
                callByContact.calls.add(c);
                callByContact.isContact = foundKey;
                calls.add(callByContact);
            } else {
                callByContact.calls.add(c);
            }
        } while (calls.size() < max && callLogCursor.moveToNext());

        callLogCursor.close();

        CallsByContact[] entries = new CallsByContact[calls.size()];
        entries = calls.toArray(entries);
        return entries;
    }

    public static Call[] getCalls(Context context, String[] contactLookupKeys, int max) {
        int[] callTypes = {};
        return getCalls(context, contactLookupKeys, callTypes, max);
    }

    public static Call[] getCalls(Context context, String[] contactLookupKeys, int[] callTypes,
                                  int max) {
        Log.d("CallLog", "Getting Call Log for a specified contact");

        // Get a cursor into the call log
        Cursor callLogCursor = getCallLogCursor(context, null, android.provider.CallLog.Calls
                .DATE + " DESC");
        if (callLogCursor == null) {
            return null;
        }

        ArrayList<Integer> validCallTypes = new ArrayList<Integer>();
        for (int t : callTypes) {
            validCallTypes.add(t);
        }
        if (validCallTypes.isEmpty()) {
            validCallTypes.add(android.provider.CallLog.Calls.INCOMING_TYPE);
            validCallTypes.add(android.provider.CallLog.Calls.OUTGOING_TYPE);
            validCallTypes.add(android.provider.CallLog.Calls.MISSED_TYPE);
        }

        // Find all the phone numbers for the given contacts
        Set<String> validNumbers = null;
        if (contactLookupKeys != null && contactLookupKeys.length > 0){
            validNumbers = new HashSet<String>();
            for (String key : contactLookupKeys){
                Map<String,String> ns = ContactUtilities.getContactPhoneNumbers(key, context, false);
                if (ns != null) {
                    for (String number : ns.values()) {
                        validNumbers.add(number);
                    }
                }
            }
        }

        ArrayList<Call> callList = new ArrayList<Call>();
        int i = 0;
        do {
            Call c = cursorToCall(callLogCursor);
            Log.d("CallLog", "Call From: " + c.number);

            if (!validCallTypes.contains(c.type)) {
                continue;
            }

            // Strip off the country code if it's from the same country as the SIM card
            c.number = ContactUtilities.formatPhoneNumberForStorage(c.number);

            // Check if this number is one of the contact's
            boolean good = false;
            if (validNumbers == null) {
                good = true;
            } else {
                for (String number : validNumbers) {
                    if (ContactUtilities.equalPhoneNumbers(number, c.number)) {
                        good = true;
                        break;
                    }
                }
            }
            if (good) {
                Log.d("CallLog", "Adding Call From: " + c.number);
                i++;
                callList.add(c);
            }
        } while (i < max && callLogCursor.moveToNext());

        callLogCursor.close();
        Call[] entries = new Call[callList.size()];
        entries = callList.toArray(entries);
        return entries;
    }

    public static Call[] getCalls(Context context, int max) {
        Log.d("CallLog", "Getting " + max + " Call Log entries");

        Cursor callLogCursor = getCallLogCursor(context, null, android.provider.CallLog.Calls.DATE + " DESC LIMIT " + Integer.toString(max));
        if (callLogCursor == null) {
            return null;
        }

        int i = 0;
        ArrayList<Call> callList = new ArrayList<Call>();
        do {
            Call c = cursorToCall(callLogCursor);
            i++;
            callList.add(c);
        } while (i < max && callLogCursor.moveToNext());

        callLogCursor.close();
        Call[] entries = new Call[callList.size()];
        entries = callList.toArray(entries);
        return entries;
    }

    private static Call cursorToCall(Cursor callLogCursor) {
        Call c = new Call();
        c.date = callLogCursor.getLong(DATE_COLUMN_INDEX);
        c.number = callLogCursor.getString(NUMBER_COLUMN_INDEX);
        c.duration = callLogCursor.getLong(DURATION_COLUMN_INDEX);
        c.type = callLogCursor.getInt(CALL_TYPE_COLUMN_INDEX);
        c.location = "";
        try {
            String geocode = callLogCursor.getString(GEOCODED_LOCATION_COLUMN_INDEX);
            if (geocode != null && !geocode.equals("")) {
                c.location = geocode;
            } else {
                String country = callLogCursor.getString(COUNTRY_ISO_COLUMN_INDEX);
                if (country != null && !country.equals("")) {
                    c.location = country;
                }
            }
        } catch (Exception e) {
            return new Call();
        }
        return c;
    }
}
