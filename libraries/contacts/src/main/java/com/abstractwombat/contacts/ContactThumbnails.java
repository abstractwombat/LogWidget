package com.abstractwombat.contacts;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import com.abstractwombat.images.ImageCache;

public class ContactThumbnails {
	private static String TAG = "ContactThumbnails";

	public int DEFAULT_IMAGE_RES_ID;	
	private Context context;
	private ImageCache cache;
	private String cacheSubdirectory = "ContactThumbnailsCache";
	private String defaultImageKey = "defaultimage";
	private boolean useDiskCache = true;

	public ContactThumbnails(){}
	
	public ContactThumbnails(Context contextIn){
		context = contextIn;
	}
	
	public ContactThumbnails(Context contextIn, int defaultImageResource){
		context = contextIn;
		initialize(defaultImageResource);
	}

	public ContactThumbnails(Context contextIn, int defaultImageResource, boolean enableDiskCache){
		context = contextIn;
		initialize(defaultImageResource, enableDiskCache);
	}
	
    public void initialize(int defaultImageResource){
        DEFAULT_IMAGE_RES_ID = defaultImageResource;
        initializeCache();
    }
    
	public void initialize(int defaultImageResource, boolean enableDiskCache){
		DEFAULT_IMAGE_RES_ID = defaultImageResource;
		useDiskCache = false;
		initializeCache();
	}
	
	public void setContext(Context contextIn){
		context = contextIn;
	}

    public Bitmap get(String lookupKey){
        return get(lookupKey, true);
    }
	public Bitmap get(String lookupKey, boolean useDefault){
		initializeCache();

		// If the key is invalid, use the default
		if (lookupKey == null || lookupKey.length() == 0){
            if (useDefault) {
                return getDefault();
            }else{
                return null;
            }
		}
		
		// Read from the cache
		Bitmap b = cache.read(context, lookupKey);
		if (b != null) {
			return b;
		}

		// Not in cache, read from the system
		Log.d(TAG, "Reading contact image from system");
		if (lookupKey != defaultImageKey){
			b = ContactUtilities.getContactPhotoThumbnail(lookupKey, context);
		}
		
		if (b == null){
            if (useDefault) {
                // No image found for this contact, use the default image
                b = getDefault();
            }else{
                return null;
            }
		}

		// Write to the cache
        if (b != null) {
            cache.write(context, lookupKey, b);
        }

		return b;
	}

	public Bitmap getDefault(){
		initializeCache();

		Bitmap b = cache.read(context, defaultImageKey);
		if (b != null) {
			Log.d(TAG, "Read default contact image from memory cache");
			return b;
		}
	
		// Not in cache, decode the resource
		Log.d(TAG, "Reading default contact image from resource");
		b = BitmapFactory.decodeResource(context.getResources(), DEFAULT_IMAGE_RES_ID);
		if (b != null){
			// Write to the cache
			cache.write(context, defaultImageKey, b);
		}
		return b;
	}

	public boolean remove(String lookupKey){
		initializeCache();
		return cache.remove(context, lookupKey);
	}
	
	public boolean clear(){
		initializeCache();
		return cache.clear(context);
	}

	private void initializeCache(){
		if (cache == null){
			cache = new ImageCache(cacheSubdirectory);
            cache.useDiskCache(useDiskCache);
		}
	}
}