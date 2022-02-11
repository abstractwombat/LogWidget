package com.abstractwombat.views;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.DialogInterface;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ArrayAdapter;

import java.util.ArrayList;
import java.util.List;

public class SearchableSpinner extends android.support.v7.widget.AppCompatSpinner implements View.OnTouchListener,
        SearchableListDialog.SearchableItem {

    private Context _context;
    private List _items;
    private SearchableListDialog _searchableListDialog;
    private RecentItemGenerator _recentItemGenerator;

    public interface RecentItemGenerator{
        List<Object> getRecentItems();
    }

    public SearchableSpinner(Context context) {
        super(context);
        this._context = context;
        init();
    }

    public SearchableSpinner(Context context, AttributeSet attrs) {
        super(context, attrs);
        this._context = context;
        init();
    }

    public SearchableSpinner(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this._context = context;
        init();
    }

    public void addRecentItemGenerator(RecentItemGenerator generator){
        _recentItemGenerator = generator;
    }

    private void init() {
        _items = new ArrayList();
        _searchableListDialog = SearchableListDialog.newInstance
                (_items);
        _searchableListDialog.setOnSearchableItemClickListener(this);
        setOnTouchListener(this);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_UP) {
            ArrayAdapter adapter = (ArrayAdapter) getAdapter();

            if (null != adapter) {

                // Refresh content #6
                // Change Start
                // Description: The items were only set initially, not reloading the data in the
                // spinner every time it is loaded with items in the adapter.
                _items.clear();
                for (int i = 0; i < adapter.getCount(); i++) {
                    _items.add(adapter.getItem(i));
                }
                // Change end.

                if (_recentItemGenerator != null){
                    _searchableListDialog.setRecentItems(_recentItemGenerator.getRecentItems());
                }
                _searchableListDialog.show(scanForActivity(_context).getFragmentManager(), "TAG");
            }
        }
        return true;
    }

    @Override
    public void onSearchableItemClicked(Object item, int position) {
        setSelection(_items.indexOf(item));
//        View view = getRootView();
//        if (view != null) {
//            InputMethodManager imm = (InputMethodManager)_context.getSystemService(Context.INPUT_METHOD_SERVICE);
//            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
//        }
    }

    public void setTitle(String strTitle) {
        _searchableListDialog.setTitle(strTitle);
    }

    public void setPositiveButton(String strPositiveButtonText) {
        _searchableListDialog.setPositiveButton(strPositiveButtonText);
    }

    public void setPositiveButton(String strPositiveButtonText, DialogInterface.OnClickListener
            onClickListener) {
        _searchableListDialog.setPositiveButton(strPositiveButtonText, onClickListener);
    }

    private Activity scanForActivity(Context cont) {
        if (cont == null)
            return null;
        else if (cont instanceof Activity)
            return (Activity) cont;
        else if (cont instanceof ContextWrapper)
            return scanForActivity(((ContextWrapper) cont).getBaseContext());

        return null;
    }
}