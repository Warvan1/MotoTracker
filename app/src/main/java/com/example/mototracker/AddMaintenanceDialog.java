package com.example.mototracker;

import android.app.Activity;
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
        int parsedOdometer = 0;
        try{
            parsedCost = addMaintenanceDataJSON.getDouble("parsedCost");
        }
        catch(RuntimeException e){}
        try{
            parsedGallons = addMaintenanceDataJSON.getDouble("parsedGallons");
        }
        catch(RuntimeException e){}
        try{
            parsedOdometer = addMaintenanceDataJSON.getInt("parsedOdometer");
        }
        catch(RuntimeException e){}

        String savedCost = "";
        String savedGallons = "";
        String savedMiles = "";
        String savedNotes = "";
        String savedType = "";
        try{
            savedCost = addMaintenanceDataJSON.getString("savedCost");
        }
        catch(RuntimeException e){}
        try{
            savedGallons = addMaintenanceDataJSON.getString("savedGallons");
        }
        catch(RuntimeException e){}
        try{
            savedMiles = addMaintenanceDataJSON.getString("savedMiles");
        }
        catch(RuntimeException e){}
        try{
            savedNotes = addMaintenanceDataJSON.getString("savedNotes");
        }
        catch(RuntimeException e){}
        try{
            savedType = addMaintenanceDataJSON.getString("savedType");
        }
        catch(RuntimeException e){}

        //create and show add maintenance popup window
        Dialog viewAddMaintenanceForm = new Dialog(context);
        viewAddMaintenanceForm.setContentView(R.layout.add_maintenance_form);
        viewAddMaintenanceForm.show();

        //access form data
        Button readGasPumpButton = viewAddMaintenanceForm.findViewById(R.id.add_maintenance_read_gas_pump);
        Button readOdometerButton = viewAddMaintenanceForm.findViewById(R.id.add_maintenance_read_odometer);
        EditText cost = viewAddMaintenanceForm.findViewById(R.id.add_maintenance_cost);
        EditText gallons = viewAddMaintenanceForm.findViewById(R.id.add_maintenance_gallons);
        TextView gallonsTitle = viewAddMaintenanceForm.findViewById(R.id.add_maintenance_gallons_title);
        EditText miles = viewAddMaintenanceForm.findViewById(R.id.add_maintenance_miles);
        EditText notes = viewAddMaintenanceForm.findViewById(R.id.add_maintenance_notes);
        final String[] type = {"Fuel"};

        //set text values for input
        if(parsedCost != 0){
            cost.setText(String.valueOf(parsedCost));
        }
        else if(!savedCost.isEmpty()){
            cost.setText(savedCost);
        }
        if(parsedGallons != 0){
            gallons.setText(String.valueOf(parsedGallons));
        }
        else if(!savedGallons.isEmpty()){
            gallons.setText(savedGallons);
        }
        if(parsedOdometer > currentCarJSON.getInt("miles")){
            miles.setText(String.valueOf(parsedOdometer));
        }
        else if(!savedMiles.isEmpty()){
            miles.setText(savedMiles);
        }
        else{
            miles.setText(currentCarJSON.getString("miles"));
        }
        if(!savedNotes.isEmpty()){
            notes.setText(savedNotes);
        }
        if(!savedType.isEmpty()){
            type[0] = savedType;
        }

        //setup add maintenance type dropdown menu
        Spinner type_spinner = viewAddMaintenanceForm.findViewById(R.id.add_maintenance_service_types_spinner);
        ArrayAdapter<CharSequence> type_adapter = ArrayAdapter.createFromResource(context,
                R.array.service_types, android.R.layout.simple_spinner_item);
        type_adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        type_spinner.setAdapter(type_adapter);
        int starting_position = getPositionOfItem(context, R.array.service_types, type[0]);
        if(starting_position != -1){
            type_spinner.setSelection(starting_position);
        }
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
            addMaintenanceDataJSON.put("taskIdentifier", "Gas Pump");
            saveFormData(addMaintenanceDataJSON, cost.getText().toString(),
                    gallons.getText().toString(), miles.getText().toString(), notes.getText().toString(), type[0]);

            CameraFragment fragment = CameraFragment.newInstance(addMaintenanceDataJSON.toString());
            fragmentSwitcher.switchFragment(fragment, fragmentManager);
        });

        //read odometer picture onClick listener
        readOdometerButton.setOnClickListener(v2 -> {
            viewAddMaintenanceForm.dismiss();

            addMaintenanceDataJSON.put("task", "Parse Text");
            addMaintenanceDataJSON.put("taskIdentifier", "Odometer");
            saveFormData(addMaintenanceDataJSON, cost.getText().toString(),
                    gallons.getText().toString(), miles.getText().toString(), notes.getText().toString(), type[0]);

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
                new HTTPRequest((Activity) context,context.getResources().getString(R.string.api_base_url) + "/addmaintenance").setQueries(query)
                        .setMethod("POST").setAuthToken(auth0.getAccessToken(), userProfile.getString("userid"))
                        .setData(addMaintenanceJSON).setCallback(res -> {
                            callback.accept("");
                        }).runAsync();
            }
            else{
                new HTTPRequest((Activity) context,context.getResources().getString(R.string.api_base_url) + "/addmaintenance").setQueries(query)
                        .setMethod("POST").setAuthToken(auth0.getAccessToken(), userProfile.getString("userid"))
                        .setData(addMaintenanceJSON).runAsync();
            }
        });
    }

    public static void saveFormData(JSONObjectWrapper addMaintenanceDataJSON, String savedCost, String savedGallons, String savedMiles, String savedNotes, String savedType){
        if(!savedCost.isEmpty()){
            addMaintenanceDataJSON.put("savedCost", savedCost);
        }
        if(!savedGallons.isEmpty()){
            addMaintenanceDataJSON.put("savedGallons", savedGallons);
        }
        if(!savedMiles.isEmpty()){
            addMaintenanceDataJSON.put("savedMiles", savedMiles);
        }
        if(!savedNotes.isEmpty()){
            addMaintenanceDataJSON.put("savedNotes", savedNotes);
        }
        if(!savedType.isEmpty()){
            addMaintenanceDataJSON.put("savedType", savedType);
        }
    }

    public static int getPositionOfItem(Context context, int arrayResourceId, String itemToFind){
        String[] array = context.getResources().getStringArray(arrayResourceId);
        for(int i = 0; i < array.length; i++){
            if(array[i].equals(itemToFind)){
                return i;
            }
        }
        return -1;
    }

    public static String parseCameraOutput(String addMaintenanceDataJSONString){
        JSONObjectWrapper addMaintenanceDataJSON = new JSONObjectWrapper(addMaintenanceDataJSONString);
        //get the parsedText and the taskIdentifier
        String text;
        String taskIdentifier;
        try{
            text = addMaintenanceDataJSON.getString("parsedText");
            taskIdentifier = addMaintenanceDataJSON.getString("taskIdentifier");
        }
        catch(RuntimeException e){
            return addMaintenanceDataJSONString;
        }

        if(taskIdentifier.equals("Gas Pump")){
            String[] lines = text.split("\n");
            ArrayList<Double> linesDouble = new ArrayList<>();
            for (int i = 0; i < lines.length; i++) {
                try{
                    linesDouble.add(Double.parseDouble(lines[i]));
                }
                catch(NumberFormatException e){
                    linesDouble.add(0.0);
                }
            }

            if(lines.length < 2){
                return addMaintenanceDataJSONString;
            }

            //Cost is the largest parsed number
            double max = Collections.max(linesDouble);
            addMaintenanceDataJSON.put("parsedCost", max);
            linesDouble.remove(max);
            //Gallons is the second largest number
            addMaintenanceDataJSON.put("parsedGallons", Collections.max(linesDouble));
        }

        if(taskIdentifier.equals("Odometer")){
            String[] lines = text.split("\n");
            ArrayList<Integer> linesInteger = new ArrayList<>();
            for (int i = 0; i < lines.length; i++) {
                try{
                    linesInteger.add(Integer.parseInt(lines[i]));
                }
                catch(NumberFormatException e){
                    linesInteger.add(0);
                }
            }

            if(lines.length < 1){
                return addMaintenanceDataJSONString;
            }

            //odometer reading is the largest parsed integer
            addMaintenanceDataJSON.put("parsedOdometer", Collections.max(linesInteger));
        }

        return addMaintenanceDataJSON.toString();
    }
}
