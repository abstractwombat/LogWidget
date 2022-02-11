package com.abstractwombat.iab;

import android.app.Activity;
import android.app.Fragment;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Toast;

import com.abstractwombat.networking.HttpForJson;
import com.android.vending.billing.IInAppBillingService;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.UUID;

/**
 * InAppPurchases
 *      Helper for inspection and purchasing of in app products. This class' life must be tied to
 *      the passed in Activity. The onActivityResult should be called from the Activity's
 *      onActivityResult function. The destroy function should be called from the Activity's
 *      onDestroy function.
 *
 * Created on 7/9/2015.
 */
public class InAppPurchases implements ServiceConnection, HttpForJson.PostReceiver {
    private static final String TAG = "InAppPurchases";

    public interface DataReceiver{
        void dataAvailable();
        void purchaseComplete(String productId, boolean success);
    }

    private static final int PURCHASE_INTENT_REQUEST_CODE = 1001;

    private Context mContext;
    private Activity mActivity;
    private IInAppBillingService mService;
    private DataReceiver mReceiver;

    public InAppPurchases(Context context, Activity activity, DataReceiver receiver){
        mContext = context;
        mActivity = activity;
        mReceiver = receiver;

        // Bind service for in-app billing
        Intent serviceIntent = new Intent("com.android.vending.billing.InAppBillingService.BIND");
        serviceIntent.setPackage("com.android.vending");
        mContext.bindService(serviceIntent, this, Context.BIND_AUTO_CREATE);
    }

    public void destroy() {
        if (mService != null) {
            mContext.unbindService(this);
        }
    }
    public Boolean isOwned(String productId){
        Bundle ownedItems = null;
        try {
            ownedItems = mService.getPurchases(3, mContext.getPackageName(), "inapp", null);
        } catch (RemoteException e) {
            return null;
        }
        // Get the response
        int response = ownedItems.getInt("RESPONSE_CODE");
        if (response == 0) {
            // Get the response data
            ArrayList<String> ownedSkus =
                    ownedItems.getStringArrayList("INAPP_PURCHASE_ITEM_LIST");
            ArrayList<String> purchaseDataList =
                    ownedItems.getStringArrayList("INAPP_PURCHASE_DATA_LIST");
            ArrayList<String> signatureList =
                    ownedItems.getStringArrayList("INAPP_DATA_SIGNATURE_LIST");
            String continuationToken =
                    ownedItems.getString("INAPP_CONTINUATION_TOKEN");

            // Iterate over the purchased item
            for (int i = 0; i < purchaseDataList.size(); ++i) {
                String purchaseData = purchaseDataList.get(i);
                String signature = signatureList.get(i);
                String sku = ownedSkus.get(i);

                if (productId.equals(sku)){
                    return true;
                }
            }
        }
        return false;
    }

    /* Purchase Procedure */
    private JSONObject payload;

    public boolean onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode != PURCHASE_INTENT_REQUEST_CODE){
            return false;
        }

        if (resultCode == Activity.RESULT_OK) {
            Log.d(TAG, "Received RESULT_OK from in app purchase");
            int responseCode = data.getIntExtra("RESPONSE_CODE", 0);
            String purchaseData = data.getStringExtra("INAPP_PURCHASE_DATA");
            String dataSignature = data.getStringExtra("INAPP_DATA_SIGNATURE");

            try {
                JSONObject jo = new JSONObject(purchaseData);
                String sku = jo.getString("productId");

                // Verify the payload
                String receivedPayload = jo.getString("developerPayload");
                JSONObject receivedPayloadJSON = new JSONObject(receivedPayload);
                String receivedProductId = receivedPayloadJSON.getString("productId");
                String receivedUuid = receivedPayloadJSON.getString("uuid");
                if (receivedUuid.equals(payload.getString("uuid")) && receivedProductId.equals
                        (payload.getString("productId")) && receivedProductId.equals(sku)) {
                    Log.d(TAG, "Local verification passed for product " + sku);
                    if (mReceiver != null){
                        mReceiver.purchaseComplete(sku, true);
                    }
                    // Verify the signature
                    verifyPurchase(sku, purchaseData, dataSignature);
                    return true;
                }else{
                    Log.d(TAG, "Local verification failed for product " + sku);
                }
            }
            catch (JSONException e) {
                Toast.makeText(mActivity, "Failed to parse purchase data.", Toast.LENGTH_LONG).show();
                e.printStackTrace();
            }
        }

        return false;
    }

    public void purchase(String productId){
        Log.d(TAG, "purchase - " + productId);
        try {
            // Create the payload
            payload = new JSONObject();
            payload.put("productId",productId);
            payload.put("uuid", UUID.randomUUID().toString());

            // Get the purchase intent
            Bundle buyIntentBundle = mService.getBuyIntent(3, mContext.getPackageName(), productId, "inapp", payload.toString());
            int response = buyIntentBundle.getInt("RESPONSE_CODE");
            if (response == 0) {
                // Start a purchase pending intent
                PendingIntent pendingIntent = buyIntentBundle.getParcelable("BUY_INTENT");
                Fragment f = new Fragment();

                mActivity.startIntentSenderForResult(pendingIntent.getIntentSender(),
                        PURCHASE_INTENT_REQUEST_CODE, new Intent(), Integer.valueOf(0), Integer.valueOf(0),
                        Integer.valueOf(0));
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (IntentSender.SendIntentException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private boolean verifyPurchase(String productId, String signedData, String signature){
        Log.d(TAG, "verifyPurchase - " + productId);
        HttpForJson httpForJson = new HttpForJson("http://www.abstractwombat.com/android/verifyPurchase.php");

        NameValuePair productPair = new BasicNameValuePair("product", productId);
        NameValuePair dataPair = new BasicNameValuePair("signed_data", signedData);
        NameValuePair signaturePair = new BasicNameValuePair("signature", signature);
        NameValuePair packagePair = new BasicNameValuePair("package", mContext.getPackageName());
        httpForJson.preformAsync(this, productPair, dataPair, signaturePair, packagePair);
        return false;
    }

    @Override
    public void receiveJSON(JSONObject json) {
        Log.d(TAG, "receiveJSON");

        String productId = json.optString("product", "");
        String error = json.optString("error", "");
        int valid = json.optInt("valid", 0);
        int result = json.optInt("result", 0);
        Log.d(TAG, "product: " + productId + " error: " + error + " valid: " + valid + " result: " + result);

        // Check for errors
        if (error != null && error != "null" && !error.isEmpty()){
            Toast.makeText(mActivity, "Signature verification error [" + error + "]", Toast.LENGTH_LONG).show();
            return;
        }

        if (valid == 1){
            // Valid signature
            Toast.makeText(mActivity, "Signature verified", Toast.LENGTH_LONG).show();
            if (mReceiver != null){
                mReceiver.purchaseComplete(productId, true);
            }
        }else{
            // Invalid signature
            Toast.makeText(mActivity, "Signature invalid!", Toast.LENGTH_LONG).show();
            mReceiver.purchaseComplete(productId, false);
        }
    }


    private ArrayList<Boolean> purchased(ArrayList<String> productIds){
        Bundle ownedItems = null;
        try {
            ownedItems = mService.getPurchases(3, mContext.getPackageName(), "inapp", null);
        } catch (RemoteException e) {
            return null;
        }
        // Initialize the return array
        ArrayList<Boolean> purchased = new ArrayList<>();
        for (int i=0; i<productIds.size(); i++) purchased.add(false);
        // Get the response
        int response = ownedItems.getInt("RESPONSE_CODE");
        if (response == 0) {
            // Get the response data
            ArrayList<String> ownedSkus =
                    ownedItems.getStringArrayList("INAPP_PURCHASE_ITEM_LIST");
            ArrayList<String>  purchaseDataList =
                    ownedItems.getStringArrayList("INAPP_PURCHASE_DATA_LIST");
            ArrayList<String>  signatureList =
                    ownedItems.getStringArrayList("INAPP_DATA_SIGNATURE_LIST");
            String continuationToken =
                    ownedItems.getString("INAPP_CONTINUATION_TOKEN");

            // Iterate over the purchased item
            for (int i = 0; i < purchaseDataList.size(); ++i) {
                String purchaseData = purchaseDataList.get(i);
                String signature = signatureList.get(i);
                String sku = ownedSkus.get(i);

                // Verify the signature
                int index = productIds.indexOf(sku);
                if (index != -1){
                    purchased.set(index, true);
                }
            }
        }
        return purchased;
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        mService = IInAppBillingService.Stub.asInterface(service);
        if (mReceiver != null){
            mReceiver.dataAvailable();
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        mService = null;

    }


}
