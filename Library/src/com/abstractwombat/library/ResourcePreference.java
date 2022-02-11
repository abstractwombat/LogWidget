package com.abstractwombat.library;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.preference.Preference;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.SimpleAdapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by Mike on 3/7/2016.
 */
public class ResourcePreference extends Preference {
    private final String TAG = "ResourcePreference";
    private List<Integer> mDrawableStrings;
    private int mSelection;
    private GridView mGridView;
    private AlertDialog mAlertDialog;

    public ResourcePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    public ResourcePreference(Context context) {
        super(context);
    }

    public int getValue(){
        return mDrawableStrings.get(mSelection);
    }

    private void init(AttributeSet attrs) {
        setPersistent(true);

        setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                showDialog();
                return true;
            }
        });

        TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.resource_preference);
        try{
            int arrayResId = a.getResourceId(R.styleable.resource_preference_drawables, -1);
            TypedArray drawableArray = null;
            if (arrayResId != -1){
                drawableArray = getContext().getResources().obtainTypedArray(arrayResId);
            }
            if (drawableArray != null) {
                mDrawableStrings = new ArrayList<>();
                for (int i=0; i<drawableArray.length(); i++){
                    int id = drawableArray.getResourceId(i, -1);
                    if (id != -1){
                        mDrawableStrings.add(id);
                    }
                }
            }
            if (drawableArray != null) drawableArray.recycle();
        }finally{
            a.recycle();
        }

        Log.d(TAG, "Initialized - " + mDrawableStrings.size() + " drawabled found");
    }


    private void showDialog() {
        Log.d(TAG, "ShowDialog");
        Context context = getContext();
        mGridView = new GridView(context);

        ArrayList<HashMap<String,Integer>> map = new ArrayList<>();
        for (Integer s : mDrawableStrings){
            HashMap<String,Integer> m = new HashMap<>();
            m.put("image", s);
            map.add(m);
        }
        String[] from={"image"};
        int[] to = {R.id.imageView};

        mGridView.setBackgroundColor(Color.argb(255, 225, 225, 225));
        SimpleAdapter adapter = new SimpleAdapter(context, map, R.layout.resourcepreference_layout, from, to);
        mGridView.setAdapter(adapter);
        mGridView.setNumColumns(2);
        mGridView.setSelection(mSelection);
        mGridView.setOnItemClickListener(mGridItemClicked);

        // Set grid view to alertDialog
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setView(mGridView);
        mAlertDialog = builder.create();
        mAlertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                View child = mGridView.getChildAt(mSelection);
                ImageView selectedView = (ImageView)child.findViewById(R.id.selected);
                selectedView.setVisibility(View.VISIBLE);
            }
        });
        mAlertDialog.show();
    }

    private AdapterView.OnItemClickListener mGridItemClicked = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            mSelection = position;
            Log.d(TAG, "Selected: " + mDrawableStrings.get(mSelection) + " at index " + mSelection);
            // Select the view, unselect all others
            for (int i=0; i<mGridView.getChildCount(); i++){
                View child = mGridView.getChildAt(i);
                ImageView selectedView = (ImageView)child.findViewById(R.id.selected);
                if (child.equals(view)){
                    selectedView.setVisibility(View.VISIBLE);
                }else {
                    selectedView.setVisibility(View.GONE);
                }
            }
            // Close the dialog
            mAlertDialog.hide();
        }
    };

    @Override
    public void setDefaultValue(Object defaultValue) {
        super.setDefaultValue(defaultValue);
        mSelection = mDrawableStrings.indexOf((Integer)defaultValue);
        if (mSelection < 0) {
            mSelection = 0;
        }
        Log.d(TAG, "setDefaultValue: " + mDrawableStrings.get(mSelection) + " at index " + mSelection);
    }

    @Override
    protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
        if(restorePersistedValue) {
            //Integer s = Integer.getInteger(getPersistedInt(-1), -1);
            //mSelection = mDrawableStrings.indexOf(s);
        } else {
            mSelection = mDrawableStrings.indexOf((Integer)defaultValue);
            if (mSelection < 0) {
                mSelection = 0;
            }
            persistInt(mDrawableStrings.get(mSelection));
        }
        Log.d(TAG, "onSetInitialValue: " + mDrawableStrings.get(mSelection) + " at index " + mSelection);
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return a.getInt(index, -1);
    }

}
