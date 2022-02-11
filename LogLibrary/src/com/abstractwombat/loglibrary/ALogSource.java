package com.abstractwombat.loglibrary;

import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

public interface ALogSource {
	void config(Context context, LogSourceConfig config);
	LogSourceConfig config();
	//Fragment configFragment();
	void update();
    int size();
	long getDateAt(int position);
	RemoteViews getViewAt(int position);
	void receiveIntent(Context context, Intent intent);
}
