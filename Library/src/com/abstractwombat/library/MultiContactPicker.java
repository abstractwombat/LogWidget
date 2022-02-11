package com.abstractwombat.library;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView.MultiChoiceModeListener;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.abstractwombat.contacts.ContactThumbnailLoader;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;


public class MultiContactPicker extends Fragment implements MultiChoiceModeListener, AdapterView.OnItemClickListener
{
	private static final String TAG = "MultiContactPicker";
	private Context context;
	private ContactAdapter adapter;
	private String title;
    private int actionBarIcon;
	private String filter;
	private ListView listView;
    public ArrayList<String> selectedKeys;
    private MultiContactPickerReceiver listener;

    public interface MultiContactPickerReceiver{
        public void picked(String[] lookupKeys);
    }

	/**
     * Create a new instance of MultiContactPicker
     */
    public static MultiContactPicker newInstance(String title, int abIcon, String filter, String[] selectedKeys, MultiContactPickerReceiver receiver) {
        MultiContactPicker f = new MultiContactPicker();
        Bundle args = new Bundle();
        args.putString("title", title);
        args.putInt("icon", abIcon);
        args.putString("filter", filter);
        args.putStringArray("selected", selectedKeys);
        f.setArguments(args);
        f.addReceiver(receiver);
        return f;
    }

    public void addReceiver(MultiContactPickerReceiver receiver){
        this.listener = receiver;
    }

	@Override
    public void onAttach(Activity activity) {
		super.onAttach(activity);
		this.context = activity;

        // If a receiver hasn't been set, then the calling activity better be it
        if (this.listener == null){
            if (!(activity instanceof MultiContactPickerReceiver)){
                throw new ClassCastException(activity.toString() + " must implement MultiContactPicker.MultiContactPickerReceiver");
            }else{
                this.listener = (MultiContactPickerReceiver)activity;
            }
        }
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		Activity activity = getActivity();
		
		// Get the arguments
        Bundle args = getArguments();
        if (args != null){
            this.title = args.getString("title");
            this.actionBarIcon = args.getInt("icon");
            this.filter = args.getString("filter");
            String[] selected = args.getStringArray("selected");
            if (selected == null){
                this.selectedKeys = new ArrayList<String>();
            }else{
                this.selectedKeys = new ArrayList<String>(Arrays.asList(selected));
            }
        }

		View view = inflater.inflate(R.layout.multicontactpicker, container, false);
		return view;
	}
	
	@Override
	public void onViewCreated(View view, Bundle savedInstanceState){
        // Get the contact cursor
        String[] projection = new String[] {
                ContactsContract.Contacts._ID,
                ContactsContract.Contacts.DISPLAY_NAME,
                ContactsContract.Contacts.LOOKUP_KEY
        };
		String selection = ContactsContract.Contacts.IN_VISIBLE_GROUP + " = '1'";
		if (filter != null && filter != ""){
			selection += " AND " + this.filter;
		}
        String sortOrder = ContactsContract.Contacts.DISPLAY_NAME + " COLLATE LOCALIZED ASC";
        Cursor cursor = context.getContentResolver().query(
                ContactsContract.Contacts.CONTENT_URI,
                projection, selection, null, sortOrder
        );

		// Set the adapter
		this.adapter = new ContactAdapter(this.context, cursor, true);
		this.listView = (ListView) view.findViewById(R.id.multicontactpickerList);
		this.listView.setAdapter(adapter);
        for (String key : this.selectedKeys){
            this.adapter.keyMap.put(key, true);
        }

        this.listView.setOnItemClickListener(this);

        String title = "Select People";
        ActionBar actionBar = getActivity().getActionBar();
        actionBar.setTitle(this.title);
        actionBar.setIcon(this.actionBarIcon);
        actionBar.setDisplayHomeAsUpEnabled(true);
	}
	
	@Override
	public void onPause(){
		super.onPause();
		Log.d(TAG, "onPause");

        if (this.listener != null && this.selectedKeys != null){
            Log.d(TAG, "Sending " + this.selectedKeys.size() + " keys to listener");
            String[] a = new String[this.selectedKeys.size()];
            a = this.selectedKeys.toArray(a);
            this.listener.picked(a);
        }
	}


    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Log.d(TAG, "onItemClick");
        CheckBox box = (CheckBox)view.findViewById(R.id.multicontactpickerCheckBox);
        box.setChecked(!box.isChecked());

        TextView keyView = (TextView)view.findViewById(R.id.multicontactpickerKey);
        String key = keyView.getText().toString();
        if (box.isChecked()){
            if (!selectedKeys.contains(key)){
                Log.d(TAG, "Added Key");
                selectedKeys.add(key);
            }
            this.adapter.keyMap.put(key, true);
        }else{
            selectedKeys.remove(key);
            this.adapter.keyMap.put(key, false);
        }
    }

    @Override
    public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {

    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        return false;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        return false;
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        return false;
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {

    }


    private class ContactAdapter extends CursorAdapter
	{
        public HashMap<String, Boolean> keyMap;
        private ContactThumbnailLoader imageLoader;

        public ContactAdapter(Context context, Cursor c, boolean autoRequery) {
            super(context, c, autoRequery);
            keyMap = new HashMap<String, Boolean>();
            imageLoader = new ContactThumbnailLoader(context, R.drawable.ic_contact_picture_holo_dark);
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            LayoutInflater inflater = LayoutInflater.from(context);
            View v = inflater.inflate(R.layout.multicontactpicker_item, parent, false);
            bindView(v, context, cursor);
            return v;
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            // Set the name
            TextView name = (TextView)view.findViewById(R.id.multicontactpickerName);
            name.setText(cursor.getString(1));
            // Set the key
            String key = cursor.getString(2);
            TextView keyView = (TextView)view.findViewById(R.id.multicontactpickerKey);
            keyView.setText(key);
            // Set the image
            ImageView image = (ImageView)view.findViewById(R.id.multicontactpickerImageView);
            imageLoader.load(key, image);
            // Set the checkbox
            CheckBox box = (CheckBox)view.findViewById(R.id.multicontactpickerCheckBox);
            if (!keyMap.containsKey(key))
                box.setChecked(false);
            else{
                box.setChecked(keyMap.get(key));
            }
        }

	}

}
