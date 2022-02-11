package com.abstractwombat.loglibrary;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.ContactsContract;
import android.telephony.PhoneNumberUtils;
import android.util.Log;

import com.abstractwombat.contacts.ContactUtilities;
import com.abstractwombat.images.ImageUtilities;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

public class SMSLog {

	public static SMS[] getSMSLog(Context context, int max){
		// Build the SMS URI
		Uri SMS_CONTENT_URI = Uri.parse("content://sms");
		Uri SMS_INBOX_CONTENT_URI = Uri.withAppendedPath(SMS_CONTENT_URI, "inbox");
        if (SMS_INBOX_CONTENT_URI == null) return null;

		// Build the sorting and limiting
		String SORT_ORDER = "date DESC limit " + max;

		// Get a cursor into the SMS content provider
		Cursor cursor = context.getContentResolver().query(
				SMS_INBOX_CONTENT_URI,
				new String[] { "thread_id", "date", "address", "body" },
				null, null, SORT_ORDER);

        if (cursor == null){
            return null;
        }
		if (!cursor.moveToFirst()){
			cursor.close();
			return null;
		}

		SMS[] smsList = new SMS[max];
		int i=0;
		do {
			SMS s = new SMS();
			s.date = cursor.getLong(1);
			s.number = cursor.getString(2);
            s.number = ContactUtilities.formatPhoneNumberForStorage(s.number);
            s.message = cursor.getString(3);
			smsList[i++] = s;
		} while (i < max && cursor.moveToNext());

		cursor.close();
		return smsList;
	}

	public static SMSByContact[] getSMSByContact(Context context, boolean removeAllDuplicates, int max){
		Uri uri = Uri.parse("content://sms/inbox");
		return getSMSByContactFromURI(context, uri, true, removeAllDuplicates, max);
	}


	public static SMSByContact[] getSentSMSByContact(Context context, boolean removeAllDuplicates, int max){
		Uri uri = Uri.parse("content://sms/sent");
		return getSMSByContactFromURI(context, uri, false, removeAllDuplicates, max);
	}

	public static SMS[] getSMSFrom(Context context, String[] contactLookupKeys, int max){
		Uri uri = Uri.parse("content://sms/inbox");
		SMS[] ss = getSMS(context, uri, contactLookupKeys, max);
		for (SMS s : ss){
			s.incoming = true;
		}
		return ss;
	}
	public static SMS[] getSMSTo(Context context, String[] contactLookupKeys, int max){
		Uri uri = Uri.parse("content://sms/sent");
		SMS[] ss = getSMS(context, uri, contactLookupKeys, max);
		for (SMS s : ss){
			s.incoming = false;
		}
		return ss;
	}

    public static SMS[] collapseDuplicates(SMS[] s, int collapse) {
        if (s.length == 0) {
            return new SMS[0];
        }
        ArrayList<SMS> r = new ArrayList<>();
        r.add(s[0]);
        for (int i = 1; i < s.length; i++) {
            SMS sms = s[i];
            SMS last = s[i - 1];
            if (collapse == 1 || collapse == 3) {
                if (sms.contactLookupKey != null && last.contactLookupKey != null && sms
                        .contactLookupKey.length() > 0 && last.contactLookupKey.length() > 0 && sms
                        .contactLookupKey.equals(last.contactLookupKey)) {
                    Log.d("collapseDuplicates", "Collapsing: " + sms.contactLookupKey + " / " + last.contactLookupKey);
                    continue;
                }
            }
            if (collapse == 2 || collapse == 3) {
                if (sms.number != null && last.number != null && sms.number.length() > 0 && last
                        .number.length() > 0 && sms.number.equals(last.number)) {
                    Log.d("collapseDuplicates", "Collapsing: " + sms.number + " / " + last.number);
                    continue;
                }
            }
            r.add(sms);
        }
        SMS[] s2 = new SMS[r.size()];
        return r.toArray(s2);
    }

    public static SMS[] aggregateByDate(SMS[] s1, SMS[] s2, boolean descending, Integer maxCount) {
        ArrayList<SMS> a = new ArrayList<>();
        aggregateByDate(s1, s2, descending, a);

        int size = Math.min(maxCount, a.size());
        SMS[] entries = new SMS[size];
        entries = a.toArray(entries);
        return entries;
    }
    public static void aggregateByDate(SMS[] s1, SMS[] s2, boolean descending, ArrayList<SMS> a) {
        if (s1 == null && s2 == null) return;
        if (s1 == null) {
            a.addAll(Arrays.asList(s2));
        }else if (s2 == null){
            a.addAll(Arrays.asList(s1));
        }else{
            a.addAll(Arrays.asList(s1));
            a.addAll(Arrays.asList(s2));
        }

        if (descending){
            Collections.sort(a, Collections.reverseOrder());
        }else{
            Collections.sort(a);
        }
    }
    public static SMS[] aggregateByDate(SMS[] s1, SMS[] s2, boolean descending) {
        ArrayList<SMS> a = new ArrayList<>();
        aggregateByDate(s1, s2, descending, a);
		SMS[] entries = new SMS[a.size()];
		entries = a.toArray(entries);
        return entries;
    }

	private static SMS[] getSMS(Context context, Uri uri, String[] contactLookupKeys, int max){
		// Build the sorting and limiting
		String sort = "date desc";

		// Get a cursor into the SMS content provider
		Cursor cursor = context.getContentResolver().query(
				uri,
				new String[] { "date", "address", "body" },
				null, null, sort);

        if (cursor == null){
            return null;
        }
		if (!cursor.moveToFirst()){
			cursor.close();
			return null;
		}
		Vector<SMS> smsList = new Vector<SMS>();

        // Find all the phone numbers for the given contacts
        Set<String> numbers = null;
        if (contactLookupKeys != null && contactLookupKeys.length > 0){
            numbers = new HashSet<String>();
            for (String key : contactLookupKeys){
                Map<String,String> ns = ContactUtilities.getContactPhoneNumbers(key, context, false);
                if(ns != null){
                    for (String number : ns.values()) {
                        numbers.add(number);
                    }
                }
            }
        }

		do {
			SMS s = new SMS();
			s.date = cursor.getLong(0);
			s.number = cursor.getString(1);
			s.message = cursor.getString(2);

            // Strip off the country code if it's from the same country as the SIM card
            s.number = ContactUtilities.formatPhoneNumberForStorage(s.number);

			boolean good = false;
			if (numbers == null){
                good = true;
            }else{
                for (String number : numbers) {
                    if (ContactUtilities.equalPhoneNumbers(number, s.number)){
                        good = true;
                        break;
                    }
                }
            }

			if (good){
				smsList.add(s);
			}

		} while (smsList.size() < max && cursor.moveToNext());

		SMS[] entries = new SMS[smsList.size()];
		entries = smsList.toArray(entries);
		cursor.close();
		return entries;
	}


	private static SMSByContact[] getSMSByContactFromURI(Context context, Uri uri, boolean incoming, boolean removeAllDuplicates, int max){
		// Build the sorting and limiting
		String sort = "date desc";

		// Get a cursor into the SMS content provider
		Cursor cursor = context.getContentResolver().query(
				uri,
				new String[] { "date", "address", "body" },
				null, null, sort);

        if (cursor == null){
            return null;
        }
		if (!cursor.moveToFirst()){
			cursor.close();
			return null;
		}
		Vector<SMSByContact> smsList = new Vector<SMSByContact>();
		Map<String,String> keyMap = new HashMap<String, String>();

		do {
			SMS s = new SMS();
			s.date = cursor.getLong(0);
			s.number = cursor.getString(1);
			s.message = cursor.getString(2);
			s.incoming = incoming;

            // Strip off the country code if it's from the same country as the SIM card
            s.number = ContactUtilities.formatPhoneNumberForStorage(s.number);

			// Look up the contact's quick lookup key by number
			String key = "";
			boolean foundKey = false;
			if (keyMap.containsKey(s.number)){
				key = keyMap.get(s.number);
			}else{
				String[] contactData = ContactUtilities.getContactDataByPhoneNumber(s.number, context, new String[]{ContactsContract.Contacts.LOOKUP_KEY});
				if (contactData != null && contactData.length > 0){
					key = contactData[0];
					keyMap.put(s.number, key);
				}
			}
			if (key.equals("")){
				key = s.number;
			}else{
				foundKey = true;
			}

			SMSByContact smsByContact = null;
			if (smsList.size() > 0){
				SMSByContact tempSBC = new SMSByContact(key);
				if (removeAllDuplicates){
					// All duplicates
					int i = smsList.indexOf(tempSBC);
					if (i != -1){
						smsByContact = smsList.elementAt(i);
					}
				}else{
					// Just consecutive duplicates
					SMSByContact lastCBC = smsList.get(smsList.size()-1);
					if (tempSBC.equals(lastCBC)){
						smsByContact = lastCBC;
					}
				}
			}

			if (smsList.size() == 0 || smsByContact == null){
				smsByContact = new SMSByContact(key);
				smsByContact.smses.add(s);
				smsByContact.isContact = foundKey;
				smsList.add(smsByContact);
			}else{
				smsByContact.smses.add(s);
			}
		} while (smsList.size() < max && cursor.moveToNext());

		SMSByContact[] entries = new SMSByContact[smsList.size()];
		entries = smsList.toArray(entries);
		cursor.close();
		return entries;
	}

    public static SMS[] getSMSSent(Context context, int max, int collapse, String[] contactLookupKeys){
        Uri uri = Uri.parse("content://sms/sent");
        return getSMS(context, uri, max, collapse, false, contactLookupKeys);
    }

    public static SMS[] getSMSReceived(Context context, int max, int collapse, String[] contactLookupKeys){
        Uri uri = Uri.parse("content://sms/inbox");
        return getSMS(context, uri, max, collapse, true, contactLookupKeys);
    }

    /**
     *
     * @param context               Context for everything
     * @param uri                   The Uri to query (i.e. content://sms/inbox)
     * @param max                   Number of items to return
     * @param collapse              0=don't collapse,
     *                              1=collapse consecutive contacts,
     *                              2=collapse consecutive numbers,
     *                              3=collapse all (no duplicate contacts/numbers)
     * @param contactLookupKeys     Only show messaged from/to these people (can be null)
     * @return                      All the requested messages
     */
    private static SMS[] getSMS(Context context, Uri uri, int max, int collapse, boolean incoming, String[] contactLookupKeys){
        // Build the sorting and limiting
        String sort = "date desc";

        // Get a cursor into the SMS content provider
        Cursor cursor = context.getContentResolver().query(
                uri,
                new String[] { "date", "address", "body" },
                null, null, sort);

        if (cursor == null){
            return null;
        }
        if (!cursor.moveToFirst()){
            Log.d("getSMS", "Failed to move cursor to first");
            cursor.close();
            return null;
        }
        Vector<SMS> smsList = new Vector<SMS>();

        // Keep track of the looked up contacts
        class ContactInfo{
            ContactInfo(){
                name = "";
                key = "";
            }
            public String name;
            public String key;
        }

        // Find all the phone numbers for the given contacts and pre-populate the keyMap
        Map<String,ContactInfo> keyMap = new HashMap<String, ContactInfo>();
        Set<String> numbers = null;
        if (contactLookupKeys != null && contactLookupKeys.length > 0){
            numbers = new HashSet<String>();
            for (String key : contactLookupKeys){
                Map<String,String> ns = ContactUtilities.getContactPhoneNumbers(key, context, false);
                ContactInfo c = new ContactInfo();
                c.name = ContactUtilities.getContactName(key, context);
                c.key = key;
                Log.d("getSMS", "Including: " + c.name);
                if (ns != null){
                    for (String number : ns.values()) {
                        keyMap.put(number, c);
                        numbers.add(number);
                    }
                }
            }

            String s = "Filtering sms to only include the following numbers: ";
            for (String n : numbers) s += n + ",";
            Log.d("getSMS", s);
        }

        do {
            SMS s = new SMS();
            s.date = cursor.getLong(0);
            s.number = cursor.getString(1);
            s.message = cursor.getString(2);
            s.incoming = incoming;
            Log.d("getSMS", "Processing message dated: " + s.date);

            // Strip off the country code if it's from the same country as the SIM card
            s.number = ContactUtilities.formatPhoneNumberForStorage(s.number);

            // Filter by the given contact
            boolean passedFilter = false;
            if (numbers == null){
                Log.d("getSMS", "  No filter");
                passedFilter = true;
            }else{
                for (String number : numbers) {
                    if (ContactUtilities.equalPhoneNumbers(number, s.number)){
                        passedFilter = true;
                        Log.d("getSMS", "  Passed filter");
                        break;
                    }
                }
            }
            if (!passedFilter) continue;

            // Find contact information from this number
            ContactInfo contactInfo = new ContactInfo();
            if (keyMap.containsKey(s.number)){
                contactInfo = keyMap.get(s.number);
            }else{
                String[] contactData = ContactUtilities.getContactDataByPhoneNumber(s.number,
                        context, new String[]{ContactsContract.Contacts.LOOKUP_KEY, ContactsContract.Contacts.DISPLAY_NAME});
                if (contactData != null && contactData.length > 0){
                    contactInfo.key = contactData[0];
                    contactInfo.name = contactData[1];
                    keyMap.put(s.number, contactInfo);
                }
            }
            s.name = contactInfo.name;
            s.contactLookupKey = contactInfo.key;

            if (collapse == 0){
                Log.d("getSMS", "  No collapsing");
                smsList.add(s);
            }else if (collapse == 1){
                // Collapse on consecutive contact
                Log.d("getSMS", "  Collapse on consecutive contact");
                if (smsList.isEmpty()) smsList.add(new SMSCollapsed(s));
                else{
                    SMS last = smsList.get(smsList.size()-1);
                    if (last.contactLookupKey.equals(s.contactLookupKey)){
                        ((SMSCollapsed)last).push(s);
                    }else{
                        smsList.add(new SMSCollapsed(s));
                    }
                }
            }else if (collapse == 2){
                // Collapse on consecutive number
                Log.d("getSMS", "  Collapse on consecutive number");
                if (smsList.isEmpty()) smsList.add(new SMSCollapsed(s));
                else{
                    SMS last = smsList.get(smsList.size()-1);
                    if (ContactUtilities.equalPhoneNumbers(last.number, s.number)){
                        ((SMSCollapsed)last).push(s);
                    }else{
                        smsList.add(new SMSCollapsed(s));
                    }
                }
            }else if (collapse == 3){
                // Collapse all (no duplicate contacts/numbers)
                if (smsList.isEmpty()) smsList.add(new SMSCollapsed(s));
                else{
                    // Check if this contact is in the smsList
                    boolean found = false;
                    for (SMS cSMS : smsList){
                        if (s.contactLookupKey == null || s.contactLookupKey.equals("")){
                            if (ContactUtilities.equalPhoneNumbers(cSMS.number, s.number)){
                                found = true;
                            }
                        }else{
                            if (s.contactLookupKey.contentEquals(cSMS.contactLookupKey)){
                                found = true;
                            }
                        }
                        if (found){
                            ((SMSCollapsed)cSMS).push(s);
                            break;
                        }
                    }
                    if (!found){
                        smsList.add(new SMSCollapsed(s));
                    }
                }
            }

        } while (smsList.size() < max && cursor.moveToNext());

        SMS[] entries = new SMS[smsList.size()];
        entries = smsList.toArray(entries);
        cursor.close();
        return entries;
    }

    public interface MMSPartChecker{
        boolean shouldFetchImage(SMS sms);
    }

    public static SMS[] getMMSReceived(Context context, int max, int collapse, String[]
            contactLookupKeys, int imageMaxDimDP, MMSPartChecker partChecker) {
        return getMMS(context, 0, max, collapse, contactLookupKeys, imageMaxDimDP, partChecker);
    }

    public static SMS[] getMMSSent(Context context, int max, int collapse, String[]
            contactLookupKeys, int imageMaxDimDP, MMSPartChecker partChecker) {
        return getMMS(context, 1, max, collapse, contactLookupKeys, imageMaxDimDP, partChecker);
    }

    /**
     *
     * @param context               Context for everything
     * @param type                  0=incoming only, 1=outgoing only, 2=both
     * @param max                   Number of items to return
     * @param collapse              0=don't collapse,
     *                              1=collapse consecutive contacts,
     *                              2=collapse consecutive numbers,
     *                              3=collapse all (no duplicate contacts/numbers)
     * @param contactLookupKeys     Only show messaged from/to these people (can be null)
     * @return                      All the requested messages
     */
    public static SMS[] getMMS(Context context, int type, int max, int collapse, String[] contactLookupKeys, int imageMaxDimDP, MMSPartChecker partChecker) {
        int imageMaxDim = (int) ImageUtilities.convertDpToPixel(imageMaxDimDP);
        final String[] projection = new String[]{"_id", "date", "ct_t", "m_id", "msg_box"};
        String sortOrder = "date DESC";
        Uri uri = Uri.parse("content://mms-sms/conversations"); // android.provider.Telephony.MmsSms.CONTENT_CONVERSATIONS_URI
        Cursor query = context.getContentResolver().query(uri, projection, null, null, sortOrder);

        ArrayList<SMS> list = new ArrayList<>();
        if (query != null && query.moveToFirst()) {
            do {
                String string = query.getString(query.getColumnIndex("ct_t"));
                if (!"application/vnd.wap.multipart.related".equalsIgnoreCase(string) &&
                        !"application/vnd.wap.multipart.mixed".equalsIgnoreCase(string)) {
                    // It's not an MMS
                    continue;
                }

                // Get the MMS' id and date
                String id = query.getString(query.getColumnIndex("_id"));
                Long date = query.getLong(query.getColumnIndex("date")) * 1000;
                String m_id = query.getString(query.getColumnIndex("m_id"));
                int messageBox = query.getInt(query.getColumnIndex("msg_box"));
                Log.d("MMS", "getMMSData: " + m_id + " Box : " + messageBox + " ID: " + id + " Date: " + new

                        SimpleDateFormat("MM/dd/yyyy").format(new Date
                        ((Long) date)));

                boolean messageInInbox = messageBox == 1;

                // Create the SMS object
                SMS sms = new SMS();
                sms.date = date;
                sms.number = getMmsNumber(context, id, messageInInbox);

                sms.incoming = messageInInbox;
                String[] contactData = ContactUtilities.getContactDataByPhoneNumber(sms.number,
                        context, new String[]{ContactsContract.Contacts.LOOKUP_KEY, ContactsContract
                                .Contacts.DISPLAY_NAME});
                if (contactData != null && contactData.length == 2) {
                    sms.contactLookupKey = contactData[0];
                    sms.name = contactData[1];
                }

                // Check if this is duplicate that needs to be skipped
                if (!list.isEmpty() && collapse != 0){
                    SMS last = list.get(list.size()-1);
                    if (collapse == 1 || collapse == 3){
                        if (sms.contactLookupKey != null && last.contactLookupKey != null && sms.contactLookupKey.equals(last.contactLookupKey)){
                            continue;
                        }
                    }
                    if (collapse == 2 || collapse == 3){
                        if (sms.number != null && last.number != null && sms.number.equals(last.number)){
                            continue;
                        }
                    }
                }

                // Check if we should load the parts
                boolean getImage = true;
                if (partChecker != null){
                    getImage = partChecker.shouldFetchImage(sms);
                }

                int thisImageDim = imageMaxDim;
                if (!getImage) {
                    thisImageDim = 0;
                }
                Object[] mmsParts = getMmsParts(context, id, thisImageDim);
                sms.message = (String) mmsParts[0];
                Log.d("MMS", "Message: " + sms.message);
                if (getImage && mmsParts.length > 1) {
                    sms.bitmap = (Bitmap) mmsParts[1];
                    if (sms.bitmap != null) {
                        sms.bitmap = ImageUtilities.scaleBitmap(sms.bitmap, imageMaxDim);
                    }
                    Log.d("MMS", "Bitmap: " + (sms.bitmap == null ? "null" : "valid"));
                }
                list.add(sms);
            } while (list.size() < max && query.moveToNext());
        }
        if (query != null) {
            query.close();
        }
        SMS[] smses = new SMS[list.size()];
        return list.toArray(smses);
    }

    private static Object[] getMmsParts(Context context, String id, int imageMaxDimPixels) {
        String selectionPart = "mid=" + id;
        Uri uri = Uri.parse("content://mms/part");
        Cursor cursor = context.getContentResolver().query(uri, null,
                selectionPart, null, null);
        Object[] objects = new Object[2];
        if (cursor != null && cursor.moveToFirst()) {
            do {
                Log.d("getMmsParts", "content://mms/part/?mid=" + id);
                for (int i = 0; i < cursor.getColumnCount(); i++) {
                    Log.d("getMmsParts", "  " + cursor.getColumnName(i) + "=" + cursor.getString(i));
                }
                String partId = cursor.getString(cursor.getColumnIndex("_id"));
                String type = cursor.getString(cursor.getColumnIndex("ct"));
                if ("text/plain".equals(type)) {
                    String data = cursor.getString(cursor.getColumnIndex("_data"));
                    String body = null;
                    if (data != null) {
                        // implementation of this method below
                        body = getMmsText(context, partId);
                    } else {
                        body = cursor.getString(cursor.getColumnIndex("text"));
                    }
                    objects[0] = body;
                }else if ("image/jpeg".equals(type) || "image/bmp".equals(type) ||
                        "image/gif".equals(type) || "image/jpg".equals(type) ||
                        "image/png".equals(type)) {
                    if (imageMaxDimPixels > 0) {
                        Bitmap bitmap = getMmsImage(context, partId, imageMaxDimPixels);
                        objects[1] = bitmap;
                    }
                    break;
                }
            } while (cursor.moveToNext());
        }
        if (cursor != null) {
            cursor.close();
        }
        return objects;
    }

    private static String getMmsText(Context context, String id) {
        Uri partURI = Uri.parse("content://mms/part/" + id);
        InputStream is = null;
        StringBuilder sb = new StringBuilder();
        try {
            Cursor cursor = context.getContentResolver().query(partURI, null, null, null, null);
            if (context != null && cursor.moveToFirst()) {
                Log.d("getMmsText", "content://mms/part/");
                for (int i = 0; i < cursor.getColumnCount(); i++) {
                    Log.d("getMmsText", "  " + cursor.getColumnName(i) + "=" + cursor.getString(i));
                }
            }

            is = context.getContentResolver().openInputStream(partURI);
            if (is != null) {
                InputStreamReader isr = new InputStreamReader(is, "UTF-8");
                BufferedReader reader = new BufferedReader(isr);
                String temp = reader.readLine();
                while (temp != null) {
                    sb.append(temp);
                    temp = reader.readLine();
                }
            }
        } catch (IOException e) {
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {}
            }
        }
        return sb.toString();
    }

    private static Bitmap getMmsImage(Context context, String _id, int imageMaxDimPixels) {
        Uri partURI = Uri.parse("content://mms/part/" + _id);
        InputStream is = null;
        Bitmap bitmap = null;
        try {
//            Cursor cursor = context.getContentResolver().query(partURI, null, null, null, null);
//            if (context != null && cursor.moveToFirst()) {
//                Log.d("getMmsImage", "content://mms/part/");
//                for (int i = 0; i < cursor.getColumnCount(); i++) {
//                    Log.d("getMmsImage", "  " + cursor.getColumnName(i) + "=" + cursor.getString(i));
//                }
//            }

            is = context.getContentResolver().openInputStream(partURI);
            int sampleSize = calculateInSampleSize(is, imageMaxDimPixels, imageMaxDimPixels);
            is = context.getContentResolver().openInputStream(partURI);
            bitmap = decodeSampledBitmapStream(is, sampleSize);
        } catch (IOException e) {}
        finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {}
            }
        }
        return bitmap;
    }

    private static String getMmsNumber(Context context, String id, boolean incoming) {
        //String uriStr = MessageFormat.format("content://mms/{0}/addr", id);
        //Uri uriAddress = Uri.parse(uriStr);

        Uri.Builder builder = Uri.parse("content://mms").buildUpon();
        builder.appendPath(String.valueOf(id)).appendPath("addr");
        Uri uriAddress = builder.build();

        String add = "";
        final String[] projection = new String[] {"address","contact_id","charset"};
        String selection = "type=137 or type=151"; // PduHeaders
        if (incoming) {
            selection = "type=137";
        }else{
            selection = "type=151";
        }
        selection += " and msg_id=" + id;
        Cursor cursor = context.getContentResolver().query(uriAddress, projection,
                selection, null, null);

        if (cursor != null && cursor.moveToFirst()) {
            do {
                add = cursor.getString(cursor.getColumnIndex("address"));
                Log.d("MMS", "ID: " + id + " address: " + add);
            } while (cursor.moveToNext());
        }
        if (cursor != null){
            cursor.close();
        }
        return add;
    }

    public static Bitmap decodeSampledBitmapStream(InputStream is, int sampleSize) {
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = false;
        options.inSampleSize = sampleSize;
        return BitmapFactory.decodeStream(is, null, options);
    }
    public static int calculateInSampleSize(InputStream is, int reqWidth, int reqHeight) {
        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(is, null, options);

        // Calculate inSampleSize
        int sampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
        Log.d("decodeSampledBitmapStream", "Calculated sample size = " + sampleSize);
        return sampleSize;
    }
    public static int calculateInSampleSize(
            BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) > reqHeight
                    && (halfWidth / inSampleSize) > reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    public static SMS[] getGroupMessages(Context context, int max) {
        String sortOrder = "date DESC limit 30";
        ContentResolver contentResolver = context.getContentResolver();
        Uri mmsUri = Uri.parse("content://mms-sms/conversations");
        if (mmsUri == null){
            Log.d("getGroupMessages", "Uri(content://mms-sms/conversations) == null");
        }
        Cursor query = contentResolver.query(MessagingUtils.sAllThreadsUri, MessagingUtils.ALL_THREADS_PROJECTION,
                null, null, sortOrder);

        RecipientIdCache.init(context);
        Contact.init(context);

        ArrayList<SMS> list = new ArrayList<>();
        if (query != null && query.moveToFirst()) {
            do {
                SMS sms = MessagingUtils.fillFromCursor(context, query, false);
                if (sms.number == null || !sms.number.contains(MessagingUtils.NUMBER_DELIMITER)){
                    Log.d("getGroupMessages", "Not a group message");
                    continue;
                }
                if (sms.message != null && sms.message.startsWith("part/")){
                    sms.message = sms.message.replaceFirst("part\\/[^:]+:", "");
                }

//                // Output all the columns
//                Log.d("getGroupMessages", MessagingUtils.sAllThreadsUri.toString());
//                for (int i = 0; i < query.getColumnCount(); i++) {
//                    Log.d("getGroupMessages", "  " + query.getColumnName(i) + "=" + query.getString(i));
//                }
//
//                // Output all the mms-sms columns for this thread id
//                if (mmsUri != null) {
//                    Uri testUri = Uri.parse("content://mms-sms/");
//                    Cursor testQuery = null;
//                    try {
//                        testQuery = contentResolver.query(mmsUri, new String[]{"*"}, "thread_id=" + query.getString(MessagingUtils.ID), null, null);
//
//                        if (testQuery != null && testQuery.moveToFirst()) {
//                            Log.d("getGroupMessages", mmsUri.toString());
//                            for (int i = 0; i < testQuery.getColumnCount(); i++) {
//                                Log.d("getGroupMessages", "  " + testQuery.getColumnName(i) + "=" +
//                                        testQuery.getString(i));
//                            }
//                        }
//                    } catch (NullPointerException e) {
//                        Log.d("getGroupMessages", "Null pointer exception while querying content://mms-sms/conversations");
//                    } finally {
//                        if (testQuery != null) {
//                            testQuery.close();
//                        }
//                    }
//                }

                if (sms.message == null || sms.message.length() == 0 || sms.message
                        .compareToIgnoreCase("NoSubject") == 0) {
                    String threadId = query.getString(MessagingUtils.ID);
                    Log.d("getGroupMessages", "Group message is empty, trying to get it through " +
                            "the part (thread id = " + threadId);
                    if (mmsUri != null) {
                        Cursor idQuery = null;
                        try {
                            idQuery = contentResolver.query(mmsUri, new
                                    String[]{"_id", "thread_id"}, "thread_id=" + threadId, null,
                                    "date DESC");
                            if (idQuery != null && idQuery.moveToFirst()) {
                                String id = idQuery.getString(0);
                                Object[] parts = getMmsParts(context, id, 0);
                                if (parts.length > 0) {
                                    sms.message = (String) parts[0];
                                }
                            }
                        }catch(NullPointerException e){
                            Log.d("getGroupMessages", "Null pointer exception while querying content://mms-sms/conversations");
                        }finally {
                            if (idQuery != null) {
                                idQuery.close();
                            }
                        }
                    } else {
                        sms.message = "Failed to read message";
                    }
                }

                sms.contactLookupKey = "";
                sms.name = "";
                String[] numbers = sms.number.split(MessagingUtils.NUMBER_DELIMITER);
                for (String n : numbers){
                    String[] cData = ContactUtilities.getContactDataByPhoneNumber(n,
                            context, new String[]{ContactsContract.Contacts.LOOKUP_KEY, ContactsContract.Contacts.DISPLAY_NAME});
                    String name = "-";
                    String clk = "-";
                    if (cData != null && cData.length > 0){
                        clk = cData[0];
                    }
                    if (cData != null && cData.length > 1){
                        name = cData[1];
                    }
                    sms.contactLookupKey += clk + MessagingUtils.LOOKUP_KEY_DELIMITER;
                    sms.name += name + MessagingUtils.NAME_DELIMITER;
                }

                Log.d("getGroupMessages", "Message: " + sms.message);
                list.add(sms);
            } while (list.size() < max && query.moveToNext());
        }
        if (query != null) {
            query.close();
        }
        SMS[] smses = new SMS[list.size()];
        return list.toArray(smses);
    }

}
