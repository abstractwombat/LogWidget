package com.abstractwombat.loglibrary;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteException;
import android.util.Log;
import android.widget.RemoteViews;

import com.abstractwombat.library.SQLDatabase;
import com.abstractwombat.library.SQLTableColumn;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

public class CombinedLogSource implements ALogSource {
	private static final String TAG = "CombinedLogSource";

	/**
	 *	Database Table Columns
	 */
	private final String COLUMN_DATE = "date";
	private final String COLUMN_SOURCE_ID = "sourceid";
	private final String COLUMN_SOURCE_POSITION = "position";
	
	private final String SOURCE_COLUMN_DATE = "date";

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
	private CombinedLogSourceConfig config;

	CombinedLogSource(){
	}
	
	@Override
	public void config(Context context, LogSourceConfig config){
		this.context = context;
		db = new SQLDatabase(this.context);
		this.config = (CombinedLogSourceConfig)config;
		this.tableName = "[" + this.config.sourceID + "]";
		if (!locks.containsKey(this.config.sourceID)){
			locks.put(this.config.sourceID, new ReentrantLock());
		}
	}

	@Override
	public LogSourceConfig config(){
		return this.config;
	}

	@Override
	public void update(){
		Log.d(TAG, "Updating Source ID: " + this.config.sourceID);
		if (this.config.sources == null) return;
		Log.d(TAG, "Combined Source contains " + this.config.sources.length + " sources");
		
		locks.get(this.config.sourceID).lock();
		try{
			// Recreate the table
			this.db.deleteTable(this.tableName);
			this.db.createTable(this.tableName, getColumns());
			class SourceData{
				Class source;
				ContentValues dbValues;
				SourceData(Class s, ContentValues cv) { this.source=s; this.dbValues=cv; }
			}
			Set<Long> duplicateDates = new HashSet<>();
			//Map<Long, List<SourceData>> duplicateDates = new HashMap<>();
			for (String sourceID : this.config.sources){
				Log.d(TAG, "Loading Source: " + sourceID);
				ALogSource source = LogSourceFactory.get(this.context, sourceID);
				if (source == null) continue;
				source.update();
				//String sourceStr = source.getClass().getName();
				for (int p=0; p<source.config().count; p++){
					// Get the date from this source
                    Long date = source.getDateAt(p);
					if (date == null || date == 0) {
						Log.d(TAG, "No date column found at position " + p + " in source " + source.getClass().getName() + " (id=" + sourceID +")");
						continue;
					}
					Log.d(TAG, "Got source date: " + date);
					// Create the table row in the database
					ContentValues c = new ContentValues();
					c.put(COLUMN_DATE, date);
					c.put(COLUMN_SOURCE_ID, sourceID);
					c.put(COLUMN_SOURCE_POSITION, p);
					// Record duplicate date entries
					ContentValues[] existingEntry = this.db.get(tableName, COLUMN_DATE, date.toString());
					if (existingEntry != null && existingEntry.length > 0){
						duplicateDates.add(date);
//						Log.d(TAG, "Previous entry at this date found");
//						if (!duplicateDates.containsKey(date)){
//							duplicateDates.put(date, new ArrayList<SourceData>());
//						}
//						duplicateDates.get(date).add(new SourceData(source.getClass(), c));
					}
					// Insert into the db
					if (!this.db.insert(tableName, c)){
						Log.d(TAG, "Failed to add data to combined source at position " + p + " from source " + source.getClass().getName() + " (id=" + sourceID +")");
					}
				}
			}
			// Reduce duplicate dates to one entry, prioritizing based on source type
			Class priority[] = { SMSLogSource.class, CallLogSource.class, HangoutsSource.class, WhatsAppSource.class, FacebookMessengerSource.class, SkypeSource.class, ViberSource.class, WeChatSource.class };
			List<Class> priorityList = Arrays.asList(priority);
			for (Long date : duplicateDates){
				ContentValues bestValues = null;
				int lowestIndex = 100;
				ContentValues[] entries = this.db.get(tableName, COLUMN_DATE, date.toString());
				for (ContentValues values : entries){
					String sourceID = values.getAsString(COLUMN_SOURCE_ID);
					ALogSource source = LogSourceFactory.get(this.context, sourceID);
					int i = priorityList.indexOf(source.getClass());
					if (i >= 0 && i < lowestIndex){
						lowestIndex = i;
						bestValues = values;
					}
				}
				if (bestValues != null){
					Log.d(TAG, "Duplicate entry to keep: " + (lowestIndex < priority.length ? priority[lowestIndex] : "unknown"));
					// Delete all entry with this date
					this.db.deleteRows(tableName, COLUMN_DATE, date.toString());
					if (!this.db.insert(tableName, bestValues)){
						Log.d(TAG, "Failed to re-add duplicate date entry");
					}
				}
			}
//			for (Map.Entry<Long, List<SourceData>> duplicates : duplicateDates.entrySet()){
//				// Choose which entry to add back
//				ContentValues bestValues = null;
//				int lowestIndex = 100;
//				for (SourceData data : duplicates.getValue()){
//					int i = priorityList.indexOf(data.source);
//					if (i < lowestIndex){
//						lowestIndex = i;
//						bestValues = data.dbValues;
//					}
//				}
//				if (bestValues != null){
//					Log.d(TAG, "Duplicate entry to keep: " + priority[lowestIndex]);
//					// Delete all entry with this date
//					this.db.deleteRows(tableName, COLUMN_DATE, duplicates.getKey().toString());
//					if (!this.db.insert(tableName, bestValues)){
//						Log.d(TAG, "Failed to re-add duplicate date entry");
//					}
//				}
//			}
		}finally{
			locks.get(this.config.sourceID).unlock();
		}
	}

    @Override
    public int size() {
        int s = 0;
        for (String sourceID : this.config.sources){
            ALogSource source = LogSourceFactory.get(this.context, sourceID);
            if (source == null) continue;
            s += source.size();
        }
        Log.d(TAG, "child source's summed count: " + s);

        int r = Math.min(s, this.config.count);
        Log.d(TAG, "size: " + r);
        return r;
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
		try {
			cv = this.db.getAt(this.tableName, position, COLUMN_DATE, true);
		}catch (SQLiteException e){
			cv = null;
		}finally{
			locks.get(this.config.sourceID).unlock();
		}
		return cv;
	}
	
	@Override
	public RemoteViews getViewAt(int position){
		Log.d(TAG, "Requested Position: " + position);

		// Fetch the appropriate row from the database
		ContentValues c = getDataAt(position);
		if (c == null){
			Log.d(TAG, "Failed to get data at position: " + position);
			return new RemoteViews(this.context.getPackageName(), R.layout.empty_row);
		}
		
		String sourceID = c.getAsString(COLUMN_SOURCE_ID);
		Integer sourcePosition = c.getAsInteger(COLUMN_SOURCE_POSITION);
		if (sourceID == null || sourcePosition == null){
			return null;
		}
		ALogSource source = LogSourceFactory.get(this.context, sourceID);
        if (source == null){
            Log.e(TAG, "Null source with id: " + sourceID);
        }
		return source.getViewAt(sourcePosition);
	}
	
	@Override
	public void receiveIntent(Context context, Intent intent){
		String action = intent.getStringExtra(context.getPackageName()+".action");
		Log.d(TAG, "Received action: " + action);
	}


	private SQLTableColumn[] getColumns(){
		List<SQLTableColumn> cols = new ArrayList<SQLTableColumn>();
		cols.add(new SQLTableColumn(COLUMN_DATE, "integer"));
		cols.add(new SQLTableColumn(COLUMN_SOURCE_ID, "text"));
		cols.add(new SQLTableColumn(COLUMN_SOURCE_POSITION, "integer"));
		SQLTableColumn[] colArray = new SQLTableColumn[cols.size()];
		return cols.toArray(colArray);
	}

}
