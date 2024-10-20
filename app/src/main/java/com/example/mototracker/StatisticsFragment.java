package com.example.mototracker;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import java.util.ArrayList;

public class StatisticsFragment extends Fragment {
    private Auth0Authentication _auth0;
    private JSONObjectWrapper _userProfile;
    private FragmentSwitcher _fragmentSwitcher;
    private JSONObjectWrapper _currentCarJSON;
    private JSONArrayWrapper _maintenanceLogModels;
    private String _graphType;
    private LineChart _chartView;
    private LineDataSet _mpgDataSet;
    private LineDataSet _dpgDataSet;
    private LineDataSet _dpmDataSet;

    public StatisticsFragment() {
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
        View view = inflater.inflate(R.layout.fragment_statistics, container, false);

        //access the fragment switcher object
        _fragmentSwitcher = FragmentSwitcher.getInstance();

        //check to make sure that we are authenticated
        _auth0 = Auth0Authentication.getInstance(this.getContext());
        if(!_auth0.isAuthenticated()){
            _fragmentSwitcher.switchFragment(new HomeFragment(), getParentFragmentManager());
            return view;
        }
        _userProfile = _auth0.getUserProfile();

        //access the chart view
        _chartView = view.findViewById(R.id.statistics_chart);

        //get the current car object
        new HTTPRequest(getString(R.string.api_base_url) + "/getcurrentcar")
                .setAuthToken(_auth0.getAccessToken(), _userProfile.getString("userid")).setCallback(res -> {
                    if(res.equals("null")){
                        return;
                    }
                    _currentCarJSON = new JSONObjectWrapper(res);

                    //set the title of the fragment header using the car name
                    TextView title = view.findViewById(R.id.statistics_title);
                    title.setText(String.format(getString(R.string.statistics_named),
                            _currentCarJSON.getString("name")));

                    //retrieve the data
                    getMaintenanceLogModelsFromAPI();
                }).runAsync();

        //setup filter dropdown menu
        Spinner statisticsSpinner = view.findViewById(R.id.statistics_spinner);
        ArrayAdapter<CharSequence> filter_type_adapter = ArrayAdapter.createFromResource(this.requireContext(),
                R.array.statistics_graphs, android.R.layout.simple_spinner_item);
        filter_type_adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        statisticsSpinner.setAdapter(filter_type_adapter);
        //item selected listener for our dropdown menu
        statisticsSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                _graphType = parent.getItemAtPosition(position).toString();
                updateChart();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        return view;
    }

    public void getMaintenanceLogModelsFromAPI(){
        JSONObjectWrapper queries = new JSONObjectWrapper();
        queries.put("filter", "Fuel");
        queries.put("statistics", 1);

        new HTTPRequest(getString(R.string.api_base_url) + "/getmaintenancelog").setQueries(queries)
                .setAuthToken(_auth0.getAccessToken(), _userProfile.getString("userid")).setCallback(res -> {
                    if(res.equals("null")){
                        return;
                    }
                    JSONObjectWrapper resJSON = new JSONObjectWrapper(res);
                    _maintenanceLogModels = resJSON.getJSONArrayWrapper("data");

                    //create our datasets from our models
                    ArrayList<Entry> mpgData = new ArrayList<>();
                    ArrayList<Entry> dpgData = new ArrayList<>();
                    ArrayList<Entry> dpmData = new ArrayList<>();
                    for(int i = 1; i < _maintenanceLogModels.length(); i++){
                        float miles = (float) _maintenanceLogModels.getJSONObjectWrapper(i).getInt("miles");
                        float milesNext = (float) _maintenanceLogModels.getJSONObjectWrapper(i-1).getInt("miles");
                        float gallons = (float) _maintenanceLogModels.getJSONObjectWrapper(i).getDouble("gallons");
                        float cost = (float) _maintenanceLogModels.getJSONObjectWrapper(i).getDouble("cost");
                        miles = miles - milesNext;

                        float value = 0;
                        if(miles >= 0 && gallons > 0){
                            value = miles / gallons;
                        }
                        mpgData.add(new Entry(i, value));

                        value = 0;
                        if(cost >= 0 && gallons > 0){
                            value = cost / gallons;
                        }
                        dpgData.add(new Entry(i, value));

                        value = 0;
                        if(cost >= 0 && miles > 0){
                            value = cost / miles;
                        }
                        dpmData.add(new Entry(i, value));
                    }

                    //init datasets
                    _mpgDataSet = new LineDataSet(mpgData, "Miles Per Gallon");
                    _dpgDataSet = new LineDataSet(dpgData, "Dollars Per Gallon");
                    _dpmDataSet = new LineDataSet(dpmData, "Dollars Per Mile");

                    //set dataset styles
                    _mpgDataSet.setLineWidth(4);
                    _mpgDataSet.setCircleRadius(4);
                    _mpgDataSet.setCircleHoleRadius(2);
                    _mpgDataSet.setValueTextSize(10);
                    _mpgDataSet.setValueTextColor(getResources().getColor(R.color.md_theme_onBackground));

                    _dpgDataSet.setLineWidth(4);
                    _dpgDataSet.setCircleRadius(4);
                    _dpgDataSet.setCircleHoleRadius(2);
                    _dpgDataSet.setValueTextSize(10);

                    _dpmDataSet.setLineWidth(4);
                    _dpmDataSet.setCircleRadius(4);
                    _dpmDataSet.setCircleHoleRadius(2);
                    _dpmDataSet.setValueTextSize(10);

                    //set chart view styles
                    XAxis xAxis = _chartView.getXAxis();
                    xAxis.setEnabled(false);
                    YAxis yAxisLeft = _chartView.getAxisLeft();
                    yAxisLeft.setTextSize(15);
                    yAxisLeft.setTextColor(getResources().getColor(R.color.md_theme_onBackground));
                    YAxis yAxisRight = _chartView.getAxisRight();
                    yAxisRight.setTextSize(15);
                    yAxisRight.setTextColor(getResources().getColor(R.color.md_theme_onBackground));

                    Legend legend = _chartView.getLegend();
                    legend.setTextSize(15);
                    legend.setTextColor(getResources().getColor(R.color.md_theme_onBackground));

                    Description description = new Description();
                    description.setEnabled(false);
                    _chartView.setDescription(description);

                    //create the graph after we have retrieved the data
                    _graphType = "Miles Per Gallon";
                    updateChart();
                }).runAsync();
    }

    public void updateChart(){
        if(_currentCarJSON == null || _maintenanceLogModels == null){
            return;
        }

        //select dataset
        ArrayList<ILineDataSet> dataSets = new ArrayList<>();
        if(_graphType.equals("Miles Per Gallon")){
            dataSets.add(_mpgDataSet);
        }
        else if(_graphType.equals("Dollars Per Gallon")){
            dataSets.add(_dpgDataSet);
        }
        else if(_graphType.equals("Dollars Per Mile")){
            dataSets.add(_dpmDataSet);
        }

        //update _chartView using dataset
        LineData lineData = new LineData(dataSets);
        _chartView.setData(lineData);
        _chartView.invalidate();
   }
}