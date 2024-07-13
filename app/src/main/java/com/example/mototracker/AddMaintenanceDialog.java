package com.example.mototracker;

import android.app.Dialog;
import android.content.Context;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.FragmentManager;

import java.util.function.Consumer;

public class AddMaintenanceDialog {

    public static void open(Context context, String fragmentName, FragmentSwitcher fragmentSwitcher, FragmentManager fragmentManager, JSONObjectWrapper currentCarJSON, double parsedCost, double parsedGallons, Consumer<String> callback){
        //if the _currentCarJSON object is null show error and return
        if(currentCarJSON == null){
            Toast.makeText(context, "Add or Select a car in the Car Manager to add maintenance.", Toast.LENGTH_LONG).show();
            return;
        }
        Auth0Authentication auth0 = Auth0Authentication.getInstance(context);
        if(!auth0.isAuthenticated()){
            fragmentSwitcher.switchFragment(new HomeFragment(), fragmentManager);
            return;
        }
        JSONObjectWrapper userProfile = auth0.getUserProfile();

        //create and show add maintenance popup window
        Dialog viewAddMaintenanceForm = new Dialog(context);
        viewAddMaintenanceForm.setContentView(R.layout.add_maintenance_form);
        viewAddMaintenanceForm.show();

        //access form data
        Button readGasPumpButton = viewAddMaintenanceForm.findViewById(R.id.add_maintenance_read_gas_pump_picture);
        EditText cost = viewAddMaintenanceForm.findViewById(R.id.add_maintenance_cost);
        if(parsedCost != 0){
            cost.setText(String.valueOf(parsedCost));
        }
        EditText gallons = viewAddMaintenanceForm.findViewById(R.id.add_maintenance_gallons);
        if(parsedGallons != 0){
            gallons.setText(String.valueOf(parsedGallons));
        }
        TextView gallonsTitle = viewAddMaintenanceForm.findViewById(R.id.add_maintenance_gallons_title);
        EditText miles = viewAddMaintenanceForm.findViewById(R.id.add_maintenance_miles);
        miles.setText(currentCarJSON.getString("miles"));
        EditText notes = viewAddMaintenanceForm.findViewById(R.id.add_maintenance_notes);
        final String[] type = {"Fuel"};

        //setup add maintenance type dropdown menu
        Spinner type_spinner = viewAddMaintenanceForm.findViewById(R.id.add_maintenance_service_types_spinner);
        ArrayAdapter<CharSequence> type_adapter = ArrayAdapter.createFromResource(context,
                R.array.service_types, android.R.layout.simple_spinner_item);
        type_adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        type_spinner.setAdapter(type_adapter);
        //item selected listener for our dropdown menu
        type_spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                type[0] = parent.getItemAtPosition(position).toString();
                if(type[0].equals("Fuel")){
                    gallons.setVisibility(View.VISIBLE);
                    gallonsTitle.setVisibility(View.VISIBLE);
                    readGasPumpButton.setVisibility(View.VISIBLE);
                }
                else{
                    gallons.setVisibility(View.GONE);
                    gallonsTitle.setVisibility(View.GONE);
                    readGasPumpButton.setVisibility(View.GONE);
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        //read gas pump picture onClick listener
        readGasPumpButton.setOnClickListener(v2 -> {
            viewAddMaintenanceForm.dismiss();

            CameraFragment fragment = CameraFragment.newInstance(fragmentName, "Parse Text");
            fragmentSwitcher.switchFragment(fragment, fragmentManager);
        });

        //add maintenance submit button onClick listener
        Button addMaintenanceSubmitButton = viewAddMaintenanceForm.findViewById(R.id.add_maintenance_submit_btn);
        addMaintenanceSubmitButton.setOnClickListener(v2 -> {
            //Input Validation
            if(miles.getText().toString().isEmpty() || Integer.parseInt(miles.getText().toString()) < currentCarJSON.getInt("miles")){
                miles.setText(currentCarJSON.getString("miles"));
            }
            if(cost.getText().toString().isEmpty()){
                cost.setText("0");
            }
            if(gallons.getText().toString().isEmpty() || !type[0].equals("Fuel")){
                gallons.setText("0");
            }
            if(notes.getText().toString().isEmpty()){
                notes.setText(" ");
            }

            //close the form
            viewAddMaintenanceForm.dismiss();

            //retrieve the form data into a json object
            JSONObjectWrapper addMaintenanceJSON = new JSONObjectWrapper();
            addMaintenanceJSON.put("type", type[0]);
            addMaintenanceJSON.put("cost", Double.parseDouble(cost.getText().toString()));
            addMaintenanceJSON.put("gallons", Double.parseDouble(gallons.getText().toString()));
            addMaintenanceJSON.put("miles", Integer.parseInt(miles.getText().toString()));
            addMaintenanceJSON.put("notes", notes.getText().toString());

            JSONObjectWrapper query = new JSONObjectWrapper();
            query.put("car_id", currentCarJSON.getInt("car_id"));

            //add a new maintenance item
            if(callback != null){
                new HTTPRequest(context.getResources().getString(R.string.api_base_url) + "/addmaintenance").setQueries(query)
                        .setMethod("POST").setAuthToken(auth0.getAccessToken(), userProfile.getString("userid"))
                        .setData(addMaintenanceJSON).setCallback(res -> {
                            callback.accept("");
                        }).runAsync();
            }
            else{
                new HTTPRequest(context.getResources().getString(R.string.api_base_url) + "/addmaintenance").setQueries(query)
                        .setMethod("POST").setAuthToken(auth0.getAccessToken(), userProfile.getString("userid"))
                        .setData(addMaintenanceJSON).runAsync();
            }
        });

    }
}
