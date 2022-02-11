package com.abstractwombat.loglibrary;


import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.util.Log;

public abstract class ALogSourcePreferenceFragment extends PreferenceFragment {
    private static final String TAG = "ALogSourcePreferenceFragment";
    private final String STATE_FILE = "State";

    protected Context context;
    protected String mLogSourceId;

    protected abstract int getPreferences();
    protected abstract void storeToFactory();
    protected abstract void loadFromFactory();

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.context = activity;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(getPreferences());
        Log.d(TAG, "OnCreate");
        if (savedInstanceState != null) {
            this.mLogSourceId = savedInstanceState.getString("sourceid");
        }else{
            // Get the source id
            Bundle args = getArguments();
            String sourceId = args.getString("sourceid");
            if (sourceId != null && sourceId.length() > 0){
                Log.d(TAG, "Got source id");
                mLogSourceId = sourceId;
            }else{
                Log.e(TAG, "Source id is required in the arguments!");
                return;
            }
            loadFromFactory();
        }
    }
	
	@Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.d(TAG, "onSaveInstanceState");
        outState.putString("sourceid", mLogSourceId);
    }
	
    @Override
    public void onStart() {
        Log.d(TAG, "onStart");
        super.onStart();
    }

    @Override
    public void onStop() {
        Log.d(TAG, "onStop");
        super.onStop();
    }

    @Override
    public void onResume() {
        Log.d(TAG, "onResume");
        super.onResume();
        loadFromFactory();
    }

    @Override
    public void onPause() {
        Log.d(TAG, "onPause");
        super.onPause();
        this.save();
    }

    public void save() {
        storeToFactory();
    }

}
