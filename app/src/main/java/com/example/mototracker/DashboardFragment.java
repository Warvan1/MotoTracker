package com.example.mototracker;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class DashboardFragment extends Fragment {
    private Auth0Authentication _auth0;
    private JSONObjectWrapper _userProfile;
    private FragmentSwitcher _fragmentSwitcher;

    public DashboardFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_dashboard, container, false);

        //access the fragment switcher object
        _fragmentSwitcher = FragmentSwitcher.getInstance();

        //check to make sure that we are authenticated
        _auth0 = Auth0Authentication.getInstance(this.getContext());
        if(!_auth0.isAuthenticated()){
            _fragmentSwitcher.switchFragment(new HomeFragment(), getParentFragmentManager());
            return view;
        }
        _userProfile = _auth0.getUserProfile();

        //retrieve user data from the database
        new HTTPRequest(getString(R.string.api_base_url) + "/getuser")
                .setAuthToken(_auth0.getAccessToken(), _userProfile.getString("userid")).setCallback(res -> {
                    if(res.equals("null")){
                        return;
                    }
                    JSONObjectWrapper resJSON = new JSONObjectWrapper(res);
                    Log.d("code", "user: " + resJSON.getString("email"));
                }).runAsync();

        new HTTPRequest(getString(R.string.api_base_url) + "/getcurrentcar")
                .setAuthToken(_auth0.getAccessToken(), _userProfile.getString("userid")).setCallback(res -> {
                    if(res.equals("null")){
                        return;
                    }
                    JSONObjectWrapper resJSON = new JSONObjectWrapper(res);
                    Log.d("getcurrentcar", "onCreateView: " + resJSON);
                }).runAsync();

        return view;
    }
}