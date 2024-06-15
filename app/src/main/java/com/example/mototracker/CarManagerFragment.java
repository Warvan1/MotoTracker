package com.example.mototracker;

import android.app.Dialog;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class CarManagerFragment extends Fragment {
    private Auth0Authentication _auth0;
    private JSONObjectWrapper _userProfile;
    private FragmentSwitcher _fragmentSwitcher;

    private JSONArrayWrapper _cars;

    public CarManagerFragment() {
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
        View view = inflater.inflate(R.layout.fragment_car_manager, container, false);

        //access the fragment switcher object
        _fragmentSwitcher = FragmentSwitcher.getInstance();

        //check to make sure that we are authenticated
        _auth0 = Auth0Authentication.getInstance(this.getContext());
        if(!_auth0.isAuthenticated()){
            _fragmentSwitcher.switchFragment(new HomeFragment(), getParentFragmentManager());
            return view;
        }
        _userProfile = _auth0.getUserProfile();

        //retrieve all cars for the user
        new HTTPRequest(getString(R.string.api_base_url) + "/getcars")
                .setAuthToken(_auth0.getAccessToken(), _userProfile.getString("user_id"))
                .setCallback(res -> {
                    _cars = new JSONArrayWrapper(res);
                    Log.d("cars", "onCreateView: " + _cars);
                }).runAsync();

        //add car dialog form view
        Dialog viewAddCarForm = new Dialog(this.requireContext());

        //floating action button on click method for adding cars
        FloatingActionButton addCarButton = view.findViewById(R.id.add_car_btn);
        addCarButton.setOnClickListener(v -> {
            //create and show the popup window
            viewAddCarForm.setContentView(R.layout.add_car_form);
            viewAddCarForm.show();

            //access form data
            EditText name = viewAddCarForm.findViewById(R.id.add_car_name);
            EditText year = viewAddCarForm.findViewById(R.id.add_car_year);
            EditText make = viewAddCarForm.findViewById(R.id.add_car_make);
            EditText model = viewAddCarForm.findViewById(R.id.add_car_model);
            EditText milage = viewAddCarForm.findViewById(R.id.add_car_milage);

            //add car button onclick listener
            Button addCarSubmitButton = viewAddCarForm.findViewById(R.id.add_car_submit_btn);
            addCarSubmitButton.setOnClickListener(v2 -> {
                //TODO: add input validation

                //close the form
                viewAddCarForm.dismiss();

                //retrieve the form data into a json object
                JSONObjectWrapper addCarJSON = new JSONObjectWrapper();
                addCarJSON.put("name", name.getText().toString());
                addCarJSON.put("year", Integer.parseInt(year.getText().toString()));
                addCarJSON.put("make", make.getText().toString());
                addCarJSON.put("model", model.getText().toString());
                addCarJSON.put("milage", Integer.parseInt(milage.getText().toString()));
                addCarJSON.put("picture", "");

                //send new car object to the server
                new HTTPRequest(getString(R.string.api_base_url) + "/addcar").setMethod("POST")
                        .setAuthToken(_auth0.getAccessToken(), _userProfile.getString("user_id"))
                        .setData(addCarJSON.toString()).runAsync();
            });

        });

        return view;
    }
}