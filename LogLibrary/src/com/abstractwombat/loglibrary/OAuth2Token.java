package com.abstractwombat.loglibrary;

import java.io.IOException;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;


public class OAuth2Token {
    private OnTokenAcquired onTokenAcquired;
    private HandlerCallback handlerCallback;
    private ResultReceiverActivity resultActivity;
    private Activity callingActivity;
    private AccountManager am;
    private Account account;
    private String authScope;
    private OAuth2TokenReceiver receiver;
    
    public interface OAuth2TokenReceiver{
    	public void receiveOAuth2Token(String token);
    	public void oAuth2Error(String e);
    }
    
    OAuth2Token(Activity activity, OAuth2TokenReceiver receiver){
    	this.callingActivity = activity;
    	this.receiver = receiver;
        this.handlerCallback = new HandlerCallback();
        this.handlerCallback.parent = this;
        this.resultActivity = new ResultReceiverActivity();
        this.resultActivity.parent = this;
        this.onTokenAcquired = new OnTokenAcquired();
        this.onTokenAcquired.activity = this.resultActivity;
        this.onTokenAcquired.parent = this;
        this.am = AccountManager.get(this.callingActivity);
		this.account = null;
		this.authScope = "";
    }

    public void begin(Account account, String authScope){
        this.account = account;
		this.authScope = authScope;
        this.begin();
    }
    
    public void begin(){
        Bundle options = new Bundle();

		this.am.getAuthToken(
			this.account,                 	// Account retrieved using getAccountsByType()
			this.authScope,        			// Auth scope
		    options,                        // Authenticator-specific options
		    this.callingActivity,                   // Your activity
		    this.onTokenAcquired,          // Callback called when a token is successfully acquired
		    new Handler(this.handlerCallback)    // Callback called if an error occurs
		);
	}

	private void setToken(String token){
    	this.receiver.receiveOAuth2Token(token);
	}
	
    private void error(String e){
    	this.receiver.oAuth2Error(e);
    }

	private class ResultReceiverActivity extends Activity{
        public OAuth2Token parent;
        
        protected void onActivityResult(int requestCode, int resultCode, Intent data){
        	if (resultCode == RESULT_OK) {
       		    this.parent.begin();
    		}else if (resultCode == RESULT_CANCELED){
    			parent.error("Cancelled");
    		}
    	}
    }
    
    private class HandlerCallback implements Handler.Callback{
        public OAuth2Token parent;

		@Override
		public boolean handleMessage(Message msg) {
			parent.error(msg.toString());
			return false;
		}
		
	}
	
	private class OnTokenAcquired implements AccountManagerCallback<Bundle> {
        public Activity activity;
        public OAuth2Token parent;

        @Override
		public void run(AccountManagerFuture<Bundle> result) {
            boolean error = false;
            
			// Get the result of the operation from the AccountManagerFuture.
			Bundle bundle = null;
			try {
				bundle = result.getResult();
			} catch (OperationCanceledException e) {
				e.printStackTrace();
                parent.error("Cancelled");
                error = true;
			} catch (AuthenticatorException e) {
				e.printStackTrace();
                parent.error("Authentication failed");
                error = true;
			} catch (IOException e) {
				e.printStackTrace();
                parent.error("Unknown error");
                error = true;
			}

            if (bundle == null){
				parent.error("Unknown error");
				return;
            }
            
            if (error){
                return;
            }

			Intent launch = (Intent) bundle.get(AccountManager.KEY_INTENT);
			if (launch != null) {
				this.activity.startActivityForResult(launch, 0);
				return;
			}else{
        		// The token is a named value in the bundle. The name of the value
    			// is stored in the constant AccountManager.KEY_AUTHTOKEN.
    			parent.setToken(bundle.getString(AccountManager.KEY_AUTHTOKEN));
			}
		}
	}

}
