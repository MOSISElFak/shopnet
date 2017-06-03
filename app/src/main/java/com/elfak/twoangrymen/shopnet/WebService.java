package com.elfak.twoangrymen.shopnet;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.telecom.Call;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;

/**
 * Created by Bencun on 5/30/2017.
 */

public class WebService extends AsyncTask<String, Void, ServerResponse> {
    private static String ServerURL = "http://192.168.1.32";
    private String serverResponse = null;

    @Override
    protected ServerResponse doInBackground(String... params) {
        ServerResponse odgovor = null;
        URL url;
        HttpURLConnection urlConnection = null;
        String dataJSON = null;
        if(params.length > 2)
            dataJSON = params[2];
        try{
            //novi url = url servera + parametar
            url = new URL(ServerURL + params[0]);
            //otvori konekciju
            urlConnection = (HttpURLConnection)url.openConnection();
            //podesi GET ili POST
            urlConnection.setRequestMethod(params[1]);
            //podesi tip zahteva
            urlConnection.setRequestProperty("Content-Type","application/json");
            if(params[1].equals("POST")){
                DataOutputStream os = new DataOutputStream(urlConnection.getOutputStream());
                os.writeBytes(URLEncoder.encode(params[2], "UTF-8"));
                os.flush();
                os.close();
            }
            int responseCode = urlConnection.getResponseCode();
            Log.v("WebService:", "Uspostavljam vezu...");
            //ukoliko server odgovori sa 200 OK
            if(responseCode == 200){
                Log.v("WebService:", "Kod 200");
                serverResponse = URLDecoder.decode(readStream(urlConnection.getInputStream()), "UTF-8");
                odgovor = new ServerResponse(responseCode, serverResponse);
            }
            if(responseCode == 400){
                Log.v("WebService:", "Kod 400");
                serverResponse = URLDecoder.decode(readStream(urlConnection.getErrorStream()), "UTF-8");
                odgovor = new ServerResponse(responseCode, serverResponse);
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return odgovor;
    }

    /*@Override
    protected void onPostExecute(String s){
        super.onPostExecute(s);
        //Log.e("Odgovor u WebService: ", "" + serverResponse);
        caller.onBackgroundTaskComplete(serverResponse);
    }*/

    private String readStream(InputStream in) {
        BufferedReader reader = null;
        StringBuffer response = new StringBuffer();
        try {
            reader = new BufferedReader(new InputStreamReader(in));
            String line = "";
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return response.toString();
    }

}
