package com.abstractwombat.contacts;


import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ContactThumbnailLoader {

    private final ExecutorService pool;
    private Map<ImageView, String> imageViews = Collections
            .synchronizedMap(new WeakHashMap<ImageView, String>());
    private Set<String> cachedKeys;
    private Bitmap placeholder;
	private ContactThumbnails thumbnails;
    private Context context;

    public ContactThumbnailLoader(Context context, int defaultRes) {
        this.context = context;
		thumbnails = new ContactThumbnails(context, defaultRes, false);
        placeholder = thumbnails.getDefault();
        pool = Executors.newFixedThreadPool(5);
        cachedKeys = new HashSet<String>();
    }

    public void load(final String key, final ImageView imageView) {
        imageViews.put(imageView, key);
        Bitmap bitmap = getBitmapFromCache(key);

        // check in UI thread, so no concurrency issues
        if (bitmap != null) {
            Log.d(null, "Item loaded from cache: " + key);
			imageView.setImageBitmap(bitmap);
        } else {
            imageView.setImageBitmap(placeholder);
			queueJob(key, imageView);
        }
    }

    private Bitmap getBitmapFromCache(String key) {
        if (cachedKeys.contains(key)) {
            return thumbnails.get(key);
        }

        return null;
    }
	
	private static void ImageViewAnimatedChange(Context c, final ImageView v, final Bitmap new_image) {
        final Animation anim_out = AnimationUtils.loadAnimation(c, android.R.anim.fade_out);
        final Animation anim_in  = AnimationUtils.loadAnimation(c, android.R.anim.fade_in);
        if (anim_out != null) {
            anim_out.setAnimationListener(new Animation.AnimationListener()
            {
                @Override public void onAnimationStart(Animation animation) {}
                @Override public void onAnimationRepeat(Animation animation) {}
                @Override public void onAnimationEnd(Animation animation)
                {
                    v.setImageBitmap(new_image);
                    if (anim_in != null) {
                        anim_in.setAnimationListener(new Animation.AnimationListener() {
                            @Override public void onAnimationStart(Animation animation) {}
                            @Override public void onAnimationRepeat(Animation animation) {}
                            @Override public void onAnimationEnd(Animation animation) {}
                        });
                        v.startAnimation(anim_in);
                    }
                }
            });
            v.startAnimation(anim_out);
        }
    }
	
    private void queueJob(final String key, final ImageView imageView) {
        /* Create handler in UI thread. */
        final Handler handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                String tag = imageViews.get(imageView);
                if (tag != null && tag.equals(key)) {
                    if (msg.obj != null) {
						//if (ContactUtilities.contactHasPhoto(key, context)){
						//	ImageViewAnimatedChange(context, imageView, (Bitmap) msg.obj);
						//}
                        imageView.setImageBitmap((Bitmap) msg.obj);
                    } else {
                        //imageView.setImageBitmap(placeholder);
                        Log.d(null, "fail " + key);
                    }
                }
            }
        };

        pool.submit(new Runnable() {
            @Override
            public void run() {
                final Bitmap bmp = thumbnails.get(key);
                cachedKeys.add(key);
                Message message = Message.obtain();
                if (message != null) {
                    message.obj = bmp;
                    Log.d(null, "Item downloaded: " + key);
                    handler.sendMessage(message);
                }
            }
        });
    }

}

