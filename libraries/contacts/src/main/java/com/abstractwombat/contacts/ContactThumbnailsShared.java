package com.abstractwombat.contacts;

import android.content.Context;
import android.graphics.Bitmap;

public class ContactThumbnailsShared {
	private static String TAG = "ContactThumbnailsShared";
	
	public static ContactThumbnails thumbnails;

    public static void initialize(int defaultImageResource){
		thumbnails = new ContactThumbnails();
		thumbnails.initialize(defaultImageResource);
    }
    
	public static void initialize(int defaultImageResource, boolean enableDiskCache){
		thumbnails = new ContactThumbnails();
		thumbnails.initialize(defaultImageResource, enableDiskCache);
	}
	
	public static Bitmap get(Context context, String lookupKey){
		thumbnails.setContext(context);
		return thumbnails.get(lookupKey);
	}
    public static Bitmap get(Context context, String lookupKey, boolean useDefault) {
        thumbnails.setContext(context);
        return thumbnails.get(lookupKey, useDefault);
    }

	public static Bitmap getDefault(Context context){
		thumbnails.setContext(context);
		return thumbnails.getDefault();
	}

	public static boolean remove(Context context, String lookupKey){
		thumbnails.setContext(context);
		return thumbnails.remove(lookupKey);
	}
	
	public static boolean clear(Context context){
		thumbnails.setContext(context);
		return thumbnails.clear();
	}
}