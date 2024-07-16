package com.example.mototracker;

import android.app.Dialog;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.time.Year;

public class CarManagerFragment extends Fragment implements RecyclerViewInterface {
    private static final String ARG_METADATA_JSON = "metadataJSON";
    private JSONObjectWrapper _addMaintenanceDataJSON = new JSONObjectWrapper();
    private Auth0Authentication _auth0;
    private JSONObjectWrapper _userProfile;
    private FragmentSwitcher _fragmentSwitcher;
    private JSONArrayWrapper _carModels;
    private RecyclerView _recyclerView;
    private CarManagerRecyclerViewAdapter _adapter;

    public CarManagerFragment() {
        // Required empty public constructor
    }

    public static CarManagerFragment newInstance(String metadataJSON){
        CarManagerFragment fragment = new CarManagerFragment();
        Bundle args = new Bundle();
        args.putString(ARG_METADATA_JSON, metadataJSON);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(getArguments() != null){
            _addMaintenanceDataJSON = new JSONObjectWrapper(getArguments().getString(ARG_METADATA_JSON));
        }
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

        //handle uri input from camera fragment
        String uriString = "";
        try{
            uriString = _addMaintenanceDataJSON.getString("photoURI");
        }
        catch(RuntimeException e){}

        //if the uri input exists then we want to send the image to the server
        if(!uriString.isEmpty()){
            sendCarImageToServer();
        }

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
    public void onItemClick(int position, int id) {
        int car_id = _carModels.getJSONObjectWrapper(position).getInt("car_id");

        //handles clicking anywhere on the car card
        if(id == 0){
            //highlight the clicked car
            highlightCurrentCar(car_id);

            //set the current car id for the user
            JSONObjectWrapper query = new JSONObjectWrapper();
            query.put("car_id", car_id);

            new HTTPRequest(getString(R.string.api_base_url) + "/setcurrentcar").setQueries(query)
                    .setAuthToken(_auth0.getAccessToken(), _userProfile.getString("userid")).runAsync();
        }
        //share button on click handler
        else if(id == 1){
            //create and show add maintenance popup window
            Dialog viewShareCarForm = new Dialog(this.requireContext());
            viewShareCarForm.setContentView(R.layout.share_car_form);
            viewShareCarForm.show();

            //access form data
            EditText email = viewShareCarForm.findViewById(R.id.share_car_email);
            TextView error_message = viewShareCarForm.findViewById(R.id.share_car_error_message);
            final String[] permissions = {"View"};

            //setup permissions dropdown menu
            Spinner permission_spinner = viewShareCarForm.findViewById(R.id.share_car_permissions_spinner);
            ArrayAdapter<CharSequence> type_adapter = ArrayAdapter.createFromResource(this.requireContext(),
                    R.array.share_permissions, android.R.layout.simple_spinner_item);
            type_adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            permission_spinner.setAdapter(type_adapter);
            //item selected listener for the permissions dropdown menu
            permission_spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    permissions[0] = parent.getItemAtPosition(position).toString();
                }
                @Override
                public void onNothingSelected(AdapterView<?> parent) {}
            });

            Button shareCarSubmitButton = viewShareCarForm.findViewById(R.id.share_car_submit_button);
            shareCarSubmitButton.setOnClickListener(v -> {
                //handle input validation
                if(verifyStringLength(error_message, email.getText().toString(), 50, "Email")){
                    return;
                }

                //retrieve the form data into json object
                JSONObjectWrapper shareCarJSON = new JSONObjectWrapper();
                shareCarJSON.put("email", email.getText().toString());
                shareCarJSON.put("permissions", permissions[0]);

                JSONObjectWrapper query = new JSONObjectWrapper();
                query.put("car_id", car_id);

                new HTTPRequest(getString(R.string.api_base_url) + "/sharecar").setMethod("POST").setQueries(query)
                        .setAuthToken(_auth0.getAccessToken(), _userProfile.getString("userid"))
                        .setData(shareCarJSON).setCallback(res -> {
                            JSONObjectWrapper resJSON = new JSONObjectWrapper(res);
                            boolean success = resJSON.getBoolean("success");
                            if(success){
                                //close the form
                                viewShareCarForm.dismiss();
                            }
                            else{
                                error_message.setText(getString(R.string.noEmailAccount));
                                error_message.setVisibility(View.VISIBLE);
                            }
                        }).runAsync();
            });
        }
    }

    @Override
    public void onItemLongClick(int position, int id) {
        int car_id = _carModels.getJSONObjectWrapper(position).getInt("car_id");
        if(id == 0){
            openDeleteCarDialog(position, car_id);
        }
        else if(id == 1){
            getNewCarImage(position, car_id);
        }
    }

    public void openDeleteCarDialog(int position, int car_id){
        if(_carModels.getJSONObjectWrapper(position).getString("user_id").equals(_userProfile.getString("userid"))) {
            //create and show the delete car popup window
            Dialog viewDeleteCarForm = new Dialog(this.requireContext());
            viewDeleteCarForm.setContentView(R.layout.delete_car_form);
            viewDeleteCarForm.show();

            //access form data
            EditText name = viewDeleteCarForm.findViewById(R.id.delete_car_name);

            //delete car forever button onClick listener
            Button deleteCarButton = viewDeleteCarForm.findViewById(R.id.delete_car_forever_btn);
            deleteCarButton.setOnClickListener(v -> {
                if (_carModels.getJSONObjectWrapper(position).getString("name").equals(name.getText().toString())) {
                    //close the form
                    viewDeleteCarForm.dismiss();

                    JSONObjectWrapper query = new JSONObjectWrapper();
                    query.put("car_id", car_id);

                    new HTTPRequest(getString(R.string.api_base_url) + "/deletecar").setQueries(query)
                            .setAuthToken(_auth0.getAccessToken(), _userProfile.getString("userid")).runAsync();

                    _carModels.remove(position);
                    _adapter.notifyItemRemoved(position);
                }
                else {
                    Toast.makeText(this.getContext(), "Did not delete: Make sure name matches car name.", Toast.LENGTH_LONG).show();
                }
            });
        }
        else{
            //create and show the delete shared car popup window
            Dialog viewDeleteSharedCarForm = new Dialog(this.requireContext());
            viewDeleteSharedCarForm.setContentView(R.layout.delete_shared_car_form);
            viewDeleteSharedCarForm.show();

            Button removeAccessButton = viewDeleteSharedCarForm.findViewById(R.id.delete_shared_car_access_btn);
            removeAccessButton.setOnClickListener(v -> {
                //close the form
                viewDeleteSharedCarForm.dismiss();

                JSONObjectWrapper query = new JSONObjectWrapper();
                query.put("car_id", car_id);

                new HTTPRequest(getString(R.string.api_base_url) + "/removemycaraccess").setQueries(query)
                        .setAuthToken(_auth0.getAccessToken(), _userProfile.getString("userid")).runAsync();

                _carModels.remove(position);
                _adapter.notifyItemRemoved(position);
            });
        }
    }

    public void getNewCarImage(int position, int car_id){
        _addMaintenanceDataJSON.put("fragmentName", "CarManager");
        _addMaintenanceDataJSON.put("task", "Take Photo");
        _addMaintenanceDataJSON.put("position", position);
        _addMaintenanceDataJSON.put("car_id", car_id);
        CameraFragment fragment = CameraFragment.newInstance(_addMaintenanceDataJSON.toString());
        _fragmentSwitcher.switchFragment(fragment, getParentFragmentManager());
    }

    public void sendCarImageToServer(){
        JSONObjectWrapper query = new JSONObjectWrapper();
        query.put("car_id", _addMaintenanceDataJSON.getInt("car_id"));

        new HTTPRequest(getString(R.string.api_base_url) + "/uploadCarImage").setMethod("POST").setQueries(query)
                .setContentType("image/jpeg").setPhotoURI(this.requireContext().getContentResolver(), _addMaintenanceDataJSON.getString("photoURI"))
                .setAuthToken(_auth0.getAccessToken(), _userProfile.getString("userid")).setCallback(res -> {
                    JSONObjectWrapper resJSON = new JSONObjectWrapper(res);
                    if(resJSON.getString("message").equals("file uploaded successfully.")){
                        _adapter.notifyItemChanged(_addMaintenanceDataJSON.getInt("position"));
                    }
                }).runAsync();
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