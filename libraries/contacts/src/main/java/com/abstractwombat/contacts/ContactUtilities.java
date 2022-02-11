package com.abstractwombat.contacts;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.ContactsContract.PhoneLookup;
import android.telephony.PhoneNumberUtils;
import android.util.Log;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ContactUtilities {

    /**
     * Valid columnNames are here http://developer.android.com/reference/android/provider/ContactsContract.Profile.html
     * i.e. ContactsContract.ContactNameColumns.DISPLAY_NAME_PRIMARY, PHONETIC_NAME
     *      ContactsContract.ContactsColumns.LOOKUP_KEY
     *
     * @param context
     * @param columnNames
     * @return
     */
    public static String[] getProfile(Context context, String[] columnNames){
        Cursor c = context.getContentResolver().query(ContactsContract.Profile.CONTENT_URI, columnNames, null, null, null);
        if (c == null) return new String[0];
        if (!c.moveToFirst()) {
            c.close();
            return null;
        }
        String[] result = new String[columnNames.length];
        for (int i = 0; i < columnNames.length; i++) {
            result[i] = c.getString(c.getColumnIndex(columnNames[i]));
        }
        c.close();
        return result;
    }

    public static String[] getContactData(String lookupKey, Context context, String[] columnNames) {
        ContentResolver cr = context.getContentResolver();
        Uri uri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_LOOKUP_URI, lookupKey);
        if (uri == null) return new String[0];
        return getContactData(context, uri, columnNames);
    }

    public static String[] getContactData(Context context, Uri uri, String[] columnNames) {
        Cursor contact = context.getContentResolver().query(uri, columnNames, null, null, null);
        if (contact == null) return new String[0];
        if (!contact.moveToFirst()) {
            contact.close();
            return null;
        }

        String[] result = new String[columnNames.length];
        for (int i = 0; i < columnNames.length; i++) {
            result[i] = contact.getString(contact.getColumnIndex(columnNames[i]));
        }
        contact.close();
        return result;
    }

    public static Integer[] getRawIds(Context context, Uri lookupUri) {
        ContentResolver cr = context.getContentResolver();

        // Query the contacts table to get the contact ID
        Cursor dataCursor = cr.query(lookupUri,
                new String[]{ContactsContract.Contacts._ID},
                null, null, null);
        ArrayList<Integer> dataIds = new ArrayList<>();
        try {
            if (dataCursor != null && dataCursor.moveToFirst()) {
                do {
                    dataIds.add(dataCursor.getInt(0));
                } while (dataCursor.moveToNext());
            }
        }finally {
            if (dataCursor != null) dataCursor.close();
        }
        if (dataIds.size() == 0) return new Integer[0];
        Log.d("getRawIds", "Data ids " + dataIds.toString());

        // Query the raw_contacts table to get the ids matching this contact ids
        String where = "";
        for (Integer id : dataIds){
            where += ContactsContract.RawContacts.CONTACT_ID + " == " + id + " AND ";
        }
        where = where.substring(0, where.length()-5);

        Cursor rawCursor = cr.query(ContactsContract.RawContacts.CONTENT_URI,
                new String[]{ContactsContract.RawContacts._ID},
                where,
                null, null);
        ArrayList<Integer> rawContactIds = new ArrayList<>();
        try {
            if (rawCursor != null && rawCursor.moveToFirst()) {
                do {
                    rawContactIds.add(rawCursor.getInt(0));
                } while (rawCursor.moveToNext());
            }
        }finally {
            if (rawCursor != null) rawCursor.close();
        }
        if (rawContactIds.size() == 0) return new Integer[0];
        Log.d("getRawIds", "Raw ids " + rawContactIds.toString());


        return rawContactIds.toArray(new Integer[rawContactIds.size()]);
    }

    public static Integer[] getRawIds(Context context, String name) {
        if (name == null || name.length() == 0) return new Integer[0];
        ContentResolver cr = context.getContentResolver();

        // Query the raw_contacts table to get the ids matching this name
        Cursor rawCursor = cr.query(ContactsContract.RawContacts.CONTENT_URI,
                new String[]{ContactsContract.RawContacts._ID},
                ContactsContract.RawContacts.DISPLAY_NAME_PRIMARY + " LIKE '" + name + "'",
                null, null);
        ArrayList<Integer> rawContactIds = new ArrayList<>();
        try {
            if (rawCursor != null && rawCursor.moveToFirst()) {
                do {
                    rawContactIds.add(rawCursor.getInt(0));
                } while (rawCursor.moveToNext());
            }
        }finally {
            if (rawCursor != null) rawCursor.close();
        }
        if (rawContactIds.size() == 0) return new Integer[0];
        Log.d("getRawIds", "Raw ids " + rawContactIds.toString());

        return rawContactIds.toArray(new Integer[rawContactIds.size()]);
    }

    public static Integer[] getDataIds(Context context, Integer[] rawIds, String where) {
        if (rawIds == null || rawIds.length == 0) return new Integer[0];
        ContentResolver cr = context.getContentResolver();
        List<Integer> rawIdList = Arrays.asList(rawIds);

        // Lookup up the data table id with the above raw ids and the hangouts mime type
        Cursor dataCursor = cr.query(ContactsContract.Data.CONTENT_URI,
                new String[]{ContactsContract.Data._ID, ContactsContract.Data.RAW_CONTACT_ID},
                where,
                null, null);
        ArrayList<Integer> dataIds = new ArrayList<>();
        try {
            if (dataCursor != null && dataCursor.moveToFirst()) {
                do {
                    int rawId = dataCursor.getInt(1);
                    if (rawIdList.contains(rawId)) {
                        dataIds.add(dataCursor.getInt(0));
                    }
                } while (dataCursor.moveToNext());
            }
        }finally {
            if (dataCursor != null) dataCursor.close();
        }
        if (dataIds.size() == 0) return new Integer[0];
        Log.d("getDataIds", "Data ids " + dataIds.toString());

        return dataIds.toArray(new Integer[dataIds.size()]);
    }

	/**
	 * Sample Usage: String[] cData = ContactUtils.getContactData(new
	 * String("1234"), context, new
	 * String[]{ContactsContract.Contacts.LOOKUP_KEY,
	 * ContactsContract.Contacts.DISPLAY_NAME});
	 * 
	 * @param phoneNumber
	 * @param context
	 * @param columnNames
	 * @return
	 */
	public static String[] getContactDataByPhoneNumber(String phoneNumber, Context context, String[] columnNames) {
		Uri uri = Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber));
        if (uri == null) return new String[0];
        if (phoneNumber == null || phoneNumber.isEmpty()) return new String[0];
        Log.d("getContactDataByPhoneNumber", "Phone Number: " + phoneNumber);
        return getContactData(context, uri, columnNames);
	}

	public static Map<String,String> getContactPhoneNumbers(String lookupKey, Context context, boolean formatNumber) {
		Log.d("getContactPhoneNumbers", "Looking up key: " + lookupKey);
        Map<String,String> map = new HashMap<String, String>();
        if (lookupKey == null) return map;

		String[] columns = new String[] {
			ContactsContract.CommonDataKinds.Phone.NUMBER,
			ContactsContract.CommonDataKinds.Phone.TYPE,
			ContactsContract.CommonDataKinds.Phone.LABEL
		};
		String where =
			ContactsContract.Data.LOOKUP_KEY + "=?" + " AND " +
			ContactsContract.Data.MIMETYPE + "='" + ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE + "'";
		
		ContentResolver cr = context.getContentResolver();
		Cursor cursor = cr.query(ContactsContract.Data.CONTENT_URI, columns, where, new String[] {lookupKey}, null);		
		
		if (cursor == null) return map;
		if (!cursor.moveToFirst()) {
			Log.d("getContactPhoneNumbers", "None found");
			cursor.close();
			return map;
		}

        Map<String,Integer> typeCount = new HashMap<String, Integer>();
        do {
			String number = cursor.getString(0);
			String label = cursor.getString(2);
			int type = cursor.getInt(1);
			if (label == null || label.length() == 0){
				label = (String) ContactsContract.CommonDataKinds.Phone.getTypeLabel(context.getResources(), type, "");
			}
			if (formatNumber && number != null){
				number = PhoneNumberUtils.formatNumber(number);
			}
			Log.d("getContactPhoneNumbersByNumberType", "Found: " + number + " - " + label + " - " + type);
            if (map.containsKey(label)){
                // Append a number to the label if it's already in the map
                if (!typeCount.containsKey(label)){
                    typeCount.put(label, 1);
                }
                int ccc = typeCount.get(label);
                ccc++;
                typeCount.put(label, ccc);
                label += " (" + ccc + ")";
            }
			map.put(label, number);
		}while (cursor.moveToNext());

		cursor.close();
		return map;
	}

	public static Map<String,String> getContactEmails(String lookupKey, Context context, boolean removeDuplicateAddresses) {
		
		String[] columns = new String[] {
			ContactsContract.CommonDataKinds.Email.ADDRESS,
			ContactsContract.CommonDataKinds.Email.TYPE,
			ContactsContract.CommonDataKinds.Email.LABEL
		};
		String where =
			ContactsContract.Data.LOOKUP_KEY + "=?" + " AND " +
			ContactsContract.Data.MIMETYPE + "='" + ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE + "'";
		
		ContentResolver cr = context.getContentResolver();
		Cursor cursor = cr.query(ContactsContract.Data.CONTENT_URI, columns, where, new String[] {lookupKey}, null);		
		
		if (cursor == null) return null;
        if (!cursor.moveToFirst()) {
            cursor.close();
            return null;
        }

		Map<String,String> map = new HashMap<String, String>();		
		do {
			String address = cursor.getString(0);
			String label = cursor.getString(2);
			int type = cursor.getInt(1);
			if (label == null || label.length() == 0){
				label = (String) ContactsContract.CommonDataKinds.Email.getTypeLabel(context.getResources(), type, "");
			}
			if (!removeDuplicateAddresses || !map.containsValue(address)){
				map.put(label, address);
			}
		}while (cursor.moveToNext());

        cursor.close();
		return map;
	}
	
	public static String[] getContactDataByEmail(String email, Context context, String[] columnNames) {
		Uri uri = Uri.withAppendedPath(ContactsContract.CommonDataKinds.Email.CONTENT_LOOKUP_URI, Uri.encode(email));
        return getContactData(context, uri, columnNames);
	}

	public static String getContactName(String lookupKey, Context context){
		Uri uri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_LOOKUP_URI, lookupKey);
        if (uri == null) return null;

        return getContactName(context, uri);
	}

    public static String getContactName(Context context, Uri lookupUri) {
        String[] columns = new String[] {
            ContactsContract.Contacts.DISPLAY_NAME_PRIMARY
        };

        ContentResolver cr = context.getContentResolver();
        Cursor contact = cr.query(lookupUri, columns, null, null, null);
        if (contact == null) return null;
        if (!contact.moveToFirst()) {
            contact.close();
            return null;
        }
        String name = contact.getString(0);
        contact.close();
        return name;
    }

    public static String getContactLookupKey(String uriString, Context context){
        Uri uri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, uriString);

        String[] columns = new String[] {
                ContactsContract.Contacts.LOOKUP_KEY
        };

        ContentResolver cr = context.getContentResolver();
        Cursor contact = cr.query(uri, columns, null, null, null);
        if (contact == null) return null;
        if (!contact.moveToFirst()) {
            contact.close();
            return null;
        }
        String lookupKey = contact.getString(0);
        contact.close();
        return lookupKey;
    }

    public static String getContactByName(String name, Context context) {
		Uri uri = ContactsContract.Contacts.CONTENT_URI;
        if (uri == null) return null;
		String where = ContactsContract.Contacts.DISPLAY_NAME_PRIMARY + " LIKE \"" + name + "\"";
		String[] columns = new String[] { ContactsContract.Contacts.LOOKUP_KEY };

		//Log.d("getContactByName", "URI: " + uri + " / Where: " + where + " / Cols" + columns);
		Cursor contact = context.getContentResolver().query(uri, columns, where, null, null);
        if (contact == null) return null;
		if (!contact.moveToFirst()) {
			contact.close();
			return null;
		}
		String key = contact.getString(0);
		contact.close();
		return key;
	}
	
	/**
	 * Gets the bitmap associated with the contact defined by the given lookup
	 * key
	 * 
	 * @param lookupKey
	 * @param context
	 * @return
	 */
	@SuppressLint("NewApi")
	public static Bitmap getContactPhoto(String lookupKey, Context context) {
		ContentResolver cr = context.getContentResolver();
		Uri lookupUri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_LOOKUP_URI, lookupKey);
        if (lookupUri == null) return null;
		Uri contactUri = ContactsContract.Contacts.lookupContact(cr, lookupUri);
        if (contactUri == null) return null;
		InputStream input = ContactsContract.Contacts.openContactPhotoInputStream(cr, contactUri, true);
		if (input != null) {
			return BitmapFactory.decodeStream(input);
		}else{
			return null;//BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_contact_picture_holo_dark);
		}
	}
	public static Bitmap getContactPhotoThumbnail(String lookupKey, Context context) {
        Uri lookupUri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_LOOKUP_URI,
                lookupKey);
        return getContactPhotoThumbnail(context, lookupUri);
	}

    public static Bitmap getContactPhotoThumbnail(Context context, Uri lookupUri) {
        ContentResolver cr = context.getContentResolver();
        if (lookupUri == null) return null;
        Uri contactUri = ContactsContract.Contacts.lookupContact(cr, lookupUri);
        if (contactUri == null) return null;
        InputStream input = ContactsContract.Contacts.openContactPhotoInputStream(cr, contactUri);
        if (input != null) {
            return BitmapFactory.decodeStream(input);
        }else{
            return null;//BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_contact_picture_holo_dark);
        }
    }

    public static boolean contactHasPhoto(String lookupKey, Context context) {
		ContentResolver cr = context.getContentResolver();
		Uri lookupUri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_LOOKUP_URI, lookupKey);
        if (lookupUri == null) return false;
		Uri contactUri = ContactsContract.Contacts.lookupContact(cr, lookupUri);
        if (contactUri == null) return false;
		InputStream input = ContactsContract.Contacts.openContactPhotoInputStream(cr, contactUri);
		if (input == null){
			return false;
		}else{
			return true;
		}
	}
	
	public static Uri getContactPhotoUri(String lookupKey, Context context) {
		ContentResolver cr = context.getContentResolver();
		Uri lookupUri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_LOOKUP_URI, lookupKey);
		Uri contactUri = ContactsContract.Contacts.lookupContact(cr, lookupUri);
        if (contactUri == null) return null;

        Cursor cursor = cr.query(contactUri,
					new String[] { ContactsContract.Contacts.PHOTO_URI },
					null, null, null);
		if (cursor == null || !cursor.moveToFirst()) return null;
		String sUri = cursor.getString(0);
		cursor.close();
		if (sUri == null) {
			return null;
		}else{
			return Uri.parse(sUri);
		}
	}
	
	public static int strequentContactCount(Context context){
		Cursor cursor = null;
        try{
            cursor = context.getContentResolver().query(
                     ContactsContract.Contacts.CONTENT_STREQUENT_URI,
                    new String[] { ContactsContract.Contacts.LOOKUP_KEY },
                    null, null, null);
        }catch(Exception e){
            e.printStackTrace();
        }
        if (cursor == null) return 0;
        int count = cursor.getCount();
        cursor.close();
        return count;
	}
	
	public static String strequentContactLookupKey(Context context, int position){
		Cursor cursor = null;
        try{
            cursor = context.getContentResolver().query(
                     ContactsContract.Contacts.CONTENT_STREQUENT_URI,
                    new String[] { ContactsContract.Contacts.LOOKUP_KEY },
                    null, null, null);
        }catch(Exception e){
            e.printStackTrace();
            return null;
        }
        if (cursor == null){
			Log.d("FrequentContact", "Failed to get a cursor to CONTENT_STREQUENT_URI at position " + position);
			return null;
		}
        if (!cursor.moveToPosition(position)){
            cursor.close();
            Log.d("FrequentContact", "Failed to get a cursor to CONTENT_STREQUENT_URI at position " + position);
            return null;
        }

        String value = cursor.getString(0);
        cursor.close();
        return value;
	}
	
	public static String[] strequentContacts(Context context){
		Cursor cursor = null;
        try{
            cursor = context.getContentResolver().query(
                         ContactsContract.Contacts.CONTENT_STREQUENT_URI,
                        new String[] { ContactsContract.Contacts.LOOKUP_KEY },
                        null, null, null);
        }catch(Exception e){
            e.printStackTrace();
            return null;
        }
        if (cursor == null) return null;
		if (!cursor.moveToFirst()){
			cursor.close();
			Log.d("StrequentContacts", "Failed to get a cursor to CONTENT_STREQUENT_URI");
			return null;
		}
		
		ArrayList<String> list = new ArrayList<String>();
		do {
			String key = cursor.getString(0);
			if (key != null && key.length() > 0){
				list.add(key);
			}else{
				Log.d("StrequentContacts", "Invalid Key returned by cursor");
			}
		}while (cursor.moveToNext());

        cursor.close();
		String[] s = new String[list.size()];
		return list.toArray(s);
	}
	
	public static String[] starredContacts(Context context){
		Cursor cursor = null;
        try{
            cursor = context.getContentResolver().query(
                     ContactsContract.Contacts.CONTENT_URI,
                    new String[] { ContactsContract.Contacts.LOOKUP_KEY },
                    "starred=?", new String[] {"1"}, null);
        }catch(Exception e){
            e.printStackTrace();
            return null;
        }
        if (cursor == null) return null;
        if (!cursor.moveToFirst()){
			cursor.close();
			Log.d("StarredContacts", "Failed to get a cursor to starred contacts");
			return null;
		}
		
		ArrayList<String> list = new ArrayList<String>();
		do {
			list.add(cursor.getString(0));
		}while (cursor.moveToNext());

        cursor.close();
		String[] s = new String[list.size()];
		return list.toArray(s);
	}
	
    public static void drawBorder(Canvas canvas, Rect dst) {
        // Darken the border
        final Paint workPaint = new Paint();
        workPaint.setColor(Color.BLACK);
        workPaint.setStyle(Paint.Style.STROKE);
        // The stroke is drawn centered on the rect bounds, and since half will be drawn outside the
        // bounds, we need to double the width for it to appear as intended.
        workPaint.setStrokeWidth(2 * 2);
        canvas.drawRect(dst, workPaint);
    }
//
//    private static final List<String> countryCodes = Arrays.asList(
//            "AD", "AE", "AF", "AL", "AM", "AN", "AO", "AQ", "AR", "AT", "AU", "AW", "AZ", "BA",
//            "BD", "BE", "BF", "BG", "BH", "BI", "BJ", "BL", "BN", "BO", "BR", "BT", "BW", "BY",
//            "BZ", "CA", "CC", "CD", "CF", "CG", "CH", "CI", "CK", "CL", "CM", "CN", "CO", "CR",
//            "CU", "CV", "CX", "CY", "CZ", "DE", "DJ", "DK", "DZ", "EC", "EE", "EG", "ER", "ES",
//            "ET", "FI", "FJ", "FK", "FM", "FO", "FR", "GA", "GB", "GE", "GH", "GI", "GL", "GM",
//            "GN", "GQ", "GR", "GT", "GW", "GY", "HK", "HN", "HR", "HT", "HU", "ID", "IE", "IL",
//            "IM", "IN", "IQ", "IR", "IT", "JO", "JP", "KE", "KG", "KH", "KI", "KM", "KP", "KR",
//            "KW", "KZ", "LA", "LB", "LI", "LK", "LR", "LS", "LT", "LU", "LV", "LY", "MA", "MC",
//            "MD", "ME", "MG", "MH", "MK", "ML", "MM", "MN", "MO", "MR", "MT", "MU", "MV", "MW",
//            "MX", "MY", "MZ", "NA", "NC", "NE", "NG", "NI", "NL", "NO", "NP", "NR", "NU", "NZ",
//            "OM", "PA", "PE", "PF", "PG", "PH", "PK", "PL", "PM", "PN", "PR", "PT", "PW", "PY",
//            "QA", "RO", "RS", "RU", "RW", "SA", "SB", "SC", "SD", "SE", "SG", "SH", "SI", "SK",
//            "SL", "SM", "SN", "SO", "SR", "ST", "SV", "SY", "SZ", "TD", "TG", "TH", "TJ", "TK",
//            "TL", "TM", "TN", "TO", "TR", "TV", "TW", "TZ", "UA", "UG", "US", "UY", "UZ", "VA",
//            "VE", "VN", "VU", "WF", "WS", "YE", "YT", "ZA", "ZM", "ZW"
//    );
//    private static final List<String> countryCodesNumeric = Arrays.asList(
//            "376", "971", "93", "355", "374", "599", "244", "672", "54", "43", "61", "297",
//            "994", "387", "880", "32", "226", "359", "973", "257", "229", "590", "673", "591",
//            "55", "975", "267", "375", "501", "1", "61", "243", "236", "242", "41", "225", "682",
//            "56", "237", "86", "57", "506", "53", "238", "61", "357", "420", "49", "253", "45",
//            "213", "593", "372", "20", "291", "34", "251", "358", "679", "500", "691", "298",
//            "33", "241", "44", "995", "233", "350", "299", "220", "224", "240", "30", "502",
//            "245", "592", "852", "504", "385", "509", "36", "62", "353", "972", "44", "91",
//            "964", "98", "39", "962", "81", "254", "996", "855", "686", "269", "850", "82",
//            "965", "7", "856", "961", "423", "94", "231", "266", "370", "352", "371", "218",
//            "212", "377", "373", "382", "261", "692", "389", "223", "95", "976", "853", "222",
//            "356", "230", "960", "265", "52", "60", "258", "264", "687", "227", "234", "505",
//            "31", "47", "977", "674", "683", "64", "968", "507", "51", "689", "675", "63", "92",
//            "48", "508", "870", "1", "351", "680", "595", "974", "40", "381", "7", "250", "966",
//            "677", "248", "249", "46", "65", "290", "386", "421", "232", "378", "221", "252",
//            "597", "239", "503", "963", "268", "235", "228", "66", "992", "690", "670", "993",
//            "216", "676", "90", "688", "886", "255", "380", "256", "1", "598", "998", "39", "58",
//            "84", "678", "681", "685", "967", "262", "27", "260", "263"
//    );
//
//    static String countryCode=null;
//    public static String getCountryCode(Context context){
//        TelephonyManager manager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
//        if (countryCode == null) {
//            String countryId = getUserCountry(context);
//            int i = countryCodes.indexOf(countryId);
//            if (i == -1) countryCode = "1";
//            else countryCode = countryCodesNumeric.get(i);
//        }
//        return countryCode;
//    }
//
//    static String simCountry=null;
//    public static String getUserCountry(Context context) {
//        try {
//            final TelephonyManager tm = (TelephonyManager) context.getSystemService(Context
//                    .TELEPHONY_SERVICE);
//            try {
//                if (simCountry == null) simCountry = tm.getSimCountryIso();
//                if (simCountry != null && simCountry.length() == 2) { // SIM country code is available
//
//                    return simCountry.toUpperCase(Locale.US);
//                }
//            }catch (Exception e){}
//            try {
//                if (tm.getPhoneType() != TelephonyManager.PHONE_TYPE_CDMA) { // device is not 3G (would be unreliable)
//
//                    String networkCountry = tm.getNetworkCountryIso();
//                    if (networkCountry != null && networkCountry.length() == 2) { // network country code is available
//                        return networkCountry.toUpperCase(Locale.US);
//                    }
//                }
//            }catch (Exception e){}
//        } catch (Exception e) { }
//        return "US";
//    }

    public static String getAreaCode(Context context, String phoneNumber){
        PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
        String region = Locale.getDefault().getCountry();
        Phonenumber.PhoneNumber number;
        try {
            number = phoneUtil.parse(phoneNumber, region);
        } catch (NumberParseException e) {
            return "";
        }
        String nationalSignificantNumber = phoneUtil.getNationalSignificantNumber(number);
        int nationalDestinationCodeLength = phoneUtil.getLengthOfNationalDestinationCode(number);

        if (nationalDestinationCodeLength > 0) {
            return nationalSignificantNumber.substring(0, nationalDestinationCodeLength);
        }else{
            return "";
        }
    }

    public static String formatPhoneNumberForDisplay(String phoneNumber) {
        Log.d("formatPhoneNumberForDisplay", "Formatting " + phoneNumber);
        PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
        Phonenumber.PhoneNumber n = null;
        String region = Locale.getDefault().getCountry();
        try {
            n = phoneUtil.parse(phoneNumber, region);
        } catch (NumberParseException e) {
            return "";
        }
//        if (n.hasCountryCode()){
//            Log.d("formatPhoneNumberForDisplay", "  had country code");
//            Phonenumber.PhoneNumber n2;
//            n2.clearCountryCode();
//            if (phoneUtil.isValidNumberForRegion(n2, region)){
//                n = n2;
//                Log.d("formatPhoneNumberForDisplay", "  is valid for region " + region);
//            }
////            if (region.equalsIgnoreCase("US") && n.getCountryCode() == 1){
////                n.clearCountryCode();
////            }
//        }
        String r = phoneUtil.formatNumberForMobileDialing(n, region, true);
        Log.d("formatPhoneNumberForDisplay", "Result = " + r);
        return r;
    }

    public static String formatPhoneNumberForStorage(String phoneNumber){
        PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
        Phonenumber.PhoneNumber n = null;
        String region = Locale.getDefault().getCountry();
        try {
            n = phoneUtil.parse(phoneNumber, region);
        } catch (NumberParseException e) {
            return "";
        }
//        if (n.hasCountryCode()){
//            if (region.equalsIgnoreCase("US") && n.getCountryCode() == 1){
//                n.clearCountryCode();
//            }
//        }
        return phoneUtil.format(n, PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL);
    }

    public static boolean equalPhoneNumbers(String p1, String p2){
        PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
        String region = Locale.getDefault().getCountry();
        try {
            Phonenumber.PhoneNumber n1 = phoneUtil.parse(p1, region);
            Phonenumber.PhoneNumber n2 = phoneUtil.parse(p2, region);
            return n1.equals(n2);
        } catch (NumberParseException e) {
            return false;
        }

    }

    public static String removeNonNumericCharacters(String s){
        String number = "";
        if (s == null || s.isEmpty()){
            return number;
        }
        for (int i=0; i<s.length(); i++){
            Character character = s.charAt(i);
            if (Character.isDigit(character)) {
                number += character;
            }
        }
        return number;
    }

}
