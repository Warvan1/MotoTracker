package com.example.mototracker;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.auth0.android.Auth0;
import com.auth0.android.authentication.AuthenticationAPIClient;
import com.auth0.android.authentication.AuthenticationException;
import com.auth0.android.callback.Callback;
import com.auth0.android.provider.WebAuthProvider;
import com.auth0.android.result.Credentials;
import com.auth0.android.result.UserProfile;

import java.util.function.Consumer;

//singleton Auth0 Authentication class
public class Auth0Authentication {
    private static Auth0Authentication _auth0Object;
    private Context _context;
    private Auth0 _auth0;
    private String _accessToken;
    private String _tokenType;
    private JSONObjectWrapper _userProfile;
    //private constructor for singleton
    private Auth0Authentication(Context context){
        _context = context;
    }
    public static Auth0Authentication getInstance(Context context){
        if(_auth0Object == null){
            _auth0Object = new Auth0Authentication(context);
            _auth0Object.initAuth0();
        }
        else{
            _auth0Object.setContext(context);
        }
        return _auth0Object;
    }
    private void setContext(Context context){
        _context = context;
    }
    private void initAuth0(){
        _auth0 = new Auth0(_context);
    }
    public String getAccessToken(){
        return String.format("%s %s", _tokenType, _accessToken);
    }
    public JSONObjectWrapper getUserProfile(){
        return _userProfile;
    }
    public Boolean isAuthenticated(){
        return _accessToken != null;
    }

    public void handleLoginOrLogout(Consumer<Number> callback){
        if(_auth0Object.isAuthenticated()){
            handleLogout(callback);
        }
        else{
            handleLogin(callback);
        }
    }

    public void handleLogin(Consumer<Number> callback){
        if(_auth0Object.isAuthenticated()){
            callback.accept(0);
            return;
        }

        WebAuthProvider.login(_auth0)
                .withScheme(_context.getString(R.string.auth0_scheme))
                .withScope("openid profile email")
                .withAudience(_context.getString(R.string.api_audience))
                .start(_context, new Callback<Credentials, AuthenticationException>() {
                    @Override
                    public void onSuccess(Credentials credentials) {
                        Log.d("code", "LOGIN SUCCESS");
                        _accessToken = credentials.getAccessToken();
                        _tokenType = credentials.getType();
                        initUserProfile(callback); //pass the callback to the initUserProfile function
                    }

                    @Override
                    public void onFailure(@NonNull AuthenticationException e) {
                        Log.d("code", "LOGIN FAILURE " + e.getMessage());
                        callback.accept(1);
                    }
                });

    }

    public void handleLogout(Consumer<Number> callback){
        if(!_auth0Object.isAuthenticated()){
            callback.accept(0);
            return;
        }

        WebAuthProvider.logout(_auth0)
                .withScheme(_context.getString(R.string.auth0_scheme))
                .start(_context, new Callback<Void, AuthenticationException>() {
                    @Override
                    public void onSuccess(Void unused) {
                        Log.d("code", "LOGOUT SUCCESS");
                        _accessToken = null;
                        _tokenType = null;
                        callback.accept(0);
                    }

                    @Override
                    public void onFailure(@NonNull AuthenticationException e) {
                        Log.d("code", "LOGOUT FAILURE:" + e.getMessage());
                        callback.accept(2);
                    }
                });
    }

    private void initUserProfile(Consumer<Number> callback){
        AuthenticationAPIClient client = new AuthenticationAPIClient(_auth0);

        client.userInfo(_accessToken)
                .start(new Callback<UserProfile, AuthenticationException>() {
                    @Override
                    public void onSuccess(UserProfile userProfile) {
                        _userProfile = new JSONObjectWrapper();
                        _userProfile.put("userid", userProfile.getId());
                        _userProfile.put("email", userProfile.getEmail());
                        _userProfile.put("email_verified", Boolean.TRUE.equals(userProfile.isEmailVerified()));

                        callback.accept(0);
                    }

                    @Override
                    public void onFailure(@NonNull AuthenticationException e) {
                        Log.d("code", "Failed to get profile: " + e.getMessage());
                        callback.accept(3);
                    }
                });
    }

}
