package com.example.mototracker;

import android.content.ContentResolver;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.function.Consumer;

public class HTTPRequest{
    private String _url;
    private String _queries;
    private String _method;
    private String _authToken;
    private String _userid;
    private String _data;
    private String _contentType;
    private ContentResolver _contentResolver;
    private Uri _photoURI;
    private Consumer<String> _callback;
    private String _response;
    public HTTPRequest(String url){
        _url = url;
        _method = "GET";
        _contentType = "application/json";
    }
    public HTTPRequest setMethod(String method){
        _method = method;
        return this;
    }
    public HTTPRequest setAuthToken(String token){
        _authToken = token;
        return this;
    }
    public HTTPRequest setAuthToken(String token, String userid){
        _authToken = token;
        _userid = userid;
        return this;
    }
    public HTTPRequest setData(JSONObjectWrapper data){
        _data = data.toString();
        return this;
    }
    public HTTPRequest setContentType(String contentType){
        _contentType = contentType;
        return this;
    }
    public HTTPRequest setPhotoURI(ContentResolver contentResolver, String photoURI){
        _contentResolver = contentResolver;
        _photoURI = Uri.parse(photoURI);
        return this;
    }
    public HTTPRequest setCallback(Consumer<String> callback){
        _callback = callback;
        return this;
    }
    public HTTPRequest setQueries(JSONObjectWrapper queries){
        Iterator<String> keys = queries.keys();
        StringBuilder queryBuilder = new StringBuilder();
        while(keys.hasNext()){
            String key = keys.next();
            if(queryBuilder.length() == 0){
                queryBuilder.append("?");
            }
            else{
                queryBuilder.append("&");
            }
            queryBuilder.append(key).append("=").append(queries.getString(key));
        }
        _queries = queryBuilder.toString();
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
            if(_queries != null){
                _url += _queries;
            }
            URL url = new URL(_url);

            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod(_method);
            if(_authToken != null) {
                urlConnection.setRequestProperty("Authorization", _authToken);
            }
            if(_userid != null) {
                urlConnection.setRequestProperty("userid", _userid);
            }

            //handle outputting the data for the body of a post request
            if(_method.equals("POST")){
                urlConnection.setRequestProperty("content-type", _contentType);
                urlConnection.setDoOutput(true);
                urlConnection.setDoInput(true);
                urlConnection.setChunkedStreamingMode(0);

                if(_contentType.equals("application/json") && _data != null){
                    OutputStream out = new BufferedOutputStream(urlConnection.getOutputStream());
                    BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8));
                    writer.write(_data);
                    writer.flush();
                    writer.close();
                }
                else if(_contentType.equals("image/jpeg") && _photoURI != null){
                    DataOutputStream outputStream = new DataOutputStream(urlConnection.getOutputStream());
                    InputStream inputStream = _contentResolver.openInputStream(_photoURI);
                    if(inputStream == null){
                        throw new IOException("invalid file input stream");
                    }

                    int bytesAvailable = inputStream.available();
                    int bufferSize = Math.min(bytesAvailable, 4096);
                    byte[] buffer = new byte[bufferSize];

                    //read from the file to the output stream in 4096 byte sections
                    int bytesRead = inputStream.read(buffer, 0, bufferSize);
                    while(bytesRead > 0){
                        outputStream.write(buffer, 0, bytesRead);
                        bytesAvailable = inputStream.available();
                        bufferSize = Math.min(bytesAvailable, 4096);
                        bytesRead = inputStream.read(buffer, 0, bufferSize);
                    }

                    inputStream.close();
                    outputStream.flush();
                    outputStream.close();
                }
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
