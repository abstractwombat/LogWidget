package com.abstractwombat.logwidget;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;

/**
 * Created by krista on 4/26/15.
 */
public class BannerManager {
    private static final String SHARED_PREFERENCE_FILE = "BannerData";

    public class BannerData {
        public String text;
        public Bitmap image;
        public String uri;
        public boolean closeable;
    }

    private BannerData getData(Context context){
        SharedPreferences prefs = context.getSharedPreferences(SHARED_PREFERENCE_FILE, context.MODE_MULTI_PROCESS);

        return new BannerData();
    }

}
