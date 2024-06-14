package com.example.mototracker;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.material.navigation.NavigationView;

import java.util.Objects;

public class DashboardFragment extends Fragment {
    private Auth0Authentication _auth0;
    private JSONObjectWrapper _userProfile;

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

        //check to make sure that we are authenticated
        _auth0 = Auth0Authentication.getInstance(this.getContext());
        if(!_auth0.isAuthenticated()){
            fragmentSwitcher(new HomeFragment());
        }
        _userProfile = _auth0.getUserProfile();

        //retrieve user data from the database
        new HTTPRequest(getString(R.string.api_base_url) + "/getuser")
                .setAuthToken(_auth0.getAccessToken(), _userProfile.getString("user_id")).setCallback(res -> {
                    JSONObjectWrapper resJSON = new JSONObjectWrapper(res);
                    Log.d("code", "user: " + resJSON.getString("email"));
                }).runAsync();

        return view;
    }

    private void fragmentSwitcher(Fragment fragment){
        FragmentManager fragmentManager = getParentFragmentManager();
        fragmentManager.beginTransaction().replace(R.id.flFragment, fragment).addToBackStack(null).commit();

        //handle updating the menu checked state when switching fragments
        NavigationView navigationView = requireActivity().findViewById(R.id.nav);
        Menu menu = navigationView.getMenu();
        for (int i = 0; i < menu.size(); i++){
            menu.getItem(i).setChecked(false);
        }
        if(fragment instanceof HomeFragment){
            menu.findItem(R.id.home).setChecked(true);
        }
        else if(fragment instanceof DashboardFragment){
            menu.findItem(R.id.dashboard).setChecked(true);
        }
    }
}