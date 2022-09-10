package com.abstractwombat.fragments;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.ClipData;
import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import androidx.core.widget.NestedScrollView;
import androidx.appcompat.widget.Toolbar;
import android.util.Log;
import android.util.TypedValue;
import android.view.DragEvent;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.UUID;


/**
 *  Presents a list of items each of which can be optionally
 *      - renamed
 *      - reordered
 *      - deleted
 *      - added to
 */
public class OrderedListFragment extends Fragment implements View.OnClickListener {
    private static final String TAG = "OrderedListFragment";

    public static int OPTION_FLAG_ALLOW_RENAME       = 1;
    public static int OPTION_FLAG_ALLOW_REORDER      = 2;
    public static int OPTION_FLAG_ALLOW_DELETE       = 4;
    public static int OPTION_FLAG_ALLOW_ADD          = 8;
    public static int OPTION_FLAG_ALLOW_CANCEL       = 16;
    public static int OPTION_FLAG_ALLOW_ADD_FOLDERS  = 32;
    public static int OPTION_FLAG_START_CREATE       = 64;

    public interface OnOrderedListFragmentCloseListener {
        public void OnOrderedListFragmentClose(String[] keys, String[] labels, boolean cancelled);
        public void OnOrderedListFragmentClose(OrderedListItem[] items, boolean cancelled);
    }

    private static final String ARG_ITEMS = "items";
    private static final String ARG_TITLE = "title";
    private static final String ARG_OPTIONS = "options";

    private ArrayList<OrderedListItem> mItems;
    private String mTitle;
    private Boolean mAllowRename;
    private Boolean mAllowReorder;
    private Boolean mAllowDelete;
    private Boolean mAllowAdd;
    private Boolean mAllowAddFolders;
    private Boolean mAllowCancel;
    private Boolean mStartCreate;

    private OnOrderedListFragmentCloseListener mListener;
    private Context mContext;
    private LayoutInflater mInflater;
    private ViewGroup mRootView;
    private LinearLayout mItemList;
    private ImageView mNewCommit;
    private EditText mNewEditText;
    private ImageView mNewFolderCommit;
    private EditText mNewFolderEditText;
    private View mDragItem;
    private ViewGroup mDragImageContainer;

    public OrderedListFragment() {}

    public static class OrderedListItem implements Parcelable{
        public OrderedListItem(String k, String l, boolean folder){
            key = k; label = l; isFolder = folder;
        }
        public OrderedListItem(String k) {
            key = k;
            label = "";
            isFolder = false;
        }
        public String key;
        public String label;
        public String parent;
        public boolean isFolder;

        OrderedListItem(Parcel in) {
            key = in.readString();
            label = in.readString();
            parent = in.readString();
            isFolder = in.readByte() != 0;
        }

        @Override
        public int hashCode() {
            return key.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof OrderedListItem){
                return key.equals(((OrderedListItem)obj).key);
            } else if (obj instanceof String) {
                return key.equals((String) obj);
            } else {
                return false;
            }
        }

        public static final Creator<OrderedListItem> CREATOR = new Creator<OrderedListItem>() {
            @Override
            public OrderedListItem createFromParcel(Parcel in) {
                return new OrderedListItem(in);
            }

            @Override
            public OrderedListItem[] newArray(int size) {
                return new OrderedListItem[size];
            }
        };

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            parcel.writeString(key);
            parcel.writeString(label);
            parcel.writeString(parent);
            parcel.writeByte((byte) (isFolder ? 1 : 0));
        }
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param items The items to populate the list
     * @param title The title to show on the toolbar
     * @param options Combination of the OPTION_FLAGs
     * @return A new instance of fragment OrderedListFragment.
     */
    public static OrderedListFragment newInstance(OrderedListItem[] items, String title, Integer options) {
        OrderedListFragment fragment = new OrderedListFragment();
        Bundle args = new Bundle();
        args.putParcelableArray(ARG_ITEMS, items);
        args.putString(ARG_TITLE, title);
        args.putInt(ARG_OPTIONS, options);
        fragment.setArguments(args);
        return fragment;
    }
    public static OrderedListFragment newInstance(String[] keys, String[] labels, String title, Integer options) {
        OrderedListItem[] items = new OrderedListItem[keys.length];
        for (int i=0; i <keys.length; i++){
            OrderedListItem item = new OrderedListItem(keys[i], labels[i], false);
            items[i] = item;
        }
        return newInstance(items, title, options);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null){
            Parcelable[] itemParcel = savedInstanceState.getParcelableArray("items");
            mItems = new ArrayList<>();
            if (itemParcel != null) {
                for (Parcelable p : itemParcel) {
                    mItems.add((OrderedListItem) p);
                }
            }
            mTitle = savedInstanceState.getString("title");
            mAllowRename = savedInstanceState.getBoolean("allowRename");
            mAllowReorder = savedInstanceState.getBoolean("allowReorder");
            mAllowDelete = savedInstanceState.getBoolean("allowDelete");
            mAllowAdd = savedInstanceState.getBoolean("allowAdd");
            mAllowAddFolders = savedInstanceState.getBoolean("allowAddFolders");
            mAllowCancel = savedInstanceState.getBoolean("allowCancel");
            mStartCreate = savedInstanceState.getBoolean("startCreate");
        } else if (getArguments() != null) {
            Parcelable[] itemParcel = getArguments().getParcelableArray(ARG_ITEMS);
            mItems = new ArrayList<>();
            if (itemParcel != null) {
                for (Parcelable p : itemParcel) {
                    mItems.add((OrderedListItem) p);
                }
            }
            mTitle = getArguments().getString(ARG_TITLE, "");
            Integer flags = getArguments().getInt(ARG_OPTIONS, 0);
            mAllowRename =     ((flags & OPTION_FLAG_ALLOW_RENAME)     == OPTION_FLAG_ALLOW_RENAME);
            mAllowReorder =    ((flags & OPTION_FLAG_ALLOW_REORDER)    == OPTION_FLAG_ALLOW_REORDER);
            mAllowDelete =     ((flags & OPTION_FLAG_ALLOW_DELETE)     == OPTION_FLAG_ALLOW_DELETE);
            mAllowAdd =        ((flags & OPTION_FLAG_ALLOW_ADD)        == OPTION_FLAG_ALLOW_ADD);
            mAllowAddFolders = ((flags & OPTION_FLAG_ALLOW_ADD_FOLDERS)== OPTION_FLAG_ALLOW_ADD_FOLDERS);
            mAllowCancel =     ((flags & OPTION_FLAG_ALLOW_CANCEL)     == OPTION_FLAG_ALLOW_CANCEL);
            mStartCreate =     ((flags & OPTION_FLAG_START_CREATE)     == OPTION_FLAG_START_CREATE);
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void	onStart(){
        super.onStart();
        Log.d(TAG, "onStart");
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");
        if (!mAllowCancel){
            save();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView");

        // Inflate the layout for this fragment
        mInflater = inflater;
        mRootView = (ViewGroup)inflater.inflate(R.layout.fragment_orderedlist, container, false);
        mDragImageContainer = (ViewGroup) mRootView.findViewById(R.id.dragImageContainer);

        // Setup the toolbar
        Toolbar toolbar = (Toolbar) mRootView.findViewById(R.id.toolbar);
        if (mAllowCancel) {
            toolbar.setNavigationIcon(R.drawable.ic_close_white_24dp);
        }else{
            toolbar.setNavigationIcon(R.drawable.ic_arrow_left_white_24dp);
        }
        toolbar.setTitle(mTitle);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Cancel
                Log.d(TAG, "Navigation clicked (cancel)");
                if (mAllowCancel) {
                    mListener.OnOrderedListFragmentClose(null, null, true);
                    mListener.OnOrderedListFragmentClose(null, true);
                }else{
                    save();
                }
            }
        });
        Button saveButton = (Button) toolbar.findViewById(R.id.button_save);
        if (mAllowCancel) {
            saveButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.d(TAG, "Save clicked");
                    save();
                }
            });
        }else{
            saveButton.setVisibility(View.GONE);
        }

        // Set the padding for the scroll view
        NestedScrollView scrollView = (NestedScrollView) mRootView.findViewById(R.id.scroll);
        ViewGroup.LayoutParams lp = toolbar.getLayoutParams();
        Resources r = getResources();
        int toolbarBuffer = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16, r
                .getDisplayMetrics());
        scrollView.setPadding(scrollView.getPaddingLeft(), scrollView.getPaddingTop() + toolbarBuffer,
                scrollView.getPaddingRight(), scrollView.getPaddingBottom());

        // Setup the item list
        mItemList = (LinearLayout) mRootView.findViewById(R.id.fragment_listmanager_item_list);
        mItemList.setOnDragListener(itemListDragListener);
        if (mItemList.getLayoutTransition() != null) {
            mItemList.getLayoutTransition().setDuration(50);
        }
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
//            layoutTransition.enableTransitionType(LayoutTransition.CHANGING);
//        }
//        mToolbar.setLayoutTransition(layoutTransition);


        // Setup the create new item row
        View newRow = scrollView.findViewById(R.id.item_add);
        if (mAllowAdd) {
            newRow.setVisibility(View.VISIBLE);
            newRow.setOnClickListener(this);
            newRow.setTag(-1);
            mNewEditText = ((EditText) newRow.findViewById(R.id.editTextViewNewItem));
            mNewEditText.setOnEditorActionListener(newItemOnEditorActionListener);
            mNewEditText.setOnFocusChangeListener(newItemFocusChangeListener);
            mNewCommit = (ImageView) newRow.findViewById(R.id.imageViewCommitNew);
            mNewCommit.setOnClickListener(this);
        }else{
            newRow.setVisibility(View.GONE);
        }

        // Setup the create new folder row
        View newFolderRow = scrollView.findViewById(R.id.item_addfolder);
        if (mAllowAddFolders) {
            newFolderRow.setVisibility(View.VISIBLE);
            newFolderRow.setOnClickListener(this);
            newFolderRow.setTag(-1);
            mNewFolderEditText = ((EditText) newFolderRow.findViewById(R.id.editTextViewNewFolder));
            mNewFolderEditText.setOnEditorActionListener(newFolderOnEditorActionListener);
            mNewFolderEditText.setOnFocusChangeListener(newFolderFocusChangeListener);
            mNewFolderCommit = (ImageView) newFolderRow.findViewById(R.id.imageViewCommitNewFolder);
            mNewFolderCommit.setOnClickListener(this);
        }else{
            newFolderRow.setVisibility(View.GONE);
        }

        // Find all the folders
        ArrayList<OrderedListItem> folders = new ArrayList<>();
        for (OrderedListItem item : mItems){
            if (item.isFolder) folders.add(item);
        }

        // Add the items views, ensuring the folders are always above their children
        ArrayList<OrderedListItem> addedItems = new ArrayList<>();
        for (OrderedListItem item : mItems) {
            if (addedItems.contains(item)) continue;
            if (item.isFolder){
                // Add the folder
                View itemView = createRow(item);
                mItemList.addView(itemView);
                // Add all its children
                for (OrderedListItem child : mItems) {
                    if (child.parent != null && item.key.equals(child.parent)){
                        mItemList.addView(createRow(child));
                        addedItems.add(child);
                    }
                }
            }else{
                mItemList.addView(createRow(item));
                addedItems.add(item);
            }
        }

//        // Add all the items
//        for (int i = 0; i< mItems.size(); i++){
//            View item = createRow(mItems.get(i));
//            mItemList.addView(item);
//        }

        return mRootView;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.d(TAG, "onSaveInstanceState");
        ArrayList<OrderedListItem> orderedItems = new ArrayList<>();
        for (int i = 0; i < mItemList.getChildCount(); i++){
            View child = mItemList.getChildAt(i);
            orderedItems.add(getOrderedListItem(child));
        }
        OrderedListItem[] itemArray = orderedItems.toArray(new OrderedListItem[orderedItems.size()]);
        outState.putParcelableArray("items", itemArray);
        outState.putString("title", mTitle);
        outState.putBoolean("allowRename", mAllowRename);
        outState.putBoolean("allowReorder", mAllowReorder);
        outState.putBoolean("allowDelete", mAllowDelete);
        outState.putBoolean("allowAdd", mAllowAdd);
        outState.putBoolean("allowAddFolders", mAllowAddFolders);
        outState.putBoolean("allowCancel", mAllowCancel);
        outState.putBoolean("startCreate", mStartCreate);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.d(TAG, "onViewCreated");
        if (mStartCreate){
            mNewEditText.requestFocus();
            showKeyboard();
        }
    }

    private class ViewHolder{
        public OrderedListItem item;
        public String key;
        public EditText editText;
        public View shadow;
        public View imageViewStart;
        public ImageView imageViewFolder;
        public TextView textView;
    }
    private View createRow(OrderedListItem item){
        Log.d(TAG, "Create row - " + item.label);

        View itemView = mInflater.inflate(R.layout.fragment_orderedlist_item, mItemList, false);
        itemView.setOnClickListener(this);

        ImageView folderImage = (ImageView) itemView.findViewById(R.id.imageViewFolder);
        if (folderImage != null) {
            if (item.isFolder) {
                folderImage.setTag(itemView);
                folderImage.setTag(R.id.folder_collapse_state, false);
                folderImage.setVisibility(View.VISIBLE);
                folderImage.setImageResource(R.drawable.ic_folder_grey600_24dp);
                folderImage.setOnClickListener(this);
            } else if (item.parent != null && !item.parent.isEmpty()) {
                folderImage.setVisibility(View.VISIBLE);
                folderImage.setImageResource(R.drawable.ic_subdirectory_arrow_right_grey600_24dp);
            } else {
                folderImage.setVisibility(View.GONE);
            }
        }

        ImageView deleteImage = (ImageView) itemView.findViewById(R.id.imageViewDelete);
        deleteImage.setVisibility(View.GONE);
        deleteImage.setOnClickListener(this);

        ImageView dragImage = (ImageView) itemView.findViewById(R.id.imageViewDrag);
        if (mAllowReorder) {
            dragImage.setTag(itemView);
            dragImage.setVisibility(View.VISIBLE);
            dragImage.setOnTouchListener(dragHandleTouchListener);
        }else{
            dragImage.setVisibility(View.INVISIBLE);
        }

        ImageView commitImage = (ImageView) itemView.findViewById(R.id.imageViewCommit);
        commitImage.setOnClickListener(this);

        TextView tv = (TextView) itemView.findViewById(R.id.textView);
        tv.setText(item.label);
        tv.setVisibility(View.VISIBLE);
        EditText et = (EditText) itemView.findViewById(R.id.editTextViewNewItem);
        et.setId(Math.abs(item.key.hashCode()));
        et.setTag(item.key);
        et.setText(item.label);
        et.setVisibility(View.GONE);
        et.setOnFocusChangeListener(editTextFocusChangeListener);
        et.setOnEditorActionListener(editTextOnEditorActionListener);

        ViewHolder vh = new ViewHolder();
        vh.item = item;
        vh.editText = et;
        vh.key = item.key;
        vh.shadow = (View) itemView.findViewById(R.id.shadow);
        vh.imageViewStart = (View) itemView.findViewById(R.id.imageViewStart);
        vh.imageViewFolder = folderImage;
        vh.textView = tv;
        itemView.setTag(vh);
        itemView.setOnClickListener(this);
        return itemView;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        Log.d(TAG, "onAttach");
        mContext = activity;
        mListener = null;
        FragmentManager fragmentManager = activity.getFragmentManager();
        int backStackEntryCount = fragmentManager.getBackStackEntryCount();
        if (backStackEntryCount > 0){
            Log.d(TAG, "    Searching backstack for listener");
            for (int i=backStackEntryCount-1; i>=0; i--){
                FragmentManager.BackStackEntry backStackEntry = fragmentManager.getBackStackEntryAt(i);
                Fragment fragment = getFragmentManager().findFragmentByTag(backStackEntry.getName());
                if (fragment instanceof OrderedListFragment){
                    continue;
                }
                if (fragment instanceof OnOrderedListFragmentCloseListener){
                    mListener = (OnOrderedListFragmentCloseListener) fragment;
                    Log.d(TAG, "    - Fragment yes! (" + backStackEntry.getName() + ")");
                    break;
                }
            }
        }
        if (mListener == null){
            Log.d(TAG, "    Checking if Activity is a listener");
            if (activity instanceof OnOrderedListFragmentCloseListener){
                mListener = (OnOrderedListFragmentCloseListener) activity;
                Log.d(TAG, "    - Activity yes!");
            }
        }
        if (mListener == null){
            Log.d(TAG, "    Failed to find a listener");
            throw new ClassCastException("Can't find OnOrderedListFragmentCloseListener implementation");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.item_add) {
            enterAddNewEditMode();
        } else if (view.getId() == R.id.item_addfolder) {
            enterAddNewFolderEditMode();
        }else if (view.getId() == R.id.imageViewFolder){
            ImageView folderImage = (ImageView) view;
            View itemView = (View) view.getTag();
            if (itemView != null){
                collapseFolder(itemView, null);
            }
        } else if (view.getId() == R.id.imageViewCommit) {
            View vp = (View) view.getParent();
            ViewHolder vh = (ViewHolder) vp.getTag();
            saveItemRename(vh.editText);
            exitItemEditMode(mItemList.indexOfChild(vp));
        } else if (view.getId() == R.id.imageViewCommitNew) {
            createNewItem();
            exitAddNewEditMode();
        } else if (view.getId() == R.id.imageViewCommitNewFolder) {
            createNewFolder();
            exitAddNewFolderEditMode();
        } else if (view.getId() == R.id.imageViewDelete) {
            View rvp = (View) view.getParent();
            View vp = (View) rvp.getParent();
            ViewHolder vh = (ViewHolder) vp.getTag();
            int i = mItems.indexOf(new OrderedListItem(vh.key));
            OrderedListItem item = mItems.get(i);
            if (item.isFolder){
                // Un-collapse
                collapseFolder(vp, false);
                // Set child to no-parent
                for (int itr=0; itr<mItemList.getChildCount(); itr++){
                    View childV = mItemList.getChildAt(itr);
                    OrderedListItem child = getOrderedListItem(childV);
                    if (child.parent != null && child.parent.equals(item.key)){
                        child.parent = "";
                        setupItemUI(childV);
                    }
                }
            }
            mItems.remove(i);
            exitItemEditMode(mItemList.indexOfChild(vp));
            mItemList.removeView(vp);
        } else if (view.getId() == R.id.item) {
            enterItemEditMode(mItemList.indexOfChild(view));
        }
    }

    private void enterAddNewEditMode(){
        Log.d(TAG, "enterAddNewEditMode");
        mNewEditText.requestFocus();
        mNewCommit.setVisibility(View.VISIBLE);
        showKeyboard();
    }
    private void exitAddNewEditMode(){
        Log.d(TAG, "exitAddNewEditMode");
        mNewCommit.setVisibility(View.GONE);
        mNewEditText.setText("");
        mNewEditText.clearFocus();
        hideKeyboard();
    }

    private void enterAddNewFolderEditMode(){
        Log.d(TAG, "enterAddNewFolderEditMode");
        mNewFolderEditText.requestFocus();
        mNewFolderCommit.setVisibility(View.VISIBLE);
        showKeyboard();
    }
    private void exitAddNewFolderEditMode(){
        Log.d(TAG, "exitAddNewFolderEditMode");
        mNewFolderCommit.setVisibility(View.GONE);
        mNewFolderEditText.setText("");
        mNewFolderEditText.clearFocus();
        hideKeyboard();
    }

    private void collapseFolder(View itemView, Boolean collapse){
        ImageView folderImage = (ImageView) itemView.findViewById(R.id.imageViewFolder);
        if (collapse == null){
            collapse = !(Boolean) folderImage.getTag(R.id.folder_collapse_state);
        }

        int index = mItemList.indexOfChild(itemView);
        if (index < 0 || index >= mItemList.getChildCount()){
            Log.d(TAG, "collapseFolder - invalid item view");
            return;
        }
        OrderedListItem listItem = getOrderedListItem(itemView);
        ArrayList<View> folderChildren = new ArrayList<>();
        for (int i=index+1; i<mItemList.getChildCount(); i++){
            View iV = mItemList.getChildAt(i);
            ViewHolder vh = (ViewHolder) iV.getTag();
            OrderedListItem item = mItems.get(mItems.indexOf(new OrderedListItem(vh.key)));
            if (item.isFolder) continue;
            if (item.parent == null || item.parent.isEmpty()) continue;
            if (item.parent.equals(listItem.key)){
                folderChildren.add(iV);
            }
        }
        int newState = View.VISIBLE;
        if (collapse) newState = View.GONE;
        for (View v : folderChildren){
            v.setVisibility(newState);
        }
        if (collapse){
            folderImage.setImageResource(R.drawable.ic_folder_multiple_grey600_24dp);
        }else{
            folderImage.setImageResource(R.drawable.ic_folder_grey600_24dp);
        }
        folderImage.setTag(R.id.folder_collapse_state, collapse);
    }

    private void enterItemEditMode(int gridEditChildIndex){
        Log.d(TAG, "enterItemEditMode");
        View item = mItemList.getChildAt(gridEditChildIndex);
        ViewHolder vh = (ViewHolder) item.getTag();
        OrderedListItem orderedListItem = getOrderedListItem(item);

        // Hide the drag handle
        ImageView dragImage = (ImageView) item.findViewById(R.id.imageViewDrag);
        dragImage.setVisibility(View.GONE);

        // Show the delete image
        ImageView deleteImage = (ImageView) item.findViewById(R.id.imageViewDelete);
        if (mAllowDelete) {
            deleteImage.setVisibility(View.VISIBLE);
        }else {
            deleteImage.setVisibility(View.INVISIBLE);
        }

        // Show the commit image
        ImageView commitImage = (ImageView) item.findViewById(R.id.imageViewCommit);
        commitImage.setVisibility(View.VISIBLE);

        if (mAllowRename) {
            // Hide the TextView
            final TextView tv = (TextView) item.findViewById(R.id.textView);
            tv.setVisibility(View.GONE);

            // Show the EditText and give it focus
            vh.editText.setVisibility(View.VISIBLE);
            vh.editText.requestFocus();

            showKeyboard();
        }
    }
    private void exitItemEditMode(int gridEditChildIndex){
        Log.d(TAG, "exitItemEditMode (" + gridEditChildIndex + ")");
        View item = mItemList.getChildAt(gridEditChildIndex);
        ViewHolder vh = (ViewHolder) item.getTag();

        // Show the drag handle
        ImageView dragImage = (ImageView) item.findViewById(R.id.imageViewDrag);
        if (mAllowReorder) {
            dragImage.setVisibility(View.VISIBLE);
        }else{
            dragImage.setVisibility(View.INVISIBLE);
        }

        // Hide the delete image
        ImageView deleteImage = (ImageView) item.findViewById(R.id.imageViewDelete);
        deleteImage.setVisibility(View.GONE);

        // Hide the commit image
        ImageView commitImage = (ImageView) item.findViewById(R.id.imageViewCommit);
        commitImage.setVisibility(View.GONE);

        if (mAllowRename) {
            // Show the TextView
            final TextView tv = (TextView) item.findViewById(R.id.textView);
            tv.setVisibility(View.VISIBLE);

            // Hide the EditText
            vh.editText.setVisibility(View.GONE);
            vh.editText.clearFocus();

            hideKeyboard();
        }
    }
    private void exitItemEditMode() {
        for (int i=0; i<mItemList.getChildCount(); i++){
            exitItemEditMode(i);
        }
    }

    private void saveItemRename(EditText editText){
        Log.d(TAG, "saveItemRename");
        String key = (String) editText.getTag();
        int i = mItems.indexOf(new OrderedListItem(key));
        String newLabel = editText.getText().toString();
        if (newLabel.isEmpty()){
            Log.d(TAG, "Tried to rename to an empty label");
            return;
        }
        mItems.get(i).label = newLabel;
        View vp = (View) editText.getParent();
        TextView tv = (TextView) vp.findViewById(R.id.textView);
        tv.setText(newLabel);
    }

    private void createNewItem(){
        Log.d(TAG, "createNewItem");
        String newLabel = mNewEditText.getText().toString();
        if (newLabel.isEmpty()){
            Log.d(TAG, "Tried to create an empty item");
            return;
        }
        String newKey = UUID.randomUUID().toString();
        OrderedListItem newItem = new OrderedListItem(newKey, newLabel, false);
        mItems.add(0, newItem);
        View item = createRow(newItem);
        mItemList.addView(item, 0);
    }

    private void createNewFolder(){
        Log.d(TAG, "createNewFolder");
        String newLabel = mNewFolderEditText.getText().toString();
        if (newLabel.isEmpty()){
            Log.d(TAG, "Tried to create an empty folder");
            return;
        }
        String newKey = UUID.randomUUID().toString();
        OrderedListItem newItem = new OrderedListItem(newKey, newLabel, true);
        mItems.add(0, newItem);
        View item = createRow(newItem);
        mItemList.addView(item, 0);
    }

    OrderedListItem getItemAtViewPosition(int i){
        View child = mItemList.getChildAt(i);
        ViewHolder vh = (ViewHolder) child.getTag();
        int index = mItems.indexOf(new OrderedListItem(vh.key));
        return mItems.get(index);
    }

    public void save() {
        int childCount = mItemList.getChildCount();
        Log.d(TAG, "childCount: " + childCount);
        String[] keys = new String[childCount];
        String[] labels = new String[childCount];
        OrderedListItem[] items = new OrderedListItem[childCount];
        for (int c = 0; c < childCount; c++) {
            items[c] = getItemAtViewPosition(c);
            keys[c] = items[c].key;
            labels[c] = items[c].label;
        }
        this.mListener.OnOrderedListFragmentClose(keys, labels, false);
        this.mListener.OnOrderedListFragmentClose(items, false);
    }

    private void showKeyboard(){
        Log.d(TAG, "showKeyboard");
        if (mContext == null) {
            mContext = (Context) getActivity();
        }
        InputMethodManager imm = (InputMethodManager) mContext.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
    }
    private void hideKeyboard(){
        Log.d(TAG, "hideKeyboard");
        if (mContext == null) {
            mContext = (Context) getActivity();
        }
        View view = mRootView.getFocusedChild();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager)mContext.getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    /**
     * Editor action listener for the "new item" EditText. Adds the new item when the user commits.
     */
    private EditText.OnEditorActionListener newItemOnEditorActionListener = new TextView.OnEditorActionListener() {
        @Override
        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                createNewItem();
                exitAddNewEditMode();
                return true; // consume.
            }
            return false; // pass on to other listeners.
        }
    };

    /**
     * Editor action listener for the "new folder" EditText. Adds the new item when the user commits.
     */
    private EditText.OnEditorActionListener newFolderOnEditorActionListener = new TextView.OnEditorActionListener() {
        @Override
        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                createNewFolder();
                exitAddNewFolderEditMode();
                return true; // consume.
            }
            return false; // pass on to other listeners.
        }
    };

    /**
     *  Editor action listener for each row. Saves the rename when the user commits the text.
     */
    private EditText.OnEditorActionListener editTextOnEditorActionListener = new TextView.OnEditorActionListener() {
        @Override
        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                // the user is done typing.
                saveItemRename((EditText) v);
                View parent = (View) v.getParent();
                exitItemEditMode(mItemList.indexOfChild(parent));
                return true; // consume.
            }
            return false; // pass on to other listeners.
        }
    };

    private View.OnFocusChangeListener newItemFocusChangeListener = new View.OnFocusChangeListener() {
        @Override
        public void onFocusChange(View v, boolean hasFocus) {
            if (hasFocus) {
                enterAddNewEditMode();
            }else{
                exitAddNewEditMode();
            }
        }
    };

    private View.OnFocusChangeListener newFolderFocusChangeListener = new View.OnFocusChangeListener() {
        @Override
        public void onFocusChange(View v, boolean hasFocus) {
            if (hasFocus) {
                enterAddNewFolderEditMode();
            }else{
                exitAddNewFolderEditMode();
            }
        }
    };

    private View.OnFocusChangeListener editTextFocusChangeListener = new View.OnFocusChangeListener() {
        @Override
        public void onFocusChange(View v, boolean hasFocus) {
            if (!hasFocus) {
                View vp = (View) v.getParent();
                exitItemEditMode(mItemList.indexOfChild(vp));
//                EditText et = (EditText) v;
//                TextView tv = (TextView)vp.findViewById(R.id.textView);
//                ImageView deleteImage = (ImageView) vp.findViewById(R.id.imageViewDelete);
//
//                String key = (String) vp.getTag();
//                String text = et.getText().toString();
//
//                int i = mKeys.indexOf(key);
//                mLabels.set(i, text);
//                tv.setText(text);
//                tv.setVisibility(View.VISIBLE);
//                et.setVisibility(View.GONE);
//                deleteImage.setVisibility(View.GONE);
            }
        }
    };


    private View.OnTouchListener dragHandleTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            Log.d(TAG, "dragHandleTouchListener");
            ClipData clipData = ClipData.newPlainText("nothing", "Here it is");
            View itemView = (View) view.getTag();

            exitItemEditMode();

            // Create a transparent view for the shadow
            View transView  = new FrameLayout(mContext);
            View.DragShadowBuilder shadow = new View.DragShadowBuilder(transView);

            // Start the drag, the touched item is the local state data
            mItemList.startDrag(clipData, shadow, itemView, 0);
            return false;
        }
    };

    private int overItemIndex(int y){
        //Log.d(TAG, "overItemIndex - searching " + x + ", " + y);
        for (int i=0; i<mItemList.getChildCount(); i++){
            View child = mItemList.getChildAt(i);
            if (child.getVisibility() == View.GONE) continue;
            int top = child.getTop();
            //Log.d(TAG, "    child : " + pos[0] + " - " + pos[1]);
            if (y > top && y < top+child.getHeight()){
                //Log.d(TAG, "overItemIndex = " + i);
                return i;
            }
        }
        return -1;
    }

    private void setDragMode(View item, boolean dragMode){
        ViewHolder vh = (ViewHolder) item.getTag();
        if (dragMode) {
            item.setBackgroundResource(R.color.orderedlist_grey);
            //vh.shadow.setVisibility(View.VISIBLE);
            vh.imageViewStart.setVisibility(View.GONE);
            vh.textView.setVisibility(View.GONE);
        }else{
            item.setBackgroundResource(0);
            setupItemUI(item);
            vh.textView.setVisibility(View.VISIBLE);

            // Check all the view's folder image status
            for (int v=0; v<mItemList.getChildCount(); v++){
                setupItemUI(mItemList.getChildAt(v));
            }
        }
    }

    private void setupItemUI(View itemView){
        ViewHolder vh = (ViewHolder) itemView.getTag();
        int index = mItems.indexOf(new OrderedListItem(vh.key));
        if (index < 0 || index >= mItems.size()){
            Log.d(TAG, "setupItemUI - item not found");
            return;
        }
        OrderedListItem listItem = mItems.get(index);
        vh.imageViewStart.setVisibility(View.VISIBLE);
        if (listItem.isFolder){
            vh.imageViewFolder.setVisibility(View.VISIBLE);
            boolean isCollapsed = (boolean) vh.imageViewFolder.getTag(R.id.folder_collapse_state);
            if (isCollapsed) {
                // Check if there are any children
                boolean hasChild = false;
                for (int i=0; i<mItemList.getChildCount(); i++){
                    OrderedListItem orderedListItem = getOrderedListItem(mItemList.getChildAt(i));
                    if (listItem.key.equals(orderedListItem.parent)){
                        hasChild = true;
                        break;
                    }
                }
                if (hasChild) {
                    vh.imageViewFolder.setImageResource(R.drawable.ic_folder_multiple_grey600_24dp);
                }else{
                    vh.imageViewFolder.setImageResource(R.drawable.ic_folder_grey600_24dp);
                }
            }else{
                vh.imageViewFolder.setImageResource(R.drawable.ic_folder_grey600_24dp);
            }
        }else if (listItem.parent != null && !listItem.parent.isEmpty()){
            vh.imageViewFolder.setVisibility(View.VISIBLE);
            vh.imageViewFolder.setImageResource(R.drawable.ic_subdirectory_arrow_right_grey600_24dp);
        }else{
            vh.imageViewFolder.setVisibility(View.GONE);
        }
    }

    private View.OnDragListener itemListDragListener = new View.OnDragListener() {
        private Integer mStartDragY;
        private Integer mItemTopStart;
        private Integer mDragDropNewPosition;
        private Integer mOriginalIndex;

        @Override
        public boolean onDrag(View view, DragEvent event) {
            View item = (View) event.getLocalState();
            switch (event.getAction()){
                case DragEvent.ACTION_DRAG_STARTED:
                    Log.d(TAG, "Starting drag operation");
                    int[] startPos = new int[2];
                    mItemTopStart = item.getTop();
                    mOriginalIndex = mItemList.indexOfChild(item);
                    mDragDropNewPosition = mOriginalIndex;

                    OrderedListItem oItem = getOrderedListItem(item);
                    mDragImageContainer.setVisibility(View.GONE);
                    mDragItem = createRow(oItem);
                    mDragImageContainer.removeAllViews();
                    mDragImageContainer.addView(mDragItem);

                    // If it's a folder, collapse all folders. Otherwise you could drag a folder into a folder.
                    if (oItem.isFolder){
//                collapseFolder(itemView, true);
                        for (int i=0; i<mItemList.getChildCount(); i++){
                            collapseFolder(mItemList.getChildAt(i), true);
                        }
                    }

                    // Prepare the item
                    setDragMode(item, true);


                    return true;
                case DragEvent.ACTION_DRAG_LOCATION:
                    int y = (int) event.getY();

                    if (mItemTopStart == null){
                        return true;
                    }

                    // Move the drag image
                    if (mStartDragY == null){
                        mStartDragY = y;
                    }
                    mDragImageContainer.setTranslationY(y-(mStartDragY - mItemTopStart));

                    // Show the drag image if it's hidden
                    if (mDragImageContainer.getVisibility() == View.GONE) {
                        mDragImageContainer.setVisibility(View.VISIBLE);
                    }

                    // Find the item we are above
                    int index = overItemIndex(y);
                    if (index < 0 || index >= mItemList.getChildCount()) {
                        //Log.d(TAG, "Dragged to invalid position");
                        return true;
                    }

                    // Same place as before
                    if (index == mDragDropNewPosition) {
                        //Log.d(TAG, "DragLocation - already in new pos!!");
                        return true;
                    }

                    // Wait for layout to complete
                    if (mItemList.getLayoutTransition().isRunning()) {
                        //Log.d(TAG, "Layout transition is ongoing");
                        return true;
                    }

                    Log.d(TAG, "Dragged item to " + index);
                    mDragDropNewPosition = moveItem(item, mItemList.getChildAt(index));
                    return true;
                case DragEvent.ACTION_DRAG_EXITED:
                    return true;
                case DragEvent.ACTION_DRAG_ENDED:
                    // Return the View to its original index
                    if (mDragImageContainer.getVisibility() != View.GONE) {
                        Log.d(TAG, "Drag ended");
                        mItemList.removeView(item);
                        setDragMode(item, false);
                        mItemList.addView(item, mOriginalIndex);
                        mDragImageContainer.setTranslationY(0);
                        mDragImageContainer.setVisibility(View.GONE);
                        mStartDragY = null;
                    }
                    return true;
                case DragEvent.ACTION_DROP:
                    // Make the View visible
                    Log.d(TAG, "Dropped view");
                    setDragMode(item, false);
                    mDragImageContainer.setVisibility(View.GONE);
                    mDragImageContainer.setTranslationY(0);
                    mStartDragY = null;
                    return true;
            }
            return true;
        }
    };

    private int moveItem(View viewToMove, View targetView) {
        OrderedListItem listItem = getOrderedListItem(viewToMove);
        int originalIndex = mItemList.indexOfChild(viewToMove);

        // Find all the items that need to be moved
        ArrayList<View> itemsToMove = new ArrayList<>();
        itemsToMove.add(viewToMove);
        if (listItem.isFolder) {
            // Get the item's children
            for (int i = 0; i < mItemList.getChildCount(); i++) {
                View childAt = mItemList.getChildAt(i);
                OrderedListItem childItem = getOrderedListItem(childAt);
                if (childItem.parent != null && listItem.key.equals(childItem.parent)) {
                    itemsToMove.add(childAt);
                }
            }
        }

        //  Find the insertion point
        int index = mItemList.indexOfChild(targetView);
        OrderedListItem targetItem = getOrderedListItem(targetView);
        Log.d(TAG, "Moving " + itemsToMove.size() + " items to position " + index);

        // Check if we're adding to a folder
        String newFolder = null;
        int indexAbove = originalIndex > index ? index - 1 : index;
        View itemAbove = mItemList.getChildAt(indexAbove);
        OrderedListItem aboveItem = null;
        if (itemAbove != null) {
            aboveItem = getOrderedListItem(itemAbove);
            if (aboveItem.isFolder) {
                newFolder = aboveItem.key;
                Log.d(TAG, "Item above is a folder with id=" + newFolder);
            } else if (aboveItem.parent != null && !aboveItem.parent.isEmpty()) {
                newFolder = aboveItem.parent;
                Log.d(TAG, "Item above is a folder with id=" + newFolder);
            }
        }

        // Can't put a folder in a folder... move it below all the target's children
        int childShift = 0;
        if (newFolder != null && listItem.isFolder) {
            int aboveIndex = index + 1;
            for (int i = aboveIndex; i < mItemList.getChildCount(); i++) {
                View child = mItemList.getChildAt(i);
                OrderedListItem childItem = getOrderedListItem(child);
                if (childItem.parent == null || !childItem.parent.equals(newFolder)) {
                    break;
                } else {
                    childShift++;
                }
            }
            Log.d(TAG, "Child shift = " + childShift);
        }

        // Place the item in the folder, adjusting the drag view's appearance
        if (!listItem.isFolder) {
            if (newFolder != null) {
                // Place item in the folder
                listItem.parent = newFolder;
                ViewHolder dragVh = (ViewHolder) mDragItem.getTag();
                dragVh.imageViewFolder.setVisibility(View.VISIBLE);
                dragVh.imageViewFolder.setImageResource(R.drawable
                        .ic_subdirectory_arrow_right_grey600_24dp);
                Log.d(TAG, "Item added to folder with id=" + newFolder);
            } else {
                listItem.parent = null;
                ViewHolder dragVh = (ViewHolder) mDragItem.getTag();
                dragVh.imageViewFolder.setVisibility(View.GONE);
                Log.d(TAG, "Item removed from folder");
            }
        }

        // Remove all the items
        for (View childAt : itemsToMove) {
            mItemList.removeView(childAt);
        }

        // Find the new index at which to insert
        index += childShift;

        // Add all the items
        index = Math.min(index, mItemList.getChildCount());
        Collections.reverse(itemsToMove);
        for (View childAt : itemsToMove) {
            mItemList.addView(childAt, index);
        }
        return index;
    }

    private OrderedListItem getOrderedListItem(View itemView) {
        ViewHolder vh = (ViewHolder) itemView.getTag();
        return vh.item;
//        int i = mItems.indexOf(new OrderedListItem(vh.key));
//        return mItems.get(i);
    }

}
