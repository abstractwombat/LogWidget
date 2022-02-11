package com.abstractwombat.contacts;

import android.util.Log;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.abstractwombat.images.ImageCache;
import com.abstractwombat.images.ImageUtilities;

public class ContactImages {
	private static String TAG = "ContactImages";
	
	public static int DEFAULT_IMAGE_RES_ID;
	
	private static ImageCache cache;
	private static String cacheSubdirectory = "ContactImagesCache";
	private static String defaultImageKey = "defaultimage";
	
	public static void initialize(Context context, int defaultImageResource){
		DEFAULT_IMAGE_RES_ID = defaultImageResource;
		initializeCache();
	}
    public static boolean hasInitialized(){
        if (cache == null) return false;
        if (DEFAULT_IMAGE_RES_ID == 0) return false;
        return true;
    }
	
	public static Bitmap get(Context context, String lookupKey, int maxImageSize){
		initializeCache();
        String cacheKey = getCacheKey(lookupKey, maxImageSize);

		// If the key is invalid, use the default
		if (lookupKey == null || lookupKey.length() == 0){
			return getDefault(context, maxImageSize);
		}

		// Read from the cache
		Bitmap b = cache.read(context, cacheKey);
		if (b != null) {
			return b;
		}

		// Not in cache, read from the system
		Log.d(TAG, "Reading contact image from system: key " + lookupKey);
		b = ContactUtilities.getContactPhoto(lookupKey, context);

		if (b == null){
			// No image found for this contact, use the default image
			b = getDefault(context, maxImageSize);
		}

        if (b != null && maxImageSize > 0){
            b = ImageUtilities.scaleBitmap(b, maxImageSize);
        }

		// Write to the cache
		if (b != null) {
			cache.write(context, cacheKey, b);
		}

		return b;
	}

	public static Bitmap getDefault(Context context, int maxImageSize){
		initializeCache();
        String cacheKey = getCacheKey(defaultImageKey, maxImageSize);

		Bitmap b = cache.read(context, cacheKey);
		if (b != null) {
			Log.d(TAG, "Read default contact image from memory cache");
			return b;
		}

    	// Not in cache, decode the resource
		Log.d(TAG, "Reading default contact image from resource");
		b = BitmapFactory.decodeResource(context.getResources(), DEFAULT_IMAGE_RES_ID);
		if (b != null){
            if (maxImageSize > 0){
                b = ImageUtilities.scaleBitmap(b, maxImageSize);
            }
			// Write to the cache
			cache.write(context, cacheKey, b);
		}
		return b;
	}

	public static boolean remove(Context context, String lookupKey, int maxImageSize){
		initializeCache();
        String cacheKey = getCacheKey(lookupKey, maxImageSize);
		return cache.remove(context, cacheKey);
	}
	
	public static boolean clear(Context context){
		initializeCache();
		return cache.clear(context);
	}

	private static void initializeCache(){
		if (cache == null){
			cache = new ImageCache(cacheSubdirectory);
		}
	}

    private static String getCacheKey(String lookupKey, int maxImageSize){
        return lookupKey + " - " + Integer.toString(maxImageSize);
    }

}