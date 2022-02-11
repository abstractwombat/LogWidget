package com.abstractwombat.loglibrary;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import java.util.ArrayList;

import com.abstractwombat.library.SQLDatabase;
import com.abstractwombat.library.SQLTableColumn;

public class GMailLogSource implements ALogSource {
	private final String TABLE_NAME = "sms";

	private final String COLUMN_DATE = "date";
	private final String COLUMN_KEY = "contactkey";
	private final String COLUMN_EMAIL = "email";
	private final String COLUMN_SUBJECT = "subject";
	private final String COLUMN_IMAGE = "image";

	private int MAX_LOG_ENTRIES;
	private Context context;
	private SQLDatabase db;
	private String tableName;
	private LogSourceConfig config;

	GMailLogSource(Context context){
		this.context = context;
		db = new SQLDatabase(this.context);
	}

	public RemoteViews getViewAt(int position){
		ContentValues c = this.db.getAt(TABLE_NAME, position, COLUMN_DATE, true);
		return new RemoteViews(null);
	}

	private SQLTableColumn[] getColumns(){
		ArrayList<SQLTableColumn> cols = new ArrayList<SQLTableColumn>();
		cols.add(new SQLTableColumn(COLUMN_DATE, "integer"));
		cols.add(new SQLTableColumn(COLUMN_KEY, "text"));
		cols.add(new SQLTableColumn(COLUMN_EMAIL, "text"));
		cols.add(new SQLTableColumn(COLUMN_SUBJECT, "text"));
		cols.add(new SQLTableColumn(COLUMN_IMAGE, "blob"));
		return (SQLTableColumn[]) cols.toArray();
	}

	@Override
	public void config(Context context, LogSourceConfig config) {
		this.context = context;
		this.config = config;
		this.tableName = "Table" + this.config.sourceID;
		db = new SQLDatabase(this.context);
	}
	@Override
	public LogSourceConfig config() {
		return this.config;
	}
	@Override
	public void receiveIntent(Context context, Intent intent) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void update() {
		// TODO Auto-generated method stub
		
	}

    @Override
    public int size() {
        // TODO
        return 0;
    }

	@Override
	public long getDateAt(int position) {
		// TODO Auto-generated method stub
		return 0;
	}
	
}
