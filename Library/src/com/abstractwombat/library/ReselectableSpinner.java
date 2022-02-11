package com.abstractwombat.library;

import java.lang.reflect.Method;
import android.content.Context;
import android.content.DialogInterface;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.AdapterView;
import android.widget.Spinner;

public class ReselectableSpinner extends Spinner {

	public ReselectableSpinner(Context context){ super(context); }
	public ReselectableSpinner(Context context, AttributeSet attrs) { super(context, attrs); }
	public ReselectableSpinner(Context context, AttributeSet attrs, int defStyle) { super(context, attrs, defStyle); }

	@Override
	public void	setSelection(int position, boolean animate) {
		boolean sameSelected = position == getSelectedItemPosition();
		super.setSelection(position, animate);
		if (sameSelected) {
			// Spinner does not call the OnItemSelectedListener if the same item is selected, so do it manually now
			final OnItemSelectedListener listener = getOnItemSelectedListener();
			if (listener != null){
				listener.onItemSelected(this, getSelectedView(), position, getSelectedItemId());
			}
		}
	}

	@Override
	public void setSelection(int position) {
		boolean sameSelected = position == getSelectedItemPosition();
		super.setSelection(position);
		if (sameSelected) {
			// Spinner does not call the OnItemSelectedListener if the same item is selected, so do it manually now
			final OnItemSelectedListener listener = getOnItemSelectedListener();
			if (listener != null){
				listener.onItemSelected(this, getSelectedView(), position, getSelectedItemId());
			}
		}
	}

}