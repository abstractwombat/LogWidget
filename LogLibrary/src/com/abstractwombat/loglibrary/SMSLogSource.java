package com.abstractwombat.loglibrary;

import android.Manifest;
import android.app.PendingIntent;
import android.app.PendingIntent.CanceledException;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.provider.ContactsContract;
import android.provider.ContactsContract.QuickContact;
import android.telephony.PhoneNumberUtils;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.RemoteViews;

import com.abstractwombat.contacts.ContactThumbnailsShared;
import com.abstractwombat.contacts.ContactUtilities;
import com.abstractwombat.images.ImageCache;
import com.abstractwombat.images.ImageUtilities;
import com.abstractwombat.library.MaterialColorGenerator;
import com.abstractwombat.library.RuntimePermissions;
import com.abstractwombat.library.SQLDatabase;
import com.abstractwombat.library.SQLTableColumn;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

public class SMSLogSource implements ALogSource {
	private static final String TAG = "SMSLogSource";
	
	/**
	 *	Database Table Columns
	 */
	private final String COLUMN_DATE = "date";
	private final String COLUMN_KEY = "contactkey";
	private final String COLUMN_NAME = "contactname";
	private final String COLUMN_NUMBER = "phonenumber";
	private final String COLUMN_INCOMING = "incoming";
    private final String COLUMN_MSG = "message";
    private final String COLUMN_IMAGE = "image";

	/**
	 *	Intent Extra Data
	 */
	private final String INTENT_ACTION_SENDSMS = "send_sms";
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
	private SQLDatabase db;
	private SMSLogSourceConfig config;
    private ImageCache cache;
    private ImageCache mmsCache;
    final String STATE_FILE = "State";

	SMSLogSource(){
	}
	

	@Override
	public void config(Context context, LogSourceConfig config) {
		this.context = context;
		this.config = (SMSLogSourceConfig)config;
		this.tableName = "[" + this.getID() + "]";
		this.db = new SQLDatabase(this.context);
		if (!locks.containsKey(this.config.sourceID)){
			locks.put(this.config.sourceID, new ReentrantLock());
		}
        if (this.config.showMMS){
            mmsCache = new ImageCache("MMSParts");
        }
        // Initialize the ContactThumbnails
        ContactThumbnailsShared.initialize(R.drawable.ic_contact_picture_holo_dark);
        cache = new ImageCache("SMSLogSource");
	}
	@Override
	public LogSourceConfig config() {
		return this.config;
	}

	private String getID(){
		return this.config.sourceID;
	}
	
	@Override
	public void update(){
		locks.get(this.config.sourceID).lock();
		try{
			// Recreate the table
			this.db.deleteTable(this.tableName);
			this.db.createTable(this.tableName, getColumns());

            SMS[] incomingS = null;
            SMS[] outgoingS = null;

            // Support a count of zero
            if (this.config.count <= 0){
                return;
            }

            // Figure out the count
            int countPerType = this.config.count;
            if (this.config.showIncoming && this.config.showOutgoing) {
                countPerType = countPerType/2;
            }

            // Figure out the collapse mode
            int collapse = 0; // Don't collapse
            if (this.config.lookupKeyFilter != null && this.config.lookupKeyFilter.length > 0) {
                if (this.config.showIncoming && !this.config.showOutgoing) {
                    collapse = 1; // Collapse consecutive contacts if only showing incoming
                }
                if (this.config.lookupKeyFilter.length == 1) {
                    collapse = 0;   // If there's only one person, don't collapse
                }
            }  else{
                if (this.config.showIncoming && !this.config.showOutgoing) {
                    collapse = 3; // Collapse based on contact if we are only showing incoming
                }
            }

            // Check for SMS permission
            boolean smsPermission = RuntimePermissions.hasPermission(context, Manifest.permission
                    .READ_SMS);
            boolean contactPermission = RuntimePermissions.hasPermission(context, Manifest
                    .permission.READ_CONTACTS);

            // Get the SMS'
            if (smsPermission && contactPermission) {
                if (this.config.lookupKeyFilter != null && this.config.lookupKeyFilter.length > 0) {
                    if (this.config.showIncoming) {
                        incomingS = SMSLog.getSMSReceived(context, countPerType, collapse, this.config.lookupKeyFilter);

                    }
                    if (this.config.showOutgoing) {
                        outgoingS = SMSLog.getSMSSent(context, countPerType, collapse, this.config.lookupKeyFilter);
                    }
                } else {
                    if (this.config.showIncoming) {
                        incomingS = SMSLog.getSMSReceived(context, countPerType, collapse, null);
                    }
                    if (this.config.showOutgoing) {
                        outgoingS = SMSLog.getSMSSent(context, countPerType, collapse, null);
                    }
                }
            }
            SMS[] sms = SMSLog.aggregateByDate(incomingS, outgoingS, true, this.config.count);

            // Set the last SMS time
            if (sms.length > 0) {
                SharedPreferences settings = context.getSharedPreferences(STATE_FILE, Context
                        .MODE_MULTI_PROCESS);
                SharedPreferences.Editor editor = settings.edit();
                editor.putLong(context.getPackageName() + ".LastSMSTime", sms[0].date);
                editor.apply();
                Log.d(TAG, "Found " + sms.length + " SMS");
            }

            // Get the MMS'
            SMS[] mms = null;
            if (smsPermission && contactPermission && this.config.showMMS) {
                mms = SMSLog.getGroupMessages(context, this.config.count);
                // Set the last MMS time
                if (mms.length > 0) {
                    mms = SMSLog.collapseDuplicates(mms, collapse);
                    final String STATE_FILE = "State";
                    SharedPreferences settings = context.getSharedPreferences(STATE_FILE, Context
                            .MODE_MULTI_PROCESS);
                    SharedPreferences.Editor editor = settings.edit();
                    editor.putLong(context.getPackageName() + ".LastMMSTime", mms[0].date);
                    editor.apply();
                    Log.d(TAG, "Found " + mms.length + " MMS");
                }
            }

            // Combine sms and mms and remove duplicates
            SMS[] combined = SMSLog.aggregateByDate(sms, mms, true, this.config.count);
            combined = SMSLog.collapseDuplicates(combined, collapse);
            Log.d(TAG, "Found " + combined.length + " combined");

            // Insert all messages into the database
            int i=0;
            for (SMS s : combined){
                Log.d(TAG, "Inserting: " + s.name + "(" + s.message + ")");
                if (i < this.config.count) {
                    insertSMS(s);
                }else{
                    break;
                }
                i++;
            }

		}finally{
			locks.get(this.config.sourceID).unlock();
		}
	}

    @Override
    public int size() {
        int s = (int)this.db.rowCount(this.tableName);
        Log.d(TAG, "size: " + s);
        return s;
    }

    @Override
    public long getDateAt(int position){
        Log.d(TAG, "Getting date at Position: " + position);

        locks.get(this.config.sourceID).lock();
        ContentValues cv;
        long date = 0;
        try{
            cv = this.db.getAt(this.tableName, position, COLUMN_DATE, true);
            if (cv != null) {
                date = cv.getAsLong(COLUMN_DATE);
            }
        }finally{
            locks.get(this.config.sourceID).unlock();
        }
        return date;
    }

    private ContentValues getDataAt(int position){
		Log.d(TAG, "Getting data at Position: " + position);
		
		locks.get(this.config.sourceID).lock();
		ContentValues cv = null;
		try{
			cv = this.db.getAt(this.tableName, position, COLUMN_DATE, true);
		}finally{
			locks.get(this.config.sourceID).unlock();
		}
		return cv;
	}
	
	@Override
	public RemoteViews getViewAt(int position){
		// Fetch the appropriate row from the database
		ContentValues c = getDataAt(position);
		if (c == null){
			Log.d(TAG, "Got null row!");
			return new RemoteViews(this.context.getPackageName(), R.layout.empty_row);
		}
		
		boolean incoming = false;
		int tempIncoming = c.getAsInteger(COLUMN_INCOMING);
		if (tempIncoming == 1) incoming = true;

        String imageKey = c.getAsString(COLUMN_IMAGE);
        Bitmap bitmap = null;
        if (imageKey != null && !imageKey.isEmpty() && mmsCache != null){
            bitmap = mmsCache.read(context, imageKey);
        }

        // Get everything out of the ContentValues
        String number = c.getAsString(COLUMN_NUMBER);
        String name = c.getAsString(COLUMN_NAME);
        String message = c.getAsString(COLUMN_MSG);
        long time = c.getAsLong(COLUMN_DATE);
        String contactKey = c.getAsString(COLUMN_KEY);

        // Setup the row intent
        Intent rowIntent = new Intent();
        rowIntent.setAction(context.getPackageName() + "." + getID());
        rowIntent.putExtra(context.getPackageName() + ".action", INTENT_ACTION_SENDSMS);
        rowIntent.putExtra(context.getPackageName() + ".number", number);

        // Construct the views
        RemoteViews rv;
        if (bitmap != null) {
            if (this.config.showImage) {
                rv = new RemoteViews(this.context.getPackageName(), R.layout.message_row_image);
            } else {
                rv = new RemoteViews(this.context.getPackageName(), R.layout
                        .message_row_image_nocontact);
            }
        } else if (incoming) {
            if (this.config.showImage) {
                rv = new RemoteViews(this.context.getPackageName(), R.layout.message_row);
            } else {
                rv = new RemoteViews(this.context.getPackageName(), R.layout.message_row_nocontact);
            }
        } else {
            if (this.config.showImage) {
                rv = new RemoteViews(this.context.getPackageName(), R.layout.message_row_outgoing);
            } else {
                rv = new RemoteViews(this.context.getPackageName(), R.layout
                        .message_row_outgoing_nocontact);
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

        // Set the row click
        rv.setOnClickFillInIntent(R.id.highlight, rowIntent);
        rv.setInt(R.id.highlight, "setBackgroundResource", R.drawable.highlight_statelist_material);

        // Show / Hide the contact image
        if (this.config.showImage) {
            rv.setViewVisibility(R.id.contact_image, View.VISIBLE);
            rv.setViewVisibility(R.id.contact_image_touch, View.VISIBLE);
        } else {
            rv.setViewVisibility(R.id.contact_image, View.GONE);
            rv.setViewVisibility(R.id.contact_image_touch, View.GONE);
        }

        // Set the text to the SMS message
        if (message != null) message = message.trim();
        if (message != null && !message.isEmpty()) {
            rv.setTextViewText(R.id.message, message);
            rv.setViewVisibility(R.id.message, View.VISIBLE);
        } else {
            rv.setViewVisibility(R.id.message, View.GONE);
        }

        // Set the maximum number of lines
        rv.setInt(R.id.message, "setMaxLines", config.maxLines);

        // Set the image
        if (bitmap != null) {
            rv.setImageViewBitmap(R.id.image, bitmap);
        }

        // Set the time
        String timeString;
        if (this.config.longDataFormat) {
            timeString = dateToLongString(time);
        } else {
            timeString = dateToString(time);
        }

        if (this.config.showName) {
            // Prepend the name
            if (name != null && name.length() > 0) {
                String nameString = "";
                if (name.contains(MessagingUtils.NAME_DELIMITER)) {
                    String[] names = name.split(MessagingUtils.NAME_DELIMITER);
                    String[] numbers = number.split(MessagingUtils.NUMBER_DELIMITER);
                    for (int i = 0; i < numbers.length; i++) {
                        if (!names[i].isEmpty() && !names[i].equals("-")) {
                            nameString += names[i] + MessagingUtils.NAME_DELIMITER;
                        } else if (!numbers[i].isEmpty()) {
                            String tempNum = ContactUtilities.formatPhoneNumberForDisplay(numbers[i]);
                            nameString += tempNum + MessagingUtils.NAME_DELIMITER;
                        }
                    }
                    if (nameString.endsWith(MessagingUtils.NAME_DELIMITER)) {
                        nameString = nameString.substring(0, nameString.length() - MessagingUtils
                                .NAME_DELIMITER.length());
                    }
                } else {
                    nameString = name;
                }
                timeString = nameString + " • " + timeString;
                Log.d(TAG, "Time string including name -" + name);
            } else {
                // If no name, prepend the number
                String numberFormatted = ContactUtilities.formatPhoneNumberForDisplay(number);
                if (numberFormatted != null && numberFormatted.length() > 0) {
                    timeString = numberFormatted + " • " + timeString;
                    Log.d(TAG, "Time string including number -" + numberFormatted);
                }
            }
        }
        rv.setTextViewText(R.id.meta, timeString);
        rv.setViewVisibility(R.id.date_padding_left, View.GONE);
        rv.setViewVisibility(R.id.date_padding_right, View.GONE);
        if (!this.config.showImage) {
            if (incoming) {
                rv.setViewVisibility(R.id.date_padding_left, View.VISIBLE);
            } else {
                rv.setViewVisibility(R.id.date_padding_right, View.VISIBLE);
            }
        }

        if (this.config.showImage) {
            // Setup the contact image intent
            if (!incoming) {
                contactKey = null;
                TelephonyManager tMgr = (TelephonyManager) context.getSystemService(Context
                        .TELEPHONY_SERVICE);
                if (tMgr != null) {
                    String phoneNumber = tMgr.getLine1Number();
                    if (phoneNumber != null) {
                        String[] keys = ContactUtilities.getContactDataByPhoneNumber(phoneNumber,
                                context, new String[]{ContactsContract.Contacts.DISPLAY_NAME,
                                        ContactsContract.Contacts.LOOKUP_KEY});
                        if (keys != null && keys.length > 1) contactKey = keys[1];
                        if (keys != null && keys.length > 1) name = keys[0];
                    }
                }
            }

            // Set the image to the contact's image
            String cacheKey = contactKey + incoming;
            if (contactKey == null || contactKey.isEmpty() || contactKey.contains(MessagingUtils
                    .LOOKUP_KEY_DELIMITER)) {
                cacheKey = number + incoming;
            }
            cacheKey += config.showEmblem;

            Log.d(TAG, "Using cache key " + cacheKey);
            Bitmap image = cache.read(this.context, contactKey + incoming);
            if (image == null) {
                if (contactKey != null && contactKey.contains(MessagingUtils
                        .LOOKUP_KEY_DELIMITER)) {
                    String[] contactKeys = contactKey.split(MessagingUtils.LOOKUP_KEY_DELIMITER);
                    String[] names = name.split(MessagingUtils.NAME_DELIMITER);
                    String[] numbers = number.split(MessagingUtils.NUMBER_DELIMITER);
                    Bitmap[] images = new Bitmap[contactKeys.length];
                    for (int i = 0; i < numbers.length; i++) {
                        String tempKey = contactKeys[i];
                        if (tempKey.isEmpty() || tempKey.equals("-")) {
                            tempKey = null;
                        }
                        String textOnImage = null;
                        if (names[i] == null || names[i].equals("-")) {
                            textOnImage = ContactUtilities.getAreaCode(context, numbers[i]);
                            if (textOnImage.length() >= 3) {
                                textOnImage = textOnImage.substring(0, 3);
                            }else if (textOnImage.length() >= 1) {
                                textOnImage = textOnImage.substring(0, 1);
                            }else textOnImage = "#";
                        } else {
                            textOnImage = names[i];
                            if (textOnImage.length() >= 1) {
                                textOnImage = textOnImage.substring(0, 1);
                            }else textOnImage = "#";
                        }
                        images[i] = getAvatar(context, tempKey, textOnImage);
                    }
                    image = ImageUtilities.layoutCircleImages(images);
                    image = ImageUtilities.scaleBitmap(image, (int) ImageUtilities
                            .convertDpToPixel(40.f));
                } else {
                    String textOnImage = "";
                    if (name != null && name.length() > 0) {
                        textOnImage = name;
                        if (textOnImage.length() >= 1) {
                            textOnImage = textOnImage.substring(0, 1);
                        }else textOnImage = "#";
                        Log.d(TAG, "Contact character sourced from name -" + name);
                    }
                    if (textOnImage.isEmpty() && number != null && !number.isEmpty()) {
                        textOnImage = ContactUtilities.getAreaCode(context, number);
                        Log.d(TAG, "Contact character sourced from number -" + number);
                        if (textOnImage.length() >= 3) {
                            textOnImage = textOnImage.substring(0, 3);
                        }
                    }
                    if (textOnImage.isEmpty() && message != null && message.length() > 0) {
                        textOnImage = message;
                        Log.d(TAG, "Contact character sourced from message -" + message);
                        textOnImage = textOnImage.substring(0, 1);
                    }
                    image = getAvatar(context, contactKey, textOnImage);
                }
                Log.d(TAG, "Image width: " + image.getWidth() + " image height: " + image.getHeight());
                if (config.showEmblem) {
                    Bitmap emblem = BitmapFactory.decodeResource(context.getResources(), R.drawable.smslog_emblem);
                    image = ImageUtilities.addCircleEmblem(context, image, emblem);
                }

                cache.write(this.context, cacheKey, image);
            }
            rv.setImageViewBitmap(R.id.contact_image, image);

            rv.setViewVisibility(R.id.contact_image_touch, View.GONE);
            if (contactKey != null && contactKey.length() > 0) {
                // Setup the contact image intent
                Intent contactImageIntent = new Intent();
                contactImageIntent.setAction(context.getPackageName() + "." + getID());
                contactImageIntent.putExtra(context.getPackageName() + ".action",
                        INTENT_ACTION_QUICKCONTACT);
                contactImageIntent.putExtra(context.getPackageName() + ".lookupKey", contactKey);
                rv.setOnClickFillInIntent(R.id.contact_image_touch, contactImageIntent);
                rv.setViewVisibility(R.id.contact_image_touch, View.VISIBLE);
            }
        }

        // Set row background color
        rv.setInt(R.id.background, "setBackgroundColor", config.rowColor);

        // Set the text colors
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
                bubbleResId = SMSLogSourceConfig.DEFAULT_BUBBLE_RESOURCE;
            }
            // Set the string so we don't have to do this again
            config.bubbleResource = bubbleResId;
            config.bubbleResourceName = context.getResources().getResourceEntryName(bubbleResId);
            LogSourceFactory.deleteSource(context, config.sourceID);
            LogSourceFactory.newSource(context, SMSLogSource.class, config);
        }else{
            bubbleResId = context.getResources().getIdentifier("drawable/" + config.bubbleResourceName, null, context.getPackageName());
        }
        rv.setImageViewResource(R.id.bubble, bubbleResId);
        rv.setInt(R.id.bubble, "setImageAlpha", Color.alpha(config.bubbleColor));
        rv.setInt(R.id.bubble, "setColorFilter", config.bubbleColor);
        return rv;
    }

    private static Bitmap getAvatar(Context context, String contactKey, String fallbackText) {
        Log.d(TAG, "getAvatar from key(" + contactKey + ") and fallbackText(" + fallbackText + ")");
        Bitmap cImage = null;
        if (contactKey != null && !contactKey.isEmpty()){
            cImage = ContactThumbnailsShared.get(context, contactKey, false);
        }
        if (cImage == null) {
            int color = MaterialColorGenerator.get(contactKey==null?fallbackText:contactKey);
            String imageChar = fallbackText.toUpperCase();
            cImage = ImageUtilities.generateCircleBitmap(context, color, 40.f, imageChar);
        } else {
            cImage = ImageUtilities.circleBitmap(context, cImage);
            cImage = ImageUtilities.scaleBitmap(cImage, (int) ImageUtilities.convertDpToPixel(40.f));
        }
        return cImage;
    }

	public void receiveIntent(Context context, Intent intent){
		String action = intent.getStringExtra(context.getPackageName()+".action");
		Log.d(TAG, "Received action: " + action);
		
		if (action.equals(INTENT_ACTION_SENDSMS)){
        	String number = intent.getStringExtra(context.getPackageName()+".number");
        	if (number == null)return;
            String intentUri = "smsto:";
            if (number.contains(MessagingUtils.NUMBER_DELIMITER)){
                String[] numbers = number.split(MessagingUtils.NUMBER_DELIMITER);
                String numberString = "";
                for (String n : numbers){
                    String tempNum = ContactUtilities.formatPhoneNumberForDisplay(n);
                    numberString += tempNum + MessagingUtils.NAME_DELIMITER;
                }
                intentUri += numberString;
            }else{
                intentUri += ContactUtilities.formatPhoneNumberForDisplay(number);
            }

            Log.d(TAG, "    " + intentUri);
            sendIt(context, Intent.ACTION_SENDTO, intentUri);
			return;
		}
		
		if (action.equals(INTENT_ACTION_QUICKCONTACT)){
	    	String lookupKey = intent.getStringExtra(context.getPackageName() + ".lookupKey");
	    	int[] location = null;
	    	location = intent.getIntArrayExtra(context.getPackageName()+".location");
	    	Rect r = intent.getSourceBounds();
	    	if (location != null){
	    		r = new Rect(location[0]-1, location[1]-1, location[0]+1, location[1]+1);
	    	}
	    	if (lookupKey == null)return;
			Uri uri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_LOOKUP_URI, lookupKey);
			QuickContact.showQuickContact(context, r, uri, ContactsContract.QuickContact.MODE_SMALL, null);
    		return;
		}		
	}

	/**
	 *	Insert a text into the database
	 */
	public void insertSMS(SMS s){
		ContentValues c = new ContentValues();
        c.put(COLUMN_DATE, s.date);
		c.put(COLUMN_MSG, s.message);
		c.put(COLUMN_NUMBER, s.number);
		c.put(COLUMN_INCOMING, s.incoming);
        c.put(COLUMN_KEY, s.contactLookupKey);
        c.put(COLUMN_NAME, s.name);
        String cacheKey = mmsPartCacheKey(s);
        if (s.bitmap != null && mmsCache != null){
            Log.d(TAG, "Writing mms image to cache (" + cacheKey + ")");
            mmsCache.write(context, cacheKey, s.bitmap);
            c.put(COLUMN_IMAGE, cacheKey);
        }else{
            c.put(COLUMN_IMAGE, "");
        }

		// Insert into the database
		this.db.insert(this.tableName, c);
	}
	
	private SQLTableColumn[] getColumns(){
		ArrayList<SQLTableColumn> cols = new ArrayList<SQLTableColumn>();
        cols.add(new SQLTableColumn(COLUMN_DATE, "integer"));
        cols.add(new SQLTableColumn(COLUMN_KEY, "text"));
        cols.add(new SQLTableColumn(COLUMN_NAME, "text"));
        cols.add(new SQLTableColumn(COLUMN_NUMBER, "text"));
        cols.add(new SQLTableColumn(COLUMN_INCOMING, "integer"));
        cols.add(new SQLTableColumn(COLUMN_MSG, "text"));
        cols.add(new SQLTableColumn(COLUMN_IMAGE, "text"));
		SQLTableColumn[] colArray = new SQLTableColumn[cols.size()];
		return cols.toArray(colArray);
	}
	
    private void sendIt(Context context, String action, Uri uri){
		Intent callIntent = new Intent(action);
		callIntent.setData(uri);
		PendingIntent pendingIntent;
		pendingIntent =  PendingIntent.getActivity(context, 0, callIntent, 0);  
		try {
			pendingIntent.send(context, 0, null);
		} catch (CanceledException e) {
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

    private String mmsPartCacheKey(SMS sms){
        return sms.contactLookupKey + Long.toString(sms.date);
    }

}
