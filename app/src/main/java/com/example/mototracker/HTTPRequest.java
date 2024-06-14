package com.example.mototracker;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

public class HTTPRequest{
    private final String _url;
    private String _method;
    private String _authToken;
    private String _user_id;
    private String _data;
    private Consumer<String> _callback;
    private String _response;
    public HTTPRequest(String url){
        _url = url;
        _method = "GET";
    }
    public HTTPRequest setMethod(String method){
        _method = method;
        return this;
    }
    public HTTPRequest setAuthToken(String token){
        _authToken = token;
        return this;
    }
    public HTTPRequest setAuthToken(String token, String user_id){
        _authToken = token;
        _user_id = user_id;
        return this;
    }
    public HTTPRequest setData(String data){
        _data = data;
        return this;
    }
    public HTTPRequest setCallback(Consumer<String> callback){
        _callback = callback;
        return this;
    }
    public void runAsync(){
        //create new background thread
        new Thread(new Runnable() {
            @Override
            public void run() {
                executeHttpRequest();

                //if there is a callback function
                //create an os handler to get the looper for the main ui thread
                //so that you can run the callback on the ui thread after the http request finishes
                if(_callback != null){
                    new Handler(Looper.getMainLooper()).post(() -> _callback.accept(_response));
                }
            }
        }).start();
    }

    private void executeHttpRequest() {
        StringBuilder responseBuilder = new StringBuilder();
        HttpURLConnection urlConnection = null;

        try {
            URL url = new URL(_url);

            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod(_method);
            if(_authToken != null) {
                urlConnection.setRequestProperty("Authorization", _authToken);
            }
            if(_user_id != null) {
                urlConnection.setRequestProperty("user_id", _user_id);
            }

            //handle outputting the data for the body of a post request
            if(_method.equals("POST") && _data != null){
                urlConnection.setRequestProperty("content-type", "application/json");
                urlConnection.setDoOutput(true);
                urlConnection.setDoInput(true);
                urlConnection.setChunkedStreamingMode(0);

                OutputStream out = new BufferedOutputStream(urlConnection.getOutputStream());
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8));
                writer.write(_data);
                writer.flush();
            }

            int code = urlConnection.getResponseCode();
            if (code != HttpURLConnection.HTTP_OK) {
                throw new IOException("Invalid response from server: " + code);
            }

            BufferedReader rd = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));

            String line;
            while ((line = rd.readLine()) != null) {
                responseBuilder.append(line);
            }
        } catch (Exception e) {
            Log.d("code", "catch url get: " + e.getMessage());
            //respond with the error
            responseBuilder = new StringBuilder("catch url get: " + e.getMessage());
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }

        _response = responseBuilder.toString();
    }
}
