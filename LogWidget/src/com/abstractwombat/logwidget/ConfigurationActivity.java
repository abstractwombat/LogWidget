package com.abstractwombat.logwidget;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.text.InputType;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.util.Xml;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.MultiAutoCompleteTextView;
import android.widget.TextView;
import android.widget.Toast;

import com.abstractwombat.contacts.ContactUtilities;
import com.abstractwombat.images.ImageCache;
import com.abstractwombat.images.ImageUtilities;
import com.abstractwombat.library.RuntimePermissions;
import com.abstractwombat.loglibrary.ALogSource;
import com.abstractwombat.loglibrary.ALogSourcePreferenceFragment;
import com.abstractwombat.loglibrary.CallLogSource;
import com.abstractwombat.loglibrary.CallLogSourceConfig;
import com.abstractwombat.loglibrary.CombinedLogSource;
import com.abstractwombat.loglibrary.CombinedLogSourceConfig;
import com.abstractwombat.loglibrary.EmailSource;
import com.abstractwombat.loglibrary.EmailSourceConfig;
import com.abstractwombat.loglibrary.FacebookMessengerSource;
import com.abstractwombat.loglibrary.FacebookMessengerSourceConfig;
import com.abstractwombat.loglibrary.HangoutsSource;
import com.abstractwombat.loglibrary.HangoutsSourceConfig;
import com.abstractwombat.loglibrary.LicenseTrial;
import com.abstractwombat.loglibrary.Licensing;
import com.abstractwombat.loglibrary.LogSourceConfig;
import com.abstractwombat.loglibrary.LogSourceFactory;
import com.abstractwombat.loglibrary.NotificationSource;
import com.abstractwombat.loglibrary.SMSLogSource;
import com.abstractwombat.loglibrary.SMSLogSourceConfig;
import com.abstractwombat.loglibrary.SkypeSource;
import com.abstractwombat.loglibrary.SkypeSourceConfig;
import com.abstractwombat.loglibrary.SourceListFragment;
import com.abstractwombat.loglibrary.ViberSource;
import com.abstractwombat.loglibrary.ViberSourceConfig;
import com.abstractwombat.loglibrary.WeChatSource;
import com.abstractwombat.loglibrary.WeChatSourceConfig;
import com.abstractwombat.loglibrary.WhatsAppSource;
import com.abstractwombat.loglibrary.WhatsAppSourceConfig;
import com.android.ex.chips.BaseRecipientAdapter;
import com.android.ex.chips.RecipientEditTextView;
import com.android.ex.chips.RecipientEntry;
import com.android.ex.chips.recipientchip.DrawableRecipientChip;
import com.github.danielnilsson9.colorpickerview.dialog.ColorPickerDialogFragment;
//import com.google.android.gms.ads.AdRequest;
//import com.google.android.gms.ads.AdView;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlSerializer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class ConfigurationActivity extends AppCompatActivity implements DialogInterface
        .OnClickListener, View.OnClickListener, FragmentManager.OnBackStackChangedListener, SourceListFragment.SourceListListener, SourceListFragment.OptionSelectedListener,
        Toolbar.OnMenuItemClickListener, RuntimePermissions.Listener {
    private static final String TAG = "ConfigurationActivity";

    private int widgetID;
    private final String STATE_FILE = "State";
    private static final String STATE_WIDGETID = "widgetID";
    private static final String STATE_INITIALCONFIGURATION = "initialConfiguration";
    private static String WIDGET_ID_FILE = "WidgetIDs";
    private static String IAP_VERIFICATION_URL = "http://www.abstractwombat.com/android/verifyPurchase.php";
    private static int PERMISSION_REQEST_CODE = 132;

    private boolean mAdsRemoved;
    private boolean mSourcesUnlocked;
    private boolean mUnlockAll;
    private String mAdsRemovedPrice;
    private String mIsPremiumPrice;
    private String mUnlockAllPrice;

    private Toolbar mToolbar;
    boolean initialConfiguration;
    private Fragment mChildFragment;
    private View okButton;
    private View cancelButton;
    private RecipientEditTextView mChipTextView;
    private boolean mIabSetupSuccess;
    private boolean mPermissionChecked;
    private RuntimePermissions mRuntimePermissions;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);

        mRuntimePermissions = new RuntimePermissions(this, getResources().getString(R.string.app_name));

        setContentView(R.layout.configuration_container);
        LogSourceConfig.defaultTheme = getResources().getInteger(R.integer.default_theme);

        adVisibility(false);

        // Store the build type
        SharedPreferences settings = this.getSharedPreferences(STATE_FILE, Context.MODE_MULTI_PROCESS);
        SharedPreferences.Editor editor = settings.edit();
        String buildTypeKey = getPackageName() + ".BuildType";
        editor.putInt(buildTypeKey, BuildConfig.buildType);
        editor.commit();

        // Setup the toolbar
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        mToolbar.inflateMenu(R.menu.source_menu);
        if (BuildConfig.buildType == 0) {
            // Hide all the menu options
            Menu tMenu = mToolbar.getMenu();
            for (int i=0; i<tMenu.size(); ++i){
                MenuItem item = tMenu.getItem(i);
                if (item.getItemId() == R.id.filter_action || item.getItemId() == R.id.back_color
                        || item.getItemId() == R.id.direction || item.getItemId() == R.id.spacing) {
                    item.setVisible(false);
                }
            }
        }
        mToolbar.setTitle(R.string.app_name);
        mToolbar.setOnMenuItemClickListener(this);
        mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        // Get the widget ID
        this.initialConfiguration = false;
        if (savedInstanceState != null) {
            this.widgetID = savedInstanceState.getInt(STATE_WIDGETID);
            this.initialConfiguration = savedInstanceState.getBoolean(STATE_INITIALCONFIGURATION);
        } else {
            // Get the widget ID
            Intent intent = getIntent();
            this.widgetID = intent.getIntExtra(this.getPackageName() + ".WidgetID",
                    AppWidgetManager.INVALID_APPWIDGET_ID);
            if (this.widgetID == AppWidgetManager.INVALID_APPWIDGET_ID) {
                this.widgetID = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                        AppWidgetManager.INVALID_APPWIDGET_ID);
                if (this.widgetID == AppWidgetManager.INVALID_APPWIDGET_ID) {
                    Log.e(TAG, "No widget ID given!");
                    return;
                }
                this.initialConfiguration = true;
            }
        }
        Log.d(TAG, "Configuring ID:" + widgetID);

        // Initialize the spacing value
        int spacing = loadOption("Spacing", -1);
        if (spacing == -1) LogSourceConfig.setDefaultSpacing(this, widgetID);

        // Setup the OK and Cancel buttons
        this.okButton = findViewById(R.id.button_ok);
        this.cancelButton = findViewById(R.id.button_cancel);
        this.okButton.setOnClickListener(this);
        this.cancelButton.setOnClickListener(this);
        int okVis = View.GONE;
        if (initialConfiguration) {
            okVis = View.VISIBLE;
        }
        this.okButton.setVisibility(okVis);
        this.cancelButton.setVisibility(okVis);

        // Set the result
        Intent resultValue = new Intent();
        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetID);
        setResult(RESULT_CANCELED, resultValue);

        // Get the sources
        ALogSource[] sources = LogSourceFactory.get(this, this.widgetID);
        // If there aren't any sources, create the defaults
        if (sources.length == 0) {
            LogSourceFactory.deleteGroup(this, this.widgetID);
            createDefaultSources();
            sources = LogSourceFactory.get(this, this.widgetID);
        } else {
            // Remove combined source
            for (ALogSource source : sources) {
                if (source.getClass().equals(CombinedLogSource.class)) {
                    LogSourceFactory.deleteSource(this, source.config().sourceID);
                    Log.d(TAG, "Removed CombinedLogSource");
                }
            }
            sources = LogSourceFactory.get(this, this.widgetID);
        }

        getFragmentManager().addOnBackStackChangedListener(this);

        // Find the current child fragment
        String childFragmentTag = "childFragmentTag";
        Fragment currentChildFragment = getFragmentManager().findFragmentByTag(childFragmentTag);
        if (savedInstanceState != null && currentChildFragment != null) {
            this.mChildFragment = currentChildFragment;
            Log.d(TAG, "Restoring Fragment from saved instance state");
        } else {
            if (sources.length == 1) {
                // Create ALogSourcePreferenceFragment to configure the single source
                ALogSourcePreferenceFragment f = LogSourceFactory.getSourceFragment(this,
                        sources[0].config().sourceID);
                this.mChildFragment = (Fragment) (f);
                getFragmentManager().beginTransaction().add(R.id.fragment_container,
                        this.mChildFragment, childFragmentTag).commit();
            } else if (sources.length > 1) {
                // Show the source list fragment
                this.mChildFragment = SourceListFragment.newInstance(widgetID);
                int container = R.id.fragment_container;
                getFragmentManager().beginTransaction().replace(R.id.fragment_container,
                        this.mChildFragment).commit();
            }
        }

        // Get the stored license state
        String unlockAllKey = this.getPackageName() + ".UnlockAll";
        Log.d(TAG, "  mUnlockAll="+mUnlockAll);
        mUnlockAll = settings.getBoolean(unlockAllKey, false);
        String adRemoveKey = this.getPackageName() + ".AdsRemoved";
        mAdsRemoved = settings.getBoolean(adRemoveKey, false);
        String premiumKey = this.getPackageName() + ".IsPremium";
        mSourcesUnlocked = settings.getBoolean(premiumKey, false);
        if (BuildConfig.buildType != 0){
            mSourcesUnlocked = true;
        }
        storeLicensingState(false);
        updateMenu();

        // Setup the filter
        mChipTextView = (RecipientEditTextView) findViewById(R.id.filter);
        mChipTextView.setTokenizer(new MultiAutoCompleteTextView.CommaTokenizer());
        mChipTextView.setAdapter(
                new BaseRecipientAdapter(BaseRecipientAdapter.QUERY_TYPE_EMAIL, this));
        mChipTextView.setHint("Create a filter");
        if (sources.length > 0 && sources[0].config().lookupKeyFilter != null && sources[0].config().lookupKeyFilter.length > 0) {
            long dataId = 0;
            for (String lookupKey : sources[0].config().lookupKeyFilter){
                String name = ContactUtilities.getContactName(lookupKey, this);

                String[] data = ContactUtilities.getContactData(lookupKey, this,
                        new String[]{ContactsContract.Contacts.DISPLAY_NAME_PRIMARY,
                                ContactsContract.Contacts.DISPLAY_NAME_SOURCE,
                                ContactsContract.Contacts._ID,
                                ContactsContract.Contacts.PHOTO_THUMBNAIL_URI});
                String displayName = null;
                int displayNameSource = ContactsContract.DisplayNameSources.UNDEFINED;
                long contactId = 0;
                Uri thumbnailUri = Uri.EMPTY;
                if (data != null && data.length == 4) {
                    displayName = data[0];
                    displayNameSource = Integer.parseInt(data[1]);
                    contactId = Long.parseLong(data[2]);
                    thumbnailUri = Uri.parse(data[3]);
                }
                RecipientEntry entry = RecipientEntry.constructTopLevelEntry(displayName,
                        displayNameSource, displayName,
                        3, "Name", contactId, null, dataId++,
                        thumbnailUri,
                        true, lookupKey);
                mChipTextView.addRecipient(entry);
            }
        }

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        Log.d(TAG, "onSaveInstanceState");
        if (savedInstanceState == null) return;
        savedInstanceState.putInt(STATE_WIDGETID, this.widgetID);
        savedInstanceState.putBoolean(STATE_INITIALCONFIGURATION, this.initialConfiguration);
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public void onStart() {
        Log.d(TAG, "onStart");
        super.onStart();
    }

    @Override
    public void onStop() {
        Log.d(TAG, "onStop");
        FragmentManager fm = getFragmentManager();
        if (fm.getBackStackEntryCount() > 0) {
            Log.d(TAG, "Stopping while the backstack is non-empty... update the widget!");
            // Save
            getFragmentManager().removeOnBackStackChangedListener(this);
            createRootSource();
            updateWidget();
        }
        super.onStop();
    }

    @Override
    public void onResume() {
        Log.d(TAG, "onResume");
        getFragmentManager().addOnBackStackChangedListener(this);
        super.onResume();
        if (!this.initialConfiguration && !mPermissionChecked){
            // Check for permissions
            ArrayList<String> permissions = new ArrayList<>();
            ArrayList<String> permissionLabels = new ArrayList<>();
            getRequiredPermissions(permissions, permissionLabels);
            mRuntimePermissions.startPermissionCheck(permissions, permissionLabels,
                    PERMISSION_REQEST_CODE, this);
        }
    }

    @Override
    public void onPause() {
        Log.d(TAG, "onPause");
        super.onPause();

        FragmentManager fm = getFragmentManager();
        if (fm.getBackStackEntryCount() > 0) {
            Log.d(TAG, "    Backstack not empty... popping");
            fm.popBackStack();
        }

        if (!this.initialConfiguration) {
            createRootSource();

            // If there's a child on the back stack, just let it be popped off by the BackStackChangedListener
            if (fm.getBackStackEntryCount() > 0) {
                return;
            }

            // Save
            getFragmentManager().removeOnBackStackChangedListener(this);
            updateWidget();

            Log.d(TAG, "    Done Configuring Sources");
        }
    }

    @Override
    public boolean onKeyDown(int keycode, KeyEvent e) {
        switch(keycode) {
            case KeyEvent.KEYCODE_MENU:
                mToolbar.showOverflowMenu();
                return true;
        }
        return super.onKeyDown(keycode, e);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.d(TAG, "onOptionsItemSelected");

        if (item.getItemId() == android.R.id.home) {
            FragmentManager fm = getFragmentManager();
            if (fm.getBackStackEntryCount() > 0) {
                fm.popBackStack();
            } else {
                Log.d(TAG, "Done Configuring Sources");
                Intent resultValue = new Intent();
                resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetID);
                setResult(RESULT_OK, resultValue);
                finish();
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean onLockedSourceTouched(final ALogSource source) {
        // Offer to start a trial or purchase
        if (source instanceof NotificationSource){
            NotificationSource nSource = (NotificationSource)source;
            final String packageName = nSource.getPackage();
            if (!packageInstalled(this, packageName)) {
                // Show a dialog to offer installation of the package
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // Open play store to install it
                        openPlayStore(packageName);
                        dialog.dismiss();
                    }
                });
                builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // User cancelled the dialog
                        dialog.cancel();
                    }
                });
                builder.setMessage("This app is not currently installed. Would you like to install it now?");
                builder.setTitle("Missing App");
                builder.setIcon(R.drawable.ic_launcher_play_store);
                AlertDialog dialog = builder.create();
                dialog.show();
                return false;
            }else if (!Licensing.isLicensed(source, this)) {
                if (LicenseTrial.isExpired(this)){
                    Log.d(TAG, "Trial expired");
                    // Expired license, show the purchase screen
                    offerPurchase();
                    return false;
                }
                // Show a dialog to offer the trial
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                final Context context = this;
                builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        Log.d(TAG, "Starting a trial");
                        long trialExpiry = LicenseTrial.startTrial(context, widgetID);
                        // Unlock premium sources
                        if (mChildFragment instanceof SourceListFragment) {
                            ALogSource[] sources = LogSourceFactory.get(context, widgetID);
                            for (ALogSource s : sources){
                                if (Licensing.requiresPremiumLicense(context, s)){
                                    boolean dontUnlock = false;
                                    if (source instanceof NotificationSource) {
                                        NotificationSource nSource = (NotificationSource) source;
                                        if (!packageInstalled(context, nSource.getPackage())) {
                                            dontUnlock = true;
                                        }
                                    }
                                    if (!dontUnlock) {
                                        ((SourceListFragment) mChildFragment).updateLockState(s, 0);
                                    }
                                }
                            }
                        }
                        dialog.dismiss();
                        Log.d(TAG, "Trial will end at " + trialExpiry);
                    }
                });
                builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // User cancelled the dialog
                        dialog.cancel();
                    }
                });
                long trialDays = getResources().getInteger(R.integer.trial_length_days);
                builder.setMessage("These messages require a premium license. Would you like to start a " + trialDays + " day free trial?");
                builder.setTitle("Free Trial");
                builder.setIcon(R.mipmap.ic_launcher);
                AlertDialog dialog = builder.create();
                dialog.show();
                return false;
            }else{
                return true;
            }

        }else {
            return true;
        }
    }

    HashMap<ALogSource, Boolean> mCheckState;

    @Override
    public void onViewCreated() {
        Log.d(TAG, "onViewCreated");
        // Store checked state
        if (mCheckState == null) {
            mCheckState = new HashMap<>();
            ALogSource[] sources = LogSourceFactory.get(this, this.widgetID);
            for (ALogSource source : sources) {
                boolean sourceEnabled = (source.config().count != 0);
                if (source instanceof NotificationSource) {
                    sourceEnabled &= ((NotificationSource) source).enabled();
                }
                Log.d(TAG, "    - " + source.getClass().toString() + "=" + sourceEnabled);
                mCheckState.put(source, sourceEnabled);
            }
        }

        updateUI(false);
        mPermissionChecked = false;
    }

    private void createDefaultSources() {
        if (BuildConfig.buildType == 1 || BuildConfig.buildType == 0) {
            // Create the SMSLog source
            Log.d(TAG, "Creating SMS Source");
            String smsID = UUID.randomUUID().toString();
            SMSLogSourceConfig configSMS = new SMSLogSourceConfig(smsID, widgetID, this);
            configSMS.showImage = true;
            configSMS.showName = true;
            configSMS.showIncoming = true;
            configSMS.showOutgoing = false;
            if (BuildConfig.buildType == 0) {
                configSMS.count = 0;
            }
            ALogSource smsSource = LogSourceFactory.newSource(this, SMSLogSource.class, configSMS);
        }
        if (BuildConfig.buildType == 2 || BuildConfig.buildType == 0) {
            // Create the CallLog source
            Log.d(TAG, "Creating Call Source");
            String callID = UUID.randomUUID().toString();
            CallLogSourceConfig configCall = new CallLogSourceConfig(callID, widgetID, this);
            configCall.showImage = true;
            configCall.showName = true;
            configCall.showCallButton = true;
            configCall.longDataFormat = false;
            if (BuildConfig.buildType == 0) {
                configCall.count = 0;
            }
            ALogSource clSource = LogSourceFactory.newSource(this, CallLogSource.class, configCall);
        }
        if (BuildConfig.buildType == 3 || BuildConfig.buildType == 0) {
            // Create the Hangout source
            Log.d(TAG, "Creating Hangouts Source");
            String hangoutsID = UUID.randomUUID().toString();
            HangoutsSourceConfig configHangouts = new HangoutsSourceConfig(hangoutsID, widgetID,
                    this);
            configHangouts.setToDefaults(this);
            if (BuildConfig.buildType == 0) {
                configHangouts.count = 0;
            }
            ALogSource clSource = LogSourceFactory.newSource(this, HangoutsSource.class,
                    configHangouts);
        }
        if (BuildConfig.buildType == 4 || BuildConfig.buildType == 0) {
            // Create the WhatsApp source
            Log.d(TAG, "Creating WhatsApp Source");
            String WhatsAppID = UUID.randomUUID().toString();
            WhatsAppSourceConfig configWhatsApp = new WhatsAppSourceConfig(WhatsAppID, widgetID,
                    this);
            configWhatsApp.setToDefaults(this);
            if (BuildConfig.buildType == 0) {
                configWhatsApp.count = 0;
            }
            ALogSource clSource = LogSourceFactory.newSource(this, WhatsAppSource.class,
                    configWhatsApp);
        }
        if (BuildConfig.buildType == 5 || BuildConfig.buildType == 0) {
            // Create the Facebook Messenger source
            Log.d(TAG, "Creating FacebookMessenger Source");
            String WhatsAppID = UUID.randomUUID().toString();
            FacebookMessengerSourceConfig configFacebookMessenger = new FacebookMessengerSourceConfig(WhatsAppID, widgetID,
                    this);
            configFacebookMessenger.setToDefaults(this);
            if (BuildConfig.buildType == 0) {
                configFacebookMessenger.count = 0;
            }
            ALogSource clSource = LogSourceFactory.newSource(this, FacebookMessengerSource.class,
                    configFacebookMessenger);
        }
        if (BuildConfig.buildType == 6 || BuildConfig.buildType == 0) {
            // Create the Viber source
            Log.d(TAG, "Creating Viber Source");
            String viberID = UUID.randomUUID().toString();
            ViberSourceConfig configViber = new ViberSourceConfig(viberID, widgetID,
                    this);
            configViber.setToDefaults(this);
            if (BuildConfig.buildType == 0) {
                configViber.count = 0;
            }
            ALogSource clSource = LogSourceFactory.newSource(this, ViberSource.class,
                    configViber);
        }
        if (BuildConfig.buildType == 7 || BuildConfig.buildType == 0) {
            // Create the WeChat source
            Log.d(TAG, "Creating WeChat Source");
            String wechatID = UUID.randomUUID().toString();
            WeChatSourceConfig configWeChat = new WeChatSourceConfig(wechatID, widgetID,
                    this);
            configWeChat.setToDefaults(this);
            if (BuildConfig.buildType == 0) {
                configWeChat.count = 0;
            }
            ALogSource clSource = LogSourceFactory.newSource(this, WeChatSource.class,
                    configWeChat);
        }
        if (BuildConfig.buildType == 8 || BuildConfig.buildType == 0) {
            // Create the Skype source
            Log.d(TAG, "Creating Skype Source");
            String skypeID = UUID.randomUUID().toString();
            SkypeSourceConfig configSkype = new SkypeSourceConfig(skypeID, widgetID,
                    this);
            configSkype.setToDefaults(this);
            if (BuildConfig.buildType == 0) {
                configSkype.count = 0;
            }
            ALogSource clSource = LogSourceFactory.newSource(this, SkypeSource.class,
                    configSkype);
        }
    }

    private void createRootSource(){
        String rootId = null;
        ALogSource[] sources = LogSourceFactory.get(this, this.widgetID);
        if (sources.length == 0){
            return;
        }else if(sources.length == 1) {
            // Get the filter
            String[] lookupFilter = getLookupKeys();
            sources[0].config().lookupKeyFilter = lookupFilter;
            LogSourceFactory.deleteSource(this, sources[0].config().sourceID);
            LogSourceFactory.newSource(this, sources[0].getClass(), sources[0].config());
            // Set the id
            rootId = sources[0].config().sourceID;
        }else {
            rootId = createCombinedSource();
        }
        if (rootId == null) return;
        // Store the combined source ID
        SharedPreferences settings = this.getSharedPreferences(STATE_FILE, this.MODE_MULTI_PROCESS);
        SharedPreferences.Editor editor = settings.edit();
        String rootIdKey = this.getPackageName() + "." + Integer.toString(widgetID) + "" +
                ".RootId";
        Log.d(TAG, "Setting root id source - " + rootIdKey);
        editor.putString(rootIdKey, rootId);
        editor.commit();
    }

    private String createCombinedSource() {
        Log.d(TAG, "Creating CombinedLogSource");
        // Delete any existing CombinedLogSources and set the count
        ALogSource[] sources = LogSourceFactory.get(this, this.widgetID);
        if(sources.length > 1) {
            // Remove combined source
            for (ALogSource source : sources) {
                if (source.getClass().equals(CombinedLogSource.class)) {
                    LogSourceFactory.deleteSource(this, source.config().sourceID);
                    Log.d(TAG, "Removed CombinedLogSource");
                }
            }
            sources = LogSourceFactory.get(this, this.widgetID);
        }

        // Get the combined source count
        int defaultCombinedCount = getResources().getInteger(R.integer.combined_source_default_count);

        // Get the filter
        String[] lookupFilter = getLookupKeys();

        // Generate a CombinedLogSource ID
        String combinedID = UUID.randomUUID().toString();

        // Count the sources that will contribute
        int sourceCount = 0;
        for (ALogSource s : sources) {
            if (s.config().count == 0) continue;
            else sourceCount++;
        }

        // Set the source counts based on the combined source count and build an array of sources
        // Also set the filter
        String[] sourceIDs = new String[sources.length];
        int i = 0;
        int count = 0;
        for (ALogSource s : sources) {
            if (s.config().count == 0) continue;
            if (s.config().count < 0) {
                s.config().count = defaultCombinedCount/2;
            }
            count += s.config().count;

            s.config().lookupKeyFilter = lookupFilter;
            LogSourceFactory.deleteSource(this, s.config().sourceID);
            LogSourceFactory.newSource(this, s.getClass(), s.config());
            sourceIDs[i++] = s.config().sourceID;
        }

        // Create the combined source from all the sources in the factory
        CombinedLogSourceConfig configCombined = new CombinedLogSourceConfig(combinedID,
                widgetID, count, sourceIDs);
        ALogSource combinedSource = LogSourceFactory.newSource(this, CombinedLogSource.class,
                configCombined);

        Log.d(TAG, "CombinedLogSource has " + count + " values");

        return combinedID;
    }

    private String[] getLookupKeys() {
        // Get the lookup keys
        DrawableRecipientChip[] recipientChips = mChipTextView.getRecipients();
        List<String> lookupFilterList = new ArrayList<>();
        for (DrawableRecipientChip entry : recipientChips){
            lookupFilterList.add(entry.getLookupKey());
        }
        String[] lookupFilter = new String[lookupFilterList.size()];
        lookupFilter = lookupFilterList.toArray(lookupFilter);
        return lookupFilter;
    }

    private void updateWidget() {
        Log.d(TAG, "updateWidget");

        // Clear all caches
        new ImageCache("NotificationCache").clear(this);
        new ImageCache("FacebookMessengerLogSource").clear(this);
        new ImageCache("HangoutsLogSource").clear(this);
        new ImageCache("WhatsAppLogSource").clear(this);
        new ImageCache("ViberLogSource").clear(this);
        new ImageCache("WeChatLogSource").clear(this);
        new ImageCache("SkypeLogSource").clear(this);
        new ImageCache("MMSParts").clear(this);
        new ImageCache("SMSLogSource").clear(this);
        new ImageCache("CallLogSource").clear(this);

        SharedPreferences settings = this.getSharedPreferences(STATE_FILE, this.MODE_MULTI_PROCESS);
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean(this.getPackageName() + "." + Integer.toString(widgetID) + ".Active",
                true);
        editor.commit();

        // Scroll the widget
        String rootIdKey = this.getPackageName() + "." + Integer.toString(widgetID) + "" +
                ".RootId";
        String rootId = settings.getString(rootIdKey, "");
        ALogSource source = LogSourceFactory.get(this, rootId);
        if (source != null) {
            int dir = loadOption("Direction", 0);
            if (dir == 0) {
                new LogRemoteViewsFactory.scrollToPosition().execute(this, widgetID, source.config().count, 0);
            }else{
                new LogRemoteViewsFactory.scrollToPosition().execute(this, widgetID, 0, 0);
            }
        }

        // Update the widget
        Intent broadcastIntent = new Intent(this, LogProvider.class);
        broadcastIntent.setAction(LogProvider.ACTION_FORCE_UPDATE);
        broadcastIntent.putExtra(this.getPackageName() + ".WidgetID", widgetID);
        this.sendBroadcast(broadcastIntent);

        // Update the widget's data
        //Intent refreshIntent = new Intent(this, LogReceiver.class);
        //refreshIntent.setAction(this.getPackageName()+".refresh");
        //refreshIntent.putExtra(this.getPackageName() + ".WidgetID", widgetID);
        //this.sendBroadcast(refreshIntent);
    }

    @Override
    public void onClick(DialogInterface dialog, int id) {

        Log.d(TAG, "Finishing from onClick");
        Intent resultValue = new Intent();
        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, this.widgetID);
        setResult(RESULT_CANCELED, resultValue);
        finish();

        switch (id) {
            case DialogInterface.BUTTON_NEGATIVE: {
                break;
            }
            case DialogInterface.BUTTON_POSITIVE: {
                launchUpgrade();
                break;
            }
        }
    }

    private void launchUpgrade() {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse("market://details?id=" + BuildConfig.upgradePackage));
        startActivity(intent);
    }

    private void openPlayStore(String packageName){
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse("market://details?id=" + packageName));
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        } else {
            Toast.makeText(this, "Play Store not found", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onClick(View v) {
        Log.d(TAG, "onClick");
        if (v.equals(this.okButton)) {
            Log.d(TAG, "OK Button Pressed");
            ArrayList<String> permissions = new ArrayList<>();
            ArrayList<String> permissionLabels = new ArrayList<>();
            getRequiredPermissions(permissions, permissionLabels);
            mRuntimePermissions.startPermissionCheck(permissions, permissionLabels, PERMISSION_REQEST_CODE, this);
        }
        if (v.equals(this.cancelButton)) {
            Log.d(TAG, "Cancel Button Pressed");
            Intent resultValue = new Intent();
            resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, this.widgetID);
            setResult(RESULT_CANCELED, resultValue);
            finish();
        }
    }

    private void getRequiredPermissions(ArrayList<String> permissions, ArrayList<String>
            permissionLabels) {
        permissions.add(Manifest.permission.READ_CONTACTS);
        permissionLabels.add("Read your contacts");

        if (BuildConfig.buildType == 1 || BuildConfig.buildType == 0) {
            permissions.add(Manifest.permission.READ_SMS);
            permissionLabels.add("Read your SMS messages");
            //permissions.add(Manifest.permission.RECEIVE_SMS);
            //permissionLabels.add("Receive your SMS messages");
        }
        if (BuildConfig.buildType == 2 || BuildConfig.buildType == 0) {
            permissions.add(Manifest.permission.READ_CALL_LOG);
            permissionLabels.add("Read your call log");
        }
        if (BuildConfig.buildType == 3 || BuildConfig.buildType == 4 || BuildConfig.buildType ==
                5 || BuildConfig.buildType == 0) {
            permissions.add(Manifest.permission.BIND_NOTIFICATION_LISTENER_SERVICE);
            permissionLabels.add("Read your notifications");
        }
    }

    @Override
    public void gotPermissions(int[] permissions) {
        if (this.initialConfiguration) {
            createWidget();
        }
        mPermissionChecked = true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[]
            grantResults) {
        if (requestCode == PERMISSION_REQEST_CODE){
            mRuntimePermissions.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

    }

    private void createWidget() {
        ALogSource[] sources = LogSourceFactory.get(this, this.widgetID);
        if (sources.length == 1 && sources[0] instanceof NotificationSource){
            // Make sure the source has notification access
            NotificationSource nSource = (NotificationSource) sources[0];
            if (!nSource.enabled()){
                Toast.makeText(this, getResources().getString(com.abstractwombat.loglibrary.R
                        .string.notitication_access_toast), Toast.LENGTH_LONG).show();
                return;
            }
            // Don't show the tip for NotificationSources
            SharedPreferences state = getSharedPreferences(STATE_FILE, Context.MODE_MULTI_PROCESS);
            SharedPreferences.Editor edit = state.edit();
            long currentTime = System.currentTimeMillis();
            edit.putLong(getPackageName() + "." + widgetID + ".TipClosed", currentTime);
            edit.putLong(getPackageName() + "." + widgetID + ".WidgetCreationTime", currentTime);
            edit.commit();
        }

        Log.d(TAG, "Updating the widget");
        createRootSource();
        updateWidget();

        Log.d(TAG, "Done Configuring Sources");
        Intent resultValue = new Intent();
        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetID);
        setResult(RESULT_OK, resultValue);
        finish();
    }

    @Override
    public void onBackStackChanged() {
        Log.d(TAG, "onBackStackChanged");
        mToolbar.setTitle(R.string.app_name);

        FragmentManager fm = getFragmentManager();
        if (fm.getBackStackEntryCount() == 0) {
            if (initialConfiguration) {
                // Show the OK/Cancel buttons
                this.okButton.setVisibility(View.VISIBLE);
                this.cancelButton.setVisibility(View.VISIBLE);
            }
            Log.d(TAG, "  backstack is empty");
            // Remove the back button
            mToolbar.setNavigationIcon(null);
            updateUI(false);
        } else {
            Log.d(TAG, "  backstack is NOT empty");
            // Hide the OK/Cancel buttons
            this.okButton.setVisibility(View.GONE);
            this.cancelButton.setVisibility(View.GONE);
            // Set the title
            mToolbar.setNavigationIcon(R.drawable.ic_arrow_left_white_24dp);
            FragmentManager.BackStackEntry entry = fm.getBackStackEntryAt(fm
                    .getBackStackEntryCount() - 1);
            String name = entry.getName();
            if (name != null && name.length() > 0) {
                mToolbar.setTitle(name);
            }
        }
    }

    @Override
    public void onBackPressed() {
        Log.d(TAG, "onBackPressed");
        if (getFragmentManager().getBackStackEntryCount() <= 0) {
            Log.d(TAG, "  backstack is empty");
            super.onBackPressed();
        } else {
            Log.d(TAG, "  backstack is NOT empty");
            getFragmentManager().popBackStack();
        }
    }

    @Override
    public boolean onMenuItemClick(MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case R.id.upgrade: {
                offerPurchase();
                break;
            }
            case R.id.back_color: {
                queryBackcolor();
                break;
            }
            case R.id.direction: {
                queryDirection();
                break;
            }
            case R.id.spacing: {
                querySpacing();
                break;
            }
            case R.id.about_action: {
                final Dialog dialog = new Dialog(this);
                dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                dialog.setContentView(R.layout.about_dialog);
                TextView textView = (TextView) dialog.findViewById(R.id.text);
                if (textView != null) {
                    textView.setMovementMethod(LinkMovementMethod.getInstance());
                }
                dialog.show();
                break;
            }
            case R.id.filter_action: {
                if (mChipTextView.getVisibility() == View.VISIBLE) {
                    mChipTextView.setVisibility(View.GONE);
                }else{
                    mChipTextView.setVisibility(View.VISIBLE);
                    mChipTextView.requestFocus();
                }
                break;
            }
        }
        return false;
    }

    private void querySpacing() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Spacing between items");
        // Set up the input
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_CLASS_NUMBER);
        int oldValue = loadOption("Spacing", getResources().getInteger(R.integer.default_spacing));
        oldValue = Math.round(ImageUtilities.convertPixelTpDp(oldValue));
        input.setText(((Integer) oldValue).toString());
        builder.setView(input);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String s = input.getText().toString();
                int value = Integer.parseInt(s);
                value = Math.round(ImageUtilities.convertDpToPixel(value));
                Log.d(TAG, "Setting spacing to " + value);
                storeOption("Spacing", value);
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        builder.create().show();
    }

    private void queryDirection() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Direction");
        CharSequence[] itemsArray = { "New messages appear at the BOTTOM of the widget", "New messages appear at the TOP of the widget" };
        builder.setItems(itemsArray, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                storeOption("Direction", which);
                updateMenu();
            }
        });
        builder.create().show();
    }

    private void querySave() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Settings");
        CharSequence[] itemsArray = { "Save settings to file", "Restore settings from file", "Reset to default" };
        final Context context = this;
        builder.setItems(itemsArray, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (which == 0){
                    // Save the settings
                    Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    intent.setType("text/xml");
                    intent.putExtra(Intent.EXTRA_TITLE, getResources().getString(R.string.app_name) + ".xml");
                    startActivityForResult(intent, SAVE_SETTINGS_REQUEST_CODE);
                }else if (which == 1){
                    // Restore the settings
                    Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    intent.setType("text/xml");
                    startActivityForResult(intent, RESTORE_SETTINGS_REQUEST_CODE);
                }else if (which == 2){
                    storeOption("Spacing", getResources().getInteger(R.integer.default_spacing));
                    storeOption("BackColor", Color.TRANSPARENT);
                    storeOption("Direction", 0);

                    LogSourceFactory.deleteGroup(context, widgetID);
                    createDefaultSources();

                    createRootSource();
                    // Recreate the source list fragment
                    getFragmentManager().beginTransaction().remove(mChildFragment).commit();
                    mChildFragment = SourceListFragment.newInstance(widgetID);
                    int container = R.id.fragment_container;
                    getFragmentManager().beginTransaction().replace(R.id.fragment_container,
                            mChildFragment).commit();
                }
            }
        });
        builder.create().show();
    }

    private void querySettingsStyle(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Settings buttons");
        CharSequence[] itemsArray = { "Small circular", "Rectangular with text" };
        final Context context = this;
        builder.setItems(itemsArray, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (which == 0 || which == 1){
                    storeOption("SettingsStyle", which);
                    updateMenu();
                }
            }
        });
        builder.create().show();
    }

    private static final int SAVE_SETTINGS_REQUEST_CODE = 42;
    private static final int RESTORE_SETTINGS_REQUEST_CODE = 43;

    private void saveSettings(Uri uri){
        String appName = getResources().getString(R.string.app_name);
        String appNameTag = appName.replace(" ", "");

        XmlSerializer serializer = Xml.newSerializer();
        try {
            OutputStream stream = getContentResolver().openOutputStream(uri);
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(stream));

            serializer.setOutput(writer);
            serializer.startDocument("UTF-8", true);
            serializer.startTag("", appNameTag);

            // Options
            int backColor = loadOption("BackColor", Color.TRANSPARENT);
            int spacing = loadOption("Spacing", -1);
            int dir = loadOption("Direction", 0);
            int settingsStyle = loadOption("SettingsStyle", 0);
            serializer.startTag("", "options");

            serializer.startTag("", "BackColor");
            serializer.text(Integer.toString(backColor));
            serializer.endTag("", "BackColor");

            serializer.startTag("", "Spacing");
            serializer.text(Integer.toString(spacing));
            serializer.endTag("", "Spacing");

            serializer.startTag("", "Direction");
            serializer.text(Integer.toString(dir));
            serializer.endTag("", "Direction");

            serializer.startTag("", "SettingsStyle");
            serializer.text(Integer.toString(settingsStyle));
            serializer.endTag("", "SettingsStyle");

            serializer.endTag("", "options");

            // Source configurations
            ALogSource[] sources = LogSourceFactory.get(this, widgetID);
            for (ALogSource s : sources){
                String className = s.config().getClass().getName();
                serializer.startTag("", className);
                serializer.text(s.config().serialize());
                serializer.endTag("", className);
            }

            serializer.endTag("", appNameTag);
            serializer.endDocument();
            writer.close();
        } catch (IOException e) {
            Log.d(TAG, "IOException while writing settings file");
            e.printStackTrace();
        }
    }
    private void restoreSettings(Uri uri) {
        HashMap<Class<?>, LogSourceConfig> configurations = new HashMap<>();
        HashMap<String, Integer> options = new HashMap<>();
        try {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(true);
            XmlPullParser xrp = factory.newPullParser();
            InputStream inputStream = getContentResolver().openInputStream(uri);
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    inputStream));
            xrp.setInput(reader);

            LogSourceConfig config = null;
            Class<?> sourceClass = null;
            boolean inOptions = false;
            String optionName = null;
            String tagName = null;

            int eventType = xrp.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    int attrCount = xrp.getAttributeCount();
                    String xrpName = xrp.getName();
                    tagName = xrpName;
                    if ("options".equals(xrpName)) {
                        inOptions = true;
                    } else if (inOptions) {
                        optionName = xrpName;
//                    } else if (xrpName.endsWith("CombinedLogSourceConfig")) {
//                        sourceClass = CombinedLogSource.class;
//                        config = new CombinedLogSourceConfig();
                    } else if (xrpName.endsWith("CallLogSourceConfig")) {
                        sourceClass = CallLogSource.class;
                        config = new CallLogSourceConfig();
                    } else if (xrpName.endsWith("EmailSourceConfig")) {
                        sourceClass = EmailSource.class;
                        config = new EmailSourceConfig();
                    } else if (xrpName.endsWith("FacebookMessengerSourceConfig")) {
                        sourceClass = FacebookMessengerSource.class;
                        config = new FacebookMessengerSourceConfig();
                    } else if (xrpName.endsWith("HangoutsSourceConfig")) {
                        sourceClass = HangoutsSource.class;
                        config = new HangoutsSourceConfig();
                    } else if (xrpName.endsWith("SkypeSourceConfig")) {
                        sourceClass = SkypeSource.class;
                        config = new SkypeSourceConfig();
                    } else if (xrpName.endsWith("SMSLogSourceConfig")) {
                        sourceClass = SMSLogSource.class;
                        config = new SMSLogSourceConfig();
                    } else if (xrpName.endsWith("ViberSourceConfig")) {
                        sourceClass = ViberSource.class;
                        config = new ViberSourceConfig();
                    } else if (xrpName.endsWith("WeChatSourceConfig")) {
                        sourceClass = WeChatSource.class;
                        config = new WeChatSourceConfig();
                    } else if (xrpName.endsWith("WhatsAppSourceConfig")) {
                        sourceClass = WhatsAppSource.class;
                        config = new WhatsAppSourceConfig();
                    }
                } else if (eventType == XmlPullParser.END_TAG) {
                    if ("options".equals(xrp.getName())) {
                        inOptions = false;
                    }else if (config != null && sourceClass != null){
                        config.groupID = this.widgetID;
                        configurations.put(sourceClass, config);
                        config = null;
                    }
                    tagName = null;
                } else if (eventType == XmlPullParser.TEXT) {
                    if (inOptions && optionName != null) {
                        int value = Integer.parseInt(xrp.getText());
                        options.put(optionName, value);
                        optionName = null;
                    } else if (config != null) {
                        config.unserialize(xrp.getText());
                    }
                }
                eventType = xrp.next();
            }

        } catch (XmlPullParserException e) {
            Log.d(TAG, "XmlPullParserException while reading settings file");
            e.printStackTrace();
        } catch (IOException e) {
            Log.d(TAG, "IOException while reading settings file");
            e.printStackTrace();
        }


        if (!options.isEmpty()) {
            // Set the options
            for (HashMap.Entry<String, Integer> e : options.entrySet()) {
                storeOption(e.getKey(), e.getValue());
            }
        }
        if (!configurations.isEmpty()){
            // Delete the sources
            LogSourceFactory.deleteGroup(this, this.widgetID);
            // Create all the sources
            for (HashMap.Entry<Class<?>, LogSourceConfig> e : configurations.entrySet()){
                ALogSource clSource = LogSourceFactory.newSource(this, e.getKey(), e.getValue());
            }

            createRootSource();
            // Recreate the source list fragment
            getFragmentManager().beginTransaction().remove(this.mChildFragment).commit();
            this.mChildFragment = SourceListFragment.newInstance(widgetID);
            int container = R.id.fragment_container;
            getFragmentManager().beginTransaction().replace(R.id.fragment_container,
                    this.mChildFragment).commit();
        }
    }

    private void queryBackcolor() {
        int backColor = loadOption("BackColor", Color.TRANSPARENT);
        ColorPickerDialogFragment dialog = ColorPickerDialogFragment
                .newInstance(0, "Background color", null, backColor, true);
        dialog.show(getFragmentManager(), "back_color_dialog");
        dialog.setListener(new ColorPickerDialogFragment.ColorPickerDialogListener() {
            @Override
            public void onColorSelected(int dialogId, int color) {
                storeOption("BackColor", color);
            }

            @Override
            public void onDialogDismissed(int dialogId) {

            }
        });
    }

    private void offerPurchase(){
        if (!mIabSetupSuccess) {
            Toast.makeText(this, "In-app purchasing not supported", Toast.LENGTH_SHORT).show();
            return;
        }
        // Check if they've already purchased everything
        if (BuildConfig.buildType == 0) {
            if (mUnlockAll || (mSourcesUnlocked && mAdsRemoved)) return;
        }else{
            if (mAdsRemoved) return;
        }

        final Activity activity = this;

        if (BuildConfig.buildType == 0) {
            // Create a dialog to show the available IAP
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Upgrade");
            int allIndexTemp = -1, adsIndexTemp = -1, premiumIndexTemp = -1;
            ArrayList<CharSequence> items = new ArrayList<>();
            if (!mUnlockAll) {
                allIndexTemp = items.size();
                items.add(mUnlockAllPrice + " - Remove ads and unlock all messages");
            }
            if (!mAdsRemoved) {
                adsIndexTemp = items.size();
                items.add(mAdsRemovedPrice + " - Remove ads");
            }
            if (!mSourcesUnlocked) {
                premiumIndexTemp = items.size();
                items.add(mIsPremiumPrice + " - Unlock all messages");
            }
            final int allIndex = allIndexTemp;
            final int adsIndex = adsIndexTemp;
            final int premiumIndex = premiumIndexTemp;
            CharSequence[] itemsArray = items.toArray(new CharSequence[items.size()]);
            builder.setItems(itemsArray, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
//                    if (which == allIndex) {
//                        mIabHelper.launchPurchaseFlow(activity, BuildConfig.iapProductAll, 1003, listener);
//                    } else if (which == adsIndex) {
//                        mIabHelper.launchPurchaseFlow(activity, BuildConfig.iapProductRemoveAds, 1001, listener);
//                    } else if (which == premiumIndex) {
//                        mIabHelper.launchPurchaseFlow(activity, BuildConfig.iapProductUnlockSources, 1002, listener);
//                    }
                }
            });
            builder.create().show();
        }else{
//            mIabHelper.launchPurchaseFlow(activity, BuildConfig.iapProductRemoveAds, 1001, listener);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SAVE_SETTINGS_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            if (data != null) {
                Uri uri = data.getData();
                Log.i(TAG, "Uri: " + uri.toString());
                saveSettings(uri);
            }
        } else if (requestCode == RESTORE_SETTINGS_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            if (data != null) {
                Uri uri = data.getData();
                Log.i(TAG, "Uri: " + uri.toString());
                restoreSettings(uri);
            }
        } else {
//            mIabHelper.handleActivityResult(requestCode, resultCode, data);
        }
    }

    private void adVisibility(boolean isVisible){
        // Look up the AdView as a resource and load a request.
//        View adViewV = this.findViewById(R.id.adView);
//        if (adViewV != null && adViewV instanceof AdView) {
//            if (isVisible) {
//                Log.d(TAG, "Showing ad");
//                AdView adView = (AdView) (adViewV);
//                AdRequest adRequest = new AdRequest.Builder().build();
//                adView.loadAd(adRequest);
//                adView.setVisibility(View.VISIBLE);
//            }else {
//                Log.d(TAG, "Hiding ad");
//                adViewV.setVisibility(View.GONE);
//            }
//        }
    }

//    @Override
//    public void onIabPurchaseFinished(IabResult result, Purchase purchase) {
//        Log.d(TAG, "Purchase finished: " + result + ", purchase: " + purchase);
//        if (purchase == null) {
//            Log.d(TAG, "Error purchasing (null purchase)");
//            purchaseFailed(null);
//            return;
//        }
//        String sku = purchase.getSku();
//
//        // if we were disposed of in the meantime, quit.
//        if (mIabHelper == null) {
//            purchaseFailed(sku);
//            return;
//        }
//
//        if (result.isFailure()) {
//            Log.d(TAG, "Error purchasing: " + result);
//            purchaseFailed(sku);
//            return;
//        }
//
//        // Verify the payload
//        String payload = purchase.getDeveloperPayload();
//
//        Log.d(TAG, "Purchase successful.");
//
//        // Verify signature
//        AsyncTask<String, Void, String> signatureVerification = new AsyncTask<String, Void, String>() {
//            @Override
//            protected String doInBackground(String... params) {
//                String sku = params[0];
//                if (Security.verifyPurchaseRemote(IAP_VERIFICATION_URL, params[1], params[2], getPackageName())) {
//                    return sku;
//                }else{
//                    Log.d(TAG, "Purchase signature verification FAILED for sku " + sku);
//                    return "";
//                }
//            }
//
//            @Override
//            protected void onPostExecute(String sku) {
//                super.onPostExecute(sku);
//                if (!sku.isEmpty()){
//                    purchaseVerified(sku);
//                }else {
//                    purchaseFailed(sku);
//                }
//            }
//        }.execute(sku, purchase.getOriginalJson(), purchase.getSignature());
//    }

    private void purchaseVerified(String sku){
        if (sku.equals(BuildConfig.iapProductAll)) {
            // Unlock everything
            Log.d(TAG, "Purchased " + BuildConfig.iapProductAll);
            mUnlockAll = true;
        }else if (sku.equals(BuildConfig.iapProductRemoveAds)) {
            // Ads should be removed
            Log.d(TAG, "Purchased " + BuildConfig.iapProductRemoveAds);
            mAdsRemoved = true;
        }else if (sku.equals(BuildConfig.iapProductUnlockSources)) {
            // Unlock all sources
            Log.d(TAG, "Purchased " + BuildConfig.iapProductUnlockSources);
            mSourcesUnlocked = true;
        }else{
            return;
        }
        storeLicensingState(true);
        updateUI(!initialConfiguration);
    }
    private void purchaseFailed(String sku) {
        String m = "Purchase failed";
        if (sku != null) {
            m = "Failed to purchase '" + sku + "'";
        }
        Toast.makeText(this, m, Toast.LENGTH_SHORT).show();
    }

    private void updateMenu() {
        if (mToolbar == null) return;
        MenuItem adMenu = mToolbar.getMenu().findItem(R.id.upgrade);
        Log.d(TAG, "UpdateMenu");
        if (adMenu != null) {
            if (mIabSetupSuccess) {
                if (BuildConfig.buildType == 0) {
                    if (mUnlockAll || (mSourcesUnlocked && mAdsRemoved)) {
                        Log.d(TAG, "  upgrade menu NOT visible");
                        adMenu.setVisible(false);
                    } else {
                        Log.d(TAG, "  upgrade menu visible");
                        adMenu.setVisible(true);
                    }
                } else {
                    adMenu.setVisible(!mAdsRemoved);
                    adMenu.setTitle("Remove ads");
                }
            } else {
                adMenu.setVisible(false);
            }
        }

        SourceListFragment sourceList = null;
        if (mChildFragment instanceof SourceListFragment){
            sourceList = (SourceListFragment) mChildFragment;
        }
        if (sourceList != null) {
            int dir = loadOption("Direction", 0);
            if (dir == 0) {
                sourceList.setOptionImage(3, R.drawable.ic_arrow_up_bold_circle_outline_white_36dp);
            } else {
                sourceList.setOptionImage(3, R.drawable.ic_arrow_down_bold_circle_outline_white_36dp);
            }
            int settingsStyle = loadOption("SettingsStyle", 0);
            if (settingsStyle == 1) {
                sourceList.setOptionImage(5, R.drawable.ic_checkbox_blank_white_36dp);
            } else {
                sourceList.setOptionImage(5, R.drawable.ic_checkbox_blank_circle_white_36dp);
            }
        }
    }

    private void updateUI(boolean restoreCheckState){
        Log.d(TAG, "Update UI (adsRemoved="+mAdsRemoved + " mUnlockAll="+mUnlockAll);
        // Set the Ad visibility
        if (LicenseTrial.trialStarted(this) && !LicenseTrial.isExpired(this)) {
            adVisibility(false);
        }else {
            adVisibility(!mAdsRemoved && !mUnlockAll);
        }

        // Setup the menu items for purchasing
        updateMenu();

        // Tell the SourceListFragment to unlock the licensed sources
        if (mChildFragment instanceof SourceListFragment){
            SourceListFragment sourceListFragment = (SourceListFragment) mChildFragment;
            ALogSource[] sources = LogSourceFactory.get(this, this.widgetID);
            for (ALogSource source : sources){
                if (source == null) continue;
                // Determine if the associated package is installed
                boolean installed = true;
                boolean enabled = true;
                if (source instanceof NotificationSource){
                    NotificationSource nSource = (NotificationSource)source;
                    if (!packageInstalled(this, nSource.getPackage())) {
                        installed = false;
                    }
                }
                Log.d(TAG, "    - installed = " + installed);
                // Set the source lock state checking licensing
                if (installed && Licensing.isLicensed(source, this)){
                    // Unlock
                    sourceListFragment.updateLockState(source, 0);
                    Log.d(TAG, "    - unlocked source");
                }else{
                    // Lock
                    sourceListFragment.updateLockState(source, 1);
                    Log.d(TAG, "    - locked source");
                }
            }
            if (sourceListFragment.allDisabled()){
                sourceListFragment.enabledUnlockedSources();
            }
            Log.d(TAG, "Restore state: " + restoreCheckState);
            if (restoreCheckState && mCheckState != null && !mCheckState.isEmpty()){
                for (HashMap.Entry<ALogSource,Boolean> e : mCheckState.entrySet()){
                    Log.d(TAG, "Setting check state: " + e.getKey().getClass().toString() + "=" + e.getValue());
                    sourceListFragment.setCheckState(e.getKey(), e.getValue());
                }
            }
        }
    }

    public static boolean packageInstalled(Context context, String packageName) {
        final PackageManager packageManager = context.getPackageManager();
        Intent intent = packageManager.getLaunchIntentForPackage(packageName);
//        IntentSender intentSender = packageManager.getLaunchIntentSenderForPackage(packageName);
        if (intent == null) {
            return false;
        }
        List<ResolveInfo> list = packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
        return list.size() > 0;
    }

//    @Override
//    public void onIabSetupFinished(IabResult result) {
//        Log.d(TAG, "onIabSetupFinished");
//        if (result.isSuccess()) {
//            List<String> skus = new ArrayList<>();
//            skus.add(BuildConfig.iapProductRemoveAds);
//            if (BuildConfig.buildType == 0) {
//                skus.add(BuildConfig.iapProductAll);
//                skus.add(BuildConfig.iapProductUnlockSources);
//            }
//            Log.d(TAG, " success");
//            mIabHelper.queryInventoryAsync(true, skus, this);
//            mIabSetupSuccess = true;
//        }else{
//            Log.d(TAG, " failed");
//            Toast.makeText(this, result.getMessage(), Toast.LENGTH_LONG).show();
//            iabFailure();
//        }
//    }

    private void iabFailure(){
        Log.d(TAG, "iabFailure");
        // Check when the last license check was
        SharedPreferences settings = this.getSharedPreferences(STATE_FILE, this.MODE_MULTI_PROCESS);
        String licenceCheck = this.getPackageName() + ".LastLicenseCheck";
        long lastCheck = settings.getLong(licenceCheck, -1);

        long time = System.currentTimeMillis();
        long graceMilliseconds = (long) (getResources().getInteger(R.integer.grace_length_days) * 24 * 60 * 60 * 1000);
        boolean onTrial = LicenseTrial.trialStarted(this) && !LicenseTrial.isExpired(this);
        if (!onTrial && time - lastCheck > graceMilliseconds) {
            Log.d(TAG, "    - grace period over");
            mIabSetupSuccess = false;
            mAdsRemoved = false;
            mSourcesUnlocked = false;
            mUnlockAll = false;
            storeLicensingState(false);
        }
        updateUI(false);
    }

//    @Override
//    public void onQueryInventoryFinished(IabResult result, Inventory inventory) {
//        Log.d(TAG, "Query inventory finished.");
//
//        // Have we been disposed of in the meantime? If so, quit.
//        if (mIabHelper == null) return;
//
//        // Is it a failure?
//        if (result.isFailure()) {
//            Log.d(TAG, "    - failed: " + result);
//            iabFailure();
//            return;
//        }
//
//        Log.d(TAG, "Query inventory was successful.");
//
//        // Check the prices of all the skus
//        SkuDetails adsRemovedDetails = inventory.getSkuDetails(BuildConfig.iapProductRemoveAds);
//        if (adsRemovedDetails != null){
//            mAdsRemovedPrice = adsRemovedDetails.getPrice();
//        }else Log.d(TAG, "No sku details for " + BuildConfig.iapProductRemoveAds);
//        SkuDetails isPremiumDetails = inventory.getSkuDetails(BuildConfig.iapProductUnlockSources);
//        if (isPremiumDetails != null) {
//            mIsPremiumPrice = isPremiumDetails.getPrice();
//        }else Log.d(TAG, "No sku details for " + BuildConfig.iapProductUnlockSources);
//        SkuDetails unlockAllDetails = inventory.getSkuDetails(BuildConfig.iapProductAll);
//        if (isPremiumDetails != null) {
//            mUnlockAllPrice = unlockAllDetails.getPrice();
//        }else Log.d(TAG, "No sku details for " + BuildConfig.iapProductAll);
//
//        // Do we own the unlock all license
//        Purchase unlockAllPurchase = inventory.getPurchase(BuildConfig.iapProductAll);
//        mUnlockAll = (unlockAllPurchase != null);
//        //if (getPackageName().contains("debug"))mUnlockAll = true;
//
//        // Do we own the remove ad
//        Purchase removeAdPurchase = inventory.getPurchase(BuildConfig.iapProductRemoveAds);
//        mAdsRemoved = (removeAdPurchase != null);
//
//        // Do we own the premium
//        Purchase premiumPurchase = inventory.getPurchase(BuildConfig.iapProductUnlockSources);
//        mSourcesUnlocked = (premiumPurchase != null);
//
//        // Consume all purchases -------------------------------------------------------------------
////        Log.d(TAG, "Consuming all skus");
////        final Context tempContext = this;
////        IabHelper.OnConsumeFinishedListener consumed = new IabHelper.OnConsumeFinishedListener() {
////            @Override
////            public void onConsumeFinished(Purchase purchase, IabResult result) {
////                if (result.isSuccess()) {
////                    Toast.makeText(tempContext, "Consumed " + purchase.getSku(), Toast.LENGTH_LONG).show();
////                }else{
////                    Toast.makeText(tempContext, "FAILED to consume " + purchase.getSku(), Toast.LENGTH_LONG).show();
////                }
////            }
////        };
////        List<Purchase> purchases = new ArrayList<>();
////        if (unlockAllPurchase != null) purchases.add(unlockAllPurchase);
////        if (removeAdPurchase != null) purchases.add(removeAdPurchase);
////        if (premiumPurchase != null) purchases.add(premiumPurchase);
////        if (purchases.size() > 0) {
////            mIabHelper.consumeAsync(purchases, consumed);
////        }
//        // Consume all purchases -------------------------------------------------------------------
//
//        Log.d(TAG, "License: All / Ads Removed / Premium " + mUnlockAll + " / " + mAdsRemoved + " / " + mSourcesUnlocked);
//        storeLicensingState(true);
//        updateUI(!initialConfiguration);
//    }

    private void storeLicensingState(boolean verified) {
        Log.d(TAG, "storeLicensingState");
        SharedPreferences settings = this.getSharedPreferences(STATE_FILE, this.MODE_MULTI_PROCESS);
        SharedPreferences.Editor editor = settings.edit();
        // Store the unlock all
        String unlockAllKey = this.getPackageName() + ".UnlockAll";
        Log.d(TAG, "  mUnlockAll="+mUnlockAll);
        editor.putBoolean(unlockAllKey, mUnlockAll);
        // Store the removal of the ads
        String adRemoveKey = this.getPackageName() + ".AdsRemoved";
        Log.d(TAG, "  mAdsRemoved="+mAdsRemoved);
        editor.putBoolean(adRemoveKey, mAdsRemoved);
        // Store the removal of the ads
        String premiumKey = this.getPackageName() + ".IsPremium";
        Log.d(TAG, "  mSourcesUnlocked="+mSourcesUnlocked);
        editor.putBoolean(premiumKey, mSourcesUnlocked);
        // Store the last license check time
        if (verified) {
            String licenceCheck = this.getPackageName() + ".LastLicenseCheck";
            long time = System.currentTimeMillis();
            Log.d(TAG, "  LastLicenseCheck=" + time);
            editor.putLong(licenceCheck, time);
        }
        editor.commit();
    }

    private int loadOption(String setting, int defaultValue){
        SharedPreferences state = getSharedPreferences(STATE_FILE, Context.MODE_MULTI_PROCESS);
        int value = state.getInt(getPackageName() + "." + widgetID + "." + setting, defaultValue);
        return value;
    }

    private void storeOption(String setting, int value){
        SharedPreferences state = getSharedPreferences(STATE_FILE, Context
                .MODE_MULTI_PROCESS);
        SharedPreferences.Editor editor = state.edit();
        editor.putInt(getPackageName() + "." + widgetID + "." + setting, value);
        editor.apply();
    }

    @Override
    public boolean onOptionTouched(int option) {
        switch (option){
            case 1:
                // Background
                queryBackcolor();
                break;
            case 2:
                // Spacing
                querySpacing();
                break;
            case 3:
                // Direction
                queryDirection();
                break;
            case 4:
                // Save
                querySave();
                break;
            case 5:
                // Settings style
                querySettingsStyle();
                break;
        }
        return false;
    }
}
