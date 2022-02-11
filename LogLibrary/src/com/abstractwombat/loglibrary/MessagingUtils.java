package com.abstractwombat.loglibrary;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.Telephony;
import android.text.TextUtils;
import android.util.Log;

import java.io.UnsupportedEncodingException;

/**
 * Created by Mike on 12/4/2015.
 */
public class MessagingUtils {

    //    public static final Uri sAllThreadsUri =
//            Telephony.Threads.CONTENT_URI.buildUpon().appendQueryParameter("simple", "true")
//                    .build();
    public static final Uri sAllThreadsUri = Uri.withAppendedPath(
            Uri.parse("content://mms-sms/"), "conversations").buildUpon().appendQueryParameter("simple", "true")
            .build();

//    public static final String[] ALL_THREADS_PROJECTION = {
//            Telephony.Threads._ID, Telephony.Threads.DATE, Telephony.Threads.MESSAGE_COUNT,
//            Telephony.Threads.RECIPIENT_IDS,
//            Telephony.Threads.SNIPPET, Telephony.Threads.SNIPPET_CHARSET, Telephony.Threads.READ,
//            Telephony.Threads.ERROR,
//            Telephony.Threads.HAS_ATTACHMENT
//    };
    public static final String[] ALL_THREADS_PROJECTION = {
            Telephony.Threads._ID, "date", "message_count", "recipient_ids", "snippet",
            "snippet_cs", "read", "error", "has_attachment"
    };

    public static final int ID = 0;
    private static final int DATE = 1;
    private static final int MESSAGE_COUNT = 2;
    private static final int RECIPIENT_IDS = 3;
    private static final int SNIPPET = 4;
    private static final int SNIPPET_CS = 5;
    private static final int READ = 6;
    private static final int ERROR = 7;
    private static final int HAS_ATTACHMENT = 8;

    public static String NUMBER_DELIMITER = ";";
    public static String NAME_DELIMITER = ", ";
    public static String LOOKUP_KEY_DELIMITER = "%delimiter%";

    public static ContactList getByIds(String spaceSepIds, boolean canBlock) {
        ContactList list = new ContactList();
        for (RecipientIdCache.Entry entry : RecipientIdCache.getAddresses(spaceSepIds)) {
            if (entry != null && !TextUtils.isEmpty(entry.number)) {
                Contact contact = Contact.get(entry.number, canBlock);
                contact.setRecipientId(entry.id);
                list.add(contact);
            }
        }
        return list;
    }

    public static SMS fillFromCursor(Context context, Cursor c, boolean allowQuery) {
        SMS sms = new SMS();

        Long threadId = c.getLong(ID);
        sms.date = c.getLong(DATE);
        int messageCount = c.getInt(MESSAGE_COUNT);

        // Replace the snippet with a default value if it's empty.
        String snippet = extractEncStrFromCursor(c, SNIPPET, SNIPPET_CS);
        if (TextUtils.isEmpty(snippet)) {
            snippet = "";
        }
        sms.message = snippet;

        boolean hasUnreadMessages = (c.getInt(READ) == 0);
        boolean hasError = (c.getInt(ERROR) != 0);
        boolean hasAttachment = (c.getInt(HAS_ATTACHMENT) != 0);

        // Fill in as much of the conversation as we can before doing the slow stuff of looking
        // up the contacts associated with this conversation.
        String recipientIds = c.getString(RECIPIENT_IDS);
        ContactList recipients = ContactList.getByIds(recipientIds, allowQuery);
        if (recipients.size() > 0){
            sms.number = recipients.get(0).getNumber();
            sms.name = recipients.get(0).getName();
        }

        if (recipients.size() == 1){
            String[] numbers = recipients.getNumbers();
            sms.number = numbers[0];
        }else if (recipients.size() > 1) {
            String delimitedNumbers = "";
            String delimitedNames = "";
            for (int i = 0; i < recipients.size(); i++) {
                delimitedNumbers += recipients.get(i).getNumber() + NUMBER_DELIMITER;
                String name = recipients.get(i).getName();
                int endPos = name.indexOf(" ");
                if (endPos > 0){
                    name = name.substring(0, endPos);
                }
                delimitedNames += name + NAME_DELIMITER;
            }
            Log.d("fillFromCursor", "recipient names=" + delimitedNames);
            sms.number = delimitedNumbers.substring(0, delimitedNumbers.length()-NUMBER_DELIMITER.length());;
            sms.name = delimitedNames.substring(0, delimitedNames.length()-NAME_DELIMITER.length());;
        }

        return sms;
    }

    private static boolean loadFromThreadId(Context context, long threadId, boolean allowQuery) {
        Cursor c = context.getContentResolver().query(sAllThreadsUri, ALL_THREADS_PROJECTION,
                "_id=" + Long.toString(threadId), null, null);
        try {
            if (c.moveToFirst()) {
                fillFromCursor(context, c, allowQuery);
            } else {
                Log.d("loadFromThreadId", "Can't find thread ID " + threadId);
                return false;
            }
        } finally {
            c.close();
        }
        return true;
    }

    public static boolean isEmailAddress(String address) {
            /*
             * The '@' char isn't a valid char in phone numbers. However, in SMS
             * messages sent by carrier, the originating-address can contain
             * non-dialable alphanumeric chars. For the purpose of thread id
             * grouping, we don't care about those. We only care about the
             * legitmate/dialable phone numbers (which we use the special phone
             * number comparison) and email addresses (which we do straight up
             * string comparison).
             */
        return (address != null) && (address.indexOf('@') != -1);
    }

    public static String extractEncStrFromCursor(Cursor cursor,
                                                 int columnRawBytes, int columnCharset) {
        String rawBytes = cursor.getString(columnRawBytes);
        int charset = cursor.getInt(columnCharset);

        if (TextUtils.isEmpty(rawBytes)) {
            return "";
        } else if (charset == CharacterSets.ANY_CHARSET) {
            return rawBytes;
        } else {
            byte[] bytes = null;
            try {
                bytes = rawBytes.getBytes(CharacterSets.MIMENAME_ISO_8859_1);
            } catch (UnsupportedEncodingException e) {
                // Impossible to reach here!
                Log.d("extractEncStrFromCursor", "ISO_8859_1 must be supported!", e);
                return null;
            }
            return new EncodedStringValue(charset, bytes).getString();
        }
    }
}
