package com.abstractwombat.networking;

import android.os.AsyncTask;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Mike on 4/24/2015.
 */
public class HttpForJson {
    public interface PostReceiver{
        public void receiveJSON(JSONObject json);
    }

    private String mUrl;

    public HttpForJson(String url){
        mUrl = url;
    }

    public JSONObject preform(NameValuePair... parameters){
        try {
            HttpClient httpclient = new DefaultHttpClient();
            HttpPost httppost = new HttpPost(mUrl);

            List<NameValuePair> params = new ArrayList<NameValuePair>();
            for (NameValuePair p : parameters) {
                params.add(p);
            }
            httppost.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));

            HttpResponse response = httpclient.execute(httppost);
            HttpEntity entity = response.getEntity();
            if (entity == null){
                return null;
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(entity.getContent(), "UTF-8"));
            String json = reader.readLine();

            JSONObject jsonObject = new JSONObject(json);
            return jsonObject;
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void preformAsync(final PostReceiver receiver, final NameValuePair... nameValuePairs){
        new AsyncTask<NameValuePair, Void, JSONObject>(){
            @Override
            protected JSONObject doInBackground(NameValuePair... params) {
                return preform(nameValuePairs);
            }
            @Override
            protected void onPostExecute(JSONObject jsonObject) {
                receiver.receiveJSON(jsonObject);
            }
        }.execute(nameValuePairs);
    }

}
