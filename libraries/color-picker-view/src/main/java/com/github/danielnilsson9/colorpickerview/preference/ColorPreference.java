/*
 * Copyright (C) 2015 Daniel Nilsson
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.danielnilsson9.colorpickerview.preference;

import android.app.Activity;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.res.TypedArray;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.View;

import com.github.danielnilsson9.colorpickerview.R;
import com.github.danielnilsson9.colorpickerview.dialog.ColorPickerDialogFragment;
import com.github.danielnilsson9.colorpickerview.view.ColorPanelView;

public class ColorPreference extends Preference implements ColorPickerDialogFragment.ColorPickerDialogListener {


	public interface OnShowDialogListener {
		public void onShowColorPickerDialog(String title, int currentColor);
	}

	private OnShowDialogListener mListener;

	private int mColor = 0xFF000000;
	private FragmentManager mFragmentManager;

	private boolean		mShowAlphaPanel;
	private String		mAlphaSliderText;
	private int 		mSliderTrackerColor;
	private int 		mBorderColor;

	public ColorPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(attrs);
	}

	public ColorPreference(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init(attrs);

	}

	private void init(AttributeSet attrs) {
		setPersistent(true);

		setWidgetLayoutResource(R.layout.colorpickerview__preference_preview_layout);

		setOnPreferenceClickListener(new OnPreferenceClickListener() {

			@Override
			public boolean onPreferenceClick(Preference preference) {

				if (mListener != null) {
					mListener.onShowColorPickerDialog((String) getTitle(), mColor);
				} else {
					showDialog();
				}
				return true;
			}
		});

		TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.colorpickerview__ColorPickerView);
		mShowAlphaPanel = a.getBoolean(R.styleable.colorpickerview__ColorPickerView_colorpickerview__alphaChannelVisible, false);
		mAlphaSliderText = a.getString(R.styleable.colorpickerview__ColorPickerView_colorpickerview__alphaChannelText);
		mSliderTrackerColor = a.getColor(R.styleable.colorpickerview__ColorPickerView_colorpickerview__sliderColor, 0xFFBDBDBD);
		mBorderColor = a.getColor(R.styleable.colorpickerview__ColorPickerView_colorpickerview__borderColor, 0xFF6E6E6E);
		a.recycle();
	}

	private void showDialog() {
		if (mListener == null) {
			Activity activity = (Activity) getContext();

			ColorPickerDialogFragment dialog = ColorPickerDialogFragment
					.newInstance(0, "Color Picker", null, mColor, mShowAlphaPanel);
			dialog.setStyle(DialogFragment.STYLE_NORMAL, 0);
			dialog.show(activity.getFragmentManager(), "pre_dialog");
			dialog.setListener(this);
		}
		if (mListener != null) {
			mListener.onShowColorPickerDialog((String) getTitle(), mColor);
		}
	}

	/**
	 * Since the color picker dialog is now a DialogFragment
	 * this preference cannot take care of showing it without
	 * access to the fragment manager. Therefore I leave it up to
	 * you to actually show the dialog once the preference is clicked.
	 *
	 * Call saveValue() once you have a color to save.
	 * @param listener
	 */
	public void setOnShowDialogListener(OnShowDialogListener listener) {
		mListener = listener;
	}


	@Override
	protected void onBindView(View view) {
		super.onBindView(view);

		ColorPanelView preview = (ColorPanelView) view.findViewById(R.id.colorpickerview__preference_preview_color_panel);

		if(preview != null) {
			preview.setColor(mColor);
		}

	}

	@Override
	protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
		if(restorePersistedValue) {
			mColor = getPersistedInt(0xFF000000);
			//Log.d("mColorPicker", "Load saved color: " + mColor);
		}
		else {
			mColor = (Integer)defaultValue;
			persistInt(mColor);
		}
	}

	@Override
	public void setDefaultValue(Object defaultValue) {
		super.setDefaultValue(defaultValue);
		mColor = (int)defaultValue;
	}

	@Override
	protected Object onGetDefaultValue(TypedArray a, int index) {
		return a.getInteger(index, 0xFF000000);
	}


	public void saveValue(int color) {
		mColor = color;
		persistInt(mColor);
		notifyChanged();
	}

	public int getValue(){
		return mColor;
	}

	@Override
	public void onColorSelected(int dialogId, int color) {
		saveValue(color);
	}

	@Override
	public void onDialogDismissed(int dialogId) {}

}
