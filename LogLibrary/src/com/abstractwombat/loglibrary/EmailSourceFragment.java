package com.abstractwombat.loglibrary;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

public class EmailSourceFragment extends PreferenceFragment implements Preference.OnPreferenceClickListener {
	private static final String TAG = "EmailSourceFragment";

	private LogSourceReceiver receiver;
	private Context context;
	LogSourceConfig initialConfig;
	private boolean deleted;
	
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		this.context = activity;
	}
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.emailsource_preferences);
		setHasOptionsMenu(true);
	}

	@Override
	public void onResume() {
		Log.d(TAG, "onResume");
		super.onResume();
		initialize();
	}

	private void initialize(){
		initialConfig = new LogSourceConfig();
		// Get the config data
		Bundle args = getArguments();
		String initialConfigData = args.getString("LogSourceConfig");
		if (initialConfigData != null){
			initialConfig.unserialize(initialConfigData);
		}else{
			Log.e(TAG, "initialConfigData is required in the arguments!");
			return;
		}
		Log.d(TAG, "Init Source ID: " + initialConfig.sourceID);
		loadFromFactory();

        Preference testPref = findPreference("emailsource_test");
        testPref.setOnPreferenceClickListener(this);
		this.deleted = false;
	}
	
	@Override
	public void onPause() {
		Log.d(TAG, "onPause");
		super.onPause();
		if (!this.deleted){
			storeToFactory();
		}
	}
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		Log.d(TAG, "onCreateOptionsMenu");
		inflater.inflate(R.menu.email_source_action_menu, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Log.d(TAG, "onOptionsItemSelected");

		if (item.getItemId() == R.id.delete_action){
			LogSourceFactory.deleteSource(this.context, this.initialConfig.sourceID);
			this.deleted = true;
			getFragmentManager().popBackStack();
			return true;
		}
		
		return false;
	}
	
	private void storeToFactory(){
		// Delete the source
		LogSourceFactory.deleteSource(this.context, this.initialConfig.sourceID);
		
		// Get all the preferences
		EditTextPreference editCount = (EditTextPreference)findPreference("emailsource_count");
        EditTextPreference editServer = (EditTextPreference)findPreference("emailsource_server");
        EditTextPreference editFolder = (EditTextPreference)findPreference("emailsource_folder");
        EditTextPreference editPort = (EditTextPreference)findPreference("emailsource_port");
        EditTextPreference editUser = (EditTextPreference)findPreference("emailsource_username");
        EditTextPreference editPass = (EditTextPreference)findPreference("emailsource_password");

		// Create the new source
		EmailSourceConfig config = new EmailSourceConfig(this.initialConfig);
		config.count = Integer.parseInt(editCount.getText());
        config.server = editServer.getText();
        config.folder = editFolder.getText();
        config.port = Integer.parseInt(editPort.getText());
        config.username = editUser.getText();
        config.password = editPass.getText();
		LogSourceFactory.newSource(this.context, EmailSource.class, config);

		Log.d(TAG, "Stored: " + config.server + "," + config.username + "," + config.password);
	}
	private void loadFromFactory(){
		// Get all the preferences
        EditTextPreference editCount = (EditTextPreference)findPreference("emailsource_count");
        EditTextPreference editServer = (EditTextPreference)findPreference("emailsource_server");
        EditTextPreference editFolder = (EditTextPreference)findPreference("emailsource_folder");
        EditTextPreference editPort = (EditTextPreference)findPreference("emailsource_port");
        EditTextPreference editUser = (EditTextPreference)findPreference("emailsource_username");
        EditTextPreference editPass = (EditTextPreference)findPreference("emailsource_password");

		ALogSource source = LogSourceFactory.get(this.context, this.initialConfig.sourceID);
		if (source == null){
			// Source with this ID doesn't exist, set the default values
			Resources res = getResources();
			editCount.setText(Integer.toString(res.getInteger(R.integer.email_source_default_count)));
            editServer.setText(res.getString(R.string.email_source_default_server));
            editFolder.setText(res.getString(R.string.email_source_default_folder));
            editPort.setText(Integer.toString(res.getInteger(R.integer.email_source_default_port)));
            editUser.setText(res.getString(R.string.email_source_default_username));
            editPass.setText("");
			Log.d(TAG, "Loaded defaults");
		}else{
			// Set from the config
			EmailSourceConfig config = (EmailSourceConfig)source.config();
			editCount.setText(Integer.toString(config.count));
            editServer.setText(config.server);
            editFolder.setText(config.folder);
            editPort.setText(Integer.toString(config.port));
            editUser.setText(config.username);
            editPass.setText(config.password);
            Log.d(TAG, "Loaded: " + config.server + "," + config.username + "," + config.password);
		}
	}

    @Override
    public boolean onPreferenceClick(Preference preference) {
        Preference testPref = findPreference("emailsource_test");
        if (preference.equals(testPref)){
            this.storeToFactory();
            ALogSource source = LogSourceFactory.get(this.context, this.initialConfig.sourceID);
            EmailSourceConfig config = (EmailSourceConfig)source.config();

            // Check input for error
            String error = "";
            if (config.server.isEmpty()) error = "Server cannot be empty!";
            if (config.username.isEmpty()) {
                if (!error.isEmpty()) error += "\n";
                error += "Username cannot be empty!";
            }
            if (config.password.isEmpty()) {
                if (!error.isEmpty()) error += "\n";
                error += "Password cannot be empty!";
            }
            if (!error.isEmpty()){
                Toast.makeText(this.context, error, Toast.LENGTH_LONG).show();
                return true;
            }

            // Check login
            checkEmailTaskComplete checked = new checkEmailTaskComplete(){
                public void onTaskComplete(int result){
                    if (result == 0){
                        Toast.makeText(context, "Successfully Connected", Toast.LENGTH_LONG).show();
                    }else if (result == 1){
                        Toast.makeText(context, "Failed to connect to the IMAP server!", Toast.LENGTH_LONG).show();
                    }else if (result == 2){
                        Toast.makeText(context, "Invalid Username and/or Password", Toast.LENGTH_LONG).show();
                    }else if (result == 3){
                        Toast.makeText(context, "Failed to connect to the IMAP server! (already connected?)", Toast.LENGTH_LONG).show();
                    }else if (result == 4){
                        Toast.makeText(context, "Failed to connect to the IMAP server! (s)", Toast.LENGTH_LONG).show();
                    }
                }
            };
            new checkEmailTask(context, checked).execute(config.server, Integer.toString(config.port), config.username, config.password);

            return true;
        }
        return false;
    }

    private interface checkEmailTaskComplete {
        public void onTaskComplete(int result);
    }
    private class checkEmailTask extends AsyncTask<String, Void, Integer> {
        private Context context;
        private ProgressDialog progDailog;
        private checkEmailTaskComplete callback;
        private IMAP imap;

        checkEmailTask(Context context, checkEmailTaskComplete callback){
            super();
            this.context = context;
            this.callback = callback;
        }

        protected Integer doInBackground(String... imapData) {
            int count = imapData.length;
            if (count != 4) return -1;
            String s = imapData[0];
            int port = Integer.parseInt(imapData[1]);
            String u = imapData[2];
            String p = imapData[3];
            imap = new IMAP();
            int error = imap.connectWithLogin(s, port, u, p);
            imap.disconnect();
            return error;
        }
        protected void onPreExecute() {
            progDailog = ProgressDialog.show(this.context, "", "Checking IMAP Login");
            progDailog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        }
        protected void onPostExecute(Integer result) {
            this.callback.onTaskComplete(result);
            progDailog.dismiss();
        }
    }

}