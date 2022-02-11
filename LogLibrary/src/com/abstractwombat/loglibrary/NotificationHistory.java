package com.abstractwombat.loglibrary;

import android.app.Notification;
import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Parcelable;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

import com.abstractwombat.images.ImageUtilities;
import com.abstractwombat.library.SQLDatabase;
import com.abstractwombat.library.SQLTableColumn;

/**
 * Provides database storage of StatusBarNotification data.
 *  - Includes a package filter
 *  - sortable indexed access of items
 *  - [future] collapsing notifications
 *
 * Created by Mike on 5/6/2015.
 */
public class NotificationHistory {
    private final String TAG = "NotificationHistory";

    public interface Specialization {
        public boolean confirmAdd(NotificationData toAdd);
        public boolean areCollapsible(NotificationData a, NotificationData b);
    }

    /**
     * Configuration
     */
    private int mMaxRows;
    private ArrayList<String> mPackageFilter;
    private Specialization mSpecialization;
    private String mSortedColumn;
    private boolean mSortedDescending;
    private Integer mImageMaxDimensionDp;

    private String mTableName;
    private Context mContext;
    private SQLDatabase mDatabase;

    /**
     *  Data describing the notification
     */
    public class NotificationData{
        public long time;
        public String title;
        public String titleBig;
        public String extraText;
        public Bitmap extraPicture;
        public String subText;
        public String infoText;
        public String summaryText;
        public String bigText;
        public String[] textLines;
        public String[] people;
    }

    /**
     *	Database Table Columns
     */
    public static final String COLUMN_WHEN                  = "time";
    public static final String COLUMN_EXTRA_TITLE           = "title";
    public static final String COLUMN_EXTRA_TITLE_BIG       = "titleBig";
    public static final String COLUMN_EXTRA_TEXT            = "extraText";
    public static final String COLUMN_EXTRA_PICTURE         = "extraPicture";
    public static final String COLUMN_EXTRA_SUB_TEXT        = "subText";
    public static final String COLUMN_EXTRA_INFO_TEXT       = "infoText";
    public static final String COLUMN_EXTRA_SUMMARY_TEXT    = "summaryText";
    public static final String COLUMN_EXTRA_BIG_TEXT        = "bigText";
    public static final String COLUMN_EXTRA_TEXT_LINES      = "textLines";
    public static final String COLUMN_EXTRA_PEOPLE          = "people";

    public NotificationHistory(Context context, String tableName, int maxRows){
        mContext = context;
        mTableName = tableName;
        mDatabase = new SQLDatabase(mContext);
        mDatabase.createTable(mTableName, getColumns());
        mSortedColumn = COLUMN_WHEN;
        mSortedDescending = true;
        mMaxRows = maxRows;
    }

    public void setSorting(String columnToSort, boolean descending){
        mSortedColumn = columnToSort;
        mSortedDescending = descending;
    }
    public void addPackageFilter(String packageName){
        if (mPackageFilter == null){
            mPackageFilter = new ArrayList<>();
        }
        mPackageFilter.add(packageName);
    }
    public void setMaxImageDimension(int maxImageDimensionDp){
        mImageMaxDimensionDp = maxImageDimensionDp;
    }

    public void addCollapsible(Specialization c){
        mSpecialization = c;
    }

    public boolean addNotification(StatusBarNotification sbNotification) {
        // Apply the package filter
        String packageName = sbNotification.getPackageName();
        if (mPackageFilter != null && mPackageFilter.size() > 0) {
            if (!mPackageFilter.contains(packageName)) {
                return false;
            }
        }
        Log.d(TAG, "Adding notification from " + packageName);

        // Get all the data out of the notification
        Notification notification = sbNotification.getNotification();
        if (notification.when == 0){
            Log.d(TAG, "Notification when==0");
            return false;
        }
        Bundle extras = notification.extras;

        // Log the extras
        Log.d(TAG, "Notification Extras:");
        for (String key: extras.keySet()){
            if (key == null) continue;
            if (extras.get(key) == null) continue;
            Log.d(TAG, "  " + key + " = " + extras.get(key).toString());
        }

        if (notification.contentIntent != null) {
            Log.d(TAG, "Notification PendingIntent:" + notification.contentIntent.toString());
        }

        // Get all the data out of the notification
        CharSequence title = extras.getString(Notification.EXTRA_TITLE);
        CharSequence titleBig = extras.getCharSequence(Notification.EXTRA_TITLE_BIG);
        CharSequence extraText = extras.getCharSequence(Notification.EXTRA_TEXT);
        Parcelable extraPictureParcel = extras.getParcelable(Notification.EXTRA_PICTURE);
        CharSequence subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT);
        CharSequence infoText = extras.getCharSequence(Notification.EXTRA_INFO_TEXT);
        CharSequence summaryText = extras.getCharSequence(Notification.EXTRA_SUMMARY_TEXT);
        CharSequence bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT);
        CharSequence[] lines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES);
        String[] people = extras.getStringArray(Notification.EXTRA_PEOPLE);

        // Create the NotificationData
        NotificationData data = new NotificationData();
        data.time = notification.when;
        data.title = (title != null) ? title.toString() : null;
        data.titleBig = (titleBig != null) ? titleBig.toString() : null;
        data.extraText = (extraText != null) ? extraText.toString() : null;
        data.subText = (subText != null) ? subText.toString() : null;
        data.infoText = (infoText != null) ? infoText.toString() : null;
        data.summaryText = (summaryText != null) ? summaryText.toString() : null;
        data.bigText = (bigText != null) ? bigText.toString() : null;
        if (lines != null && lines.length > 0) {
            int i = 0;
            data.textLines = new String[lines.length];
            for (CharSequence c : lines) data.textLines[i++] = c.toString();
        }else{
            data.people = null;
        }
        if (people != null && people.length > 0) {
            int i = 0;
            data.people = new String[people.length];
            for (CharSequence c : people) data.people[i++] = c.toString();
        }else{
            data.people = null;
        }
        if (extraPictureParcel != null){
            if (mImageMaxDimensionDp != null){
                float maxDim = ImageUtilities.convertDpToPixel((float)mImageMaxDimensionDp);
                data.extraPicture = ImageUtilities.scaleBitmap((Bitmap) extraPictureParcel, (int)maxDim);
            }else {
                data.extraPicture = (Bitmap) extraPictureParcel;
            }
        }else{
            data.extraPicture = null;
        }

        if (mSpecialization == null || mSpecialization.confirmAdd(data)){
            return addNotification(data);
        }
        return false;
    }

    private boolean addNotification(NotificationData data){
        // Create the ContentValues
        ContentValues c = new ContentValues();
        c.put(COLUMN_WHEN              , data.time);
        c.put(COLUMN_EXTRA_TITLE       , data.title);
        c.put(COLUMN_EXTRA_TITLE_BIG   , data.titleBig);
        c.put(COLUMN_EXTRA_TEXT        , data.extraText);
        c.put(COLUMN_EXTRA_SUB_TEXT    , data.subText);
        c.put(COLUMN_EXTRA_INFO_TEXT   , data.infoText);
        c.put(COLUMN_EXTRA_SUMMARY_TEXT, data.summaryText);
        c.put(COLUMN_EXTRA_BIG_TEXT    , data.bigText);

        String textLinesCombined = "";
        if (data.textLines != null && data.textLines.length > 0) {
            for (String s : data.textLines) textLinesCombined += s + "|";
            textLinesCombined = textLinesCombined.trim();
            if (textLinesCombined.endsWith("|")) {
                textLinesCombined = textLinesCombined.substring(0, textLinesCombined.length() - 1);
            }
        }
        c.put(COLUMN_EXTRA_TEXT_LINES  , textLinesCombined);

        String peopleCombined = "";
        if (data.people != null && data.people.length > 0) {
            for (String s : data.people) {
                peopleCombined += s + "|";
                Log.d(TAG, "Notification person: " + s);
            }
            peopleCombined = peopleCombined.substring(0, peopleCombined.length() - 1);
        }
        c.put(COLUMN_EXTRA_PEOPLE      , peopleCombined);
        Log.d(TAG, "Notification people: " + peopleCombined);

        byte[] extraPictueBytes = (byte[])null;
        if (data.extraPicture != null) {
            ByteArrayOutputStream image = new ByteArrayOutputStream();
            data.extraPicture.compress(Bitmap.CompressFormat.PNG, 100, image);
            extraPictueBytes = image.toByteArray();
            Log.d(TAG, "Adding notification with " + data.extraPicture.getWidth() + "x" + data.extraPicture.getHeight() + "image");
        }
        c.put(COLUMN_EXTRA_PICTURE, extraPictueBytes);

        // Check if this must be collapsed
        long count = rowCount();
        if (count>0 && mSpecialization != null){
            NotificationData lastData = this.getAt(0);

            for (int i=0; i<count; i++){
                NotificationData testData = this.getAt(i);
                Log.d(TAG, Integer.toString(i) + " @ " + testData.time + " [" + testData.extraText + "]");
            }

            if (mSpecialization.areCollapsible(lastData, data)){
                // Remove the last one, to be replaced by teh current one
                mDatabase.deleteRows(mTableName, COLUMN_WHEN, String.valueOf(lastData.time));
            }
        }

        // Insert into the database
        boolean inserted = mDatabase.insert(mTableName, c);
        if (!inserted) return false;

        // Check if the DB has grown beyond maxRows
        long size = rowCount();
        if (mMaxRows > 0 && size > mMaxRows){
            // Delete the last row (according to the sorting)
            int removed = 0;
            ContentValues cv = mDatabase.getAt(mTableName, (int)(size-1), mSortedColumn, mSortedDescending);
            if (cv != null && cv.size() > 0) {
                removed = mDatabase.deleteRows(mTableName, COLUMN_WHEN, cv.getAsString(COLUMN_WHEN));
            }
            Log.d(TAG, "Removed " + removed + " rows from NotificationHistory to maintain mMaxRows");
        }

        return inserted;
    }

    public NotificationData getAt(int i){
        ContentValues cv = mDatabase.getAt(mTableName, i, mSortedColumn, mSortedDescending);
        if (cv == null) return null;
        NotificationData data = new NotificationData();
        byte[] extraPicture = null;

        data.time =          cv.getAsLong(COLUMN_WHEN);
        data.title =         cv.getAsString(COLUMN_EXTRA_TITLE);
        data.titleBig =      cv.getAsString(COLUMN_EXTRA_TITLE_BIG);
        data.extraText =     cv.getAsString(COLUMN_EXTRA_TEXT        );
        extraPicture =       cv.getAsByteArray(COLUMN_EXTRA_PICTURE);
        data.subText =       cv.getAsString(COLUMN_EXTRA_SUB_TEXT    );
        data.infoText =      cv.getAsString(COLUMN_EXTRA_INFO_TEXT);
        data.summaryText =   cv.getAsString(COLUMN_EXTRA_SUMMARY_TEXT);
        data.bigText =       cv.getAsString(COLUMN_EXTRA_BIG_TEXT    );

        String textLinesCombined = cv.getAsString(COLUMN_EXTRA_TEXT_LINES);
        data.textLines = textLinesCombined.split("\\|");

        String peopleCombined = cv.getAsString(COLUMN_EXTRA_PEOPLE);
        data.people = peopleCombined.split("\\|");

        if (extraPicture != null) {
            ByteArrayInputStream image = new ByteArrayInputStream(extraPicture);
            data.extraPicture = BitmapFactory.decodeStream(image);
        }

        return data;
    }

    public long rowCount(){
        return mDatabase.rowCount(mTableName);
    }

    public boolean contains(long time){
        ContentValues[] cv = mDatabase.get(mTableName, COLUMN_WHEN, String.valueOf(time));
        if (cv != null && cv.length > 0){
            return true;
        }else{
            return false;
        }
    }

    private SQLTableColumn[] getColumns(){
        List<SQLTableColumn> cols = new ArrayList<SQLTableColumn>();

        cols.add(new SQLTableColumn(COLUMN_WHEN              , "integer"));
        cols.add(new SQLTableColumn(COLUMN_EXTRA_TITLE       , "text"));
        cols.add(new SQLTableColumn(COLUMN_EXTRA_TITLE_BIG   , "text"));
        cols.add(new SQLTableColumn(COLUMN_EXTRA_TEXT        , "text"));
        cols.add(new SQLTableColumn(COLUMN_EXTRA_PICTURE     , "blob"));
        cols.add(new SQLTableColumn(COLUMN_EXTRA_SUB_TEXT    , "text"));
        cols.add(new SQLTableColumn(COLUMN_EXTRA_INFO_TEXT   , "text"));
        cols.add(new SQLTableColumn(COLUMN_EXTRA_SUMMARY_TEXT, "text"));
        cols.add(new SQLTableColumn(COLUMN_EXTRA_BIG_TEXT    , "text"));
        cols.add(new SQLTableColumn(COLUMN_EXTRA_TEXT_LINES  , "text"));
        cols.add(new SQLTableColumn(COLUMN_EXTRA_PEOPLE      , "text"));

        SQLTableColumn[] colArray = new SQLTableColumn[cols.size()];
        return cols.toArray(colArray);
    }

}
