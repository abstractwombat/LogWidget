package com.abstractwombat.loglibrary;

import android.widget.ListAdapter;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RemoteViews;
import android.database.DataSetObserver;

public class LogSourceAdapter implements ListAdapter {
	private ALogSource mSource;
	private Context mContext;
	
	public LogSourceAdapter(Context context, ALogSource source){
		mSource = source;
		mContext = context;
	}
	
	public View getView(int position, View convertView, ViewGroup parent){
		RemoteViews rm = mSource.getViewAt(position);
		return rm.apply(mContext, parent);
	}
	
	public Object getItem(int position){
		return (Object)mSource.getDateAt(position);
	}
	
	public int getCount(){
		return mSource.config().count;
	}
	
	public void registerDataSetObserver(DataSetObserver observer){
	}
	public void unregisterDataSetObserver(DataSetObserver observer){
	}
	
	public int getViewTypeCount(){
		return 3;
	}
	public int getItemViewType(int position){
		return 1;
	}
	public boolean isEmpty(){
		return false;
	}
	public boolean	hasStableIds(){
		return false;
	}
	public boolean areAllItemsEnabled(){
		return true;
	}
	public boolean isEnabled(int position){
		return true;
	}
	public long getItemId(int position){
		return 0;
	}
}
