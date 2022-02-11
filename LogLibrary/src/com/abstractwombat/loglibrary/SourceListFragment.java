package com.abstractwombat.loglibrary;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Context;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.widget.SwitchCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class SourceListFragment extends Fragment implements View.OnClickListener, CompoundButton
        .OnCheckedChangeListener {
	private static final String TAG = "SourceListFragment";
    private View mRootView;

    public interface SourceListListener {
        boolean onLockedSourceTouched(ALogSource source);
        void onViewCreated();
    }

    public interface OptionSelectedListener {
        boolean onOptionTouched(int option);
    }

    /**
     * Create a new instance of SourceListFragment
     */
    public static SourceListFragment newInstance(int groupId) {
        SourceListFragment f = new SourceListFragment();
        Bundle args = new Bundle();
		args.putInt("GroupId", groupId);
        f.setArguments(args);
        return f;
    }

	private Context context;
	private int groupId;
	private LinearLayout sourceContainer;
    private SourceListListener mSourceListListener;
    private OptionSelectedListener mOptionSelectedListener;
    private List<SourceView> mSourceViews;
    private boolean mAutoConfigureOnFailedEnable;

    private class SourceView{
        public View root;
        public ImageView icon;
        public TextView label;
        public TextView details;
        public Button settings;
        public SwitchCompat enable;
        public ImageButton lock;
        public ProgressBar progress;

        @Override
        public boolean equals(Object object) {
            Log.d(TAG, "SourceView equals");
            return this.label.getText().toString().equals(((SourceView)object).label.getText().toString());
        }
    }
	@Override
    public void onAttach(Activity activity) {
		super.onAttach(activity);
		this.context = activity;
        if (activity instanceof SourceListListener) {
            mSourceListListener = (SourceListListener) activity;
        }
        if (activity instanceof OptionSelectedListener) {
            mOptionSelectedListener = (OptionSelectedListener) activity;
        }
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        mRootView = inflater.inflate(R.layout.source_list, container, false);
        return mRootView;
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState){
        super.onViewCreated(view, savedInstanceState);
        Log.d(TAG, "onViewCreated");
        // Restore the lock state
        if (mSourceViews != null && savedInstanceState != null) {
            Log.d(TAG, "Restoring lock state");
            for (SourceView sourceView : mSourceViews) {
                int lockVis = savedInstanceState.getInt(sourceView.label.getText().toString() + ".lock");
                if (lockVis == View.GONE) sourceView.lock.setVisibility(View.GONE);
                else sourceView.lock.setVisibility(View.VISIBLE);

                int progressVis = savedInstanceState.getInt(sourceView.label.getText().toString() + ".progress");
                if (progressVis == View.GONE) sourceView.progress.setVisibility(View.GONE);
                else sourceView.progress.setVisibility(View.VISIBLE);
            }
        }
	}

    @Override
    public void onSaveInstanceState(Bundle outState) {
        Log.d(TAG, "onSaveInstanceState");
        // Save the lock state
        if (mSourceViews != null) {
            Log.d(TAG, "Saving lock state");
            for (SourceView sourceView : mSourceViews) {
                outState.putInt(sourceView.label.getText().toString() + ".lock", sourceView.lock.getVisibility());
                outState.putInt(sourceView.label.getText().toString() + ".progress", sourceView
                        .progress.getVisibility());
            }
        }
        super.onSaveInstanceState(outState);
    }

    private void createSourceList(){
        Log.d(TAG, "createSourceList");
        if (mSourceViews == null) {
            Log.d(TAG, "mSourceViews was null");
            mSourceViews = new ArrayList<>();
        }

        ArrayList<ALogSource> itemList = getSources();
        if (this.sourceContainer.getChildCount() == itemList.size()){
            return;
        }

        Collections.sort(itemList, new Comparator<ALogSource>() {
            @Override
            public int compare(ALogSource lhs, ALogSource rhs) {
                String lLabel = LogSourceFactory.getSourceLabel(context, lhs.config().sourceID);
                String rLabel = LogSourceFactory.getSourceLabel(context, rhs.config().sourceID);
                return lLabel.compareTo(rLabel);
            }
        });

        this.sourceContainer.removeAllViews();
        for (ALogSource source : itemList){
            if (source == null || source instanceof CombinedLogSource) continue;
            SourceView sourceView = createSourceView(source, this.sourceContainer);
            ViewGroup parentGroup = (ViewGroup) sourceView.root.getParent();
            if (parentGroup != null){
                Log.d(TAG, "Removing view from parent");
                parentGroup.removeView(sourceView.root);
            }
            if (sourceContainer.indexOfChild(sourceView.root) == -1) {
                mSourceViews.add(sourceView);
                sourceView.root.setTag(source);
                sourceView.root.setOnClickListener(this);
                this.sourceContainer.addView(sourceView.root);
            }
        }
        if (mSourceListListener != null){
            mSourceListListener.onViewCreated();
        }
    }

    private SourceView createSourceView(final ALogSource source, View parent) {
        SourceView sourceView = null;

        int lockState = 2;
        if (mSourceViews != null){
            String labelString = LogSourceFactory.getSourceLabel(context, source.config().sourceID);
            SourceView tempSourceView = getSourceView(labelString);
            if (tempSourceView != null) {
                Log.d(TAG, "Reusing source view");
                sourceView = tempSourceView;
            }
        }
        if (Licensing.isLicensed(source, context)){
            lockState = 0;
        }

        if (sourceView == null) {
            // Inflate the View
            LayoutInflater inflater = (LayoutInflater) this.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            sourceView = new SourceView();
            sourceView.root = inflater.inflate(R.layout.source_list_item, null);

            // Get all the child views
            sourceView.icon = (ImageView) sourceView.root.findViewById(R.id.source_icon);
            sourceView.label = (TextView) sourceView.root.findViewById(R.id.source_label);
            //sourceView.details = (TextView) sourceView.root.findViewById(R.id.source_details);
            sourceView.settings = (Button) sourceView.root.findViewById(R.id.source_settings);
            sourceView.enable = (SwitchCompat) sourceView.root.findViewById(R.id.source_switch);
            sourceView.lock = (ImageButton) sourceView.root.findViewById(R.id.source_lock);
            sourceView.progress = (ProgressBar) sourceView.root.findViewById(R.id.source_progress);
        }

        // Don't auto configure sources
        mAutoConfigureOnFailedEnable = false;

        //Set the label
        String labelString = LogSourceFactory.getSourceLabel(context, source.config().sourceID);
        sourceView.label.setText(labelString);
        Log.d(TAG, "Creating source labeled '" + labelString + "'");
        //Set the details
        //sourceView.details.setText(source.config().getSummary());
        // Setup the settings button
        sourceView.settings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ALogSourcePreferenceFragment f = LogSourceFactory.getSourceFragment(context,
                        source.config().sourceID);
                configureSource(f, source.config().sourceID);
            }
        });
        // Set the lock state to indeterminate
        updateLockState(source, lockState);
//        if (setLockStateToIndeterminate) {
//            sourceView.lock.setVisibility(View.GONE);
//            sourceView.progress.setVisibility(View.VISIBLE);
//        }
        // Set the switch state
        sourceView.enable.setOnCheckedChangeListener(this);
        //sourceView.enable.setOnClickListener(this);
        sourceView.enable.setTag(source.config().sourceID);
        boolean sourceEnabled = (source.config().count != 0);
        if (source instanceof NotificationSource){
            sourceEnabled &= ((NotificationSource)source).enabled();
        }
        sourceView.enable.setChecked(sourceEnabled);
        // Set the icon
        int iconRes = LogSourceFactory.getSourceIcon(context, source.config().sourceID);
        if (iconRes != 0) {
            sourceView.icon.setImageResource(iconRes);
        }

        // If the user enables a source, we want to auto configure
        mAutoConfigureOnFailedEnable = true;

        return sourceView;
    }

    // Unlocked         State==0
    // Locked           State==1
    // Indeterminate    State==2
    public void updateLockState(ALogSource source, int state){
        String sourceLabel = LogSourceFactory.getSourceLabel(context, source.config().sourceID);
        Log.d(TAG, "Setting source '" + sourceLabel + "' lock state to " + state);

        SourceView sourceView = getSourceView(sourceLabel);
        if (sourceView == null) return;

        Log.d(TAG, "Setting source labeled \"" + sourceLabel + "\" to state " + state);
        if (state == 0) {
            // Unlocked
            sourceView.lock.setVisibility(View.GONE);
            sourceView.progress.setVisibility(View.GONE);
        }else if(state == 1){
            // Locked
            sourceView.lock.setVisibility(View.VISIBLE);
            sourceView.lock.setTag(source);
            sourceView.lock.setOnClickListener(this);
            sourceView.progress.setVisibility(View.GONE);
            sourceView.enable.setChecked(false);
        }else{
            // Indeterminate
            sourceView.lock.setVisibility(View.GONE);
            sourceView.progress.setVisibility(View.VISIBLE);
        }
    }

    public void enabledUnlockedSources(){
        // Don't auto configure sources
        mAutoConfigureOnFailedEnable = false;

        for (SourceView s : mSourceViews){
            if (s.lock.getVisibility() != View.VISIBLE){
                ALogSource source = (ALogSource) s.root.getTag();
                if (source != null) {
                    s.enable.setChecked(true);
                }
            }
        }

        // If the user enables a source, we want to auto configure
        mAutoConfigureOnFailedEnable = true;
    }

    public void setCheckState(ALogSource source, boolean checked){
        String sourceLabel = LogSourceFactory.getSourceLabel(context, source.config().sourceID);
        Log.d(TAG, "Setting source '" + sourceLabel + "' check state to " + checked);

        SourceView sourceView = getSourceView(sourceLabel);
        if (sourceView == null) return;
        sourceView.enable.setChecked(checked);
    }

    public boolean allDisabled() {
        boolean allDisabled = true;
        for (SourceView s : mSourceViews){
            if (s.enable.isChecked()){
                allDisabled = false;
                break;
            }
        }
        return allDisabled;
    }

    private SourceView getSourceView(String sourceLabel) {
        SourceView sourceView = null;
        for (SourceView s : mSourceViews){
            if (s.label.getText().toString().equals(sourceLabel)){
                sourceView = s;
                break;
            }
        }
        if (sourceView == null){
            Log.d(TAG, "Couldn't find sourceView");
            return null;
        }
        return sourceView;
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        Log.d(TAG, "onCheckedChanged");
        String sourceId = (String) buttonView.getTag();
        ALogSource source = LogSourceFactory.get(context, sourceId);
        LogSourceConfig config = source.config();
        Class<?> sourceClass = source.getClass();

        boolean isCheckedFollowThrough = isChecked;

        // Check if this source can be enabled
        if (isChecked && source instanceof NotificationSource && !((NotificationSource)source).enabled()){
            if (mAutoConfigureOnFailedEnable) {
                // Launch configuration fragment if needed
                Log.d(TAG, "Auto configuring source since it's can't be enabled");
                NotificationSource nSource = (NotificationSource) source;
                Toast.makeText(context, getResources().getString(R.string.notitication_access_toast), Toast.LENGTH_LONG).show();
                configureSource(LogSourceFactory.getSourceFragment(context, config.sourceID), config.sourceID);
            }
            Log.d(TAG, "Source can't be enabled, disabling");

            // Turn off the switch
            buttonView.setChecked(false);
            isCheckedFollowThrough = false;
        }

        // Set the count
        if (isCheckedFollowThrough) {
            config.count = getResources().getInteger(R.integer.combined_source_default_count) / 2;
        }else{
            config.count = 0;
        }

        // Setup the image color
        View parent = (View)buttonView.getParent();
        View imageView = null;
        while (parent != null && imageView == null){
            View p = parent.findViewById(R.id.source_icon);
            if (p != null) imageView = (ImageView) p;
            parent = (View) parent.getParent();
        }
        if (imageView != null){
            enableImage((ImageView) imageView, isCheckedFollowThrough);
        }

        // Delete the source
        LogSourceFactory.deleteSource(context, source.config().sourceID);
        // Create the new source
        LogSourceFactory.newSource(context, sourceClass, config);

    }

    private void enableImage(ImageView view, boolean enable){
        Log.d(TAG, "Changing image color: " + enable);
        if (enable){
            view.clearColorFilter();
        }else{
            ColorMatrix matrix = new ColorMatrix();
            matrix.setSaturation(0);
            ColorMatrixColorFilter filter = new ColorMatrixColorFilter(matrix);
            view.setColorFilter(filter);
        }
    }

	private ArrayList<ALogSource> getSources() {
        // Get all the sources for this widget out of the factory
        ALogSource[] sources = LogSourceFactory.get(this.context, this.groupId);
		ArrayList<ALogSource> itemList = new ArrayList<ALogSource>(Arrays.asList(sources));
        return itemList;
	}

    @Override
    public void onResume() {
		super.onResume();
        Log.d(TAG, "onResume");
        mAutoConfigureOnFailedEnable = false;
        this.groupId = getArguments().getInt("GroupId");
        this.sourceContainer = (LinearLayout) getView().findViewById(R.id.source_container);
        addOnCheckedListener((ViewGroup) getView());

        LinearLayout option1 = (LinearLayout) getView().findViewById(R.id.option_background);
        option1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mOptionSelectedListener != null){
                    mOptionSelectedListener.onOptionTouched(1);
                }
            }
        });
        LinearLayout option2 = (LinearLayout) getView().findViewById(R.id.option_spacing);
        option2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mOptionSelectedListener != null){
                    mOptionSelectedListener.onOptionTouched(2);
                }
            }
        });
        LinearLayout option3 = (LinearLayout) getView().findViewById(R.id.option_direction);
        option3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mOptionSelectedListener != null){
                    mOptionSelectedListener.onOptionTouched(3);
                }
            }
        });
        LinearLayout option4 = (LinearLayout) getView().findViewById(R.id.option_save);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            option4.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (mOptionSelectedListener != null) {
                        mOptionSelectedListener.onOptionTouched(4);
                    }
                }
            });
        }else{
            option4.setVisibility(View.GONE);
        }
        LinearLayout option5 = (LinearLayout) getView().findViewById(R.id.option_settingsstyle);
        option5.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mOptionSelectedListener != null){
                    mOptionSelectedListener.onOptionTouched(5);
                }
            }
        });

        createSourceList();
        addOnCheckedListener((ViewGroup) getView());
        mAutoConfigureOnFailedEnable = true;
	}

    public void setOptionImage(int option, int res){
        Log.d(TAG, "setOptionImage " + option);
        if (mRootView == null){
            Log.d(TAG, "    - view not created yet");
            return;
        }
        ImageView view = null;
        switch(option){
            case 1:
                view = (ImageView)mRootView.findViewById(R.id.option1_image);
                break;
            case 2:
                view = (ImageView)mRootView.findViewById(R.id.option2_image);
                break;
            case 3:
                view = (ImageView)mRootView.findViewById(R.id.option3_image);
                break;
            case 4:
                view = (ImageView)mRootView.findViewById(R.id.option4_image);
                break;
            case 5:
                view = (ImageView)mRootView.findViewById(R.id.option5_image);
                break;
        }
        if (view != null) {
            view.setImageResource(res);
            Log.d(TAG, "    - image set");
        }else{
            Log.d(TAG, "    - failed to find ImageView");
        }
    }

    @Override
    public void onViewStateRestored(Bundle savedInstanceState) {
        Log.d(TAG, "onViewStateRestored");
        super.onViewStateRestored(savedInstanceState);
    }

    @Override
	public void onPause() {
        super.onPause();
        removeOnCheckedListener((ViewGroup) getView());
		Log.d(TAG, "onPause");
	}

    private void removeOnCheckedListener(ViewGroup view) {
        for (int i = 0;i < view.getChildCount();i++) {
            View child = view.getChildAt(i);
            try {
                ViewGroup viewGroup = (ViewGroup) child;
                removeOnCheckedListener(viewGroup);
            } catch (ClassCastException e) {
                //just ignore the exception - it is used as a check
            }
            if (child instanceof SwitchCompat){
                ((SwitchCompat)child).setOnCheckedChangeListener(null);
            }
        }
    }
    private void addOnCheckedListener(ViewGroup view) {
        for (int i = 0;i < view.getChildCount();i++) {
            View child = view.getChildAt(i);
            try {
                ViewGroup viewGroup = (ViewGroup) child;
                addOnCheckedListener(viewGroup);
            } catch (ClassCastException e) {
                //just ignore the exception - it is used as a check
            }
            if (child instanceof SwitchCompat){
                ((SwitchCompat)child).setOnCheckedChangeListener(this);
            }
        }
    }

    private boolean isLocked(ALogSource source){
        String labelString = LogSourceFactory.getSourceLabel(context, source.config().sourceID);
        for (SourceView sourceView : mSourceViews){
            if (sourceView.label.getText().toString().equals(labelString)){
                return sourceView.lock.getVisibility() == View.VISIBLE;
            }
        }
        return true;
    }

    @Override
    public void onClick(View v) {
        ALogSource source = (ALogSource) v.getTag();
        boolean toggleSource = false;
        if (!isLocked(source)){
            toggleSource = true;
        }else{
            Log.d(TAG, "Source is locked");
            if (mSourceListListener != null){
                if (mSourceListListener.onLockedSourceTouched(source)){
                    toggleSource = true;
                }
            }
        }
        if (toggleSource) {
            SwitchCompat enable = (SwitchCompat)v.findViewById(R.id.source_switch);
            String labelString = LogSourceFactory.getSourceLabel(context, source.config().sourceID);
            SourceView sView = getSourceView(labelString);
            enable.setChecked(!enable.isChecked());
        }
    }

	private void configureSource(Fragment f, String sourceId) {
        if (f == null) return;
        // Get the parent container
        int parentId = ((View) getView().getParent()).getId();
        // Replace this view with a child
        FragmentTransaction trans = getFragmentManager().beginTransaction();
        trans.replace(parentId, f, "SourceConfig");
        trans.addToBackStack(LogSourceFactory.getSourceLabel(context, sourceId));
        trans.commit();
    }
}
