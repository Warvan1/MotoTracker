package com.example.mototracker;

import android.app.Dialog;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class MaintenanceLogFragment extends Fragment {
    private Auth0Authentication _auth0;
    private JSONObjectWrapper _userProfile;
    private FragmentSwitcher _fragmentSwitcher;
    private JSONObjectWrapper _currentCarJSON;

    public MaintenanceLogFragment() {
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
        View view = inflater.inflate(R.layout.fragment_maintenance_log, container, false);

        //access the fragment switcher object
        _fragmentSwitcher = FragmentSwitcher.getInstance();

        //check to make sure that we are authenticated
        _auth0 = Auth0Authentication.getInstance(this.getContext());
        if(!_auth0.isAuthenticated()){
            _fragmentSwitcher.switchFragment(new HomeFragment(), getParentFragmentManager());
            return view;
        }
        _userProfile = _auth0.getUserProfile();

        //get the current car object
        new HTTPRequest(getString(R.string.api_base_url) + "/getcurrentcar")
                .setAuthToken(_auth0.getAccessToken(), _userProfile.getString("user_id")).setCallback(res -> {
                    if(res.equals("null")){
                        return;
                    }
                    _currentCarJSON = new JSONObjectWrapper(res);
                }).runAsync();

        //add maintenance dialog form view
        Dialog viewAddMaintenanceForm = new Dialog(this.requireContext());

        //floating action button on click method for adding maintenance
        FloatingActionButton addMaintenanceButton = view.findViewById(R.id.add_maintenance_btn);
        addMaintenanceButton.setOnClickListener(v -> {
            //if the _currentCarJSON object is null show error and return
            if(_currentCarJSON == null){
                Toast.makeText(this.getContext(), "Add or Select a car in the Car Manager to add maintenance.", Toast.LENGTH_LONG).show();
                return;
            }
            //create and show popup window
            viewAddMaintenanceForm.setContentView(R.layout.add_maintenance_form);
            viewAddMaintenanceForm.show();

            //access form data
            EditText cost = viewAddMaintenanceForm.findViewById(R.id.add_maintenance_cost);
            EditText miles = viewAddMaintenanceForm.findViewById(R.id.add_maintenance_miles);
            EditText notes = viewAddMaintenanceForm.findViewById(R.id.add_maintenance_notes);
            final String[] type = {"Fuel"};

            //setup add maintenance type dropdown menu
            Spinner type_spinner = viewAddMaintenanceForm.findViewById(R.id.add_maintenance_service_types_spinner);
            ArrayAdapter<CharSequence> type_adapter = ArrayAdapter.createFromResource(this.requireContext(),
                    R.array.service_types, android.R.layout.simple_spinner_item);
            type_adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            type_spinner.setAdapter(type_adapter);
            //item selected listener for our dropdown menu
            type_spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    type[0] = parent.getItemAtPosition(position).toString();
                }
                @Override
                public void onNothingSelected(AdapterView<?> parent) {}
            });

            //add maintenance onClick listener
            Button addMaintenanceSubmitButton = viewAddMaintenanceForm.findViewById(R.id.add_maintenance_submit_btn);
            addMaintenanceSubmitButton.setOnClickListener(v2 -> {
                //TODO: add input validation

                //close the form
                viewAddMaintenanceForm.dismiss();

                //retrieve the form data into a json object
                JSONObjectWrapper addMaintenanceJSON = new JSONObjectWrapper();
                addMaintenanceJSON.put("type", type[0]);
                addMaintenanceJSON.put("cost", Integer.parseInt(cost.getText().toString()));
                addMaintenanceJSON.put("miles", Integer.parseInt(miles.getText().toString()));
                addMaintenanceJSON.put("notes", notes.getText().toString());

                Log.d("maintenanceJSON", "json: " + addMaintenanceJSON);
                new HTTPRequest(getString(R.string.api_base_url) + "/addmaintenance?car_id=" + _currentCarJSON.getInt("car_id"))
                        .setMethod("POST").setAuthToken(_auth0.getAccessToken(), _userProfile.getString("user_id"))
                        .setData(addMaintenanceJSON.toString()).runAsync();
            });
        });

        return view;
    }
}