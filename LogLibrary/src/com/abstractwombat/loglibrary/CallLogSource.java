package com.abstractwombat.loglibrary;

import android.Manifest;
import android.app.PendingIntent;
import android.app.PendingIntent.CanceledException;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.provider.CallLog;
import android.provider.ContactsContract;
import android.provider.ContactsContract.QuickContact;
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
import com.abstractwombat.library.RuntimePermissions;
import com.abstractwombat.library.SQLDatabase;
import com.abstractwombat.library.SQLTableColumn;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

public class CallLogSource implements ALogSource {
	private static final String TAG = "CallLogSource";

	/**
	 *	Database Table Columns
	 */
	private final String COLUMN_ID = "id";
	private final String COLUMN_DATE = "date";
	private final String COLUMN_KEY = "contactkey";
	private final String COLUMN_NAME = "contactname";
	private final String COLUMN_NUMBER = "phonenumber";
	private final String COLUMN_NUMBER_LABEL = "numberlabel";
	private final String COLUMN_LOCATION = "location";
	private final String COLUMN_DURATION = "duration";
	private final String COLUMN_TYPE_IMAGE = "typeimage";
	private final String COLUMN_COUNT = "count";

	/**
	 *	Intent Extra Data
	 */
	private final String INTENT_ACTION_OPENCALLLOG = "open_calllog";
	private final String INTENT_ACTION_MAKECALL = "make_call";
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
	private ImageCache cache;
	private SQLDatabase db;
	CallLogSourceConfig config;
	
	CallLogSource(){
	}
	
	@Override
	public void config(Context context, LogSourceConfig config){
		this.context = context;
		this.db = new SQLDatabase(this.context);
		this.config = (CallLogSourceConfig)config;
		this.tableName = "[" + this.getID() + "]";
		if (!locks.containsKey(this.config.sourceID)){
			locks.put(this.config.sourceID, new ReentrantLock());
		}
        // Initialize the ContactThumbnails
        ContactThumbnailsShared.initialize(R.drawable.ic_contact_picture_holo_dark);
		cache = new ImageCache("CallLogSource");
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
		Log.d(TAG, "Updating Source ID: " + this.getID());
		Log.d(TAG, "	config: showImage=" + config.showImage + ",showName=" + config.showName + ",showCallButton=" + config.showCallButton + ",longDataFormat=" + config.longDataFormat);
	
		locks.get(this.config.sourceID).lock();
		try{
			// Recreate the table
			this.db.deleteTable(this.tableName);
			this.db.createTable(this.tableName, getColumns());

            // Support a count of zero
            if (this.config.count <= 0){
                return;
            }

			// Check for call log permission
			boolean callPermission = RuntimePermissions.hasPermission(context, Manifest.permission
					.READ_CALL_LOG);
			boolean contactPermission = RuntimePermissions.hasPermission(context, Manifest
					.permission.READ_CONTACTS);

			if (callPermission && contactPermission) {
				// Make an array of call types
				ArrayList<Integer> callTypeList = new ArrayList<Integer>();
				if (this.config.showIncoming)callTypeList.add(CallLog.Calls.INCOMING_TYPE);
				if (this.config.showOutgoing)callTypeList.add(CallLog.Calls.OUTGOING_TYPE);
				if (this.config.showMissed)callTypeList.add(CallLog.Calls.MISSED_TYPE);
				int[] callTypes = new int[callTypeList.size()];
				int i=0;
				for (Integer t : callTypeList) { callTypes[i++] = t; }

				// Get the calls
				CallsByNumber[] calls = com.abstractwombat.loglibrary.CallLog.getCallsByNumber(this
								.context, false, this.config.lookupKeyFilter, callTypes, 0,
						this.config.count);

				// Insert all calls into the database
				for (CallsByNumber c : calls) {
					String label = "";
					String[] keyA = null;
					if (c.number == null || c.number.length() == 0) {
						label = "Unknown";
					} else {
						keyA = ContactUtilities.getContactDataByPhoneNumber(c.number, this.context, new String[]{ContactsContract.Contacts.LOOKUP_KEY});

					}
					if (keyA != null && keyA.length > 0) {
						Map<String, String> numbers = ContactUtilities.getContactPhoneNumbers(keyA[0], this.context, false);
						for (Map.Entry<String, String> entry : numbers.entrySet()) {
							if (PhoneNumberUtils.compare(entry.getValue(), c.number)) {
								label = entry.getKey();
								break;
							}
						}
					}
					insertCall(c, label);
				}
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
		Log.d(TAG, "Requested Position: " + position);
		// Construct the views
		RemoteViews rv;
		if (this.config.showImage) {
			rv = new RemoteViews(this.context.getPackageName(), R.layout.call_row_material);
		}else{
			rv = new RemoteViews(this.context.getPackageName(), R.layout.call_row_no_image_material);
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

		// Fetch the appropriate row from the database
		ContentValues c = getDataAt(position);
		if (c == null){
			Log.d(TAG, "Got null row!");
			return new RemoteViews(this.context.getPackageName(), R.layout.empty_row);
		}

		// Setup the contact image intent
		String contactKey = c.getAsString(COLUMN_KEY);
		rv.setViewVisibility(R.id.contact_image_touch, View.GONE);
		if (this.config.showImage && contactKey != null && contactKey.length() > 0){
			Intent contactImageIntent = new Intent();
			contactImageIntent.setAction(context.getPackageName()+"."+getID());
			contactImageIntent.putExtra(context.getPackageName() + ".action", INTENT_ACTION_QUICKCONTACT);
			contactImageIntent.putExtra(context.getPackageName() + ".lookupKey", contactKey);
			rv.setOnClickFillInIntent(R.id.contact_image_touch, contactImageIntent);
			rv.setViewVisibility(R.id.contact_image_touch, View.VISIBLE);
        }

		// Set the call type image
		int typeCount = c.getAsInteger(COLUMN_COUNT);
		rv.setViewVisibility(R.id.call_type_icon, View.GONE);
		if (typeCount > 0){
			byte[] typeimagebytes = c.getAsByteArray(COLUMN_TYPE_IMAGE);
			if (typeimagebytes != null){
				ByteArrayInputStream image = new ByteArrayInputStream(typeimagebytes);
				rv.setViewVisibility(R.id.call_type_icon, View.VISIBLE);
				rv.setImageViewBitmap(R.id.call_type_icon, BitmapFactory.decodeStream(image));
			}
		}

		// Format the phone number
		String number = c.getAsString(COLUMN_NUMBER);
		String formattedNumber = number == null ? "" : PhoneNumberUtils.formatNumber(number);
		Log.d(TAG, "Creating row for number: " + formattedNumber);
		if (formattedNumber.length() == 0) formattedNumber = "Unknown";

		// Remove any parenthesis from the formatted phone number
		formattedNumber = formattedNumber.replace("(", "");
		formattedNumber = formattedNumber.replace(")", "");

		// Get the number label
		String numberLabel = c.getAsString(COLUMN_NUMBER_LABEL);

        // Set the time
        long time = c.getAsLong(COLUMN_DATE);
        String timeString;
        if (this.config.longDataFormat == true){
            timeString = dateToLongString(time);
        }else{
            timeString = dateToString(time);
        }

		// Set the text rows
		if (this.config.showName){
			if (c.getAsString(COLUMN_NAME).length() > 0){
				rv.setTextViewText(R.id.name, c.getAsString(COLUMN_NAME));
				rv.setTextViewText(R.id.number, numberLabel);
			}else{
				rv.setTextViewText(R.id.name, formattedNumber);
				rv.setTextViewText(R.id.number, c.getAsString(COLUMN_LOCATION));
			}
			if (typeCount > 3){
				timeString = "(" + typeCount + ") " + timeString;
			}
			rv.setTextViewText(R.id.call_date, timeString);
		}else{
			String numberString = formattedNumber;
			if (numberLabel != null && numberLabel.length() > 0){
				numberString = numberLabel;
			}
			rv.setTextViewText(R.id.name, numberString);
			rv.setTextViewText(R.id.number, timeString);
			String durationString = durationToString(c.getAsLong(COLUMN_DURATION));
			rv.setTextViewText(R.id.call_date, durationString);
		}

		// Set the image to the contact's image
		if (this.config.showImage){
			rv.setViewVisibility(R.id.contact_image, View.VISIBLE);
			String cacheKey = contactKey + config.showEmblem;
			if (contactKey == null || contactKey.isEmpty()){
				cacheKey = formattedNumber + config.showEmblem;
			}
			Log.d(TAG, "Contact Image - cacheKey=" + cacheKey);
			Bitmap image = cache.read(this.context, cacheKey);
			if (image == null) {
				Log.d(TAG, "Contact Image - Not in cache");
				image = ContactThumbnailsShared.get(this.context, contactKey, false);
				if (image == null) {
					String name = c.getAsString(COLUMN_NAME);
					String colorKey = name;
					String textOnImage = "";
					if (name != null && name.length() > 0) {
						textOnImage = name.substring(0, 1);
						textOnImage = textOnImage.toUpperCase();
					} else {
						colorKey = formattedNumber;
						textOnImage = ContactUtilities.getAreaCode(this.context, formattedNumber);
					}
					Log.d(TAG, "Contact Image - generating image, name=" + name + ", text=" + textOnImage);
					int color = MaterialColorGenerator.get(colorKey);
					image = ImageUtilities.generateCircleBitmap(this.context, color, 40.f,
							textOnImage);
				} else {
					Log.d(TAG, "Contact Image - using contact image, contactKey=" + contactKey);
					image = ImageUtilities.circleBitmap(context, image);
					image = ImageUtilities.scaleBitmap(image, (int) ImageUtilities.convertDpToPixel(40.f));
				}
				if (config.showEmblem) {
					Bitmap emblem = BitmapFactory.decodeResource(context.getResources(), R.drawable.calllog_emblem);
					image = ImageUtilities.addCircleEmblem(context, image, emblem);
				}
				cache.write(this.context, cacheKey, image);
			}
			rv.setImageViewBitmap(R.id.contact_image, image);
		}else{
			rv.setViewVisibility(R.id.contact_image, View.GONE);
		}

		// Set the call button and row intents
		Intent makeCallIntent = new Intent();
		makeCallIntent.setAction(context.getPackageName()+"."+getID());
		makeCallIntent.putExtra(context.getPackageName() + ".action", INTENT_ACTION_MAKECALL);
		makeCallIntent.putExtra(context.getPackageName() + ".number", formattedNumber);
		if (this.config.showCallButton){
			rv.setViewVisibility(R.id.divider, View.VISIBLE);
			rv.setViewVisibility(R.id.secondary_action_icon, View.VISIBLE);
			rv.setOnClickFillInIntent(R.id.secondary_action_icon, makeCallIntent);
			Intent openCallLogIntent = new Intent();
			openCallLogIntent.setAction(context.getPackageName()+"."+getID());
			openCallLogIntent.putExtra(context.getPackageName() + ".action", INTENT_ACTION_OPENCALLLOG);
			rv.setOnClickFillInIntent(R.id.highlight, openCallLogIntent);
			rv.setImageViewResource(R.id.secondary_action_icon, R.drawable.ic_audio_phone_light);
			rv.setImageViewResource(R.id.divider, R.drawable.ic_divider_dashed_holo_light);
		}else{
			rv.setViewVisibility(R.id.divider, View.GONE);
			rv.setViewVisibility(R.id.secondary_action_icon, View.GONE);
			rv.setOnClickFillInIntent(R.id.highlight, makeCallIntent);
		}
		rv.setInt(R.id.highlight, "setBackgroundResource", R.drawable.highlight_statelist_material);

		// Set row background color
		rv.setInt(R.id.background, "setBackgroundColor", config.rowColor);

		// Set the custom text colors
		int textR = Color.red(config.textColor);
		int textG = Color.green(config.textColor);
		int textB = Color.blue(config.textColor);
		rv.setTextColor(R.id.name, Color.argb(Math.round(.87f * 255.f), textR, textG, textB));
		rv.setTextColor(R.id.number, Color.argb(Math.round(.87f * 255.f), textR, textG, textB));
		rv.setTextColor(R.id.call_date, Color.argb(Math.round(.54f * 255.f), textR, textG, textB));

		// Set the text size
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
			rv.setTextViewTextSize(R.id.name, TypedValue.COMPLEX_UNIT_SP, config.textSize);
			rv.setTextViewTextSize(R.id.number, TypedValue.COMPLEX_UNIT_SP, config.textSize * 0.7333333f);
			rv.setTextViewTextSize(R.id.call_date, TypedValue.COMPLEX_UNIT_SP, config.textSize * 0.7333333f);
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
				bubbleResId = CallLogSourceConfig.DEFAULT_BUBBLE_RESOURCE;
			}
			// Set the string so we don't have to do this again
			config.bubbleResource = bubbleResId;
			config.bubbleResourceName = context.getResources().getResourceEntryName(bubbleResId);
			LogSourceFactory.deleteSource(context, config.sourceID);
			LogSourceFactory.newSource(context, CallLogSource.class, config);
		}else{
			bubbleResId = context.getResources().getIdentifier("drawable/" + config.bubbleResourceName, null, context.getPackageName());
		}
		rv.setImageViewResource(R.id.bubble, bubbleResId);
		rv.setInt(R.id.bubble, "setImageAlpha", Color.alpha(config.bubbleColor));
		rv.setInt(R.id.bubble, "setColorFilter", config.bubbleColor);

		return rv;
	}

	@Override
	public void receiveIntent(Context context, Intent intent){
		String action = intent.getStringExtra(context.getPackageName()+".action");
		Log.d(TAG, "Received action: " + action);

		if (action.equals(INTENT_ACTION_OPENCALLLOG)){
			Intent showCallLog = new Intent();
			showCallLog.setAction(Intent.ACTION_VIEW);
			showCallLog.setType(CallLog.Calls.CONTENT_TYPE);

//			int cid = intent.getIntExtra(context.getPackageName()+".id", -1);
//			Log.d(TAG, "Received call log row id: " + cid);
//			Uri.Builder builder = CallLog.Calls.CONTENT_FILTER_URI.buildUpon();
//			Uri u = builder.appendPath(Integer.toString(cid)).build();
//			showCallLog.setData(u);

			showCallLog.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			context.startActivity(showCallLog);
			return;
		}

		if (action.equals(INTENT_ACTION_MAKECALL)){
	        String number = intent.getStringExtra(context.getPackageName()+".number");
	        if (number == null)return;
	        Uri u = Uri.parse("tel:" + number);
	        Intent callIntent = new Intent(Intent.ACTION_DIAL);
	    	callIntent.setData(u);
	    	PendingIntent pendingIntent;
	    	pendingIntent =  PendingIntent.getActivity(context, 0, callIntent, 0);  
	    	try {
	    		pendingIntent.send(context, 0, null);
	    	} catch (CanceledException e) {
	    		// TODO Auto-generated catch block
	    		e.printStackTrace();
	    	}
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
	 *	Remove the call with the given date from the database
	 */
	public boolean deleteCall(long date){
		int del = this.db.deleteRows(this.tableName, COLUMN_DATE, Long.toString(date));
		if (del == 1){
			return true;
		}else{
			return false;
		}
	}

	/**
	 *	Insert a call, collapsed by number into the database
	 */
	public void insertCall(CallsByNumber callByNumber, String numberLabel){
		if (callByNumber == null){
			Log.d(TAG, "Inserting null call!");
			return;
		}
		ContentValues c = new ContentValues();
		Call call = callByNumber.calls.elementAt(0);
		c.put(COLUMN_DATE, call.date);
		c.put(COLUMN_DURATION, call.duration);
		c.put(COLUMN_NUMBER, call.number);
		c.put(COLUMN_NUMBER_LABEL, numberLabel);
		c.put(COLUMN_LOCATION, call.location);
		c.put(COLUMN_COUNT, callByNumber.calls.size());
		// Generate the type image
		int type1 = 0;
		int type2 = 0;
		int type3 = 0;
		if (callByNumber.calls.size() > 0) type1 = callByNumber.calls.elementAt(0).type;
		if (callByNumber.calls.size() > 1) type2 = callByNumber.calls.elementAt(1).type;
		if (callByNumber.calls.size() > 2) type3 = callByNumber.calls.elementAt(2).type;
		Bitmap typeImage = getCallTypeImage(type1, type2, type3);
		if (typeImage != null){	
			ByteArrayOutputStream image = new ByteArrayOutputStream();
			typeImage.compress(Bitmap.CompressFormat.PNG, 100, image);
			c.put(COLUMN_TYPE_IMAGE, image.toByteArray());
		}
		
		// Get the lookup ID of this contact
        String[] contactData = null;
        if (call.number != null && call.number.length()  > 0) {
            contactData = ContactUtilities.getContactDataByPhoneNumber(call.number, this.context,
                    new String[]{ContactsContract.Contacts.LOOKUP_KEY, ContactsContract.Contacts.DISPLAY_NAME});
        }
		if (contactData == null || contactData.length < 1){
			c.put(COLUMN_KEY, "");
			c.put(COLUMN_NAME, "");
		}else{
			c.put(COLUMN_KEY, contactData[0]);
			c.put(COLUMN_NAME, contactData[1]);
		}

		// Insert into the database
		boolean inserted = this.db.insert(this.tableName, c);
	}
	
	/**
	 *	Insert a call into the database
	 */
	public void insertCall(Call call, String numberLabel){
		if (call == null){
			Log.d(TAG, "Inserting null call!");
			return;
		}
		Log.d(TAG, "Inserting call with number: " + call.number);
		
		ContentValues c = new ContentValues();
		c.put(COLUMN_DATE, call.date);
		c.put(COLUMN_DURATION, call.duration);
		c.put(COLUMN_NUMBER, call.number);
		c.put(COLUMN_NUMBER_LABEL, numberLabel);
		c.put(COLUMN_LOCATION, call.location);
		c.put(COLUMN_COUNT, 1);

		// Generate the type image
		Bitmap typeImage = getCallTypeImage(call.type);
		if (typeImage != null){
			ByteArrayOutputStream image = new ByteArrayOutputStream();
			typeImage.compress(Bitmap.CompressFormat.PNG, 100, image);
			c.put(COLUMN_TYPE_IMAGE, image.toByteArray());
		}else{
			c.put(COLUMN_TYPE_IMAGE, (byte[])null);
		}
		
		// Get the lookup ID of this contact
        String[] contactData = null;
        if (call.number != null && call.number.length()  > 0){
            contactData = ContactUtilities.getContactDataByPhoneNumber(call.number, this.context,
                    new String[]{ContactsContract.Contacts.LOOKUP_KEY, ContactsContract.Contacts.DISPLAY_NAME});
        }
		if (contactData == null || contactData.length < 1){
			c.put(COLUMN_KEY, "");
			c.put(COLUMN_NAME, "");
		}else{
			c.put(COLUMN_KEY, contactData[0]);
			c.put(COLUMN_NAME, contactData[1]);
		}

		// Insert into the database
		boolean inserted = this.db.insert(this.tableName, c);
		if (!inserted){
			Log.d(TAG, "Failed to insert call with number: " + call.number);
		}
	}

	
	private SQLTableColumn[] getColumns(){
		List<SQLTableColumn> cols = new ArrayList<SQLTableColumn>();
		cols.add(new SQLTableColumn(COLUMN_ID, "integer"));
		cols.add(new SQLTableColumn(COLUMN_DATE, "integer"));
		cols.add(new SQLTableColumn(COLUMN_KEY, "text"));
		cols.add(new SQLTableColumn(COLUMN_NAME, "text"));
		cols.add(new SQLTableColumn(COLUMN_NUMBER, "text"));
		cols.add(new SQLTableColumn(COLUMN_NUMBER_LABEL, "text"));
		cols.add(new SQLTableColumn(COLUMN_LOCATION, "text"));
		cols.add(new SQLTableColumn(COLUMN_TYPE_IMAGE, "blob"));
		cols.add(new SQLTableColumn(COLUMN_COUNT, "integer"));
		cols.add(new SQLTableColumn(COLUMN_DURATION, "integer"));
		SQLTableColumn[] colArray = new SQLTableColumn[cols.size()];
		return cols.toArray(colArray);
	}

	private Bitmap getCallTypeImage(int type){
		int r = getCallTypeResource(type);
		if (r == 0) return null;
		Resources res = context.getResources();
		return BitmapFactory.decodeResource(res, r);
	}
	
	private Bitmap getCallTypeImage(int type1, int type2, int type3){
		ArrayList<Bitmap> bmpA = new ArrayList<Bitmap>();
	
		Bitmap b1 = getCallTypeImage(type1);
		if (b1 != null) bmpA.add(b1);
		Bitmap b2 = getCallTypeImage(type2);
		if (b2 != null) bmpA.add(b2);
		Bitmap b3 = getCallTypeImage(type3);
		if (b3 != null) bmpA.add(b3);
		
		Bitmap[] bmps = new Bitmap[bmpA.size()];
		bmpA.toArray(bmps);
		return ImageUtilities.stackImagesHorizontal(bmps);
	}
	
	private int getCallTypeResource(int type){
		switch (type) {
			case CallLog.Calls.INCOMING_TYPE:
				return R.drawable.ic_call_incoming_holo_dark;
			case CallLog.Calls.OUTGOING_TYPE:
				return R.drawable.ic_call_outgoing_holo_dark;
			case CallLog.Calls.MISSED_TYPE:
				return R.drawable.ic_call_missed_holo_dark;
		}
		return 0;
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
