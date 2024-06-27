package com.example.mototracker;

import android.os.Bundle;

import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;

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
        TextView carName = view.findViewById(R.id.dashboard_car_name);
        TextView carYear = view.findViewById(R.id.dashboard_car_year);
        TextView carMake = view.findViewById(R.id.dashboard_car_make);
        TextView carModel = view.findViewById(R.id.dashboard_car_model);
        TextView carTotalCosts = view.findViewById(R.id.dashboard_car_total_costs);
        TextView carMiles = view.findViewById(R.id.dashboard_car_miles);
        TextView fuelMilesPerGallon = view.findViewById(R.id.dashboard_fuel_mpg);
        TextView fuelDollarsPerGallon = view.findViewById(R.id.dashboard_fuel_dpg);
        TextView fuelDollarsPerMile = view.findViewById(R.id.dashboard_fuel_dpm);

        new HTTPRequest(getString(R.string.api_base_url) + "/getcurrentcar")
                .setAuthToken(_auth0.getAccessToken(), _userProfile.getString("userid")).setCallback(res -> {
                    if(res.equals("null")){
                        carCard.setVisibility(View.GONE);
                        fuelCard.setVisibility(View.GONE);
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
                        dpg = resJSON.getDouble("total_costs") / resJSON.getDouble("total_gallons");
                    }
                    fuelDollarsPerGallon.setText(String.format(getString(R.string.dollarsPerGallonFormat), df.format(dpg)));
                    double dpm = 0;
                    if(resJSON.getDouble("miles") != 0){
                        dpm = resJSON.getDouble("total_costs") / resJSON.getDouble("miles");
                    }
                    fuelDollarsPerMile.setText(String.format(getString(R.string.dollarsPerMileFormat), df.format(dpm)));

                }).runAsync();

        return view;
    }
}