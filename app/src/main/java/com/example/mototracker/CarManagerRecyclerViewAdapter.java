package com.example.mototracker;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Objects;

public class CarManagerRecyclerViewAdapter extends RecyclerView.Adapter<CarManagerRecyclerViewAdapter.MyViewHolder> {
    private final RecyclerViewInterface _CarManager_recyclerViewInterface;
    private Context _context;
    private JSONArrayWrapper _carModels;
    private Auth0Authentication _auth0;
    private JSONObjectWrapper _userProfile;

    public CarManagerRecyclerViewAdapter(Context context, JSONArrayWrapper carModels, RecyclerViewInterface recyclerViewInterface){
        _context = context;
        _carModels = carModels;
        _CarManager_recyclerViewInterface = recyclerViewInterface;

        //get the user profile from the auth object
        _auth0 = Auth0Authentication.getInstance(context);
        if(_auth0.isAuthenticated()){
            _userProfile = _auth0.getUserProfile();
        }
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
        holder._milesView.setText(String.format(_context.getString(R.string.milesFormat),
                _carModels.getJSONObjectWrapper(position).getString("miles")));
        //handle currentCar coloring
        if(_carModels.getJSONObjectWrapper(position).getBoolean("current_car")){
            holder._cardView.setCardBackgroundColor(_context.getResources().getColor(R.color.md_theme_tertiaryContainer_mediumContrast));
            holder._selectedView.setVisibility(View.VISIBLE);
        }
        else{
            holder._cardView.setCardBackgroundColor(_context.getResources().getColor(R.color.md_theme_primaryContainer_mediumContrast));
            holder._selectedView.setVisibility(View.INVISIBLE);
        }
        //handle showing the share button if you own the car
        if(_carModels.getJSONObjectWrapper(position).getString("user_id").equals(_userProfile.getString("userid"))){
            holder._shareButtonView.setVisibility(View.VISIBLE);
        }
        else{
            holder._shareButtonView.setVisibility(View.GONE);
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
        public TextView _milesView;
        public TextView _selectedView;
        public Button _shareButtonView;
        public CardView _cardView;

        public MyViewHolder(@NonNull View itemView, RecyclerViewInterface recyclerViewInterface){
            super(itemView);
            _nameView = itemView.findViewById(R.id.car_manager_row_name);
            _yearView = itemView.findViewById(R.id.car_manager_row_year);
            _makeView = itemView.findViewById(R.id.car_manager_row_make);
            _modelView = itemView.findViewById(R.id.car_manager_row_model);
            _milesView = itemView.findViewById(R.id.car_manager_row_miles);
            _selectedView = itemView.findViewById(R.id.car_manager_row_selected);
            _shareButtonView = itemView.findViewById(R.id.car_manager_share_button);
            _cardView = itemView.findViewById(R.id.car_manager_row_card);

            if(recyclerViewInterface != null){
                itemView.setOnClickListener(v -> {
                    int position = getAdapterPosition();
                    if(position != RecyclerView.NO_POSITION){
                        recyclerViewInterface.onItemClick(position, 0);
                    }
                });

                //share button on click handler
                _shareButtonView.setOnClickListener(v -> {
                    int position = getAdapterPosition();
                    if(position != RecyclerView.NO_POSITION){
                        recyclerViewInterface.onItemClick(position, 1);
                    }

                });

                itemView.setOnLongClickListener(v -> {
                    int position = getAdapterPosition();
                    if(position != RecyclerView.NO_POSITION){
                        recyclerViewInterface.onItemLongClick(position);
                    }
                    return true;
                });
            }
        }
    }
}
