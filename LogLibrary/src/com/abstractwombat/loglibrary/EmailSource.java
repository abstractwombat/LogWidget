package com.abstractwombat.loglibrary;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.ContactsContract;
import android.provider.ContactsContract.QuickContact;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.ReentrantLock;

import javax.mail.Address;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.internet.MimeMessage;

import com.abstractwombat.contacts.ContactThumbnailsShared;
import com.abstractwombat.contacts.ContactUtilities;
import com.abstractwombat.library.SQLDatabase;
import com.abstractwombat.library.SQLTableColumn;

public class EmailSource implements ALogSource {
	private static final String TAG = "EmailSource";

	/**
	 *	Database Table Columns
	 */
	private final String COLUMN_ID = "id";
	private final String COLUMN_DATE = "date";
	private final String COLUMN_KEY = "contactkey";
	private final String COLUMN_NAME = "contactname";
    private final String COLUMN_ADDRESS = "address";
    private final String COLUMN_SUBJECT= "subject";
    private final String COLUMN_MESSAGE = "message";
    private final String COLUMN_MESSAGEID = "msgid";
    private final int maxMessageLength = 500;

    /**
     *	Intent Extra Data
     */
    private final String INTENT_ACTION_VIEWEMAIL = "viewemail";
    private final String INTENT_ACTION_REPLY = "reply";
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
    private IMAP imap;
	EmailSourceConfig config;
	
	EmailSource(){
        this.imap = new IMAP();
	}
	
	@Override
	public void config(Context context, LogSourceConfig config){
		this.context = context;
		this.db = new SQLDatabase(this.context);
		this.config = (EmailSourceConfig)config;
		this.tableName = "[" + this.getID() + "]";
		if (!locks.containsKey(this.config.sourceID)){
			locks.put(this.config.sourceID, new ReentrantLock());
		}
        // Initialize the ContactThumbnails
        ContactThumbnailsShared.initialize(R.drawable.ic_contact_picture_holo_dark);
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
        String folder = "inbox";
        int maxEmailAge = 60; // Days

        this.imap.disconnect();

        locks.get(this.config.sourceID).lock();
        try{

            AsyncTask<String, Void, ContentValues[]> task = new checkEmailTask(context, this.imap, this).execute(
                    config.server, Integer.toString(config.port), folder,
                    config.username, config.password, Integer.toString(config.count),
                    Integer.toString(maxEmailAge));
            ContentValues[] values = new ContentValues[0];
            try {
                values = task.get(30, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
                Log.d(TAG, "CheckEmailTask interrupted");
                return;
            } catch (ExecutionException e) {
                e.printStackTrace();
                Log.d(TAG, "CheckEmailTask exception");
                return;
            } catch (TimeoutException e) {
                e.printStackTrace();
                Log.d(TAG, "TimeoutException exception");
                return;
            }
            if (values == null || values.length == 0){
                Log.d(TAG, "No data returned from CheckEmailTask");
                return;
            }

			// Recreate the table
			this.db.deleteTable(this.tableName);
			this.db.createTable(this.tableName, getColumns());

            for (int i=0; i<Math.min(values.length, config.count); i++){
                insertValue(values[i]);
            }
		}finally{
			locks.get(this.config.sourceID).unlock();
		}
	}

    @Override
    public int size() {
        return (int)this.db.rowCount(this.tableName);
    }

    private class checkEmailTask extends AsyncTask<String, Void, ContentValues[]> {
        private Context context;
        private IMAP imap;
        private EmailSource source;

        checkEmailTask(Context context, IMAP imap, EmailSource source){
            super();
            this.context = context;
            this.imap = imap;
            this.source = source;
        }

        protected ContentValues[] doInBackground(String... imapData) {
            int count = imapData.length;
            if (count != 7) return null;
            String s = imapData[0];
            int port = Integer.parseInt(imapData[1]);
            String folder = imapData[2];
            String u = imapData[3];
            String p = imapData[4];
            int msgCount = Integer.parseInt(imapData[5]);
            int age = Integer.parseInt(imapData[6]);

            // Connect to IMAP
            int error = imap.connectWithLogin(s, port, u, p);
            if (error == 1){
                Log.e(TAG, "Failed to connect to the IMAP server!");
            }else if (error == 2){
                Log.e(TAG, "Invalid Username and/or Password");
            }else if (error == 3){
                Log.e(TAG, "Failed to connect to the IMAP server! (already connected?)");
            }else if (error == 4){
                Log.e(TAG, "Failed to connect to the IMAP server! (success?)");
            }
            if (error != 0) return null;

            // Get the messaged
            Message[] messages = this.imap.getRecentMessages("inbox", 14);
            Log.d(TAG, "Got " + messages.length + " messages");

            ContentValues[] values = new ContentValues[messages.length];
            int i=0;
            for (Message m : messages){
                try {
                    Log.d(TAG, "Message: " + m.getSubject().toString());
                } catch (MessagingException e) {
                    Log.d(TAG, "MessagingException while listing messages");
                    e.printStackTrace();
                }
                values[i++] = source.messageToContent(m);
                if (i >= msgCount) break;
            }
            imap.disconnect();
            return values;
        }
        protected void onPreExecute() {
        }
        protected void onPostExecute(Integer result) {
        }
    }

    @Override
    public long getDateAt(int position){
        Log.d(TAG, "Getting date at Position: " + position);

        locks.get(this.config.sourceID).lock();
        ContentValues cv;
        long date = 0;
        try{
            cv = this.db.getAt(this.tableName, position, COLUMN_DATE, true);
            date = cv.getAsLong(COLUMN_DATE);
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
		RemoteViews rv = new RemoteViews(this.context.getPackageName(), R.layout.email_row);

		// Fetch the appropriate row from the database
		ContentValues c = getDataAt(position);
		if (c == null){
			Log.d(TAG, "Got null row!");
			return new RemoteViews(this.context.getPackageName(), R.layout.empty_row);
		}

		// Setup the contact image intent
		String contactKey = c.getAsString(COLUMN_KEY);
		rv.setViewVisibility(R.id.email_row_contact_image_touch, View.GONE);
		if (contactKey != null && contactKey.length() > 0){
			Intent contactImageIntent = new Intent();
			contactImageIntent.setAction(context.getPackageName()+"."+getID());
			contactImageIntent.putExtra(context.getPackageName() + ".action", INTENT_ACTION_QUICKCONTACT);
			contactImageIntent.putExtra(context.getPackageName() + ".lookupKey", contactKey);
			rv.setOnClickFillInIntent(R.id.email_row_contact_image_touch, contactImageIntent);
			rv.setViewVisibility(R.id.email_row_contact_image_touch, View.VISIBLE);
		}

		// Set the date
		long time = c.getAsLong(COLUMN_DATE);
		String timeString = dateToString(time);
        rv.setTextViewText(R.id.date, timeString);

        // Set the subject
        String subject = c.getAsString(COLUMN_SUBJECT);
        rv.setTextViewText(R.id.subject, subject);

        // Set the name
        rv.setTextViewText(R.id.name, c.getAsString(COLUMN_NAME));

        // Set the message
        rv.setTextViewText(R.id.message, c.getAsString(COLUMN_MESSAGE));

		// Set the image to the contact's image
		rv.setViewVisibility(R.id.email_row_contact_image, View.VISIBLE);
		Bitmap image = ContactThumbnailsShared.get(this.context, contactKey);
		rv.setImageViewBitmap(R.id.email_row_contact_image, image);

        // Create a Reply intent
        Intent replyIntent = new Intent();
        replyIntent.setAction(context.getPackageName()+"."+getID());
        replyIntent.putExtra(context.getPackageName() + ".action", INTENT_ACTION_REPLY);
        replyIntent.putExtra(context.getPackageName() + ".email", c.getAsString(COLUMN_ADDRESS));
        replyIntent.putExtra(context.getPackageName() + ".subject", subject);

        // Create a view email intent
        Intent viewIntent = new Intent();
        replyIntent.setAction(context.getPackageName()+"."+getID());
        replyIntent.putExtra(context.getPackageName() + ".action", INTENT_ACTION_VIEWEMAIL);
        replyIntent.putExtra(context.getPackageName() + ".messageID", c.getAsString(COLUMN_MESSAGEID));
        replyIntent.putExtra(context.getPackageName() + ".server", this.config.server);
        replyIntent.putExtra(context.getPackageName() + ".folder", this.config.folder);
        replyIntent.putExtra(context.getPackageName() + ".port", this.config.port);
        replyIntent.putExtra(context.getPackageName() + ".username", this.config.username);
        replyIntent.putExtra(context.getPackageName() + ".password", this.config.password);

        // Set the row's intent
        rv.setOnClickFillInIntent(R.id.email_row_parent, replyIntent);

		return rv;
	}

	@Override
	public void receiveIntent(Context context, Intent intent){
		String action = intent.getStringExtra(context.getPackageName()+".action");
		Log.d(TAG, "Received action: " + action);

        if (action.equals(INTENT_ACTION_VIEWEMAIL)){
            String messageID = intent.getStringExtra(context.getPackageName()+".messageID");
            String server = intent.getStringExtra(context.getPackageName()+".server");
            String folder = intent.getStringExtra(context.getPackageName()+".folder");
            int port = intent.getIntExtra(context.getPackageName()+".port", 0);
            String username = intent.getStringExtra(context.getPackageName()+".username");
            String password = intent.getStringExtra(context.getPackageName()+".password");


            Intent viewIntent = new Intent(context, ViewEmailActivity.class);
            viewIntent.putExtra("Server", server);
            viewIntent.putExtra("Port", port);
            viewIntent.putExtra("Username", username);
            viewIntent.putExtra("Password", password);
            viewIntent.putExtra("MessageID", messageID);
            viewIntent.putExtra("Folder", folder);

            viewIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(viewIntent);
            return;
        }

		if (action.equals(INTENT_ACTION_REPLY)){
            String address = intent.getStringExtra(context.getPackageName() + ".email");
            String subject = "RE: " + intent.getStringExtra(context.getPackageName() + ".subject");

            Intent email = new Intent(Intent.ACTION_SEND);
            email.putExtra(Intent.EXTRA_EMAIL, new String[]{address});
            email.putExtra(Intent.EXTRA_SUBJECT, subject);
            email.putExtra(Intent.EXTRA_TEXT, "");
            email.setType("message/rfc822");
            email.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            Intent chooser = Intent.createChooser(email, "Choose an Email client :");
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(chooser);
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
	 *	Insert a Message into the database
	 */
	public void insertMessage(Message msg){
        ContentValues c = messageToContent(msg);
        insertValue(c);
	}
    public void insertValue(ContentValues c){
        if (c == null){
            Log.d(TAG, "Inserting null Message!");
            return;
        }
        // Insert into the database
        boolean inserted = this.db.insert(this.tableName, c);
        if (!inserted){
            Log.d(TAG, "Failed to insert message from address: " + c.getAsString(COLUMN_ADDRESS));
        }
    }
    private ContentValues messageToContent(Message msg){
        if (msg == null) return null;

        // Extract the data from the Message
        long time = 0;
        String fromAddress = "";
        String name = "";
        String key = "";
        String subject = "";
        String message = "";
        String msgid = "";
        try {
            time = msg.getReceivedDate().getTime();
            Address[] addresses = msg.getFrom();
            if (addresses.length > 0){
                fromAddress = addresses[0].toString();
                String[] data = ContactUtilities.getContactDataByEmail(fromAddress, this.context,
                        new String[]{ContactsContract.Contacts.LOOKUP_KEY, ContactsContract.Contacts.DISPLAY_NAME});
                if (data != null && data.length == 2){
                    name = data[1];
                    key = data[0];
                }else{
                    name = fromAddress;
                    key = "";
                }
            }
            subject = msg.getSubject();
            message = getMessageBody(msg);
            MimeMessage mm = (MimeMessage)(msg);
            msgid = mm.getMessageID();//.getHeader("Message-Id")[0]; //getMessageNumber();
        } catch (MessagingException e) {
            Log.d(TAG, "MessagingException while create ContentValues from a Message");
            e.printStackTrace();
            return null;
        }

        // Limit message
        if (message.length() > maxMessageLength){
            message = message.substring(0, maxMessageLength);
        }

        // Create the ContentValues
        ContentValues c = new ContentValues();
        c.put(COLUMN_DATE, time);
        c.put(COLUMN_NAME, name);
        c.put(COLUMN_KEY, key);
        c.put(COLUMN_ADDRESS, fromAddress);
        c.put(COLUMN_SUBJECT, subject);
        c.put(COLUMN_MESSAGE, message);
        c.put(COLUMN_MESSAGEID, msgid);

        return c;
    }

	private String getMessageBody(Message m){
        StringBuilder body = new StringBuilder();
        Object content = null;
        try {
            content = m.getContent();
            if (content instanceof String){
                body.append((String)content);
            }else if (content instanceof Multipart){
                Multipart mp = (Multipart)content;
                for(int i = 0; i < mp.getCount(); i++) {
                    BodyPart bp = mp.getBodyPart(i);
                    String disp = bp.getDisposition();
                    if(disp != null && (disp.equals(BodyPart.ATTACHMENT))) {
                        // Do something
                    } else {
                        body.append(bp.getContent());
                    }
                }

            }
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        } catch (MessagingException e) {
            e.printStackTrace();
            return "";
        }
        return body.toString();
    }

	private SQLTableColumn[] getColumns(){
		List<SQLTableColumn> cols = new ArrayList<SQLTableColumn>();
		cols.add(new SQLTableColumn(COLUMN_ID, "integer"));
		cols.add(new SQLTableColumn(COLUMN_DATE, "integer"));
		cols.add(new SQLTableColumn(COLUMN_KEY, "text"));
		cols.add(new SQLTableColumn(COLUMN_NAME, "text"));
		cols.add(new SQLTableColumn(COLUMN_ADDRESS, "text"));
        cols.add(new SQLTableColumn(COLUMN_SUBJECT, "text"));
        cols.add(new SQLTableColumn(COLUMN_MESSAGE, "text"));
        cols.add(new SQLTableColumn(COLUMN_MESSAGEID, "text"));

        SQLTableColumn[] colArray = new SQLTableColumn[cols.size()];
		return cols.toArray(colArray);
	}

	private String dateToString(long timeInMilliSeconds) {
		Date date = new Date(timeInMilliSeconds);
		Calendar cal = new GregorianCalendar();
		cal.setTime(date);
		
		Calendar now = Calendar.getInstance();
		DateFormat dateForm = DateFormat.getDateInstance(DateFormat.SHORT);
		DateFormat timeForm = DateFormat.getTimeInstance(DateFormat.SHORT);

		// Check if this is today
		if (cal.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR) && 
			now.get(Calendar.YEAR) == cal.get(Calendar.YEAR)){
			// Just return the time
			return timeForm.format(date);
		}

		// Check if this is yesterday
		if (cal.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR)-1 && 
			now.get(Calendar.YEAR) == cal.get(Calendar.YEAR)){
			// Just return the time
			return "Yesterday";
		}

		// Check if this is this week
		if (cal.get(Calendar.DAY_OF_YEAR) > now.get(Calendar.DAY_OF_YEAR)-7 && 
			now.get(Calendar.YEAR) == cal.get(Calendar.YEAR)){
			// Just return the day of the week
			SimpleDateFormat outFormat = new SimpleDateFormat("EEEE");
			return outFormat.format(date);
		}

		// Older that a week, show full date
		return dateForm.format(date);
	}

}
