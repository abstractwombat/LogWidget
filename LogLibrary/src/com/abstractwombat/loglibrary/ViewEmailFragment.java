package com.abstractwombat.loglibrary;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.abstractwombat.contacts.ContactUtilities;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.concurrent.ExecutionException;

import javax.mail.Address;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;

public class ViewEmailFragment extends DialogFragment {
	private static final String TAG = "ViewEmailFragment";
    /**
     * Create a new instance of ViewEmailFragment
     */
    static ViewEmailFragment newInstance(String messageID, String folder, String server, int port, String username, String password) {
        ViewEmailFragment f = new ViewEmailFragment();
        Bundle args = new Bundle();
        args.putString("MessageID", messageID);
        args.putString("Folder", folder);
        args.putString("Server", server);
        args.putInt("Port", port);
        args.putString("Username", username);
        args.putString("Password", password);
        f.setArguments(args);
        return f;
    }

	public interface ViewEmailCompleteListener{
		public void onViewEmailDismissed();
		public void onViewEmailCancelled();
	}

    private int error;
	private Context mContext;
    private ViewEmailCompleteListener mCompleteListener;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
		Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);
    }
	
	@Override
    public void onAttach(Activity activity) {
		Log.d(TAG, "onAttach");
		super.onAttach(activity);
		mContext = activity;
		if (activity instanceof ViewEmailCompleteListener){
			mCompleteListener = (ViewEmailCompleteListener)activity;
		}
	}

	@Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        super.onCreateDialog(savedInstanceState);
		AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
		View v = getActivity().getLayoutInflater().inflate(R.layout.view_email, null);

        String s = getArguments().getString("Server");
        int p = getArguments().getInt("Port");
        String user = getArguments().getString("Username");
        String pass = getArguments().getString("Password");
        String messageID = getArguments().getString("MessageID");
        String folder = getArguments().getString("Folder");
        AsyncTask task = new loadEmailTask(mContext, v).execute(s, Integer.toString(p), folder, messageID, user, pass);
        try {
            task.get();
        } catch (InterruptedException e) {
            e.printStackTrace();
            return null;
        } catch (ExecutionException e) {
            e.printStackTrace();
            return null;
        }

        builder.setView(v);
        return builder.create();
	}

    private class loadEmailTask extends AsyncTask<String, Void, View> {
        private Context mContext;
        private View mView;

        private String mFrom;
        private String mSubject;
        private String mDate;
        private String mBody;

        loadEmailTask(Context context, View view){
            super();
            this.mContext = context;
            this.mView = view;
        }

        protected View doInBackground(String... imapData) {
            int count = imapData.length;
            if (count != 6) return null;
            String s = imapData[0];
            int port = Integer.parseInt(imapData[1]);
            String folder = imapData[2];
            String messageID = imapData[3];
            String u = imapData[4];
            String p = imapData[5];

            // Connect to IMAP
            IMAP imap = new IMAP();
            int error = imap.connectWithLogin(s, port, u, p);
            if (error == 1){
                Log.e(TAG, "Failed to connect to the IMAP server!");
            }else if (error == 2){
                Log.e(TAG, "Invalid Username and/or Password");
            }else if (error == 3){
                Log.e(TAG, "Failed to connect to the IMAP server! (already connected?)");
            }else if (error == 4){
                Log.e(TAG, "Failed to connect to the IMAP server! (success?)");
            }
            if (error != 0) return null;


            Message msg = imap.getMessage(folder, messageID);

            // Find contact info from the email
            String name = "";
            String key = "";
            Address[] addresses = new Address[0];
            try {
                addresses = msg.getFrom();
                if (addresses.length > 0){
                    mFrom = addresses[0].toString();
                    String[] data = ContactUtilities.getContactDataByEmail(mFrom, mContext,
                            new String[]{ContactsContract.Contacts.LOOKUP_KEY, ContactsContract
                                    .Contacts.DISPLAY_NAME});
                    if (data != null && data.length == 2){
                        name = data[1];
                        key = data[0];
                    }else{
                        name = mFrom;
                        key = "";
                    }
                }
            } catch (MessagingException e) {
                e.printStackTrace();
            }

            // Set the from text
            try {
                mFrom = msg.getFrom().toString();
            } catch (MessagingException e) {
                e.printStackTrace();
                mFrom = "Error";
            }

            // Set the subject
            try {
                mSubject = msg.getSubject();
            } catch (MessagingException e) {
                e.printStackTrace();
                mSubject = "Error";
            }

            // Set the date
            long time = 0;
            try {
                time = msg.getReceivedDate().getTime();
            } catch (MessagingException e) {
                e.printStackTrace();
            }
            mDate = dateToString(time);

            // Set the body of the email
            String mBody = getMessageBody(msg);

            imap.disconnect();
            return mView;
        }
        protected void onPreExecute() {
        }
        protected void onPostExecute() {
            TextView fromView = (TextView)mView.findViewById(R.id.from);
            TextView subjectView = (TextView)mView.findViewById(R.id.subject);
            TextView dateView = (TextView)mView.findViewById(R.id.date);
            TextView bodyView = (TextView)mView.findViewById(R.id.body);

            fromView.setText(mFrom);
            subjectView.setText(mSubject);
            dateView.setText(mDate);
            bodyView.setText(mBody);
        }

        private String getMessageBody(Message m){
            StringBuilder body = new StringBuilder();
            Object content = null;
            try {
                content = m.getContent();
                if (content instanceof String){
                    body.append((String)content);
                }else if (content instanceof Multipart){
                    Multipart mp = (Multipart)content;
                    for(int i = 0; i < mp.getCount(); i++) {
                        BodyPart bp = mp.getBodyPart(i);
                        String disp = bp.getDisposition();
                        if(disp != null && (disp.equals(BodyPart.ATTACHMENT))) {
                            // Do something
                        } else {
                            body.append(bp.getContent());
                        }
                    }

                }
            } catch (IOException e) {
                e.printStackTrace();
                return "";
            } catch (MessagingException e) {
                e.printStackTrace();
                return "";
            }
            return body.toString();
        }

        private String dateToString(long timeInMilliSeconds) {
            Date date = new Date(timeInMilliSeconds);
            Calendar cal = new GregorianCalendar();
            cal.setTime(date);

            Calendar now = Calendar.getInstance();
            DateFormat dateForm = DateFormat.getDateInstance(DateFormat.SHORT);
            DateFormat timeForm = DateFormat.getTimeInstance(DateFormat.SHORT);

            // Check if this is today
            if (cal.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR) &&
                    now.get(Calendar.YEAR) == cal.get(Calendar.YEAR)){
                // Just return the time
                return timeForm.format(date);
            }

            // Check if this is yesterday
            if (cal.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR)-1 &&
                    now.get(Calendar.YEAR) == cal.get(Calendar.YEAR)){
                // Just return the time
                return "Yesterday";
            }

            // Check if this is this week
            if (cal.get(Calendar.DAY_OF_YEAR) > now.get(Calendar.DAY_OF_YEAR)-7 &&
                    now.get(Calendar.YEAR) == cal.get(Calendar.YEAR)){
                // Just return the day of the week
                SimpleDateFormat outFormat = new SimpleDateFormat("EEEE");
                return outFormat.format(date);
            }

            // Older that a week, show full date
            return dateForm.format(date);
        }

    }


    @Override
	public void onDismiss(DialogInterface dialog){
		super.onDismiss(dialog);
		if (mCompleteListener != null){
			mCompleteListener.onViewEmailDismissed();
		}
	}
	@Override
	public void onCancel (DialogInterface dialog){
		super.onDismiss(dialog);
		if (mCompleteListener != null){
			mCompleteListener.onViewEmailCancelled();
		}
	}

}
