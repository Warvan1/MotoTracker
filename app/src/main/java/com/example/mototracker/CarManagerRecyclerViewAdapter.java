package com.example.mototracker;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

public class CarManagerRecyclerViewAdapter extends RecyclerView.Adapter<CarManagerRecyclerViewAdapter.MyViewHolder> {
    private final recyclerViewInterface _CarManager_recyclerViewInterface;
    private Context _context;
    private JSONArrayWrapper _carModels;

    public CarManagerRecyclerViewAdapter(Context context, JSONArrayWrapper carModels, recyclerViewInterface recyclerViewInterface){
        _context = context;
        _carModels = carModels;
        _CarManager_recyclerViewInterface = recyclerViewInterface;
    }

    @NonNull
    @Override
    public CarManagerRecyclerViewAdapter.MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        //inflate the layout of each of our rows using the fragment_car_manager_recycler_row.xml file
        LayoutInflater inflater = LayoutInflater.from(_context);
        View view = inflater.inflate(R.layout.fragment_car_manager_recycler_row, parent, false);

        return new CarManagerRecyclerViewAdapter.MyViewHolder(view, _CarManager_recyclerViewInterface);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        //assigning values to the views we created in the xml layout file based on the position of the recycler view
        holder._nameView.setText(_carModels.getJSONObjectWrapper(position).getString("name"));
        holder._yearView.setText(_carModels.getJSONObjectWrapper(position).getString("year"));
        holder._makeView.setText(_carModels.getJSONObjectWrapper(position).getString("make"));
        holder._modelView.setText(_carModels.getJSONObjectWrapper(position).getString("model"));
        holder._milageView.setText(_carModels.getJSONObjectWrapper(position).getString("milage"));
        //handle currentCar coloring
        if(_carModels.getJSONObjectWrapper(position).getBoolean("current_car")){
            holder._cardView.setCardBackgroundColor(_context.getResources().getColor(R.color.md_theme_tertiaryContainer_mediumContrast));
            holder._selectedView.setVisibility(View.VISIBLE);
        }
        else{
            holder._cardView.setCardBackgroundColor(_context.getResources().getColor(R.color.md_theme_primaryContainer_mediumContrast));
            holder._selectedView.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public int getItemCount() {
        return _carModels.length();
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder{
        //manage the views for a row
        //acts as the onCreate method for a row
        public TextView _nameView;
        public TextView _yearView;
        public TextView _makeView;
        public TextView _modelView;
        public TextView _milageView;
        public TextView _selectedView;
        public CardView _cardView;

        public MyViewHolder(@NonNull View itemView, recyclerViewInterface recyclerViewInterface){
            super(itemView);
            _nameView = itemView.findViewById(R.id.car_manager_row_name);
            _yearView = itemView.findViewById(R.id.car_manager_row_year);
            _makeView = itemView.findViewById(R.id.car_manager_row_make);
            _modelView = itemView.findViewById(R.id.car_manager_row_model);
            _milageView = itemView.findViewById(R.id.car_manager_row_milage);
            _selectedView = itemView.findViewById(R.id.car_manager_row_selected);
            _cardView = itemView.findViewById(R.id.car_manager_row_card);

            if(recyclerViewInterface != null){
                itemView.setOnClickListener(v -> {
                    int position = getAdapterPosition();
                    if(position != RecyclerView.NO_POSITION){
                        recyclerViewInterface.onItemClick(position);
                    }
                });
            }
        }
    }
}
