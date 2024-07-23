package com.example.mototracker;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

public class HomeFragment extends Fragment {
    //define an HomeFragment Interface for calling the HandleAuth
    public interface OnFragmentInteractionListener {
        void onHomeHandleAuth();
    }

    private OnFragmentInteractionListener _mListener;
    private Auth0Authentication _auth0;

    public HomeFragment() {
        // Required empty public constructor
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if(context instanceof OnFragmentInteractionListener){
            _mListener = (OnFragmentInteractionListener) context;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        _auth0 = Auth0Authentication.getInstance(this.getContext());
        Button loginButton = view.findViewById(R.id.home_login_button);

        if(_auth0.isAuthenticated()){
            loginButton.setText(getString(R.string.logout));
        }

        loginButton.setOnClickListener(v -> {
            if(_mListener != null){
                _mListener.onHomeHandleAuth();
            }
        });

        return view;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        _mListener = null;
    }
}