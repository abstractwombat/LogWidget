package com.abstractwombat.library;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDoneException;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.util.Log;

import java.util.HashSet;

/**
 *	Convenience wrapper for an SQL database.
 */
public class SQLDatabase{
	private static final String TAG = "SQLDatabase";
	private static Object lock = new Object();
	private SQLiteOpenHelper dbOpener;
	private static HashSet<String> createdTables = new HashSet<>();

	public SQLDatabase(Context context){
		this.dbOpener = new DBOpenHelper(context);
	}

	/**
	 *	Creates the table
	 */
	public void createTable(String tableName, SQLTableColumn[] columns){
		String SQLTableColumns = columnsToString(columns);
		if (!createdTables.contains(tableName)) {
			createTable(tableName, SQLTableColumns);
			createdTables.add(tableName);
		}
	}

	/**
	 *	Deletes the table
	 */
	public void deleteTable(String tableName){
		synchronized(lock){
			SQLiteDatabase writeDB = this.dbOpener.getWritableDatabase();
			if (writeDB!=null){
				writeDB.execSQL("DROP TABLE IF EXISTS " + tableName);
				writeDB.close();
				createdTables.remove(tableName);
			}
		}
	}
	
	/**
	 *	Gets the number of rows in the given table
	 */
	public long rowCount(String tableName){
		long c = 0;
		synchronized(lock){
			SQLiteDatabase readDB = this.dbOpener.getReadableDatabase();
			SQLiteStatement statement = readDB.compileStatement("SELECT Count(*) FROM " + tableName);
			try {
				c = statement.simpleQueryForLong();
			} catch (SQLiteDoneException e){
				e.printStackTrace();
			}
			if (readDB!=null) readDB.close();
		}
		return c;
	}
	
	/**
	 *	Inserts the given row into the given table
	 */
	public boolean insert(String tableName, ContentValues contentValues){
		SQLiteDatabase writeDB = null;
		long i = -1;
		synchronized(lock){
			writeDB = this.dbOpener.getWritableDatabase();
			i = writeDB.insert(tableName, null, contentValues);
		}
		writeDB.close();

		if (i == -1){
			Log.w(TAG, "Failed to insert row into table: " + tableName);
			return false;
		}else{
			Log.d(TAG, "Inserted row at row id (" + i + ") into table: " + tableName);
			return true;
		}
	}
	
	/**
	 *	Updates the rows where "columnName"="value" with the data in the given columnName
	 */
	public int update(String tableName, String columnName, String value, ContentValues newValues){
		SQLiteDatabase writeDB = null;
		int updated = 0;
		synchronized(lock){
			writeDB = this.dbOpener.getWritableDatabase();
			updated = writeDB.update(tableName, newValues, columnName + "=?", new String[] { value });	
		}
		Log.d(TAG, "Updated " + updated + " rows in table: " + tableName);
		if (writeDB!=null) writeDB.close();
		return updated;
	}
	
	/**
	 *	Deletes all the rows from the given table with the given value in the given column.
	 */
	public int deleteRows(String tableName, String columnName, String value){
		SQLiteDatabase writeDB = null;
		int deleted = 0;
		synchronized(lock){
			writeDB = this.dbOpener.getWritableDatabase();
			deleted = writeDB.delete(tableName, columnName + "=?", new String[] { value });
		}
		Log.d(TAG, "Deleted " + deleted + " rows from table: " + tableName);
		if (writeDB!=null) writeDB.close();
		return deleted;
	}

	/**
	 *	Gets all the rows in the given table, sorted by the given column.
	 */
	public ContentValues[] get(String tableName, String sortByColumn, boolean descending){
		// Query the database
		SQLiteDatabase readDB = null;
		Cursor cursor = null;
		synchronized(lock){
			readDB = this.dbOpener.getReadableDatabase();
			cursor = this.query(readDB, tableName, sortByColumn, descending);
		}
		
		if (cursor == null || !cursor.moveToFirst()){
			if (cursor!=null) cursor.close();
			if (readDB!=null) readDB.close();
			Log.w(TAG, "get: Failed to move moveToFirst");
			return null;
		}
		ContentValues[] cv = new ContentValues[cursor.getCount()];
		int i=0;
		do {
			cv[i++] = cursorToValues(cursor);
		} while (cursor.moveToNext());
		cursor.close();
		readDB.close();
		return cv;
	}

	/**
	 *	Gets the rows in the given table with the given value in the given column.
	 */
	public ContentValues[] get(String tableName, String columnName, String value){
		// Query the database
		SQLiteDatabase readDB = null;
		Cursor cursor = null;
		synchronized(lock){
			readDB = this.dbOpener.getReadableDatabase();
			cursor = readDB.query(tableName, null, columnName + "=?", new String[] { value }, null, null, null);
		}
		
		if (cursor == null || !cursor.moveToFirst()){
			if (cursor!=null) cursor.close();
			if (readDB!=null) readDB.close();
			Log.w(TAG, "get: Failed to move moveToFirst");
			return null;
		}
		ContentValues[] cv = new ContentValues[cursor.getCount()];
		int i=0;
		do {
			cv[i++] = cursorToValues(cursor);
		} while (cursor.moveToNext());
		cursor.close();
		readDB.close();
		return cv;
	}
	
	/**
	 *	Gets the row at the given position in the given table, sorted by the given column.
	 */
	public ContentValues getAt(String tableName, int position, String sortByColumn, boolean descending){
		SQLiteDatabase readDB = null;
		Cursor cursor = null;
		synchronized(lock){
			readDB = this.dbOpener.getReadableDatabase();
			cursor = this.query(readDB, tableName, sortByColumn, descending);
		}
			
		// Move the cursor to the first position
		if (cursor == null || !cursor.moveToPosition(position)){
			if (cursor!=null) cursor.close();
			if (readDB!=null) readDB.close();
			Log.w(TAG, "get: Failed to move to: " + position);
			return null;
		}
		
		ContentValues content = cursorToValues(cursor);
		cursor.close();
		readDB.close();
		
		return content;
	}
	
	private Cursor query(SQLiteDatabase readDB, String tableName, String sortByColumn, boolean descending){
		// Format the ORDER BY statement
		String orderBy = sortByColumn;
		if (descending){
			orderBy += " DESC";
		}else{
			orderBy += " ASC";
		}
		// Query the database
		Cursor cursor = null;
		synchronized(lock){
			cursor = readDB.query(tableName, null, null, null, null, null, orderBy);
		}
		return cursor;
	}
	
	private ContentValues cursorToValues(Cursor cursor){
		ContentValues cv = new ContentValues();
		// Iterate over the columns
		for (int i=0; i<cursor.getColumnCount(); i++){
			String columnName = cursor.getColumnName(i);
			switch (cursor.getType(i)){
				case Cursor.FIELD_TYPE_NULL:
					break;
				case Cursor.FIELD_TYPE_INTEGER:
					cv.put(columnName, cursor.getLong(i));
					break;
				case Cursor.FIELD_TYPE_FLOAT:
					cv.put(columnName, cursor.getFloat(i));
					break;
				case Cursor.FIELD_TYPE_STRING:
					cv.put(columnName, cursor.getString(i));
					break;
				case Cursor.FIELD_TYPE_BLOB:
					cv.put(columnName, cursor.getBlob(i));
					break;
			}
		}

		return cv;
	}
	
	private void createTable(String tablename, String SQLTableColumns){
		synchronized(lock){
			Log.d(TAG, "Creating table : " + tablename);
			SQLiteDatabase writeDB = this.dbOpener.getWritableDatabase();
			writeDB.execSQL("CREATE TABLE if not exists " + tablename + " " + SQLTableColumns + ";");
			writeDB.close();
		}
	}
	
	private String columnsToString(SQLTableColumn[] columns){
		String s = "(_id integer primary key autoincrement, ";
		for (SQLTableColumn c : columns){
			s += c.name + " " + c.type + ", ";
		}
		s = s.substring(0, s.length()-2);
		s += ")";
		return s;
	}

	private class DBOpenHelper extends SQLiteOpenHelper {
		public static final String DATABASE_NAME = "sbcontactwidget";
		public static final int DATABASE_VERSION = 1;
		
		DBOpenHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}
		@Override
		public void onCreate(SQLiteDatabase arg0) {
		}
		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		}
	}

}
