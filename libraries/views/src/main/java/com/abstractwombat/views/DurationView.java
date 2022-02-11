package com.abstractwombat.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.os.Build;
import android.support.constraint.ConstraintLayout;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

/**
 * TODO: document your custom view class.
 */
public class DurationView extends ConstraintLayout {
    private static String TAG = "DurationView";

    private int mTime;
    private String mHourLabel;
    private String mMinuteLabel;
    private String mSecondLabel;

    private TextView mTextViewHours;
    private TextView mTextViewMinutes;
    private TextView mTextViewSeconds;
    private TextView mTextViewHoursLabel;
    private TextView mTextViewMinutesLabel;
    private TextView mTextViewSecondsLabel;

    public DurationView(Context context) {
        super(context);
        init(context, null, 0);
    }

    public DurationView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs, 0);
    }

    public DurationView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs, defStyle);
    }

    private void init(Context context, AttributeSet attrs, int defStyle) {
        Log.d(TAG, "init");
        // Create the View
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View parent = inflater.inflate(R.layout.duration_view, this, false);
        mTextViewHours = (TextView) parent.findViewById(R.id.textViewHours);
        mTextViewMinutes = (TextView) parent.findViewById(R.id.textViewMinutes);
        mTextViewSeconds = (TextView) parent.findViewById(R.id.textViewSeconds);
        mTextViewHoursLabel = (TextView) parent.findViewById(R.id.textViewHoursLabel);
        mTextViewMinutesLabel = (TextView) parent.findViewById(R.id.textViewMinutesLabel);
        mTextViewSecondsLabel = (TextView) parent.findViewById(R.id.textViewSecondsLabel);
        addView(parent);

        // Load attributes
        final TypedArray a = getContext().obtainStyledAttributes(
                attrs, R.styleable.DurationView, defStyle, 0);

        setTime(a.getInteger(R.styleable.DurationView_timeSeconds, 0));
        if (a.hasValue(R.styleable.DurationView_hourLabel)) {
            setHourLabel(a.getString(R.styleable.DurationView_hourLabel));
        }
        if (a.hasValue(R.styleable.DurationView_minuteLabel)) {
            setMinuteLabel(a.getString(R.styleable.DurationView_minuteLabel));
        }
        if (a.hasValue(R.styleable.DurationView_secondLabel)) {
            setSecondLabel(a.getString(R.styleable.DurationView_secondLabel));
        }

        // Value text appearance
        if (a.hasValue(R.styleable.DurationView_textAppearanceValues)) {
            Log.d(TAG, "    found textAppearanceValues");
            TypedValue valueTextAppearance = new TypedValue();
            a.getValue(R.styleable.DurationView_textAppearanceValues, valueTextAppearance);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                mTextViewHours.setTextAppearance(valueTextAppearance.data);
                mTextViewMinutes.setTextAppearance(valueTextAppearance.data);
                mTextViewSeconds.setTextAppearance(valueTextAppearance.data);
            }else{
                mTextViewHours.setTextAppearance(context, valueTextAppearance.data);
                mTextViewMinutes.setTextAppearance(context, valueTextAppearance.data);
                mTextViewSeconds.setTextAppearance(context, valueTextAppearance.data);
            }
        }

        // Label text appearance
        if (a.hasValue(R.styleable.DurationView_textAppearanceLabels)) {
            Log.d(TAG, "    found textAppearanceLabels");
            TypedValue labelTextAppearance = new TypedValue();
            a.getValue(R.styleable.DurationView_textAppearanceLabels, labelTextAppearance);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                mTextViewHoursLabel.setTextAppearance(labelTextAppearance.data);
                mTextViewMinutesLabel.setTextAppearance(labelTextAppearance.data);
                mTextViewSecondsLabel.setTextAppearance(labelTextAppearance.data);
            }else{
                mTextViewHoursLabel.setTextAppearance(context, labelTextAppearance.data);
                mTextViewMinutesLabel.setTextAppearance(context, labelTextAppearance.data);
                mTextViewSecondsLabel.setTextAppearance(context, labelTextAppearance.data);
            }
        }

        // Value text size
        if (a.hasValue(R.styleable.DurationView_textSizeValues)) {
            int valueTextSize = a.getDimensionPixelSize(R.styleable.DurationView_textSizeValues, 18);
            Log.d(TAG, "    found textSizeValues=" + valueTextSize);
            mTextViewHours.setTextSize(TypedValue.COMPLEX_UNIT_PX, valueTextSize);
            mTextViewMinutes.setTextSize(TypedValue.COMPLEX_UNIT_PX, valueTextSize);
            mTextViewSeconds.setTextSize(TypedValue.COMPLEX_UNIT_PX, valueTextSize);
        }

        // Label text size
        if (a.hasValue(R.styleable.DurationView_textSizeLabels)) {
            int labelTextSize = a.getDimensionPixelSize(R.styleable.DurationView_textSizeLabels, 10);
            Log.d(TAG, "    found textSizeLabels=" + labelTextSize);
            mTextViewHoursLabel.setTextSize(TypedValue.COMPLEX_UNIT_PX, labelTextSize);
            mTextViewMinutesLabel.setTextSize(TypedValue.COMPLEX_UNIT_PX, labelTextSize);
            mTextViewSecondsLabel.setTextSize(TypedValue.COMPLEX_UNIT_PX, labelTextSize);
        }

        // Value text color
        if (a.hasValue(R.styleable.DurationView_textColorValues)) {
            int valueTextColor = a.getColor(R.styleable.DurationView_textColorValues, Color.BLACK);
            Log.d(TAG, "    found textColorValues=" + valueTextColor);
            mTextViewHours.setTextColor(valueTextColor);
            mTextViewMinutes.setTextColor(valueTextColor);
            mTextViewSeconds.setTextColor(valueTextColor);
        }

        // Label text size
        if (a.hasValue(R.styleable.DurationView_textColorLabels)) {
            int labelTextColor = a.getColor(R.styleable.DurationView_textColorLabels, Color.BLACK);
            Log.d(TAG, "    found textColorLabels=" + labelTextColor);
            mTextViewHoursLabel.setTextColor(labelTextColor);
            mTextViewMinutesLabel.setTextColor(labelTextColor);
            mTextViewSecondsLabel.setTextColor(labelTextColor);
        }

        a.recycle();

    }


    public int getTime() {
        return mTime;
    }

    public void setTime(int seconds) {
        if (seconds < 0){
            this.mTime = 0;
        }else {
            this.mTime = seconds;
        }
        updateTime(mTime);
    }

    public String getHourLabel() {
        return mHourLabel;
    }

    public void setHourLabel(String mHourLabel) {
        this.mHourLabel = mHourLabel;
    }

    public String getMinuteLabel() {
        return mMinuteLabel;
    }

    public void setMinuteLabel(String mMinuteLabel) {
        this.mMinuteLabel = mMinuteLabel;
    }

    public String getSecondLabel() {
        return mSecondLabel;
    }

    public void setSecondLabel(String mSecondLabel) {
        this.mSecondLabel = mSecondLabel;
    }

    private void updateTime(int durationInSeconds){
        int totalSeconds = durationInSeconds;
        int hours = totalSeconds / (60*60);
        int remaining = totalSeconds - (hours * 60 * 60);
        int minutes = remaining / 60;
        remaining = remaining - (minutes * 60);
        int seconds = remaining;

        mTextViewHours.setVisibility(VISIBLE);
        mTextViewHoursLabel.setVisibility(VISIBLE);
        mTextViewMinutes.setVisibility(VISIBLE);
        mTextViewMinutesLabel.setVisibility(VISIBLE);

        if (hours == 0) {
            mTextViewHours.setVisibility(GONE);
            mTextViewHoursLabel.setVisibility(GONE);
            if (minutes == 0){
                mTextViewMinutes.setVisibility(GONE);
                mTextViewMinutesLabel.setVisibility(GONE);
            }
        }

        mTextViewHours.setText(Integer.toString(hours));
        String minString = Integer.toString(minutes);
        if (minutes < 10) minString = "0" + minString;
        mTextViewMinutes.setText(minString);
        String secString = Integer.toString(seconds);
        if (seconds < 10) secString = "0" + secString;
        mTextViewSeconds.setText(secString);
    }

}
