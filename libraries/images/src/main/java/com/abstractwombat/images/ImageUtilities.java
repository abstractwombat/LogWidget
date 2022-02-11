package com.abstractwombat.images;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.DisplayMetrics;
import android.util.Log;

/**
 * Created by Mike on 5/20/13.
 */
public class ImageUtilities {

    public static float convertDpToPixel(float dp){
        DisplayMetrics metrics = Resources.getSystem().getDisplayMetrics();
        float px = dp * (metrics.densityDpi / 160f);
        return px;
    }

    public static float convertPixelTpDp(float px){
        DisplayMetrics metrics = Resources.getSystem().getDisplayMetrics();
        float dp = px / (metrics.densityDpi / 160f);
        return dp;
    }

    public static Bitmap scaleBitmap(Bitmap b, int maxDimension){
        float h = (float)b.getHeight();
        float w = (float)b.getWidth();
        float scaledH = h;
        float scaledW = w;
        float max = (float)maxDimension;
        //if (h > max || w > max){
            if (h > w) {
                scaledH = max;
                scaledW = scaledH * (w/h);
            }else{
                scaledW = max;
                scaledH = scaledW * (h/w);
            }
        //}
        int sW = (int)scaledW;
        int sH = (int)scaledH;
        if (sW <= 0 || sH <= 0){
            Log.d("scaleBitmap", "Attempted to scale to 0 - H/scaledH:" + h + "/" + sH + " W/scaledW: " + w + "/" + sW);
            sH = maxDimension;
            sW = maxDimension;
        }
        Log.d("scaleBitmap", "Scaling image from (" + h + "," + w + ") to (" + sH + "," + sW + ")");
        return Bitmap.createScaledBitmap(b, sW, sH, true);
    }

    public static Bitmap addQuickContactEmblem(Context context, Bitmap photo, int emblemSize) {
        Resources r = context.getResources();
        int w = photo.getWidth();
        int h = photo.getHeight();

        Bitmap icon = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(icon);

        // Draw the photo
        Paint photoPaint = new Paint();
        photoPaint.setDither(false);
        photoPaint.setFilterBitmap(true);
        Rect src = new Rect(0,0, w, h);
        canvas.drawBitmap(photo, src, src, photoPaint);

        // Create a path for the triangle
        Path path = new Path();
        path.moveTo(w, h);
        path.lineTo(w-emblemSize, h);
        path.lineTo(w, h-emblemSize);
        path.lineTo(w, h);

        // Create a Paint for the triangle
        Paint emblemPaint = new Paint();
        emblemPaint.setARGB(100, 255,255,255);
        emblemPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        // Draw the triangle
        canvas.drawPath(path, emblemPaint);

        // Create the line
        Path line = new Path();
        line.moveTo(w-emblemSize, h);
        line.lineTo(w, h-emblemSize);
        // Create a Paint for the line
        Paint linePaint = new Paint();
        linePaint.setARGB(200, 0,0,0);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(2);
        // Draw the line
        canvas.drawPath(line, linePaint);

        return icon;
    }

    public static Bitmap stackImagesHorizontal(Bitmap[] bmps) {
        // Calculate the height and widget of the new image
        int w = 0;
        int h = 0;
        for (Bitmap b : bmps) {
            int th = b.getHeight();
            if (th > h) {
                h = th;
            }
            w = w + b.getWidth();
            ;
        }

        Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);
        Paint p = new Paint();
        p.setDither(false);
        p.setFilterBitmap(true);

        int cx = 0;
        for (Bitmap b : bmps) {
            canvas.drawBitmap(b, cx, 0, p);
            cx = cx + b.getWidth();
        }

        return bmp;
    }

    /*
        Layout a set of circle images. All circles will be the same size as the first image. Only
        uses up to 4 of the images.
     */
    public static Bitmap layoutCircleImages(Bitmap[] bitmaps){
        int count = bitmaps.length;
        if (count == 0){
            return null;
        }
        if (count == 1) {
            return bitmaps[0];
        }
        float diameter = bitmaps[0].getWidth();
        float radius = diameter / 2.f;

        if (count == 2) {
            float centerToCenterOffset = 0.7071068f * diameter;
            float w = radius + centerToCenterOffset + radius;
            float h = w;
            Bitmap bmp = Bitmap.createBitmap((int)w, (int)h, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bmp);
            Paint p = new Paint();
            p.setDither(false);
            p.setFilterBitmap(true);
            canvas.drawBitmap(bitmaps[0], 0, 0, p);
            canvas.drawBitmap(sizeBitmap(bitmaps[1], (int)diameter, (int)diameter), centerToCenterOffset, centerToCenterOffset, p);
            return bmp;
        }
        if (count == 3) {
            float triangleHeight = radius * 1.7320508f; // r*tan60
            float w = diameter * 2;
            float h = w;
            Bitmap bmp = Bitmap.createBitmap((int)w, (int)h, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bmp);
            Paint p = new Paint();
            p.setDither(false);
            p.setFilterBitmap(true);
            canvas.drawBitmap(bitmaps[0], radius, 0, p);
            canvas.drawBitmap(sizeBitmap(bitmaps[1], (int)diameter, (int)diameter), 0, triangleHeight, p);
            canvas.drawBitmap(sizeBitmap(bitmaps[2], (int)diameter, (int)diameter), diameter, triangleHeight, p);
            return bmp;
        }
        if (count >= 4){
            float w = diameter * 2;
            float h = w;
            Bitmap bmp = Bitmap.createBitmap((int)w, (int)h, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bmp);
            Paint p = new Paint();
            p.setDither(false);
            p.setFilterBitmap(true);
            canvas.drawBitmap(bitmaps[0], 0, 0, p);
            canvas.drawBitmap(sizeBitmap(bitmaps[1], (int)diameter, (int)diameter), diameter, 0, p);
            canvas.drawBitmap(sizeBitmap(bitmaps[2], (int)diameter, (int)diameter), 0, diameter, p);
            canvas.drawBitmap(sizeBitmap(bitmaps[3], (int)diameter, (int)diameter), diameter, diameter, p);
            return bmp;
        }
        return null;
    }

    public static Bitmap sizeBitmap(Bitmap b, int width, int height){
        if (b.getWidth() != width || b.getHeight() != height) {
            return Bitmap.createScaledBitmap(b, width, height, true);
        }else{
            return b;
        }
    }

    public static Bitmap layoutRectangleImages(Bitmap[] bitmaps){
        int count = bitmaps.length;
        if (count == 1) {
            return bitmaps[0];
        }
        if (count == 2) {
            int w = bitmaps[0].getWidth() + bitmaps[1].getWidth();
            int h = bitmaps[0].getHeight() + bitmaps[1].getHeight();
            Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bmp);
            Paint p = new Paint();
            p.setDither(false);
            p.setFilterBitmap(true);
            canvas.drawBitmap(bitmaps[0], 0, 0, p);
            canvas.drawBitmap(bitmaps[1], bitmaps[0].getWidth(), bitmaps[0].getHeight(), p);
            return bmp;
        }
        if (count == 3) {
            int w = bitmaps[1].getWidth() + bitmaps[2].getWidth();
            int h = bitmaps[0].getHeight() + Math.max(bitmaps[1].getWidth(), bitmaps[2].getWidth());
            Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bmp);
            Paint p = new Paint();
            p.setDither(false);
            p.setFilterBitmap(true);
            canvas.drawBitmap(bitmaps[0], (w / 2) - (bitmaps[0].getWidth() / 2), 0, p);
            canvas.drawBitmap(bitmaps[1], 0, bitmaps[0].getHeight(), p);
            canvas.drawBitmap(bitmaps[2], bitmaps[1].getWidth(), bitmaps[0].getHeight(), p);
            return bmp;
        }
        if (count >= 4){
            int w = Math.max(bitmaps[0].getWidth() + bitmaps[1].getWidth(), bitmaps[2].getWidth() + bitmaps[3].getWidth());
            int h = Math.max(bitmaps[0].getHeight() + bitmaps[2].getHeight(), bitmaps[1].getHeight() + bitmaps[3].getHeight());
            Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bmp);
            Paint p = new Paint();
            p.setDither(false);
            p.setFilterBitmap(true);
            canvas.drawBitmap(bitmaps[0], 0, 0, p);
            canvas.drawBitmap(bitmaps[1], bitmaps[0].getWidth(), 0, p);
            canvas.drawBitmap(bitmaps[2], 0, bitmaps[0].getHeight(), p);
            canvas.drawBitmap(bitmaps[3], bitmaps[0].getWidth(), bitmaps[0].getHeight(), p);
            return bmp;
        }
        return null;
    }

    public static Bitmap addCircleEmblem(Context context, Bitmap image, Bitmap emblem){
        int imageWidth = image.getWidth();
        int imageHeight = image.getHeight();
        int emblemWidth = emblem.getWidth();
        int emblemHeight = emblem.getHeight();
        int emblemPadding = 0;

        // Create the bmp and canvas
        Bitmap bmp = Bitmap.createBitmap(imageWidth, imageHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);

        // Add the original image
        Paint p = new Paint();
        p.setDither(false);
        p.setFilterBitmap(true);
        canvas.drawBitmap(image, 0, 0, p);

        // Remove a circle from the image
//        Paint transparentPaint = new Paint();
//        transparentPaint.setColor(context.getResources().getColor(android.R.color.transparent));
//        transparentPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
//        canvas.drawCircle(imageWidth - (emblemWidth/2) - emblemPadding,
//                imageHeight - (emblemHeight/2) - emblemPadding,
//                (emblemWidth / 2) + emblemPadding, transparentPaint);

        // Draw the emblem
        canvas.drawBitmap(emblem, imageWidth - emblemWidth - emblemPadding,
                imageHeight - emblemHeight - emblemPadding, p);

        return bmp;
    }
    public static Bitmap circleBitmap(Context context, Bitmap bitmap){
        Resources r = context.getResources();
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();

        Bitmap output = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);
        final float densityMultiplier = context.getResources().getDisplayMetrics().density;

        final int color = 0xff424242;
        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, w, h);
        final RectF rectF = new RectF(rect);

        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(color);
        canvas.drawRoundRect(rectF, w/2, h/2, paint);

        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, 0,0, paint);

        return output;
    }

    public static Bitmap generateCircleBitmap(Context context, int circleColor, float diameterDP, String text){
        final int textColor = 0xffffffff;

        float diameterPixels = convertDpToPixel(diameterDP);
        float radiusPixels = diameterPixels/2;


        // Create the bitmap
        Bitmap output = Bitmap.createBitmap((int) diameterPixels, (int) diameterPixels,
                Bitmap.Config.ARGB_8888);

        // Create the canvas to draw on
        Canvas canvas = new Canvas(output);
        canvas.drawARGB(0, 0, 0, 0);

        // Draw the circle
        final Paint paintC = new Paint();
        paintC.setAntiAlias(true);
        paintC.setColor(circleColor);
        canvas.drawCircle(radiusPixels, radiusPixels, radiusPixels, paintC);

        // Draw the text
        if (text != null && text.length() > 0) {
            final Paint paintT = new Paint();
            paintT.setColor(textColor);
            paintT.setAntiAlias(true);
            float textSize = radiusPixels * 7 / 8;
            if (text.length() == 1) {
                textSize = radiusPixels * 3 / 2;
            }
            paintT.setTextSize(textSize);
            Typeface typeFace = Typeface.createFromAsset(context.getAssets(),"fonts/Roboto-Light.ttf");
            paintT.setTypeface(typeFace);
            final Rect textBounds = new Rect();
            paintT.getTextBounds(text, 0, text.length(), textBounds);
            canvas.drawText(text, radiusPixels - textBounds.exactCenterX(), radiusPixels - textBounds.exactCenterY(), paintT);
        }

        return output;
    }

    public static Bitmap addMessagingEmblem(Context context, Bitmap photo, boolean rightSide) {
        Resources r = context.getResources();
        int w = photo.getWidth();
        int h = photo.getHeight();

        Bitmap icon = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(icon);

        // Draw the photo
        Paint photoPaint = new Paint();
        photoPaint.setDither(false);
        photoPaint.setFilterBitmap(true);
        Rect src = new Rect(0,0, w, h);
        canvas.drawBitmap(photo, src, src, photoPaint);

        // Y Position of the center of the emblem
        int centerHeight = h/3;
        int emblemSize = h/3;

        // Create a path for the triangle
        Path path = new Path();
        if (rightSide){
            path.moveTo(w, centerHeight-(emblemSize/2));
            path.lineTo(w-(emblemSize/3), centerHeight);
            path.lineTo(w, centerHeight+(emblemSize/2));
            path.lineTo(w, centerHeight-(emblemSize/2));
        }else{
            path.moveTo(0, (centerHeight)-(emblemSize/2));
            path.lineTo(0+(emblemSize/3), centerHeight);
            path.lineTo(0, (centerHeight)+(emblemSize/2));
            path.lineTo(0, (centerHeight)-(emblemSize/2));
        }
        // Create a Paint for the triangle
        Paint emblemPaint = new Paint();
        emblemPaint.setARGB(0, 255,255,255);
        emblemPaint.setStyle(Paint.Style.FILL);
        emblemPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        // Draw the triangle
        canvas.drawPath(path, emblemPaint);

        return icon;
    }

    public static Bitmap addMessagingEmblem(Context context, Bitmap photo, int emblemSize, boolean rightSide) {
        Resources r = context.getResources();
        int w = photo.getWidth();
        int h = photo.getHeight();

        Bitmap icon = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(icon);

        // Draw the photo
        Paint photoPaint = new Paint();
        photoPaint.setDither(false);
        photoPaint.setFilterBitmap(true);
        Rect src = new Rect(0,0, w, h);
        canvas.drawBitmap(photo, src, src, photoPaint);

        // Y Position of the center of the emblem
        int centerHeight = (h/3) - (emblemSize/4);

        // Create a path for the triangle
        Path path = new Path();
        if (rightSide){
            path.moveTo(w, centerHeight-(emblemSize/2));
            path.lineTo(w-(emblemSize/2), centerHeight);
            path.lineTo(w, centerHeight+(emblemSize/2));
            path.lineTo(w, centerHeight-(emblemSize/2));
        }else{
            path.moveTo(0, (centerHeight)-(emblemSize/2));
            path.lineTo(0+(emblemSize/2), centerHeight);
            path.lineTo(0, (centerHeight)+(emblemSize/2));
            path.lineTo(0, (centerHeight)-(emblemSize/2));
        }
        // Create a Paint for the triangle
        Paint emblemPaint = new Paint();
        emblemPaint.setARGB(0, 255,255,255);
        emblemPaint.setStyle(Paint.Style.FILL);
        emblemPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        // Draw the triangle
        canvas.drawPath(path, emblemPaint);

        return icon;
    }
}
