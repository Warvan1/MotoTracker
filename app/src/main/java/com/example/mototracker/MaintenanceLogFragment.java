package com.example.mototracker;

import android.app.Dialog;
import android.graphics.PorterDuff;
import android.os.Bundle;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.Collections;

public class MaintenanceLogFragment extends Fragment implements RecyclerViewInterface {
    private static final String ARG_METADATA_JSON = "metadataJSON";
    private JSONObjectWrapper _addMaintenanceDataJSON = new JSONObjectWrapper();
    private Auth0Authentication _auth0;
    private JSONObjectWrapper _userProfile;
    private FragmentSwitcher _fragmentSwitcher;
    private JSONObjectWrapper _currentCarJSON;
    private JSONArrayWrapper _maintenanceLogModels;
    private RecyclerView _recyclerView;
    private MaintenanceLogRecyclerViewAdapter _adapter;
    private String _filter_service_type;
    private LinearLayout _pagingBar;
    private int _logTotalPages;
    private int _logPage;

    public MaintenanceLogFragment() {
        // Required empty public constructor
    }

    public static MaintenanceLogFragment newInstance(String metadataJSON){
        MaintenanceLogFragment fragment = new MaintenanceLogFragment();
        Bundle args = new Bundle();
        args.putString(ARG_METADATA_JSON, metadataJSON);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(getArguments() != null){
            _addMaintenanceDataJSON = new JSONObjectWrapper(AddMaintenanceDialog.parseCameraOutput(getArguments().getString(ARG_METADATA_JSON)));
        }
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

        //get our recyclerView for our adapter
        _recyclerView = view.findViewById(R.id.maintenance_log_recycler_view);

        //get our paging bar and set its initial status to invisible
        _pagingBar = view.findViewById(R.id.maintenance_log_paging_bar);
        _pagingBar.setVisibility(View.GONE);

        //access add maintenance floating action button
        FloatingActionButton addMaintenanceButton = view.findViewById(R.id.add_maintenance_btn);
        //fix the color of the floating action button icon
        addMaintenanceButton.getDrawable().setColorFilter(ContextCompat.getColor(this.requireContext(), R.color.md_theme_onPrimary), PorterDuff.Mode.SRC_IN);
        //floating action button on click method for adding maintenance
        addMaintenanceButton.setOnClickListener(v -> {
            //open the add maintenance dialog box
            _addMaintenanceDataJSON.put("fragmentName", "MaintenanceLog");
            AddMaintenanceDialog.open(this.requireContext(), _fragmentSwitcher, getParentFragmentManager(), _currentCarJSON, _addMaintenanceDataJSON, callback -> {
                getMaintenanceLogModelsFromAPI();
            });
        });

        //get the current car object
        new HTTPRequest(getString(R.string.api_base_url) + "/getcurrentcar")
                .setAuthToken(_auth0.getAccessToken(), _userProfile.getString("userid")).setCallback(res -> {
                    if(res.equals("null")){
                        return;
                    }
                    _currentCarJSON = new JSONObjectWrapper(res);

                    //set the title of the fragment header using the car name
                    TextView title = view.findViewById(R.id.maintenance_log_title);
                    title.setText(String.format(getString(R.string.maintenanceLog_named),
                            _currentCarJSON.getString("name")));

                    //only show the add maintenance button if we have permission to edit the car
                    if(!_currentCarJSON.getString("permissions").equals("Edit")){
                        addMaintenanceButton.setVisibility(View.GONE);
                    }
                    else{
                        addMaintenanceButton.setVisibility(View.VISIBLE);
                    }

                    //open an add car dialog if we have parsed text input to the fragment
                    String parsedText = "";
                    try{
                        parsedText = _addMaintenanceDataJSON.getString("parsedText");
                    }
                    catch (RuntimeException e){}
                    if(!parsedText.isEmpty()){
                        //open the add maintenance dialog box
                        _addMaintenanceDataJSON.put("fragmentName", "MaintenanceLog");
                        AddMaintenanceDialog.open(this.requireContext(), _fragmentSwitcher, getParentFragmentManager(), _currentCarJSON, _addMaintenanceDataJSON, callback -> {
                            getMaintenanceLogModelsFromAPI();
                        });
                    }

                }).runAsync();

        //setup filter dropdown menu
        Spinner filter_type_spinner = view.findViewById(R.id.filter_service_types_spinner);
        ArrayAdapter<CharSequence> filter_type_adapter = ArrayAdapter.createFromResource(this.requireContext(),
                R.array.service_types_all, android.R.layout.simple_spinner_item);
        filter_type_adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        filter_type_spinner.setAdapter(filter_type_adapter);
        //item selected listener for our dropdown menu
        filter_type_spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                _filter_service_type = parent.getItemAtPosition(position).toString();
                getMaintenanceLogModelsFromAPI();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
        return view;
    }

    @Override
    public void onItemClick(int position, int id) {

    }

    @Override
    public void onItemLongClick(int position, int id) {
        if(!_currentCarJSON.getString("permissions").equals("Edit")){
            return;
        }
        int maintenance_id = _maintenanceLogModels.getJSONObjectWrapper(position).getInt("maintenance_id");

        //create and show the delete maintenance popup window
        Dialog viewDeleteMaintenanceForm = new Dialog(this.requireContext());
        viewDeleteMaintenanceForm.setContentView(R.layout.delete_maintenance_form);
        viewDeleteMaintenanceForm.show();

        //delete maintenance entry button onClick listener
        Button deleteMaintenanceButton = viewDeleteMaintenanceForm.findViewById(R.id.delete_maintenance_forever_btn);
        deleteMaintenanceButton.setOnClickListener(v -> {
            //close the form
            viewDeleteMaintenanceForm.dismiss();

            JSONObjectWrapper query = new JSONObjectWrapper();
            query.put("maintenance_id", maintenance_id);

            new HTTPRequest(getString(R.string.api_base_url) + "/deletemaintenancelog").setQueries(query)
                    .setAuthToken(_auth0.getAccessToken(), _userProfile.getString("userid")).setCallback(res -> {
                        getMaintenanceLogModelsFromAPI();
                    }).runAsync();

        });
    }

    public void getMaintenanceLogModelsFromAPI(){
        JSONObjectWrapper queries = new JSONObjectWrapper();
        //handle filter by service type
        if(_filter_service_type != null && !_filter_service_type.equals("All")){
            queries.put("filter", _filter_service_type);
        }
        //grab specific page if set
        if(_logPage >= 1){
            queries.put("page", _logPage);
        }

        new HTTPRequest(getString(R.string.api_base_url) + "/getmaintenancelog").setQueries(queries)
                .setAuthToken(_auth0.getAccessToken(), _userProfile.getString("userid")).setCallback(res -> {
                    if(res.equals("null")){
                        return;
                    }
                    JSONObjectWrapper resJSON = new JSONObjectWrapper(res);
                    _maintenanceLogModels = resJSON.getJSONArrayWrapper("data");
                    _logTotalPages = resJSON.getInt("totalPages");
                    _logPage = resJSON.getInt("page");

                    //update the recycler view with a new adapter
                    _adapter = new MaintenanceLogRecyclerViewAdapter(this.getContext(), _maintenanceLogModels, this);
                    _recyclerView.setAdapter(_adapter);
                    _recyclerView.setLayoutManager(new LinearLayoutManager(this.getContext()));

                    //handle the paging bar
                    handlePagingBar();
                }).runAsync();

        //update the current car object
        new HTTPRequest(getString(R.string.api_base_url) + "/getcurrentcar")
                .setAuthToken(_auth0.getAccessToken(), _userProfile.getString("userid")).setCallback(res -> {
                    if(res.equals("null")){
                        return;
                    }
                    _currentCarJSON = new JSONObjectWrapper(res);
                }).runAsync();
    }

    public void handlePagingBar(){
        if(_logTotalPages > 1){
            _pagingBar.setVisibility(View.VISIBLE);
        }
        else{
            _pagingBar.setVisibility(View.GONE);
            return;
        }

        //handle the x / x in the middle of the paging bar
        TextView pagingDetail = _pagingBar.findViewById(R.id.maintenance_log_paging_detail);
        pagingDetail.setText(String.format(getString(R.string.pageDetail), _logPage, _logTotalPages));

        //handle the previous and next buttons
        Button previousButton = _pagingBar.findViewById(R.id.maintenance_log_paging_previous);
        Button nextButton = _pagingBar.findViewById(R.id.maintenance_log_paging_next);
        if(_logPage > 1){
            previousButton.setEnabled(true);
            previousButton.setOnClickListener(v -> {
                _logPage--;
                getMaintenanceLogModelsFromAPI();
            });
        }
        else{
            previousButton.setEnabled(false);
        }
        if(_logPage < _logTotalPages){
            nextButton.setEnabled(true);
            nextButton.setOnClickListener(v -> {
                _logPage++;
                getMaintenanceLogModelsFromAPI();
            });
        }
        else{
            nextButton.setEnabled(false);
        }
    }
}