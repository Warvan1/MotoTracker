package com.example.mototracker;

import android.util.Log;

import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;

public class JSONObjectWrapper {
    private final JSONObject _jsonObject;
    JSONObjectWrapper(){
        _jsonObject = new JSONObject();
    }
    JSONObjectWrapper(String jsonString){
        try{
            _jsonObject = new JSONObject(jsonString);
        } catch (JSONException e){
            Log.d("JsonWrapper", "error: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }
    JSONObjectWrapper(JSONObject jsonObject){
        _jsonObject = jsonObject;
    }
    public JSONObject getJSONObject(){
        return _jsonObject;
    }
    @NonNull
    public String toString(){
        return _jsonObject.toString();
    }
    public void put(String key, String value){
        try{
            _jsonObject.put(key, value);
        } catch (JSONException e){
            Log.d("JsonWrapper", "error: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }
    public void put(String key, int value){
        try{
            _jsonObject.put(key, value);
        } catch (JSONException e){
            Log.d("JsonWrapper", "error: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }
    public void put(String key, long value){
        try{
            _jsonObject.put(key, value);
        } catch (JSONException e){
            Log.d("JsonWrapper", "error: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }
    public void put(String key, double value){
        try{
            _jsonObject.put(key, value);
        } catch (JSONException e){
            Log.d("JsonWrapper", "error: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }
    public void put(String key, boolean value){
        try{
            _jsonObject.put(key, value);
        } catch (JSONException e){
            Log.d("JsonWrapper", "error: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }
    public void put(String key, JSONObject value){
        try{
            _jsonObject.put(key, value);
        } catch (JSONException e){
            Log.d("JsonWrapper", "error: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }
    public void put(String key, JSONArray value){
        try{
            _jsonObject.put(key, value);
        } catch (JSONException e){
            Log.d("JsonWrapper", "error: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }
    public void put(String key, JSONObjectWrapper value){
        try{
            _jsonObject.put(key, value.getJSONObject());
        } catch (JSONException e){
            Log.d("JsonWrapper", "error: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }
    public void put(String key, JSONArrayWrapper value){
        try{
            _jsonObject.put(key, value.getJSONArray());
        } catch (JSONException e){
            Log.d("JsonWrapper", "error: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }
    public String getString(String key){
        try{
            return _jsonObject.getString(key);
        } catch (JSONException e){
            Log.d("JsonWrapper", "error: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }
    public int getInt(String key){
        try{
            return _jsonObject.getInt(key);
        } catch (JSONException e){
            Log.d("JsonWrapper", "error: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }
    public long getLong(String key){
        try{
            return _jsonObject.getLong(key);
        } catch (JSONException e){
            Log.d("JsonWrapper", "error: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }
    public double getDouble(String key){
        try{
            return _jsonObject.getDouble(key);
        } catch (JSONException e){
            Log.d("JsonWrapper", "error: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }
    public boolean getBoolean(String key){
        try{
            return _jsonObject.getBoolean(key);
        } catch (JSONException e){
            Log.d("JsonWrapper", "error: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }
    public JSONObject getJSONObject(String key){
        try{
            return _jsonObject.getJSONObject(key);
        } catch (JSONException e){
            Log.d("JsonWrapper", "error: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }
    public JSONObjectWrapper getJSONObjectWrapper(String key){
        try{
            return new JSONObjectWrapper(_jsonObject.getJSONObject(key));
        } catch (JSONException e){
            Log.d("JsonWrapper", "error: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }
    public JSONArray getJSONArray(String key){
        try{
            return _jsonObject.getJSONArray(key);
        } catch (JSONException e){
            Log.d("JsonWrapper", "error: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }
    public JSONArrayWrapper getJSONArrayWrapper(String key){
        try{
            return new JSONArrayWrapper(_jsonObject.getJSONArray(key));
        } catch (JSONException e){
            Log.d("JsonWrapper", "error: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }
    public void remove(String key){
        _jsonObject.remove(key);
    }
    public Iterator<String> keys(){
        return _jsonObject.keys();
    }

}