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

import java.util.ArrayList;
import java.util.Collections;
import java.util.function.Consumer;

public class AddMaintenanceDialog {

    public static void open(Context context, FragmentSwitcher fragmentSwitcher, FragmentManager fragmentManager, JSONObjectWrapper currentCarJSON, JSONObjectWrapper addMaintenanceDataJSON, Consumer<String> callback){
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

        double parsedCost = 0;
        double parsedGallons = 0;
        try{
            parsedCost = addMaintenanceDataJSON.getDouble("parsedCost");
        }
        catch(RuntimeException e){}
        try{
            parsedGallons = addMaintenanceDataJSON.getDouble("parsedGallons");
        }
        catch(RuntimeException e){}

        //create and show add maintenance popup window
        Dialog viewAddMaintenanceForm = new Dialog(context);
        viewAddMaintenanceForm.setContentView(R.layout.add_maintenance_form);
        viewAddMaintenanceForm.show();

        //access form data
        Button readGasPumpButton = viewAddMaintenanceForm.findViewById(R.id.add_maintenance_read_gas_pump_picture);
        EditText cost = viewAddMaintenanceForm.findViewById(R.id.add_maintenance_cost);
        EditText gallons = viewAddMaintenanceForm.findViewById(R.id.add_maintenance_gallons);
        if(parsedCost != 0){
            cost.setText(String.valueOf(parsedCost));
        }
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

            addMaintenanceDataJSON.put("task", "Parse Text");
            CameraFragment fragment = CameraFragment.newInstance(addMaintenanceDataJSON.toString());
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

    public static String parseCameraOutput(String addMaintenanceDataJSONString){
        JSONObjectWrapper addMaintenanceDataJSON = new JSONObjectWrapper(addMaintenanceDataJSONString);
        String text;
        try{
            text = addMaintenanceDataJSON.getString("parsedText");
        }
        catch(RuntimeException e){
            return addMaintenanceDataJSON.toString();
        }

        String[] lines = text.split("\n");
        if(lines.length < 2){
            return addMaintenanceDataJSON.toString();
        }

        ArrayList<Double> linesDouble = new ArrayList<>();
        for (int i = 0; i < lines.length; i++) {
            try{
                linesDouble.add(Double.parseDouble(lines[i]));
            }
            catch(NumberFormatException e){
                linesDouble.add(0.0);
            }
        }
        //Cost is the largest parsed number
        double max = Collections.max(linesDouble);
        addMaintenanceDataJSON.put("parsedCost", max);
        linesDouble.remove(max);
        //Gallons is the second largest number
        addMaintenanceDataJSON.put("parsedGallons", Collections.max(linesDouble));

        return addMaintenanceDataJSON.toString();
    }
}
