package com.example.mototracker;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class MaintenanceLogRecyclerViewAdapter extends RecyclerView.Adapter<MaintenanceLogRecyclerViewAdapter.MyViewHolder> {
    private final RecyclerViewInterface _maintenanceLog_recycler_ViewInterface;
    private Context _context;
    private JSONArrayWrapper _maintenanceLogModels;

    public MaintenanceLogRecyclerViewAdapter(Context context, JSONArrayWrapper maintenanceLogModels, RecyclerViewInterface recyclerViewInterface){
        _context = context;
        _maintenanceLogModels = maintenanceLogModels;
        _maintenanceLog_recycler_ViewInterface = recyclerViewInterface;
    }

    @NonNull
    @Override
    public MaintenanceLogRecyclerViewAdapter.MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(_context);
        View view = inflater.inflate(R.layout.fragment_maintenance_log_recycler_row, parent, false);
        return new MaintenanceLogRecyclerViewAdapter.MyViewHolder(view, _maintenanceLog_recycler_ViewInterface);
    }

    @Override
    public void onBindViewHolder(@NonNull MaintenanceLogRecyclerViewAdapter.MyViewHolder holder, int position) {
        //assigning values to the views we created in the xml layout file based on the position of the recycler view
        holder._typeView.setText(_maintenanceLogModels.getJSONObjectWrapper(position).getString("service_type"));
        holder._dateView.setText(_maintenanceLogModels.getJSONObjectWrapper(position).getString("timestamp"));
        holder._costView.setText(_maintenanceLogModels.getJSONObjectWrapper(position).getString("cost"));
        holder._milesView.setText(_maintenanceLogModels.getJSONObjectWrapper(position).getString("miles"));
        holder._notesView.setText(_maintenanceLogModels.getJSONObjectWrapper(position).getString("notes"));
    }

    @Override
    public int getItemCount() {
        return _maintenanceLogModels.length();
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder{
        //manage the views for a row
        //acts as the onCreate method for a row
        public TextView _typeView;
        public TextView _dateView;
        public TextView _costView;
        public TextView _milesView;
        public TextView _notesView;

        public MyViewHolder(@NonNull View itemView, RecyclerViewInterface recyclerViewInterface) {
            super(itemView);
            _typeView = itemView.findViewById(R.id.maintenance_log_row_service_type);
            _dateView = itemView.findViewById(R.id.maintenance_log_row_date);
            _costView = itemView.findViewById(R.id.maintenance_log_row_cost);
            _milesView = itemView.findViewById(R.id.maintenance_log_row_miles);
            _notesView = itemView.findViewById(R.id.maintenance_log_row_notes);
        }
    }
}
