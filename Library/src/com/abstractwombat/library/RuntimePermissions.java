package com.abstractwombat.library;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by Mike on 11/23/2015.
 */
public class RuntimePermissions {
    private final String TAG = "RuntimePermissions";
    private Activity mActivity;
    private String mAppName;

    public interface Listener{
        void gotPermissions(int[] permissions);
    }

    public RuntimePermissions(Activity activity, String appName){
        mActivity = activity;
        mAppName = appName;
    }

    private class RequestData{
        int mRequestCode;
        ArrayList<String> mRequestedPermissions;
        Listener mListener;
    }
    HashMap<Integer, RequestData> mRequestData;

    public void startPermissionCheck(ArrayList<String> permissions, ArrayList<String>
            permissionLabels, int requestCode, Listener listener) {
        final RequestData rData = new RequestData();
        rData.mListener = listener;
        rData.mRequestCode = requestCode;

        final ArrayList<String> permissionsNeeded = new ArrayList<>();
        ArrayList<String> permissionsNeededLabels = new ArrayList<>();
        for (int i = 0; i < permissions.size(); i++) {
            String p = permissions.get(i);
            if (!hasPermission(mActivity, p)) {
                permissionsNeeded.add(p);
                if (ActivityCompat.shouldShowRequestPermissionRationale(mActivity, p)) {
                    permissionsNeededLabels.add(permissionLabels.get(i));
                }
            }
        }

        rData.mRequestedPermissions = new ArrayList<>(permissionsNeeded);
        if (mRequestData == null){
            mRequestData = new HashMap<>();
        }
        mRequestData.put(requestCode, rData);

        if (permissionsNeeded.size() > 0) {
            if (permissionsNeededLabels.size() > 0) {
                // Need Rationale
                String message = "Please grant permission to\n  " + permissionsNeededLabels
                        .get(0);
                for (int i = 1; i < permissionsNeededLabels.size(); i++) {
                    message = message + "\n  " + permissionsNeededLabels.get(i);
                }
                showMessageOKCancel(mAppName, message, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ActivityCompat.requestPermissions(mActivity, permissionsNeeded.toArray(new
                                String[permissionsNeeded
                                .size()]), rData.mRequestCode);
                    }
                });
                return;
            }
        }

        if (permissionsNeeded.isEmpty()){
            int[] granted = new int[permissions.size()];
            for (int i=0; i<permissions.size(); i++){
                granted[i] = PackageManager.PERMISSION_GRANTED;
            }
            sendPermissionsToListener(rData.mRequestCode, granted);
        }else {
            ActivityCompat.requestPermissions(mActivity, permissionsNeeded.toArray(new
                    String[permissionsNeeded.size()]), rData.mRequestCode);
        }
        return;
    }

    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (!mRequestData.containsKey(requestCode)){
            return;
        }
        RequestData rData = mRequestData.get(requestCode);

        // Initial
        int[] listenersResults = new int[rData.mRequestedPermissions.size()];
        for (int i=0; i<rData.mRequestedPermissions.size(); i++) {
            listenersResults[i] = PackageManager.PERMISSION_GRANTED;
        }

        for (int i=0; i<permissions.length; i++) {
            String p = permissions[i];
            int index = rData.mRequestedPermissions.indexOf(p);
            if (index >= 0) {
                listenersResults[index] = grantResults[i];
            }
        }
        sendPermissionsToListener(rData.mRequestCode, listenersResults);
    }

    private void sendPermissionsToListener(int requestCode, int[] permissions){
        if (!mRequestData.containsKey(requestCode)){
            return;
        }
        RequestData rData = mRequestData.get(requestCode);
        if (rData.mListener != null){
            rData.mListener.gotPermissions(permissions);
        }
        mRequestData.remove(requestCode);
    }

    private void showMessageOKCancel(String title, String message, DialogInterface.OnClickListener okListener) {
        new AlertDialog.Builder(mActivity)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", okListener)
                .setNegativeButton("Cancel", null)
                .create()
                .show();
    }

    public static boolean hasPermission(Context context, String permission){
        if (ActivityCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
            return true;
        }
        return false;
    }

}
