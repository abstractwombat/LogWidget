package com.abstractwombat.fragments;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.TreeMap;

/**
 *  Presents a table of data with the following options
 *      - Number of columns that are header
 *      - Read-only
 *      - Searchable
 *      - Create new entry
 *      - Delete entry
 *      - Cancellable
 */
public class TableFragment extends Fragment {
    private static final String TAG = "TableFragment";
    public static final String FRAGMENT_NAME = TAG;

    public static int OPTION_FLAG_ALLOW_SEARCH   = 1;
    public static int OPTION_FLAG_READ_ONLY      = 2;
    public static int OPTION_FLAG_ALLOW_DELETE   = 4;
    public static int OPTION_FLAG_ALLOW_ADD      = 16;
    public static int OPTION_FLAG_ALLOW_CANCEL   = 32;

    public interface OnTableFragmentCloseListener {
        public void OnTableFragmentClose(ArrayList<TableRow> rows, boolean cancelled);
    }

    private static final String ARG_COLUMNS     = "columns";
    private static final String ARG_ROWS        = "rows";
    private static final String ARG_TITLE       = "title";
    private static final String ARG_OPTIONS     = "options";

    public static class TableColumn implements Parcelable {
        public String label;
        public String unit;
        public boolean searchable;
        public boolean editable;
        public boolean header;
        public boolean category;

        public TableColumn(){};
        protected TableColumn(Parcel in) {
            label = in.readString();
            unit = in.readString();
            searchable = in.readByte() != 0;
            editable = in.readByte() != 0;
            header = in.readByte() != 0;
            category = in.readByte() != 0;
        }

        public final Creator<TableColumn> CREATOR = new Creator<TableColumn>() {
            @Override
            public TableColumn createFromParcel(Parcel in) {
                return new TableColumn(in);
            }

            @Override
            public TableColumn[] newArray(int size) {
                return new TableColumn[size];
            }
        };

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(label);
            dest.writeString(unit);
            dest.writeByte((byte) (searchable ? 1 : 0));
            dest.writeByte((byte) (editable ? 1 : 0));
            dest.writeByte((byte) (header ? 1 : 0));
            dest.writeByte((byte) (category ? 1 : 0));
        }
    }
    public static class TableRow implements Parcelable {
        public String[] entries;

        public TableRow(){};
        protected TableRow(Parcel in) {
            entries = in.createStringArray();
        }

        public final Creator<TableRow> CREATOR = new Creator<TableRow>() {
            @Override
            public TableRow createFromParcel(Parcel in) {
                return new TableRow(in);
            }

            @Override
            public TableRow[] newArray(int size) {
                return new TableRow[size];
            }
        };

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeStringArray(entries);
        }
    }
    private class TableRowView implements Parcelable {
        public TableRow row;
        public Boolean expanded;
        public TableRowView(TableRow r, Boolean e){
            this.row = r;
            this.expanded = e;
        }

        protected TableRowView(Parcel in) {
            row = in.readParcelable(TableRow.class.getClassLoader());
            expanded = in.readByte() == 1;
        }

        public final Creator<TableRowView> CREATOR = new Creator<TableRowView>() {
            @Override
            public TableRowView createFromParcel(Parcel in) {
                return new TableRowView(in);
            }

            @Override
            public TableRowView[] newArray(int size) {
                return new TableRowView[size];
            }
        };

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeParcelable(row, flags);
            dest.writeByte((byte) (expanded ? 1 : 0));
        }
    }

    private OnTableFragmentCloseListener mListener;
    private ArrayList<TableColumn> mColumns;
    private ArrayList<TableRow> mRows;
    private int mHeaderTextColumn;
    private int mGroupColumn;
    private String mTitle;
    private Boolean mReadOnly;
    private Boolean mAllowSearch;
    private Boolean mAllowDelete;
    private Boolean mAllowAdd;
    private Boolean mAllowCancel;
    private int mCategoryColumn = -1;
    private int mSortColumn = -1;

    private Context mContext;
    private RecyclerView mRecyclerView;
    private Toolbar mToolbar;
    private EditText mSearchBox;
    private ImageView mClearSearch;
    private RecyclerAdapter mAdapter;
    private DividerItemDecoration mListDivider;

    private TreeMap<String,ArrayList<TableRowView>> mVisibleRowsCategory;


    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param columns   The columns of the table
     * @param rows      The rows of the table
     * @param title     The title to display on the actionbar
     * @param options   Combination of the flags (use "|" to combine)
     * @return          TableFragment instance
     */
    public static TableFragment newInstance(ArrayList<TableColumn> columns, ArrayList<TableRow>
            rows, String title, Integer options) {
        TableFragment fragment = new TableFragment();
        Bundle args = new Bundle();
        args.putParcelableArrayList(ARG_COLUMNS, columns);
        args.putParcelableArrayList(ARG_ROWS, rows);
        args.putString(ARG_TITLE, title);
        args.putInt(ARG_OPTIONS, options);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            mColumns = savedInstanceState.getParcelableArrayList("columns");
            mRows = savedInstanceState.getParcelableArrayList("rows");
            mVisibleRowsCategory = (TreeMap<String, ArrayList<TableRowView>>) savedInstanceState.getSerializable("visible");
            mTitle = savedInstanceState.getString("title");
            mReadOnly = savedInstanceState.getBoolean("readOnly");
            mAllowSearch = savedInstanceState.getBoolean("allowSearch");
            mAllowDelete = savedInstanceState.getBoolean("allowDelete");
            mAllowAdd = savedInstanceState.getBoolean("allowAdd");
            mAllowCancel = savedInstanceState.getBoolean("allowCancel");
        }else if (getArguments() != null) {
            mColumns = getArguments().getParcelableArrayList(ARG_COLUMNS);
            mRows = getArguments().getParcelableArrayList(ARG_ROWS);
            if (mColumns == null || mRows == null) {
                Log.d(TAG, "Invalid initialization!");
                return;
            }
            mTitle = getArguments().getString(ARG_TITLE, "");
            Integer flags = getArguments().getInt(ARG_OPTIONS, 0);
            mAllowSearch = ((flags & OPTION_FLAG_ALLOW_SEARCH) == OPTION_FLAG_ALLOW_SEARCH);
            mReadOnly    = ((flags & OPTION_FLAG_READ_ONLY)    == OPTION_FLAG_READ_ONLY);
            mAllowDelete = ((flags & OPTION_FLAG_ALLOW_DELETE) == OPTION_FLAG_ALLOW_DELETE);
            mAllowAdd    = ((flags & OPTION_FLAG_ALLOW_ADD)    == OPTION_FLAG_ALLOW_ADD);
            mAllowCancel = ((flags & OPTION_FLAG_ALLOW_CANCEL) == OPTION_FLAG_ALLOW_CANCEL);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        Log.d(TAG, "onSaveInstanceState");
        super.onSaveInstanceState(outState);
        outState.putParcelableArrayList("columns", mColumns);
        outState.putParcelableArrayList("rows", mRows);
        outState.putSerializable("visible", mVisibleRowsCategory);
        outState.putString("title", mTitle);
        outState.putBoolean("readOnly", mReadOnly);
        outState.putBoolean("allowSearch", mAllowSearch);
        outState.putBoolean("allowDelete", mAllowDelete);
        outState.putBoolean("allowAdd", mAllowAdd);
        outState.putBoolean("allowCancel", mAllowCancel);
    }

    @Override
    public void onViewStateRestored(Bundle savedInstanceState) {
        Log.d(TAG, "onViewStateRestored");
        super.onViewStateRestored(savedInstanceState);
    }

    @Override
    public void	onStart(){
        super.onStart();
        Log.d(TAG, "onStart");
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "onStart");
        if (!mAllowCancel){
            save();
        }
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
                if (fragment instanceof TableFragment){
                    continue;
                }
                if (fragment instanceof OnTableFragmentCloseListener){
                    mListener = (OnTableFragmentCloseListener) fragment;
                    Log.d(TAG, "    - Fragment yes! (" + backStackEntry.getName() + ")");
                    break;
                }
            }
        }
        if (mListener == null){
            Log.d(TAG, "    Checking if Activity is a listener");
            if (activity instanceof OnTableFragmentCloseListener){
                mListener = (OnTableFragmentCloseListener) activity;
                Log.d(TAG, "    - Activity yes!");
            }
        }
        if (mListener == null){
            Log.d(TAG, "    Failed to find a listener");
            throw new ClassCastException("Can't find OnTableFragmentCloseListener implementation");
        }
    }

    public void save() {
        if (this.mListener != null) {
            this.mListener.OnTableFragmentClose(mRows, false);
        }else{
            Log.d(TAG, "Saving... no listener");
        }
    }

    private void removeFromBackStack(){
        FragmentManager fragmentManager = getActivity().getFragmentManager();
        int backCount = fragmentManager.getBackStackEntryCount();
        if (backCount > 0){
            if (FRAGMENT_NAME.equals(fragmentManager.getBackStackEntryAt(backCount-1).getName())){
                fragmentManager.popBackStack();
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView");
        if (mContext == null){
            mContext = getActivity();
        }

        View view = inflater.inflate(R.layout.fragment_table, container, false);

        // Setup the navigation icon
        mToolbar = (Toolbar) view.findViewById(R.id.toolbar);
        if (mAllowCancel) {
            mToolbar.setNavigationIcon(R.drawable.ic_close_white_24dp);
        }else{
            mToolbar.setNavigationIcon(R.drawable.ic_arrow_left_white_24dp);
        }
        mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "Navigation clicked (cancel)");
                if (mAllowCancel) {
                    if (mListener != null) {
                        mListener.OnTableFragmentClose(mRows, true);
                    }
                }else{
                    save();
                }
                removeFromBackStack();
            }
        });

        // Setup the save button
        View saveButton = mToolbar.findViewById(R.id.button_save);
        if (mAllowCancel){
            saveButton.setVisibility(View.VISIBLE);
            saveButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    save();
                    removeFromBackStack();
                }
            });
        }else{
            saveButton.setVisibility(View.GONE);
        }

//        CoordinatorLayout.LayoutParams lp = (CoordinatorLayout.LayoutParams) mToolbar
//                .getLayoutParams();
//        mRecyclerView.setPadding(mRecyclerView.getPaddingLeft(), lp.height + getToolbarBuffer(),
//                mRecyclerView.getPaddingRight(), mRecyclerView.getPaddingBottom());

        // Setup the search bar
        mSearchBox = (EditText) mToolbar.findViewById(R.id.editText);
        mSearchBox.addTextChangedListener(searchTextWatcher);
        mClearSearch = (ImageView) mToolbar.findViewById(R.id.clearEditText);
        mClearSearch.setVisibility(View.INVISIBLE);

        // Setup the add button
        FloatingActionButton newFAB = (FloatingActionButton) view.findViewById(R.id.button_new);
        if (mAllowAdd){
            Log.d(TAG, "Showing FAB");
            newFAB.setVisibility(View.VISIBLE);
            newFAB.setOnClickListener(newItemClickListener);
        }else{
            Log.d(TAG, "Hiding FAB");
            newFAB.setVisibility(View.GONE);
        }

        // Setup the RecyclerView
        mRecyclerView = (RecyclerView) view.findViewById(R.id.list);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(mContext));
        mListDivider = new DividerItemDecoration(mContext);
        mRecyclerView.addItemDecoration(mListDivider);

        // Find the column to use as the header text (first header)
        int headerTextColumn = 0;
        for (TableColumn column : mColumns){
            if (column.header){
                headerTextColumn = mColumns.indexOf(column);
                Log.d(TAG, "Found header text index " + headerTextColumn);
                break;
            }
        }

        // Check for a category column
        for (TableColumn column : mColumns){
            if (column.category){
                mCategoryColumn = mColumns.indexOf(column);
                Log.d(TAG, "Found category column index " + mCategoryColumn);
                break;
            }
        }
        mSortColumn = headerTextColumn;

        if (savedInstanceState == null) {
            buildVisibleList(null);
        }

        // Set the adapter
        mAdapter = new RecyclerAdapter(headerTextColumn);
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());

        mClearSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mSearchBox.setText("");
                buildVisibleList(null);
            }
        });
        return view;
    }

    private void buildVisibleList(String searchString){
        Log.d(TAG, "buildVisibleList");

        if (mVisibleRowsCategory != null && (searchString == null || searchString.isEmpty())) {
            // Check if the current visible data is filtered
            int count = 0;
            for (TreeMap.Entry<String, ArrayList<TableRowView>> entry : mVisibleRowsCategory.entrySet()) {
                count += entry.getValue().size();
            }
            if (mRows.size() == count){
                // Everything is here, nothing left to do
                return;
            }
        }

        // Find the searchable columns if we need them
        ArrayList<Integer> searchableCols = null;
        if (searchString != null && !searchString.isEmpty()){
            searchableCols = new ArrayList<>();
            for (int c=0; c<mColumns.size(); c++){
                if (mColumns.get(c).searchable)
                    searchableCols.add(c);
            }
            Log.d(TAG, "    Found " + searchableCols.size() + " columns to search");
        }
        // Build a list of the existing TableRowViews that we need to keep
        HashMap<TableRow, TableRowView> existingRows = null;
        if (mVisibleRowsCategory != null){
            existingRows = new HashMap<>();
            for (TreeMap.Entry<String,ArrayList<TableRowView>> entry : mVisibleRowsCategory.entrySet()){
                for (TableRowView trv : entry.getValue()){
                    existingRows.put(trv.row, trv);
                }
            }
        }
        // Group the rows into categories
        mVisibleRowsCategory = new TreeMap<>(new Comparator() {
            @Override
            public int compare(Object lhs, Object rhs) {
                return ((String)lhs).toLowerCase().compareTo(((String)rhs).toLowerCase());
            }
        });
        for (TableRow row : mRows){
            String key = "";
            if (mCategoryColumn >= 0 && mCategoryColumn < row.entries.length){
                key = row.entries[mCategoryColumn];
            }
            if (!mVisibleRowsCategory.containsKey(key)){
                mVisibleRowsCategory.put(key, new ArrayList<TableRowView>());
            }

            TableRowView newRow = null;
            if (existingRows != null){
                newRow = existingRows.get(row);
            }
            if (newRow == null){
                newRow = new TableRowView(row, false);
            }
            if (searchableCols == null || searchableCols.isEmpty()) {
                mVisibleRowsCategory.get(key).add(newRow);
            }else{
                for (Integer index : searchableCols){
                    if (newRow.row.entries[index].toLowerCase().contains(searchString)){
                        mVisibleRowsCategory.get(key).add(newRow);
                        break;
                    }
                }
            }
        }

        // Sort each category
        for (TreeMap.Entry<String,ArrayList<TableRowView>> entry : mVisibleRowsCategory.entrySet()){
            Collections.sort(entry.getValue(), new Comparator<TableRowView>() {
                @Override
                public int compare(TableRowView lhs, TableRowView rhs) {
                    return lhs.row.entries[mSortColumn].compareTo(rhs.row.entries[mSortColumn]);
                }
            });
        }

    }

    private View.OnClickListener newItemClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Log.d(TAG, "newItemClickListener");
            TableRow newRow = new TableRow();
            newRow.entries = new String[mColumns.size()];
            for (int i=0; i<mColumns.size(); i++) newRow.entries[i] = "";
            mRows.add(0, newRow);
            if (!mVisibleRowsCategory.containsKey("")){
                mVisibleRowsCategory.put("", new ArrayList<TableRowView>());
            }
            mVisibleRowsCategory.get("").add(0, new TableRowView(newRow, true));
            mRecyclerView.scrollToPosition(0);
            mAdapter.notifyItemInserted(0);
        }
    };

    private TextWatcher searchTextWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }
        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            String searchString = s.toString().toLowerCase();
            Log.d(TAG, "Search string chaanged - " + searchString);
            // Search
            if (!searchString.isEmpty()){
                mClearSearch.setVisibility(View.VISIBLE);
            }else{
                mClearSearch.setVisibility(View.INVISIBLE);
            }
            buildVisibleList(searchString);
            mAdapter.notifyDataSetChanged();
        }
        @Override
        public void afterTextChanged(Editable s) {

        }
    };

    private class EditTextWatcher implements TextWatcher {
        private TableRow mRow;
        private int mColumn;
        private TextView mAlternateTextView;

        public EditTextWatcher(TableRow r, int c, TextView altText){
            mRow = r;
            mColumn = c;
            mAlternateTextView = altText;
        }
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }
        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            mRow.entries[mColumn] = s.toString();
            if (mAlternateTextView != null){
                mAlternateTextView.setText(s);
            }
        }
        @Override
        public void afterTextChanged(Editable s) {

        }
    };

    private class ViewHolder extends RecyclerView.ViewHolder {
        protected TableRowView item;
        protected View header;
        protected TextView text;
        protected ImageView delete;
        protected LinearLayout stack;
        protected View shadow;

        public ViewHolder(View view, int type) {
            super(view);
            header = view.findViewById(R.id.header);
            text = (TextView) view.findViewById(R.id.textView);
            delete = (ImageView) view.findViewById(R.id.deleteButton);
            shadow = view.findViewById(R.id.shadow);
            stack = (LinearLayout) view.findViewById(R.id.item_stack);
        }
    }
    private class RecyclerAdapter extends RecyclerView.Adapter<ViewHolder> {
        private LayoutInflater mInflater;
        private int titleColumnIndex;

        public RecyclerAdapter(){
            this.titleColumnIndex = 0;
        }
        public RecyclerAdapter(int headerTextColumn){
            this.titleColumnIndex = headerTextColumn;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            mInflater = LayoutInflater.from(parent.getContext());
            View view = mInflater.inflate(R.layout.fragment_table_item, null);
            return new ViewHolder(view, viewType);
        }

        @Override
        public int getItemViewType(int position) {
            return 0;
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            Log.d(TAG, "onBindViewHolder " + position);

            // Get the item
            Object itemObject = (Object) getItem(position);
            if (itemObject == null) {
                Log.d(TAG, "    Item not found at position " + position);
                return;
            }

            // Item must be a TableRowView
            if (!(itemObject instanceof TableRowView)){
                Log.d(TAG, "    Error: item must be a TableRowView");
                return;
            }
            TableRowView item = (TableRowView)itemObject;
            holder.item = item;

            // Set text
            String label = item.row.entries[titleColumnIndex];
            holder.text.setText(label);

            // Show / Hide the expanded view
            if (item.expanded) {
                Log.d(TAG, "    Item in mExpandedRows");
                if (holder.stack.getChildCount() != 0) {
                    holder.stack.removeAllViews();
                }
                holder.stack.setVisibility(View.VISIBLE);
                holder.delete.setVisibility(View.VISIBLE);
                holder.shadow.setVisibility(View.VISIBLE);
                for (int i=0; i<item.row.entries.length; i++){
                    View view = mInflater.inflate(R.layout.fragment_table_edit_item, holder.stack, false);
                    TextView labelView = (TextView) view.findViewById(R.id.label);
                    labelView.setText(mColumns.get(i).label);
                    EditText valueView = (EditText) view.findViewById(R.id.value);
                    valueView.setText(item.row.entries[i]);
                    if (i == titleColumnIndex) {
                        valueView.addTextChangedListener(new EditTextWatcher(item.row, i, holder.text));
                    }else{
                        valueView.addTextChangedListener(new EditTextWatcher(item.row, i, null));
                    }
                    TextView unitView = (TextView) view.findViewById(R.id.unit);
                    unitView.setText(mColumns.get(i).unit);
                    holder.stack.addView(view);
                }
            }else{
                holder.stack.setVisibility(View.GONE);
                holder.delete.setVisibility(View.GONE);
                holder.shadow.setVisibility(View.GONE);
                holder.stack.removeAllViews();
            }

            // Setup the delete button
            holder.delete.setTag(item);
            holder.delete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    TableRowView delRow = (TableRowView) v.getTag();
                    Log.d(TAG, "Delete pressed on item");

                    // Remove from mVisibleRowsCategory
                    int position = 0;
                    String removedRowsCategory = "";
                    for (TreeMap.Entry<String,ArrayList<TableRowView>> entry : mVisibleRowsCategory.entrySet()){
                        int i = entry.getValue().indexOf(delRow);
                        if (i >= 0){
                            entry.getValue().remove(i);
                            position += i;
                            removedRowsCategory = entry.getKey();
                            break;
                        }
                        position += entry.getValue().size();
                    }

                    // Remove from the main container
                    if (!mRows.remove(delRow.row)){
                        Log.d(TAG, "    -Failed to delete item");
                    }

                    // Check if this is last in its category
                    if (mVisibleRowsCategory.get(removedRowsCategory).isEmpty()){
                        mVisibleRowsCategory.remove(removedRowsCategory);
                    }

                    Log.d(TAG, "    -" + mRows.size() + " rows left");
                    mAdapter.notifyItemRemoved(position);
                }
            });

            // Setup the header's click listener
            holder.header.setTag(holder);
            holder.header.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ViewHolder vh = (ViewHolder) v.getTag();
                    vh.item.expanded = !vh.item.expanded;

                    int position = 0;
                    for (TreeMap.Entry<String,ArrayList<TableRowView>> entry : mVisibleRowsCategory.entrySet()){
                        int i = entry.getValue().indexOf(vh.item);
                        if (i >= 0){
                            position += i;
                            break;
                        }else{
                            position += entry.getValue().size();
                        }
                    }
                    mAdapter.notifyItemChanged(position);
                }
            });
        }

        @Override
        public int getItemCount() {
            int count = 0;
            for (TreeMap.Entry<String,ArrayList<TableRowView>> entry : mVisibleRowsCategory.entrySet()){
                count += entry.getValue().size();
            }
            Log.d(TAG, "Adapter count " + count);
            return count;
        }

        private Object getItem(int position) {
            int count = 0;
            for (TreeMap.Entry<String,ArrayList<TableRowView>> entry : mVisibleRowsCategory.entrySet()){
                if (position < count + entry.getValue().size()){
                    return entry.getValue().get(position - count);
                }
                count += entry.getValue().size();
            }
            return null;
        }

    }

    public class DividerItemDecoration extends RecyclerView.ItemDecoration {
        private Drawable mHeading;
        private Drawable mShadow;
        private int mHeadingHeight;
        private float mTextHeight;
        private int mTextSize;
        private float mTextIndent;
        private int mShadowHeight;

        /**
         * Default divider will be used
         */
        public DividerItemDecoration(Context context) {
            mHeading = ContextCompat.getDrawable(context, R.drawable.list_heading);
            mShadow = ContextCompat.getDrawable(context, R.drawable.shadow_dark);

            mHeadingHeight = mHeading.getIntrinsicHeight();
            mTextHeight = headerTextPaint.descent() + headerTextPaint.ascent();
            mTextIndent = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16,
                    getResources().getDisplayMetrics());
            mShadowHeight = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4,
                    getResources().getDisplayMetrics());
       }

        private Paint headerTextPaint = new Paint();
        private Paint headerBackPaint = new Paint();
        {
            int textSizePixel = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 14,
                    getResources().getDisplayMetrics());
            headerTextPaint.setTextSize(textSizePixel);
            headerTextPaint.setColor(Color.WHITE);
            headerTextPaint.setTextAlign(Paint.Align.LEFT);
            headerBackPaint.setColor(Color.DKGRAY);
        }

        @Override
        public void onDrawOver(Canvas c, RecyclerView parent, RecyclerView.State state) {
            if (mCategoryColumn < 0 ){
                return;
            }
            for (int i = 0; i < parent.getChildCount(); i++) {
                View child = parent.getChildAt(i);
                int translationY = (int)(child.getTranslationY() + 0.5f);
                int position = parent.getChildAdapterPosition(child);
                StringBuilder label = new StringBuilder();
                if (isHeaderPosition(position, label)) {
                    mHeading.setBounds(child.getLeft(), child.getTop() - mHeadingHeight + translationY, child
                            .getRight(), child.getTop() + translationY);
                    mHeading.draw(c);
                    float textY = child.getTop() - (mHeadingHeight / 2) - (mTextHeight / 2) + translationY;
                    String labelString = label.toString();
                    if (labelString.isEmpty()) labelString = "Unnamed category";
                    c.drawText(labelString.toUpperCase(), child.getLeft() + mTextIndent, textY,
                            headerTextPaint);
                    mShadow.setBounds(child.getLeft(), child.getTop() + translationY, child.getRight
                            (), child.getTop() + mShadowHeight + translationY);
                    mShadow.draw(c);
                }
            }
        }

        @Override
        public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
            if (mCategoryColumn < 0 ){
                return;
            }
            int position = parent.getChildAdapterPosition(view);

            if (isHeaderPosition(position, null)) {
                outRect.set(0, mHeading.getIntrinsicHeight(), 0, 0);
            }
        }

        private boolean isHeaderPosition(int position, StringBuilder headerLabel){
            boolean isHeaderRow = false;
            int count = 0;
            for (TreeMap.Entry<String, ArrayList<TableRowView>> entry : mVisibleRowsCategory.entrySet()) {
                if (position == count) {
                    isHeaderRow = true;
                    if (headerLabel != null){
                        headerLabel.append(entry.getKey());
                    }
                    return true;
                }
                count += entry.getValue().size();
            }
            return false;
        }

//        @Override
//        public void onDraw(Canvas c, RecyclerView parent, RecyclerView.State state) {
//            int left = parent.getPaddingLeft();
//            int right = parent.getWidth() - parent.getPaddingRight();
//
//            String groupName = "";
//            int childCount = parent.getChildCount();
//            for (int i = 0; i < childCount; i++) {
//                View child = parent.getChildAt(i);
//                int translationY = (int)(child.getTranslationY() + 0.5f);
//                TableRow row = mRows.get(i);
//
//                RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) child.getLayoutParams();
//
//                int top = child.getBottom() + params.bottomMargin + translationY;
//                int bottom = top + mDivider.getIntrinsicHeight() + translationY;
//
//                mDivider.setBounds(left, top, right, bottom);
//                mDivider.draw(c);
//            }
//        }
    }
}
