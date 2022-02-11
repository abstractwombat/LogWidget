package com.abstractwombat.loglibrary;

import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.provider.ContactsContract;
import android.provider.ContactsContract.QuickContact;
import android.provider.Settings;
import android.service.notification.StatusBarNotification;
import android.telephony.PhoneNumberUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.RemoteViews;

import com.abstractwombat.contacts.ContactThumbnailsShared;
import com.abstractwombat.contacts.ContactUtilities;
import com.abstractwombat.images.ImageCache;
import com.abstractwombat.images.ImageUtilities;
import com.abstractwombat.library.MaterialColorGenerator;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

public class WeChatSource implements ALogSource, NotificationHistory.Specialization, NotificationSource {
	private static final String TAG = "WeChatSource";
    public static final String WECHAT_PACKAGE = "com.tencent.mm";
    private final Integer MAX_IMAGE_DIMENSION_DP = 128;

	/**
	 *	Intent Extra Data
	 */
    private final String INTENT_ACTION_ADDNOTIFICATION = "add_notification";
    private final String INTENT_ACTION_OPENWECHAT = "open_wechat";
	private final String INTENT_ACTION_QUICKCONTACT = "quick_contact";

	/**
	 *	Synchronization
	 */
	private static Map<String, ReentrantLock> locks = new HashMap<String, ReentrantLock>();

	/**
	 *	Date members
	 */
	private String tableName;
	private Context context;
	private NotificationHistory db;
    private ImageCache cache;
    WeChatSourceConfig config;

    public class WeChatData{
        long date;
        String key;
        String name;
        Bitmap image;
    }

	WeChatSource(){
	}

	@Override
	public void config(Context context, LogSourceConfig config){
		this.context = context;
		this.config = (WeChatSourceConfig)config;
		this.tableName = "[" + this.getID() + "]";
        this.db = new NotificationHistory(context, tableName, this.config().count);
        this.db.setSorting(NotificationHistory.COLUMN_WHEN, true);
        this.db.addPackageFilter(WECHAT_PACKAGE);
        this.db.setMaxImageDimension(MAX_IMAGE_DIMENSION_DP);
        this.db.addCollapsible(this);
		if (!locks.containsKey(this.config.sourceID)){
			locks.put(this.config.sourceID, new ReentrantLock());
		}
        // Initialize the ContactThumbnails
        ContactThumbnailsShared.initialize(R.drawable.ic_contact_picture_holo_dark);
        cache = new ImageCache("WeChatLogSource");
    }

	@Override
	public LogSourceConfig config(){
		return this.config;
	}

	private String getID(){
		return this.config.sourceID;
	}

	@Override
	public void update(){
        // Updates to the DB happen in the notification listener
		Log.d(TAG, "Updating Source ID: " + this.getID());
	}

    @Override
    public int size() {
        int s = (int)this.db.rowCount();
        Log.d(TAG, "size: " + s);
        return s;
    }

    @Override
	public long getDateAt(int position){
        long date = 0;
        NotificationHistory.NotificationData data = this.db.getAt(position);
        if (data == null){
            return date;
        }
        date = data.time;
		return date;
	}


    @Override
	public RemoteViews getViewAt(int position){
		Log.d(TAG, "Requested Position: " + position);

        // Fetch the appropriate row from the database
        NotificationHistory.NotificationData data = this.db.getAt(position);
		if (data == null){
			Log.d(TAG, "Got null row!");
			return new RemoteViews(this.context.getPackageName(), R.layout.empty_row);
		}

        // Get everything from the notification data
        WeChatNotificationInfo info = extractWeChatInfo(data);

        // Construct the views
        RemoteViews rv = null;
        if (data.extraPicture != null) {
            if (this.config.showImage) {
                rv = new RemoteViews(this.context.getPackageName(), R.layout.message_row_image);
            } else {
                rv = new RemoteViews(this.context.getPackageName(), R.layout
                        .message_row_image_nocontact);
            }
        } else {
            if (this.config.showImage) {
                rv = new RemoteViews(this.context.getPackageName(), R.layout.message_row);
            } else {
                rv = new RemoteViews(this.context.getPackageName(), R.layout.message_row_nocontact);
            }
        }

        // Set the padding
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            SharedPreferences settings = context.getSharedPreferences("State", Context
                    .MODE_MULTI_PROCESS);
            int spacing = settings.getInt(context.getPackageName() + "." + config.groupID + ".Spacing", -1);
            if (spacing == -1) LogSourceConfig.setDefaultSpacing(context, config.groupID);
            Log.d(TAG, "Setting widget " + config.groupID + " padding to " + spacing);
            rv.setViewPadding(R.id.row, 0, spacing, 0, spacing);
        }

        // Setup the row intent
        Intent rowIntent = new Intent();
        rowIntent.setAction(context.getPackageName()+"."+getID());
        rowIntent.putExtra(context.getPackageName() + ".name", info.name);
        rowIntent.putExtra(context.getPackageName() + ".number", info.phoneNumber);
        rowIntent.putExtra(context.getPackageName() + ".lookupUri", info.lookupUri.toString());
        rowIntent.putExtra(context.getPackageName() + ".action", INTENT_ACTION_OPENWECHAT);

        // Set the row click
        rv.setOnClickFillInIntent(R.id.highlight, rowIntent);
        rv.setInt(R.id.highlight, "setBackgroundResource", R.drawable.highlight_statelist_material);

        // Setup the contact image intent
		rv.setViewVisibility(R.id.contact_image_touch, View.GONE);
		if (this.config.showImage && info.lookupUri != null){
            Intent contactImageIntent = new Intent();
            contactImageIntent.setAction(context.getPackageName()+"."+getID());
            contactImageIntent.putExtra(context.getPackageName() + ".action", INTENT_ACTION_QUICKCONTACT);
            contactImageIntent.putExtra(context.getPackageName() + ".lookupUri", info.lookupUri.toString());
            rv.setOnClickFillInIntent(R.id.contact_image_touch, contactImageIntent);
            rv.setViewVisibility(R.id.contact_image_touch, View.VISIBLE);
        }

        // Set the image to the contact's image
        rv.setViewVisibility(R.id.contact_image, View.GONE);
        if (this.config.showImage){
            // Figure out the key to use in the cache
            String cacheKey = null;
            if (info.lookupUri != null){
                cacheKey = info.lookupUri.toString();
            }else if (info.phoneNumber != null){
                cacheKey = info.phoneNumber;
            }else if (info.name != null){
                cacheKey = info.name;
            }
            cacheKey += config.showEmblem;
            // Get the image
            Bitmap image = cache.read(this.context, cacheKey);
            if (image == null) {
                Bitmap cImage = null;
                if (info.lookupUri != null){
                    String lookupKey = info.lookupUri.toString().replace(ContactsContract.Contacts.CONTENT_LOOKUP_URI.toString(), "");
                    cImage = ContactThumbnailsShared.get(this.context, lookupKey, false);
                }
                if (cImage == null) {
                    // Figure out the object to hash to pick a circle color
                    String colorKey = null;
                    if (info.name != null && info.name.length() > 0){
                        colorKey = info.name;
                    }else if (info.phoneNumber != null){
                        colorKey = info.phoneNumber;
                    }else if (info.lookupUri != null){
                        colorKey = info.lookupUri.toString();
                    }
                    // Figure out the text to put on the circle
                    String textOnImage = "";
                    if (info.name != null && info.name.length() > 0) {
                        textOnImage = info.name.substring(0, 1);
                        textOnImage = textOnImage.toUpperCase();
                    } else if (info.message != null && info.message.length() > 0) {
                        textOnImage = info.message.substring(0, 1);
                        textOnImage = textOnImage.toUpperCase();
                    } else {
                        textOnImage = "*";
                    }
                    int color = MaterialColorGenerator.get(colorKey);
                    image = ImageUtilities.generateCircleBitmap(this.context, color, 40.f, textOnImage);
                } else {
                    // Make the contact image a circle
                    image = ImageUtilities.circleBitmap(context, cImage);
                    image = ImageUtilities.scaleBitmap(image, (int) ImageUtilities.convertDpToPixel(40.f));
                }
                if (config.showEmblem) {
                    Bitmap emblem = BitmapFactory.decodeResource(context.getResources(), R.drawable.wechat_emblem);
                    image = ImageUtilities.addCircleEmblem(context, image, emblem);
                }

                cache.write(this.context, cacheKey, image);
            }
            rv.setImageViewBitmap(R.id.contact_image, image);
            rv.setViewVisibility(R.id.contact_image, View.VISIBLE);
        }

        // Set the text
        rv.setTextViewText(R.id.message, info.message);

        // Set the maximum number of lines
        rv.setInt(R.id.message, "setMaxLines", config.maxLines);

        // Set the image
        if (data.extraPicture != null) {
            rv.setImageViewBitmap(R.id.image, data.extraPicture);
        }

        // Set the time
        String timeString;
        if (this.config.longDataFormat == true){
            timeString = dateToLongString(info.time);
        }else{
            timeString = dateToString(info.time);
        }

        if (this.config.showName){
            // Prepend the name
            if (info.name != null && info.name.length() > 0){
                timeString = info.name + " • " + timeString;
            }else if (info.phoneNumber != null){
                // If no name, prepend the number
                String number = ContactUtilities.formatPhoneNumberForDisplay(info.phoneNumber);
                if (number != null && number.length() > 0) {
                    timeString = number + " • " + timeString;
                }
            }
        }
        rv.setTextViewText(R.id.meta, timeString);

        // Set row background color
        rv.setInt(R.id.background, "setBackgroundColor", config.rowColor);

        // Set the custom colors
        int textR = Color.red(config.textColor);
        int textG = Color.green(config.textColor);
        int textB = Color.blue(config.textColor);
        rv.setTextColor(R.id.message, Color.argb(Math.round(.87f * 255.f), textR, textG, textB));
        rv.setTextColor(R.id.meta, Color.argb(Math.round(.54f * 255.f), textR, textG, textB));

        // Set the text size
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            rv.setTextViewTextSize(R.id.message, TypedValue.COMPLEX_UNIT_SP, config.textSize);
            rv.setTextViewTextSize(R.id.meta, TypedValue.COMPLEX_UNIT_SP, config.textSize * 0.7333333f);
        }

        // Set the bubble
        int bubbleResId = 0;
        if (config.bubbleResourceName.isEmpty()) {
            Log.d(TAG, "Resource name is empty");
            // Check if the bubbleResource id is valid (updates can invalidate resources)
            boolean foundBubbleResource = false;
            TypedArray drawableArray = context.getResources().obtainTypedArray(R.array.bubble_styles);
            for (int ta = 0; ta < drawableArray.length(); ta++) {
                int id = drawableArray.getResourceId(ta, -1);
                if (id == config.bubbleResource){
                    foundBubbleResource = true;
                    break;
                }
            }
            if (foundBubbleResource) {
                // It's good
                Log.d(TAG, "Resource id was still good");
                bubbleResId = config.bubbleResource;
            }else{
                Log.d(TAG, "Resource id was INVALID, setting to default");
                bubbleResId = WeChatSourceConfig.DEFAULT_BUBBLE_RESOURCE;
            }
            // Set the string so we don't have to do this again
            config.bubbleResource = bubbleResId;
            config.bubbleResourceName = context.getResources().getResourceEntryName(bubbleResId);
            LogSourceFactory.deleteSource(context, config.sourceID);
            LogSourceFactory.newSource(context, WeChatSource.class, config);
        }else{
            bubbleResId = context.getResources().getIdentifier("drawable/" + config.bubbleResourceName, null, context.getPackageName());
        }
        rv.setImageViewResource(R.id.bubble, bubbleResId);
        rv.setInt(R.id.bubble, "setImageAlpha", Color.alpha(config.bubbleColor));
        rv.setInt(R.id.bubble, "setColorFilter", config.bubbleColor);

        return rv;
	}

    private class WeChatNotificationInfo {
        long time;
        String name;
        String message;
        String phoneNumber;
        Uri lookupUri;
    }

    private WeChatNotificationInfo extractWeChatInfo(NotificationHistory.NotificationData data){
        WeChatNotificationInfo info = new WeChatNotificationInfo();
        info.time = data.time;

        // Extract what we can from the people data
        if (data.people != null && data.people.length > 0){
            for (String personData : data.people){
                if (personData.startsWith("content://")) {
                    // It's a URI, assume lookup
                    info.lookupUri = Uri.parse(personData);
                    Log.d(TAG, "Notification person uri: " + personData);
                    continue;
                }
                if(personData.startsWith("tel:")){
                    // It's a phone number
                    info.phoneNumber = personData.replace("tel:", "");
                    String[] keys = ContactUtilities.getContactDataByPhoneNumber(info.phoneNumber, context, new String[]{ContactsContract.Contacts.LOOKUP_KEY});
                    if (keys != null && keys.length > 0){
                        info.lookupUri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_LOOKUP_URI, keys[0]);
                    }
                    Log.d(TAG, "Notification SMS phone number: " + personData);
                    continue;
                }
            }

        }

        // Get the message
        info.message = data.extraText;
        if (data.textLines != null && data.textLines.length > 0){
            String tempMessage = data.textLines[data.textLines.length-1];
            if (tempMessage != null && !tempMessage.isEmpty()) {
                info.message = tempMessage;
            }
        }
        Log.d(TAG, "Notification message: " + info.message);

        // Get the name
        if (info.lookupUri != null) {
            // Use the Uri first
            info.name = ContactUtilities.getContactName(context, info.lookupUri);
        }
        if (info.name == null && info.phoneNumber != null) {
            // Use the phone number
            String[] keys = ContactUtilities.getContactDataByPhoneNumber(info.phoneNumber,
                    context, new String[]{ContactsContract.Contacts.DISPLAY_NAME});
            if (keys != null && keys.length > 0){
                info.name = keys[0];
            }
        }
        if (info.name == null) {
            // Title is the last resort
            info.name = data.title;
        }
        Log.d(TAG, "Notification name: " + info.name);

        // Use the name to get the lookup uri
        if (info.lookupUri == null){
            // This is a last resort as it will fail with duplicate names
            String lookup = ContactUtilities.getContactByName(info.name, context);
            info.lookupUri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_LOOKUP_URI, lookup);
            Log.d(TAG, "Notification lookup uri from name: " + lookup);
        }

        return info;
    }

    @Override
    public boolean addNotification(StatusBarNotification notification){
        Long time = notification.getNotification().when;
        if (time == 0) time = notification.getPostTime();
        if (this.db.contains(time)) {
            return false;
        }else{
            return this.db.addNotification(notification);
        }
    }

    @Override
    public boolean enabled(){
        ContentResolver cr = context.getContentResolver();
        String enabledListeners = Settings.Secure.getString(cr, "enabled_notification_listeners");
        if (enabledListeners == null){
            Log.d(TAG, "Failed to get enabled notification listeners");
            return false;
        }
        String[] pieces = enabledListeners.split("/|:");
        List<String> piecesArray = Arrays.asList(pieces);
        if (piecesArray.contains("com.abstractwombat.logwidget.NotificationListener") && piecesArray.contains(context.getPackageName())){
            Log.d(TAG, "Enabled notification listeners (" + true + "): " + enabledListeners);
            return true;
        }else{
            Log.d(TAG, "Enabled notification listeners (" + false + "): " + enabledListeners);
            return false;
        }
    }

    @Override
    public String getPackage() {
        return WECHAT_PACKAGE;
    }

    @Override
    public boolean confirmAdd(NotificationHistory.NotificationData toAdd) {
        // Make sure we are licensed
        if (!Licensing.sourcesLicensed(context)){
            Log.d(TAG, "Not Licensed");
            return false;
        }

        // Check if this matches the filter
        if (this.config.lookupKeyFilter != null && this.config.lookupKeyFilter.length > 0) {
            WeChatNotificationInfo info = extractWeChatInfo(toAdd);
            for (String lookupKey : this.config.lookupKeyFilter) {
                String name = ContactUtilities.getContactName(lookupKey, context);
                if (name.equalsIgnoreCase(info.name)){
                    return true;
                }
            }
            Log.d(TAG, "Excluded by filter");
            return false;
        }

        Log.d(TAG, "Confirming notification addition");
        return true;
    }

    @Override
    public boolean areCollapsible(NotificationHistory.NotificationData a, NotificationHistory
            .NotificationData b) {
        WeChatNotificationInfo aInfo = extractWeChatInfo(a);
        WeChatNotificationInfo bInfo = extractWeChatInfo(b);
        if (aInfo.name == null || bInfo.name == null) {
            return false;
        }else {
            return aInfo.name.equals(bInfo.name);
        }
    }

	@Override
	public void receiveIntent(Context context, Intent intent){
		String action = intent.getStringExtra(context.getPackageName()+".action");
		Log.d(TAG, "Received action: " + action);

        if (action.equals(INTENT_ACTION_ADDNOTIFICATION)){
            StatusBarNotification notification = intent.getParcelableExtra(context.getPackageName() +
                    ".notification");
            this.db.addNotification(notification);
        }
		if (action.equals(INTENT_ACTION_OPENWECHAT)){
            String name = intent.getStringExtra(context.getPackageName() + ".name");
            String lookupUri = intent.getStringExtra(context.getPackageName() + ".lookupUri");

            // Convert the uri to a lookup key
            String lookupKey = null;
            if (lookupUri != null && lookupUri.length() > 0) {
                Uri uri = Uri.parse(lookupUri);
                String[] tempArray = ContactUtilities.getContactData(context, uri,
                        new String[]{ContactsContract.Data.LOOKUP_KEY});
                if (tempArray != null && tempArray.length > 0) {
                    lookupKey = tempArray[0];
                }
            }

            if (lookupKey == null) {
                // Get the contact by name
                Log.d(TAG, "No look up key... try to get it from the name (" + name + ")");
                lookupKey = ContactUtilities.getContactByName(name, context);
            }

            Intent wechatIntent = null;
            if (lookupKey != null) {
                // Get the contact's phone numbers
                Map<String, String> phoneNumberMap = ContactUtilities.getContactPhoneNumbers
                        (lookupKey, context, false);
                // Strip all formatting from the numbers
                Set<String> phoneNumbers = new HashSet<>();
                for (Map.Entry<String, String> entry : phoneNumberMap.entrySet()) {
                    String number = ContactUtilities.formatPhoneNumberForStorage(entry.getValue());
                    phoneNumbers.add(number);
                }
                Log.d(TAG, "Found " + phoneNumberMap.size() + " phone numbers");

                // Inspect the Data table to find recent conversations
                Cursor c = context.getContentResolver().query(ContactsContract.Data.CONTENT_URI,
                        new String[]{ContactsContract.Contacts.Data._ID,
                                ContactsContract.Data.DATA1},
                        ContactsContract.Data.MIMETYPE + " = 'vnd.android.cursor.item/vnd.com.tencent.mm.chatting.profile'",
                        null, null);

                // Search the conversations for any of our contact's numbers
                String foundNumber = null;
                String contactId = null;
                if (c != null && c.moveToFirst()) {
                    do {
                        String wechatNumber = c.getString(1);
                        wechatNumber = ContactUtilities.formatPhoneNumberForStorage(wechatNumber);
                        if (phoneNumbers.contains(wechatNumber) /*&& mimeType.equals
                            (preferredMimeType)*/) {
                            contactId = c.getString(0);
                            Log.d(TAG, "Found the contact's WeChat number (" + wechatNumber + ") " +
                                    "and " +
                                    "contact id (" + contactId + ")");
                            foundNumber = wechatNumber;
                            break;
                        }
                    } while (c.moveToNext());
                }
                if (c != null) c.close();

                if (contactId != null) {
                    // Show this contact in WeChat
                    wechatIntent = new Intent(Intent.ACTION_VIEW,
                            Uri.parse("content://com.android.contacts/data/" + contactId));
                    wechatIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    wechatIntent.setPackage(WECHAT_PACKAGE);
                }
            }

            if (wechatIntent == null) {
                // Can't find the contact, just launch WeChat app
                PackageManager manager = context.getPackageManager();
                wechatIntent = manager.getLaunchIntentForPackage(WECHAT_PACKAGE);
                if (wechatIntent != null) {
                    wechatIntent.addCategory(Intent.CATEGORY_LAUNCHER);
                    wechatIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                }
            }
            if (wechatIntent != null){
                context.startActivity(wechatIntent);
            }

            return;
        }
		if (action.equals(INTENT_ACTION_QUICKCONTACT)){
	    	String lookupUri = intent.getStringExtra(context.getPackageName() + ".lookupUri");
	    	int[] location = null;
	    	location = intent.getIntArrayExtra(context.getPackageName()+".location");
	    	Rect r = intent.getSourceBounds();
	    	if (location != null){
	    		r = new Rect(location[0]-1, location[1]-1, location[0]+1, location[1]+1);
	    	}
	    	if (lookupUri == null)return;
			QuickContact.showQuickContact(context, r, Uri.parse(lookupUri), QuickContact.MODE_SMALL, null);
    		return;
		}
	}

    private void sendIt(Context context, String action, Uri uri){
        Intent callIntent = new Intent(action);
        callIntent.setData(uri);
        PendingIntent pendingIntent;
        pendingIntent =  PendingIntent.getActivity(context, 0, callIntent, 0);
        try {
            pendingIntent.send(context, 0, null);
        } catch (PendingIntent.CanceledException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    private void sendIt(Context context, String action, String uri){
        sendIt(context, action, Uri.parse(uri));
    }

    private String dateToString(long timeInMilliSeconds) {
        Date date = new Date(timeInMilliSeconds);

        Calendar now = Calendar.getInstance();
        long msDiff = now.getTimeInMillis() - timeInMilliSeconds;
        long daysAgo = TimeUnit.DAYS.convert(msDiff, TimeUnit.MILLISECONDS);

        String formatString;
        if (daysAgo == 0){
            // Today, so just the time
            formatString = "h:mm a";
        }else if (daysAgo < 7){
            // This week, so use the day of week and time
            formatString = "EEE, h:mm a";
        }else{
            // Week ago, short date and time
            formatString = "MMM d, h:mm a";
        }
        SimpleDateFormat outFormat = new SimpleDateFormat(formatString);
        return outFormat.format(date);
    }

    private String dateToLongString(long timeInMilliSeconds) {
        Date date = new Date(timeInMilliSeconds);
        DateFormat dateForm = DateFormat.getDateInstance(DateFormat.LONG);
        DateFormat timeForm = DateFormat.getTimeInstance(DateFormat.SHORT);
        SimpleDateFormat dayFormat = new SimpleDateFormat("EE");

        String timeS = timeForm.format(date);
        String dayWS = dayFormat.format(date);
        String dateS = dateForm.format(date);
        return timeS + ", " + dayWS + ", " + dateS;
    }

    private String dateToAgoString(long timeInMilliSeconds) {
        Calendar now = Calendar.getInstance();
        long msDiff = now.getTimeInMillis() - timeInMilliSeconds;
        long daysAgo = TimeUnit.DAYS.convert(msDiff, TimeUnit.MILLISECONDS);

        // Check if this is today
        if (daysAgo < 1) {
            long hoursAgo = TimeUnit.HOURS.convert(msDiff, TimeUnit.MILLISECONDS);
            if (hoursAgo == 0) {
                long minutesAgo = TimeUnit.MINUTES.convert(msDiff, TimeUnit.MILLISECONDS);
                if (minutesAgo == 0) {
                    return "Just now";
                } else {
                    return "" + minutesAgo + " minutes ago";
                }
            } else {
                return "" + hoursAgo + " hours ago";
            }
        }else if (daysAgo == 1){
            return "Yesterday";
        }else if (daysAgo < 7){
            return "" + daysAgo + " days ago";
        }else {
            Date date = new Date(timeInMilliSeconds);
            SimpleDateFormat outFormat = new SimpleDateFormat("MMMM d");
            return outFormat.format(date);
        }
    }

	private String durationToString(long durationInSeconds) {
		if (durationInSeconds == 0) {
			return "0 seconds";
		}
		long ONE_SECOND = 1000;
		long SECONDS = 60;
		// long ONE_MINUTE = ONE_SECOND * 60;
		long MINUTES = 60;
		long HOURS = 24;
		String res = "";
		int seconds = 0;
		int minutes = 0;
		int hours = 0;
		// int days = 0;

		if (durationInSeconds > 0) {
			seconds = (int) (durationInSeconds % SECONDS);
			durationInSeconds /= SECONDS;
		}
		if (durationInSeconds > 0) {
			minutes = (int) (durationInSeconds % MINUTES);
			durationInSeconds /= MINUTES;
		}
		if (durationInSeconds > 0) {
			hours = (int) (durationInSeconds % HOURS);
			// days = (int) (durationInSeconds / HOURS);
		}
		if (minutes == 0) {
			res = Long.toString(seconds) + " seconds";
		} else if (hours == 0) {
			String minString = "mins";
			if (minutes == 1) minString = "min";
			String secString = "secs";
			if (seconds == 1) secString = "sec";
			res = Long.toString(minutes) + " " + minString + " and " + Long.toString(seconds) + " " + secString;
		}
		return res;
	}


}
