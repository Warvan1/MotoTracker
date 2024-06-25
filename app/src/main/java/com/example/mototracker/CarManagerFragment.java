package com.example.mototracker;

import android.app.Dialog;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.time.Year;

public class CarManagerFragment extends Fragment implements RecyclerViewInterface {
    private Auth0Authentication _auth0;
    private JSONObjectWrapper _userProfile;
    private FragmentSwitcher _fragmentSwitcher;
    private JSONArrayWrapper _carModels;
    private RecyclerView _recyclerView;
    private CarManagerRecyclerViewAdapter _adapter;

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

        //get our recyclerView for our adapter
        _recyclerView = view.findViewById(R.id.car_manager_recycler_view);

        //retrieve all cars for the user
        new HTTPRequest(getString(R.string.api_base_url) + "/getcars")
                .setAuthToken(_auth0.getAccessToken(), _userProfile.getString("userid"))
                .setCallback(res -> {
                    JSONObjectWrapper resJson = new JSONObjectWrapper(res);
                    _carModels = resJson.getJSONArrayWrapper("cars");
                    //add a current_car value to all of the car models
                    for(int i = 0; i < _carModels.length(); i++){
                        _carModels.getJSONObjectWrapper(i).put("current_car", false);
                    }

                    //create an adapter for our recycler view using the retrieved data
                    _adapter = new CarManagerRecyclerViewAdapter(this.getContext(), _carModels, this);
                    _recyclerView.setAdapter(_adapter);
                    _recyclerView.setLayoutManager(new LinearLayoutManager(this.getContext()));

                    //highlight the current Car
                    highlightCurrentCar(resJson.getInt("current_car"));
                }).runAsync();

        //floating action button on click method for adding cars
        FloatingActionButton addCarButton = view.findViewById(R.id.add_car_btn);
        addCarButton.setOnClickListener(v -> {
            //create and show add car popup window
            Dialog viewAddCarForm = new Dialog(this.requireContext());
            viewAddCarForm.setContentView(R.layout.add_car_form);
            viewAddCarForm.show();

            //access form data
            EditText name = viewAddCarForm.findViewById(R.id.add_car_name);
            EditText year = viewAddCarForm.findViewById(R.id.add_car_year);
            EditText make = viewAddCarForm.findViewById(R.id.add_car_make);
            EditText model = viewAddCarForm.findViewById(R.id.add_car_model);
            EditText miles = viewAddCarForm.findViewById(R.id.add_car_miles);
            TextView error_message = viewAddCarForm.findViewById(R.id.add_car_error_message);
            error_message.setVisibility(View.GONE);

            //add car submit button onclick listener
            Button addCarSubmitButton = viewAddCarForm.findViewById(R.id.add_car_submit_btn);
            addCarSubmitButton.setOnClickListener(v2 -> {
                //Input Validation
                if(verifyStringLength(error_message, name.getText().toString(), 15, "Name")){
                    return;
                }
                if(year.getText().toString().isEmpty()){
                    error_message.setText(String.format(getString(R.string.nameIsRequired), "Year"));
                    error_message.setVisibility(View.VISIBLE);
                    return;
                }
                if(Integer.parseInt(year.getText().toString()) < 1900){
                    error_message.setText(R.string.yearMinMessage);
                    error_message.setVisibility(View.VISIBLE);
                    return;
                }
                if(Integer.parseInt(year.getText().toString()) > Year.now().getValue() + 1){
                    error_message.setText(String.format(getString(R.string.yearMaxMessage), (Year.now().getValue() + 1)));
                    error_message.setVisibility(View.VISIBLE);
                    return;
                }
                if(verifyStringLength(error_message, make.getText().toString(), 10, "Make")){
                    return;
                }
                if(verifyStringLength(error_message, model.getText().toString(), 10, "Model")){
                    return;
                }
                if(miles.getText().toString().isEmpty()){
                    miles.setText("0");
                }

                //close the form
                viewAddCarForm.dismiss();

                //retrieve the form data into a json object
                JSONObjectWrapper addCarJSON = new JSONObjectWrapper();
                addCarJSON.put("name", name.getText().toString());
                addCarJSON.put("year", Integer.parseInt(year.getText().toString()));
                addCarJSON.put("make", make.getText().toString());
                addCarJSON.put("model", model.getText().toString());
                addCarJSON.put("miles", Integer.parseInt(miles.getText().toString()));
                addCarJSON.put("picture", "");

                //send new car object to the server
                new HTTPRequest(getString(R.string.api_base_url) + "/addcar").setMethod("POST")
                        .setAuthToken(_auth0.getAccessToken(), _userProfile.getString("userid"))
                        .setData(addCarJSON).setCallback(res -> {
                            //remove all current car highlights
                            highlightCurrentCar(0);
                            //add car model from server to our recycler view
                            JSONObjectWrapper carModel = new JSONObjectWrapper(res);
                            carModel.put("current_car", true);
                            _carModels.put(carModel);
                            _adapter.notifyItemInserted(_carModels.length());
                        }).runAsync();
            });
        });
        return view;
    }

    @Override
    public void onItemClick(int position) {
        int car_id = _carModels.getJSONObjectWrapper(position).getInt("car_id");

        //highlight the clicked car
        highlightCurrentCar(car_id);

        //set the current car id for the user
        JSONObjectWrapper query = new JSONObjectWrapper();
        query.put("car_id", car_id);

        new HTTPRequest(getString(R.string.api_base_url) + "/setcurrentcar").setQueries(query)
                .setAuthToken(_auth0.getAccessToken(), _userProfile.getString("userid")).runAsync();
    }

    @Override
    public void onItemLongClick(int position) {
        int car_id = _carModels.getJSONObjectWrapper(position).getInt("car_id");

        //create and show the delete car popup window
        Dialog viewDeleteCarForm = new Dialog(this.requireContext());
        viewDeleteCarForm.setContentView(R.layout.delete_car_form);
        viewDeleteCarForm.show();

        //access form data
        EditText name = viewDeleteCarForm.findViewById(R.id.delete_car_name);

        //delete car forever button onClick listener
        Button deleteCarButton = viewDeleteCarForm.findViewById(R.id.delete_car_forever_btn);
        deleteCarButton.setOnClickListener(v -> {
            if(_carModels.getJSONObjectWrapper(position).getString("name").equals(name.getText().toString())){
                //close the form
                viewDeleteCarForm.dismiss();

                JSONObjectWrapper query = new JSONObjectWrapper();
                query.put("car_id", car_id);

                new HTTPRequest(getString(R.string.api_base_url) + "/deletecar").setQueries(query)
                        .setAuthToken(_auth0.getAccessToken(), _userProfile.getString("userid")).runAsync();

                _carModels.remove(position);
                _adapter.notifyItemRemoved(position);
            }
            else{
                Toast.makeText(this.getContext(), "Did not delete: Make sure name matches car name.", Toast.LENGTH_LONG).show();
            }
        });
    }

    public void highlightCurrentCar(int currentCar){
        for(int i = 0; i < _carModels.length(); i++){
            if(_carModels.getJSONObjectWrapper(i).getBoolean("current_car")){
                _carModels.getJSONObjectWrapper(i).put("current_car", false);
                _adapter.notifyItemChanged(i);
            }
            if(_carModels.getJSONObjectWrapper(i).getInt("car_id") == currentCar){
                _carModels.getJSONObjectWrapper(i).put("current_car", true);
                _adapter.notifyItemChanged(i);
            }
        }
    }

    public boolean verifyStringLength(TextView error_message, String str, int max, String name){
        if(str.isEmpty()){
            error_message.setText(String.format(getString(R.string.nameIsRequired), name));
            error_message.setVisibility(View.VISIBLE);
            return true;
        }
        else if(str.length() > max){
            error_message.setText(String.format(getString(R.string.nameMaxLength), name, max));
            error_message.setVisibility(View.VISIBLE);
            return true;
        }
        return false;
    }

}