package com.example.mototracker;

import android.os.Bundle;

import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

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

        //get dashboard view items
        CardView carCard = view.findViewById(R.id.dashboard_car_card);
        CardView fuelCard = view.findViewById(R.id.dashboard_fuel_card);
        CardView oilChangeCard = view.findViewById(R.id.dashboard_oil_change_card);
        CardView tireRotationCard = view.findViewById(R.id.dashboard_tire_rotation_card);
        CardView airFilterCard = view.findViewById(R.id.dashboard_air_filter_card);
        CardView inspectionCard = view.findViewById(R.id.dashboard_inspection_card);
        CardView registrationCard = view.findViewById(R.id.dashboard_registration_card);
        TextView carName = view.findViewById(R.id.dashboard_car_name);
        TextView carYear = view.findViewById(R.id.dashboard_car_year);
        TextView carMake = view.findViewById(R.id.dashboard_car_make);
        TextView carModel = view.findViewById(R.id.dashboard_car_model);
        TextView carTotalCosts = view.findViewById(R.id.dashboard_car_total_costs);
        TextView carMiles = view.findViewById(R.id.dashboard_car_miles);
        TextView fuelMilesPerGallon = view.findViewById(R.id.dashboard_fuel_mpg);
        TextView fuelDollarsPerGallon = view.findViewById(R.id.dashboard_fuel_dpg);
        TextView fuelDollarsPerMile = view.findViewById(R.id.dashboard_fuel_dpm);
        TextView oilChangeTime = view.findViewById(R.id.dashboard_oil_change_time);
        TextView oilChangeMiles = view.findViewById(R.id.dashboard_oil_change_miles);
        TextView tireRotationTime = view.findViewById(R.id.dashboard_tire_rotation_time);
        TextView tireRotationMiles = view.findViewById(R.id.dashboard_tire_rotation_miles);
        TextView airFilterTime = view.findViewById(R.id.dashboard_air_filter_time);
        TextView airFilterMiles = view.findViewById(R.id.dashboard_air_filter_miles);
        TextView inspectionTime = view.findViewById(R.id.dashboard_inspection_time);
        TextView registrationTime = view.findViewById(R.id.dashboard_registration_time);

        new HTTPRequest(getString(R.string.api_base_url) + "/getcurrentcar")
                .setAuthToken(_auth0.getAccessToken(), _userProfile.getString("userid")).setCallback(res -> {
                    if(res.equals("null")){
                        carCard.setVisibility(View.GONE);
                        fuelCard.setVisibility(View.GONE);
                        oilChangeCard.setVisibility(View.GONE);
                        tireRotationCard.setVisibility(View.GONE);
                        airFilterCard.setVisibility(View.GONE);
                        inspectionCard.setVisibility(View.GONE);
                        registrationCard.setVisibility(View.GONE);
                        return;
                    }
                    carCard.setVisibility(View.VISIBLE);
                    fuelCard.setVisibility(View.VISIBLE);

                    //json object containing all of our car data
                    JSONObjectWrapper resJSON = new JSONObjectWrapper(res);
                    //used to round doubles to 2 decimal places
                    DecimalFormat df = new DecimalFormat("###.##");

                    //set dashboard view items using the returned car data in resJSON
                    carName.setText(resJSON.getString("name"));
                    carYear.setText(resJSON.getString("year"));
                    carMake.setText(resJSON.getString("make"));
                    carModel.setText(resJSON.getString("model"));
                    carMiles.setText(String.format(getString(R.string.milesFormat), resJSON.getString("miles")));
                    carTotalCosts.setText(String.format(getString(R.string.totalCostsFormat), resJSON.getString("total_costs")));
                    carMiles.setText(String.format(getString(R.string.milesFormat), resJSON.getString("miles")));
                    double mpg = 0;
                    if(resJSON.getDouble("total_gallons") != 0){
                        mpg = resJSON.getInt("miles") / resJSON.getDouble("total_gallons");
                    }
                    fuelMilesPerGallon.setText(String.format(getString(R.string.milesPerGallonFormat), df.format(mpg)));
                    double dpg = 0;
                    if(resJSON.getDouble("total_gallons") != 0){
                        dpg = resJSON.getDouble("total_fuel_costs") / resJSON.getDouble("total_gallons");
                    }
                    fuelDollarsPerGallon.setText(String.format(getString(R.string.dollarsPerGallonFormat), df.format(dpg)));
                    double dpm = 0;
                    if(resJSON.getDouble("miles") != 0){
                        dpm = resJSON.getDouble("total_fuel_costs") / resJSON.getDouble("miles");
                    }
                    fuelDollarsPerMile.setText(String.format(getString(R.string.dollarsPerMileFormat), df.format(dpm)));

                    handleEventTracking(resJSON, oilChangeCard, oilChangeTime, oilChangeMiles, "oil_change_time", "oil_change_miles");
                    handleEventTracking(resJSON, tireRotationCard, tireRotationTime, tireRotationMiles, "tire_rotation_time", "tire_rotation_miles");
                    handleEventTracking(resJSON, airFilterCard, airFilterTime, airFilterMiles, "air_filter_time", "air_filter_miles");
                    handleEventTracking(resJSON, inspectionCard, inspectionTime, "inspection_time");
                    handleEventTracking(resJSON, registrationCard, registrationTime, "registration_time");

                }).runAsync();

        return view;
    }

    public void handleEventTracking(JSONObjectWrapper resJSON, CardView cardView, TextView timeView, TextView milesView, String time, String miles){
        if(resJSON.getInt(time) == 0){
            cardView.setVisibility(View.GONE);
        }
        else{
            cardView.setVisibility(View.VISIBLE);
            String formatedTimeString = formatTimeInterval(resJSON.getLong(time), new Date().getTime());
            int milesDifference = resJSON.getInt("miles") - resJSON.getInt(miles);
            timeView.setText(String.format(getString(R.string.timeSinceLastFormat), formatedTimeString));
            milesView.setText(String.format(getString(R.string.milesSinceLastFormat), milesDifference));
        }
    }
    public void handleEventTracking(JSONObjectWrapper resJSON, CardView cardView, TextView timeView, String time){
        if(resJSON.getInt(time) == 0){
            cardView.setVisibility(View.GONE);
        }
        else{
            cardView.setVisibility(View.VISIBLE);
            String formatedTimeString = formatTimeInterval(resJSON.getLong(time), new Date().getTime());
            timeView.setText(String.format(getString(R.string.timeSinceLastFormat), formatedTimeString));
        }
    }

    public String formatTimeInterval(long startTimestamp, long endTimestamp){
        // Create Calendars for start and end timestamps
        Calendar startCal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        startCal.setTimeInMillis(startTimestamp);

        Calendar endCal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        endCal.setTimeInMillis(endTimestamp);

        int years = endCal.get(Calendar.YEAR) - startCal.get(Calendar.YEAR);
        int months = endCal.get(Calendar.MONTH) - startCal.get(Calendar.MONTH);
        int days = endCal.get(Calendar.DAY_OF_MONTH) - startCal.get(Calendar.DAY_OF_MONTH);

        if (days < 0) {
            startCal.add(Calendar.MONTH, 1);
            days += startCal.getActualMaximum(Calendar.DAY_OF_MONTH);
            months--;
        }
        if (months < 0) {
            months += 12;
            years--;
        }

        if(years == 0 && months == 0){
            return String.format("%d days", days);
        }
        else if(years == 0){
            return String.format("%d months, %d days", months, days);
        }
        return String.format("%d years, %d months, %d days", years, months, days);
    }
}