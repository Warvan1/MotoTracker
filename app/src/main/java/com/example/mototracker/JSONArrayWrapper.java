package com.example.mototracker;

import android.util.Log;

import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class JSONArrayWrapper {
    private final JSONArray _jsonArray;
    JSONArrayWrapper(){
        _jsonArray = new JSONArray();
    }
    JSONArrayWrapper(String jsonString){
        try{
            _jsonArray = new JSONArray(jsonString);
        } catch (JSONException e){
            Log.d("JsonWrapper", "error: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }
    JSONArrayWrapper(JSONArray jsonArray){
        _jsonArray = jsonArray;
    }
    public JSONArray getJSONArray(){
        return _jsonArray;
    }
    @NonNull
    public String toString(){
        return _jsonArray.toString();
    }
    public int length(){
        return _jsonArray.length();
    }
    public void put(String value){
        _jsonArray.put(value);
    }
    public void put(int value){
        _jsonArray.put(value);
    }
    public void put(long value){
        _jsonArray.put(value);
    }
    public void put(double value){
        try{
            _jsonArray.put(value);
        } catch(JSONException e){
            Log.d("JsonWrapper", "error: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }
    public void put(boolean value){
        _jsonArray.put(value);
    }
    public void put(JSONObject value){
        _jsonArray.put(value);
    }
    public void put(JSONArray value){
        _jsonArray.put(value);
    }
    public void put(JSONObjectWrapper value){
        _jsonArray.put(value.getJSONObject());
    }
    public void put(JSONArrayWrapper value){
        _jsonArray.put(value.getJSONArray());
    }
    public String getString(int index){
        try{
            return _jsonArray.getString(index);
        } catch (JSONException e){
            Log.d("JsonWrapper", "error: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }
    public int getInt(int index){
        try{
            return _jsonArray.getInt(index);
        } catch (JSONException e){
            Log.d("JsonWrapper", "error: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }
    public long getLong(int index){
        try{
            return _jsonArray.getLong(index);
        } catch (JSONException e){
            Log.d("JsonWrapper", "error: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }
    public double getDouble(int index){
        try{
            return _jsonArray.getDouble(index);
        } catch (JSONException e){
            Log.d("JsonWrapper", "error: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }
    public boolean getBoolean(int index){
        try{
            return _jsonArray.getBoolean(index);
        } catch (JSONException e){
            Log.d("JsonWrapper", "error: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }
    public JSONObject getJSONObject(int index){
        try{
            return _jsonArray.getJSONObject(index);
        } catch (JSONException e){
            Log.d("JsonWrapper", "error: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }
    public JSONObjectWrapper getJSONObjectWrapper(int index){
        try{
            return new JSONObjectWrapper(_jsonArray.getJSONObject(index));
        } catch (JSONException e){
            Log.d("JsonWrapper", "error: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }
    public JSONArray getJSONArray(int index){
        try{
            return _jsonArray.getJSONArray(index);
        } catch (JSONException e){
            Log.d("JsonWrapper", "error: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }
    public JSONArrayWrapper getJSONArrayWrapper(int index){
        try{
            return new JSONArrayWrapper(_jsonArray.getJSONArray(index));
        } catch (JSONException e){
            Log.d("JsonWrapper", "error: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }
    public void remove(int index){
        _jsonArray.remove(index);
    }

}
