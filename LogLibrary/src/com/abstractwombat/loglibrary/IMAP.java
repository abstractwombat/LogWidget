package com.abstractwombat.loglibrary;

import android.util.Log;

import java.util.ArrayList;
import java.util.Date;
import java.util.Properties;
import java.util.TreeMap;

import javax.mail.AuthenticationFailedException;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.search.MessageIDTerm;
import javax.mail.search.SearchTerm;
import javax.mail.search.SentDateTerm;

import com.sun.mail.imap.IMAPFolder;
import com.sun.mail.imap.IMAPSSLStore;
import com.sun.mail.imap.IMAPStore;

public class IMAP {
    private static final String TAG = "IMAP";
	private static final long maxEmailAgeDays = 20;
	private IMAPStore store;
    private boolean oAuth2Initialized;

    public IMAP(){
        oAuth2Initialized = false;
	}

    public void disconnect(){
        if (this.store == null) return;
        try {
            this.store.close();
        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }

    public int connectWithLogin(String server, int port, String username, String password){
        Log.d(TAG, "Login: " + username + ":" + password + "@" + server + ":" + port);
        try {
            Properties props = System.getProperties();
            props.setProperty("mail.store.protocol", "imaps");
            Session session = Session.getDefaultInstance(props, null);
            this.store = (IMAPSSLStore) session.getStore("imaps");
            store.connect(server, port, username, password);
        } catch (IllegalStateException e) {
            e.printStackTrace();
            Log.d(TAG, "Failed to connect! Already connected");
            return 3;   // Already connected to the IMAP server
        } catch (AuthenticationFailedException e) {
            e.printStackTrace();
            Log.d(TAG, "Failed to connect! Authentication error");
            return 2;   // Invalid Username and/or Password
        } catch (MessagingException e) {
            e.printStackTrace();
            Log.d(TAG, "Failed to connect! Unknown failure");
            return 1;   // Failed to connect to the IMAP server
        }
        if (this.store.isConnected()){
            return 0;   // Success
        }else{
            return 5;// Success, but not connected
        }
    }

    public boolean connect(String server, int port, String email, String oAuth2Token){
        if (!oAuth2Initialized){
            OAuth2Helper.initialize();
            oAuth2Initialized = true;
        }
   		try {
            this.store = OAuth2Helper.connectToImap(server, port, email, oAuth2Token, true);
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
        return this.store.isConnected();
	}

    public String[] getFolders(){
        ArrayList<String> folderList = new ArrayList<String>();
        Folder[] folders = new Folder[0];
        try {
            folders = this.store.getDefaultFolder().list("*");
            for (javax.mail.Folder folder : folders) {
                if ((folder.getType() & javax.mail.Folder.HOLDS_MESSAGES) != 0) {
                    folderList.add(folder.getFullName());
                    // folder.getMessageCount());
                }
            }
        } catch (MessagingException e) {
            e.printStackTrace();
            Log.d(TAG, "Failed to list IMAP folders");
            return null;
        }
        return (String[])folderList.toArray();
    }

    public Message getMessage(String folderName, String messageID){
        // Open the folder
        IMAPFolder folder;
        try {
            folder = (IMAPFolder) store.getFolder(folderName);

            folder.open(Folder.READ_ONLY);
            MessageIDTerm search = new MessageIDTerm(messageID);
            Message[] messages = folder.search(search);

            if (messages != null && messages.length > 0){
                return messages[0];
            }else{
                return null;
            }

        } catch (MessagingException e) {
            return null;
        }
    }

    public Message[] getRecentMessages(String folderName, long maxAgeDays){
		long MAX_EMAIL_AGE = System.currentTimeMillis() - (1000 * 60 * 60 * 24/* hours */* maxAgeDays /* days */);
		Message[] messages = null;

		// Open the folder
		IMAPFolder folder;
		try {
			folder = (IMAPFolder) store.getFolder(folderName);

			folder.open(Folder.READ_ONLY);

			// Get all the "Recent" email
			SearchTerm search = new SentDateTerm(javax.mail.search.ComparisonTerm.GT,  new Date(MAX_EMAIL_AGE));
			messages = folder.search(search);
            Log.d(TAG, "Got " + messages.length + " messages");
			
			// Sort the messages
			TreeMap<Long, Message> sortedMessages = new TreeMap<Long, Message>();
			for (Message m : messages){
				long time = m.getReceivedDate().getTime();
				sortedMessages.put(time, m);
                Log.d(TAG, "Added 1 message");
			}
			ArrayList<Message> messageList = new ArrayList<Message>();
			Long currentTime = sortedMessages.lastKey();
			do {
				Message m = sortedMessages.get(currentTime);
				messageList.add(m);
				Long nextTime = sortedMessages.lowerKey(currentTime);
				currentTime = nextTime;
			} while (currentTime != null);

		} catch (MessagingException e) {
            Log.d(TAG, "Messaging exception in getRecentMessages");
			return messages;
		}
		
		return messages;
	}

}
