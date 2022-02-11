package com.abstractwombat.loglibrary;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.util.Log;

public class ViewEmailActivity extends Activity implements ViewEmailFragment.ViewEmailCompleteListener{
	private static final String TAG = "ViewEmailActivity";

    @Override
    public void onCreate(Bundle savedInstanceState){
		Log.d(TAG, "onCreate");
		super.onCreate(savedInstanceState);
		
		// Get the arguments out of the intent
		Intent intent = getIntent();
        String server = intent.getStringExtra("Server");
        int port = intent.getIntExtra("Port", 0);
        String username = intent.getStringExtra("Username");
        String password = intent.getStringExtra("Password");
        String messageID = intent.getStringExtra("MessageID");
        String folder = intent.getStringExtra("Folder");

        // Create the Fragment
		ViewEmailFragment f = ViewEmailFragment.newInstance(messageID, folder, server, port, username, password);
        f.show(getFragmentManager(), "dialog");
    }

	@Override
	public void onPause(){
		super.onPause();
		Log.d(TAG, "onPause");
		finish();
	}
	
	/**
	 *	ViewEmailCompleteListener
	 */
	public void onViewEmailDismissed(){
		Log.d(TAG, "onViewEmailDismissed");
		finish();
	}
	public void onViewEmailCancelled(){
		Log.d(TAG, "onViewEmailCancelled");
		finish();
	}
}
