package com.abstractwombat.images;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.util.Log;
import android.util.LruCache;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;

public class ImageCache {
	private static final String TAG = "ImageCache";

	private String cacheSubdirectory;
	private final Object mDiskCacheLock = new Object();
	private LruCache<String, Bitmap> memoryCache;
	private int fractionOfProcessMemory = 8;	// Use 1/8 of the available memory for this memory cache.
    private boolean useDiskCache;
	private Bitmap.CompressFormat diskCompressionFormat;
	private int diskCompressionQuality;

	ImageCache(){
		this.initialize("ImageCache");
	}
	public ImageCache(String cacheSubdirectory){
		this.initialize(cacheSubdirectory);
	}
	
	public void initialize(String cacheSubdirectory){
		diskCompressionFormat = Bitmap.CompressFormat.PNG;
		diskCompressionQuality = 100;
        useDiskCache = true;
		this.cacheSubdirectory = cacheSubdirectory;
		Log.d(TAG, "Initialize: cacheSubdirectory=" + cacheSubdirectory);
	}

    public void useDiskCache(boolean enableDiskCache){
        useDiskCache = enableDiskCache;

    }

	public Bitmap read(Context context, String key){
		if (key == null || key.length() == 0){
			Log.d(TAG, "read - Invalid key!");
			return null;
		}
		Log.d(TAG, "Request for image with key: " + key);
		
		// Check the memory cache
		initMemoryCache();
		Bitmap b = memoryCache.get(key);
		if (b != null) {
			Log.d(TAG, "Read contact image from memory cache");
			return b;
		}
		Log.d(TAG, "Cache size: " + memoryCache.size());

		// Try the disk cache
        if (useDiskCache){
    		b = readImageFromDisk(context, key);
        }
		if (b != null) {
			Log.d(TAG, "Read contact image from disk cache");
			// Write to the memory cache
			memoryCache.put(key, b);
			return b;
		}

		// Not in either cache
		return null;
	}
	public void write(Context context, String key, Bitmap image){
		if (image == null){
			Log.d(TAG, "write - Invalid image!");
			return;
		}
		initMemoryCache();
		Log.d(TAG, "Write image with key: " + key);
		// Write to the memory cache
		memoryCache.put(key, image);
		// Write to the disk cache
        if (useDiskCache){
    		writeImageToDisk(context, key, image);
        }
	}
	
	public boolean remove(Context context, String key){
		Log.d(TAG, "Removing image with key: " + key);
		synchronized (mDiskCacheLock) {
			initMemoryCache();
			memoryCache.remove(key);
            if (useDiskCache){
    			File f = getDiskCacheDir(context, key);
	    		return f.delete();
            }
            return true;
		}
	}
	
	public boolean clear(Context context){
		Log.d(TAG, "Clearing cache");
		synchronized (mDiskCacheLock) {
			initMemoryCache();
			memoryCache.evictAll();
			boolean deleted = true;
            if (useDiskCache){
                File dir = getDiskCacheDir(context, "");
                if (dir.isDirectory()) {
                    File[] children = dir.listFiles();
                    for (File f : children){
                        deleted &= f.delete();
                    }
                }
                deleted &= dir.delete();
            }
			return deleted;
		}
	}

    public boolean contains(Context context, String key){
		synchronized (mDiskCacheLock) {
			initMemoryCache();
			Bitmap b = memoryCache.get(key);
			if (b != null) return true;
			if (useDiskCache) {
				File f = getDiskCacheDir(context, key);
				if (f.exists()) {
					return true;
				}
			}
			return false;
		}
    }

	private Bitmap readImageFromDisk(Context context, String key){
		Bitmap b = null;
		synchronized (mDiskCacheLock) {
			// Read from the disk cache
			InputStream in = null;
			try {
				File f = getDiskCacheDir(context, key);
				in = new BufferedInputStream(new FileInputStream(f));			
				b = BitmapFactory.decodeStream(in);
			}catch(FileNotFoundException e){
				Log.d(TAG, "File not found - key " + key);
			}finally{
				if (in != null){
					try{
						in.close();
					}catch(IOException e){
					}
				}
			}
		}
		return b;
	}
	
	private void writeImageToDisk(Context context, String key, Bitmap b){
		synchronized (mDiskCacheLock) {
			// Write to the disk cache
			File f = getDiskCacheDir(context, key);
			OutputStream out = null;
			try {
				out = new BufferedOutputStream(new FileOutputStream(f));
				b.compress(diskCompressionFormat, diskCompressionQuality, out);
			}catch(FileNotFoundException e){
				Log.d(TAG, key + " : File not found");
			}finally{
				if (out != null){
					try{
						out.flush();
						out.close();
					}catch(IOException e){
						Log.d(TAG, "Failed to write file with key: " + key);
					}
				}
			}
		}
	}
	
	// Creates a unique subdirectory of the designated app cache directory. Tries to use external
	// but if not mounted, falls back on internal storage.
	private File getDiskCacheDir(Context context, String uniqueName) {
		// Check if media is mounted or storage is built-in, if so, try and use external cache dir
		// otherwise use internal cache dir
		final String cachePath;
		//if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState()) || !isExternalStorageRemovable() {
		//	cachePath = getExternalCacheDir(context).getPath();
		//}else{
			cachePath = context.getCacheDir().getPath();
		//}
		String encoded = uniqueName;
		if (uniqueName != null && !uniqueName.isEmpty()){
			try{
				encoded = URLEncoder.encode(uniqueName, "UTF-8");
			}catch (java.io.UnsupportedEncodingException e){
			}
		}
		
		String dirString = cachePath + File.separator + cacheSubdirectory;
		String fileString = dirString;
		if (!encoded.isEmpty()){
			fileString += File.separator + encoded;
		}
		
		File dir = new File(cachePath + File.separator + cacheSubdirectory);
		dir.mkdirs();
		
		return new File(fileString);
	}	

	private void initMemoryCache(){
		if (memoryCache != null) return;
		final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
		final int cacheSize = maxMemory / fractionOfProcessMemory;
		Log.d(TAG, "Init Cache to size: " + cacheSize + "KB");
		memoryCache = new LruCache<String, Bitmap>(cacheSize) {
			@TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
			protected int sizeOf(String key, Bitmap data) {
				int s;
				if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB_MR1) {
		            s = data.getRowBytes() * data.getHeight()/1024;
		        } else {
		            s = data.getByteCount()/1024;
		        }
				return s;
			}
		};
	}
}